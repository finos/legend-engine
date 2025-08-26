// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.duckdb.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalDuckDBPCTCodeRepositoryProvider;
import org.finos.legend.pure.m3.PlatformCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_DuckDB_EssentialFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = PlatformCodeRepositoryProvider.essentialFunctions;
    private static final Adapter adapter = CoreRelationalDuckDBPCTCodeRepositoryProvider.duckDBAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Add
            one("meta::pure::functions::collection::tests::add::testAddWithOffset_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'add_T_MANY__Integer_1__T_1__T_$1_MANY$_'", AdapterQualifier.needsImplementation),

            // Concatenate
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateMixedType_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateSimple_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::concatenate::testConcatenateTypeInference_Function_1__Boolean_1_", "\"Cannot cast a collection of size 3 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),

            // Contains
            one("meta::pure::functions::collection::tests::contains::testContainsNonPrimitive_Function_1__Boolean_1_", "\"Parameter to IN operation isn't a literal!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::contains::testContainsPrimitive_Function_1__Boolean_1_", "java.sql.SQLException: Conversion Error: Unimplemented type for cast (INTEGER -> DATE)\n\nLINE 1: select 1 in (1, 2, 5, 2, 'a', true, DATE '2014-02-01', 'c')\n               ^", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::contains::testContainsWithFunction_Function_1__Boolean_1_", "no viable alternative at input '->meta::pure::functions::collection::contains(meta::pure::functions::collection::tests::contains::ClassWithoutEquality.all()->meta::pure::functions::multiplicity::toOne(),comparator(a:meta::pure::functions::collection::tests::contains::ClassWithoutEquality[1],'", AdapterQualifier.needsInvestigation),

            // Drop
            one("meta::pure::functions::collection::tests::drop::testDropExceedsSizeOnNonEmptyList_Function_1__Boolean_1_", "Variant ->drop() not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::drop::testDropInList_Function_1__Boolean_1_", "Variant ->drop() not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::drop::testDropManyOnNonEmptyList_Function_1__Boolean_1_", "Variant ->drop() not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::drop::testDropNegativeOnEmptyList_Function_1__Boolean_1_", "Invalid type for parameter inside the drop function. Expected a value, found operation/function", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::drop::testDropNegativeOnNonEmptyList_Function_1__Boolean_1_", "Variant ->drop() not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::drop::testDropOneOnNonEmptyList_Function_1__Boolean_1_", "Variant ->drop() not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::drop::testDropZeroOnNonEmptyList_Function_1__Boolean_1_", "Variant ->drop() not supported yet", AdapterQualifier.needsImplementation),

            // Exists
            one("meta::pure::functions::collection::tests::exists::testExists_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),

            // Find
            one("meta::pure::functions::collection::tests::find::testFindInstance_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/essential/collection/iteration/find.pure:38cc38-42); error compiling generated Java code:", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::find::testFindLiteralFromVar_Function_1__Boolean_1_", "Function does not exist 'meta::pure::functions::collection::find(String[3],LambdaFunction<{String[1]->Boolean[1]}>[1])'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::find::testFindLiteral_Function_1__Boolean_1_", "Function does not exist 'meta::pure::functions::collection::find(String[4],LambdaFunction<{String[1]->Boolean[1]}>[1])'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::find::testFindUsingVarForFunction_Function_1__Boolean_1_", "Error dynamically evaluating value specification (from /platform/pure/essential/collection/iteration/find.pure:47cc38-42); error compiling generated Java code:", AdapterQualifier.needsInvestigation),

            // Fold
            one("meta::pure::functions::collection::tests::fold::testFoldCollectionAccumulator_Function_1__Boolean_1_", "The initial value type must be the same as the list child type or a common super type", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::fold::testFoldWithEmptyAccumulator_Function_1__Boolean_1_", "The initial value type must be the same as the list child type or a common super type", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::fold::testFoldWithSingleValue_Function_1__Boolean_1_", "The initial value type must be the same as the list child type or a common super type", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::fold::testFoldEmptyListAndEmptyIdentity_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::fold::testFoldEmptyListAndNonEmptyIdentity_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::fold::testFoldFiltering_Function_1__Boolean_1_", "Function does not exist 'meta::pure::functions::lang::copy(FO_Person[1],String[1],KeyExpression[1])'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::fold::testFoldMixedAccumulatorTypes_Function_1__Boolean_1_", "java.sql.SQLException: Binder Error: The initial value type must be the same as the list child type or a common super type", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::fold::testFoldToMany_Function_1__Boolean_1_", "Function does not exist 'meta::pure::functions::lang::copy(FO_Person[1],String[1],KeyExpression[1])'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::fold::testFold_Function_1__Boolean_1_", "Function does not exist 'meta::pure::functions::lang::copy(FO_Person[1],String[1],KeyExpression[1])'", AdapterQualifier.unsupportedFeature),

            // ForAll
            one("meta::pure::functions::collection::tests::forall::testforAllOnEmptySet_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'forAll_T_MANY__Function_1__Boolean_1_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::forall::testforAllOnNonEmptySetIsFalse_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'forAll_T_MANY__Function_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::forall::testforAllOnNonEmptySetIsTrue_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'forAll_T_MANY__Function_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),

            // Head
            one("meta::pure::functions::collection::tests::head::testHeadComplex_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::head::testHeadOnEmptySet_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::head::testHeadOnOneElement_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::head::testHeadSimple_Function_1__Boolean_1_", "\"Cannot cast a collection of size 2 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),

            // Indexof
            one("meta::pure::functions::collection::tests::indexof::testIndexOfOneElement_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), \"\nexpected: 0\nactual:   1\"", AdapterQualifier.needsInvestigation),

            // Init
            one("meta::pure::functions::collection::tests::init::testInitOnEmptySet_Function_1__Boolean_1_", "Function does not exist 'meta::pure::functions::collection::init(Nil[0])'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::init::testInitOneElement_Function_1__Boolean_1_", "Function does not exist 'meta::pure::functions::collection::init(String[1])'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::init::testInit_Function_1__Boolean_1_", "Function does not exist 'meta::pure::functions::collection::init(String[3])'", AdapterQualifier.unsupportedFeature),

            // Last
            one("meta::pure::functions::collection::tests::last::testLastFromEmpty_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'last_T_MANY__T_$0_1$_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::last::testLastOfOneElementList_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'last_T_MANY__T_$0_1$_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::last::testLast_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'last_T_MANY__T_$0_1$_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),

            // Mod
            one("meta::pure::functions::math::tests::mod::testModInEval_Function_1__Boolean_1_", "\"Unused format args. [3] arguments provided to expression \"cast(fmod(%s,%s) as integer)\"\"", AdapterQualifier.unsupportedFeature),

            // Pow
            one("meta::pure::functions::math::tests::pow::testNumberPow_Function_1__Boolean_1_", "\"\nexpected: 9.0\nactual:   27.0\"", AdapterQualifier.needsInvestigation),

            // Abs
            one("meta::pure::functions::math::tests::abs::testDecimalAbs_Function_1__Boolean_1_", "\"\nexpected: 3.0D\nactual:   3D\"", AdapterQualifier.needsInvestigation),

            // Sort
            one("meta::pure::functions::collection::tests::sort::testMixedSortNoComparator_Function_1__Boolean_1_", "Conversion Error: Casting value \"342.0\" to type DECIMAL(10,10) failed: value is out of range!", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortReversed_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortWithFunctionVariables_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::sort::testSimpleSortWithKey_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::sort::testSimpleSort_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'sort_T_m__Function_$0_1$__Function_$0_1$__T_m_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),

            // Tail
            one("meta::pure::functions::collection::tests::tail::testTailOnEmptySet_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'tail_T_MANY__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::tail::testTailOneElement_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'tail_T_MANY__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::tail::testTail_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'tail_T_MANY__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),

            // Take
            one("meta::pure::functions::collection::tests::take::testTakeExceedsSizeOnNonEmptyList_Function_1__Boolean_1_", "Variant ->take() not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::take::testTakeInList_Function_1__Boolean_1_", "Variant ->take() not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::take::testTakeManyOnNonEmptyList_Function_1__Boolean_1_", "Variant ->take() not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::take::testTakeNegativeOnEmptyList_Function_1__Boolean_1_", "Invalid type for parameter inside the take/limit function. Expected a value, found operation/function", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::take::testTakeNegativeOnNonEmptyList_Function_1__Boolean_1_", "Variant ->take() not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::take::testTakeOneOnNonEmptyList_Function_1__Boolean_1_", "Variant ->take() not supported yet", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::collection::tests::take::testTakeZeroOnNonEmptyList_Function_1__Boolean_1_", "Variant ->take() not supported yet", AdapterQualifier.needsImplementation),

            // Zip
            one("meta::pure::functions::collection::tests::zip::testZipBothListsAreOfPairs_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipBothListsEmpty_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipBothListsSameLength_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::zip::testZipFirstListEmpty_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipFirstListLonger_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::zip::testZipFirstListsIsOfPairs_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipSecondListEmpty_Function_1__Boolean_1_", "\"The system is trying to get an element at offset 0 where the collection is of size 0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::zip::testZipSecondListLonger_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::zip::testZipSecondListsIsOfPairs_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'zip_T_MANY__U_MANY__Pair_MANY_'.", AdapterQualifier.needsInvestigation),


            // Format
            one("meta::pure::functions::string::tests::format::testFormatBoolean_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatDate_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatFloatWithRounding_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatFloatWithTruncation_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatFloatWithZeroPadding_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatFloat_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatInEval_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatInEvaluate_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::format::testFormatIntegerWithZeroPadding_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatInteger_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatList_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::format::testFormatPair_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::format::testFormatRepr_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testFormatString_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::format::testSimpleFormatDate_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'format_String_1__Any_MANY__String_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),

            // JoinStrings
            one("meta::pure::functions::string::tests::joinStrings::testJoinStringsNoStrings_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'tail_T_MANY__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::joinStrings::testJoinStringsSingleString_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), \"\nexpected: '[a]'\nactual:   '['\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::joinStrings::testJoinStringsUsingGenericArrow_Function_1__Boolean_1_", "\"\nexpected: '[a,b,c]'\nactual:   '[,a,b,c,]'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::joinStrings::testJoinStrings_Function_1__Boolean_1_", "\"\nexpected: '[a,b,c]'\nactual:   '[,a,b,c,]'\"", AdapterQualifier.needsInvestigation),

            // Split
            one("meta::pure::functions::string::tests::split::testSplitWithNoSplit_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'split_String_1__String_1__String_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::split::testSplit_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'split_String_1__String_1__String_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),

            // SubString
            one("meta::pure::functions::string::tests::substring::testStartEnd_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), \"\nexpected: 'the quick brown fox jumps over the lazy dog'\nactual:   'the quick brown fox jumps over the lazy do'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::substring::testStart_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), \"\nexpected: 'he quick brown fox jumps over the lazy dog'\nactual:   'the quick brown fox jumps over the lazy dog'\"", AdapterQualifier.needsInvestigation),

            // ToString
            one("meta::pure::functions::string::tests::toString::testClassToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testComplexClassToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testDateTimeToString_Function_1__Boolean_1_", "\"\nexpected: '2014-01-01T00:00:00.000+0000'\nactual:   '2014-01-01 00:00:00'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testDateTimeWithTimezoneToString_Function_1__Boolean_1_", "\"\nexpected: '2014-01-01T00:00:00.0000+0000'\nactual:   '2014-01-01 00:00:00'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testDateToString_Function_1__Boolean_1_", "Date has no day: 2014-01", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::toString::testEnumerationToString_Function_1__Boolean_1_", "\"Match failure: ClassInstanceHolderObject instanceOf ClassInstanceHolder\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testListToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testPairCollectionToString_Function_1__Boolean_1_", "Cast exception: StoreRoutingStrategy cannot be cast to StoreMappingRoutingStrategy", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testPairToString_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testPersonToString_Function_1__Boolean_1_", "\"Assert failed\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::toString::testSimpleDateToString_Function_1__Boolean_1_", "\"\nexpected: '2014-01-02T01:54:27.352+0000'\nactual:   '2014-01-02 01:54:27.352'\"", AdapterQualifier.needsInvestigation),

            // Rem
            one("meta::pure::functions::math::tests::rem::testRemWithDecimals_Function_1__Boolean_1_", "\"\nexpected: 0.14D\nactual:   0.14\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::rem::testRemInEvalWithNegativeNumbers_Function_1__Boolean_1_", "\"Unused format args. [3] arguments provided to expression \"mod(%s,%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::rem::testRemInEval_Function_1__Boolean_1_", "\"Unused format args. [3] arguments provided to expression \"mod(%s,%s)\"\"", AdapterQualifier.unsupportedFeature),

            // Round
            one("meta::pure::functions::math::tests::round::testPositiveFloatRoundHalfEvenDown_Function_1__Boolean_1_", "\"\nexpected: 16\nactual:   17\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::round::testNegativeFloatRoundHalfEvenUp_Function_1__Boolean_1_", "\"\nexpected: -16\nactual:   -17\"", AdapterQualifier.needsInvestigation),

            // toDecimal
            one("meta::pure::functions::math::tests::toDecimal::testIntToDecimal_Function_1__Boolean_1_", "Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), \"\nexpected: 8D\nactual:   8.0D\"", AdapterQualifier.needsInvestigation),

            // At
            one("meta::pure::functions::collection::tests::at::testAtOtherScenario_Function_1__Boolean_1_", "\"->at(...) function is supported only after direct access of 1->MANY properties. Current expression: ['a', 'b', 'c'] -> map(x:String[1] | [$x, 'z'] -> plus();) -> at(0)\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::at::testAtWithVariable_Function_1__Boolean_1_", "\"->at(...) function is supported only after direct access of 1->MANY properties. Current expression: ['a', 'b', 'c'] -> at(1)\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::at::testAt_Function_1__Boolean_1_", "\"->at(...) function is supported only after direct access of 1->MANY properties. Current expression: ['a', 'b', 'c'] -> at(0)\"", AdapterQualifier.unsupportedFeature),

            // RemoveDuplicates
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesEmptyListExplicit_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesEmptyList_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesPrimitiveNonStandardFunction_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesPrimitiveStandardFunctionExplicit_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::removeDuplicates::testRemoveDuplicatesPrimitiveStandardFunction_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::removeDuplicatesBy::testRemoveDuplicatesByPrimitive_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'removeDuplicates_T_MANY__Function_$0_1$__Function_$0_1$__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),

            //Date
            one("meta::pure::functions::date::tests::testAdjustByDaysBigNumber_Function_1__Boolean_1_", "java.sql.SQLException: Invalid Input Error: Type INT64 with value 12345678912 can't be cast because the value is out of range for the destination type INT32", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::date::tests::testAdjustByHoursBigNumber_Function_1__Boolean_1_", "java.sql.SQLException: Out of Range Error: Interval value 12345678912 hours out of range", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::date::tests::testAdjustByMicrosecondsBigNumber_Function_1__Boolean_1_", "\"\nexpected: %2021-06-21T09:37:37.4990000+0000\nactual:   %2021-06-21T09:37:37.499+0000\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::date::tests::testAdjustByMinutesBigNumber_Function_1__Boolean_1_", "\"\nexpected: %-21457-01-08T20:48:00+0000\nactual:   %21459-07-29T20:48:00+0000\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::date::tests::testAdjustByMonthsBigNumber_Function_1__Boolean_1_", "java.sql.SQLException: Invalid Input Error: Type INT64 with value 9600000000 can't be cast because the value is out of range for the destination type INT32", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::date::tests::testAdjustReflectiveEvaluation_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::lang::eval(NativeFunction<{Date[1], Integer[1], DurationUnit[1]->Date[1]}>[1],StrictDate[1],Integer[1],DurationUnit[1])'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateFromHour_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'date_Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateFromMinute_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'date_Integer_1__Integer_1__Integer_1__Integer_1__Integer_1__DateTime_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateFromMonth_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'date_Integer_1__Integer_1__Date_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateFromSecond_Function_1__Boolean_1_", "\"\nexpected: %1973-11-13T23:09:11+0000\nactual:   %1973-11-13T23:09:11.000000000+0000\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::date::tests::testDateFromSubSecond_Function_1__Boolean_1_", "\"\nexpected: %1973-11-13T23:09:11.0+0000\nactual:   %1973-11-13T23:09:11.000000000+0000\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::date::tests::testDateFromYear_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'date_Integer_1__Date_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHasDay_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasDay_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHasHour_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasHour_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHasMinute_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasMinute_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHasMonthReflect_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasMonth_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHasMonth_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasMonth_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHasSecond_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasSecond_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHasSubsecondWithAtLeastPrecision_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasSubsecondWithAtLeastPrecision_Date_1__Integer_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHasSubsecond_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'hasSubsecond_Date_1__Boolean_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustByMonths_Function_1__Boolean_1_", "Date has no day: 2012-03", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testAdjustByWeeksBigNumber_Function_1__Boolean_1_", "java.sql.SQLException: Invalid Input Error: Type INT64 with value 12345678912 can't be cast because the value is out of range for the destination type INT32", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::date::tests::testAdjustByYears_Function_1__Boolean_1_", "\"DuckDB doesn't support YEAR and YEAR-MONTH\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDateDiffWeeks_Function_1__Boolean_1_", "\"\nexpected: 1\nactual:   0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::date::tests::testDateDiffYears_Function_1__Boolean_1_", "\"DuckDB doesn't support YEAR and YEAR-MONTH\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDatePartYearMonthOnly_Function_1__Boolean_1_", "Date has no day: 1973-11", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testDatePartYearOnly_Function_1__Boolean_1_", "\"DuckDB doesn't support YEAR and YEAR-MONTH\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testHour_Function_1__Boolean_1_", "\"\nexpected: 17\nactual:   0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::date::tests::testMinute_Function_1__Boolean_1_", "\"\nexpected: 9\nactual:   0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::date::tests::testMonthNumber_Function_1__Boolean_1_", "Date has no day: 2015-04", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::testYear_Function_1__Boolean_1_", "\"DuckDB doesn't support YEAR and YEAR-MONTH\"", AdapterQualifier.unsupportedFeature),

            // Match
            one("meta::pure::functions::lang::tests::match::testMatchManyWithMany_Function_1__Boolean_1_", "\"Match only supports operands with multiplicity [1]..! Current operand : ['w', 'w', 'w']\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchOneWithMany_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchOneWithZeroOne_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchOneWith_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchWithExtraParam_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'match_Any_MANY__Function_$1_MANY$__P_o__T_m_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::match::testMatchWithExtraParamsAndFunctionsAsParam_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'match_Any_MANY__Function_$1_MANY$__P_o__T_m_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctionsAsParamManyMatch_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctionsAsParam_Function_1__Boolean_1_", "\"Cast exception: Literal cannot be cast to SemiStructuredPropertyAccess\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctionsManyMatch_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchWithFunctions_Function_1__Boolean_1_", "\"Cast exception: Literal cannot be cast to SemiStructuredPropertyAccess\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchWithMixedReturnType_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchZeroWithMany_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatchZeroWithZero_Function_1__Boolean_1_", "\"Match does not support Non-Primitive return type..! Current return type : Any\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::lang::tests::match::testMatch_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),

            // IndexOf
            one("meta::pure::functions::string::tests::indexOf::testFromIndex_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'indexOf_String_1__String_1__Integer_1__Integer_1_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::indexOf::testSimple_Function_1__Boolean_1_",  "Assert failure at (resource:/platform/pure/essential/tests/assert.pure line:21 column:5), \"\nexpected: 4\nactual:   5\"", AdapterQualifier.needsInvestigation),

            // ParseBoolean
            one("meta::pure::functions::string::tests::parseBoolean::testParseFalse_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseBoolean' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::parseBoolean::testParseTrue_Function_1__Boolean_1_", "\"[unsupported-api] The function 'parseBoolean' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            // ParseDate
            one("meta::pure::functions::string::tests::parseDate::testParseDateTypes_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toTimestamp' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::parseDate::testParseDateWithTimezone_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toTimestamp' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::parseDate::testParseDateWithZ_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toTimestamp' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::string::tests::parseDate::testParseDate_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toTimestamp' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            // ParseDecimal
            one("meta::pure::functions::string::tests::parseDecimal::testParseDecimal_Function_1__Boolean_1_", "java.sql.SQLException: Conversion Error: Could not convert string \"3.14159d\" to DECIMAL(18,3)\n\nLINE 1: select cast('3.14159d' as decimal)\n               ^", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::string::tests::parseDecimal::testParseZero_Function_1__Boolean_1_", "\"\nexpected: 0.000D\nactual:   0.0D\"", AdapterQualifier.needsInvestigation)
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.DuckDB).getFirst())
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
