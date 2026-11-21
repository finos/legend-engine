# Preeval Java Native — P1 Core Traversal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the P0 identity `prevalNative` with a real Java port of preeval's *core traversal*, which covers:
- node dispatch;
- expression-sequence and `let` handling;
- variable substitution;
- `InstanceValue` and `KeyExpression` recursion;
- lambda holders;
- generic-type resolution;
- the stop check;
- full re-activation.

It runs behind a `PrevalRuntime` port with compiled and interpreted adapters. The P1 target subset of `tests.pure` must pass under `JAVA` and `SHADOW`.

**Architecture:**
- **`Preevaluator` (shared module).** A mode-agnostic Java translation of the `preeval.pure` functions `prevalInternal`, `prevalFunctionDefinition` and `prevalGenericFunctionExpression`, minus the P2 rules, plus the scope and generic-type helpers. It reads metamodel nodes through legend-pure's M3 interfaces.
- **`PrevalRuntime` port.** Every mode-specific operation goes through it: re-activation, open variables, node copying and construction, instance checks on engine types, and multiplicities.
- **`PrevalHooks`.** Carries the Pure callbacks, currently `stopPreeval`.
- **Adapters.**
  - `CompiledPrevalRuntime` copies nodes with `CompiledSupport.copy` + typed setters, exactly as compiled Pure translates `^$x(...)`, and re-activates via `Pure.reactivate`.
  - `InterpretedPrevalRuntime` follows legend-pure's interpreted `Copy`, `Reactivate` and `OpenVariableValues` natives.

**Tech Stack:** Java 11, legend-pure 5.105.0 (m3/m4, compiled and interpreted runtimes), Eclipse Collections, Pure, JUnit 5 (plain unit tests), JUnit 4/3 (Pure harness and suite runners).

**Spec:** `docs/superpowers/specs/2026-09-27-preeval-java-native-design.md` (phase **P1** of §5, plus "P1 requirements carried from P0 review").
**Previous plan:** `docs/superpowers/plans/2026-09-27-preeval-java-native-p0-scaffold.md`. P0 is complete on branch `preeval-native`.

## Global Constraints

- JDK 11. Every Maven command is prefixed with `. /home/aziem/bin/jdk11.sh &&`.
- Every Maven command carries `-Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true`. Lifecycle builds add `-T 3` and always use `clean`. Build only the modules you touched (`-pl`).
- Direct Surefire runs (`org.apache.maven.plugins:maven-surefire-plugin:2.22.2:test`) **must** add `-DargLine=`. Without it the fork dies with `Could not find or load main class ${argLine}`.
- Checkstyle: `mvn checkstyle:check -Dcheckstyle.config.location=$(pwd)/checkstyle.xml -pl <modules>`, run from the repo root.
- **TDD RED for Java changes:** run the failing build with `-Dmdep.analyze.skip=true`, so the failure comes from the test rather than from `maven-dependency-plugin` (`failOnWarning=true, ignoreNonCompile=true`). The final GREEN build runs **without** that flag and must be analyzer-clean.
- New files start with the Apache 2.0 header, first line `Copyright 2026 Goldman Sachs`.
- Java: 4-space indent, braces on their own lines (including single-statement `if`), no tabs. Pure: 2-space indent.
- No explanatory comments, tests included. The one exception is the spec's `// parity:` marker for deliberately reproduced Pure quirks, which carries a one-line reason.
- Commits are authored solely by the user: **no** `Co-Authored-By` or `Claude-Session` trailers, ever. Use sentence-case imperative messages. Never `git add -A` / `git add .`. Never bare `git stash`.
- Do not touch the untracked `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/test/java/org/finos/legend/engine/pure/code/core/interpreted/` directory.
- The `core_functions_preeval` Pure repo stays **platform-only**. Pure test snippets in the shared harness may use only `platform` functions: `in`, `contains` and friends are **not** available there, so use `==`, `||`, `forAll`, `exists`, `at`, `toOne`, `size`, `instanceOf` and `cast`.
- The switch default stays `PURE`. Nothing in P1 changes production behaviour.
- **Parity rule (spec §3.4):** port Pure's behaviour exactly, quirks included. Anything that looks like a bug is reproduced and marked `// parity: <reason>`.
- **Non-mutation rule (spec §5):** Java never mutates an input node. Every change goes through a `PrevalRuntime.with…` method that copies first.
- **P2 boundary:** do not implement the P2 rules. The following are all P2:
  - the `if` handler;
  - the `and`/`or` handler;
  - function inlining;
  - `eval` expansion;
  - every `notPrevalReason` handler (TDS `columns`, eval-on-Column, map/fold unroll, concatenate, cast-of-empty, filter-false/true, toOne, toOneMany, genericType).

  If a P1 target test turns out to need one of them, move it to the runner's `DEFERRED_TO_P2` set with the reason, and record that in your report.

## Background the implementer needs

- **Pure source being ported:** `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/main/resources/core/pure/router/preeval/preeval.pure`.
  - `prevalInternal`: lines ~140–341.
  - `prevalFunctionDefinition`: ~343–439.
  - `prevalGenericFunctionExpression`: ~472–879. P1 ports only the parameter/generic-type prologue, the stop check, the `notPrevalReason` computation and the final "Performing preval" re-activation branch.
  - Helpers: ~1018–1235.
- **Test suite:** `.../router/preeval/tests.pure`. Each test calls `assertRoundTrip(input[, expected])`.
- **How the switch works (P0):**
  - Pure overloads E and F call `prevalWithImplementation(item, state, extensions, meta::pure::functions::preeval::preevalImplementation())`. `JAVA` calls `prevalJava`, which calls the native `meta::pure::functions::preeval::prevalNative(item, inScopeVars, rollingInScopeVars, hooks, debug)`.
  - `SHADOW` runs both implementations and calls `assertSamePrevalResult`.
  - The implementation comes from system property `legend.engine.preeval.implementation`, read by `org.finos.legend.engine.pure.preeval.PreevalImplementation.current()`.
- **Module family:** `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/`. Short names used below: `SHARED`, `COMPILED`, `INTERPRETED`, `PUREMOD`.
  - `legend-engine-pure-functions-preeval-pure` (`PUREMOD`): repo `core_functions_preeval`.
  - `legend-engine-pure-runtime-java-extension-shared-functions-preeval` (`SHARED`): package `org.finos.legend.engine.pure.preeval`, with test-jar harness `AbstractTestPrevalNative`.
  - `legend-engine-pure-runtime-java-extension-compiled-functions-preeval` (`COMPILED`): `CompiledPreeval`, `natives/PrevalNative`, `natives/PreevalImplementationNative`, `PreevalCompiledExtension`. `CompiledPreeval.preval(Object, PureMap, PureMap, CoreInstance, CoreInstance, ExecutionSupport)` is what generated code calls.
  - `legend-engine-pure-runtime-java-extension-interpreted-functions-preeval` (`INTERPRETED`): `natives/PrevalNative`, `natives/PreevalImplementationNative`, `PreevalInterpretedExtension`. The interpreted engine is at `provided` scope and must stay there.
- **legend-pure API facts**, verified against 5.105.0 jars. If `javap` disagrees with anything here, trust `javap` and record the deviation.
  - **Compiled:**
    - Metamodel objects implement the M3 interfaces (`org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.*`). A top-level lambda literal arrives as `org.finos.legend.pure.generated.PureCompiledLambda`.
    - `org.finos.legend.pure.runtime.java.compiled.generation.processors.support.CompiledSupport.copy(T)` copies a node. The typed `_x(...)` setters then set fields and return `this`. `PureCompiledLambda.copy()` keeps its compiled body, which is exactly what Pure's own `^$lf(...)` does.
    - `Pure.reactivate(ValueSpecification, PureMap, Bridge, ExecutionSupport)` returns `Object`; normalise it with `CompiledSupport.toPureCollection(Object)`.
    - `Pure.evaluate(ExecutionSupport, Function<?>, Bridge, Object...)` returns `Object`. Pass `RichIterable`s for `[*]` parameters.
    - `Pure.getOpenVariables(Function<?>, Bridge)` returns a `PureMap` of `String` → `List`.
    - `Pure.instanceOf(Object, Type, ExecutionSupport)` also works on raw Java primitives.
    - The bridge is `org.finos.legend.pure.generated.CoreGen.bridge`. Build a `List` with `CoreGen.bridge.buildList()._valuesAddAll(values)`.
    - `new PureMap(Maps.mutable.empty())` builds a map.
    - Canonical multiplicities come from `((CompiledExecutionSupport) es).getMetadata("meta::pure::metamodel::multiplicity::PackageableMultiplicity", "meta::pure::metamodel::multiplicity::PureOne")` (likewise `PureZero`).
    - New nodes are built as `new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("Anonymous_NoCounter")`, `new Root_meta_pure_metamodel_multiplicity_Multiplicity_Impl("Anonymous_NoCounter")` and `Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl`. These are in `legend-engine-pure-platform-java`, which is already a dependency.
    - `InstanceValue._values()` holds **raw Java primitives** (`String`, `Long`, `Boolean`, `Double`/`BigDecimal`, `PureDate`).
  - **Interpreted:**
    - A `Map` argument arrives as `InstanceValue{values=[MapCoreInstance]}`. Keys are String CoreInstances (`getName()`); values are `List` instances whose `values` property holds the elements.
    - `ProcessorSupport.newEphemeralAnonymousCoreInstance(path)` is the constructor for transient nodes.
    - The generic copy is legend-pure `Copy.java`: a new ephemeral instance of the same classifier, with every simple property copied, then the chosen properties overridden.
    - Re-activation follows legend-pure `Reactivate.java`: build a `VariableContext` of ephemeral `InstanceValue`s, then call `FunctionExecutionInterpreted.executeValueSpecification(vs, resolvedTypeParameters, resolvedMultiplicityParameters, functionExpressionCallStack, variableContext, profiler, instantiationContext, executionSupport)`. **Note the argument order.**
    - Calling a Pure function follows legend-pure `Filter.java`: `functionExecution.executeFunction(false, FunctionCoreInstanceWrapper.toFunction(fn), args, ...)`, with args built by `ValueSpecificationBootstrap.wrapValueSpecification(...)` and the context from the protected `NativeFunction.getParentOrEmptyVariableContextForLambda(variableContext, fn)`.
    - Open variables follow legend-pure `OpenVariableValues.java`: `LambdaWithContext.getVariableContext().getValue(name)`.
    - `Instance.instanceOf(ci, path, ps)` does type checks.
    - `Multiplicity.newMultiplicity(int, ps)` builds an exact multiplicity; `ps.package_getByUserPath(M3Paths.PureOne)` gives the canonical one.
    - Results go back to Pure via `ValueSpecificationBootstrap.wrapValueSpecification(result, false, ps)`.
    - A `NativeFunction` receives `(FunctionExecutionInterpreted, ModelRepository)` in its constructor. **Store both.**

## P1 target tests (the exit criterion)

The subset of `meta::pure::router::preeval::tests` whose expected output needs only core mechanisms. The runner enables them in slices:

| Slice | Enabled in | Tests |
|---|---|---|
| `SKELETON` | Task 4 | testPrerouting5, testFilterNoSimplification, testToOneManyElimination2, testToOneElimination2, testFromWith, testFromStopFunctions, testPreroutingRemoveUnnecessaryStatements, testLambdaParamOverride, testPrerouting_parameterAssignedToVariable, testPrerouting9, testLambdaParamOverride2 |
| `REACTIVATION` | Task 6 | testPrerouting1, testPrerouting2, testPrerouting3, testPrerouting4, testPrerouting39, testPrerouting_PropertyValue, testSchemaStateUnfurl, testToOneManyElimination1, testToOneElimination1, testPrerouting36, testFilterFalseSimplificationAll, testPrerouting27a |
| `LAMBDA_HOLDERS` | Task 7 | testRelationAggregation, testPrerouting_constantInAgg, testPrerouting_mappedModelAgg |

The classification came from static analysis. If a listed test fails only because it needs a P2 rule, move it to `DEFERRED_TO_P2` with the reason. If a test not on the list passes, leave it off; P2 owns the full suite.

## File Structure

