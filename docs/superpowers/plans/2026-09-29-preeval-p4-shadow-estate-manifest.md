# Preeval P4 — shadow estate manifest

Plan: `docs/superpowers/plans/2026-09-29-preeval-java-native-p4-shadow-estate.md`.

Categories: JAVA-BUG (Java differs from Pure; fix Java), PURE-QUIRK (Pure behaviour Java must reproduce; fix Java with `// parity:`), COMPARATOR (describe/comparator false positive or crash; fix describe), NOT-SHADOW (fails identically under the PURE control; listed only).

## Comparator baseline (Task 1)

| Test | Runner | Mode | First differing node (PURE / JAVA) | Category | Status |
|---|---|---|---|---|---|

None: the per-node comparator found no new differences.

## Estate runs (Task 5, 2026-09-29, HEAD d84e96362ed)
| Suite | Module | Classes | Control ran / failing | SHADOW ran / failing | SHADOW-only |
|---|---|---|---|---|---|
| SQL | `legend-engine-xts-sql/legend-engine-xt-sql-transformation/legend-engine-xt-sql-pure` | `Test_SQL_Pure` | 237 / 2 | 237 / 3 | 1 |
| LIN | `legend-engine-xts-analytics/legend-engine-xts-analytics-lineage/legend-engine-xt-analytics-lineage-pure` | `Test_Analytics_Lineage` (51), `Test_Analytics_Lineage__M2M_MFT` (166), `Test_Analytics_Lineage_Relational_MFT` (374) | 591 / 0 | 591 / 1 | 1 |
| RC | `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-generation/legend-engine-xt-relationalStore-pure/legend-engine-xt-relationalStore-core-pure` | `Test_Pure_Relational` (2841); `Test_Pure_Relational_MFT_Collection` is a collection builder with no tests, so 0 ran | 2841 / 3 | 2841 / 3 | 0 |
| H2 | `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-h2/legend-engine-xt-relationalStore-h2-PCT` | `Test_Relational_H2_PCT` (1415), `Test_Relational_H2_Semistructured` (171), `Test_Relational_H2_NonRelationalToRelational` (1) | 1587 / 1 | 1587 / 263 | 262 |
| DUCK | `legend-engine-xts-relationalStore/legend-engine-xt-relationalStore-dbExtension/legend-engine-xt-relationalStore-duckdb/legend-engine-xt-relationalStore-duckdb-PCT` | `Test_Relational_DuckDB_PCT` (1415), `Test_Relational_DuckDB_Semistructured` (171), `Test_Relational_DuckDB_NonRelationalToRelational` (1) | 1587 / 0 | 1587 / 262 | 262 |

## Estate findings
| # | Root cause | Category | Smallest failing test | Other affected tests (count) | Status |
|---|---|---|---|---|---|
| 1 | `describePrevalResult` value step: `transformFunctionBody` → `transformAny` → `possiblyTransformNewFunction` fails with `Instance of type 'meta::pure::functions::relation::_Window' can't be translated`. The value is an `InstanceValue` holding the `_Window` that preeval produced by evaluating `over(...)`. | COMPARATOR | H2 PCT (`Test_Relational_H2_PCT`) `meta::pure::functions::relation::tests::size::testWindowSize` | H2 222, DUCK 223 (the same test set in both; 2 H2 tests and 1 DUCK test also hide an existing manifest exclusion) | fixed in Task 6.1 ("Describe preeval SHADOW values without the protocol") |
| 2 | Same step as #1, but the `InstanceValue` holds a `SortInfo` that preeval produced from `ascending(~c)` / `descending(~c)`: `Instance of type 'meta::pure::functions::relation::SortInfo' can't be translated` | COMPARATOR | H2 PCT (`Test_Relational_H2_PCT`) `meta::pure::functions::relation::tests::sort::testSimpleSortShared` | H2 38, DUCK 39 (the same test set in both; 7 H2 tests also hide an existing manifest exclusion) | fixed in Task 6.1 ("Describe preeval SHADOW values without the protocol") |
| 3 | Same step as #1, but the `InstanceValue` holds a relation `#TDS` literal that `sqlToPure` built from a `DynamicSQLSource` generation function: `Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated` | COMPARATOR | SQL (`Test_SQL_Pure`) `meta::external::query::sql::transformation::queryToPure::tests::dynamic::testDynamicWithNonDataTypeColumn` | 0 | fixed in Task 6.1 ("Describe preeval SHADOW values without the protocol") |
| 4 | `describePrevalResult` value step: `transformAny` Runtime branch → `transformRuntime` → `transformConnection` fails with `TestDatabaseConnection Connection type not supported Yet!`. The `InstanceValue` holds the Runtime that preeval produced from `testRuntime()`. The lineage preval call passes `[] + lineagePreEvalExtension()`, which has no relational `SerializerExtension_vX_X_X`, so no handler matches the connection. | COMPARATOR | LIN (`Test_Analytics_Lineage`) `meta::analytics::lineage::tests::relation::testProject` | 0 | fixed in Task 6.1 ("Describe preeval SHADOW values without the protocol") |

