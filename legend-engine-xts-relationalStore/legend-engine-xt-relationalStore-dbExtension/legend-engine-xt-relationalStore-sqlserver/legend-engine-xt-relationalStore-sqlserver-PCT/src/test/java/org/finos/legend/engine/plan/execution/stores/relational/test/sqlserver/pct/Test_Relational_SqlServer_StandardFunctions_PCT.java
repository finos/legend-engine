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

package org.finos.legend.engine.plan.execution.stores.relational.test.sqlserver.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.pure.runtime.testConnection.CoreExternalTestConnectionCodeRepositoryProvider;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreStandardFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_SqlServer_StandardFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.sqlserverAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // And
            one("meta::pure::functions::collection::tests::and::testAnd_Function_1__Boolean_1_", "Can't find the packageable element 'andtrue'", AdapterQualifier.unsupportedFeature),

            // StD Dev
            one("meta::pure::functions::math::tests::stdDev::testFloatStdDev_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'var_pop' is not a recognized built-in function name."),
            one("meta::pure::functions::math::tests::stdDev::testIntStdDev_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'var_pop' is not a recognized built-in function name."),
            one("meta::pure::functions::math::tests::stdDev::testMixedStdDev_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'var_pop' is not a recognized built-in function name."),
            one("meta::pure::functions::math::tests::stdDev::testNegativeNumberStdDev_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'var_pop' is not a recognized built-in function name."),
            one("meta::pure::functions::math::tests::stdDev::testPopulationStandardDeviation_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'var_pop' is not a recognized built-in function name."),

            // Variance
            one("meta::pure::functions::math::tests::variance::testVariancePopulation_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"varp(%s)\"\""),
            one("meta::pure::functions::math::tests::variance::testVarianceSample_Function_1__Boolean_1_", "\"Unused format args. [3] arguments provided to expression \"var(%s)\"\""),
            one("meta::pure::functions::math::tests::variance::testVariance_Population_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'var_pop' is not a recognized built-in function name."),
            one("meta::pure::functions::math::tests::variance::testVariance_Sample_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'var_pop' is not a recognized built-in function name."),

            // Covariance/Correlation
            one("meta::pure::functions::math::tests::corr::testCorr_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"CORR(%s, %s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::corr::testSimpleWindowCorr_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'CORR' is not a recognized built-in function name."),
            one("meta::pure::functions::math::tests::covarPopulation::testCovarPopulation_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"COVAR_POP(%s, %s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::covarPopulation::testSimpleWindowCovarPopulation_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'COVAR_POP' is not a recognized built-in function name."),
            one("meta::pure::functions::math::tests::covarSample::testCovarSample_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'COVAR_SAMP' is not a recognized built-in function name."),
            one("meta::pure::functions::math::tests::covarSample::testSimpleWindowCovarSample_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: 'COVAR_SAMP' is not a recognized built-in function name."),

            // In
            one("meta::pure::functions::collection::tests::in::testInIsEmpty_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'in'."),
            one("meta::pure::functions::collection::tests::in::testInPrimitive_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'in'."),
            one("meta::pure::functions::collection::tests::in::testIn_relation_extend_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'in'."),
            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "\"Parameter to IN operation isn't a literal!\""),

            one("meta::pure::functions::tests::date::testDayOfWeek_Relation_Function_1__Boolean_1_", "Error while executing: insert into leSchema.tb_"),

            one("meta::pure::functions::collection::tests::or::testOr_Function_1__Boolean_1_", "Can't find the packageable element 'ortrue'", AdapterQualifier.unsupportedFeature),

            // Xor
            one("meta::pure::functions::boolean::tests::operation::xor::testXor_BinaryExpressions_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near '='."),
            one("meta::pure::functions::boolean::tests::operation::xor::testXor_BinaryTruthTable_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'or'."),

            // Max
            one("meta::pure::functions::math::tests::max::testMax_Floats_Function_1__Boolean_1_", "[unsupported-api] The function 'array_max' (state: [Select, false]) is not supported yet", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Function_1__Boolean_1_", "[unsupported-api] The function 'array_max' (state: [Select, false]) is not supported yet", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "\"\nexpected: 2\nactual:   2.0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::max::testMax_Function_1__Boolean_1_", "Cannot cast a collection of size 0 to multiplicity [1]", AdapterQualifier.unsupportedFeature),

            // MaxBy
            one("meta::pure::functions::math::tests::maxBy::testMaxBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'maxBy' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::maxBy::testSimpleGroupByMaxBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'maxBy' (state: [Select, false]) is not supported yet\""),

            // Min
            one("meta::pure::functions::math::tests::min::testMin_Floats_Function_1__Boolean_1_", "[unsupported-api] The function 'array_min' (state: [Select, false]) is not supported yet", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Function_1__Boolean_1_", "[unsupported-api] The function 'array_min' (state: [Select, false]) is not supported yet", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.23D\nactual:   1.23\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::min::testMin_Function_1__Boolean_1_", "Cannot cast a collection of size 0 to multiplicity [1]", AdapterQualifier.unsupportedFeature),

            // MinBy
            one("meta::pure::functions::math::tests::minBy::testMinBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'minBy' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::minBy::testSimpleGroupByMinBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'minBy' (state: [Select, false]) is not supported yet\""),

            // Median
            pack("meta::pure::functions::math::tests::median", "com.microsoft.sqlserver.jdbc.SQLServerException: 'median' is not a recognized built-in function name.", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),

            // Mode
            pack("meta::pure::functions::math::tests::mode", "com.microsoft.sqlserver.jdbc.SQLServerException: 'mode' is not a recognized built-in function name.", AdapterQualifier.needsInvestigation),

            // Percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Aggregate_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: The function 'percentile_disc' must have an OVER clause."),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Window_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,val,newCol\n   1,1.0,1.8\n   1,2.0,1.8\n   1,3.0,1.8\n   2,1.5,2.3\n   2,2.5,2.3\n   2,3.5,2.3\n   3,1.0,1.4\n   3,1.5,1.4\n   3,2.0,1.4\n#'\nactual:   '#TDS\n   id,val,newCol\n   1,1.0,1.7999999999999998\n   1,2.0,1.7999999999999998\n   1,3.0,1.7999999999999998\n   2,1.5,2.3\n   2,2.5,2.3\n   2,3.5,2.3\n   3,1.0,1.4\n   3,1.5,1.4\n   3,2.0,1.4\n#'\""),

            // Average
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\"", AdapterQualifier.needsInvestigation),

            // Percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_", "[unsupported-api] relational lambda processing not supported for Database Type:", AdapterQualifier.unsupportedFeature),

            // CosH
            pack("meta::pure::functions::math::tests::trigonometry::cosh", "com.microsoft.sqlserver.jdbc.SQLServerException: 'cosh' is not a recognized built-in function name.", AdapterQualifier.needsInvestigation),

            // SinH
            pack("meta::pure::functions::math::tests::trigonometry::sinh", "com.microsoft.sqlserver.jdbc.SQLServerException: 'sinh' is not a recognized built-in function name", AdapterQualifier.needsInvestigation),

            // TanH
            pack("meta::pure::functions::math::tests::trigonometry::tanh", "com.microsoft.sqlserver.jdbc.SQLServerException: 'tanh' is not a recognized built-in function name", AdapterQualifier.needsInvestigation),

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

            // Hash
            one("meta::pure::functions::hashCode::tests::testHashCode_Function_1__Boolean_1_", "\"[unsupported-api] The function 'hashCode' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::hashCode::tests::testHashCodeAggregate_Function_1__Boolean_1_", "\"[unsupported-api] The function 'hashAgg' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            // TimeBucket
            pack("meta::pure::functions::date::tests::timeBucket::dateTime", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketDays_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketMonths_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketWeeks_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketYears_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            // Inequalities
            pack("meta::pure::functions::boolean::tests::inequalities::between", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_DateTime_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'is'."),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_NumberFloat_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'is'."),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_NumberInteger_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'is'."),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_NumberLong_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'is'."),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_StrictDate_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'is'."),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_String_Function_1__Boolean_1_", "com.microsoft.sqlserver.jdbc.SQLServerException: Incorrect syntax near the keyword 'is'."),

            // Greatest
            pack("meta::pure::functions::collection::tests::greatest", "com.microsoft.sqlserver.jdbc.SQLServerException: 'greatest' is not a recognized built-in function name."),

            // Least
            pack("meta::pure::functions::collection::tests::least", "com.microsoft.sqlserver.jdbc.SQLServerException: 'least' is not a recognized built-in function name.")

            );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.SqlServer).getFirst())
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
