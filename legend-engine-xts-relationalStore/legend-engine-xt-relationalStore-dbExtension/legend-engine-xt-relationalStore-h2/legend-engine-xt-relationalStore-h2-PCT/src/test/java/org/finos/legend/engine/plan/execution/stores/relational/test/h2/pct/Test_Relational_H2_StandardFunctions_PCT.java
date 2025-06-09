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

package org.finos.legend.engine.plan.execution.stores.relational.test.h2.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalH2PCTCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreStandardFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.*;

public class Test_Relational_H2_StandardFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
    private static final Adapter adapter = CoreRelationalH2PCTCodeRepositoryProvider.H2Adapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // StD Dev
            one("meta::pure::functions::math::tests::stdDev::testFloatStdDev_Function_1__Boolean_1_", "\"No matched function in function registry for - var_pop(DoubleSqlType, DoubleSqlType, DoubleSqlType, BooleanSqlType).\nAvailable variations for that function - [\n  var_pop(DoubleSqlType):DoubleSqlType,\n  var_pop(AbstractNumericSqlType):AbstractNumericSqlType\n]\""),
            one("meta::pure::functions::math::tests::stdDev::testIntStdDev_Function_1__Boolean_1_", "\"No matched function in function registry for - var_pop(IntegerSqlType, IntegerSqlType, IntegerSqlType, BooleanSqlType).\nAvailable variations for that function - [\n  var_pop(DoubleSqlType):DoubleSqlType,\n  var_pop(AbstractNumericSqlType):AbstractNumericSqlType\n]\""),
            one("meta::pure::functions::math::tests::stdDev::testMixedStdDev_Function_1__Boolean_1_", "\"No matched function in function registry for - var_pop(DoubleSqlType, IntegerSqlType, DoubleSqlType, BooleanSqlType).\nAvailable variations for that function - [\n  var_pop(DoubleSqlType):DoubleSqlType,\n  var_pop(AbstractNumericSqlType):AbstractNumericSqlType\n]\""),
            one("meta::pure::functions::math::tests::stdDev::testNegativeNumberStdDev_Function_1__Boolean_1_", "\"No matched function in function registry for - var_pop(IntegerSqlType, IntegerSqlType, IntegerSqlType, BooleanSqlType).\nAvailable variations for that function - [\n  var_pop(DoubleSqlType):DoubleSqlType,\n  var_pop(AbstractNumericSqlType):AbstractNumericSqlType\n]\""),
            one("meta::pure::functions::math::tests::stdDev::testPopulationStandardDeviation_Function_1__Boolean_1_", "\"No matched function in function registry for - var_pop(IntegerSqlType, IntegerSqlType, BooleanSqlType).\nAvailable variations for that function - [\n  var_pop(DoubleSqlType):DoubleSqlType,\n  var_pop(AbstractNumericSqlType):AbstractNumericSqlType\n]\""),

            // Variance
            one("meta::pure::functions::math::tests::variance::testVarianceSample_Function_1__Boolean_1_", "No matched function in function registry for - var_samp(DoubleSqlType, DoubleSqlType, DoubleSqlType)."),
            one("meta::pure::functions::math::tests::variance::testVariancePopulation_Function_1__Boolean_1_", "\"No matched function in function registry for - var_pop(IntegerSqlType, IntegerSqlType)."),
            one("meta::pure::functions::math::tests::variance::testVariance_Population_Function_1__Boolean_1_", "\"No matched function in function registry for - var_pop(IntegerSqlType, IntegerSqlType, BooleanSqlType).\nAvailable variations for that function - [\n  var_pop(DoubleSqlType):DoubleSqlType,\n  var_pop(AbstractNumericSqlType):AbstractNumericSqlType\n]\""),
            one("meta::pure::functions::math::tests::variance::testVariance_Sample_Function_1__Boolean_1_", "\"No matched function in function registry for - var_pop(DoubleSqlType, DoubleSqlType, DoubleSqlType, BooleanSqlType).\nAvailable variations for that function - [\n  var_pop(DoubleSqlType):DoubleSqlType,\n  var_pop(AbstractNumericSqlType):AbstractNumericSqlType\n]\""),

            // Covariance/Correlation
            one("meta::pure::functions::math::tests::corr::testCorr_Function_1__Boolean_1_", "\"No matched function in function registry for - corr(IntegerSqlType, IntegerSqlType, IntegerSqlType, IntegerSqlType).\nAvailable variations for that function - [\n  corr(DoubleSqlType, DoubleSqlType):DoubleSqlType,\n  corr(AbstractNumericSqlType, AbstractNumericSqlType):AbstractNumericSqlType\n]\""),
            one("meta::pure::functions::math::tests::covarPopulation::testCovarPopulation_Function_1__Boolean_1_", "\"No matched function in function registry for - covar_pop(IntegerSqlType, IntegerSqlType, IntegerSqlType, IntegerSqlType).\nAvailable variations for that function - [\n  covar_pop(DoubleSqlType, DoubleSqlType):DoubleSqlType,\n  covar_pop(AbstractNumericSqlType, AbstractNumericSqlType):AbstractNumericSqlType\n]\""),
            one("meta::pure::functions::math::tests::covarSample::testCovarSample_Function_1__Boolean_1_", "\"No matched function in function registry for - covar_samp(IntegerSqlType, IntegerSqlType, IntegerSqlType, IntegerSqlType).\nAvailable variations for that function - [\n  covar_samp(DoubleSqlType, DoubleSqlType):DoubleSqlType,\n  covar_samp(AbstractNumericSqlType, AbstractNumericSqlType):AbstractNumericSqlType\n]\""),

            // In
            one("meta::pure::functions::collection::tests::in::testInIsEmpty_Function_1__Boolean_1_", "NullPointer exception"),
            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "\"Parameter to IN operation isn't a literal!\""),
            one("meta::pure::functions::collection::tests::in::testInPrimitive_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLSyntaxErrorException: Values of types \"INTEGER\" and \"DATE\" are not comparable; SQL statement:\nselect 1 in (1, 2, 5, 2, 'a', true, DATE'2014-02-01', 'c') [90110-214]"),

            // Max
            one("meta::pure::functions::math::tests::max::testMax_Floats_Function_1__Boolean_1_", "No matched function in function registry for - max(DoubleSqlType, DoubleSqlType, DoubleSqlType, DoubleSqlType, DoubleSqlType)."),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Function_1__Boolean_1_", "No matched function in function registry for - max(IntegerSqlType, IntegerSqlType, IntegerSqlType, IntegerSqlType, IntegerSqlType)."),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1\""),
            one("meta::pure::functions::collection::tests::max::testMax_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'"),

            // Min
            one("meta::pure::functions::math::tests::min::testMin_Floats_Function_1__Boolean_1_", "No matched function in function registry for - min(DoubleSqlType, DoubleSqlType, DoubleSqlType, DoubleSqlType, DoubleSqlType)."),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Function_1__Boolean_1_", "No matched function in function registry for - min(IntegerSqlType, IntegerSqlType, IntegerSqlType, IntegerSqlType, IntegerSqlType)."),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.23D\nactual:   1.23\""),
            one("meta::pure::functions::collection::tests::min::testMin_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'fold_T_MANY__Function_1__V_m__V_m_'"),

            // Median
            one("meta::pure::functions::math::tests::median::testMedian_Floats_Function_1__Boolean_1_", "\"\nexpected: 3.0\nactual:   5.0\""),
            one("meta::pure::functions::math::tests::median::testMedian_Integers_Function_1__Boolean_1_", "\"\nexpected: 3.0\nactual:   5.0\""),
            one("meta::pure::functions::math::tests::median::testMedian_Numbers_Function_1__Boolean_1_", "\"\nexpected: 3.0\nactual:   5.0\""),

            // Mode
            one("meta::pure::functions::math::tests::mode::testMode_Number_Function_1__Boolean_1_", "\"\nexpected: 2.0\nactual:   3\""),

            // Date
            pack("meta::pure::functions::date::tests::timeBucket::dateTime", "H2 SQL Dialect does not support the function - time_bucket"),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketDays_Function_1__Boolean_1_", "\"H2 SQL Dialect does not support the function - time_bucket\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketMonths_Function_1__Boolean_1_", "\"H2 SQL Dialect does not support the function - time_bucket\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketWeeks_Function_1__Boolean_1_", "\"H2 SQL Dialect does not support the function - time_bucket\""),
            one("meta::pure::functions::date::tests::timeBucket::strictDate::testTimeBucketYears_Function_1__Boolean_1_", "\"H2 SQL Dialect does not support the function - time_bucket\""),

            // Average
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Function_1__Boolean_1_", "\"\nexpected: 3.8\nactual:   5.0\""),
            one("meta::pure::functions::math::tests::average::testAverage_Integers_Function_1__Boolean_1_", "\"\nexpected: 3.8\nactual:   5.0\""),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Function_1__Boolean_1_", "\"\nexpected: 3.8\nactual:   5.0\""),
            one("meta::pure::functions::math::tests::average::testAverage_Floats_Relation_Aggregate_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   grp,newCol\n   0,101.0\n   1,41.0\n   2,16.0\n   3,81.0\n   4,31.0\n   5,51.0\n#'\nactual:   '#TDS\n   grp,newCol\n   0,101\n   1,41\n   2,16\n   3,81\n   4,31\n   5,51\n#'\""),
            one("meta::pure::functions::math::tests::average::testAverage_Numbers_Relation_Aggregate_Function_1__Boolean_1_", "\"\nexpected: '#TDS\n   grp,newCol\n   0,101.0\n   1,41.0\n   2,16.0\n   3,81.0\n   4,31.0\n   5,51.0\n#'\nactual:   '#TDS\n   grp,newCol\n   0,101\n   1,41\n   2,16\n   3,81\n   4,31\n   5,51\n#'\""),

            // Percentile
            one("meta::pure::functions::math::tests::percentile::testPercentile_Function_1__Boolean_1_", "No SQL translation exists for the PURE function 'range_Integer_1__Integer_1__Integer_1__Integer_MANY_'"),

            // CosH
            one("meta::pure::functions::math::tests::trigonometry::cosh::testCosH_EvalFuncSig_Function_1__Boolean_1_", "No matched function in function registry for - cosh(DoubleSqlType, DoubleSqlType).\nAvailable variations for that function - [\n  cosh(AbstractNumericSqlType):DoubleSqlType\n]"),

            // SinH
            one("meta::pure::functions::math::tests::trigonometry::sinh::testSinH_EvalFuncSig_Function_1__Boolean_1_", "No matched function in function registry for - sinh(DoubleSqlType, DoubleSqlType).\nAvailable variations for that function - [\n  sinh(AbstractNumericSqlType):DoubleSqlType\n]"),

            // TanH
            one("meta::pure::functions::math::tests::trigonometry::tanh::testTanH_EvalFuncSig_Function_1__Boolean_1_", "No matched function in function registry for - tanh(DoubleSqlType, DoubleSqlType).\nAvailable variations for that function - [\n  tanh(AbstractNumericSqlType):DoubleSqlType\n]"),

            // Bitwise
            one("meta::pure::functions::math::tests::bitAnd::testBitAnd_LargeNumbers_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLDataException: Data conversion error converting \"-9223372036854775808\"; SQL statement:\nselect bitand(cast('-9223372036854775808' as integer), -999999999) [22018-214]\n\tat org.h2.message.DbException.getJdbcSQLException(DbException.java:506)\n\tat org.h2.message.DbException.getJdbcSQLException(DbException.java:477)\n\tat org.h2.message.DbException.get(DbException.java:212)\n\tat org.h2.value.ValueStringBase.getInt(ValueStringBase.java:133)\n\tat org.h2.value.Value.convertToInt(Value.java:1575)\n\tat org.h2.value.Value.convertTo(Value.java:1135)\n\tat org.h2.value.Value.castTo(Value.java:1075)\n\tat org.h2.expression.function.CastSpecification.getValue(CastSpecification.java:38)\n\tat org.h2.expression.function.CastSpecification.optimize(CastSpecification.java:49)\n\tat org.h2.expression.function.BitFunction.optimize(BitFunction.java:578)\n\tat org.h2.command.query.Select.prepareExpressions(Select.java:1170)\n\tat org.h2.command.query.Query.prepare(Query.java:218)\n\tat org.h2.command.Parser.prepareCommand(Parser.java:575)\n\tat org.h2.engine.SessionLocal.prepareLocal(SessionLocal.java:631)\n\tat org.h2.server.TcpServerThread.process(TcpServerThread.java:288)\n\tat org.h2.server.TcpServerThread.run(TcpServerThread.java:191)\n\tat java.base/java.lang.Thread.run(Thread.java:834)\nCaused by: java.lang.NumberFormatException: For input string: \"-9223372036854775808\"\n\tat java.base/java.lang.NumberFormatException.forInputString(NumberFormatException.java:65)\n\tat java.base/java.lang.Integer.parseInt(Integer.java:652)\n\tat java.base/java.lang.Integer.parseInt(Integer.java:770)\n\tat org.h2.value.ValueStringBase.getInt(ValueStringBase.java:131)\n\t... 13 more\n"),
            one("meta::pure::functions::math::tests::bitNot::testBitNot_LargeNumbers_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLDataException: Data conversion error converting \"-9223372036854775808\"; SQL statement:\nselect bitnot(cast('-9223372036854775808' as integer)) [22018-214]\n\tat org.h2.message.DbException.getJdbcSQLException(DbException.java:506)\n\tat org.h2.message.DbException.getJdbcSQLException(DbException.java:477)\n\tat org.h2.message.DbException.get(DbException.java:212)\n\tat org.h2.value.ValueStringBase.getInt(ValueStringBase.java:133)\n\tat org.h2.value.Value.convertToInt(Value.java:1575)\n\tat org.h2.value.Value.convertTo(Value.java:1135)\n\tat org.h2.value.Value.castTo(Value.java:1075)\n\tat org.h2.expression.function.CastSpecification.getValue(CastSpecification.java:38)\n\tat org.h2.expression.function.CastSpecification.optimize(CastSpecification.java:49)\n\tat org.h2.expression.function.BitFunction.optimize(BitFunction.java:578)\n\tat org.h2.command.query.Select.prepareExpressions(Select.java:1170)\n\tat org.h2.command.query.Query.prepare(Query.java:218)\n\tat org.h2.command.Parser.prepareCommand(Parser.java:575)\n\tat org.h2.engine.SessionLocal.prepareLocal(SessionLocal.java:631)\n\tat org.h2.server.TcpServerThread.process(TcpServerThread.java:288)\n\tat org.h2.server.TcpServerThread.run(TcpServerThread.java:191)\n\tat java.base/java.lang.Thread.run(Thread.java:834)\nCaused by: java.lang.NumberFormatException: For input string: \"-9223372036854775808\"\n\tat java.base/java.lang.NumberFormatException.forInputString(NumberFormatException.java:65)\n\tat java.base/java.lang.Integer.parseInt(Integer.java:652)\n\tat java.base/java.lang.Integer.parseInt(Integer.java:770)\n\tat org.h2.value.ValueStringBase.getInt(ValueStringBase.java:131)\n\t... 13 more\n"),
            one("meta::pure::functions::math::tests::bitOr::testBitOr_LargeNumbers_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLDataException: Data conversion error converting \"-9223372036854775808\"; SQL statement:\nselect bitor(cast('-9223372036854775808' as integer), -999999999) [22018-214]\n\tat org.h2.message.DbException.getJdbcSQLException(DbException.java:506)\n\tat org.h2.message.DbException.getJdbcSQLException(DbException.java:477)\n\tat org.h2.message.DbException.get(DbException.java:212)\n\tat org.h2.value.ValueStringBase.getInt(ValueStringBase.java:133)\n\tat org.h2.value.Value.convertToInt(Value.java:1575)\n\tat org.h2.value.Value.convertTo(Value.java:1135)\n\tat org.h2.value.Value.castTo(Value.java:1075)\n\tat org.h2.expression.function.CastSpecification.getValue(CastSpecification.java:38)\n\tat org.h2.expression.function.CastSpecification.optimize(CastSpecification.java:49)\n\tat org.h2.expression.function.BitFunction.optimize(BitFunction.java:578)\n\tat org.h2.command.query.Select.prepareExpressions(Select.java:1170)\n\tat org.h2.command.query.Query.prepare(Query.java:218)\n\tat org.h2.command.Parser.prepareCommand(Parser.java:575)\n\tat org.h2.engine.SessionLocal.prepareLocal(SessionLocal.java:631)\n\tat org.h2.server.TcpServerThread.process(TcpServerThread.java:288)\n\tat org.h2.server.TcpServerThread.run(TcpServerThread.java:191)\n\tat java.base/java.lang.Thread.run(Thread.java:834)\nCaused by: java.lang.NumberFormatException: For input string: \"-9223372036854775808\"\n\tat java.base/java.lang.NumberFormatException.forInputString(NumberFormatException.java:65)\n\tat java.base/java.lang.Integer.parseInt(Integer.java:652)\n\tat java.base/java.lang.Integer.parseInt(Integer.java:770)\n\tat org.h2.value.ValueStringBase.getInt(ValueStringBase.java:131)\n\t... 13 more\n"),
            one("meta::pure::functions::math::tests::bitXor::testBitXor_LargeNumbers_Function_1__Boolean_1_", "org.h2.jdbc.JdbcSQLDataException: Data conversion error converting \"-9223372036854775808\"; SQL statement:\nselect bitxor(cast('-9223372036854775808' as integer), -999999999) [22018-214]\n\tat org.h2.message.DbException.getJdbcSQLException(DbException.java:506)\n\tat org.h2.message.DbException.getJdbcSQLException(DbException.java:477)\n\tat org.h2.message.DbException.get(DbException.java:212)\n\tat org.h2.value.ValueStringBase.getInt(ValueStringBase.java:133)\n\tat org.h2.value.Value.convertToInt(Value.java:1575)\n\tat org.h2.value.Value.convertTo(Value.java:1135)\n\tat org.h2.value.Value.castTo(Value.java:1075)\n\tat org.h2.expression.function.CastSpecification.getValue(CastSpecification.java:38)\n\tat org.h2.expression.function.CastSpecification.optimize(CastSpecification.java:49)\n\tat org.h2.expression.function.BitFunction.optimize(BitFunction.java:578)\n\tat org.h2.command.query.Select.prepareExpressions(Select.java:1170)\n\tat org.h2.command.query.Query.prepare(Query.java:218)\n\tat org.h2.command.Parser.prepareCommand(Parser.java:575)\n\tat org.h2.engine.SessionLocal.prepareLocal(SessionLocal.java:631)\n\tat org.h2.server.TcpServerThread.process(TcpServerThread.java:288)\n\tat org.h2.server.TcpServerThread.run(TcpServerThread.java:191)\n\tat java.base/java.lang.Thread.run(Thread.java:834)\nCaused by: java.lang.NumberFormatException: For input string: \"-9223372036854775808\"\n\tat java.base/java.lang.NumberFormatException.forInputString(NumberFormatException.java:65)\n\tat java.base/java.lang.Integer.parseInt(Integer.java:652)\n\tat java.base/java.lang.Integer.parseInt(Integer.java:770)\n\tat org.h2.value.ValueStringBase.getInt(ValueStringBase.java:131)\n\t... 13 more\n"),
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_UpTo62Bits_Function_1__Boolean_1_", "\"\nexpected: 0\nactual:   70368744177664\""),
            one("meta::pure::functions::math::tests::bitShiftLeft::testBitShiftLeft_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"No error was thrown\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\""),
            one("meta::pure::functions::math::tests::bitShiftRight::testBitShiftRight_MoreThan62Bits_Function_1__Boolean_1_", "\"Execution error message mismatch.\nThe actual message was \"No error was thrown\"\nwhere the expected message was:\"Unsupported number of bits to shift - max bits allowed is 62\"\""),

            // And
            one("meta::pure::functions::collection::tests::and::testAnd_Function_1__Boolean_1_", "Can't find the packageable element 'andtrue'"),

            // Or
            one("meta::pure::functions::collection::tests::or::testOr_Function_1__Boolean_1_", "Can't find the packageable element 'ortrue'"),

            // Greatest
            one("meta::pure::functions::collection::tests::greatest::testGreatest_DateTime_Function_1__Boolean_1_", "\"\nexpected: %2025-02-10T20:10:20+0000\nactual:   %2025-02-10T20:10:20.000000000+0000\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Number_Function_1__Boolean_1_", "\"\nexpected: 2\nactual:   2.0\""),
            one("meta::pure::functions::collection::tests::greatest::testGreatest_Single_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1D\""),

            // Least
            one("meta::pure::functions::collection::tests::least::testLeast_DateTime_Function_1__Boolean_1_", "\"\nexpected: %2025-01-10T15:25:30+0000\nactual:   %2025-01-10T15:25:30.000000000+0000\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Number_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1.0\""),
            one("meta::pure::functions::collection::tests::least::testLeast_Single_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1D\"")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.H2).getFirst())
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
