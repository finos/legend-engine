# Legend Engine — Module Guide

> This guide lists every top-level module group and its significant sub-modules.  
> For the overall structure, see [Architecture Overview](../architecture/overview.md).

---

## How Modules Are Organised

The root `pom.xml` groups modules into logical categories. The naming conventions are:

| Prefix | Meaning |
|--------|---------|
| `legend-engine-core` | Foundational subsystems (grammar, compiler, plan gen/exec, Pure runtime) |
| `legend-engine-xts-*` | **Extension** modules — stores, formats, DSLs, function activators |
| `legend-engine-config` | Server assembly, REPL, configuration |
| `legend-engine-application-query` | Standalone query application back-end |

Sub-module suffixes follow a consistent convention:

| Suffix | Meaning |
|--------|---------|
| `-grammar` | ANTLR4 parsers / composers for that DSL section |
| `-protocol` | Java POJOs used for JSON serialisation (the "wire format") |
| `-compiler` | Topo/graph compilation: protocol → Pure metamodel |
| `-pure` | Pure source code resources (`.pure` files bundled as classpath resources) |
| `-generation` | Execution plan generation logic |
| `-execution` / `-executionPlan` | Runtime execution logic |
| `-http-api` | JAX-RS resource classes (REST endpoints) |
| `-analytics` | Read-only analytical queries over the metamodel |
| `-test-runner` | Shared test infrastructure for that feature area |
| `-PCT` | **Pure Compatibility Tests** — cross-store function-correctness tests |
| `-FCT` | **Function Compatibility Tests** |
| `-MFT` | **Mapping Functionality Tests** |

---

## legend-engine-core

The heart of the engine. All other modules depend on subsets of this.

### legend-engine-core-base

#### legend-engine-core-language-pure

| Sub-module | Purpose |
|------------|---------|
| `legend-engine-language-pure-grammar` | Converts Legend grammar text ↔ `PureModelContextData` JSON. Uses ANTLR4 grammars. Entry point: `PureGrammarParser` / `PureGrammarComposer`. |
| `legend-engine-language-pure-grammar-http-api` | REST endpoints wrapping the grammar module (`/api/pure/v1/grammar/*`). |
| `legend-engine-language-pure-compiler` | Builds a `PureModel` (compiled Pure graph) from `PureModelContextData`. Multi-pass compiler driven by `Processor<T>` handlers registered via `CompilerExtension` SPI. Entry point: `PureModel`. |
| `legend-engine-language-pure-compiler-http-api` | REST endpoints for compilation (`/api/pure/v1/compilation/*`). |
| `legend-engine-language-pure-modelManager` | `ModelManager` — caches compiled `PureModel` instances, supports live reload from SDLC. |
| `legend-engine-language-pure-modelManager-sdlc` | Loads models from Legend SDLC (GitLab-backed model store). |
| `legend-engine-protocol` | Core protocol POJOs (versioned, Jackson-annotated). |
| `legend-engine-protocol-pure` | Pure-specific protocol classes: `PureModelContextData`, execution plan nodes, etc. |

#### legend-engine-core-executionPlan-generation

| Sub-module | Purpose |
|------------|---------|
| `legend-engine-executionPlan-generation` | `PlanGenerator` — bridges Java ↔ Pure to produce an `ExecutionPlan`. Applies `PlanTransformer` chain (e.g. `JavaPlatformBinder` that injects generated Java source). |

#### legend-engine-core-executionPlan-execution

| Sub-module | Purpose |
|------------|---------|
| `legend-engine-executionPlan-execution` | `PlanExecutor` — walks the plan tree, dispatches to registered `StoreExecutor` implementations. Manages `ExecutionState`, result streaming, JIT Java compilation. |
| `legend-engine-executionPlan-execution-http-api` | REST endpoints (`/api/pure/v1/execution/*`, `/api/pure/v1/executionPlan/*`). |
| `legend-engine-executionPlan-execution-store-inMemory` | Executes in-memory (M2M) graph-fetch nodes — no external store required. |
| `legend-engine-executionPlan-execution-authorizer` | Pluggable authorisation hooks called before plan execution. |
| `legend-engine-executionPlan-dependencies` | Shared dependency jar for execution-time classpath (FreeMarker, etc.). |

