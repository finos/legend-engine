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

package org.finos.legend.engine.plan.execution.stores.relational.test.athena.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalAthenaPCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreStandardFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Athena_StandardFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
    private static final Adapter adapter = CoreRelationalAthenaPCTCodeRepositoryProvider.athenaAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(

            //In
            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "\"Parameter to IN operation isn't a literal!\""),
            one("meta::pure::functions::collection::tests::in::testInIsEmpty_Function_1__Boolean_1_", "NullPointer exception"),
            one("meta::pure::functions::collection::tests::in::testInPrimitive_Function_1__Boolean_1_", "TYPE_MISMATCH: line 1:26: IN value and list items must be the same type or coercible to a common type. Cannot find common type between integer and varchar(1), all types (without duplicates): [integer, varchar(1), boolean, date]"),

            //Bit And
            one("meta::pure::functions::math::tests::bitAnd::testBitAnd_LargeNumbers_Function_1__Boolean_1_", "INVALID_CAST_ARGUMENT: Cannot cast '-9223372036854775808' to INT"),

            //Bit Not
            one("meta::pure::functions::math::tests::bitNot::testBitNot_LargeNumbers_Function_1__Boolean_1_", "INVALID_CAST_ARGUMENT: Cannot cast '-9223372036854775808' to INT"),

            //Bit Or
            one("meta::pure::functions::math::tests::bitOr::testBitOr_LargeNumbers_Function_1__Boolean_1_", "INVALID_CAST_ARGUMENT: Cannot cast '-9223372036854775808' to INT"),

            //Bit Shift Left
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"No error was thrown\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\""),
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_UpTo62Bits_Function_1__Boolean_1_", "\"\nexpected: 0\nactual:   70368744177664\""),

            //Bit Shift Right
            one("meta::pure::functions::math::tests::bitShiftRight::testBitShiftRight_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"No error was thrown\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\""),

            //Bit Xor
            one("meta::pure::functions::math::tests::bitXor::testBitXor_LargeNumbers_Function_1__Boolean_1_", "INVALID_CAST_ARGUMENT: Cannot cast '-9223372036854775808' to INT"),

            //Max
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "\"\nexpected: 2\nactual:   2.0\""),

            //Average (Average of lists is calculated wrongly)
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Function_1__Boolean_1_", "\"\nexpected: 3.8\nactual:   5.0\""),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Function_1__Boolean_1_", "\"\nexpected: 3.8\nactual:   5.0\""),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Function_1__Boolean_1_", "\"\nexpected: 3.8\nactual:   5.0\""),

            //Median
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - percentile_cont\""),
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - percentile_cont\""),
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Relation_Window_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - percentile_cont\""),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - percentile_cont\""),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Relation_Aggregate_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - percentile_cont\""),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Relation_Window_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - percentile_cont\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - percentile_cont\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - percentile_cont\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Relation_Window_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - percentile_cont\""),

            //And
            one("meta::pure::functions::collection::tests::and::testAnd_Function_1__Boolean_1_", "Can't find the packageable element 'andtrue'"),

            //TimeBucket
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketDays_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - time_bucket\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketHours_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - time_bucket\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketMinutes_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - time_bucket\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketMonths_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - time_bucket\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketSeconds_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - time_bucket\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketWeeks_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - time_bucket\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketYears_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - time_bucket\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketDays_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - time_bucket\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketMonths_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - time_bucket\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketWeeks_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - time_bucket\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketYears_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - time_bucket\""),

            //HashCode
            one("meta::pure::functions::hashCode::tests::testHashCode_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for hashCode().\""),

            //HashCodeAggregate
            one("meta::pure::functions::math::hashCode::tests::testHashCodeAggregate_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for hashAgg().\""),

            //Corr
            one("meta::pure::functions::math::tests::corr::testCorr_Function_1__Boolean_1_", "\"No matched function in function registry for - corr(IntegerSqlType, IntegerSqlType, IntegerSqlType, IntegerSqlType).\nAvailable variations for that function - [\n  corr(DoubleSqlType, DoubleSqlType):DoubleSqlType,\n  corr(AbstractNumericSqlType, AbstractNumericSqlType):AbstractNumericSqlType\n]\""),

            //CovarPopulation
            one("meta::pure::functions::math::tests::covarPopulation::testCovarPopulation_Function_1__Boolean_1_", "\"No matched function in function registry for - covar_pop(IntegerSqlType, IntegerSqlType, IntegerSqlType, IntegerSqlType).\nAvailable variations for that function - [\n  covar_pop(DoubleSqlType, DoubleSqlType):DoubleSqlType,\n  covar_pop(AbstractNumericSqlType, AbstractNumericSqlType):AbstractNumericSqlType\n]\""),

            //CovarSample
            one("meta::pure::functions::math::tests::covarSample::testCovarSample_Function_1__Boolean_1_", "\"No matched function in function registry for - covar_samp(IntegerSqlType, IntegerSqlType, IntegerSqlType, IntegerSqlType).\nAvailable variations for that function - [\n  covar_samp(DoubleSqlType, DoubleSqlType):DoubleSqlType,\n  covar_samp(AbstractNumericSqlType, AbstractNumericSqlType):AbstractNumericSqlType\n]\""),

            //Max
            one("meta::pure::functions::math::tests::max::testMax_Floats_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariantList().\""),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariantList().\""),

            //MaxBy
            one("meta::pure::functions::math::tests::maxBy::testMaxBy_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for maxBy().\""),
            one("meta::pure::functions::math::tests::maxBy::testSimpleGroupByMaxBy_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for maxBy().\""),

            //Min
            one("meta::pure::functions::math::tests::min::testMin_Floats_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariantList().\""),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for toVariantList().\""),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.23D\nactual:   1.23\""),
            //MinBy
            one("meta::pure::functions::math::tests::minBy::testMinBy_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for minBy().\""),
            one("meta::pure::functions::math::tests::minBy::testSimpleGroupByMinBy_Function_1__Boolean_1_", "\"Couldn't find DynaFunction to Postgres model translation for minBy().\""),

            //Mode
            one("meta::pure::functions::math::tests::mode::testMode_Float_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - mode\""),
            one("meta::pure::functions::math::tests::mode::testMode_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - mode\""),
            one("meta::pure::functions::math::tests::mode::testMode_Floats_Relation_Window_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - mode\""),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - mode\""),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Relation_Aggregate_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - mode\""),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Relation_Window_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - mode\""),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - mode\""),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Relation_Aggregate_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - mode\""),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Relation_Window_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - mode\""),

            //Percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            //StdDev
            one("meta::pure::functions::math::tests::stdDev::testFloatStdDev_Function_1__Boolean_1_", "\"No matched function in function registry for - var_pop(DoubleSqlType, DoubleSqlType, DoubleSqlType, BooleanSqlType).\nAvailable variations for that function - [\n  var_pop(DoubleSqlType):DoubleSqlType,\n  var_pop(AbstractNumericSqlType):AbstractNumericSqlType\n]\""),
            one("meta::pure::functions::math::tests::stdDev::testIntStdDev_Function_1__Boolean_1_", "\"No matched function in function registry for - var_pop(IntegerSqlType, IntegerSqlType, IntegerSqlType, BooleanSqlType).\nAvailable variations for that function - [\n  var_pop(DoubleSqlType):DoubleSqlType,\n  var_pop(AbstractNumericSqlType):AbstractNumericSqlType\n]\""),
            one("meta::pure::functions::math::tests::stdDev::testMixedStdDev_Function_1__Boolean_1_", "\"No matched function in function registry for - var_pop(DoubleSqlType, IntegerSqlType, DoubleSqlType, BooleanSqlType).\nAvailable variations for that function - [\n  var_pop(DoubleSqlType):DoubleSqlType,\n  var_pop(AbstractNumericSqlType):AbstractNumericSqlType\n]\""),
            one("meta::pure::functions::math::tests::stdDev::testNegativeNumberStdDev_Function_1__Boolean_1_", "\"No matched function in function registry for - var_pop(IntegerSqlType, IntegerSqlType, IntegerSqlType, BooleanSqlType).\nAvailable variations for that function - [\n  var_pop(DoubleSqlType):DoubleSqlType,\n  var_pop(AbstractNumericSqlType):AbstractNumericSqlType\n]\""),
            one("meta::pure::functions::math::tests::stdDev::testPopulationStandardDeviation_Function_1__Boolean_1_", "\"No matched function in function registry for - var_pop(IntegerSqlType, IntegerSqlType, BooleanSqlType).\nAvailable variations for that function - [\n  var_pop(DoubleSqlType):DoubleSqlType,\n  var_pop(AbstractNumericSqlType):AbstractNumericSqlType\n]\""),

            //Trigonometry
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_EvalFuncSig_Function_1__Boolean_1_", "\"No matched function in function registry for - cosh(DoubleSqlType, DoubleSqlType).\nAvailable variations for that function - [\n  cosh(AbstractNumericSqlType):DoubleSqlType\n]\""),
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_EvalFuncSig_Function_1__Boolean_1_", "\"No matched function in function registry for - sinh(DoubleSqlType, DoubleSqlType).\nAvailable variations for that function - [\n  sinh(AbstractNumericSqlType):DoubleSqlType\n]\""),
            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_EvalFuncSig_Function_1__Boolean_1_", "\"No matched function in function registry for - tanh(DoubleSqlType, DoubleSqlType).\nAvailable variations for that function - [\n  tanh(AbstractNumericSqlType):DoubleSqlType\n]\""),

            //Variance
            one("meta::pure::functions::math::tests::variance::testVariancePopulation_Function_1__Boolean_1_", "\"No matched function in function registry for - var_pop(IntegerSqlType, IntegerSqlType).\nAvailable variations for that function - [\n  var_pop(DoubleSqlType):DoubleSqlType,\n  var_pop(AbstractNumericSqlType):AbstractNumericSqlType\n]\""),
            one("meta::pure::functions::math::tests::variance::testVarianceSample_Function_1__Boolean_1_", "\"No matched function in function registry for - var_samp(DoubleSqlType, DoubleSqlType, DoubleSqlType).\nAvailable variations for that function - [\n  var_samp(DoubleSqlType):DoubleSqlType,\n  var_samp(AbstractNumericSqlType):AbstractNumericSqlType\n]\""),
            one("meta::pure::functions::math::tests::variance::testVariance_Population_Function_1__Boolean_1_", "\"No matched function in function registry for - var_pop(IntegerSqlType, IntegerSqlType, BooleanSqlType).\nAvailable variations for that function - [\n  var_pop(DoubleSqlType):DoubleSqlType,\n  var_pop(AbstractNumericSqlType):AbstractNumericSqlType\n]\""),
            one("meta::pure::functions::math::tests::variance::testVariance_Sample_Function_1__Boolean_1_", "\"No matched function in function registry for - var_pop(DoubleSqlType, DoubleSqlType, DoubleSqlType, BooleanSqlType).\nAvailable variations for that function - [\n  var_pop(DoubleSqlType):DoubleSqlType,\n  var_pop(AbstractNumericSqlType):AbstractNumericSqlType\n]\""),

            //Collections Greatest
            one("meta::pure::functions::collection::tests::greatest::testGreatest_DateTime_Function_1__Boolean_1_", "\"\nexpected: %2025-02-10T20:10:20+0000\nactual:   %2025-02-10T20:10:20.000000000+0000\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Number_Function_1__Boolean_1_", "\"\nexpected: 2\nactual:   2.0\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Single_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1D\""),

            //Collections Least
            one("meta::pure::functions::collection::tests::least::testLeast_DateTime_Function_1__Boolean_1_", "\"\nexpected: %2025-01-10T15:25:30+0000\nactual:   %2025-01-10T15:25:30.000000000+0000\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Number_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1.0\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Single_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1D\""),

            //Collections Max
            one("meta::pure::functions::collection::tests::max::testMax_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'tail_T_MANY__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            //Collections Min
            one("meta::pure::functions::collection::tests::min::testMin_Function_1__Boolean_1_", "\"No SQL translation exists for the PURE function 'tail_T_MANY__T_MANY_'. \nIf you would like to add a SQL translation for the function then follow the step-by-step guide on the PURE wiki.\""),

            //Collections Or
            one("meta::pure::functions::collection::tests::or::testOr_Function_1__Boolean_1_", "Can't find the packageable element 'ortrue'"),

            //Percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Aggregate_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - percentile_disc\""),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Window_Function_1__Boolean_1_", "\"Athena SQL Dialect does not support the function - percentile_cont\"")

    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Athena).getFirst())
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
