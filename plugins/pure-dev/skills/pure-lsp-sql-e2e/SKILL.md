---
name: pure-lsp-sql-e2e
description: "Runs cases from the legend-engine SQL e2e parity corpus (TestPostgresParity, ~2300 SQL cases compared against a real Postgres) through the warm interpreted Pure LSP with `pure-sql-e2e`, so edits to fromPure.pure and other SQL-translation Pure sources are testable in seconds instead of a 15-25 min Maven rebuild. Use when working on Legend SQL -> Pure translation, sqlToPure, the postgresSql parser/router, or when the user asks to run/iterate on SQL e2e or parity tests, investigate a parity divergence, or check whether a SQL construct or function is supported. CRITICAL: interpreted results are hypotheses, not findings - every finding must be confirmed with a compiled -Dtest.filter run before being treated as a real bug."
---

# SQL e2e parity corpus: interpreted dev loop

Iterate on Legend's SQL→Pure translation against the real parity corpus without rebuilding. The
corpus, the FROM-rewrite and the reference Postgres execution stay in Java, behind three natives —
`resolveCaseRefs`, `executeSQLE2ETest`, `executeAdhocSQL`. Everything the Legend side does — SQL
parse, `sqlToPure`, routing, plan-gen, dialect SQL emission — runs in Pure, so `.pure` edits are
picked up by a warm LSP session immediately.

`pure-sql-e2e` invokes those natives directly with a case id and a mode. **Nothing is generated into
`framework.pure` and no file is edited to run a case.**

Never `curl` the bridge — the CLIs are the only reliable client (see
`references/lsp-devloop-usage-rules.md`).

## ⚠️ The rule that matters most

**A result found here is a hypothesis, never a finding.** The Pure *interpreter* diverges from
compiled on constructs that are completely fine in production. In one full-corpus sweep, **every**
divergence investigated in depth turned out to be an artifact — of the corpus, the comparator, or
the interpreter. Zero product bugs.

Always confirm against the compiled suite before filing or fixing anything:

```bash
mvn test -pl legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests \
    -Dtest=TestPostgresParity -Dtest.filter=<substring-of-test-id>
```

Two known interpreter defects account for most false results — `pair` type-variable capture
(breaks `= ANY` / `> ALL` / `IN (subquery)`) and `reactivate` not constant-folding (breaks
`LIMIT`/`OFFSET` and `ORDER BY ... NULLS FIRST/LAST`). Mechanisms and fixes are in the suite README;
you rarely need them, you just need to not trust the result.

And do not trust a wrong-answer (`FAIL`) result either: the interpreter can silently compute a
different *value*, e.g. `NULL = NULL` → `true` interpreted vs `NULL` compiled.

## Commands

```bash
pure-sql-e2e ids   <filter>...            [--mode tds|relation|both]
pure-sql-e2e run   <case-id-or-filter>... [--mode ...] [--serial] [--json]
pure-sql-e2e case  <case-id>              [--mode ...]
pure-sql-e2e adhoc "<sql>"                [--mode both]
```

A filter is an exact case id, a category (`structural/joins`), a `prefix*`, or `''` for everything.
Several can be named at once and are unioned. `--mode` is a single input applied to every case in
the run and defaults to `both`.

```bash
pure-sql-e2e ids   structural/null_semantics
pure-sql-e2e run   null_equals_null null_not_equals --mode tds
pure-sql-e2e run   structural/null_semantics --mode tds
pure-sql-e2e case  null_equals_null --mode tds
pure-sql-e2e adhoc "SELECT (NULL = NULL) AS result FROM dates WHERE id = 1" --mode both
```

`run` reports each case as a test entry with its own status, duration and message, streamed as the
run proceeds, with a `runId` that Ctrl-C actually cancels. The daemon owns and bounds the fan-out at
`executionConcurrency` (derived `0.75 × cores`; check it with `pure-lsp status`) — **do not
hand-chunk to match a pool size.**

### Green means matched Postgres, nothing else

A case is green only when it produced the same rows as the reference Postgres. `ERROR`, `FAIL`,
`BUG` and `SKIP` all read red, and the corpus's recorded baseline is not consulted at all. So a
whole-category run is a status report, not a regression check: the ~1,100 legitimately-unsupported
`ERROR`-baselined cases and the 62 `skip:` cases are red by construction. Each entry's console
output carries a `STATUS|<id>|<mode>|<status>|<ms>` line distinguishing them, and the compiled
suite remains the authority on whether a status is a regression.

### 30-entry cap

`run` refuses more than 30 entries and prints the compiled command instead — the same ceiling, for
the same reason, as an explicit function list in `pure-lsp execute-parallel`: a run holds the graph
read lock for its whole duration, so every compile queues behind it and the session serves stale
code until it finishes. At tens of seconds of interpreted plan-gen per case, a whole category
outlives the edit-test loop this exists to serve.

Narrow with `--mode tds`, or use `ids` (uncapped) to pick a runnable subset. `--mode both` counts as
two entries per case.

## Setup

The normal discovery route works — `legend-engine-pure-ide-light-http-server` declares both
`legend-engine-xt-sql-e2e-pure` and `-tests`, so `pure-lsp-classpath` picks them up:

```bash
pure-lsp-launch-engine        # passes -Dsql.e2e.corpus.dir so corpus edits are live
```

If `pure-sql-e2e` fails with `"The function '<mangled_id>' is not supported by this execution
platform"`, the classpath predates those declarations — refresh it with `pure-lsp-classpath
legend-engine --force` (~17s) and relaunch. That error means a missing jar, not an unregistered
native.

**A running daemon has already loaded the natives/extensions into memory at JVM startup.** After
any Java-side change in `legend-engine-xt-sql-e2e-pure` (not just `.pure` edits), kill and relaunch
the daemon — a `check` alone will not pick up a rebuilt jar.

On first corpus access the harness starts a Testcontainers Postgres, seeds the schema, and caches
everything for the JVM's lifetime — so the container cost is paid once per daemon. Resolving a case
also warms its reference-Postgres result, which is why the first `ids`/`run` against a wide filter
is slower than later ones.

## Corpus traps

These are documented once, in the suite's README (below) — **read it before investigating any
failure.** In brief: a query with no table reference never reaches Legend (it is answered by
Postgres and passes vacuously); any `sql:` value containing `#` must be quoted or YAML truncates it;
and `surefire:test` skips `process-resources`/`test-compile`, so refresh `target/` after editing
corpus YAML or the test class.

**Corpus edits are live** when the daemon was launched with
`-Dsql.e2e.corpus.dir=<engine>/legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests/src/main/resources`
(the launcher passes this). No rebuild, no restart — the runner re-reads on mtime change. Editing
`schema.yaml` is the exception and still needs a restart.

## Further reading (in legend-engine)

- `legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests/README.md` — **the one SQL e2e doc**, and
  it sits with the suite: how to run compiled, baseline statuses, the traps, known-unsupported
  areas, harness configuration, and how the interpreted loop is built. **Read before investigating
  any failure.**
