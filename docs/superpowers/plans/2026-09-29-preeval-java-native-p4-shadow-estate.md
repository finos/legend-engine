# Preeval Java Native — P4 Shadow Estate Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `SHADOW` a real parity gate by comparing every node of the result, then run the downstream suites under `SHADOW` and end with zero unexplained differences.

**Architecture:**
- `describePrevalResult` in `preeval.pure` gains a per-node section: generic type and multiplicity for every expression node, and robust printing for nodes that have no type.
- The compiled-core runners name each test after its implementation.
- The estate runs each downstream suite twice, once under `PURE` as a control and once under `SHADOW`. Any failure that appears only under `SHADOW` is a finding, recorded in a committed manifest and then fixed.

**Tech Stack:** Java 11, Pure, legend-pure 5.105.0 (compiled and interpreted), JUnit 3 suites run by the Surefire 2.22.2 JUnit 4 provider.

**Spec:** `docs/superpowers/specs/2026-09-27-preeval-java-native-design.md`: phase **P4** of §5, §4.3 (shadow estate), and the P2/P3 known gaps it carries.

**Previous plans:** the P0–P3 plans in `docs/superpowers/plans/`. All are complete on branch `preeval-native`.

## Global Constraints

- JDK 11. Prefix every Maven command with `. /home/aziem/bin/jdk11.sh &&` and pass `-Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true`.
  - Lifecycle builds add `-T 3` and always `clean`.
  - Build only the modules named. Use `-am` only as the stale-jar fallback in Task 5.
- Direct Surefire runs **must** pass `-DargLine=...`. Put forked-JVM system properties inside it, e.g. `-DargLine="-Xmx6g -Dlegend.engine.preeval.implementation=SHADOW"`.
- Checkstyle: run `mvn checkstyle:check -Dcheckstyle.config.location=$(pwd)/checkstyle.xml -pl <modules>` from the repo root.
- New files need the Apache header, first line `Copyright 2026 Goldman Sachs`.
- RED builds of Java changes use `-Dmdep.analyze.skip=true`. The final GREEN build must pass the analyzer.
- Code style:
  - No explanatory comments, except `// parity: <reason>` for a Pure quirk reproduced on purpose.
  - Java: 4-space indent, braces on their own lines. Pure: 2-space indent.
- Commits are authored solely by the user: **no** `Co-Authored-By` or `Claude-Session` trailers.
  - Messages are sentence-case imperatives.
  - Never `git add -A` / `git add .`. Never a bare `git stash`.
- **Never touch or add** the untracked `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/test/java/org/finos/legend/engine/pure/code/core/interpreted/` directory. It is the user's work in progress.
- The switch default stays `PURE`. Nothing in P4 changes production behaviour.
- compiled-core has only JUnit 4 (`junit:junit`) on its test classpath. Adding JUnit 5 would switch the Surefire provider for every suite in the module, so new Java tests there use JUnit 4.
- Maven commands in subagents run in the **foreground** with a 600000 ms timeout, never backgrounded or polled. A run that cannot fit in 10 minutes is split by `-Dtest=` class, or handed to the controller (Task 5).

## Decisions already made (user, 2026-09-29)

- **Source information:** keep the compiled deviation. Compiled copies keep the original node's source information, so errors point at user code. `SHADOW` keeps ignoring source information. Task 7 records this in the spec.
- **CI:** the estate runs locally and its results go in a committed manifest. No workflow change in P4. A CI `SHADOW` job belongs to P5.

## Rulings made while planning

- **The wrapper type argument is not compared.** PURE's `PrevalWrapper<T>` type argument depends on which part of `preeval.pure` built the result, not on the kind of value:
  - a `FunctionExpression` result comes back as `<FunctionExpression>` at lines ~524, ~575 and ~911, but as `<Any>` at ~646 and ~936;
  - `<InstanceValue>` and `<ValueSpecification>` also appear.

  Java cannot reproduce this from the value alone. The only cast of the wrapper in the repo is `preeval.pure:84`, to `PrevalWrapper<FunctionDefinition<Any>>`, and P3 already made `prevalJava` produce that. Task 7 records the type argument as outside the parity contract, which closes the P3 "wrapper typing" known gap. Cost if wrong: a future caller that casts to a narrower wrapper fails under interpreted `JAVA` only, which the interpreted runner would catch.
- **Two sources of estate findings.**
  - A failure in the `PURE` control is a pre-existing failure. It is listed, not fixed.
  - Only `SHADOW`-only failures are P4 findings.
- **Task 6 is written after Task 5.** Its sub-tasks depend on what the estate finds. After Task 5 commits the manifest, the controller amends this plan with one Task 6 sub-task per root cause, commits the amendment, then executes it. This is the same reason each phase gets its own plan.
- **Runner names.** The two compiled-core runners share one package-private helper, `PreevalImplementationTests`. The relational runners keep their `TestSetup` wrappers: the relational JAVA/SHADOW runner has one test, and Task 4's interpreted runner has one test under three implementations. Consolidation is P6 (final-review M-7).

## File Structure

```
compiled-core (CC = legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core)
  src/main/resources/core/pure/router/preeval/preeval.pure                  MODIFY (T1): per-node describe, robust type printing
  src/main/resources/core/pure/router/preeval/testImplementationSwitch.pure MODIFY (T1): 2 new tests
  src/test/java/org/finos/legend/engine/pure/code/core/PreevalImplementationTests.java     CREATE (T3)
  src/test/java/org/finos/legend/engine/pure/code/core/TestPreevalImplementationTests.java CREATE (T3)
  src/test/java/org/finos/legend/engine/pure/code/core/Test_Pure_Preeval_Java.java         MODIFY (T1 maybe, T3)
  src/test/java/org/finos/legend/engine/pure/code/core/Test_Interpreted_Preeval.java       MODIFY (T1 maybe, T3)
relational core-pure (RC = legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure)
  pom.xml                                                                    MODIFY (T4): interpreted test deps
  src/test/java/org/finos/legend/pure/code/core/relational/Test_Interpreted_Relational_Preeval.java  CREATE (T4)
preeval Java modules (FAM = legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval)
  …shared/compiled/interpreted sources                                       MODIFY (T2, T6) as findings require
docs/superpowers/plans/2026-09-29-preeval-p4-shadow-estate-manifest.md       CREATE (T1), EXTEND (T2, T4, T5, T6)
docs/superpowers/specs/2026-09-27-preeval-java-native-design.md              MODIFY (T7)
```

**Estate modules** (paths usable with `-pl`):

