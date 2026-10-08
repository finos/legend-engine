# Preeval Java Native — P5 Benchmark and Cutover Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Measure the Java preeval against the Pure one, prove on the full default-profile CI estate that `SHADOW` finds no differences, and make `JAVA` the default, with `PURE` kept selectable for one release as a rollback path.

**Architecture:**
- The shared Java core gains opt-in statistics: rule fire counts, and call counts and time for each Pure hook.
- The pipeline benchmark gains a `--preeval` comparison mode. It runs each workload under `PURE` and `JAVA` alternately in one JVM, and times every `traceSpan('preval')` through its own OpenTracing tracer.
- A manual `preeval_implementation` input on the CI build workflow runs the whole default-profile test estate under `SHADOW`.
- The cutover itself changes one default, in `PreevalImplementation.parse`.

**Tech Stack:** Java 11 (CI builds and benchmarks on JDK 17), Pure, legend-pure 5.105.0, OpenTracing 0.32.0, JUnit 5 (new Java tests outside compiled-core), JUnit 4 (compiled-core and the legend-pure native test harness), GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-09-27-preeval-java-native-design.md`: phase **P5** of §5, §4.5 (performance), §6 (cutover criteria), §8 (the per-hook call-count risk), and the "Known gaps (carried forward)" under "P4 status".

**Previous plans:** P0–P4 in `docs/superpowers/plans/`, all complete on branch `preeval-native`. The P4 estate manifest is `docs/superpowers/plans/2026-09-29-preeval-p4-shadow-estate-manifest.md`.

## Global Constraints

- JDK 11. Prefix every Maven command with `. /home/aziem/bin/jdk11.sh &&` and pass `-Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true`.
  - Lifecycle builds add `-T 3` and always `clean`.
  - Build only the modules named.
- Direct Surefire runs **must** pass `-DargLine=...`. Put forked-JVM system properties inside it, e.g. `-DargLine="-Xmx6g -Dlegend.engine.preeval.implementation=SHADOW"`.
- Checkstyle: run `mvn checkstyle:check -Dcheckstyle.config.location=$(pwd)/checkstyle.xml -pl <modules>` from the repo root.
- New files need the Apache header, first line `Copyright 2026 Goldman Sachs`.
- RED builds of Java changes use `-Dmdep.analyze.skip=true`. The final GREEN build must pass the dependency analyzer.
- Code style:
  - No explanatory comments, except `// parity: <reason>` for a Pure quirk reproduced on purpose.
  - Java: 4-space indent, braces on their own lines. Pure: 2-space indent.
- Commits are authored solely by the user: **no** `Co-Authored-By` or `Claude-Session` trailers.
  - Messages are sentence-case imperatives.
  - Never `git add -A` / `git add .`. Never a bare `git stash`.
- **Never touch or add** the untracked `legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/test/java/org/finos/legend/engine/pure/code/core/interpreted/` directory. It is the user's work in progress. Its one test (`Test_Interpreted_GraphFetchUnion`) runs in full compiled-core builds; exclude it from recorded counts.
- compiled-core and the legend-pure native test harness (`AbstractTestPrevalNative`) use JUnit 4. New Java tests elsewhere use JUnit 5.
- Maven commands in subagents run in the **foreground** with a 600000 ms timeout. A run that cannot fit in 10 minutes is split by `-Dtest=` class, or handed to the controller.
- Scratch output (logs, reports, recordings) goes in the plan workspace `.superpowers/sdd/2026-09-30-preeval-java-native-p5-benchmark-cutover/`, never `/tmp`. P4 lost a run to a reboot that wiped `/tmp`.
- **Outward-facing actions need the user's go-ahead:** pushing any branch, dispatching a workflow, and downloading CI artifacts from a fork run. The controller asks, then runs them.
- Until Task 6, the switch default stays `PURE`. Tasks 1–5 do not change production behaviour.

## Decisions already made

- **User, 2026-09-29:** source information stays a deliberate deviation. The CI `SHADOW` job belongs to P5.
- **Spec §6:** cutover needs zero `SHADOW` differences across default-profile CI suites, a green interpreted runner, and no benchmark regression. After cutover the default is `JAVA`, and `PURE` stays selectable for one release.

## Rulings made while planning

- **Preeval time comes from the existing `traceSpan('preval')`.** Every public `preval` overload (A–F) runs inside one `traceSpan('preval')` in `preeval.pure`. Compiled `traceSpan` (`FunctionsHelper.traceSpan`) opens a span only when `GlobalTracer.isRegistered()`. The benchmark registers its own tracer, `SpanTimer`, which times outermost `preval` spans with `System.nanoTime()`. `opentracing-mock` is not used, because its timestamps have millisecond precision. Cost if wrong: tracer overhead lands on both implementations equally, so the ratio stays valid, but absolute `planPure` numbers in `--preeval` mode are not comparable with the default suite's.
- **One JVM, alternating implementations.** `PreevalImplementation.current()` re-reads the system property on every call, so the comparison flips `legend.engine.preeval.implementation` between iterations. The order alternates each iteration, so JIT drift does not favour either side.
- **Statistics are counts, not per-rule times.** Rules recurse into each other, so a per-rule timer would count nested work twice. Hook time *is* measured, because hooks are leaf calls back into Pure, and they are exactly what the §8 risk is about. Statistics are off unless `legend.engine.preeval.statistics=true`. The comparison collects them in a separate pass, so they never perturb the timed runs.
- **No SQL-over-Legend workload.** The benchmark module does not depend on the SQL modules. SQL's `preval` call (`fromPure.pure:251`) runs once per query on an already small lambda, and the P4 estate covered its correctness. Cost if wrong: a SQL-only slowdown is caught only by the default suite after cutover.
- **The regression rule.** A workload regresses when `JAVA` exceeds `PURE`, on either median `planPure` or median preval time, by more than **both** `--margin` (default 0.10) **and** `--min-delta-ms` (default 10). Anything smaller is JIT noise. A regression is a **stop**: see Task 3.
- **The rollback path stays tested.** After cutover the default run is `JAVA`, so the compiled runners `Test_Pure_Preeval_Java` and `Test_Pure_Relational_Preeval_Java` add `PURE` to their implementations. `Test_Interpreted_Preeval` and `Test_Interpreted_Relational_Preeval` already run all three.
- **Two branches for the CI estate.** `build.yml` cancels an in-progress run with the same ref, so the control and `SHADOW` runs go on two fork branches that point at one commit.
- **Perf baseline after cutover.** `perf-baseline.json` has no entry for this machine (AMD Ryzen 9 PRO 6950H), only for the CI runners and a Mac. After cutover the CI Pipeline Benchmark reports a deviation, which is expected, since preeval got faster. The rebase goes through the **Performance Baseline** workflow on the P5 PR, which the user runs. This plan records the local `--suite default` numbers under `JAVA` in the results doc for reference.
- **Interpreted estate coverage stays with the runners.** No estate suite runs interpreted, because the PCT and relational adapters are compiled-only. So the P4 gap "interpreted SHADOW never ran on the estate" is answered by `Test_Interpreted_Preeval` and `Test_Interpreted_Relational_Preeval` staying green under all three implementations, and it is recorded as accepted in Task 7.

## Review Focus

1. **Concurrent plan generation with statistics on.** A server generates plans on many threads. The counters must not lose increments: Task 1 adds `testCountersAreThreadSafe`.
2. **Nested `preval` spans.** Store routing can call `preval` while an outer `preval` span is open on the same thread. Only the outermost span may count, and depth is tracked per thread: Task 2 adds `testNestedSameNameSpansCountOnce` and `testDepthIsPerThread`.
3. **Another tracer already registered, or no spans recorded.** The comparison would then measure nothing and report a meaningless ratio. It must fail loudly: Task 2 adds `testCheckSpansRejectsZeroSpans`, and `GlobalTracer.registerIfAbsent` returning `false` throws.
4. **`PURE` and `JAVA` recording different numbers of `preval` spans.** That would mean the implementations are not running the same work, so the ratio is invalid: Task 2 adds `testCheckSpansRejectsUnequalCounts`.
5. **An operator pinning `PURE` after cutover.** The rollback must still be honoured: Task 6 adds `testExplicitPureIsStillSelectable`.

---

## File Structure

