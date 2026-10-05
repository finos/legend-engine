---
name: pure-lsp-go
description: "Executes Pure code through the running Legend Pure LSP bridge as a fast REPL-style dev loop — compiles everything currently loaded and runs a function literally named go():Any[*], instead of writing a full PCT/JUnit test just to try something out. Returns BOTH the function's return value (typed, in returnValue — primitives, enums and collections of those need no print()) and anything it printed (in output); only complex returns like a Class instance or Relation still need print(). Use whenever the user wants to run/execute/try a Pure function or snippet quickly, asks 'can I just run this Pure code', wants to sanity-check a Pure function's output with sample inputs, or mentions the LSP's execute/go/REPL capability."
---

# Execute Pure code via the LSP bridge's go()

Requires a running bridge (`pure-lsp health`) — and never `curl` it, `pure-lsp` is the only
reliable client. See `references/lsp-devloop-usage-rules.md` for both, plus the `welcome.pure` and
database-setup conventions this skill assumes.

## The mental model

There's exactly one designated entry point: a function literally named `go`, with signature
`go():Any[*]` (or `go():String[*]` / `go():String[1]`). It's not "call any function with
arguments" — it's "run this one zero-argument function." To exercise your actual function with
specific inputs, write `go()` as a thin wrapper around it:

```pure
function myCustomFunction(x:Integer[1], y:Integer[1]):Integer[1]
{
  $x + $y;
}

function go():Any[*]
{
  print('result = ' + myCustomFunction(19, 23)->toString(), 1);
}
```

## Steps

Use the repo-root **`welcome.pure`** (`$LEGEND_ENGINE_ROOT/welcome.pure`) as the `go()` host — a
git-ignored REPL file that already defines `function go():Any[*]`. Do not create a fresh
`/tmp/<name>.pure` per run: it collides with orphan overlays, and a file outside a registered repo
is silently ignored (`No go() function found in compiled sources`). Full reasoning:
`references/lsp-devloop-usage-rules.md`.

1. Edit `welcome.pure`'s `go()` body with what you want to run (add any missing `import`; comment out
   pre-existing scaffolding lines that fail locally, e.g. the `getTestConnection(DatabaseType.Snowflake)`
   line).
2. Execute it, passing the file so it is (re)compiled atomically before `go()` runs:
   ```bash
   pure-lsp go $LEGEND_ENGINE_ROOT/welcome.pure
   ```
   Returns `{"success": bool, "error": str|null, "output": str}` plus return-value fields.
   `output` is whatever `go()` printed via `print(...)`; its **return value** arrives separately in
   `returnValue`, with `returnKind` / `returnType` / `returnSize` / `returnTruncated` alongside it.

   So `go()` does not need to print a primitive result to make it visible — returning it is enough.
   Only a `complex` return (a Class instance, Relation or lambda) still needs `print()`: those
   report their type and size with a `null` value rather than having their object graph walked.
   See the `pure-lsp-execute` skill for the full field table.

To unload a pushed file (an orphan `go()` overlay, say): `POST /delete {"uri": ...}` — deleting it
on disk does not clear the overlay. See the conventions reference.

### Multi-file: compile several files, then execute, in one call

When `go()` and the function(s) it calls live in separate files (or you're validating several
related changes at once), pass the files straight to `go` instead of `check`-ing each one first:

```bash
pure-lsp go Main.pure Helper.pure OtherHelper.pure
```

One atomic compile of the whole batch, then `go()` — argument order is irrelevant, and a single
broken file rolls the whole thing back (`success: false`, `errorUri` naming the real culprit) so
`go()` never runs against a partial model. **Always pass every edited file in one call** rather
than `check`ing them one at a time; that's rule 2 in `references/lsp-devloop-usage-rules.md`.

## Things worth knowing

- `go` **recompiles the entire session**, not just the file you just checked. If some unrelated
  file already loaded into this session has a compile error, `go()` will fail even though your
  new snippet is perfectly fine — read the error's source file/location before assuming your code
  is broken.
- `go` runs on the **interpreted** engine. By default that means no plan generation — a plain
  `execute()` runs fully interpreted (see "Execution routing" below for how to change that). A
  successful interpreted `go()` is a fast dev-loop signal, not proof the compiled engine (or PCT)
  will agree — but with a backend Server wired in you can make it exercise the real plan-gen +
  execute path.
- On failure, `output`/`error` includes whatever was printed before the crash plus either a Pure
  stack trace (for a Pure-level exception) or a raw Java stack trace (for anything else) — read
  the whole message, the useful part isn't always at the top.

## Execution routing: interpreted vs. plan-gen + backend Server

`meta::pure::router::execute(...)` (in `core/pure/router/router_entry.pure`) wraps the query in the
`mayExecuteLegendTest(serverFn, interpretedFallbackFn)` native, which branches on whether the LSP
JVM has the `legend.test.server.*` system properties set:

- **Not set** → fully interpreted, no plan generation. This is the default. Note that some store
  paths (e.g. RFPM relational graph-fetch) are simply **unimplemented** in the interpreted executor
  and will fail here even though they work in the real engine — that's a signal to use the server
  path, not a bug in your code. (The fallback also hardcodes `noDebug()`.)
- **Set** → `legendExecute`/`alloyExecute` in
  `core/pure/protocol/vX_X_X/invocations/execution.pure`: the real plan-gen + execute path, the same
  one `Test_*_UsingPureClientTestSuite` uses. `go()` output then shows the routing trace and the
  generated SQL. How to get them set: see the conventions reference.

Which branch you get, and what happens inside `legendExecute` once you're on the server path, is
governed by Pure options (`ForceInterpreted`, `ExecPlan`, `PlanLocal`, `FullInteractiveExec`,
`ExecDebug`, `ShowLocalPlan`) that you can flip live with no restart.

**Those options are documented in one place: the `pure-lsp-set-option` skill** — what each one does,
its default, how they interact, and which combination to set for interpreted vs local/remote
plan-gen (including how to check the backend is actually wired in, rather than silently falling
back to interpreted).

Only the `go()`-relevant summary is repeated here:

- Out of the box (no options set, no backend wired in) `go()` is **fully interpreted**.
- The fast dev-loop combination is `ExecPlan` + `PlanLocal` + `FullInteractiveExec`: plan generation
  runs locally against your live Pure graph and only `executePlan($host,$port)` goes to the backend,
  so a Pure-only edit needs no jar rebuild and no server restart.
- `ForceInterpreted` is the only reliable way to stay interpreted once a backend is configured — it
  short-circuits before `mayExecuteLegendTest` is reached.

Quick recipe for a server-routed `go()`: `pure-lsp-launch-engine` does it in one step (backend +
jvm-args, on by default). Then follow the database-setup convention
(`createTablesAndFillDb()` first, `meta::pure::router::execute` not `meta::pure::mapping::execute`)
from `references/lsp-devloop-usage-rules.md`.
