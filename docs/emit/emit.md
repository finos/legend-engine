# Engine Model Integration Test (EMIT) — A Legend Engine Test Harness

## 1. Motivation

Today, the canonical way to validate a Legend model end-to-end — parsing, compiling, running
generations, executing tests, and generating execution plans — is through a
**legend-sdlc project build**. This requires a full Maven project structure, SDLC Maven plugins
(`legend-sdlc-generation-model-maven-plugin`, `legend-sdlc-generation-file-maven-plugin`,
`legend-sdlc-test-maven-plugin`, `legend-sdlc-generation-service-execution-maven-plugin`), and
the associated project configuration (GAV coordinates, dependency management, etc.).

There is a need for a **lightweight alternative** that lives entirely inside `legend-engine`.
Given a set of `.pure` files (written in Legend grammar — i.e., the grammar used in Legend Studio,
not the M3/Pure grammar), the harness should:

1. **Parse** the `.pure` files into `PureModelContextData`.
2. **Compile** the `PureModelContextData` into a `PureModel`.
3. **Run model generations** (via `ModelGenerationExtension` SPI).
4. **Run artifact/file generations** (via `GenerationExtension` and `ArtifactGenerationExtension` SPIs).
5. **Run all tests** defined in the model (via the `Testable` infrastructure / `TestableRunner`).
6. **Generate execution plans** for any `Service` elements in the model (via `ServicePlanGenerator`).

Each phase must succeed for the overall test to pass, and failures in any phase should produce
clear, actionable diagnostics.

### Secondary Goal: A Searchable Example Catalog

Beyond validation, EMIT tests should serve as a **living repository of examples** showing how
different Legend features can be used — both individually and in combination. Over time, this
collection should become the canonical reference for "how do I do X with Y?" questions, covering
simple patterns (a class with a constraint) through complex compositions (a service with a
relational mapping, connection, and test suite using shared test data).

To support this, the EMIT framework lays the metadata foundations from the
start:
- A **structured directory layout** with machine-readable metadata per model
  (`*.emit.yaml`).
- A **tagging taxonomy** for features, stores, and complexity levels (§6.2).
- A descriptor model (`EMITModelDescriptor`) and classpath discovery
  (`EMITModelDiscovery`) that an indexer can build on.

A queryable **catalog index** and user-friendly search/browsing tools are
future work, but the metadata foundations make them straightforward to add
later.

---

## 2. Comparison with Existing Test Harnesses

### 2.1 PCT (Pure Compatibility Testing)

| Aspect | PCT | EMIT |
|---|---|---|
| **What is tested** | Individual Pure platform functions (e.g., `between`, `timeBucket`) | An entire Legend model, end-to-end |
| **Input** | Pure function definitions, already part of the compiled core | `.pure` files in Legend grammar (user-authored models) |
| **Scope** | Functional correctness of a single function across target runtimes (databases) | Full build pipeline: parse → compile → generate → test → plan. |
| **Target runtimes** | Executes against multiple database adapters (DuckDB, Snowflake, etc.) | Engine-only; no external database targets required (though model tests may use them) |
| **Test discovery** | Tests tagged with `@PCT` stereotypes in Pure code | Tests discovered from `Testable` elements (services, mappings, functions) in the input model |
| **JUnit integration** | Custom `TestSuite` builders (`PureTestBuilderCompiled`) | JUnit 5 integration via a `@TestFactory` yielding granular dynamic tasks |
| **Location** | `legend-engine-core/legend-engine-core-pure` and per-database PCT modules | New modules under `legend-engine-core/legend-engine-core-emit/` (`legend-engine-emit` + `legend-engine-emit-junit`) |

### 2.2 MFT (Mapping Feature Testing)

| Aspect | MFT | EMIT |
|---|---|---|
| **What is tested** | Mapping features (e.g., union, filter, groupBy) against store targets | An entire Legend model build pipeline |
| **Input** | Pure functions annotated with `@MFT{testCollection}` stereotypes | `.pure` files in Legend grammar |
| **Scope** | Validates that specific model constructs work correctly against a given store runtime | Validates the full lifecycle: parsing, compilation, generation, testing, plan generation |
| **Evaluator/Adaptor** | Uses evaluator and adaptor functions (e.g., relational adaptor, M2M evaluator) | N/A — runs against the engine directly; store-specific behavior is tested via model-embedded tests |
| **Test structure** | Test collections organized by Pure package hierarchy | One test per model (or per `.pure` file set), with sub-results per phase |
| **Compiled mode only** | Yes (uses `CompiledExecutionSupport`) | Uses the standard `PureModel` compilation path (protocol → PureModel), same as Studio/SDLC |

### 2.3 Legend SDLC Project Build