```
SHARED  src/main/java/org/finos/legend/engine/pure/preeval/
  PreevalImplementation.java        MODIFY (T1): cache the parsed property by raw value
  PrevalState.java                  CREATE (T3): immutable traversal state
  PrevalResult.java                 MODIFY (T3): value becomes Object; helpers
  Scope.java                        CREATE (T3): areAllInScope / openVars / resolveVariable
  PrevalRuntime.java                CREATE (T3): the mode port
  PrevalHooks.java                  CREATE (T3): Pure callbacks (stopPreeval)
  MetamodelPaths.java               CREATE (T3): engine/metamodel type and function paths
  Preevaluator.java                 MODIFY (T4, T6, T7, T8): the traversal
  GenericTypes.java                 CREATE (T4): resolveGenericType / resolveFunctionType
  LambdaHolders.java                CREATE (T7): BCS / AggregateValue / AggColSpec(Array)
  DebugTrace.java                   CREATE (T4): SLF4J trace gated by DebugContext.debug
  PrevalResults.java                DELETE (T4): result construction moves into the adapters
SHARED  src/test/java/org/finos/legend/engine/pure/preeval/
  TestPreevalImplementation.java    MODIFY (T1)
  TestPrevalState.java              CREATE (T3)
  TestScope.java                    CREATE (T3)
  AbstractTestPrevalNative.java     MODIFY (T4–T8): harness scenarios for both modes
COMPILED src/main/java/org/finos/legend/engine/pure/preeval/compiled/
  CompiledPreeval.java              MODIFY (T4, T8)
  CompiledPrevalRuntime.java        CREATE (T4, extended T6/T7)
  CompiledPrevalHooks.java          CREATE (T4)
  natives/PrevalNative.java         MODIFY (T8): pass source information
INTERPRETED src/main/java/org/finos/legend/engine/pure/preeval/interpreted/
  InterpretedPrevalRuntime.java     CREATE (T5, extended T6/T7)
  InterpretedPrevalHooks.java       CREATE (T5)
  natives/PrevalNative.java         MODIFY (T5, T8)
compiled-core
  src/main/resources/core/pure/router/preeval/preeval.pure                  MODIFY (T2)
  src/main/resources/core/pure/router/preeval/testImplementationSwitch.pure MODIFY (T2)
  src/test/java/org/finos/legend/engine/pure/code/core/Test_Pure_Preeval_Java.java  CREATE (T4, extended T6/T7)
docs/superpowers/specs/2026-09-27-preeval-java-native-design.md  MODIFY (T9)
```

---

### Task 1: Validate the implementation switch once per property value

**Files:**
- Modify: `SHARED/src/main/java/org/finos/legend/engine/pure/preeval/PreevalImplementation.java`
- Test: `SHARED/src/test/java/org/finos/legend/engine/pure/preeval/TestPreevalImplementation.java`

**Interfaces:**
- Produces: `PreevalImplementation.current()`, same signature. It re-parses only when the raw property string changes, so tests can still switch implementations between suites.

- [ ] **Step 1: Add characterization tests for `current()`**

Append to `TestPreevalImplementation`:
```java
    @Test
    public void testCurrentFollowsSystemProperty()
    {
        withProperty("java", () -> Assertions.assertEquals(PreevalImplementation.JAVA, PreevalImplementation.current()));
        withProperty(null, () -> Assertions.assertEquals(PreevalImplementation.PURE, PreevalImplementation.current()));
        withProperty("SHADOW", () -> Assertions.assertEquals(PreevalImplementation.SHADOW, PreevalImplementation.current()));
    }

    @Test
    public void testCurrentRejectsUnknownValueEveryTime()
    {
        withProperty("FAST", () ->
        {
            Assertions.assertThrows(IllegalArgumentException.class, PreevalImplementation::current);
            Assertions.assertThrows(IllegalArgumentException.class, PreevalImplementation::current);
        });
    }

    private static void withProperty(String value, Runnable body)
    {
        String previous = System.getProperty(PreevalImplementation.SYSTEM_PROPERTY);
        try
        {
            if (value == null)
            {
                System.clearProperty(PreevalImplementation.SYSTEM_PROPERTY);
            }
            else
            {
                System.setProperty(PreevalImplementation.SYSTEM_PROPERTY, value);
            }
            body.run();
        }
        finally
        {
            if (previous == null)
            {
                System.clearProperty(PreevalImplementation.SYSTEM_PROPERTY);
            }
            else
            {
                System.setProperty(PreevalImplementation.SYSTEM_PROPERTY, previous);
            }
        }
    }
```

- [ ] **Step 2: Run them; they pass against the current uncached implementation (baseline)**

```bash
. /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval
```
Expected: `Tests run: 6, Failures: 0`. These tests pin behaviour before the refactor, so there is no RED step.

- [ ] **Step 3: Cache by raw value**

Replace `current()` in `PreevalImplementation` and add the nested holder. Keep `parse` unchanged.
```java
    private static volatile Resolved resolved = new Resolved(null, PURE);

    public static PreevalImplementation current()
    {
        String raw = System.getProperty(SYSTEM_PROPERTY);
        Resolved cached = resolved;
        if (!Objects.equals(cached.raw, raw))
        {
            cached = new Resolved(raw, parse(raw));
            resolved = cached;
        }
        return cached.implementation;
    }

    private static final class Resolved
    {
        private final String raw;
        private final PreevalImplementation implementation;

        private Resolved(String raw, PreevalImplementation implementation)
        {
            this.raw = raw;
            this.implementation = implementation;
        }
    }
```
Add `import java.util.Objects;`.

- [ ] **Step 4: Run the tests again**

Same command as Step 2. Expected: `Tests run: 6, Failures: 0`, `BUILD SUCCESS`, analyzer clean.

- [ ] **Step 5: Commit**

```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval/src
git commit -m "Parse the preeval implementation property once per value"
```

---

### Task 2: SHADOW compares types and multiplicities; JAVA rejects unsupported State

**Files:**
- Modify: `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/main/resources/core/pure/router/preeval/preeval.pure`: `prevalJava` and `describePrevalResult` in the "Implementation Switch" section
- Test: `.../core/pure/router/preeval/testImplementationSwitch.pure`

**Interfaces:**
- Consumes: `prevalWithImplementation`, `assertSamePrevalResult`, `getPreevalStateWithAdditionalStopInlineFunc` (P0).
- Produces:
  - `describePrevalResult` now also includes the value's type and multiplicity.
  - `prevalJava` fails with `The Java preeval implementation requires a State with no type parameters, path or depth` when the State carries any of those.

- [ ] **Step 1: Write the failing Pure tests**

Append to `testImplementationSwitch.pure`:
```pure
function <<test.Test>> meta::pure::router::preeval::tests::implementationSwitch::testShadowRejectsDifferentTypes():Boolean[1]
{
  let integerOne = ^InstanceValue(genericType = ^GenericType(rawType = Integer), multiplicity = PureOne, values = 1);
  let numberOne = ^InstanceValue(genericType = ^GenericType(rawType = Number), multiplicity = PureOne, values = 1);
  assertError(
    | assertSamePrevalResult(
        ^PrevalWrapper<Any>(value = $integerOne, canPreval = true, openVars = [], modified = false),
        ^PrevalWrapper<Any>(value = $numberOne, canPreval = true, openVars = [], modified = false),
        newMap([]->cast(@Pair<String, List<Any>>)),
        []),
    {message:String[1], source:SourceInformation[0..1] | assert($message->startsWith('preeval SHADOW mismatch'), |$message)}
  );
}

function <<test.Test>> meta::pure::router::preeval::tests::implementationSwitch::testShadowRejectsDifferentMultiplicities():Boolean[1]
{
  let one = ^InstanceValue(genericType = ^GenericType(rawType = Integer), multiplicity = PureOne, values = 1);
  let zeroMany = ^InstanceValue(genericType = ^GenericType(rawType = Integer), multiplicity = ZeroMany, values = 1);
  assertError(
    | assertSamePrevalResult(
        ^PrevalWrapper<Any>(value = $one, canPreval = true, openVars = [], modified = false),
        ^PrevalWrapper<Any>(value = $zeroMany, canPreval = true, openVars = [], modified = false),
        newMap([]->cast(@Pair<String, List<Any>>)),
        []),
    {message:String[1], source:SourceInformation[0..1] | assert($message->startsWith('preeval SHADOW mismatch'), |$message)}
  );
}

function <<test.Test>> meta::pure::router::preeval::tests::implementationSwitch::testJavaImplementationRejectsStateWithDepth():Boolean[1]
{
  let f = {p:Integer[1] | $p};
  let state = getPreevalStateWithAdditionalStopInlineFunc($f->openVariableValues(), [], []);
  assertError(
    | prevalWithImplementation($f->evaluateAndDeactivate(), ^$state(depth = 1), [], 'JAVA'),
    {message:String[1], source:SourceInformation[0..1] | assert($message->contains('requires a State with no type parameters, path or depth'), |$message)}
  );
}
```

- [ ] **Step 2: Build and run; confirm they fail**

```bash
. /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -DskipTests -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core \
&& mvn -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -DargLine= \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core \
  org.apache.maven.plugins:maven-surefire-plugin:2.22.2:test -Dtest=Test_Pure_Preeval
```
Expected: the 3 new tests fail. The type and multiplicity tests expected an error that was never raised. The depth test got no error because JAVA returned the input.

- [ ] **Step 3: Implement**

In `prevalJava`, make this the first statement:
```pure
  assert($state.inScopeTypeParams->keys()->isEmpty() && $state.path->isEmpty() && $state.depth->isEmpty(), | 'The Java preeval implementation requires a State with no type parameters, path or depth');
```
Replace the body of `describePrevalResult` with:
```pure
  let value = $r.value->match([
    fd:FunctionDefinition<Any>[1] | $fd->meta::protocols::pure::vX_X_X::transformation::fromPureGraph::valueSpecification::transformFunctionBody($extensions)->meta::json::toJSON(50000),
    vs:ValueSpecification[1]      | $vs->meta::protocols::pure::vX_X_X::transformation::fromPureGraph::valueSpecification::transformValueSpecification([], $inScopeVars, $extensions)->meta::json::toJSON(50000),
    a:Any[1]                      | $a->toString()
  ]);
  let type = $r.value->match([
    fd:FunctionDefinition<Any>[1] | $fd->functionReturnType()->meta::pure::metamodel::serialization::grammar::printGenericType() + $fd->functionReturnMultiplicity()->meta::pure::metamodel::serialization::grammar::printMultiplicity(),
    vs:ValueSpecification[1]      | $vs.genericType->meta::pure::metamodel::serialization::grammar::printGenericType() + $vs.multiplicity->meta::pure::metamodel::serialization::grammar::printMultiplicity(),
    a:Any[1]                      | ''
  ]);
  'canPreval=' + $r.canPreval->toString() + ', modified=' + $r.modified->toString() + ', openVars=' + $r.openVars->sort()->joinStrings('[', ',', ']') + ', type=' + $type + ', value=' + $value;
```

- [ ] **Step 4: Build and run again**

Same commands as Step 2. Expected: `Test_Pure_Preeval` has `Failures: 0, Errors: 0`, with 111 tests (108 + 3).

- [ ] **Step 5: Commit**

```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/main/resources/core/pure/router/preeval/preeval.pure \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/main/resources/core/pure/router/preeval/testImplementationSwitch.pure
git commit -m "Compare types in SHADOW mode and reject State the Java preeval cannot honour"
```

---

### Task 3: Core value types, scope helpers and the runtime port

**Files:**
- Create: `SHARED/.../PrevalState.java`, `Scope.java`, `PrevalRuntime.java`, `PrevalHooks.java`, `MetamodelPaths.java`
- Modify: `SHARED/.../PrevalResult.java`
- Test: `SHARED/src/test/java/org/finos/legend/engine/pure/preeval/TestPrevalState.java`, `TestScope.java`

**Interfaces:**
- Produces (all in `org.finos.legend.engine.pure.preeval`):
  - `PrevalResult`:
    - `new PrevalResult(Object value, boolean canPreval, ImmutableList<String> openVars, boolean modified)`
    - `static unmodified(Object)`, which has `canPreval=true`
    - `static unmodified(Object, boolean canPreval)`
    - `getValue(): Object`, `canPreval()`, `getOpenVars()`, `isModified()`
    - `markModified(): PrevalResult`
    - `static boolean anyModified(Iterable<PrevalResult>)`
    - `static boolean allCanPreval(Iterable<PrevalResult>)`
  - `PrevalState`:
    - `static PrevalState initial(ImmutableMap<String, ImmutableList<Object>> inScopeVars, ImmutableMap<String, ImmutableList<Object>> rollingInScopeVars, boolean debug)`
    - getters `getInScopeVars()`, `getRollingInScopeVars()`, `getInScopeTypeParams(): ImmutableMap<String, Object>`, `getPath(): ImmutableList<Object>`, `getDepth(): int` (−1 when unset), `isDebug()`
    - `withInScopeVars(...)`, `withRollingInScopeVars(...)`, `withInScopeTypeParams(...)`
    - `deeper(Object functionDefinitionOrNull)`: depth+1, and appends to the path when the argument is non-null
  - `Scope(java.util.function.Function<Object, String> variableNameOf)`:
    - `areAllInScope(ListIterable<String> vars, ImmutableMap<String, ImmutableList<Object>> inScope)`
    - `openVars(ListIterable<String> vars, ImmutableMap<...> inScope): ImmutableList<String>`
    - `resolveVariable(String name, ImmutableMap<...> inScope): String`, which throws `IllegalStateException("Circular variable reference: " + name)`
  - `PrevalHooks`: `boolean stopPreeval(ListIterable<?> values)`.
  - `PrevalRuntime`: the full interface below. Tasks 4–7 implement its methods in both adapters.
  - `MetamodelPaths`: string constants.

- [ ] **Step 1: Write failing unit tests**

