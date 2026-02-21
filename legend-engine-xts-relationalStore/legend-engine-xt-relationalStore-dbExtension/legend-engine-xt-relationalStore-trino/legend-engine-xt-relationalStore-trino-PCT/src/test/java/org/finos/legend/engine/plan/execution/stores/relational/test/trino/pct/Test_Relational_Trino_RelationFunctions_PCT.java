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
import org.finos.legend.pure.code.core.RelationCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;


public class Test_Relational_Trino_RelationFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = RelationCodeRepositoryProvider.relationFunctions;
    private static final Adapter adapter = CoreRelationalTrinoPCTCodeRepositoryProvider.trinoAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Aggregate
            one("meta::pure::functions::relation::tests::aggregate::testSimpleAggregate_AggColSpecArray_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::aggregate::testSimpleAggregate_WithFilter_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // AsOfJoin
            one("meta::pure::functions::relation::tests::asOfJoin::testAsOfJoinWithKeyMatch_Function_1__Boolean_1_", "\"AsOfJoins are not supported in the generic generator!\""),
            one("meta::pure::functions::relation::tests::asOfJoin::testAsOfJoinWithKeyMatch_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_Function_1__Boolean_1_", "\"AsOfJoins are not supported in the generic generator!\""),
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),

            // Composition
            one("meta::pure::functions::relation::tests::composition::TestJoin_CurrentUserId_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::composition::testCoalesceInPreFilter_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testExtendAddOnNull_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testExtendFilterOutNull_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testExtendFilter_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testExtendJoinStringOnNull_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testExtendWindowFilter_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testFilterPostProject_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testGroupByCastAfterAgg_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testGroupByCastBeforeAgg_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testGroupByFilterExtendFilter_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testGroupByOnNull_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testGroupBy_Conflicting_Alias_With_Table_Columns_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testMixColumnNamesRenameExtend_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testMixColumnNamesRenameFilter_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testMultiCoalesceInProject_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testMultiIfTDS_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testOLAPAggCastWithPartitionWindow_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testOLAPCastAggWithPartitionWindow_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testOLAPCastExtractAggWithPartitionWindow_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testOLAPCastExtractCastAggWithPartitionWindow_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testProjectDistinct_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testProjectJoinWithProjectProject_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testProjectNumbersPlusTimesMinus_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testProjectOfComputedColumn_withCast_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testTDSPlusTimesMinus_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testVariantArrayColumn_reverse_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariantArrayColumn_sort_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_contains_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_contains' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_distinct_removeDuplicates_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_extend_indexExtraction_filter_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_functionComposition_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Where, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_indexOf_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_position' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_isEmpty_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_isNotEmpty_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_projectModelProperty_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_slice_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariant_if_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::testWindowFunctionsAfterProject_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::test_Distinct_Filter_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::test_Distinct_GroupBy_Filter_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::test_Distinct_GroupBy_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::test_Distinct_Size_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_ComplexGroupBy_Pivot_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_GroupBy_Pivot_Extend_Sort_Limit_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_Pivot_GroupBy_Extend_Sort_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::test_GroupBy_Distinct_Filter_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::test_GroupBy_Distinct_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::test_GroupBy_Filter_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::test_GroupBy_GroupBy_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::test_Limit_Size_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::test_Pivot_Filter_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::test_Project_Filter_Before_Static_Pivot_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::test_Slice_Size_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::composition::test_Static_Pivot_Filter_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Concatenate
            one("meta::pure::functions::relation::tests::concatenate::testSimpleConcatenateShared_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::concatenate::testSimpleConcatenate_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),

            // CumulativeDistribution
            one("meta::pure::functions::relation::tests::cumulativeDistribution::testOLAPWithPartitionAndOrderCummulativeDistribution_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // DenseRank
            one("meta::pure::functions::relation::tests::denseRank::testOLAPWithPartitionAndOrderDenseRank_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Distinct
            one("meta::pure::functions::relation::tests::distinct::testDistinctAll_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::distinct::testDistinctAll_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::distinct::testDistinctMultiple_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::distinct::testDistinctMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::distinct::testDistinctSingle_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::distinct::testDistinctSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),

            // Drop
            one("meta::pure::functions::relation::tests::drop::testSimpleDropShared_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::drop::testSimpleDrop_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),

            // Eval
            one("meta::pure::functions::relation::tests::eval::testSimpleEval_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Extend
            one("meta::pure::functions::relation::tests::extend::testOLAPAggIntegerWithPartitionAndOrderWindow_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggIntegerWithPartitionAndOrderWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggNoWindowChainedWithSimple_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggNoWindowChainedWithSimple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggNoWindowMultipleColums_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggNoWindowMultipleColums_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggNoWindow_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggNoWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderASCUnboundedWindow_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderUnboundedWindow_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderUnboundedWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndUnboundedWindow_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithOrderWindow_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithOrderWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderUnboundedWindowMultipleColumns_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderUnboundedWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionWindowMultipleColumns_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionWindow_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithMultiplePartitionsAndOrderWindowMultipleColumns_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithMultiplePartitionsAndOrderWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithPartitionAndMultipleOrderWindowMultipleColumnsWithFilter_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithPartitionAndMultipleOrderWindowMultipleColumns_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithPartitionAndOrderWindowMultipleColumns_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithPartitionAndOrderWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendFloat_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendFloat_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendInt_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendInt_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendStrShared_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendStr_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::extend::testSimpleMultipleColumns_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::extend::testSimpleMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_filter_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_fold_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_indexExtraction_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_keyExtraction_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_map_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\""),

            // Filter
            one("meta::pure::functions::relation::tests::filter::testSimpleFilterShared_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::filter::testSimpleFilter_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOnIndexExtractionValue_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOnKeyExtractionValue_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOutputFromLambda_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Where, false]) is not supported yet\""),

            // First
            one("meta::pure::functions::relation::tests::first::testOLAPWithPartitionAndOrderFirstWindow_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // GroupBy
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleMultiple_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleSingle_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleMultiple_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleSingle_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),

            // Join
            one("meta::pure::functions::relation::tests::join::testFullJoin_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::join::testJoin_forFailedJoinWhenNoRowsMatchJoinCondition_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::join::testRightJoin_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::join::testSimpleJoinShared_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::join::testSimpleJoin_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),

            // Lag
            one("meta::pure::functions::relation::tests::lag::testOLAPWithPartitionAndOrderWindowUsingLag_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Last
            one("meta::pure::functions::relation::tests::last::testOLAPWithPartitionAndOrderLastWindow_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Lateral
            one("meta::pure::functions::relation::tests::lateral::testLateralJoinAreInnerJoins_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::lateral::testLateralJoin_Chained_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::lateral::testLateralJoin_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),

            // Lead
            one("meta::pure::functions::relation::tests::lead::testOLAPWithPartitionAndOrderWindow_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Limit
            one("meta::pure::functions::relation::tests::limit::testSimpleLimitShared_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::limit::testSimpleLimit_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),

            // Nth
            one("meta::pure::functions::relation::tests::nth::testOLAPWithPartitionAndOrderNthWindow2_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::nth::testOLAPWithPartitionAndOrderNthWindow_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Ntile
            one("meta::pure::functions::relation::tests::ntile::testOLAPWithPartitionAndOrderNTile_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Over
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_CurrentRow_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_CurrentRow_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithDifferentDurationUnits_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithDifferentDurationUnits_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_CurrentRow_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_CurrentRow_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NPreceding_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NPreceding_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_CurrentRow_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_CurrentRow_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NPreceding_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NPreceding_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Trino\""),
            one("meta::pure::functions::relation::tests::over::testRange_CurrentRow_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_CurrentRow_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_ExplicitOffsets_WithNullValues_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_ExplicitOffsets_WithNullValues_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_NFollowing_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_NFollowing_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_NFollowing_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_NFollowing_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_NPreceding_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_NPreceding_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_UnboundedFollowing_WithNullValues_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_CurrentRow_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_CurrentRow_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_NFollowing_WithNullValues_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_NPreceding_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_NPreceding_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_WithNumbers_CurrentRow_NFollowing_WithoutPartition_WithSingleOrderBy_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRange_WithNumbers_NFollowing_NFollowing_WithoutPartition_WithSingleOrderBy_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRows_CurrentRow_CurrentRow_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRows_CurrentRow_NFollowing_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRows_CurrentRow_UnboundedFollowing_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRows_NFollowing_NFollowing_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRows_NFollowing_UnboundedFollowing_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRows_NPreceding_CurrentRow_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRows_NPreceding_NFollowing_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRows_NPreceding_NPreceding_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRows_NPreceding_UnboundedFollowing_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRows_UnboundedPreceding_CurrentRow_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRows_UnboundedPreceding_CurrentRow_WithMultiplePartitions_WithSingleOrderBy_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRows_UnboundedPreceding_NFollowing_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRows_UnboundedPreceding_NPreceding_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRows_UnboundedPreceding_UnboundedFollowing_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRows_UnboundedPreceding_UnboundedFollowing_WithMultiplePartitions_WithoutOrderBy_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::over::testRows_UnboundedPreceding_UnboundedFollowing_WithSinglePartition_WithoutOrderBy_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // PercentRank
            one("meta::pure::functions::relation::tests::percentRank::testOLAPWithPartitionAndOrderPercentRank_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Pivot
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleMultiple_Dynamic_Aggregation_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleMultiple_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleSingle_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleMultiple_Dynamic_Aggregation_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleMultiple_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleSingle_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::pivot::testStaticPivot_SingleSingle_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::pivot::testStaticPivot_SingleSingle_StringPivotValue_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Project
            one("meta::pure::functions::relation::tests::project::testSimpleProjectList_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::project::testSimpleProjectWithEmpty_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::project::testSimpleProject_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::project::testSimpleRelationProjectLiterals_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::project::testSimpleRelationProject_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Rank
            one("meta::pure::functions::relation::tests::rank::testOLAPWithPartitionAndOrderRank_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Rename
            one("meta::pure::functions::relation::tests::rename::testSimpleRenameShared_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::rename::testSimpleRename_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),

            // RowNumber
            one("meta::pure::functions::relation::tests::rowNumber::testOLAPWithPartitionAndRowNumber_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Select
            one("meta::pure::functions::relation::tests::select::testMultiColsSelectShared_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::select::testMultiColsSelect_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::select::testSelectAll_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::select::testSelectAll_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::select::testSingleColSelectShared_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::select::testSingleColSelect_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),

            // Size
            one("meta::pure::functions::relation::tests::size::testComparisonOperationAfterSize_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::size::testComparisonOperationAfterSize_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::size::testGroupBySize_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::size::testGroupBySize_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::size::testSimpleSize_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::size::testSimpleSize_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),
            one("meta::pure::functions::relation::tests::size::testSize_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::size::testSize_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::size::testWindowSize_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Slice
            one("meta::pure::functions::relation::tests::slice::testSimpleSliceShared_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::slice::testSimpleSlice_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),

            // Sort
            one("meta::pure::functions::relation::tests::sort::testSimpleSortShared_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::relation::tests::sort::testSimpleSort_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Trino\""),

            // Write
            one("meta::pure::functions::relation::tests::write::testWrite_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Flatten (Variant)
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_LateralJoin_Function_1__Boolean_1_", "\"[unsupported-api] Lateral operation not supported for Database Type: Trino\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_LateralJoin_Nested_Extend_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_LateralJoin_Nested_Function_1__Boolean_1_", "\"[unsupported-api] Lateral operation not supported for Database Type: Trino\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_Scalar_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_Variant_Array_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_Variant_Map_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_Variant_Navigation_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Trino\"")
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
