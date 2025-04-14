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

package org.finos.legend.engine.plan.execution.stores.relational.test.memsql.pct;

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


public class Test_Relational_MemSQL_RelationFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = RelationCodeRepositoryProvider.relationFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.memsqlAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            //asOfJoin
            one("meta::pure::functions::relation::tests::asOfJoin::testAsOfJoinWithKeyMatch_Function_1__Boolean_1_", "AsOfJoins are not supported in the generic generator!"),
            one("meta::pure::functions::relation::tests::asOfJoin::testAsOfJoinWithKeyMatch_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_Function_1__Boolean_1_", "AsOfJoins are not supported in the generic generator!"),
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),

            //composition
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_ComplexGroupBy_Pivot_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   city,country,2011__|__newCol,4022__|__newCol,6035__|__newCol\n   LDN,UK,3000,null,null\n   NYC,USA,null,null,20200\n   SAN,USA,null,2600,null\n#'\nactual:   '#TDS\n   city,country,year,treePlanted\n   LDN,UK,2011,3000\n   NYC,USA,6035,20200\n   SAN,USA,4022,2600\n#'\""),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_GroupBy_Pivot_Extend_Sort_Limit_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   city,country,2011__|__newCol,2012__|__newCol,newCol\n   LDN,UK,3000,null,LDN_0\n   NYC,USA,5000,15200,NYC_0\n   SAN,USA,2600,null,SAN_0\n#'\nactual:   '#TDS\n   year,city,country,treePlanted,newCol\n   2011,LDN,UK,3000,LDN_0\n   2011,NYC,USA,5000,NYC_0\n   2012,NYC,USA,15200,NYC_0\n#'\""),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_Pivot_GroupBy_Extend_Sort_Function_1__Boolean_1_", "NullPointer exception"),
            one("meta::pure::functions::relation::tests::composition::test_Pivot_Filter_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   city,country,2000__|__newCol,2011__|__newCol,2012__|__newCol\n   NYC,USA,15000,5000,15200\n#'"),

            //concatenate
            one("meta::pure::functions::relation::tests::concatenate::testSimpleConcatenate_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),


            //cumulativeDistribution
            one("meta::pure::functions::relation::tests::cumulativeDistribution::testOLAPWithPartitionAndOrderCummulativeDistribution_Function_1__Boolean_1_", "Feature 'CUME_DIST window function' is not supported by SingleStore."),

            //distinct
            one("meta::pure::functions::relation::tests::distinct::testDistinctAll_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::distinct::testDistinctMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::distinct::testDistinctSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),

            //drop
            one("meta::pure::functions::relation::tests::drop::testSimpleDrop_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),

            //extend
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_Function_1__Boolean_1_", "Feature 'GROUP_CONCAT in window function' is not supported by SingleStore."),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggIntegerWithPartitionAndOrderWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggNoWindowChainedWithSimple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggNoWindowMultipleColums_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggNoWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithOrderWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_Function_1__Boolean_1_", "Feature 'GROUP_CONCAT in window function' is not supported by SingleStore."),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionWindow_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithMultiplePartitionsAndOrderWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPWithPartitionAndOrderWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendFloat_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   val,doub,name\n   1,1.2,2.2\n   3,2.3,3.3\n   4,4.2,5.2\n   5,4.2,5.2\n   6,4.5,5.5\n#'"),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendFloat_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendInt_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::extend::testSimpleExtendStr_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::extend::testSimpleMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),

            //filter
            one("meta::pure::functions::relation::tests::filter::testSimpleFilter_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),

            //groupBy
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_MultipleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBy_SingleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),

            //join
            one("meta::pure::functions::relation::tests::join::testSimpleJoin_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),

            //limit
            one("meta::pure::functions::relation::tests::limit::testSimpleLimit_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),

            //pivot
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleMultiple_Function_1__Boolean_1_",  "\"\nexpected: '#TDS\n   year,UK__|__LDN__|__sum,UK__|__LDN__|__count,USA__|__NYC__|__sum,USA__|__NYC__|__count,USA__|__SAN__|__sum,USA__|__SAN__|__count\n   2000,null,null,15000,2,2000,1\n   2011,3000,1,5000,1,2600,2\n   2012,null,null,15200,2,null,null\n#'"),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleSingle_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   year,UK__|__LDN__|__sum,USA__|__NYC__|__sum,USA__|__SAN__|__sum\n   2000,null,15000,2000\n   2011,3000,5000,2600\n   2012,null,15200,null\n#'"),
            one("meta::pure::functions::relation::tests::pivot::testPivot_MultipleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleMultiple_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   city,country,2000__|__sum,2000__|__count,2011__|__sum,2011__|__count,2012__|__sum,2012__|__count\n   LDN,UK,null,null,3000,1,null,null\n   NYC,USA,15000,2,5000,1,15200,2\n   SAN,USA,2000,1,2600,2,null,null\n#'"),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleMultiple_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleSingle_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   city,country,2000__|__newCol,2011__|__newCol,2012__|__newCol\n   LDN,UK,null,3000,null\n   NYC,USA,15000,5000,15200\n   SAN,USA,2000,2600,null\n#'"),
            one("meta::pure::functions::relation::tests::pivot::testPivot_SingleSingle_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),

            //rename
            one("meta::pure::functions::relation::tests::rename::testSimpleRename_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),

            //select
            one("meta::pure::functions::relation::tests::select::testMultiColsSelect_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::select::testSelectAll_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::select::testSingleColSelect_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_Function_1__Boolean_1_", "Error while executing: Create Table"),
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_MultipleExpressions_Function_1__Boolean_1_", "Error while executing: Create Table"),

            //size
            one("meta::pure::functions::relation::tests::size::testComparisonOperationAfterSize_Function_1__Boolean_1_", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::relation::tests::size::testComparisonOperationAfterSize_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::size::testGroupBySize_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),
            one("meta::pure::functions::relation::tests::size::testSimpleSize_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),

            //Slice
            one("meta::pure::functions::relation::tests::slice::testSimpleSlice_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\""),

            //Sort
            one("meta::pure::functions::relation::tests::sort::testSimpleSort_MultipleExpressions_Function_1__Boolean_1_", "\"Common table expression not supported on DB MemSQL\"")

    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.MemSQL).getFirst())
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
