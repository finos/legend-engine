# Pure LSP dev-loop: usage rules and shared conventions

Two hard-won rules for driving the dev loop, then the conventions every pure-lsp skill assumes.
(See `lsp-devloop-internals.md` for the mechanics behind them, and the `pure-lsp-set-option` skill
for the execute()/go() routing options.)

Skills state these in one line and link here for the reasoning — so the reasoning lives in exactly
one place. If you are following a skill and a rule below contradicts it, this file is the one to
trust; say so rather than silently picking one.

## 1. Keep the LSP daemon long-running — don't thrash it into recovery

Starting the daemon is expensive (engine-scale ~3 min boot). Launch it as a real background task so
it survives tool-call teardown (and use socket-daemon mode, `--socket-port`, so it survives even a
task stop). Avoid actions that push it into `recovering`/`failed`.

- **Why:** a recovery loop defeats the whole point of the dev loop — a warm session — and forces a
  multi-minute rebuild.
- **How to apply:** check `pure-lsp status` before heavy operations; if it's `recovering`, wait for
  `ready` rather than piling on more pushes. Prefer one atomic multi-file submission over many
  sequential single-file `check` calls, which can thrash a core-file session into recovery.

## 2. Submit ALL edited files together to `go` (executeGo), not one at a time

`executeGo`/`go` takes a `files` param and compiles every submitted file in one atomic pass
(`SourceMutationService#applyBulkChangesAndCompile`) before running `go()`. Drive it as
`pure-lsp go <file1> <file2> ... <scratchGoFile>`, listing every file you edited plus the scratch
`go()` wrapper (or the canonical `welcome.pure`).

- **Why:** individually pushing core files (e.g. `execution.pure`, `router_entry.pure`) via separate
  `check` calls can push the session into recovery. The atomic batch is the safe path.
- **How to apply:** collect the full set of edited `.pure` files for the change under test and pass
  them all in a single `go` (or `check-many`) call. Order does not matter — the atomic compile sees
  them all at once, so cross-file refs resolve. A broken file fails the whole call (rolls back),
  which is the desired all-or-nothing behavior.

## 3. Don't run hundreds of tests through the LSP — past a point, Maven is the right tool

