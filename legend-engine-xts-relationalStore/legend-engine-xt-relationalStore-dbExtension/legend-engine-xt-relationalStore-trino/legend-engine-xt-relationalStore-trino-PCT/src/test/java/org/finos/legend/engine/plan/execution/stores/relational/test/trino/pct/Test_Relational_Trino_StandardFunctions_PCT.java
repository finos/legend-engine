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
            // Boolean - Between
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_DateTime_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_NumberFloat_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_NumberInteger_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_NumberLong_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_StrictDate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_String_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Boolean - Xor
            one("meta::pure::functions::boolean::tests::operation::xor::testXor_BinaryExpressions_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::boolean::tests::operation::xor::testXor_BinaryTruthTable_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Collection - And
            one("meta::pure::functions::collection::tests::and::testAnd_Function_1__Boolean_1_", "Can't find the packageable element 'andtrue'"),

            // Collection - Greatest
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Boolean_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_DateTime_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Date_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Empty_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Float_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Integer_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Number_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Single_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_StrictDate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_String_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Collection - In
            one("meta::pure::functions::collection::tests::in::testInForDecimal_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::in::testInIsEmpty_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "\"Parameter to IN operation isn't a literal!\""),
            one("meta::pure::functions::collection::tests::in::testInPrimitive_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::in::testIn_relation_extend_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::in::testIn_relation_filter_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Collection - Least
            one("meta::pure::functions::collection::tests::least::testLeast_Boolean_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::least::testLeast_DateTime_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::least::testLeast_Date_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::least::testLeast_Empty_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::least::testLeast_Float_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::least::testLeast_Integer_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::least::testLeast_Number_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::least::testLeast_Single_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::least::testLeast_StrictDate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::collection::tests::least::testLeast_String_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Collection - Max
            one("meta::pure::functions::collection::tests::max::testMax_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),

            // Collection - Min
            one("meta::pure::functions::collection::tests::min::testMin_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),

            // Collection - Or
            one("meta::pure::functions::collection::tests::or::testOr_Function_1__Boolean_1_", "Can't find the packageable element 'ortrue'"),

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

            // Date - DayOfWeek
            one("meta::pure::functions::tests::date::testDayOfWeek_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::tests::date::testDayOfWeek_Relation_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // HashCode
            one("meta::pure::functions::hashCode::tests::testHashCode_Function_1__Boolean_1_", "\"[unsupported-api] The function 'hashCode' (state: [Select, false]) is not supported yet\""),

            // Math - Average
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::average::testAverage_Round_Integers_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

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
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_MoreThan62Bits_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitShiftLeft' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_UpTo62Bits_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitShiftLeft' (state: [Select, false]) is not supported yet\""),

            // Math - BitShiftRight
            one("meta::pure::functions::math::tests::bitShiftRight::testBitShiftRight_MoreThan62Bits_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitShiftRight' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitShiftRight::testBitShiftRight_UpTo62Bits_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitShiftRight' (state: [Select, false]) is not supported yet\""),

            // Math - BitXor
            one("meta::pure::functions::math::tests::bitXor::testBitXor_LargeNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitXor' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitXor::testBitXor_NegativeNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitXor' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitXor::testBitXor_SmallNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitXor' (state: [Select, false]) is not supported yet\""),

            // Math - Corr
            one("meta::pure::functions::math::tests::corr::testCorr_Function_1__Boolean_1_", "Unused format args. [4] arguments provided to expression \"CORR(%s, %s)\""),
            one("meta::pure::functions::math::tests::corr::testSimpleWindowCorr_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Math - CovarPopulation
            one("meta::pure::functions::math::tests::covarPopulation::testCovarPopulation_Function_1__Boolean_1_", "Unused format args. [4] arguments provided to expression \"COVAR_POP(%s, %s)\""),
            one("meta::pure::functions::math::tests::covarPopulation::testSimpleWindowCovarPopulation_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Math - CovarSample
            one("meta::pure::functions::math::tests::covarSample::testCovarSample_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::covarSample::testSimpleWindowCovarSample_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Math - HashCodeAggregate
            one("meta::pure::functions::math::hashCode::tests::testHashCodeAggregate_Function_1__Boolean_1_", "\"[unsupported-api] The function 'hashAgg' (state: [Select, false]) is not supported yet\""),

            // Math - Max
            one("meta::pure::functions::math::tests::max::testMax_Floats_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::max::testMax_Floats_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::max::testMax_Floats_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Math - MaxBy
            one("meta::pure::functions::math::tests::maxBy::testMaxBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'maxBy' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::maxBy::testSimpleGroupByMaxBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'maxBy' (state: [Select, false]) is not supported yet\""),

            // Math - Median
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Function_1__Boolean_1_", "Unused format args. [5] arguments provided to expression \"median(%s)\""),
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Function_1__Boolean_1_", "Unused format args. [5] arguments provided to expression \"median(%s)\""),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Function_1__Boolean_1_", "Unused format args. [5] arguments provided to expression \"median(%s)\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Math - Min
            one("meta::pure::functions::math::tests::min::testMin_Floats_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::min::testMin_Floats_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::min::testMin_Floats_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Math - MinBy
            one("meta::pure::functions::math::tests::minBy::testMinBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'minBy' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::minBy::testSimpleGroupByMinBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'minBy' (state: [Select, false]) is not supported yet\""),

            // Math - Mode
            one("meta::pure::functions::math::tests::mode::testMode_Float_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::mode::testMode_Floats_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::mode::testMode_Floats_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Math - Percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: Trino\""),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Math - Pi
            one("meta::pure::functions::math::tests::pi::testPi_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Math - StdDev
            one("meta::pure::functions::math::tests::stdDev::testFloatStdDev_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::stdDev::testIntStdDev_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::stdDev::testMixedStdDev_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::stdDev::testNegativeNumberStdDev_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::stdDev::testPopulationStandardDeviation_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::stdDev::testSimpleGroupByStandardDeviationPopulation_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::stdDev::testSimpleGroupByStandardDeviationSample_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::stdDev::testSimpleWindowStandardDeviationPopulation_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::stdDev::testSimpleWindowStandardDeviationSample_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Math - Sum
            one("meta::pure::functions::math::tests::sum::testSum_Floats_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::sum::testSum_Floats_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::sum::testSum_Floats_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::sum::testSum_Integers_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::sum::testSum_Integers_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::sum::testSum_Integers_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::sum::testSum_Numbers_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::sum::testSum_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::sum::testSum_Numbers_Relation_Window_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Math - ToDegrees
            one("meta::pure::functions::math::tests::toDegrees::testToDegrees_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Math - ToRadians
            one("meta::pure::functions::math::tests::toRadians::testToRadians_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Math - Trigonometry (Cosh)
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_EvalFuncSig_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_Eval_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_Floats_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_Identities_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_Integers_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Math - Trigonometry (Sinh)
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_EvalFuncSig_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_Eval_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_Floats_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_Identities_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_Integers_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Math - Trigonometry (Tanh)
            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_EvalFuncSig_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_Eval_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_Floats_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_Identities_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_Integers_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Math - Variance
            one("meta::pure::functions::math::tests::variance::testSimpleGroupByVariancePopulation_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::variance::testSimpleGroupByVarianceSample_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::variance::testSimpleWindowVariancePopulation_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::variance::testSimpleWindowVarianceSample_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::variance::testVariancePopulation_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::variance::testVarianceSample_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::variance::testVariance_Population_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::variance::testVariance_Sample_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

            // Math - Wavg
            one("meta::pure::functions::math::tests::wavg::testSimpleGroupByMultipleWavg_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),
            one("meta::pure::functions::math::tests::wavg::testSimpleGroupByWavg_Function_1__Boolean_1_", "Error while executing: Create Schema leSchema"),

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
