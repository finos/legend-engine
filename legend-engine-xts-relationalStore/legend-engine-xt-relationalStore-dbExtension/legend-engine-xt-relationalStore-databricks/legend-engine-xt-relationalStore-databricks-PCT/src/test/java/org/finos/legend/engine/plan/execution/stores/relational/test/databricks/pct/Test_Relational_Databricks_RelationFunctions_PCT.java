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

package org.finos.legend.engine.plan.execution.stores.relational.test.databricks.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.pure.runtime.testConnection.CoreExternalTestConnectionCodeRepositoryProvider;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.RelationCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Databricks_RelationFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = RelationCodeRepositoryProvider.relationFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.databricksAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(

            //asOfJoin
            one("meta::pure::functions::relation::tests::asOfJoin::testAsOfJoinWithKeyMatch_Function_1__Boolean_1_", "AsOfJoins are not supported in the generic generator!"),
            one("meta::pure::functions::relation::tests::asOfJoin::testAsOfJoinWithKeyMatch_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_Function_1__Boolean_1_", "AsOfJoins are not supported in the generic generator!"),
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),

            //composition
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_ComplexGroupBy_Pivot_Function_1__Boolean_1_", "\"pivot is not supported\""),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_GroupBy_Pivot_Extend_Sort_Limit_Function_1__Boolean_1_", "\"pivot is not supported\""),
            one("meta::pure::functions::relation::tests::composition::test_Pivot_Filter_Function_1__Boolean_1_", "\"pivot is not supported\""),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_Pivot_GroupBy_Extend_Sort_Function_1__Boolean_1_", "\"pivot is not supported\""),

            //concatenate
            one("meta::pure::functions::relation::tests::concatenate::testSimpleConcatenate_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),

            //distinct
            one("meta::pure::functions::relation::tests::distinct::testDistinctAll_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::distinct::testDistinctMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::distinct::testDistinctSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),

            //drop
            one("meta::pure::functions::relation::tests::drop::testSimpleDrop_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),

            //extend
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_Function_1__Boolean_1_", "[UNSUPPORTED_EXPR_FOR_WINDOW] Expression \"array_join(array(name), )\" not supported within a window function."),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_Function_1__Boolean_1_", "[UNSUPPORTED_EXPR_FOR_WINDOW] Expression \"array_join(array(name), )\" not supported within a window function."),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendFloat_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   val,doub,name\n   1,1.2,2.2\n   3,2.3,3.3\n   4,4.2,5.2\n   5,4.2,5.2\n   6,4.5,5.5\n#'\nactual:   '#TDS\n   val,doub,name\n   1,1.2,2.200000047683716\n   3,2.3,3.299999952316284\n   4,4.2,5.199999809265137\n   5,4.2,5.199999809265137\n   6,4.5,5.5\n#'\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggIntegerWithPartitionAndOrderWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggNoWindowChainedWithSimple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggNoWindowMultipleColums_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggNoWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithOrderWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithMultiplePartitionsAndOrderWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithPartitionAndOrderWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendFloat_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendInt_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendStr_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::extend::testSimpleMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),

            //filter
            one("meta::pure::functions::relation::tests::filter::testSimpleFilter_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),

            //groupBy
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleMultiple_Function_1__Boolean_1_", "[MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned."),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleSingle_Function_1__Boolean_1_", "[MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned."),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleMultiple_Function_1__Boolean_1_", "[MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned."),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleSingle_Function_1__Boolean_1_", "[MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned."),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),

            //join
            one("meta::pure::functions::relation::tests::join::testSimpleJoin_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::join::testJoin_forFailedJoinWhenNoRowsMatchJoinCondition_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),

            //limit
            one("meta::pure::functions::relation::tests::limit::testSimpleLimit_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),

            //pivot
            pack("meta::pure::functions::relation::tests::pivot", "\"pivot is not supported\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),

            //rename
            one("meta::pure::functions::relation::tests::rename::testSimpleRename_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),

            //select
            one("meta::pure::functions::relation::tests::select::testMultiColsSelect_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::select::testSelectAll_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::select::testSingleColSelect_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_Function_1__Boolean_1_", "Error while executing: Create Table leSchema.tb_"),
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),

            //size
            one("meta::pure::functions::relation::tests::size::testGroupBySize_Function_1__Boolean_1_", "[MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned."),
            one("meta::pure::functions::relation::tests::size::testComparisonOperationAfterSize_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::size::testGroupBySize_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),
            one("meta::pure::functions::relation::tests::size::testSimpleSize_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),

            //Slice
            one("meta::pure::functions::relation::tests::slice::testSimpleSlice_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\""),

            //sort
            one("meta::pure::functions::relation::tests::sort::testSimpleSort_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB Databricks\"")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Databricks).getFirst())
        );
    }

    @Override
    public ReportScope getReportScope()
    {
        return reportScope;
    }

    @Override
    public MutableList<ExclusionSpecification> expectedFailures()
    {
        return expectedFailures;
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
