# Preeval Java Native — P2 Rules Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Port the remaining preeval rewrite rules to Java, so that every `tests.pure` test and the relational `testPreeval.pure` test passes under `JAVA` and `SHADOW` in compiled mode. The rules are:
- `if`, `and`/`or`;
- function inlining and `eval` expansion;
- `map`/`fold` unrolling and `concatenate`;
- cast-of-empty, filter-constant, `toOne`/`toOneMany` and `genericType`;
- TDS `columns` and eval-on-Column.

**Architecture:** First extract P1's inline logic into an explicit rule structure, with no behaviour change:
- `PreParameterRule`s run before parameters are pre-evaluated (`if`, `and`/`or`);
- `ExpressionRule`s run after the parameter prologue, in three ordered lists (`EXPANSION`, `NOT_PREVALLED`, and the final `REACTIVATE`);
- the rule order is pinned by a test.

Then fix the P1 port gaps (`values`, `isPureOne`, error text). Then turn the target runner into "every test except a checked-in `KNOWN_DIVERGENT` list" and record the baseline. Each rule task then empties part of that list. Mode-specific operations stay behind `PrevalRuntime`, and Pure callbacks stay behind `PrevalHooks`.

**Tech Stack:** Java 11, legend-pure 5.105.0, Eclipse Collections, Pure, JUnit 5 (unit tests), JUnit 3/4 (Pure harness and suite runners).

**Spec:** `docs/superpowers/specs/2026-09-27-preeval-java-native-design.md`. This is phase **P2** of §5. Also read "P2 requirements carried from P1 review" and the "P1 structure vs §3.4/§3.5" note.
**Previous plans:** `docs/superpowers/plans/2026-09-27-preeval-java-native-p0-scaffold.md` and `docs/superpowers/plans/2026-09-27-preeval-java-native-p1-core-traversal.md`. Both are complete on branch `preeval-native`.

## Global Constraints

- JDK 11. Prefix every Maven command with `. /home/aziem/bin/jdk11.sh &&`.
- Every Maven command carries `-Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true`. Lifecycle builds also carry `-T 3` and always `clean`. Build only the touched modules (`-pl`).
- Direct Surefire runs (`org.apache.maven.plugins:maven-surefire-plugin:2.22.2:test`) **must** pass `-DargLine=`. To give the forked JVM a system property, put it inside `argLine`, for example `-DargLine=-Dlegend.engine.preeval.test.includeKnownDivergent=true`.
- Checkstyle: `mvn checkstyle:check -Dcheckstyle.config.location=$(pwd)/checkstyle.xml -pl <modules>`, run from the repo root.
- TDD RED for Java changes uses `-Dmdep.analyze.skip=true`. The final GREEN build runs without it and must be analyzer-clean (`failOnWarning=true, ignoreNonCompile=true`).
- New files get the Apache 2.0 header, first line `Copyright 2026 Goldman Sachs`. Java uses 4-space indent, braces on their own lines, no tabs. Pure uses 2-space indent.
- No explanatory comments. The only exception is `// parity: <reason>` for a Pure quirk that is reproduced on purpose.
- Commits are authored solely by the user: **no** `Co-Authored-By` or `Claude-Session` trailers. Messages are sentence-case imperative. Never `git add -A` / `git add .`. Never a bare `git stash`. Do not touch the untracked `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/test/java/org/finos/legend/engine/pure/code/core/interpreted/` directory.
- `core_functions_preeval` stays **platform-only**. Shared-harness Pure snippets use `platform` functions only.
- The switch default stays `PURE`. Nothing in P2 changes production behaviour.
- **Parity rule:** port Pure's behaviour exactly, quirks included, marking each quirk `// parity:`. **Non-mutation rule:** every rewrite goes through a `PrevalRuntime.with…` copy. **Pure wins:** where this plan's reference Java disagrees with `preeval.pure`, the Pure behaviour is correct. Fix the Java and record the difference.

## Background the implementer needs

- **Pure source being ported:** `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/main/resources/core/pure/router/preeval/preeval.pure`. Line numbers are approximate. Search by function or handler.

  | Rule | Where in preeval.pure |
  |---|---|
  | `if` handler | `prevalInternal`, the handler list at ~171 |
  | `and`/`or` | `prevalBooleanFunctionExpression`, ~441 |
  | Inline, eval expansion, and the `notPrevalReason` handler list | `prevalGenericFunctionExpression`, ~519–855 |
  | `shouldInline` | ~1049 |
  | `canInlineEvalFunctionExpression` / `isEval` / `getFirstParamOfEval` / `convertEvalExpressionToAppliedFunctionExpression` | ~1058–1094 |
  | `addToScope` (three overloads) | ~1107–1160 |
  | `isFilterFunctionReturningConstant` / `isGetAll` | ~881–898 |

- **Java code as P1 left it**, in `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval/src/main/java/org/finos/legend/engine/pure/preeval/`:
  - `Preevaluator`: the whole traversal. `prevalFunctionExpression` holds the parameter prologue, the stop check, `notPrevalReason` and re-activation, all inline.
  - `Scope`, `GenericTypes`, `LambdaHolders`, `PrevalState`, `PrevalResult`, `PrevalRuntime`, `PrevalHooks` (with `stopPreeval` only), `MetamodelPaths`, `DebugTrace`.
- **Adapters:**
  - `…-compiled-functions-preeval/.../compiled/{CompiledPreeval, CompiledPrevalRuntime, CompiledPrevalHooks}`. The compiled hooks read a Pure `PrevalHooks` property with `getValueForMetaPropertyToOne(name)` and call it through `Pure.evaluate(es, fn, CoreGen.bridge, args...)`.
  - `…-interpreted-functions-preeval/.../interpreted/{InterpretedPrevalRuntime, InterpretedPrevalHooks, natives/PrevalNative}`. The interpreted hooks evaluate through `InterpretedPrevalRuntime.evaluateBoolean(fn, ctx, values)`, which uses the `Filter.java` pattern.
- **The Pure `PrevalHooks` class** (repo `core_functions_preeval`) already has these properties: `stopPreeval`, `shouldInline`, `isGeneratedMilestoningProperty`, `isGetAllFunction`, `resolveTdsSchema`. The last is `Function<{ValueSpecification[1], Map<String, List<Any>>[1]->Any[*]}>`. compiled-core's `toPrevalHooks` fills them from `State` and the extensions.
- **Runner:** `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/test/java/org/finos/legend/engine/pure/code/core/Test_Pure_Preeval_Java.java` runs the enabled tests under `JAVA` and `SHADOW`. It saves and restores the system property. It rejects any enabled name that doesn't match exactly one collected test.
- **Harness:** `…-shared-functions-preeval/src/test/java/org/finos/legend/engine/pure/preeval/AbstractTestPrevalNative.java` (8 tests). `HOOKS` has `stopPreeval` returning false for FunctionExpressions and true otherwise, and `shouldInline = false`. Build custom hooks inline when a test needs inlining.
- **Verified legend-pure API facts** (5.105.0). If `javap` disagrees, trust `javap` and record the deviation.
  - Compiled: `CompiledSupport.copy(T)` plus the typed `_x(...)` setters, returning the copy. `Pure.evaluate`, `Pure.reactivate`, `Pure.getOpenVariables`, `Pure.instanceOf`. `CoreGen.bridge`. `CoreGen.safeGetGenericType(Object, ExecutionSupport)` gives the Pure type of any value, raw primitives included.
  - Interpreted: `ProcessorSupport.newEphemeralAnonymousCoreInstance`, and the `Copy.java` pattern already in `InterpretedPrevalRuntime.copy`. `Instance.getValueForMetaPropertyToManyResolved` resolves stubs. `InstanceValueCoreInstanceWrapper._values()` goes through `AnyHelper.UNWRAP_PRIMITIVES`, turning a Float's `BigDecimal` into a `double`, and does **not** resolve stubs. `Multiplicity.newMultiplicity(int, ps)`. The `OpenVariableValues.java` pattern builds a `MapCoreInstance`.
- **Pure helper semantics the rules rely on**, all verified:
  - `isMultiplicityConcrete(m)` is `m.multiplicityParameter->isEmpty()`.
  - `hasLowerBound(m)`: `lowerBound` is present and its value is present **and not 0**. This is a quirk, so a lower bound of 0 counts as "no lower bound".
  - `hasUpperBound(m)`: `upperBound` is present, its value is present, and the value is not `-1`.
  - `getLowerBound(m)` is `m.lowerBound.value`.
  - `isConcrete(genericType)` is `genericType.rawType->isNotEmpty()`.
  - `Multiplicity == PureOne` is **identity** with the canonical packaged multiplicity.
  - `collection == scalar` is true only when the collection holds exactly one element equal to the scalar.

## Expected rule coverage (from static classification; the baseline run in Task 3 is authoritative)

