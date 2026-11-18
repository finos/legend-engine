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
          EMITModelDiscovery.java         ← Filesystem + classpath descriptor discovery
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
    legend-engine-emit-report/            ← HTML coverage renderer (framework-side)
      pom.xml
      src/
        main/java/.../emit/report/
          EMIT_to_HTML.java               ← buildHTML(descriptors, repoRoot) / writeHTML(...)
    legend-engine-emit-maven-plugin/      ← Build-time HTML coverage report generator
      pom.xml
      src/
        main/java/.../emit/maven/
          EMITCoverageReportGenerationMojo.java   ← @Mojo("generate-EMIT-coverage-report"), generate-resources phase
```

- **`legend-engine-emit`** contains the core pipeline (runner, result models,
  model loader, catalog infrastructure) plus `EMITModelDiscovery` — the
  descriptor-discovery API used by the JUnit binding
  (`@TestFactory` classpath enumeration) and by the maven plugin (walking
  each recorded `emit-models/` directory for `*.emit.yaml`). It has no
  JUnit dependency and can be used standalone in scripts or CI pipelines.
- **`legend-engine-emit-junit`** depends on `legend-engine-emit` and adds the
  JUnit 5 integration layer (`EMITTestSuiteBuilder`). Test modules that want
  `@TestFactory`-based EMIT execution depend on this module.
- **`legend-engine-emit-report`** houses `EMIT_to_HTML`, the pure HTML
  renderer for a set of `EMITModelDescriptor`s. It exposes two static
  entry points: `buildHTML(descriptors, repoRoot)` returns the HTML as a
  string; `writeHTML(descriptors, outputFile, repoRoot)` writes it to
  disk (creating parent directories as needed). The renderer has no
  awareness of Maven, of the filesystem walk, or of the classpath — those
  concerns live in the maven plugin and in `EMITModelDiscovery`
  respectively. Keeping the renderer isolated in its own module means
  the maven plugin can depend on it without dragging in JUnit, and the
  runner module (`legend-engine-emit`) can stay JUnit-free.
- **`legend-engine-emit-maven-plugin`** is the build-time tool the server
  pom binds into `generate-resources`. Its `generate-EMIT-coverage-report`
  goal locates the reactor root by walking up `MavenProject.getParent()`
  from the consuming project (stopping at the topmost ancestor that
  still has a `getBasedir()` on disk, so externally-resolved parent
  POMs are not crossed), then traverses that root with
  `Files.walkFileTree`. Every check happens uniformly in
  `preVisitDirectory` using `stream().anyMatch(...)`:
  - `excludedDirectoryNamePrefixes` — `String.startsWith` on the last
    name component (default `["."]`, prunes `.git`, `.idea`, `.mvn`, …).
  - `excludedDirectoryNames` — `String.equals` on the last name
    component (default `["target"]`).
  - `excludedRelativeSubpaths` — `rel.endsWith(fs.getPath(subpath))`
    (default `["src/main"]`). `Path.endsWith` is component-wise, so
    excluding `a/b/c` never accidentally prunes a sibling `a/b/cd`, and
    everything works cross-platform without manual separator
    concatenation.
  - `includedRelativeSubpaths` — same component-wise
    `rel.endsWith(fs.getPath(subpath))` check (default
    `["src/test/resources/emit-models"]`). Matches record the dir as an
    emit-models dir and return `SKIP_SUBTREE` — no need to descend,
    since the mojo then hands each recorded dir to
    `EMITModelDiscovery.findEmitYamls(dir)` inside its static
    `parseDescriptorsUnder` helper to enumerate the `*.emit.yaml`
    inside.

  The resulting descriptors are passed to
  `EMIT_to_HTML.writeHTML(descriptors, outputFilePath.toPath(), repoRoot)`.
  The default `outputFilePath` is
  `${project.build.outputDirectory}/emit/emit-coverage.html`
  (i.e. `target/classes/emit/emit-coverage.html`), which lands the
  rendered report under `target/classes/` — so Maven's jar packaging
  automatically bundles it into the consuming jar with no extra
  `<configuration>` needed. `legend-engine-server-http-server` pins
  `outputFilePath` explicitly to that same value (so the server jar's
  bundled resource path stays fixed even if the plugin default is ever
  changed) and extends `excludedDirectoryNames` to include
  `legend-engine-emit-junit`, so the binding module's bootstrap
  examples are not double-counted in the server-side report. All four
  discovery-config fields plus `outputFilePath` are `@Parameter`s, so
  any consuming pom can tune them via `<configuration>` without forking
  the plugin. See §5.4.

The parent aggregator sits directly under `legend-engine-core/` (as a sibling of
`legend-engine-core-testable`, `legend-engine-core-pure`, etc.) because EMIT spans the full engine
pipeline — parsing, compilation, generation, test execution, and plan generation — rather than
being scoped to the `Testable` metamodel concept alone.

**Everything inside `legend-engine-core-emit/` is framework.** The EMIT models under
`legend-engine-emit/src/test/resources/emit-models/` are self-test fixtures for the runner —
several drive fake test-only SPIs (`EmitDemo*Extension`) rather than real engine extensions, and
one (`compile-failure`) is expected to fail. They exist to exercise the runner, not to demonstrate
engine features, and should not be read as catalog examples. (Note that the §5.4 coverage report
does still render them: the server pom excludes only `legend-engine-emit-junit` by name, so
`legend-engine-emit`'s own fixtures reach the dashboard. Adding `legend-engine-emit` to
`excludedDirectoryNames` would keep self-test fixtures out of the catalog view.) Catalog models for
core engine features live in a separate module,
`legend-engine-core/legend-engine-core-emit-tests`, which is a *sibling* of
`legend-engine-core-emit` rather than a child of it precisely so the two are not confused.

#### Distributed Test Locations

Actual EMIT tests are **spread throughout the codebase**, living in the modules that own the
features being tested. Each module that contributes EMIT tests adds a test-scoped dependency
on `legend-engine-emit-junit` and places its models under a classpath root, conventionally
`src/test/resources/emit-models/`. When a single module hosts more than one independently
owned suite, each suite gets its own disambiguated root and its own `*EMITTests` runner instead
of sharing `emit-models/`, so a failure is unambiguously attributable to one feature area rather
than the module as a whole. `legend-engine-xt-relationalStore-emit` is the concrete example —
it owns both the classic relational-mapping suite and the relation (`~func`) mapping suite:

```
legend-engine-xts-relationalStore/
  legend-engine-xt-relationalStore-emit/    ← Module owning both EMIT suites below
    src/test/java/.../emit/
      relational/RelationalEMITTests.java   ← Classic relational-mapping suite runner
      relation/RelationEMITTests.java       ← Relation (`~func`) mapping suite runner
    src/test/resources/
      relational-emit-models/               ← Classic relational-mapping models
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
      relation-emit-models/                 ← Relation (`~func`) mapping models
        relation-simple.emit.yaml
        relation-simple/
          ...
        relation-modelJoin.emit.yaml        ← Relation leaf combined with a relational leaf
        relation-milestoning.emit.yaml
        ...
```

(Tests reuse `relational-shared-domain` via the descriptor `dependencies`
mechanism; see §4.1.)

The same pattern applies to other extension modules (`legend-engine-xts-service`, `legend-engine-xts-generation`, etc.) — each owns the EMIT models for its feature area, with one `*.emit.yaml` per test and a sibling source-root directory. Most modules host a single suite and keep the plain `emit-models/` root name; disambiguate only when a module genuinely hosts more than one independently-owned suite, as above.

Feature areas that are not extensions at all — core Pure language constructs and model-to-model
mappings — are owned the same way by `legend-engine-core/legend-engine-core-emit-tests`. It applies
the same two-suite split as the relational module, because language constructs and M2M mappings are
independent subjects that should be runnable and attributable separately:

```
legend-engine-core/
  legend-engine-core-emit-tests/            ← Core-feature catalog models (no store extension)
    src/test/java/.../emit/core/
      grammar/GrammarEMITTests.java         ← Pure language suite
      m2m/M2MEMITTests.java                 ← Model-to-model mapping suite
    src/test/resources/
      grammar-emit-models/                  ← Parse + compile only: no store, no mapping
        grammar-constraint.emit.yaml
        grammar-constraint/model.pure
        grammar-measure.emit.yaml
        ...
      m2m-emit-models/                      ← M2M mappings with executing test suites
        m2m-transform.emit.yaml
        m2m-transform/
          model/source.pure
          model/target.pure
          mapping/employeeMapping.pure
        ...