| Key | Module | Test classes |
|---|---|---|
| RC | `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure` | `Test_Pure_Relational`, `Test_Pure_Relational_MFT_Collection` (also covers service execution `meta::alloy::service::execution` and relational lineage) |
| H2 | `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-h2/legend-engine-xt-relationalStore-h2-PCT` | `Test_Relational_H2_PCT`, `Test_Relational_H2_Semistructured`, `Test_Relational_H2_NonRelationalToRelational` |
| DUCK | `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-duckdb/legend-engine-xt-relationalStore-duckdb-PCT` | `Test_Relational_DuckDB_PCT`, `Test_Relational_DuckDB_Semistructured`, `Test_Relational_DuckDB_NonRelationalToRelational` |
| SQL | `legend-engine-xts-sql/legend-engine-xt-sql-transformation/legend-engine-xt-sql-pure` | `Test_SQL_Pure` (includes `testTranspile.pure`, 214 tests, several calling `preval` directly) |
| LIN | `legend-engine-xts-analytics/legend-engine-xts-analytics-lineage/legend-engine-xt-analytics-lineage-pure` | `Test_Analytics_Lineage`, `Test_Analytics_Lineage__M2M_MFT`, `Test_Analytics_Lineage_Relational_MFT` |

There is no standalone service Pure suite: `legend-engine-language-pure-dsl-service-pure` has no `<<test.Test>>` functions. Its `preval` call (`core_service/service/extension.pure:28`) runs under RC's `meta::alloy::service::execution` collection. Other `preval` callers that are out of the spec's estate list, `openapi` (`pureToOpenApi.pure:172`) and `dataquality` (`dataprofile_test.pure:104`), are listed in the manifest as not covered.

**Standard commands:**
- **CORE-BUILD**:
  ```bash
  . /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -DskipTests -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core
  ```
- **FAMILY**:
  ```bash
  . /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-compiled-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-interpreted-functions-preeval
  ```
- **CC-TEST `<classes>`**:
  ```bash
  . /home/aziem/bin/jdk11.sh && mvn -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -DargLine= -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core org.apache.maven.plugins:maven-surefire-plugin:2.22.2:test -Dtest=<classes>
  ```
  - COMPILED = CC-TEST `'Test_Pure_Preeval,Test_Pure_Preeval_Java'`
  - INTERP = CC-TEST `Test_Interpreted_Preeval`
- **RC-BUILD**:
  ```bash
  . /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -DskipTests -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -pl legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure
  ```
- **RC-TEST `<classes>`**: CC-TEST with `-pl` pointing at RC.
- **Baseline counts at plan time (HEAD `59b8110cf0a`):**
  - `Test_Pure_Preeval` 111
  - `Test_Pure_Preeval_Java` 222
  - `Test_Interpreted_Preeval` 336
  - `Test_Pure_Relational_Preeval_Java` 2
  - `Test_Pure_Core` 1192

---

### Task 1: Per-node `SHADOW` comparator, robust type printing, and the comparator baseline

**Files:**
- Modify: `CC/src/main/resources/core/pure/router/preeval/preeval.pure`: `describePrevalResult`, plus three new private functions after it
- Modify: `CC/src/main/resources/core/pure/router/preeval/testImplementationSwitch.pure`: two new tests
- Create: `docs/superpowers/plans/2026-09-29-preeval-p4-shadow-estate-manifest.md`
- Maybe modify: `KNOWN_DIVERGENT` in `Test_Pure_Preeval_Java.java`, `KNOWN_INTERPRETED_FAILURES` in `Test_Interpreted_Preeval.java`

**Interfaces:**
- Produces:
  - `describePrevalResult` output, whose format is `…, type=<t>, value=<json>, nodes=<lines>`;
  - `meta::pure::router::preeval::describeTypeAndMultiplicity(vs:ValueSpecification[1]):String[1]`, which prints `<no genericType>` / `<no multiplicity>` when either is empty;
  - `meta::pure::router::preeval::describeNodes(value:Any[1], indent:String[1]):String[*]`.
- The mismatch message prefix `preeval SHADOW mismatch` is unchanged.

- [ ] **Step 1: Write the failing tests**

Append to `testImplementationSwitch.pure`:

```pure
function <<test.Test>> meta::pure::router::preeval::tests::implementationSwitch::testShadowRejectsDifferentNestedTypes():Boolean[1]
{
  let fe = {|[1]->toOne()}->evaluateAndDeactivate().expressionSequence->at(0)->cast(@SimpleFunctionExpression);
  let param = $fe.parametersValues->at(0)->cast(@InstanceValue);
  let widened = ^$fe(parametersValues = ^$param(genericType = ^GenericType(rawType = Number)));
  assertError(
    | assertSamePrevalResult(
        ^PrevalWrapper<Any>(value = $fe, canPreval = true, openVars = [], modified = false),
        ^PrevalWrapper<Any>(value = $widened, canPreval = true, openVars = [], modified = false),
        newMap([]->cast(@Pair<String, List<Any>>)),
        []),
    {message:String[1], source:SourceInformation[0..1] | assert($message->startsWith('preeval SHADOW mismatch'), |$message)}
  );
}

function <<test.Test>> meta::pure::router::preeval::tests::implementationSwitch::testShadowDescribesHandBuiltInstanceValues():Boolean[1]
{
  let iv = ^InstanceValue(genericType = ^GenericType(rawType = Integer), multiplicity = PureOne, values = 1);
  assertSamePrevalResult(
      ^PrevalWrapper<Any>(value = $iv, canPreval = true, openVars = [], modified = false),
      ^PrevalWrapper<Any>(value = $iv, canPreval = true, openVars = [], modified = false),
      newMap([]->cast(@Pair<String, List<Any>>)),
      []);
}
```

- [ ] **Step 2: RED**

Run CORE-BUILD, COMPILED, then INTERP. Expected:
- **`testShadowRejectsDifferentNestedTypes` fails in both modes** with "No error was thrown": the protocol JSON of both values is identical, and so are the top-level type and multiplicity.
- **`testShadowDescribesHandBuiltInstanceValues` passes compiled and fails interpreted** with a bare NPE from `printGenericType`. This is the latent P3 gap.
- **If RED differs, stop and report.** For example, if the nested test's JSON already differs, the test is not proving the comparator; pick a nested change the JSON does not carry. If the interpreted NPE comes from `transformValueSpecification` rather than `printGenericType`, the robust printing below does not cover it.

- [ ] **Step 3: Implement**