| Task | Rule family | Tests expected to leave `KNOWN_DIVERGENT` |
|---|---|---|
| 4 | `if`, `and`/`or` | testPrerouting23a, testPrerouting23b, testPrerouting24a, testPrerouting24c, testPrerouting24d, testPrerouting24e, testPrerouting24f |
| 5 | inlining, `eval` expansion | testPrerouting2b, testPrerouting6, testPrerouting7, testPrerouting10–18, testPrerouting20, testPrerouting25a/b/c, testPrerouting26, testPrerouting27b, testPrerouting28, testPrerouting32b, testPrerouting35, testPrerouting37, testPrerouting38, testPrerouting40a, testDecimalType, testEvalWithArgs1–5, testLetOnlyStatement, testInline, testAdditionalStopFunction, testProjectWithInferredParameterType, testPrerouting24b, testRecursiveSimpleConcreteFunctionDefinition, testPrerouting_OptionalLimit1, testPrerouting_OptionalLimit2, testPrerouting_mappedTdsAgg |
| 6 | `map`/`fold` unroll, `concatenate` | testPrerouting19, testPrerouting21, testPrerouting29a, testPrerouting29b, testPrerouting30, testPrerouting31b, testPrerouting_mapOnInstanceValuesExpanded, testPrerouting_foldOnInstanceValuesExpanded, testPrerouting_foldOnInstanceValuesExpanded2, testPrerouting_concatenateInstanceValuesExpanded_Basic, testPrerouting_concatenateInstanceValuesExpanded_Complex |
| 7 | cast-empty, filter false/true, `toOne`/`toOneMany`, `genericType` | testPrerouting_castEmptyCollection, testFilterFalseSimplification, testFilterFalseSimplification2, testFilterFalseConstantSimplification, testFilterFalseSimplificationReturnType, testFilterTrueSimplification, testFilterTrueSimplification2, testFilterTrueConstantSimplification, testToOneManyElimination3, testToOneElimination3, testPrerouting40b, testGetGenericType |
| 8 | TDS `columns`, eval-on-Column | testPrerouting33, testPrerouting34, tesColumnEvalOnRelation, tesColumnEvalOnRelationWithCast |

Some of these tests need rules from several families; each leaves the list only once all of its rules exist. Some may already pass at baseline, because re-activation happens to give the same result. After each rule task, run with `includeKnownDivergent=true` and remove **every** name that now passes, whichever family it was expected in.

## File Structure

```
SHARED  src/main/java/org/finos/legend/engine/pure/preeval/
  PrevalServices.java          CREATE (T1): what rules may call back into
  Prologue.java                CREATE (T1): immutable result of the parameter prologue
  PreParameterRule.java        CREATE (T1)
  ExpressionRule.java          CREATE (T1)
  Rules.java                   CREATE (T1, extended T4–T8): the ordered rule lists
  rules/ReactivateRule.java    CREATE (T1): P1's re-activation, moved
  rules/IfRule.java, rules/AndOrRule.java                                     CREATE (T4)
  rules/InlineRule.java, rules/EvalExpansionRule.java                         CREATE (T5)
  rules/MapUnrollRule.java, rules/FoldUnrollRule.java, rules/ConcatenateRule.java   CREATE (T6)
  rules/EmptyCastRule.java, rules/FilterFalseRule.java, rules/FilterTrueRule.java,
  rules/ToOneManyRule.java, rules/ToOneRule.java, rules/GenericTypeRule.java    CREATE (T7)
  rules/TdsColumnsRule.java, rules/EvalOnColumnRule.java                      CREATE (T8)
  rules/RuleSupport.java       CREATE (T4): shared static helpers for rules
  rules/UnrollSupport.java     CREATE (T6): shared map/fold helpers
  Multiplicities.java          CREATE (T6): Pure multiplicity helper semantics
  Preevaluator.java            MODIFY (T1, T2, T5)
  Scope.java                   unchanged; addToScope lives in Preevaluator (T5), exposed via PrevalServices
  PrevalRuntime.java, PrevalHooks.java, MetamodelPaths.java, DebugTrace.java   MODIFY (T1–T8)
SHARED  src/test/java/org/finos/legend/engine/pure/preeval/
  TestRules.java               CREATE (T1, updated T4–T8): pins rule order
  AbstractTestPrevalNative.java MODIFY (T2, T4–T7): both-mode scenarios
COMPILED / INTERPRETED adapters and hooks   MODIFY (T2, T4–T8)
compiled-core src/test/java/.../Test_Pure_Preeval_Java.java   MODIFY (T3–T8)
docs/superpowers/plans/2026-09-28-preeval-p2-shadow-baseline.md   CREATE (T3), updated by T4–T8
legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/test/java/org/finos/legend/pure/code/core/relational/Test_Pure_Relational_Preeval_Java.java   CREATE (T9)
docs/superpowers/specs/2026-09-27-preeval-java-native-design.md   MODIFY (T9)
```

Short names below: `SHARED`, `COMPILED`, `INTERPRETED` are the three preeval Java modules (paths as in Background); `CC` is `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core`.

**Standard verification commands**, referred to by name in the tasks:
- **FAMILY**:
  ```bash
  . /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-compiled-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-interpreted-functions-preeval
  ```
- **CORE-BUILD**:
  ```bash
  . /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -DskipTests -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core
  ```
- **RUNNER**:
  ```bash
  . /home/aziem/bin/jdk11.sh && mvn -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -DargLine= -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core org.apache.maven.plugins:maven-surefire-plugin:2.22.2:test -Dtest='Test_Pure_Preeval_Java,Test_Pure_Preeval'
  ```
- **RUNNER-ALL**: RUNNER with `-DargLine=-Dlegend.engine.preeval.test.includeKnownDivergent=true` in place of `-DargLine=`. It also runs the known-divergent tests, to see which now pass.

---

### Task 1: Extract the rule structure (no behaviour change)

**Files:**
- Create: `SHARED/.../PrevalServices.java`, `Prologue.java`, `PreParameterRule.java`, `ExpressionRule.java`, `Rules.java`, `rules/ReactivateRule.java`
- Modify: `SHARED/.../Preevaluator.java` (implements `PrevalServices`; `prevalFunctionExpression` is driven by `Rules`), `DebugTrace.java` (adds `message`)
- Test: `SHARED/src/test/java/org/finos/legend/engine/pure/preeval/TestRules.java`

**Interfaces:**
- Produces:
  - `interface PrevalServices`, package-private and implemented by `Preevaluator`:
    - `PrevalResult preval(Object, PrevalState)`
    - `PrevalRuntime runtime()`, `PrevalHooks hooks()`, `Scope scope()`, `GenericTypes genericTypes()`
    - `boolean isInstanceValue(Object, ImmutableMap<String, ImmutableList<Object>>)`
    - `ImmutableList<String> openVars(Iterable<PrevalResult>, PrevalState)`
    - `void trace(PrevalState, String)`
  - `final class Prologue`:
    - getters `original()`, `rewritten()`, `parameters(): ImmutableList<PrevalResult>`, `modified()`, `openVars()`, `state()`, `notPrevalReason()` (nullable)
    - `rewrittenParameters(): ImmutableList<ValueSpecification>`
    - `notPrevalled(): PrevalResult`
    - `withNotPrevalReason(String): Prologue`
  - `interface PreParameterRule`: `boolean matches(FunctionExpression, PrevalServices)`; `PrevalResult apply(FunctionExpression, PrevalState, PrevalServices)`. A `null` return means "fall through to generic processing".
  - `interface ExpressionRule`: `boolean matches(Prologue, PrevalServices)`; `PrevalResult apply(Prologue, PrevalServices)`.
  - `final class Rules`: `static final ImmutableList<PreParameterRule> PRE_PARAMETER`, `static final ImmutableList<ExpressionRule> EXPANSION`, `static final ImmutableList<ExpressionRule> NOT_PREVALLED`, `static final ExpressionRule REACTIVATE`.

- [ ] **Step 1: Write the rule-order pin test**

```java
package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.factory.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestRules
{
    @Test
    public void testPreParameterRuleOrder()
    {
        Assertions.assertEquals(Lists.immutable.empty(), Rules.PRE_PARAMETER.collect(r -> r.getClass().getSimpleName()));
    }

    @Test
    public void testExpansionRuleOrder()
    {
        Assertions.assertEquals(Lists.immutable.empty(), Rules.EXPANSION.collect(r -> r.getClass().getSimpleName()));
    }

    @Test
    public void testNotPrevalledRuleOrder()
    {
        Assertions.assertEquals(Lists.immutable.empty(), Rules.NOT_PREVALLED.collect(r -> r.getClass().getSimpleName()));
    }

    @Test
    public void testReactivationIsTheFallback()
    {
        Assertions.assertEquals("ReactivateRule", Rules.REACTIVATE.getClass().getSimpleName());
    }
}
```

- [ ] **Step 2: RED**

Run FAMILY with `-Dmdep.analyze.skip=true`. Expected: `COMPILATION ERROR`, because `Rules` does not exist yet.

- [ ] **Step 3: Implement**

`PrevalServices.java`:
```java
package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.map.ImmutableMap;

public interface PrevalServices
{
    PrevalResult preval(Object item, PrevalState state);

    PrevalRuntime runtime();

    PrevalHooks hooks();

    Scope scope();

    GenericTypes genericTypes();

    boolean isInstanceValue(Object value, ImmutableMap<String, ImmutableList<Object>> inScopeVars);

    ImmutableList<String> openVars(Iterable<PrevalResult> results, PrevalState state);

    void trace(PrevalState state, String message);
}
```
The rules live in the `rules` subpackage, so `PrevalServices`, `Prologue`, `PreParameterRule`, `ExpressionRule`, `PrevalResult`, `PrevalState`, `Scope`, `GenericTypes`, `PrevalRuntime`, `PrevalHooks` and `MetamodelPaths` must all be `public`. Make them public where they are not. `DebugTrace` stays package-private and is reached through `PrevalServices.trace`.