```
preeval shared (SH = legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval)
  src/main/java/org/finos/legend/engine/pure/preeval/PreevalStatistics.java     CREATE (T1)
  src/main/java/org/finos/legend/engine/pure/preeval/CountingPrevalHooks.java   CREATE (T1)
  src/main/java/org/finos/legend/engine/pure/preeval/Preevaluator.java          MODIFY (T1): count rule applications
  src/main/java/org/finos/legend/engine/pure/preeval/PreevalImplementation.java MODIFY (T6): default JAVA
  src/test/java/org/finos/legend/engine/pure/preeval/TestPreevalStatistics.java CREATE (T1)
  src/test/java/org/finos/legend/engine/pure/preeval/AbstractTestPrevalNative.java MODIFY (T1): end-to-end statistics tests (run compiled and interpreted)
  src/test/java/org/finos/legend/engine/pure/preeval/TestPreevalImplementation.java MODIFY (T6)
preeval compiled (CP = …/legend-engine-pure-runtime-java-extension-compiled-functions-preeval)
  src/main/java/org/finos/legend/engine/pure/preeval/compiled/CompiledPreeval.java              MODIFY (T1): instrument hooks
preeval interpreted (IP = …/legend-engine-pure-runtime-java-extension-interpreted-functions-preeval)
  src/main/java/org/finos/legend/engine/pure/preeval/interpreted/natives/PrevalNative.java      MODIFY (T1): instrument hooks
perf benchmark (PB = legend-engine-config/legend-engine-perf-benchmark)
  pom.xml                                                                MODIFY (T2): opentracing, preeval shared, JUnit 5
  src/main/java/org/finos/legend/engine/perf/SpanTimer.java              CREATE (T2)
  src/main/java/org/finos/legend/engine/perf/PreevalComparison.java      CREATE (T2)
  src/main/java/org/finos/legend/engine/perf/PipelineBench.java          MODIFY (T2): runOnce visibility, --preeval dispatch
  src/test/java/org/finos/legend/engine/perf/TestSpanTimer.java          CREATE (T2)
  src/test/java/org/finos/legend/engine/perf/TestPreevalComparison.java  CREATE (T2)
  README.md                                                              MODIFY (T2): "Comparing preeval implementations"
compiled-core (CC = legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core)
  src/test/java/org/finos/legend/engine/pure/code/core/Test_Pure_Preeval_Java.java  MODIFY (T6): add PURE
relational core-pure (RC = legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure)
  src/test/java/org/finos/legend/pure/code/core/relational/Test_Pure_Relational_Preeval_Java.java  MODIFY (T6): add PURE
.github/workflows/build.yml                                               MODIFY (T4): preeval_implementation input
docs/engineering/architecture/preeval.md                                  MODIFY (T6): implementation switch section
docs/superpowers/plans/2026-09-30-preeval-p5-results.md                   CREATE (T3), EXTEND (T5, T6)
docs/superpowers/specs/2026-09-27-preeval-java-native-design.md           MODIFY (T3 target, T7 status)
```

**Standard commands:**
- **FAMILY** (builds and tests the three preeval Java modules):
  ```bash
  . /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-compiled-functions-preeval,legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-interpreted-functions-preeval
  ```
- **CORE-BUILD**:
  ```bash
  . /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -DskipTests -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core
  ```
- **CC-TEST `<classes>`**:
  ```bash
  . /home/aziem/bin/jdk11.sh && mvn -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -DargLine=-Xmx6g -pl legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core org.apache.maven.plugins:maven-surefire-plugin:2.22.2:test -Dtest=<classes>
  ```
  - COMPILED = CC-TEST `'Test_Pure_Preeval,Test_Pure_Preeval_Java'`
  - INTERP = CC-TEST `Test_Interpreted_Preeval`
- **RC-BUILD**: CORE-BUILD with `-pl` pointing at RC. **RC-TEST `<classes>`**: CC-TEST with `-pl` pointing at RC.
- **PB-BUILD** (builds and tests the benchmark module):
  ```bash
  . /home/aziem/bin/jdk11.sh && mvn clean install -T 3 -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -pl legend-engine-config/legend-engine-perf-benchmark
  ```
- **PB-RUN `<args>`** (from the repo root; `W` is the plan workspace):
  ```bash
  . /home/aziem/bin/jdk11.sh && mvn -q -Drevision=preeval-native-SNAPSHOT -Dmaven.gitcommitid.skip=true -pl legend-engine-config/legend-engine-perf-benchmark dependency:build-classpath -Dmdep.outputFile=$W/perf-cp.txt \
    && java -Xmx8g -Xss8m -cp "legend-engine-config/legend-engine-perf-benchmark/target/classes:$(cat $W/perf-cp.txt)" org.finos.legend.engine.perf.PipelineBench <args>
  ```
- **Baseline counts at plan time (HEAD `d1514f658e5`):** `Test_Pure_Preeval` 131, `Test_Pure_Preeval_Java` 262, `Test_Interpreted_Preeval` 396, `Test_Pure_Core` 1212, `Test_Pure_Relational_Preeval_Java` 2, `Test_Interpreted_Relational_Preeval` 3.

---

### Task 1: Opt-in preeval statistics in the Java core

**Files:**
- Create: `SH/src/main/java/org/finos/legend/engine/pure/preeval/PreevalStatistics.java`
- Create: `SH/src/main/java/org/finos/legend/engine/pure/preeval/CountingPrevalHooks.java`
- Modify: `SH/src/main/java/org/finos/legend/engine/pure/preeval/Preevaluator.java` (the four rule `apply` sites in `prevalFunctionExpression`, ~lines 261–299)
- Modify: `CP/src/main/java/org/finos/legend/engine/pure/preeval/compiled/CompiledPreeval.java:49`
- Modify: `IP/src/main/java/org/finos/legend/engine/pure/preeval/interpreted/natives/PrevalNative.java:67`
- Test: `SH/src/test/java/org/finos/legend/engine/pure/preeval/TestPreevalStatistics.java` (JUnit 5)
- Test: `SH/src/test/java/org/finos/legend/engine/pure/preeval/AbstractTestPrevalNative.java` (JUnit 4; runs as `TestPrevalNativeCompiled` and `TestPrevalNativeInterpreted`)

**Interfaces:**
- Produces:
  - `PreevalStatistics.SYSTEM_PROPERTY = "legend.engine.preeval.statistics"`
  - `static boolean enabled()`
  - `static PrevalHooks instrument(PrevalHooks hooks)`
  - `static void ruleApplied(Object rule)`
  - `static java.util.Map<String, Long> ruleCounts()`, `hookCallCounts()`, `hookNanos()`
  - `static void reset()`

  Hook keys are `stopPreeval`, `shouldInline`, `isGeneratedMilestoningProperty`, `isGetAllFunction` and `resolveTdsSchema`. Rule keys are the rule's simple class name, e.g. `ReactivateRule`.

- [ ] **Step 1: Write the failing unit tests**

`TestPreevalStatistics.java`:

```java
// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.pure.preeval;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestPreevalStatistics
{
    private static final PrevalHooks FIXED = new PrevalHooks()
    {
        @Override
        public boolean stopPreeval(ListIterable<?> values)
        {
            return true;
        }

        @Override
        public boolean shouldInline(Object function)
        {
            return false;
        }

        @Override
        public boolean isGeneratedMilestoningProperty(Object function)
        {
            return true;
        }

        @Override
        public boolean isGetAllFunction(Object function)
        {
            return false;
        }

        @Override
        public ImmutableList<Object> resolveTdsSchema(ValueSpecification value, ImmutableMap<String, ImmutableList<Object>> rollingInScopeVars)
        {
            return Lists.immutable.with("schema");
        }
    };

    @AfterEach
    public void clear()
    {
        System.clearProperty(PreevalStatistics.SYSTEM_PROPERTY);
        PreevalStatistics.reset();
    }

    @Test
    public void testInstrumentLeavesHooksUnwrappedWhenDisabled()
    {
        Assertions.assertSame(FIXED, PreevalStatistics.instrument(FIXED));
    }

    @Test
    public void testInstrumentedHooksDelegateAndRecordEachCall()
    {
        System.setProperty(PreevalStatistics.SYSTEM_PROPERTY, "true");
        PrevalHooks hooks = PreevalStatistics.instrument(FIXED);
        Assertions.assertNotSame(FIXED, hooks);

        Assertions.assertTrue(hooks.stopPreeval(Lists.immutable.empty()));
        Assertions.assertTrue(hooks.stopPreeval(Lists.immutable.empty()));
        Assertions.assertFalse(hooks.shouldInline("f"));
        Assertions.assertTrue(hooks.isGeneratedMilestoningProperty("f"));
        Assertions.assertFalse(hooks.isGetAllFunction("f"));
        Assertions.assertEquals(Lists.immutable.with("schema"), hooks.resolveTdsSchema(null, null));

        Assertions.assertEquals(Long.valueOf(2), PreevalStatistics.hookCallCounts().get("stopPreeval"));
        Assertions.assertEquals(Long.valueOf(1), PreevalStatistics.hookCallCounts().get("shouldInline"));
        Assertions.assertEquals(Long.valueOf(1), PreevalStatistics.hookCallCounts().get("isGeneratedMilestoningProperty"));
        Assertions.assertEquals(Long.valueOf(1), PreevalStatistics.hookCallCounts().get("isGetAllFunction"));
        Assertions.assertEquals(Long.valueOf(1), PreevalStatistics.hookCallCounts().get("resolveTdsSchema"));
        Assertions.assertEquals(PreevalStatistics.hookCallCounts().keySet(), PreevalStatistics.hookNanos().keySet());
        Assertions.assertTrue(PreevalStatistics.hookNanos().values().stream().allMatch(n -> n >= 0));
    }

    @Test
    public void testRuleApplicationsAreIgnoredWhenDisabled()
    {
        PreevalStatistics.ruleApplied(Rules.REACTIVATE);
        Assertions.assertTrue(PreevalStatistics.ruleCounts().isEmpty());
    }

    @Test
    public void testRuleApplicationsAreCountedBySimpleClassName()
    {
        System.setProperty(PreevalStatistics.SYSTEM_PROPERTY, "true");
        PreevalStatistics.ruleApplied(Rules.REACTIVATE);
        PreevalStatistics.ruleApplied(Rules.REACTIVATE);
        PreevalStatistics.ruleApplied(Rules.EXPANSION.getFirst());
        Assertions.assertEquals(Long.valueOf(2), PreevalStatistics.ruleCounts().get("ReactivateRule"));
        Assertions.assertEquals(Long.valueOf(1), PreevalStatistics.ruleCounts().get("InlineRule"));
    }

    @Test
    public void testResetClearsEveryCounter()
    {
        System.setProperty(PreevalStatistics.SYSTEM_PROPERTY, "true");
        PreevalStatistics.ruleApplied(Rules.REACTIVATE);
        PreevalStatistics.instrument(FIXED).stopPreeval(Lists.immutable.empty());
        PreevalStatistics.reset();
        Assertions.assertTrue(PreevalStatistics.ruleCounts().isEmpty());
        Assertions.assertTrue(PreevalStatistics.hookCallCounts().isEmpty());
        Assertions.assertTrue(PreevalStatistics.hookNanos().isEmpty());
    }

    @Test
    public void testCountersAreThreadSafe() throws Exception
    {
        System.setProperty(PreevalStatistics.SYSTEM_PROPERTY, "true");
        PrevalHooks hooks = PreevalStatistics.instrument(FIXED);
        Runnable work = () ->
        {
            for (int i = 0; i < 10_000; i++)
            {
                PreevalStatistics.ruleApplied(Rules.REACTIVATE);
                hooks.stopPreeval(Lists.immutable.empty());
            }
        };
        Thread first = new Thread(work);
        Thread second = new Thread(work);
        first.start();
        second.start();
        first.join();
        second.join();
        Assertions.assertEquals(Long.valueOf(20_000), PreevalStatistics.ruleCounts().get("ReactivateRule"));
        Assertions.assertEquals(Long.valueOf(20_000), PreevalStatistics.hookCallCounts().get("stopPreeval"));
    }
}
```

