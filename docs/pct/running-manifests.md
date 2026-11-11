# Running PCT manifests in Pure IDE Light and the LSP

PCT manifests are JSON resources inside their owning Pure repository. The same path works in
Pure IDE Light, the Pure LSP, and the compiled Maven suites:

```text
/<repository>/<Category>Functions_manifest.json
```

Use this Pure repository path as the manifest argument, including the leading `/`. The Maven
module prefix and `src/main/resources` belong to the filesystem location, not the Pure path.
Repositories that host several adapters keep one `<adapter>` subdirectory per adapter, so their
path is `/<repository>/<adapter>/<Category>Functions_manifest.json`.

## Find a manifest

In Pure IDE Light's file tree, expand the repository (and the adapter directory for repositories
that host several adapters).
`PureIDELight` loads Engine manifest repositories from the checkout, so saving a manifest updates
the source file. `PureIDELight_NoExtension` discovers manifest repositories on its classpath and
includes their dependencies; those resources are read-only. Rebuild the owning module to update
a packaged resource.

The LSP scans repository definitions under its configured `repoRoots`. With the Engine checkout
as a source root, it reads these JSON files directly from disk when `loadPCTManifest` is called.
Save JSON edits normally; only `.pure` sources should be sent to the Pure compiler.

From the checkout root, list the available files with:

```bash
rg --files -g '*_manifest.json'
```

These are the Engine manifest directories. Each directory contains the categories supported by
that adapter's Maven PCT suite.

| Adapter | Pure manifest directory |
| --- | --- |
| BigQuery | `/core_relational_bigquery_pct/` |
| ClickHouse | `/core_relational_clickhouse_pct/` |
| Databricks | `/core_relational_databricks_pct/` |
| DB2 | `/core_relational_db2_pct/` |
| DuckDB | `/core_relational_duckdb_pct/` |
| H2 | `/core_relational_h2_pct/` |
| Oracle | `/core_relational_oracle_pct/` |
| Postgres | `/core_relational_postgres_pct/` |
| Snowflake | `/core_relational_snowflake_pct/` |
| Trino | `/core_relational_trino_pct/` |
| MemSQL | `/core_external_test_connection/relational-memsql/` |
| Spanner | `/core_external_test_connection/relational-spanner/` |
| SQL Server | `/core_external_test_connection/relational-sqlserver/` |
| Deephaven | `/core_deephaven_pct/` |
| Java platform binding | `/core_java_platform_binding/` |
| Core Relation | `/core_functions_relation/core-interpreted/` and `core-compiled/` |
| Core Standard | `/core_functions_standard/core-interpreted/` and `core-compiled/` |
| Core Unclassified | `/core_functions_unclassified/core-interpreted/` and `core-compiled/` |
| Core Variant | `/core_functions_variant/core-interpreted/` and `core-compiled/` |
| Core Scenario Quant | `/core_scenario_quant/core-interpreted/` and `core-compiled/` |


## Run with expected failures

`meta::pure::test::surveyor::runPCTTestsFromPath` takes three strings: the test package or function,
a source path prefix, and the manifest path. It loads the adapter and exclusions from the manifest
and returns a `TestReport`, also printing a summary. Narrow the package or name a single test while
iterating.

For example, in Pure IDE Light's `welcome.pure`, run this through **Go**:

```pure
function go():Any[*]
{
    meta::pure::test::surveyor::runPCTTestsFromPath(
        'meta::pure::functions::collection::tests::at',
        '/platform/pure/essential/',
        '/core_relational_h2_pct/EssentialFunctions_manifest.json'
    );
}
```

The same call can run through the LSP's `legend/executeFunction` with the signature
`meta::pure::test::surveyor::runPCTTestsFromPath(String[1],String[1],String[1]):meta::pure::test::surveyor::TestReport[1]`
and the three strings in `arguments`. With the development CLI connected to a ready LSP:

```bash
pure-lsp execute \
  'meta::pure::test::surveyor::runPCTTestsFromPath_String_1__String_1__String_1__TestReport_1_' \
  --arg 'meta::pure::functions::collection::tests::at' \
  --arg '/platform/pure/essential/' \
  --arg '/core_relational_h2_pct/EssentialFunctions_manifest.json'
```

A single-function `pure-lsp execute <test> --pct-adapter ...` run does not load a manifest's
exclusions; scoped runs do (see below). Inspect the report's `failCount` and `errorCount` (or its printed
summary): returning a report successfully does not mean all contained tests passed.