`Prologue.java`:
```java
package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.FunctionExpression;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

public final class Prologue
{
    private final FunctionExpression original;
    private final FunctionExpression rewritten;
    private final ImmutableList<PrevalResult> parameters;
    private final boolean modified;
    private final ImmutableList<String> openVars;
    private final PrevalState state;
    private final String notPrevalReason;

    public Prologue(FunctionExpression original, FunctionExpression rewritten, ImmutableList<PrevalResult> parameters, boolean modified, ImmutableList<String> openVars, PrevalState state, String notPrevalReason)
    {
        this.original = original;
        this.rewritten = rewritten;
        this.parameters = parameters;
        this.modified = modified;
        this.openVars = openVars;
        this.state = state;
        this.notPrevalReason = notPrevalReason;
    }

    public FunctionExpression original()
    {
        return this.original;
    }

    public FunctionExpression rewritten()
    {
        return this.rewritten;
    }

    public ImmutableList<PrevalResult> parameters()
    {
        return this.parameters;
    }

    public boolean modified()
    {
        return this.modified;
    }

    public ImmutableList<String> openVars()
    {
        return this.openVars;
    }

    public PrevalState state()
    {
        return this.state;
    }

    public String notPrevalReason()
    {
        return this.notPrevalReason;
    }

    public ImmutableList<ValueSpecification> rewrittenParameters()
    {
        return Lists.immutable.withAll(this.rewritten._parametersValues());
    }

    public PrevalResult notPrevalled()
    {
        return new PrevalResult(this.rewritten, PrevalResult.allCanPreval(this.parameters), this.openVars, this.modified);
    }

    public Prologue withNotPrevalReason(String reason)
    {
        return new Prologue(this.original, this.rewritten, this.parameters, this.modified, this.openVars, this.state, reason);
    }
}
```
`PreParameterRule.java` and `ExpressionRule.java`, following the Interfaces block above:
```java
public interface PreParameterRule
{
    boolean matches(FunctionExpression expression, PrevalServices services);

    PrevalResult apply(FunctionExpression expression, PrevalState state, PrevalServices services);
}
```
```java
public interface ExpressionRule
{
    boolean matches(Prologue prologue, PrevalServices services);

    PrevalResult apply(Prologue prologue, PrevalServices services);
}
```
`Rules.java`:
```java
package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.pure.preeval.rules.ReactivateRule;

public final class Rules
{
    public static final ImmutableList<PreParameterRule> PRE_PARAMETER = Lists.immutable.empty();
    public static final ImmutableList<ExpressionRule> EXPANSION = Lists.immutable.empty();
    public static final ImmutableList<ExpressionRule> NOT_PREVALLED = Lists.immutable.empty();
    public static final ExpressionRule REACTIVATE = new ReactivateRule();

    private Rules()
    {
    }
}
```
`rules/ReactivateRule.java` is P1's final re-activation block, moved verbatim:
```java
package org.finos.legend.engine.pure.preeval.rules;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.pure.preeval.ExpressionRule;
import org.finos.legend.engine.pure.preeval.MetamodelPaths;
import org.finos.legend.engine.pure.preeval.PrevalResult;
import org.finos.legend.engine.pure.preeval.PrevalServices;
import org.finos.legend.engine.pure.preeval.PrevalState;
import org.finos.legend.engine.pure.preeval.Prologue;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;

public final class ReactivateRule implements ExpressionRule
{
    private static final String[] HOLDERS = {MetamodelPaths.BASIC_COLUMN_SPECIFICATION, MetamodelPaths.COLLECTION_AGGREGATE_VALUE, MetamodelPaths.TDS_AGGREGATE_VALUE, MetamodelPaths.AGG_COL_SPEC_ARRAY, MetamodelPaths.AGG_COL_SPEC};

    @Override
    public boolean matches(Prologue prologue, PrevalServices services)
    {
        return prologue.notPrevalReason() == null;
    }

    @Override
    public PrevalResult apply(Prologue prologue, PrevalServices services)
    {
        PrevalState state = prologue.state();
        ImmutableList<Object> reactivated = services.runtime().reactivate(prologue.rewritten(), state.getInScopeVars());
        PrevalState emptyScope = state.withInScopeTypeParams(Maps.immutable.empty()).withInScopeVars(Maps.immutable.empty());
        ImmutableList<Object> values = reactivated.collect(v -> isHolder(v, services) ? services.preval(v, emptyScope).getValue() : v);
        Object value = values.size() == 1 && values.getOnly() instanceof InstanceValue
                ? values.getOnly()
                : services.runtime().newInstanceValue(prologue.rewritten()._genericType(), services.runtime().exactly(values.size()), values);
        return new PrevalResult(value, true, Lists.immutable.empty(), true);
    }

    private static boolean isHolder(Object value, PrevalServices services)
    {
        for (String path : HOLDERS)
        {
            if (services.runtime().isInstanceOf(value, path))
            {
                return true;
            }
        }
        return false;
    }
}
```
`DebugTrace.message(PrevalState state, String text)` logs `[depth] text` at INFO when `state.isDebug()`.

In `Preevaluator`:
- Make the class `implements PrevalServices`.
- Add the trivial accessors, and make `isInstanceValue` public.
- Add `openVars(results, state)`, which is `this.scope.openVars(Lists.mutable.withAll(results).flatCollect(PrevalResult::getOpenVars), state.getInScopeVars())`.
- Add `trace`, which calls `DebugTrace.message`.
- Rewrite `prevalFunctionExpression` as below. Keep `notPrevalReason`, `parameterNameCount` and `isAnyOf` as they are, and delete `isReactivatedLambdaHolder`.

```java
    private PrevalResult prevalFunctionExpression(FunctionExpression expression, PrevalState state)
    {
        PreParameterRule preParameterRule = Rules.PRE_PARAMETER.detect(r -> r.matches(expression, this));
        if (preParameterRule != null)
        {
            PrevalResult handled = preParameterRule.apply(expression, state, this);
            if (handled != null)
            {
                return handled;
            }
        }
        Prologue prologue = prologue(expression, state);
        FunctionExpression rewritten = prologue.rewritten();
        boolean canPrevalFunction = !(this.runtime.hasStereotype(rewritten._func(), MetamodelPaths.FUNCTION_TYPE_PROFILE, "SideEffectFunction")
                || this.runtime.hasStereotype(rewritten._func(), MetamodelPaths.FUNCTION_TYPE_PROFILE, "NotImplementedFunction")
                || this.hooks.stopPreeval(Lists.immutable.with(rewritten)));
        if (!canPrevalFunction)
        {
            trace(state, "Unable to perform preval: " + this.runtime.typeDescription(rewritten._func()));
            boolean canPreval = this.runtime.isFunction(rewritten._func(), MetamodelPaths.LET_FUNCTION) && PrevalResult.allCanPreval(prologue.parameters());
            return new PrevalResult(rewritten, canPreval, prologue.openVars(), prologue.modified());
        }
        ExpressionRule expansion = Rules.EXPANSION.detect(r -> r.matches(prologue, this));
        if (expansion != null)
        {
            return expansion.apply(prologue, this);
        }
        Prologue reasoned = prologue.withNotPrevalReason(notPrevalReason(rewritten, prologue.parameters(), state));
        if (reasoned.notPrevalReason() != null)
        {
            ExpressionRule handler = Rules.NOT_PREVALLED.detect(r -> r.matches(reasoned, this));
            if (handler != null)
            {
                return handler.apply(reasoned, this);
            }
            trace(state, "Not prevalling (" + reasoned.notPrevalReason() + ")");
            return reasoned.notPrevalled();
        }
        trace(state, "Performing preval");
        return Rules.REACTIVATE.apply(reasoned, this);
    }

    private Prologue prologue(FunctionExpression expression, PrevalState state)
    {
        MutableList<? extends ValueSpecification> parameters = Lists.mutable.withAll(expression._parametersValues());
        // parity: Pure zips parameter names with values, so surplus values are dropped
        int parameterCount = parameters.isEmpty() ? 0 : Math.min(parameters.size(), parameterNameCount(expression._func()));
        ImmutableList<PrevalResult> results = parameters.subList(0, parameterCount).collect(p -> prevalInternal(p, state)).toImmutable();
        PrevalResult genericType = this.genericTypes.resolveGenericType(expression._genericType(), state);
        boolean modified = PrevalResult.anyModified(results) || genericType.isModified();
        FunctionExpression rewritten = expression;
        if (modified)
        {
            ListIterable<? extends ValueSpecification> newParameters = PrevalResult.anyModified(results)
                    ? results.collect(r -> this.runtime.withGenericType((ValueSpecification) r.getValue(), (GenericType) this.genericTypes.resolveGenericType(((ValueSpecification) r.getValue())._genericType(), state).getValue()))
                    : parameters;
            rewritten = this.runtime.withParametersAndGenericType(expression, newParameters, (GenericType) genericType.getValue());
        }
        return new Prologue(expression, rewritten, results, modified, openVars(results, state), state, null);
    }
```
Pure selects its dedicated handler by the exact function, and the handler itself decides whether to fall back to generic processing. In Java, a `PreParameterRule` that matches but returns `null` falls through in the same way.

- [ ] **Step 4: GREEN**

Run FAMILY (shared adds `TestRules` 4/4; the harness is 8/8 in each mode), then CORE-BUILD, then RUNNER. Expected:
- `Test_Pure_Preeval_Java` stays at 52/52;
- `Test_Pure_Preeval` stays at 111/111.

This is a refactor. Any change in a result is a bug.

- [ ] **Step 5: Commit**

```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval/src
git status --short
git commit -m "Extract ordered preeval rules from the Java traversal"
```

---

### Task 2: Read InstanceValue values and PureOne through the runtime; Pure-identical error text

**Files:**
- Modify: `SHARED/.../PrevalRuntime.java`: add `values`, `isPureOne`, `isPureZero`
- Modify: `SHARED/.../Preevaluator.java`, `LambdaHolders.java`, `rules/ReactivateRule.java`: every read of `InstanceValue._values()` goes through `runtime.values(iv)`, and every `== runtime.pureOne()` through `runtime.isPureOne(m)`
- Modify: `COMPILED/.../CompiledPrevalRuntime.java`, `INTERPRETED/.../InterpretedPrevalRuntime.java`
- Test: `SHARED/src/test/java/.../AbstractTestPrevalNative.java`

