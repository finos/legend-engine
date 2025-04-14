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
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_Function_1__Boolean_1_", "AsOfJoins are not supported in the generic generator!"),

            //composition
            one("meta::pure::functions::relation::tests::composition::testFilterPostProject_Function_1__Boolean_1_", "\nexpected: '#TDS\n   legalName,firstName\n   Firm X,Peter\n   Firm X,John\n   Firm X,John\n   Firm X,Anthony\n#'\nactual:   '#TDS\n   legalName,firstName\n   Firm X,John\n   Firm X,Peter\n   Firm X,Anthony\n   Firm X,John\n#'"),
            one("meta::pure::functions::relation::tests::composition::test_Distinct_GroupBy_Filter_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [MISSING_AGGREGATION] org.apache.spark.sql.catalyst.ExtendedAnalysisException: [MISSING_AGGREGATION] The non-aggregating expression \"newCol\" is based on columns which are not participating in the GROUP BY clause."),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_ComplexGroupBy_Pivot_Function_1__Boolean_1_", "\nexpected: '#TDS\n   city,country,2011__|__newCol,4022__|__newCol,6035__|__newCol\n   LDN,UK,3000,null,null\n   NYC,USA,null,null,20200\n   SAN,USA,null,2600,null\n#'\nactual:   '#TDS\n   city,country,year,treePlanted\n   LDN,UK,2011,3000\n   NYC,USA,6035,20200\n   SAN,USA,4022,2600\n#'"),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_GroupBy_Pivot_Extend_Sort_Limit_Function_1__Boolean_1_", "\nexpected: '#TDS\n   city,country,2011__|__newCol,2012__|__newCol,newCol\n   LDN,UK,3000,null,LDN_0\n   NYC,USA,5000,15200,NYC_0\n   SAN,USA,2600,null,SAN_0\n#'\nactual:   '#TDS\n   year,city,country,treePlanted,newCol\n   2011,LDN,UK,3000,LDN_0\n   2011,NYC,USA,5000,NYC_0\n   2012,NYC,USA,15200,NYC_0\n#'"),
            one("meta::pure::functions::relation::tests::composition::test_GroupBy_Filter_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [MISSING_AGGREGATION] org.apache.spark.sql.catalyst.ExtendedAnalysisException: [MISSING_AGGREGATION] The non-aggregating expression \"newCol\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(newCol)\" if you do not care which of the values within a group is returned. SQLSTATE: 42803"),
            one("meta::pure::functions::relation::tests::composition::test_Pivot_Filter_Function_1__Boolean_1_", "\nexpected: '#TDS\n   city,country,2000__|__newCol,2011__|__newCol,2012__|__newCol\n   NYC,USA,15000,5000,15200\n#'\nactual:   '#TDS\n   city,country,year,treePlanted\n   NYC,USA,2000,5000\n   NYC,USA,2012,7600\n   NYC,USA,2011,5000\n   NYC,USA,2000,10000\n   NYC,USA,2012,7600\n#'"),

            //concatenate
            one("meta::pure::functions::relation::tests::concatenate::testSimpleConcatenateShared_Function_1__Boolean_1_", "\nexpected: '#TDS\n   val,str\n   1,a\n   3,ewe\n   4,qw\n   5,qwea\n   6,eeewe\n   7,qqwew\n#'\nactual:   '#TDS\n   val,str\n   3,ewe\n   4,qw\n   1,a\n   6,eeewe\n   7,qqwew\n   5,qwea\n#'"),

            //drop
            one("meta::pure::functions::relation::tests::drop::testSimpleDropShared_Function_1__Boolean_1_", "\nexpected: '#TDS\n   val,str\n   5,wwe\n   6,weq\n#'\nactual:   '#TDS\n   val,str\n   4,qw\n   5,wwe\n#'"),

            //extend
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [UNSUPPORTED_EXPR_FOR_WINDOW] org.apache.spark.sql.catalyst.ExtendedAnalysisException: [UNSUPPORTED_EXPR_FOR_WINDOW] Expression \"array_join(array(name), )\" not supported within a window function. SQLSTATE: 42P20; line 2 pos 136"),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [UNSUPPORTED_EXPR_FOR_WINDOW] org.apache.spark.sql.catalyst.ExtendedAnalysisException: [UNSUPPORTED_EXPR_FOR_WINDOW] Expression \"array_join(array(name), )\" not supported within a window function. SQLSTATE: 42P20; line 2 pos 136"),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendFloat_Function_1__Boolean_1_", "\nexpected: '#TDS\n   val,doub,name\n   1,1.2,2.2\n   3,2.3,3.3\n   4,4.2,5.2\n   5,4.2,5.2\n   6,4.5,5.5\n#'\nactual:   '#TDS\n   val,doub,name\n   5,4.2,5.199999809265137\n   4,4.2,5.199999809265137\n   3,2.3,3.299999952316284\n   1,1.2,2.200000047683716\n   6,4.5,5.5\n#'"),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendInt_Function_1__Boolean_1_", "\nexpected: '#TDS\n   val,str,name\n   1,a,2\n   3,ewe,4\n   4,qw,5\n   5,wwe,6\n   6,weq,7\n#'\nactual:   '#TDS\n   val,str,name\n   5,wwe,6\n   6,weq,7\n   3,ewe,4\n   4,qw,5\n   1,a,2\n#'"),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendStrShared_Function_1__Boolean_1_", "\nexpected: '#TDS\n   val,str,name\n   1,a,a1\n   3,ewe,ewe3\n   4,qw,qw4\n   5,wwe,wwe5\n   6,weq,weq6\n#'\nactual:   '#TDS\n   val,str,name\n   6,weq,weq6\n   5,wwe,wwe5\n   1,a,a1\n   3,ewe,ewe3\n   4,qw,qw4\n#'"),
            one("meta::pure::functions::relation::tests::extend::testSimpleMultipleColumns_Function_1__Boolean_1_", "\nexpected: '#TDS\n   val,str,name,other\n   1,a,2,a_ext\n   3,ewe,4,ewe_ext\n   4,qw,5,qw_ext\n   5,wwe,6,wwe_ext\n   6,weq,7,weq_ext\n#'\nactual:   '#TDS\n   val,str,name,other\n   5,wwe,6,wwe_ext\n   6,weq,7,weq_ext\n   3,ewe,4,ewe_ext\n   4,qw,5,qw_ext\n   1,a,2,a_ext\n#'"),

            //filter
            one("meta::pure::functions::relation::tests::filter::testSimpleFilterShared_Function_1__Boolean_1_", "\nexpected: '#TDS\n   val\n   3\n   4\n#'\nactual:   '#TDS\n   val\n   4\n   3\n#'"),

            //groupBy
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleMultiple_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [MISSING_AGGREGATION] org.apache.spark.sql.catalyst.ExtendedAnalysisException: [MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned. SQLSTATE: 42803"),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleSingle_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [MISSING_AGGREGATION] org.apache.spark.sql.catalyst.ExtendedAnalysisException: [MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned. SQLSTATE: 42803"),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleMultiple_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [MISSING_AGGREGATION] org.apache.spark.sql.catalyst.ExtendedAnalysisException: [MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned. SQLSTATE: 42803"),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleSingle_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [MISSING_AGGREGATION] org.apache.spark.sql.catalyst.ExtendedAnalysisException: [MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned. SQLSTATE: 42803"),

            //limit
            one("meta::pure::functions::relation::tests::limit::testSimpleLimitShared_Function_1__Boolean_1_", "\nexpected: '#TDS\n   val,str\n   1,a\n   3,ewe\n   4,qw\n#'\nactual:   '#TDS\n   val,str\n   6,weq\n   3,ewe\n   4,qw\n#'"),

            //pivot
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleMultiple_Function_1__Boolean_1_", "\nexpected: '#TDS\n   year,UK__|__LDN__|__sum,UK__|__LDN__|__count,USA__|__NYC__|__sum,USA__|__NYC__|__count,USA__|__SAN__|__sum,USA__|__SAN__|__count\n   2000,null,null,15000,2,2000,1\n   2011,3000,1,5000,1,2600,2\n   2012,null,null,15200,2,null,null\n#'\nactual:   '#TDS\n   city,country,year,treePlanted\n   SAN,USA,2000,2000\n   NYC,USA,2000,10000\n   NYC,USA,2000,5000\n   NYC,USA,2011,5000\n   SAN,USA,2011,100\n   SAN,USA,2011,2500\n   LDN,UK,2011,3000\n   NYC,USA,2012,7600\n   NYC,USA,2012,7600\n#'"),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleSingle_Function_1__Boolean_1_", "\nexpected: '#TDS\n   year,UK__|__LDN__|__sum,USA__|__NYC__|__sum,USA__|__SAN__|__sum\n   2000,null,15000,2000\n   2011,3000,5000,2600\n   2012,null,15200,null\n#'\nactual:   '#TDS\n   city,country,year,treePlanted\n   NYC,USA,2000,5000\n   SAN,USA,2000,2000\n   NYC,USA,2000,10000\n   SAN,USA,2011,100\n   SAN,USA,2011,2500\n   NYC,USA,2011,5000\n   LDN,UK,2011,3000\n   NYC,USA,2012,7600\n   NYC,USA,2012,7600\n#'"),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleMultiple_Function_1__Boolean_1_", "\nexpected: '#TDS\n   city,country,2000__|__sum,2000__|__count,2011__|__sum,2011__|__count,2012__|__sum,2012__|__count\n   LDN,UK,null,null,3000,1,null,null\n   NYC,USA,15000,2,5000,1,15200,2\n   SAN,USA,2000,1,2600,2,null,null\n#'\nactual:   '#TDS\n   city,country,year,treePlanted\n   LDN,UK,2011,3000\n   NYC,USA,2011,5000\n   NYC,USA,2012,7600\n   NYC,USA,2000,5000\n   NYC,USA,2012,7600\n   NYC,USA,2000,10000\n   SAN,USA,2011,2500\n   SAN,USA,2000,2000\n   SAN,USA,2011,100\n#'"),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleSingle_Function_1__Boolean_1_", "\nexpected: '#TDS\n   city,country,2000__|__newCol,2011__|__newCol,2012__|__newCol\n   LDN,UK,null,3000,null\n   NYC,USA,15000,5000,15200\n   SAN,USA,2000,2600,null\n#'\nactual:   '#TDS\n   city,country,year,treePlanted\n   LDN,UK,2011,3000\n   NYC,USA,2012,7600\n   NYC,USA,2012,7600\n   NYC,USA,2011,5000\n   NYC,USA,2000,10000\n   NYC,USA,2000,5000\n   SAN,USA,2000,2000\n   SAN,USA,2011,2500\n   SAN,USA,2011,100\n#'"),

            //project
            one("meta::pure::functions::relation::tests::project::testSimpleProjectWithEmpty_Function_1__Boolean_1_", "\nexpected: '#TDS\n   one,two,three\n   ok,no,null\n   ok,other,null\n#'\nactual:   '#TDS\n   one,two,three\n   ok,other,null\n   ok,no,null\n#'"),

            //rename
            one("meta::pure::functions::relation::tests::rename::testSimpleRenameShared_Function_1__Boolean_1_", "\nexpected: '#TDS\n   val,newStr\n   1,a\n   3,ewe\n   4,qw\n   5,wwe\n   6,weq\n#'\nactual:   '#TDS\n   val,newStr\n   6,weq\n   5,wwe\n   1,a\n   3,ewe\n   4,qw\n#'"),

            //select
            one("meta::pure::functions::relation::tests::select::testMultiColsSelectShared_Function_1__Boolean_1_", "\nexpected: '#TDS\n   val,other\n   1,a\n   3,b\n   4,c\n   5,d\n   6,e\n#'\nactual:   '#TDS\n   val,other\n   6,e\n   5,d\n   1,a\n   3,b\n   4,c\n#'"),
            one("meta::pure::functions::relation::tests::select::testSelectAll_Function_1__Boolean_1_", "\nexpected: '#TDS\n   val,str,other\n   1,a,a\n   3,ewe,b\n   4,qw,c\n   5,wwe,d\n   6,weq,e\n#'\nactual:   '#TDS\n   val,str,other\n   5,wwe,d\n   3,ewe,b\n   1,a,a\n   6,weq,e\n   4,qw,c\n#'"),
            one("meta::pure::functions::relation::tests::select::testSingleColSelectShared_Function_1__Boolean_1_", "\nexpected: '#TDS\n   str\n   a\n   ewe\n   qw\n   wwe\n   weq\n#'\nactual:   '#TDS\n   str\n   weq\n   wwe\n   ewe\n   qw\n   a\n#'"),
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_Function_1__Boolean_1_", "Error while executing: Create Table leSchema.tb_322897_1737750977292(val INT,str VARCHAR(200),other kind VARCHAR(200));"),

            //size
            one("meta::pure::functions::relation::tests::size::testGroupBySize_Function_1__Boolean_1_", "java.sql.SQLException: [Databricks][DatabricksJDBCDriver](500051) ERROR processing query/statement. Error Code: 0, SQL state: org.apache.hive.service.cli.HiveSQLException: Error running query: [MISSING_AGGREGATION] org.apache.spark.sql.catalyst.ExtendedAnalysisException: [MISSING_AGGREGATION] The non-aggregating expression \"name\" is based on columns which are not participating in the GROUP BY clause.\nAdd the columns or the expression to the GROUP BY, aggregate the expression, or use \"any_value(name)\" if you do not care which of the values within a group is returned. SQLSTATE: 42803"),

            //Slice
            one("meta::pure::functions::relation::tests::slice::testSimpleSliceShared_Function_1__Boolean_1_", "\nexpected: '#TDS\n   val,str\n   3,ewe\n   4,qw\n#'\nactual:   '#TDS\n   val,str\n   4,qw\n   6,weq\n#'")
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
