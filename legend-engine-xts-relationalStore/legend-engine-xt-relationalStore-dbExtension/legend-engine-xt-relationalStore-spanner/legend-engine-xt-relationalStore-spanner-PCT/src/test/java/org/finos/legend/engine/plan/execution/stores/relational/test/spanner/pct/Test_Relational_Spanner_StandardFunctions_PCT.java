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

package org.finos.legend.engine.plan.execution.stores.relational.test.spanner.pct;

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

public class Test_Relational_Spanner_StandardFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.spannerAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(

            //in
            one("meta::pure::functions::collection::tests::in::testInIsEmpty_Function_1__Boolean_1_", "NullPointer exception"),
            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "Error while executing: insert into Firm (_pureId,legalName) values (10,'f1');"),
            one("meta::pure::functions::collection::tests::in::testInPrimitive_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: NOT_FOUND: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: NOT_FOUND: [ERROR] operator does not exist: bigint = text\nHint: No operator matches the given name and argument types. You might need to add explicit type casts. - Statement: 'select 1 in (1, 2, 5, 2, Text'a', Boolean'true', Date'2014-02-01', Text'c')'"),

            //max
            one("meta::pure::functions::math::tests::max::testMax_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"max(%s)\"\""),
            one("meta::pure::functions::math::tests::max::testMax_Floats_Relation_Window_Function_1__Boolean_1_", "\"[unsupported-api] Window Columns not supported for Database Type: Spanner\""),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"max(%s)\"\""),
            one("meta::pure::functions::math::tests::max::testMax_Integers_Relation_Window_Function_1__Boolean_1_", "\"[unsupported-api] Window Columns not supported for Database Type: Spanner\""),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.0D\nactual:   1\""),
            one("meta::pure::functions::math::tests::max::testMax_Numbers_Relation_Window_Function_1__Boolean_1_", "\"[unsupported-api] Window Columns not supported for Database Type: Spanner\""),

            //min
            one("meta::pure::functions::math::tests::min::testMin_Floats_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"min(%s)\"\""),
            one("meta::pure::functions::math::tests::min::testMin_Floats_Relation_Window_Function_1__Boolean_1_", "\"[unsupported-api] Window Columns not supported for Database Type: Spanner\""),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Function_1__Boolean_1_", "\"Unused format args. [5] arguments provided to expression \"min(%s)\"\""),
            one("meta::pure::functions::math::tests::min::testMin_Integers_Relation_Window_Function_1__Boolean_1_", "\"[unsupported-api] Window Columns not supported for Database Type: Spanner\""),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Function_1__Boolean_1_", "\"\nexpected: 1.23D\nactual:   1.23\""),
            one("meta::pure::functions::math::tests::min::testMin_Numbers_Relation_Window_Function_1__Boolean_1_", "\"[unsupported-api] Window Columns not supported for Database Type: Spanner\""),

            //stdDev
            one("meta::pure::functions::math::tests::stdDev::testFloatStdDev_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: INVALID_ARGUMENT: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: INVALID_ARGUMENT: [ERROR] syntax error at or near \"[\" - Statement: 'select sqrt(var_pop([1.0,2.0,3.0]))'"),
            one("meta::pure::functions::math::tests::stdDev::testIntStdDev_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: INVALID_ARGUMENT: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: INVALID_ARGUMENT: [ERROR] syntax error at or near \"[\" - Statement: 'select sqrt(var_pop([1,2,3]))'"),
            one("meta::pure::functions::math::tests::stdDev::testMixedStdDev_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: INVALID_ARGUMENT: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: INVALID_ARGUMENT: [ERROR] syntax error at or near \"[\" - Statement: 'select sqrt(var_pop([1.0,2,3.0]))'"),
            one("meta::pure::functions::math::tests::stdDev::testNegativeNumberStdDev_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: INVALID_ARGUMENT: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: INVALID_ARGUMENT: [ERROR] syntax error at or near \"[\" - Statement: 'select sqrt(var_pop([-2,-4,-6]))'"),
            one("meta::pure::functions::math::tests::stdDev::testPopulationStandardDeviation_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: INVALID_ARGUMENT: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: INVALID_ARGUMENT: [ERROR] syntax error at or near \"[\" - Statement: 'select sqrt(var_pop([1,2]))'"),
            one("meta::pure::functions::math::tests::stdDev::testSimpleGroupByStandardDeviationPopulation_Function_1__Boolean_1_", "\"[unsupported-api] The function 'stdDevPopulation' (state: [Select, false]) is not supported yet\""),
            one("meta::pure::functions::math::tests::stdDev::testSimpleGroupByStandardDeviationSample_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: Postgres function stddev_samp(double precision) is not supported"),

            //variance
            one("meta::pure::functions::math::tests::variance::testSimpleGroupByVariancePopulation_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: Postgres function var_pop(bigint) is not supported"),
            one("meta::pure::functions::math::tests::variance::testSimpleGroupByVarianceSample_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: UNIMPLEMENTED: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: UNIMPLEMENTED: Postgres function var_samp(double precision) is not supported"),
            one("meta::pure::functions::math::tests::variance::testVariancePopulation_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: INVALID_ARGUMENT: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: INVALID_ARGUMENT: [ERROR] syntax error at or near \"[\" - Statement: 'select var_pop([1,2])'"),
            one("meta::pure::functions::math::tests::variance::testVarianceSample_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: INVALID_ARGUMENT: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: INVALID_ARGUMENT: [ERROR] syntax error at or near \"[\" - Statement: 'select var_samp([1.0,2.0,3.0])'"),
            one("meta::pure::functions::math::tests::variance::testVariance_Population_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: INVALID_ARGUMENT: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: INVALID_ARGUMENT: [ERROR] syntax error at or near \"[\" - Statement: 'select var_pop([1,2])'"),
            one("meta::pure::functions::math::tests::variance::testVariance_Sample_Function_1__Boolean_1_", "org.finos.legend.engine.spanner.jdbc.shaded.com.google.cloud.spanner.jdbc.JdbcSqlExceptionFactory$JdbcSqlExceptionImpl: INVALID_ARGUMENT: org.finos.legend.engine.spanner.jdbc.shaded.io.grpc.StatusRuntimeException: INVALID_ARGUMENT: [ERROR] syntax error at or near \"[\" - Statement: 'select var_pop([1.0,2.0,3.0])'"),

            // Date
            pack("meta::pure::functions::date::tests::timeBucket", "unsupported-api] The function 'timeBucket' (state: [Select, false]) is not supported yet")

    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Spanner).getFirst())
        );
    }

    @Override
    public ReportScope getReportScope()
    {
        return reportScope;
    }

    @Override
    public MutableList<ExclusionSpecification> expectedFailures()
    {
        return expectedFailures;
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