| Aspect | SDLC Build | EMIT |
|---|---|---|
| **What is tested** | Effectively the same pipeline: compile, generate, test, plan | Same pipeline |
| **Input** | A full Maven project with entity JSON files, `pom.xml`, and SDLC project structure | One or more `.pure` files — no project infrastructure required |
| **Execution mechanism** | Maven plugins (`legend-sdlc-generation-*-maven-plugin`) | Direct Java API calls in `legend-engine` |
| **Dependency** | Requires `legend-sdlc` | No `legend-sdlc` dependency; self-contained in `legend-engine` |
| **Use case** | CI/CD validation of production projects | Rapid testing of model snippets, regression testing, engine development |
| **Where it runs** | Maven build (`mvn install`) | JUnit test execution |

### Summary

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        Legend Test Harnesses                             │
├────────────┬─────────────────────────────────────────────────────────────┤
│    PCT     │ Tests individual Pure platform functions across target      │
│            │ runtimes (databases). Focus: function-level correctness.    │
├────────────┼─────────────────────────────────────────────────────────────┤
│    MFT     │ Tests mapping features (M2M, relational, etc.) against      │
│            │ store adaptors. Focus: store-target feature correctness.    │
├────────────┼─────────────────────────────────────────────────────────────┤
│  EMIT (new) │ Tests an entire model through the full build pipeline      │
│            │ (parse → compile → generate → test → plan).                 │
│            │ Focus: model-level end-to-end correctness.                  │
├────────────┼─────────────────────────────────────────────────────────────┤
│ SDLC Build │ Same pipeline as EMIT, but via Maven plugins and full       │
│            │ project structure. Focus: production project validation.    │
└────────────┴─────────────────────────────────────────────────────────────┘
```

---

## 3. Architecture

### 3.1 High-Level Pipeline

```
                    ┌─────────────────────────────────────┐
                    │       *.emit.yaml (input)           │
                    │   (descriptor + .pure source set)   │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │  Phase 0: INITIALIZATION            │
                    │  Load descriptor, resolve files,    │
                    │  validate clashes, segment scope    │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │  Phase 1: PARSE                     │
                    │  PureGrammarParser.parseModel()     │
                    │  → PureModelContextData             │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │  Phase 2: COMPILE                   │
                    │  PureModel(pmcd, ...)               │
                    │  → PureModel                        │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │  Phase 3: MODEL GENERATION          │
                    │  ModelGenerationExtension SPI       │
                    │  (if GenerationSpecification exists)│
                    │  → PureModelContextData (generated) │
                    │  Re-compile with generated model    │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │  Phase 4: FILE GENERATION           │
                    │  a) GenerationExtension SPI         │
                    │     (FileGenerationSpecification)   │
                    │  b) ArtifactGenerationExtension SPI │
                    │     (per-element artifact gen)      │
                    │  → generated files                  │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │  Phase 5: TEST EXECUTION            │
                    │  - TestableRunner.doTests()         │
                    │  - Legacy MappingTestRunner         │
                    │  - Legacy ServiceTestRunner         │
                    │  → Test results                     │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │  Phase 6: PLAN GENERATION           │
                    │  For each Service:                  │
                    │  ServicePlanGenerator               │
                    │    .generateServiceExecutionPlan()  │
                    │  → ExecutionPlan                    │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │         RESULT / REPORT             │
                    │  Per-phase pass/fail + diagnostics  │
                    └─────────────────────────────────────┘
```

### 3.2 Module Layout

#### Framework Modules

The EMIT framework is split across two modules under a parent aggregator:

```
legend-engine-core/
  legend-engine-core-emit/                ← Parent aggregator (pom-only)
    pom.xml
    legend-engine-emit/                   ← Core runner module
      pom.xml
      src/
        main/java/.../emit/
          EMITRunner.java                 ← Pipeline orchestrator (calls EMITTasks)
          EMITTasks.java                  ← Stateless per-item engine for each phase
          EMITModel.java                  ← Compiled model + primary-scope filter
          EMITResult.java                 ← Aggregated pipeline result
          EMITPhase.java                  ← Phase enum
          EMITPhaseResult.java            ← Per-phase result
          EMITModelLoader.java            ← .pure file loading utility
          EMITSourceFile.java             ← Resolved source file
          EMITSourceSet.java              ← Collection of source files
          catalog/
            EMITModelDescriptor.java      ← Parsed emit.yaml metadata
          error/
            EMITException.java            ← Wrapped infrastructure failure
            EMITAssertionError.java       ← Test/assertion failure
            EMITErrorTools.java           ← Diagnostic helpers
        test/java/.../emit/
          TestEMITRunner.java             ← Self-test: drives the framework itself
        test/resources/
          emit-models/                    ← Bootstrap models for self-testing
            basic/
              class-simple.emit.yaml      ← Trivial parse + compile fixture
              compile-failure.emit.yaml   ← Negative case: compile error
              m2m-passing.emit.yaml       ← M2M mapping with a passing test
              m2m-mixed.emit.yaml         ← M2M mapping with a passing and a failing test
              m2m-with-dep.emit.yaml      ← Pulls in m2m-dep-source as a dependency
    legend-engine-emit-junit/             ← JUnit 5 binding module
      pom.xml
      src/
        main/java/.../emit/junit/
          EMITTestSuiteBuilder.java       ← @TestFactory task builder
          EMITModelDiscovery.java         ← Classpath .emit.yaml scanner
