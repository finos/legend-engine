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

package org.finos.legend.engine.plan.execution.stores.relational.test.clickhouse.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalClickHousePCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.RelationCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_ClickHouse_RelationFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = RelationCodeRepositoryProvider.relationFunctions;
    private static final Adapter adapter = CoreRelationalClickHousePCTCodeRepositoryProvider.clickhouseAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Aggregate
            one("meta::pure::functions::relation::tests::aggregate::testSimpleAggregate_AggColSpecArray_Function_1__Boolean_1_", "code is not under aggregate function and not in GROUP BY keys"),

            // AsOfJoin
            one("meta::pure::functions::relation::tests::asOfJoin::testAsOfJoinWithKeyMatch_Function_1__Boolean_1_", "\"AsOfJoins are not supported in the generic generator!\""),
            one("meta::pure::functions::relation::tests::asOfJoin::testAsOfJoinWithKeyMatch_MultipleExpressions_Function_1__Boolean_1_", "\"AsOfJoins are not supported in the generic generator!\""),
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_Function_1__Boolean_1_", "\"AsOfJoins are not supported in the generic generator!\""),
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_MultipleExpressions_Function_1__Boolean_1_", "\"AsOfJoins are not supported in the generic generator!\""),

            // Composition
            one("meta::pure::functions::relation::tests::composition::testExtendJoinStringOnNull_Function_1__Boolean_1_", "Aggregate function with name 'concatWithSeparator' does not exist."),
            one("meta::pure::functions::relation::tests::composition::TestJoin_CurrentUserId_Function_1__Boolean_1_", "java.sql.SQLException: Code: 403. DB::Exception: Cannot determine join keys in LEFT JOIN ... ON t1_1.user = CURRENT_USER(). (INVALID_JOIN_ON_EXPRESSION) (version 25.1.1.4165 (official build)) "),
            one("meta::pure::functions::relation::tests::composition::testExtendWindowFilter_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,grp,name,newCol\n   2,1,B,16\n   6,1,F,16\n   8,1,H,16\n#'\nactual:   '#TDS\n   id,grp,name,newCol\n   10,0,J,10\n   2,1,B,16\n   6,1,F,16\n   8,1,H,16\n   1,2,A,6\n   5,2,E,6\n   3,3,C,10\n   7,3,G,10\n   4,4,D,4\n   9,5,I,9\n#'\""),
            one("meta::pure::functions::relation::tests::composition::testFilterPostProject_Function_1__Boolean_1_", "Error while executing: Create Table leSchema.FirmTypeForCompositionTests"),
            one("meta::pure::functions::relation::tests::composition::testGroupByFilterExtendFilter_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   grp,rank,sumId,sumRank\n   A,1,9,3\n   A,2,8,3\n#'"),
            one("meta::pure::functions::relation::tests::composition::testVariantArrayColumn_reverse_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariantArrayColumn_sort_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_contains_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_contains' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_distinct_removeDuplicates_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_extend_indexExtraction_filter_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_functionComposition_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Where, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_indexOf_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_position' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_isEmpty_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_isNotEmpty_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_projectModelProperty_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_slice_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariant_if_Function_1__Boolean_1_", "Error while executing: Create Table leSchema.tb"),
            one("meta::pure::functions::relation::tests::composition::testWindowFunctionsAfterProject_Function_1__Boolean_1_", "Error while executing: Create Table leSchema.PersonTypeForCompositionTests"),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_ComplexGroupBy_Pivot_Function_1__Boolean_1_", "\"pivot is not supported for ClickHouse\""),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_GroupBy_Pivot_Extend_Sort_Limit_Function_1__Boolean_1_", "\"pivot is not supported for ClickHouse\""),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_Pivot_GroupBy_Extend_Sort_Function_1__Boolean_1_", "\"pivot is not supported for ClickHouse\""),
            one("meta::pure::functions::relation::tests::composition::test_Pivot_Filter_Function_1__Boolean_1_", "\"pivot is not supported for ClickHouse\""),
            one("meta::pure::functions::relation::tests::composition::test_Static_Pivot_Filter_Function_1__Boolean_1_", "\"pivot is not supported for ClickHouse\""),
            one("meta::pure::functions::relation::tests::composition::test_Project_Filter_Before_Static_Pivot_Function_1__Boolean_1_", "\"pivot is not supported for ClickHouse\""),

            // CumulativeDistribution
            one("meta::pure::functions::relation::tests::cumulativeDistribution::testOLAPWithPartitionAndOrderCummulativeDistribution_Function_1__Boolean_1_", "Aggregate function with name 'cume_dist' does not exist"),

            // Extend
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderASCUnboundedWindow_Function_1__Boolean_1_", "Aggregate function with name 'concatWithSeparator' does not exist."),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderUnboundedWindow_Function_1__Boolean_1_", "Aggregate function with name 'concatWithSeparator' does not exist."),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderUnboundedWindow_MultipleExpressions_Function_1__Boolean_1_", "Aggregate function with name 'concatWithSeparator' does not exist."),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_Function_1__Boolean_1_", "Aggregate function with name 'concatWithSeparator' does not exist."),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_MultipleExpressions_Function_1__Boolean_1_", "Aggregate function with name 'concatWithSeparator' does not exist."),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndUnboundedWindow_Function_1__Boolean_1_", "Aggregate function with name 'concatWithSeparator' does not exist."),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderUnboundedWindowMultipleColumns_Function_1__Boolean_1_", "Aggregate function with name 'concatWithSeparator' does not exist."),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderUnboundedWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "Aggregate function with name 'concatWithSeparator' does not exist."),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_Function_1__Boolean_1_", "Aggregate function with name 'concatWithSeparator' does not exist."),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "Aggregate function with name 'concatWithSeparator' does not exist."),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithMultiplePartitionsAndOrderWindowMultipleColumns_Function_1__Boolean_1_", "Aggregate function with name 'lead' does not exist."),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithMultiplePartitionsAndOrderWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "Aggregate function with name 'lead' does not exist."),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithPartitionAndMultipleOrderWindowMultipleColumnsWithFilter_Function_1__Boolean_1_", "Aggregate function with name 'lead' does not exist."),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithPartitionAndMultipleOrderWindowMultipleColumns_Function_1__Boolean_1_", "Aggregate function with name 'lead' does not exist."),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithPartitionAndOrderWindowMultipleColumns_Function_1__Boolean_1_", "Aggregate function with name 'lead' does not exist."),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithPartitionAndOrderWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "Aggregate function with name 'lead' does not exist."),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendFloat_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   val,doub,name\n   1,1.2,2.2\n   3,2.3,3.3\n   4,4.2,5.2\n   5,4.2,5.2\n   6,4.5,5.5\n#'\nactual:   '#TDS\n   val,doub,name\n   1,1.2,2.200000047683716\n   3,2.3,3.299999952316284\n   4,4.2,5.199999809265137\n   5,4.2,5.199999809265137\n   6,4.5,5.5\n#'\""),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendFloat_MultipleExpressions_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   val,doub,name\n   1,1.2,2.2\n   3,2.3,3.3\n   4,4.2,5.2\n   5,4.2,5.2\n   6,4.5,5.5\n#'\nactual:   '#TDS\n   val,doub,name\n   1,1.2,2.200000047683716\n   3,2.3,3.299999952316284\n   4,4.2,5.199999809265137\n   5,4.2,5.199999809265137\n   6,4.5,5.5\n#'\""),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_Function_1__Boolean_1_", "Error while executing: Create Table leSchema.tb"),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_filter_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_fold_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_map_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_indexExtraction_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_keyExtraction_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: ClickHouse\""),

            // GroupBy
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleMultiple_Function_1__Boolean_1_", "name is not under aggregate function and not in GROUP BY keys"),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleMultiple_MultipleExpressions_Function_1__Boolean_1_", "name is not under aggregate function and not in GROUP BY keys"),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleSingle_Function_1__Boolean_1_", "name is not under aggregate function and not in GROUP BY keys"),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleSingle_MultipleExpressions_Function_1__Boolean_1_", "name is not under aggregate function and not in GROUP BY keys"),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleMultiple_Function_1__Boolean_1_", "name is not under aggregate function and not in GROUP BY keys"),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleMultiple_MultipleExpressions_Function_1__Boolean_1_", "name is not under aggregate function and not in GROUP BY keys"),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleSingle_Function_1__Boolean_1_", "name is not under aggregate function and not in GROUP BY keys"),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleSingle_MultipleExpressions_Function_1__Boolean_1_", "name is not under aggregate function and not in GROUP BY keys"),

            // Lead
            one("meta::pure::functions::relation::tests::lead::testOLAPWithPartitionAndOrderWindow_Function_1__Boolean_1_", "Aggregate function with name 'lead' does not exist"),

            // Project
            one("meta::pure::functions::relation::tests::project::testSimpleProjectList_Function_1__Boolean_1_", "Error while executing: Create Table leSchema.TypeForProjectTest"),
            one("meta::pure::functions::relation::tests::project::testSimpleProjectWithEmpty_Function_1__Boolean_1_", "Error while executing: Create Table leSchema"),
            one("meta::pure::functions::relation::tests::project::testSimpleProject_Function_1__Boolean_1_", "Error while executing: Create Table leSchema.TypeForProjectTest"),

            // Size
            one("meta::pure::functions::relation::tests::size::testGroupBySize_Function_1__Boolean_1_", "name is not under aggregate function and not in GROUP BY keys"),

            // Filter
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOnIndexExtractionValue_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOnKeyExtractionValue_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOutputFromLambda_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Where, false]) is not supported yet\""),

            // Lag
            one("meta::pure::functions::relation::tests::lag::testOLAPWithPartitionAndOrderWindowUsingLag_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,grp,name,newCol\n   10,0,J,null\n   8,1,H,null\n   6,1,F,H\n   2,1,B,F\n   5,2,E,null\n   1,2,A,E\n   7,3,G,null\n   3,3,C,G\n   4,4,D,null\n   9,5,I,null\n#'\nactual:   '#TDS\n   id,grp,name,newCol\n   10,0,J,J\n   8,1,H,H\n   6,1,F,H\n   2,1,B,F\n   5,2,E,E\n   1,2,A,E\n   7,3,G,G\n   3,3,C,G\n   4,4,D,D\n   9,5,I,I\n#'\""),

            // Lateral
            one("meta::pure::functions::relation::tests::lateral::testLateralJoinAreInnerJoins_Function_1__Boolean_1_", "\"[unsupported-api] Lateral operation not supported for Database Type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::lateral::testLateralJoin_Chained_Function_1__Boolean_1_", "\"[unsupported-api] Lateral operation not supported for Database Type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::lateral::testLateralJoin_Function_1__Boolean_1_", "\"[unsupported-api] Lateral operation not supported for Database Type: ClickHouse\""),

            // Over
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithDifferentDurationUnits_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithDifferentDurationUnits_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_CurrentRow_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_CurrentRow_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NPreceding_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NPreceding_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NPreceding_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NPreceding_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: ClickHouse\""),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_UnboundedFollowing_WithNullValues_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   p,o,i,newCol\n   0,1,10,50\n   0,1,10,50\n   0,null,30,30\n   100,1,10,110\n   100,2,20,110\n   100,3,30,100\n   100,null,20,50\n   100,null,30,50\n   200,1,10,110\n   200,1,10,110\n   200,3,30,90\n   200,3,30,90\n   200,null,10,30\n   200,null,20,30\n   300,1,10,30\n   300,2,20,30\n#'\nactual:   '#TDS\n   p,o,i,newCol\n   0,1,10,50\n   0,1,10,50\n   0,null,30,50\n   100,1,10,110\n   100,2,20,110\n   100,3,30,100\n   100,null,20,100\n   100,null,30,100\n   200,1,10,110\n   200,1,10,110\n   200,3,30,90\n   200,3,30,90\n   200,null,10,90\n   200,null,20,90\n   300,1,10,30\n   300,2,20,30\n#'\""),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_NFollowing_WithNullValues_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   p,o,i,newCol\n   300,2,20,30\n   300,1,10,30\n   200,null,20,30\n   200,null,10,30\n   200,3,30,90\n   200,3,30,90\n   200,1,10,110\n   200,1,10,110\n   100,null,30,50\n   100,null,20,50\n   100,3,30,100\n   100,2,20,110\n   100,1,10,110\n   0,null,30,30\n   0,1,10,50\n   0,1,10,50\n#'\nactual:   '#TDS\n   p,o,i,newCol\n   300,2,20,30\n   300,1,10,30\n   200,null,20,110\n   200,null,10,110\n   200,3,30,60\n   200,3,30,60\n   200,1,10,80\n   200,1,10,80\n   100,null,30,110\n   100,null,20,110\n   100,3,30,50\n   100,2,20,60\n   100,1,10,60\n   0,null,30,50\n   0,1,10,20\n   0,1,10,20\n#'\""),
            one("meta::pure::functions::relation::tests::over::testRange_WithNumbers_CurrentRow_NFollowing_WithoutPartition_WithSingleOrderBy_Function_1__Boolean_1_", "Frame end offset for 'RANGE' frame must be a nonnegative 32-bit integer, '2.1' of type 'Float64' given."),
            one("meta::pure::functions::relation::tests::over::testRange_WithNumbers_NFollowing_NFollowing_WithoutPartition_WithSingleOrderBy_Function_1__Boolean_1_", "'RANGE' frame must be a nonnegative 32-bit integer, '0.5' of type 'Float64' given."),
            one("meta::pure::functions::relation::tests::over::testRange_ExplicitOffsets_WithNullValues_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   p,o,i,newCol\n   0,1,10,20\n   0,1,10,20\n   0,null,30,30\n   100,1,10,30\n   100,2,20,60\n   100,3,30,50\n   100,null,20,50\n   100,null,30,50\n   200,1,10,20\n   200,1,10,20\n   200,3,30,60\n   200,3,30,60\n   200,null,10,30\n   200,null,20,30\n   300,1,10,30\n   300,2,20,30\n#'\nactual:   '#TDS\n   p,o,i,newCol\n   0,1,10,50\n   0,1,10,50\n   0,null,30,50\n   100,1,10,30\n   100,2,20,110\n   100,3,30,100\n   100,null,20,100\n   100,null,30,100\n   200,1,10,20\n   200,1,10,20\n   200,3,30,90\n   200,3,30,90\n   200,null,10,90\n   200,null,20,90\n   300,1,10,30\n   300,2,20,30\n#'\""),

            // Pivot
            pack("meta::pure::functions::relation::tests::pivot", "pivot is not supported for ClickHouse"),

            // Size
            one("meta::pure::functions::relation::tests::size::testComparisonOperationAfterSize_Function_1__Boolean_1_", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::relation::tests::size::testComparisonOperationAfterSize_MultipleExpressions_Function_1__Boolean_1_", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::relation::tests::size::testGroupBySize_MultipleExpressions_Function_1__Boolean_1_", "java.sql.SQLException: Code: 215. DB::Exception: Column t_0.name is not under aggregate function and not in GROUP BY keys. In query t2 AS (SELECT t_0.grp AS grp, concatWithSeparator(t_0.name, '') AS newCol, sum(t_0.id) AS YoCol FROM t AS t_0 GROUP BY grp)."),

            // Write
            one("meta::pure::functions::relation::tests::write::testWrite_Function_1__Boolean_1_", "\"\nexpected: 5\nactual:   -1\""),

            // Flatten
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_LateralJoin_Function_1__Boolean_1_", "\"[unsupported-api] Lateral operation not supported for Database Type: ClickHouse\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_LateralJoin_Nested_Extend_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: ClickHouse\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_LateralJoin_Nested_Function_1__Boolean_1_", "\"[unsupported-api] Lateral operation not supported for Database Type: ClickHouse\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_Scalar_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: ClickHouse\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_Variant_Array_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: ClickHouse\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_Variant_Map_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: ClickHouse\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_Variant_Navigation_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: ClickHouse\"")


            );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.ClickHouse).getFirst())
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