In `preeval.pure`, replace the `vs:ValueSpecification[1]` branch of `describePrevalResult`'s `type` match:
```pure
    vs:ValueSpecification[1]      | $vs->describeTypeAndMultiplicity(),
```
Then replace its final line with:
```pure
  'canPreval=' + $r.canPreval->toString() + ', modified=' + $r.modified->toString() + ', openVars=' + $r.openVars->sort()->joinStrings('[', ',', ']') + ', type=' + $type + ', value=' + $value + ', nodes=' + $r.value->describeNodes('')->joinStrings('\n', '\n', '');
```
Add after `describePrevalResult`:
```pure
function <<access.private>> meta::pure::router::preeval::describeTypeAndMultiplicity(vs:ValueSpecification[1]):String[1]
{
  if($vs.genericType->isEmpty(), |'<no genericType>', |$vs.genericType->toOne()->meta::pure::metamodel::serialization::grammar::printGenericType())
    + if($vs.multiplicity->isEmpty(), |'<no multiplicity>', |$vs.multiplicity->toOne()->meta::pure::metamodel::serialization::grammar::printMultiplicity());
}

function <<access.private>> meta::pure::router::preeval::describeNodes(value:Any[1], indent:String[1]):String[*]
{
  $value->match([
    fd:FunctionDefinition<Any>[1] | $fd.expressionSequence->evaluateAndDeactivate()->map(e | $e->describeNodes($indent)),
    fe:FunctionExpression[1]      | [$indent + 'fe ' + if($fe.functionName->isEmpty(), |'?', |$fe.functionName->toOne()) + ' : ' + $fe->describeTypeAndMultiplicity()]
                                      ->concatenate($fe.parametersValues->map(p | $p->describeNodes($indent + '  '))),
    iv:InstanceValue[1]           | [$indent + 'iv : ' + $iv->describeTypeAndMultiplicity()]
                                      ->concatenate($iv.values->map(v | $v->describeInstanceValueElement($indent + '  '))),
    ve:VariableExpression[1]      | $indent + 'var ' + $ve.name + ' : ' + $ve->describeTypeAndMultiplicity(),
    vs:ValueSpecification[1]      | $indent + $vs->class()->elementToPath() + ' : ' + $vs->describeTypeAndMultiplicity(),
    a:Any[1]                      | []
  ]);
}

function <<access.private>> meta::pure::router::preeval::describeInstanceValueElement(element:Any[1], indent:String[1]):String[*]
{
  $element->match([
    l:LambdaFunction<Any>[1]  | [$indent + 'lambda (' + $l->functionType().parameters->map(p | $p.name + ':' + $p->describeTypeAndMultiplicity())->joinStrings(', ') + ') : '
                                  + $l->functionReturnType()->meta::pure::metamodel::serialization::grammar::printGenericType() + $l->functionReturnMultiplicity()->meta::pure::metamodel::serialization::grammar::printMultiplicity()]
                                  ->concatenate($l->describeNodes($indent + '  ')),
    vs:ValueSpecification[1]  | $vs->describeNodes($indent),
    k:KeyExpression[1]        | [$indent + 'key ' + $k.key.values->map(v | $v->toString())->joinStrings(',')]
                                  ->concatenate($k.expression->describeNodes($indent + '  ')),
    a:Any[1]                  | []
  ]);
}
```
Pure's `->` binds tighter than `+`, which is why each node line is a bracketed one-element list before `->concatenate`. Adapt only what the compiler forces, and report each adaptation, for example a `->toOne()` or a `->cast(...)`, or a different `KeyExpression` property name. Named functions inside an `InstanceValue` are deliberately not descended into; they fall into the `a:Any[1]` branch.

- [ ] **Step 4: GREEN for the new tests; take the comparator baseline**

Run CORE-BUILD, COMPILED and INTERP, then RC-BUILD and RC-TEST `Test_Pure_Relational_Preeval_Java`.

Expected:
- Both new tests pass in both modes. Each collection gains 2 tests: `Test_Pure_Preeval` 113, `Test_Pure_Preeval_Java` 226, `Test_Interpreted_Preeval` 342 (114 × 3; the interpreted collection has 112 today because it also runs `testPrerouting20a`).
- **Any other failure under `JAVA`/`SHADOW` is a comparator finding:** Java's nested types differ from Pure's. Under `PURE` there must be none.

For each finding:
1. Add its name to `KNOWN_DIVERGENT`, or to `KNOWN_INTERPRETED_FAILURES` if it is interpreted-only.
2. Record it in the manifest (Step 5) with the first differing `nodes=` line from both sides of the mismatch message.
3. Re-run until green.

Do **not** fix Java in this task.

- [ ] **Step 5: Create the manifest**

`docs/superpowers/plans/2026-09-29-preeval-p4-shadow-estate-manifest.md`:

```markdown
# Preeval P4 — shadow estate manifest

Plan: `docs/superpowers/plans/2026-09-29-preeval-java-native-p4-shadow-estate.md`.

Categories: JAVA-BUG (Java differs from Pure; fix Java), PURE-QUIRK (Pure behaviour Java must reproduce; fix Java with `// parity:`), COMPARATOR (describe/comparator false positive or crash; fix describe), NOT-SHADOW (fails identically under the PURE control; listed only).

## Comparator baseline (Task 1)

| Test | Runner | Mode | First differing node (PURE / JAVA) | Category | Status |
|---|---|---|---|---|---|
```
Add one row per Step 4 finding, or the single line `None: the per-node comparator found no new differences.`

- [ ] **Step 6: Checkstyle and commit**

Run Checkstyle on CC. Commit `preeval.pure`, `testImplementationSwitch.pure`, the manifest, and either runner if it changed.

Message: `Compare every node's type and multiplicity in preeval SHADOW`.

---

### Task 2: Close the comparator baseline

**Skip this task, with a ledger note, if Task 1's manifest says `None`.**

**Files:** preeval Java modules under FAM: the shared core, `CompiledPrevalRuntime`, `InterpretedPrevalRuntime`, as the findings require. Also the two runners' known lists and the manifest.

- [ ] **Step 1: Group the findings by root cause**

For each Task 1 row, run the test alone with `-Dlegend.engine.preeval.test.includeKnownDivergent=true` inside `-DargLine`, and read the full mismatch.

Group the rows by the Java code path that builds the differing node. Expect a few causes, such as a rule copying a `FunctionExpression` without re-deriving `genericType`, or `GenericTypes` resolution.

- [ ] **Step 2: Fix each group in turn**

