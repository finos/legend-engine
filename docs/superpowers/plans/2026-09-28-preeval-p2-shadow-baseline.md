# Preeval P2 shadow baseline

Taken 2026-09-28, at commit `c2745b7cf794220e86ade7bcdc0d67b299cb5004` (branch `preeval-native`), against `-Drevision=preeval-native-SNAPSHOT`.

Runner: `Test_Pure_Preeval_Java` now collects every test under `meta::pure::router::preeval::tests` that satisfies `PureTestBuilder.satisfiesConditionsModular`, running each under both `JAVA` and `SHADOW`, skipping names in `KNOWN_DIVERGENT` unless `-Dlegend.engine.preeval.test.includeKnownDivergent=true` is set.

Counts:
- Collected preeval tests (all of `meta::pure::router::preeval::tests`, including the `implementationSwitch` sub-package): **111**.
- Of those, 12 are `implementationSwitch` tests (`testPureImplementationSimplifies`, `testJavaImplementationIsReachable`, `testJavaImplementationRejectsStateWithDepth`, `testShadowAcceptsMatchingResults`, `testShadowRejectsDifferentFlags`, `testShadowRejectsDifferentValues`, `testShadowRejectsDifferentFunctionExpressionValues`, `testShadowAcceptsMatchingFunctionExpressionValues`, `testUnknownImplementationIsRejected`, `testConfiguredImplementationIsAKnownValue`, `testShadowRejectsDifferentTypes`, `testShadowRejectsDifferentMultiplicities`) — all pass under both `JAVA` and `SHADOW` at baseline.
- Passing under both `JAVA` and `SHADOW` at baseline (RUNNER-ALL, `KNOWN_DIVERGENT` temporarily empty): **62** (the 12 `implementationSwitch` tests + the 26 P1 target tests + 24 tests the coverage table expected to diverge but that already pass — see below).
- Failing under `JAVA` and/or `SHADOW` at baseline: **49** — all placed in `KNOWN_DIVERGENT`.
- After populating `KNOWN_DIVERGENT` with the 49 names, RUNNER (no `includeKnownDivergent`) is GREEN: `Test_Pure_Preeval_Java` 124/124 (62 collected tests × 2 implementations), `Test_Pure_Preeval` 111/111.

None of the 26 P1 target tests regressed. Every failing test's name is present in the plan's expected-rule-coverage table (`global-constraints.md`); there are no `unexpected` failures.

## Tests failing at baseline (now in `KNOWN_DIVERGENT`)

