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

package org.finos.legend.engine.plan.execution.stores.relational.test.clickhouse.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalClickHousePCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreStandardFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_ClickHouse_StandardFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
    private static final Adapter adapter = CoreRelationalClickHousePCTCodeRepositoryProvider.clickhouseAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // Between
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_DateTime_Function_1__Boolean_1_", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_NumberFloat_Function_1__Boolean_1_", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_NumberInteger_Function_1__Boolean_1_", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_NumberLong_Function_1__Boolean_1_", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_StrictDate_Function_1__Boolean_1_", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::boolean::tests::inequalities::between::testBetween_String_Function_1__Boolean_1_", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')"),

            // Xor
            one("meta::pure::functions::boolean::tests::operation::xor::testXor_BinaryExpressions_Function_1__Boolean_1_", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')"),

            // And
            one("meta::pure::functions::collection::tests::and::testAnd_Function_1__Boolean_1_", "Can't find the packageable element 'andtrue'"),

            // Greatest
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Boolean_Function_1__Boolean_1_", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_DateTime_Function_1__Boolean_1_", "\"\nexpected: %2025-02-10T20:10:20+0000\nactual:   %2025-02-10T20:10:20.000000000+0000\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Number_Function_1__Boolean_1_", "\"\nexpected: 2\nactual:   2.0\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Single_Function_1__Boolean_1_", "\"\nexpected: %2025-02-10T20:10:20+0000\nactual:   %2025-02-10T20:10:20.000000000+0000\""),

            // In
            one("meta::pure::functions::collection::tests::in::testInForDecimal_Function_1__Boolean_1_", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::collection::tests::in::testInIsEmpty_Function_1__Boolean_1_", "NullPointer exception"),
            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "\"Parameter to IN operation isn't a literal!\""),
            one("meta::pure::functions::collection::tests::in::testInPrimitive_Function_1__Boolean_1_", "java.sql.SQLException: Code: 53. DB::Exception: Cannot convert string 'a' to type UInt8. (TYPE_MISMATCH) (version 25.1.1.4165 (official build)) "),

            // Least
            one("meta::pure::functions::collection::tests::least::testLeast_Boolean_Function_1__Boolean_1_", "class java.lang.Long cannot be cast to class java.lang.Boolean (java.lang.Long and java.lang.Boolean are in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::collection::tests::least::testLeast_DateTime_Function_1__Boolean_1_", "\"\nexpected: %2025-01-10T15:25:30+0000\nactual:   %2025-01-10T15:25:30.000000000+0000\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Number_Function_1__Boolean_1_", "There is no supertype for types Float64, Float64, Decimal(32, 16), UInt8, UInt8 because some of them have no lossless conversion to Decimal: In scope SELECT least(4.23, 7.345, CAST(1., 'Decimal(32, 16)'), 3, 4)"),
            one("meta::pure::functions::collection::tests::least::testLeast_Single_Function_1__Boolean_1_", "\"\nexpected: %2025-02-10T20:10:20+0000\nactual:   %2025-02-10T20:10:20.000000000+0000\""),

            // Max
            one("meta::pure::functions::collection::tests::max::testMax_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),

            // Min
            one("meta::pure::functions::collection::tests::min::testMin_Function_1__Boolean_1_", "\"Cannot cast a collection of size 0 to multiplicity [1]\""),

            // Or
            one("meta::pure::functions::collection::tests::or::testOr_Function_1__Boolean_1_", "Can't find the packageable element 'ortrue'"),

            // TimeBucket
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketDays_Function_1__Boolean_1_", "\"\nexpected: %1951-10-20T00:00:00.000000000+0000\nactual:   %2087-11-24T06:28:16.000000000+0000\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketHours_Function_1__Boolean_1_", "\"\nexpected: %2024-01-30T22:00:00.000000000+0000\nactual:   %2024-01-31T00:00:00.000000000+0000\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketMinutes_Function_1__Boolean_1_", "\"\nexpected: %1951-10-21T08:57:00.000000000+0000\nactual:   %2087-11-26T15:25:16.000000000+0000\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketMonths_Function_1__Boolean_1_", "\"\nexpected: %2024-01-01T00:00:00.000000000+0000\nactual:   %2023-01-01T00:00:00.000000000+0000\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketSeconds_Function_1__Boolean_1_", "\"\nexpected: %1951-10-21T08:59:00.000000000+0000\nactual:   %2087-11-26T15:27:15.000000000+0000\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketWeeks_Function_1__Boolean_1_", "\"\nexpected: %2024-01-29T00:00:00.000000000+0000\nactual:   %2024-01-22T00:00:00.000000000+0000\""),
            one("meta::pure::functions::date::tests::timeBucket::dateTime::testTimeBucketYears_Function_1__Boolean_1_", "\"\nexpected: %2024-01-01T00:00:00.000000000+0000\nactual:   %2016-01-01T00:00:00.000000000+0000\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketDays_Function_1__Boolean_1_", "\"\nexpected: %1951-10-20\nactual:   %1970-01-01\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketMonths_Function_1__Boolean_1_", "\"\nexpected: %2024-01-01\nactual:   %2023-01-01\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketWeeks_Function_1__Boolean_1_", "\"\nexpected: %2024-01-29\nactual:   %2024-01-22\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketYears_Function_1__Boolean_1_", "\"\nexpected: %2024-01-01\nactual:   %2016-01-01\""),

            // HashCode
            one("meta::pure::functions::hashCode::tests::testHashCode_Function_1__Boolean_1_", "\"[unsupported-api] The function 'hashCode' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::hashCode::tests::testHashCodeAggregate_Function_1__Boolean_1_", "\"[unsupported-api] The function 'hashAgg' (state: [Select, false]) is not supported yet\""),

            // Average
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\""),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\""),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"avg(1.0 * %s)\"\""),

            // BitAnd
            one("meta::pure::functions::math::tests::bitAnd::testBitAnd_LargeNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitAnd' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitAnd::testBitAnd_NegativeNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitAnd' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitAnd::testBitAnd_SmallNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitAnd' (state: [Select, false]) is not supported yet\""),

            // BitNot
            one("meta::pure::functions::math::tests::bitNot::testBitNot_LargeNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitNot' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitNot::testBitNot_NegativeNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitNot' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitNot::testBitNot_SmallNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitNot' (state: [Select, false]) is not supported yet\""),

            // BitOr
            one("meta::pure::functions::math::tests::bitOr::testBitOr_LargeNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitOr' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitOr::testBitOr_NegativeNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitOr' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitOr::testBitOr_SmallNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitOr' (state: [Select, false]) is not supported yet\""),

            // BitShiftLeft
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"[unsupported-api] The function 'bitShiftLeft' (state: [Select, false]) is not supported yet\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\""),
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_UpTo62Bits_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitShiftLeft' (state: [Select, false]) is not supported yet\""),

            // BitShiftRight
            one("meta::pure::functions::math::tests::bitShiftRight::testBitShiftRight_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"[unsupported-api] The function 'bitShiftRight' (state: [Select, false]) is not supported yet\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\""),
            one("meta::pure::functions::math::tests::bitShiftRight::testBitShiftRight_UpTo62Bits_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitShiftRight' (state: [Select, false]) is not supported yet\""),

            // BitXor
            one("meta::pure::functions::math::tests::bitXor::testBitXor_LargeNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitXor' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitXor::testBitXor_NegativeNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitXor' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::bitXor::testBitXor_SmallNumbers_Function_1__Boolean_1_", "\"[unsupported-api] The function 'bitXor' (state: [Select, false]) is not supported yet\""),

            // Corr
            one("meta::pure::functions::math::tests::corr::testCorr_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"CORR(%s, %s)\"\""),

            // CovarPopulation
            one("meta::pure::functions::math::tests::covarPopulation::testCovarPopulation_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"COVAR_POP(%s, %s)\"\""),

            // CovarSample
            one("meta::pure::functions::math::tests::covarSample::testCovarSample_Function_1__Boolean_1_", "\"Unused format args. [4] arguments provided to expression \"COVAR_SAMP(%s, %s)\"\""),

            // Max
            one("meta::pure::functions::math::tests::max::testMax_Floats_Function_1__Boolean_1_", "\"\nexpected: []\nactual:   [0.0]\""),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Function_1__Boolean_1_", "\"\nexpected: []\nactual:   [0]\""),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "\"\nexpected: 2\nactual:   2.0\""),

            // MaxBy
            one("meta::pure::functions::math::tests::maxBy::testMaxBy_Function_1__Boolean_1_", "java.sql.SQLException: Code: 42. DB::Exception: Aggregate function argMax requires two arguments."),

            // Median
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"median(%s)\"\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Relation_Window_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,grp,name,newCol\n   10.1,0,J,10.1\n   2.2,1,B,6.6\n   6.6,1,F,6.6\n   8.8,1,H,6.6\n   1.1,2,A,3.3\n   5.5,2,E,3.3\n   3.3,3,C,5.5\n   7.7,3,G,5.5\n   4.4,4,D,4.4\n   9.9,5,I,9.9\n#'\nactual:   '#TDS\n   id,grp,name,newCol\n   10.1,0,J,10.100000381469727\n   2.2,1,B,6.599999904632568\n   6.6,1,F,6.599999904632568\n   8.8,1,H,6.599999904632568\n   1.1,2,A,3.300000011920929\n   5.5,2,E,3.300000011920929\n   3.3,3,C,5.4999998807907104\n   7.7,3,G,5.4999998807907104\n   4.4,4,D,4.400000095367432\n   9.9,5,I,9.899999618530273\n#'\""),

            // Min
            one("meta::pure::functions::math::tests::min::testMin_Floats_Function_1__Boolean_1_", "\"\nexpected: []\nactual:   [0.0]\""),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Function_1__Boolean_1_", "\"\nexpected: []\nactual:   [0]\""),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.23D\nactual:   1.23\""),

            // MinBy
            one("meta::pure::functions::math::tests::minBy::testMinBy_Function_1__Boolean_1_", "java.sql.SQLException: Code: 42. DB::Exception: Aggregate function argMin requires two arguments."),

            // Mode
            one("meta::pure::functions::math::tests::mode::testMode_Float_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"topK(1)(%s)[1]\"\""),
            one("meta::pure::functions::math::tests::mode::testMode_Floats_Relation_Window_Function_1__Boolean_1_", "Expected one of: token, Comma, FROM, PREWHERE, WHERE, GROUP BY, WITH, HAVING, WINDOW, QUALIFY, ORDER BY, LIMIT, OFFSET, FETCH, SETTINGS, UNION, EXCEPT, INTERSECT, INTO OUTFILE, FORMAT, ParallelWithClause, PARALLEL WITH, end of query."),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"topK(1)(%s)[1]\"\""),
            one("meta::pure::functions::math::tests::mode::testMode_Integer_Relation_Window_Function_1__Boolean_1_", "Expected one of: token, Comma, FROM, PREWHERE, WHERE, GROUP BY, WITH, HAVING, WINDOW, QUALIFY, ORDER BY, LIMIT, OFFSET, FETCH, SETTINGS, UNION, EXCEPT, INTERSECT, INTO OUTFILE, FORMAT, ParallelWithClause, PARALLEL WITH, end of query."),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"topK(1)(%s)[1]\"\""),
            one("meta::pure::functions::math::tests::mode::testMode_Number_Relation_Window_Function_1__Boolean_1_", "Expected one of: token, Comma, FROM, PREWHERE, WHERE, GROUP BY, WITH, HAVING, WINDOW, QUALIFY, ORDER BY, LIMIT, OFFSET, FETCH, SETTINGS, UNION, EXCEPT, INTERSECT, INTO OUTFILE, FORMAT, ParallelWithClause, PARALLEL WITH, end of query."),

            // Percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_", "\"[unsupported-api] relational lambda processing not supported for Database Type: ClickHouse\""),
            one("meta::pure::functions::math::tests::percentile::testPercentile_Relation_Window_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   id,val,newCol\n   1,1.0,1.8\n   1,2.0,1.8\n   1,3.0,1.8\n   2,1.5,2.3\n   2,2.5,2.3\n   2,3.5,2.3\n   3,1.0,1.4\n   3,1.5,1.4\n   3,2.0,1.4\n#'\nactual:   '#TDS\n   id,val,newCol\n   1,1.0,2.2\n   1,2.0,2.2\n   1,3.0,2.2\n   2,1.5,2.6999999999999997\n   2,2.5,2.6999999999999997\n   2,3.5,2.6999999999999997\n   3,1.0,1.6\n   3,1.5,1.6\n   3,2.0,1.6\n#'\""),

            // Cosh
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_EvalFuncSig_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"cosh(%s)\"\""),

            // Sinh
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_EvalFuncSig_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"sinh(%s)\"\""),

            // Tanh
            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_EvalFuncSig_Function_1__Boolean_1_", "\"Unused format args. [2] arguments provided to expression \"(2 / (1.0 + exp(-2 * %s)) - 1)\"\""),

            // Wavg
            one("meta::pure::functions::math::tests::wavg::testSimpleGroupByMultipleWavg_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   grp,wavgCol1,wavgCol2\n   1,180.0,220.0\n   2,150.0,175.0\n   3,362.5,325.0\n   4,700.0,700.0\n   5,350.0,350.0\n#'\nactual:   '#TDS\n   grp,wavgCol1,wavgCol2\n   1,180.00000268220901,220.00000029802322\n   2,150.0,175.0\n   3,362.5,325.0\n   4,700.0,700.0\n   5,350.0,350.0\n#'\""),
            one("meta::pure::functions::math::tests::wavg::testSimpleGroupByWavg_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   grp,wavgCol\n   1,180.0\n   2,150.0\n   3,362.5\n   4,700.0\n   5,350.0\n#'\nactual:   '#TDS\n   grp,wavgCol\n   1,180.00000268220901\n   2,150.0\n   3,362.5\n   4,700.0\n   5,350.0\n#'\""),

            // GenerateGuid
            one("meta::pure::functions::string::generation::tests::generateGuid::testGenerateGuidWithRelation_Function_1__Boolean_1_", "Failed to parse input: null"),
            one("meta::pure::functions::string::generation::tests::generateGuid::testGenerateGuid_Function_1__Boolean_1_", "Failed to parse input: null")

            );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.ClickHouse).getFirst())
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
