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

package org.finos.legend.engine.plan.execution.stores.relational.test.snowflake.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalSnowflakePCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.RelationCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Snowflake_RelationFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = RelationCodeRepositoryProvider.relationFunctions;
    private static final Adapter adapter = CoreRelationalSnowflakePCTCodeRepositoryProvider.snowflakeAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Listagg
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_Function_1__Boolean_1_", "Cumulative window frame unsupported for function LISTAGG", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_Function_1__Boolean_1_", "Cumulative window frame unsupported for function LISTAGG", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_MultipleExpressions_Function_1__Boolean_1_", "Cumulative window frame unsupported for function LISTAGG", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_MultipleExpressions_Function_1__Boolean_1_", "Cumulative window frame unsupported for function LISTAGG", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndUnboundedWindow_Function_1__Boolean_1_", "Window frame requires an ORDER BY clause.", AdapterQualifier.unsupportedFeature),

            // Snowflake doesn't seem to comply to the specification for first, last, nth (they should be bound to the frame)
            one("meta::pure::functions::relation::tests::last::testOLAPWithPartitionAndOrderLastWindow_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,grp,name,newCol\n   10,0,J,10\n   8,1,H,8\n   6,1,F,6\n   2,1,B,2\n   5,2,E,5\n   1,2,A,1\n   7,3,G,7\n   3,3,C,3\n   4,4,D,4\n   9,5,I,9\n#'\nactual:   '#TDS\n   id,grp,name,newCol\n   10,0,J,10\n   8,1,H,2\n   6,1,F,2\n   2,1,B,2\n   5,2,E,1\n   1,2,A,1\n   7,3,G,3\n   3,3,C,3\n   4,4,D,4\n   9,5,I,9\n#'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::nth::testOLAPWithPartitionAndOrderNthWindow2_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,grp,name,newCol\n   10,0,J,null\n   8,1,H,null\n   6,1,F,6\n   2,1,B,6\n   5,2,E,null\n   1,2,A,1\n   7,3,G,null\n   3,3,C,3\n   4,4,D,null\n   9,5,I,null\n#'\nactual:   '#TDS\n   id,grp,name,newCol\n   10,0,J,null\n   8,1,H,6\n   6,1,F,6\n   2,1,B,6\n   5,2,E,1\n   1,2,A,1\n   7,3,G,3\n   3,3,C,3\n   4,4,D,null\n   9,5,I,null\n#'\"", AdapterQualifier.needsInvestigation),

            // Composition
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_isEmpty_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,payload,empty\n   0,[],true\n   1,[1],false\n   2,[2,3,4],false\n   3,null,true\n#'\nactual:   '#TDS\n   id,payload,empty\n   0,\"[]\",false\n   1,\"[1]\",false\n   2,[2,3,4],false\n   3,null,true\n#'\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::relation::tests::composition::testVariantColumn_isNotEmpty_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,payload,notEmpty\n   0,[],false\n   1,[1],true\n   2,[2,3,4],true\n   3,null,false\n#'\nactual:   '#TDS\n   id,payload,notEmpty\n   0,\"[]\",true\n   1,\"[1]\",true\n   2,[2,3,4],true\n   3,null,false\n#'\"", AdapterQualifier.needsInvestigation),

            // Snowflake doesn't support window frame without ORDER BY
            one("meta::pure::functions::relation::tests::over::testRows_UnboundedPreceding_UnboundedFollowing_WithSinglePartition_WithoutOrderBy_Function_1__Boolean_1_", "Window frame requires an ORDER BY clause", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRows_UnboundedPreceding_UnboundedFollowing_WithMultiplePartitions_WithoutOrderBy_Function_1__Boolean_1_", "Window frame requires an ORDER BY clause", AdapterQualifier.unsupportedFeature),

            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_CurrentRow_WithSinglePartition_WithOrderByASC_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error:\nInvalid RANGE BETWEEN frame 'RANGE BETWEEN CURRENT ROW AND CURRENT ROW': timestamp ORDER BY clause only allows interval window frame boundaries.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::relation::tests::over::testRangeInterval_CurrentRow_CurrentRow_WithSinglePartition_WithOrderByDESC_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error:\nInvalid RANGE BETWEEN frame 'RANGE BETWEEN CURRENT ROW AND CURRENT ROW': timestamp ORDER BY clause only allows interval window frame boundaries.", AdapterQualifier.unsupportedFeature)
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Snowflake).getFirst())
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
