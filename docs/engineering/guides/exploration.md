# Codebase Exploration & Discovery Guide

This guide helps new engineers systematically understand the `legend-engine` codebase.
Follow the phases in order; each phase builds on the previous one.

---

## Phase 1 — Orientation (Day 1)

### 1.1 Read the Architecture Docs First

Before touching code:

1. [Architecture Overview](../architecture/overview.md) — what the project does and its 5 core pipelines.
2. [Module Reference](../reference/modules.md) — every module's purpose.
3. [Technology Stack](../reference/tech-stack.md) — key libraries and why they were chosen.
4. [Domain & Key Concepts](../architecture/domain-concepts.md) — the Class/Mapping/Runtime/Store model and the glossary.

### 1.2 Build the Project

```bash
mvn install -DskipTests -T 4
```

Confirm all modules build cleanly. Any error here is a setup issue — see
[Troubleshooting](getting-started.md#10-common-troubleshooting).

### 1.3 Understand the Reactor Order

```bash
mvn install -DskipTests --no-transfer-progress 2>&1 | grep "Building legend-engine"
```

This prints every module in the order Maven builds it. The order reflects the dependency graph.
Compare it against the module groups in the [Module Reference](../reference/modules.md).

---

## Phase 2 — Dependency Mapping (Days 1–2)

### 2.1 Single-module dependency tree

```bash
# Intra-project dependencies for the compiler module
mvn dependency:tree \
  -pl legend-engine-core/legend-engine-core-base/legend-engine-core-language-pure/legend-engine-language-pure-compiler \
  -Dincludes=org.finos.legend.engine
```

### 2.2 Full module graph (DOT format)

```bash
mvn com.github.ferstl:depgraph-maven-plugin:graph \
  -DshowGroupIds=true -DgraphFormat=dot \
  -DincludeGroupIds=org.finos.legend.engine
# Render: dot -Tpng target/dependency-graph.dot -o deps.png
```

### 2.3 Identify unused / missing dependencies

```bash
mvn dependency:analyze -pl <module-path>
# Outputs: "Used undeclared" (should be declared) and "Unused declared" (may be safe to remove)
```

---

## Phase 3 — Entry Points (Days 2–3)

### 3.1 Trace the HTTP entry points

Find all JAX-RS `@Path` resource classes:

```bash
grep -r "@Path" --include="*.java" -l | grep -v target \
  | xargs grep -l "@GET\|@POST\|@PUT\|@DELETE" \
  | sort
```

Start with these key resources in the server:

- `GrammarToJson` / `JsonToGrammar` — grammar ↔ protocol conversion
- `Compile` — compilation endpoint
- `ExecutePlanStrategic` / `ExecutePlanLegacy` — execution
- `GeneratePlan` — plan generation

### 3.2 Trace a query end-to-end

The best way to understand the engine is to follow one request through all layers:

1. **Start at `Server.java`** — find where `ExecutePlanStrategic` is registered.
2. **Step into `PlanExecutor.execute(...)`** — see how the plan tree is walked.
3. **Find `ExecutionNodeExecutor`** — see how each node type dispatches to a store.
4. **Find `RelationalExecutor.executeRelational(...)`** — see SQL generation and JDBC execution.
5. **Set breakpoints in `PlanGenerator.generateExecutionPlan(...)`** — watch Pure router invocation.

### 3.3 Understand the Pure-to-Java boundary

The boundary between Java and Pure is `PlanGenerator`:

```java
// Java calls Pure here:
Root_meta_pure_executionPlan_ExecutionPlan purePlan =
    PlanGenerator.generateExecutionPlanAsPure(lambda, mapping, runtime, ctx, pureModel, ...);
```

And in `PureModel` compilation:

```java
// Java compiles protocol POJOs into Pure graph objects here:
PureModel pureModel = Compiler.compile(pureModelContextData, deploymentMode, identity);
```

---

## Phase 4 — Extension Points (Days 3–4)

### 4.1 Find all ServiceLoader SPI registrations

```bash
find . -path "*/META-INF/services/org.finos*" -not -path "*/target/*" | sort
```

Examine each file — it lists the implementation class for that SPI.

### 4.2 Find all CompilerExtension implementations

```bash
grep -r "implements CompilerExtension" --include="*.java" -l | grep -v target | sort
```

Each result is a module adding new element types to the compiler.

### 4.3 Find all StoreExecutorBuilder implementations

```bash
grep -r "implements StoreExecutorBuilder" --include="*.java" -l | grep -v target | sort
```

Each result is a module adding a new store runtime executor.

### 4.4 Understand the Pure Extension registry

```bash
# Every Pure file that defines or returns an Extension
grep -r "meta::pure::extension::Extension" --include="*.pure" -l \
  | grep -v target | sort
```

Look for functions named `\w+Extension()` — these are the factory functions that create
`Extension` instances for each `xts-*` module.

---

## Phase 5 — Pure Codebase (Days 4–5)

### 5.1 Navigate the compiled core Pure files

The ~570 `.pure` files in `legend-engine-pure-code-compiled-core` are the engine's
Pure standard library. Key directories:

```text
core/pure/router/          ← query routing logic
core/pure/executionPlan/   ← execution plan metamodel and generation
core/pure/graphFetch/      ← graph fetch
core/pure/milestoning/     ← bi-temporal support
core/pure/tds/             ← tabular data set functions
core/pure/binding/         ← external format binding
core/pure/extensions/      ← Extension class definition
core/store/m2m/            ← model-to-model mapping execution
core_service/service/      ← service metamodel (in legend-engine-xts-service)
```

### 5.2 Use the Pure IDE for interactive exploration

Start the Pure IDE (see [Getting Started](getting-started.md#4-running-the-pure-ide))
and use it to:

- Browse the Pure package tree
- Execute small Pure expressions (`F9`)
- Inspect the result of `meta::pure::router::routeFunction(...)`
- Print execution plans: `planToString($plan, true, [])`

### 5.3 Count Pure files per module

```bash
find . -name "*.pure" -not -path "*/target/*" \
  | sed 's|./||' | awk -F'/' '{print $1}' \
  | sort | uniq -c | sort -rn | head -20
```

---

## Phase 6 — Tests as Documentation (Days 5–7)

Tests are often the clearest explanation of expected behaviour.

### 6.1 Find tests for a specific feature

```bash
# Tests covering RelationalExecutor
grep -r "RelationalExecutor" --include="*Test*.java" -l | grep -v target
# Tests covering milestoning
grep -r "milestoning\|processingDate\|businessDate" --include="*Test*.java" -l | grep -v target
```

### 6.2 Read PCT test functions

PCT Pure test functions show exactly what a function is supposed to do:

```bash
find . -name "*.pure" -not -path "*/target/*" | xargs grep -l "<<PCT.test>>" | sort
```

Open one and read the test function — it is both specification and test.

### 6.3 Read integration test configs

The server integration tests in `legend-engine-server-integration-tests` exercise the full
HTTP stack. Read them to understand what the production API is expected to do.

---

## Phase 7 — Tribal Knowledge (Ongoing)

Use the [Interview Checklist](../maintenance/documentation-plan.md#6-interview-checklist-tribal-knowledge-capture)
when talking to existing team members. Record findings as ADRs in
[`decisions/`](../decisions/).

### Key questions to ask

- Which modules change most frequently — and why?
- Which areas have the most hidden invariants or implicit contracts?
- What is the intended evolution of `[specific subsystem]`?
- Which tests are known to be brittle?
- What would break silently if someone changed `[X]`?

---

## Reference: Useful One-Liners

```bash
# Count Java files per module group
find . -name "*.java" -not -path "*/target/*" \
  | sed 's|./||' | awk -F'/' '{print $1}' \
  | sort | uniq -c | sort -rn | head -20

# Count Pure files per module group
find . -name "*.pure" -not -path "*/target/*" \
  | sed 's|./||' | awk -F'/' '{print $1}' \
  | sort | uniq -c | sort -rn | head -20

# Find all @Deprecated usages (technical debt signals)
grep -r "@Deprecated\|@doc.deprecated\|<<doc.deprecated>>" \
  --include="*.java" --include="*.pure" -l | grep -v target | wc -l

# Find all TODOs
grep -rn "TODO\|FIXME\|HACK\|XXX" --include="*.java" --include="*.pure" \
  | grep -v target | wc -l

# Find the largest Java files (complexity signals)
find . -name "*.java" -not -path "*/target/*" \
  | xargs wc -l | sort -rn | head -20

# Find all Pure functions with more than 1 implementation (overloading)
grep -rn "^function " --include="*.pure" | grep -v target \
  | sed 's|.*function ||' | awk -F'(' '{print $1}' \
  | sort | uniq -d | head -20
```