## Pre-existing failures (NOT-SHADOW)
| Suite | Test | Failure (first line) |
|---|---|---|
| SQL | `meta::external::query::sql::transformation::queryToPure::tests::testToChar` | `Assert failure at (resource:/platform/pure/essential/tests/assertEquals.pure line:31 column:5), "expected 6 to equal 7 sql: to_char(DateTime, 'D')"` |
| SQL | `meta::external::query::sql::transformation::queryToPure::tests::testToCharCombined` | `Assert failure at (resource:/platform/pure/essential/tests/assertEquals.pure line:31 column:5), "expected 09-9-08-8-07-7-2023-023-23-APRIL    -APRIL-april ... to equal ..."` |
| RC | `meta::relational::tests::projection::filter::dates::recent::testMostRecentDayOfWeekWithDate` | `Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "expected size: 3, actual size: 2"` |
| RC | `meta::relational::tests::projection::filter::dates::recent::testPreviousDayOfWeekWithDate` | `Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), "expected size: 3, actual size: 2"` |
| RC | `meta::relational::tests::query::function::testDayOfWeekNumberFunction` | `Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), " expected: 'Day Of Week Number\n2\n2\n2\n4...' actual: 'Day Of Week Number\n1\n1\n1\n3...'"` |
| H2 | `meta::pure::functions::date::tests::testDateDiffWeeks_Function_1__Boolean_1_` | `PCT expected-failure mismatch ... expected : "\nexpected: 0\nactual:   -1"  actual : \nexpected: 1\nactual:   0` |

## Not covered
- `legend-engine-xts-openapi` (`pureToOpenApi.pure:172`) and `legend-engine-xts-dataquality` (`dataprofile_test.pure:104`) call `preval` but are outside the spec's estate list.

## Triage notes

### Method and cross-checks
- The instance-level comparison lines up each `<testcase>` by its position in the XML. Classname, name and order are identical in both modes for every suite, so no test ran in only one mode.
- For every failure in the control run, the SHADOW run fails at the same position with a byte-identical message (5 of the 6 were checked on the full message; H2 `testDateDiffWeeks` was also identical). No SHADOW-only failure hides under a key that also fails in control. RC fails on the same 3 test instances with the same messages in both modes.
- Duplicate keys: LIN 591 tests / 76 keys, RC 2841 / 2448, H2 and DUCK 1587 / 1541 (46 duplicate PCT names). Because the comparison is per instance, the key collapse hides nothing.
- `diff.py` output has no `CONTROL-ONLY` or `RAN-IN-ONE-ONLY` lines in any suite. The H2 control failure (`testDateDiffWeeks`) shares its key with a SHADOW failure, which is why H2 shows 263 failing but only 262 SHADOW-only.
- No report or log contains `preeval SHADOW mismatch`, a `prevalNative` exception, or the `prevalJava` State assertion. Every SHADOW-only failure is thrown while describing the Pure result, before any comparison with Java. That describe runs first in `assertSamePrevalResult`, and in every group the offending value is already in Pure's own output (see below). **These 526 tests therefore give no evidence either way about Java parity.** They mask it; they do not show a Java bug.
- The failures carry no Pure stack. Surefire keeps only the `assert.pure` frame and the JUnit frames. Each root cause was located from the message text and the vX_X_X sources.
- Test counts per group are for SHADOW-only failures. H2 and DUCK fail on exactly the same set of 262 test names: 223 `_Window` and 39 `SortInfo` in each. The Findings table lists the smallest test separately, so its "other" column is one lower for H2.

