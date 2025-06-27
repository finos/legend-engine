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

package org.finos.legend.engine.plan.execution.stores.relational.test.postgres.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalPostgresPCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreStandardFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Postgres_StandardFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
    private static final Adapter adapter = CoreRelationalPostgresPCTCodeRepositoryProvider.postgresAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // StD Dev
            one("meta::pure::functions::math::tests::stdDev::testFloatStdDev_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"[\"\n  Position: 21", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::stdDev::testIntStdDev_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"[\"\n  Position: 21", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::stdDev::testMixedStdDev_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"[\"\n  Position: 21", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::stdDev::testNegativeNumberStdDev_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"[\"\n  Position: 21", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::stdDev::testPopulationStandardDeviation_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"[\"\n  Position: 21", AdapterQualifier.needsInvestigation),

            // Variance
            one("meta::pure::functions::math::tests::variance::testVariancePopulation_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"[\"\n  Position: 16", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::variance::testVarianceSample_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"[\"\n  Position: 17", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::variance::testVariance_Population_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"[\"\n  Position: 16", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::variance::testVariance_Sample_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"[\"\n  Position: 16", AdapterQualifier.unsupportedFeature),

            // Covariance/Correlation
            one("meta::pure::functions::math::tests::corr::testCorr_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"CORR(%s, %s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::covarPopulation::testCovarPopulation_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"COVAR_POP(%s, %s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::covarSample::testCovarSample_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"COVAR_SAMP(%s, %s)\"\"", AdapterQualifier.unsupportedFeature),

            // In
            one("meta::pure::functions::collection::tests::in::testInIsEmpty_Function_1__Boolean_1_", "NullPointer exception", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "\"Parameter to IN operation isn't a literal!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::in::testInPrimitive_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: operator does not exist: integer = text\n  Hint: No operator matches the given name and argument type(s). You might need to add explicit type casts.\n  Position: 10", AdapterQualifier.needsInvestigation),

            // Max
            one("meta::pure::functions::math::tests::max::testMax_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"max(%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"max(%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::max::testMax_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'", AdapterQualifier.unsupportedFeature),

            // MaxBY
            one("meta::pure::functions::math::tests::maxBy::testMaxBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'maxBy' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::maxBy::testSimpleGroupByMaxBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'maxBy' (state: [Select, false]) is not supported yet\""),

            // Min
            one("meta::pure::functions::math::tests::min::testMin_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"min(%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"min(%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.23D\nactual:   1.23\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::min::testMin_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'", AdapterQualifier.unsupportedFeature),

            // MinBy
            one("meta::pure::functions::math::tests::minBy::testMinBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'minBy' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::minBy::testSimpleGroupByMinBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'minBy' (state: [Select, false]) is not supported yet\""),

            // Median
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Relation_Aggregate_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: function median(double precision) does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Relation_Window_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: function median(double precision) does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Relation_Aggregate_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: function median(integer) does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Relation_Window_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: function median(integer) does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: function median(double precision) does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Relation_Window_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: function median(double precision) does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts.", AdapterQualifier.needsInvestigation),

            // Mode
            one("meta::pure::functions::math::tests::mode::testMode_Float_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: WITHIN GROUP is required for ordered-set aggregate mode", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::mode::testMode_Floats_Relation_Aggregate_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: WITHIN GROUP is required for ordered-set aggregate mode", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::mode::testMode_Floats_Relation_Window_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: WITHIN GROUP is required for ordered-set aggregate mode", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: WITHIN GROUP is required for ordered-set aggregate mode", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Relation_Aggregate_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: WITHIN GROUP is required for ordered-set aggregate mode", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Relation_Window_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: WITHIN GROUP is required for ordered-set aggregate mode", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: WITHIN GROUP is required for ordered-set aggregate mode", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Relation_Aggregate_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: WITHIN GROUP is required for ordered-set aggregate mode", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Relation_Window_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: WITHIN GROUP is required for ordered-set aggregate mode", AdapterQualifier.needsInvestigation),

            // Date
            pack("meta::pure::functions::date::tests::timeBucket::dateTime", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketDays_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketMonths_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketWeeks_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketYears_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            // Average
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\"", AdapterQualifier.unsupportedFeature),

            // Percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Window_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: OVER is not supported for ordered-set aggregate percentile_cont", AdapterQualifier.unsupportedFeature),

            // CosH
            pack("meta::pure::functions::math::tests::trigonometry::cosh", "does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts.\n  Position: 8", AdapterQualifier.needsInvestigation),

            // SinH
            pack("meta::pure::functions::math::tests::trigonometry::sinh", "does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts.\n  Position: 8", AdapterQualifier.needsInvestigation),

            // TanH
            pack("meta::pure::functions::math::tests::trigonometry::tanh", "does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts.\n  Position: 8", AdapterQualifier.needsInvestigation),

            // Bitwise
            pack("meta::pure::functions::math::tests::bitAnd", "\"[unsupported-api] The function 'bitAnd' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::math::tests::bitNot", "\"[unsupported-api] The function 'bitNot' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::math::tests::bitOr", "\"[unsupported-api] The function 'bitOr' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::math::tests::bitXor", "\"[unsupported-api] The function 'bitXor' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            pack("meta::pure::functions::math::tests::bitShiftLeft", "\"Execution error message mismatch.\nThe actual message was \"[unsupported-api] The function 'bitShiftLeft' (state: [Select, false]) is not supported yet\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\"", AdapterQualifier.assertErrorMismatch),
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_UpTo62Bits_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitShiftLeft' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::bitShiftRight::testBitShiftRight_UpTo62Bits_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitShiftRight' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"[unsupported-api] The function 'bitShiftLeft' (state: [Select, false]) is not supported yet\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\"", AdapterQualifier.assertErrorMismatch),
            one("meta::pure::functions::math::tests::bitShiftRight::testBitShiftRight_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"[unsupported-api] The function 'bitShiftRight' (state: [Select, false]) is not supported yet\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\"", AdapterQualifier.assertErrorMismatch),

            // And
            one("meta::pure::functions::collection::tests::and::testAnd_Function_1__Boolean_1_", "Can't find the packageable element 'andtrue'", AdapterQualifier.unsupportedFeature),

            // Or
            one("meta::pure::functions::collection::tests::or::testOr_Function_1__Boolean_1_", "Can't find the packageable element 'ortrue'", AdapterQualifier.unsupportedFeature),

            // Inequalities
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_String_Function_1__Boolean_1_", "Assert failed", AdapterQualifier.needsInvestigation),

            // Greatest
            one("meta::pure::functions::collection::tests::greatest::testGreatest_DateTime_Function_1__Boolean_1_", "\"\nexpected: %2025-02-10T20:10:20+0000\nactual:   %2025-02-10T20:10:20.000000000+0000\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Number_Function_1__Boolean_1_", "\"\nexpected: 7.345D\nactual:   7.345\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Single_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1D\"", AdapterQualifier.needsInvestigation),

            // Least
            one("meta::pure::functions::collection::tests::least::testLeast_DateTime_Function_1__Boolean_1_", "\"\nexpected: %2025-01-10T15:25:30+0000\nactual:   %2025-01-10T15:25:30.000000000+0000\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::least::testLeast_Number_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::least::testLeast_Single_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1D\"", AdapterQualifier.needsInvestigation)
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Postgres).getFirst())
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