`TestPrevalState.java`:
```java
package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestPrevalState
{
    @Test
    public void testInitialStateHasNoDepthPathOrTypeParameters()
    {
        PrevalState state = PrevalState.initial(Maps.immutable.with("x", Lists.immutable.with(1L)), Maps.immutable.empty(), false);
        Assertions.assertEquals(-1, state.getDepth());
        Assertions.assertTrue(state.getPath().isEmpty());
        Assertions.assertTrue(state.getInScopeTypeParams().isEmpty());
        Assertions.assertEquals(Lists.immutable.with(1L), state.getInScopeVars().get("x"));
    }

    @Test
    public void testDeeperIncrementsDepthAndExtendsPathOnlyForFunctionDefinitions()
    {
        Object function = new Object();
        PrevalState state = PrevalState.initial(Maps.immutable.empty(), Maps.immutable.empty(), false);
        PrevalState once = state.deeper(null);
        PrevalState twice = once.deeper(function);
        Assertions.assertEquals(0, once.getDepth());
        Assertions.assertTrue(once.getPath().isEmpty());
        Assertions.assertEquals(1, twice.getDepth());
        Assertions.assertEquals(Lists.immutable.with(function), twice.getPath());
    }

    @Test
    public void testWithMethodsLeaveTheOriginalUnchanged()
    {
        PrevalState state = PrevalState.initial(Maps.immutable.empty(), Maps.immutable.empty(), true);
        PrevalState changed = state.withInScopeVars(Maps.immutable.with("a", Lists.immutable.empty()));
        Assertions.assertTrue(state.getInScopeVars().isEmpty());
        Assertions.assertTrue(changed.getInScopeVars().containsKey("a"));
        Assertions.assertTrue(changed.isDebug());
    }
}
```
`TestScope.java`. Values starting with `$` stand for variable expressions in this test.
```java
package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestScope
{
    private final Scope scope = new Scope(v -> (v instanceof String && ((String) v).startsWith("$")) ? ((String) v).substring(1) : null);

    @Test
    public void testConcreteVariablesAreInScope()
    {
        ImmutableMap<String, ImmutableList<Object>> vars = Maps.immutable.with("a", Lists.immutable.with(1L));
        Assertions.assertTrue(this.scope.areAllInScope(Lists.immutable.with("a"), vars));
        Assertions.assertFalse(this.scope.areAllInScope(Lists.immutable.with("b"), vars));
        Assertions.assertTrue(this.scope.areAllInScope(Lists.immutable.empty(), vars));
    }

    @Test
    public void testVariablesBoundToOtherVariablesFollowTheChain()
    {
        ImmutableMap<String, ImmutableList<Object>> vars = Maps.immutable.with("a", Lists.immutable.with("$b"), "b", Lists.immutable.with(2L));
        Assertions.assertTrue(this.scope.areAllInScope(Lists.immutable.with("a"), vars));
        ImmutableMap<String, ImmutableList<Object>> dangling = Maps.immutable.with("a", Lists.immutable.with("$c"));
        Assertions.assertFalse(this.scope.areAllInScope(Lists.immutable.with("a"), dangling));
    }

    @Test
    public void testSelfReferenceIsNotInScope()
    {
        ImmutableMap<String, ImmutableList<Object>> vars = Maps.immutable.with("a", Lists.immutable.with("$a"));
        Assertions.assertFalse(this.scope.areAllInScope(Lists.immutable.with("a"), vars));
    }

    @Test
    public void testOpenVarsAreDistinctAndExcludeResolvableOnes()
    {
        ImmutableMap<String, ImmutableList<Object>> vars = Maps.immutable.with("a", Lists.immutable.with(1L));
        Assertions.assertEquals(Lists.immutable.with("b", "c"), this.scope.openVars(Lists.immutable.with("b", "a", "c", "b"), vars));
    }

    @Test
    public void testResolveVariableFollowsAliases()
    {
        ImmutableMap<String, ImmutableList<Object>> vars = Maps.immutable.with("a", Lists.immutable.with("$b"), "b", Lists.immutable.with(1L));
        Assertions.assertEquals("b", this.scope.resolveVariable("a", vars));
        Assertions.assertEquals("z", this.scope.resolveVariable("z", vars));
    }

    @Test
    public void testResolveVariableRejectsCircularReference()
    {
        ImmutableMap<String, ImmutableList<Object>> vars = Maps.immutable.with("a", Lists.immutable.with("$a"));
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, () -> this.scope.resolveVariable("a", vars));
        Assertions.assertEquals("Circular variable reference: a", e.getMessage());
    }
}
```

- [ ] **Step 2: Run them; confirm they fail to compile**

```bash
. /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -Dmdep.analyze.skip=true \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval
```
Expected: `COMPILATION ERROR`, `cannot find symbol ... PrevalState` / `Scope`.

- [ ] **Step 3: Implement**

`PrevalState.java`:
```java
package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;

public final class PrevalState
{
    private final ImmutableMap<String, ImmutableList<Object>> inScopeVars;
    private final ImmutableMap<String, ImmutableList<Object>> rollingInScopeVars;
    private final ImmutableMap<String, Object> inScopeTypeParams;
    private final ImmutableList<Object> path;
    private final int depth;
    private final boolean debug;

    private PrevalState(ImmutableMap<String, ImmutableList<Object>> inScopeVars, ImmutableMap<String, ImmutableList<Object>> rollingInScopeVars, ImmutableMap<String, Object> inScopeTypeParams, ImmutableList<Object> path, int depth, boolean debug)
    {
        this.inScopeVars = inScopeVars;
        this.rollingInScopeVars = rollingInScopeVars;
        this.inScopeTypeParams = inScopeTypeParams;
        this.path = path;
        this.depth = depth;
        this.debug = debug;
    }

    public static PrevalState initial(ImmutableMap<String, ImmutableList<Object>> inScopeVars, ImmutableMap<String, ImmutableList<Object>> rollingInScopeVars, boolean debug)
    {
        return new PrevalState(inScopeVars, rollingInScopeVars, Maps.immutable.empty(), Lists.immutable.empty(), -1, debug);
    }

    public ImmutableMap<String, ImmutableList<Object>> getInScopeVars()
    {
        return this.inScopeVars;
    }

    public ImmutableMap<String, ImmutableList<Object>> getRollingInScopeVars()
    {
        return this.rollingInScopeVars;
    }

    public ImmutableMap<String, Object> getInScopeTypeParams()
    {
        return this.inScopeTypeParams;
    }

    public ImmutableList<Object> getPath()
    {
        return this.path;
    }

    public int getDepth()
    {
        return this.depth;
    }

    public boolean isDebug()
    {
        return this.debug;
    }

    public PrevalState withInScopeVars(ImmutableMap<String, ImmutableList<Object>> vars)
    {
        return new PrevalState(vars, this.rollingInScopeVars, this.inScopeTypeParams, this.path, this.depth, this.debug);
    }

    public PrevalState withRollingInScopeVars(ImmutableMap<String, ImmutableList<Object>> vars)
    {
        return new PrevalState(this.inScopeVars, vars, this.inScopeTypeParams, this.path, this.depth, this.debug);
    }

    public PrevalState withInScopeTypeParams(ImmutableMap<String, Object> typeParams)
    {
        return new PrevalState(this.inScopeVars, this.rollingInScopeVars, typeParams, this.path, this.depth, this.debug);
    }

    public PrevalState deeper(Object functionDefinition)
    {
        return new PrevalState(this.inScopeVars, this.rollingInScopeVars, this.inScopeTypeParams, functionDefinition == null ? this.path : this.path.newWith(functionDefinition), this.depth + 1, this.debug);
    }
}
```
`PrevalResult.java` replaces the P0 class. The value becomes `Object`.
```java
package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.impl.utility.Iterate;

public final class PrevalResult
{
    private final Object value;
    private final boolean canPreval;
    private final ImmutableList<String> openVars;
    private final boolean modified;

    public PrevalResult(Object value, boolean canPreval, ImmutableList<String> openVars, boolean modified)
    {
        this.value = value;
        this.canPreval = canPreval;
        this.openVars = openVars;
        this.modified = modified;
    }

    public static PrevalResult unmodified(Object value)
    {
        return unmodified(value, true);
    }

    public static PrevalResult unmodified(Object value, boolean canPreval)
    {
        return new PrevalResult(value, canPreval, Lists.immutable.empty(), false);
    }

    public static boolean anyModified(Iterable<PrevalResult> results)
    {
        return Iterate.anySatisfy(results, PrevalResult::isModified);
    }

    public static boolean allCanPreval(Iterable<PrevalResult> results)
    {
        return Iterate.allSatisfy(results, PrevalResult::canPreval);
    }

    public Object getValue()
    {
        return this.value;
    }

    public boolean canPreval()
    {
        return this.canPreval;
    }

    public ImmutableList<String> getOpenVars()
    {
        return this.openVars;
    }

    public boolean isModified()
    {
        return this.modified;
    }

    public PrevalResult markModified()
    {
        return this.modified ? this : new PrevalResult(this.value, this.canPreval, this.openVars, true);
    }
}
```
`eclipse-collections` is currently `runtime` scope in the shared pom; `Iterate` needs it at compile scope. Change that dependency to compile scope by deleting its `<scope>runtime</scope>` line.

`Scope.java` ports Pure's `areAllInScope`, `openVars` and `resolveVariable` (preeval.pure ~1162–1221).
```java
package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;

import java.util.function.Function;

public final class Scope
{
    private final Function<Object, String> variableNameOf;

    public Scope(Function<Object, String> variableNameOf)
    {
        this.variableNameOf = variableNameOf;
    }

    public boolean areAllInScope(ListIterable<String> vars, ImmutableMap<String, ImmutableList<Object>> inScope)
    {
        if (vars.isEmpty())
        {
            return true;
        }
        MutableList<String> unresolved = Lists.mutable.empty();
        vars.forEach(v ->
        {
            ImmutableList<Object> value = inScope.get(v);
            if (value == null)
            {
                unresolved.add(v);
            }
            else
            {
                String alias = aliasOf(value);
                if (alias != null)
                {
                    unresolved.add(alias);
                }
            }
        });
        ListIterable<String> distinct = unresolved.distinct();
        if (distinct.anySatisfy(vars::contains))
        {
            return false;
        }
        return areAllInScope(distinct.reject(vars::contains), inScope);
    }

    public ImmutableList<String> openVars(ListIterable<String> vars, ImmutableMap<String, ImmutableList<Object>> inScope)
    {
        return vars.distinct().reject(v -> areAllInScope(Lists.immutable.with(v), inScope)).toImmutable();
    }

    public String resolveVariable(String name, ImmutableMap<String, ImmutableList<Object>> inScope)
    {
        ImmutableList<Object> value = inScope.get(name);
        String alias = value == null ? null : aliasOf(value);
        if (alias == null)
        {
            return name;
        }
        if (alias.equals(name))
        {
            throw new IllegalStateException("Circular variable reference: " + name);
        }
        return resolveVariable(alias, inScope);
    }

    private String aliasOf(ImmutableList<Object> value)
    {
        return value.size() == 1 ? this.variableNameOf.apply(value.getOnly()) : null;
    }
}
```
`PrevalHooks.java`:
```java
package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.list.ListIterable;

public interface PrevalHooks
{
    boolean stopPreeval(ListIterable<?> values);
}
```
`MetamodelPaths.java`:
```java
package org.finos.legend.engine.pure.preeval;

public final class MetamodelPaths
{
    public static final String LET_FUNCTION = "meta::pure::functions::lang::letFunction_String_1__T_m__T_m_";
    public static final String CAST_FUNCTION = "meta::pure::functions::lang::cast_Any_m__T_1__T_m_";
    public static final String FUNCTION_TYPE_PROFILE = "meta::pure::profiles::functionType";
    public static final String TDS = "meta::pure::metamodel::relation::TDS";
    public static final String COLUMN_SPECIFICATION = "meta::pure::tds::ColumnSpecification";
    public static final String BASIC_COLUMN_SPECIFICATION = "meta::pure::tds::BasicColumnSpecification";
    public static final String TDS_AGGREGATE_VALUE = "meta::pure::tds::AggregateValue";
    public static final String COLLECTION_AGGREGATE_VALUE = "meta::pure::functions::collection::AggregateValue";
    public static final String AGG_COL_SPEC = "meta::pure::metamodel::relation::AggColSpec";
    public static final String AGG_COL_SPEC_ARRAY = "meta::pure::metamodel::relation::AggColSpecArray";
    public static final String FUNC_COL_SPEC = "meta::pure::metamodel::relation::FuncColSpec";
    public static final String SCHEMA_STATE = "meta::pure::tds::schema::SchemaState";
    public static final String ROOT_GRAPH_FETCH_TREE = "meta::pure::graphFetch::RootGraphFetchTree";
    public static final String BINDING = "meta::external::format::shared::binding::Binding";
    public static final String STORE = "meta::pure::store::Store";
    public static final String RELATION_STORE_ACCESSOR = "meta::pure::store::RelationStoreAccessor";
    public static final String TEST_PARAMETERS = "meta::pure::test::mft::TestParameters";
    public static final String RELATION_ELEMENT_ACCESSOR = "meta::pure::metamodel::relation::RelationElementAccessor";

    private MetamodelPaths()
    {
    }
}
```
`PrevalRuntime.java`. Methods are grouped by the task that first needs them; both adapters implement every method by the end of Task 7. The interface is declared in full now so the core compiles against a stable port.
```java
package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;

public interface PrevalRuntime
{
    boolean isInstanceOf(Object value, String typePath);

    String typeDescription(Object value);

    boolean isFunction(Object function, String functionPath);

    boolean hasStereotype(Object function, String profilePath, String stereotype);

    ImmutableList<String> parameterNames(Object function);

    String stringValue(Object primitive);

    Multiplicity pureOne();

    Multiplicity pureZero();

    Multiplicity exactly(int size);

    ImmutableMap<String, ImmutableList<Object>> openVariableValues(LambdaFunction<?> lambda);

    InstanceValue newInstanceValue(GenericType genericType, Multiplicity multiplicity, ListIterable<?> values);

    <T extends FunctionDefinition<?>> T withExpressionSequence(T function, ListIterable<? extends ValueSpecification> expressionSequence, ListIterable<String> openVariables);

    FunctionExpression withParametersAndGenericType(FunctionExpression expression, ListIterable<? extends ValueSpecification> parameters, GenericType genericType);

    <T extends ValueSpecification> T withGenericType(T valueSpecification, GenericType genericType);

    InstanceValue withValues(InstanceValue instanceValue, ListIterable<?> values, GenericType genericType, Multiplicity multiplicity);

    KeyExpression withExpression(KeyExpression keyExpression, ValueSpecification expression);

    GenericType withTypeArguments(GenericType genericType, ListIterable<? extends GenericType> typeArguments);

    GenericType withRawType(GenericType genericType, FunctionType rawType);

    FunctionType withSignature(FunctionType functionType, ListIterable<? extends VariableExpression> parameters, GenericType returnType);

    ImmutableList<Object> reactivate(ValueSpecification valueSpecification, ImmutableMap<String, ImmutableList<Object>> inScopeVars);

    Object getPropertyValue(Object instance, String property);

    ImmutableList<Object> getPropertyValues(Object instance, String property);

    Object withPropertyValues(Object instance, String property, ListIterable<?> values);

    RuntimeException error(String message);
}
```
Semantics each adapter must honour:
- **`with*` methods:** always return a **copy**. Unlisted properties are copied from the original and the input is never changed.
- **`withExpressionSequence`:** `openVariables == null` means "leave `openVariables` as is"; it is only non-null for lambdas.
- **`exactly(n)`:**
  - 0 returns `pureZero()`;
  - 1 returns `pureOne()`;
  - otherwise a new multiplicity with lower = upper = n.
