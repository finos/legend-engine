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

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;
import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.pure.runtime.testConnection.CoreExternalTestConnectionCodeRepositoryProvider;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreStandardFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_Relational_Databricks_StandardFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.databricksAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(

            //and
            one("meta::pure::functions::collection::tests::and::testAnd_Function_1__Boolean_1_", "Can't find the packageable element 'andtrue'"),

            //or
            one("meta::pure::functions::collection::tests::or::testOr_Function_1__Boolean_1_", "Can't find the packageable element 'ortrue'"),

            // StD Dev
            one("meta::pure::functions::math::tests::stdDev::testFloatStdDev_Function_1__Boolean_1_", "[PARSE_SYNTAX_ERROR] Syntax error at or near '('."),
            one("meta::pure::functions::math::tests::stdDev::testIntStdDev_Function_1__Boolean_1_", "[PARSE_SYNTAX_ERROR] Syntax error at or near '('."),
            one("meta::pure::functions::math::tests::stdDev::testMixedStdDev_Function_1__Boolean_1_", "[PARSE_SYNTAX_ERROR] Syntax error at or near '('."),
            one("meta::pure::functions::math::tests::stdDev::testNegativeNumberStdDev_Function_1__Boolean_1_", "[PARSE_SYNTAX_ERROR] Syntax error at or near '('."),
            one("meta::pure::functions::math::tests::stdDev::testPopulationStandardDeviation_Function_1__Boolean_1_", "[PARSE_SYNTAX_ERROR] Syntax error at or near '('."),

            // Variance
            one("meta::pure::functions::math::tests::variance::testVariancePopulation_Function_1__Boolean_1_", "[PARSE_SYNTAX_ERROR] Syntax error at or near '['."),
            one("meta::pure::functions::math::tests::variance::testVarianceSample_Function_1__Boolean_1_", "[PARSE_SYNTAX_ERROR] Syntax error at or near '['."),
            one("meta::pure::functions::math::tests::variance::testVariance_Population_Function_1__Boolean_1_", "[PARSE_SYNTAX_ERROR] Syntax error at or near '['."),
            one("meta::pure::functions::math::tests::variance::testVariance_Sample_Function_1__Boolean_1_", "[PARSE_SYNTAX_ERROR] Syntax error at or near '['."),

            // In
            one("meta::pure::functions::collection::tests::in::testInIsEmpty_Function_1__Boolean_1_", "NullPointer exception"),
            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::collection::in(Firm[*],Firm[*])'"),

            // Date
            pack("meta::pure::functions::date::tests::timeBucket", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),

            //avg
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\""),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\""),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\""),

            //max
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "\"\nexpected: 2\nactual:   2.0\""),
            one("meta::pure::functions::math::tests::max::testMax_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"max(%s)\"\""),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"max(%s)\"\""),

            //min
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.23D\nactual:   1.23\""),
            one("meta::pure::functions::math::tests::min::testMin_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"min(%s)\"\""),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"min(%s)\"\""),

            //percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::collection::range(?)'"),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Window_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,val,newCol\n   1,1.0,2.4\n   1,2.0,2.4\n   1,3.0,2.4\n   2,1.5,2.9\n   2,2.5,2.9\n   2,3.5,2.9\n   3,1.0,1.7\n   3,1.5,1.7\n   3,2.0,1.7\n#'\nactual:   '#TDS\n   id,val,newCol\n   1,1.0,1.8\n   1,2.0,1.8\n   1,3.0,1.8\n   2,1.5,2.3\n   2,2.5,2.3\n   2,3.5,2.3\n   3,1.0,1.4000000000000001\n   3,1.5,1.4000000000000001\n   3,2.0,1.4000000000000001\n#'\""),

            //median
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Relation_Window_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,grp,name,newCol\n   10.1,0,J,10.1\n   2.2,1,B,6.6\n   6.6,1,F,6.6\n   8.8,1,H,6.6\n   1.1,2,A,3.3\n   5.5,2,E,3.3\n   3.3,3,C,5.5\n   7.7,3,G,5.5\n   4.4,4,D,4.4\n   9.9,5,I,9.9\n#'\nactual:   '#TDS\n   id,grp,name,newCol\n   10.1,0,J,10.100000381469727\n   2.2,1,B,6.599999904632568\n   6.6,1,F,6.599999904632568\n   8.8,1,H,6.599999904632568\n   1.1,2,A,3.300000011920929\n   5.5,2,E,3.300000011920929\n   3.3,3,C,5.4999998807907104\n   7.7,3,G,5.4999998807907104\n   4.4,4,D,4.400000095367432\n   9.9,5,I,9.899999618530273\n#'\""),

            //mode
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"mode(%s)\"\""),
            one("meta::pure::functions::math::tests::mode::testMode_Float_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"mode(%s)\"\""),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"mode(%s)\"\""),

            // CosH
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_EvalFuncSig_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"cosh(%s)\"\""),

            // SinH
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_EvalFuncSig_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"sinh(%s)\"\""),

            // TanH
            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_EvalFuncSig_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"tanh(%s)\"\""),

            // Greatest
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Boolean_Function_1__Boolean_1_", "\"Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_DateTime_Function_1__Boolean_1_", "\"Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Date_Function_1__Boolean_1_", "\"Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Float_Function_1__Boolean_1_", "\"Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Integer_Function_1__Boolean_1_", "\"Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Number_Function_1__Boolean_1_", "\"Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_StrictDate_Function_1__Boolean_1_", "\"Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_String_Function_1__Boolean_1_", "\"Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery\""),

            // Least
            one("meta::pure::functions::collection::tests::least::testLeast_Boolean_Function_1__Boolean_1_", "\"Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery\""),
            one("meta::pure::functions::collection::tests::least::testLeast_DateTime_Function_1__Boolean_1_", "\"Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Date_Function_1__Boolean_1_", "\"Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Float_Function_1__Boolean_1_", "\"Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Integer_Function_1__Boolean_1_", "\"Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Number_Function_1__Boolean_1_", "\"Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery\""),
            one("meta::pure::functions::collection::tests::least::testLeast_StrictDate_Function_1__Boolean_1_", "\"Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery\""),
            one("meta::pure::functions::collection::tests::least::testLeast_String_Function_1__Boolean_1_", "\"Cast exception: SelectSQLQuery cannot be cast to TdsSelectSqlQuery\"")

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
