# 01 — Project Overview & Introduction

## What is Legend Engine?

Legend Engine is the **execution backbone** of the [FINOS Legend](https://legend.finos.org/) platform — an open-source, model-driven data management ecosystem originally developed at Goldman Sachs. Legend enables organizations to define, query, and govern data models using a high-level language called **Pure**, and then execute those models against diverse data stores without writing store-specific code.

Legend Engine specifically provides:

- A **Pure parser and compiler** — transforming Pure source text into an in-memory graph of typed metamodel objects.
- An **execution engine** — generating and executing *execution plans* when given a Pure function, a Mapping, and a Runtime.
- A **extensible platform** — plugin-based architecture for adding stores, data formats, deployment targets, and query protocols.

## Role in the Legend Ecosystem

Legend Engine does not operate in isolation. It is part of a family of projects:

| Project | Purpose |
|---------|---------|
| **[legend-pure](https://github.com/finos/legend-pure)** | The Pure language runtime, metamodel (M3), interpreter, and compiled execution engine |
| **legend-engine** *(this project)* | Grammar, compiler, plan generation, plan execution, store integrations, HTTP API |
| **[legend-shared](https://github.com/finos/legend-shared)** | Shared infrastructure (Pac4j auth, server utils, OpenTracing) |
| **[legend-sdlc](https://github.com/finos/legend-sdlc)** | Software Development Lifecycle — versioning, review, CI/CD for Pure models |
| **[legend-depot](https://github.com/finos/legend-depot)** | Artifact storage for published Legend models |
| **[legend-studio](https://github.com/finos/legend-studio)** | Web-based IDE for authoring Pure models and mappings |

Legend Engine depends on `legend-pure` (for the Pure metamodel and runtime) and `legend-shared` (for server infrastructure). It is consumed by `legend-sdlc`, `legend-studio`, and downstream applications.

## Value Proposition

The core idea behind Legend (and Legend Engine in particular) is **write models once, execute anywhere**:

1. Users define data models, mappings, and business logic in **Pure** — a platform-independent, strongly-typed language.
2. Legend Engine **compiles** these definitions into an in-memory graph.
3. When a query is issued, the engine generates an **execution plan** — a tree of operations targeting the appropriate stores.
4. The plan is **executed** against real databases (Postgres, Snowflake, DuckDB, etc.), REST APIs, document stores, or in-memory.
5. Results flow back through the plan, get assembled, and are returned to the caller.

This architecture means that switching from one database to another, or combining data from multiple stores, requires no changes to the core business logic.

## Repository Structure

The project follows a **Maven multi-module** layout with clear naming conventions:

```
legend-engine/
├── legend-engine-core/                    # Core platform (always required)
│   ├── legend-engine-core-base/           # Language pipeline + execution engine
│   │   ├── legend-engine-core-language-pure/   # Grammar, compiler, protocol, model manager
│   │   ├── legend-engine-core-executionPlan-generation/
│   │   └── legend-engine-core-executionPlan-execution/
│   ├── legend-engine-core-pure/           # Compiled Pure code, function libraries, IDE
│   ├── legend-engine-core-shared/         # Shared utilities, extension SPIs, vault
│   ├── legend-engine-core-external-format/ # External format base framework
│   ├── legend-engine-core-identity/       # Identity abstractions
│   ├── legend-engine-core-testable/       # Test framework, MFT, test runners
│   └── legend-engine-core-query-pure-http-api/  # Query HTTP endpoints
│
├── legend-engine-xts-*/                   # Extensions (30+ modules)
│   ├── legend-engine-xts-relationalStore/ # Relational store (SQL databases)
│   ├── legend-engine-xts-serviceStore/    # REST API as data source
│   ├── legend-engine-xts-mongodb/         # MongoDB document store
│   ├── legend-engine-xts-elasticsearch/   # Elasticsearch
│   ├── legend-engine-xts-deephaven/       # Deephaven real-time grids
│   ├── legend-engine-xts-json/            # JSON Schema external format
│   ├── legend-engine-xts-xml/             # XML/XSD external format
│   ├── legend-engine-xts-flatdata/        # Flat file (CSV, etc.) format
│   ├── legend-engine-xts-avro/            # Avro format
│   ├── legend-engine-xts-arrow/           # Arrow format
│   ├── legend-engine-xts-protobuf/        # Protobuf format
│   ├── legend-engine-xts-snowflake/       # Snowflake function activator
│   ├── legend-engine-xts-bigqueryFunction/ # BigQuery function activator
│   ├── legend-engine-xts-hostedService/   # Hosted REST service activator
│   ├── legend-engine-xts-service/         # Legend services with test suites
│   ├── legend-engine-xts-persistence/     # Data persistence pipelines
│   ├── legend-engine-xts-sql/             # SQL query protocol
│   ├── legend-engine-xts-graphQL/         # GraphQL query protocol
│   ├── legend-engine-xts-authentication/  # Authentication framework
│   ├── legend-engine-xts-analytics/       # Lineage, search, and other analytics
│   └── ... (more extensions)
│
├── legend-engine-config/                  # Runtime configuration & server
│   ├── legend-engine-server/              # Dropwizard HTTP server
│   ├── legend-engine-repl/                # Interactive REPL
│   ├── legend-engine-extensions-collection-*/ # Extension assembly modules
│   └── legend-engine-configuration/       # Configuration classes
│
├── docs/                                  # Documentation (including this suite)
├── pom.xml                                # Root POM (~4400 lines, manages all versions)
└── README.md
```

## Module Naming Conventions

| Prefix/Suffix | Meaning |
|---------------|---------|
| `legend-engine-core-*` | Core platform modules, always required |
| `legend-engine-xts-*` | Extensions — stores, formats, activators, protocols |
| `*-pure` | Pure code resources (`.pure` files compiled or bundled as PARs) |
| `*-protocol` | JSON-serializable protocol models (Java POJOs for serialization) |
| `*-grammar` | ANTLR4-based grammar for a Pure DSL section |
| `*-compiler` | Compiler pass — transforms protocol to Pure graph objects |
| `*-execution` | Execution-time logic (plan generation, plan execution) |
| `*-http-api` | REST endpoints (JAX-RS resources) |
| `*-generation` | Code or artifact generation |
| `*-PCT` | Platform Compatibility Tests for a specific store/database |
| `*-MFT-pure` | Multi-Function Tests in Pure |

## Build System

- **Build tool**: Maven 3.6+
- **Language**: Java (JDK 11 required to build, target bytecode Java 8)
- **Pure runtime**: Legend Pure `5.80.0` (metamodel, interpreter, compiled runtime)
- **Key plugins**: ANTLR4 (grammar generation), JaCoCo (coverage), Checkstyle (code style), Pure PAR/Java generation
- **Dependency management**: ~200+ managed dependencies in the root POM with enforcer rules for version convergence, banned dependencies (log4j, commons-logging), and strict dependency analysis

## Quick Start

```bash
# Build everything (slow, ~1 hour)
mvn install

# Start the server
java -cp ... org.finos.legend.engine.server.Server server legend-engine-config/legend-engine-server/legend-engine-server-http-server/src/test/resources/org/finos/legend/engine/server/test/userTestConfig.json

# Access swagger
open http://127.0.0.1:6300/api/swagger

# Start Pure IDE
java -cp ... org.finos.legend.engine.ide.PureIDELight server legend-engine-pure/legend-engine-pure-ide/legend-engine-pure-ide-light-http-server/src/main/resources/ideLightConfig.json

# Access Pure IDE
open http://127.0.0.1:9200/ide
```

## Next

→ [02 — Architecture Overview](02-architecture-overview.md)
