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

package org.finos.legend.engine.plan.execution.stores.relational.test.oracle.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalOraclePCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreStandardFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Oracle_StandardFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
    private static final Adapter adapter = CoreRelationalOraclePCTCodeRepositoryProvider.oracleAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Between
            pack("meta::pure::functions::boolean::tests::inequalities::between", "class java.lang.String cannot be cast to class java.lang.Boolean", AdapterQualifier.needsInvestigation),

            //Xor
            one("meta::pure::functions::boolean::tests::operation::xor::testXor_BinaryExpressions_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::boolean::tests::operation::xor::testXor_BinaryTruthTable_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),

            //And
            one("meta::pure::functions::collection::tests::and::testAnd_Function_1__Boolean_1_", "Can't find the packageable element 'andtrue'", AdapterQualifier.needsInvestigation),

            //Greatest
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Boolean_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_DateTime_Function_1__Boolean_1_", "\"\nexpected: %2025-02-10T20:10:20+0000\nactual:   %2025-02-10T20:10:20.000000000+0000\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Number_Function_1__Boolean_1_", "\"\nexpected: 2\nactual:   2.0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Single_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1D\"", AdapterQualifier.needsInvestigation),

            //In
            one("meta::pure::functions::collection::tests::in::testInIsEmpty_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "\"Parameter to IN operation isn't a literal!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::in::testInPrimitive_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00932: inconsistent datatypes: expected NUMBER got DATE\n\nhttps://docs.oracle.com/error-help/db/ora-00932/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::in::testInForDecimal_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap'", AdapterQualifier.needsInvestigation),

            //Least
            one("meta::pure::functions::collection::tests::least::testLeast_Boolean_Function_1__Boolean_1_", "class java.lang.String cannot be cast to class java.lang.Boolean (java.lang.String and java.lang.Boolean are in module java.base of loader 'bootstrap')", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::least::testLeast_DateTime_Function_1__Boolean_1_", "\"\nexpected: %2025-01-10T15:25:30+0000\nactual:   %2025-01-10T15:25:30.000000000+0000\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::least::testLeast_Number_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1.0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::least::testLeast_Single_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1D\"", AdapterQualifier.needsInvestigation),

            //Max
            one("meta::pure::functions::collection::tests::max::testMax_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),

            //Min
            one("meta::pure::functions::collection::tests::min::testMin_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\"", AdapterQualifier.needsInvestigation),

            //Or
            one("meta::pure::functions::collection::tests::or::testOr_Function_1__Boolean_1_", "Can't find the packageable element 'ortrue'", AdapterQualifier.needsInvestigation),

            //Time bucket
            pack("meta::pure::functions::date::tests::timeBucket::dateTime", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketDays_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketMonths_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketWeeks_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketYears_Function_1__Boolean_1_", "\"[unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            //Hash code
            one("meta::pure::functions::hashCode::tests::testHashCode_Function_1__Boolean_1_", "\"[unsupported-api] The function 'hashCode' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::math::hashCode::tests::testHashCodeAggregate_Function_1__Boolean_1_", "\"[unsupported-api] The function 'hashAgg' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            // Average
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\"", AdapterQualifier.needsInvestigation),

            // Bitwise
            pack("meta::pure::functions::math::tests::bitAnd", "\"[unsupported-api] The function 'bitAnd' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            pack("meta::pure::functions::math::tests::bitNot",  "\"[unsupported-api] The function 'bitNot' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            pack("meta::pure::functions::math::tests::bitOr", "\"[unsupported-api] The function 'bitOr' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            pack("meta::pure::functions::math::tests::bitXor", "\"[unsupported-api] The function 'bitXor' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"[unsupported-api] The function 'bitShiftLeft' (state: [Select, false]) is not supported yet\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_UpTo62Bits_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitShiftLeft' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::math::tests::bitShiftRight::testBitShiftRight_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"[unsupported-api] The function 'bitShiftRight' (state: [Select, false]) is not supported yet\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::math::tests::bitShiftRight::testBitShiftRight_UpTo62Bits_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitShiftRight' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            // Corr
            one("meta::pure::functions::math::tests::corr::testCorr_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"CORR(%s, %s)\"\"", AdapterQualifier.needsInvestigation),

            // Covar
            one("meta::pure::functions::math::tests::covarPopulation::testCovarPopulation_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"COVAR_POP(%s, %s)\"\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::covarSample::testCovarSample_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"COVAR_SAMP(%s, %s)\"\"", AdapterQualifier.needsInvestigation),

            //Max
            one("meta::pure::functions::math::tests::max::testMax_Floats_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_max' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_max' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "\"\nexpected: 2\nactual:   2.0\"", AdapterQualifier.needsInvestigation),

            //MaxBy
            one("meta::pure::functions::math::tests::maxBy::testMaxBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'maxBy' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::math::tests::maxBy::testSimpleGroupByMaxBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'maxBy' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            // Median
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\"", AdapterQualifier.needsInvestigation),

            // Min
            one("meta::pure::functions::math::tests::min::testMin_Floats_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_min' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'array_min' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.23D\nactual:   1.23\"", AdapterQualifier.needsInvestigation),

            // MinBy
            one("meta::pure::functions::math::tests::minBy::testMinBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'minBy' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::math::tests::minBy::testSimpleGroupByMinBy_Function_1__Boolean_1_", "\"[unsupported-api] The function 'minBy' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            // Mode
            pack("meta::pure::functions::math::tests::mode", "java.sql.SQLSyntaxErrorException: ORA-00936: missing expression\n\nhttps://docs.oracle.com/error-help/db/ora-00936/", AdapterQualifier.needsInvestigation),

            // Average
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\"", AdapterQualifier.needsInvestigation),

            // Percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: Oracle\"", AdapterQualifier.needsImplementation),

            // StD Dev
            one("meta::pure::functions::math::tests::stdDev::testFloatStdDev_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00936: missing expression\n\nhttps://docs.oracle.com/error-help/db/ora-00936/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::stdDev::testIntStdDev_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00936: missing expression\n\nhttps://docs.oracle.com/error-help/db/ora-00936/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::stdDev::testMixedStdDev_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00936: missing expression\n\nhttps://docs.oracle.com/error-help/db/ora-00936/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::stdDev::testNegativeNumberStdDev_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00936: missing expression\n\nhttps://docs.oracle.com/error-help/db/ora-00936/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::stdDev::testPopulationStandardDeviation_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00936: missing expression\n\nhttps://docs.oracle.com/error-help/db/ora-00936/", AdapterQualifier.needsInvestigation),

            // Cosh
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_EvalFuncSig_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"cosh(%s)\"\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_Identities_Function_1__Boolean_1_", "For input string: \"13440585709080677242063127757900067936760000\"", AdapterQualifier.needsInvestigation),

            // Sinh
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_EvalFuncSig_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"sinh(%s)\"\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_Identities_Function_1__Boolean_1_", "For input string: \"-13440585709080677242063127757900067936720000\"", AdapterQualifier.needsInvestigation),

            // Tanh
            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_EvalFuncSig_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"tanh(%s)\"\"", AdapterQualifier.needsInvestigation),

            // Variance
            one("meta::pure::functions::math::tests::variance::testVariancePopulation_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00936: missing expression\n\nhttps://docs.oracle.com/error-help/db/ora-00936/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::variance::testVarianceSample_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00936: missing expression\n\nhttps://docs.oracle.com/error-help/db/ora-00936/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::variance::testVariance_Population_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00936: missing expression\n\nhttps://docs.oracle.com/error-help/db/ora-00936/", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::variance::testVariance_Sample_Function_1__Boolean_1_", "java.sql.SQLSyntaxErrorException: ORA-00936: missing expression\n\nhttps://docs.oracle.com/error-help/db/ora-00936/", AdapterQualifier.needsInvestigation),

            //Guid
            one("meta::pure::functions::string::generation::tests::generateGuid::testGenerateGuidWithRelation_Function_1__Boolean_1_", "\"[unsupported-api] The function 'generateGuid' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),
            one("meta::pure::functions::string::generation::tests::generateGuid::testGenerateGuid_Function_1__Boolean_1_", "\"[unsupported-api] The function 'generateGuid' (state: [Select, false]) is not supported yet\"", AdapterQualifier.needsImplementation),

            //Date
            one("meta::pure::functions::tests::date::testDayOfWeek_Relation_Function_1__Boolean_1_", "Error while executing: insert into leSchema.tb_", AdapterQualifier.needsInvestigation)
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Oracle).getFirst())
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