```

> **Coverage-report caveat:** the HTML coverage report's discovery walk (§5.4) only looks under
> a root literally named `emit-models/` by default. A module using a disambiguated root name —
> like `relational-emit-models/` and `relation-emit-models/` above, or `grammar-emit-models/` and
> `m2m-emit-models/` in the core-feature module — is invisible to the report
> unless its consuming pom adds that root to `includedRelativeSubpaths`. `legend-engine-server-http-server`
> does this explicitly (see the table in §5.4) so every such suite is represented in
> the coverage dashboard; any other module that disambiguates its root needs the same override.
> **This is the standing cost of the two-suite split** — pay it deliberately, and update the
> server pom in the same PR that introduces the root.

The `EMITRunner` automatically discovers all `*.emit.yaml` files. The YAML file explicitly configures which directories and files comprise the test.

This distribution model has several advantages:
- **Ownership**: Tests live near the code they exercise, so feature owners maintain them.
- **Dependencies**: Each test module has the right classpath for its feature (e.g., a relational
  EMIT test module naturally has relational store dependencies on its classpath).
- **Parallelism**: Tests run as part of each module's build, not as a single bottleneck module.
- **Build-time HTML aggregation**: At server build time,
  `legend-engine-emit-maven-plugin` walks the legend-engine multi-module
  tree, discovers every module's `emit-models/`, parses each `*.emit.yaml`,
  and renders the HTML coverage report to
  `${project.build.outputDirectory}/emit/emit-coverage.html` — i.e.
  under `target/classes/`, so Maven's jar packaging automatically
  bundles it into the server jar. At runtime the server's `EMIT`
  JAX-RS handler simply streams that pre-rendered resource. No
  per-module configuration is required — dropping a yaml under any
  module's `src/test/resources/emit-models/` is enough. See §5.4.

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

- **Run Testable tests**: Find all `Testable` elements in the compiled `PureModel`. A Testable's tests may be grouped into **test suites** (services, mappings, functions) or be **top-level atomic tests** with no enclosing suite (e.g., `Persistence`); both shapes are discovered and executed. A function's top-level tests are compiled into a suite named `default`.
- **Require a runner**: Each Testable's tests run through the `TestableRunnerExtension` that claims its element type. The framework supplies the mapping, service, and function runners; any other (e.g., persistence) must be on the hosting module's test classpath. A Testable with tests that no runner claims fails the phase rather than being skipped, and the JUnit integration reports it as a failing `<element> / No Test Runner` task.
- **Run Legacy Mapping tests**: Find `Mapping` elements with legacy `MappingTest` / `MappingTestSuite` elements.
- **Run Legacy Service tests**: Find `Service` elements with legacy `ServiceTest` elements.
- **Dependency Exclusion**: Only execute tests for elements defined in the primary `model`. Any test defined in an element loaded via `dependencies` MUST be ignored.
- Use `TestableRunner.doTests(...)`, the legacy `MappingTestRunner`, and the legacy `ServiceTestRunner` to execute the in-scope tests, producing their respective result objects.
- **Success criteria**: All in-scope tests (Testable and legacy) pass.
- **Failure mode**: Failed/error `TestResult`, `RichMappingTestResult`, or `RichServiceTestResult` entries, or a Testable with tests and no runner on the classpath.
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
    Stream<DynamicContainer> emit()
    {
        // Discovers models and returns one DynamicContainer per model, with
        // tasks grouped by phase (parse, compile, generation, test, plan).
        return EMITTestSuiteBuilder.testContainers("emit-models/");
    }
}
```

`EMITTestSuiteBuilder` also exposes `tests(String)` for callers that
prefer a flat `Stream<DynamicTest>` (each task carries a `[model-name] …`
prefix), plus per-model selectors on both entry points: `testContainers(root,
names…)` / `tests(root, names…)` / `testContainer(root, name)`. The
single-arg `taskStream(...)` / `taskList(...)` methods are the former names
for the flat form and are now **deprecated** — prefer `tests(...)` or
`testContainers(...)`.

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

### 5.4 HTML Coverage Report

Beyond per-model JUnit output, EMIT exposes a **read-only HTML coverage
dashboard** that visualises which models exist in the catalog and which
features they exercise. It complements §5.2's per-model execution view
with a catalog-wide one — feature combinations, a heatmap of
descriptor-to-feature coverage, and a list of taxonomy features that no
model currently covers (the "coverage gaps" tab).

There is a single path to the HTML: it is produced at server build time
by `legend-engine-emit-maven-plugin` and served at runtime by the
`EMIT` JAX-RS handler streaming the pre-rendered classpath resource.

**Build-time generation.** The mojo's `generate-EMIT-coverage-report`
goal (bound to `generate-resources`) locates the reactor root by walking
up `MavenProject.getParent()` from the consuming project, stopping at
the topmost ancestor that still has a `getBasedir()` on disk — so
externally-resolved parent POMs from a Maven repository are not crossed
and no specific groupId/artifactId is hardcoded. It then traverses that
root's directory tree with `Files.walkFileTree`. Every check happens
inside a single `preVisitDirectory` visitor using `stream().anyMatch(...)`:
name-prefix exclusion (default `["."]`, prunes `.git`/`.idea`/`.mvn`/…),
name-equals exclusion (default `["target"]`), relative-subpath exclusion
(default `["src/main"]`), and relative-subpath **inclusion** (default
`["src/test/resources/emit-models"]`). The subpath checks use
`rel.endsWith(fs.getPath(subpath))`, which is component-wise — no manual
separator gymnastics, and no false hits like `a/b/cd` matching `a/b/c`.
Any subtree matching an exclusion returns `SKIP_SUBTREE`; a match on
inclusion also returns `SKIP_SUBTREE` after recording the dir (the mojo
then enumerates its `*.emit.yaml` files itself via
`EMITModelDiscovery.findEmitYamls(dir)`). The recorded descriptors are
handed to `EMIT_to_HTML.writeHTML(descriptors, outputFile, repoRoot)`
in the `legend-engine-emit-report` module, which delegates to
`buildHTML(...)` for the rendering and takes care of creating the
output's parent directories.

**Configuration.** All five parameters are `@Parameter` fields, so a
consuming pom can tune them via `<configuration>` without forking the
plugin. Defaults, and the overrides `legend-engine-server-http-server`
still applies, are:

