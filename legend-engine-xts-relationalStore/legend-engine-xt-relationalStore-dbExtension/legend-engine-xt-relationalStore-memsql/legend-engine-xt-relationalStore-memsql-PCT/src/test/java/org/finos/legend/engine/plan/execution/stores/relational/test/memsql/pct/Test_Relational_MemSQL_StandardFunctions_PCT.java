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
import org.finos.legend.pure.code.core.CoreStandardFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_MemSQL_StandardFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.memsqlAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // And
            one("meta::pure::functions::collection::tests::and::testAnd_Function_1__Boolean_1_", "Can't find the packageable element 'andtrue'"),

            // StD Dev
            one("meta::pure::functions::math::tests::stdDev::testFloatStdDev_Function_1__Boolean_1_", "A non-scalar value ' [1.0,2.0,3.0]' is used as a scalar"),
            one("meta::pure::functions::math::tests::stdDev::testIntStdDev_Function_1__Boolean_1_", "A non-scalar value ' [1,2,3]' is used as a scalar"),
            one("meta::pure::functions::math::tests::stdDev::testMixedStdDev_Function_1__Boolean_1_", "A non-scalar value ' [1.0,2,3.0]' is used as a scalar"),
            one("meta::pure::functions::math::tests::stdDev::testNegativeNumberStdDev_Function_1__Boolean_1_", "A non-scalar value ' [-2,-4,-6]' is used as a scalar"),
            one("meta::pure::functions::math::tests::stdDev::testPopulationStandardDeviation_Function_1__Boolean_1_", "A non-scalar value ' [1,2]' is used as a scalar"),

            // Variance
            one("meta::pure::functions::math::tests::variance::testVariancePopulation_Function_1__Boolean_1_", "A non-scalar value ' [1,2]' is used as a scalar"),
            one("meta::pure::functions::math::tests::variance::testVarianceSample_Function_1__Boolean_1_", "A non-scalar value ' [1.0,2.0,3.0]' is used as a scalar"),
            one("meta::pure::functions::math::tests::variance::testVariance_Population_Function_1__Boolean_1_", "A non-scalar value ' [1,2]' is used as a scalar"),
            one("meta::pure::functions::math::tests::variance::testVariance_Sample_Function_1__Boolean_1_", "A non-scalar value ' [1.0,2.0,3.0]' is used as a scalar"),

            //wavg
            one("meta::pure::functions::math::tests::wavg::testSimpleGroupByMultipleWavg_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   grp,wavgCol1,wavgCol2\n   1,180.0,220.0\n   2,150.0,175.0\n   3,362.5,325.0\n   4,700.0,700.0\n   5,350.0,350.0\n#'\nactual:   '#TDS\n   grp,wavgCol1,wavgCol2\n   1,180.00000268220901,220.00000029802322\n   2,150.0,175.0\n   3,362.5,325.0\n   4,700.0,700.0\n   5,350.0,350.0\n#'\""),
            one("meta::pure::functions::math::tests::wavg::testSimpleGroupByWavg_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   grp,wavgCol\n   1,180.0\n   2,150.0\n   3,362.5\n   4,700.0\n   5,350.0\n#'\nactual:   '#TDS\n   grp,wavgCol\n   1,180.00000268220901\n   2,150.0\n   3,362.5\n   4,700.0\n   5,350.0\n#'\""),

            // In
            one("meta::pure::functions::collection::tests::in::testInIsEmpty_Function_1__Boolean_1_", "NullPointer exception"),
            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "Error while executing: insert into leSchema.Firm (_pureId,legalName) values (10,'f1');"),
            one("meta::pure::functions::collection::tests::in::testInPrimitive_Function_1__Boolean_1_", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')"),

            // Or
            one("meta::pure::functions::collection::tests::or::testOr_Function_1__Boolean_1_", "Can't find the packageable element 'ortrue'"),

            // Max
            one("meta::pure::functions::math::tests::max::testMax_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"max(%s)\"\""),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"max(%s)\"\""),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "\"\nexpected: 2\nactual:   2.0\""),

            // Min
            one("meta::pure::functions::math::tests::min::testMin_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"min(%s)\"\""),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"min(%s)\"\""),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.23D\nactual:   1.23\""),

            // Average
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\""),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\""),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\""),

            // Percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::collection::range(?)'"),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Window_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,val,newCol\n   1,1.0,2.4\n   1,2.0,2.4\n   1,3.0,2.4\n   2,1.5,2.9\n   2,2.5,2.9\n   2,3.5,2.9\n   3,1.0,1.7\n   3,1.5,1.7\n   3,2.0,1.7\n#'"),

            // TimeBucket
            one("meta::pure::functions::date::tests::timeBucket::testTimeBucketDays_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::testTimeBucketHours_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::testTimeBucketMinutes_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::testTimeBucketMonths_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::testTimeBucketSeconds_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::testTimeBucketWeeks_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::testTimeBucketYears_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\"")
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
