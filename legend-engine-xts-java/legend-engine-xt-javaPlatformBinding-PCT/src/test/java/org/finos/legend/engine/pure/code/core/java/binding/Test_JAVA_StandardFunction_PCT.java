// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.pure.code.core.java.binding;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.code.core.CoreJavaPlatformBindingCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreStandardFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_JAVA_StandardFunction_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
    private static final Adapter adapter = CoreJavaPlatformBindingCodeRepositoryProvider.javaAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            one("meta::pure::functions::math::tests::stdDev::testSimpleGroupByStandardDeviationPopulation_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::stdDev::testSimpleGroupByStandardDeviationSample_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),

            one("meta::pure::functions::math::tests::variance::testSimpleGroupByVariancePopulation_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::variance::testSimpleGroupByVarianceSample_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),

            one("meta::pure::functions::math::tests::wavg::testSimpleGroupByWavg_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::wavg::testSimpleGroupByMultipleWavg_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),

            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "\"Unhandled value type: meta::pure::functions::collection::tests::in::Firm\""),

            // Max
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1.0\""),
            one("meta::pure::functions::math::tests::max::testMax_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::max::testMax_Floats_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::collection::tests::max::testMax_Function_1__Boolean_1_", "Cannot cast a collection of size 0 to multiplicity [1]"),

            // Min
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.23D\nactual:   1.23\""),
            one("meta::pure::functions::math::tests::min::testMin_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::min::testMin_Floats_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::collection::tests::min::testMin_Function_1__Boolean_1_", "Cannot cast a collection of size 0 to multiplicity [1]"),

            // Median
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Function_1__Boolean_1_", "Failed in node: root"),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Function_1__Boolean_1_", "Failed in node: root"),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),

            // Mode
            one("meta::pure::functions::math::tests::mode::testMode_Float_Function_1__Boolean_1_", "\"copy_T_1__String_1__KeyExpression_MANY__T_1_ is prohibited!\""),
            one("meta::pure::functions::math::tests::mode::testMode_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::mode::testMode_Floats_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Function_1__Boolean_1_", "\"copy_T_1__String_1__KeyExpression_MANY__T_1_ is prohibited!\""),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Function_1__Boolean_1_", "\"copy_T_1__String_1__KeyExpression_MANY__T_1_ is prohibited!\""),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),

            // Date
            pack("meta::pure::functions::date::tests::timeBucket::dateTime", "\"meta::pure::functions::date::timeBucket_DateTime_1__Integer_1__DurationUnit_1__DateTime_1_ is not supported yet!\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketDays_Function_1__Boolean_1_", "\"meta::pure::functions::date::timeBucket_StrictDate_1__Integer_1__DurationUnit_1__StrictDate_1_ is not supported yet!\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketHours_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"meta::pure::functions::date::timeBucket_StrictDate_1__Integer_1__DurationUnit_1__StrictDate_1_ is not supported yet!\"\nwhere the expected message was:\"Unsupported duration unit for StrictDate. Units can only be: [YEARS, DAYS, MONTHS, WEEKS]\"\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketMinutes_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"meta::pure::functions::date::timeBucket_StrictDate_1__Integer_1__DurationUnit_1__StrictDate_1_ is not supported yet!\"\nwhere the expected message was:\"Unsupported duration unit for StrictDate. Units can only be: [YEARS, DAYS, MONTHS, WEEKS]\"\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketMonths_Function_1__Boolean_1_", "\"meta::pure::functions::date::timeBucket_StrictDate_1__Integer_1__DurationUnit_1__StrictDate_1_ is not supported yet!\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketSeconds_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"meta::pure::functions::date::timeBucket_StrictDate_1__Integer_1__DurationUnit_1__StrictDate_1_ is not supported yet!\"\nwhere the expected message was:\"Unsupported duration unit for StrictDate. Units can only be: [YEARS, DAYS, MONTHS, WEEKS]\"\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketWeeks_Function_1__Boolean_1_", "\"meta::pure::functions::date::timeBucket_StrictDate_1__Integer_1__DurationUnit_1__StrictDate_1_ is not supported yet!\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketYears_Function_1__Boolean_1_", "\"meta::pure::functions::date::timeBucket_StrictDate_1__Integer_1__DurationUnit_1__StrictDate_1_ is not supported yet!\""),

            // Average
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Function_1__Boolean_1_", "error: incompatible types: double cannot be converted to java.util.List<java.lang.Number>"),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Function_1__Boolean_1_", "error: incompatible types: long cannot be converted to java.util.List<java.lang.Number>"),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Function_1__Boolean_1_", "error: incompatible types: java.math.BigDecimal cannot be converted to java.util.List<java.lang.Number>"),
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),

            // Percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_", "eval_Function_1__T_n__V_m_ is prohibited!"),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),

            // StdDev
            one("meta::pure::functions::math::tests::stdDev::testSimpleWindowStandardDeviationPopulation_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::stdDev::testSimpleWindowStandardDeviationSample_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),

            // Variance
            one("meta::pure::functions::math::tests::variance::testSimpleWindowVariancePopulation_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::variance::testSimpleWindowVarianceSample_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),

            // Covariance/Correlation
            one("meta::pure::functions::math::tests::corr::testCorr_Function_1__Boolean_1_", "\"meta::pure::functions::collection::zip_T_MANY__U_MANY__Pair_MANY_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::corr::testSimpleWindowCorr_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::covarPopulation::testCovarPopulation_Function_1__Boolean_1_", "\"meta::pure::functions::collection::zip_T_MANY__U_MANY__Pair_MANY_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::covarPopulation::testSimpleWindowCovarPopulation_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::covarSample::testCovarSample_Function_1__Boolean_1_", "\"meta::pure::functions::collection::zip_T_MANY__U_MANY__Pair_MANY_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::covarSample::testSimpleWindowCovarSample_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),

            // Sum
            one("meta::pure::functions::math::tests::sum::testSum_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::sum::testSum_Floats_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::sum::testSum_Integers_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::sum::testSum_Integers_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::sum::testSum_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::pure::functions::math::tests::sum::testSum_Numbers_Relation_Window_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),

            // CosH
            pack("meta::pure::functions::math::tests::trigonometry::cosh", "\"meta::pure::functions::math::cosh_Number_1__Float_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_EvalFuncSig_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),

            // SinH
            pack("meta::pure::functions::math::tests::trigonometry::sinh", "\"meta::pure::functions::math::sinh_Number_1__Float_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_EvalFuncSig_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),

            // TanH
            pack("meta::pure::functions::math::tests::trigonometry::tanh", "\"meta::pure::functions::math::tanh_Number_1__Float_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_EvalFuncSig_Function_1__Boolean_1_", "\"eval_Function_1__T_n__V_m_ is prohibited!\""),

            // Greatest
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Empty_Function_1__Boolean_1_", "\"Cast exception: JSONNull cannot be cast to JSONObject\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Number_Function_1__Boolean_1_", "\"\nexpected: 7.345D\nactual:   7.345\""),

            // Least
            one("meta::pure::functions::collection::tests::least::testLeast_Empty_Function_1__Boolean_1_", "\"Cast exception: JSONNull cannot be cast to JSONObject\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Number_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1.0\""),

            // Bitwise
            pack("meta::pure::functions::math::tests::bitAnd", "\"meta::pure::functions::math::bitAnd_Integer_1__Integer_1__Integer_1_ is not supported yet!\""),
            pack("meta::pure::functions::math::tests::bitNot",  "\"meta::pure::functions::math::bitNot_Integer_1__Integer_1_ is not supported yet!\""),
            pack("meta::pure::functions::math::tests::bitOr", "\"meta::pure::functions::math::bitOr_Integer_1__Integer_1__Integer_1_ is not supported yet!\""),
            pack("meta::pure::functions::math::tests::bitXor", "\"meta::pure::functions::math::bitXor_Integer_1__Integer_1__Integer_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"meta::pure::functions::math::bitShiftLeft_Integer_1__Integer_1__Integer_1_ is not supported yet!\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\""),
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_UpTo62Bits_Function_1__Boolean_1_", "\"meta::pure::functions::math::bitShiftLeft_Integer_1__Integer_1__Integer_1_ is not supported yet!\""),
            one("meta::pure::functions::math::tests::bitShiftRight::testBitShiftRight_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"meta::pure::functions::math::bitShiftRight_Integer_1__Integer_1__Integer_1_ is not supported yet!\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\""),
            one("meta::pure::functions::math::tests::bitShiftRight::testBitShiftRight_UpTo62Bits_Function_1__Boolean_1_", "\"meta::pure::functions::math::bitShiftRight_Integer_1__Integer_1__Integer_1_ is not supported yet!\""),

            // UUID
            one("meta::pure::functions::string::generation::tests::testGenerateGuidWithRelation_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"")
    );

    public static Test suite()
    {
        return PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter);
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