**Interfaces:**
- Produces:
  - `ImmutableList<Object> values(InstanceValue)`: compiled returns `_values()`; interpreted returns the stub-resolved CoreInstances from `Instance.getValueForMetaPropertyToManyResolved(iv, M3Properties.values, ps)`.
  - `boolean isPureOne(Multiplicity)` / `isPureZero(Multiplicity)`: identity with the canonical packaged multiplicity. In interpreted mode, compare the underlying CoreInstance when `m` is a wrapper.
  - `typeDescription(Object)` returns Pure's text:
    - for a value whose type is a `PackageableElement`, the element's user path (for example `Integer` for a raw `Long`);
    - otherwise the Pure type printed the way `Type->makeString()` prints it. Use legend-pure's type printer if `javap` shows one (for example in `org.finos.legend.pure.m3.navigation.type`); if not, fall back to the class simple name and record that.

    The compiled adapter gets the Pure type of a raw value with `CoreGen.safeGetGenericType(value, es)._rawType()`.

- [ ] **Step 1: Failing harness tests**

Add to `AbstractTestPrevalNative`:
1. `testFloatPrecisionSurvivesInstanceValueRebuild`. A 22-significant-digit Float literal is kept inside an InstanceValue that gets rebuilt, because a sibling value changes. The test asserts the literal is exactly equal afterwards.
   ```pure
   let vars = newMap(pair('x', list(2)));
   let x = 0;
   let iv = {|[1.1234567890123456789012, $x]}->evaluateAndDeactivate().expressionSequence->at(0);
   let r = prevalNative($iv, $vars, $vars, <HOOKS>, noDebug());
   assert($r.modified, |'expected modified');
   assert($r.value->cast(@InstanceValue).values->at(0) == 1.1234567890123456789012, |'expected exact Float');
   ```
   Adapt the snippet to how the collection literal is represented in each mode, and record what you changed. The test must fail in interpreted mode before the fix, and pass in both modes after it. If `pair`/`list` are not visible, build the map with `^Pair<String, List<Any>>(first = 'x', second = ^List<Any>(values = 2))`.
2. `testUnsupportedTypeNamesThePureType`. Use hooks where `stopPreeval` returns false for Integers, so `a->forAll(x | !$x->instanceOf(Integer))` holds only for non-Integers. Substituting `$v` with the value `3` must then raise an error whose message contains `Unsupported type: Integer` in **both** modes.

- [ ] **Step 2: RED**

Run FAMILY with `-Dmdep.analyze.skip=true -fae`. Expected:
- test 1 fails in interpreted mode;
- test 2 fails in compiled mode, where the message says `Long`.

- [ ] **Step 3: Implement** as described under **Interfaces**. `withValues` and `newInstanceValue` in interpreted mode keep rewrapping any Java primitive they are given. With `values()` fixed, they now only ever get CoreInstances.

- [ ] **Step 4: GREEN**

Run FAMILY (harness 10/10 in both modes), then CORE-BUILD, then RUNNER (52/52 and 111/111, unchanged).

- [ ] **Step 5: Commit**

```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval/src legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-compiled-functions-preeval/src legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-interpreted-functions-preeval/src
git status --short
git commit -m "Read preeval values and multiplicities through the runtime with Pure error text"
```

---

### Task 3: Run every preeval test against a checked-in divergence baseline

**Files:**
- Modify: `CC/src/test/java/org/finos/legend/engine/pure/code/core/Test_Pure_Preeval_Java.java`
- Create: `docs/superpowers/plans/2026-09-28-preeval-p2-shadow-baseline.md`

**Interfaces:**
- Produces:
  - The runner collects **every** test under `meta::pure::router::preeval::tests` that passes `satisfiesConditionsModular`, and runs them under `JAVA` and `SHADOW`.
  - It skips names in `static final ImmutableSet<String> KNOWN_DIVERGENT` unless the system property `legend.engine.preeval.test.includeKnownDivergent` is `true`.
  - `suite()` throws `IllegalStateException` if any `KNOWN_DIVERGENT` name does not match exactly one collected test.
  - `SKELETON`, `REACTIVATION`, `LAMBDA_HOLDERS` and `DEFERRED_TO_P2` are removed.

- [ ] **Step 1: Rewrite the selection**

Replace the enabled-set logic:
```java
    private static final String INCLUDE_KNOWN_DIVERGENT = "legend.engine.preeval.test.includeKnownDivergent";

    static final ImmutableSet<String> KNOWN_DIVERGENT = Sets.immutable.empty();

    private static TestSuite targets(CompiledExecutionSupport executionSupport)
    {
        boolean includeKnownDivergent = Boolean.getBoolean(INCLUDE_KNOWN_DIVERGENT);
        MutableBag<String> collected = Bags.mutable.empty();
        TestCollection tests = TestCollection.collectTests("meta::pure::router::preeval::tests", executionSupport.getProcessorSupport(), fn -> PureTestBuilderCompiled.generatePureTestCollection(fn, executionSupport), ci ->
        {
            if (!PureTestBuilder.satisfiesConditionsModular(ci, executionSupport.getProcessorSupport()))
            {
                return false;
            }
            String name = ((Function<?>) ci)._functionName();
            collected.add(name);
            return includeKnownDivergent || !KNOWN_DIVERGENT.contains(name);
        });
        ImmutableSet<String> unmatched = KNOWN_DIVERGENT.reject(name -> collected.occurrencesOf(name) == 1);
        if (unmatched.notEmpty())
        {
            throw new IllegalStateException("KNOWN_DIVERGENT names that do not match exactly one collected preeval test: " + unmatched.toSortedList().makeString(", "));
        }
        return PureTestBuilderCompiled.buildSuite(tests, executionSupport);
    }
```
Delete `enabledTargets()` and the slice sets.

- [ ] **Step 2: Record the baseline**

Run CORE-BUILD, then RUNNER-ALL, writing the output to a log. For every test that fails under `JAVA` and/or `SHADOW`, record:
- the test name;
- which implementation(s) fail;
- the first error line;
- the expected rule family from the plan's coverage table, or `unexpected` if it is not listed there.

Put these rows in `docs/superpowers/plans/2026-09-28-preeval-p2-shadow-baseline.md` (Apache header not needed for Markdown), and put the same names in `KNOWN_DIVERGENT`.

Stop and report DONE_WITH_CONCERNS in either of these cases:
- a test from the P1 target set (the 26 that passed at the end of P1) fails;
- an `unexpected` failure looks like a P1 bug rather than a missing rule.

- [ ] **Step 3: GREEN**

Run RUNNER. Expected: every collected test not in `KNOWN_DIVERGENT` passes under both implementations, and `Test_Pure_Preeval` is at 111/111.

- [ ] **Step 4: Commit**

```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/test/java/org/finos/legend/engine/pure/code/core/Test_Pure_Preeval_Java.java docs/superpowers/plans/2026-09-28-preeval-p2-shadow-baseline.md
git status --short
git commit -m "Run all preeval tests in Java against a known-divergence baseline"
```

---

### Task 4: `if` and `and`/`or`

**Files:**
- Create: `SHARED/.../rules/IfRule.java`, `rules/AndOrRule.java`, `rules/RuleSupport.java`
- Modify: `Rules.java` (`PRE_PARAMETER = [IfRule, AndOrRule]`), `TestRules.java`, `MetamodelPaths.java`, `PrevalRuntime.java` and both adapters
- Modify: `Test_Pure_Preeval_Java.KNOWN_DIVERGENT` and the baseline doc
- Test: `AbstractTestPrevalNative` (both modes)

**Interfaces:**
- Produces:
  - `MetamodelPaths.IF_FUNCTION = "meta::pure::functions::lang::if_Boolean_1__Function_1__Function_1__T_m_"`
  - `MetamodelPaths.AND_FUNCTION = "meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_"`
  - `MetamodelPaths.OR_FUNCTION = "meta::pure::functions::boolean::or_Boolean_1__Boolean_1__Boolean_1_"`
  - `MetamodelPaths.EVAL_FUNCTION = "meta::pure::functions::lang::eval_Function_1__V_m_"`
  - `PrevalRuntime.Boolean booleanValue(Object value)`: `null` when the value is not a Boolean. In compiled mode it is a `java.lang.Boolean`; in interpreted mode it is a Boolean CoreInstance.
  - `PrevalRuntime.Object function(String functionPath)`: the function element, looked up with `ps.package_getByUserPath` and cached.
  - `PrevalRuntime.FunctionExpression withFuncAndParameters(FunctionExpression, Object func, ListIterable<? extends ValueSpecification> parameters)`: a copy with `func` and `parametersValues` replaced.
  - `RuleSupport.singleBooleanEquals(PrevalServices, Object instanceValue, boolean expected)`: true when the InstanceValue holds exactly one value and that value is a Boolean equal to `expected`.

Check the if/and/or paths above against `preeval.pure` (`if_Boolean_1__Function_1__Function_1__T_m_`, `and_Boolean_1__Boolean_1__Boolean_1_`, `or_Boolean_1__Boolean_1__Boolean_1_`, `eval_Function_1__V_m_`) and correct any package prefix with `javap`/grep.

- [ ] **Step 1: Failing tests**

Update `TestRules.testPreParameterRuleOrder` to expect `["IfRule", "AndOrRule"]`. Add these harness tests, which run in both modes:
- `{|if(true, |'a', |[]->toOne())}`: the result is modified, and the lambda body folds to `'a'`. The dead branch is never evaluated, so there is no `toOne` error.
- The FE `{|true && $b}` with `b` not in scope: the result is the prevalled `$b` expression, modified, with open var `b`.
- The FE `{|$b || true}` with `b` not in scope: the result is an InstanceValue `true`, modified.

- [ ] **Step 2: RED**

