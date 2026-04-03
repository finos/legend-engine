# 02 — Architecture Overview

## End-to-End Data Flow

Legend Engine processes data through a well-defined pipeline. Understanding this pipeline is essential to re-engineering any component:

```mermaid
flowchart LR
    A["Pure Source Text"] --> B["Grammar Parser"]
    B --> C["Protocol Model (JSON)"]
    C --> D["Compiler"]
    D --> E["Pure Graph (M3)"]
    E --> F["Plan Generator"]
    F --> G["Execution Plan"]
    G --> H["Plan Executor"]
    H --> I["Store / Database"]
    I --> H
    H --> J["Results"]
```

Each stage is independently extensible:

| Stage | Module Area | What Happens |
|-------|-------------|--------------|
| **Parse** | `legend-engine-language-pure-grammar` | ANTLR4 turns Pure text into a parse tree, then into protocol objects |
| **Protocol** | `legend-engine-protocol-pure` | JSON-serializable Java POJOs representing every Pure element |
| **Compile** | `legend-engine-language-pure-compiler` | Multi-pass transformation from protocol objects to Pure graph (M3 metamodel) |
| **Plan Generation** | `legend-engine-executionPlan-generation` | Given a Pure function + Mapping + Runtime, produce an execution plan tree |
| **Plan Execution** | `legend-engine-executionPlan-execution` | Walk the plan tree, dispatch to store-specific executors, assemble results |

## Layer Architecture

The system is organized into horizontal layers, each with clear responsibilities:

```mermaid
graph TB
    subgraph "Presentation Layer"
        HTTP["HTTP API (JAX-RS / Dropwizard)"]
        REPL["REPL"]
        IDE["Pure IDE"]
    end

    subgraph "Language Layer"
        Grammar["Grammar (ANTLR4)"]
        Protocol["Protocol (JSON POJOs)"]
        Compiler["Compiler (Multi-Pass)"]
        ModelMgr["Model Manager / SDLC"]
    end

    subgraph "Execution Layer"
        PlanGen["Plan Generator"]
        PlanExec["Plan Executor"]
        Auth["Authorizer"]
    end

    subgraph "Store Layer"
        Relational["Relational Store"]
        ServiceStore["Service Store"]
        MongoDB["MongoDB"]
        ES["Elasticsearch"]
        DH["Deephaven"]
        InMem["In-Memory"]
    end

    subgraph "External Formats"
        JSON["JSON"]
        XML["XML/XSD"]
        FlatData["FlatData"]
        Avro["Avro"]
        Arrow["Arrow"]
        Proto["Protobuf"]
    end

    subgraph "Cross-Cutting"
        Identity["Identity & Auth"]
        Vault["Credential Vault"]
        Extensions["Extension SPI"]
        TestFW["Test Framework"]
    end

    HTTP --> Grammar
    REPL --> Grammar
    IDE --> Grammar
    Grammar --> Protocol
    Protocol --> Compiler
    Compiler --> PlanGen
    ModelMgr --> Compiler
    PlanGen --> PlanExec
    Auth --> PlanExec
    PlanExec --> Relational
    PlanExec --> ServiceStore
    PlanExec --> MongoDB
    PlanExec --> ES
    PlanExec --> DH
    PlanExec --> InMem
    Relational --> Identity
    ServiceStore --> Identity
```

## Key Abstraction: The "Three Pillars"

Every new feature in Legend (a new store, format, or deployment target) plugs into the platform through **three pillars** that map to the three pipeline stages:

```mermaid
graph LR
    subgraph "Pillar 1: Language"
        G["Grammar Extension"]
        P["Protocol Extension"]
        C["Compiler Extension"]
    end

    subgraph "Pillar 2: Execution"
        PG["Plan Generation Extension"]
        PE["Plan Execution Extension"]
    end

    subgraph "Pillar 3: Pure Code"
        PC["Pure Core Functions"]
        PM["Pure Metamodel Extensions"]
    end

    G --> P --> C --> PG --> PE
    PM --> C
    PC --> PG
```

For example, adding support for a new database (say, MySQL) requires:
1. **Grammar extension** — new DSL syntax for MySQL-specific connection config
2. **Protocol extension** — Java POJOs for MySQL connection, datasource spec
3. **Compiler extension** — compiling MySQL protocol objects into Pure graph objects
4. **Plan generation extension** — generating SQL specifically for MySQL dialect
5. **Plan execution extension** — connecting to MySQL and executing the generated SQL
6. **Pure code** — any MySQL-specific Pure functions or metamodel entries

## Module Dependency Hierarchy

The dependency flow is **strictly top-down** — lower layers never depend on higher layers:

