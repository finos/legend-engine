# Legend Engine — User-Facing Functionality

> **Owner:** Engineering team  
>
> **Audience:** Developers, data engineers, data architects, and platform operators who want to
> understand what Legend Engine lets users and applications *do*. For implementation details see the
> [Architecture Overview](architecture/overview.md).

---

## 1. What is Legend Engine?

Legend Engine is the **execution backbone of the Legend platform**. It lets you express your
business domain, data mappings, and queries in a single high-level language (Pure), then run
them against any supported data store — without writing database-specific code.

It is consumed by graphical tools (Legend Studio, Legend Query), an interactive command-line
environment (Legend REPL), and any HTTP client, making the same capabilities available whether
you prefer a visual IDE, a terminal, or a programmatic API.

---

## 2. Data Modelling

### 2.1 Domain Models

Users describe their business domain as a set of **classes** (typed objects with properties),
**associations** (navigable relationships between classes), **enumerations** (fixed sets of named
values), and **constraints** (business rules that instances must satisfy). Derived or computed
properties can be expressed inline, and all model elements can be annotated with custom
stereotypes and tags for documentation or tooling purposes.

Everything lives in a hierarchical package namespace, keeping large models organised.

### 2.2 Relational Database Models

The physical structure of a relational database — its schemas, tables, views, and the
foreign-key or expression-based relationships between them — is described declaratively
alongside the domain model. Row-level security filters and bi-temporal markers (for
point-in-time data) are also declared here, and the engine enforces them automatically at
query time.

### 2.3 Mappings

A **Mapping** is the bridge between the logical domain and the physical data. It specifies
how each domain class and its properties correspond to database columns, SQL expressions, or
transformations over another class model. A single class can be mapped to multiple tables,
multiple physical sources can be combined via union or merge, and mappings can be layered
(model-to-model) so that one logical view sits on top of another. Aggregation-aware mappings
allow the engine to silently redirect queries to pre-aggregated tables when the query shape
matches.

The engine validates that every mapped property has a complete, consistent physical
implementation before any query is run.

### 2.4 Runtimes and Connections

A **Runtime** combines a mapping with the live credentials and connection details needed to
reach the underlying store. Connections cover relational databases (JDBC), inline JSON or XML
data, chained model-to-model mappings, and HTTP-based service stores.

Authentication is handled separately from connection logic: supported strategies include
username/password, Kerberos, OAuth 2.0, GCP service accounts, AWS IAM, and API keys.
Credentials are resolved at runtime from a configurable vault (environment variables,
properties files, or a cloud secrets manager), so no secrets appear in model definitions.

---

## 3. Querying

### 3.1 Writing Queries

Queries are written as lambda functions in the Pure language. A query selects a class, applies
filters, shapes the result (projecting columns or fetching a nested object graph), and can sort,
paginate, or aggregate the output. The engine compiles the query against its mapping and
runtime, generates an optimised execution plan, and streams back the results.

### 3.2 Tabular (TDS) Queries

For tabular results, the full spectrum of SQL operations is available through Pure functions:
filtering, projection, grouping and aggregation, joins (inner, left, right, full outer),
sorting, pagination, distinct, union, and window / OLAP aggregations. The engine translates
these to the correct SQL dialect for the target database automatically.

### 3.3 Object-Graph Queries

Instead of flat rows, a graph fetch query returns a **tree of objects** whose shape is
declared upfront. Only the specified properties are fetched, avoiding over-fetching. Graph
fetches can span multiple stores: the engine batches requests across store boundaries
automatically.

A checked variant of graph fetch collects data quality defects (missing required values,
constraint violations) and attaches them to the relevant object rather than aborting the
entire query, giving callers a complete picture of data health.

### 3.4 Parameterised Queries

Queries accept typed parameters that are supplied at execution time. The engine validates
parameter types and multiplicities before executing, giving clear errors for mismatched
inputs.

### 3.5 Bi-temporal (Milestoned) Queries

Classes that carry temporal markers (processing time, business time, or both) have the
relevant `WHERE` clauses injected automatically when a time context is provided. Users pass
the desired date(s) directly in the query call — no manual date-range predicates required.

### 3.6 SQL Interface

Users who prefer plain SQL can connect any JDBC/ODBC-compatible tool (DBeaver, Tableau,
Power BI, etc.) directly to Legend Engine via a standard PostgreSQL wire-protocol endpoint.
Queries flow through the same planning and execution pipeline as Pure queries.

### 3.7 GraphQL Interface

GraphQL clients can query Legend models through a dedicated GraphQL endpoint. Both data
fetches and schema introspection are supported, and queries are translated internally to the
same query-plan infrastructure.

---

## 4. Execution Plans

### 4.1 Inspectable Query Plans

