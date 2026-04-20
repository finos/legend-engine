# Contributor Workflow Guide

This guide covers the three most common extension tasks in `legend-engine`:

1. [Adding a new grammar section](#1-adding-a-new-grammar-section)
2. [Adding a new store extension](#2-adding-a-new-store-extension)
3. [Adding a new function activator](#3-adding-a-new-function-activator)

Before starting, ensure you can build the project cleanly —
see [Getting Started](getting-started.md).

---

## 1. Adding a New Grammar Section

A grammar section adds a new `###SectionType` block to the Legend grammar language.
Use this when introducing a new kind of `PackageableElement` (e.g. a new DSL).

### Which compiler pipeline do you need?

There are **two distinct compilation paths** in the Legend stack. Choosing the wrong one is
the most common early mistake for new contributors:

| | **Alloy compiler** (this guide) | **`legend-pure` compiler** |
|---|---|---|
| **What it compiles** | User-authored `PackageableElement` types: stores, connections, mappings, runtimes, services, function activators, custom DSLs that users create in Studio or via the grammar API | Pure language constructs: new metamodel types, native functions, standard-library additions, platform-level abstractions |
| **Where it lives** | `legend-engine` — `CompilerExtension` + `Processor<T>` | `legend-pure` — `CompiledExtension` + generated Java |
| **Input** | JSON protocol POJOs (`PureModelContextData`) at request time | `.pure` source files compiled once at build time into pre-shipped JARs |
| **Who writes it** | Contributors extending `legend-engine` with new user-facing features | Contributors to the `legend-pure` project itself |
| **Example** | Adding `###Snowflake` support, a new `FunctionActivator` type, a new connection kind | Adding a new collection function like `groupBy`, or a new stereotype |

**Use the Alloy path (this guide) if:** the new element is something a user would write in
Legend Studio or the grammar API, it needs to be stored in a `PureModelContextData` snapshot,
and it corresponds to a user-authored domain artefact (a store connection config, a deployment
target, a custom DSL block).

**Use the `legend-pure` path instead if:** you are adding a new concept to the Pure language
itself — a new metamodel class in `meta::pure::*`, a native function, or a standard-library
abstraction. See [legend-pure Contributor Workflow — Adding a new DSL extension](https://github.com/finos/legend-pure/blob/main/docs/guides/contributor-workflow.md).

> **Note:** Many features require *both* paths — a `legend-pure` metaclass (the M2 type) and
> a `legend-engine` `CompilerExtension` (the Alloy compiler support for user-authored instances
> of that type). The `legend-pure` step is the prerequisite; complete it first.

---

### Step-by-step

> **Prerequisite:** Before writing the grammar, ensure the Pure metamodel type your element
> compiles into already exists in `legend-pure`. If it does not, add it there first — see
> [legend-pure Contributor Workflow — Adding a new DSL extension](https://github.com/finos/legend-pure/blob/main/docs/guides/contributor-workflow.md).
> The steps below assume you have already identified the `Root_meta_..._T` Pure type
> that your protocol POJO will compile to.

**1. Create the module structure** (mirror an existing `-grammar` module):

```text
legend-engine-xts-myfeature/
  legend-engine-xt-myfeature-grammar/
    src/main/antlr4/.../MyFeatureLexerGrammar.g4
    src/main/antlr4/.../MyFeatureParserGrammar.g4
    src/main/java/.../grammar/from/MyFeatureSectionParser.java
    src/main/java/.../grammar/to/MyFeatureSectionComposer.java
    src/main/resources/META-INF/services/
      org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension
      org.finos.legend.engine.language.pure.grammar.to.extension.PureGrammarComposerExtension
```

**2. Write the ANTLR4 grammar** (`.g4` files).
Look at `DomainParserGrammar.g4` for a comprehensive example.

**3. Write the `SectionParser`** — implement `PureGrammarParserExtension`:

```java
public class MyFeatureGrammarParserExtension implements PureGrammarParserExtension {
    @Override
    public List<Function<SectionSourceCode, SectionParser>> getExtraSectionParsers() {
        return Lists.immutable.with(MyFeatureSectionParser::new);
    }
}
```

**4. Write the `SectionComposer`** — implement `PureGrammarComposerExtension`.

**5. Register in `META-INF/services/`** — add the FQCN of your extension class to each service file.

**6. Add a round-trip test**:

```java
String grammar = "###MyFeature\n MyFeature my::Feature { ... }";
PureModelContextData parsed = PureGrammarParser.newInstance().parseModel(grammar);
String recomposed = PureGrammarComposer.newInstance().renderPureModelContextData(parsed);
assertEquals(grammar, recomposed);
```

**7. Add your module to `pom.xml`** in the root and in the server/extensions collection POMs.

---

## 2. Adding a New Store Extension

A store extension adds a new data source type (relational dialect, NoSQL, REST service, etc.)
that can be used in mappings and runtimes.

> **legend-pure:** [Contributor Workflow — Adding a new store connector](https://github.com/finos/legend-pure/blob/main/docs/guides/contributor-workflow.md)
> covers the base Pure-side store infrastructure (`StoreContract`, `StoreQuery`, `SetImplementation`)
> that your extension builds on. The steps below cover the Java execution layer; the Pure metamodel
> and grammar layers are documented there.

### Anatomy of a store extension

A complete store extension spans several layers:

| Layer | What to implement |
|-------|------------------|
| **Protocol** | Java POJOs for the connection spec, store element, mapping elements |
| **Grammar** | ANTLR4 grammar + `SectionParser` / `SectionComposer` |
| **Compiler** | `CompilerExtension` with `Processor<MyStoreElement>` |
| **Pure** | `StoreContract` — routing strategy and plan-generation logic |
| **Executor** | `StoreExecutorBuilder` + `StoreExecutor` — runtime execution |

### Step-by-step

**1. Protocol POJOs** — create versioned Jackson-annotated classes in a `-protocol` module.
Use `@JsonTypeInfo` / `@JsonSubTypes` for polymorphic types.

**2. Grammar** — follow the grammar section guide above.

**3. Compiler extension**:

```java
public class MyStoreCompilerExtension implements CompilerExtension {
    @Override
    public Iterable<? extends Processor<?>> getExtraProcessors() {
        return Lists.immutable.with(
            Processor.newProcessor(MyStoreElement.class, this::processMyStoreElement)
        );
    }
    private Root_meta_my_store_MyStoreElement processMyStoreElement(
            MyStoreElement element, CompileContext context) {
        // build Pure metamodel object from protocol POJO
    }
}
```

Register in `META-INF/services/org.finos.legend.engine.language.pure.compiler.toPureGraph.extension.CompilerExtension`.

**4. Pure StoreContract** — in a `-pure` module, implement `meta::pure::store::StoreContract`:

```pure
function meta::mystore::contract(): meta::pure::store::StoreContract[1]
{
  ^StoreContract(
    id = 'myStore',
    planExecution = meta::mystore::planExecution_StoreQuery_1__RoutedValueSpecification_0_1__Mapping_1__Runtime_1__ExecutionContext_1__Extension_MANY__DebugContext_1__Result_1_,
    planGenerationExtensions = ...
  )
}
```

**5. Executor**:

```java
public class MyStoreExecutorBuilder implements StoreExecutorBuilder {
    @Override
    public StoreType getStoreType() { return StoreType.MY_STORE; }
    @Override
    public StoreExecutor build(StoreExecutorConfiguration config) {
        return new MyStoreExecutor((MyStoreExecutorConfiguration) config);
    }
}
```

Register in `META-INF/services/org.finos.legend.engine.plan.execution.stores.StoreExecutorBuilder`.

**6. Tests** — add PCT tests if your store supports standard Pure functions. Add unit tests
for the compiler, grammar round-trip, and executor.

---

## 3. Adding a New Function Activator

A function activator deploys a Pure function to an external platform. Use this to add
support for a new deployment target (e.g. a new cloud function platform).

### Key interfaces

- `FunctionActivator` — Pure class (in `meta::external::function::activator`) that your
  activator extends; carries the function reference and deployment config.
- `FunctionActivatorDeploymentManager` — Java interface for the deployment logic.
- `FunctionActivatorArtifact` — Java class representing the deployable artifact.

### Step-by-step

**1. Define the Pure metamodel** — extend `FunctionActivator`:

```pure
Class meta::myplatform::activator::MyPlatformFunctionActivator
    extends meta::external::function::activator::FunctionActivator
{
  deploymentConfig: MyPlatformDeploymentConfig[1];
}
```

**2. Grammar** — add a grammar section to parse `MyPlatformFunctionActivator { ... }`.

**3. Protocol** — add Java POJOs mirroring the Pure metamodel.

**4. Compiler** — add a `Processor<MyPlatformFunctionActivator>` in a `CompilerExtension`.

**5. Deployment manager** — implement `FunctionActivatorDeploymentManager`:

```java
public class MyPlatformDeploymentManager
        implements FunctionActivatorDeploymentManager<MyPlatformDeploymentConfig, MyPlatformArtifact> {
    @Override
    public MyPlatformArtifact generate(PureModel pureModel, MyPlatformFunctionActivator activator, ...) {
        // Generate the artifact (SQL, JAR, container image, etc.)
    }
    @Override
    public DeploymentResult deploy(MyPlatformArtifact artifact, MyPlatformDeploymentConfig config) {
        // Call the external platform API
    }
}
```

**6. HTTP API** — add a REST resource class (extending or using `FunctionActivatorAPI`)
and register it in the server.

**7. Tests** — unit-test the artifact generation with mock plan; integration-test deployment
against a sandbox environment if one is available.

---

## 4. General Contribution Tips

### Before opening a PR

- [ ] `mvn install -DskipTests` passes locally.
- [ ] `mvn checkstyle:check` passes with zero violations.
- [ ] Grammar changes include a round-trip test.
- [ ] New modules are listed in the root `pom.xml` and in the appropriate
      `legend-engine-extensions-collection-*` aggregator.
- [ ] Copyright header is on all new files.
- [ ] Per-module `README.md` exists (use [template](../templates/module-readme-template.md)).
- [ ] See the full checklist in [Coding Standards](../standards/coding-standards.md#4-pull-request-checklist).

### Understanding the extension wiring

All extension wiring happens in `Server.java`. Search for `ServiceLoader`, `ExtensionLoader`,
and the explicit constructor calls for stores that need configuration injection. If your
extension is not appearing at runtime, verify:

1. The `META-INF/services/` file contains the correct FQCN.
2. The module is on the server classpath (check the `legend-engine-extensions-collection-execution` POM).
3. The `Extension` Pure object is included in the extensions list passed to `PlanGenerator`.
