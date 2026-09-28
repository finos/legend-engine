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
| `tesColumnEvalOnRelation` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | TDS columns / eval-on-Column — **resolved by Task 8** |
| `tesColumnEvalOnRelationWithCast` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | TDS columns / eval-on-Column — **resolved by Task 8** |
| `testAdditionalStopFunction` | both | Assert failure at (resource:/platform/pure/essential/tests/assertFalse.pure line:29 column:5), "Failed to find match for myStopFunction_Integer_1__Integer_1_" | inlining/eval expansion — **resolved by Task 5** |
| `testEvalWithArgs2` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | inlining/eval expansion — **resolved by Task 5** |
| `testEvalWithArgs3` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | inlining/eval expansion — **resolved by Task 5** |
| `testEvalWithArgs4` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | inlining/eval expansion — **resolved by Task 5** |
| `testEvalWithArgs5` | both | Assert failure at (resource:/platform/pure/essential/tests/assertFalse.pure line:29 column:5), "Failed to find match for myTestFunc2ThatDoesSomethingViaEval_P_1__Boolean_1_" | inlining/eval expansion — **resolved by Task 5** |
| `testFilterFalseConstantSimplification` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType — **resolved by Task 7** |
| `testFilterFalseSimplification` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType — **resolved by Task 7** |
| `testFilterFalseSimplification2` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType — **resolved by Task 7** |
| `testFilterFalseSimplificationReturnType` | SHADOW | Assert failure at (resource:/core/pure/router/preeval/preeval.pure line:177 column:3), "preeval SHADOW mismatch | cast-empty/filter/toOne(Many)/genericType — **resolved by Task 7** |
| `testFilterTrueConstantSimplification` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType — **resolved by Task 7** |
| `testFilterTrueSimplification` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType — **resolved by Task 7** |
| `testFilterTrueSimplification2` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType — **resolved by Task 7** |
| `testGetGenericType` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType — **resolved by Task 7** |
| `testInline` | both | Assert failure at (resource:/platform/pure/essential/tests/assertFalse.pure line:29 column:5), "Failed to find match for myTestFuncThatDoesEval_P_1__Boolean_1_" | inlining/eval expansion — **resolved by Task 5** |
| `testPrerouting12` | both | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:26 column:5), "Instance of type 'String' can't be translated" | inlining/eval expansion — **resolved by Task 5** |
| `testPrerouting19` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | map/fold unroll, concatenate — **resolved by Task 6** |
| `testPrerouting23a` | both | » PureExecution Execution error at ??... | if/and-or — **resolved by Task 4** |
| `testPrerouting23b` | both | » PureExecution Execution error at ??... | if/and-or — **resolved by Task 4** |
| `testPrerouting24a` | both | » PureExecution Execution error at ??... | if/and-or — **resolved by Task 4** |
| `testPrerouting24b` | both | » PureExecution Execution error at ??... | inlining/eval expansion — **resolved by Task 4** (re-activation now converges once `if`/`and`/`or` short-circuit) |
| `testPrerouting24c` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | if/and-or — **resolved by Task 4** |
| `testPrerouting24d` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | if/and-or — **resolved by Task 4** |
| `testPrerouting24e` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | if/and-or — **resolved by Task 4** |
| `testPrerouting24f` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | if/and-or — **resolved by Task 4** |
| `testPrerouting25a` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | inlining/eval expansion — **resolved by Task 5** |
| `testPrerouting26` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | inlining/eval expansion — **resolved by Task 5** |
| `testPrerouting29a` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | map/fold unroll, concatenate — **resolved by Task 6** |
| `testPrerouting29b` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | map/fold unroll, concatenate — **resolved by Task 6** |
| `testPrerouting2b` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | inlining/eval expansion — **resolved by Task 5** |
| `testPrerouting30` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | map/fold unroll, concatenate — **resolved by Task 6** |
| `testPrerouting32b` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5) | inlining/eval expansion — **resolved by Task 5 (fix round 1)** |
| `testPrerouting33` | both | Assert failure at (resource:/platform/pure/essential/tests/assertFalse.pure line:29 column:5), "Failed to find match for filterToStringColumns_TabularDataSet_1__TabularDataSet_1_" | TDS columns / eval-on-Column — **resolved by Task 8** |
| `testPrerouting34` | both | Assert failure at (resource:/platform/pure/essential/tests/assertFalse.pure line:29 column:5), "Failed to find match for extendColumns_TabularDataSet_1__String_MANY__TabularDataSet_1_" | TDS columns / eval-on-Column — **resolved by Task 8** |
| `testPrerouting37` | both | Assert failure at (resource:/platform/pure/essential/tests/assertFalse.pure line:29 column:5), "Failed to find match for otherFunc2_Integer_MANY__FunctionDefinition_1__Integer_MANY__Integer_MANY_" | inlining/eval expansion — **resolved by Task 5** |
| `testPrerouting40b` | both | Assert failure at (resource:/platform/pure/essential/tests/assertFalse.pure line:29 column:5), "Failed to find match for myToOne_T_1__T_1_" | cast-empty/filter/toOne(Many)/genericType — **resolved by Task 7** |
| `testPrerouting_OptionalLimit1` | both | Assert failure at (resource:/platform/pure/essential/tests/assertFalse.pure line:29 column:5), "Failed to find match for optionalLimit_TabularDataSet_1__Integer_$0_1$__TabularDataSet_1_" | inlining/eval expansion — **resolved by Task 5** |
| `testPrerouting_OptionalLimit2` | both | Assert failure at (resource:/platform/pure/essential/tests/assertFalse.pure line:29 column:5), "Failed to find match for optionalLimit_TabularDataSet_1__Integer_$0_1$__TabularDataSet_1_" | inlining/eval expansion — **resolved by Task 5** |
| `testPrerouting_castEmptyCollection` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType — **resolved by Task 7** |
| `testPrerouting_concatenateInstanceValuesExpanded_Basic` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | map/fold unroll, concatenate — **resolved by Task 6** |
| `testPrerouting_concatenateInstanceValuesExpanded_Complex` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | map/fold unroll, concatenate — **resolved by Task 6** |
| `testPrerouting_foldOnInstanceValuesExpanded` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | map/fold unroll, concatenate — **resolved by Task 6** |
| `testPrerouting_foldOnInstanceValuesExpanded2` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | map/fold unroll, concatenate — **resolved by Task 6** |
| `testPrerouting_mapOnInstanceValuesExpanded` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | map/fold unroll, concatenate — **resolved by Task 6** |
| `testProjectWithInferredParameterType` | both | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:26 column:5), "Instance of type 'meta::pure::tds::TabularDataSet' can't be translated" | inlining/eval expansion — **resolved by Task 5** |
| `testRecursiveSimpleConcreteFunctionDefinition` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | inlining/eval expansion — **resolved by Task 5** |
| `testToOneElimination3` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType — **resolved by Task 7** |
| `testToOneManyElimination3` | JAVA | Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "Mismatch between expected and actual result content." | cast-empty/filter/toOne(Many)/genericType — **resolved by Task 7** |