### legend-engine-core-pure

| Sub-module | Purpose |
|------------|---------|
| `legend-engine-pure-code-compiled-core` | ~568 `.pure` files bundled as classpath resources. This is the engine's "standard library": router, execution plan metamodel, graph fetch, milestoning, M2M store, TDS, relations, bindings, and extension contracts. |
| `legend-engine-pure-code-functions-*` | Additional Pure function libraries: JSON, standard, relation, variant, javaCompiler, legendCompiler, planExecution, pureExtensions. |
| `legend-engine-pure-ide` | Pure IDE web server (`PureIDELight`) — used for interactive Pure development. |
| `legend-engine-pure-platform-modular-generation` | Code generation helpers for Pure platform bindings. |

---

## Store Modules (legend-engine-xts-relationalStore)

| Sub-module | Purpose |
|------------|---------|
| `legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-{dialect}` | One sub-module per supported SQL dialect: H2, PostgreSQL, Oracle, Snowflake, BigQuery, DuckDB, ClickHouse, Databricks, Hive, Presto/Trino, Redshift, MSSQL, Sybase, MySQL/MariaDB, Spanner, SparSQL, Athena, MemSQL. Each provides grammar, Pure functions, DDL commands, and a JDBC driver adapter. |
| `legend-engine-xt-relationalStore-execution/legend-engine-xt-relationalStore-executionPlan` | `RelationalExecutor` — executes SQL nodes. Handles parameterised SQL via FreeMarker templates, block connections, temp tables, graph-fetch streaming. |
| `legend-engine-xt-relationalStore-execution/legend-engine-xt-relationalStore-executionPlan-connection` | `ConnectionManagerSelector` — picks the right `DatabaseManager` and opens JDBC connections. HikariCP pool. |
| `legend-engine-xt-relationalStore-execution/legend-engine-xt-relationalStore-executionPlan-connection-authentication` | Pluggable auth-flow framework. `DatabaseAuthenticationFlowProvider` resolves credentials from the `CredentialProviderProvider` chain. |
| `legend-engine-xt-relationalStore-analytics` | REST analytics for relational models (entitlement checks, table stats). |
| `legend-engine-xt-relationalStore-PCT` | Pure Compatibility Tests — ensure SQL generation produces correct results across all dialects. |
| `legend-engine-xt-relationalStore-FCT-pure` | Function Compatibility Tests for relational functions. |
| `legend-engine-xt-relationalStore-MFT-pure` | Mapping Functionality Tests. |

---

## Service Store (legend-engine-xts-serviceStore)

Allows a Legend Mapping to call an HTTP/REST service as its data source. Includes grammar, protocol, Pure routing logic, and a WireMock-based test framework.

---

## External Format Modules

Each `xts-*` module below adds a new data format that can be used with the `Binding` concept.

| Module | Format |
|--------|--------|
| `legend-engine-xts-json` | JSON (schema inference, serialisation/deserialisation) |
| `legend-engine-xts-xml` | XML (XSD-backed) |
| `legend-engine-xts-flatdata` | CSV / fixed-width flat files |
| `legend-engine-xts-avro` | Apache Avro |
| `legend-engine-xts-protobuf` | Protocol Buffers |
| `legend-engine-xts-arrow` | Apache Arrow IPC format |
| `legend-engine-xts-powerbi` | PowerBI-compatible output |

---

## Query Protocol Modules

| Module | Description |
|--------|-------------|
| `legend-engine-xts-sql` | Parse SQL text → Pure query → execution plan. Provides a PostgreSQL wire-protocol server (`legend-engine-xt-sql-postgres-server`) enabling standard JDBC/ODBC tools to query Legend models. |
| `legend-engine-xts-graphQL` | Parse GraphQL queries → Pure query → execution. Includes a relational extension for direct DB fan-out. |
| `legend-engine-xts-tds` | Tabular Data Set (TDS) utilities: join, project, filter, group-by operations on in-memory result sets. |

---

## Function Activator Modules

