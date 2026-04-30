---
name: SQL generator override points per dialect
description: For each dialect under legend-engine-xt-relationalStore-dbExtension, the extension .pure file that overrides DbExtension and registers the loader
type: reference
---

Each dialect lives under `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-<dialect>/` with sub-modules suffixed `-pure`, `-grammar`, `-protocol`, `-execution`, `-PCT`, `-connection`, and for some `-sqlDialectTranslation-pure`.

The dialect's Pure SQL override sits in `<dialect>-pure/src/main/resources/core_relational_<dialect>/relational/sqlQueryToString/<Dialect>Extension.pure`. Pattern: a `<<db.ExtensionLoader>>`-annotated `dbExtensionLoaderFor<Dialect>()` returns a `DbExtensionLoader` pointing at `createDbExtensionFor<Dialect>__DbExtension_1_`. The `createDbExtensionFor<Dialect>` function merges `getDefaultLiteralProcessors()` + dialect-specific overrides, and `getDynaFunctionToSqlDefault()` + `getDynaFunctionToSqlFor<Dialect>()`.

## Dialects and their extension files

- H2 (default-profile PCT target) — `core_relational/relational/sqlQueryToString/dbSpecific/h2/h2Extension.pure` plus version-specific `h2Extension1_4_200.pure` and `h2Extension2_1_214.pure` (lives under `legend-engine-xt-relationalStore-core-pure`, not the db-extension dir).
- DuckDB (default) — `core_relational_duckdb/.../sqlQueryToString/duckDBExtension.pure` (plus `typeConversion.pure`).
- Postgres — `core_relational_postgres/.../sqlQueryToString/postgresExtension.pure` + `typeConversion.pure`.
- Snowflake — `core_relational_snowflake/.../sqlQueryToString/snowflakeExtension.pure`. Has Semistructured sibling suite `Test_Relational_Snowflake_Semistructured.java`.
- BigQuery — `core_relational_bigquery/.../sqlQueryToString/bigQueryExtension.pure`.
- ClickHouse — `core_relational_clickhouse/.../sqlQueryToString/clickHouseExtension.pure` + `typeConversion.pure`. Uses double-quote identifiers; bool as `'t/f'::Bool`.
- Databricks — `core_relational_databricks/.../sqlQueryToString/databricksExtension.pure`.
- Presto — `core_relational_presto/.../sqlQueryToString/prestoExtension.pure`.
- Trino — `core_relational_trino/.../sqlQueryToString/trinoExtension.pure`.
- Redshift — `core_relational_redshift/.../sqlQueryToString/redshiftExtension.pure`.
- SQL Server — `core_relational_sqlserver/.../sqlQueryToString/sqlServerExtension.pure`.
- Sybase ASE — `core_relational_sybase/.../sqlQueryToString/sybaseASEExtension.pure`.
- Sybase IQ — `core_relational_sybaseiq/.../sqlQueryToString/sybaseIQExtension.pure`.
- Hive — `core_relational_hive/.../sqlQueryToString/hiveExtension.pure`.
- MemSQL — `core_relational_memsql/.../sqlQueryToString/memSQLExtension.pure`.
- Oracle — `core_relational_oracle/.../sqlQueryToString/oracleExtension.pure`.
- Spanner — `core_relational_spanner/.../sqlQueryToString/spannerExtension.pure`.
- SparkSQL — `core_relational_sparksql/.../sqlQueryToString/sparkSQLExtension.pure`.
- Athena, Aurora, Sybase — present under dbExtension/; Athena's Pure extension is bundled differently (no dedicated `<dialect>Extension.pure` visible at top-level).

## Core scaffolding (not dialect-specific)

- `core_relational/relational/sqlQueryToString/dbExtension.pure` — `DbConfig`, `DbExtension`, `DbExtensionLoader`, `DynaFunctionToSql`, `ToSql`.
- `core_relational/relational/sqlQueryToString/extensionDefaults.pure` — default `dynaFnToSql` bindings for ANSI-style functions (e.g. `cosh`, `today`, simple comparisons).
- `core_relational/relational/sqlQueryToString/dbSpecific/composite/compositeExtension.pure`, `db2/db2Extension.pure`, `debugPrint/debugPrintExtension.pure` — in-tree extensions not under the dbExtension/ folder.

## Override surface (DbExtension function-valued properties)

Each dialect overrides as many of these as it needs (defaults in `extensionDefaults.pure`): `selectSQLQueryProcessor`, `dynaFuncDispatch`, `literalProcessor`, `identifierProcessor`, `schemaNameToIdentifier`, `columnNameToIdentifier`, `tableNameToIdentifier`, `joinStringsProcessor`, `windowColumnProcessor`, `commonTableExpressionsProcessor`, `ddlCommandsTranslator`, `isBooleanLiteralSupported`, `isDbReservedIdentifier`, `buildDummyConnection`, `dataTypeToSqlText`, `pureTypeToDatabaseTypeConverter`.
