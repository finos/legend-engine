---
name: Extension registration topography (dual SPI)
description: Two parallel mechanisms — Pure Extension/<<db.ExtensionLoader>> and Java META-INF/services/ — both needed for an end-to-end dialect or feature
type: reference
---

A new relational dialect or store extension is incomplete unless BOTH SPIs are wired. A Pure-only registration never reaches planning; a Java-only registration never gets dispatched.

## Pure side

- **Dialect loader:** function with `<<db.ExtensionLoader>>` stereotype in `<dialect>Extension.pure` returning `DbExtensionLoader(dbType=DatabaseType.<X>, loader=createDbExtensionFor<X>__DbExtension_1_)`. Discovered via `getDbExtensionLoaders()` in `dbExtension.pure` using reflection on the stereotype.
- **Store contract:** `meta::relational::contract::relationalStoreContract()` returns `StoreContract` with `planExecution`, `supports`, `shouldStopRouting`, `shouldStopPreeval`, `routeFunctionExpressions`, `connectionEquality`, `localizeXStoreAssociation`, `planGraphFetchExecution`. Aggregated by `meta::pure::extension::Extension` via `availableStores`.
- **Extension aggregation:** `meta::relational::extension::relationalExtensions()` collects all relational sub-extensions; consumed by `meta::pure::extension::Extension`'s `availableStores`, `availableExternalFormats`, `availableFeatures`, `availablePlatformBindings`. Threaded through every planning/execution call as explicit parameter.

## Java side (META-INF/services files)

Each dialect typically has a sub-module per role, each shipping services files:

- `<dialect>-grammar`:
  - `org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension`
  - `org.finos.legend.engine.language.pure.grammar.from.IRelationalGrammarParserExtension`
  - `org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension`
- `<dialect>-protocol`:
  - `org.finos.legend.engine.protocol.pure.v1.extension.PureProtocolExtension`
- `<dialect>-execution`:
  - `org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionExtension`
  - `org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic.StrategicConnectionExtension`
  - `org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProvider` (usually)
- `<dialect>-connection` (newer modules):
  - `org.finos.legend.connection.RelationalDatabaseManager`
- `<dialect>-pure`:
  - `org.finos.legend.pure.m3.serialization.filesystem.repository.CodeRepositoryProvider` — tells legend-pure about the `core_relational_<dialect>` resource tree.
- `<dialect>-PCT`:
  - `org.finos.legend.pure.m3.pct.shared.provider.PCTReportProvider` — makes PCT outcome data discoverable.
  - `org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration` — wires a test connection (Testcontainers or H2 in-proc).
  - also `CodeRepositoryProvider`.
- `<dialect>-sqlDialectTranslation-pure` (newer sql-reversePCT story, e.g. DuckDB, H2, MemSQL):
  - `org.finos.legend.engine.pure.code.core.LegendPureCoreExtension`
  - `CodeRepositoryProvider`

## Concrete examples

- Relational store Pure registration: `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/main/resources/core_relational/relational/contract/storeContract.pure` (function `relationalStoreContract`).
- DuckDB Pure dialect loader: `core_relational_duckdb/relational/sqlQueryToString/duckDBExtension.pure` (`<<db.ExtensionLoader>>`-annotated).
- Postgres Java services: `legend-engine-xt-relationalStore-postgres-execution/src/main/resources/META-INF/services/org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionExtension`.
- PCT report provider wiring: `legend-engine-xt-relationalStore-h2-PCT/src/main/resources/META-INF/services/org.finos.legend.pure.m3.pct.shared.provider.PCTReportProvider`.

## When adding a new dialect

Checklist (see docs/engineering/architecture/router-and-pure-to-sql.md §7):

1. `<dialect>-pure` with `<<db.ExtensionLoader>>` loader + `DatabaseType.<X>` enum entry.
2. `DatabaseManager` Java class (JDBC URL, driver, commands) registered via `ConnectionExtension` / `StrategicConnectionExtension`.
3. `<dialect>-grammar` + `-protocol` extension SPIs if introducing new DSL constructs.
4. `<dialect>-PCT` module + entry in `.github/workflows/resources/modulesToTest.json`.
5. Decide default-profile vs `pct-cloud-test`: default = local-runnable (H2, DuckDB), cloud = needs secrets and Testcontainers/remote.