"Fails under: both" means both the `JAVA` and `SHADOW` runs failed (the `JAVA` result diverges from the expected Pure result, and — separately — the internal `assertSamePrevalResult` shadow comparison also fails, since it fires only under `SHADOW`). "JAVA" means only the direct `JAVA` implementation run failed against the expected result while the `SHADOW` comparison of the same divergence happened to still agree (rare; see `testFilterFalseSimplificationReturnType`, where `SHADOW` alone fails because the wrapper's `modified` flag disagrees even though the final `JAVA` value passed the outer assertion).

## Tests the coverage table expected to diverge, but that already pass at baseline

These stay out of `KNOWN_DIVERGENT`; no rule work is needed for them (already correct, apparently by a different existing code path or coincidental re-activation equivalence):

`testDecimalType`, `testEvalWithArgs1`, `testLetOnlyStatement`, `testPrerouting10`, `testPrerouting11`, `testPrerouting13`, `testPrerouting14`, `testPrerouting15`, `testPrerouting16`, `testPrerouting17`, `testPrerouting18`, `testPrerouting20`, `testPrerouting21` (re-entered `KNOWN_DIVERGENT` in Task 5, resolved by Task 6, see below), `testPrerouting25b`, `testPrerouting25c`, `testPrerouting27b`, `testPrerouting28`, `testPrerouting31b`, `testPrerouting35`, `testPrerouting38`, `testPrerouting40a`, `testPrerouting6`, `testPrerouting7`, `testPrerouting_mappedTdsAgg` (24 tests).

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

## Task 5 update (inlining, `eval` expansion)

After implementing `InlineRule` and `EvalExpansionRule` (plus `addToScope`, the `shouldInline`/`isGeneratedMilestoningProperty` hooks, and a compiled-reactivation flattening parity fix), RUNNER-ALL was re-run. Of the 41 `KNOWN_DIVERGENT` names, 15 now pass under both `JAVA` and `SHADOW`:

`testAdditionalStopFunction`, `testEvalWithArgs2`, `testEvalWithArgs3`, `testEvalWithArgs4`, `testEvalWithArgs5`, `testInline`, `testPrerouting12`, `testPrerouting25a`, `testPrerouting26`, `testPrerouting2b`, `testPrerouting37`, `testPrerouting_OptionalLimit1`, `testPrerouting_OptionalLimit2`, `testProjectWithInferredParameterType`, `testRecursiveSimpleConcreteFunctionDefinition`.

`testPrerouting20` passed at baseline, failed once inlining exposed a nested collection from compiled reactivation of `map`, and passes again with the flattening fix in `CompiledPrevalRuntime.reactivate`.

**Re-entered `KNOWN_DIVERGENT`: `testPrerouting21`** (map/fold unroll, Task 6). It passed at baseline only because nothing was inlined. Once `myExpandableFunc2` is inlined, `range(0, 3, 1)->map(index|$p)` is left for compiled reactivation, which yields `[a]|[a]|[a]` instead of `a|a|a`. A hand-written, never-prevalled copy of the same expression reactivates to the same wrong value, so this is compiled-reactivation behaviour rather than an inlining bug; Pure avoids it through its `map`-over-InstanceValue unroll handler, which Task 6 ports.

**Still divergent at the initial Task 5 commit, expected for Task 5: `testPrerouting32b`** (resolved in fix round 1, below) (fails under `JAVA` only). The preval result itself matches Pure (the column lambda prints as `$row.getInteger('age')->toString()` under both), but evaluating the `JAVA` result lambda produces the original column lambda (`'ag' + $x`). The copied compiled lambda appears to execute its original precompiled body. Stamping the copy with the call site's source information did not change the outcome; left for follow-up.

Still failing, owned by later tasks: the map/fold/concatenate family (Task 6), filter/cast/toOne/genericType family (Task 7), TDS columns / eval-on-Column (Task 8).

`KNOWN_DIVERGENT` now holds 27 names (41 − 15 + 1). RUNNER is green: `Test_Pure_Preeval_Java` 168/168 (84 collected × 2 implementations), `Test_Pure_Preeval` 111/111.

### Task 5 fix round 1

`testPrerouting32b` now passes under both `JAVA` and `SHADOW`. `CompiledPrevalRuntime.withExpressionSequence` returned the copied `PureCompiledLambda` wrapper, whose `pureFunction()` is the original precompiled body, so evaluating the rewritten lambda ran the original body. It now mirrors preeval.pure's generated code: it keeps the setter's return value (which unwraps the `PureCompiledLambda`), and for lambdas makes the second `^$lf(openVariables = ...)` copy with the call site's source information, so the precompiled-lambda lookup misses and the new body is reactivated. The compiled reactivation flattening was narrowed to single-element nested collections (Pure's result `match`); re-checked with the unwrap fix in place, it is still required (`testReactivatedNestedCollectionIsFlattened` fails in compiled mode without it).

