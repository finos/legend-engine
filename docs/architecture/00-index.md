# Legend Engine — Architecture Documentation

A comprehensive documentation suite for understanding Legend Engine end-to-end.

**Audience**: Platform users and engineers evaluating or re-engineering Legend Engine features.

## Reading Order

| # | Document | Description |
|---|----------|-------------|
| 01 | [Project Overview](01-project-overview.md) | What Legend Engine is, repository structure, module conventions |
| 02 | [Architecture Overview](02-architecture-overview.md) | End-to-end data flow, layer diagram, key design principles |
| 03 | [Pure Language Pipeline](03-pure-language-pipeline.md) | Grammar, protocol, compiler, model manager |
| 04 | [Execution Pipeline](04-execution-pipeline.md) | Plan generation, plan execution, store executors |
| 05 | [Extension System & SPI](05-extension-system.md) | How to extend Legend Engine with new languages, stores, formats |
| 06 | [Store Extensions](06-store-extensions.md) | Relational, Service Store, MongoDB, Elasticsearch, Deephaven |
| 07 | [External Format Framework](07-external-format-framework.md) | SchemaSet, Binding, format extensions (XML, JSON, FlatData, Avro, etc.) |
| 08 | [Function Activators & Deployment](08-function-activators.md) | Snowflake, BigQuery, Hosted Service, Persistence, Function JAR |
| 09 | [Query Protocols](09-query-protocols.md) | SQL and GraphQL translation layers |
| 10 | [Identity, Auth & Security](10-identity-auth-security.md) | Identity, credentials, vault, connection factory |
| 11 | [PCT Framework](11-pct-framework.md) | Platform Compatibility Testing across databases |
| 12 | [Runtime, Server & REPL](12-runtime-server-repl.md) | Dropwizard server, REPL, Pure IDE, configuration |
| 13 | [Non-Functional Requirements](13-non-functional-requirements.md) | Build, CI, security, performance, extensibility, observability |

## Appendices

| # | Document | Description |
|---|----------|-------------|
| A1 | [Grammar Extensions Reference](A1-grammar-extensions-reference.md) | Complete catalog of all `###Section` grammar extensions |
| A2 | [Compiler Extensions Reference](A2-compiler-extensions-reference.md) | All compiler extensions, processors, and hook methods |
| A3 | [Artifact Generation Reference](A3-artifact-generation-reference.md) | All generation extensions across four interface types |

## Existing Documentation

These architecture docs complement the existing topic-specific docs in the parent [`docs/`](../) directory. Cross-references are provided throughout.