Add to `AbstractTestPrevalNative.java`, next to the other `@Test` methods (it already defines `HOOKS` and `executeTestFunction`; add the import `org.junit.Assert` if it is not there):

```java
    @Test
    public void testStatisticsCountRulesAndHooksWhenEnabled()
    {
        String previous = System.getProperty(PreevalStatistics.SYSTEM_PROPERTY);
        System.setProperty(PreevalStatistics.SYSTEM_PROPERTY, "true");
        PreevalStatistics.reset();
        try
        {
            executeTestFunction(
                    "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                    "prevalNative({|1 + 1}, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());");
            Assert.assertTrue(PreevalStatistics.ruleCounts().toString(), PreevalStatistics.ruleCounts().getOrDefault("ReactivateRule", 0L) >= 1);
            Assert.assertTrue(PreevalStatistics.hookCallCounts().toString(), PreevalStatistics.hookCallCounts().getOrDefault("stopPreeval", 0L) >= 1);
        }
        finally
        {
            if (previous == null)
            {
                System.clearProperty(PreevalStatistics.SYSTEM_PROPERTY);
            }
            else
            {
                System.setProperty(PreevalStatistics.SYSTEM_PROPERTY, previous);
            }
            PreevalStatistics.reset();
        }
    }

    @Test
    public void testStatisticsStayEmptyWhenDisabled()
    {
        System.clearProperty(PreevalStatistics.SYSTEM_PROPERTY);
        PreevalStatistics.reset();
        executeTestFunction(
                "let emptyVars = newMap([]->cast(@Pair<String, List<Any>>));",
                "prevalNative({|1 + 1}, $emptyVars, $emptyVars, " + HOOKS + ", noDebug());");
        Assert.assertTrue(PreevalStatistics.ruleCounts().isEmpty());
        Assert.assertTrue(PreevalStatistics.hookCallCounts().isEmpty());
    }
```

- [ ] **Step 2: RED**

Run FAMILY with `-Dmdep.analyze.skip=true`.
Expected: compilation failure in the shared module's tests: `cannot find symbol ... PreevalStatistics`.

- [ ] **Step 3: Implement**

`PreevalStatistics.java`:

```java
// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.pure.preeval;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

public final class PreevalStatistics
{
    public static final String SYSTEM_PROPERTY = "legend.engine.preeval.statistics";

    private static final ConcurrentMap<String, LongAdder> RULE_COUNTS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, LongAdder> HOOK_CALLS = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, LongAdder> HOOK_NANOS = new ConcurrentHashMap<>();

    private PreevalStatistics()
    {
    }

    public static boolean enabled()
    {
        return Boolean.getBoolean(SYSTEM_PROPERTY);
    }

    public static PrevalHooks instrument(PrevalHooks hooks)
    {
        return enabled() ? new CountingPrevalHooks(hooks) : hooks;
    }

    public static void ruleApplied(Object rule)
    {
        if (enabled())
        {
            add(RULE_COUNTS, rule.getClass().getSimpleName(), 1);
        }
    }

    static void hookCalled(String hook, long nanos)
    {
        add(HOOK_CALLS, hook, 1);
        add(HOOK_NANOS, hook, nanos);
    }

    public static Map<String, Long> ruleCounts()
    {
        return snapshot(RULE_COUNTS);
    }

    public static Map<String, Long> hookCallCounts()
    {
        return snapshot(HOOK_CALLS);
    }

    public static Map<String, Long> hookNanos()
    {
        return snapshot(HOOK_NANOS);
    }

    public static void reset()
    {
        RULE_COUNTS.clear();
        HOOK_CALLS.clear();
        HOOK_NANOS.clear();
    }

    private static void add(ConcurrentMap<String, LongAdder> counters, String key, long amount)
    {
        counters.computeIfAbsent(key, k -> new LongAdder()).add(amount);
    }

    private static Map<String, Long> snapshot(ConcurrentMap<String, LongAdder> counters)
    {
        Map<String, Long> result = new TreeMap<>();
        counters.forEach((key, value) -> result.put(key, value.sum()));
        return Collections.unmodifiableMap(result);
    }
}
```

`CountingPrevalHooks.java`:

```java
// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.pure.preeval;

import java.util.function.Supplier;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.ListIterable;
import org.eclipse.collections.api.map.ImmutableMap;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.valuespecification.ValueSpecification;

final class CountingPrevalHooks implements PrevalHooks
{
    private final PrevalHooks delegate;

    CountingPrevalHooks(PrevalHooks delegate)
    {
        this.delegate = delegate;
    }

    @Override
    public boolean stopPreeval(ListIterable<?> values)
    {
        return timed("stopPreeval", () -> this.delegate.stopPreeval(values));
    }

    @Override
    public boolean shouldInline(Object function)
    {
        return timed("shouldInline", () -> this.delegate.shouldInline(function));
    }

    @Override
    public boolean isGeneratedMilestoningProperty(Object function)
    {
        return timed("isGeneratedMilestoningProperty", () -> this.delegate.isGeneratedMilestoningProperty(function));
    }

    @Override
    public boolean isGetAllFunction(Object function)
    {
        return timed("isGetAllFunction", () -> this.delegate.isGetAllFunction(function));
    }

    @Override
    public ImmutableList<Object> resolveTdsSchema(ValueSpecification value, ImmutableMap<String, ImmutableList<Object>> rollingInScopeVars)
    {
        return timed("resolveTdsSchema", () -> this.delegate.resolveTdsSchema(value, rollingInScopeVars));
    }

    private static <T> T timed(String hook, Supplier<T> call)
    {
        long start = System.nanoTime();
        try
        {
            return call.get();
        }
        finally
        {
            PreevalStatistics.hookCalled(hook, System.nanoTime() - start);
        }
    }
}
```

`Preevaluator.prevalFunctionExpression`: call `PreevalStatistics.ruleApplied(<rule>)` immediately before each of the four `apply` calls. The rest of the method is unchanged:

```java
        PreParameterRule preParameterRule = Rules.PRE_PARAMETER.detect(r -> r.matches(expression, this));
        if (preParameterRule != null)
        {
            PreevalStatistics.ruleApplied(preParameterRule);
            PrevalResult handled = preParameterRule.apply(expression, state, this);
```
```java
        ExpressionRule expansion = Rules.EXPANSION.detect(r -> r.matches(prologue, this));
        if (expansion != null)
        {
            PreevalStatistics.ruleApplied(expansion);
            return expansion.apply(prologue, this);
        }
```
```java
            if (handlers.notEmpty())
            {
                PreevalStatistics.ruleApplied(handlers.getFirst());
                return handlers.getFirst().apply(reasoned, this);
            }
```
```java
        trace(state, () -> "Performing preval");
        PreevalStatistics.ruleApplied(Rules.REACTIVATE);
        return Rules.REACTIVATE.apply(reasoned, this);
```

`CompiledPreeval.java:49`:

```java
        PrevalResult result = new Preevaluator(runtime, PreevalStatistics.instrument(new CompiledPrevalHooks(hooks, executionSupport))).preval(item, state);
```

`PrevalNative.java:67` (interpreted): wrap the existing `new InterpretedPrevalHooks(...)` argument in `PreevalStatistics.instrument(...)`, leaving its arguments as they are:

```java
        PrevalResult result = new Preevaluator(runtime, PreevalStatistics.instrument(new InterpretedPrevalHooks(runtime,
                stopPreeval, getParentOrEmptyVariableContextForLambda(variableContext, stopPreeval),
                shouldInline, getParentOrEmptyVariableContextForLambda(variableContext, shouldInline),
                isGeneratedMilestoningProperty, getParentOrEmptyVariableContextForLambda(variableContext, isGeneratedMilestoningProperty),
                isGetAllFunction, getParentOrEmptyVariableContextForLambda(variableContext, isGetAllFunction),
                resolveTdsSchema, getParentOrEmptyVariableContextForLambda(variableContext, resolveTdsSchema)))).preval(item, state);
```