Before (or instead of) executing a query, the engine can produce an **execution plan** — a
serialisable, human-readable representation of exactly what will happen: which SQL will be
sent to which database, how results will be transformed, and what the output shape will be.
Plans can be inspected in Studio's Execution Plan Viewer, cached for re-use, or stored
offline for audit and debugging purposes.

### 4.2 Direct Plan Execution

A pre-generated plan can be submitted directly for execution with a set of parameter values,
bypassing the compilation and planning steps entirely. This is useful for high-throughput
scenarios where plans are generated once and reused many times.

### 4.3 Result Formats

Query results can be streamed in several formats, chosen per request:

| Format | Best for |
|---|---|
| JSON (row objects) | Default; general-purpose consumption |
| CSV | Tabular downloads and spreadsheet import |
| Apache Arrow IPC | High-throughput analytical pipelines |
| JSON (object graph) | Graph-fetch / object-serialisation results |

---

## 5. Services

### 5.1 What is a Service?

A **Service** is a versioned, deployable REST endpoint built from a Pure query. It bundles
the query, its mapping and runtime, URL pattern, ownership, documentation, and test cases
into a single artefact. Once deployed, callers hit the endpoint's URL with path or query
parameters and receive results in the requested format.

### 5.2 Service Capabilities

- **Parameterised URLs** — path segments are automatically bound to query parameters.
- **Multi-environment routing** — a single service definition can target different databases
  or mappings based on a discriminator (e.g. `dev` vs `prod`), without duplicating the query.
- **Built-in test suites** — expected results are embedded in the service definition and can
  be validated in CI without a live database.
- **Post-execution validations** — data quality assertions run after execution and fail the
  response if the result set violates declared constraints.
- **Ownership controls** — services have declared owners (individuals or CI/CD pipelines)
  who are authorised to update or redeploy them.
- **LLM tool exposure** — a service can be surfaced as an AI tool call via the Model Context
  Protocol (see §13).

### 5.3 Caching and Performance

The engine compiles and plans a service query on first call, then caches the plan for
subsequent requests. Results are streamed, keeping memory usage constant regardless of result
set size.

---

## 6. Deploying Pure Functions (Function Activators)

Function Activators package a Pure function as a native artefact on an external compute
platform. The engine handles compilation, code generation, and deployment; users simply
declare the target platform and trigger the deployment.

| Target platform | What is deployed |
|---|---|
| Snowflake | A stored procedure or user-defined function |
| Google BigQuery | A remote function backed by a Cloud Run service |
| SingleStore | A native database function |
| Legend-hosted REST | A versioned REST endpoint managed by the platform |
| JVM / JAR | A self-contained executable JAR |
| Databricks | A notebook or job |

All targets share the same authoring experience: write the function once in Pure, choose a
target, deploy.

---

## 7. Data Quality

### 7.1 Constraints

Business rules are declared as named constraints on domain classes. When constraint checking
is enabled, the engine evaluates every result object against these rules and surfaces
violations alongside the data.

### 7.2 Data Quality Validations

A dedicated **Data Quality Validation** definition lets teams codify repeatable quality checks
over a mapped class: which data to examine (via an optional filter), and which constraints
must hold. Validation runs produce a stream of results where each failing object is annotated
with the specific defects found, enabling targeted remediation.

### 7.3 Non-fatal Checked Execution

Any query can be run in "checked" mode, which collects constraint violations and missing
required values as defects attached to each result object, rather than aborting the query on
the first failure. This is particularly useful for profiling data quality across an entire
dataset.

---

## 8. External Formats and Bindings

A **Binding** declares that a domain class model corresponds to a specific serialisation
format. With a binding in place, the engine can:

- **Read** (internalize) raw bytes in that format and produce typed domain objects.
- **Write** (externalize) domain objects back to bytes in that format.

Supported formats include JSON, XML, CSV and fixed-width flat files, Apache Avro, Protocol
Buffers, and Apache Arrow IPC.

Existing schemas (JSON Schema, XSD, Avro, Protobuf) can be **imported** to automatically
generate the corresponding domain model and binding, eliminating manual class-by-class
modelling.

---

## 9. Code and Schema Generation

The engine can generate artefacts directly from Pure models, without executing any query:

| What you get | Typical use case |
|---|---|
| Java classes | Embedding the domain model in a JVM application |
| Haskell types | Functional language integration |
| DAML records | Smart-contract or ledger integration |
| Morphir IR | Cross-platform functional representation |
| Rosetta DSL | Financial domain language interoperability |
| OpenAPI 3.0 spec | Auto-generating API documentation for a Service |
| Avro / Protobuf schema | Schema-registry publishing and schema evolution |

Generation is available via a general-purpose REST endpoint or format-specific endpoints.

