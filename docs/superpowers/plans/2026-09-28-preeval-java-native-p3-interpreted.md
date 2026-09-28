# Preeval Java Native — P3 Interpreted Mode Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Run the whole `meta::pure::router::preeval::tests` suite in interpreted mode under `PURE`, `JAVA` and `SHADOW`, and make it green. Fix the one interpreted-only bug in `prevalJava`, the two interpreted-hostile test constructions, and the interpreted adapter's remaining unresolved value reads.

**Architecture:** A new compiled-core test runner, `Test_Interpreted_Preeval`. It builds the interpreted suite **once**, because every `PureTestBuilderInterpreted` runtime build is expensive, and wraps that one suite in three `TestSetup`s that set `legend.engine.preeval.implementation`. The interpreted native test dependencies it needs are added to compiled-core at test scope.

**Tech Stack:** Java 11, legend-pure 5.105.0 interpreted runtime, Pure, JUnit 3 suites.

**Spec:** `docs/superpowers/specs/2026-09-27-preeval-java-native-design.md`, phase **P3** of §5 plus its P2 known gaps.
**Previous plans:** the P0, P1 and P2 plans in `docs/superpowers/plans/`. All are complete on branch `preeval-native`.

## Global Constraints

- JDK 11. Prefix every Maven command with `. /home/aziem/bin/jdk11.sh &&`, and pass `-Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true`. Lifecycle builds add `-T 3` and always `clean`. compiled-core's PAR plugin fails without `clean`.
- Direct Surefire runs **must** pass `-DargLine=`. To set a forked-JVM system property, put it inside that flag, e.g. `-DargLine=-Dlegend.engine.preeval.implementation=JAVA`.
- Checkstyle: `mvn checkstyle:check -Dcheckstyle.config.location=$(pwd)/checkstyle.xml -pl <modules>`, run from the repo root. New Java files need the Apache header (first line `Copyright 2026 Goldman Sachs`) followed by a blank line before `package`.
- RED builds of Java changes use `-Dmdep.analyze.skip=true`. The final GREEN build must pass the analyzer.
- No explanatory comments, except `// parity:` for a Pure quirk reproduced on purpose. Java: 4-space indent, braces on their own lines. Pure: 2-space indent.
- Commits are authored solely by the user: **no** `Co-Authored-By` or `Claude-Session` trailers. Messages are sentence-case imperatives. Never `git add -A` / `git add .`. Never a bare `git stash`.
- **Never touch or add** the untracked `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/test/java/org/finos/legend/engine/pure/code/core/interpreted/` directory. It is someone else's work in progress. The new runner goes in package `org.finos.legend.engine.pure.code.core`, **not** in `…core.interpreted`.
- The switch default stays `PURE`. Nothing in P3 changes compiled production behaviour.

## Background: spike findings (2026-09-28, throwaway, fully reverted)

A spike ran `PureTestBuilderInterpreted.buildSuite("meta::pure::router::preeval::tests")` (112 tests) in compiled-core under each implementation.

| Implementation | Errors | Cause |
|---|---|---|
| PURE | 2 | `implementationSwitch::testShadowRejectsDifferentTypes` and `testShadowRejectsDifferentMultiplicities` build `^InstanceValue(genericType=^GenericType(rawType=Integer), multiplicity=PureOne, values=1)` by hand. In interpreted mode, `describePrevalResult` on such a value throws a bare NPE: `M3ProcessorSupport.getClassifier` ← `Instance.instanceOf` ← interpreted `InstanceOf`, inside the vX_X_X `transformValueSpecification` InstanceValue branch or the `printGenericType` match. `assertError` then sees an empty message, so the `startsWith('preeval SHADOW mismatch')` check fails. |
| JAVA | 101 | The same 2, plus 99 tests failing with `Cast exception: PrevalWrapper<Any> cannot be cast to PrevalWrapper<FunctionDefinition<Any>>` at preeval.pure line ~84. `prevalJava` always builds `^PrevalWrapper<Any>`. Interpreted `cast` checks type arguments, while compiled erases them, so compiled mode never saw this. PURE builds `^PrevalWrapper<FunctionDefinition<Any>>` for function definitions. |
| SHADOW | 2 | The same 2. SHADOW returns the PURE wrapper, which hides the cast bug. |

