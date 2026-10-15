# Legend Engine — SQL E2E Parity Tests

End-to-end tests that compare SQL execution between a real **PostgreSQL 16** database and the **Legend SQL wire-protocol server**. Every SQL query is run against both, and results are compared cell-by-cell to verify parity.

> 📌 **Before investigating a failure, read [Traps that have already cost real time](#traps-that-have-already-cost-real-time)
> and [Known unsupported areas](#known-unsupported-areas-baselined-error--expected-do-not-chase).**
> A large number of `ERROR` baselines are expected — the corpus deliberately enumerates the whole
> Postgres surface, most of which Legend does not implement — and three traps have each cost real
> debugging time.

## What this module does

1. Starts a Postgres 16 container (via Testcontainers)
2. Starts a Legend SQL server (wire-protocol, backed by the same Postgres via a Pure model)
3. Loads YAML test definitions from `src/main/resources/parity-tests/`
4. For each test, runs the SQL against both Postgres and Legend (via TDS **and** Relation paths)
5. Compares results and records status: `PASS`, `FAIL`, `ERROR`, `SKIP`, or `BUG`
6. Produces coverage reports in `target/` (`function-coverage.md`, `structural-parity.md`)

## Prerequisites

- **JDK 11** (`[11.0.10, 12)`)
- **Docker** (required by Testcontainers)
- Module must be built after its dependencies: `mvn clean install -DskipTests -pl legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests -am`

## Running the tests

```bash
# Run the full parity suite
mvn test -pl legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests

# Run and update YAML files with current statuses
mvn test -pl legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests -Dparity.updateStatus=true

# Run with fix-detection suppressed (useful during bulk fix work)
mvn test -pl legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests -Dparity.ignoreFixDetection=true

# One case or one family (substring match on the test id)
mvn test -pl legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests \
    -Dtest=TestPostgresParity -Dtest.filter=to_char_token__HH
```

If you edit corpus YAML or the test class and then invoke `surefire:test` directly (instead of
`mvn test`), refresh the copies under `target/` first — surefire skips both phases and will
silently use stale files:

```bash
mvn -o -pl legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests process-test-resources   # YAML changed
mvn -o -pl legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests test-compile             # test class changed
```

There is also an **interpreted dev loop** for iterating on the Pure SQL-translation sources in
seconds instead of a 15–25 min rebuild — see [The interpreted dev loop](#the-interpreted-dev-loop)
below. It is not a CI signal and not a correctness oracle; trap 1 explains why.

## Traps that have already cost real time

### 1. Interpreted results are not trustworthy — always confirm against compiled

The interpreted dev loop diverges from compiled on constructs that are perfectly fine in
production. Every divergence investigated in depth so far has been an artifact, not a product bug.
Confirm with `-Dtest.filter` before filing or fixing anything.

**A wrong-answer (`FAIL`) result is no more trustworthy than an `ERROR`.** The interpreter can
silently compute a different value — `NULL = NULL` evaluates to `true` interpreted and to `NULL`
compiled. `null_equals_null`, `null_not_equals`, `null_is_distinct_from` and
`case_simple_null_compare` all look like three-valued-logic bugs interpreted, and all four pass
both paths compiled.

Two confirmed Pure-interpreter defects are behind most of them:

**`pair` type-variable capture.** `pair<U,V>` over a polymorphic function reference that also names
a parameter `U` drops that function's own quantifier. `quantifiedComparisonFunction` /
`scalarComparisonFunction` (`fromPure.pure`) build exactly this shape via `pair(op, equalAny_U_...)`,
breaking every quantified/scalar subquery (`= ANY`, `> ALL`, `IN (subquery)`) with
`Type Error: 'ConcreteFunctionDefinition<<U,Z> {...}>' not a subtype of 'SqlTransformContext'` at
`pair.pure:41`. Compiled is immune — generated Java uses wildcards and does no runtime generic
resolution. Appending `->cast(@meta::pure::metamodel::function::Function<Any>)` pins the type
variable and fixes it (a no-op for compiled).

**`reactivate` does not constant-fold interpreted.** `LIMIT n OFFSET m` emits `slice(rel, m, m + n)`
and relies on pre-evaluation (`preeval.pure:859`) to fold it before `pureToSQLQuery.pure:3854`'s
`instanceOf(Literal)` assert. `reactivate` has two independent native implementations: compiled
folds `plus(2,1)` → `3`; interpreted passes the raw `InstanceValue` straight to `plus`, surfacing as
`"Not a number: ... instanceOf InstanceValue"` (TDS) or `"Invalid type for second parameter inside
the slice function"` (Relation). `ORDER BY ... NULLS FIRST/LAST` fails in the same subsystem
(`preeval.pure:238`). Trap: the reported location `compileUtils.pure:102-110` is not the failing
code — it is the `SourceInformation` stamped on the `plus` node when it was built.

Neither is a Relation-only bug, though it looks that way: `processQuantifiedComparisonExpression` /
`processScalarSubqueryComparison` (`fromPure.pure:3500`/`:3562`) both open with `assertRelation(...)`,
so the TDS path bails by design and is baselined `ERROR`. "TDS passes" there means "TDS errors as
intended".

### 2. A query with no table reference never reaches Legend

`StatementDispatcherVisitor.visitDefault` routes any statement whose `TableNameExtractor` finds no
qualified name to `ExecutionType.Metadata_Generic`, which `SQLManager` serves from a **real Postgres
container**, not Legend. So `SELECT to_char(TIMESTAMP '...', 'HH')` with no `FROM` is answered by
Postgres and compared against Postgres — it passes while testing nothing.

This is correct production behaviour (a BI client's `SELECT 1` should not invoke the planner), but
it means **a new test case must reference a real table or it is vacuous**. Append something like
`FROM dates WHERE id = 1`. A derived table does not count — only real table names and table
functions do.

6 cases remain deliberately table-free: `functions/sequence_functions` (3 — `NEXTVAL`/`SETVAL` are
side-effecting and both sides share one database, so they can never match; arguably should be
`skip:`), `smoke_tests` (2 — deliberate coverage of the constant-query dispatch path), and
`structural/values_clause` (1 — standalone `VALUES` has no `SELECT` to attach a `FROM` to).

### 3. Quote any `sql:` value containing `#`

YAML treats ` #` in an unquoted scalar as a comment. 18 cases silently executed as truncated
nonsense for months — `SELECT json_val #> '{a,b}' AS result FROM json_data WHERE id = 2` ran as
`SELECT json_val`, losing the `FROM` too. Always quote:

```yaml
sql: "SELECT id, int_val # small_val AS result FROM numbers WHERE id = 1"
```

## Known unsupported areas (baselined `ERROR` — expected, do not chase)

| area | detail |
|---|---|
| `to_char` tokens | Only `YYYY YYY YY Y MM MONTH/MON(+case) DD DDD D DAY/DY(+case) HH24 MI SS WW Q` are implemented (`toCharFormats()` in `fromPure.pure`). `HH`, `HH12`, `IYYY`, `IW`, `CC`, `TZ*`, `AM/PM`, `MS/US`, `FF1`–`FF6` etc. are explicitly `toCharUnsupported` and have been since 2023-11-13. |
| `EXTRACT` fields | Only `YEAR QUARTER MONTH WEEK DOW DOY DAY HOUR MINUTE SECOND EPOCH` (`extractFieldToValue()`). `CENTURY`, `DECADE`, `ISOYEAR`, `MILLENNIUM`, `TIMEZONE*`, `JULIAN`, `MICROSECONDS`, `MILLISECONDS` are not. |
| geometric operators | 157 cases, effectively none supported (`#`, `##`, `<->`, …). |
| range / network / bitstring / FTS / other-type operators | largely unsupported. |
| JSON | `json_data` is in the fixture and the compiled model, but JSON column types raise `"JSON not supported yet!"` in the Pure relational metamodel, so the interpreted model omits the table entirely. Some `#>`-family operators *do* work on the Relation path. |
| set-returning functions | `UNNEST`, `generate_series` etc. unsupported. |
| sequences | `NEXTVAL`/`SETVAL` unsupported (and untestable here, see trap 2). |
| standalone `VALUES` | TDS: "values only supported on relation inputs"; Relation: "only single value values currently supported". |

## Directory structure

The corpus and the harness classes it needs live in `src/main` rather than `src/test`, so the
interpreted dev-loop module (`legend-engine-xt-sql-e2e-pure`) can depend on them. Everything that
is only meaningful to the compiled suite stays in `src/test`.

```
src/main/
├── java/.../postgres/e2e/
│   ├── TestCaseLoader.java          # YAML → Java POJO loader
│   ├── AstFromRewriter.java         # Rewrites table refs to func() calls
│   ├── DirectPostgresRunner.java    # Runs SQL against real Postgres
│   ├── ResultMatrix.java            # Typed result grid
│   ├── ResultComparator.java        # Cell-by-cell comparison
│   ├── SchemaManager.java           # Creates tables from YAML schema
│   └── SqlE2ERunner.java            # Process-wide harness for the interpreted dev loop
└── resources/
    ├── e2e-model.pure               # Pure model (classes, mapping, store, functions)
    └── parity-tests/
        ├── schema.yaml              # Shared table definitions + seed data
        ├── smoke_tests.yaml         # basic smoke tests
        ├── functions/               # Per-category function tests (575 Postgres signatures)
        ├── operators/               # Per-operator tests
        ├── predicates/ format_tokens/
        ├── structural/              # SQL construct tests (JOINs, CTEs, subqueries, etc.)
        ├── window_frames/           # Window function frame tests
        └── compositions/            # Complex multi-feature queries

src/test/java/.../postgres/e2e/
├── TestPostgresParity.java          # Main test suite (JUnit 5 @TestFactory)
├── TestSqlE2ERunner.java            # Covers SqlE2ERunner (corpus load, rewrite, reference exec)
├── ParityReport.java                # Console + JSON report
├── YamlStatusUpdater.java           # Writes expected status back to YAML
├── E2eTestSourceProvider.java       # Wires Legend SQL to the Postgres container
├── E2eTestPostgresServer.java       # Minimal Legend wire-protocol server
├── E2eLegendTestClient.java         # HTTP client for Legend SQL API
└── coverage/                        # Report generators
    ├── FunctionCoverageReport.java
    ├── StructuralParityReport.java
    ├── FunctionCatalogExtractor.java
    ├── FunctionCoverageMapper.java
    └── ErrorCategorizer.java
```

## YAML test format

Each test case has the following fields:

```yaml
- id: abs__big__from_table          # Globally unique ID (validated at startup)
  sql: "SELECT ABS(int_val) FROM numbers ORDER BY 1"  # SQL to run against both Postgres and Legend
  function: abs                      # (optional) Postgres function name for coverage linking
  signature: "abs(bigint) → bigint"  # (optional) Exact Postgres catalog signature
  feature: "INNER JOIN"              # (optional) Structural feature name
  category: "joins"                  # (optional) Structural category grouping
  skip: "reason"                     # (optional) If set, test is skipped with this reason
  join_func: true                    # (optional) Use pre-built joined function instead of FROM rewriting
  expected_tds_status: PASS          # Expected status for TDS path
  expected_rel_status: PASS          # Expected status for Relation path
```

Valid statuses:

| status | meaning |
|---|---|
| `PASS` | Legend matched the reference Postgres |
| `FAIL` | both ran; results differ |
| `ERROR` | Legend could not plan/execute it (usually an unsupported construct) |
| `BUG` | Postgres itself rejected the reference SQL |
| `SKIP` | explicitly skipped via `skip:` |

A run fails only when a case *diverges from its baseline*, so a red run means "behaviour changed",
not "something is broken". Current corpus: **2,307 cases**. TDS baselines: 1005 PASS / 1128 ERROR /
42 FAIL / 70 BUG / 62 SKIP. Relation baselines: 1161 PASS / 968 ERROR / 46 FAIL / 70 BUG / 62 SKIP.

## When a fix causes a test to pass

When you implement new functionality (e.g., adding support for a SQL function or fixing a bug), previously-failing tests may start passing. The build will **fail** with a message like:

```
FIX DETECTED: my_test_id|TDS was expected ERROR, now PASS.
Update the test's expected_tds_status in the YAML file to PASS.
```

**What to do:**

1. **Run the tests with status update enabled:**
   ```bash
   mvn test -pl legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests -Dparity.updateStatus=true
   ```
   This automatically updates `expected_tds_status` and `expected_rel_status` in every YAML file to match the current actual results.

2. **Review the changes** — `git diff` will show which tests changed status. Verify these are expected:
   - `ERROR → PASS` or `FAIL → PASS` — your fix worked ✅
   - `PASS → ERROR` or `PASS → FAIL` — this is a **regression**, investigate before committing ❌

3. **Commit the updated YAML files** alongside your code change. The PR diff will clearly show which tests improved.

### Alternative: manual update

If you prefer to update only specific tests:

1. Open the relevant YAML file in `src/main/resources/parity-tests/`
2. Find the test by its `id`
3. Change `expected_tds_status: ERROR` to `expected_tds_status: PASS` (and/or `expected_rel_status`)
4. Re-run to confirm the build passes

## Adding new tests

1. **Choose the right file** — function tests go in `parity-tests/functions/<category>.yaml`, structural tests in `parity-tests/structural/<feature>.yaml`

2. **Add a test case** with a globally unique `id`:
   ```yaml
   - id: my_new_function__basic
     sql: "SELECT MY_FUNC(col) AS result FROM numbers ORDER BY 1"
     function: my_func
     signature: "my_func(integer) → integer"
   ```

3. **Run once** to establish the baseline status:
   ```bash
   mvn test -pl legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests -Dparity.updateStatus=true
   ```
   This will add `expected_tds_status` and `expected_rel_status` fields automatically.

4. **Commit** the YAML file with the status fields.

> **Important:** Test IDs must be globally unique across all YAML files. The test suite validates this at startup and fails immediately if duplicates are found.

## Adding new tables

1. Add the table definition to `parity-tests/schema.yaml` (columns + at least 10 rows of seed data)
2. Add the corresponding table definition to the Pure model in `e2e-model.pure` (Relational schema, Pure class, mapping)
3. Add TDS and Relation Pure functions for the new table in `e2e-model.pure`

## Regression enforcement

The `expected_tds_status` / `expected_rel_status` fields in each YAML test case act as the regression baseline:

| Scenario | Build result | Action |
|----------|-------------|--------|
| Expected `PASS`, actual `PASS` | ✅ Pass | None |
| Expected `ERROR`, actual `ERROR` | ✅ Pass | None |
| Expected `PASS`, actual `ERROR` | ❌ **Fail** (regression) | Fix the regression |
| Expected `ERROR`, actual `PASS` | ❌ **Fail** (fix detected) | Update YAML status to `PASS` |
| No expected status set | ✅ Pass | New test — run with `-Dparity.updateStatus=true` |

## System properties

| Property | Default | Description |
|----------|---------|-------------|
| `parity.updateStatus` | `false` | Write current statuses back to YAML files |
| `parity.ignoreFixDetection` | `false` | Suppress "FIX DETECTED" failures (regressions still fail) |
| `parity.failOnError` | `false` | Fail the build if any test has `FAIL` or `ERROR` status |
| `sql.e2e.corpus.dir` | *(unset — read from the classpath)* | Resource root (the directory containing `parity-tests/`) to read the corpus from instead, picking up edits live. See [Live corpus edits](#live-corpus-edits). |
| `sql.e2e.postgres.host` | *(unset — start a container)* | Use an already-running Postgres instead of Testcontainers. Setting `host` switches the mode; `.port`/`.database`/`.user`/`.password` default to `5432`/`postgres`/`postgres`/`postgres`. |
| `legend.engine.testcontainer.registry` | *(unset)* | Prefix for the Postgres image, for hosts that cannot pull from Docker Hub. |

## The interpreted dev loop

`legend-engine-xt-sql-e2e-pure` runs slices of this corpus through the **interpreted** Pure LSP so
`.pure` edits to the SQL→Pure translation are testable in seconds rather than a 15–25 min rebuild.
The `sql-e2e-devloop` agent skill is the runbook; this section is how it is built and why.

- **This module** (`-e2e-tests`) — `SqlE2ERunner`: lazily starts the Testcontainers Postgres, seeds
  the schema, loads the YAML corpus, and resolves each `(case id, path)` pair — FROM-rewrite plus
  cached reference result — into a typed `CaseRef`. No JSON anywhere in this path.
- **`legend-engine-xt-sql-e2e-pure`** — Pure repo `core_external_query_sql_e2e`: `model.pure` (test
  schema and mapping) and `framework.pure` (the driver), backed by three natives —
  `resolveCaseRefs`, `executeSQLE2ETest`, `executeAdhocSQL` — with compiled and interpreted
  implementations sharing one `SqlE2ENativeHelper`.
- Result comparison reuses this module's own `ResultComparator`/`ResultMatrix` (via
  `TdsJsonResultMatrix`), so the two suites cannot disagree about what a divergence is.

### Why it is shaped this way

**Three natives, not more.** Everything the Legend side does — SQL parse, `sqlToPure`, routing,
plan generation, dialect SQL emission — must stay in Pure, because that is the code under edit.
Everything else is plain data marshalling that Java can do directly once it is already the side
resolving and invoking the Pure work function. Earlier revisions exposed separate natives for
corpus fetch, connection and comparison; folding each into the caller that already had the data
removed them without moving any Pure-side logic.

**Java owns trapping and timing.** Pure has no `try`/`catch`, so a single failing case would abort
a whole category run. `executeSQLE2ETest` wraps the call, classifies the outcome, and times it.
Skip / corpus-bug / rewrite-error cases are classified from the resolved `CaseRef` *before* Pure is
entered at all, so they cost nothing.

**Owned result types, not `meta::pure::test::surveyor`'s.** A corpus case is not a Pure test
function, and `BUG` — the reference SQL itself was rejected by Postgres — has no surveyor
equivalent. Borrowing surveyor's vocabulary forced a wrapper class purely to bolt `BUG` on from
outside.

**One comparator.** The interpreted loop originally had its own string-canonicalising comparator.
Two comparators meant two definitions of "divergence", and the bespoke one was the source of most
false positives. It is gone.

### Live corpus edits

By default the corpus is read from the classpath — i.e. the built jar — so a long-running LSP
daemon never sees a YAML edit. Point `sql.e2e.corpus.dir` at the source tree and edits become live,
with no rebuild and no daemon restart (the `pure-lsp-launch-engine` launcher passes this for you):

```bash
pure-lsp-server ... \
  "--jvm-arg=-Dsql.e2e.corpus.dir=$LEGEND_ENGINE_ROOT/legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests/src/main/resources"
```

The runner re-reads when a corpus file's mtime changes (checked at most once a second; a full parse
of all 73 files is ~80 ms). Reference results are cached by SQL text, so an edited case misses
naturally and re-executes — nothing to invalidate by hand.

**Editing `schema.yaml` still needs a restart.** A reload refreshes the cases and the known-table
set only; re-running `SchemaManager` would drop and recreate tables in the live Postgres.

### Gaps specific to the interpreted dev loop

These affect `legend-engine-xt-sql-e2e-pure` only; the compiled suite is unaffected.

1. **Constant-only queries** (`SELECT 1`) fail in-process — TDS hits a plan-JSON serialisation bug,
   Relation hits `wrapPrimitiveInTDS is not supported yet`. They pass on the wire path.
2. **`json_data` is absent from the interpreted Pure model** (JSON column types unsupported), so
   every JSON-column case is unavailable there.
3. **Parameterised accessors** (`tds_persons_by_dept`) are skipped; no corpus case uses them.

## Generated reports

After each run, reports are generated in `target/`:

- **`function-coverage.md`** — Per-signature coverage of all 575 Postgres built-in function signatures (PASS/PARTIAL/FAIL/ERROR per TDS and Relation path)
- **`structural-parity.md`** — Per-feature coverage of SQL constructs (JOINs, CTEs, window frames, etc.)
- **`failure-details.md`** — Full result set comparisons for every FAIL and ERROR test. Shows the original SQL, rewritten SQL, cell-level diffs, and complete Expected (Postgres) vs Actual (Legend) result tables. Both `function-coverage.md` and `structural-parity.md` link directly to entries in this file — click any error category link (e.g. `FUNCTION_NOT_SUPPORTED`, `RESULT_MISMATCH`) to jump to the full details.
- **`parity-report.json`** — Machine-readable JSON with all test results

Each markdown report is also rendered to a standalone `.html` file beside it.

## Published reports

The four HTML reports are published to the Legend documentation site on every `master` build, at
<https://legend.finos.org/sql-parity/summary.html>, and are described for users at
<https://legend.finos.org/docs/reference/legend-sql>.

The `legend-docs` job in `.github/workflows/build.yml` does the publishing: the `sql` group of `test-modules`
uploads `target/*.html` as the `sql-parity-report` artifact, and `legend-docs` copies it into
`website/static/sql-parity/` of `finos/legend`, alongside the PCT reports it collects the same way.