`KNOWN_DIVERGENT` now holds 26 names. RUNNER: `Test_Pure_Preeval_Java` 170/170 (85 collected × 2), `Test_Pure_Preeval` 111/111.

## Task 6 update (`map`/`fold` unroll, `concatenate`)

After implementing `MapUnrollRule`, `FoldUnrollRule` and `ConcatenateRule` (`Rules.NOT_PREVALLED`, in that order), plus `PrevalRuntime.lowerBound`/`upperBound`/`isMultiplicityConcrete` and the `Multiplicities` helpers, RUNNER-ALL was re-run. Of the 26 `KNOWN_DIVERGENT` names, 10 now pass under both `JAVA` and `SHADOW`:

`testPrerouting19`, `testPrerouting21`, `testPrerouting29a`, `testPrerouting29b`, `testPrerouting30`, `testPrerouting_concatenateInstanceValuesExpanded_Basic`, `testPrerouting_concatenateInstanceValuesExpanded_Complex`, `testPrerouting_foldOnInstanceValuesExpanded`, `testPrerouting_foldOnInstanceValuesExpanded2`, `testPrerouting_mapOnInstanceValuesExpanded`.

`testPrerouting21`, re-entered in Task 5, passes again: the inlined `range(0, 3, 1)->map(index|$p)` is now unrolled by `MapUnrollRule` instead of being left to compiled reactivation. `testPrerouting31b` (statically attributed to this family) already passed at baseline. No other name changed state, and no unexpected failure appeared.