Function Activators are the mechanism for *deploying* a Pure function to an external platform.

| Module | Platform |
|--------|---------|
| `legend-engine-xts-functionActivator` | Core SPI (grammar, compiler, deployment API) |
| `legend-engine-xts-snowflake` | Deploy as Snowflake Native App (stored procedure) |
| `legend-engine-xts-snowflakeApp` | Deploy as Snowflake M2M UDF |
| `legend-engine-xts-bigqueryFunction` | Deploy as BigQuery Remote Function |
| `legend-engine-xts-memsqlFunction` | Deploy as SingleStore (MemSQL) Function |
| `legend-engine-xts-hostedService` | Deploy as a hosted REST service |
| `legend-engine-xts-service` | Legend Service DSL (compile, execute, test, deploy) |
| `legend-engine-xts-functionJar` | Package Pure function as a self-contained JAR |
| `legend-engine-xts-persistence` | ETL pipeline specification (Trigger + Service + Target model) |

---

## New Packageable Element Modules

| Module | Adds |
|--------|------|
| `legend-engine-xts-text` | `Text` element — uninterpreted text blobs in the model |
| `legend-engine-xts-diagram` | `Diagram` element — visual layout of classes |
| `legend-engine-xts-data-space` | `DataSpace` element — discovery metadata for datasets |
| `legend-engine-xts-changetoken` | Change-token framework — upcast/downcast code generation for versioned payloads |
| `legend-engine-xts-generation` | Artifact generation framework — produce code/schemas from the model |
| `legend-engine-xts-dataquality` | Data quality rules specification and execution |

---

## Other Extension Modules

| Module | Description |
|--------|-------------|
| `legend-engine-xts-authentication` | Credential vault hierarchy, GCP/AWS federation, Pure authentication DSL |
| `legend-engine-xts-identity` | Extended `Identity` model (beyond `legend-shared`) |
| `legend-engine-xts-analytics` | Cross-cutting analytics APIs: lineage, class/mapping/binding/function analytics |
| `legend-engine-xts-openapi` | Model → OpenAPI 3.0 schema generation |
| `legend-engine-xts-protocol-java-generation` | Java class generation from the Pure protocol definitions |
| `legend-engine-xts-relationalai` | RelationalAI graph compute store integration |
| `legend-engine-xts-deephaven` | Deephaven real-time table store integration |
| `legend-engine-xts-elasticsearch` | Elasticsearch v7 store integration |
| `legend-engine-xts-mongodb` | MongoDB store integration |
| `legend-engine-xts-iceberg` | Apache Iceberg table format integration |
| `legend-engine-xts-ingest` | Data ingestion framework |

### Target Languages (code generation)

| Module | Target |
|--------|--------|
| `legend-engine-xts-java` | Java code generation from Pure models |
| `legend-engine-xts-haskell` | Haskell |
| `legend-engine-xts-daml` | DAML (Digital Asset) |
| `legend-engine-xts-morphir` | Morphir (functional IR) |
| `legend-engine-xts-rosetta` | Rosetta DSL |

---

## Configuration & Assembly Modules

### legend-engine-config

| Sub-module | Purpose |
|------------|---------|
| `legend-engine-server/legend-engine-server-http-server` | Main `Server` class (Dropwizard `Application`). Wires together all extensions, plan executor, model manager, and registers all REST resources. The production entry point. |
| `legend-engine-server/legend-engine-server-support-core` | Server-level utilities (health checks, metrics). |
| `legend-engine-server/legend-engine-server-integration-tests` | End-to-end integration test suite against a live server. |
| `legend-engine-extensions-collection-execution` | Aggregator POM — pulls in all execution-time extension jars. |
| `legend-engine-extensions-collection-generation` | Aggregator POM — pulls in all generation-time extension jars. |
| `legend-engine-repl` | Interactive REPL (`legend-engine-repl-client`). Shells: default, relational, DataCube. Backed by local DuckDB for quick iteration. |

---

## legend-engine-application-query

Standalone back-end for the Legend Query UI. Manages saved queries in MongoDB.
Entry point: `ApplicationQuery` JAX-RS resource.