For each group:
1. Remove its names from the known list, which is the RED state.
2. Fix Java so the node's `genericType` and multiplicity match Pure's. If Pure's value is a quirk, reproduce it with a `// parity: <reason>` comment.
3. Run FAMILY, CORE-BUILD, then COMPILED and INTERP. That group is GREEN and nothing else regresses.
4. Update the manifest row status to `fixed in <short sha>`.
5. Commit per group: `Match Pure's nested <node kind> types in Java preeval`.

- [ ] **Step 3: GREEN**

Both known lists are empty again; COMPILED 113 + 226, INTERP 342, RC-TEST `Test_Pure_Relational_Preeval_Java` 2, CC-TEST `Test_Pure_Core` 1192; Checkstyle clean on every touched module.

---

### Task 3: Name each runner test after its implementation

**Files:**
- Create: `CC/src/test/java/org/finos/legend/engine/pure/code/core/PreevalImplementationTests.java`
- Create: `CC/src/test/java/org/finos/legend/engine/pure/code/core/TestPreevalImplementationTests.java`
- Modify: `Test_Pure_Preeval_Java.java` and `Test_Interpreted_Preeval.java`. Replace each file's private `withImplementation` and `PROPERTY` with the helper.

**Interfaces:**
- Produces `static TestSuite PreevalImplementationTests.withImplementation(String implementation, TestSuite source)`:
  - It returns a structurally identical suite in which every `TestCase` leaf `x` becomes a `TestCase` named `x.getName() + "[" + implementation + "]"`.
  - Running that leaf sets `legend.engine.preeval.implementation` to `implementation`, calls the delegate's `runBare()`, and restores the previous value, or clears it, in `finally`.
  - `PureTestBuilder.PureTestCase` overrides only `runTest()`, so `runBare()` runs it with its own set-up and tear-down.

- [ ] **Step 1: Write the failing test** (JUnit 4, per Global Constraints)

```java
package org.finos.legend.engine.pure.code.core;

import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

public class TestPreevalImplementationTests
{
    private static final String PROPERTY = "legend.engine.preeval.implementation";

    @After
    public void clearProperty()
    {
        System.clearProperty(PROPERTY);
    }

    @Test
    public void testLeavesAreRenamedAfterTheImplementation()
    {
        TestSuite source = new TestSuite();
        source.addTest(new Recording("testSomething"));
        TestSuite named = PreevalImplementationTests.withImplementation("JAVA", source);
        Assert.assertEquals("testSomething[JAVA]", ((TestCase) named.testAt(0)).getName());
    }

    @Test
    public void testImplementationIsSetDuringTheRunAndRestoredAfter()
    {
        System.setProperty(PROPERTY, "PURE");
        Recording recording = new Recording("testSomething");
        TestSuite source = new TestSuite();
        source.addTest(recording);
        TestResult result = new TestResult();
        PreevalImplementationTests.withImplementation("SHADOW", source).run(result);
        Assert.assertTrue(result.wasSuccessful());
        Assert.assertEquals("SHADOW", recording.seen);
        Assert.assertEquals("PURE", System.getProperty(PROPERTY));
    }

    @Test
    public void testImplementationIsClearedAfterAFailingRunWhenPreviouslyUnset()
    {
        TestSuite source = new TestSuite();
        source.addTest(new Failing("testFails"));
        TestResult result = new TestResult();
        PreevalImplementationTests.withImplementation("JAVA", source).run(result);
        Assert.assertEquals(1, result.failureCount());
        Assert.assertNull(System.getProperty(PROPERTY));
    }

    private static final class Recording extends TestCase
    {
        private String seen;

        private Recording(String name)
        {
            super(name);
        }

        @Override
        protected void runTest()
        {
            this.seen = System.getProperty(PROPERTY);
        }
    }

    private static final class Failing extends TestCase
    {
        private Failing(String name)
        {
            super(name);
        }

        @Override
        protected void runTest()
        {
            fail("expected");
        }
    }
}
```

- [ ] **Step 2: RED**

Run CORE-BUILD with `-Dmdep.analyze.skip=true`. Expected: `COMPILATION ERROR`, because `PreevalImplementationTests` does not exist yet.

- [ ] **Step 3: Implement**

```java
package org.finos.legend.engine.pure.code.core;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Enumeration;

final class PreevalImplementationTests
{
    private static final String PROPERTY = "legend.engine.preeval.implementation";

    private PreevalImplementationTests()
    {
    }

    static TestSuite withImplementation(String implementation, TestSuite source)
    {
        TestSuite result = new TestSuite(source.getName() == null ? implementation : source.getName() + "[" + implementation + "]");
        for (Enumeration<Test> tests = source.tests(); tests.hasMoreElements(); )
        {
            Test test = tests.nextElement();
            if (test instanceof TestSuite)
            {
                result.addTest(withImplementation(implementation, (TestSuite) test));
            }
            else
            {
                result.addTest(new ImplementationTestCase(implementation, (TestCase) test));
            }
        }
        return result;
    }

    private static final class ImplementationTestCase extends TestCase
    {
        private final String implementation;
        private final TestCase delegate;

        private ImplementationTestCase(String implementation, TestCase delegate)
        {
            super(delegate.getName() + "[" + implementation + "]");
            this.implementation = implementation;
            this.delegate = delegate;
        }

        @Override
        public void runBare() throws Throwable
        {
            String previous = System.getProperty(PROPERTY);
            System.setProperty(PROPERTY, this.implementation);
            try
            {
                this.delegate.runBare();
            }
            finally
            {
                if (previous == null)
                {
                    System.clearProperty(PROPERTY);
                }
                else
                {
                    System.setProperty(PROPERTY, previous);
                }
            }
        }
    }
}
```

Then:
- **`Test_Pure_Preeval_Java.suite()`:** add `PreevalImplementationTests.withImplementation("JAVA", targets(executionSupport))` and the `"SHADOW"` equivalent.
- **`Test_Interpreted_Preeval.suite()`:** add the three `withImplementation(..., selected)` calls. The same `selected` suite is wrapped three times; the wrappers are distinct objects around shared delegates.
- **Clean up both files:** delete their private `withImplementation`, the `PROPERTY` constants, and the now-unused imports (`TestSetup`, `Test`).

- [ ] **Step 4: GREEN**