Add the import `org.finos.legend.engine.pure.preeval.PreevalStatistics` to both adapter files.

- [ ] **Step 4: GREEN**

Run FAMILY (analyzer on).
Expected: BUILD SUCCESS, with `TestPreevalStatistics` 6/6. `TestPrevalNativeCompiled` and `TestPrevalNativeInterpreted` each gain 2 tests, all green.

Then run CORE-BUILD, then COMPILED and INTERP.
Expected: 131 / 262 / 396, unchanged. With statistics off, nothing observable changes.

- [ ] **Step 5: Checkstyle and commit**

Run Checkstyle on the three preeval modules. Then:

```bash
P=legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval
SH=$P/legend-engine-pure-runtime-java-extension-shared-functions-preeval
git add $SH/src/main/java/org/finos/legend/engine/pure/preeval/PreevalStatistics.java \
        $SH/src/main/java/org/finos/legend/engine/pure/preeval/CountingPrevalHooks.java \
        $SH/src/main/java/org/finos/legend/engine/pure/preeval/Preevaluator.java \
        $SH/src/test/java/org/finos/legend/engine/pure/preeval/TestPreevalStatistics.java \
        $SH/src/test/java/org/finos/legend/engine/pure/preeval/AbstractTestPrevalNative.java \
        $P/legend-engine-pure-runtime-java-extension-compiled-functions-preeval/src/main/java/org/finos/legend/engine/pure/preeval/compiled/CompiledPreeval.java \
        $P/legend-engine-pure-runtime-java-extension-interpreted-functions-preeval/src/main/java/org/finos/legend/engine/pure/preeval/interpreted/natives/PrevalNative.java
git commit -m "Count preeval rule applications and hook calls on request"
```

---

### Task 2: Preeval comparison mode in the pipeline benchmark

**Files:**
- Modify: `PB/pom.xml`
- Create: `PB/src/main/java/org/finos/legend/engine/perf/SpanTimer.java`
- Create: `PB/src/main/java/org/finos/legend/engine/perf/PreevalComparison.java`
- Modify: `PB/src/main/java/org/finos/legend/engine/perf/PipelineBench.java` (`main`, and `runOnce` visibility)
- Modify: `PB/README.md`
- Test: `PB/src/test/java/org/finos/legend/engine/perf/TestSpanTimer.java`, `TestPreevalComparison.java` (JUnit 5)

**Interfaces:**
- Consumes (Task 1): `PreevalStatistics.SYSTEM_PROPERTY`, `reset()`, `ruleCounts()`, `hookCallCounts()`, `hookNanos()`. Also `PreevalImplementation.SYSTEM_PROPERTY`.
- Produces:
  - `SpanTimer implements io.opentracing.Tracer`, with `long totalNanos(String operation)`, `long count(String operation)` and `void reset()`.
  - `PreevalComparison.run(String[] args): int`, dispatched from `PipelineBench.main` on `--preeval`.
  - `static List<String> regressions(List<Map<String, Object>> workloads, double margin, long minDeltaMs)`.
  - `static void checkSpans(String workload, long pureSpans, long javaSpans)`.
  - Results JSON with this shape:
    ```json
    { "suite": "preeval", "generatedAt": "...", "environment": { ... },
      "workloads": [ { "workload": "simple@scale100/H2",
                       "PURE": { "planPureMs": 40, "prevalMs": 12.5, "prevalSpans": 3 },
                       "JAVA": { "planPureMs": 31, "prevalMs": 3.9, "prevalSpans": 3 },
                       "prevalSpeedup": 3.2,
                       "statistics": { "rules": {...}, "hookCalls": {...}, "hookMs": {...} } } ],
      "totals": { "prevalMsPure": 0.0, "prevalMsJava": 0.0, "prevalSpeedup": 0.0 },
      "regressions": [ ] }
    ```

- [ ] **Step 1: Dependencies**

In `PB/pom.xml`, add these to `<dependencies>`. The versions are managed by the root pom.

```xml
        <dependency>
            <groupId>org.finos.legend.engine</groupId>
            <artifactId>legend-engine-pure-runtime-java-extension-shared-functions-preeval</artifactId>
        </dependency>
        <dependency>
            <groupId>io.opentracing</groupId>
            <artifactId>opentracing-api</artifactId>
        </dependency>
        <dependency>
            <groupId>io.opentracing</groupId>
            <artifactId>opentracing-util</artifactId>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-api</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter-engine</artifactId>
            <scope>test</scope>
        </dependency>
```

If the root `dependencyManagement` lacks the shared preeval artifact, add it there next to `legend-engine-pure-runtime-java-extension-compiled-functions-preeval` with `<version>${project.version}</version>`, and report the change.

- [ ] **Step 2: Write the failing tests**

`TestSpanTimer.java`:

```java
// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.perf;

import io.opentracing.Scope;
import io.opentracing.Span;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestSpanTimer
{
    @Test
    public void testFinishedSpansAreCountedAndTimedByOperation() throws Exception
    {
        SpanTimer timer = new SpanTimer();
        Span span = timer.buildSpan("preval").start();
        Thread.sleep(5);
        span.finish();
        timer.buildSpan("other").start().finish();
        Assertions.assertEquals(1, timer.count("preval"));
        Assertions.assertTrue(timer.totalNanos("preval") >= 5_000_000L, String.valueOf(timer.totalNanos("preval")));
        Assertions.assertEquals(1, timer.count("other"));
        Assertions.assertEquals(0, timer.count("absent"));
        Assertions.assertEquals(0, timer.totalNanos("absent"));
    }

    @Test
    public void testNestedSameNameSpansCountOnce()
    {
        SpanTimer timer = new SpanTimer();
        Span outer = timer.buildSpan("preval").start();
        Span inner = timer.buildSpan("preval").start();
        inner.finish();
        outer.finish();
        Assertions.assertEquals(1, timer.count("preval"));
    }

    @Test
    public void testDepthIsPerThread() throws Exception
    {
        SpanTimer timer = new SpanTimer();
        Span outer = timer.buildSpan("preval").start();
        Thread other = new Thread(() -> timer.buildSpan("preval").start().finish());
        other.start();
        other.join();
        outer.finish();
        Assertions.assertEquals(2, timer.count("preval"));
    }

    @Test
    public void testActivatedSpanIsActiveUntilItsScopeCloses()
    {
        SpanTimer timer = new SpanTimer();
        Span span = timer.buildSpan("preval").start();
        try (Scope ignored = timer.activateSpan(span))
        {
            Assertions.assertSame(span, timer.activeSpan());
        }
        Assertions.assertNull(timer.activeSpan());
        span.finish();
    }

    @Test
    public void testResetClearsTotals()
    {
        SpanTimer timer = new SpanTimer();
        timer.buildSpan("preval").start().finish();
        timer.reset();
        Assertions.assertEquals(0, timer.count("preval"));
        Assertions.assertEquals(0, timer.totalNanos("preval"));
    }
}
```

`TestPreevalComparison.java`:

```java
// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.perf;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestPreevalComparison
{
    @Test
    public void testFasterJavaIsNotARegression()
    {
        Assertions.assertEquals(Collections.emptyList(), PreevalComparison.regressions(Collections.singletonList(workload("w", 100, 40.0, 80, 10.0)), 0.10, 10));
    }

    @Test
    public void testSlowerPlanPureBeyondBothThresholdsIsARegression()
    {
        List<String> found = PreevalComparison.regressions(Collections.singletonList(workload("w", 100, 10.0, 125, 10.0)), 0.10, 10);
        Assertions.assertEquals(1, found.size(), found.toString());
        Assertions.assertTrue(found.get(0).contains("w") && found.get(0).contains("planPure"), found.get(0));
    }

    @Test
    public void testSlowerPrevalBeyondBothThresholdsIsARegression()
    {
        List<String> found = PreevalComparison.regressions(Collections.singletonList(workload("w", 100, 20.0, 100, 35.0)), 0.10, 10);
        Assertions.assertEquals(1, found.size(), found.toString());
        Assertions.assertTrue(found.get(0).contains("preval"), found.get(0));
    }

    @Test
    public void testSmallAbsoluteDeltaIsNoise()
    {
        Assertions.assertEquals(Collections.emptyList(), PreevalComparison.regressions(Collections.singletonList(workload("w", 20, 2.0, 28, 9.0)), 0.10, 10));
    }

    @Test
    public void testSmallRelativeDeltaIsNoise()
    {
        Assertions.assertEquals(Collections.emptyList(), PreevalComparison.regressions(Collections.singletonList(workload("w", 1000, 10.0, 1050, 10.0)), 0.10, 10));
    }

    @Test
    public void testEveryRegressedWorkloadIsReported()
    {
        List<String> found = PreevalComparison.regressions(Arrays.asList(workload("a", 100, 10.0, 200, 10.0), workload("b", 100, 10.0, 200, 10.0)), 0.10, 10);
        Assertions.assertEquals(2, found.size(), found.toString());
    }

    @Test
    public void testCheckSpansRejectsZeroSpans()
    {
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, () -> PreevalComparison.checkSpans("w", 0, 0));
        Assertions.assertTrue(e.getMessage().contains("no preval spans"), e.getMessage());
    }

    @Test
    public void testCheckSpansRejectsUnequalCounts()
    {
        IllegalStateException e = Assertions.assertThrows(IllegalStateException.class, () -> PreevalComparison.checkSpans("w", 3, 4));
        Assertions.assertTrue(e.getMessage().contains("PURE 3") && e.getMessage().contains("JAVA 4"), e.getMessage());
    }

    @Test
    public void testCheckSpansAcceptsEqualPositiveCounts()
    {
        PreevalComparison.checkSpans("w", 3, 3);
    }

    private static Map<String, Object> workload(String id, long purePlan, double purePreval, long javaPlan, double javaPreval)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workload", id);
        result.put("PURE", side(purePlan, purePreval));
        result.put("JAVA", side(javaPlan, javaPreval));
        return result;
    }

    private static Map<String, Object> side(long planPureMs, double prevalMs)
    {
        Map<String, Object> side = new LinkedHashMap<>();
        side.put("planPureMs", planPureMs);
        side.put("prevalMs", prevalMs);
        side.put("prevalSpans", 1L);
        return side;
    }
}
```