```mermaid
graph TD
    Server["legend-engine-config (Server, REPL)"]
    ExtCollections["Extension Collections"]
    XTS["legend-engine-xts-* (Extensions)"]
    CoreQuery["core-query-pure-http-api"]
    CoreTestable["core-testable"]
    CoreExtFormat["core-external-format"]
    CoreBase["core-base (Language + Execution)"]
    CorePure["core-pure (Compiled Pure Code)"]
    CoreShared["core-shared (SPI, Utils, Vault)"]
    CoreIdentity["core-identity"]

    Server --> ExtCollections
    ExtCollections --> XTS
    XTS --> CoreBase
    XTS --> CorePure
    XTS --> CoreExtFormat
    CoreQuery --> CoreBase
    CoreTestable --> CoreBase
    CoreExtFormat --> CoreBase
    CoreBase --> CoreShared
    CoreBase --> CorePure
    CorePure --> CoreShared
    CoreShared --> CoreIdentity
```

## Design Principles

### 1. Extension-First Architecture
Every major feature area (stores, formats, activators, query protocols) is an **extension**. The core platform defines SPIs and contracts; concrete implementations live in `xts-*` modules. This keeps the core small and allows independent evolution of each extension.

### 2. Separation of Grammar / Compiler / Execution
These three concerns are cleanly separated into distinct modules and phases. Grammar modules know nothing about execution. Compiler modules know nothing about SQL generation. This separation enables:
- Independent testing of each phase
- Swapping parsers or compilers without affecting execution
- Clear ownership boundaries for large teams

### 3. Protocol as the Interchange Format
The **protocol model** (`legend-engine-protocol-pure`) serves as the universal interchange format. Pure source text is parsed into protocol. SDLC stores protocol. The compiler reads protocol. APIs accept and return protocol. This means any tool that can produce or consume protocol JSON can integrate with Legend Engine.

### 4. Pure / Java Duality
Many concepts exist in both Pure and Java:
- **Pure metamodel** — the type-theoretic model (classes, properties, functions) living in the Pure graph
- **Protocol model** — Java POJOs for JSON serialization
- **Runtime implementations** — Java code that actually executes (SQL generation, connection management)

The compiler bridges from protocol (Java) to Pure graph (M3). Plan generation bridges from Pure graph back to Java execution code.

### 5. Protocol Versioning
Protocol models are versioned (e.g., `v1`) to support backward compatibility. When the metamodel evolves, new protocol versions can be introduced without breaking existing clients. The API layer accepts a `clientVersion` parameter to negotiate which protocol to use.

### 6. Store-Agnostic Execution Plans
Execution plans are **abstract** — they describe *what* to compute, not how. Store-specific plan nodes (e.g., `SQLExecutionNode`, `InMemoryExecutionNode`) handle the *how*. This allows the plan generator to compose operations across multiple stores in a single plan.

## Data Model Concepts

To understand how Legend Engine works, you need to understand the key modeling concepts:

| Concept | Purpose | Example |
|---------|---------|---------|
| **Class** | A data model type with properties | `Person { name: String, age: Integer }` |
| **Mapping** | Rules for transforming between models or from store to model | `Mapping PersonMapping { Person: Relational { ... } }` |
| **Runtime** | Configuration specifying which stores and connections to use | `Runtime { mappings: [...], connections: [...] }` |
| **Store** | Physical data storage definition | `Database MyDB { Table PERSON (...) }` |
| **Connection** | How to connect to a store | `RelationalDatabaseConnection { type: Postgres, ... }` |
| **Function** | A Pure function expressing business logic | `|Person.all()->filter(p | $p.age > 18)` |
| **Service** | A packaged, tested, deployable function | `Service GetAdults { ... }` |
| **Binding** | Links an external format schema to a model | `Binding PersonBinding { ... }` |

## Execution Example: End-to-End

Here's what happens when a user executes `|Person.all()->filter(p | $p.age > 18)` with a relational mapping:

```mermaid
sequenceDiagram
    participant Client
    participant API as HTTP API
    participant Grammar as Grammar Parser
    participant Compiler as Compiler
    participant PlanGen as Plan Generator
    participant PlanExec as Plan Executor
    participant DB as PostgreSQL

    Client->>API: POST /execute (Pure function + mapping + runtime)
    API->>Grammar: Parse Pure function text
    Grammar->>API: Protocol objects
    API->>Compiler: Compile protocol + mapping + runtime
    Compiler->>API: Pure graph with compiled function
    API->>PlanGen: Generate plan for function + mapping
    PlanGen->>API: ExecutionPlan with SQLExecutionNode
    Note over PlanGen: SQL: SELECT * FROM PERSON WHERE AGE > 18
    API->>PlanExec: Execute plan
    PlanExec->>DB: Execute SQL via JDBC
    DB->>PlanExec: ResultSet
    PlanExec->>API: Serialized result (TDS / JSON)
    API->>Client: Response
```

## Next

→ [03 — Pure Language Pipeline](03-pure-language-pipeline.md)