Run FAMILY with `-Dmdep.analyze.skip=true -fae`. Expected: `TestRules` fails; the harness tests fail, or pass for the wrong reason, because the generic path gives a different value. Record which.

- [ ] **Step 3: Implement**

`rules/RuleSupport.java`:
```java
package org.finos.legend.engine.pure.preeval.rules;

import org.eclipse.collections.api.list.ImmutableList;
import org.finos.legend.engine.pure.preeval.PrevalServices;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.InstanceValue;

final class RuleSupport
{
    private RuleSupport()
    {
    }

    static boolean singleBooleanEquals(PrevalServices services, Object instanceValue, boolean expected)
    {
        if (!(instanceValue instanceof InstanceValue))
        {
            return false;
        }
        ImmutableList<Object> values = services.runtime().values((InstanceValue) instanceValue);
        if (values.size() != 1)
        {
            return false;
        }
        Boolean value = services.runtime().booleanValue(values.getOnly());
        return value != null && value == expected;
    }
}
```
`rules/IfRule.java` ports the `if` handler:
```java
public final class IfRule implements PreParameterRule
{
    @Override
    public boolean matches(FunctionExpression expression, PrevalServices services)
    {
        return services.runtime().isFunction(expression._func(), MetamodelPaths.IF_FUNCTION);
    }

    @Override
    public PrevalResult apply(FunctionExpression expression, PrevalState state, PrevalServices services)
    {
        ImmutableList<ValueSpecification> parameters = Lists.immutable.withAll(expression._parametersValues());
        PrevalResult condition = services.preval(parameters.get(0), state);
        if (!services.isInstanceValue(condition.getValue(), state.getInScopeVars()))
        {
            return null;
        }
        ValueSpecification branch = RuleSupport.singleBooleanEquals(services, condition.getValue(), true) ? parameters.get(1) : parameters.get(2);
        FunctionExpression evaluation = services.runtime().withFuncAndParameters(expression, services.runtime().function(MetamodelPaths.EVAL_FUNCTION), Lists.immutable.with(branch));
        return services.preval(evaluation, state).markModified();
    }
}
```
`rules/AndOrRule.java` ports `prevalBooleanFunctionExpression`:
```java
public final class AndOrRule implements PreParameterRule
{
    @Override
    public boolean matches(FunctionExpression expression, PrevalServices services)
    {
        return services.runtime().isFunction(expression._func(), MetamodelPaths.AND_FUNCTION) || services.runtime().isFunction(expression._func(), MetamodelPaths.OR_FUNCTION);
    }

    @Override
    public PrevalResult apply(FunctionExpression expression, PrevalState state, PrevalServices services)
    {
        boolean shortcut = !services.runtime().isFunction(expression._func(), MetamodelPaths.AND_FUNCTION);
        ImmutableList<ValueSpecification> parameters = Lists.immutable.withAll(expression._parametersValues());
        PrevalResult first = services.preval(parameters.get(0), state);
        if (services.isInstanceValue(first.getValue(), state.getInScopeVars()))
        {
            return RuleSupport.singleBooleanEquals(services, first.getValue(), shortcut)
                    ? first.markModified()
                    : services.preval(parameters.get(1), state).markModified();
        }
        PrevalResult second = services.preval(parameters.get(1), state);
        if (services.isInstanceValue(second.getValue(), state.getInScopeVars()))
        {
            return RuleSupport.singleBooleanEquals(services, second.getValue(), shortcut) ? second.markModified() : first.markModified();
        }
        ImmutableList<PrevalResult> both = Lists.immutable.with(first, second);
        FunctionExpression rewritten = services.runtime().withParametersAndGenericType(expression, both.collect(r -> (ValueSpecification) r.getValue()), expression._genericType());
        return new PrevalResult(rewritten, PrevalResult.allCanPreval(both), services.openVars(both, state), PrevalResult.anyModified(both));
    }
}
```
Pure's second branch reads `if($newParam2Wrapper.value->cast(@InstanceValue).values != $shortcutVal, | p1, | p2)`. So when the second value equals the shortcut, return `second`; otherwise return `first`. The code above does exactly that. Check it against the Pure source.

In `Rules`, set `PRE_PARAMETER = Lists.immutable.with(new IfRule(), new AndOrRule())`.

Adapters:
- `booleanValue`: compiled checks `instanceof Boolean`. Interpreted checks for a CoreInstance of Pure type Boolean, via `PrimitiveUtilities`, and uses `getBooleanValue`.
- `function`: cached `package_getByUserPath`.
- `withFuncAndParameters`: compiled uses `CompiledSupport.copy` then `_func(...)._parametersValues(...)` and returns the copy. Interpreted uses `copy(...)` with overrides for `func` and `parametersValues`.

- [ ] **Step 4: GREEN and baseline update**

Run FAMILY (harness green in both modes, `TestRules` green), then CORE-BUILD, then RUNNER-ALL. Remove every name that now passes from `KNOWN_DIVERGENT` and from the baseline doc. Then run RUNNER, which must pass with no failures, and `Test_Pure_Preeval` must stay at 111.

- [ ] **Step 5: Commit**

```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval/src legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-compiled-functions-preeval/src legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-interpreted-functions-preeval/src legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/test/java/org/finos/legend/engine/pure/code/core/Test_Pure_Preeval_Java.java docs/superpowers/plans/2026-09-28-preeval-p2-shadow-baseline.md
git status --short
git commit -m "Port the if and boolean short-circuit preeval rules to Java"
```

---

### Task 5: Function inlining and `eval` expansion

**Files:**
- Create: `SHARED/.../rules/InlineRule.java`, `rules/EvalExpansionRule.java`
- Modify:
  - `Preevaluator.java` and `PrevalServices.java`: add `addToScope`.
  - `PrevalState.java`: add `withPath(ImmutableList<Object>)`.
  - `PrevalHooks.java`: add `shouldInline` and `isGeneratedMilestoningProperty`.
  - `PrevalRuntime.java`: add `resolvedTypeParameters`, `typeParameterNames` and `isQualifiedPropertyOf`.
  - `MetamodelPaths.java`: add the eval signatures and `TDS_ROW`.
  - Both adapters and both hooks classes.
  - `Rules.java`: `EXPANSION = [InlineRule, EvalExpansionRule]`.
  - `TestRules.java`.
  - `KNOWN_DIVERGENT` and the baseline doc.
- Test: `AbstractTestPrevalNative` (both modes)

**Interfaces:**
- Produces:
  - `PrevalServices.PrevalState addToScope(PrevalState state, Object function, ListIterable<? extends GenericType> resolvedTypeParameters, ListIterable<? extends ValueSpecification> parameters, boolean cleanUp)` ports `addToScope` (~1117–1160):
    - type parameters are zipped by name with the resolved generic types, each passed through `genericTypes().resolveGenericType(gt, state)`;
    - parameters are zipped by name, skipping a parameter whose value is a single VariableExpression with the same name;
    - with `cleanUp`, start from empty in-scope vars and type params;
    - then add everything to both maps. A later key replaces an earlier one.
  - `PrevalState.withPath(ImmutableList<Object>)`.
  - `PrevalHooks.boolean shouldInline(Object function)`, `boolean isGeneratedMilestoningProperty(Object function)`.
  - `PrevalRuntime.ImmutableList<GenericType> resolvedTypeParameters(FunctionExpression)`, `ImmutableList<String> typeParameterNames(Object function)`, `boolean isQualifiedPropertyOf(Object function, String ownerPath)`.
  - `MetamodelPaths.EVAL_FUNCTIONS`: an `ImmutableList<String>` of the 8 eval signatures listed in `isEval` (~1067–1077). Take the exact names from the Pure source.
  - `MetamodelPaths.TDS_ROW = "meta::pure::tds::TDSRow"`.

- [ ] **Step 1: Failing tests**

Update `TestRules` so `EXPANSION` is `["InlineRule", "EvalExpansionRule"]`. Add these harness tests, which run in both modes:
- `testInlinesSingleExpressionFunction`:
  - Compile a helper `function test::preeval::addOne(i:Integer[1]):Integer[1] { $i + 1 }` into a separate test source, created and deleted inside the test.
  - Use hooks with `shouldInline = {f:Function<Any>[1] | $f->instanceOf(ConcreteFunctionDefinition)}`.
  - The FE `{|test::preeval::addOne(2)}` must fold to `3`, modified.
- `testDoesNotInlineRecursivePath`: a helper that calls itself must terminate. The `path` check stops re-inlining.
- `testExpandsEvalOfLiteralLambda`: `{|{x:Integer[1] | $x + 1}->eval(4)}` must fold to `5`.

- [ ] **Step 2: RED**

Run FAMILY with `-Dmdep.analyze.skip=true -fae`.

- [ ] **Step 3: Implement**

`InlineRule` ports `shouldInline` (~1049) and the inlining branch (~519–531):
```java
public final class InlineRule implements ExpressionRule
{
    @Override
    public boolean matches(Prologue prologue, PrevalServices services)
    {
        Object function = prologue.rewritten()._func();
        return function instanceof FunctionDefinition
                && Lists.mutable.withAll(((FunctionDefinition<?>) function)._expressionSequence()).size() == 1
                && !services.runtime().isQualifiedPropertyOf(function, MetamodelPaths.TDS_ROW)
                && !services.hooks().isGeneratedMilestoningProperty(function)
                && services.hooks().shouldInline(function)
                && !prologue.state().getPath().anySatisfy(p -> p == function);
    }

    @Override
    public PrevalResult apply(Prologue prologue, PrevalServices services)
    {
        FunctionExpression expression = prologue.rewritten();
        FunctionDefinition<?> function = (FunctionDefinition<?>) expression._func();
        PrevalState state = prologue.state();
        services.trace(state, "Inlining: " + services.runtime().typeDescription(function));
        PrevalState scoped = services.addToScope(state, function, services.runtime().resolvedTypeParameters(expression), prologue.rewrittenParameters(), true);
        PrevalState inlined = scoped.withPath(state.getPath().newWith(function));
        ValueSpecification body = Lists.immutable.<ValueSpecification>withAll(function._expressionSequence()).getOnly();
        ValueSpecification typed = services.runtime().withGenericType(body, (GenericType) services.genericTypes().resolveGenericType(body._genericType(), inlined).getValue());
        return services.preval(typed, inlined).markModified();
    }
}
```
Pure's `shouldInline` checks `$f->instanceOf(AbstractProperty) && hasGeneratedMilestoningPropertyStereotype`. The Pure hook `isGeneratedMilestoningProperty` built by compiled-core's `toPrevalHooks` already includes the `AbstractProperty` check.

