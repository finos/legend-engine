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
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
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
            // Listagg
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderASCUnboundedWindow_Function_1__Boolean_1_", "[UNSUPPORTED_EXPR_FOR_WINDOW] Expression \"array_join(array(name), )\" not supported within a window function. SQLSTATE: 42P20;", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderUnboundedWindow_Function_1__Boolean_1_", "[UNSUPPORTED_EXPR_FOR_WINDOW] Expression \"array_join(array(name), )\" not supported within a window function. SQLSTATE: 42P20;", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderUnboundedWindow_MultipleExpressions_Function_1__Boolean_1_", "[UNSUPPORTED_EXPR_FOR_WINDOW] Expression \"array_join(array(name), )\" not supported within a window function. SQLSTATE: 42P20;", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndUnboundedWindow_Function_1__Boolean_1_", "[UNSUPPORTED_EXPR_FOR_WINDOW] Expression \"array_join(array(name), )\" not supported within a window function. SQLSTATE: 42P20;", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderUnboundedWindowMultipleColumns_Function_1__Boolean_1_", "[UNSUPPORTED_EXPR_FOR_WINDOW] Expression \"array_join(array(name), )\" not supported within a window function. SQLSTATE: 42P20;", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderUnboundedWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "[UNSUPPORTED_EXPR_FOR_WINDOW] Expression \"array_join(array(name), )\" not supported within a window function. SQLSTATE: 42P20;", AdapterQualifier.unsupportedFeature),

            //asOfJoin
            one("meta::pure::functions::relation::tests::asOfJoin::testAsOfJoinWithKeyMatch_Function_1__Boolean_1_", "AsOfJoins are not supported in the generic generator!", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_Function_1__Boolean_1_", "AsOfJoins are not supported in the generic generator!", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::asOfJoin::testAsOfJoinWithKeyMatch_MultipleExpressions_Function_1__Boolean_1_", "\"AsOfJoins are not supported in the generic generator!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_MultipleExpressions_Function_1__Boolean_1_", "\"AsOfJoins are not supported in the generic generator!\"", AdapterQualifier.unsupportedFeature),

            //composition
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_ComplexGroupBy_Pivot_Function_1__Boolean_1_", "\"pivot is not supported\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_GroupBy_Pivot_Extend_Sort_Limit_Function_1__Boolean_1_", "\"pivot is not supported\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Pivot_Filter_Function_1__Boolean_1_", "\"pivot is not supported\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_Pivot_GroupBy_Extend_Sort_Function_1__Boolean_1_", "\"pivot is not supported\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantArrayColumn_reverse_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantArrayColumn_sort_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_extend_indexExtraction_filter_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_functionComposition_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Where, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testExtendJoinStringOnNull_Function_1__Boolean_1_", "[UNSUPPORTED_EXPR_FOR_WINDOW]", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_isEmpty_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_isNotEmpty_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_indexOf_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_position' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
    
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOnIndexExtractionValue_Function_1__Boolean_1_", "java.sql.SQLException: Binder Error: No function matches the given name and argument types 'len(BIGINT)'. You might need to add explicit type casts.\n", AdapterQualifier.needsInvestigation),

            //extend
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_Function_1__Boolean_1_", "[UNSUPPORTED_EXPR_FOR_WINDOW] Expression \"array_join(array(name), )\" not supported within a window function.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_Function_1__Boolean_1_", "[UNSUPPORTED_EXPR_FOR_WINDOW] Expression \"array_join(array(name), )\" not supported within a window function.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendFloat_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   val,doub,name\n   1,1.2,2.2\n   3,2.3,3.3\n   4,4.2,5.2\n   5,4.2,5.2\n   6,4.5,5.5\n#'\nactual:   '#TDS\n   val,doub,name\n   1,1.2,2.200000047683716\n   3,2.3,3.299999952316284\n   4,4.2,5.199999809265137\n   5,4.2,5.199999809265137\n   6,4.5,5.5\n#'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "[UNSUPPORTED_EXPR_FOR_WINDOW] Expression \"array_join(array(name), )\" not supported within a window function. SQLSTATE: 42P20;", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_filter_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_fold_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_map_Function_1__Boolean_1_", "\"[unsupported-api] The function 'toVariant' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendFloat_MultipleExpressions_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   val,doub,name\n   1,1.2,2.2\n   3,2.3,3.3\n   4,4.2,5.2\n   5,4.2,5.2\n   6,4.5,5.5\n#'\nactual:   '#TDS\n   val,doub,name\n   1,1.2,2.200000047683716\n   3,2.3,3.299999952316284\n   4,4.2,5.199999809265137\n   5,4.2,5.199999809265137\n   6,4.5,5.5\n#'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_MultipleExpressions_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [UNSUPPORTED_EXPR_FOR_WINDOW] org.apache.spark.sql.catalyst.ExtendedAnalysisException: [UNSUPPORTED_EXPR_FOR_WINDOW] Expression \"array_join(array(name), )\" not supported within a window function. SQLSTATE: 42P20;", AdapterQualifier.needsInvestigation),

            //filter
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOnIndexExtractionValue_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOnKeyExtractionValue_Function_1__Boolean_1_", "\"[unsupported-api] Semi structured array element processing not supported for Database Type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::filter::testVariantColumn_filterOutputFromLambda_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_size' (state: [Where, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            //groupBy
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleMultiple_Function_1__Boolean_1_", "[MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleSingle_Function_1__Boolean_1_", "[MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleMultiple_Function_1__Boolean_1_", "[MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleSingle_Function_1__Boolean_1_", "[MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleSingle_MultipleExpressions_Function_1__Boolean_1_", "[MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleMultiple_MultipleExpressions_Function_1__Boolean_1_", "[MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleSingle_MultipleExpressions_Function_1__Boolean_1_", "[MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleMultiple_MultipleExpressions_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [MISSING_AGGREGATION] org.apache.spark.sql.catalyst.ExtendedAnalysisException: [MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.", AdapterQualifier.needsInvestigation),

            //aggregate
            one("meta::pure::functions::relation::tests::aggregate::testSimpleAggregate_AggColSpecArray_Function_1__Boolean_1_", "[MISSING_GROUP_BY] The query does not include a GROUP BY clause. Add GROUP BY or turn it into the window functions using OVER clauses.", AdapterQualifier.unsupportedFeature),

            //pivot
            pack("meta::pure::functions::relation::tests::pivot", "\"pivot is not supported\"", AdapterQualifier.unsupportedFeature),

            //select
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_Function_1__Boolean_1_", "Error while executing: Create Table leSchema.tb_", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_MultipleExpressions_Function_1__Boolean_1_", "Error while executing: Create Table leSchema.tb_", AdapterQualifier.needsInvestigation),

            //size
            one("meta::pure::functions::relation::tests::size::testGroupBySize_Function_1__Boolean_1_", "[MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::size::testGroupBySize_MultipleExpressions_Function_1__Boolean_1_", "[MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_indexExtraction_Function_1__Boolean_1_", "[unsupported-api] Semi structured array element processing not supported for Database Type: Databricks", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::relation::tests::extend::testVariantColumn_keyExtraction_Function_1__Boolean_1_", "[unsupported-api] Semi structured array element processing not supported for Database Type: Databricks", AdapterQualifier.needsImplementation),

            // Result TDS decimal precision tolerance needs to be implemented
            one("meta::pure::functions::relation::tests::over::testRange_WithNumbers_CurrentRow_NFollowing_WithoutPartition_WithSingleOrderBy_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   menu_category,menu_cogs_usd,sum_cogs\n   Beverage,0.5,10.65\n   Beverage,0.65,9.65\n   Beverage,0.75,9.0\n   Dessert,0.5,10.65\n   Dessert,1.0,11.25\n   Dessert,1.25,10.25\n   Dessert,2.5,9.5\n   Dessert,3.0,7.0\n   Snack,1.25,10.25\n   Snack,2.25,11.75\n   Snack,4.0,4.0\n#'\nactual:   '#TDS\n   menu_category,menu_cogs_usd,sum_cogs\n   Beverage,0.5,10.649999976158142\n   Beverage,0.65,9.649999976158142\n   Beverage,0.75,9.0\n   Dessert,0.5,10.649999976158142\n   Dessert,1.0,11.25\n   Dessert,1.25,10.25\n   Dessert,2.5,9.5\n   Dessert,3.0,7.0\n   Snack,1.25,10.25\n   Snack,2.25,11.75\n   Snack,4.0,4.0\n#'\"", AdapterQualifier.needsInvestigation),

            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithDifferentDurationUnits_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithDifferentDurationUnits_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NFollowing_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_CurrentRow_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_CurrentRow_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NPreceding_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_NPreceding_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_UnboundedFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_NPreceding_UnboundedFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NFollowing_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NFollowing_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NPreceding_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_UnboundedPreceding_NPreceding_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "\"Window function with range frame using interval is not supported for this database type: Databricks\"", AdapterQualifier.unsupportedFeature),

            // lateral
            one("meta::pure::functions::relation::tests::lateral::testLateralJoinAreInnerJoins_Function_1__Boolean_1_", "\"[unsupported-api] Lateral operation not supported for Database Type: Databricks\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::lateral::testLateralJoin_Function_1__Boolean_1_", "\"[unsupported-api] Lateral operation not supported for Database Type: Databricks\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::lateral::testLateralJoin_Chained_Function_1__Boolean_1_", "\"[unsupported-api] Lateral operation not supported for Database Type: Databricks\"", AdapterQualifier.needsInvestigation),

            // flatten
            pack("meta::pure::functions::relation::variant::tests::flatten", "Datatype to SQL text not supported for Database Type: Databricks", AdapterQualifier.unsupportedFeature)
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
