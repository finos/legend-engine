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
import org.finos.legend.pure.code.core.RelationCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;


public class Test_Relational_SqlServer_RelationFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = RelationCodeRepositoryProvider.relationFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.sqlserverAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Simple Agg
            one("meta::pure::functions::relation::tests::aggregate::testSimpleAggregate_AggColSpecArray_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::size::testGroupBySize_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),

            // Listagg
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderASCUnboundedWindow_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderUnboundedWindow_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderUnboundedWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndUnboundedWindow_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderUnboundedWindowMultipleColumns_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderUnboundedWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"", AdapterQualifier.unsupportedFeature),

            //asOfJoin
            one("meta::pure::functions::relation::tests::asOfJoin::testAsOfJoinWithKeyMatch_Function_1__Boolean_1_", "AsOfJoins are not supported in the generic generator!", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::asOfJoin::testAsOfJoinWithKeyMatch_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_Function_1__Boolean_1_", "AsOfJoins are not supported in the generic generator!", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"", AdapterQualifier.unsupportedFeature),

            //composition
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_ComplexGroupBy_Pivot_Function_1__Boolean_1_", "\"pivot is not supported\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_GroupBy_Pivot_Extend_Sort_Limit_Function_1__Boolean_1_", "\"pivot is not supported\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_Pivot_GroupBy_Extend_Sort_Function_1__Boolean_1_", "\"pivot is not supported\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Pivot_Filter_Function_1__Boolean_1_", "\"pivot is not supported\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testExtendWindowFilter_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,grp,name,newCol\n   2,1,B,16\n   6,1,F,16\n   8,1,H,16\n#'\nactual:   '#TDS\n   id,grp,name,newCol\n   10,0,J,10\n   2,1,B,16\n   6,1,F,16\n   8,1,H,16\n   1,2,A,6\n   5,2,E,6\n   3,3,C,10\n   7,3,G,10\n   4,4,D,4\n   9,5,I,9\n#'\""),
            one("meta::pure::functions::relation::tests::composition::testGroupByFilterExtendFilter_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   grp,rank,sumId,sumRank\n   A,1,9,3\n   A,2,8,3\n#'\nactual:   '#TDS\n   grp,rank,sumId,sumRank\n   A,1,9,3\n   D,1,10,1\n   A,2,8,3\n   B,2,8,2\n#'\""),
            one("meta::pure::functions::relation::tests::composition::testExtendJoinStringOnNull_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            //concatenate
            one("meta::pure::functions::relation::tests::concatenate::testSimpleConcatenate_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"", AdapterQualifier.unsupportedFeature),

            //cumulativeDistribution
            one("meta::pure::functions::relation::tests::cumulativeDistribution::testOLAPWithPartitionAndOrderCummulativeDistribution_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"round(%s, 0)\"\""),

            //distinct
            one("meta::pure::functions::relation::tests::distinct::testDistinctAll_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::distinct::testDistinctMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::distinct::testDistinctSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),

            //drop
            one("meta::pure::functions::relation::tests::drop::testSimpleDrop_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::drop::testSimpleDropShared_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near 'limit'."),

            //extend
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggIntegerWithPartitionAndOrderWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggNoWindowChainedWithSimple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggNoWindowMultipleColums_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggNoWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithOrderWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithPartitionAndMultipleOrderWindowMultipleColumnsWithFilter_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: ORDER BY list of RANGE window frame has total size of 1028 bytes. Largest size supported is 900 bytes."),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithPartitionAndMultipleOrderWindowMultipleColumns_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: ORDER BY list of RANGE window frame has total size of 1028 bytes. Largest size supported is 900 bytes."),

            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithMultiplePartitionsAndOrderWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithPartitionAndOrderWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendFloat_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendInt_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendStr_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testSimpleMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),

            //filter
            one("meta::pure::functions::relation::tests::filter::testSimpleFilter_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),

            //groupBy
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleMultiple_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleSingle_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleMultiple_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleSingle_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),

            // Nth Value
            one("meta::pure::functions::relation::tests::nth::testOLAPWithPartitionAndOrderNthWindow2_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'nth_value' is not a recognized built-in function name."),
            one("meta::pure::functions::relation::tests::nth::testOLAPWithPartitionAndOrderNthWindow_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'nth_value' is not a recognized built-in function name."),

            // Over
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_CurrentRow_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: insert into leSchema.tb_"),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_CurrentRow_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: insert into leSchema.tb_"),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: insert into leSchema.tb_"),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: insert into leSchema.tb_"),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_CurrentRow_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "Error while executing: insert into leSchema.tb_"),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_CurrentRow_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "Error while executing: insert into leSchema.tb_"),

            //join
            one("meta::pure::functions::relation::tests::join::testSimpleJoin_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::join::testJoin_forFailedJoinWhenNoRowsMatchJoinCondition_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),

            //limit
            one("meta::pure::functions::relation::tests::limit::testSimpleLimit_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),

            //pivot
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleMultiple_Function_1__Boolean_1_", "\"pivot is not supported\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleMultiple_Dynamic_Aggregation_Function_1__Boolean_1_", "\"pivot is not supported\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleSingle_Function_1__Boolean_1_", "\"pivot is not supported\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleMultiple_Function_1__Boolean_1_", "\"pivot is not supported\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleMultiple_Dynamic_Aggregation_Function_1__Boolean_1_", "\"pivot is not supported\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleSingle_Function_1__Boolean_1_", "\"pivot is not supported\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),

            //rename
            one("meta::pure::functions::relation::tests::rename::testSimpleRename_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),

            //select
            one("meta::pure::functions::relation::tests::select::testMultiColsSelect_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::select::testSelectAll_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::select::testSingleColSelect_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),

            //size
            one("meta::pure::functions::relation::tests::size::testComparisonOperationAfterSize_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '>'."),
            one("meta::pure::functions::relation::tests::size::testComparisonOperationAfterSize_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::size::testGroupBySize_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::size::testSimpleSize_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),

            //Slice
            one("meta::pure::functions::relation::tests::slice::testSimpleSlice_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::slice::testSimpleSliceShared_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near 'limit'."),

            //Sort
            one("meta::pure::functions::relation::tests::sort::testSimpleSort_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\"",  AdapterQualifier.unsupportedFeature),

            // SqlServer does not support range frame with explicit offsets on either side
            one("meta::pure::functions::relation::tests::over::testRange_NFollowing_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: RANGE is only supported with UNBOUNDED and CURRENT ROW window frame delimiters."),
            one("meta::pure::functions::relation::tests::over::testRange_NFollowing_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: RANGE is only supported with UNBOUNDED and CURRENT ROW window frame delimiters."),
            one("meta::pure::functions::relation::tests::over::testRange_NFollowing_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: RANGE is only supported with UNBOUNDED and CURRENT ROW window frame delimiters."),
            one("meta::pure::functions::relation::tests::over::testRange_NFollowing_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: RANGE is only supported with UNBOUNDED and CURRENT ROW window frame delimiters."),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: RANGE is only supported with UNBOUNDED and CURRENT ROW window frame delimiters."),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: RANGE is only supported with UNBOUNDED and CURRENT ROW window frame delimiters."),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_NPreceding_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: RANGE is only supported with UNBOUNDED and CURRENT ROW window frame delimiters."),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_NPreceding_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: RANGE is only supported with UNBOUNDED and CURRENT ROW window frame delimiters."),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_UnboundedFollowing_WithNullValues_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: RANGE is only supported with UNBOUNDED and CURRENT ROW window frame delimiters."),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: RANGE is only supported with UNBOUNDED and CURRENT ROW window frame delimiters."),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: RANGE is only supported with UNBOUNDED and CURRENT ROW window frame delimiters."),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: RANGE is only supported with UNBOUNDED and CURRENT ROW window frame delimiters."),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_NPreceding_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: RANGE is only supported with UNBOUNDED and CURRENT ROW window frame delimiters."),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_NPreceding_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: RANGE is only supported with UNBOUNDED and CURRENT ROW window frame delimiters."),
            one("meta::pure::functions::relation::tests::over::testRange_WithNumbers_CurrentRow_NFollowing_WithoutPartition_WithSingleOrderBy_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '2.1'."),
            one("meta::pure::functions::relation::tests::over::testRange_WithNumbers_NFollowing_NFollowing_WithoutPartition_WithSingleOrderBy_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '0.5'."),
            one("meta::pure::functions::relation::tests::over::testRange_ExplicitOffsets_WithNullValues_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: RANGE is only supported with UNBOUNDED and CURRENT ROW window frame delimiters."),
            one("meta::pure::functions::relation::tests::over::testRange_ExplicitOffsets_WithNullValues_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: RANGE is only supported with UNBOUNDED and CURRENT ROW window frame delimiters."),
            one("meta::pure::functions::relation::tests::over::testRange_UnboundedPreceding_NFollowing_WithNullValues_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: RANGE is only supported with UNBOUNDED and CURRENT ROW window frame delimiters."),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithDifferentDurationUnits_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithDifferentDurationUnits_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_CurrentRow_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_CurrentRow_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NPreceding_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NPreceding_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NPreceding_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NPreceding_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRange_NPreceding_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: RANGE is only supported with UNBOUNDED and CURRENT ROW window frame delimiters."),
            one("meta::pure::functions::relation::tests::over::testRows_UnboundedPreceding_UnboundedFollowing_WithMultiplePartitions_WithoutOrderBy_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Window frame with ROWS or RANGE must have an ORDER BY clause."),
            one("meta::pure::functions::relation::tests::over::testRows_UnboundedPreceding_UnboundedFollowing_WithSinglePartition_WithoutOrderBy_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Window frame with ROWS or RANGE must have an ORDER BY clause."),
            one("meta::pure::functions::relation::tests::composition::testVariantArrayColumn_reverse_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantArrayColumn_sort_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_extend_indexExtraction_filter_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: SqlServer\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_functionComposition_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Where, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testExtendJoinStringOnNull_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_isEmpty_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_isNotEmpty_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_indexOf_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_position' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_slice_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Slice_Size_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near 'limit'."),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_contains_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_contains' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_distinct_removeDuplicates_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_projectModelProperty_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: SqlServer\""),

            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOnIndexExtractionValue_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: SqlServer\""),
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOnKeyExtractionValue_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: SqlServer\""),
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOutputFromLambda_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Where, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            one("meta::pure::functions::relation::tests::extend::testVariantColumn_map_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_fold_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: SqlServer\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_filter_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_indexExtraction_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: SqlServer\""),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_keyExtraction_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: SqlServer\""),

            pack("meta::pure::functions::relation::tests::pivot", "pivot is not supported"),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB SqlServer\""),

            // lateral
            pack("meta::pure::functions::relation::tests::lateral", "Common table expression not supported on DB SqlServer", AdapterQualifier.unsupportedFeature),

            // flatten
            pack("meta::pure::functions::relation::variant::tests::flatten", "[unsupported-api] Semi structured array element processing not supported for Database Type: SqlServer", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_LateralJoin_Function_1__Boolean_1_", "\"[unsupported-api] Lateral operation not supported for Database Type: SqlServer\""),
            one("meta::pure::functions::relation::variant::tests::flatten::testFlatten_LateralJoin_Nested_Function_1__Boolean_1_", "\"[unsupported-api] Lateral operation not supported for Database Type: SqlServer\"")
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