- **`isInstanceOf`:** returns `false`, not an exception, when `typePath` is not loaded in the current repository set.
- **`isFunction`:** true when `function` is the element at `functionPath`.
- **`reactivate`:** returns the re-activated values, flattened.
- **`error`:** builds a `PureExecutionException` carrying the native call site's source information. The adapters are constructed per native call, so they know it.

- [ ] **Step 4: Run the tests**

Same command as Step 2, without `-Dmdep.analyze.skip=true`. Expected: all shared tests pass (`TestPreevalImplementation` 6, `TestPrevalState` 3, `TestScope` 6), and the analyzer is clean.

`PrevalResults` still compiles against the new `PrevalResult`. It needs one change: `result.getValue()` is now `Object`, so cast it with `(CoreInstance) result.getValue()`. The compiled and interpreted modules must still build unchanged. Check by building the family:
```bash
. /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-compiled-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-interpreted-functions-preeval
```
Expected: `BUILD SUCCESS`, with the compiled and interpreted harness tests at 3/3 each.

- [ ] **Step 5: Commit**

```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval
git commit -m "Add preeval traversal state, scope helpers and the runtime port"
```

---

### Task 4: Traversal skeleton and the compiled adapter

This task covers everything except re-activation and lambda holders, and runs in compiled mode. The interpreted native keeps its P0 identity behaviour until Task 5.

**Files:**
- Create: `SHARED/.../Preevaluator.java` (replaces the P0 identity class), `GenericTypes.java`, `DebugTrace.java`
- Delete: `SHARED/.../PrevalResults.java`
- Create: `COMPILED/.../compiled/CompiledPrevalRuntime.java`, `CompiledPrevalHooks.java`
- Modify: `COMPILED/.../compiled/CompiledPreeval.java`
- Modify: `INTERPRETED/.../natives/PrevalNative.java`: temporary identity, building the result inline (see Step 5)
- Modify: `SHARED/src/test/java/.../AbstractTestPrevalNative.java`: the `HOOKS` `stopPreeval` becomes `true`
- Create: `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/test/java/org/finos/legend/engine/pure/code/core/Test_Pure_Preeval_Java.java`
- Modify: the shared pom, adding `org.slf4j:slf4j-api` (compile scope) for `DebugTrace`.

**Interfaces:**
- Consumes: Task 3 types.
- Produces:
  - `new Preevaluator(PrevalRuntime runtime, PrevalHooks hooks)` with `PrevalResult preval(Object item, PrevalState state)`.
  - `GenericTypes(PrevalRuntime)` with `PrevalResult resolveGenericType(GenericType, PrevalState)` and `FunctionType resolveFunctionType(FunctionType, PrevalState)`.
  - `CompiledPrevalRuntime(ExecutionSupport es)`.
  - `CompiledPrevalHooks(Root_meta_pure_functions_preeval_PrevalHooks hooks, ExecutionSupport es)`.
  - `Test_Pure_Preeval_Java` with static sets `SKELETON`, `REACTIVATION`, `LAMBDA_HOLDERS`, `DEFERRED_TO_P2` and method `enabledTargets()`.

- [ ] **Step 1: Write the failing runner**

`Test_Pure_Preeval_Java.java`:
```java
package org.finos.legend.engine.pure.code.core;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.Function;
import org.finos.legend.pure.m3.execution.test.PureTestBuilder;
import org.finos.legend.pure.m3.execution.test.TestCollection;
import org.finos.legend.pure.runtime.java.compiled.execution.CompiledExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_Pure_Preeval_Java
{
    private static final String PROPERTY = "legend.engine.preeval.implementation";

    static final ImmutableSet<String> SKELETON = Sets.immutable.with(
            "testPrerouting5", "testFilterNoSimplification", "testToOneManyElimination2", "testToOneElimination2",
            "testFromWith", "testFromStopFunctions", "testPreroutingRemoveUnnecessaryStatements", "testLambdaParamOverride",
            "testPrerouting_parameterAssignedToVariable", "testPrerouting9", "testLambdaParamOverride2");
    static final ImmutableSet<String> REACTIVATION = Sets.immutable.empty();
    static final ImmutableSet<String> LAMBDA_HOLDERS = Sets.immutable.empty();
    static final ImmutableSet<String> DEFERRED_TO_P2 = Sets.immutable.empty();

    public static TestSuite suite()
    {
        CompiledExecutionSupport executionSupport = PureTestBuilderCompiled.getClassLoaderExecutionSupport();
        executionSupport.getConsole().disable();
        TestSuite suite = new TestSuite();
        suite.addTest(withImplementation("JAVA", targets(executionSupport)));
        suite.addTest(withImplementation("SHADOW", targets(executionSupport)));
        return suite;
    }

    static ImmutableSet<String> enabledTargets()
    {
        return SKELETON.newWithAll(REACTIVATION).newWithAll(LAMBDA_HOLDERS).newWithoutAll(DEFERRED_TO_P2);
    }

    private static TestSuite targets(CompiledExecutionSupport executionSupport)
    {
        ImmutableSet<String> enabled = enabledTargets();
        return PureTestBuilderCompiled.buildSuite(TestCollection.collectTests("meta::pure::router::preeval::tests", executionSupport.getProcessorSupport(), fn -> PureTestBuilderCompiled.generatePureTestCollection(fn, executionSupport), ci -> enabled.contains(((Function<?>) ci)._functionName()) && PureTestBuilder.satisfiesConditionsModular(ci, executionSupport.getProcessorSupport())), executionSupport);
    }

    private static Test withImplementation(String implementation, Test tests)
    {
        return new TestSetup(tests)
        {
            private String previous;

            @Override
            protected void setUp()
            {
                this.previous = System.getProperty(PROPERTY);
                System.setProperty(PROPERTY, implementation);
            }

            @Override
            protected void tearDown()
            {
                if (this.previous == null)
                {
                    System.clearProperty(PROPERTY);
                }
                else
                {
                    System.setProperty(PROPERTY, this.previous);
                }
            }
        };
    }
}
```
If `PureTestBuilderCompiled.buildSuite` returns a type other than `TestSuite` in 5.105.0, adapt the return type of `targets` and record it.

- [ ] **Step 2: Build compiled-core and run the runner; confirm RED**

```bash
. /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -DskipTests -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core \
&& mvn -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -DargLine= \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core \
  org.apache.maven.plugins:maven-surefire-plugin:2.22.2:test -Dtest=Test_Pure_Preeval_Java
```
Expected: some SKELETON tests fail under `JAVA` and/or `SHADOW`. Those are the tests whose expected output differs from their input, which the identity cannot produce. Record which ones fail and why. Tests that pass under the identity are not evidence; they are no-change tests.

- [ ] **Step 3: Implement `GenericTypes` (port of `resolveGenericType` / `resolveFunctionType`, preeval.pure ~1018–1047)**

```java
package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.FunctionType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;

public final class GenericTypes
{
    private final PrevalRuntime runtime;

    public GenericTypes(PrevalRuntime runtime)
    {
        this.runtime = runtime;
    }

    public PrevalResult resolveGenericType(GenericType genericType, PrevalState state)
    {
        if (genericType._typeParameter() == null)
        {
            if (genericType._rawType() instanceof FunctionType)
            {
                // parity: Pure reports FunctionType resolution as modified with canPreval=false even when nothing changes
                return new PrevalResult(this.runtime.withRawType(genericType, resolveFunctionType((FunctionType) genericType._rawType(), state)), false, Lists.immutable.empty(), true);
            }
            ListIterable<PrevalResult> arguments = Lists.mutable.withAll(genericType._typeArguments()).collect(argument -> resolveGenericType(argument, state));
            if (!PrevalResult.anyModified(arguments))
            {
                return PrevalResult.unmodified(genericType);
            }
            return new PrevalResult(this.runtime.withTypeArguments(genericType, arguments.collect(a -> (GenericType) a.getValue())), PrevalResult.allCanPreval(arguments), Lists.immutable.empty(), true);
        }
        Object bound = state.getInScopeTypeParams().get(genericType._typeParameter()._name());
        return bound == null ? PrevalResult.unmodified(genericType) : new PrevalResult(bound, true, Lists.immutable.empty(), true);
    }

    public FunctionType resolveFunctionType(FunctionType functionType, PrevalState state)
    {
        ListIterable<VariableExpression> parameters = Lists.mutable.<VariableExpression>withAll(functionType._parameters()).collect(p -> this.runtime.withGenericType(p, (GenericType) resolveGenericType(p._genericType(), state).getValue()));
        return this.runtime.withSignature(functionType, parameters, (GenericType) resolveGenericType(functionType._returnType(), state).getValue());
    }
}
```

- [ ] **Step 4: Implement `Preevaluator`: the skeleton**