```

- **`legend-engine-emit`** contains the core pipeline (runner, result models, model loader, catalog
  infrastructure) and has no JUnit dependency. It can be used standalone in scripts or CI pipelines.
- **`legend-engine-emit-junit`** depends on `legend-engine-emit` and adds the JUnit 5 integration
  layer (`EMITTestSuiteBuilder`, `EMITModelDiscovery`). Test modules that want `@TestFactory`-based
  EMIT execution depend on this module.

The parent aggregator sits directly under `legend-engine-core/` (as a sibling of
`legend-engine-core-testable`, `legend-engine-core-pure`, etc.) because EMIT spans the full engine
pipeline — parsing, compilation, generation, test execution, and plan generation — rather than
being scoped to the `Testable` metamodel concept alone.

#### Distributed Test Locations

Actual EMIT tests are **spread throughout the codebase**, living in the modules that own the
features being tested. Each module that contributes EMIT tests adds a test-scoped dependency
on `legend-engine-emit-junit` and places its models under `src/test/resources/emit-models/`:

```
legend-engine-xts-relationalStore/
  legend-engine-xt-relationalStore-emit/    ← Module owning the relational EMIT examples
    src/test/resources/
      emit-models/
        relational-simple.emit.yaml         ← Test descriptor configures sources
        relational-simple/                  ← Model sources (paths resolved relative to YAML)
          store/db.pure
          mapping/personMapping.pure
          data/testData.pure
        relational-joins.emit.yaml          ← Sibling tests at the same level
        relational-joins/
          ...
        relational-shared-domain.emit.yaml  ← Reusable shared model (types only)
        relational-shared-domain/
          model/person.pure
          model/firm.pure
          model/employment.pure
```

(Tests reuse `relational-shared-domain` via the descriptor `dependencies`
mechanism; see §4.1.)

The same pattern applies to other extension modules (`legend-engine-xts-service`, `legend-engine-xts-generation`, etc.) — each owns the EMIT models for its feature area, with one `*.emit.yaml` per test and a sibling source-root directory.

The `EMITRunner` automatically discovers all `*.emit.yaml` files. The YAML file explicitly configures which directories and files comprise the test.

This distribution model has several advantages:
- **Ownership**: Tests live near the code they exercise, so feature owners maintain them.
- **Dependencies**: Each test module has the right classpath for its feature (e.g., a relational
  EMIT test module naturally has relational store dependencies on its classpath).
- **Parallelism**: Tests run as part of each module's build, not as a single bottleneck module.
- **Catalog aggregation**: The `EMITCatalogBuilder` can scan across multiple classpath roots
  to assemble a unified catalog from all distributed models.

### 3.3 Core API

```java
public class EMITRunner
{
  public EMITRunner();
  public EMITRunner(EMITModelLoader loader);

  /**
   * Run the full build pipeline against an already-loaded descriptor.
   * The descriptor carries the resolved primary model files and the
   * dependency files (already segmented by scope).
   */
  public EMITResult run(EMITModelDescriptor descriptor);

  /**
   * Convenience: load the descriptor at the given *.emit.yaml path
   * (resolving its model and dependencies) and run the full pipeline.
   */
  public EMITResult runFromYaml(Path emitYaml);
}
```

```java
public class EMITResult
{
    public List<EMITPhaseResult> getPhaseResults();
    public EMITPhaseResult       getPhase(EMITPhase phase);
    public boolean               isSuccess();      // true iff no phase is FAILURE
    public String                getSummary();     // human-readable per-phase report
    public void                  add(EMITPhaseResult result);
}
```

```java
public enum EMITPhase
{
    INITIALIZATION,    // descriptor load, file resolution, clash validation
    PARSE,
    COMPILE,
    MODEL_GENERATION,
    FILE_GENERATION,
    TEST_EXECUTION,
    PLAN_GENERATION
}
```

```java
public class EMITPhaseResult
{
    public enum Status { SUCCESS, FAILURE, SKIPPED, NOT_RUN }

    public EMITPhase  getPhase();
    public Status     getStatus();
    public long       getDurationMs();
    public String     getMessage();      // human-readable summary or skip/failure reason
    public Throwable  getThrowable();    // populated on FAILURE; may be null
    public List<?>    getOutputs();      // phase-specific outputs: PureModelContextData,
                                         // PureModel, FileGenerationOutput, RunTestsResult,
                                         // ExecutionPlan map, etc.

    public boolean    isSuccess();       // true for SUCCESS or SKIPPED
    public boolean    isFailure();       // true only for FAILURE