`EvalExpansionRule` ports `canInlineEvalFunctionExpression` and the expansion branch (~532–547):
```java
public final class EvalExpansionRule implements ExpressionRule
{
    @Override
    public boolean matches(Prologue prologue, PrevalServices services)
    {
        Object first = firstParameter(prologue, services);
        return isEval(prologue, services) && first instanceof FunctionDefinition && Lists.mutable.withAll(((FunctionDefinition<?>) first)._expressionSequence()).size() == 1;
    }

    @Override
    public PrevalResult apply(Prologue prologue, PrevalServices services)
    {
        FunctionExpression expression = prologue.rewritten();
        FunctionDefinition<?> function = (FunctionDefinition<?>) firstParameter(prologue, services);
        PrevalState state = prologue.state();
        services.trace(state, "Expanding eval");
        ImmutableList<GenericType> typeParameters = services.runtime().resolvedTypeParameters(expression);
        ImmutableList<ValueSpecification> parameters = prologue.rewrittenParameters();
        PrevalState scoped = services.addToScope(services.addToScope(state, expression._func(), typeParameters, parameters, true), function, typeParameters, parameters.drop(1), false);
        ValueSpecification body = Lists.immutable.<ValueSpecification>withAll(function._expressionSequence()).getOnly();
        ValueSpecification typed = services.runtime().withGenericType(body, (GenericType) services.genericTypes().resolveGenericType(body._genericType(), scoped).getValue());
        return services.preval(typed, scoped).markModified();
    }

    static boolean isEval(Prologue prologue, PrevalServices services)
    {
        Object function = prologue.rewritten()._func();
        return MetamodelPaths.EVAL_FUNCTIONS.anySatisfy(path -> services.runtime().isFunction(function, path));
    }

    static Object firstParameter(Prologue prologue, PrevalServices services)
    {
        ValueSpecification first = prologue.rewrittenParameters().getFirst();
        return first instanceof InstanceValue ? services.runtime().values((InstanceValue) first).getOnly() : first;
    }
}
```
Pure's `addToScope(sfe, state)` passes `cleanUp = true`; the 4-argument overload passes `false`. Mirror that exactly.

`Preevaluator.addToScope`:
```java
    @Override
    public PrevalState addToScope(PrevalState state, Object function, ListIterable<? extends GenericType> resolvedTypeParameters, ListIterable<? extends ValueSpecification> parameters, boolean cleanUp)
    {
        ImmutableList<String> typeParameterNames = this.runtime.typeParameterNames(function);
        MutableMap<String, Object> typeParameters = Maps.mutable.empty();
        for (int i = 0; i < Math.min(typeParameterNames.size(), resolvedTypeParameters.size()); i++)
        {
            typeParameters.put(typeParameterNames.get(i), this.genericTypes.resolveGenericType(resolvedTypeParameters.get(i), state).getValue());
        }
        ImmutableList<String> parameterNames = this.runtime.parameterNames(function);
        MutableMap<String, ImmutableList<Object>> variables = Maps.mutable.empty();
        for (int i = 0; i < Math.min(parameterNames.size(), parameters.size()); i++)
        {
            ValueSpecification value = parameters.get(i);
            if (!(value instanceof VariableExpression && parameterNames.get(i).equals(((VariableExpression) value)._name())))
            {
                variables.put(parameterNames.get(i), Lists.immutable.with(value));
            }
        }
        PrevalState base = cleanUp ? state.withInScopeVars(Maps.immutable.empty()).withInScopeTypeParams(Maps.immutable.empty()) : state;
        return base.withInScopeVars(base.getInScopeVars().newWithAllKeyValues(variables.keyValuesView()))
                .withInScopeTypeParams(base.getInScopeTypeParams().newWithAllKeyValues(typeParameters.keyValuesView()));
    }
```
Hooks:
- Compiled: read the `shouldInline` and `isGeneratedMilestoningProperty` properties once in the constructor, as `stopPreeval` is, and evaluate each with a single argument through `Pure.evaluate`.
- Interpreted: `PrevalNative` resolves the two properties and passes them in, each with its own `getParentOrEmptyVariableContextForLambda` context. Evaluate through `evaluateBoolean` with a single-element list.

Runtime:
- `resolvedTypeParameters`: compiled uses `_resolvedTypeParameters()`; interpreted uses `Instance.getValueForMetaPropertyToManyResolved(fe, M3Properties.resolvedTypeParameters, ps)`, each wrapped with `GenericTypeCoreInstanceWrapper`.
- `typeParameterNames`: from `ps.function_getFunctionType(f)`, then `typeParameters`, then `name`.
- `isQualifiedPropertyOf`: true for a `QualifiedProperty` whose `owner` user path equals `ownerPath`.

In `Rules`, set `EXPANSION = Lists.immutable.with(new InlineRule(), new EvalExpansionRule())`.

- [ ] **Step 4: GREEN and baseline update.** Same procedure as Task 4 Step 4.

- [ ] **Step 5: Commit**

Stage the same paths as Task 4 Step 5. Message: `Port function inlining and eval expansion to the Java preeval`.

---

### Task 6: `map`/`fold` unrolling and `concatenate`

**Files:**
- Create: `SHARED/.../rules/MapUnrollRule.java`, `rules/FoldUnrollRule.java`, `rules/ConcatenateRule.java`, `SHARED/.../Multiplicities.java`
- Modify: `Rules.java` (`NOT_PREVALLED = [MapUnrollRule, FoldUnrollRule, ConcatenateRule]` for now; Task 8 inserts `TdsColumnsRule` and `EvalOnColumnRule` at the front), `TestRules.java`, `MetamodelPaths.java`, `PrevalRuntime.java` and both adapters, `KNOWN_DIVERGENT`, the baseline doc
- Test: `AbstractTestPrevalNative` (both modes)

**Interfaces:**
- Produces:
  - `MetamodelPaths.MAP_FUNCTIONS`: the 3 map signatures at ~602.
  - `MetamodelPaths.FOLD_FUNCTION = "…fold_T_MANY__Function_1__V_m__V_m_"`.
  - `MetamodelPaths.CONCATENATE_FUNCTION = "…concatenate_T_MANY__T_MANY__T_MANY_"`.
  - Take the package prefixes from `preeval.pure` or grep them.
  - `PrevalRuntime`:
    - `Long lowerBound(Multiplicity)` and `Long upperBound(Multiplicity)`: `null` when absent;
    - `boolean isMultiplicityConcrete(Multiplicity)`.
  - `Multiplicities`: static `hasLowerBound`, `hasUpperBound`, `isToOne`, following the Pure semantics in Background.

- [ ] **Step 1: Failing tests**

`TestRules`: `NOT_PREVALLED` is `["MapUnrollRule", "FoldUnrollRule", "ConcatenateRule"]`. Add these harness tests, which run in both modes:
- `{|[1, 2]->map(x | $x + 1)}` folds to `[2, 3]` with multiplicity exactly 2.
- `{|[1, 2, 3]->fold({x, a | $a + $x}, 0)}` folds to `6`.
- `{|[1, 2]->concatenate([3])}` folds to `[1, 2, 3]`.

- [ ] **Step 2: RED.** Run FAMILY with `-Dmdep.analyze.skip=true -fae`.

- [ ] **Step 3: Implement**

The rules below port the handlers at ~601–743. Two things matter here: `map` and `fold` resolve the body's type against the **original** expression's generic type (`$sfe.genericType`), and all three rules match only when every rewritten parameter `isInstanceValue`.