The Pure line references are for review. This skeleton ports `prevalInternal` (all node kinds except lambda holders, which are Task 7), `prevalFunctionDefinition`, and the prologue of `prevalGenericFunctionExpression` up to and including the `notPrevalReason` branch. When every parameter is concrete, it returns the expression **not prevalled**; Task 6 adds re-activation at the marked point.
```java
package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.FunctionDefinition;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.KeyExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.multiplicity.Multiplicity;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.type.generics.GenericType;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.VariableExpression;

public final class Preevaluator
{
    private final PrevalRuntime runtime;
    private final PrevalHooks hooks;
    private final Scope scope;
    private final GenericTypes genericTypes;

    public Preevaluator(PrevalRuntime runtime, PrevalHooks hooks)
    {
        this.runtime = runtime;
        this.hooks = hooks;
        this.scope = new Scope(v -> v instanceof VariableExpression ? ((VariableExpression) v)._name() : null);
        this.genericTypes = new GenericTypes(runtime);
    }

    public PrevalResult preval(Object item, PrevalState state)
    {
        return prevalInternal(item, state);
    }

    PrevalResult prevalInternal(Object item, PrevalState origState)
    {
        PrevalState state = origState.deeper(item instanceof FunctionDefinition ? item : null);
        DebugTrace.processing(state, item);
        PrevalResult result = dispatch(item, state);
        DebugTrace.returning(state, result);
        return result;
    }

    private PrevalResult dispatch(Object item, PrevalState state)
    {
        if (item instanceof LambdaFunction)
        {
            return prevalLambda((LambdaFunction<?>) item, state);
        }
        if (item instanceof FunctionDefinition)
        {
            return prevalFunctionDefinition((FunctionDefinition<?>) item, state.withInScopeVars(Maps.immutable.empty()));
        }
        if (item instanceof FunctionExpression)
        {
            return prevalFunctionExpression((FunctionExpression) item, state);
        }
        if (item instanceof VariableExpression)
        {
            return prevalVariable((VariableExpression) item, state);
        }
        if (item instanceof InstanceValue)
        {
            return prevalInstanceValue((InstanceValue) item, state);
        }
        if (item instanceof KeyExpression)
        {
            return prevalKeyExpression((KeyExpression) item, state);
        }
        if (isAnyOf(item, MetamodelPaths.SCHEMA_STATE, MetamodelPaths.ROOT_GRAPH_FETCH_TREE, MetamodelPaths.BINDING, MetamodelPaths.STORE))
        {
            return PrevalResult.unmodified(item);
        }
        if (this.runtime.isInstanceOf(item, MetamodelPaths.RELATION_STORE_ACCESSOR))
        {
            return PrevalResult.unmodified(item, false);
        }
        if (isAnyOf(item, MetamodelPaths.AGG_COL_SPEC, MetamodelPaths.FUNC_COL_SPEC, MetamodelPaths.TEST_PARAMETERS))
        {
            return PrevalResult.unmodified(item);
        }
        if (this.runtime.isInstanceOf(item, MetamodelPaths.RELATION_ELEMENT_ACCESSOR))
        {
            return PrevalResult.unmodified(item, false);
        }
        assertStops(Lists.immutable.with(item));
        return PrevalResult.unmodified(item);
    }

    private PrevalResult prevalLambda(LambdaFunction<?> lambda, PrevalState state)
    {
        ImmutableList<String> argumentNames = this.runtime.parameterNames(lambda);
        MutableMap<String, ImmutableList<Object>> vars = Maps.mutable.withMapIterable(this.runtime.openVariableValues(lambda));
        state.getInScopeVars().forEachKeyValue((name, value) ->
        {
            if (!argumentNames.contains(name))
            {
                vars.put(name, value);
            }
        });
        return prevalFunctionDefinition(lambda, state.withInScopeVars(vars.toImmutable()));
    }

    private PrevalResult prevalFunctionDefinition(FunctionDefinition<?> function, PrevalState origState)
    {
        ListIterable<? extends ValueSpecification> items = Lists.mutable.withAll(function._expressionSequence());
        MutableList<PrevalResult> results = Lists.mutable.empty();
        PrevalState state = origState;
        for (int index = 0; index < items.size(); index++)
        {
            ValueSpecification item = items.get(index);
            boolean isLast = index == items.size() - 1;
            if (!isLast && !isLet(item))
            {
                continue;
            }
            PrevalResult r = prevalInternal(item, state);
            if (!isLet(r.getValue()))
            {
                results.add(r);
                continue;
            }
            FunctionExpression let = (FunctionExpression) r.getValue();
            ListIterable<? extends ValueSpecification> letParameters = Lists.mutable.withAll(let._parametersValues());
            String varName = this.runtime.stringValue(((InstanceValue) letParameters.get(0))._values().getOnly());
            ImmutableList<Object> varValue = letParameters.get(1) instanceof InstanceValue
                    ? Lists.immutable.withAll(((InstanceValue) letParameters.get(1))._values())
                    : Lists.immutable.with(letParameters.get(1));
            boolean shouldInlineVariable = isSingle(varValue, VariableExpression.class)
                    || (r.canPreval() && this.scope.areAllInScope(r.getOpenVars(), state.getInScopeVars()) && !isSingle(varValue, LambdaFunction.class));
            state = state.withRollingInScopeVars(state.getRollingInScopeVars().newWithKeyValue(varName, varValue));
            if (shouldInlineVariable)
            {
                state = state.withInScopeVars(state.getInScopeVars().newWithKeyValue(varName, varValue));
            }
            if (isLast)
            {
                Object lastValue = isSingle(varValue, ValueSpecification.class)
                        ? varValue.getOnly()
                        : this.runtime.newInstanceValue(let._genericType(), this.runtime.exactly(varValue.size()), varValue);
                results.add(new PrevalResult(lastValue, r.canPreval(), r.getOpenVars(), true));
            }
            else if (!shouldInlineVariable)
            {
                results.add(r);
            }
        }
        boolean modified = results.size() != items.size() || PrevalResult.anyModified(results);
        Object value = function;
        if (modified)
        {
            ListIterable<ValueSpecification> sequence = results.collect(r -> (ValueSpecification) r.getValue());
            ListIterable<String> openVariables = null;
            if (function instanceof LambdaFunction)
            {
                ImmutableList<String> resultOpen = results.flatCollect(PrevalResult::getOpenVars).collect(v -> this.scope.resolveVariable(v, origState.getInScopeVars())).toImmutable();
                openVariables = Lists.mutable.withAll(((LambdaFunction<?>) function)._openVariables())
                        .collect(v -> this.scope.resolveVariable(v, origState.getInScopeVars()))
                        .distinct()
                        .select(resultOpen::contains);
            }
            value = this.runtime.withExpressionSequence(function, sequence, openVariables);
        }
        ImmutableList<String> openVars = value instanceof LambdaFunction ? Lists.immutable.withAll(((LambdaFunction<?>) value)._openVariables()) : Lists.immutable.empty();
        return new PrevalResult(value, PrevalResult.allCanPreval(results), openVars, modified);
    }

    private PrevalResult prevalFunctionExpression(FunctionExpression expression, PrevalState state)
    {
        ListIterable<? extends ValueSpecification> parameters = Lists.mutable.withAll(expression._parametersValues());
        int parameterCount = Math.min(parameters.size(), this.runtime.parameterNames(expression._func()).size());
        // parity: Pure zips parameter names with values, so surplus values are dropped
        ListIterable<PrevalResult> results = parameters.subList(0, parameterCount).collect(p -> prevalInternal(p, state));
        PrevalResult genericType = this.genericTypes.resolveGenericType(expression._genericType(), state);
        boolean modified = PrevalResult.anyModified(results) || genericType.isModified();
        FunctionExpression newExpression = expression;
        if (modified)
        {
            ListIterable<? extends ValueSpecification> newParameters = PrevalResult.anyModified(results)
                    ? results.collect(r -> this.runtime.withGenericType((ValueSpecification) r.getValue(), (GenericType) this.genericTypes.resolveGenericType(((ValueSpecification) r.getValue())._genericType(), state).getValue()))
                    : parameters;
            newExpression = this.runtime.withParametersAndGenericType(expression, newParameters, (GenericType) genericType.getValue());
        }
        ImmutableList<String> openVars = this.scope.openVars(results.flatCollect(PrevalResult::getOpenVars), state.getInScopeVars());
        boolean canPrevalFunction = !(this.runtime.hasStereotype(newExpression._func(), MetamodelPaths.FUNCTION_TYPE_PROFILE, "SideEffectFunction")
                || this.runtime.hasStereotype(newExpression._func(), MetamodelPaths.FUNCTION_TYPE_PROFILE, "NotImplementedFunction")
                || this.hooks.stopPreeval(Lists.immutable.with(newExpression)));
        if (!canPrevalFunction)
        {
            boolean canPreval = this.runtime.isFunction(newExpression._func(), MetamodelPaths.LET_FUNCTION) && PrevalResult.allCanPreval(results);
            return new PrevalResult(newExpression, canPreval, openVars, modified);
        }
        String notPrevalReason = notPrevalReason(newExpression, results, state);
        if (notPrevalReason != null)
        {
            return new PrevalResult(newExpression, PrevalResult.allCanPreval(results), openVars, modified);
        }
        return new PrevalResult(newExpression, PrevalResult.allCanPreval(results), openVars, modified);
    }

    private String notPrevalReason(FunctionExpression expression, ListIterable<PrevalResult> results, PrevalState state)
    {
        if (Lists.mutable.withAll(expression._parametersValues()).anySatisfy(pv -> !isInstanceValue(pv, state.getInScopeVars())))
        {
            return "params are not instance values";
        }
        if (!PrevalResult.allCanPreval(results))
        {
            return "params can not preval";
        }
        if (!this.scope.areAllInScope(results.flatCollect(PrevalResult::getOpenVars).distinct(), state.getInScopeVars()))
        {
            return "open variables not in scope";
        }
        if (this.runtime.isFunction(expression._func(), MetamodelPaths.CAST_FUNCTION) && results.notEmpty()
                && results.get(0).getValue() instanceof InstanceValue && ((InstanceValue) results.get(0).getValue())._values().isEmpty())
        {
            return "cast of empty collection";
        }
        return null;
    }

    private PrevalResult prevalVariable(VariableExpression variable, PrevalState state)
    {
        String name = variable._name();
        ImmutableList<Object> values = state.getInScopeVars().get(name);
        if (values == null)
        {
            return new PrevalResult(variable, true, Lists.immutable.with(name), false);
        }
        Object substitute = substituteFor(variable, values);
        PrevalState withoutVariable = state.withInScopeVars(state.getInScopeVars().newWithoutKey(name));
        return prevalInternal(substitute, withoutVariable).markModified();
    }

    private Object substituteFor(VariableExpression variable, ImmutableList<Object> values)
    {
        if (values.isEmpty())
        {
            return this.runtime.newInstanceValue(variable._genericType(), variable._multiplicity(), values);
        }
        if (isSingle(values, InstanceValue.class) || isSingle(values, VariableExpression.class) || isSingle(values, FunctionExpression.class))
        {
            return values.getOnly();
        }
        if (allAre(values, MetamodelPaths.TDS_AGGREGATE_VALUE) || allAre(values, MetamodelPaths.COLLECTION_AGGREGATE_VALUE)
                || allAre(values, MetamodelPaths.AGG_COL_SPEC_ARRAY) || allAre(values, MetamodelPaths.AGG_COL_SPEC) || allAre(values, MetamodelPaths.BASIC_COLUMN_SPECIFICATION)
                || (values.size() == 1 && isAnyOf(values.getOnly(), MetamodelPaths.ROOT_GRAPH_FETCH_TREE, MetamodelPaths.BINDING, MetamodelPaths.SCHEMA_STATE, MetamodelPaths.STORE, MetamodelPaths.TEST_PARAMETERS)))
        {
            return this.runtime.newInstanceValue(variable._genericType(), variable._multiplicity(), values);
        }
        assertStops(values);
        return this.runtime.newInstanceValue(variable._genericType(), variable._multiplicity(), values);
    }

    private PrevalResult prevalInstanceValue(InstanceValue instanceValue, PrevalState state)
    {
        ListIterable<PrevalResult> values = Lists.mutable.withAll(instanceValue._values()).collect(v -> prevalInternal(v, state));
        ImmutableList<String> openVars = this.scope.openVars(values.flatCollect(PrevalResult::getOpenVars), state.getInScopeVars());
        if (!PrevalResult.anyModified(values))
        {
            return new PrevalResult(instanceValue, PrevalResult.allCanPreval(values), openVars, false);
        }
        MutableList<Object> cleanValues = Lists.mutable.empty();
        values.forEach(r ->
        {
            Object v = r.getValue();
            if (v instanceof InstanceValue && ((InstanceValue) v)._values().size() == 1)
            {
                cleanValues.addAllIterable(((InstanceValue) v)._values());
            }
            else
            {
                cleanValues.add(v);
            }
        });
        GenericType genericType = (GenericType) this.genericTypes.resolveGenericType(instanceValue._genericType(), state).getValue();
        // parity: Cast(@X) produces an empty InstanceValue with multiplicity PureOne, which Pure preserves
        Multiplicity multiplicity = instanceValue._multiplicity() == this.runtime.pureOne() && cleanValues.isEmpty() ? instanceValue._multiplicity() : this.runtime.exactly(cleanValues.size());
        return new PrevalResult(this.runtime.withValues(instanceValue, cleanValues, genericType, multiplicity), PrevalResult.allCanPreval(values), openVars, true);
    }

    private PrevalResult prevalKeyExpression(KeyExpression keyExpression, PrevalState state)
    {
        PrevalResult expression = prevalInternal(keyExpression._expression(), state);
        Object value = expression.isModified() ? this.runtime.withExpression(keyExpression, (ValueSpecification) expression.getValue()) : keyExpression;
        return new PrevalResult(value, expression.canPreval(), this.scope.openVars(expression.getOpenVars(), state.getInScopeVars()), expression.isModified());
    }

    private boolean isInstanceValue(Object value, ImmutableMap<String, ImmutableList<Object>> inScopeVars)
    {
        if (value instanceof InstanceValue)
        {
            return Lists.mutable.withAll(((InstanceValue) value)._values()).allSatisfy(v -> (v instanceof ValueSpecification) ? isInstanceValue(v, inScopeVars) : !this.runtime.isInstanceOf(v, MetamodelPaths.TDS));
        }
        if (value instanceof VariableExpression)
        {
            return inScopeVars.containsKey(((VariableExpression) value)._name());
        }
        if (value instanceof FunctionExpression)
        {
            return false;
        }
        throw this.runtime.error("Unexpected value in isInstanceValue: " + this.runtime.typeDescription(value));
    }

    private boolean isLet(Object value)
    {
        return value instanceof FunctionExpression && this.runtime.isFunction(((FunctionExpression) value)._func(), MetamodelPaths.LET_FUNCTION);
    }

    private void assertStops(ImmutableList<Object> values)
    {
        if (!this.hooks.stopPreeval(values))
        {
            throw this.runtime.error("Unsupported type: " + this.runtime.typeDescription(values.getFirst()));
        }
    }

    private boolean isAnyOf(Object value, String... typePaths)
    {
        for (String typePath : typePaths)
        {
            if (this.runtime.isInstanceOf(value, typePath))
            {
                return true;
            }
        }
        return false;
    }

    private boolean allAre(ImmutableList<Object> values, String typePath)
    {
        return values.allSatisfy(v -> this.runtime.isInstanceOf(v, typePath));
    }

    private static boolean isSingle(ImmutableList<Object> values, Class<?> type)
    {
        return values.size() == 1 && type.isInstance(values.getOnly());
    }
}
```
Notes the implementer must respect:
- **Leave unported for now:** the `if`/`and`/`or` dedicated handlers and the inlining/eval branches (P2). Lambda-holder dispatch comes in Task 7. The final `return` in `prevalFunctionExpression` is where Task 6 adds re-activation, so leave the duplicate return in place until then.
- **Result multiplicity when the last statement is a `let`:** Pure uses `$a->size()->toMultiplicity()`, which is `exactly(size)`.
- **Missing `Maps.mutable.withMapIterable`:** if it isn't available in the Eclipse Collections version on the classpath, use `Maps.mutable.empty()` followed by `putAll(...castToMap())` and record the change.

