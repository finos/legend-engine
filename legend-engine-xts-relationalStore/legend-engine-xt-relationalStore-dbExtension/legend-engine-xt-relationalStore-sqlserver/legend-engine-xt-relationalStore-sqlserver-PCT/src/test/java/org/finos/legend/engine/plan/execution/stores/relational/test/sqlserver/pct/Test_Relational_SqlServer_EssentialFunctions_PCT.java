// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.sqlserver.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.pure.runtime.testConnection.CoreExternalTestConnectionCodeRepositoryProvider;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalSqlServerCodeRepositoryProvider;
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_SqlServer_EssentialFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = PlatformCodeRepositoryProvider.essentialFunctions;
    private static final Adapter adapter = CoreRelationalSqlServerCodeRepositoryProvider.sqlserverAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Add
            one("meta::pure::functions::collection::tests::add::testAddWithOffset_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'add_T_MANY__Integer_1__T_1__T_$1_MANY$_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::add::testAdd_Function_1__Boolean_1_", "[unsupported-api] The function 'array_append' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),

            // Concatenate
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateMixedType_Function_1__Boolean_1_", "\"Any is not managed yet!\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateSimple_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_concatenate' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateTypeInference_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_concatenate' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsInvestigation),

            // Contains
            one("meta::pure::functions::collection::tests::contains::testContainsNonPrimitive_Function_1__Boolean_1_", "\"Parameter to IN operation isn't a literal!\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::contains::testContainsPrimitive_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'in'.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::contains::testContainsWithFunction_Function_1__Boolean_1_", "no viable alternative at input '->meta::pure::functions::collection::contains(meta::pure::functions::collection::tests::contains::ClassWithoutEquality.all()->meta::pure::functions::multiplicity::toOne(),meta::pure::functions::collection::tests::contains::comparator(a:meta::pure::functions::collection::tests::contains::ClassWithoutEquality[1],'", AdapterQualifier.needsInvestigation),

            // Drop
            one("meta::pure::functions::collection::tests::drop::testDropExceedsSizeOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::drop::testDropExceedsSizeOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::drop::testDropInList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::drop::testDropManyOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::drop::testDropNegativeOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::drop::testDropNegativeOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::drop::testDropOneOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::drop::testDropOneOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::drop::testDropZeroOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::drop::testDropZeroOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::drop::testDropManyOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_drop' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            // Exists
            one("meta::pure::functions::collection::tests::exists::testExists_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),

            // Find
            one("meta::pure::functions::collection::tests::find::testFindInstance_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/essential/collection/iteration/find.pure:38cc38-42); error compiling generated Java code:", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::find::testFindLiteralFromVar_Function_1__Boolean_1_", "Function does not exist 'meta::pure::functions::collection::find(String[3],LambdaFunction<{String[1]->Boolean[1]}>[1])'", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::find::testFindLiteral_Function_1__Boolean_1_", "Function does not exist 'meta::pure::functions::collection::find(String[4],LambdaFunction<{String[1]->Boolean[1]}>[1])'", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::find::testFindUsingVarForFunction_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/essential/collection/iteration/find.pure:47cc38-42); error compiling generated Java code:", AdapterQualifier.needsInvestigation),

            // Fold
            one("meta::pure::functions::collection::tests::fold::testFoldCollectionAccumulator_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: SqlServer", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::fold::testFoldEmptyListAndEmptyIdentity_Function_1__Boolean_1_", "Any is not managed yet!", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::fold::testFoldEmptyListAndNonEmptyIdentity_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: ", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::fold::testFoldFiltering_Function_1__Boolean_1_", "Function does not exist 'meta::pure::functions::lang::copy(FO_Person[1],String[1],KeyExpression[1])'", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::fold::testFoldMixedAccumulatorTypes_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: SqlServer", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::fold::testFoldToMany_Function_1__Boolean_1_", "Function does not exist 'meta::pure::functions::lang::copy(FO_Person[1],String[1],KeyExpression[1])'", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::fold::testFoldWithEmptyAccumulator_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: SqlServer", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::fold::testFoldWithSingleValue_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: SqlServer", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::fold::testFold_Function_1__Boolean_1_", "Function does not exist 'meta::pure::functions::lang::copy(FO_Person[1],String[1],KeyExpression[1])'", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::fold::testIntegerSum_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: SqlServer", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::fold::testStringSum_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type: SqlServer", AdapterQualifier.needsImplementation),

            one("meta::pure::functions::collection::tests::get::testGet_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: SqlServer\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::keys::testKeys_Function_1__Boolean_1_", "\"[unsupported-api] The function 'keys' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::put::testPut_addsEntry_Function_1__Boolean_1_", "\"[unsupported-api] The function 'mapConcatenate' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::put::testPut_emptyMap_Function_1__Boolean_1_", "\"[unsupported-api] The function 'mapConcatenate' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::put::testPut_overridesEntry_Function_1__Boolean_1_", "\"[unsupported-api] The function 'mapConcatenate' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::putAll::testPutAll_emptyInputMap_Function_1__Boolean_1_", "\"[unsupported-api] The function 'mapConcatenate' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::putAll::testPutAll_emptyPutEntries_Function_1__Boolean_1_", "\"[unsupported-api] The function 'mapConcatenate' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::putAll::testPutAll_overridesExistingAndAddNew_Function_1__Boolean_1_", "\"[unsupported-api] The function 'mapConcatenate' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::values::testValues_Function_1__Boolean_1_", "\"[unsupported-api] The function 'values' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            // ForAll
            one("meta::pure::functions::collection::tests::forall::testforAllOnEmptySet_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'forAll_T_MANY__Function_1__Boolean_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::forall::testforAllOnNonEmptySetIsFalse_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'forAll_T_MANY__Function_1__Boolean_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::forall::testforAllOnNonEmptySetIsTrue_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'forAll_T_MANY__Function_1__Boolean_1_'.", AdapterQualifier.needsImplementation),

            // IndexOf
            one("meta::pure::functions::collection::tests::indexof::testIndexOfOneElement_Function_1__Boolean_1_", "\"\nexpected: 0\nactual:   1\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::indexof::testIndexOf_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_position' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            // Init
            one("meta::pure::functions::collection::tests::init::testInitOnEmptySet_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_init' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::init::testInitOneElement_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_init' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::init::testInit_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_init' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            // Head
            one("meta::pure::functions::collection::tests::head::testHeadComplex_Function_1__Boolean_1_", "Cannot cast a collection of size 0 to multiplicity [1]", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::head::testHeadOnEmptySet_Function_1__Boolean_1_", "[unsupported-api] The function 'array_first' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::head::testHeadOnOneElement_Function_1__Boolean_1_", "[unsupported-api] The function 'array_first' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::head::testHeadSimple_Function_1__Boolean_1_", "[unsupported-api] The function 'array_first' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),

            // Last
            one("meta::pure::functions::collection::tests::last::testLastFromEmpty_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_last' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::last::testLastOfOneElementList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_last' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::last::testLast_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_last' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            // Pow
            one("meta::pure::functions::math::tests::pow::testNumberPow_Function_1__Boolean_1_", "\"\nexpected: 9.0\nactual:   27.0\"", AdapterQualifier.needsInvestigation),

            // Reverse
            one("meta::pure::functions::collection::tests::reverse::testReverseEmpty_Function_1__Boolean_1_", "[unsupported-api] The function 'array_reverse' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::reverse::testReverse_Function_1__Boolean_1_", "[unsupported-api] The function 'array_reverse' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),

            // Slice
            one("meta::pure::functions::collection::tests::slice::testSliceEqualBounds_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_slice' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::slice::testSliceInList_Function_1__Boolean_1_", "[unsupported-api] The function 'array_slice' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::slice::testSliceOnBounds_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_slice' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::slice::testSliceOutOfBounds_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_slice' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::slice::testSlice_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_slice' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::slice::testSliceOnEmpty_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_slice' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            // Sort
            one("meta::pure::functions::collection::tests::sort::testMixedSortNoComparator_Function_1__Boolean_1_", "[unsupported-api] The function 'array_sort' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortNoComparator_Function_1__Boolean_1_", "[unsupported-api] The function 'array_sort' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortReversed_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortWithFunctionVariables_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortWithKey_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::sort::testSimpleSort_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::sort::testSortEmptySet_Function_1__Boolean_1_", "[unsupported-api] The function 'array_sort' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),

            // Tail
            one("meta::pure::functions::collection::tests::tail::testTailOnEmptySet_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_tail' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::tail::testTailOneElement_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_tail' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::tail::testTail_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_tail' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            // Take
            one("meta::pure::functions::collection::tests::take::testTakeExceedsSizeOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::take::testTakeOneOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::take::testTakeManyOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::take::testTakeZeroOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::take::testTakeExceedsSizeOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::take::testTakeInList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::take::testTakeManyOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::take::testTakeNegativeOnEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::take::testTakeNegativeOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::take::testTakeOneOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::take::testTakeZeroOnNonEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_take' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            // Zip
            one("meta::pure::functions::collection::tests::zip::testZipBothListsAreOfPairs_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipBothListsEmpty_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipBothListsSameLength_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::zip::testZipFirstListEmpty_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipFirstListLonger_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::zip::testZipFirstListsIsOfPairs_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipSecondListEmpty_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipSecondListLonger_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::zip::testZipSecondListsIsOfPairs_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'.", AdapterQualifier.needsInvestigation),


            // Format
            one("meta::pure::functions::string::tests::format::testFormatBoolean_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::format::testFormatDate_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::format::testFormatFloatWithRounding_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::format::testFormatFloatWithTruncation_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::format::testFormatFloatWithZeroPadding_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::format::testFormatFloat_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::format::testFormatInEval_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 1 where the collection is of size 1\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::format::testFormatInEvaluate_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::format::testFormatIntegerWithZeroPadding_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::format::testFormatInteger_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::format::testFormatList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::format::testFormatPair_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::format::testFormatRepr_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::format::testFormatString_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::format::testSimpleFormatDate_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.needsImplementation),

            // Contains
            one("meta::pure::functions::string::tests::contains::testFalseContains_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'like'.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::contains::testTrueContains_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'like'.", AdapterQualifier.needsInvestigation),

            // EndsWith
            one("meta::pure::functions::string::tests::endswith::testFalseEndsWith_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'like'.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::endswith::testTrueEndsWith_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'like'.", AdapterQualifier.needsInvestigation),

            // JoinStrings
            one("meta::pure::functions::string::tests::joinStrings::testJoinStringsNoStrings_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_tail' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::joinStrings::testJoinStringsSingleString_Function_1__Boolean_1_", "\"The database type 'SqlServer' is not supported yet!\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::joinStrings::testJoinStringsUsingGenericArrow_Function_1__Boolean_1_", "\"\nexpected: '[a,b,c]'\nactual:   '[,a,b,c,]'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::joinStrings::testJoinStrings_Function_1__Boolean_1_", "\"\nexpected: '[a,b,c]'\nactual:   '[,a,b,c,]'\"", AdapterQualifier.needsInvestigation),

            // Split
            one("meta::pure::functions::string::tests::split::testSplitWithNoSplit_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'split_String_1__String_1__String_MANY_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::split::testSplit_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'split_String_1__String_1__String_MANY_'.", AdapterQualifier.needsImplementation),

            // Substring
            one("meta::pure::functions::string::tests::substring::testStartEnd_Function_1__Boolean_1_", "\"\nexpected: 'the quick brown fox jumps over the lazy dog'\nactual:   'the quick brown fox jumps over the lazy do'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::substring::testStart_Function_1__Boolean_1_", "\"\nexpected: 'the quick brown fox jumps over the lazy dog'\nactual:   'the quick brown fox jumps over the lazy do'\"", AdapterQualifier.needsInvestigation),

            // ToString
            one("meta::pure::functions::string::tests::toString::testClassToString_Function_1__Boolean_1_", "Match failure: ClassInstanceHolderObject instanceOf ClassInstanceHolder", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testComplexClassToString_Function_1__Boolean_1_", "type not supported: meta::pure::functions::string::tests::toString::ErrorType", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testDateTimeToString_Function_1__Boolean_1_", "\"\nexpected: '2014-01-01T00:00:00.000+0000'\nactual:   '2014-01-01 00:00:00.000'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testDateTimeWithTimezoneToString_Function_1__Boolean_1_", "\"\nexpected: '2014-01-01T00:00:00.0000+0000'\nactual:   '2014-01-01 00:00:00.0000'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testDateToString_Function_1__Boolean_1_", "Date has no day: 2014-01", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::toString::testEnumerationToString_Function_1__Boolean_1_", "\"Match failure: ClassInstanceHolderObject instanceOf ClassInstanceHolder\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testListToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testPairCollectionToString_Function_1__Boolean_1_", "Cannot cast a collection of size 2 to multiplicity [1]", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testPairToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testPersonToString_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'like'.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testSimpleDateToString_Function_1__Boolean_1_", "\"\nexpected: '2014-01-02T01:54:27.352+0000'\nactual:   '2014-01-02 01:54:27.352'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testStringToString_Function_1__Boolean_1_", "\"\nexpected: 'the quick brown fox jumps over the lazy dog'\nactual:   'the quick brown fox jumps over'\"", AdapterQualifier.needsInvestigation),

            one("meta::pure::functions::string::tests::format::testFormatInEval_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'.", AdapterQualifier.needsImplementation),

            // Mod
            one("meta::pure::functions::math::tests::mod::testModInEval_Function_1__Boolean_1_", "\"Unused format args. [3] arguments provided to expression \"%s %% %s\"\""),
            one("meta::pure::functions::math::tests::mod::testModWithNegativeNumbers_Function_1__Boolean_1_", "\"\nexpected: 3\nactual:   -2\""),

            //Pow
            one("meta::pure::functions::math::tests::pow::testComplexPow_Function_1__Boolean_1_", "\"\nexpected: 16.0\nactual:   16\""),
            one("meta::pure::functions::math::tests::pow::testSimplePow_Function_1__Boolean_1_", "\"\nexpected: 4.0\nactual:   4\""),

            // Rem
            one("meta::pure::functions::math::tests::rem::testRemInEvalWithFloat_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'mod' is not a recognized built-in function name.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::rem::testRemInEvalWithMixedIntegersAndFloats_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'mod' is not a recognized built-in function name."),
            one("meta::pure::functions::math::tests::rem::testRemInEvalWithNegativeNumbers_Function_1__Boolean_1_", "\"Unused format args. [3] arguments provided to expression \"mod(%s,%s)\"\""),
            one("meta::pure::functions::math::tests::rem::testRemInEval_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'mod' is not a recognized built-in function name."),
            one("meta::pure::functions::math::tests::rem::testRemWithDecimals_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'mod' is not a recognized built-in function name."),
            one("meta::pure::functions::math::tests::rem::testRemWithFloats_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'mod' is not a recognized built-in function name."),
            one("meta::pure::functions::math::tests::rem::testRemWithMixedIntegersAndFloats_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'mod' is not a recognized built-in function name."),
            one("meta::pure::functions::math::tests::rem::testRemWithNegativeFloats_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'mod' is not a recognized built-in function name."),
            one("meta::pure::functions::math::tests::rem::testRemWithNegativeIntegers_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'mod' is not a recognized built-in function name."),
            one("meta::pure::functions::math::tests::rem::testRem_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'mod' is not a recognized built-in function name."),

            // Round
            one("meta::pure::functions::math::tests::round::testDecimalRound_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"round(%s, 0)\"\""),
            one("meta::pure::functions::math::tests::round::testFloatRoundWithScale_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"round(%s, 0)\"\""),
            one("meta::pure::functions::math::tests::round::testPositiveFloatRoundHalfEvenDown_Function_1__Boolean_1_", "\"\nexpected: 16\nactual:   17\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::round::testNegativeFloatRoundHalfEvenUp_Function_1__Boolean_1_", "\"\nexpected: -16\nactual:   -17\"", AdapterQualifier.needsInvestigation),

            // ToDecimal
            one("meta::pure::functions::math::tests::toDecimal::testDecimalToDecimal_Function_1__Boolean_1_", "\"\nexpected: 3.8D\nactual:   4D\""),
            one("meta::pure::functions::math::tests::toDecimal::testDoubleToDecimal_Function_1__Boolean_1_", "\"\nexpected: 3.8D\nactual:   4D\""),

            //Trigonometry
            one("meta::pure::functions::math::tests::testCubeRootEval_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'cbrt' is not a recognized built-in function name.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::testCubeRoot_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'cbrt' is not a recognized built-in function name.", AdapterQualifier.needsInvestigation),

            // At
            one("meta::pure::functions::collection::tests::at::testAtOtherScenario_Function_1__Boolean_1_", "\"->at(...) function is supported only after direct access of 1->MANY properties. Current expression: ['a', 'b', 'c'] -> map(x:String[1] | [$x, 'z'] -> plus();) -> at(0)\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::at::testAtWithVariable_Function_1__Boolean_1_", "\"->at(...) function is supported only after direct access of 1->MANY properties. Current expression: ['a', 'b', 'c'] -> at(1)\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::at::testAt_Function_1__Boolean_1_", "\"->at(...) function is supported only after direct access of 1->MANY properties. Current expression: ['a', 'b', 'c'] -> at(0)\"", AdapterQualifier.needsImplementation),

            // RemoveDuplicates
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesEmptyListExplicit_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesEmptyList_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_distinct' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesPrimitiveNonStandardFunction_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesPrimitiveStandardFunctionExplicit_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesPrimitiveStandardFunctionMixedTypes_Function_1__Boolean_1_", "\"Any is not managed yet!\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesPrimitiveStandardFunctionSimple_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_distinct' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::removeDuplicatesBy::testRemoveDuplicatesByPrimitive_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'.", AdapterQualifier.needsImplementation),

            //Date
            one("meta::pure::functions::date::tests::testAdjustByDaysBigNumber_Function_1__Boolean_1_", "\"Failed to execute plan - RuntimeException - com.microsoft.sqlserver.jdbc.SQLServerException: Arithmetic overflow error converting expression to data type int.\""),
            one("meta::pure::functions::date::tests::testAdjustByHoursBigNumber_Function_1__Boolean_1_", "\"Failed to execute plan - RuntimeException - com.microsoft.sqlserver.jdbc.SQLServerException: Arithmetic overflow error converting expression to data type int.\""),
            one("meta::pure::functions::date::tests::testAdjustByMicrosecondsBigNumber_Function_1__Boolean_1_", "\"Failed to execute plan - RuntimeException - com.microsoft.sqlserver.jdbc.SQLServerException: Arithmetic overflow error converting expression to data type int.\""),
            one("meta::pure::functions::date::tests::testAdjustByMillisecondsBigNumber_Function_1__Boolean_1_", "\"Failed to execute plan - RuntimeException - com.microsoft.sqlserver.jdbc.SQLServerException: Arithmetic overflow error converting expression to data type int.\""),
            one("meta::pure::functions::date::tests::testAdjustByMilliseconds_Function_1__Boolean_1_", "\"\nexpected: %2015-04-15T13:11:11.339+0000\nactual:   %2015-04-15T13:11:11.337+0000\""),
            one("meta::pure::functions::date::tests::testAdjustByMinutesBigNumber_Function_1__Boolean_1_", "\"Failed to execute plan - RuntimeException - com.microsoft.sqlserver.jdbc.SQLServerException: Arithmetic overflow error converting expression to data type int.\""),
            one("meta::pure::functions::date::tests::testAdjustByMonthsBigNumber_Function_1__Boolean_1_", "\"Failed to execute plan - RuntimeException - com.microsoft.sqlserver.jdbc.SQLServerException: Arithmetic overflow error converting expression to data type int.\""),
            one("meta::pure::functions::date::tests::testAdjustBySecondsBigNumber_Function_1__Boolean_1_", "\"Failed to execute plan - RuntimeException - com.microsoft.sqlserver.jdbc.SQLServerException: Arithmetic overflow error converting expression to data type int.\""),
            one("meta::pure::functions::date::tests::testAdjustReflectiveEvaluation_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::lang::eval(NativeFunction<{Date[1], Integer[1], DurationUnit[1]->Date[1]}>[1],StrictDate[1],Integer[1],DurationUnit[1])'.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::date::tests::testDateFromDay_Function_1__Boolean_1_", "[unsupported-api] The function 'date' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testDateFromHour_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'date_Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testDateFromMinute_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testDateFromMonth_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'date_Integer_1__Integer_1__Date_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testDateFromSecond_Function_1__Boolean_1_", "[unsupported-api] The function 'date' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testDateFromSubSecond_Function_1__Boolean_1_", "[unsupported-api] The function 'date' (state: [Select, false]) is not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testDateFromYear_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'date_Integer_1__Date_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testHasDay_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasDay_Date_1__Boolean_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testHasHour_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasHour_Date_1__Boolean_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testHasMinute_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasMinute_Date_1__Boolean_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testHasMonthReflect_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasMonth_Date_1__Boolean_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testHasMonth_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasMonth_Date_1__Boolean_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testHasSecond_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasSecond_Date_1__Boolean_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testHasSubsecondWithAtLeastPrecision_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasSubsecondWithAtLeastPrecision_Date_1__Integer_1__Boolean_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testHasSubsecond_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasSubsecond_Date_1__Boolean_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testAdjustByMonths_Function_1__Boolean_1_", "Date has no day: 2012-03", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testAdjustByWeeksBigNumber_Function_1__Boolean_1_", "\"Failed to execute plan - RuntimeException - com.microsoft.sqlserver.jdbc.SQLServerException: Arithmetic overflow error converting expression to data type int.\""),
            one("meta::pure::functions::date::tests::testAdjustByYears_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testDateDiffWeeks_Function_1__Boolean_1_", "\"\nexpected: 0\nactual:   -1\""),
            one("meta::pure::functions::date::tests::testDateDiffYears_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testDatePartYearMonthOnly_Function_1__Boolean_1_", "Date has no day: 1973-11", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testDatePartYearOnly_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testHour_Function_1__Boolean_1_", "\"\nexpected: 17\nactual:   0\""),
            one("meta::pure::functions::date::tests::testMinute_Function_1__Boolean_1_", "\"\nexpected: 9\nactual:   0\""),
            one("meta::pure::functions::date::tests::testMonthNumber_Function_1__Boolean_1_", "Date has no day: 2015-04", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::testYear_Function_1__Boolean_1_", "\"Ensure the target system understands Year or Year-month semantic.\"", AdapterQualifier.needsImplementation),


            // MultiIf
            one("meta::pure::functions::lang::tests::if::testSimpleIf_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: An expression of non-boolean type specified in a context where a condition is expected, near 'then'."),

            // Match
            one("meta::pure::functions::lang::tests::match::testMatchManyWithMany_Function_1__Boolean_1_", "\"Match only supports operands with multiplicity [1]..! Current operand : ['w', 'w', 'w']\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchOneWithMany_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchOneWithZeroOne_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchOneWith_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchWithExtraParam_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'match_Any_MANY__Function_$1_MANY$__P_o__T_m_'", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::lang::tests::match::testMatchWithExtraParamsAndFunctionsAsParam_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'match_Any_MANY__Function_$1_MANY$__P_o__T_m_'", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctionsAsParamManyMatch_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctionsAsParam_Function_1__Boolean_1_", "\"Cast exception: Literal cannot be cast to SemiStructuredPropertyAccess\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctionsManyMatch_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctions_Function_1__Boolean_1_", "\"Cast exception: Literal cannot be cast to SemiStructuredPropertyAccess\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchWithMixedReturnType_Function_1__Boolean_1_", "type not supported: meta::pure::functions::lang::tests::model::LA_GeographicEntityType", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchZeroWithMany_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchZeroWithZero_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatch_Function_1__Boolean_1_", "type not supported: meta::pure::functions::lang::tests::match::MA_GeographicEntityType", AdapterQualifier.needsInvestigation),

            // IndexOf
            one("meta::pure::functions::string::tests::indexOf::testFromIndex_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'indexOf_String_1__String_1__Integer_1__Integer_1_'.", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::indexOf::testSimple_Function_1__Boolean_1_", "\"\nexpected: 4\nactual:   5\"", AdapterQualifier.needsInvestigation),

            // ParseBoolean
            one("meta::pure::functions::string::tests::parseBoolean::testParseFalse_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseBoolean' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::parseBoolean::testParseTrue_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseBoolean' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            // ParseDate
            one("meta::pure::functions::string::tests::parseDate::testParseDateTypes_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toTimestamp' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::parseDate::testParseDateWithTimezone_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toTimestamp' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::parseDate::testParseDateWithZ_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toTimestamp' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::parseDate::testParseDate_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toTimestamp' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            // ParseDecimal
            one("meta::pure::functions::string::tests::parseDecimal::testParseDecimal_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseDecimal' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::parseDecimal::testParseDecimalWithPrecisionScale_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseDecimal' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::tests::parseDecimal::testParseZero_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseDecimal' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            // ParseInteger
            one("meta::pure::functions::string::tests::parseInteger::testParseInteger_Function_1__Boolean_1_", "\"Failed to execute plan - RuntimeException - com.microsoft.sqlserver.jdbc.SQLServerException: The conversion of the varchar value '9999999999999992' overflowed an int column.\""),

            // StartsWith
            one("meta::pure::functions::string::tests::startswith::testFalseStartsWith_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'like'.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::startswith::testTrueStartsWith_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'like'.", AdapterQualifier.needsInvestigation)
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.SqlServer).getFirst())
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