When the spike fixed `prevalJava` to build the typed wrapper, JAVA and SHADOW both dropped to exactly the 2 PURE failures. No failures originated in Java: `InterpretedPrevalRuntime`, `InterpretedPrevalHooks` and `PrevalNative` agree with PURE on all 110 other tests, including the TDS `columns` and eval-on-Column paths.

**Interpreted test dependencies that were enough**, at test scope in compiled-core:
- `org.finos.legend.engine`: `legend-engine-pure-runtime-java-extension-interpreted-functions-standard`, `-unclassified`, `-json`, `-relation`. Versions come from root dependencyManagement.
- `org.finos.legend.pure`: `legend-pure-runtime-java-extension-interpreted-store-relational`, `legend-pure-runtime-java-extension-interpreted-dsl-tds`, and `legend-pure-runtime-java-extension-interpreted-dsl-path`. The last is not in dependencyManagement, so add `<version>${legend.pure.version}</version>`.
- The interpreted engine is already `provided` and `interpreted-functions-preeval` is already `runtime`. Do not re-declare them.

Per-implementation run time was about 16–71 s, plus the runtime build.

## File Structure

```
compiled-core (CC = legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core)
  pom.xml                                                                  MODIFY (T1): interpreted test deps
  src/test/java/org/finos/legend/engine/pure/code/core/Test_Interpreted_Preeval.java   CREATE (T1)
  src/main/resources/core/pure/router/preeval/preeval.pure                 MODIFY (T1): typed wrapper in prevalJava
  src/main/resources/core/pure/router/preeval/testImplementationSwitch.pure MODIFY (T2)
INTERPRETED adapter
  …/interpreted/InterpretedPrevalRuntime.java                              MODIFY (T3)
docs/superpowers/specs/2026-09-27-preeval-java-native-design.md           MODIFY (T4)
```

**Standard commands:**
- **CORE-BUILD**:
  ```bash
  . /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -DskipTests -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core
  ```
- **INTERP**:
  ```bash
  . /home/aziem/bin/jdk11.sh && mvn -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -DargLine= -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core org.apache.maven.plugins:maven-surefire-plugin:2.22.2:test -Dtest=Test_Interpreted_Preeval
  ```
