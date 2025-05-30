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

package org.finos.legend.engine.plan.execution.stores.relational.test.h2.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalH2PCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreStandardFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.*;

public class Test_Relational_H2_StandardFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
    private static final Adapter adapter = CoreRelationalH2PCTCodeRepositoryProvider.H2Adapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // StD Dev
            one("meta::pure::functions::math::tests::stdDev::testFloatStdDev_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLSyntaxErrorException: Syntax error in SQL statement \"select sqrt(var_samp([*][CAST(1.0 AS FLOAT),CAST(2.0 AS FLOAT),CAST(3.0 AS FLOAT)]))\"; expected \"DISTINCT, ALL, INTERSECTS (, NOT, EXISTS, UNIQUE, INTERSECTS\"; SQL statement:\nselect sqrt(var_samp([CAST(1.0 AS FLOAT),CAST(2.0 AS FLOAT),CAST(3.0 AS FLOAT)])) [42001-214]"),
            one("meta::pure::functions::math::tests::stdDev::testIntStdDev_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLSyntaxErrorException: Syntax error in SQL statement \"select sqrt(var_samp([*][1,2,3]))\"; expected \"DISTINCT, ALL, INTERSECTS (, NOT, EXISTS, UNIQUE, INTERSECTS\"; SQL statement:\nselect sqrt(var_samp([1,2,3])) [42001-214]"),
            one("meta::pure::functions::math::tests::stdDev::testMixedStdDev_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLSyntaxErrorException: Syntax error in SQL statement \"select sqrt(var_samp([*][CAST(1.0 AS FLOAT),2,CAST(3.0 AS FLOAT)]))\"; expected \"DISTINCT, ALL, INTERSECTS (, NOT, EXISTS, UNIQUE, INTERSECTS\"; SQL statement:\nselect sqrt(var_samp([CAST(1.0 AS FLOAT),2,CAST(3.0 AS FLOAT)])) [42001-214]"),
            one("meta::pure::functions::math::tests::stdDev::testNegativeNumberStdDev_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLSyntaxErrorException: Syntax error in SQL statement \"select sqrt(var_samp([*][-2,-4,-6]))\"; expected \"DISTINCT, ALL, INTERSECTS (, NOT, EXISTS, UNIQUE, INTERSECTS\"; SQL statement:\nselect sqrt(var_samp([-2,-4,-6])) [42001-214]"),
            one("meta::pure::functions::math::tests::stdDev::testPopulationStandardDeviation_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLSyntaxErrorException: Syntax error in SQL statement \"select sqrt(var_pop([*][1,2]))\"; expected \"DISTINCT, ALL, INTERSECTS (, NOT, EXISTS, UNIQUE, INTERSECTS\"; SQL statement:\nselect sqrt(var_pop([1,2])) [42001-214]"),

            // Variance
            one("meta::pure::functions::math::tests::variance::testVariancePopulation_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLSyntaxErrorException: Syntax error in SQL statement \"select var_pop([*][1,2])\"; expected \"DISTINCT, ALL, INTERSECTS (, NOT, EXISTS, UNIQUE, INTERSECTS\"; SQL statement:\nselect var_pop([1,2]) [42001-214]"),
            one("meta::pure::functions::math::tests::variance::testVarianceSample_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLSyntaxErrorException: Syntax error in SQL statement \"select var_samp([*][CAST(1.0 AS FLOAT),CAST(2.0 AS FLOAT),CAST(3.0 AS FLOAT)])\"; expected \"DISTINCT, ALL, INTERSECTS (, NOT, EXISTS, UNIQUE, INTERSECTS\"; SQL statement:\nselect var_samp([CAST(1.0 AS FLOAT),CAST(2.0 AS FLOAT),CAST(3.0 AS FLOAT)]) [42001-214]"),
            one("meta::pure::functions::math::tests::variance::testVariance_Population_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLSyntaxErrorException: Syntax error in SQL statement \"select var_pop([*][1,2])\"; expected \"DISTINCT, ALL, INTERSECTS (, NOT, EXISTS, UNIQUE, INTERSECTS\"; SQL statement:\nselect var_pop([1,2]) [42001-214]"),
            one("meta::pure::functions::math::tests::variance::testVariance_Sample_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLSyntaxErrorException: Syntax error in SQL statement \"select var_samp([*][CAST(1.0 AS FLOAT),CAST(2.0 AS FLOAT),CAST(3.0 AS FLOAT)])\"; expected \"DISTINCT, ALL, INTERSECTS (, NOT, EXISTS, UNIQUE, INTERSECTS\"; SQL statement:\nselect var_samp([CAST(1.0 AS FLOAT),CAST(2.0 AS FLOAT),CAST(3.0 AS FLOAT)]) [42001-214]"),

            // Numeric
            one("meta::pure::functions::math::tests::toDegrees::testToDegrees_Function_1__Boolean_1_", "\"\nexpected: 180.0\nactual:   180.00000000000003\""),

            // In
            one("meta::pure::functions::collection::tests::in::testInIsEmpty_Function_1__Boolean_1_", "NullPointer exception"),
            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::collection::in(Firm[*],Firm[*])'"),
            one("meta::pure::functions::collection::tests::in::testInPrimitive_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLSyntaxErrorException: Values of types \"INTEGER\" and \"DATE\" are not comparable; SQL statement:\nselect 1 in (1, 2, 5, 2, 'a', true, DATE'2014-02-01', 'c') [90110-214]"),

            // Max
            one("meta::pure::functions::math::tests::max::testMax_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"max(%s)\"\""),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"max(%s)\"\""),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1\""),

            // Min
            one("meta::pure::functions::math::tests::min::testMin_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"min(%s)\"\""),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"min(%s)\"\""),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.23D\nactual:   1.23\""),

            // Median
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),

            // Mode
            one("meta::pure::functions::math::tests::mode::testMode_Float_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"mode(%s)\"\""),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"mode(%s)\"\""),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"mode(%s)\"\""),

            // Date
            pack("meta::pure::functions::date::tests::timeBucket::dateTime", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketDays_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketMonths_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketWeeks_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketYears_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),

            // Average
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\""),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\""),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\""),
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   grp,newCol\n   0,101.0\n   1,41.0\n   2,16.0\n   3,81.0\n   4,31.0\n   5,51.0\n#'\nactual:   '#TDS\n   grp,newCol\n   0,101\n   1,41\n   2,16\n   3,81\n   4,31\n   5,51\n#'\""),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   grp,newCol\n   0,101.0\n   1,41.0\n   2,16.0\n   3,81.0\n   4,31.0\n   5,51.0\n#'\nactual:   '#TDS\n   grp,newCol\n   0,101\n   1,41\n   2,16\n   3,81\n   4,31\n   5,51\n#'\""),

            // Percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::collection::range(?)'"),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Window_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,val,newCol\n   1,1.0,2.4\n   1,2.0,2.4\n   1,3.0,2.4\n   2,1.5,2.9\n   2,2.5,2.9\n   2,3.5,2.9\n   3,1.0,1.7\n   3,1.5,1.7\n   3,2.0,1.7\n#'\nactual:   '#TDS\n   id,val,newCol\n   1,1.0,1.8\n   1,2.0,1.8\n   1,3.0,1.8\n   2,1.5,2.3\n   2,2.5,2.3\n   2,3.5,2.3\n   3,1.0,1.4\n   3,1.5,1.4\n   3,2.0,1.4\n#'\""),

            // CosH
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_EvalFuncSig_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"cosh(%s)\"\""),

            // SinH
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_EvalFuncSig_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"sinh(%s)\"\""),

            // TanH
            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_EvalFuncSig_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"tanh(%s)\"\""),

            // And
            one("meta::pure::functions::collection::tests::and::testAnd_Function_1__Boolean_1_", "Can't find the packageable element 'andtrue'"),

            // Or
            one("meta::pure::functions::collection::tests::or::testOr_Function_1__Boolean_1_", "Can't find the packageable element 'ortrue'")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.H2).getFirst())
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
