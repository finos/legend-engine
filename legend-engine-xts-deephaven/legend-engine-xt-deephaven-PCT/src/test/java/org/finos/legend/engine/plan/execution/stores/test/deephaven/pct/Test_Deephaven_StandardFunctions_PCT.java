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

package org.finos.legend.engine.plan.execution.stores.test.deephaven.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.deephaven.test.TestDeephavenConnectionIntegrationLoader;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreDeephavenPCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreStandardFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Deephaven_StandardFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
    private static final Adapter adapter = CoreDeephavenPCTCodeRepositoryProvider.deephavenAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_DateTime_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_NumberFloat_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_NumberInteger_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_NumberLong_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_StrictDate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_String_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_\""),
            
            one("meta::pure::functions::boolean::tests::operation::xor::testXor_BinaryExpressions_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_\""),
            one("meta::pure::functions::boolean::tests::operation::xor::testXor_BinaryTruthTable_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::boolean::and_Boolean_1__Boolean_1__Boolean_1_\""),

            one("meta::pure::functions::collection::tests::and::testAnd_Function_1__Boolean_1_", "Can't find the packageable element 'andtrue'"),
            
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Boolean_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::greatest_X_$1_MANY$__X_1_\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_DateTime_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::greatest_X_$1_MANY$__X_1_\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Date_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::greatest_X_$1_MANY$__X_1_\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Empty_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::greatest_X_MANY__X_$0_1$_\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Float_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::greatest_X_$1_MANY$__X_1_\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Integer_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::greatest_X_$1_MANY$__X_1_\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Number_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::greatest_X_$1_MANY$__X_1_\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Single_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::greatest_X_$1_MANY$__X_1_\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_StrictDate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::greatest_X_$1_MANY$__X_1_\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_String_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::greatest_X_$1_MANY$__X_1_\""),

            one("meta::pure::functions::collection::tests::in::testInForDecimal_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::in_Any_1__Any_MANY__Boolean_1_\""),
            one("meta::pure::functions::collection::tests::in::testInIsEmpty_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::in_Any_$0_1$__Any_MANY__Boolean_1_\""),
            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "Unexpected token"),
            one("meta::pure::functions::collection::tests::in::testInPrimitive_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::in_Any_1__Any_MANY__Boolean_1_\""),
            one("meta::pure::functions::collection::tests::in::testIn_relation_extend_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1__FuncColSpecArray_1__Relation_1_\""),

            one("meta::pure::functions::collection::tests::least::testLeast_Boolean_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::least_X_$1_MANY$__X_1_\""),
            one("meta::pure::functions::collection::tests::least::testLeast_DateTime_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::least_X_$1_MANY$__X_1_\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Date_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::least_X_$1_MANY$__X_1_\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Empty_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::least_X_MANY__X_$0_1$_\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Float_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::least_X_$1_MANY$__X_1_\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Integer_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::least_X_$1_MANY$__X_1_\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Number_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::least_X_$1_MANY$__X_1_\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Single_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::least_X_$1_MANY$__X_1_\""),
            one("meta::pure::functions::collection::tests::least::testLeast_StrictDate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::least_X_$1_MANY$__X_1_\""),
            one("meta::pure::functions::collection::tests::least::testLeast_String_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::least_X_$1_MANY$__X_1_\""),

            one("meta::pure::functions::collection::tests::max::testMax_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::fold_T_MANY__Function_1__V_m__V_m_\""),

            one("meta::pure::functions::collection::tests::min::testMin_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::fold_T_MANY__Function_1__V_m__V_m_\""),

            one("meta::pure::functions::collection::tests::or::testOr_Function_1__Boolean_1_", "Can't find the packageable element 'ortrue'"),

            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketDays_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::date::timeBucket_DateTime_1__Integer_1__DurationUnit_1__DateTime_1_\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketHours_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::date::timeBucket_DateTime_1__Integer_1__DurationUnit_1__DateTime_1_\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketMinutes_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::date::timeBucket_DateTime_1__Integer_1__DurationUnit_1__DateTime_1_\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketMonths_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::date::timeBucket_DateTime_1__Integer_1__DurationUnit_1__DateTime_1_\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketSeconds_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::date::timeBucket_DateTime_1__Integer_1__DurationUnit_1__DateTime_1_\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketWeeks_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::date::timeBucket_DateTime_1__Integer_1__DurationUnit_1__DateTime_1_\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketYears_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::date::timeBucket_DateTime_1__Integer_1__DurationUnit_1__DateTime_1_\""),

            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketDays_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::date::timeBucket_StrictDate_1__Integer_1__DurationUnit_1__StrictDate_1_\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketHours_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"function not supported yet: meta::pure::functions::date::timeBucket_StrictDate_1__Integer_1__DurationUnit_1__StrictDate_1_\"\nwhere the expected message was:\"Unsupported duration unit for StrictDate. Units can only be: [YEARS, DAYS, MONTHS, WEEKS]\"\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketMinutes_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"function not supported yet: meta::pure::functions::date::timeBucket_StrictDate_1__Integer_1__DurationUnit_1__StrictDate_1_\"\nwhere the expected message was:\"Unsupported duration unit for StrictDate. Units can only be: [YEARS, DAYS, MONTHS, WEEKS]\"\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketMonths_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::date::timeBucket_StrictDate_1__Integer_1__DurationUnit_1__StrictDate_1_\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketSeconds_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"function not supported yet: meta::pure::functions::date::timeBucket_StrictDate_1__Integer_1__DurationUnit_1__StrictDate_1_\"\nwhere the expected message was:\"Unsupported duration unit for StrictDate. Units can only be: [YEARS, DAYS, MONTHS, WEEKS]\"\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketWeeks_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::date::timeBucket_StrictDate_1__Integer_1__DurationUnit_1__StrictDate_1_\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketYears_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::date::timeBucket_StrictDate_1__Integer_1__DurationUnit_1__StrictDate_1_\""),

            one("meta::pure::functions::hashCode::tests::testHashCode_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::hash::hashCode_Any_MANY__Integer_1_\""),

            one("meta::pure::functions::math::hashCode::tests::testHashCodeAggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),

            one("meta::pure::functions::math::tests::average::testAverage_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::average::testAverage_Round_Integers_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),

            one("meta::pure::functions::math::tests::bitAnd::testBitAnd_LargeNumbers_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::bitAnd_Integer_1__Integer_1__Integer_1_\""),
            one("meta::pure::functions::math::tests::bitAnd::testBitAnd_NegativeNumbers_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::bitAnd_Integer_1__Integer_1__Integer_1_\""),
            one("meta::pure::functions::math::tests::bitAnd::testBitAnd_SmallNumbers_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::bitAnd_Integer_1__Integer_1__Integer_1_\""),

            one("meta::pure::functions::math::tests::bitNot::testBitNot_LargeNumbers_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::bitNot_Integer_1__Integer_1_\""),
            one("meta::pure::functions::math::tests::bitNot::testBitNot_NegativeNumbers_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::bitNot_Integer_1__Integer_1_\""),
            one("meta::pure::functions::math::tests::bitNot::testBitNot_SmallNumbers_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::bitNot_Integer_1__Integer_1_\""),

            one("meta::pure::functions::math::tests::bitOr::testBitOr_LargeNumbers_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::bitOr_Integer_1__Integer_1__Integer_1_\""),
            one("meta::pure::functions::math::tests::bitOr::testBitOr_NegativeNumbers_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::bitOr_Integer_1__Integer_1__Integer_1_\""),
            one("meta::pure::functions::math::tests::bitOr::testBitOr_SmallNumbers_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::bitOr_Integer_1__Integer_1__Integer_1_\""),

            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"function not supported yet: meta::pure::functions::math::bitShiftLeft_Integer_1__Integer_1__Integer_1_\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\""),
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_UpTo62Bits_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::bitShiftLeft_Integer_1__Integer_1__Integer_1_\""),

            one("meta::pure::functions::math::tests::bitShiftRight::testBitShiftRight_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"function not supported yet: meta::pure::functions::math::bitShiftRight_Integer_1__Integer_1__Integer_1_\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\""),
            one("meta::pure::functions::math::tests::bitShiftRight::testBitShiftRight_UpTo62Bits_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::bitShiftRight_Integer_1__Integer_1__Integer_1_\""),

            one("meta::pure::functions::math::tests::bitXor::testBitXor_LargeNumbers_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::bitXor_Integer_1__Integer_1__Integer_1_\""),
            one("meta::pure::functions::math::tests::bitXor::testBitXor_NegativeNumbers_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::bitXor_Integer_1__Integer_1__Integer_1_\""),
            one("meta::pure::functions::math::tests::bitXor::testBitXor_SmallNumbers_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::bitXor_Integer_1__Integer_1__Integer_1_\""),

            one("meta::pure::functions::math::tests::corr::testCorr_Function_1__Boolean_1_", "Unexpected error executing Deephaven query: io.deephaven.client.impl.TableHandle$TableHandleException"), // io.deephaven.function.Numeric.cor(new int[]{1, 3.0, 6.0}, 10, -20, 30) -> Cannot find method cor([I, int, int, int) in class io.deephaven.function.Numeric
            one("meta::pure::functions::math::tests::corr::testSimpleWindowCorr_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),

            one("meta::pure::functions::math::tests::covarPopulation::testCovarPopulation_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::covarPopulation_Number_MANY__Number_MANY__Number_$0_1$_\""),
            one("meta::pure::functions::math::tests::covarPopulation::testSimpleWindowCovarPopulation_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),

            one("meta::pure::functions::math::tests::covarSample::testCovarSample_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::covarSample_Number_MANY__Number_MANY__Number_$0_1$_\""),
            one("meta::pure::functions::math::tests::covarSample::testSimpleWindowCovarSample_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),

            one("meta::pure::functions::math::tests::max::testMax_Floats_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::lang::if_Boolean_1__Function_1__Function_1__T_m_\""),
            one("meta::pure::functions::math::tests::max::testMax_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::max::testMax_Floats_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::lang::if_Boolean_1__Function_1__Function_1__T_m_\""),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::lang::if_Boolean_1__Function_1__Function_1__T_m_\""),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),

            one("meta::pure::functions::math::tests::maxBy::testMaxBy_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::maxBy_T_MANY__Number_MANY__T_$0_1$_\""),
            one("meta::pure::functions::math::tests::maxBy::testSimpleGroupByMaxBy_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_\""),

            one("meta::pure::functions::math::tests::median::testMedian_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Function_1__Boolean_1_", "Unexpected error executing Deephaven query: io.deephaven.client.impl.TableHandle$TableHandleException"), // Caused by: io.deephaven.UncheckedDeephavenException: Error Compiling Formula Expression: io.deephaven.function.Numeric.median(new int[] { 5, 1.0, 2, 8.0, 3, 4 }) incompatible types: possible lossy conversion from double to int
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),

            one("meta::pure::functions::math::tests::min::testMin_Floats_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::lang::if_Boolean_1__Function_1__Function_1__T_m_\""),
            one("meta::pure::functions::math::tests::min::testMin_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::min::testMin_Floats_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::lang::if_Boolean_1__Function_1__Function_1__T_m_\""),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::lang::if_Boolean_1__Function_1__Function_1__T_m_\""),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),

            one("meta::pure::functions::math::tests::minBy::testMinBy_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::minBy_T_MANY__Number_MANY__T_$0_1$_\""),
            one("meta::pure::functions::math::tests::minBy::testSimpleGroupByMinBy_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_\""),

            one("meta::pure::functions::math::tests::mode::testMode_Float_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::mode_Float_MANY__Float_1_\""),
            one("meta::pure::functions::math::tests::mode::testMode_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::mode::testMode_Floats_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::mode_Integer_MANY__Integer_1_\""),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::mode_Integer_MANY__Integer_1_\""),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),

            one("meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::collection::map_T_m__Function_1__V_m_\""),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),

            one("meta::pure::functions::math::tests::pi::testPi_Function_1__Boolean_1_", "\"Match failure: LiteralObject instanceOf Literal\""),

            one("meta::pure::functions::math::tests::stdDev::testFloatStdDev_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::stdDev_Number_$1_MANY$__Boolean_1__Number_1_\""),
            one("meta::pure::functions::math::tests::stdDev::testIntStdDev_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::stdDev_Number_$1_MANY$__Boolean_1__Number_1_\""),
            one("meta::pure::functions::math::tests::stdDev::testMixedStdDev_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::stdDev_Number_$1_MANY$__Boolean_1__Number_1_\""),
            one("meta::pure::functions::math::tests::stdDev::testNegativeNumberStdDev_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::stdDev_Number_$1_MANY$__Boolean_1__Number_1_\""),
            one("meta::pure::functions::math::tests::stdDev::testPopulationStandardDeviation_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::stdDev_Number_$1_MANY$__Boolean_1__Number_1_\""),
            one("meta::pure::functions::math::tests::stdDev::testSimpleGroupByStandardDeviationPopulation_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::stdDev::testSimpleGroupByStandardDeviationSample_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::stdDev::testSimpleWindowStandardDeviationPopulation_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::stdDev::testSimpleWindowStandardDeviationSample_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),

            one("meta::pure::functions::math::tests::sum::testSum_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::sum::testSum_Floats_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::sum::testSum_Integers_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::sum::testSum_Integers_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::sum::testSum_Numbers_Function_1__Boolean_1_", "Unexpected error executing Deephaven query: io.deephaven.client.impl.TableHandle$TableHandleException"), // Caused by: io.deephaven.UncheckedDeephavenException: Error Compiling Formula Expression: io.deephaven.function.Numeric.sum(new int[] { 15, 13, 2.0, 1, 1.0 }) incompatible types: possible lossy conversion from double to int
            one("meta::pure::functions::math::tests::sum::testSum_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::sum::testSum_Numbers_Relation_Window_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),

            one("meta::pure::functions::math::tests::toRadians::testToRadians_Function_1__Boolean_1_", "Unexpected error executing Deephaven query: io.deephaven.client.impl.TableHandle$TableHandleException"), // Caused by: io.deephaven.UncheckedDeephavenException: Error Compiling Formula Expression: divide(io.deephaven.function.Numeric.product(new int[] { 180, 3.141592653589793 }), 180) incompatible types: possible lossy conversion from double to int

            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_Identities_Function_1__Boolean_1_", "\"Cannot cast a collection of size 2 to multiplicity [1]\""),

            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_Identities_Function_1__Boolean_1_", "Unexpected error executing Deephaven query: io.deephaven.client.impl.TableHandle$TableHandleException"),

            one("meta::pure::functions::math::tests::variance::testSimpleGroupByVariancePopulation_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::variance::testSimpleGroupByVarianceSample_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpecArray_1__AggColSpecArray_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::variance::testSimpleWindowVariancePopulation_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::variance::testSimpleWindowVarianceSample_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1___Window_1__AggColSpec_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::variance::testVariancePopulation_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::math::variancePopulation_Number_MANY__Number_1_\""),
            one("meta::pure::functions::math::tests::variance::testVariance_Population_Function_1__Boolean_1_", "\"Variance population is not supported yet in Deephaven extension\""),

            one("meta::pure::functions::math::tests::wavg::testSimpleGroupByMultipleWavg_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpecArray_1__Relation_1_\""),
            one("meta::pure::functions::math::tests::wavg::testSimpleGroupByWavg_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),

            one("meta::pure::functions::string::generation::tests::generateGuid::testGenerateGuidWithRelation_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::extend_Relation_1__FuncColSpec_1__Relation_1_\""),
            one("meta::pure::functions::string::generation::tests::generateGuid::testGenerateGuid_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::string::generation::generateGuid__String_1_\""),

            one("meta::pure::functions::tests::date::testDayOfWeek_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::date::dayOfWeek_Date_1__DayOfWeek_1_\""),
            one("meta::pure::functions::tests::date::testDayOfWeek_Relation_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestDeephavenConnectionIntegrationLoader.extensions().getFirst())
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
