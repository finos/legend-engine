// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.pure.code.core.java.binding;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.code.core.CoreJavaPlatformBindingCodeRepositoryProvider;
import org.finos.legend.pure.code.core.RelationCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_JAVA_RelationFunction_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = RelationCodeRepositoryProvider.relationFunctions;
    private static final Adapter adapter = CoreJavaPlatformBindingCodeRepositoryProvider.javaAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            pack("meta::pure::functions::relation::tests::concatenate", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::drop", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::extend", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::cumulativeDistribution", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::denseRank", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::first", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::lag", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::last", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::lead", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::nth", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::ntile", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::percentRank", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::rank", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::rowNumber", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::distinct", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::filter", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::groupBy", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::aggregate", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::join", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::limit", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::pivot", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::project", "\"meta::pure::functions::relation::project_C_MANY__FuncColSpecArray_1__Relation_1_ is not supported yet!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::project::testSimpleRelationProject_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::project::testSimpleRelationProjectLiterals_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::rename", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::select", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::size", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::slice", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::sort", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::asOfJoin", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testFilterPostProject_Function_1__Boolean_1_", "\"meta::pure::functions::relation::filter_Relation_1__Function_1__Relation_1_ is not supported yet!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testWindowFunctionsAfterProject_Function_1__Boolean_1_", "\"meta::pure::functions::relation::sort_Relation_1__SortInfo_MANY__Relation_1_ is not supported yet!\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::write", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::over", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::eval", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::tests::lateral", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::relation::variant::tests::flatten", "\"meta::pure::functions::relation::variant::flatten_T_MANY__ColSpec_1__Relation_1_ is not supported yet!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_LateralJoin_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_LateralJoin_Nested_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_LateralJoin_Nested_Extend_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testExtendAddOnNull_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testExtendFilterOutNull_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testProjectOfComputedColumn_withCast_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testMultiCoalesceInProject_Function_1__Boolean_1_", "Error in 'test::testFunction': Can't find a match for function 'new(Class<OptionalPersonTypeForCompositionTests>[1],String[1])'.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testProjectNumbersPlusTimesMinus_Function_1__Boolean_1_", "\"meta::pure::functions::relation::project_C_MANY__FuncColSpecArray_1__Relation_1_ is not supported yet!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::TestJoin_CurrentUserId_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testExtendFilter_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testExtendJoinStringOnNull_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testExtendWindowFilter_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testGroupByCastAfterAgg_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testGroupByCastBeforeAgg_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testGroupByFilterExtendFilter_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testGroupByOnNull_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testGroupBy_Conflicting_Alias_With_Table_Columns_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testMixColumnNamesRenameExtend_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testMixColumnNamesRenameFilter_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testMultiIfTDS_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testOLAPAggCastWithPartitionWindow_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testOLAPCastAggWithPartitionWindow_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testOLAPCastExtractAggWithPartitionWindow_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testOLAPCastExtractCastAggWithPartitionWindow_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testProjectDistinct_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testProjectJoinWithProjectProject_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testTDSPlusTimesMinus_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testProjectDistinct_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantArrayColumn_reverse_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantArrayColumn_sort_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_contains_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_distinct_removeDuplicates_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_extend_indexExtraction_filter_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_functionComposition_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_indexOf_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_isEmpty_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_isNotEmpty_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_modelOutputNotSupported_Function_1__Boolean_1_", "Execution error message mismatch.\nThe actual message was \"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"\nwhere the expected message was:\"The type meta::pure::functions::relation::tests::composition::Person is not supported yet!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_projectModelProperty_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_slice_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariant_if_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Distinct_Filter_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Distinct_GroupBy_Filter_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Distinct_GroupBy_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Distinct_Size_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_ComplexGroupBy_Pivot_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_GroupBy_Pivot_Extend_Sort_Limit_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_Pivot_GroupBy_Extend_Sort_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_GroupBy_Distinct_Filter_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_GroupBy_Distinct_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_GroupBy_Filter_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_GroupBy_GroupBy_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Limit_Size_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Pivot_Filter_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Project_Filter_Before_Static_Pivot_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Slice_Size_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Static_Pivot_Filter_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Slice_Size_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"", AdapterQualifier.unsupportedFeature)
    );

    public static Test suite()
    {
        return PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter);
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
