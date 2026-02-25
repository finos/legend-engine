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

package org.finos.legend.engine.plan.execution.stores.relational.test.trino.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalTrinoPCTCodeRepositoryProvider;
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Trino_EssentialFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = PlatformCodeRepositoryProvider.essentialFunctions;
    private static final Adapter adapter = CoreRelationalTrinoPCTCodeRepositoryProvider.trinoAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Add
            one("meta::pure::functions::collection::tests::add::testAddWithOffset_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'add_T_MANY__Integer_1__T_1__T_$1_MANY$_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::add::testAdd_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_append' (state: [Select, false]) is not supported yet\""),

            // At
            one("meta::pure::functions::collection::tests::at::testAtOtherScenario_Function_1__Boolean_1_", "\"->at(...) function is supported only after direct access of 1->MANY properties. Current expression: ['a', 'b', 'c'] -> map(x:String[1] | [$x, 'z'] -> plus();) -> at(0)\""),
            one("meta::pure::functions::collection::tests::at::testAtWithVariable_Function_1__Boolean_1_", "\"->at(...) function is supported only after direct access of 1->MANY properties. Current expression: ['a', 'b', 'c'] -> at(1)\""),
            one("meta::pure::functions::collection::tests::at::testAt_Function_1__Boolean_1_", "\"->at(...) function is supported only after direct access of 1->MANY properties. Current expression: ['a', 'b', 'c'] -> at(0)\""),

            // Concatenate
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateMixedType_Function_1__Boolean_1_", "\"Any is not managed yet!\""),
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateSimple_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_concatenate' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateTypeInference_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_concatenate' (state: [Select, false]) is not supported yet\""),

            // Contains
            one("meta::pure::functions::collection::tests::contains::testContainsNonPrimitive_Function_1__Boolean_1_", "\"Parameter to IN operation isn't a literal!\""),
            one("meta::pure::functions::collection::tests::contains::testContainsPrimitive_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::collection::tests::contains::testContainsWithFunction_Function_1__Boolean_1_", "no viable alternative at input '->meta::pure::functions::collection::contains(meta::pure::functions::collection::tests::contains::ClassWithoutEquality.all()->meta::pure::functions::multiplicity::toOne(),meta::pure::functions::collection::tests::contains::comparator(a:meta::pure::functions::collection::tests::contains::ClassWithoutEquality[1],'"),

            // Drop
            one("meta::pure::functions::collection::tests::drop::testDropExceedsSizeOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::drop::testDropExceedsSizeOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::drop::testDropInList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::drop::testDropManyOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::drop::testDropManyOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::drop::testDropNegativeOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::drop::testDropNegativeOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::drop::testDropOneOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::drop::testDropOneOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::drop::testDropZeroOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::drop::testDropZeroOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\""),

            // Exists
            one("meta::pure::functions::collection::tests::exists::testExistsInSelect_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::collection::tests::exists::testExists_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),

            // Find
            one("meta::pure::functions::collection::tests::find::testFindInstance_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/essential/collection/iteration/find.pure:38cc38-42)"),
            one("meta::pure::functions::collection::tests::find::testFindLiteralFromVar_Function_1__Boolean_1_", "Function does not exist 'meta::pure::functions::collection::find(String[3],LambdaFunction<{String[1]->Boolean[1]}>[1])'"),
            one("meta::pure::functions::collection::tests::find::testFindLiteral_Function_1__Boolean_1_", "Function does not exist 'meta::pure::functions::collection::find(String[4],LambdaFunction<{String[1]->Boolean[1]}>[1])'"),
            one("meta::pure::functions::collection::tests::find::testFindUsingVarForFunction_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/essential/collection/iteration/find.pure:47cc38-42)"),

            // Fold
            one("meta::pure::functions::collection::tests::fold::testFoldCollectionAccumulator_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::collection::tests::fold::testFoldEmptyListAndEmptyIdentity_Function_1__Boolean_1_", "\"Any is not managed yet!\""),
            one("meta::pure::functions::collection::tests::fold::testFoldEmptyListAndNonEmptyIdentity_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::collection::tests::fold::testFoldFiltering_Function_1__Boolean_1_", "Function does not exist 'meta::pure::functions::lang::copy(FO_Person[1],String[1],KeyExpression[1])'"),
            one("meta::pure::functions::collection::tests::fold::testFoldMixedAccumulatorTypes_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::collection::tests::fold::testFoldToMany_Function_1__Boolean_1_", "Function does not exist 'meta::pure::functions::lang::copy(FO_Person[1],String[1],KeyExpression[1])'"),
            one("meta::pure::functions::collection::tests::fold::testFoldWithEmptyAccumulator_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::collection::tests::fold::testFoldWithSingleValue_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::collection::tests::fold::testFold_Function_1__Boolean_1_", "Function does not exist 'meta::pure::functions::lang::copy(FO_Person[1],String[1],KeyExpression[1])'"),
            one("meta::pure::functions::collection::tests::fold::testIntegerSum_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::collection::tests::fold::testStringSum_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: Trino\""),

            // ForAll
            one("meta::pure::functions::collection::tests::forall::testforAllOnEmptySet_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'forAll_T_MANY__Function_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::forall::testforAllOnNonEmptySetIsFalse_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'forAll_T_MANY__Function_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::forall::testforAllOnNonEmptySetIsTrue_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'forAll_T_MANY__Function_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            // Get
            one("meta::pure::functions::collection::tests::get::testGet_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Trino\""),

            // Head
            one("meta::pure::functions::collection::tests::head::testHeadComplex_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::collection::tests::head::testHeadOnEmptySet_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_first' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::head::testHeadOnOneElement_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_first' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::head::testHeadSimple_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_first' (state: [Select, false]) is not supported yet\""),

            // IndexOf
            one("meta::pure::functions::collection::tests::indexof::testIndexOfOneElement_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::collection::tests::indexof::testIndexOf_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_position' (state: [Select, false]) is not supported yet\""),

            // Init
            one("meta::pure::functions::collection::tests::init::testInitOnEmptySet_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_init' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::init::testInitOneElement_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_init' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::init::testInit_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_init' (state: [Select, false]) is not supported yet\""),

            // Keys
            one("meta::pure::functions::collection::tests::keys::testKeys_Function_1__Boolean_1_", "\"[unsupported-api] The function 'keys' (state: [Select, false]) is not supported yet\""),

            // Last
            one("meta::pure::functions::collection::tests::last::testLastFromEmpty_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_last' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::last::testLastOfOneElementList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_last' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::last::testLast_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_last' (state: [Select, false]) is not supported yet\""),

            // Put
            one("meta::pure::functions::collection::tests::put::testPut_addsEntry_Function_1__Boolean_1_", "\"[unsupported-api] The function 'mapConcatenate' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::put::testPut_emptyMap_Function_1__Boolean_1_", "\"[unsupported-api] The function 'mapConcatenate' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::put::testPut_overridesEntry_Function_1__Boolean_1_", "\"[unsupported-api] The function 'mapConcatenate' (state: [Select, false]) is not supported yet\""),

            // PutAll
            one("meta::pure::functions::collection::tests::putAll::testPutAll_emptyInputMap_Function_1__Boolean_1_", "\"[unsupported-api] The function 'mapConcatenate' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::putAll::testPutAll_emptyPutEntries_Function_1__Boolean_1_", "\"[unsupported-api] The function 'mapConcatenate' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::putAll::testPutAll_overridesExistingAndAddNew_Function_1__Boolean_1_", "\"[unsupported-api] The function 'mapConcatenate' (state: [Select, false]) is not supported yet\""),

            // RemoveDuplicates
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesEmptyListExplicit_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_distinct' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesPrimitiveNonStandardFunction_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesPrimitiveStandardFunctionExplicit_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesPrimitiveStandardFunctionMixedTypes_Function_1__Boolean_1_", "\"Any is not managed yet!\""),
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesPrimitiveStandardFunctionSimple_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_distinct' (state: [Select, false]) is not supported yet\""),

            // RemoveDuplicatesBy
            one("meta::pure::functions::collection::tests::removeDuplicatesBy::testRemoveDuplicatesByPrimitive_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            // Reverse
            one("meta::pure::functions::collection::tests::reverse::testReverseEmpty_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_reverse' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::reverse::testReverse_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_reverse' (state: [Select, false]) is not supported yet\""),

            // Slice
            one("meta::pure::functions::collection::tests::slice::testSliceEqualBounds_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_slice' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::slice::testSliceInList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_slice' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::slice::testSliceOnBounds_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_slice' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::slice::testSliceOnEmpty_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_slice' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::slice::testSliceOutOfBounds_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_slice' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::slice::testSlice_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_slice' (state: [Select, false]) is not supported yet\""),

            // Sort
            one("meta::pure::functions::collection::tests::sort::testMixedSortNoComparator_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_sort' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortNoComparator_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_sort' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortReversed_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortWithFunctionVariables_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortWithKey_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::sort::testSimpleSort_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::sort::testSortEmptySet_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_sort' (state: [Select, false]) is not supported yet\""),

            // Tail
            one("meta::pure::functions::collection::tests::tail::testTailOnEmptySet_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_tail' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::tail::testTailOneElement_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_tail' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::tail::testTail_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_tail' (state: [Select, false]) is not supported yet\""),

            // Take
            one("meta::pure::functions::collection::tests::take::testTakeExceedsSizeOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::take::testTakeExceedsSizeOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::take::testTakeInList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::take::testTakeManyOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::take::testTakeManyOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::take::testTakeNegativeOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::take::testTakeNegativeOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::take::testTakeOneOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::take::testTakeOneOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::take::testTakeZeroOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::take::testTakeZeroOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\""),

            // Values
            one("meta::pure::functions::collection::tests::values::testValues_Function_1__Boolean_1_", "\"[unsupported-api] The function 'values' (state: [Select, false]) is not supported yet\""),

            // Zip
            one("meta::pure::functions::collection::tests::zip::testZipBothListsAreOfPairs_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::zip::testZipBothListsEmpty_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\""),
            one("meta::pure::functions::collection::tests::zip::testZipBothListsSameLength_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::zip::testZipFirstListEmpty_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\""),
            one("meta::pure::functions::collection::tests::zip::testZipFirstListLonger_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::zip::testZipFirstListsIsOfPairs_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::zip::testZipSecondListEmpty_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\""),
            one("meta::pure::functions::collection::tests::zip::testZipSecondListLonger_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::collection::tests::zip::testZipSecondListsIsOfPairs_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            // Date - Adjust
            one("meta::pure::functions::date::tests::testAdjustByDaysBigNumber_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testAdjustByDays_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testAdjustByHoursBigNumber_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testAdjustByHours_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testAdjustByMicrosecondsBigNumber_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testAdjustByMicroseconds_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testAdjustByMillisecondsBigNumber_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testAdjustByMilliseconds_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testAdjustByMinutesBigNumber_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testAdjustByMinutes_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testAdjustByMonthsBigNumber_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testAdjustByMonths_Function_1__Boolean_1_", "Date has no day: 2012-03"),
            one("meta::pure::functions::date::tests::testAdjustBySecondsBigNumber_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testAdjustBySeconds_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testAdjustByWeeksBigNumber_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testAdjustByWeeks_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testAdjustByYears_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\""),
            one("meta::pure::functions::date::tests::testAdjustReflectiveEvaluation_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::lang::eval(NativeFunction<{Date[1], Integer[1], DurationUnit[1]->Date[1]}>[1],StrictDate[1],Integer[1],DurationUnit[1])'"),

            // Date - DateDiff
            one("meta::pure::functions::date::tests::testDateDiffDays_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testDateDiffHours_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testDateDiffMilliseconds_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testDateDiffMinutes_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testDateDiffMonths_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testDateDiffSeconds_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testDateDiffWeeks_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testDateDiffWithDifferentTimeZones_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testDateDiffYears_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\""),

            // Date - DateFrom
            one("meta::pure::functions::date::tests::testDateFromDay_Function_1__Boolean_1_", "\"[unsupported-api] The function 'date' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::testDateFromHour_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'date_Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::date::tests::testDateFromMinute_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::date::tests::testDateFromMonth_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'date_Integer_1__Integer_1__Date_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::date::tests::testDateFromSecond_Function_1__Boolean_1_", "\"[unsupported-api] The function 'date' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::testDateFromSubSecond_Function_1__Boolean_1_", "\"[unsupported-api] The function 'date' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::testDateFromYear_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'date_Integer_1__Date_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            // Date - DatePart
            one("meta::pure::functions::date::tests::testDatePartTrivial_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testDatePartYearMonthOnly_Function_1__Boolean_1_", "Date has no day: 1973-11"),
            one("meta::pure::functions::date::tests::testDatePartYearOnly_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\""),
            one("meta::pure::functions::date::tests::testDatePart_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // Date - Parts
            one("meta::pure::functions::date::tests::testDayOfMonth_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testHasDay_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasDay_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::date::tests::testHasHour_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasHour_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::date::tests::testHasMinute_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasMinute_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::date::tests::testHasMonthReflect_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasMonth_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::date::tests::testHasMonth_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasMonth_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::date::tests::testHasSecond_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasSecond_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::date::tests::testHasSubsecondWithAtLeastPrecision_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasSubsecondWithAtLeastPrecision_Date_1__Integer_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::date::tests::testHasSubsecond_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasSubsecond_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::date::tests::testHour_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testMinute_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testMonthNumber_Function_1__Boolean_1_", "Date has no day: 2015-04"),
            one("meta::pure::functions::date::tests::testSecond_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testYeaReflect_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::date::tests::testYear_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\""),

            // If
            one("meta::pure::functions::lang::tests::if::testIfWithEvaluate_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::lang::tests::if::testIfWithFunctionCall_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::lang::tests::if::testMultiIf_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::lang::tests::if::testSimpleIf_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // Match
            one("meta::pure::functions::lang::tests::match::testMatchManyWithMany_Function_1__Boolean_1_", "\"Match only supports operands with multiplicity [1]..! Current operand : ['w', 'w', 'w']\""),
            one("meta::pure::functions::lang::tests::match::testMatchOneWithMany_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\""),
            one("meta::pure::functions::lang::tests::match::testMatchOneWithZeroOne_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\""),
            one("meta::pure::functions::lang::tests::match::testMatchOneWith_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\""),
            one("meta::pure::functions::lang::tests::match::testMatchWithExtraParam_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'match_Any_MANY__Function_$1_MANY$__P_o__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::lang::tests::match::testMatchWithExtraParamsAndFunctionsAsParam_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'match_Any_MANY__Function_$1_MANY$__P_o__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctionsAsParamManyMatch_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\""),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctionsAsParam_Function_1__Boolean_1_", "\"Cast exception: Literal cannot be cast to SemiStructuredPropertyAccess\""),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctionsManyMatch_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\""),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctions_Function_1__Boolean_1_", "\"Cast exception: Literal cannot be cast to SemiStructuredPropertyAccess\""),
            one("meta::pure::functions::lang::tests::match::testMatchWithMixedReturnType_Function_1__Boolean_1_", "\"type not supported: meta::pure::functions::lang::tests::model::LA_GeographicEntityType\""),
            one("meta::pure::functions::lang::tests::match::testMatchZeroWithMany_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\""),
            one("meta::pure::functions::lang::tests::match::testMatchZeroWithZero_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\""),
            one("meta::pure::functions::lang::tests::match::testMatch_Function_1__Boolean_1_", "\"type not supported: meta::pure::functions::lang::tests::match::MA_GeographicEntityType\""),

            // Math - Abs
            one("meta::pure::functions::math::tests::abs::testBigFloatAbs_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::abs::testBigIntAbs_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::abs::testDecimalAbs_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::abs::testFloatAbs_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::abs::testIntAbs_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // Math - Ceiling
            one("meta::pure::functions::math::tests::ceiling::testDecimalCeiling_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::ceiling::testNegativeFloatCeiling_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::ceiling::testNegativeFloatWithZeroDecimalCeiling_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::ceiling::testNegativeIntegerCeiling_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::ceiling::testPositiveFloatCeiling_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::ceiling::testPositiveFloatWithZeroDecimalCeiling_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::ceiling::testPositiveIntegerCeiling_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // Math - Exp
            one("meta::pure::functions::math::tests::exp::testNumberExpLowPrecision_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::exp::testNumberExp_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::exp::testSimpleLowPrecision_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::exp::testSimple_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // Math - Floor
            one("meta::pure::functions::math::tests::floor::testDecimalFloor_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::floor::testNegativeFloatFloor_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::floor::testNegativeFloatWithZeroDecimalFloor_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::floor::testNegativeIntegerFloor_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::floor::testPositiveFloatFloor_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::floor::testPositiveFloatWithZeroDecimalFloor_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::floor::testPositiveIntegerFloor_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // Math - Log
            one("meta::pure::functions::math::tests::log::testNumberLogLowPrecision_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::log::testNumberLog_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::log::testSimpleLowPrecision_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::log::testSimple_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // Math - Log10
            one("meta::pure::functions::math::tests::log10::testNumberLog10_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::log10::testSimpleLog10_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // Math - Mod
            one("meta::pure::functions::math::tests::mod::testModInEval_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::mod::testModWithNegativeNumbers_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::mod::testMod_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // Math - Pow
            one("meta::pure::functions::math::tests::pow::testComplexPow_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::pow::testNumberPow_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::pow::testSimplePow_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // Math - Rem
            one("meta::pure::functions::math::tests::rem::testRemInEvalWithFloat_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::rem::testRemInEvalWithMixedIntegersAndFloats_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::rem::testRemInEvalWithNegativeNumbers_Function_1__Boolean_1_", "Unused format args. [3] arguments provided to expression"),
            one("meta::pure::functions::math::tests::rem::testRemInEval_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::rem::testRemWithDecimals_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::rem::testRemWithFloats_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::rem::testRemWithMixedIntegersAndFloats_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::rem::testRemWithNegativeFloats_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::rem::testRemWithNegativeIntegers_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::rem::testRem_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // Math - Round
            one("meta::pure::functions::math::tests::round::testDecimalRound_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::round::testFloatRoundWithScale_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::round::testNegativeFloatRoundDown_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::round::testNegativeFloatRoundHalfEvenDown_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::round::testNegativeFloatRoundHalfEvenUp_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::round::testNegativeFloatRoundUp_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::round::testNegativeFloatWithZeroDecimalRound_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::round::testNegativeIntegerRound_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::round::testPositiveFloatRoundDown_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::round::testPositiveFloatRoundHalfEvenDown_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::round::testPositiveFloatRoundHalfEvenUp_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::round::testPositiveFloatRoundUp_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::round::testPositiveFloatWithZeroDecimalRound_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::round::testPositiveIntegerRound_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // Math - Sign
            one("meta::pure::functions::math::tests::sign::testSign_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // Math - ToDecimal
            one("meta::pure::functions::math::tests::toDecimal::testDecimalToDecimal_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toDecimal' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::toDecimal::testDoubleToDecimal_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toDecimal' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::toDecimal::testIntToDecimal_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toDecimal' (state: [Select, false]) is not supported yet\""),

            // Math - ToFloat
            one("meta::pure::functions::math::tests::toFloat::testDecimalToFloat_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toFloat' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::toFloat::testDoubleToFloat_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toFloat' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::toFloat::testIntToFloat_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toFloat' (state: [Select, false]) is not supported yet\""),

            // Math - Trigonometry
            one("meta::pure::functions::math::tests::trigonometry::testArcCosineEval_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::trigonometry::testArcCosine_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::trigonometry::testArcSineEval_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::trigonometry::testArcSine_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::trigonometry::testArcTangent2Eval_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::trigonometry::testArcTangent2_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::trigonometry::testArcTangentEval_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::trigonometry::testArcTangent_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::trigonometry::testCoTangentEval_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::trigonometry::testCoTangent_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::trigonometry::testCosEval_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::trigonometry::testCosine_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::trigonometry::testSineEval_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::trigonometry::testSine_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::trigonometry::testTangentEval_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::trigonometry::testTangent_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // Math - Roots
            one("meta::pure::functions::math::tests::testCubeRootEval_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::testCubeRoot_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::testSquareRootEval_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::math::tests::testSquareRoot_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // String - Contains
            one("meta::pure::functions::string::tests::contains::testFalseContains_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::contains::testTrueContains_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // String - EndsWith
            one("meta::pure::functions::string::tests::endswith::testFalseEndsWith_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::endswith::testTrueEndsWith_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // String - Format
            one("meta::pure::functions::string::tests::format::testFormatBoolean_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatDate_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatFloatWithRounding_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatFloatWithTruncation_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatFloatWithZeroPadding_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatFloat_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatInEval_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatInEvaluate_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::string::tests::format::testFormatIntegerWithZeroPadding_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatInteger_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::string::tests::format::testFormatPair_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::string::tests::format::testFormatRepr_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatString_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testSimpleFormatDate_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            // String - IndexOf
            one("meta::pure::functions::string::tests::indexOf::testFromIndex_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'indexOf_String_1__String_1__Integer_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::indexOf::testSimple_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // String - JoinStrings
            one("meta::pure::functions::string::tests::joinStrings::testJoinStringsNoStrings_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_tail' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::joinStrings::testJoinStringsSingleString_Function_1__Boolean_1_", "\"The database type 'Trino' is not supported yet!\""),
            one("meta::pure::functions::string::tests::joinStrings::testJoinStringsUsingGenericArrow_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::joinStrings::testJoinStrings_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // String - Length
            one("meta::pure::functions::string::tests::length::testEmptyLength_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::length::testLength_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // String - ParseBoolean
            one("meta::pure::functions::string::tests::parseBoolean::testParseFalse_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseBoolean' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::parseBoolean::testParseTrue_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseBoolean' (state: [Select, false]) is not supported yet\""),

            // String - ParseDate
            one("meta::pure::functions::string::tests::parseDate::testParseDateTypes_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::parseDate::testParseDateWithTimezone_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::parseDate::testParseDateWithZ_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::parseDate::testParseDate_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // String - ParseDecimal
            one("meta::pure::functions::string::tests::parseDecimal::testParseDecimalWithPrecisionScale_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseDecimal' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::parseDecimal::testParseDecimal_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseDecimal' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::parseDecimal::testParseZero_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseDecimal' (state: [Select, false]) is not supported yet\""),

            // String - ParseFloat
            one("meta::pure::functions::string::tests::parseFloat::testParseFloat_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::parseFloat::testParseZero_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // String - ParseInteger
            one("meta::pure::functions::string::tests::parseInteger::testParseInteger_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // String - Replace
            one("meta::pure::functions::string::tests::replace::testReplace_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // String - Reverse
            one("meta::pure::functions::string::tests::reverse::testReverseString_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // String - Split
            one("meta::pure::functions::string::tests::split::testSplitWithNoSplit_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'split_String_1__String_1__String_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::split::testSplit_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'split_String_1__String_1__String_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            // String - StartsWith
            one("meta::pure::functions::string::tests::startswith::testFalseStartsWith_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::startswith::testTrueStartsWith_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // String - Substring
            one("meta::pure::functions::string::tests::substring::testStartEnd_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::substring::testStart_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // String - ToString
            one("meta::pure::functions::string::tests::toString::testBooleanToString_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::toString::testClassToString_Function_1__Boolean_1_", "\"Match failure: ClassInstanceHolderObject instanceOf ClassInstanceHolder\""),
            one("meta::pure::functions::string::tests::toString::testComplexClassToString_Function_1__Boolean_1_", "\"type not supported: meta::pure::functions::string::tests::toString::ErrorType\""),
            one("meta::pure::functions::string::tests::toString::testDateTimeToString_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::toString::testDateTimeWithTimezoneToString_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::toString::testDateToString_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::toString::testEnumerationToString_Function_1__Boolean_1_", "\"Match failure: ClassInstanceHolderObject instanceOf ClassInstanceHolder\""),
            one("meta::pure::functions::string::tests::toString::testFloatToStringWithExcessTrailingZeros_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::toString::testFloatToStringWithNegativeExponent_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::toString::testFloatToStringWithNoLeadingZero_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::toString::testFloatToStringWithPositiveExponent_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::toString::testFloatToString_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::toString::testIntegerToString_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::toString::testListToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::string::tests::toString::testPairCollectionToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 2 to multiplicity [1]\""),
            one("meta::pure::functions::string::tests::toString::testPairToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::string::tests::toString::testPersonToString_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::toString::testSimpleDateToString_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::toString::testStringToString_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),

            // String - ToLower/ToUpper/Trim
            one("meta::pure::functions::string::tests::tolower::testToLower_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::toupper::testToUpper_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::trim::testLTrim_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::trim::testRTrim_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present"),
            one("meta::pure::functions::string::tests::trim::testTrim_Function_1__Boolean_1_", "com.zaxxer.hikari.pool.HikariPool$PoolInitializationException: Failed to initialize pool: java.lang.RuntimeException: java.util.NoSuchElementException: No value present")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Trino).getFirst())
        );
    }

    @Override
    public MutableList<ExclusionSpecification> expectedFailures()
    {
        return expectedFailures;
    }

    @Override
    public ReportScope getReportScope()
    {
        return reportScope;
    }

    @Override
    public Adapter getAdapter()
    {
        return adapter;
    }

    @Override
    public String getPlatform()
    {
        return platform;
    }
}
