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
            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "\"Parameter to IN operation isn't a literal!\""),
            one("meta::pure::functions::collection::tests::in::testInPrimitive_Function_1__Boolean_1_", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')"),

            // Or
            one("meta::pure::functions::collection::tests::or::testOr_Function_1__Boolean_1_", "Can't find the packageable element 'ortrue'"),

            // Xor
            one("meta::pure::functions::boolean::tests::operation::xor::testXor_BinaryExpressions_Function_1__Boolean_1_", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::boolean::tests::operation::xor::testXor_BinaryTruthTable_Function_1__Boolean_1_", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')"),

            // Max
            one("meta::pure::functions::math::tests::max::testMax_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"max(%s)\"\""),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"max(%s)\"\""),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "\"\nexpected: 2\nactual:   2.0\""),
            one("meta::pure::functions::collection::tests::max::testMax_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::collection::max(?)'"),

            // Min
            one("meta::pure::functions::math::tests::min::testMin_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"min(%s)\"\""),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"min(%s)\"\""),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.23D\nactual:   1.23\""),
            one("meta::pure::functions::collection::tests::min::testMin_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::collection::min(?)'"),

            // Median
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Relation_Window_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,grp,name,newCol\n   10.1,0,J,10.1\n   2.2,1,B,6.6\n   6.6,1,F,6.6\n   8.8,1,H,6.6\n   1.1,2,A,3.3\n   5.5,2,E,3.3\n   3.3,3,C,5.5\n   7.7,3,G,5.5\n   4.4,4,D,4.4\n   9.9,5,I,9.9\n#'"),

            // Mode
            one("meta::pure::functions::math::tests::mode::testMode_Float_Function_1__Boolean_1_", "Function 'for_testing.mode' is not defined"),
            one("meta::pure::functions::math::tests::mode::testMode_Floats_Relation_Aggregate_Function_1__Boolean_1_", "Function 'for_testing.mode' is not defined"),
            one("meta::pure::functions::math::tests::mode::testMode_Floats_Relation_Window_Function_1__Boolean_1_", "You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near 'OVER (Partition By "),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Function_1__Boolean_1_", "Function 'for_testing.mode' is not defined"),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Relation_Aggregate_Function_1__Boolean_1_", "Function 'for_testing.mode' is not defined"),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Relation_Window_Function_1__Boolean_1_", "You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near 'OVER (Partition By "),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Function_1__Boolean_1_", "Function 'for_testing.mode' is not defined"),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Relation_Aggregate_Function_1__Boolean_1_", "Function 'for_testing.mode' is not defined"),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Relation_Window_Function_1__Boolean_1_", "You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near 'OVER (Partition By "),

            // Average
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\""),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\""),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\""),

            // Percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::collection::range(?)'"),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Window_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,val,newCol\n   1,1.0,2.4\n   1,2.0,2.4\n   1,3.0,2.4\n   2,1.5,2.9\n   2,2.5,2.9\n   2,3.5,2.9\n   3,1.0,1.7\n   3,1.5,1.7\n   3,2.0,1.7\n#'"),

            // CosH
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_EvalFuncSig_Function_1__Boolean_1_", "Function 'for_testing.cosh' is not defined"),
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_Eval_Function_1__Boolean_1_", "Function 'for_testing.cosh' is not defined"),

            // SinH
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_EvalFuncSig_Function_1__Boolean_1_", "Function 'for_testing.sinh' is not defined"),
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_Eval_Function_1__Boolean_1_", "Function 'for_testing.sinh' is not defined"),

            // TanH
            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_EvalFuncSig_Function_1__Boolean_1_", "Function 'for_testing.tanh' is not defined"),
            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_Eval_Function_1__Boolean_1_", "Function 'for_testing.tanh' is not defined"),

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

            // TimeBucket
            pack("meta::pure::functions::date::tests::timeBucket::dateTime", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketDays_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketMonths_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketWeeks_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketYears_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),

            // Inequalities
            pack("meta::pure::functions::boolean::tests::inequalities::between", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')"),

            // Greatest
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Boolean_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_DateTime_Function_1__Boolean_1_", "Invalid date string: '2025-02-10 20:10:20'"),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Date_Function_1__Boolean_1_", "Invalid date string: '2025-02-10 20:10:20'"),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Empty_Function_1__Boolean_1_", "Too few arguments to function 'greatest'"),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Number_Function_1__Boolean_1_", "\"\nexpected: 2\nactual:   2.0\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Single_Function_1__Boolean_1_", "Too few arguments to function 'greatest'"),

            // Least
            one("meta::pure::functions::collection::tests::least::testLeast_Boolean_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::collection::tests::least::testLeast_DateTime_Function_1__Boolean_1_", "Invalid date string: '2025-01-10 15:25:30'"),
            one("meta::pure::functions::collection::tests::least::testLeast_Date_Function_1__Boolean_1_", "Invalid date string: '2025-01-10 15:25:30'"),
            one("meta::pure::functions::collection::tests::least::testLeast_Empty_Function_1__Boolean_1_", "Too few arguments to function 'least'"),
            one("meta::pure::functions::collection::tests::least::testLeast_Number_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1.0\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Single_Function_1__Boolean_1_", "Too few arguments to function 'least'")
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