### Group 1–3: relation-DSL values that `transformAny` cannot translate (COMPARATOR)
- Throw site: `core/pure/protocol/vX_X_X/transfers/valueSpecification.pure:699`, `possiblyTransformNewFunction`:
  `fail('Instance of type \'' + $class->elementToPath() + '\' can\'t be translated')`. `transformAny` reaches it through its `a:Any[1]` fallback (line 647). There is no branch for `meta::pure::functions::relation::_Window`, `meta::pure::functions::relation::SortInfo` or `meta::pure::metamodel::relation::TDS`; the only sort branch is for the legacy `meta::pure::tds::SortInformation`.
- Describe step: the value transform, i.e. the `fd:FunctionDefinition` branch of `describePrevalResult` (`preeval.pure` ~line 187, `transformFunctionBody($extensions)->toJSON`). The type step and the node walk are never reached.
- Why Pure's own result contains the value: in `prevalInternal`'s function-expression path, the "Performing preval" branch (`preeval.pure` ~line 950) reactivates any function expression whose parameters are all instance values and have no open variables. It then wraps the result in `^InstanceValue(genericType = $newSfe.genericType, values = $val)`. `over(~grp)`, `ascending(~id)` and `~id->ascending()` qualify: `stopPreeval` gates on the expression, not on the resulting value's type. So the PURE-side describe fails before the Java-side describe runs. For group 3, `sqlToPure` (`fromPure.pure:251`) builds the lambda with the evaluated `#TDS` literal already inside an `InstanceValue`, before `preval` is called.
- Excerpts:
  - H2/DUCK: `PCT expected-failure mismatch for meta::pure::functions::relation::tests::composition::testExtendAddOnNull_Function_1__Boolean_1_ / expected : (none - no exclusion for this test) / actual : Instance of type 'meta::pure::functions::relation::_Window' can't be translated`
  - SQL: `Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:26 column:5), "Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated"`
- Group 1 covers relation::tests (186), math::tests `*_Relation_Window` (30), `meta::external::scenario::quant::*` (5) and date::tests `testExtendMax/MinDate` (2). Group 2 covers sort/limit/slice/drop `*Shared`, `joinStrings::*`, `composition::*Pivot*` / `*_Sort_*`, and `variant::flatten::testFlatten_LateralJoin_Nested*` (39).
- Tests whose manifest exclusion is now hidden (all switched to the COMPARATOR error):
  - H2 `_Window`: `composition::testStaticPivot_AfterExtendConcatenate` and `quant::gap::testGapAnalysis`.
  - H2 `SortInfo`: `testVariantMapColumn_keys/values_LateralFlatten`, three `test_Extend_Filter_Select_*Pivot*`, and `flatten::testFlatten_LateralJoin_Nested(_Extend)`.
  - DUCK `_Window`: `percentile::testPercentile_Relation_Window`.
