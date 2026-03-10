// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.trino.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalTrinoPCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreStandardFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Trino_StandardFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
    private static final Adapter adapter = CoreRelationalTrinoPCTCodeRepositoryProvider.trinoAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Collection - Greatest
            one("meta::pure::functions::collection::tests::greatest::testGreatest_DateTime_Function_1__Boolean_1_", "2025-02-10T20:10:20.000000000+0000"),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Number_Function_1__Boolean_1_", "expected: 2\nactual:   2.0"),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Single_Function_1__Boolean_1_", "2025-02-10T20:10:20.000000000+0000"),

            // Collection - In
            one("meta::pure::functions::collection::tests::in::testInIsEmpty_Function_1__Boolean_1_", "NullPointer exception"),
            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "\"Parameter to IN operation isn't a literal!\""),
            one("meta::pure::functions::collection::tests::in::testInPrimitive_Function_1__Boolean_1_", "Cannot find common type between integer and varchar(1)"),

            // Collection - Least
            one("meta::pure::functions::collection::tests::least::testLeast_DateTime_Function_1__Boolean_1_", "2025-01-10T15:25:30.000000000+0000"),
            one("meta::pure::functions::collection::tests::least::testLeast_Number_Function_1__Boolean_1_", "expected: 1.0D\nactual:   1.0"),
            one("meta::pure::functions::collection::tests::least::testLeast_Single_Function_1__Boolean_1_", "2025-02-10T20:10:20.000000000+0000"),

            // Collection - Max
            one("meta::pure::functions::collection::tests::max::testMax_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),

            // Collection - Min
            one("meta::pure::functions::collection::tests::min::testMin_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),

            // Date - TimeBucket (DateTime)
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketDays_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketHours_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketMinutes_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketMonths_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketSeconds_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketWeeks_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketYears_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),

            // Date - TimeBucket (StrictDate)
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketDays_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketMonths_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketWeeks_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketYears_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),

            // HashCode
            one("meta::pure::functions::hashCode::tests::testHashCode_Function_1__Boolean_1_", "\"[unsupported-api] The function 'hashCode' (state: [Select, false]) is not supported yet\""),

            // Math - BitAnd
            one("meta::pure::functions::math::tests::bitAnd::testBitAnd_LargeNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitAnd' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitAnd::testBitAnd_NegativeNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitAnd' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitAnd::testBitAnd_SmallNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitAnd' (state: [Select, false]) is not supported yet\""),

            // Math - BitNot
            one("meta::pure::functions::math::tests::bitNot::testBitNot_LargeNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitNot' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitNot::testBitNot_NegativeNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitNot' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitNot::testBitNot_SmallNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitNot' (state: [Select, false]) is not supported yet\""),

            // Math - BitOr
            one("meta::pure::functions::math::tests::bitOr::testBitOr_LargeNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitOr' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitOr::testBitOr_NegativeNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitOr' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitOr::testBitOr_SmallNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitOr' (state: [Select, false]) is not supported yet\""),

            // Math - BitShiftLeft
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"[unsupported-api] The function 'bitShiftLeft' (state: [Select, false]) is not supported yet\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\""),
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_UpTo62Bits_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitShiftLeft' (state: [Select, false]) is not supported yet\""),

            // Math - BitShiftRight
            one("meta::pure::functions::math::tests::bitShiftRight::testBitShiftRight_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"[unsupported-api] The function 'bitShiftRight' (state: [Select, false]) is not supported yet\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\""),
            one("meta::pure::functions::math::tests::bitShiftRight::testBitShiftRight_UpTo62Bits_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitShiftRight' (state: [Select, false]) is not supported yet\""),

            // Math - BitXor
            one("meta::pure::functions::math::tests::bitXor::testBitXor_LargeNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitXor' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitXor::testBitXor_NegativeNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitXor' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitXor::testBitXor_SmallNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitXor' (state: [Select, false]) is not supported yet\""),

            // Math - Corr
            one("meta::pure::functions::math::tests::corr::testCorr_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"CORR(%s, %s)\"\""),

            // Math - CovarPopulation
            one("meta::pure::functions::math::tests::covarPopulation::testCovarPopulation_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"COVAR_POP(%s, %s)\"\""),

            // Math - CovarSample
            one("meta::pure::functions::math::tests::covarSample::testCovarSample_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"COVAR_SAMP(%s, %s)\"\""),

            // Math - Average
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\""),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\""),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\""),
            one("meta::pure::functions::math::tests::average::testAverage_Round_Integers_Relation_Window_Function_1__Boolean_1_", "Not supported data type :'DOUBLE' for Pure type: 'Integer'"),

            // Math - HashCodeAggregate
            one("meta::pure::functions::math::hashCode::tests::testHashCodeAggregate_Function_1__Boolean_1_", "\"[unsupported-api] The function 'hashAgg' (state: [Select, false]) is not supported yet\""),

            // Math - Max (array)
            one("meta::pure::functions::math::tests::max::testMax_Floats_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_max' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_max' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "\"\nexpected: 2\nactual:   2.0\""),

            // Math - MaxBy
            one("meta::pure::functions::math::tests::maxBy::testMaxBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'maxBy' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::maxBy::testSimpleGroupByMaxBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'maxBy' (state: [Select, false]) is not supported yet\""),

            // Math - Median
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Relation_Aggregate_Function_1__Boolean_1_", "Function 'median' not registered"),
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Relation_Window_Function_1__Boolean_1_", "Function 'median' not registered"),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Relation_Aggregate_Function_1__Boolean_1_", "Function 'median' not registered"),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Relation_Window_Function_1__Boolean_1_", "Function 'median' not registered"),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "Function 'median' not registered"),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Relation_Window_Function_1__Boolean_1_", "Function 'median' not registered"),

            // Math - Min (array)
            one("meta::pure::functions::math::tests::min::testMin_Floats_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_min' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_min' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.23D\nactual:   1.23\""),

            // Math - MinBy
            one("meta::pure::functions::math::tests::minBy::testMinBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'minBy' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::minBy::testSimpleGroupByMinBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'minBy' (state: [Select, false]) is not supported yet\""),

            // Math - Mode
            one("meta::pure::functions::math::tests::mode::testMode_Float_Function_1__Boolean_1_", "Function 'mode' not registered"),
            one("meta::pure::functions::math::tests::mode::testMode_Floats_Relation_Aggregate_Function_1__Boolean_1_", "Function 'mode' not registered"),
            one("meta::pure::functions::math::tests::mode::testMode_Floats_Relation_Window_Function_1__Boolean_1_", "Function 'mode' not registered"),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Function_1__Boolean_1_", "Function 'mode' not registered"),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Relation_Aggregate_Function_1__Boolean_1_", "Function 'mode' not registered"),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Relation_Window_Function_1__Boolean_1_", "Function 'mode' not registered"),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Function_1__Boolean_1_", "Function 'mode' not registered"),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Relation_Aggregate_Function_1__Boolean_1_", "Function 'mode' not registered"),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Relation_Window_Function_1__Boolean_1_", "Function 'mode' not registered"),

            // Math - Percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Aggregate_Function_1__Boolean_1_", "mismatched input '('. Expecting: 'BY'"),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Window_Function_1__Boolean_1_", "mismatched input '('. Expecting: 'BY'"),

            // Math - StdDev
            one("meta::pure::functions::math::tests::stdDev::testFloatStdDev_Function_1__Boolean_1_", "mismatched input '['. Expecting: ')', '*', 'ALL', 'DISTINCT', 'ORDER', <expression>"),
            one("meta::pure::functions::math::tests::stdDev::testIntStdDev_Function_1__Boolean_1_", "mismatched input '['. Expecting: ')', '*', 'ALL', 'DISTINCT', 'ORDER', <expression>"),
            one("meta::pure::functions::math::tests::stdDev::testMixedStdDev_Function_1__Boolean_1_", "mismatched input '['. Expecting: ')', '*', 'ALL', 'DISTINCT', 'ORDER', <expression>"),
            one("meta::pure::functions::math::tests::stdDev::testNegativeNumberStdDev_Function_1__Boolean_1_", "mismatched input '['. Expecting: ')', '*', 'ALL', 'DISTINCT', 'ORDER', <expression>"),
            one("meta::pure::functions::math::tests::stdDev::testPopulationStandardDeviation_Function_1__Boolean_1_", "mismatched input '['. Expecting: ')', '*', 'ALL', 'DISTINCT', 'ORDER', <expression>"),

            // Math - Variance
            one("meta::pure::functions::math::tests::variance::testVariancePopulation_Function_1__Boolean_1_", "mismatched input '['. Expecting: ')', '*', 'ALL', 'DISTINCT', 'ORDER', <expression>"),
            one("meta::pure::functions::math::tests::variance::testVarianceSample_Function_1__Boolean_1_", "mismatched input '['. Expecting: ')', '*', 'ALL', 'DISTINCT', 'ORDER', <expression>"),
            one("meta::pure::functions::math::tests::variance::testVariance_Population_Function_1__Boolean_1_", "mismatched input '['. Expecting: ')', '*', 'ALL', 'DISTINCT', 'ORDER', <expression>"),
            one("meta::pure::functions::math::tests::variance::testVariance_Sample_Function_1__Boolean_1_", "mismatched input '['. Expecting: ')', '*', 'ALL', 'DISTINCT', 'ORDER', <expression>"),

            // Date
            one("meta::pure::functions::tests::date::testDayOfWeek_Function_1__Boolean_1_", "Invalid format string: %A"),
            one("meta::pure::functions::tests::date::testDayOfWeek_Relation_Function_1__Boolean_1_", "Error while executing: insert into leSchema"),

            // String - GenerateGuid
            one("meta::pure::functions::string::generation::tests::generateGuid::testGenerateGuidWithRelation_Function_1__Boolean_1_", "\"[unsupported-api] The function 'generateGuid' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::string::generation::tests::generateGuid::testGenerateGuid_Function_1__Boolean_1_", "\"[unsupported-api] The function 'generateGuid' (state: [Select, false]) is not supported yet\"")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Trino).getFirst())
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