Run CORE-BUILD (analyzer on), then:
- CC-TEST `TestPreevalImplementationTests`: 3/3.
- COMPILED: the counts are unchanged from Task 2's end state.
- INTERP: the counts are unchanged.
- In `target/surefire-reports/TEST-org.finos.legend.engine.pure.code.core.Test_Interpreted_Preeval.xml`, count the `<testcase` entries whose `name` ends in `[PURE]`, in `[JAVA]` and in `[SHADOW]`. The three counts must be equal, and no two entries may share a `name`. Check it with:
  ```bash
  grep -o 'testcase name="[^"]*"' <file> | sort | uniq -d | wc -l
  ```
  which must print `0`.
- Checkstyle on CC.

- [ ] **Step 5: Commit**

Message: `Name each preeval runner test after its implementation`.

---

### Task 4: Interpreted relational `testPrerouting42`

**Files:**
- Modify: `RC/pom.xml`: add test-scope dependencies.
- Create: `RC/src/test/java/org/finos/legend/pure/code/core/relational/Test_Interpreted_Relational_Preeval.java`

**Interfaces:**
- Consumes the interpreted natives from P3.
- Produces a runner that builds `PureTestBuilderInterpreted.buildSuite("meta::pure::router::preeval::tests")` once, filters it to exactly one test named `testPrerouting42` (and throws `IllegalStateException` otherwise), and runs it under `PURE`, `JAVA` and `SHADOW`.

- [ ] **Step 1: Dependencies**

Add at test scope, the same set P3 added to compiled-core:
- engine: `legend-engine-pure-runtime-java-extension-interpreted-functions-standard`, `-unclassified`, `-json`, `-relation`;
- pure: `legend-pure-runtime-java-extension-interpreted-store-relational`, `-interpreted-dsl-tds`, and `-interpreted-dsl-path` with `<version>${legend.pure.version}</version>`.

Also add `org.finos.legend.pure:legend-pure-runtime-java-engine-interpreted` at test scope. It is `provided` in compiled-core, so it is not transitive. Check RC's pom first and skip any dependency it already declares.

- [ ] **Step 2: Runner**

Model it on `CC/…/Test_Interpreted_Preeval.java`: build once, walk the suite recursively, and keep the one `TestCase` whose `getName()` equals `testPrerouting42`. Wrap it in three `TestSetup`s, each of which saves, sets and restores `legend.engine.preeval.implementation`. Copy the `withImplementation` method from `Test_Pure_Relational_Preeval_Java.java` in the same package verbatim, so both relational runners use the same idiom.

- [ ] **Step 3: Run (spike gate)**

Run RC-BUILD with `-Dmdep.analyze.skip=true`, then RC-TEST `Test_Interpreted_Relational_Preeval`.

The interpreted runtime compiles every Pure repository on RC's classpath. That can be slow, and it can fail for reasons unrelated to preeval, such as an unimplemented interpreted native in a relational repository.

- **If all 3 pass:** run RC-BUILD again with the analyzer on and fix what it flags. Then run Checkstyle on RC, and commit `RC/pom.xml` and the runner with the message `Run relational testPrerouting42 in interpreted mode`.
- **If `PURE` fails, or the runtime cannot be built:** the gap is a relational interpreted-mode gap, not a preeval one.
  1. Revert both files.
  2. Record the first error, with its stack frame and the missing native or repository, in the manifest under a new `## Interpreted relational (Task 4)` heading, categorised NOT-SHADOW.
  3. Commit the manifest only, with the message `Record why relational preeval cannot run interpreted yet`.
- **If `PURE` passes but `JAVA` or `SHADOW` fails:** that is a finding. Keep the runner, and exclude the failing implementation(s) via a `KNOWN_INTERPRETED_FAILURES`-style set holding the implementation names.
  1. Record it in the manifest as JAVA-BUG or PURE-QUIRK.
  2. Commit with the message `Run relational testPrerouting42 in interpreted mode`.
  3. Task 6 fixes it.

---

### Task 5: Estate baseline — run the downstream suites under `PURE` and under `SHADOW`

**Executed by the controller, not a subagent.** Several suites exceed the 10-minute subagent limit. The controller runs each Maven command in the background and waits for the completion notification, never polling. A triage subagent then reads the reports.

**Files:** the manifest only.

- [ ] **Step 1: Build**

```bash
. /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -DskipTests -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core,legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure,legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-h2/legend-engine-xt-relationalStore-h2-PCT,legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-duckdb/legend-engine-xt-relationalStore-duckdb-PCT,legend-engine-xts-sql/legend-engine-xt-sql-transformation/legend-engine-xt-sql-pure,legend-engine-xts-analytics/legend-engine-xts-analytics-lineage/legend-engine-xt-analytics-lineage-pure
```
Run FAMILY first if Task 2 or Task 6 has changed Java since the last FAMILY build.

- [ ] **Step 2: Run each suite twice**

For each estate key K, with test classes C from the Estate modules table and `S` the scratchpad directory:

```bash
# control
. /home/aziem/bin/jdk11.sh && mvn -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -pl <K module> org.apache.maven.plugins:maven-surefire-plugin:2.22.2:test -Dtest='<C>' -DargLine="-Xmx6g" -Dsurefire.reports.directory=$S/estate/K/pure > $S/estate/K-pure.log 2>&1
# shadow
. /home/aziem/bin/jdk11.sh && mvn -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -pl <K module> org.apache.maven.plugins:maven-surefire-plugin:2.22.2:test -Dtest='<C>' -DargLine="-Xmx6g -Dlegend.engine.preeval.implementation=SHADOW" -Dsurefire.reports.directory=$S/estate/K/shadow > $S/estate/K-shadow.log 2>&1
```
- Run the suites one at a time, because they share the machine.
- Surefire exits non-zero when tests fail; that is expected for PCT suites with manifest exclusions. The reports are what matter.

**Stale-jar fallback.** The control run might fail en masse with `NoSuchMethodError`, `ClassNotFoundException` or a Pure metadata error in code P4 never touched. If so, rebuild that one module with `-am` added to Step 1's command restricted to it, note it in the ledger, and rerun both modes.

- [ ] **Step 3: Diff**

Save this script as `$S/estate/diff.py`. It is scratch and is not committed.