A scoped run (`pure-lsp execute --package/--source`, or the IDE's test tree) executes on the
**interpreted** engine. Maven/surefire runs the same tests **compiled**. For a quick loop over a file
or a leaf package the LSP wins easily — no build, warm graph, sub-second feedback. At suite scale it
loses badly, and the gap is far bigger than "a bit slower".

Measured on this box, same tests both ways:

| Scope | LSP (interpreted) | surefire (compiled) |
|---|---|---|
| `meta::pure::functions::string::tests` (243 tests) | **1.4 s** | — |
| `meta::analytics::lineage::tests` + 2 siblings (51 tests) | **83 s** | **8.2 s** |
| `meta::pure::functions` (~400 tests) | **>30 min, abandoned** | **9.7 s** |

- **Why:** per-test cost swings ~200x with what the test *does*, not how many there are. Plain
  Pure-language assertions (string/collection) are ~5 ms interpreted; anything that drives
  `execute()`/plan-gen/relational/graphFetch/lineage is ~1-3 s interpreted and dominates everything
  else. A long run also holds the graph **read lock** for its whole duration, so `check`/`go`/the
  auto-sync hook block behind it — which is the real reason to keep runs short, even though they can
  now be stopped (Ctrl-C cancels; `pure-lsp cancel-tests --all` clears anything abandoned).
- **How to apply — the threshold is wall-clock, not test count:**
  - **Quick iteration (the sweet spot): one file, or one leaf subpackage — roughly ≤50 tests, or any
    scope you've seen finish in under ~30 s.** Use `--source <file>` or the deepest package that
    still covers your change; add `--no-recursive` to stop a parent package pulling in subtrees.
  - **Sanity-check before widening.** Run the leaf package first and read the reported `in N.Ns`. If
    it implies more than ~2 s/test, do not run the parent package — that is a plan-gen/relational
    suite and it will not finish in a useful time.
  - **Switch to Maven when the scope exceeds ~200 tests, or the estimate exceeds ~5 minutes** —
    whichever comes first. Five minutes is the point where holding the session's read lock costs more
    than the rebuild would, and a compiled run is the better answer anyway:
    ```bash
    mvn surefire:test -pl <module> -Dtest=<TestClass> -DskipTests=false
    ```
    (needs a prior `mvn clean install -pl <module> -DskipTests`; never plain `mvn test` on a
    Pure-source module — see the root CLAUDE.md.)
  - **Never run a whole top-level package** (`meta::pure::functions`, `meta::relational`) through the
    LSP. That is always a Maven job.

---

# Shared conventions

## Talking to the bridge

**Never `curl` the bridge** — it is blocked by this environment's permissions. `pure-lsp` talks to
it over HTTP via Python's `urllib` and is the only reliable client. Every bridge operation has a
`pure-lsp` subcommand (`health`, `status`, `check`, `check-many`, `go`, `execute`,
`execute-parallel`, `cancel-tests` — `execute`/`execute-parallel` also take `--package`/`--source`
to run a whole test scope); if you think one is missing, it is more likely you want `--help` than a
raw HTTP call.

**You do not need to know the port.** `pure-lsp` resolves, in order: `$PURE_LSP_PORT`, then a live
bridge discovered from the `/tmp/pure_lsp_server_<port>.json` sidecar each bridge writes at launch,
then 8991. Pass `--port` only to disambiguate when several bridges are up. (`pure-lsp-option` and
the PostToolUse sync hook use the same resolution. They used to hardcode 8992, which no launcher
binds — the hook consequently no-opped silently against every real session.)

**`port` in `pure-lsp status` output is the daemon's SOCKET port, not the bridge's HTTP port.** Use
`pure-lsp health` for the HTTP port.

## Before any check/go/execute: the bridge must be up and ready

`pure-lsp status` must report `ready` (or `degraded`). If nothing is listening, start one:
`pure-lsp-launch-engine` (legend-engine scope) is the no-decisions entrypoint (wraps `pure-lsp-launch
<project>`); `pure-lsp-connect` attaches to a daemon that is already running. If `status` is
`recovering`/`failed`, see rule 1 and `pure-lsp-restart` (documented in `pure-lsp-status`).

## `welcome.pure` is the canonical `go()` host

Use the target repo's root `welcome.pure` (`$LEGEND_ENGINE_ROOT/welcome.pure`) as the scratch host
for `go()` — `pure-lsp-launch-engine` creates a minimal one for you if it's missing and the checkout
`.gitignore`s it. Do **not** create a fresh `/tmp/<name>.pure` per run. Two independent reasons:

1. **Orphan overlays.** The warm session keeps every pushed file as an overlay. Deleting the file on
   disk does NOT clear it, so a second `go()` definition collides:
   `The function 'go__Any_MANY_' is defined more than once`.
2. **Files outside a repo root are silently ignored.** A `.pure` that is not inside a registered
   Pure repository (one with a `*/src/main/resources/*.definition.json`) is never loaded. `check`
   returns `{"diagnostics": []}` with exit 0 — looking clean for code that does not compile at all —
   and `go` fails with `No go() function found in compiled sources`. This includes a file at a repo's
   bare root. `welcome.pure` works anyway because that exact filename is special-cased in the LSP's
   URI-to-source mapping; rename it and it stops compiling. For ad-hoc content that belongs to no
   module, pipe it via stdin (`pure-lsp check -`), which has no on-disk identity and is compiled for
   real.

To unload a pushed file (an orphan `go()` overlay, or anything you no longer want compiled):
`pure-lsp` has no subcommand for it — POST `/delete {"uri": ...}` to the bridge, which maps to the
LSP's `legend/deleteFile` and clears open-docs + runtime + overlay, then recompiles. Accepts a
`file://` uri, an absolute path, or a raw sourceId. `removed: false` means it was not in the session
(not an error). Reusing `welcome.pure` means you rarely need this.

## Running a query that touches the database

`<<test.BeforePackage>>` setUp does **not** run under `go()` or a direct single-function `execute`.
(It *does* run under a scoped `execute --package`/`--source` run, which brackets each package node
with its own hooks — that is the easiest way to get correct setup.) For a single function, call the
suite's `...::createTablesAndFillDb()` yourself first, and use
`meta::pure::router::execute(f, mapping, testRuntime(db), relationalExtensions())` — **not**
`meta::pure::mapping::execute`.

Concurrent writers race on the shared H2 (two `createTablesAndFillDb()` calls →
`Table PERSONTABLE already exists`). Do setup once, serially, then fan out read-only work with
`pure-lsp execute-parallel`. In a scoped run the hooks are already serial, but if the *tests*
themselves write to the shared DB, run the scope with `pure-lsp execute --package ...` (serial)
rather than `execute-parallel`.

## Interpreted vs. real plan-gen

`go()`/`execute` run the **interpreted** engine unless a backend engine Server is up AND the LSP JVM
was launched with the `-Dlegend.test.server.*` jvm-args pointing at it. That wiring is a launch-time
decision — it cannot be added to a running daemon without restarting it. `pure-lsp-launch-engine`
does it by default (`--no-backend` opts out); `pure-backend-start` brings up the backend alone; a
hand-built `pure-lsp-server` call takes `--jvm-arg` passthrough.

Once wired, which path a query actually takes is controlled by Pure options — documented in the
`pure-lsp-set-option` skill, which also gives the combination to set per intent and how to confirm
the backend is really wired in rather than silently falling back to interpreted.