    public static EMITPhaseResult success(...);
    public static EMITPhaseResult failure(...);
    public static EMITPhaseResult skipped(EMITPhase phase, String reason);
    public static EMITPhaseResult notRun(EMITPhase phase, String reason);
}
```

---

## 4. Phase Details

### 4.1 Phase 0: Initialization

- Parse the `*.emit.yaml` file to read the explicit source configuration, which includes `model` (the primary model files) and optionally `dependencies`.
- `model`: Specified using a `root` directory and a list of `files`, which are resolved relative to the root.
- `dependencies`: Each dependency can be specified in one of two ways:
    - An `*.emit.yaml` file path (using `source`) with an optional `excludes` list supporting `*` and `**` wildcards.
    - A `root` directory and a list of `files`, exactly like the `model` specification.
- **Scope Segmentation**: Maintain a distinction between files loaded from the primary `model` and those loaded via `dependencies`. Dependencies are not in scope for generations, tests, etc.
- **Clash Validation**: Assert that no two files resolve to the same virtual path.
- **Success criteria**: Descriptor is well-formed and all source files resolve uniquely.
- **Failure mode**: An `EMITPhaseResult` for `INITIALIZATION` is produced with the error; subsequent phases are skipped.

### 4.2 Phase 1: Parse

- Read the content of each discovered file (from both the primary model and dependencies).
- Call `PureGrammarParser.newInstance().parseModel(content)` for each file.
- Combine the resulting `PureModelContextData` objects using the builder's `addPureModelContextData()`.
- **Success criteria**: No grammar parse exceptions.
- **Failure mode**: `EngineException` with source location information.

### 4.3 Phase 2: Compile

- Construct a `PureModel` from the merged `PureModelContextData` via
  `new PureModel(pmcd, null, DeploymentMode.PROD)`.
- **Success criteria**: No compilation errors.
- **Failure mode**: `EngineException` or `CompilationException` with details.

### 4.4 Phase 3: Model Generation

- Discover `GenerationSpecification` elements in the `PureModelContextData`.
- If present, use the `ModelGenerationExtension` SPI (loaded via `ServiceLoader`) to produce
  additional `PureModelContextData`.
- Merge the generated data with the original data and re-compile to produce an enriched
  `PureModel`.
- This is functionally equivalent to what the SDLC model generation Maven plugin does.
- **Success criteria**: Generation completes without exceptions; re-compilation succeeds.
- **Skipped if**: No `GenerationSpecification` elements exist in the model.

### 4.5 Phase 4: File Generation

This phase covers both types of file generation:

#### 4.5a Specification-Driven File Generations

- Discover the `GenerationSpecification` element and iterate over its `fileGenerations` list.
- For each `FileGenerationSpecification` referenced, find the corresponding `GenerationExtension` SPI (loaded via `ServiceLoader`) matching the specification's type.
- Execute the generation extension to produce a list of `GenerationOutput` (e.g., Avro schemas, JSON Schemas, Protobuf definitions).
- **Skipped if**: No `GenerationSpecification` exists, or its `fileGenerations` list is empty.

#### 4.5b Element-Driven Artifact Generations

- Load all registered `ArtifactGenerationExtension` SPIs via `ServiceLoader`.
- For each packageable element, iterate over extensions and call `canGenerate(element)` to check applicability.
- For applicable elements, call `extension.generate(element, pureModel, pmcd, clientVersion)` to produce a list of `Artifact` objects.
- **Skipped if**: No elements are candidates for any registered artifact generation extension.

**Success criteria**: Both sub-phases complete without exceptions.

### 4.6 Phase 5: Test Execution

- **Run Testable tests**: Find all `Testable` elements in the compiled `PureModel`. A Testable's tests may be grouped into **test suites** (services, mappings, functions) or be **top-level atomic tests** with no enclosing suite (e.g., `Persistence`); both shapes are discovered and executed.
- **Run Legacy Mapping tests**: Find `Mapping` elements with legacy `MappingTest` / `MappingTestSuite` elements.
- **Run Legacy Service tests**: Find `Service` elements with legacy `ServiceTest` elements.
- **Dependency Exclusion**: Only execute tests for elements defined in the primary `model`. Any test defined in an element loaded via `dependencies` MUST be ignored.
- Use `TestableRunner.doTests(...)`, the legacy `MappingTestRunner`, and the legacy `ServiceTestRunner` to execute the in-scope tests, producing their respective result objects.
- **Success criteria**: All in-scope tests (Testable and legacy) pass.
- **Failure mode**: Failed/error `TestResult`, `RichMappingTestResult`, or `RichServiceTestResult` entries.
- **Skipped if**: No test elements (Testable or legacy) exist in the primary model.

### 4.7 Phase 6: Plan Generation

- Find all `Service` elements in the `PureModelContextData`.
- For each service, call
  `ServicePlanGenerator.generateServiceExecutionPlan(service, context, pureModel, ...)`.
- This handles both `PureSingleExecution` (producing a `SingleExecutionPlan`) and
  `PureMultiExecution` (producing a `CompositeExecutionPlan`).
- Collect and report the generated `ExecutionPlan` objects.
- **Success criteria**: Plan generation completes without exceptions for all services.
- **Skipped if**: No `Service` elements exist.

---

## 5. Execution and Reporting

EMIT models can be executed both programmatically (standalone) and through JUnit.

### 5.1 Standalone Execution

The `EMITRunner` can be invoked directly to execute the full pipeline for a given model descriptor:

```java
EMITRunner runner = new EMITRunner();
EMITResult result = runner.run(descriptor);

