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
            one("meta::pure::functions::collection::tests::contains::testContainsPrimitive_Function_1__Boolean_1_", "line 1:26: IN value and list items must be the same type or coercible to a common type. Cannot find common type between integer and varchar(1), all types (without duplicates): [integer, varchar(1), boolean, date]"),
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
            one("meta::pure::functions::collection::tests::indexof::testIndexOf_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_position' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::collection::tests::indexof::testIndexOfOneElement_Function_1__Boolean_1_", "\"\nexpected: 0\nactual:   1\""),

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
            one("meta::pure::functions::date::tests::testAdjustByDaysBigNumber_Function_1__Boolean_1_", "java.sql.SQLException: Invalid column index: 1"),
            one("meta::pure::functions::date::tests::testAdjustByHoursBigNumber_Function_1__Boolean_1_", "integer overflow"),
            one("meta::pure::functions::date::tests::testAdjustByMicroseconds_Function_1__Boolean_1_", "'microsecond' is not a valid TIMESTAMP field"),
            one("meta::pure::functions::date::tests::testAdjustByMicrosecondsBigNumber_Function_1__Boolean_1_", "'microsecond' is not a valid TIMESTAMP field"),
            one("meta::pure::functions::date::tests::testAdjustByMillisecondsBigNumber_Function_1__Boolean_1_", "integer overflow"),
            one("meta::pure::functions::date::tests::testAdjustByMinutesBigNumber_Function_1__Boolean_1_", "integer overflow"),
            one("meta::pure::functions::date::tests::testAdjustByMonths_Function_1__Boolean_1_", "Date has no day: 2012-03"),
            one("meta::pure::functions::date::tests::testAdjustByMonthsBigNumber_Function_1__Boolean_1_", "java.sql.SQLException"),
            one("meta::pure::functions::date::tests::testAdjustBySecondsBigNumber_Function_1__Boolean_1_", "integer overflow"),
            one("meta::pure::functions::date::tests::testAdjustByWeeksBigNumber_Function_1__Boolean_1_", "java.sql.SQLException"),
            one("meta::pure::functions::date::tests::testAdjustByYears_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\""),

            // Date - DateDiff
            one("meta::pure::functions::date::tests::testDateDiffWeeks_Function_1__Boolean_1_", "\"\nexpected: 1\nactual:   0\""),
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
            one("meta::pure::functions::date::tests::testDatePartYearMonthOnly_Function_1__Boolean_1_", "Date has no day: 1973-11"),
            one("meta::pure::functions::date::tests::testDatePartYearOnly_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\""),

            // Date - Parts
            one("meta::pure::functions::date::tests::testHasDay_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasDay_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::date::tests::testHasHour_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasHour_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::date::tests::testHasMinute_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasMinute_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::date::tests::testHasMonth_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasMonth_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::date::tests::testHasSecond_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasSecond_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::date::tests::testHasSubsecondWithAtLeastPrecision_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasSubsecondWithAtLeastPrecision_Date_1__Integer_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::date::tests::testHasSubsecond_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasSubsecond_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::date::tests::testHour_Function_1__Boolean_1_", "\"\nexpected: 17\nactual:   0\""),
            one("meta::pure::functions::date::tests::testMinute_Function_1__Boolean_1_", "\"\nexpected: 9\nactual:   0\""),
            one("meta::pure::functions::date::tests::testMonthNumber_Function_1__Boolean_1_", "Date has no day: 2015-04"),
            one("meta::pure::functions::date::tests::testYear_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\""),

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

            // Math - Pow
            one("meta::pure::functions::math::tests::pow::testNumberPow_Function_1__Boolean_1_", "\"\nexpected: 9.0\nactual:   27.0\""),

            // Math - Rem
            one("meta::pure::functions::math::tests::rem::testRemWithDecimals_Function_1__Boolean_1_", "\"\nexpected: 0.14D\nactual:   0.14\""),

            // Math - Round
            one("meta::pure::functions::math::tests::round::testNegativeFloatRoundHalfEvenUp_Function_1__Boolean_1_", "\"\nexpected: -16\nactual:   -17\""),
            one("meta::pure::functions::math::tests::round::testPositiveFloatRoundHalfEvenDown_Function_1__Boolean_1_", "\"\nexpected: 16\nactual:   17\""),

            // Math - ToDecimal
            one("meta::pure::functions::math::tests::toDecimal::testDecimalToDecimal_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toDecimal' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::toDecimal::testDoubleToDecimal_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toDecimal' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::toDecimal::testIntToDecimal_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toDecimal' (state: [Select, false]) is not supported yet\""),

            // Math - ToFloat
            one("meta::pure::functions::math::tests::toFloat::testDecimalToFloat_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toFloat' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::toFloat::testDoubleToFloat_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toFloat' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::toFloat::testIntToFloat_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toFloat' (state: [Select, false]) is not supported yet\""),

            // String - Format
            one("meta::pure::functions::string::tests::format::testFormatBoolean_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatDate_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatFloatWithRounding_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatFloatWithTruncation_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatFloatWithZeroPadding_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatFloat_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatIntegerWithZeroPadding_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatInteger_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::string::tests::format::testFormatPair_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::string::tests::format::testFormatRepr_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testFormatString_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::format::testSimpleFormatDate_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            // String - IndexOf
            one("meta::pure::functions::string::tests::indexOf::testFromIndex_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'indexOf_String_1__String_1__Integer_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::indexOf::testSimple_Function_1__Boolean_1_", "\"\nexpected: 4\nactual:   5\""),

            // String - JoinStrings
            one("meta::pure::functions::string::tests::joinStrings::testJoinStrings_Function_1__Boolean_1_", "\"\nexpected: '[a,b,c]'\nactual:   '[,a,b,c,]'\""),
            one("meta::pure::functions::string::tests::joinStrings::testJoinStringsNoStrings_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_tail' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::joinStrings::testJoinStringsSingleString_Function_1__Boolean_1_", "\"The database type 'Trino' is not supported yet!\""),
            one("meta::pure::functions::string::tests::joinStrings::testJoinStringsUsingGenericArrow_Function_1__Boolean_1_", "\"\nexpected: '[a,b,c]'\nactual:   '[,a,b,c,]'\""),

            // String - ParseBoolean
            one("meta::pure::functions::string::tests::parseBoolean::testParseFalse_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseBoolean' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::parseBoolean::testParseTrue_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseBoolean' (state: [Select, false]) is not supported yet\""),

            // String - ParseDate
            one("meta::pure::functions::string::tests::parseDate::testParseDate_Function_1__Boolean_1_", "is malformed at \"T10:01:35.231\""),
            one("meta::pure::functions::string::tests::parseDate::testParseDateTypes_Function_1__Boolean_1_", "is malformed at \"T00:00:00.000000\""),
            one("meta::pure::functions::string::tests::parseDate::testParseDateWithTimezone_Function_1__Boolean_1_", "is malformed at \"T10:01:35.231-0500\""),
            one("meta::pure::functions::string::tests::parseDate::testParseDateWithZ_Function_1__Boolean_1_", "is malformed at \"T10:01:35.231Z\""),

            // String - ParseDecimal
            one("meta::pure::functions::string::tests::parseDecimal::testParseDecimalWithPrecisionScale_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseDecimal' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::parseDecimal::testParseDecimal_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseDecimal' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::tests::parseDecimal::testParseZero_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseDecimal' (state: [Select, false]) is not supported yet\""),

            // String - ParseInteger
            one("meta::pure::functions::string::tests::parseInteger::testParseInteger_Function_1__Boolean_1_", "Cannot cast '9999999999999992' to INT"),

            // String - Split
            one("meta::pure::functions::string::tests::split::testSplitWithNoSplit_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'split_String_1__String_1__String_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),
            one("meta::pure::functions::string::tests::split::testSplit_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'split_String_1__String_1__String_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            // String - Substring
            one("meta::pure::functions::string::tests::substring::testStartEnd_Function_1__Boolean_1_", "\"\nexpected: 'the quick brown fox jumps over the lazy dog'\nactual:   ''\""),
            one("meta::pure::functions::string::tests::substring::testStart_Function_1__Boolean_1_", "\"\nexpected: 'the quick brown fox jumps over the lazy dog'\nactual:   ''\""),

            // String - ToString
            one("meta::pure::functions::string::tests::toString::testClassToString_Function_1__Boolean_1_", "\"Match failure: ClassInstanceHolderObject instanceOf ClassInstanceHolder\""),
            one("meta::pure::functions::string::tests::toString::testComplexClassToString_Function_1__Boolean_1_", "\"type not supported: meta::pure::functions::string::tests::toString::ErrorType\""),
            one("meta::pure::functions::string::tests::toString::testDateTimeToString_Function_1__Boolean_1_", "\"\nexpected: '2014-01-01T00:00:00.000+0000'\nactual:   '2014-01-01 00:00:00.000'\""),
            one("meta::pure::functions::string::tests::toString::testDateTimeWithTimezoneToString_Function_1__Boolean_1_", "\"\nexpected: '2014-01-01T00:00:00.0000+0000'\nactual:   '2014-01-01 00:00:00.0000'\""),
            one("meta::pure::functions::string::tests::toString::testDateToString_Function_1__Boolean_1_", "Date has no day: 2014-01"),
            one("meta::pure::functions::string::tests::toString::testEnumerationToString_Function_1__Boolean_1_", "\"Match failure: ClassInstanceHolderObject instanceOf ClassInstanceHolder\""),
            one("meta::pure::functions::string::tests::toString::testListToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::string::tests::toString::testPairCollectionToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 2 to multiplicity [1]\""),
            one("meta::pure::functions::string::tests::toString::testPairToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),
            one("meta::pure::functions::string::tests::toString::testPersonToString_Function_1__Boolean_1_", "\"Assert failed\""),
            one("meta::pure::functions::string::tests::toString::testSimpleDateToString_Function_1__Boolean_1_", "\"\nexpected: '2014-01-02T01:54:27.352+0000'\nactual:   '2014-01-02 01:54:27.352'\""),
            // Error-case tests from legend-pure2 migration (mirrored from H2 defaults)
            one("meta::pure::functions::collection::tests::at::testAtError_Function_1__Boolean_1_", "->at(...) function is supported only after direct access of 1->MANY properties"),
            one("meta::pure::functions::collection::tests::slice::testSliceError_Function_1__Boolean_1_", "The function 'array_slice' (state: [Select, false]) is not supported yet"),
            one("meta::pure::functions::date::tests::testDayOfMonthError_Function_1__Boolean_1_", "Ensure the target system understands Year or Year-month semantic."),
            one("meta::pure::functions::date::tests::testHourError_Function_1__Boolean_1_", "Ensure the target system understands Year or Year-month semantic."),
            one("meta::pure::functions::date::tests::testMinuteError_Function_1__Boolean_1_", "Ensure the target system understands Year or Year-month semantic."),
            one("meta::pure::functions::date::tests::testNewDateError_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'date_Integer_1__Integer_1__Date_1_'"),
            one("meta::pure::functions::date::tests::testSecondError_Function_1__Boolean_1_", "Ensure the target system understands Year or Year-month semantic."),
            one("meta::pure::functions::math::tests::rem::testRemError_Function_1__Boolean_1_", "Unexpected error executing function with params [Anonymous_Lambda]"),
            one("meta::pure::functions::math::tests::testSquareRootError_Function_1__Boolean_1_", "No error was thrown"),
            one("meta::pure::functions::math::tests::trigonometry::testArcCosineError_Function_1__Boolean_1_", "No error was thrown"),
            one("meta::pure::functions::math::tests::trigonometry::testArcSineError_Function_1__Boolean_1_", "No error was thrown")
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