`DebugTrace.java`:
```java
package org.finos.legend.engine.pure.preeval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DebugTrace
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DebugTrace.class);

    private DebugTrace()
    {
    }

    static void processing(PrevalState state, Object item)
    {
        if (state.isDebug())
        {
            LOGGER.info("[{}] Processing {} inScopeVars={}", state.getDepth(), item == null ? "null" : item.getClass().getSimpleName(), state.getInScopeVars().keysView().toSortedList());
        }
    }

    static void returning(PrevalState state, PrevalResult result)
    {
        if (state.isDebug())
        {
            LOGGER.info("[{}] Returning {} modified={} canPreval={} openVars={}", state.getDepth(), result.getValue() == null ? "null" : result.getValue().getClass().getSimpleName(), result.isModified(), result.canPreval(), result.getOpenVars());
        }
    }
}
```

- [ ] **Step 5: Implement the compiled adapter and wire the native**

`CompiledPrevalHooks.java`:
```java
package org.finos.legend.engine.pure.preeval.compiled;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ListIterable;
import org.finos.legend.engine.pure.preeval.PrevalHooks;
import org.finos.legend.pure.generated.CoreGen;
import org.finos.legend.pure.generated.Root_meta_pure_functions_preeval_PrevalHooks;
import org.finos.legend.pure.m3.execution.ExecutionSupport;
import org.finos.legend.pure.runtime.java.compiled.generation.processors.support.Pure;

final class CompiledPrevalHooks implements PrevalHooks
{
    private final Root_meta_pure_functions_preeval_PrevalHooks hooks;
    private final ExecutionSupport executionSupport;

    CompiledPrevalHooks(Root_meta_pure_functions_preeval_PrevalHooks hooks, ExecutionSupport executionSupport)
    {
        this.hooks = hooks;
        this.executionSupport = executionSupport;
    }

    @Override
    public boolean stopPreeval(ListIterable<?> values)
    {
        return (Boolean) Pure.evaluate(this.executionSupport, this.hooks._stopPreeval(), CoreGen.bridge, Lists.mutable.withAll(values));
    }
}
```
`CompiledPrevalRuntime.java`: implement **every** `PrevalRuntime` method except `reactivate` (Task 6) and the three property methods (Task 7). Those four throw `UnsupportedOperationException("P1 Task 6")` / `("P1 Task 7")` until then. Required behaviour, following the "legend-pure API facts" above:
- **`isInstanceOf`:** look up and cache `Type` by path via `((CompiledExecutionSupport) es).getProcessorSupport().package_getByUserPath(path)`. A `null` type returns `false`. Then call `Pure.instanceOf(value, type, es)`.
- **`typeDescription`:** for a `CoreInstance` whose classifier is a `PackageableElement`, return `PackageableElement.getUserPathForPackageableElement(processorSupport.getClassifier(ci))`. Otherwise return `value.getClass().getSimpleName()`.
- **`isFunction`:** reference equality with the element at the path (cached lookup via `package_getByUserPath`). If lookup returns null, return false.
- **`hasStereotype`:** `function instanceof ElementWithStereotypes` (`org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.extension.ElementWithStereotypes`), then check whether any `_stereotypes()` has `_value()` equal to `stereotype` and a `_profile()` whose user path equals `profilePath`.
- **`parameterNames`:** `(FunctionType) processorSupport.function_getFunctionType((CoreInstance) function)`, then `_parameters()`, then `_name()`.
- **`stringValue`:** `(String) primitive`.
- **`pureOne` / `pureZero`:** `es.getMetadata("meta::pure::metamodel::multiplicity::PackageableMultiplicity", "meta::pure::metamodel::multiplicity::PureOne" / "...PureZero")`, cached.
- **`exactly`:** as specified in Task 3. For n > 1:
  ```java
  new Root_meta_pure_metamodel_multiplicity_Multiplicity_Impl("Anonymous_NoCounter")
      ._lowerBound(new Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl("Anonymous_NoCounter")._value((long) n))
      ._upperBound(new Root_meta_pure_metamodel_multiplicity_MultiplicityValue_Impl("Anonymous_NoCounter")._value((long) n))
  ```
- **`openVariableValues`:** `Pure.getOpenVariables(lambda, CoreGen.bridge).getMap()`, converted to `String → ImmutableList.withAll(((org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<?>) v)._values())`.
- **`newInstanceValue`:**
  ```java
  new Root_meta_pure_metamodel_valuespecification_InstanceValue_Impl("Anonymous_NoCounter")
      ._genericType(gt)._multiplicity(m)._values(Lists.mutable.withAll(values))
  ```
- **`with*`:** `CompiledSupport.copy(node)` followed by the typed setters:
  - `_expressionSequence(...)`, plus `_openVariables(...)` when non-null;
  - `_parametersValues(...)._genericType(...)`;
  - `_genericType(...)`;
  - `_values(...)._genericType(...)._multiplicity(...)`;
  - `_expression(...)`;
  - `_typeArguments(...)`;
  - `_rawType(...)`;
  - `_parameters(...)._returnType(...)`.
- **`error`:** `new PureExecutionException(this.sourceInformation, message)`. In this task `sourceInformation` is `null`; Task 8 supplies it.

Then rewrite `CompiledPreeval.preval`:
```java
    public static CoreInstance preval(Object item, PureMap inScopeVars, PureMap rollingInScopeVars, CoreInstance hooks, CoreInstance debug, ExecutionSupport executionSupport)
    {
        CompiledPrevalRuntime runtime = new CompiledPrevalRuntime(executionSupport);
        PrevalState state = PrevalState.initial(toVars(inScopeVars), toVars(rollingInScopeVars), ((Root_meta_pure_tools_DebugContext) debug)._debug());
        PrevalResult result = new Preevaluator(runtime, new CompiledPrevalHooks((Root_meta_pure_functions_preeval_PrevalHooks) hooks, executionSupport)).preval(item, state);
        return new Root_meta_pure_functions_preeval_PrevalResult_Impl("Anonymous_NoCounter")
                ._value(result.getValue())
                ._canPreval(result.canPreval())
                ._openVars(Lists.mutable.withAll(result.getOpenVars()))
                ._modified(result.isModified());
    }

    static ImmutableMap<String, ImmutableList<Object>> toVars(PureMap map)
    {
        MutableMap<String, ImmutableList<Object>> vars = Maps.mutable.empty();
        map.getMap().forEachKeyValue((key, value) -> vars.put((String) key, Lists.immutable.withAll(((org.finos.legend.pure.m3.coreinstance.meta.pure.functions.collection.List<?>) value)._values())));
        return vars.toImmutable();
    }
```
Delete `PrevalResults.java` from the shared module. The interpreted native used it, so replace that call with inline ephemeral construction for now, building the `meta::pure::functions::preeval::PrevalResult` instance exactly as `PrevalResults` did but with `processorSupport.newEphemeralAnonymousCoreInstance`. Take the value from `PrevalResult.unmodified(item)`. Task 5 replaces this.

In `AbstractTestPrevalNative.HOOKS`, change `stopPreeval = {a:Any[*] | false}` to `stopPreeval = {a:Any[*] | true}`. Java now calls the hook on primitive leaves, and Pure's default stop strategy returns `true` for them.

- [ ] **Step 6: Build the preeval family and compiled-core, then run everything**

```bash
. /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-compiled-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-interpreted-functions-preeval \
&& mvn clean install -T 3 -DskipTests -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core \
&& mvn -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -DargLine= \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core \
  org.apache.maven.plugins:maven-surefire-plugin:2.22.2:test -Dtest='Test_Pure_Preeval_Java,Test_Pure_Preeval'
```
Expected:
- the harness tests stay green in both modes (3 + 3);
- `Test_Pure_Preeval_Java` passes every SKELETON test under both `JAVA` and `SHADOW`;
- `Test_Pure_Preeval` (default PURE) is unchanged at 111/111.

For each SKELETON failure, decide whether it is a P1 bug to fix, or a test that needs a P2 rule; the latter goes in `DEFERRED_TO_P2` with the reason in the report. `SHADOW` failures print both descriptions. Compare them field by field.

- [ ] **Step 7: Commit**

```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval/pom.xml \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval/src \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-compiled-functions-preeval/src \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-interpreted-functions-preeval/src \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/test/java/org/finos/legend/engine/pure/code/core/Test_Pure_Preeval_Java.java
git status --short   # confirm only this task's files are staged
git commit -m "Port the preeval traversal skeleton to Java with a compiled-mode runtime"
```

---

### Task 5: Interpreted adapter

**Files:**
- Create: `INTERPRETED/.../interpreted/InterpretedPrevalRuntime.java`, `InterpretedPrevalHooks.java`
- Modify: `INTERPRETED/.../natives/PrevalNative.java`
- Modify: `SHARED/src/test/java/.../AbstractTestPrevalNative.java` (new scenario)

**Interfaces:**
- Consumes: `Preevaluator`, `PrevalRuntime`, `PrevalHooks`, `PrevalState` (Tasks 3–4).
- Produces: `InterpretedPrevalRuntime`, a per-call object constructed from the native's execute context. It implements the same methods as the compiled adapter in Task 4. `reactivate` and the property methods throw `UnsupportedOperationException` until Tasks 6 and 7.

- [ ] **Step 1: Add a failing harness scenario that needs real traversal**

Append to `AbstractTestPrevalNative`:
```java
    @Test
    public void testPrevalNativeReportsOpenVariablesOfFunctionExpression()
    {
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let fe = {p:Integer[1] | $p + 1}->evaluateAndDeactivate().expressionSequence->at(0);",
                "let r = prevalNative($fe, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert(!$r.modified, |'expected unmodified');",
                "assert($r.value == $fe, |'expected the same function expression back');",
                "assert($r.openVars->size() == 1 && $r.openVars->at(0) == 'p', |'expected open variable p');");
    }
```

- [ ] **Step 2: Run the preeval family; confirm it passes compiled and fails interpreted**

```bash
. /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -Dmdep.analyze.skip=true \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-compiled-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-interpreted-functions-preeval -fae
```
Expected: `TestPrevalNativeCompiled` passes 4/4, and `TestPrevalNativeInterpreted` fails the new test (`expected open variable p`, because the identity returns no open variables).

- [ ] **Step 3: Implement the interpreted adapter and wire the native**

`PrevalNative` (interpreted):
- stores `FunctionExecutionInterpreted functionExecution` and `ModelRepository repository` from its constructor;
- decodes arguments;
- builds a per-call `InterpretedPrevalRuntime`;
- runs `Preevaluator`;
- builds the result.

