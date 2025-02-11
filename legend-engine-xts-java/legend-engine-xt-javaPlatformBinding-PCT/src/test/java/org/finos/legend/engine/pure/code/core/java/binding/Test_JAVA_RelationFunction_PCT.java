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
            // Concatenate
            pack("meta::pure::functions::relation::tests::concatenate", "\"meta::pure::functions::relation::concatenate_Relation_1__Relation_1__Relation_1_ is not supported yet!\""),

            // Drop
            pack("meta::pure::functions::relation::tests::drop", "\"meta::pure::functions::relation::drop_Relation_1__Integer_1__Relation_1_ is not supported yet!\""),

            // Extend
            one("meta::pure::functions::relation::tests::extend::testSimpleMultipleColumns_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1__FuncColSpecArray_1__Relation_1_ is not supported yet!\""),

            // Extend (OLAP)
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendFloat_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1__FuncColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggNoWindowChainedWithSimple_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1__FuncColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithOrderWindow_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendInt_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1__FuncColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggNoWindowMultipleColums_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendStrShared_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1__FuncColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggNoWindow_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggIntegerWithPartitionAndOrderWindow_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionWindowMultipleColumns_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionWindow_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithMultiplePartitionsAndOrderWindowMultipleColumns_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__FuncColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithPartitionAndOrderWindowMultipleColumns_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__FuncColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::cumulativeDistribution::testOLAPWithPartitionAndOrderCummulativeDistribution_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__FuncColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::denseRank::testOLAPWithPartitionAndOrderDenseRank_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__FuncColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::first::testOLAPWithPartitionAndOrderFirstWindow_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__FuncColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::lag::testOLAPWithPartitionAndOrderWindowUsingLag_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__FuncColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::last::testOLAPWithPartitionAndOrderLastWindow_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__FuncColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::lead::testOLAPWithPartitionAndOrderWindow_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__FuncColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::nth::testOLAPWithPartitionAndOrderNthWindow2_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__FuncColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::nth::testOLAPWithPartitionAndOrderNthWindow_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__FuncColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::ntile::testOLAPWithPartitionAndOrderNTile_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__FuncColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::percentRank::testOLAPWithPartitionAndOrderPercentRank_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__FuncColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::rank::testOLAPWithPartitionAndOrderRank_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__FuncColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::rowNumber::testOLAPWithPartitionAndRowNumber_Function_1__Boolean_1_", "\"meta::pure::functions::relation::extend_Relation_1___Window_1__FuncColSpec_1__Relation_1_ is not supported yet!\""),

            // Distinct
            one("meta::pure::functions::relation::tests::distinct::testDistinctAll_Function_1__Boolean_1_", "\"meta::pure::functions::relation::distinct_Relation_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::distinct::testDistinctMultiple_Function_1__Boolean_1_", "\"meta::pure::functions::relation::distinct_Relation_1__ColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::distinct::testDistinctSingle_Function_1__Boolean_1_", "\"meta::pure::functions::relation::distinct_Relation_1__ColSpecArray_1__Relation_1_ is not supported yet!\""),

            // Filter
            pack("meta::pure::functions::relation::tests::filter", "\"meta::pure::functions::relation::filter_Relation_1__Function_1__Relation_1_ is not supported yet!\""),

            // GroupBy
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBySingleSingle_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupByMultipleMultiple_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupByMultipleSingle_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpecArray_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBySingleMultiple_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),

            // Join
            one("meta::pure::functions::relation::tests::join::testSimpleJoinShared_Function_1__Boolean_1_", "\"meta::pure::functions::relation::join_Relation_1__Relation_1__JoinKind_1__Function_1__Relation_1_ is not supported yet!\""),

            // Limit
            one("meta::pure::functions::relation::tests::limit::testSimpleLimitShared_Function_1__Boolean_1_", "\"meta::pure::functions::relation::limit_Relation_1__Integer_1__Relation_1_ is not supported yet!\""),

            // Pivot
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleMultiple_Function_1__Boolean_1_", "\"meta::pure::functions::relation::pivot_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleSingle_Function_1__Boolean_1_", "\"meta::pure::functions::relation::pivot_Relation_1__ColSpecArray_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleMultiple_Function_1__Boolean_1_", "\"meta::pure::functions::relation::pivot_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleSingle_Function_1__Boolean_1_", "\"meta::pure::functions::relation::pivot_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),

            // Project
            pack("meta::pure::functions::relation::tests::project", "\"meta::pure::functions::relation::project_C_MANY__FuncColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::project::testSimpleRelationProject_Function_1__Boolean_1_", "\"meta::pure::functions::relation::project_Relation_1__FuncColSpecArray_1__Relation_1_ is not supported yet!\""),

            // Rename
            one("meta::pure::functions::relation::tests::rename::testSimpleRenameShared_Function_1__Boolean_1_", "\"meta::pure::functions::relation::rename_Relation_1__ColSpec_1__ColSpec_1__Relation_1_ is not supported yet!\""),

            // Select
            one("meta::pure::functions::relation::tests::select::testMultiColsSelectShared_Function_1__Boolean_1_", "\"meta::pure::functions::relation::select_Relation_1__ColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::select::testSingleColSelectShared_Function_1__Boolean_1_", "\"meta::pure::functions::relation::select_Relation_1__ColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_Function_1__Boolean_1_", "\"meta::pure::functions::relation::select_Relation_1__ColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::select::testSelectAll_Function_1__Boolean_1_", "\"meta::pure::functions::relation::select_Relation_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_Function_1__Boolean_1_", "\"meta::pure::functions::relation::select_Relation_1__ColSpec_1__Relation_1_ is not supported yet!\""),

            // Size
            pack("meta::pure::functions::relation::tests::size", "\"meta::pure::functions::relation::size_Relation_1__Integer_1_ is not supported yet!\""),

            // Slice
            one("meta::pure::functions::relation::tests::slice::testSimpleSliceShared_Function_1__Boolean_1_", "\"meta::pure::functions::relation::slice_Relation_1__Integer_1__Integer_1__Relation_1_ is not supported yet!\""),

            // Sort
            one("meta::pure::functions::relation::tests::sort::testSimpleSortShared_Function_1__Boolean_1_", "\"meta::pure::functions::relation::sort_Relation_1__SortInfo_MANY__Relation_1_ is not supported yet!\""),

            // AsOfJoin
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_Function_1__Boolean_1_", "\"meta::pure::functions::relation::asOfJoin_Relation_1__Relation_1__Function_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::asOfJoin::testAsOfJoinWithKeyMatch_Function_1__Boolean_1_", "\"meta::pure::functions::relation::asOfJoin_Relation_1__Relation_1__Function_1__Function_1__Relation_1_ is not supported yet!\""),

            // Composition
            one("meta::pure::functions::relation::tests::composition::testExtendFilter_Function_1__Boolean_1_", "\"meta::pure::functions::relation::filter_Relation_1__Function_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::composition::testFilterPostProject_Function_1__Boolean_1_", "\"meta::pure::functions::relation::filter_Relation_1__Function_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::composition::test_Distinct_Filter_Function_1__Boolean_1_", "\"meta::pure::functions::relation::filter_Relation_1__Function_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::composition::test_Distinct_GroupBy_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::composition::test_GroupBy_Distinct_Function_1__Boolean_1_", "\"meta::pure::functions::relation::distinct_Relation_1__ColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::composition::test_GroupBy_GroupBy_Function_1__Boolean_1_", "\"meta::pure::functions::relation::groupBy_Relation_1__ColSpecArray_1__AggColSpec_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::composition::test_Pivot_Filter_Function_1__Boolean_1_", "\"meta::pure::functions::relation::pivot_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_ComplexGroupBy_Pivot_Function_1__Boolean_1_", "\"meta::pure::functions::relation::pivot_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_Pivot_GroupBy_Extend_Sort_Function_1__Boolean_1_", "\"meta::pure::functions::relation::pivot_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_GroupBy_Pivot_Extend_Sort_Limit_Function_1__Boolean_1_", "\"meta::pure::functions::relation::pivot_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::composition::test_Distinct_GroupBy_Filter_Function_1__Boolean_1_", "\"meta::pure::functions::relation::filter_Relation_1__Function_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::composition::test_GroupBy_Distinct_Filter_Function_1__Boolean_1_", "\"meta::pure::functions::relation::filter_Relation_1__Function_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::composition::test_GroupBy_Filter_Function_1__Boolean_1_", "\"meta::pure::functions::relation::filter_Relation_1__Function_1__Relation_1_ is not supported yet!\""),
            one("meta::pure::functions::relation::tests::composition::testWindowFunctionsAfterProject_Function_1__Boolean_1_", "\"meta::pure::functions::relation::sort_Relation_1__SortInfo_MANY__Relation_1_ is not supported yet!\""),

            // Write
            one("meta::pure::functions::relation::testWrite_Function_1__Boolean_1_", "\"meta::pure::functions::relation::write_Relation_1__RelationElementAccessor_1__Integer_1_ is not supported yet!\"")
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
