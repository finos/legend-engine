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

package org.finos.legend.engine.plan.execution.stores.relational.test.duckdb.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalDuckDBPCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreStandardFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.AdapterQualifier;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_DuckDB_StandardFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
    private static final Adapter adapter = CoreRelationalDuckDBPCTCodeRepositoryProvider.duckDBAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // In
            one("meta::pure::functions::collection::tests::in::testInIsEmpty_Function_1__Boolean_1_", "NullPointer exception", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "\"Parameter to IN operation isn't a literal!\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::collection::tests::in::testInPrimitive_Function_1__Boolean_1_", "java.sql.SQLException: Conversion Error: Unimplemented type for cast (INTEGER -> DATE)\n\nLINE 1: select 1 in (1, 2, 5, 2, 'a', true, DATE '2014-02-01', 'c')\n               ^", AdapterQualifier.needsInvestigation),

            // Covariance/Correlation
            one("meta::pure::functions::math::tests::corr::testCorr_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"CORR(%s, %s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::covarPopulation::testCovarPopulation_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"COVAR_POP(%s, %s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::covarSample::testCovarSample_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"COVAR_SAMP(%s, %s)\"\"", AdapterQualifier.unsupportedFeature),

            // Numeric
            one("meta::pure::functions::math::tests::toDegrees::testToDegrees_Function_1__Boolean_1_", "java.sql.SQLException: Out of Range Error: Overflow in multiplication of DECIMAL(18) (10 * 565486677646162740). You might want to add an explicit cast to a bigger decimal.", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::math::tests::toRadians::testToRadians_Function_1__Boolean_1_", "java.sql.SQLException: Out of Range Error: Overflow in multiplication of DECIMAL(18) (10 * 565486677646162740). You might want to add an explicit cast to a bigger decimal.", AdapterQualifier.needsInvestigation),

            // Max
            one("meta::pure::functions::math::tests::max::testMax_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"max(%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"max(%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "\"\nexpected: 2\nactual:   2.0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::max::testMax_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'", AdapterQualifier.unsupportedFeature),

            // MaxBy
            one("meta::pure::functions::math::tests::maxBy::testMaxBy_Function_1__Boolean_1_", "ava.sql.SQLException: Binder Error: No function matches the given name and argument types 'max_by(INTEGER, INTEGER, INTEGER, INTEGER)'", AdapterQualifier.unsupportedFeature),

            // Min
            one("meta::pure::functions::math::tests::min::testMin_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"min(%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"min(%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.23D\nactual:   1.23\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::min::testMin_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'", AdapterQualifier.unsupportedFeature),

            // MinBy
            one("meta::pure::functions::math::tests::minBy::testMinBy_Function_1__Boolean_1_", "java.sql.SQLException: Binder Error: No function matches the given name and argument types 'min_by(INTEGER, INTEGER, INTEGER, INTEGER)'", AdapterQualifier.unsupportedFeature),

            // Median
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Relation_Window_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,grp,name,newCol\n   10.1,0,J,10.1\n   2.2,1,B,6.6\n   6.6,1,F,6.6\n   8.8,1,H,6.6\n   1.1,2,A,3.3\n   5.5,2,E,3.3\n   3.3,3,C,5.5\n   7.7,3,G,5.5\n   4.4,4,D,4.4\n   9.9,5,I,9.9\n#'\nactual:   '#TDS\n   id,grp,name,newCol\n   10.1,0,J,10.1\n   2.2,1,B,6.6\n   6.6,1,F,6.6\n   8.8,1,H,6.6\n   1.1,2,A,3.3000002\n   5.5,2,E,3.3000002\n   3.3,3,C,5.5\n   7.7,3,G,5.5\n   4.4,4,D,4.4\n   9.9,5,I,9.9\n#'\"", AdapterQualifier.needsInvestigation),

            // Mode
            one("meta::pure::functions::math::tests::mode::testMode_Float_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"mode(%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"mode(%s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"mode(%s)\"\"", AdapterQualifier.unsupportedFeature),

            // Average
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\"", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\"", AdapterQualifier.unsupportedFeature),

            // Percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'", AdapterQualifier.unsupportedFeature),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Window_Function_1__Boolean_1_", "java.sql.SQLException: Catalog Error: Aggregate Function with name percentile_cont does not exist!", AdapterQualifier.needsInvestigation),

            // CosH
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_EvalFuncSig_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"cosh(%s)\"\"", AdapterQualifier.needsInvestigation),

            // SinH
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_EvalFuncSig_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"sinh(%s)\"\"", AdapterQualifier.needsInvestigation),

            // TanH
            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_EvalFuncSig_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"tanh(%s)\"\"", AdapterQualifier.needsInvestigation),

            // Bitwise
            one("meta::pure::functions::math::tests::bitShiftRight::testBitShiftRight_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"No error was thrown\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\"", AdapterQualifier.assertErrorMismatch),
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"Unexpected error executing function with params [Anonymous_Lambda]\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\"", AdapterQualifier.assertErrorMismatch),

            // Hash
            one("meta::pure::functions::hashCode::tests::testHashCode_Function_1__Boolean_1_", "\"Unused format args. [3] arguments provided to expression \"hash(%s)::BITSTRING::BIGINT\"\""),
            one("meta::pure::functions::math::hashCode::tests::testHashCodeAggregate_Function_1__Boolean_1_", "\"[unsupported-api] The function 'hashAgg' (state: [Select, false]) is not supported yet\""),

            // Hash
            one("meta::pure::functions::math::hashCode::tests::testHashCodeAggregate_Function_1__Boolean_1_", "\"[unsupported-api] The function 'hashAgg' (state: [Select, false]) is not supported yet\"", AdapterQualifier.unsupportedFeature),

            // And
            one("meta::pure::functions::collection::tests::and::testAnd_Function_1__Boolean_1_", "Can't find the packageable element 'andtrue'", AdapterQualifier.needsInvestigation),

            // Or
            one("meta::pure::functions::collection::tests::or::testOr_Function_1__Boolean_1_", "Can't find the packageable element 'ortrue'", AdapterQualifier.needsInvestigation),

            // Greatest
            one("meta::pure::functions::collection::tests::greatest::testGreatest_DateTime_Function_1__Boolean_1_", "\"\nexpected: %2025-02-10T20:10:20+0000\nactual:   %2025-02-10T20:10:20.000000000+0000\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Number_Function_1__Boolean_1_", "\"\nexpected: 2\nactual:   2.0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Single_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1D\"", AdapterQualifier.needsInvestigation),

            // Least
            one("meta::pure::functions::collection::tests::least::testLeast_DateTime_Function_1__Boolean_1_", "\"\nexpected: %2025-01-10T15:25:30+0000\nactual:   %2025-01-10T15:25:30.000000000+0000\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::least::testLeast_Number_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1.0\"", AdapterQualifier.needsInvestigation),
            one("meta::pure::functions::collection::tests::least::testLeast_Single_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1D\"", AdapterQualifier.needsInvestigation)
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource)TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.DuckDB).getFirst())
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