if (!result.isSuccess())
{
    System.err.println(result.getSummary());
}
```

This is useful for scripting, CI pipelines, or any context where JUnit is not the execution harness.

### 5.2 JUnit Integration

The JUnit 5 integration lives in the `legend-engine-emit-junit` module, separate from the core runner. To achieve granular pass/fail reporting without forcing developers to write individual tests by hand, EMIT uses JUnit 5's `@TestFactory` mechanism. The framework provides a builder that discovers models and flattens every operation on them — including parsing and compilation themselves — into discrete `DynamicTest` tasks.

To test EMIT models in a module, create a single test class:

```java
public class MyModuleEMITTestSuite
{
    @TestFactory
    Stream<DynamicTest> emit()
    {
        // Discovers models and returns a flattened stream of granular dynamic
        // tests covering initialization (parse, compile, model generation),
        // generation, testing, and plan creation.
        return EMITTestSuiteBuilder.taskStream("emit-models/");
    }
}
```

`EMITTestSuiteBuilder` also exposes `taskList(String)` for callers that prefer
a `List<DynamicTest>` (e.g., for fan-out into nested containers).

When JUnit invokes the factory method, `EMITTestSuiteBuilder` scans for `*.emit.yaml` files and loads each model descriptor (only this load is eager — the model's container is named after the descriptor). Phase 1 (Parse), Phase 2 (Compile), and Phase 3 (Model Generation) each run *inside* their own DynamicTest: JUnit consumes dynamic-test streams lazily, executing each task before pulling the next, so the duration JUnit reports for the Parsing or Compilation task is the real cost of that phase. If a phase fails, its task fails and no subsequent tasks are emitted for that model. Downstream task discovery is deferred until after the initialization tasks have run, and operates on the model-generation-enriched PMCD, so any element produced by a `GenerationSpecification` is eligible for downstream tasks. By inspecting that enriched model the builder identifies every file-generation specification, every artifact-generation candidate, every test (modern Testable plus legacy Mapping/Service tests), and every service.

It then yields one `DynamicTest` per granular operation, with descriptive names. Each test suite's atomic-test tasks are framed by an `Open Test Suite` task (the suite's setup cost: plan generation, runtime build) and a `Close Test Suite` task (releasing the session's runtime/connection resources), so the time spent — and any failure — in suite setup and teardown is reported in its own right rather than being folded into the first test or lost entirely:

- `[service-simple] Load Model Descriptor`
- `[service-simple] Initialization: Parsing`
- `[service-simple] Initialization: Compilation`
- `[service-simple] Initialization: Model Generation`
- `[service-simple] File Generation: demo::MyAvroGenerationSpec`
- `[service-simple] Artifact Generation: demo::PersonService (ServiceArtifactExtension)`
- `[service-simple] Test: demo::PersonService / testSuite_1 / Open Test Suite`
- `[service-simple] Test: demo::PersonService / testSuite_1 / test_1`
- `[service-simple] Test: demo::PersonService / testSuite_1 / test_2`
- `[service-simple] Test: demo::PersonService / testSuite_1 / Close Test Suite`
- `[service-simple] Plan: demo::PersonService`
- `[service-simple] Legacy Mapping Test: demo::PersonMapping / legacyTest_1`
- `[service-simple] Legacy Service Test: demo::PersonService`

For a Testable whose tests are **top-level atomic tests** (no enclosing suite — e.g., a `Persistence`), the test task carries no suite segment between the testable and the atomic-test id:

- `[persistence-snapshot] Test: demo::MyPersistence / test_1`

Because each `DynamicTest` is a distinct JUnit test, IDEs and build servers (like Maven Surefire) report granular pass/fail statuses, durations, and diffs natively.

To optimize performance, each phase's result is memoized and shared among all tasks belonging to the same model: the `PureModel` produced inside the Compilation task is reused by every downstream task, and a suite's session is opened once and shared across its atomic tests. The memoization also works on demand — executing a single task out of context (e.g. re-running one atomic test from an IDE) computes whatever earlier phases it needs itself, and tasks for already-computed phases simply replay the memoized outcome.

### 5.3 Result Model and Reporting

Each phase captures:
- **Success/failure status**
- **Duration** (milliseconds)
- **Exception** with full stack trace (on failure)
- **Error message** with source location (for parse/compile failures)
- **Phase-specific artifacts** (on success)

The `EMITResult.getSummary()` method produces a human-readable report. Status
markers are `OK` (success), `FAIL` (failure), `SKIP` (phase intentionally
skipped because there was nothing to do), and `--` (phase not run because an
earlier phase failed):

```
EMIT Result: FAILED
  OK   INITIALIZATION   (8ms) - 4 model files, 1 dependency file
  OK   PARSE            (42ms) - 5 files, 12 elements
  OK   COMPILE          (318ms) - PureModel built successfully
  SKIP MODEL_GENERATION (0ms) - no GenerationSpecification
  OK   FILE_GENERATION  (87ms) - 3 file generations, 4 artifact extensions
  FAIL TEST_EXECUTION   (203ms) - 3 tests (testable: 3, legacy mapping: 0, legacy service: 0); 1 failed
        EMITAssertionError: Test did not pass; status=FAIL; assertions=[...]
  --   PLAN_GENERATION  (0ms) - skipped due to failure in TEST_EXECUTION