```java
    @Override
    public CoreInstance execute(ListIterable<? extends CoreInstance> params, Stack<MutableMap<String, CoreInstance>> resolvedTypeParameters, Stack<MutableMap<String, CoreInstance>> resolvedMultiplicityParameters, VariableContext variableContext, MutableStack<CoreInstance> functionExpressionCallStack, Profiler profiler, InstantiationContext instantiationContext, ExecutionSupport executionSupport, Context context, ProcessorSupport processorSupport) throws PureExecutionException
    {
        InterpretedPrevalRuntime runtime = new InterpretedPrevalRuntime(this.functionExecution, this.repository, processorSupport, resolvedTypeParameters, resolvedMultiplicityParameters, variableContext, functionExpressionCallStack, profiler, instantiationContext, executionSupport);
        CoreInstance item = params.get(0).getValueForMetaPropertyToOne(M3Properties.values);
        CoreInstance hooks = Instance.getValueForMetaPropertyToOneResolved(params.get(3), M3Properties.values, processorSupport);
        CoreInstance debug = Instance.getValueForMetaPropertyToOneResolved(params.get(4), M3Properties.values, processorSupport);
        PrevalState state = PrevalState.initial(runtime.toVars(params.get(1)), runtime.toVars(params.get(2)), PrimitiveUtilities.getBooleanValue(debug.getValueForMetaPropertyToOne("debug")));
        PrevalResult result = new Preevaluator(runtime, new InterpretedPrevalHooks(runtime, Instance.getValueForMetaPropertyToOneResolved(hooks, "stopPreeval", processorSupport), getParentOrEmptyVariableContextForLambda(variableContext, Instance.getValueForMetaPropertyToOneResolved(hooks, "stopPreeval", processorSupport)))).preval(item, state);
        return ValueSpecificationBootstrap.wrapValueSpecification(runtime.toPureResult(result), false, processorSupport);
    }
```
`InterpretedPrevalRuntime` must provide the following. Read the named legend-pure classes under `/home/aziem/pure/legend-pure-revision/legend-pure-runtime/legend-pure-runtime-java-engine-interpreted/src/main/java/org/finos/legend/pure/runtime/java/interpreted/natives/` and check signatures with `javap` on the 5.105.0 jars.
- **`toVars(CoreInstance mapArgument)`:** `((MapCoreInstance) Instance.getValueForMetaPropertyToManyResolved(mapArgument, M3Properties.values, ps).getFirst()).getMap()`. Each key becomes `key.getName()`, and each value becomes `ImmutableList.withAll(list.getValueForMetaPropertyToMany(M3Properties.values))`.
- **`toPureResult(PrevalResult)`:**
  - `ps.newEphemeralAnonymousCoreInstance("meta::pure::functions::preeval::PrevalResult")`;
  - `value` is the result value;
  - `canPreval` / `modified` are `repository.newBooleanCoreInstance(b)`;
  - `openVars` are `repository.newStringCoreInstance(s)`.
- **`isInstanceOf`:**
  - non-`CoreInstance` values return `false`;
  - if `ps.package_getByUserPath(path) == null`, return `false`;
  - otherwise `Instance.instanceOf((CoreInstance) value, path, ps)`.
- **`typeDescription`:** the user path of `ps.getClassifier(ci)`.
- **`isFunction`:** reference equality with `ps.package_getByUserPath(functionPath)`.
- **`hasStereotype`:** `Instance.getValueForMetaPropertyToManyResolved(function, M3Properties.stereotypes, ps)`. For each stereotype, compare `PrimitiveUtilities.getStringValue(s.getValueForMetaPropertyToOne(M3Properties.value))` and the user path of `Instance.getValueForMetaPropertyToOneResolved(s, M3Properties.profile, ps)`.
- **`parameterNames`:** `ps.function_getFunctionType(function)`, then `parameters`, then `name`.
- **`stringValue`:** `PrimitiveUtilities.getStringValue((CoreInstance) primitive)`.
- **`pureOne` / `pureZero`:** `ps.package_getByUserPath(M3Paths.PureOne / PureZero)`.
- **`exactly`:**
  - 0 returns `pureZero()`;
  - 1 returns `pureOne()`;
  - otherwise `Multiplicity.newMultiplicity(n, ps)` (`org.finos.legend.pure.m3.navigation.multiplicity.Multiplicity`), wrapped with `MultiplicityCoreInstanceWrapper.toMultiplicity` where the M3 type is needed.
- **`openVariableValues`:** if the lambda is a `org.finos.legend.pure.runtime.java.interpreted.LambdaWithContext`, then for each `_openVariables()` name take `getVariableContext().getValue(name)` and its `values`. Otherwise return an empty map (mirror `OpenVariableValues.java`).
- **`newInstanceValue`:** an ephemeral `M3Paths.InstanceValue` with `genericType`, `multiplicity` and `values` set via `Instance.setValueForProperty` / `setValuesForProperty`. Values are CoreInstances in interpreted mode.
- **`with*`:** a private `copy(CoreInstance src, MapIterable<String, ListIterable<? extends CoreInstance>> overrides)`, mirroring `Copy.java`:
  - `repository.newEphemeralAnonymousCoreInstance(null, ps.getClassifier(src))`;
  - for each key of `ps.class_getSimplePropertiesByName(classifier)`, the override if present, otherwise `src.getValueForMetaPropertyToMany(key)`, added when non-empty.

  Return the copy wrapped to the M3 interface the method declares, using `*CoreInstanceWrapper.toX`:
  - `LambdaFunctionCoreInstanceWrapper`, `FunctionExpressionCoreInstanceWrapper` and `InstanceValueCoreInstanceWrapper` in `…metamodel.valuespecification` or `…metamodel.function`;
  - `KeyExpressionCoreInstanceWrapper` is in `…metamodel.function`;
  - `GenericTypeCoreInstanceWrapper` is in `…metamodel.type.generics`.

  A `LambdaWithContext` input copies to a plain lambda. That is what interpreted Pure's `^$lf(...)` does, and it is intentional (`// parity:`).
- **`error`:** `new PureExecutionException(functionExpressionCallStack.isEmpty() ? null : functionExpressionCallStack.peek().getSourceInformation(), message)`.

`InterpretedPrevalHooks`:
```java
final class InterpretedPrevalHooks implements PrevalHooks
{
    private final InterpretedPrevalRuntime runtime;
    private final CoreInstance stopPreeval;
    private final VariableContext evaluationContext;

    InterpretedPrevalHooks(InterpretedPrevalRuntime runtime, CoreInstance stopPreeval, VariableContext evaluationContext)
    {
        this.runtime = runtime;
        this.stopPreeval = stopPreeval;
        this.evaluationContext = evaluationContext;
    }

    @Override
    public boolean stopPreeval(ListIterable<?> values)
    {
        return this.runtime.evaluateBoolean(this.stopPreeval, this.evaluationContext, values);
    }
}
```
`InterpretedPrevalRuntime.evaluateBoolean(fn, ctx, values)` mirrors `Filter.java`:
```java
functionExecution.executeFunction(false, FunctionCoreInstanceWrapper.toFunction(fn),
    Lists.immutable.with(ValueSpecificationBootstrap.wrapValueSpecification((ListIterable<CoreInstance>) values, false, ps)),
    resolvedTypeParameters, resolvedMultiplicityParameters, ctx, functionExpressionCallStack, profiler, instantiationContext, executionSupport)
```
The result is read with `PrimitiveUtilities.getBooleanValue(Instance.getValueForMetaPropertyToOneResolved(result, M3Properties.values, ps))`. If the collection overload of `wrapValueSpecification` has a different signature in 5.105.0, use the one `javap` shows and record it.

- [ ] **Step 4: Run the preeval family**

Same command as Step 2, without `-Dmdep.analyze.skip=true`. Expected: both harness classes pass 4/4 and the analyzer is clean. If the interpreted run reveals a divergence from compiled (for example `_func()` returning an unresolved import stub), fix it inside the adapter only, and record it.

- [ ] **Step 5: Commit**

```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-interpreted-functions-preeval/src \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval/src/test
git status --short   # confirm only this task's files are staged
git commit -m "Run the Java preeval traversal in interpreted mode"
```

---

### Task 6: Re-activation

**Files:**
- Modify: `SHARED/.../Preevaluator.java`: the final re-activation branch of `prevalFunctionExpression`
- Modify: `COMPILED/.../CompiledPrevalRuntime.java`, `INTERPRETED/.../InterpretedPrevalRuntime.java`: `reactivate`
- Modify: `SHARED/src/test/java/.../AbstractTestPrevalNative.java`
- Modify: `Test_Pure_Preeval_Java.java`: fill `REACTIVATION` with the slice list from the table above

**Interfaces:**
- Consumes: `PrevalRuntime.reactivate(ValueSpecification, ImmutableMap<String, ImmutableList<Object>>)`.
- Produces: fully concrete function expressions are replaced by the `InstanceValue` of their result.

- [ ] **Step 1: Failing tests**

In `AbstractTestPrevalNative`, **replace** `testPrevalNativeReturnsInputUnmodified` with the following, which folds a constant expression:
```java
    @Test
    public void testPrevalNativeFoldsConstantExpression()
    {
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let f = {|1 + 1};",
                "let r = prevalNative($f, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());",
                "assert($r.modified, |'expected modified');",
                "let body = $r.value->cast(@LambdaFunction<Any>).expressionSequence->at(0);",
                "assert($body->instanceOf(InstanceValue), |'expected an instance value');",
                "assert($body->cast(@InstanceValue).values->toOne() == 2, |'expected 2');",
                "assert($f->evaluateAndDeactivate().expressionSequence->at(0)->instanceOf(FunctionExpression), |'the input lambda must not be mutated');");
    }

    @Test
    public void testPrevalNativeSubstitutesAndFoldsInScopeVariables()
    {
        executeTestFunction(
                "let vars = newMap(pair('x', list(3)));",
                "let fe = {|$x + 1}->evaluateAndDeactivate().expressionSequence->at(0);",
                "let r = prevalNative($fe, $vars, $vars, " + HOOKS + ", noDebug());",
                "assert($r.modified, |'expected modified');",
                "assert($r.value->cast(@InstanceValue).values->toOne() == 4, |'expected 4');");
    }
```
The second test's lambda `{|$x + 1}` refers to an undeclared `x`. If the Pure compiler rejects the snippet, declare `let x = 0;` before it; the native's `vars` map supplies 3, which overrides the captured value. Record which form you used. If `pair`/`list` are not visible to the platform-only harness, build the map as `newMap(^Pair<String, List<Any>>(first = 'x', second = ^List<Any>(values = 3)))`.

In `testPrevalNativeReturnsFunctionExpressionUnmodified`, the input `{|1 + 1}` FE now folds. Change the snippet to `{p:Integer[1] | $p + 1}`, which stays unmodified because `p` is not in scope, and keep its identity assertions.

In `Test_Pure_Preeval_Java`, set `REACTIVATION` to the 12 names in the table.

- [ ] **Step 2: Confirm RED**

Run the preeval family build with `-Dmdep.analyze.skip=true -fae`, then the compiled-core runner. Use the commands from Task 4 Step 6, adding `-Dmdep.analyze.skip=true -fae` to the family build.

Expected: the fold and substitute tests fail in both modes, and the REACTIVATION tests fail under `JAVA`/`SHADOW`.

- [ ] **Step 3: Implement**

In `Preevaluator.prevalFunctionExpression`, replace the final `return` (after the `notPrevalReason` block) with a port of preeval.pure ~857–874:
```java
        ImmutableList<Object> reactivated = this.runtime.reactivate(newExpression, state.getInScopeVars());
        PrevalState emptyScope = state.withInScopeTypeParams(Maps.immutable.empty()).withInScopeVars(Maps.immutable.empty());
        ImmutableList<Object> values = reactivated.collect(v -> isReactivatedLambdaHolder(v) ? prevalInternal(v, emptyScope).getValue() : v);
        Object value = isSingle(values, InstanceValue.class) ? values.getOnly() : this.runtime.newInstanceValue(newExpression._genericType(), this.runtime.exactly(values.size()), values);
        return new PrevalResult(value, true, Lists.immutable.empty(), true);
```
with:
```java
    private boolean isReactivatedLambdaHolder(Object value)
    {
        return isAnyOf(value, MetamodelPaths.BASIC_COLUMN_SPECIFICATION, MetamodelPaths.COLLECTION_AGGREGATE_VALUE, MetamodelPaths.TDS_AGGREGATE_VALUE, MetamodelPaths.AGG_COL_SPEC_ARRAY, MetamodelPaths.AGG_COL_SPEC);
    }
```
`prevalInternal` on a holder still falls through to the stop check until Task 7. That matches Pure only once Task 7 lands, so the one RLH test is in `LAMBDA_HOLDERS`, not here.

Compiled `reactivate`:
```java
    @Override
    public ImmutableList<Object> reactivate(ValueSpecification valueSpecification, ImmutableMap<String, ImmutableList<Object>> inScopeVars)
    {
        PureMap vars = new PureMap(Maps.mutable.empty());
        inScopeVars.forEachKeyValue((name, values) -> vars.getMap().put(name, CoreGen.bridge.buildList()._valuesAddAll(values)));
        return Lists.immutable.withAll(CompiledSupport.toPureCollection(Pure.reactivate(valueSpecification, vars, CoreGen.bridge, this.executionSupport)));
    }
```
Interpreted `reactivate` mirrors `Reactivate.java` (lines ~55–80):
```java
        VariableContext context = VariableContext.newVariableContext();
        inScopeVars.forEachKeyValue((name, values) ->
        {
            CoreInstance value = this.processorSupport.newEphemeralAnonymousCoreInstance(M3Paths.InstanceValue);
            Instance.setValuesForProperty(value, M3Properties.values, Lists.mutable.withAll((Iterable<CoreInstance>) (Iterable<?>) values), this.processorSupport);
            context.registerValue(name, value);
        });
        CoreInstance result = this.functionExecution.executeValueSpecification((CoreInstance) valueSpecification, this.resolvedTypeParameters, this.resolvedMultiplicityParameters, this.functionExpressionCallStack, context, this.profiler, this.instantiationContext, this.executionSupport);
        return Lists.immutable.withAll(Instance.getValueForMetaPropertyToManyResolved(result, M3Properties.values, this.processorSupport));
```
`registerValue` declares `VariableContext.VariableNameConflictException`. Wrap it into `error(...)`.