```python
import glob
import sys
import xml.etree.ElementTree as ET

def results(directory):
    ran, failing = set(), {}
    for path in glob.glob(directory + '/TEST-*.xml'):
        for tc in ET.parse(path).getroot().iter('testcase'):
            key = (tc.get('classname') or '') + '#' + (tc.get('name') or '')
            ran.add(key)
            bad = [c for c in tc if c.tag in ('failure', 'error')]
            if bad:
                failing[key] = ((bad[0].get('message') or '') + ' ' + (bad[0].text or '')).replace('\n', ' ')[:400]
    return ran, failing

control_ran, control = results(sys.argv[1])
shadow_ran, shadow = results(sys.argv[2])
print('ran: control', len(control_ran), 'shadow', len(shadow_ran), '| failing: control', len(control), 'shadow', len(shadow))
for key in sorted(control_ran ^ shadow_ran):
    print('RAN-IN-ONE-ONLY', key)
for key in sorted(shadow.keys() - control.keys()):
    print('SHADOW-ONLY', key, '|', shadow[key])
for key in sorted(control.keys() - shadow.keys()):
    print('CONTROL-ONLY', key, '|', control[key])
```
Run `python3 $S/estate/diff.py $S/estate/K/pure $S/estate/K/shadow` for each K.

- [ ] **Step 4: Triage (subagent)**

Dispatch one triage subagent with the diff outputs and the report directories. For every `SHADOW-ONLY` line it:
1. reads the failure;
2. assigns a category: JAVA-BUG (including a Java exception from `prevalNative`), PURE-QUIRK, or COMPARATOR (`describePrevalResult` crashed, or the difference is an artefact of the description, e.g. an unsupported value in `transformValueSpecification`);
3. groups the lines by root cause, identified from the first differing `nodes=` / `value=` segment or the Java stack frame;
4. names the smallest failing test per group.

`CONTROL-ONLY` and `RAN-IN-ONE-ONLY` lines must be explained. Flaky tests are a valid explanation, confirmed by rerunning that test alone in both modes.

- [ ] **Step 5: Manifest and commit**

Extend the manifest:

```markdown
## Estate runs (Task 5, <date>, HEAD <sha>)

| Suite | Module | Classes | Control ran / failing | SHADOW ran / failing | SHADOW-only |
|---|---|---|---|---|---|

## Estate findings

| # | Root cause | Category | Smallest failing test | Other affected tests (count) | Status |
|---|---|---|---|---|---|

## Pre-existing failures (NOT-SHADOW)

| Suite | Test | Failure (first line) |
|---|---|---|

## Not covered

- `legend-engine-xts-openapi` (`pureToOpenApi.pure:172`) and `legend-engine-xts-dataquality` (`dataprofile_test.pure:104`) call `preval` but are outside the spec's estate list.
```

Commit the manifest with the message `Record the preeval shadow estate baseline`.

---

### Task 6: Fix the estate findings

This task is written after Task 5. Once the manifest is committed, the controller amends this plan with one sub-task, **6.1, 6.2, …**, per root cause in `## Estate findings`, commits the amendment with the message `Plan the P4 estate fixes`, and executes the sub-tasks in order.

Each sub-task follows this shape:

1. **Reproduce locally where possible.** Add a `<<test.Test>>` to the `meta::pure::router::preeval::tests` namespace (compiled-core `tests.pure`) that reproduces the finding without the estate module, so it fails under `SHADOW` via COMPILED.
   - If the finding needs estate-only types, such as relational or SQL, put the test in that module's preeval test file. For RC that is `core_relational/relational/router/tests/testPreeval.pure`, and it is then picked up by `Test_Pure_Relational_Preeval_Java` if the name is added to its target selection.
   - If no smaller reproduction exists, the estate test itself is the reproduction.
2. **RED.** The new test fails under `JAVA`/`SHADOW` and passes under `PURE`.
3. **Fix.**
   - JAVA-BUG: fix Java to match Pure.
   - PURE-QUIRK: fix Java to reproduce the quirk, with `// parity: <reason>`.
   - COMPARATOR: fix `describePrevalResult`, and add a `testImplementationSwitch.pure` test that pins the fixed description.
4. **GREEN.** Run FAMILY, CORE-BUILD, COMPILED and INTERP, then RC-BUILD and RC-TEST `Test_Pure_Relational_Preeval_Java`. The controller reruns the affected estate suite under `SHADOW` and diffs it against the Task 5 control. That finding's tests leave `SHADOW-ONLY`, and nothing new appears.
5. **Update the manifest** row's status to `fixed in <sha>`, and **commit** with a message naming the behaviour, e.g. `Match Pure when preeval unrolls a map over a relational property`.

**Exit for Task 6:** every row in `## Estate findings` is `fixed`, and a final `SHADOW` rerun of all five suites shows no `SHADOW-ONLY` lines against the control. The controller runs it and records the result in the manifest's runs table as a second dated row per suite.

#### Task 6 amendment (2026-09-29, after manifest `f4c04d260e9`)

The manifest's four findings share one root cause, and all four are COMPARATOR. `describePrevalResult` renders the value through the vX_X_X protocol (`transformFunctionBody` / `transformValueSpecification`). That transform cannot translate some values preeval legitimately evaluates into an `InstanceValue`:
- `_Window` (from `over`);
- `SortInfo` (from `ascending` / `descending`);
- a relation `#TDS` literal;
- a Runtime whose connection has no serializer among the caller's extensions.

In every case it throws on the **Pure** side, before the Java comparison is reached. So the 526 affected tests say nothing about Java parity yet, and they may hide real mismatches. Task 6.2 re-runs the estate once the comparator is fixed.

**Ruling:** the comparison stops using the protocol for values. `describeNodes` becomes a complete, protocol-free structural description. It records:
- function identity;
- property identity;
- literal values with their types;
- variable and parameter names;
- lambda signatures;
- key expressions;
- other class instances, by classifier path and their sorted property values, recursively.

`value=` is removed for `FunctionDefinition` and `ValueSpecification` results, because `nodes=` now carries everything it did. Cost if wrong: a divergence the JSON would have caught but the walk misses. The tests below pin each distinction the JSON used to make.

### Task 6.1: Protocol-free structural value description in `describePrevalResult`

**Files:**
- Modify: `CC/src/main/resources/core/pure/router/preeval/preeval.pure`: `describePrevalResult`, `describeNodes`, `describeInstanceValueElement`, plus new private helpers
- Modify: `CC/src/main/resources/core/pure/router/preeval/testImplementationSwitch.pure`: a probe class and new tests
- Modify: `docs/superpowers/plans/2026-09-29-preeval-p4-shadow-estate-manifest.md`: row status

**Interfaces:**
- `describePrevalResult` output becomes `canPreval=…, modified=…, openVars=[…], type=<t>, nodes=<lines>` for `FunctionDefinition` and `ValueSpecification` values.
- For any other value, the old `value=<toString>` is kept, because top-level results that are neither are compared as before.
- The `preeval SHADOW mismatch` prefix is unchanged.