- **COMPILED**: the same with `-Dtest='Test_Pure_Preeval,Test_Pure_Preeval_Java'`.
- **FAMILY**: the four-module preeval family build (see the P2 plan's Standard verification commands).

---

### Task 1: Interpreted runner, and the typed `PrevalWrapper` in `prevalJava`

**Files:**
- Modify: `CC/pom.xml`: add the seven test-scope dependencies listed in Background.
- Create: `CC/src/test/java/org/finos/legend/engine/pure/code/core/Test_Interpreted_Preeval.java`
- Modify: `CC/src/main/resources/core/pure/router/preeval/preeval.pure`: the body of `prevalJava`

**Interfaces:**
- Produces `Test_Interpreted_Preeval`:
  - It has a static `ImmutableSet<String> KNOWN_INTERPRETED_FAILURES`, initially the 2 spike names: `testShadowRejectsDifferentTypes` and `testShadowRejectsDifferentMultiplicities`.
  - It builds the suite **once**, then drops tests by name. `PureTestBuilder.PureTestCase` names are the Pure `functionName`.
  - It wraps the filtered suite in three `TestSetup`s, for `PURE`, `JAVA` and `SHADOW`.
  - It throws `IllegalStateException` from `suite()` if a `KNOWN_INTERPRETED_FAILURES` name matches no collected test.
  - The system property `legend.engine.preeval.test.includeKnownDivergent=true` disables the filtering.

- [ ] **Step 1: Add the dependencies and the runner**

```java
package org.finos.legend.engine.pure.code.core;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.api.set.MutableSet;
import org.finos.legend.pure.runtime.java.interpreted.testHelper.PureTestBuilderInterpreted;

import java.util.Enumeration;

public class Test_Interpreted_Preeval
{
    private static final String PROPERTY = "legend.engine.preeval.implementation";
    private static final String INCLUDE_KNOWN_DIVERGENT = "legend.engine.preeval.test.includeKnownDivergent";

    static final ImmutableSet<String> KNOWN_INTERPRETED_FAILURES = Sets.immutable.with("testShadowRejectsDifferentTypes", "testShadowRejectsDifferentMultiplicities");

    public static TestSuite suite()
    {
        TestSuite all = PureTestBuilderInterpreted.buildSuite("meta::pure::router::preeval::tests");
        MutableSet<String> seen = Sets.mutable.empty();
        TestSuite selected = filter(all, Boolean.getBoolean(INCLUDE_KNOWN_DIVERGENT), seen);
        ImmutableSet<String> unmatched = KNOWN_INTERPRETED_FAILURES.reject(seen::contains);
        if (unmatched.notEmpty())
        {
            throw new IllegalStateException("KNOWN_INTERPRETED_FAILURES names that match no collected preeval test: " + unmatched.toSortedList().makeString(", "));
        }
        TestSuite suite = new TestSuite();
        suite.addTest(withImplementation("PURE", selected));
        suite.addTest(withImplementation("JAVA", selected));
        suite.addTest(withImplementation("SHADOW", selected));
        return suite;
    }

    private static TestSuite filter(TestSuite source, boolean includeKnown, MutableSet<String> seen)
    {
        TestSuite result = new TestSuite(source.getName());
        for (Enumeration<Test> tests = source.tests(); tests.hasMoreElements(); )
        {
            Test test = tests.nextElement();
            if (test instanceof TestSuite)
            {
                result.addTest(filter((TestSuite) test, includeKnown, seen));
            }
            else
            {
                String name = ((TestCase) test).getName();
                seen.add(name);
                if (includeKnown || !KNOWN_INTERPRETED_FAILURES.contains(name))
                {
                    result.addTest(test);
                }
            }
        }
        return result;
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
If `TestSuite.tests()` returns a raw `Enumeration` in the JUnit version on the classpath, adapt the generic and record it. Wrapping the same `TestSuite` instance in three `TestSetup`s is deliberate: the test cases are stateless, and building three runtimes would triple the cost.

- [ ] **Step 2: RED**

Run CORE-BUILD, then INTERP. Expected: the `PURE` and `SHADOW` suites pass (110 each). The `JAVA` suite fails 99 tests with `Cast exception: PrevalWrapper<Any> cannot be cast to PrevalWrapper<FunctionDefinition<Any>>`.

- [ ] **Step 3: Fix `prevalJava`**

Replace the final `^PrevalWrapper<Any>(...)` expression of `prevalJava` in preeval.pure with the typed construction PURE uses:
```pure
  $r.value->match([
    fd:FunctionDefinition<Any>[1] | ^PrevalWrapper<FunctionDefinition<Any>>(value = $fd, canPreval = $r.canPreval, openVars = $r.openVars, modified = $r.modified),
    a:Any[1] | ^PrevalWrapper<Any>(value = $a, canPreval = $r.canPreval, openVars = $r.openVars, modified = $r.modified)
  ]);
```
Keep the existing `assert` and the `let r = …prevalNative(...)` line unchanged.

- [ ] **Step 4: GREEN**

Run CORE-BUILD, INTERP and COMPILED. Expected:
- INTERP: 330 tests (110 × 3), 0 failures.
- COMPILED: `Test_Pure_Preeval` 111/111 and `Test_Pure_Preeval_Java` 222/222, both unchanged.
- The compiled-core analyzer stays clean. The new dependencies are test scope, which `ignoreNonCompile` exempts.

- [ ] **Step 5: Commit**

```bash
git add legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/pom.xml \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/test/java/org/finos/legend/engine/pure/code/core/Test_Interpreted_Preeval.java \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/main/resources/core/pure/router/preeval/preeval.pure
git status --short
git commit -m "Run the preeval suite in interpreted mode and return typed Java preeval wrappers"
```

---

### Task 2: Make the SHADOW-rejection tests interpreted-safe

**Files:**
- Modify: `CC/src/main/resources/core/pure/router/preeval/testImplementationSwitch.pure`: `testShadowRejectsDifferentTypes`, `testShadowRejectsDifferentMultiplicities`
- Modify: `Test_Interpreted_Preeval.KNOWN_INTERPRETED_FAILURES`, which becomes empty

- [ ] **Step 1: Find the NPE site**

Diagnose in one focused investigation. Call `describePrevalResult` on the hand-built value from an interpreted snippet; a temporary extra test run by INTERP is fine, and must not be committed. Name the exact Pure expression that NPEs.

Then decide:
- **(a) The NPE comes from how the value is built.** For example, an `^InstanceValue` made with `^GenericType(rawType=Integer)` has no classifierGenericType or other metadata that interpreted `instanceOf` needs. Rebuild the tests' values from real expressions. Examples:
  - integer versus number: `{|1}->evaluateAndDeactivate().expressionSequence->at(0)` against the same node copied with `^$iv(genericType = ^GenericType(rawType = Number))`;
  - multiplicity: `^$iv(multiplicity = ZeroMany)`.

  Keep each test's intent: SHADOW rejects a difference in only the type, or only the multiplicity.
- **(b) The NPE would also hit real preeval results.** For example, it happens for any InstanceValue whose genericType was set by `^`. Then this is an interpreted-mode bug in `describePrevalResult` that real SHADOW runs could hit. Make `describePrevalResult` robust instead, and add a regression test.

Record which case applies, with the stack trace.

- [ ] **Step 2: Implement, and empty `KNOWN_INTERPRETED_FAILURES`**

- [ ] **Step 3: GREEN**

Run CORE-BUILD, INTERP (336 = 112 × 3, 0 failures) and COMPILED (111 and 222, unchanged). The two tests must still fail if SHADOW stops comparing type or multiplicity: temporarily break the comparison to check, and do not commit that.

- [ ] **Step 4: Commit**

Stage the changed paths explicitly. Message: `Build SHADOW rejection test values that work in interpreted mode`.

---

### Task 3: Resolve interpreted value reads consistently

This is carried from the P2 final review.

**Files:**
- Modify: `…-interpreted-functions-preeval/src/main/java/org/finos/legend/engine/pure/preeval/interpreted/InterpretedPrevalRuntime.java`: `toVars` (~line 101) and `openVariableValues` (~line 227)
- Test: `…-shared-functions-preeval/src/test/java/org/finos/legend/engine/pure/preeval/AbstractTestPrevalNative.java` (both modes)

- [ ] **Step 1: A failing interpreted scenario**

Try to write a harness test where a vars-map value or a closure-captured value is a stub: an enum value such as `DurationUnit.DAYS`, captured by a lambda or passed in `inScopeVars`. It should reach `stopPreeval` or `isInstanceOf`, and pass in compiled mode but fail or misbehave in interpreted mode today.

If no platform-only scenario makes the unresolved read observable, record that. The fix is then a consistency change covered by the existing suites.

- [ ] **Step 2: Implement**

Read values with `Instance.getValueForMetaPropertyToManyResolved(…, M3Properties.values, processorSupport)` in both methods.

- [ ] **Step 3: GREEN**

Run FAMILY (harness in both modes), CORE-BUILD, INTERP and COMPILED, all unchanged or better.

- [ ] **Step 4: Commit**

Message: `Resolve stub values in interpreted preeval variable maps`.

---

### Task 4: Exit verification and spec

**Files:**
- Modify: `docs/superpowers/specs/2026-09-27-preeval-java-native-design.md` §5

- [ ] **Step 1: Full regression**

Run COMPILED plus `Test_Pure_Core` (1192), INTERP (336), and Checkstyle on compiled-core and the interpreted module. All green.

- [ ] **Step 2: Spec**

In §5:
- mark P3 done: `Test_Interpreted_Preeval` 336/336 under PURE, JAVA and SHADOW;
- record the `prevalJava` typed-wrapper bug as found by P3. Compiled erasure and SHADOW both hid it, which argues for keeping interpreted runs in CI;
- update the P2 known gaps: the interpreted TDS/Relation paths are now exercised by the interpreted suite, and the unresolved-getter item is done;
- note that compiled-core's test run now takes about 2–3 more minutes for the interpreted suite;
- note that interpreted coverage of the relational `testPrerouting42` is still pending, because it needs relational interpreted natives in that module. That is a P4 item.

- [ ] **Step 3: Commit**

Message: `Record P3 interpreted-mode completion in the preeval spec`.

## P3 exit criteria (spec §5)

- `Test_Interpreted_Preeval` is green under PURE, JAVA and SHADOW, and `KNOWN_INTERPRETED_FAILURES` is empty.
- The compiled suites are unchanged: 111, 222, and 1192 for `Test_Pure_Core`.
- The spec records P3's findings and the remaining relational-interpreted gap.