- [ ] **Step 3: RED**

Run PB-BUILD with `-Dmdep.analyze.skip=true`.
Expected: test compilation fails with `cannot find symbol ... SpanTimer` / `PreevalComparison`.

- [ ] **Step 4: Implement**

`SpanTimer.java`:

```java
// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.perf;

import io.opentracing.Scope;
import io.opentracing.ScopeManager;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tag;
import io.opentracing.util.ThreadLocalScopeManager;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.LongAdder;

public final class SpanTimer implements Tracer
{
    private static final SpanContext NO_CONTEXT = new SpanContext()
    {
        @Override
        public String toTraceId()
        {
            return "";
        }

        @Override
        public String toSpanId()
        {
            return "";
        }

        @Override
        public Iterable<Map.Entry<String, String>> baggageItems()
        {
            return Collections.emptyList();
        }
    };

    private final ScopeManager scopeManager = new ThreadLocalScopeManager();
    private final ConcurrentMap<String, LongAdder> nanos = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, LongAdder> counts = new ConcurrentHashMap<>();
    private final ThreadLocal<Map<String, Integer>> depths = ThreadLocal.withInitial(HashMap::new);

    public long totalNanos(String operation)
    {
        LongAdder total = this.nanos.get(operation);
        return total == null ? 0 : total.sum();
    }

    public long count(String operation)
    {
        LongAdder total = this.counts.get(operation);
        return total == null ? 0 : total.sum();
    }

    public void reset()
    {
        this.nanos.clear();
        this.counts.clear();
    }

    @Override
    public ScopeManager scopeManager()
    {
        return this.scopeManager;
    }

    @Override
    public Span activeSpan()
    {
        return this.scopeManager.activeSpan();
    }

    @Override
    public Scope activateSpan(Span span)
    {
        return this.scopeManager.activate(span);
    }

    @Override
    public SpanBuilder buildSpan(String operationName)
    {
        return new Builder(operationName);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C carrier)
    {
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C carrier)
    {
        return null;
    }

    @Override
    public void close()
    {
    }

    private final class Builder implements SpanBuilder
    {
        private final String operation;

        private Builder(String operation)
        {
            this.operation = operation;
        }

        @Override
        public SpanBuilder asChildOf(SpanContext parent)
        {
            return this;
        }

        @Override
        public SpanBuilder asChildOf(Span parent)
        {
            return this;
        }

        @Override
        public SpanBuilder addReference(String referenceType, SpanContext referencedContext)
        {
            return this;
        }

        @Override
        public SpanBuilder ignoreActiveSpan()
        {
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, String value)
        {
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, boolean value)
        {
            return this;
        }

        @Override
        public SpanBuilder withTag(String key, Number value)
        {
            return this;
        }

        @Override
        public <T> SpanBuilder withTag(Tag<T> tag, T value)
        {
            return this;
        }

        @Override
        public SpanBuilder withStartTimestamp(long microseconds)
        {
            return this;
        }

        @Override
        @Deprecated
        public Span startManual()
        {
            return start();
        }

        @Override
        public Span start()
        {
            return new TimedSpan(this.operation);
        }

        @Override
        @Deprecated
        public Scope startActive(boolean finishSpanOnClose)
        {
            return SpanTimer.this.scopeManager.activate(start(), finishSpanOnClose);
        }
    }

    private final class TimedSpan implements Span
    {
        private final String operation;
        private final boolean outermost;
        private final long startNanos;
        private boolean finished;

        private TimedSpan(String operation)
        {
            this.operation = operation;
            this.outermost = SpanTimer.this.depths.get().merge(operation, 1, Integer::sum) == 1;
            this.startNanos = System.nanoTime();
        }

        @Override
        public SpanContext context()
        {
            return NO_CONTEXT;
        }

        @Override
        public Span setTag(String key, String value)
        {
            return this;
        }

        @Override
        public Span setTag(String key, boolean value)
        {
            return this;
        }

        @Override
        public Span setTag(String key, Number value)
        {
            return this;
        }

        @Override
        public <T> Span setTag(Tag<T> tag, T value)
        {
            return this;
        }

        @Override
        public Span log(Map<String, ?> fields)
        {
            return this;
        }

        @Override
        public Span log(long timestampMicroseconds, Map<String, ?> fields)
        {
            return this;
        }

        @Override
        public Span log(String event)
        {
            return this;
        }

        @Override
        public Span log(long timestampMicroseconds, String event)
        {
            return this;
        }

        @Override
        public Span setBaggageItem(String key, String value)
        {
            return this;
        }

        @Override
        public String getBaggageItem(String key)
        {
            return null;
        }

        @Override
        public Span setOperationName(String operationName)
        {
            return this;
        }

        @Override
        public void finish()
        {
            if (this.finished)
            {
                return;
            }
            this.finished = true;
            long elapsed = System.nanoTime() - this.startNanos;
            SpanTimer.this.depths.get().merge(this.operation, -1, (depth, change) -> depth + change == 0 ? null : depth + change);
            if (this.outermost)
            {
                SpanTimer.this.nanos.computeIfAbsent(this.operation, k -> new LongAdder()).add(elapsed);
                SpanTimer.this.counts.computeIfAbsent(this.operation, k -> new LongAdder()).increment();
            }
        }

        @Override
        public void finish(long finishMicros)
        {
            finish();
        }
    }
}
```

`PreevalComparison.java`:

```java
// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.perf;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentracing.util.GlobalTracer;
import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.finos.legend.engine.pure.preeval.PreevalImplementation;
import org.finos.legend.engine.pure.preeval.PreevalStatistics;

public final class PreevalComparison
{
    private static final String SPAN = "preval";
    private static final List<String> IMPLEMENTATIONS = Arrays.asList("PURE", "JAVA");

    private PreevalComparison()
    {
    }

    public static int run(String[] args) throws Exception
    {
        Map<String, String> options = options(args);
        int iterations = Integer.parseInt(options.getOrDefault("iters", "10"));
        int warmup = Integer.parseInt(options.getOrDefault("warmup", "3"));
        double margin = Double.parseDouble(options.getOrDefault("margin", "0.10"));
        long minDelta = Long.parseLong(options.getOrDefault("min-delta-ms", "10"));
        String out = options.get("out");

        SpanTimer timer = new SpanTimer();
        if (!GlobalTracer.registerIfAbsent(timer))
        {
            throw new IllegalStateException("A tracer is already registered, so preval spans cannot be timed");
        }

        List<Map<String, Object>> results = new ArrayList<>();
        try
        {
            for (BenchConfig config : workloads(iterations, warmup))
            {
                System.out.println("  comparing " + config.workloadId());
                results.add(compare(new PipelineBench(config).quiet(), config, timer));
            }
        }
        finally
        {
            System.clearProperty(PreevalImplementation.SYSTEM_PROPERTY);
            System.clearProperty(PreevalStatistics.SYSTEM_PROPERTY);
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("suite", "preeval");
        report.put("generatedAt", Instant.now().toString());
        report.put("environment", Environment.capture());
        report.put("workloads", results);
        report.put("totals", totals(results));
        List<String> regressions = regressions(results, margin, minDelta);
        report.put("regressions", regressions);
        print(results, PipelineBench.asMap(report.get("totals")), regressions);
        if (out != null)
        {
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(new File(out), report);
            System.out.println("results written to " + out);
        }
        return regressions.isEmpty() ? 0 : 1;
    }

    static List<BenchConfig> workloads(int iterations, int warmup)
    {
        List<BenchConfig> configs = new ArrayList<>();
        configs.add(workload("simple", 100));
        configs.add(workload("simple", 1000));
        configs.add(workload("join8", 30));
        configs.add(workload("agg4", 30));
        configs.add(workload("groupBy", 30));
        BenchConfig relation = workload("simple", 50);
        relation.relationFunction = true;
        configs.add(relation);
        configs.add(workload("variantrel", 30));
        BenchConfig milestoned = workload("simple", 30);
        milestoned.milestoned = true;
        configs.add(milestoned);
        configs.add(workload("graph4", 30));
        BenchConfig view = workload("m2mview", 30);
        view.modelToModel = true;
        configs.add(view);
        for (BenchConfig config : configs)
        {
            config.iterations = iterations;
            config.warmup = warmup;
        }
        return configs;
    }

    static List<String> regressions(List<Map<String, Object>> workloads, double margin, long minDeltaMs)
    {
        List<String> found = new ArrayList<>();
        for (Map<String, Object> workload : workloads)
        {
            Map<String, Object> pure = PipelineBench.asMap(workload.get("PURE"));
            Map<String, Object> java = PipelineBench.asMap(workload.get("JAVA"));
            for (String measure : Arrays.asList("planPureMs", "prevalMs"))
            {
                double before = ((Number) pure.get(measure)).doubleValue();
                double after = ((Number) java.get(measure)).doubleValue();
                if (after - before > minDeltaMs && after > before * (1 + margin))
                {
                    found.add(workload.get("workload") + ": " + measure.replace("Ms", "") + " " + before + " ms under PURE, " + after + " ms under JAVA");
                }
            }
        }
        return found;
    }

    static void checkSpans(String workload, long pureSpans, long javaSpans)
    {
        if (pureSpans == 0 && javaSpans == 0)
        {
            throw new IllegalStateException(workload + ": no preval spans were recorded, so there is nothing to compare");
        }
        if (pureSpans != javaSpans)
        {
            throw new IllegalStateException(workload + ": preval ran a different number of times (PURE " + pureSpans + ", JAVA " + javaSpans + ")");
        }
    }

    private static Map<String, Object> compare(PipelineBench bench, BenchConfig config, SpanTimer timer)
    {
        Map<String, List<Long>> planPure = new LinkedHashMap<>();
        Map<String, List<Long>> prevalNanos = new LinkedHashMap<>();
        Map<String, Long> spans = new LinkedHashMap<>();
        for (String implementation : IMPLEMENTATIONS)
        {
            planPure.put(implementation, new ArrayList<>());
            prevalNanos.put(implementation, new ArrayList<>());
        }
        for (int i = 0; i < config.warmup + config.iterations; i++)
        {
            List<String> order = i % 2 == 0 ? IMPLEMENTATIONS : Arrays.asList("JAVA", "PURE");
            for (String implementation : order)
            {
                System.setProperty(PreevalImplementation.SYSTEM_PROPERTY, implementation);
                timer.reset();
                Map<String, Object> row = bench.runOnce();
                if (i >= config.warmup)
                {
                    planPure.get(implementation).add((Long) row.get("planPure"));
                    prevalNanos.get(implementation).add(timer.totalNanos(SPAN));
                    spans.put(implementation, timer.count(SPAN));
                }
            }
        }
        checkSpans(config.workloadId(), spans.get("PURE"), spans.get("JAVA"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("workload", config.workloadId());
        for (String implementation : IMPLEMENTATIONS)
        {
            Map<String, Object> side = new LinkedHashMap<>();
            side.put("planPureMs", median(planPure.get(implementation)));
            side.put("prevalMs", round(median(prevalNanos.get(implementation)) / 1_000_000.0));
            side.put("prevalSpans", spans.get(implementation));
            result.put(implementation, side);
        }
        double pureMs = ((Number) PipelineBench.asMap(result.get("PURE")).get("prevalMs")).doubleValue();
        double javaMs = ((Number) PipelineBench.asMap(result.get("JAVA")).get("prevalMs")).doubleValue();
        result.put("prevalSpeedup", javaMs == 0 ? null : round(pureMs / javaMs));
        result.put("statistics", statistics(bench));
        return result;
    }

    private static Map<String, Object> statistics(PipelineBench bench)
    {
        System.setProperty(PreevalImplementation.SYSTEM_PROPERTY, "JAVA");
        System.setProperty(PreevalStatistics.SYSTEM_PROPERTY, "true");
        PreevalStatistics.reset();
        try
        {
            bench.runOnce();
            Map<String, Object> statistics = new LinkedHashMap<>();
            statistics.put("rules", PreevalStatistics.ruleCounts());
            statistics.put("hookCalls", PreevalStatistics.hookCallCounts());
            Map<String, Object> hookMs = new LinkedHashMap<>();
            PreevalStatistics.hookNanos().forEach((hook, nanos) -> hookMs.put(hook, round(nanos / 1_000_000.0)));
            statistics.put("hookMs", hookMs);
            return statistics;
        }
        finally
        {
            System.clearProperty(PreevalStatistics.SYSTEM_PROPERTY);
            PreevalStatistics.reset();
        }
    }

    private static Map<String, Object> totals(List<Map<String, Object>> results)
    {
        double pure = 0;
        double java = 0;
        for (Map<String, Object> result : results)
        {
            pure += ((Number) PipelineBench.asMap(result.get("PURE")).get("prevalMs")).doubleValue();
            java += ((Number) PipelineBench.asMap(result.get("JAVA")).get("prevalMs")).doubleValue();
        }
        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("prevalMsPure", round(pure));
        totals.put("prevalMsJava", round(java));
        totals.put("prevalSpeedup", java == 0 ? null : round(pure / java));
        return totals;
    }

    private static void print(List<Map<String, Object>> results, Map<String, Object> totals, List<String> regressions)
    {
        System.out.println(String.format("%-36s %10s %10s %10s %10s %8s", "workload", "plan PURE", "plan JAVA", "prev PURE", "prev JAVA", "speedup"));
        for (Map<String, Object> result : results)
        {
            Map<String, Object> pure = PipelineBench.asMap(result.get("PURE"));
            Map<String, Object> java = PipelineBench.asMap(result.get("JAVA"));
            System.out.println(String.format("%-36s %10s %10s %10s %10s %8s", result.get("workload"), pure.get("planPureMs"), java.get("planPureMs"), pure.get("prevalMs"), java.get("prevalMs"), result.get("prevalSpeedup")));
        }
        System.out.println("total preval ms: PURE " + totals.get("prevalMsPure") + ", JAVA " + totals.get("prevalMsJava") + ", speedup " + totals.get("prevalSpeedup"));
        if (regressions.isEmpty())
        {
            System.out.println("no regressions");
        }
        else
        {
            System.out.println("REGRESSIONS:");
            regressions.forEach(r -> System.out.println("  " + r));
        }
    }

    private static BenchConfig workload(String query, int scale)
    {
        BenchConfig config = new BenchConfig();
        config.query = query;
        config.scale = scale;
        return config;
    }

    private static long median(List<Long> values)
    {
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compare);
        return sorted.get(sorted.size() / 2);
    }

    private static double round(double value)
    {
        return Math.round(value * 100.0) / 100.0;
    }

    private static Map<String, String> options(String[] args)
    {
        Map<String, String> options = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++)
        {
            if (args[i].startsWith("--"))
            {
                String key = args[i].substring(2);
                if (i + 1 < args.length && !args[i + 1].startsWith("--"))
                {
                    options.put(key, args[++i]);
                }
                else
                {
                    options.put(key, "true");
                }
            }
        }
        return options;
    }
}
```

`PipelineBench.java`:
- Change `private Map<String, Object> runOnce()` to package-private `Map<String, Object> runOnce()`.
- In `main`, dispatch `--preeval` before the existing `--suite` check:

```java
        for (String arg : args)
        {
            if (arg.equals("--preeval"))
            {
                System.exit(PreevalComparison.run(args));
            }
            if (arg.equals("--suite"))
            {
                System.exit(PerfSuite.run(args));
            }
        }
```

`README.md`: add a section `## Comparing preeval implementations` after `## Suite runs, baselines and regression checks`. It should cover:
- the command, `PipelineBench --preeval [--iters 10] [--warmup 3] [--out results.json] [--margin 0.10] [--min-delta-ms 10]`;
- that it alternates `PURE` and `JAVA` in one JVM and times `traceSpan('preval')` with its own tracer, so absolute `planPure` numbers include tracer overhead and are not comparable with `--suite default`;
- the regression rule (both thresholds);
- that the statistics pass runs once per workload under `JAVA` with `legend.engine.preeval.statistics=true`;
- that it exits non-zero on a regression.

- [ ] **Step 5: GREEN**

Run PB-BUILD (analyzer on).
Expected: BUILD SUCCESS, with `TestSpanTimer` 5/5 and `TestPreevalComparison` 9/9.

- [ ] **Step 6: Smoke run**

Run PB-RUN `--preeval --iters 1 --warmup 1 --out $W/smoke.json`. That is controller-sized: tell the controller if it exceeds 10 minutes.
Expected:
- every workload prints a row;
- no `IllegalStateException`;
- each workload's `prevalSpans` is above 0 and equal for PURE and JAVA;
- `statistics.rules` is not empty.

If a workload's generated model fails to compile or plan under both implementations, it is a generator limitation, not a preeval finding. Drop that workload from `PreevalComparison.workloads`, note it in the report, and rerun.

- [ ] **Step 7: Checkstyle and commit**

Run Checkstyle on PB. Then:

```bash
git add legend-engine-config/legend-engine-perf-benchmark/pom.xml legend-engine-config/legend-engine-perf-benchmark/README.md legend-engine-config/legend-engine-perf-benchmark/src/main/java/org/finos/legend/engine/perf/SpanTimer.java legend-engine-config/legend-engine-perf-benchmark/src/main/java/org/finos/legend/engine/perf/PreevalComparison.java legend-engine-config/legend-engine-perf-benchmark/src/main/java/org/finos/legend/engine/perf/PipelineBench.java legend-engine-config/legend-engine-perf-benchmark/src/test/java/org/finos/legend/engine/perf/TestSpanTimer.java legend-engine-config/legend-engine-perf-benchmark/src/test/java/org/finos/legend/engine/perf/TestPreevalComparison.java
git commit -m "Compare preeval implementations in the pipeline benchmark"
```