```

---

## 6. Example Catalog Design

The EMIT model collection doubles as a **searchable catalog of feature examples**. This section
defines the metadata conventions and tooling foundations that make this possible.

### 6.1 Model Metadata (`*.emit.yaml`)

Every EMIT test is defined by a `*.emit.yaml` file with structured metadata and source configurations:

```yaml
# service-relational-with-generation.emit.yaml
name: service-relational-with-generation
title: "Relational Service with File Generation"
description: |
  Demonstrates a service backed by a relational mapping with a
  Relational-to-H2 connection, including test data, test suites,
  and an Avro file generation specification.

# Explicit source configuration. The primary model is specified with a root
# directory and an explicit list of files relative to that root.
# Dependencies can be specified either as another emit.yaml file with exclusions
# or as a root and list of files similar to the primary model.
modelSources:
  model:
    root: emit-models/complex/service-relational-with-generation
    files:
      - model/types.pure
      - store/db.pure
      - mapping/mapping.pure
      - service/myService.pure
  dependencies:
    - root: emit-models/shared-types
      files:
        - model/common.pure
    - source: emit-models/core-api.emit.yaml
      excludes:
        - emit-models/core-api/**/*_experimental.pure

# Features exercised by this model — domain:capability pairs.
# Uses a controlled taxonomy (see §6.2).
features:
  - execution:file-generation
  - execution:service
  - execution:service-test
  - grammar:association
  - scaffolding:class
  - scaffolding:relational-connection
  - scaffolding:relational-mapping
  - scaffolding:relational-store

# Store types involved.
stores:
  - relational

# Complexity level: basic, intermediate, advanced
complexity: advanced

# Optional: free-form tags for additional discoverability.
tags:
  - avro
  - h2
  - test-data
