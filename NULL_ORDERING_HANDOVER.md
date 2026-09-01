# NULLS FIRST / NULLS LAST — Handover

Repos: `legend-pure` (`~/legend-pure`, branch `nulls-first-last`) and `legend-engine`
(this repo).

## Branch and policy

Branch: `nulls-high-db-extensions`, forked from `338364667f` (one commit before the opt-in-only
follow-up on `nulls-first-last`).

The platform contract on this branch is nulls-high even when a query does not specify null
ordering:

- ascending defaults to nulls last;
- descending defaults to nulls first;
- explicit `NULLS FIRST` / `NULLS LAST` is always honored;
- database-specific native behavior and rendering live in each `DbExtension`;
- shared null-ordering generation is database agnostic.

## Architecture

`DbExtension` now requires a `NullOrderingSupport` capability with:

- `nativeNullOrdering`: reports the dialect's native position for a direction;
- `sortItemProcessor`: renders a requested position using either a native clause or dialect
  emulation.

`DbConfig.processSortItem` delegates to that capability. Root `ORDER BY`, window `ORDER BY`, and
`WITHIN GROUP` all use this entry point. The shared code computes only the platform's nulls-high
target; it contains no `DatabaseType` checks or dialect matrix.

For root ORDER BY aliases, the displayed sort key and the null-check expression are passed
separately. CASE-based dialects rank nulls using the underlying relational expression and retain
the alias for the actual sort key. This avoids invalid forms such as
`CASE WHEN "projectedAlias" IS NULL ...` on SQL Server.

## Dialect declarations

| Extension behavior | Databases |
|---|---|
| Native nulls-high; omit unspecified clauses | H2, DB2, Oracle, Postgres/Aurora, Redshift, Snowflake, Composite, DebugPrint |
| Native nulls-low; add a clause for unspecified ordering | BigQuery, Databricks, Hive, SparkSQL, Spanner |
| Native NULLS LAST for both directions; add `NULLS FIRST` for unspecified DESC | ClickHouse, DuckDB, Presto, Trino/Athena |
| Native nulls-low without clause syntax; use CASE rank key | SQL Server, MemSQL/SingleStore, Sybase ASE, Sybase IQ |

The CASE form is:

`CASE WHEN <underlying expression> IS NULL THEN 0 ELSE 1 END [DESC], <sort key> [DESC]`

The rank key is ascending for nulls first and descending for nulls last.

## H2

H2 2.x is configured as nulls-high at its two shared default-property sites:

- `H2Manager` for execution-plan connections, including embedded file URLs;
- `H2Defaults` for the H2 2.1.214 extension runtime.

Both append `DEFAULT_NULL_ORDERING=HIGH` alongside `MODE=LEGACY`. The H2 dialect-translation path
no longer injects canonical null clauses when ordering is unspecified; absent order maps to
`SortItemNullOrdering.UNDEFINED`, while explicit FIRST/LAST is preserved.

## Existing Relation API work

- `NullOrder` and optional `nullOrder` fields exist in the legend-pure and relational metamodels
  (legend-pure commit `81fb9df6`).
- Relation supports `nullsFirst` / `nullsLast` and the two-argument
  `ascending(column, NullOrder)` / `descending(column, NullOrder)` forms.
- Compiled and interpreted in-memory execution carries explicit null ordering.
- TDS and relational bridges carry null order into root, window, and ordered-aggregate models.
- Relational routing of chained `->nullsFirst()` / `->nullsLast()` no longer NPEs.

## Validation

- Scoped `legend-engine-xt-relationalStore-core-pure` clean install: passed after the final
  alias-safe contract and focused tests were added.
- Scoped clean install of 19 directly touched dialect/H2 modules: passed. No `-am` or dependent
  rebuild was used.
- Focused `TestNullOrderingSupport`: 5 tests, 0 failures, 0 errors. Covers native nulls-high,
  native nulls-low, uniform NULLS LAST, explicit clause rendering, and CASE rendering with a
  separate underlying null-check expression.

Earlier RelationFunctions PCT history on the parent work:

- H2: 459 tests, with three unspecified-order expectation failures and one stale compiled-reference
  error before H2 was switched to nulls-high.
- DuckDB: 459 tests, with two unspecified DESC expectation failures and the same stale-reference
  error before DuckDB reconciliation was finalized here.

H2 and DuckDB RelationFunctions PCT have not yet been rerun on this branch after the final changes.

## Follow-ups

- Rerun H2 and DuckDB RelationFunctions PCT using freshly rebuilt direct PCT modules when desired.
- Add end-to-end SQL assertions for explicit root, window, and ordered-aggregate null ordering in
  representative clause and CASE dialects.
- Verify cloud dialect native defaults against live instances before treating the declarations as
  empirically confirmed.
- Refresh Legend SQL parity expectations and documentation as needed.

## Build constraints

- Follow repository `AGENTS.md`: no full builds, Maven `-am`, or dependent rebuilds.
- Rebuild only directly touched modules from the repository root with
  `mvn clean install -DskipTests -pl <package-path>`.
- The engine is pinned to legend-pure `5.96.0`; the legend-pure feature branch POM may use a newer
  snapshot version.

## Relevant commits

- `e6d27f29a1` — WIP end-to-end Relation API null-ordering support.
- `338364667f` — relational chained-modifier NPE fix and first shared reconciliation attempt.
- `644f8c94a8` — opt-in-only alternative on the sibling `nulls-first-last` branch; intentionally
  not included here.
