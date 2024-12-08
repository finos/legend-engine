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

package org.finos.legend.engine.plan.execution.stores.relational.test.oracle.pct;

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


public class Test_Relational_Oracle_RelationFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = RelationCodeRepositoryProvider.relationFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.oracleAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Pivot
            pack("meta::pure::functions::relation::tests::pivot", "\"pivot is not supported\""),
            one("meta::pure::functions::relation::tests::composition::test_Pivot_Filter_Function_1__Boolean_1_", "\"pivot is not supported\""),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_ComplexGroupBy_Pivot_Function_1__Boolean_1_", "\"pivot is not supported\""),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_Pivot_GroupBy_Extend_Sort_Function_1__Boolean_1_", "\"pivot is not supported\""),
            one("meta::pure::functions::relation::tests::composition::test_Extend_Filter_Select_GroupBy_Pivot_Extend_Sort_Limit_Function_1__Boolean_1_", "\"pivot is not supported\""),

            // BUG: unsupported compositions
            one("meta::pure::functions::relation::tests::composition::test_Distinct_GroupBy_Filter_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00904: \"tb_8886574_1733619047175_1\".\"str\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/"),
            one("meta::pure::functions::relation::tests::composition::test_GroupBy_Distinct_Filter_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00904: \"restrict__d#2\".\"newCol\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/"),
            one("meta::pure::functions::relation::tests::composition::test_GroupBy_Filter_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00904: \"tb_8994851_1733619055923_1\".\"STR\": invalid identifier\n\nhttps://docs.oracle.com/error-help/db/ora-00904/"),

            // BUG: Column name with special characters is not properly escaped
            one("meta::pure::functions::relation::tests::select::testSingleSelectWithQuotedColumn_Function_1__Boolean_1_", "Error while executing: BEGIN\\n    EXECUTE IMMEDIATE 'Create Table LESCHEMA.tb_1132184_1733612266195(val INT,str VARCHAR(200),other kind VARCHAR(200))';\\n    EXECUTE IMMEDIATE 'GRANT SELECT,INSERT,UPDATE,DELETE ON LESCHEMA.tb_1132184_1733612266195 TO PUBLIC';\\nEND;"),

            // Needs support for asOf Join
            one("meta::pure::functions::relation::tests::asOfJoin::testAsOfJoinWithKeyMatch_Function_1__Boolean_1_", "\"AsOfJoins are not supported in the generic generator!\""),
            one("meta::pure::functions::relation::tests::asOfJoin::testSimpleAsOfJoin_Function_1__Boolean_1_", "\"AsOfJoins are not supported in the generic generator!\""),

            one("meta::pure::functions::relation::tests::extend::testOLAPAggStringWithPartitionAndOrderWindow_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::extend::testOLAPAggWithPartitionAndOrderWindowMultipleColumns_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),

            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupByMultipleMultiple_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupByMultipleSingle_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBySingleMultiple_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::relation::tests::groupBy::testSimpleGroupBySingleSingle_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\""),

            one("meta::pure::functions::relation::tests::size::testGroupBySize_Function_1__Boolean_1_", "\"[unsupported-api] The function 'joinStrings' (state: [Select, false]) is not supported yet\"")
            );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Oracle).getFirst())
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