```

### 6.2 Feature Taxonomy

Each value in the `features` list uses the format **`domain:capability`**, where
*domain* identifies the pipeline stage or concern area and *capability* names the
specific code path being exercised. This makes the exact coverage of every test
immediately visible — you can tell at a glance which domains interact and which
capabilities are novel.

The taxonomy is extensible; add new entries as the catalog grows (and document them
here in the same PR). Features are sorted alphabetically by domain, then capability.

#### Scaffolding (always present — excluded from complexity scoring)

These features appear in virtually every EMIT test and represent baseline
infrastructure rather than the feature under test:

| Feature | Description |
|---|---|
| `scaffolding:class` | At least one Pure class definition |
| `scaffolding:relational-store` | A relational store definition (typically H2) |
| `scaffolding:relational-connection` | A relational database connection |
| `scaffolding:relational-mapping` | Base relational class mapping (no special features) |
| `scaffolding:m2m-mapping` | Base model-to-model mapping |
| `scaffolding:runtime` | A runtime definition |
| `scaffolding:model-connection` | A model connection (M2M tests) |

#### Grammar — Pure language constructs

| Feature | Description |
|---|---|
| `grammar:association` | Association (1:1, 1:N, N:M) |
| `grammar:class-inheritance` | Class inheritance hierarchy |
| `grammar:constraint` | Class-level constraint |
| `grammar:derived-property` | Derived (computed) property |
| `grammar:enumeration` | Enumeration type definition |
| `grammar:function` | Standalone Pure function |
| `grammar:measure` | Measure / unit definition |
| `grammar:nested-association` | Multi-level association traversal |
| `grammar:profile` | Profile / stereotype / tag definition |
| `grammar:qualified-property` | Qualified (parameterised) property |

#### Mapping — transform and mapping-level features

| Feature | Description |
|---|---|
| `mapping:aggregation-aware-mapping` | Aggregation-aware mapping |
| `mapping:cross-store` | Cross-store mapping (M2M ↔ relational) |
| `mapping:enumeration-mapping` | Enumeration value mapping / transform |
| `mapping:m2m-derived-source-property` | Derived property on M2M source class |
| `mapping:m2m-local-property` | Local property in M2M mapping |
| `mapping:m2m-transform` | Model-to-model transform expression |
| `mapping:mapping` | Generic mapping (legacy tag) |
| `mapping:mapping-include` | Mapping include composition |
| `mapping:operation-mapping` | Operation mapping (inheritance dispatch) |
| `mapping:operation-mapping-merge` | Merge operation mapping |
| `mapping:operation-mapping-merge-validation` | Merge with cross-set validation |
| `mapping:relational-association-implementation` | Association implementation in relational mapping |
| `mapping:relational-distinct` | DISTINCT projection |
| `mapping:relational-embedded` | Embedded relational mapping |
| `mapping:relational-group-by` | GROUP BY projection |
| `mapping:relational-inline-embedded` | Inline embedded mapping |
| `mapping:relational-joined-table-inheritance` | Joined-table inheritance strategy |
| `mapping:relational-literal` | Literal value in mapping |
| `mapping:relational-literal-list` | Literal list in mapping |
| `mapping:relational-main-table-alias` | Main table alias override |
| `mapping:relational-otherwise-embedded` | Otherwise-embedded mapping |
| `mapping:relational-polymorphic-query` | Polymorphic query dispatch |
| `mapping:relational-primary-key` | Explicit primary key specification |
| `mapping:relational-single-table-inheritance` | Single-table inheritance strategy |
| `mapping:relational-table-alias-column` | Table-alias column reference |
| `mapping:router-union` | Router union mapping |
| `mapping:store-union` | Store union mapping |

#### Store — relational store-level features

| Feature | Description |
|---|---|
| `store:relational-cross-schema` | Cross-schema table reference |
| `store:relational-cross-table-filter` | Cross-table filter |
| `store:relational-dyna-function` | Dynamic function in store definition |
| `store:relational-filter` | Table-level filter |
| `store:relational-inline-view` | Inline view (subquery) |
| `store:relational-inner-join` | Inner join |
| `store:relational-left-outer-join` | Left outer join |
| `store:relational-multi-table` | Multi-table store definition |
| `store:relational-nested-join` | Nested (chained) join |
| `store:relational-outer-join` | Full outer join |
| `store:relational-right-outer-join` | Right outer join |
| `store:service-store` | Service store |
| `store:flat-data-store` | Flat-data store |

#### Milestoning — temporal features

| Feature | Description |
|---|---|
| `milestoning:all-versions-in-range-query` | All-versions-in-range temporal query |
| `milestoning:all-versions-query` | All-versions temporal query |
| `milestoning:bi-temporal` | Bi-temporal milestoning (business + processing) |
| `milestoning:business-temporal` | Business-temporal milestoning |
| `milestoning:milestoning` | Generic milestoning marker |
| `milestoning:point-in-time-query` | Point-in-time temporal query |
| `milestoning:processing-temporal` | Processing-temporal milestoning |

#### Execution — service, generation, and test infrastructure

| Feature | Description |
|---|---|
| `execution:bigquery-function` | BigQuery function activator |
| `execution:binding` | External format binding |
| `execution:data-element` | Data element definition |
| `execution:external-format` | External format specification |
| `execution:external-format-binding` | External format binding |
| `execution:file-generation` | File generation specification |
| `execution:hosted-service` | Hosted service function activator |
| `execution:model-generation` | Model generation specification |
| `execution:multi-execution-service` | Multi-execution service |
| `execution:plan-generation` | Execution plan generation |
| `execution:post-validation` | Service post-validation |
| `execution:schema-set` | Schema set definition |
| `execution:service` | Service definition |
| `execution:service-test` | Service test suite |
| `execution:shared-test-data` | Shared test data element |
| `execution:snowflake-app` | Snowflake app function activator |
| `execution:test-data` | Test data definition |

#### Persistence — service-driven dataset ingestion

| Feature | Description |
|---|---|
| `persistence:append-only` | Append-only updates handling on a non-temporal target |
| `persistence:auditing` | Audit column (e.g. batch date-time) written to the target |
| `persistence:bitemporal` | Bitemporal target milestoning (processing + source-derived dimensions) |
| `persistence:delete-indicator` | Delta delete-indicator action (soft deletes) |
| `persistence:delta` | Delta dataset type |
| `persistence:graph-fetch-service-output` | Graph-fetch service output (vs. TDS) |
| `persistence:nontemporal` | Non-temporal target (no milestoning) |
| `persistence:notifier` | Persistence notifier (email / PagerDuty notifyees) |
| `persistence:persistence` | Persistence element (service-driven dataset ingestion) |
| `persistence:service-output-target` | Service-output target (`serviceOutputTargets` block) |
| `persistence:snapshot` | Snapshot dataset type |
| `persistence:unitemporal` | Unitemporal (processing-time) target milestoning |

#### Complexity

Complexity is determined by **domain-crossing depth** — how many distinct pipeline
domains are exercised by non-scaffolding features:

| Complexity | Criteria | EMIT value proposition |
|---|---|---|
| **basic** | 1–2 domains | Single capability or simple two-domain interaction through the full pipeline |
| **intermediate** | 3–4 domains | Capabilities from multiple domains interacting |
| **advanced** | 5+ domains | Broad multi-domain composition or cross-store scenarios |

#### Evolving the Taxonomy

When harvesting models from a new Studio project or adding a test for a new engine
feature, you may encounter capabilities that do not map to any existing entry. In
that case:

1. Add the new `domain:capability` entry to the appropriate table above.
2. Include the taxonomy update in the same PR as the new EMIT test.
3. If a new *domain* is needed (e.g., `data-quality`), add a new
   subsection and update the complexity-scoring guidance accordingly.

### 6.3 Catalog Index

Today the catalog metadata is captured by `EMITModelDescriptor` (under
`org.finos.legend.engine.test.emit.catalog`), which is parsed from each
`*.emit.yaml` and carries the descriptor fields plus the resolved primary /
dependency file paths.

The classpath scan that discovers descriptors is implemented in
`EMITModelDiscovery` (in `legend-engine-emit-junit`), but it is currently
oriented toward the JUnit `@TestFactory` flow rather than catalog query.

A dedicated catalog index — querying models by feature, store, complexity,
and free-text search over titles/descriptions/tags — is **not yet
implemented**. The descriptor and discovery primitives are designed so that
an in-memory index (and later a JSON serialisation for a search UI) can be
layered on top without disrupting the existing pipeline. See §9.

---

## 7. Sample `.pure` Model Input

A minimal example model for EMIT testing, with its accompanying `emit.yaml`:

**`service-simple.emit.yaml`**:
```yaml
name: service-simple
title: "Simple Service with M2M Mapping"
description: |
  A basic service using a model-to-model mapping. Demonstrates
  class definition, empty mapping, service definition with a
  query and test suite.

