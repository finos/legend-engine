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

package org.finos.legend.engine.plan.execution.stores.relational.test.snowflake.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalSnowflakePCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreStandardFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Snowflake_StandardFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
    private static final Adapter adapter = CoreRelationalSnowflakePCTCodeRepositoryProvider.snowflakeAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // StD Dev
            one("meta::pure::functions::math::tests::stdDev::testFloatStdDev_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 12\nInvalid argument types for function '*': (ARRAY, ARRAY)", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::stdDev::testIntStdDev_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 12\nInvalid argument types for function '*': (ARRAY, ARRAY)", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::stdDev::testMixedStdDev_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 12\nInvalid argument types for function '*': (ARRAY, ARRAY)", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::stdDev::testNegativeNumberStdDev_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 12\nInvalid argument types for function '*': (ARRAY, ARRAY)", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::stdDev::testPopulationStandardDeviation_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 12\nInvalid argument types for function '*': (ARRAY, ARRAY)", AdapterQualifier.needsInvestigation),

            // Covariance/Correlation
            one("meta::pure::functions::math::tests::corr::testCorr_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"CORR(%s, %s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::covarPopulation::testCovarPopulation_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"COVAR_POP(%s, %s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::covarSample::testCovarSample_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"COVAR_SAMP(%s, %s)\"\"", AdapterQualifier.unsupportedFeature),

            // Variance
            one("meta::pure::functions::math::tests::variance::testVariancePopulation_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 7\nInvalid argument types for function '*': (ARRAY, ARRAY)", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::variance::testVarianceSample_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 7\nInvalid argument types for function '*': (ARRAY, ARRAY)", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::variance::testVariance_Population_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 7\nInvalid argument types for function '*': (ARRAY, ARRAY)", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::variance::testVariance_Sample_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 7\nInvalid argument types for function '*': (ARRAY, ARRAY)", AdapterQualifier.unsupportedFeature),

            // In
            one("meta::pure::functions::collection::tests::in::testInIsEmpty_Function_1__Boolean_1_", "NullPointer exception", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "\"Parameter to IN operation isn't a literal!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::in::testInPrimitive_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error:\nCan not convert parameter 'CAST('2014-02-01' AS DATE)' of type [DATE] into expected type [BOOLEAN]", AdapterQualifier.needsInvestigation),

            // Max
            one("meta::pure::functions::math::tests::max::testMax_Floats_Function_1__Boolean_1_", "Cast exception: String cannot be cast to Number", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Function_1__Boolean_1_", "Cast exception: String cannot be cast to Number", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "\"\nexpected: 2\nactual:   2.0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::max::testMax_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'tail_T_MANY__T_MANY_'", AdapterQualifier.needsImplementation),

            // MaxBy
            one("meta::pure::functions::math::tests::maxBy::testMaxBy_Function_1__Boolean_1_", "SQL compilation error: error line 1 at position 7\ntoo many arguments for function [MAX_BY(1, 2, 10, 20)] expected 3, got 4"),

            // Min
            one("meta::pure::functions::math::tests::min::testMin_Floats_Function_1__Boolean_1_", "Cast exception: String cannot be cast to Number", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Function_1__Boolean_1_", "Cast exception: String cannot be cast to Number", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.23D\nactual:   1.23\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::min::testMin_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'tail_T_MANY__T_MANY_'", AdapterQualifier.needsInvestigation),

            // MinBy
            one("meta::pure::functions::math::tests::minBy::testMinBy_Function_1__Boolean_1_", "SQL compilation error: error line 1 at position 7\ntoo many arguments for function [MIN_BY(1, 2, 10, 20)] expected 3, got 4"),

            // Median
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\"", AdapterQualifier.needsImplementation),

            // Mode
            one("meta::pure::functions::math::tests::mode::testMode_Float_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"mode(%s)\"\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"mode(%s)\"\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"mode(%s)\"\"", AdapterQualifier.needsImplementation),

            // And
            one("meta::pure::functions::collection::tests::and::testAnd_Function_1__Boolean_1_", "Can't find the packageable element 'andtrue'", AdapterQualifier.needsInvestigation),

            // Or
            one("meta::pure::functions::collection::tests::or::testOr_Function_1__Boolean_1_", "Can't find the packageable element 'ortrue'", AdapterQualifier.needsInvestigation),

            // Average
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\"", AdapterQualifier.needsImplementation),

            // Percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Window_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,val,newCol\n   1,1.0,1.8\n   1,2.0,1.8\n   1,3.0,1.8\n   2,1.5,2.3\n   2,2.5,2.3\n   2,3.5,2.3\n   3,1.0,1.4\n   3,1.5,1.4\n   3,2.0,1.4\n#'\nactual:   '#TDS\n   id,val,newCol\n   1,1.0,1.7999999999999998\n   1,2.0,1.7999999999999998\n   1,3.0,1.7999999999999998\n   2,1.5,2.3\n   2,2.5,2.3\n   2,3.5,2.3\n   3,1.0,1.4\n   3,1.5,1.4\n   3,2.0,1.4\n#'\"", AdapterQualifier.needsInvestigation),

            // Sum
            one("meta::pure::functions::math::tests::sum::testSum_Numbers_Function_1__Boolean_1_", "\"\nexpected: 32.0\nactual:   32\"", AdapterQualifier.needsInvestigation),

            // CosH
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_EvalFuncSig_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"cosh(%s)\"\"", AdapterQualifier.unsupportedFeature),

            // SinH
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_EvalFuncSig_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"sinh(%s)\"\"", AdapterQualifier.unsupportedFeature),

            // TanH
            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_EvalFuncSig_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"tanh(%s)\"\"", AdapterQualifier.unsupportedFeature),

            // Greatest
            one("meta::pure::functions::collection::tests::greatest::testGreatest_DateTime_Function_1__Boolean_1_", "\"\nexpected: %2025-02-10T20:10:20+0000\nactual:   %2025-02-10T20:10:20.000000000+0000\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Number_Function_1__Boolean_1_", "\"\nexpected: 2\nactual:   2.0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Single_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1D\"", AdapterQualifier.needsInvestigation),

            // Least
            one("meta::pure::functions::collection::tests::least::testLeast_DateTime_Function_1__Boolean_1_", "\"\nexpected: %2025-01-10T15:25:30+0000\nactual:   %2025-01-10T15:25:30.000000000+0000\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::least::testLeast_Number_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1.0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::least::testLeast_Single_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1D\"", AdapterQualifier.needsInvestigation),

            // Bitwise
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"Unexpected error executing function with params [Anonymous_Lambda]\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\"", AdapterQualifier.assertErrorMismatch),
            one("meta::pure::functions::math::tests::bitShiftRight::testBitShiftRight_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"No error was thrown\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\"", AdapterQualifier.assertErrorMismatch)
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Snowflake).getFirst())
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