```java
public final class MapUnrollRule implements ExpressionRule
{
    @Override
    public boolean matches(Prologue prologue, PrevalServices services)
    {
        Object function = prologue.rewritten()._func();
        return MetamodelPaths.MAP_FUNCTIONS.anySatisfy(path -> services.runtime().isFunction(function, path))
                && prologue.rewrittenParameters().allSatisfy(p -> services.isInstanceValue(p, prologue.state().getInScopeVars()));
    }

    @Override
    public PrevalResult apply(Prologue prologue, PrevalServices services)
    {
        PrevalState state = prologue.state();
        services.trace(state, "Expanding map");
        ImmutableList<ValueSpecification> parameters = prologue.rewrittenParameters();
        ImmutableList<ValueSpecification> inputs = UnrollSupport.inputs(parameters.get(0), state, services);
        FunctionDefinition<?> lambda = UnrollSupport.lambda(parameters.get(1), services);
        ImmutableList<PrevalResult> results = inputs.collect(v -> UnrollSupport.applyBody(lambda, Lists.immutable.with(v), prologue.original(), state, services));
        MutableList<Object> values = Lists.mutable.empty();
        results.forEach(r ->
        {
            Object v = r.getValue();
            if (v instanceof InstanceValue)
            {
                values.addAllIterable(services.runtime().values((InstanceValue) v));
            }
            else
            {
                values.add(v);
            }
        });
        Object value = services.runtime().newInstanceValue(prologue.rewritten()._genericType(), services.runtime().exactly(values.size()), values);
        return new PrevalResult(value, PrevalResult.allCanPreval(results), services.openVars(results, state), true);
    }
}
```
`UnrollSupport` is a package-private helper in the `rules` package, shared by map and fold:
```java
final class UnrollSupport
{
    private UnrollSupport()
    {
    }

    static ImmutableList<ValueSpecification> inputs(ValueSpecification collection, PrevalState state, PrevalServices services)
    {
        return services.runtime().reactivate(collection, state.getInScopeVars()).collect(v -> v instanceof ValueSpecification
                ? (ValueSpecification) v
                : services.runtime().newInstanceValue(collection._genericType(), services.runtime().pureOne(), Lists.immutable.with(v)));
    }

    static FunctionDefinition<?> lambda(ValueSpecification parameter, PrevalServices services)
    {
        Object function = services.runtime().values((InstanceValue) parameter).getOnly();
        if (!(function instanceof FunctionDefinition) || Lists.mutable.withAll(((FunctionDefinition<?>) function)._expressionSequence()).size() != 1)
        {
            throw services.runtime().error("Assert failure: expected a single-expression function definition");
        }
        return (FunctionDefinition<?>) function;
    }

    static PrevalResult applyBody(FunctionDefinition<?> lambda, ImmutableList<ValueSpecification> arguments, FunctionExpression original, PrevalState state, PrevalServices services)
    {
        PrevalState scoped = services.addToScope(state, lambda, Lists.immutable.empty(), arguments, false);
        ValueSpecification body = Lists.immutable.<ValueSpecification>withAll(lambda._expressionSequence()).getOnly();
        ValueSpecification typed = services.runtime().withGenericType(body, (GenericType) services.genericTypes().resolveGenericType(original._genericType(), scoped).getValue());
        return services.preval(typed, scoped);
    }
}
```
`FoldUnrollRule`: the same match with `FOLD_FUNCTION`. `apply`:
```java
        PrevalResult accumulator = services.preval(parameters.get(2), state);
        for (ValueSpecification input : UnrollSupport.inputs(parameters.get(0), state, services))
        {
            accumulator = UnrollSupport.applyBody(lambda, Lists.immutable.with(input, (ValueSpecification) accumulator.getValue()), prologue.original(), state, services);
        }
        return accumulator.markModified();
```
`ConcatenateRule` ports the predicate at ~687–716, including the guard for zero-multiplicity variables:
```java
    @Override
    public boolean matches(Prologue prologue, PrevalServices services)
    {
        PrevalRuntime runtime = services.runtime();
        return runtime.isFunction(prologue.rewritten()._func(), MetamodelPaths.CONCATENATE_FUNCTION)
                && prologue.rewrittenParameters().allSatisfy(p ->
                {
                    Multiplicity m = p._multiplicity();
                    return runtime.isPureOne(m) || runtime.isPureZero(m)
                            || (runtime.isMultiplicityConcrete(m) && Multiplicities.hasUpperBound(runtime, m) && Multiplicities.hasLowerBound(runtime, m) && runtime.lowerBound(m).equals(runtime.upperBound(m)));
                })
                && services.scope().areAllInScope(
                        prologue.parameters().select(r -> r.getValue() instanceof VariableExpression && runtime.isPureZero(((VariableExpression) r.getValue())._multiplicity())).flatCollect(PrevalResult::getOpenVars).distinct(),
                        prologue.state().getInScopeVars());
    }

    @Override
    public PrevalResult apply(Prologue prologue, PrevalServices services)
    {
        PrevalRuntime runtime = services.runtime();
        ImmutableList<ValueSpecification> parameters = prologue.rewrittenParameters();
        Object value;
        if (runtime.isPureZero(parameters.get(0)._multiplicity()))
        {
            value = parameters.get(1);
        }
        else if (runtime.isPureZero(parameters.get(1)._multiplicity()))
        {
            value = parameters.get(0);
        }
        else
        {
            MutableList<Object> values = Lists.mutable.empty();
            parameters.forEach(p ->
            {
                if (p instanceof InstanceValue)
                {
                    values.addAllIterable(runtime.values((InstanceValue) p));
                }
                else
                {
                    values.add(p);
                }
            });
            value = runtime.newInstanceValue((GenericType) services.genericTypes().resolveGenericType(prologue.rewritten()._genericType(), prologue.state()).getValue(), runtime.exactly(values.size()), values);
        }
        return new PrevalResult(value, PrevalResult.allCanPreval(prologue.parameters()), prologue.openVars(), true);
    }
```
`Multiplicities`:
- `hasUpperBound`: `upperBound(m) != null && upperBound(m) != -1`.
- `hasLowerBound`: `lowerBound(m) != null && lowerBound(m) != 0` (`// parity:` Pure treats a lower bound of 0 as absent).

Adapters: compiled reads `_lowerBound()._value()`, `_upperBound()._value()` and `_multiplicityParameter()`. Interpreted uses the resolved getters.

- [ ] **Step 4: GREEN and baseline update.** Same procedure as Task 4 Step 4.

- [ ] **Step 5: Commit**

Stage the same paths as Task 4 Step 5. Message: `Port map, fold and concatenate unrolling to the Java preeval`.

---

### Task 7: Cast-of-empty, filter-constant, `toOne`/`toOneMany`, `genericType`

**Files:**
- Create: `rules/EmptyCastRule.java`, `rules/FilterFalseRule.java`, `rules/FilterTrueRule.java`, `rules/ToOneManyRule.java`, `rules/ToOneRule.java`, `rules/GenericTypeRule.java`
- Modify:
  - `Rules.java`: `NOT_PREVALLED = [MapUnrollRule, FoldUnrollRule, ConcatenateRule, EmptyCastRule, FilterFalseRule, FilterTrueRule, ToOneManyRule, ToOneRule, GenericTypeRule]`.
  - `TestRules.java`.
  - `PrevalHooks.java`: add `isGetAllFunction`.
  - `PrevalRuntime.java`: add `genericTypeOf(String typePath)`.
  - `MetamodelPaths.java`: add `FILTER_FUNCTION`, `TO_ONE_FUNCTION`, `TO_ONE_MANY_FUNCTION`, `GENERIC_TYPE_FUNCTION` and `NIL = "meta::pure::metamodel::type::Nil"`. Take the exact signatures from preeval.pure.
  - Both adapters and both hooks classes.
  - `KNOWN_DIVERGENT` and the baseline doc.
- Test: `AbstractTestPrevalNative` (both modes)

**Interfaces:**
- Produces:
  - `PrevalHooks.boolean isGetAllFunction(Object function)`.
  - `PrevalRuntime.GenericType genericTypeOf(String typePath)`: a new generic type whose `rawType` is the element at `typePath`.
  - `RuleSupport.isFilterReturningConstant(Prologue, PrevalServices, boolean)` and `RuleSupport.isGetAll(Object, PrevalServices)` port Pure's ~881–898.

- [ ] **Step 1: Failing tests**

`TestRules` has the new order. Add these harness tests, which run in both modes:
- `{|[1, 2]->filter(x | false)}` gives an empty value of generic type `Nil`, multiplicity `PureZero`.
- `{|[1, 2]->filter(x | true)}` gives `[1, 2]` (the first parameter).
- `{p:Integer[1] | $p->toOne()}` gives body `$p`, modified.
- `{|[]->cast(@String)}` gives an empty InstanceValue, modified.
- `{|1->genericType()}` gives an InstanceValue holding the `Integer` generic type.

Some of these may already fold through re-activation today. Record which ones, and keep only the ones that are RED before the change, plus at least one per rule that is **not** reachable by re-activation. For example: filter-false with a non-concrete input such as `{p:Integer[*] | $p->filter(x | false)}`, and `toOne` on a to-one variable.

- [ ] **Step 2: RED.** Run FAMILY with `-Dmdep.analyze.skip=true -fae`.

- [ ] **Step 3: Implement**

These port the handlers at ~744–840, in this order: empty cast, filter false, filter true, toOneMany, toOne, genericType.
- **`EmptyCastRule.matches`:**
  - `func` is `CAST_FUNCTION`;
  - the first parameter result's value is an InstanceValue whose `values(...)` is empty.

  **`apply`:**
  - build `newInstanceValue(resolveGenericType(rewritten._genericType(), state).value, rewritten._multiplicity(), [])`;
  - return `new PrevalResult(v, true, [], true)`.
- **`FilterFalseRule.matches`:** `RuleSupport.isFilterReturningConstant(p, s, false) && !RuleSupport.isGetAll(p.rewrittenParameters().getFirst(), s)`.

  **`apply`:** `newInstanceValue(runtime.genericTypeOf(NIL), runtime.pureZero(), [])` with `(true, [], true)`.
- **`FilterTrueRule.matches`:** `isFilterReturningConstant(p, s, true)`.

  **`apply`:** `new PrevalResult(p.rewrittenParameters().getFirst(), true, [], true)`.
- **`ToOneManyRule.matches`:**
  - `func` is `TO_ONE_MANY_FUNCTION`;
  - there is exactly one rewritten parameter, whose multiplicity `m` satisfies `isMultiplicityConcrete(m) && Multiplicities.hasLowerBound(m) && lowerBound(m) > 0`.

  **`apply`:** `p.parameters().getOnly().markModified()`.
- **`ToOneRule.matches`:**
  - `func` is `TO_ONE_FUNCTION`;
  - the single rewritten parameter is a `VariableExpression` whose multiplicity is concrete and `isPureOne`.

  **`apply`:** `p.parameters().getOnly().markModified()`.
- **`GenericTypeRule.matches`:** `func` is `GENERIC_TYPE_FUNCTION`, and the first rewritten parameter's `_genericType()._rawType() != null`.

  **`apply`:** `newInstanceValue(rewritten._genericType(), rewritten._multiplicity(), [firstParameter._genericType()])` with `(true, [], true)`.