Still failing, owned by later tasks: the filter/cast/toOne/genericType family (Task 7) and TDS columns / eval-on-Column (Task 8).

`KNOWN_DIVERGENT` now holds 16 names.

## Task 7 update (cast-of-empty, filter `false`/`true`, `toOneMany`, `toOne`, `genericType`)

After implementing `EmptyCastRule`, `FilterFalseRule`, `FilterTrueRule`, `ToOneManyRule`, `ToOneRule` and `GenericTypeRule` (appended to `Rules.NOT_PREVALLED` in that order), the `isGetAllFunction` hook, `PrevalRuntime.genericTypeOf`, and Pure's evaluate-every-predicate handler selection, RUNNER-ALL was re-run. Of the 16 `KNOWN_DIVERGENT` names, 12 now pass under both `JAVA` and `SHADOW`:

`testFilterFalseConstantSimplification`, `testFilterFalseSimplification`, `testFilterFalseSimplification2`, `testFilterFalseSimplificationReturnType`, `testFilterTrueConstantSimplification`, `testFilterTrueSimplification`, `testFilterTrueSimplification2`, `testGetGenericType`, `testPrerouting40b`, `testPrerouting_castEmptyCollection`, `testToOneElimination3`, `testToOneManyElimination3`.

This is exactly the family's expected list. No other name changed state, and no unexpected failure appeared.

Still failing, owned by Task 8: `tesColumnEvalOnRelation`, `tesColumnEvalOnRelationWithCast`, `testPrerouting33`, `testPrerouting34`.

`KNOWN_DIVERGENT` now holds 4 names.

## Task 8 update (TDS `columns`, eval-on-Column)

After implementing `TdsColumnsRule` and `EvalOnColumnRule` (prepended to `Rules.NOT_PREVALLED`, which now matches Pure's full handler order), the `resolveTdsSchema` hook, and `PrevalRuntime.isPropertyOf` / `functionReturnType` / `functionReturnMultiplicity` / `withGenericTypeAndMultiplicity`, RUNNER-ALL was re-run. All 4 remaining `KNOWN_DIVERGENT` names now pass under both `JAVA` and `SHADOW`:

`tesColumnEvalOnRelation`, `tesColumnEvalOnRelationWithCast`, `testPrerouting33`, `testPrerouting34`.

No other name changed state. `KNOWN_DIVERGENT` is now empty; RUNNER is green with `Test_Pure_Preeval_Java` 222/222 (111 × 2) and `Test_Pure_Preeval` 111/111.
