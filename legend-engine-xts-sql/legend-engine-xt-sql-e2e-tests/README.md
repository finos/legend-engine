# Legend Engine — SQL E2E Parity Tests

End-to-end tests that compare SQL execution between a real **PostgreSQL 16** database and the **Legend SQL wire-protocol server**. Every SQL query is run against both, and results are compared cell-by-cell to verify parity.

## What this module does

1. Starts a Postgres 16 container (via Testcontainers)
2. Starts a Legend SQL server (wire-protocol, backed by the same Postgres via a Pure model)
3. Loads YAML test definitions from `src/test/resources/parity-tests/`
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
```

## Directory structure

```
src/test/
├── java/.../postgres/e2e/
│   ├── TestPostgresParity.java      # Main test suite (JUnit 5 @TestFactory)
│   ├── TestCaseLoader.java          # YAML → Java POJO loader
│   ├── AstFromRewriter.java         # Rewrites table refs to func() calls
│   ├── DirectPostgresRunner.java    # Runs SQL against real Postgres
│   ├── ResultMatrix.java            # Typed result grid
│   ├── ResultComparator.java        # Cell-by-cell comparison
│   ├── ParityReport.java            # Console + JSON report
│   ├── SchemaManager.java           # Creates tables from YAML schema
│   ├── YamlStatusUpdater.java       # Writes expected status back to YAML
│   ├── E2eTestSourceProvider.java   # Wires Legend SQL to the Postgres container
│   ├── E2eTestPostgresServer.java   # Minimal Legend wire-protocol server
│   ├── E2eLegendTestClient.java     # HTTP client for Legend SQL API
│   └── coverage/                    # Report generators
│       ├── FunctionCoverageReport.java
│       ├── StructuralParityReport.java
│       ├── FunctionCatalogExtractor.java
│       ├── FunctionCoverageMapper.java
│       └── ErrorCategorizer.java
└── resources/
    ├── e2e-model.pure               # Pure model (classes, mapping, store, functions)
    └── parity-tests/
        ├── schema.yaml              # Shared table definitions + seed data
        ├── smoke_tests.yaml         # 10 basic smoke tests
        ├── functions/               # Per-category function tests (575 Postgres signatures)
        ├── structural/              # SQL construct tests (JOINs, CTEs, subqueries, etc.)
        ├── window_frames/           # Window function frame tests
        └── compositions/            # Complex multi-feature queries
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

Valid statuses: `PASS`, `FAIL` (result mismatch), `ERROR` (Legend exception), `SKIP`, `BUG` (Postgres exception).

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

1. Open the relevant YAML file in `src/test/resources/parity-tests/`
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

## Generated reports

After each run, reports are generated in `target/`:

- **`function-coverage.md`** — Per-signature coverage of all 575 Postgres built-in function signatures (PASS/PARTIAL/FAIL/ERROR per TDS and Relation path)
- **`structural-parity.md`** — Per-feature coverage of SQL constructs (JOINs, CTEs, window frames, etc.)
- **`failure-details.md`** — Full result set comparisons for every FAIL and ERROR test. Shows the original SQL, rewritten SQL, cell-level diffs, and complete Expected (Postgres) vs Actual (Legend) result tables. Both `function-coverage.md` and `structural-parity.md` link directly to entries in this file — click any error category link (e.g. `FUNCTION_NOT_SUPPORTED`, `RESULT_MISMATCH`) to jump to the full details.
- **`parity-report.json`** — Machine-readable JSON with all test results


