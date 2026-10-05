---
name: pure-lsp-execute-parallel
description: "Runs several Pure functions concurrently against one warm LSP daemon via `pure-lsp execute-parallel`, instead of N serial go() calls. Also runs EVERY test in a package or a .pure file concurrently with `--package`/`--source`, discovering them server-side (vanilla <<test.Test>> and <<PCT.test>> alike) instead of needing the list up front. Either form returns per-entry results (status, duration, message, and the function's typed return value in returnValue) and a cancellable runId, with the daemon owning and bounding the fan-out - so do not hand-chunk batches to match a pool size. An explicit function list needs NO <<test.Test>> annotation and gets no hook bracketing, which makes this the way to evaluate many ordinary value-returning functions at once and collect their results keyed by functionPath - reach for it instead of writing a runAll() aggregator that print()s each result with a delimiter you then have to parse back out. Use when the user wants to run many functions or tests at once against a running session, wants to run 'all the tests in' a package or file, wants the results/values of a batch of functions, asks about running things 'in parallel' or 'concurrently' on the LSP, or wants faster turnaround on a batch of independent executions. Not for isolated sessions - each daemon is a single shared session."
---

# In-process parallel execution: run many functions on ONE daemon

Run several arbitrary zero-arg functions by Pure path CONCURRENTLY against the same compiled graph
on a single warm daemon. For running just ONE such function, use `pure-lsp-execute` instead
(`pure-lsp execute 'my::pkg::testFoo():Boolean[1]'`) — this skill is specifically for firing a batch
of them at once.

Never `curl` the bridge — `pure-lsp` is the only reliable client (see `references/lsp-devloop-usage-rules.md`).

Run several CONCURRENTLY (compiles any `--file` once, then runs them on the daemon and collects
per-function results):

```bash
pure-lsp execute-parallel 'my::pkg::t1():Boolean[1]' 'my::pkg::t2():Boolean[1]' ...
```

Optionally compile edited files first — once, up front, before any function runs. They take a
repeatable `--file` flag rather than trailing positionals, because the positional arguments are
the function list:

```bash
pure-lsp execute-parallel 'my::pkg::t1()' 'my::pkg::t2()' --file Main.pure --file Helper.pure
```

Each function accepts a signature (`a::b::t():Boolean[1]`), a mangled id (`a::b::t__Boolean_1_`),
or a bare path (`a::b::t`, common return shapes tried).

**Both forms go to `legend/executeTests`** — an explicit list and a `--package`/`--source` scope
alike. The daemon owns the fan-out, bounded at `executionConcurrency` (derived `0.75 x cores`,
reported by `pure-lsp status` alongside `requestPoolSize`). That bound is the point: Pure execution
is CPU-bound, so past roughly the core count extra threads cost throughput rather than adding it,
and an unbounded batch used to occupy every *request* thread and starve `status`/`check`/
`cancel-tests` — a daemon that looks wedged. Don't hand-chunk to match a pool size; just send the
batch.

An explicit list is run as one flat group: no discovery, so the functions need not carry
`<<test.Test>>` nor share a package — and correspondingly no `<<test.BeforePackage>>` bracketing
(name a scope instead if you need that). An unresolvable function path fails the whole run up front
naming the bad path, rather than being reported as a failed test.

More than **30** functions is refused client-side. See the cap and the thresholds behind it in
`references/test-scopes.md`.

## Batch-running value-returning functions (not just tests)

Each entry carries its own `returnValue`, so an explicit list is the way to evaluate many
functions and collect their **results** — not only their pass/fail. No `print()` wrappers, no
aggregator function that concatenates output with a delimiter you then have to parse back out.

```bash
pure-lsp execute-parallel \
  'probe::a1():String[1]' 'probe::a2():String[1]' 'probe::a3():String[1]' --json
```

Every entry in `tests[]` gains `returnKind` / `returnType` / `returnValue` / `returnSize` /
`returnTruncated` (same meanings as in the `pure-lsp-execute` skill), keyed by `functionPath`:

```json
{"tests": [
  {"functionPath": "probe::a1", "status": "passed", "durationMs": 412,
   "returnKind": "primitive", "returnType": "String", "returnValue": "select ..."},
  {"functionPath": "probe::a2", "status": "passed", "durationMs": 398,
   "returnKind": "primitive", "returnType": "String", "returnValue": "select ..."}
]}
```

Without `--json` the tree view shows a one-line `= <value>` preview under each entry.

Reach for this whenever you would otherwise write a `runAll()` that calls N functions and prints
each with a label — this does the same job, concurrently, with the labels and values already
separated. A `complex` return still shows type and size only; print what you need from those.

## One parameterised function over many argument sets

A function list cannot express "run this one function 30 times with different arguments" — every
entry would name the same function, and the results would be indistinguishable. For that, the
`/execute-tests` endpoint takes `invocations` instead: a list of
`{"path", "arguments": ["String[1]", ...], "label"}`, where `label` is what the entry is reported
under. The CLI does not surface this (there is no sane flag shape for per-entry arguments); callers
build the payload directly, as `pure-sql-e2e` does for the SQL e2e corpus — see the
`pure-lsp-sql-e2e` skill.

## Running a whole package or file, without listing the tests

When you don't have (or don't want to assemble) the list of test paths, name a scope instead and
the daemon discovers them itself:

```bash
pure-lsp execute-parallel --package meta::pure::functions::collection::tests
pure-lsp execute-parallel --source path/to/MyTests.pure
```

**`references/test-scopes.md` is the reference for the scope form** — the flags, nested
`<<test.BeforePackage>>` bracketing, PCT adapters, output shape, the 30-test cap and when to switch
to Maven. It is shared with `pure-lsp-execute`, which takes the identical flags and runs the scope
serially.

What is specific to running a scope *here*:

**Concurrency stays inside a package node.** The tree walk itself is serial on the server: a node's
hooks bracket its whole subtree and sibling subtrees routinely share state. Only the tests *within*
one node are fanned out — they already share that node's completed setup. So `--package` is
meaningfully faster than serial, without reordering setup.

**Concurrency does not raise the ceiling on how much you should run.** Parallelism only fans out
within a package node, so it does not rescue a suite that is slow because its tests drive plan-gen,
and the run still holds the graph read lock throughout. Same thresholds as the serial path.

## Isolation and the shared backend

Each execution is isolated — own console, own thread-local transaction — so results and printed
output don't cross-contaminate.

**The backend DB is not.** Server (9095) and H2 (9092) are shared, so functions that WRITE
concurrently race (two `createTablesAndFillDb()` → `Table PERSONTABLE already exists`). Do table
setup once serially via `pure-lsp execute`, then fan out the read-only query/test functions here.
For a scope whose *tests* write to the shared DB, run it serially with `pure-lsp execute --package`.

**Native safety:** verified for relational plan-gen, graphFetch and print. There is no broad audit
that no interpreted native writes shared non-transactional state mid-execution, so for novel
workloads sanity-check a parallel run against a serial one before trusting it.