**Required description content** (each line keeps the existing `indent + kind + ' : ' + type/multiplicity` shape where a node has a type):
- **Top-level `FunctionDefinition`:** a first line `function (<param>:<type><mult>, …)` giving each parameter's name and type, then its body nodes.
- **`FunctionExpression`:**
  - if `func` is a `PackageableElement`, `fe <elementToPath(func)>`;
  - if `func` is an `AbstractProperty`, `fe property <owner path>.<name>`;
  - otherwise `fe <functionName>`;

  followed by the type/multiplicity and then the parameters.
- **`VariableExpression`:** unchanged (`var <name> : …`).
- **`InstanceValue` elements:**
  - `ValueSpecification`, `LambdaFunction` and `KeyExpression`: as now.
  - A primitive (`String`, `Boolean`, `Integer`, `Float`, `Decimal`, `Number`, `Date`, `StrictDate`, `DateTime`, `StrictTime`): `<type path> <toString>`. Strings are wrapped in single quotes.
  - An `Enum`: `enum <enumeration path>.<name>`.
  - A `PackageableElement`: `element <path>`.
  - A function held as a value that is not a `LambdaFunction` (for example a property or a native): its identity, as for `fe`.
  - Any other instance: `instance <classifier path>`, then one child line per property in sorted name order, `<name> = …`, each value described by the same element rules at `indent + '  '`.
    - Recursion depth is capped at 4 below the instance. At the cap, print `instance <classifier path> …`.
    - Cycles are bounded by the cap.
- **Robustness:** every `printGenericType` / `printMultiplicity` call goes through `describeTypeAndMultiplicity` or an equivalent `isEmpty` guard, including the lambda return type (Task 1 deferred minor).

- [ ] **Step 1: Write the failing tests**

Add to `testImplementationSwitch.pure`:

```pure
Class meta::pure::router::preeval::tests::implementationSwitch::ShadowProbe
{
  name : String[1];
  size : Integer[1];
}

function <<access.private>> meta::pure::router::preeval::tests::implementationSwitch::probeValue(probe:ShadowProbe[1]):InstanceValue[1]
{
  let iv = {|1}->evaluateAndDeactivate().expressionSequence->at(0)->cast(@InstanceValue);
  ^$iv(genericType = ^GenericType(rawType = ShadowProbe), values = $probe);
}

function <<test.Test>> meta::pure::router::preeval::tests::implementationSwitch::testShadowAcceptsEqualInstancesWithoutProtocolSupport():Boolean[1]
{
  assertSamePrevalResult(
      ^PrevalWrapper<Any>(value = probeValue(^ShadowProbe(name = 'a', size = 1)), canPreval = true, openVars = [], modified = true),
      ^PrevalWrapper<Any>(value = probeValue(^ShadowProbe(name = 'a', size = 1)), canPreval = true, openVars = [], modified = true),
      newMap([]->cast(@Pair<String, List<Any>>)),
      []);
}

function <<test.Test>> meta::pure::router::preeval::tests::implementationSwitch::testShadowRejectsDifferentInstancesWithoutProtocolSupport():Boolean[1]
{
  assertError(
    | assertSamePrevalResult(
        ^PrevalWrapper<Any>(value = probeValue(^ShadowProbe(name = 'a', size = 1)), canPreval = true, openVars = [], modified = true),
        ^PrevalWrapper<Any>(value = probeValue(^ShadowProbe(name = 'a', size = 2)), canPreval = true, openVars = [], modified = true),
        newMap([]->cast(@Pair<String, List<Any>>)),
        []),
    {message:String[1], source:SourceInformation[0..1] | assert($message->startsWith('preeval SHADOW mismatch'), |$message)}
  );
}

function <<test.Test>> meta::pure::router::preeval::tests::implementationSwitch::testShadowRejectsDifferentFunctions():Boolean[1]
{
  assertError(
    | assertSamePrevalResult(
        ^PrevalWrapper<Any>(value = {|[1, 2]->first()}, canPreval = true, openVars = [], modified = false),
        ^PrevalWrapper<Any>(value = {|[1, 2]->last()}, canPreval = true, openVars = [], modified = false),
        newMap([]->cast(@Pair<String, List<Any>>)),
        []),
    {message:String[1], source:SourceInformation[0..1] | assert($message->startsWith('preeval SHADOW mismatch'), |$message)}
  );
}

function <<test.Test>> meta::pure::router::preeval::tests::implementationSwitch::testShadowRejectsDifferentProperties():Boolean[1]
{
  assertError(
    | assertSamePrevalResult(
        ^PrevalWrapper<Any>(value = {p:Pair<Integer, Integer>[1] | $p.first}, canPreval = true, openVars = [], modified = false),
        ^PrevalWrapper<Any>(value = {p:Pair<Integer, Integer>[1] | $p.second}, canPreval = true, openVars = [], modified = false),
        newMap([]->cast(@Pair<String, List<Any>>)),
        []),
    {message:String[1], source:SourceInformation[0..1] | assert($message->startsWith('preeval SHADOW mismatch'), |$message)}
  );
}

function <<test.Test>> meta::pure::router::preeval::tests::implementationSwitch::testShadowRejectsDifferentParameterNames():Boolean[1]
{
  assertError(
    | assertSamePrevalResult(
        ^PrevalWrapper<Any>(value = {a:Integer[1] | 1}, canPreval = true, openVars = [], modified = false),
        ^PrevalWrapper<Any>(value = {b:Integer[1] | 1}, canPreval = true, openVars = [], modified = false),
        newMap([]->cast(@Pair<String, List<Any>>)),
        []),
    {message:String[1], source:SourceInformation[0..1] | assert($message->startsWith('preeval SHADOW mismatch'), |$message)}
  );
}

function <<test.Test>> meta::pure::router::preeval::tests::implementationSwitch::testShadowRejectsDifferentTypesInsideNestedLambdas():Boolean[1]
{
  let fe = {|[1]->map(x | $x)}->evaluateAndDeactivate().expressionSequence->at(0)->cast(@SimpleFunctionExpression);
  let lambdaHolder = $fe.parametersValues->at(1)->cast(@InstanceValue);
  let lambda = $lambdaHolder.values->at(0)->cast(@LambdaFunction<Any>);
  let body = $lambda.expressionSequence->at(0)->cast(@VariableExpression);
  let widenedLambda = ^$lambda(expressionSequence = ^$body(genericType = ^GenericType(rawType = Number)));
  let widened = ^$fe(parametersValues = [$fe.parametersValues->at(0), ^$lambdaHolder(values = $widenedLambda)]);
  assertError(
    | assertSamePrevalResult(
        ^PrevalWrapper<Any>(value = $fe, canPreval = true, openVars = [], modified = false),
        ^PrevalWrapper<Any>(value = $widened, canPreval = true, openVars = [], modified = false),
        newMap([]->cast(@Pair<String, List<Any>>)),
        []),
    {message:String[1], source:SourceInformation[0..1] | assert($message->startsWith('preeval SHADOW mismatch'), |$message)}
  );
}
```