| Test | Fails under | First error line | Expected rule family |
|---|---|---|---|
| `tesColumnEvalOnRelation` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | TDS columns / eval-on-Column |
| `tesColumnEvalOnRelationWithCast` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | TDS columns / eval-on-Column |
| `testAdditionalStopFunction` | both | Assert failure at (resource:/platform/pure/essential/tests/assertFalse.pure line:29 column:5), "Failed to find match for myStopFunction_Integer_1__Integer_1_" | inlining/eval expansion |
| `testEvalWithArgs2` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | inlining/eval expansion |
| `testEvalWithArgs3` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | inlining/eval expansion |
| `testEvalWithArgs4` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | inlining/eval expansion |
| `testEvalWithArgs5` | both | Assert failure at (resource:/platform/pure/essential/tests/assertFalse.pure line:29 column:5), "Failed to find match for myTestFunc2ThatDoesSomethingViaEval_P_1__Boolean_1_" | inlining/eval expansion |
| `testFilterFalseConstantSimplification` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType |
| `testFilterFalseSimplification` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType |
| `testFilterFalseSimplification2` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType |
| `testFilterFalseSimplificationReturnType` | SHADOW | Assert failure at (resource:/core/pure/router/preeval/preeval.pure line:177 column:3), "preeval SHADOW mismatch | cast-empty/filter/toOne(Many)/genericType |
| `testFilterTrueConstantSimplification` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType |
| `testFilterTrueSimplification` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType |
| `testFilterTrueSimplification2` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType |
| `testGetGenericType` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType |
| `testInline` | both | Assert failure at (resource:/platform/pure/essential/tests/assertFalse.pure line:29 column:5), "Failed to find match for myTestFuncThatDoesEval_P_1__Boolean_1_" | inlining/eval expansion |
| `testPrerouting12` | both | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:26 column:5), "Instance of type 'String' can't be translated" | inlining/eval expansion |
| `testPrerouting19` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | map/fold unroll, concatenate |
| `testPrerouting23a` | both | » PureExecution Execution error at ??... | if/and-or — **resolved by Task 4** |
| `testPrerouting23b` | both | » PureExecution Execution error at ??... | if/and-or — **resolved by Task 4** |
| `testPrerouting24a` | both | » PureExecution Execution error at ??... | if/and-or — **resolved by Task 4** |
| `testPrerouting24b` | both | » PureExecution Execution error at ??... | inlining/eval expansion — **resolved by Task 4** (re-activation now converges once `if`/`and`/`or` short-circuit) |
| `testPrerouting24c` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | if/and-or — **resolved by Task 4** |
| `testPrerouting24d` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | if/and-or — **resolved by Task 4** |
| `testPrerouting24e` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | if/and-or — **resolved by Task 4** |
| `testPrerouting24f` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | if/and-or — **resolved by Task 4** |
| `testPrerouting25a` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | inlining/eval expansion |
| `testPrerouting26` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | inlining/eval expansion |
| `testPrerouting29a` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | map/fold unroll, concatenate |
| `testPrerouting29b` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | map/fold unroll, concatenate |
| `testPrerouting2b` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | inlining/eval expansion |
| `testPrerouting30` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | map/fold unroll, concatenate |
| `testPrerouting32b` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5) | inlining/eval expansion |
| `testPrerouting33` | both | Assert failure at (resource:/platform/pure/essential/tests/assertFalse.pure line:29 column:5), "Failed to find match for filterToStringColumns_TabularDataSet_1__TabularDataSet_1_" | TDS columns / eval-on-Column |
| `testPrerouting34` | both | Assert failure at (resource:/platform/pure/essential/tests/assertFalse.pure line:29 column:5), "Failed to find match for extendColumns_TabularDataSet_1__String_MANY__TabularDataSet_1_" | TDS columns / eval-on-Column |
| `testPrerouting37` | both | Assert failure at (resource:/platform/pure/essential/tests/assertFalse.pure line:29 column:5), "Failed to find match for otherFunc2_Integer_MANY__FunctionDefinition_1__Integer_MANY__Integer_MANY_" | inlining/eval expansion |
| `testPrerouting40b` | both | Assert failure at (resource:/platform/pure/essential/tests/assertFalse.pure line:29 column:5), "Failed to find match for myToOne_T_1__T_1_" | cast-empty/filter/toOne(Many)/genericType |
| `testPrerouting_OptionalLimit1` | both | Assert failure at (resource:/platform/pure/essential/tests/assertFalse.pure line:29 column:5), "Failed to find match for optionalLimit_TabularDataSet_1__Integer_$0_1$__TabularDataSet_1_" | inlining/eval expansion |
| `testPrerouting_OptionalLimit2` | both | Assert failure at (resource:/platform/pure/essential/tests/assertFalse.pure line:29 column:5), "Failed to find match for optionalLimit_TabularDataSet_1__Integer_$0_1$__TabularDataSet_1_" | inlining/eval expansion |
| `testPrerouting_castEmptyCollection` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType |
| `testPrerouting_concatenateInstanceValuesExpanded_Basic` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | map/fold unroll, concatenate |
| `testPrerouting_concatenateInstanceValuesExpanded_Complex` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | map/fold unroll, concatenate |
| `testPrerouting_foldOnInstanceValuesExpanded` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | map/fold unroll, concatenate |
| `testPrerouting_foldOnInstanceValuesExpanded2` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | map/fold unroll, concatenate |
| `testPrerouting_mapOnInstanceValuesExpanded` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | map/fold unroll, concatenate |
| `testProjectWithInferredParameterType` | both | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:26 column:5), "Instance of type 'meta::pure::tds::TabularDataSet' can't be translated" | inlining/eval expansion |
| `testRecursiveSimpleConcreteFunctionDefinition` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | inlining/eval expansion |
| `testToOneElimination3` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType |
| `testToOneManyElimination3` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType |

