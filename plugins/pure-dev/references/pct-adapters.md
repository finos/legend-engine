# PCT adapter Pure paths

`--pct-adapter` (on `pure-lsp-execute`/`pure-lsp-execute-parallel`) accepts the adapter's bare Pure
path (below), its simple name, or its display name (e.g. `'DuckDB'`) — resolution tries all of
those server-side (`LegendPureSession#resolvePctAdapter` in your legend-pure checkout). This requires a
`legend-pure-lsp-server` built from a commit including the fix that made the single-function
`execute` path share that resolver with the `--package`/`--source` path (2026-09-22) — on an older
build, single-function `execute --pct-adapter` only accepts the exact mangled id
(`..._Function_1__X_o_`); `--package`/`--source` scoped runs were unaffected either way. These
paths change infrequently — this is a point-in-time dump, refresh it (see below) if a target here
fails to resolve after a rebase.

| Adapter name | Pure path |
|---|---|
| In-Memory | `meta::pure::test::pct::testAdapterForInMemoryExecution` |
| Python - LegendQL | `meta::external::python::reversePCT::legendQL::pythonLegendQLReversePCTAdapter` |
| Python - PandasAPI | `meta::external::python::reversePCT::pandasAPI::pythonPandasAPIReversePCTAdapter` |
| Databricks | `meta::relational::tests::pct::testAdapterForRelationalWithDatabricksExecution` |
| Spanner | `meta::relational::tests::pct::testAdapterForRelationalWithSpannerExecution` |
| MemSQL | `meta::relational::tests::pct::testAdapterForRelationalWithMemSQLExecution` |
| SqlServer | `meta::relational::tests::pct::testAdapterForRelationalWithSqlServerExecution` |
| Java Platform Binding | `meta::pure::executionPlan::platformBinding::legendJava::pct::testAdapterForJavaBindingExecution` |
| SQL | `meta::external::query::sql::reversePCT::framework::sql` |
| ClickHouse | `meta::relational::tests::pct::clickhouse::testAdapterForRelationalWithClickHouseExecution` |
| DuckDB | `meta::relational::tests::pct::duckDB::testAdapterForRelationalWithDuckDBExecution` |
| H2 | `meta::relational::tests::pct::h2::testAdapterForRelationalWithH2Execution` |
| Oracle | `meta::relational::tests::pct::oracle::testAdapterForRelationalWithOracleExecution` |
| Postgres | `meta::relational::tests::pct::postgres::testAdapterForRelationalWithPostgresExecution` |
| Snowflake | `meta::relational::tests::pct::snowflake::testAdapterForRelationalWithSnowflakeExecution` |
| Trino | `meta::relational::tests::pct::trino::testAdapterForRelationalWithTrinoExecution` |
| Deephaven | `meta::external::store::deephaven::tests::pct::testAdapterForDeephavenExecution` |

Dumped from an engine-scope LSP session (`legend-pure` + `legend-engine` repo-roots) via
`meta::pure::ide::testing::getPCTAdapters():Pair<String,String>[*]`.

## Refreshing this list

`execute`/`go` only surface `print()` output, never a function's return value — so you can't call
`getPCTAdapters()` directly and read its result. Print it explicitly instead, e.g. via `go()`:

```pure
function go():Any[*]
{
  meta::pure::ide::testing::getPCTAdapters()->map(p | print($p.first + ' => ' + $p.second + '\n', 1));
}
```

then `pure-lsp go $LEGEND_ENGINE_ROOT/welcome.pure`. `getPCTAdapters()`'s `second` field is the
adapter function's **mangled id** (e.g. `...testAdapterForRelationalWithDuckDBExecution_Function_1__X_o_`),
not the bare path `--pct-adapter` expects — strip the trailing `_Function_1__X_o_` mangling suffix
(the generic `<<PCT.adapter>> <X|o>(f:Function<{->X[o]}>[1]):X[o]` signature always mangles to that
same suffix) to get the bare path used in the table above.

Remember to restore `welcome.pure`'s prior content afterward — it's a shared, gitignored REPL
scratch file, not a place to leave one-off debug snippets.
