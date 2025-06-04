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
            one("meta::pure::functions::math::tests::stdDev::testFloatStdDev_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"[\"\n  Position: 21"),
            one("meta::pure::functions::math::tests::stdDev::testIntStdDev_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"[\"\n  Position: 21"),
            one("meta::pure::functions::math::tests::stdDev::testMixedStdDev_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"[\"\n  Position: 21"),
            one("meta::pure::functions::math::tests::stdDev::testNegativeNumberStdDev_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"[\"\n  Position: 21"),
            one("meta::pure::functions::math::tests::stdDev::testPopulationStandardDeviation_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"[\"\n  Position: 21"),

            // Variance
            one("meta::pure::functions::math::tests::variance::testVariancePopulation_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"[\"\n  Position: 16"),
            one("meta::pure::functions::math::tests::variance::testVarianceSample_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"[\"\n  Position: 17"),
            one("meta::pure::functions::math::tests::variance::testVariance_Population_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"[\"\n  Position: 16"),
            one("meta::pure::functions::math::tests::variance::testVariance_Sample_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: syntax error at or near \"[\"\n  Position: 16"),

            // In
            one("meta::pure::functions::collection::tests::in::testInIsEmpty_Function_1__Boolean_1_", "NullPointer exception"),
            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "\"Parameter to IN operation isn't a literal!\""),
            one("meta::pure::functions::collection::tests::in::testInPrimitive_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: operator does not exist: integer = text\n  Hint: No operator matches the given name and argument type(s). You might need to add explicit type casts.\n  Position: 10"),

            // Max
            one("meta::pure::functions::math::tests::max::testMax_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"max(%s)\"\""),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"max(%s)\"\""),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1\""),
            one("meta::pure::functions::collection::tests::max::testMax_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::collection::max(?)'"),

            // Min
            one("meta::pure::functions::math::tests::min::testMin_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"min(%s)\"\""),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"min(%s)\"\""),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.23D\nactual:   1.23\""),
            one("meta::pure::functions::collection::tests::min::testMin_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::collection::min(?)'"),

            // Median
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Relation_Aggregate_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: function median(double precision) does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts."),
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Relation_Window_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: function median(double precision) does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts."),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Relation_Aggregate_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: function median(integer) does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts."),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Relation_Window_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: function median(integer) does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts."),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: function median(double precision) does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts."),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Relation_Window_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: function median(double precision) does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts."),

            // Mode
            one("meta::pure::functions::math::tests::mode::testMode_Float_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: WITHIN GROUP is required for ordered-set aggregate mode"),
            one("meta::pure::functions::math::tests::mode::testMode_Floats_Relation_Aggregate_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: WITHIN GROUP is required for ordered-set aggregate mode"),
            one("meta::pure::functions::math::tests::mode::testMode_Floats_Relation_Window_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: WITHIN GROUP is required for ordered-set aggregate mode"),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: WITHIN GROUP is required for ordered-set aggregate mode"),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Relation_Aggregate_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: WITHIN GROUP is required for ordered-set aggregate mode"),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Relation_Window_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: WITHIN GROUP is required for ordered-set aggregate mode"),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: WITHIN GROUP is required for ordered-set aggregate mode"),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Relation_Aggregate_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: WITHIN GROUP is required for ordered-set aggregate mode"),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Relation_Window_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: WITHIN GROUP is required for ordered-set aggregate mode"),

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

            // Percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::collection::range(?)'"),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Window_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: OVER is not supported for ordered-set aggregate percentile_cont"),

            // CosH
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_Eval_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: function cosh(numeric) does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts.\n  Position: 8"),
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_EvalFuncSig_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: function cosh(integer) does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts.\n  Position: 8"),

            // SinH
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_EvalFuncSig_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: function sinh(integer) does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts.\n  Position: 8"),
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_Eval_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: function sinh(numeric) does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts.\n  Position: 8"),

            // TanH
            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_EvalFuncSig_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: function tanh(integer) does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts.\n  Position: 8"),
            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_Eval_Function_1__Boolean_1_", "org.postgresql.util.PSQLException: ERROR: function tanh(numeric) does not exist\n  Hint: No function matches the given name and argument types. You might need to add explicit type casts.\n  Position: 8"),

            // Bitwise
            pack("meta::pure::functions::math::tests::tbd::bitAnd", "\"[unsupported-api] The function 'bitAnd' (state: [Select, false]) is not supported yet\""),
            pack("meta::pure::functions::math::tests::tbd::bitNot", "\"[unsupported-api] The function 'bitNot' (state: [Select, false]) is not supported yet\""),
            pack("meta::pure::functions::math::tests::tbd::bitOr", "\"[unsupported-api] The function 'bitOr' (state: [Select, false]) is not supported yet\""),
            pack("meta::pure::functions::math::tests::tbd::bitXor", "\"[unsupported-api] The function 'bitXor' (state: [Select, false]) is not supported yet\""),
            pack("meta::pure::functions::math::tests::tbd::bitShiftLeft", "\"Execution error message mismatch.\nThe actual message was \"[unsupported-api] The function 'bitShiftLeft' (state: [Select, false]) is not supported yet\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\""),
            one("meta::pure::functions::math::tests::tbd::bitShiftLeft::testBitShiftLeft_UpTo62Bits_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitShiftLeft' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::tbd::bitShiftRight::testBitShiftRight_UpTo62Bits_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitShiftRight' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::tbd::bitShiftLeft::testBitShiftLeft_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"[unsupported-api] The function 'bitShiftLeft' (state: [Select, false]) is not supported yet\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\""),
            one("meta::pure::functions::math::tests::tbd::bitShiftRight::testBitShiftRight_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"[unsupported-api] The function 'bitShiftRight' (state: [Select, false]) is not supported yet\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\""),

            // And
            one("meta::pure::functions::collection::tests::and::testAnd_Function_1__Boolean_1_", "Can't find the packageable element 'andtrue'"),

            // Or
            one("meta::pure::functions::collection::tests::or::testOr_Function_1__Boolean_1_", "Can't find the packageable element 'ortrue'"),

            // Inequalities
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_String_Function_1__Boolean_1_", "Assert failed"),

            // Greatest
            one("meta::pure::functions::collection::tests::greatest::testGreatest_DateTime_Function_1__Boolean_1_", "\"\nexpected: %2025-02-10T20:10:20+0000\nactual:   %2025-02-10T20:10:20.000000000+0000\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Number_Function_1__Boolean_1_", "\"\nexpected: 7.345D\nactual:   7.345\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Single_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1D\""),

            // Least
            one("meta::pure::functions::collection::tests::least::testLeast_DateTime_Function_1__Boolean_1_", "\"\nexpected: %2025-01-10T15:25:30+0000\nactual:   %2025-01-10T15:25:30.000000000+0000\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Number_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Single_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1D\"")
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