"Fails under: both" means both the `JAVA` and `SHADOW` runs failed (the `JAVA` result diverges from the expected Pure result, and — separately — the internal `assertSamePrevalResult` shadow comparison also fails, since it fires only under `SHADOW`). "JAVA" means only the direct `JAVA` implementation run failed against the expected result while the `SHADOW` comparison of the same divergence happened to still agree (rare; see `testFilterFalseSimplificationReturnType`, where `SHADOW` alone fails because the wrapper's `modified` flag disagrees even though the final `JAVA` value passed the outer assertion).

## Tests the coverage table expected to diverge, but that already pass at baseline

These stay out of `KNOWN_DIVERGENT`; no rule work is needed for them (already correct, apparently by a different existing code path or coincidental re-activation equivalence):

`testDecimalType`, `testEvalWithArgs1`, `testLetOnlyStatement`, `testPrerouting10`, `testPrerouting11`, `testPrerouting13`, `testPrerouting14`, `testPrerouting15`, `testPrerouting16`, `testPrerouting17`, `testPrerouting18`, `testPrerouting20`, `testPrerouting21`, `testPrerouting25b`, `testPrerouting25c`, `testPrerouting27b`, `testPrerouting28`, `testPrerouting31b`, `testPrerouting35`, `testPrerouting38`, `testPrerouting40a`, `testPrerouting6`, `testPrerouting7`, `testPrerouting_mappedTdsAgg` (24 tests).

## Verification

- CORE-BUILD: green.
- RUNNER-ALL (`KNOWN_DIVERGENT` temporarily empty, to establish this baseline): `Test_Pure_Preeval` 111/111; `Test_Pure_Preeval_Java` 222 run, 88 failures + 8 errors (96 failing test executions across 49 distinct test names).
- After populating `KNOWN_DIVERGENT` with the 49 names above and rebuilding: RUNNER is green — `Test_Pure_Preeval_Java` 124/124 (62 collected × 2 implementations), `Test_Pure_Preeval` 111/111.
- None of the 26 P1 target tests appear among the 49 failing names.
- Every failing name is listed in the plan's expected-rule-coverage table; there are no `unexpected` failures.

## Task 4 update (`if`, `and`/`or`)

After implementing `IfRule` and `AndOrRule` (`rules/IfRule.java`, `rules/AndOrRule.java`, `rules/RuleSupport.java`), RUNNER-ALL was re-run (`-Dlegend.engine.preeval.test.includeKnownDivergent=true`). Of the 49 `KNOWN_DIVERGENT` names, exactly 8 now pass under both `JAVA` and `SHADOW` and no longer appear in any failure output:

`testPrerouting23a`, `testPrerouting23b`, `testPrerouting24a`, `testPrerouting24b`, `testPrerouting24c`, `testPrerouting24d`, `testPrerouting24e`, `testPrerouting24f`.

This matches the plan's expected removals for Task 4 (`testPrerouting23a/b, 24a, 24c–f`) plus one bonus name, `testPrerouting24b` (originally attributed to the inlining/eval-expansion family in Task 5), which now passes because re-activation converges once the `if`/`and`/`or` short-circuits are in place — exactly the "some may already pass... because re-activation happens to give the same result" case the plan calls out. All remaining 41 `KNOWN_DIVERGENT` names were re-verified still failing in the same run; no new (unexpected) failures appeared, and no P1/harness test regressed.

`KNOWN_DIVERGENT` in `Test_Pure_Preeval_Java` now holds 41 names (49 − 8). RUNNER (no `includeKnownDivergent`) is green: `Test_Pure_Preeval_Java` 140/140 (70 collected × 2 implementations), `Test_Pure_Preeval` 111/111.