(Also stage the root `pom.xml` if Step 1 changed it.)

---

### Task 3: Baseline run and gate (controller)

**Executed by the controller.** The run is long and its result decides whether P5 continues.

**Files:** Create `docs/superpowers/plans/2026-09-30-preeval-p5-results.md`. Modify the spec §4.5 (target gain).

- [ ] **Step 1: Quiet machine, full run**

Close other Maven builds. Run PB-RUN `--preeval --iters 10 --warmup 3 --out $W/preeval-baseline.json > $W/preeval-baseline.log 2>&1` in the background, and wait for the completion notification.
Expected: exit 0 (no regressions), or exit 1 with a `REGRESSIONS:` list.

- [ ] **Step 2: Repeat once**

Run the same command a second time with `--out $W/preeval-baseline-2.json`. A single run can be skewed by the machine, so the gate uses both runs:
- a workload counts as regressed only if both runs flag it;
- the recorded medians are those of the second run.

- [ ] **Step 3: Gate**

- **No workload regressed in both runs:** continue.
- **Otherwise: STOP.** Record the table and the statistics, and ask the user. Read `statistics.hookMs` against `JAVA.prevalMs` first. If `stopPreeval` time dominates, that is the §8 risk ("callbacks into Pure per node erase the gain"), and its mitigation — moving the hard-coded stop list into Java — is a design change that needs its own plan. Do not start Task 6.

- [ ] **Step 4: Results doc**

Create `docs/superpowers/plans/2026-09-30-preeval-p5-results.md` with:
- **Benchmark (Task 3, <date>, HEAD <sha>, <cpu>, JDK <version>, heap).** One table row per workload: planPure PURE / JAVA, preval PURE / JAVA (ms), speedup, spans. Then the totals line from both runs.
- **Hook cost.** For each workload, from `statistics`: `stopPreeval` calls and ms, with the other hooks' calls and ms, and the hook share of `JAVA.prevalMs`.
- **Rule fire counts.** A table of workload by rule, from `statistics.rules`.
- **Gate:** passed or stopped, with the reason.

- [ ] **Step 5: Target**

In the spec §4.5, replace "The target gain is set after the first baseline run." with: "Target gain, set from the P5 baseline (<date>): total preval time <PURE> ms → <JAVA> ms (×<speedup>) across the ten comparison workloads, with no workload regressing; see `docs/superpowers/plans/2026-09-30-preeval-p5-results.md`." Commit both files with the message `Record the preeval benchmark baseline`.

---

### Task 4: CI input to run the test estate under a chosen preeval implementation

**Files:** Modify `.github/workflows/build.yml`.

**Interfaces:**
- Produces: a `workflow_dispatch` input `preeval_implementation` (string, default empty). Task 5 consumes it.

- [ ] **Step 1: Write the check that fails**

Save this as `$W/check-build-yml.py`. It is scratch and is not committed.

```python
import sys
import yaml

workflow = yaml.safe_load(open('.github/workflows/build.yml'))
triggers = workflow[True] if True in workflow else workflow['on']
inputs = triggers['workflow_dispatch']['inputs']
assert 'preeval_implementation' in inputs, 'missing input'
assert inputs['preeval_implementation']['default'] == '', 'default must be empty'
expected = "-DargLine=\"-Xmx6g${{ inputs.preeval_implementation != '' && format(' -Dlegend.engine.preeval.implementation={0}', inputs.preeval_implementation) || '' }}\""
for job in ('test', 'test-modules'):
    steps = [s for s in workflow['jobs'][job]['steps'] if s.get('name') == 'Test']
    assert len(steps) == 1, job
    assert expected in steps[0]['run'], job + ' does not pass the preeval implementation'
print('ok')
```

Run `python3 $W/check-build-yml.py`.
Expected: `AssertionError: missing input`. If PyYAML is missing, install it with `pip install --user pyyaml`.

- [ ] **Step 2: Implement**

Under `on.workflow_dispatch.inputs`, after `pure_repo`, add:

```yaml
      preeval_implementation:
        description: "Preeval implementation for test JVMs (PURE, JAVA or SHADOW). Leave empty for the default."
        required: false
        default: ""
        type: string
```

In the `test` and `test-modules` jobs' `Test` steps, replace `-DargLine="-Xmx6g"` with:

```
-DargLine="-Xmx6g${{ inputs.preeval_implementation != '' && format(' -Dlegend.engine.preeval.implementation={0}', inputs.preeval_implementation) || '' }}"
```

Leave the benchmark job alone.

- [ ] **Step 3: GREEN**

Run `python3 $W/check-build-yml.py`.
Expected: `ok`.
Run `git diff .github/workflows/build.yml`.
Expected: exactly the one input block and the two `argLine` changes.

- [ ] **Step 4: Commit**

```bash
git add .github/workflows/build.yml
git commit -m "Let a manual CI run choose the preeval implementation"
```

---

### Task 5: Full default-profile estate under `SHADOW` (controller and user)

**Executed by the controller. Every push or dispatch needs the user's go-ahead** (Global Constraints).

**Files:** Extend `docs/superpowers/plans/2026-09-30-preeval-p5-results.md`.

- [ ] **Step 1: Ask, then push two branches**

Ask the user which fork remote to use. Then push HEAD twice:

```bash
git push <fork> HEAD:refs/heads/preeval-p5-shadow
git push <fork> HEAD:refs/heads/preeval-p5-control
```

- [ ] **Step 2: Dispatch both runs**

```bash
gh workflow run build.yml --repo <fork-owner>/legend-engine --ref preeval-p5-shadow -f preeval_implementation=SHADOW
gh workflow run build.yml --repo <fork-owner>/legend-engine --ref preeval-p5-control -f preeval_implementation=PURE
```

Wait for both with `gh run watch`, using a ScheduleWakeup or Monitor rather than polling. Record the run ids. Report failures of the `build` job as blockers.

- [ ] **Step 3: Download the reports**

```bash
gh run download <shadow-run-id> --repo <fork-owner>/legend-engine -D $W/ci/shadow
gh run download <control-run-id> --repo <fork-owner>/legend-engine -D $W/ci/control
```

Each artifact `test-results` or `test-results-<group>` holds the Surefire XML reports for one CI job.

- [ ] **Step 4: Diff each artifact**

Save this as `$W/diff.py`. It compares test by test, aligned by position within each report file, so repeated test names cannot hide a failure.

```python
import glob, os, sys
import xml.etree.ElementTree as ET

def load(directory):
    out = {}
    for path in sorted(glob.glob(directory + '/TEST-*.xml')):
        cases = []
        for tc in ET.parse(path).getroot().iter('testcase'):
            key = (tc.get('classname') or '') + '#' + (tc.get('name') or '')
            bad = [c for c in tc if c.tag in ('failure', 'error')]
            msg = ((bad[0].get('message') or '') + ' ' + (bad[0].text or '')).replace('\n', ' ')[:400] if bad else None
            cases.append((key, msg))
        out[os.path.basename(path)] = cases
    return out

c, s = load(sys.argv[1]), load(sys.argv[2])
n = lambda d: sum(len(v) for v in d.values())
f = lambda d: sum(1 for v in d.values() for _, m in v if m)
print('ran: control', n(c), 'shadow', n(s), '| failing: control', f(c), 'shadow', f(s))
for fn in sorted(set(c) ^ set(s)):
    print('RAN-IN-ONE-ONLY file', fn)
for fn in sorted(set(c) & set(s)):
    a, b = c[fn], s[fn]
    if len(a) != len(b):
        print('MISALIGNED', fn, len(a), len(b))
    for i, ((ka, ma), (kb, mb)) in enumerate(zip(a, b)):
        if ka != kb:
            print('MISALIGNED', fn, i, ka, kb); continue
        if mb and not ma:
            print('SHADOW-ONLY', ka, '|', mb)
        elif ma and not mb:
            print('CONTROL-ONLY', ka, '|', ma)
        elif ma and mb and ma != mb:
            print('DIFFERENT-FAILURE', ka, '| control:', ma[:180], '| shadow:', mb[:180])
```

For each artifact name A: `python3 $W/diff.py $W/ci/control/A $W/ci/shadow/A > $W/ci/A-diff.txt`.
- Tests that run in parallel (`-T2`), or that depend on the date or network, can flake. Any `CONTROL-ONLY`, `DIFFERENT-FAILURE`, `MISALIGNED` or `RAN-IN-ONE-ONLY` line must be explained. Rerun the test locally in both modes if unsure.
- A `SHADOW-ONLY` line with a message-only failure (PCT) is located with a JFR `jdk.JavaExceptionThrow` recording. P4 Task 6.3 has the recipe.

- [ ] **Step 5: Record**