---

## 10. Persistence (ETL Pipelines)

The **Persistence** DSL lets users define full ETL pipeline specifications declaratively:
what data to move, when to move it, and where to put it.

- **Triggers** — scheduled (cron), event-driven (upstream data readiness), or continuous
  (change-data capture / streaming).
- **Targets** — relational tables, MongoDB collections, or object stores such as S3.
- **Load modes** — full snapshot replacement or incremental delta (append or upsert).
- **Slowly-changing dimensions** — Type 1 (overwrite) and Type 2 (history-preserving) SCD
  strategies are built in.
- **Alerting** — failure notifications via PagerDuty or email.
- **Multi-environment** — a single pipeline definition can be instantiated with different
  connection details for dev, staging, and production.

---

## 11. Analytics and Discovery

### 11.1 Data Lineage

Given any query or service, the engine can trace exactly which source columns feed which
output columns, through every join and transformation step. This lineage is available as a
REST API response and is visualised in Studio.

### 11.2 DataSpaces

A **DataSpace** is a curated discovery package that groups related classes, mappings,
runtimes, diagrams, and documentation into one browsable entry point. It is the recommended
starting point for a user exploring an unfamiliar data domain.

### 11.3 Entitlement and Access Analytics

REST endpoints are available to determine which tables and columns a query touches, and
whether a given user identity has the necessary permissions to access them — useful for
access-review workflows and query-level entitlement checks.

### 11.4 Model Analytics

The engine exposes analytical views over the model itself: class hierarchy navigation, mapping
coverage (which classes are mapped and where), function dependency graphs, and
schema-to-binding cross-references.

---

## 12. Interactive Tooling

### 12.1 Browser-based Pure IDE

A lightweight browser IDE is bundled with the engine for writing, running, and debugging Pure
code interactively. It provides syntax highlighting, auto-complete, one-keystroke execution,
and inline breakpoint debugging with variable inspection. No separate installation is needed;
it starts alongside the engine server.

### 12.2 Command-line REPL

The Legend REPL is a terminal-based interactive environment for Pure. Beyond evaluating
expressions, it includes:

- **DataCube** — a grid UI backed by a local in-process database for fast, server-free
  exploration of CSV and Parquet data.
- **Relational shell** — connects to any live JDBC database for interactive SQL-backed
  queries.

No server deployment is required; the REPL runs as a standalone process.

### 12.3 REST API Documentation

All engine REST endpoints are self-documented and browsable via a Swagger UI hosted on the
running server. Endpoints are organised by domain area: grammar, compilation, query
execution, plan generation, code generation, services, and analytics.

---

## 13. AI Integration (Model Context Protocol)

Legend Services can be exposed as **tool calls for LLM agents** via the
[Model Context Protocol](https://modelcontextprotocol.io/). An agent can discover what
services are available, inspect their parameter schemas, and invoke them to retrieve
structured data — enabling natural-language query interfaces over any Legend-managed dataset
without custom integration work.

---

## 14. Capability Summary

| Capability | What you can do |
|---|---|
| **Domain modelling** | Define classes, relationships, enumerations, and business-rule constraints in a language-neutral way |
| **Database modelling** | Describe relational schemas declaratively, including security filters and temporal markers |
| **Mapping** | Bridge domain classes to physical data sources; compose and union mappings across stores |
| **Connections & auth** | Connect to any supported store with a wide range of authentication strategies; keep secrets out of model files |
| **Pure queries** | Express filter, project, aggregate, join, and paginate operations as composable functions |
| **SQL & GraphQL access** | Query Legend models using standard SQL or GraphQL from any compatible tool |
| **Execution plan inspection** | View, cache, and reuse the generated query plan before or after execution |
| **Result streaming** | Receive results as JSON rows, CSV, or Apache Arrow for any query or service |
| **Services** | Package queries as versioned, tested, owned REST endpoints with URL parameters |
| **Function Activators** | Deploy Pure functions as native artefacts on Snowflake, BigQuery, Databricks, and more |
| **Data quality** | Declare constraints and validation rules; run checked queries that collect defects non-fatally |
| **External formats** | Read and write JSON, XML, CSV, Avro, Protobuf, and Arrow; import existing schemas automatically |
| **Code & schema generation** | Generate Java, Haskell, OpenAPI, Avro, Protobuf, and other artefacts from Pure models |
| **Persistence / ETL** | Define scheduled or streaming pipelines that load data into relational, document, or object stores |
| **Lineage & analytics** | Trace column-level lineage, check entitlements, and analyse model coverage |
| **Interactive tooling** | Write and debug Pure in a browser IDE or a command-line REPL |
| **AI tool integration** | Expose services as LLM tool calls via the Model Context Protocol |