## Run with the adapter

Spelling out the manifest path is avoidable. Each adapter names its manifest directory with a
`PCT.manifestDirectory` tag, and `meta::pure::test::pct::utils::discoverPCTManifestPath(filter, adapter)`
(`core_pct_utils/manifests.pure`) joins it with the manifest of the category the
filter names or lies in (table below). `meta::pure::test::pct::utils::runPCTTestsFor` takes the adapter
instead of a manifest path:

```pure
function go():Any[*]
{
    meta::pure::test::pct::utils::runPCTTestsFor(
        'meta::pure::functions::relation::tests::extend',
        'relation',
        meta::relational::tests::pct::h2::testAdapterForRelationalWithH2Execution_Function_1__X_o_
    );
}
```

This resolves to `/core_relational_h2_pct/RelationFunctions_manifest.json`; an adapter sharing a
repository resolves under its subdirectory, e.g. `/core_external_test_connection/relational-memsql/`.
The In-Memory adapter carries no tag: its manifests live in each category's own repository, and
discovery picks the `core-interpreted` one because the Pure IDE and the LSP both execute
interpreted.

`discoverPCTManifestPath` carries the `<<PCT.manifestResolver>>` stereotype, which is how the LSP's
`legend/executeTests` (`pure-lsp execute --package/--source ... --pct-adapter`) finds it to apply
the same manifests to every PCT test it runs, per test source: a test failing with its
`expectedError` passes, and a test that passes despite an exclusion fails so the stale entry gets
removed, as in the Maven suites.

Discovery fails with `Cannot automatically identify manifest file to use ...` when the filter names
no category (`''`, `/platform/`), or the adapter has no `PCT.manifestDirectory` tag; pass the manifest path instead. The LSP runs such tests without expected failures and reports why under
`warnings`.

The manifest contains the adapter and exclusions; it does not select the test category or change
the execution engine. Choose the filter that matches the category, as the Maven suites do. Either
form of `runPCTTestsFor` (adapter or manifest path) accepts a filter that contains the category's
name, in any case (`essential`, `EssentialFunctions`), or a source path under the category's source
prefix (`/platform/pure/essential/collection/`). A path is matched by its prefix only, so a
directory that merely contains another category's name stays in its own category. The Surveyor's `runPCTTestsFromPath` takes the source prefix only:

| Category | Test package | Name | Source prefix |
| --- | --- | --- | --- |
| Essential | `meta::pure::functions` | `essential` | `/platform/pure/essential/` |
| Grammar | `meta::pure::functions` | `grammar` | `/platform/pure/grammar/functions/` |
| Relation | `meta::pure::functions::relation` | `relation` | `/core_functions_relation/` |
| Standard | `meta::pure::functions` | `standard` | `/core_functions_standard/` |
| Unclassified | `meta::pure::functions` | `unclassified` | `/core_functions_unclassified/` |
| Variant | `meta::pure::functions::variant` | `variant` | `/core_functions_variant/functions/` |
| Scenario Quant | `meta::external::scenario::quant` | `scenario_quant` | `/core_scenario_quant/` |

Use a descendant package or single function for a small run. Whole categories are better run with
their compiled Maven suites. For core tests in an interpreted IDE/LSP session, choose the
`core-interpreted` manifest; loading `core-compiled` does not switch the session to compiled mode.

Adapters retain their usual execution requirements: provision their database, credentials, or
Engine backend as appropriate. Loading and browsing the manifest itself does not start these
services. All manifest exclusions must resolve to functions in the loaded runtime, so load the
category's test repository as well as the adapter repository when using a restricted repository set.

## Validate changes

After changing a module, clean-install it from the repository root:

```bash
mvn clean install -DskipTests -pl <module-path>
```

Once its outputs are current, rerun its PCT suite through Surefire directly:

```bash
mvn -pl <module-path> org.apache.maven.plugins:maven-surefire-plugin:2.22.2:test -Dtest=<PCT-test-class>
```

`TestPCTManifestRepositories` in `legend-engine-pure-ide-light-http-server` checks every Engine
manifest against the Pure IDE Light filesystem repositories and LSP repository scanner.
`TestPCTManifestRepositorySelection` checks classpath discovery with and without optional adapters.
`TestPCTManifestExecution` exercises Surveyor through an embedded LSP session and verifies that
saved manifest exclusions take effect on the next invocation.
