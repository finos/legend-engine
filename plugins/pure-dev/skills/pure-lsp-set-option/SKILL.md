---
name: pure-lsp-set-option
description: "Sets or unsets a Pure runtime option (the flags behind isOptionSet('...')) live in the running LSP, no restart. Covers ForceInterpreted, ExecPlan, PlanLocal, FullInteractiveExec, ExecDebug and ShowLocalPlan, and is the single source of truth for what each does and which combination to set for interpreted vs local/remote plan-gen. Use for 'set a pure option', 'turn on debug for execute', 'switch to local/remote plan generation', 'force interpreted execution', or any change to execute()/go() behaviour gated behind isOptionSet."
---

# Set / unset a Pure runtime option in the LSP

`isOptionSet('X')` inside Pure resolves, in the LSP JVM, to `Boolean.getBoolean("pure.options.X")`
— read live on every call (default `RuntimeOptions.systemPropertyOptions("pure.options.")`). So a
Pure option can be flipped at runtime by setting/clearing that system property inside the LSP JVM.
The bridge exposes this over HTTP so you don't need to restart the (expensive, ~3-min-boot)
engine-scale LSP just to change a flag.

Never `curl` the bridge; `bin/pure-lsp-option` (below) uses Python's `urllib`. See `references/lsp-devloop-usage-rules.md`.

## Endpoints

- `POST /set-option   {"name": "X"}` → `System.setProperty("pure.options.X","true")`  → `isOptionSet('X')` becomes true.
- `POST /unset-option {"name": "X"}` → `System.clearProperty("pure.options.X")`          → `isOptionSet('X')` becomes false.
- Either endpoint also accepts an explicit `{"value": true|false}`; the path just supplies the default.

Both return `{"success": true, "name": "X", "effective": <bool>}` where `effective` is the live
`isOptionSet` value read back immediately after the change — use it to confirm the toggle landed.

## Usage

All the HTTP mechanics (building the request body, POSTing to `/set-option`/`/unset-option`, error
handling) are implemented deterministically in `bin/pure-lsp-option` — run it rather than re-deriving
an inline Python call by hand:

```bash
pure-lsp-option <set|unset> <NAME> [--value true|false] [--port N]
```

`--port` is optional: without it the script resolves `$PURE_LSP_PORT`, else a running bridge
discovered from its sidecar, else 8991. Pass `--port <N>` explicitly only when several bridges are
up and you mean a specific one.

Examples:

```bash
pure-lsp-option set   ForceInterpreted                  # bypass mayExecuteLegendTest entirely, even if a backend is up
pure-lsp-option set   PlanLocal                          # plan-gen runs LOCALLY (no longer affects FULL_INTERACTIVE)
pure-lsp-option set   FullInteractiveExec                # FULL_INTERACTIVE model, independent of PlanLocal
pure-lsp-option set   ExecPlan                           # use the plan-gen+execute path at all
pure-lsp-option unset ExecDebug                          # quiet (noDebug) plan-gen
```

On success it prints the bridge's JSON response line to stdout and exits 0:
```json
{"success": true, "name": "ForceInterpreted", "effective": true}
```
`effective` is the live `isOptionSet` value read back immediately after the change — use it to confirm
the toggle landed. Non-zero exit (`1` = bad usage, `3` = HTTP/connection error) means the call did not
land; the real error is printed to stderr — don't assume the option changed.

## The options that matter for execute()/go() routing

These are the flags currently gated in `core/pure/protocol/vX_X_X/invocations/execution.pure`
(`legendExecute`) and `core/pure/router/router_entry.pure` (`meta::pure::router::execute`). Verify
against the current source before relying on exact behaviour — they are dev-loop overrides, not
long-term protocol:

| Option                | When SET                                                        | Default (UNSET)                          |
|-----------------------|----------------------------------------------------------------|------------------------------------------|
| `ForceInterpreted`    | `router::execute` calls the plain interpreted `execute()` directly, BEFORE `mayExecuteLegendTest` runs — bypasses backend routing entirely even if a backend Server is configured | Normal `mayExecuteLegendTest` routing (backend wins if configured, regardless of `ExecPlan`) |
| `ExecPlan`            | Route through the plan-gen + `executePlan` path (only reached when `ForceInterpreted` is unset and a backend is configured) | Old `alloyExecute` path |
| `PlanLocal`           | Plan-gen runs LOCALLY (interpreted, live Pure)                  | Remote `generatePlan` on the backend Server (its jars decide the plan) |
| `FullInteractiveExec` | `FULL_INTERACTIVE` execution mode (model inlined from the local graph) — independent of `PlanLocal` now | `SEMI_INTERACTIVE` (metadata-server pointer model) |
| `ExecDebug`           | `debug()` verbosity (routing trace + per-node SQL) in plan-gen and the interpreted fallback | `noDebug()` — quiet |
| `ShowLocalPlan`       | Print the full bound plan via `planToString(true, ...)`         | Not printed                              |