- [ ] **Step 4: Run GREEN**

Run the same commands without the skip flags. Expected:
- the harness passes 5/5 in both modes;
- `Test_Pure_Preeval_Java` passes every SKELETON + REACTIVATION test under both `JAVA` and `SHADOW`;
- `Test_Pure_Preeval` is still 111/111.

Handle failures as in Task 4 Step 6.

- [ ] **Step 5: Commit**

```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval/src \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-compiled-functions-preeval/src \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-interpreted-functions-preeval/src \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/test/java/org/finos/legend/engine/pure/code/core/Test_Pure_Preeval_Java.java
git status --short   # confirm only this task's files are staged
git commit -m "Re-activate concrete function expressions in the Java preeval"
```

---

### Task 7: Lambda holders

**Files:**
- Create: `SHARED/.../LambdaHolders.java`
- Modify: `SHARED/.../Preevaluator.java`: dispatch to `LambdaHolders` in Pure's order
- Modify: both adapters: `getPropertyValue`, `getPropertyValues`, `withPropertyValues`
- Modify: `Test_Pure_Preeval_Java.java`: fill `LAMBDA_HOLDERS`

**Interfaces:**
- Produces: `LambdaHolders(PrevalRuntime runtime, java.util.function.BiFunction<Object, PrevalState, PrevalResult> preval)`, with `PrevalResult prevalHolder(Object item, PrevalState state)`. It returns `null` when the item is not a holder.

- [ ] **Step 1: Failing test**

Set `LAMBDA_HOLDERS` to `testRelationAggregation`, `testPrerouting_constantInAgg` and `testPrerouting_mappedModelAgg`. Run the compiled-core runner (Task 4 Step 6 commands). Expected: those tests fail, with the `Unsupported type` error or a SHADOW mismatch.

- [ ] **Step 2: Implement**

`LambdaHolders` ports preeval.pure lines ~259–321 in Pure's order.
```java
package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.MutableMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.function.LambdaFunction;

import java.util.function.BiFunction;

final class LambdaHolders
{
    private final PrevalRuntime runtime;
    private final BiFunction<Object, PrevalState, PrevalResult> preval;

    LambdaHolders(PrevalRuntime runtime, BiFunction<Object, PrevalState, PrevalResult> preval)
    {
        this.runtime = runtime;
        this.preval = preval;
    }

    PrevalResult prevalHolder(Object item, PrevalState state)
    {
        if (this.runtime.isInstanceOf(item, MetamodelPaths.COLUMN_SPECIFICATION))
        {
            return this.runtime.isInstanceOf(item, MetamodelPaths.BASIC_COLUMN_SPECIFICATION) ? prevalBasicColumnSpecification(item, state) : PrevalResult.unmodified(item);
        }
        if (this.runtime.isInstanceOf(item, MetamodelPaths.TDS_AGGREGATE_VALUE) || this.runtime.isInstanceOf(item, MetamodelPaths.COLLECTION_AGGREGATE_VALUE))
        {
            return prevalPair(item, "mapFn", "aggregateFn", state);
        }
        if (this.runtime.isInstanceOf(item, MetamodelPaths.AGG_COL_SPEC))
        {
            return prevalPair(item, "map", "reduce", state);
        }
        if (this.runtime.isInstanceOf(item, MetamodelPaths.AGG_COL_SPEC_ARRAY))
        {
            ListIterable<PrevalResult> specs = this.runtime.getPropertyValues(item, "aggSpecs").collect(s -> this.preval.apply(s, state));
            return PrevalResult.anyModified(specs)
                    ? new PrevalResult(this.runtime.withPropertyValues(item, "aggSpecs", specs.collect(PrevalResult::getValue)), true, Lists.immutable.empty(), true)
                    : PrevalResult.unmodified(item);
        }
        return null;
    }

    private PrevalResult prevalBasicColumnSpecification(Object item, PrevalState state)
    {
        Object function = this.runtime.getPropertyValue(item, "func");
        MutableMap<String, ImmutableList<Object>> vars = Maps.mutable.withMapIterable(state.getInScopeVars());
        if (function instanceof LambdaFunction)
        {
            this.runtime.openVariableValues((LambdaFunction<?>) function).forEachKeyValue(vars::put);
        }
        PrevalResult result = this.preval.apply(function, state.withInScopeTypeParams(Maps.immutable.empty()).withInScopeVars(vars.toImmutable()));
        return result.isModified()
                ? new PrevalResult(this.runtime.withPropertyValues(item, "func", Lists.immutable.with(result.getValue())), true, Lists.immutable.empty(), true)
                : PrevalResult.unmodified(item);
    }

    private PrevalResult prevalPair(Object item, String first, String second, PrevalState state)
    {
        PrevalResult firstResult = this.preval.apply(this.runtime.getPropertyValue(item, first), state);
        PrevalResult secondResult = this.preval.apply(this.runtime.getPropertyValue(item, second), state);
        if (!firstResult.isModified() && !secondResult.isModified())
        {
            return PrevalResult.unmodified(item);
        }
        Object withFirst = this.runtime.withPropertyValues(item, first, Lists.immutable.with(firstResult.getValue()));
        return new PrevalResult(this.runtime.withPropertyValues(withFirst, second, Lists.immutable.with(secondResult.getValue())), true, Lists.immutable.empty(), true);
    }
}
```
In `Preevaluator.dispatch`, add the following straight after the `KeyExpression` branch. Pure checks lambda holders after `KeyExpression` and before the leaves.
```java
        PrevalResult holder = this.lambdaHolders.prevalHolder(item, state);
        if (holder != null)
        {
            return holder;
        }
```
Construct `this.lambdaHolders = new LambdaHolders(runtime, this::prevalInternal);` in the constructor.

Pure's `openVariableValues()` on a BCS function also applies to non-lambda functions, where it returns an empty map. So Java calls it only for lambdas, and treats anything else as empty.

Adapters:
- **compiled `getPropertyValue`:** `((CoreInstance) instance).getValueForMetaPropertyToOne(property)`. **Caution:** compiled objects wrap primitives in `ValCoreInstance`. The holder properties used here are all functions or class instances, so no unwrapping is needed; record it if one isn't.
- **compiled `getPropertyValues`:** `Lists.immutable.withAll(((CoreInstance) instance).getValueForMetaPropertyToMany(property))`.
- **compiled `withPropertyValues`:** `CompiledSupport.copy(instance)`, then find the public method named `"_" + property` taking a single `RichIterable` parameter (to-many) or a single non-primitive parameter (to-one), and invoke it by reflection. For to-one properties, pass `values.getOnly()`.
- **interpreted:** `Instance.getValueForMetaPropertyToOneResolved` / `getValueForMetaPropertyToManyResolved`, and `copy(...)` with an override (Task 5).

- [ ] **Step 3: Run GREEN**

Run the Task 4 Step 6 commands. Expected: `Test_Pure_Preeval_Java` passes all enabled targets under `JAVA` and `SHADOW`, `Test_Pure_Preeval` is 111/111, and the harness is green in both modes.

- [ ] **Step 4: Commit**

```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval/src \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-compiled-functions-preeval/src \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-interpreted-functions-preeval/src \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/test/java/org/finos/legend/engine/pure/code/core/Test_Pure_Preeval_Java.java
git status --short   # confirm only this task's files are staged
git commit -m "Pre-evaluate lambda holders in the Java preeval"
```

---

### Task 8: Errors carry the native call site

**Files:**
- Modify: `COMPILED/.../natives/PrevalNative.java`: `hasSrcInformation=true`, adding the parameter type `"org.finos.legend.pure.m4.coreinstance.SourceInformation"` before `"ExecutionSupport"`
- Modify: `COMPILED/.../CompiledPreeval.java`: `preval(Object, PureMap, PureMap, CoreInstance, CoreInstance, SourceInformation, ExecutionSupport)`
- Modify: `COMPILED/.../CompiledPrevalRuntime.java`: constructor takes `SourceInformation` and `error(...)` uses it
- Modify: `SHARED/.../Scope.java` callers: the core catches the `IllegalStateException` from `resolveVariable` and rethrows it as `runtime.error(e.getMessage())`
- Test: `AbstractTestPrevalNative`

**Interfaces:**
- Produces: every error the Java preeval raises is a `PureExecutionException` with the source information of the `prevalNative` call site.

- [ ] **Step 1: Failing harness test**

```java
    @Test
    public void testUnsupportedValueIsReportedAtTheCallSite()
    {
        compileTestSource("unsupported.pure", "Class test::preeval::Opaque {}\n");
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "let opaque = ^test::preeval::Opaque();",
                "let hooks = ^PrevalHooks(stopPreeval = {a:Any[*] | !$a->forAll(x | $x->instanceOf(test::preeval::Opaque))}, shouldInline = {f:Function<Any>[1] | false}, isGeneratedMilestoningProperty = {f:Function<Any>[1] | false}, isGetAllFunction = {f:Function<Any>[1] | false}, resolveTdsSchema = {vs:ValueSpecification[1], vars:Map<String, List<Any>>[1] | []});",
                "let vars = newMap(pair('v', list($opaque)));",
                "let fe = {|$v}->evaluateAndDeactivate().expressionSequence->at(0);",
                "assertError(|prevalNative($fe, $vars, $vars, $hooks, noDebug()), {m:String[1], s:SourceInformation[0..1] | assert($m->contains('Unsupported type: test::preeval::Opaque') && $s->isNotEmpty(), |$m)});");
    }
```
Delete `unsupported.pure` in the existing `@After` alongside `fromString.pure`. If `{|$v}` does not compile because of the undeclared `v`, bind `let v = $opaque;` first. The explicit `vars` map drives substitution either way.

`$s->isNotEmpty()` must be a platform function. If it is not available in the harness, use `$s->size() == 1`.

- [ ] **Step 2: RED**

Run the family build with `-Dmdep.analyze.skip=true -fae`. Expected: the test fails in compiled mode because the source information is empty (null). Interpreted mode may already pass, because it reads the call stack.

- [ ] **Step 3: Implement** as listed under **Files**.

- [ ] **Step 4: GREEN.** Run the family build without the skip flags, then the Task 4 Step 6 commands.

Expected: the harness is 6/6 in both modes, and the compiled-core runners are unchanged.

- [ ] **Step 5: Commit**

```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval/src \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-compiled-functions-preeval/src \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-interpreted-functions-preeval/src
git status --short   # confirm only this task's files are staged
git commit -m "Report Java preeval errors at the prevalNative call site"
```

---

### Task 9: P1 exit verification, Checkstyle and spec status

**Files:**
- Modify: `docs/superpowers/specs/2026-09-27-preeval-java-native-design.md`: mark P1 done in §5, list the tests deferred to P2 with reasons, and tick off each "P1 requirements carried from P0 review" item that P1 satisfied.

- [ ] **Step 1: Full regression**

```bash
. /home/aziem/bin/jdk11.sh && mvn -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -DargLine= \
  -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core \
  org.apache.maven.plugins:maven-surefire-plugin:2.22.2:test -Dtest='Test_Pure_Core,Test_Pure_Preeval,Test_Pure_Preeval_Java'
```
Expected: `Failures: 0, Errors: 0` for all three. `Test_Pure_Core` is the router hot-path regression gate, and it still runs PURE by default.

- [ ] **Step 2: Checkstyle**

Run on the four preeval modules and compiled-core (Global Constraints command). Expected: 0 violations.

- [ ] **Step 3: Spec update, then commit**

```bash
git add docs/superpowers/specs/2026-09-27-preeval-java-native-design.md
git commit -m "Record P1 completion and deferred tests in the preeval spec"
```

## P1 exit criteria (spec §5)

- `Test_Pure_Preeval_Java` is green under both `JAVA` and `SHADOW` for every enabled target. `DEFERRED_TO_P2` holds only tests that need P2 rules, each with its reason written down.
- The harness is green in compiled and interpreted modes, covering folding, substitution, open variables and call-site errors.
- `Test_Pure_Core` and `Test_Pure_Preeval` pass with the default `PURE`.
- The carried P0 requirements are done:
  - SHADOW compares types and multiplicities (T2);
  - Java does not mutate its inputs: every change goes through `with*` copies (T3–T7);
  - JAVA rejects a State with type parameters, path or depth (T2);
  - errors carry the call-site source information (T8);
  - the adapters construct results and instances (T4/T5);
  - non-empty `openVars` are exercised (T5);
  - the switch property is validated once per value (T1);
  - the RED runs use `-Dmdep.analyze.skip=true` (all tasks).
