# Running a test scope through the LSP

Shared by `pure-lsp-execute` (serial) and `pure-lsp-execute-parallel` (concurrent). Both take the
same scope flags and both go to the server's `legend/executeTests`; which subcommand you invoke is
what decides concurrency, so there is no `--parallel` flag. Each skill states only what is specific
to it and links here for the rest.

## Naming a scope

Instead of a function path, name a package or a file and the daemon discovers the tests itself:

```bash
pure-lsp execute          --package meta::pure::functions::collection::tests
pure-lsp execute-parallel --source path/to/MyTests.pure
```

Discovery is a real stereotype walk server-side — the same `TestCollection` walk `PureTestBuilder`
and the Maven/JUnit side use, picking up both `<<test.Test>>` and `<<PCT.test>>`. Not a text scan.

| Flag | Effect |
|---|---|
| `--no-recursive` | with `--package`: that package's own tests only, not its subpackages |
| `--vanilla-only` | skip `<<PCT.test>>` functions |
| `--pct-only` | only `<<PCT.test>>` functions |
| `--pct-adapter <path>` | the ONE adapter applied to every PCT test in the run |
| `--json` | raw result payload instead of the rendered tree |

`--source` (whose tests to run) is distinct from `execute-parallel`'s `--file` (a file to compile
before running).

## Output

An indented tree grouped by package, plus an `N passed, N failed, N skipped` summary; exit 1 if
anything failed. `--json` gives `{success, total, passed, failed, skipped, durationMs, tests[]}`,
each entry carrying `status`, `durationMs`, `message`, captured `output`, and the function's own
**return value** as `returnKind` / `returnType` / `returnValue` / `returnSize` / `returnTruncated`
(see the `pure-lsp-execute` skill for the field semantics).

```
meta::pure::functions::collection::tests
  PASS setUpTests (before)  4ms
  meta::pure::functions::collection::tests::map
    PASS testMapString  2ms
```

The tree prints a `= <value>` line under an entry that returned something, so an explicit list of
ordinary value-returning functions reads as a result table. A plain `true` from a passing test is
suppressed — tests conventionally end in `true` and `status` already says so — but a `false` return
still shows, because `status` tracks whether the function threw, not what it returned.

`<<test.ToFix>>` tests are reported as `SKIP` rather than dropped, and excluded from the pass/fail
tallies so those stay comparable with a surefire run of the same package.

## Setup/teardown is nested, and that is the point

A wide package usually carries several `<<test.BeforePackage>>`/`<<test.AfterPackage>>` functions
scoped to different subpackages, plus inherited ones from ancestors. Each runs **once per package
node**, bracketing only its own subtree, in `PureTestBuilder` order (before → subpackages → the
node's own tests → after).

This is why a scoped run is not equivalent to looping over a hand-built list of test paths, or to
`pure-lsp execute --before/--after` per test: `--before` runs the hook around *every* call, and the
hook it names is only the nearest one. Hooks appear as their own entries marked `(before)`/`(after)`,
and a failing hook is reported without aborting the run — exactly as JUnit does.

## PCT tests

Pass `--pct-adapter`. Without one, PCT tests are reported as skipped; a scope containing *only* PCT
tests fails outright rather than reporting an all-skipped success.

The adapter argument accepts the mangled id, the bare Pure path, the simple function name, or the
display name (`'DuckDB'`) — all four are tried server-side. **Adapter paths are pre-dumped in
`pct-adapters.md`**; look there rather than grepping `.pure` sources for the `<<PCT.adapter>>`
definition.

For a whole package's PCT tests prefer `--package ... --pct-only --pct-adapter <path>` over
resolving one function's mangled id by hand — the daemon discovers the tests and needs no mangled
id from you at all.

## Know when to stop and run Maven instead

A scoped run is **interpreted** and holds the graph read lock for its whole duration, so every
compile — `check`, `go`, the auto-sync hook — queues behind it and the session serves stale code
until it finishes. That is the real ceiling, not CPU.

- Quick iteration: one file, or one leaf subpackage — roughly ≤50 tests, or any scope you have seen
  finish in under ~30s.
- Switch to `mvn surefire:test` once a scope exceeds ~200 tests or ~5 minutes.
- Never run a whole top-level package (`meta::pure::functions`, `meta::relational`) here.

Rule 3 in `lsp-devloop-usage-rules.md` has the measured numbers behind those thresholds — the same
51 tests take 83s here and 8.2s under surefire.

An explicit list of more than **30** functions is refused client-side, for the same reason. A
`--package`/`--source` scope is sized by the daemon, not the client, so the limit travels in the
payload as `maxTests` and the client warns after the fact if a scope exceeded it. Over the limit:
stagger (one `--source` at a time, or a narrower `--package --no-recursive`), or use the compiled
Maven suite — which is the right tool at that size anyway, and the only one that exercises the
generated-Java path. Interpreted green does not prove compiled green.

Ctrl-C cancels for real: the client sends a `runId` and cancels that run on the daemon rather than
abandoning it (`pure-lsp cancel-tests --all` clears anything abandoned). Cancellation is
cooperative, so a test sitting in a backend or JDBC call only stops once that call returns.

## Shared backend DB

The backend Server (9095) and H2 (9092) are shared across a daemon. Functions that **write** the DB
concurrently race — two `createTablesAndFillDb()` calls give
`Table PERSONTABLE already exists`. Hooks run serially, so a `<<test.BeforePackage>>` that sets up
tables is safe under either subcommand; but if the *tests themselves* write to the shared DB, run
the scope serially with `pure-lsp execute --package ...`. Parallel read-only execution is safe.