`RuleSupport`:
```java
    static boolean isFilterReturningConstant(Prologue prologue, PrevalServices services, boolean expected)
    {
        if (!services.runtime().isFunction(prologue.rewritten()._func(), MetamodelPaths.FILTER_FUNCTION))
        {
            return false;
        }
        ValueSpecification last = prologue.rewrittenParameters().getLast();
        LambdaFunction<?> lambda = (LambdaFunction<?>) services.runtime().values((InstanceValue) last).getOnly();
        ValueSpecification lastExpression = Lists.immutable.<ValueSpecification>withAll(lambda._expressionSequence()).getLast();
        return lastExpression instanceof InstanceValue && singleBooleanEquals(services, lastExpression, expected);
    }

    static boolean isGetAll(Object value, PrevalServices services)
    {
        if (!(value instanceof FunctionExpression))
        {
            return false;
        }
        FunctionExpression expression = (FunctionExpression) value;
        if (services.hooks().isGetAllFunction(expression._func()))
        {
            return true;
        }
        ImmutableList<ValueSpecification> parameters = Lists.immutable.withAll(expression._parametersValues());
        return parameters.notEmpty() && isGetAll(parameters.getFirst(), services);
    }
```
Pure's `isFilterFunctionReturningConstant` casts the last parameter to an InstanceValue of LambdaFunction. When that cast would fail in Pure it fails here too, which is `// parity:`.

Hooks: `isGetAllFunction` follows the `shouldInline` pattern from Task 5. Runtime: `genericTypeOf` is compiled `new Root_meta_pure_metamodel_type_generics_GenericType_Impl("Anonymous_NoCounter")._rawType((Type) element)`, and interpreted an ephemeral `M3Paths.GenericType` with `rawType` set.

- [ ] **Step 4: GREEN and baseline update.** Same procedure as Task 4 Step 4.

- [ ] **Step 5: Commit**

Stage the same paths as Task 4 Step 5. Message: `Port cast, filter, toOne and genericType simplifications to the Java preeval`.

---

### Task 8: TDS `columns` and eval-on-Column

**Files:**
- Create: `rules/TdsColumnsRule.java`, `rules/EvalOnColumnRule.java`
- Modify:
  - `Rules.java`: `NOT_PREVALLED = [TdsColumnsRule, EvalOnColumnRule, MapUnrollRule, FoldUnrollRule, ConcatenateRule, EmptyCastRule, FilterFalseRule, FilterTrueRule, ToOneManyRule, ToOneRule, GenericTypeRule]`. This is Pure's full handler order.
  - `TestRules.java`.
  - `PrevalHooks.java`: add `resolveTdsSchema`.
  - `PrevalRuntime.java`: add `isPropertyOf`, `functionReturnType`, `functionReturnMultiplicity` and `withGenericTypeAndMultiplicity`.
  - `MetamodelPaths.java`: add `TABULAR_DATA_SET = "meta::pure::tds::TabularDataSet"`, `RELATION_TYPE = "meta::pure::metamodel::relation::RelationType"` and `COLUMN = "meta::pure::metamodel::relation::Column"`.
  - Both adapters and both hooks classes.
  - `KNOWN_DIVERGENT` and the baseline doc.

**Interfaces:**
- Produces:
  - `PrevalHooks.ImmutableList<Object> resolveTdsSchema(ValueSpecification value, ImmutableMap<String, ImmutableList<Object>> rollingInScopeVars)`.
    - Compiled: build a `PureMap` of `String` → `CoreGen.bridge.buildList()._valuesAddAll(values)`, then call `Pure.evaluate(es, fn, CoreGen.bridge, value, map)` and flatten the result with `CompiledSupport.toPureCollection`.
    - Interpreted: build a `MapCoreInstance` using the `OpenVariableValues.java` pattern, pass both arguments wrapped non-executable through `executeFunction`, and return the resolved `values`.
  - `PrevalRuntime`:
    - `boolean isPropertyOf(Object function, String propertyName, String ownerPath)`: an `AbstractProperty` with that name whose owner's user path is `ownerPath`;
    - `GenericType functionReturnType(Object function)` and `Multiplicity functionReturnMultiplicity(Object function)`: from the function type;
    - `<T extends ValueSpecification> T withGenericTypeAndMultiplicity(T, GenericType, Multiplicity)`.

- [ ] **Step 1: Failing tests**

`TestRules` has the full order. TDS and Relation types are engine types that the platform-only harness cannot build. So these rules are covered by the compiled-core runner tests only (testPrerouting33, testPrerouting34, tesColumnEvalOnRelation, tesColumnEvalOnRelationWithCast), which are RED at this point. Record them as RED evidence from RUNNER-ALL.

- [ ] **Step 2: RED.** Run FAMILY with `-Dmdep.analyze.skip=true -fae` (`TestRules` fails), then CORE-BUILD and RUNNER-ALL (the four tests fail).

- [ ] **Step 3: Implement**

These port the handlers at ~566–600.

**`TdsColumnsRule.matches`:**
- `runtime.isPropertyOf(func, "columns", TABULAR_DATA_SET)`;
- `scope().areAllInScope(parameters' openVars, state.getRollingInScopeVars())`. Note that this uses the **rolling** vars.

**`TdsColumnsRule.apply`:**
- `values = hooks.resolveTdsSchema(rewrittenParameters().getOnly(), state.getRollingInScopeVars())`;
- `newInstanceValue(rewritten._genericType(), exactly(values.size()), values)`, with `(true, [], true)`.

**`EvalOnColumnRule.matches`:**
- `EvalExpansionRule.isEval(p, s)`;
- `first = EvalExpansionRule.firstParameter(p, s)`;
- `!(first instanceof LambdaFunction) && runtime.isInstanceOf(first, COLUMN)`.

**`EvalOnColumnRule.apply`:**
- `value = runtime.withFuncAndParameters(rewritten, first, rewrittenParameters().drop(1))`;
- if `runtime.isInstanceOf(value._genericType()._rawType(), RELATION_TYPE)`, then `value = runtime.withGenericTypeAndMultiplicity(value, runtime.functionReturnType(first), runtime.functionReturnMultiplicity(first))`;
- return `(value, true, [], true)`.

- [ ] **Step 4: GREEN and baseline update.** Same procedure as Task 4 Step 4. After this task, `KNOWN_DIVERGENT` should be empty. Give a reason in the baseline doc for every name that remains.

- [ ] **Step 5: Commit**

Stage the same paths as Task 4 Step 5. Message: `Port TDS columns and eval-on-Column rules to the Java preeval`.

---

### Task 9: Relational coverage, exit verification, spec

**Files:**
- Create: `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/test/java/org/finos/legend/pure/code/core/relational/Test_Pure_Relational_Preeval_Java.java`
- Modify: `docs/superpowers/specs/2026-09-27-preeval-java-native-design.md`, the baseline doc

- [ ] **Step 1: Relational runner**

Copy `Test_Pure_Preeval_Java`'s structure: `TestSetup` property handling, the JAVA and SHADOW suites, and the name guard. Collect only the relational preeval tests, defined in `REL/relational/router/tests/testPreeval.pure` in package `meta::pure::router::preeval::tests`. Select by name: `testPrerouting42`. The two `ToFix` tests are not collected. Guard that `testPrerouting42` matches exactly one collected test.

- [ ] **Step 2: Build and run**

```bash
. /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -DskipTests -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -pl legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure \
&& mvn -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -DargLine= -pl legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure org.apache.maven.plugins:maven-surefire-plugin:2.22.2:test -Dtest=Test_Pure_Relational_Preeval_Java
```
Expected: 2/2 (JAVA and SHADOW). If the module's upstream engine dependencies are missing from `~/.m2` at this revision, add `-am` to the install and record it. A failure caused by a missing rule is a P2 bug: fix it in the rule, not in the test.

- [ ] **Step 3: Full regression**

Run RUNNER plus `-Dtest='Test_Pure_Core,Test_Pure_Preeval,Test_Pure_Preeval_Java'`, and Checkstyle on the four preeval modules, compiled-core and relational-core-pure.

Expected:
- 0 failures and 0 violations;
- `KNOWN_DIVERGENT` is empty, or every remaining entry has a documented reason.

- [ ] **Step 4: Spec update**

In §5:
- mark P2 done;
- list any remaining `KNOWN_DIVERGENT` entries with their reasons;
- convert "P2 requirements carried from P1 review" into a status list;
- replace the "P1 structure vs §3.4/§3.5" deviations with a line saying that P2 Task 1 extracted the rule structure.

Keep it concise.

- [ ] **Step 5: Commit**

```bash
git add legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/test/java/org/finos/legend/pure/code/core/relational/Test_Pure_Relational_Preeval_Java.java docs/superpowers/specs/2026-09-27-preeval-java-native-design.md docs/superpowers/plans/2026-09-28-preeval-p2-shadow-baseline.md
git status --short
git commit -m "Verify relational preeval under Java and record P2 completion"
```

## P2 exit criteria (spec §5)

- Every collected `tests.pure` test passes under `JAVA` and `SHADOW` in compiled mode. `KNOWN_DIVERGENT` is empty, or each remaining entry has a documented reason.
- Relational `testPrerouting42` passes under `JAVA` and `SHADOW`.
- The harness is green in both modes, with scenarios for `if`, `and`/`or`, inlining, `eval`, `map`, `fold`, `concatenate`, cast, filter, `toOne` and `genericType`.
- `TestRules` pins the full Pure handler order.
- `Test_Pure_Core` and `Test_Pure_Preeval` pass with the default `PURE`.
- The P2 requirements carried from the P1 review are closed or explicitly re-deferred in the spec.