modelSources:
  model:
    root: emit-models/basic/service-simple
    files:
      - model/model.pure
  dependencies:
    - source: emit-models/basic/base-types.emit.yaml

features:
  - execution:service
  - execution:service-test
  - grammar:derived-property
  - mapping:mapping
  - scaffolding:class
complexity: basic
tags:
  - m2m
  - getting-started
```

**`model/model.pure`**:
```pure
###Pure
Class demo::Person
{
  firstName: String[1];
  lastName: String[1];
  fullName() { $this.firstName + ' ' + $this.lastName }: String[1];
}

###Mapping
Mapping demo::PersonMapping
(
)

###Service
Service demo::PersonService
{
  pattern: '/api/person';
  documentation: 'A simple person service';
  autoActivateUpdates: true;
  execution: Single
  {
    query: |demo::Person.all()->project([x|$x.firstName, x|$x.lastName], ['First Name', 'Last Name']);
    mapping: demo::PersonMapping;
  }
  testSuites:
  [
    testSuite_1:
    {
      tests:
      [
        test_1:
        {
          asserts:
          [
            assert_1:
              EqualToJson
              #{
                expected:
                  ExternalFormat
                  #{
                    contentType: 'application/json';
                    data: '[]';
                  }#;
              }#
          ]
        }
      ]
    }
  ]
}
```

---

## 8. Dependencies

Both EMIT modules depend only on existing `legend-engine` modules — there is no dependency on `legend-sdlc`.

The core runner module (`legend-engine-emit`) has dependencies that fall into a small set of categories, each contributing one stage of the pipeline:

- **Protocol & grammar** — `PureModelContextData`, version registry, and the `PureGrammarParser` that turns `.pure` source into protocol POJOs.
- **Compiler** — `PureModel` construction from a `PureModelContextData`.
- **Generation SPIs** — `ModelGenerationExtension`, `GenerationExtension`, and `ArtifactGenerationExtension`, plus their shared base modules.
- **Test runners** — `TestableRunner` for the modern `Testable` infrastructure, plus the legacy `MappingTestRunner` and `ServiceTestRunner` for backward compatibility.
- **Plan generation** — `ServicePlanGenerator` (for `Service` elements) and the underlying `PlanGenerator`.
- **Service DSL** — protocol/compiler artifacts for the `Service` element type, since `Service` is special-cased by Phase 6.

The JUnit binding module (`legend-engine-emit-junit`) depends on `legend-engine-emit` and on JUnit 5 (`junit-jupiter-api`).

Concrete artifact coordinates are kept in each module's `pom.xml`; listing them here would drift.

---

## 9. Future Extensions

- **Store implementation variation**: Run a model against alternative implementations of
  its stores (e.g., a relational model against DuckDB, Postgres, Snowflake) without
  duplicating the model. The available variations should be **discovered from the model
  and the classpath**, not declared in `emit.yaml` — keeping the test descriptor focused
  on what the model is, and letting an external tool decide which variations to actually
  run.
- **Catalog search tool**: A web UI or CLI tool for searching and browsing the catalog index
  (e.g., `emit search --feature relational-mapping --complexity basic`).
- **Auto-derived metadata**: Introspect `PureModelContextData` to supplement `emit.yaml` tags.
- **Catalog completeness CI check**: Validate that every model has `emit.yaml`, required fields
  are present, and feature tags come from the controlled vocabulary.
- **Model diff testing**: Compare EMIT results between two model versions.
- **Performance benchmarking**: Track phase durations over time.
- **Model coverage**: Track which model elements are exercised by tests.
- **Feature coverage matrix**: Auto-generate a matrix showing which features are covered by
  examples and which lack coverage, helping guide the creation of new examples.
