---
name: pure-lsp-execute-parallel
description: "Runs 2 to 30 Pure functions or tests concurrently on the warm LSP daemon via `pure-lsp execute-parallel`, or every test in a package or .pure file with --package/--source. Use whenever more than one function or test is to be run, including several tests on one PCT adapter, and never a shell loop of `pure-lsp execute`. Returns per-entry status, duration and typed returnValue plus a cancellable runId. Also the way to collect the values of many value-returning functions without a runAll() aggregator. About 2x faster than serial for five Snowflake PCT tests (51 s against 116 s)."
---

# Run many functions concurrently on one daemon

Needs a running bridge; use only the `pure-lsp` CLI, never `curl` (`references/lsp-devloop-usage-rules.md`). For exactly one function use `pure-lsp-execute`.

```bash
pure-lsp execute-parallel 'my::pkg::t1():Boolean[1]' 'my::pkg::t2():Boolean[1]' [--json]
```

Each entry is a signature, mangled id or bare path. No `<<test.Test>>` annotation is needed, but there is no `<<test.BeforePackage>>` bracketing; name a scope instead if you need hooks. The daemon bounds the fan-out (`executionConcurrency`, about 0.75 x cores, in `pure-lsp status`). Past the core count extra threads only cost throughput, and an unbounded batch starves `status`/`check`/`cancel-tests`, so send the whole batch and do not chunk it. An unresolvable path fails the whole run up front, naming it. More than 30 entries is refused client-side (`references/test-scopes.md`).

## PCT tests

One call takes one adapter, applied to every entry. Ids are `<fqn>_Function_1__Boolean_1_`:

```bash
P=meta::pure::functions::relation::tests::composition
pure-lsp execute-parallel "$P::testA_Function_1__Boolean_1_" "$P::testB_Function_1__Boolean_1_" --pct-adapter DuckDB
```

For several adapters, make one call per adapter. Adapter paths: `references/pct-adapters.md`.

## Values, not just pass/fail

With `--json`, each entry in `tests[]` carries `functionPath`, `status`, `durationMs` and `returnKind`/`returnType`/`returnValue`/`returnSize`/`returnTruncated`, as in `pure-lsp-execute`. Without `--json` the tree shows a one-line `= <value>` under each entry. Use this instead of a `runAll()` that prints delimited output.

## One parameterised function over many argument sets

A list can't name one function 30 times. The `/execute-tests` endpoint takes `invocations`: `[{"path", "arguments": ["String[1]", ...], "label"}]`, reported by `label`. The CLI doesn't surface it; callers build the payload, as `pure-sql-e2e` does (`pure-lsp-sql-e2e` skill).

## Edits and scopes

- Compile edited files first with a repeatable flag: `--file Main.pure --file Helper.pure`.
- Whole package or file, discovered server-side: `--package <pkg>` or `--source <file.pure>`. Concurrency applies within a package node only; hooks still bracket each subtree in order.

## Caveats

- Container-backed adapters (Postgres) start a Testcontainers container on first use in a session. Run one test serially first; fanning out on a cold session starts one container per entry and most fail with "Container startup failed".
- Each execution has its own console and thread-local transaction; the backend DB (server 9095, H2 9092) is shared. Concurrent writers race (`Table ... already exists`). Do table setup once with `pure-lsp execute`, then fan out read-only tests; run tests that write serially with `pure-lsp execute --package`.
- Parallelism does not rescue plan-gen-heavy suites, and a run holds the graph read lock throughout.
- Verified safe for relational plan-gen, graphFetch and print; for any other workload sanity-check a parallel run against a serial one first.
