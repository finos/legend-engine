Welcome to the Legend Engine developer documentation. This living documentation set is
version-controlled alongside the code and should be updated as part of every significant
change.

---

## 5-Minute Overview

### What is Legend Engine?

**Legend Engine is the execution backbone of the [FINOS Legend](https://legend.finos.org/) platform.**

Legend (originally developed at Goldman Sachs) is an open-source suite for financial
data management and governance. Legend Engine sits above `legend-pure` in the stack: it
takes the Pure language runtime provided by `legend-pure` and builds a production-ready
HTTP execution service on top of it.

Given a Pure query (lambda function), a Mapping, and a Runtime, Legend Engine:

1. **Parses** the textual Legend grammar into a versioned JSON protocol.
2. **Compiles** the protocol into an in-memory Pure object graph (`PureModel`).
3. **Plans** the execution: routes sub-expressions to stores, generates SQL/query fragments,
   and assembles an `ExecutionPlan` tree.
4. **Executes** the plan: connects to databases or services, streams results, and serialises
   them to JSON / CSV / Arrow.

It is deployed as a **Dropwizard HTTP server** and is used by Legend Studio, Legend Query,
the Legend REPL, and any custom client.

See [Architecture Overview](architecture/overview.md) for the full component map, pipeline
descriptions, and extension architecture.

---

### Key Functional Concepts

#### 1. The Five Core Pipelines

| Pipeline | Entry point | Description |
|----------|-------------|-------------|
| **Grammar ↔ Protocol** | `POST /api/pure/v1/grammar/transformGrammarToJson` | Converts Legend text ↔ `PureModelContextData` JSON |
| **Compilation** | `POST /api/pure/v1/compilation/compile` | Builds a fully-typed `PureModel` from a model snapshot |
| **Plan Generation** | `POST /api/pure/v1/executionPlan/generate` | Produces a serialisable `ExecutionPlan` from a query |
| **Plan Execution** | `POST /api/pure/v1/execution/execute` | Executes a plan against live stores |
| **Service Execution** | Service endpoint | Compiles + plans + executes a packaged Legend Service inline |

#### 2. The Module Taxonomy

Modules are grouped by a consistent naming prefix:

| Prefix | Role |
|--------|------|
| `legend-engine-core` | Compiler, grammar, plan generation, plan execution, Pure runtime |
| `legend-engine-xts-*` | Extension modules — stores, formats, DSLs, function activators |
| `legend-engine-config` | Server assembly, REPL, configuration |
| `legend-engine-application-query` | Saved-query backend |

Sub-module suffixes (`-grammar`, `-protocol`, `-compiler`, `-pure`, `-execution`, `-http-api`) are
consistent across all `xts-*` modules. See [Module Reference](architecture/modules.md).

#### 3. The Extension Model

Legend Engine is designed for extension without modifying core modules:

- **Java side:** SPI interfaces discovered via `java.util.ServiceLoader` (`META-INF/services/`).
- **Pure side:** `meta::pure::extension::Extension` — a class threaded through all planning
  and execution functions as an explicit parameter.

Every `xts-*` module registers both Java and Pure extension objects. See
[Architecture Overview — Extension Architecture](architecture/overview.md#4-extension-architecture).

#### 4. Stores

A Store is an abstract data source. The engine ships with:

| Store | Module |
|-------|--------|
| **Relational** (15+ SQL dialects) | `legend-engine-xts-relationalStore` |
| **Model-to-Model (M2M)** | built into `legend-engine-core` |
| **Service Store** (HTTP) | `legend-engine-xts-serviceStore` |
| **MongoDB** | `legend-engine-xts-mongodb` |
| **Elasticsearch** | `legend-engine-xts-elasticsearch` |
| **Deephaven** | `legend-engine-xts-deephaven` |
| **RelationalAI** | `legend-engine-xts-relationalai` |

#### 5. PCT — Cross-Store Compatibility Tests

PCT (Pure Compatibility Tests) annotates Pure functions with `<<PCT.test>>` and runs them
against every registered store on every CI build, ensuring consistent behaviour across
all backends. See [Testing Strategy — PCT](testing/testing-strategy.md#5-pct-pure-compatibility-tests).

---

## Documentation Map

### Architecture

| Section | What it covers |
|---------|---------------|
| [Architecture Overview](architecture/overview.md) | What Legend Engine is, the 5 core pipelines, extension model, module dependency layers |
| [Domain & Key Concepts](architecture/domain-concepts.md) | Class/Mapping/Runtime/Store model, execution concepts, glossary, design patterns |
| [Key Java Areas](architecture/key-java-areas.md) | Grammar layer, PureModel compiler, PlanGenerator, PlanExecutor, Relational executor, Auth, Server assembly |
| [Key Pure Areas](architecture/key-pure-areas.md) | Router, ExecutionPlan metamodel, GraphFetch, Milestoning, TDS/Relation, M2M chain, Service metamodel, Binding, PCT |
| [Alloy Compiler](architecture/alloy-compiler.md) | Deep-dive on the Alloy (Legend Engine) compiler: compilation phases, type/function resolution, extension points, and testing |
| [Execution Plans](architecture/execution-plans.md) | What execution plans are, the planning/execution split, node catalogue, caching, versioning |
| [Router & Pure-to-SQL](architecture/router-and-pure-to-sql.md) | Routing strategies, clustering, SQL generation pipeline, dialect extension |
| [Pre-Evaluation (preeval)](architecture/preeval.md) | AST simplification pass that runs before the router: constant folding, let inlining, short-circuiting |

### Guides

| Section | What it covers |
|---------|---------------|
| [Getting Started Guide](guides/getting-started.md) | Prerequisites, clone → build → run, Pure IDE, REPL, config files, troubleshooting |
| [Build & CI Guide](guides/build-and-ci.md) | Full build lifecycle, Maven plugins, profiles, GitHub Actions pipeline, Docker, releases |
| [Contributor Workflow](guides/contributor-workflow.md) | How to add a grammar section, store extension, or function activator |
| [Exploration & Discovery](guides/exploration.md) | Systematic approach for new engineers exploring the codebase |
| [Identity, Authentication & Traceability](guides/identity-authentication-guide.md) | Identity model, credential types, authentication flows, vault integration, end-to-end traceability |
| [Logging, Tracing & Observability](guides/logging-tracing-observability.md) | Structured logging with `LogInfo`, OpenTracing, OpenTelemetry, Prometheus metrics |

### Standards & Process

| Section | What it covers |
|---------|---------------|
| [Coding Standards](standards/coding-standards.md) | Checkstyle rules, naming conventions, Git workflow, PR checklist, logging, exceptions |
| [Testing Strategy](testing/testing-strategy.md) | Testing pyramid, frameworks, PCT, how to run tests, CI matrix, coverage |
| [Documentation Maintenance](maintenance/maintenance.md) | Keeping docs up to date, ownership model, review cadence |
| [Phased Documentation Plan](maintenance/documentation-plan.md) | The phased timeline that produced this documentation set |
| [Architecture Decision Records](decisions/) | Significant technical decisions with context and consequences |

### Reference

| Section | What it covers |
|---------|---------------|
| [Module Reference](reference/modules.md) | Every module group and sub-module with entry-point classes — look up any module here |
| [Technology Stack](reference/tech-stack.md) | Third-party library catalogue: versions, rationale, upgrade instructions |
| [TDS & Relation Function Reference](reference/tds-and-relation.md) | Complete function catalogue for `TabularDataSet` and `Relation<T>` APIs |
| [Module README Template](templates/module-readme-template.md) | Standard template for per-module README files |

---

## Quick-Start (TL;DR)

```bash
# Prerequisites: JDK 11, Maven 3.6.2+
git clone https://github.com/finos/legend-engine.git
cd legend-engine
mvn install -DskipTests -T 4          # fast first build (~15-25 min)
mvn install -T 4                       # full build with tests
```

**Run the server:**

```bash
# Main class: org.finos.legend.engine.server.Server
# Args: server legend-engine-config/legend-engine-server/legend-engine-server-http-server/src/test/resources/org/finos/legend/engine/server/test/userTestConfig.json
```

Swagger UI: <http://127.0.0.1:6300/api/swagger>

See the [Getting Started Guide](guides/getting-started.md) for the full walkthrough.

---

## Security

To report a vulnerability, follow the process in
[`SECURITY.md`](../../SECURITY.md) — do **not** open a public GitHub issue.

---

## Contributing to Docs

1. All documentation lives under `docs/engineering/` in Markdown.
2. Every PR that changes code in a module **must** update the corresponding docs if
   behaviour, dependencies, or build steps change.
3. Use the [Module README Template](templates/module-readme-template.md) when adding a new module.
4. See [Documentation Maintenance](maintenance/maintenance.md) for ownership and review cadence.