Append `## Full estate under SHADOW (Task 5, <date>, HEAD <sha>)` to the results doc, with:
- a table per CI job: tests ran and failing in control and in shadow, and SHADOW-only;
- the run ids;
- `### Findings`, in the P4 manifest format (#, root cause, category, smallest failing test, other affected, status);
- `### Pre-existing and flaky`: tests failing in both runs, and flakes with their rerun evidence.

Commit the doc with the message `Record the preeval shadow run over the full CI estate`.

- [ ] **Step 6: Findings loop**

- **No SHADOW-only lines:** continue to Task 6.
- **Otherwise:** amend this plan with one sub-task per root cause, **5.1, 5.2, …**, each shaped like P4 Task 6: reproduce locally with a test, RED, fix (JAVA-BUG → Java; PURE-QUIRK → Java with `// parity:`; COMPARATOR → `describePrevalResult` plus a pinning test), GREEN, commit.
  - Commit the amendment as `Plan the P5 estate fixes`.
  - After the fixes, rerun Steps 1–5 on new branch names (`-2` suffix). The exit is a clean rerun.

---

### Task 6: Cutover — `JAVA` becomes the default

**Precondition:** the Task 3 gate passed and Task 5 is clean. The spec §6 criteria are then met: zero `SHADOW` differences on default-profile CI, the interpreted runners green (checked again in Step 5), and no benchmark regression.

**Files:**
- Modify: `SH/src/main/java/org/finos/legend/engine/pure/preeval/PreevalImplementation.java` (`parse`)
- Modify: `SH/src/test/java/org/finos/legend/engine/pure/preeval/TestPreevalImplementation.java`
- Modify: `CC/src/test/java/org/finos/legend/engine/pure/code/core/Test_Pure_Preeval_Java.java` (`suite`)
- Modify: `RC/src/test/java/org/finos/legend/pure/code/core/relational/Test_Pure_Relational_Preeval_Java.java` (`suite`)
- Modify: `docs/engineering/architecture/preeval.md`
- Extend: `docs/superpowers/plans/2026-09-30-preeval-p5-results.md`

**Interfaces:**
- Produces: `PreevalImplementation.parse(null)` and `parse(blank)` return `JAVA`. `-Dlegend.engine.preeval.implementation=PURE` still selects the Pure implementation.

- [ ] **Step 1: Write the failing tests**

In `TestPreevalImplementation.java`:
- rename `testUnsetDefaultsToPure` to `testUnsetDefaultsToJava`, and `testBlankDefaultsToPure` to `testBlankDefaultsToJava`, asserting `PreevalImplementation.JAVA`;
- in `testCurrentFollowsSystemProperty`, change the `withProperty(null, ...)` expectation to `JAVA`;
- add:

```java
    @Test
    public void testExplicitPureIsStillSelectable()
    {
        Assertions.assertEquals(PreevalImplementation.PURE, PreevalImplementation.parse("PURE"));
        withProperty("pure", () -> Assertions.assertEquals(PreevalImplementation.PURE, PreevalImplementation.current()));
    }
```

- [ ] **Step 2: RED**

Run FAMILY with `-Dmdep.analyze.skip=true`.
Expected: `TestPreevalImplementation` fails 3 tests (`expected: <JAVA> but was: <PURE>`), and `testExplicitPureIsStillSelectable` passes.

- [ ] **Step 3: Implement**

In `PreevalImplementation.parse`, change `return PURE;` in the null/blank branch to `return JAVA;`. Also change the initial cache, `private static volatile Resolved resolved = new Resolved(null, PURE);`, to `new Resolved(null, JAVA)`.

Keep the rollback path tested in compiled mode. In `Test_Pure_Preeval_Java.suite()` and `Test_Pure_Relational_Preeval_Java.suite()`, add a `PURE` wrapper before the existing `JAVA` and `SHADOW` ones. The compiled-core version:

```java
        suite.addTest(PreevalImplementationTests.withImplementation("PURE", targets(executionSupport)));
        suite.addTest(PreevalImplementationTests.withImplementation("JAVA", targets(executionSupport)));
        suite.addTest(PreevalImplementationTests.withImplementation("SHADOW", targets(executionSupport)));
```

In the relational runner, add the matching `PURE` line using the file's own `withImplementation` helper and `targets(...)` call, in the same position.

In `docs/engineering/architecture/preeval.md`, add a section `## Implementation switch` after the file's overview table:
- Preeval runs in Java (`legend-engine-pure-code-functions-preeval`) by default.
- `-Dlegend.engine.preeval.implementation=PURE` restores the Pure implementation (`prevalInternal`) for one release, as a rollback path.
- `SHADOW` runs both and fails on any difference; it is for diagnosis, not production.
- `-Dlegend.engine.preeval.statistics=true` records rule and hook counts, readable through `PreevalStatistics`.

- [ ] **Step 4: GREEN**

Run FAMILY (analyzer on).
Expected: BUILD SUCCESS; `TestPreevalImplementation` 7/7.

Then run CORE-BUILD, and CC-TEST `'Test_Pure_Preeval,Test_Pure_Preeval_Java,Test_Interpreted_Preeval,Test_Pure_Core'`.
Expected: 131 (now under JAVA by default), 393 (131 × PURE/JAVA/SHADOW), 396 and 1212, all green.

Then run RC-BUILD, and RC-TEST `'Test_Pure_Relational_Preeval_Java,Test_Interpreted_Relational_Preeval'`.
Expected: 3 and 3.

Run Checkstyle on SH, CC and RC.

- [ ] **Step 5: Estate under the new default (controller)**

Rebuild the five P4 estate modules (the P4 plan's Task 5 Step 1 build command). Then copy `.superpowers/sdd/2026-09-29-preeval-java-native-p4-shadow-estate/estate2/run.sh` and `diff.py` into `$W/estate/`, and in the copied `run.sh`:
- set `E` to `$W/estate`;
- rename the modes `pure shadow` to `default pure`;
- set `ARG` to `-Xmx6g` for `default` and `-Xmx6g -Dlegend.engine.preeval.implementation=PURE` for `pure`;
- diff `$E/$K/pure` against `$E/$K/default`.

Run it for `SQL LIN RC H2 DUCK` in turn, in the background. The `default` mode now runs the new `JAVA` default, and `pure` is the rollback path.
Expected: each suite's failures are exactly the six pre-existing NOT-SHADOW failures in the P4 manifest, in both modes.

Then run PB-RUN `--suite default --iters 5 --warmup 2 --out $W/default-java.json`. This machine has no baseline entry, so nothing is checked. Record the per-workload `planPure` warm medians in the results doc beside Task 3's numbers.

- [ ] **Step 6: Commit**

```bash
SH=legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-functions-preeval/legend-engine-pure-runtime-java-extension-shared-functions-preeval
git add $SH/src/main/java/org/finos/legend/engine/pure/preeval/PreevalImplementation.java \
        $SH/src/test/java/org/finos/legend/engine/pure/preeval/TestPreevalImplementation.java \
        legend-engine-core/legend-engine-core-pure/legend-engine-pure-code-compiled-core/src/test/java/org/finos/legend/engine/pure/code/core/Test_Pure_Preeval_Java.java \
        legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure/src/test/java/org/finos/legend/pure/code/core/relational/Test_Pure_Relational_Preeval_Java.java \
        docs/engineering/architecture/preeval.md docs/superpowers/plans/2026-09-30-preeval-p5-results.md
git commit -m "Make the Java preeval the default"
```

---

### Task 7: Exit verification and spec

**Files:** Modify `docs/superpowers/specs/2026-09-27-preeval-java-native-design.md`.

- [ ] **Step 1: Regression**

Run FAMILY. Then a full compiled-core `mvn clean install` with tests on and `-DargLine=-Xmx6g`, recording per-class counts (excluding the untracked `Test_Interpreted_GraphFetchUnion`). Then RC-TEST for both relational preeval runners, PB-BUILD, and Checkstyle on every module P5 touched.

- [ ] **Step 2: Spec**

- Mark the P5 row in the §5 phase table **done**: "`JAVA` is the default; preeval ×<speedup> across the comparison workloads; zero `SHADOW` differences on the full default-profile CI estate; see the P5 results doc".
- Add `### P5 status (<date>)` after P4's. Record:
  - **the benchmark:** method (alternating, one JVM, `SpanTimer`), workloads, the results table summary, the target gain, and that SQL-over-Legend is not benchmarked, with the reason;
  - **hook cost:** the measured `stopPreeval` share. This closes or confirms the §8 per-hook risk;
  - **the CI estate:** run ids, counts, findings and fixes;
  - **the cutover:** default `JAVA`, `PURE` rollback for one release, compiled runners now cover `PURE`/`JAVA`/`SHADOW`, statistics switch;
  - **the perf baseline:** CI runner entries need a rebase through the Performance Baseline workflow on the P5 PR (user action), and this machine has no entry;
  - **carried P4 gaps:** interpreted estate coverage answered by the interpreted runners (accepted), and the others unchanged;
  - **P6 inputs:** delete `prevalInternal`, the switch, the `SHADOW` comparator (`describePrevalResult` and helpers) and `assertSamePrevalResult`'s unused parameters. Keep or drop `PreevalStatistics`; recommend keeping it, since it costs one property read per rule application when off.
- In §6, add under the criteria: "Met on <date>; see P5 status."

- [ ] **Step 3: Commit**

Message: `Record P5 benchmark and cutover in the preeval spec`.

## P5 exit criteria (spec §5, §6)

- The benchmark shows no regression on any comparison workload, and the gain is recorded as the target.
- Zero `SHADOW`-only failures across the full default-profile CI estate, compared with a `PURE` control of the same commit, with every other difference explained.
- `JAVA` is the default. `PURE` remains selectable and tested in compiled and interpreted modes.
- The spec records the benchmark, the hook cost, the CI estate, the cutover and the P6 inputs.