| Parameter | Default | Server override |
|---|---|---|
| `outputFilePath` | `${project.build.outputDirectory}/emit/emit-coverage.html` (i.e. `target/classes/emit/...`, so jar packaging automatically bundles it into the consuming jar) | Pinned explicitly to the default so the server jar's bundled resource path stays fixed if the plugin default ever changes |
| `includedRelativeSubpaths` | `["src/test/resources/emit-models"]` | `["src/test/resources/emit-models", "src/test/resources/relational-emit-models", "src/test/resources/relation-emit-models", "src/test/resources/grammar-emit-models", "src/test/resources/m2m-emit-models", "src/test/resources/harvested-emit-models"]` (adds the two disambiguated relational-store roots, the two core-feature roots and the harvested-corpus root on top of the default; the last covers every module's harvested corpus, see `emit-authoring.md` §8.1) |
| `excludedDirectoryNames` | `["target"]` | `["target", "legend-engine-emit-junit"]` (excludes the binding module's bootstrap examples from the server-side report) |
| `excludedDirectoryNamePrefixes` | `["."]` | — |
| `excludedRelativeSubpaths` | `["src/main"]` | — |

**Runtime serving.** The `EMIT` handler at `GET /api/emit/html` streams
the `/emit/emit-coverage.html` classpath resource. If the resource is
missing — e.g. the server was assembled without the mojo bound in its
pom — the endpoint returns `503 Service Unavailable` with an explicit
message about rebuilding the server module. The runtime has no
dependency on `EMITModelDiscovery`, no catalog manifest, and nothing
that could drift between build and serve — whatever HTML the mojo wrote
is exactly what is served.

Adding a new yaml is just dropping it under any module's
`src/test/resources/emit-models/`; the next clean build of the server
picks it up automatically. The mojo silently skips any module whose pom
declares a customized `<build><testResources>`, since the report only
supports the conventional `src/test/resources` layout.

### 5.5 Snapshots for comparing engine revisions

Setting the `legend.emit.snapshot.dir` system property makes `EMITTestSuiteBuilder` end every
model with a **Snapshot** group, and `EMITSnapshot` writes what the engine produced under
`<dir>/<suite>/<model>/`:

| File | Content |
|---|---|
| `model.json` | parse and compile outcome, primary source ids |
| `pmcd.json` | the parsed `PureModelContextData`, serialized by the standard protocol mapper |
| `pmcd.roundtrip.json` | `pmcd.json` read back and re-serialized by the same engine |
| `plans/service/<service>.json` | the execution plan of each primary-scope service, or its error |
| `plans/test/<testable>/<suite>/<test>.json` | what each test runs, from its runner's debug (`TestRunner.debugTestSuite` / `debugAtomicTest`): the execution plan, or for a test that runs none (persistence) its `statements`; or the error |
| `../_subtypes.json` | once per suite: every `_type` each polymorphic protocol base resolves |
| `complete.json` | written last; a model directory without it was cut short (crash, OOM, killed fork) |

With `legend.emit.snapshot.crossRead=<other snapshot dir>` set as well, the group also deserializes
the other snapshot's `pmcd.json` with this engine, re-serializes it (`pmcd.crossread.json`) and
compiles it (`crossread.json`). Running that in both directions is a protocol compatibility check
between two revisions. Compare `pmcd.crossread.json` with `pmcd.roundtrip.json`, not with
`pmcd.json`: both have been through this engine's deserializer, so differences between parsing and
deserializing cancel out.

Errors are recorded without stack frames, so line numbers do not differ between revisions. When the
property is unset nothing changes: no Snapshot group is emitted and nothing is written.

The `engine-review` Claude Code plugin (`plugins/engine-review`) drives this end to end. It builds
two revisions with the same corpus copied onto both, snapshots them, and reports SQL, plan and
protocol differences. Every model in the catalog is part of that corpus, which is one more reason to
keep the catalog covering the shapes that matter.

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
# Connection-dialect tags (h2, duckdb, snowflake, ...) cover two independent things:
# the dialect Plan Generation renders SQL in (whatever's declared) and the engine Test
# Execution actually runs on (h2 or duckdb, forced by the query's return type,
# regardless of what's declared) — a fixture can need tags for both. See
# `emit-authoring.md` §11.4.
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
| `scaffolding:relation-function` | A Pure function returning a `Relation<Any>`, used as a mapping source |
| `scaffolding:relation-mapping` | Base relation-function class mapping (`~func` / `~src`, no special features) |
| `scaffolding:m2m-mapping` | Base model-to-model mapping |
| `scaffolding:runtime` | A runtime definition |
| `scaffolding:model-connection` | A model connection (M2M tests) |
| `scaffolding:model-chain-connection` | A `ModelChainConnection` bound to `ModelStore`, serving an M2M mapping's source class from a downstream mapping |
| `scaffolding:non-relational-connection` | Baseline connection to a non-relational store — a service store, flat-data source or document store — standing in for the relational connection the other scaffolding assumes |
| `scaffolding:runtime-merge` | Execution source assembled at run time by merging a caller-document-backed runtime with a stored runtime, so different levels of one read resolve against different sources instead of against the single binding a declared runtime carries |
| `scaffolding:service-store-connection` | A service store connection |

#### Grammar — Pure language constructs

| Feature | Description |
|---|---|
| `grammar:abstract-class` | Abstract class definition (not directly instantiated) |
| `grammar:association` | Association (1:1, 1:N, N:M) |
| `grammar:association-shadowing-property` | Class declaring a to-many property with the same name and type as the endpoint an association already contributes to it, so the class carries a stored property that shadows the association-supplied one |
| `grammar:class-inheritance` | Class inheritance hierarchy |
| `grammar:class-temporal-stereotype` | Milestoning stereotype applied to the class itself (`<<temporal.businesstemporal>>`, `<<temporal.processingtemporal>>`, `<<temporal.bitemporal>>`), declaring the temporal shape as a property of the type rather than of a store relation |
| `grammar:collection-multiplicity-bound` | Property declared with an explicit bounded collection multiplicity (for example `[1..4]`) rather than an unbounded `[*]`, so the bound is part of the published contract |
| `grammar:complex-typed-property` | Property whose type is another authored class rather than a primitive, both as a repeated child collection and as a single child reference, so element traversal (schema generation, graph fetch) has to walk into the nested type instead of reading a scalar |
| `grammar:constraint` | Class-level constraint |
| `grammar:constraint-collection-op` | Class constraint whose body quantifies element-wise over a collection property, so the predicate holds per member rather than over the owning instance |
| `grammar:constraint-function-call` | Class constraint whose boolean expression delegates to a standalone Pure function instead of being written inline, so the same check is shared by several constraints and evaluated as a function call in the generated plan |
| `grammar:derived-property` | Derived (computed) property |
| `grammar:derived-property-collection-op` | Derived property whose body applies a collection function (`map`, `filter`, `fold`, ...) |
| `grammar:diagram` | `###Diagram` model-visualization element |
| `grammar:duplicate-enumeration` | Two enumerations declaring the same value set, kept as distinct types so separate mappings can target them independently |
| `grammar:empty-named-qualified-property` | Qualified property declared with an empty name: it compiles and participates in the type, but is unaddressable from a query |
| `grammar:enumeration` | Enumeration type definition |
| `grammar:equality-key-stereotype` | `equality` stereotype marking the subset of properties that define instance identity for the class |
| `grammar:foreign-model-property` | Class whose nested property is typed by a separately packaged object tree it does not own, so the wrapper stays queryable as a flat row while its one complex property is the entry point into a graph owned by another package |
| `grammar:function` | Standalone Pure function |
| `grammar:function-overloading` | Standalone Pure functions sharing one qualified name and differing only in the class of their collection parameter, so dispatch is by parameter type and each body may group or project a different shape — the same call site therefore returns differently shaped tabular results depending on the class it is applied to, and one overload pair republishes one class's properties under the other's column names so the two classes become interchangeable downstream |
| `grammar:instance-of-discrimination` | Runtime type test (`instanceOf`) used to discriminate between the concrete variants of a polymorphic source hierarchy before casting, so each variant selects its own transform arm and the order the variants are tested in decides which arm a combined variant lands in |
| `grammar:marker-subtype-hierarchy` | Subtype hierarchy whose specializations declare no properties of their own, existing purely as type markers that downstream queries dispatch on |
| `grammar:match-expression` | `match([...])` dispatching on subtype |
| `grammar:measure` | Measure / unit definition |
| `grammar:milestoned-property-call` | Property call supplying an explicit milestoning date argument at the call site rather than inheriting the enclosing temporal context |
| `grammar:milestoned-qualified-property-traversal` | Qualified property whose body traverses a milestoned property, so the derivation propagates the temporal context into its result |
| `grammar:nested-association` | Multi-level association traversal |
| `grammar:nested-collection-property` | Property whose type is a class that itself declares a collection property, so the published shape is a nested tree rather than a flat record |
| `grammar:profile` | Profile / stereotype / tag definition |
| `grammar:qualified-property` | Qualified (parameterised) property |
| `grammar:qualified-property-date-difference` | Qualified property computing an interval between two date properties of the owning class |
| `grammar:quoted-identifier` | Package or element name written as a quoted identifier because it is not a legal bare name |
| `grammar:section-index` | `SectionIndex` packageable element recording import-aware sections so that shortened element references in element bodies resolve against declared imports |
| `grammar:self-referencing-property` | Optional property whose type is the class that declares it, giving a recursive parent chain of unbounded depth whose only terminator is the property being empty, so graph traversal over the type is self-referential rather than layered |
| `grammar:shared-reference-class` | Reference class reused as the property type of several owning classes rather than duplicated per owner, so one definition fixes the shape of a shared sub-record |
| `grammar:stereotype-tag-metadata` | Element annotated with both a profile stereotype and a tagged value, so classification and free-form metadata travel together on the same declaration |
| `grammar:storage-type-stereotype` | Stereotype recording the intended physical storage type or width of a property, carrying a persistence hint on the model declaration |
| `grammar:tagged-value` | Tagged value from a profile applied to an element (class, enumeration or property) and read back as model metadata, including a quoted tag name |
| `grammar:text-element` | `###Text` documentation element |

#### Mapping — transform and mapping-level features

| Feature | Description |
|---|---|
| `mapping:aggregation-aware-mapping` | Aggregation-aware mapping |
| `mapping:cross-store` | Cross-store mapping (M2M ↔ relational) |
| `mapping:enum-of-expression` | Enumeration mapping whose source values come from a computed expression rather than from a column |
| `mapping:enumeration-mapping` | Enumeration value mapping / transform |
| `mapping:enumeration-mapping-of-expression` | Enumeration mapping consumed from a transform whose mapped source is an expression over the source instance rather than a bare source enum property, so the enum value is resolved from a computed input |
| `mapping:instance-source-filter` | `~filter` predicate attached to an instance-sourced (`~src`) mapping set, gating which source instances are materialized at all rather than filtering the mapped result: a wide boolean conjunction combining emptiness checks on deep traversed property paths, a string-prefix test, a membership test over a case-folded value and a disjunctive arm over an optional to-many property, applied on both the root set and a nested set so a source whose traversed path is empty is dropped |
| `mapping:m2m-arithmetic-transform` | Arithmetic expression as the binding of a target property inside a model-to-model (`~src`) transform body: one binding negates a sum of five optional masses, one multiplies a mass by a negative constant, one subtracts an opening day number from a closing one so the result can come out negative, and one multiplies three optional measurements, so each published number is a signed function of nullable sources evaluated in the engine rather than a copy or a store-pushed expression |
| `mapping:m2m-chained-relational` | M2M mapping whose `~src` class is itself mapped to a relational store, chained through a `ModelChainConnection` |
| `mapping:m2m-collection-flatten` | Model-to-model transform flattening a nested collection of the source into a repeated target property, so one source instance yields many target rows |
| `mapping:m2m-conditional-transform` | Model-to-model property transform whose body is a conditional defaulting each navigated leaf independently, so an absent source leaf yields that property's own fallback rather than suppressing the rest of the mapped instance |
| `mapping:m2m-covariant-list-return` | M2M transform helper function whose returned list is statically a strict subtype of the function's declared return list |
| `mapping:m2m-derived-source-property` | Derived property on M2M source class |
| `mapping:m2m-enum-of-expression` | Model-to-model enumeration mapping whose source values are produced by a computed expression rather than read from a source property |
| `mapping:m2m-enum-to-string-coercion` | Model-to-model transform coercing an enumeration value to its string name, so the target carries the label rather than the typed value |
| `mapping:m2m-enum-values-expansion` | Model-to-model transform expanding an enumeration's full value set into the target, so the output enumerates the vocabulary rather than the source instance |
| `mapping:m2m-local-property` | Local property in M2M mapping |
| `mapping:m2m-nested-class-mapping-reference` | Model-to-model property mapping naming another class mapping by id as the target of a nested property, so the nested shape is built by a separately declared mapping |
| `mapping:m2m-nested-property` | Model-to-model class mapping binding a complex-typed property to another set implementation of the same source instance, so one parsed source row narrows into a nested object graph resolved by set id |
| `mapping:m2m-null-sentinel` | Model-to-model transform translating a sentinel source value into null (or a null source into a sentinel), so absent data is represented explicitly on one side of the transform |
| `mapping:m2m-scalar-to-list-widening` | M2M transform helper function declared to return a list whose body ends in a to-one or zero-to-one expression |
| `mapping:m2m-source-filter` | `~filter` predicate on a model-to-model (`~src`) class mapping, gating which source instances are materialized so filtered-out sources produce no target instance, asserted through mapping and service tests over both retained and dropped sources |
| `mapping:m2m-string-function-transform` | Model-to-model transform composing string functions over source properties to derive the target value |
| `mapping:m2m-subtype-dispatch` | Model-to-model transform dispatching on the runtime subtype of the source instance, so different specializations map through different bodies |
| `mapping:m2m-target-implementation` | Model-to-model class mapping whose target is reached by a named implementation id from another class mapping's property rather than by the root default, so one source document materializes both a summary instance and the nested view instance bound beside it, and the nested branch is populated only when the graph fetch asks for it |
| `mapping:m2m-target-instantiation` | Target class constructed with the `new` operator (`^Target(...)`) inside an M2M transform |
| `mapping:m2m-to-many-property-mapping` | Model-to-model property mapping whose target property is to-many, so one transform contributes a collection rather than a single value |
| `mapping:m2m-transform` | Model-to-model transform expression |
| `mapping:mapping` | Generic mapping (legacy tag) |
| `mapping:mapping-include` | Mapping include composition |
| `mapping:mapping-include-store-substitution` | Mapping include that substitutes one store for another, so the included set implementations are re-pointed at a different physical store without being redeclared |
| `mapping:operation-mapping` | Operation mapping (inheritance dispatch) |
| `mapping:operation-mapping-merge` | Merge operation mapping |
| `mapping:operation-mapping-merge-validation` | Merge with cross-set validation |
| `mapping:parallel-schema-variant-mappings` | Sibling mappings binding the same classes, associations and enumeration mappings with byte-for-byte identical transform logic, differing only in which physical schema of one store their main-table aliases and join references resolve against, so a per-deployment schema copy is a mapping-level variation rather than a separate modeling concept and each variant is independently testable over its own schema's fixtures |
| `mapping:processing-milestoning` | Relational mapping of a processing-temporal class onto a table with processing milestoning |
| `mapping:relational-arithmetic-transform` | Arithmetic over columns in a relational property mapping |
| `mapping:relational-association-implementation` | Association implementation in relational mapping |
| `mapping:relational-binding-backed-property` | Relational property whose value is parsed out of a column through an external-format binding, so a stored document becomes typed model content |
| `mapping:relational-case-expression` | Relational property transform built from a `case` expression over column values, compiled into a SQL `CASE` in the generated plan |
| `mapping:relational-class-mapping-extends` | Relational class mapping declared as an extension of another class mapping by id, inheriting its column bindings |
| `mapping:relational-conditional-transform` | Relational property transform whose value comes from a nested conditional cascade over columns rather than a direct column read |
| `mapping:relational-date-function-transform` | Relational property transform applying date functions — date-component extraction and evaluation-time date resolution — to a bound column, rather than reading the column or composing it with text/arithmetic functions |
| `mapping:relational-date-part-transform` | Relational property transform reducing a date column to one of its calendar parts (`year(...)`) so the class exposes the part as a plain integer property, computed in the generated SQL rather than stored as its own column |
| `mapping:relational-distinct` | DISTINCT projection |
| `mapping:relational-embedded` | Embedded relational mapping |
| `mapping:relational-enum-of-expression` | Enumeration-mapped relational property whose source is a computed expression (conditional over columns) instead of a plain column, so the enum mapping resolves the derived code |
| `mapping:relational-external-format-binding-property` | Relational property parsed from a column through an external-format binding |
| `mapping:relational-filter-scoped` | Relational class implementation declaring a `~filter` scope naming a store-declared filter, so the implementation's extent is a permanent restriction of the relation for every consumer rather than a query-time predicate; the filter must name the same relation the implementation's main table resolves to, so a filter declared over a base table cannot scope an implementation whose main table is a view over it |
| `mapping:relational-group-by` | GROUP BY projection |
| `mapping:relational-inline-embedded` | Inline embedded mapping |
| `mapping:relational-join-path` | Property transform reaching its column through a declared join path (`@Join`) rather than a column of the main table |
| `mapping:relational-join-reached-column` | Column reached over a named join and bound to a property of the mapped class |
| `mapping:relational-join-reached-property` | Property of a mapped class bound to a column reached through a named join from the main table rather than to a column of the main table itself |
| `mapping:relational-join-transform` | Property mapping applying a transform over a column reached through a named join, so the join-reached value is reshaped before it is bound |
| `mapping:relational-joined-table-inheritance` | Joined-table inheritance strategy |
| `mapping:relational-literal` | Literal value in mapping |
| `mapping:relational-literal-default` | Relational property bound to a literal value rather than to a column, so every row of the extent carries the same constant |
| `mapping:relational-literal-list` | Literal list in mapping |
| `mapping:relational-main-table-alias` | Main table alias override |
| `mapping:relational-multi-hop-join-path` | Relational property bound through a chained join path (`@a > @b > @c`) spanning three or more joins |
| `mapping:relational-null-sentinel` | Relational property transform substituting an explicit sentinel value when the underlying column is null or empty |
| `mapping:relational-otherwise-embedded` | Otherwise-embedded mapping |
| `mapping:relational-polymorphic-query` | Polymorphic query dispatch |
| `mapping:relational-primary-key` | Explicit primary key specification |
| `mapping:relational-property-expression` | Relational property mapping whose value is a nested SQL expression over columns rather than a bare column reference — folded string concatenation with a blank-name fallback, a graded `case` over a compared column, and a date-part extraction — so the transform is pushed into the generated statement instead of being computed after the fetch |
| `mapping:relational-property-mapping-implementation` | Reference property mapping qualified by an explicit target set implementation (`prop[setId]: [<db>]@Join`), so a class reached by a join is resolved through a named sibling set rather than through the class's root set, letting two mappings of the same class select different physical registry tables over the same join name |
| `mapping:relational-semi-structured-extract` | Relational property extracted from a document-valued column, so a field of the stored payload is bound as a scalar property |
| `mapping:relational-sentinel-key-column` | Relational class mappings whose declared primary key includes an interval-closing column even though neither the table nor the class declares milestoning, so version identity is hand-rolled: the open row is the one carrying the sentinel value and a consumer must know that convention to select it. Distinct from `milestoning:*`, which the model deliberately does not use, and from `mapping:relational-primary-key`, which covers the plain business key |
| `mapping:relational-set-implementation-extends` | Relational set implementation declared as an extension of another set implementation by id |
| `mapping:relational-set-implementation-filter` | Row filter declared as the `~filter` scope of a relational set implementation, so every consumer of that extent inherits the predicate |
| `mapping:relational-single-table-inheritance` | Single-table inheritance strategy |
| `mapping:relational-string-function-transform` | Relational property transform applying string functions (case folding, trimming, concatenation, substring) to column values |
| `mapping:relational-subtype-extends` | Subtype set implementation declared as an extension of its supertype's set implementation, so the specialization reuses the parent's column bindings |
| `mapping:relational-subtype-filter` | Subtype set implementation narrowed by a filter, so the specialization selects its own rows out of the shared extent |
| `mapping:relational-subtype-mapping` | Subtype mapping (discriminator/filter-based subtype resolution) |
| `mapping:relational-table-alias-column` | Table-alias column reference |
| `mapping:relational-target-implementation` | Property mapping naming the class implementation the produced value belongs to (`property[TargetImpl]: …`), resolving an otherwise ambiguous target when the property's type has several relational implementations, so the produced instance is typed as one subtype implementation and rows outside that implementation's scope resolve to nothing |
| `mapping:relational-view-backed-class` | Class whose set implementation nominates a store `View` as its main table, so the mapped extent is the derived relation instead of a base table |
| `mapping:relational-view-backed-property` | Relational property bound to a column of a derived relation (`View`) reached over a join, so an aggregate computed in the store surfaces as a class property |
| `mapping:relational-view-source` | Class mapping whose main relation is a declared view rather than a base table, so the mapped extent is the view's query result |
| `mapping:router-union` | Router union mapping |
| `mapping:service-store-mapping` | `ServiceStore` class mapping binding a class onto a declared service path — request parameters supplied from mapping-level bindings and the response mapped through an external-format binding, so a remote call is mapped like any other set implementation |
| `mapping:store-union` | Store union mapping |
| `mapping:wide-flat-projection-class` | Class mapping binding a wide denormalized reporting table column-for-column onto one flat class, so the mapped extent mirrors the physical row |

**Relation-function mappings** (`Relation` class mappings — `~func` / `~src`; grammar
in `RelationFunctionMappingParserGrammar.g4`). These are mapping-level capabilities and
so live in the `mapping:` domain alongside their relational counterparts:

| Feature | Description |
|---|---|
| `mapping:relation-binding-transformer` | `Binding <qualifiedName>:` property transformer |
| `mapping:relation-embedded` | Embedded property mapping — `prop ( ... )` |
| `mapping:relation-expression-rhs` | Full `$row` expression as a property RHS (vs. a bare column) |
| `mapping:relation-filter` | `->filter` applied in the source relation (the `~func` body) |
| `mapping:relation-filter-join` | Inline `$src->filterJoin(rel(), {..}).COL` property (source column gated by a join) |
| `mapping:relation-group-by` | `->groupBy` aggregation in the source relation, mapped as the class shape |
| `mapping:relation-inline-embedded` | Inline embedded property mapping — `prop () Inline [setId]` |
| `mapping:relation-inline-model-join` | Inline `modelJoin({..})` class-navigation property with a model-level condition (no association; target resolved from the property's declared type) |
| `mapping:relation-local-property` | Local property — `+prop: Type[mult]: <rhs>` |
| `mapping:relation-model-join` | ModelJoin association between relation-mapped classes |
| `mapping:relation-navigation-join` | Inline `$src->navigationJoin(rel(), {..}).COL` property (joined-side column, chainable) |
| `mapping:relation-primary-key` | Explicit `~primaryKey: col` / `~primaryKey: [a, b]` |
| `mapping:relation-src` | `~src` inline-source form (vs. `~func`) |
| `mapping:relation-union` | Union of relation set implementations |
| `mapping:relation-window-function` | Window function (`over(...)` / `->extend`) in the source relation |
| `mapping:relation-xstore-association` | `XStore` cross-set association between relation-mapped classes (distinct from ModelJoin) |

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
| `store:relational-semistructured` | `SEMISTRUCTURED` column holding a JSON document |
| `store:relational-semistructured-array-functions` | Collection functions over a semi-structured array |
| `store:relational-semistructured-array-index` | Fixed-index access into a semi-structured array |
| `store:relational-semistructured-binding` | JSON document typed as a model via an ExternalFormat binding |
| `store:relational-semistructured-explode` | `explodeSemiStructured` fanning a JSON array into rows |
| `store:relational-semistructured-flatten` | Lateral flatten of a to-many semi-structured property |
| `store:relational-semistructured-navigation` | Path navigation into a semi-structured document |
| `store:relational-zoned-timestamp` | Column the database holds as a `TIMESTAMP WITH TIME ZONE` (declared `TIMESTAMP` in the store, which has no zoned type) |
| `store:service-store` | Service store |
| `store:flat-data-store` | Flat-data store |
| `store:document-store` | `###MongoDB` document store element declaring collections of document-shaped records, read through an external-format binding rather than as relational rows |
| `store:elasticsearch-store` | `###Elasticsearch` store element declaring a search index as the physical source a mapping binds to |
| `store:mongodb-collection-schema-validation` | Document collection whose accepted shape is pinned by an embedded JSON schema together with a validation level and a validation action, so required fields, BSON types and nested-object constraints are declared on the store rather than enforced downstream |
| `store:mongodb-store` | `###MongoDB` `Database` element declaring named document collections as the queryable store, so a mapping binds class properties to document paths of a collection instead of to columns of a relational table |
| `store:relational-batch-bookkeeping-columns` | `Database` table whose columns pair the payload with batch bookkeeping: two wide-decimal batch boundary markers and a digest string holding the table's only declared key, so the mapped class takes its identity from the digest and the batch markers travel alongside the payload wherever the class is queried |
| `store:relational-composite-join` | Named join whose condition spans several column pairs, so the two relations are matched on a composite key |
| `store:relational-conditional-column` | `Database` element whose derived relation carries a column computed by a nested conditional over aggregated columns, so the banding is fixed inside the store and every mapping and query inherits it instead of restating it |
| `store:relational-cross-store-join` | Named join whose two sides live in different stores, so the join crosses a store boundary rather than staying inside one database |
| `store:relational-join` | A named join defined in the store |
| `store:relational-join-equality` | Named join whose predicate is a single-column equality between two tables |
| `store:relational-join-literal-condition` | Named join whose predicate is a key equality conjoined with an equality against a string literal on a discriminator column, so one foreign-key column resolves to a different parent table per literal value; two such joins share the child column and differ only in the literal, which is how a polymorphic reference is modeled relationally |
| `store:relational-join-range` | Named join whose predicate combines an equality with a two-sided range comparison, so the joined row is selected by a validity window rather than by key alone |
| `store:relational-latest-version-view` | View selecting the latest version of each key, so consumers read a current-state relation without restating the version predicate |
| `store:relational-multi-column-primary-key` | Table with a composite (multi-column) primary key |
| `store:relational-multi-store` | Model declaring several relational stores, so mappings and connections must name which physical database each extent resolves against |
| `store:relational-nested-view` | View built over another view rather than over a base table, so the store layers derived relations |
| `store:relational-self-join` | Self-join (a table joined to itself) |
| `store:relational-self-join-target-placeholder` | Join using the `{target}` placeholder so a table joins to itself, letting one relation be reached twice in different roles |
| `store:relational-semi-structured-extraction-join` | Named join whose condition uses a value extracted from a document-valued column, so the match key is computed out of a semi-structured payload |
| `store:relational-store-include` | Relational store composition via `include`: the including store resolves the tables, columns, joins and filters of the included stores as if locally declared and contributes only joins or views of its own. Store-level composition, distinct from `mapping:mapping-include` |
| `store:relational-view` | A `View` (derived table) defined in the store |
| `store:relational-view-aggregate` | Store `View` whose columns are aggregate expressions over a base table, exposing a pre-aggregated relation |
| `store:relational-view-distinct` | Store `View` declaring `~distinct`, so duplicate rows of the derived relation collapse before any mapping reads it |
| `store:relational-view-group-by` | Store `View` declaring an explicit `~groupBy` key alongside its aggregate columns |
| `store:unbounded-text-column-convention` | Text columns declared without a length bound as a store-wide convention, so the physical width is left to the target database rather than pinned in the model |
| `store:vendor-store` | Store declared against a specific database vendor's dialect, so portability across vendors is an explicit property of the model rather than an assumption |
| `store:wide-denormalised-table` | Single reporting table of several hundred mixed-type columns holding an entire subject denormalized into one row per record, with no joins available |

#### Milestoning — temporal features

| Feature | Description |
|---|---|
| `milestoning:all-versions-in-range-query` | All-versions-in-range temporal query |
| `milestoning:all-versions-query` | All-versions temporal query |
| `milestoning:bi-temporal` | Bi-temporal milestoning (business + processing) |
| `milestoning:business-snapshot` | Business-dated snapshot milestoning: each load stamps a business date on a full restatement of the relation, so a read selects one dated snapshot rather than an open interval |
| `milestoning:business-temporal` | Business-temporal milestoning |
| `milestoning:latest-batch-query` | Extent access selecting only the most recently landed batch of a batch-milestoned relation, comparing each row's batch against a grouped high-water mark instead of naming an instant or a batch, so the result depends on which batches have completed rather than on a supplied parameter. Pending in the canonical `emit.md` §6.2 taxonomy |
| `milestoning:milestoned-association` | Association whose ends are milestoned |
| `milestoning:milestoning` | Generic milestoning marker |
| `milestoning:physical-batch-column-filter` | Batch versioning carried as plain integer `batch_in`/`batch_out` columns and filtered in the query against an open-ended sentinel, rather than declared to the platform as milestoning — so batch selection is the query's responsibility |
| `milestoning:point-in-time-query` | Point-in-time temporal query |
| `milestoning:processing-date-parameter` | Service query parameterized by a processing date for milestoned data |
| `milestoning:processing-temporal` | Processing-temporal milestoning |

#### Execution — service, generation, and test infrastructure

| Feature | Description |
|---|---|
| `execution:bigquery-function` | BigQuery function activator |
| `execution:binding` | External format binding |
| `execution:checked-graph-fetch` | Execution whose graph-fetch tree is traversed in checked mode (`graphFetchChecked(...)->serialize(...)`), so each returned document carries the source record, the typed values built from it and the per-property defects raised while binding it, and a row that violates a constraint or fails to bind is reported inside the payload instead of failing the execution |
| `execution:data-element` | Data element definition |
| `execution:data-space` | `###DataSpace` (Data Product) element wired into a service execution context |
| `execution:data-space-executable` | `###DataSpace` `executables` entry naming a query or function published through the data space alongside its execution contexts |
| `execution:data-space-template-executable` | Data space `executables:` entry carrying an inline query template — title, description and a query bound to the data space's own execution context — so a curated executable ships with the data product instead of being defined as a separate service |
| `execution:embedded-runtime` | Execution whose runtime is declared inline on the element that uses it (an embedded runtime literal with its connections) instead of referencing a named `###Runtime` element |
| `execution:empty-ownership-identifier` | Service ownership declared with a present but empty deployment identifier, so the ownership block is structurally complete while carrying no identifying value |
| `execution:external-format` | External format specification |
| `execution:external-format-binding` | External format binding |
| `execution:external-format-flatdata` | FlatData external format |
| `execution:external-format-json` | JSON Schema external format |
| `execution:external-format-protobuf` | Protobuf external format |
| `execution:external-format-schema-set` | `###ExternalFormat` `SchemaSet` element holding one or more named schemas of a declared format that bindings resolve their content against |
| `execution:external-format-serialization` | Result serialized through an external-format binding inside the query body, so the document shape is chosen in the service rather than by a serialization node on the execution |
| `execution:external-format-xsd` | XSD external format |
| `execution:file-generation` | File generation specification |
| `execution:file-generation-avro` | Avro file generation |
| `execution:file-generation-graphql` | GraphQL file generation |
| `execution:file-generation-json-schema` | JSON Schema file generation |
| `execution:file-generation-protobuf` | Protobuf file generation |
| `execution:function-activator` | Function activator element |
| `execution:function-test` | Test block attached to a standalone function element: named cases pairing a literal argument list with an expected return value, so shared helper branches are covered directly rather than through a mapping or service |
| `execution:generation-specification` | `GenerationSpecification` element |
| `execution:graph-fetch` | Graph fetch query (`graphFetch`/`serialize`) executed in a test |
| `execution:graphql-query` | Read contract expressed as a typed, paged query member on a stereotyped class binding — a named query field taking paging arguments and filtering, sorting and paging a relational extent — rather than as a service, mapping test or access point |
| `execution:hosted-service` | Hosted service function activator |
| `execution:legacy-mapping-test` | Deprecated `MappingTests` block on a Mapping (EMIT Phase 5's legacy `MappingTestRunner`) |
| `execution:legacy-service-test` | Deprecated `test: Single` / `test: Multi` block on a Service (EMIT Phase 5's legacy `ServiceTestRunner`) |
| `execution:m2m-migration-missing-java-types` | Query opts into `Feature.M2M_MIGRATION_MISSING_JAVA_TYPES`, so plan generation adds the Java types and getters M2M plan code references but source-tree calculation left out |
| `execution:mapping-test` | `Mapping` `testSuites` block: a mapping-level test suite executing a stored query against embedded test data and asserting the mapped result |
| `execution:model-generation` | Model generation specification |
| `execution:mongodatabase` | `###MongoDB` `Database` element declaring collections whose document shape is fixed by an embedded JSON schema |
| `execution:multi-execution-service` | Multi-execution service |
| `execution:opaque-service-pattern` | `Service` published under a URL pattern that carries no descriptive route segment — an opaque token stands in for the resource name — so the deployed route reveals nothing about the queried domain while remaining a normal single-execution service with ownership and a test suite; the taxonomy carries `execution:service` and `execution:service-test` but no id for the routing convention itself, so it is proposed under the closest domain |
| `execution:persistence` | `Persistence` element defining a batch/service-driven persistence pipeline |
| `execution:plan-generation` | Execution plan generation |
| `execution:post-validation` | Service post-validation |
| `execution:schema-set` | Schema set definition |
| `execution:serialization-date-time-format` | `dateTimeFormat` on the `AlloySerializationConfig` a graph fetch `serialize` writes its date-times with |
| `execution:service` | Service definition |
| `execution:service-inline-runtime` | Service declaring its runtime inline in the execution block instead of naming a stored `Runtime`, so the execution source travels with the endpoint |
| `execution:service-mcp-server` | `Service` declaring an `mcpServer` binding, so the packaged service is additionally published as a named tool of a model-context-protocol server alongside its own pattern and execution, and the binding is validated by the same compile and plan path as the service itself. Pending in the canonical `emit.md` §6.2 taxonomy |
| `execution:service-ownership` | `Service` declaring structured ownership as a first-class field in either of two shapes — an explicit list of owning identities, or a single opaque owning-entity identifier — rather than as free-text documentation |
| `execution:service-ownership-did` | Service ownership block identifying the owner by deployment identifier (`Deployment { identifier: ... }`) rather than by a list of users |
| `execution:service-parameter` | Service whose query takes parameters, with tests that pass them values (`parameters:`) |
| `execution:service-path-parameter` | Service pattern embedding a parameter in the URL path (`/{param}`), so the argument binds positionally from the path rather than from the query string |
| `execution:service-single-execution` | Service carrying a single `PureSingleExecution` block — one mapping, one runtime, one query |
| `execution:service-test` | Service test suite |
| `execution:service-test-external-format-json` | Service test whose expected result is an external-format JSON document rather than a tabular fixture, so the assertion compares serialized content |
| `execution:service-test-serialization-format` | Service test declaring the serialization format its assertion compares against, pinning the wire shape the endpoint is expected to emit |
| `execution:service-user-ownership` | Service ownership declared as an explicit user list rather than a deployment or directory identity, naming the individual owners accountable for the endpoint, including services owned jointly by more than one user |
| `execution:shared-test-data` | Shared test data element |
| `execution:snowflake-app` | Snowflake app function activator |
| `execution:snowflake-m2m-udf` | `###Snowflake` `SnowflakeM2MUdf` function activator publishing a scalar, string-returning model-to-model function as a warehouse-resident user-defined function: a UDF name, the bound single-argument function signature, a deployment-identifier ownership block, a prose description carried into the warehouse object, a deployment schema and a deployment stage, and a dedicated activation connection, so a graph-fetch serialization of a nested document is callable inside the warehouse rather than behind a service path, and the activator inherits its bound function's parameter list and scalar return type so the two are regenerated together |
| `execution:snowflake-udtf-activator` | `###Snowflake` `SnowflakeApp` activator publishing a tabular-returning function as a warehouse user-defined table function under an application name, a deployment-identifier ownership, a usage role and an activation configuration pointing at a warehouse connection |
| `execution:test-data` | Test data definition |
| `execution:test-data-branch-coverage` | Test fixtures deliberately shaped so every conditional arm, outer-join miss and out-of-range milestoning row of the elements under test is reached by at least one row, making branch coverage a property of the committed data rather than of the assertions |
| `execution:test-suite` | A Testable `testSuites` block (mapping or service) exercising TEST_EXECUTION |
| `execution:unresolvable-mapping-runtime-pairing` | Service executable naming a mapping whose store the paired runtime never connects, so the element compiles and its plan-independent metadata resolves while execution against that pairing cannot be satisfied |

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

#### Query — query shapes the router rewrites

These tag what the model's *queries* do, as opposed to how it is mapped. They exist so the corpus,
which `engine-review` compares across revisions, can be checked for the shapes that regress.

| Feature | Description |
|---|---|
| `query:exists-to-many` | `exists()` over a to-many property in a filter |
| `query:exists-negated` | `!exists()` over a to-many property |
| `query:exists-nested` | `exists()` inside another `exists()` lambda |
| `query:multiple-exists-same-property` | Several `exists()` on the same to-many property combined with `&&` / `\|\|` |
| `query:to-many-projection` | Projecting a to-many property (one row per element) |
| `query:to-many-aggregation` | Aggregating a to-many property (`size()`, `count()`) inside a projection |
| `query:aggregate-conditional` | Aggregation whose accumulator body is a conditional, so the rows folded into each group are chosen by a predicate inside the aggregate rather than by a prior filter |
| `query:aggregate-max` | Whole-extent aggregation with no grouping key, reducing a narrowed relation to a single row carrying an extreme value, so the plan is the degenerate keyless form of an aggregation and must still yield a defined result for an empty input. Pending in the canonical `emit.md` §6.2 taxonomy |
| `query:aggregate-relation` | Aggregation expressed in relation grammar, grouping and folding a relation value rather than a tabular projection |
| `query:aggregation` | Grouped aggregation over a projected result |
| `query:arithmetic` | Numeric computation inside a query projection — stored attributes combined with literals and scaled before a division, with the rounded result projected as its own column, so operand emptiness and the rounding boundary are separate branches of the same expression |
| `query:class-instance-result` | Service returning class instances rather than a tabular result, so the response shape is an object graph |
| `query:collection-index-access` | Query reading a positional element out of a collection produced upstream in the same expression (a split of a delimited identifier), so an index rather than a filter selects the value |
| `query:collection-op` | Query using collection operations — membership and mapping over a multiplicity-many navigation — inside a predicate or a projection expression |
| `query:conditional-expression` | Multi-armed `if` cascade evaluated as the whole result of a function or transform — emptiness guard, ordered value bands and a sentinel fallback arm — so branch selection happens in the executed expression rather than in a projected column |
| `query:conditional-projection` | A projected column computed by a multi-armed conditional (`if`/`case` cascade) over other columns within a service query |
| `query:date-arithmetic` | Date arithmetic inside a query body (`adjust`/`dateDiff`/`datePart`), used to express rolling windows and elapsed-time predicates |
| `query:date-comparison` | Filter comparing date values — a bounded window, an inequality against a parameter, or a comparison of two date properties |
| `query:date-part-comparison` | `Service` query reducing a timestamp to its date portion before comparing it — day equality against a date parameter, or explicit extraction followed by a bound — and projecting the derived portion as its own column beside the raw instant |
| `query:date-part-extraction` | Projection extracting a component of a date (year, month, day) as its own column |
| `query:date-range-comparison` | Query predicate comparing a date column against a two-sided date range |
| `query:date-today` | Query taking `today()` as its date reference, so the result depends on execution time rather than on a supplied parameter |
| `query:deep-property-path` | Query projecting property paths that walk several hops through a nested object graph, with the deeper hops resolving for some rows and falling away to no value for others |
| `query:distinct` | `distinct()` applied to a tabular query result |
| `query:enum-comparison` | Query predicate comparing navigated properties against constants drawn from closed enumerated value sets, with several distinct enumerations in one predicate |
| `query:filter` | Query stage restricting the mapped extent before projection, with predicates over set membership and over an emptiness-or-equality guard on an optional property |
| `query:from-with-dynamic-model-connection` | `from()` supplying a model connection constructed in the query body, so the execution source is chosen at query time rather than bound by a stored runtime |
| `query:function-projection-delegation` | Projection delegating column derivation to a named function, so the projected expression is defined once and reused across endpoints |
| `query:group-by` | Query stage grouping projected rows by key columns and reducing the remaining columns with aggregate functions |
| `query:in-query-runtime-binding` | Runtime named inside the query body through `from()`, so the execution source is selected by the query rather than by the enclosing execution block |
| `query:join` | Query assembling its result by joining two independently projected tabular results on a shared column with an explicit join type, rather than by navigating a mapped property through a store join |
| `query:join-traversal` | Query body reaching related instances by property path rather than by an explicit join operation — up to two hops, in the projection and again in the predicate — so the platform introduces the joins implicitly from the mapping's bindings and the to-many hop multiplies rows, which is what the de-duplication stage downstream then collapses; the taxonomy names only the explicit query join operation and the mapping-side multi-hop join path. Pending in the canonical `emit.md` §6.2 taxonomy |
| `query:let-binding` | Local variable bindings opening a query body, evaluated before the extent is touched and reused by several later predicates and projected columns, so the body is a multi-statement sequence rather than a single expression |
| `query:let-bound-procedural-body` | Query body composed of `let` statements ahead of the returned expression, so intermediate values are named and reused rather than inlined |
| `query:limit` | Row-count cap closing a query body with no ordering stage before it, so the plan truncates the projected result to a fixed width and the returned rows are any subset of the matching set of that size rather than a determined one |
| `query:literal-group-key` | Aggregation keyed partly on a column whose value is a fixed literal token rather than a store-backed measure, so every grouped row carries the provenance label of the run that produced it and the group shape is decided by a column with no store counterpart. Pending in the canonical `emit.md` §6.2 taxonomy |
| `query:match-dispatch` | `match([...])` in a shared function dispatching on the multiplicity of its argument, each arm holding its own nested conditional, so the executed branch is chosen by the resolved type/multiplicity of the value rather than by a predicate over it |
| `query:membership-test` | Predicate testing whether a value belongs to an authored collection of literals (`in`/`contains`) instead of comparing against a single constant, used to classify inputs against closed code sets |
| `query:multi-statement-body` | Multi-expression query body: several statements evaluated in sequence with the last supplying the result, so a single endpoint carries procedural structure |
| `query:nested-property-path` | Filter or projection navigating a multi-segment property path, so the value is reached through intermediate objects rather than off the root |
| `query:parameterless-literal-filter` | Filter comparing only against literals, so the endpoint takes no parameters and the restriction is fixed at declaration time |
| `query:post-projection-filter` | Predicate applied to the projected tabular result rather than to the mapped extent, reading a projected column by its output label, so the plan filters after projection and the predicate sees only the columns the projection emitted |
| `query:project` | Query stage projecting navigated properties into a labeled tabular result |
| `query:relation-extend` | `DataProduct` access point (and function) relation query adding derived columns with an `extend` step whose column expression is compound: a null/emptiness guard selecting between a case-folded, substring-replaced source column and a literal default. Relation-level derived column rather than a class property transform |
| `query:relation-select` | `select()` in relation grammar, narrowing a relation value to a named column set |
| `query:semi-structured-extraction` | Published access-point query whose width is decided by the query rather than by any declared schema: a document-valued column is parsed, flattened and then mined for one named path per projected column — including nested paths reached by successive key lookups with explicit type coercions — so dozens of scalar columns exist only as extractions out of a semi-structured payload |
| `query:slice` | `slice()` pagination over an ordered result, returning a bounded window of rows by position |
| `query:sort` | Ordering applied to a query result |
| `query:sort-limit` | Query body ordering its result with `sort(...)` and bounding it with `limit(...)`/`take(...)` |
| `query:string-function` | String shaping inside query-side helper functions — left-padding to a fixed width, substring extraction and character replacement — called from standalone functions that feed a projection rather than from property mappings |
| `query:string-substitution` | Projection substituting substrings of a column value, so the emitted string is rewritten in the query |
| `query:subtype-cast` | Query narrowing an instance to a declared subtype with an explicit subtype cast before navigating the specialization's own properties |
| `query:tabular-select` | Column selection / restriction over a tabular data set |
| `query:tds-enum-access` | Enumerated column of an already-projected tabular result read back as an enumeration value (`getEnum('<column>')`) inside a post-projection predicate, so the enumeration comparison happens against the tabular row rather than against the class property. Pending in the canonical `emit.md` §6.2 taxonomy |
| `query:tds-extend` | A computed column added onto an existing tabular result instead of re-projected, so later restriction and de-duplication stages operate over a column that exists only in the result and has no store counterpart |
| `query:tds-join` | Explicit join of two already-projected tabular results on named correlation columns, correlating rows outside the model's association graph: the correlation columns must survive both projections and the declared join kind decides whether unmatched rows survive |
| `query:tds-restrict` | Tabular stage narrowing an already-projected result to a subset of its columns, placed beside de-duplication so the retained columns — not the originally projected set — define row identity downstream |
| `query:type-reassertion` | Cast re-asserting the declared type of an already-typed value, so a widened or inferred type is pinned back to the expected one before use |
| `query:union` | Query combining two independently filtered projections of the same class into one result (`concatenate(...)`), then de-duplicating the union rather than filtering a single extent |
| `query:window` | Relation window stage partitioning a projected relation and ranking rows within each partition (`extend(~col->over(~sortCol->descending()), ~Rank: {p, w, r \\| $p->rank($w, $r)})`), `copper-harbor-2`, so the derived column depends on sibling rows rather than on the row alone |

#### Connection — connections, datasources and authentication

How a model reaches its store: the connection element, its datasource specification and its authentication
strategy, and the post-processors that rewrite generated SQL on the way.

| Feature | Description |
|---|---|
| `connection:db2` | Relational connection of type `DB2` |
| `connection:delegated-kerberos-authentication` | `DelegatedKerberos` authentication strategy naming the server principal the connection authenticates against |
| `connection:embedded-seed-data` | Embedded-database connection whose datasource specification carries its fixture rows inline as tabular text (schema, table, header, rows), so connecting both provisions and populates the database and every execution starts from a fixed dataset |
| `connection:h2` | Relational connection of type `H2` |
| `connection:heterogeneous-vendor-pair` | Two connections over one store declaring different database vendors, so the same mapping plans against two SQL dialects depending on which connection the runtime resolves |
| `connection:local-embedded-test-data-setup` | `PackageableConnection` over a local embedded database whose datasource carries an inline ordered setup-statement list — schema creation, a defensive relation drop, a relation creation restating the store's column types, and the row inserts — with a local default authentication strategy instead of a brokered credential. The statements run before every query, so the connection is self-seeding and reproducible without external population, and the physical types the query executes against come from the setup list rather than from the store declaration |
| `connection:mapper-postprocessor` | Mapper (table/schema remapping) query post-processor |
| `connection:oauth-authentication` | OAuth authentication strategy on a connection |
| `connection:quote-identifiers` | `quoteIdentifiers: true` on a relational connection |
| `connection:snowflake` | Snowflake-backed `RelationalDatabaseConnection` |
| `connection:snowflake-public-key-authentication` | Snowflake `RelationalDatabaseConnection` authenticated by the `SnowflakePublic` key-pair strategy: a public user name together with vault references for the private key and its pass phrase, so the warehouse session is opened without a password or token exchange |
| `connection:snowflake-vps-specification` | Snowflake VPS (`accountType: VPS`) datasource specification |
| `connection:static-datasource` | `Static` datasource specification naming the host, port and database of the connection directly rather than a cloud-warehouse specification |
| `connection:sub-select-cte-postprocessor` | `ExtractSubQueriesAsCTEsPostProcessor` on a relational connection, rewriting repeated sub-selects in the generated statement into common table expressions so a query whose grouped view is referenced more than once materializes it once |
| `connection:sybase` | Sybase-backed `RelationalDatabaseConnection` |

#### Data quality — data-quality validations

| Feature | Description |
|---|---|
| `data-quality:relation-validation` | `DataQualityRelationValidation` with row-level and/or aggregate assertions |

#### Complexity

Complexity is determined by **domain-crossing depth** — how many distinct pipeline
domains are exercised by non-scaffolding features:

| Complexity | Criteria | EMIT value proposition |
|---|---|---|
| **basic** | 1–2 domains | Single capability or simple two-domain interaction through the full pipeline |
| **intermediate** | 3–4 domains | Capabilities from multiple domains interacting |
| **advanced** | 5+ domains | Broad multi-domain composition or cross-store scenarios |

Every domain but `scaffolding` counts, the `connection` and `data-quality` domains included.

#### Evolving the Taxonomy

When harvesting models from a new Studio project or adding a test for a new engine
feature, you may encounter capabilities that do not map to any existing entry. In
that case:

1. Add the new `domain:capability` entry to the appropriate table above.
2. Include the taxonomy update in the same PR as the new EMIT test.
3. If a new *domain* is needed (e.g., `data-quality`), add a new
   subsection and update the complexity-scoring guidance accordingly.

**Pick the domain by concern, not by convenience, and reuse before you invent.**
Two rules keep the matrix meaningful:

- A capability that is store-agnostic gets **one** entry reused by every store
  flavor. `mapping:enumeration-mapping` and `mapping:mapping-include` cover the
  relational *and* relation-function forms; do not mint
  `grammar:enumeration-mapping` / `grammar:mapping-include` alongside them.
- A capability with a dedicated domain belongs in that domain. Milestoning is
  `milestoning:*` — tagging a temporal model `grammar:milestoning` hides it from
  the milestoning coverage roll-up even though the test genuinely exercises it.

Because an untaxonomised or mis-domained tag makes a covered capability read as a
gap, coverage reports are only as accurate as this table. Adding the entry in the
same PR is what keeps the two in sync.

### 6.3 Catalog Index

Today the catalog metadata is captured by `EMITModelDescriptor` (under
`org.finos.legend.engine.test.emit.catalog`), which is parsed from each
`*.emit.yaml` and carries the descriptor fields plus the resolved primary /
dependency file paths.

Descriptor discovery is implemented in `EMITModelDiscovery` (in
`legend-engine-emit`), which exposes two parallel families of `find…`
methods:

- **Filesystem** — `findEmitYamls(Path)` walks a single directory tree
  for `*.emit.yaml`. Used by the maven plugin's
  `parseDescriptorsUnder(...)` helper: the plugin's own walker
  (`collectEmitModelsDirs`) applies the exclusion / inclusion rules and
  hands each recorded `emit-models/` directory to `findEmitYamls(Path)`
  to enumerate the yamls inside. Also usable directly by any tool that
  already knows which directory to scan.
- **Classpath** — `findEmitYamls(String)` /
  `findEmitYamls(ClassLoader, String)` enumerate every yaml under the
  given classpath root across all loaded jars; `findEmitYaml(String,
  String)` / `findEmitYaml(ClassLoader, String, String)` resolve a single
  yaml by name. Used by `EMITTestSuiteBuilder` so that the JUnit
  `@TestFactory` picks up models from a module's own
  `src/test/resources/emit-models/` and from any test-scoped jar that
  contains the same layout.

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
- **Catalog search CLI**: A `emit search --feature relational-mapping --complexity basic`
  command that filters the same descriptor set the HTML dashboard (§5.4) renders.
  The dashboard's client-side filter bar already covers the in-browser case; a
  scriptable CLI would help CI workflows and offline browsing.
- **Auto-derived metadata**: Introspect `PureModelContextData` to supplement `emit.yaml` tags.
- **Catalog completeness CI check**: Validate that every model has `emit.yaml`, required fields
  are present, and feature tags come from the controlled vocabulary.
- **Model diff testing**: Compare EMIT results between two model versions.
- **Performance benchmarking**: Track phase durations over time.