- Suggested fix direction: fix this in the comparator and leave both preeval implementations alone, because the protocol transform is the wrong tool for arbitrary evaluated values. Change `describePrevalResult` to stop producing the value via `transformFunctionBody` / `transformValueSpecification`. Instead, use a printer that tolerates any `InstanceValue` payload: either `meta::pure::metamodel::serialization::grammar::printFunctionDefinition` / `printValueSpecification` (which already prints `#TDS` literals, as `testDynamicWithNonDataTypeColumn` itself asserts), or a sanitising pre-pass that replaces each value `transformAny` cannot handle with `type + toString()`. The fix must also extend `describeInstanceValueElement` so its `a:Any[1]` branch prints the element's type (and `toString` for DataType values) instead of `[]`. Otherwise a divergence inside a `_Window` or `SortInfo` would go unseen. Adding `_Window`/`SortInfo`/relation-`TDS` branches to vX_X_X `transformAny` is the alternative, but it is a protocol change beyond this workstream's scope. Re-run H2/DUCK/SQL SHADOW afterwards: these 525 tests are the estate's only relation/OLAP preeval coverage, and real Java mismatches may be waiting behind them.
- As fixed (Task 6.1): neither the grammar printer nor a sanitising pre-pass was used. `describePrevalResult` dropped the protocol `value=` segment for `FunctionDefinition` and `ValueSpecification` results, and `describeNodes` became a complete protocol-free structural walk. `describeInstanceValueElement` now prints primitives with their type, enums, packageable elements and function identities, and prints any other instance as `instance <classifier path>` with its sorted property values, capped at 4 levels. `_Window`, `SortInfo` and `#TDS` values are therefore described property by property, so a divergence inside them is still seen.

### Group 4: Runtime with a connection the passed extensions cannot serialise (COMPARATOR)
- Throw site: `core/pure/protocol/vX_X_X/transfers/store.pure:49`, `transformConnection`'s `other:` fallback: `fail('' + $failureClass + ' Connection type not supported Yet!')`. The relational handler for `TestDatabaseConnection` exists (`core_relational/relational/protocols/pure/vX_X_X/transfers/connection_relational.pure:41`), but it is registered only through `relationalExtensions()`.
- Describe step: the value transform. `transformAny`'s `r:meta::core::runtime::Runtime[1]` branch (valueSpecification.pure ~line 561) calls `transformRuntime($extensions)`.
- Trigger: `lineageTests.pure:839` `testProject` builds the query `...->from(meta::external::store::relational::tests::testRuntime())` and calls `computeLineage($query, [], [], [], config)`. That call reaches `getLineageInputFromFunctionDefinition` (`fullAnalytics.pure:165`), which calls `preval($extensions->concatenate(lineagePreEvalExtension()))`, so the extensions are only the lineage pre-eval extension. `testRuntime()` has no parameters, so preeval evaluates it and wraps the Runtime (with its `TestDatabaseConnection`) in an `InstanceValue`. `stopPreeval` explicitly allows `Runtime` values. The Pure-side describe then fails because no serializer extension is available.
- Excerpt: `Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:26 column:5), "TestDatabaseConnection Connection type not supported Yet!"`
- Suggested fix direction: this is a comparator artefact caused by the caller's extension set. The fix belongs in `describePrevalResult`, with no change to preeval or the lineage caller. Describe `Runtime`, `PackageableRuntime`, `Connection` and `Mapping` values by identity rather than protocol, for example `elementToPath()` for packageable ones and `class + connectionStores.connection->class()` for a bare Runtime. The printer or sanitiser from groups 1–3 would cover this too if it treats `Runtime` as an opaque value. Do not make the comparator add `relationalExtensions()`, because that would couple core preeval to a store.
- As fixed (Task 6.1): the Runtime is not special-cased. It falls under the generic instance rule, which describes it by classifier path and sorted property values (connections included), and the caller's extensions are no longer used by the describe.

### Pre-existing (NOT-SHADOW)
- All 6 fail identically in both modes and depend on the date or environment: day-of-week and `to_char 'D'` semantics, and H2 `DATEDIFF` week boundaries. They are unrelated to preeval.

### Other observations
- `Test_Pure_Relational_MFT_Collection` (listed in the global-constraints RC row) is only a static `buildCollection` helper, with no `@Test` and no suite, so it contributes 0 tests. The relational MFT collection runs through LIN's `Test_Analytics_Lineage_Relational_MFT`, where all 374 pass in SHADOW.
- No JAVA-BUG or PURE-QUIRK was observed. Every SHADOW-only failure was thrown before the Java result was described.

Note: `Test_Pure_Relational_MFT_Collection` (listed for RC in the plan) is a collection builder with no tests of its own, so RC ran `Test_Pure_Relational` only. The relational MFT collection is exercised through LIN `Test_Analytics_Lineage_Relational_MFT` (374 tests, all green under SHADOW).