Notes:
- `ForceInterpreted` shadows `ExecPlan`/`PlanLocal` when set — the whole backend-routing branch is
  skipped, so it doesn't matter what the other two are set to. Nothing stops you from leaving them set
  alongside it, just know they have no effect while `ForceInterpreted` is on.
- `PlanLocal` is the key to fast Pure iteration: local plan-gen uses the live Pure graph in the LSP
  session, so a Pure-only edit needs no jar rebuild — only Java execution goes to the backend
  Server. With `PlanLocal` unset, the server's own jars decide the plan. It used to also gate
  `FULL_INTERACTIVE`/`SEMI_INTERACTIVE` — that's now the independent `FullInteractiveExec` flag, which
  the LSP dev-loop flow sets unconditionally regardless of `PlanLocal`.
- These are OR-independent system properties; setting one does not clear another. If you want a
  clean state, explicitly unset the ones you are not using.
- Changing a Pure option requires nothing but the HTTP call — the next `go()`/`execute()` reads the
  new value. (Contrast: editing the `legend/setOption` handler itself is a Java change to the LSP
  server and needs a rebuild + restart.)

## Choosing a combination

Three execution paths, plus two independent verbosity toggles. **Set the trues and explicitly unset
everything else** — these are sticky system properties on a long-lived daemon, so a `PlanLocal` or
`ExecDebug` left over from an earlier query silently changes the next one.

| intent | `ForceInterpreted` | `ExecPlan` | `PlanLocal` |
|---|:--:|:--:|:--:|
| fully interpreted — no plan-gen, no backend call | **set** | unset | unset |
| plan-gen locally, against your live Pure edits *(the dev-loop default)* | unset | **set** | **set** |
| plan-gen on the backend, from its jars *(what a released build would do)* | unset | **set** | unset |

| toggle | set | unset *(default)* |
|---|---|---|
| `ExecDebug` | routing trace + generated SQL | quiet |
| `ShowLocalPlan` | prints the bound plan | not printed (and n/a unless plan-gen is local) |

`FullInteractiveExec` is not a per-run choice: set it once for any LSP-driven session and leave it.
You are iterating on a live local graph, and `SEMI_INTERACTIVE`'s metadata-pointer model would not
reflect your uncommitted edits.

**Preconditions.** The interpreted row needs nothing — `ForceInterpreted` bypasses a configured
backend rather than letting it win the routing decision. Both plan-gen rows need the backend up on
9095 **and** the LSP started with the `-Dlegend.test.server.*` jvm-args (`pure-backend-start`, or
`pure-lsp-launch-engine`, which does both). Miss the jvm-args and `execute()`
silently runs interpreted regardless of `ExecPlan` — you get a clean passing result while believing
you exercised plan-gen. Check before trusting one:

```bash
ps -C java -o args= | grep -q 'legend\.test\.server\.host' \
  && echo "backend wired in" || echo "NOT wired in - execute() will run interpreted"
```

## When the option lives in a jar-resident core file

`execution.pure` and `router_entry.pure` come from the compiled-core JAR on the LSP classpath by
default, not from a workspace source you can push. Setting/unsetting an *option* they already read
works live (above). But *adding a new `isOptionSet(...)` gate* to those files means editing the
source first, then validating it one of two ways:
- **Source-root legend-engine too**: restart the LSP with `legend-engine` added as a second
  `--repo-root` alongside `legend-pure` (see `references/lsp-devloop-internals.md`) — this
  makes `core/pure/**` (including `execution.pure`/`router_entry.pure`) source-backed, so
  `pure-lsp check <file>` works directly and the new gate is live for `go()` in that same session.
  This is the fast path and doesn't need a Maven rebuild at all.
- **Jar-only session**: overlay the edited `.pure` into the `.m2` jar
  (`jar uf <jar> -C <stage> <entry>`) and restart the LSP, or do a full `mvn clean install` of that
  module.

Either way, do NOT try to push those core files into a *plain* (legend-pure-only) session as
overlays — the LSP cannot write them there and it will fall into a recovery loop.