`testShadowRejectsDifferentTypesInsideNestedLambdas` pins the Task 1 deferred minor. It may already pass, since Task 1's walk covers lambda bodies, but it must stay green.

- [ ] **Step 2: RED**

Run CORE-BUILD, then COMPILED and INTERP. Expected:
- `testShadowAcceptsEqualInstancesWithoutProtocolSupport` and `testShadowRejectsDifferentInstancesWithoutProtocolSupport` fail in both modes. Today `transformValueSpecification` throws `... can't be translated` on the `ShadowProbe` value, so the first gets an unexpected error and the second gets a message that does not start with `preeval SHADOW mismatch`.
- The function, property, parameter-name and nested-lambda tests pass today, because JSON or `nodes=` already distinguishes them. They are regression pins for the next step.

If a probe test does not fail as described, stop and report. The probe then does not reproduce the estate's failure mode.

- [ ] **Step 3: Implement** the Required description content above in `preeval.pure`.
  - Remove the `value=` JSON segment for `FunctionDefinition` and `ValueSpecification` values; keep `$a->toString()` for other values.
  - Remove any imports and calls that are no longer used.
  - Use `Class.properties` (plus `propertiesFromAssociations` if needed) and evaluate each property on the instance, e.g. `$p->eval($instance)`. Check how `meta::pure::functions::meta` or other core code reads property values generically, and reuse that idiom.
  - Report each compiler-forced adaptation.

- [ ] **Step 4: GREEN**

Run CORE-BUILD, then:
- **COMPILED:** `Test_Pure_Preeval` 119 and `Test_Pure_Preeval_Java` 238, i.e. 6 more tests per collection.
- **INTERP:** `Test_Interpreted_Preeval` 360, i.e. (114 + 6) × 3.
- **Relational:** RC-BUILD, then RC-TEST `Test_Pure_Relational_Preeval_Java` 2 and `Test_Interpreted_Relational_Preeval` 3.

Every existing `testImplementationSwitch` test stays green, including the Task 1 tests. Then run CC-TEST `Test_Pure_Core`, which must stay at 1192, and Checkstyle on CC.

- [ ] **Step 5: Manifest and commit**

Set rows 1–4 of `## Estate findings` to `fixed in <sha>`. Correct the triage note wording if the fix differs from its suggested direction. Then:
```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/main/resources/core/pure/router/preeval/preeval.pure legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/main/resources/core/pure/router/preeval/testImplementationSwitch.pure docs/superpowers/plans/2026-09-29-preeval-p4-shadow-estate-manifest.md
git commit -m "Describe preeval SHADOW values without the protocol"
```

### Task 6.2: Estate re-run after the comparator fix (controller)

Re-run Task 5 Step 1 (build) and Step 2 (both modes, all five suites). Then re-diff each suite, with `$S/estate/run.sh` and `diff.py`.

- **If there are no `SHADOW-ONLY` lines,** add a second dated row per suite to the manifest's runs table, and commit it with the message `Record the preeval shadow estate after the comparator fix`.
- **If `SHADOW-ONLY` lines remain,** the relation and OLAP coverage the old comparator crash hid is now real:
  1. Triage them as in Task 5 Step 4.
  2. Append them to `## Estate findings` as rows 5 onward.
  3. Commit the manifest.
  4. Amend this plan with sub-tasks 6.3 onward, one per root cause, following the Task 6 shape.
  5. Commit the amendment with the message `Plan the remaining P4 estate fixes`.
  6. Execute them.

---

### Task 7: Exit verification and spec

**Files:** Modify `docs/superpowers/specs/2026-09-27-preeval-java-native-design.md` §5.

- [ ] **Step 1: Regression**

Run CORE-BUILD, then a full compiled-core `mvn clean install` with tests on and `-DargLine=-Xmx6g`. Every class must be green; record the per-class counts. Also run RC-TEST for `Test_Pure_Relational_Preeval_Java` and, if Task 4 committed it, `Test_Interpreted_Relational_Preeval`. Run Checkstyle on every module P4 touched.

- [ ] **Step 2: Spec**

Add a `### P4 status (<date>)` section after P3's. Mark the P4 row in the phase table done, noting "zero unexplained differences" and linking the manifest. Record:
- **The per-node comparator.** What `nodes=` covers:
  - every `FunctionExpression`, `InstanceValue`, `VariableExpression` and other `ValueSpecification` node, with its generic type and multiplicity;
  - nested lambdas with their signature;
  - `KeyExpression`s.

  What it deliberately skips: named functions held as values. This closes the P2 known gap "`SHADOW` does not gate nested types".
- **Robust type printing** in `describePrevalResult`. This closes the P3 latent-NPE gap. The two `preeval.pure` construction sites need no change.
- **Source information: kept as a deliberate deviation** (user decision, 2026-09-29), and why: errors point at user code, and `SHADOW` does not compare source information. Remove "Decide before P4" from the P2 gap.
- **Wrapper type argument outside the parity contract:** the ruling above. Close the P3 wrapper-typing gap with that reasoning.
- **Unique runner test names:** the compiled-core runners now report `name[IMPLEMENTATION]`. Close the P3 Surefire gap, and note that the relational runners still use `TestSetup` (P6).
- **Interpreted relational:** Task 4's outcome, either green, or the recorded blocker still open.
- **The estate:** the suites run, the finding counts by category, the fixes, the pre-existing failures, and what is not covered, with a pointer to the manifest.

- [ ] **Step 3: Commit**

Message: `Record P4 shadow estate completion in the preeval spec`.

## P4 exit criteria (spec §5)

- `SHADOW` compares type and multiplicity at every node, and the compiled, interpreted and relational preeval runners are green with empty known lists.
- The five estate suites show no `SHADOW`-only failures against their `PURE` control. Every other difference is explained in the committed manifest.
- The spec records the comparator, the source-information decision, the wrapper-type ruling, the interpreted-relational outcome and the estate result.
