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
import org.finos.legend.engine.pure.runtime.testConnection.CoreExternalTestConnectionCodeRepositoryProvider;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreStandardFunctionsCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Snowflake_StandardFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.snowflakeAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // StD Dev
            one("meta::pure::functions::math::tests::stdDev::testFloatStdDev_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 12\nInvalid argument types for function '*': (ARRAY, ARRAY)"),
            one("meta::pure::functions::math::tests::stdDev::testIntStdDev_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 12\nInvalid argument types for function '*': (ARRAY, ARRAY)"),
            one("meta::pure::functions::math::tests::stdDev::testMixedStdDev_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 12\nInvalid argument types for function '*': (ARRAY, ARRAY)"),
            one("meta::pure::functions::math::tests::stdDev::testNegativeNumberStdDev_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 12\nInvalid argument types for function '*': (ARRAY, ARRAY)"),
            one("meta::pure::functions::math::tests::stdDev::testPopulationStandardDeviation_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 12\nInvalid argument types for function '*': (ARRAY, ARRAY)"),

            // Variance
            one("meta::pure::functions::math::tests::variance::testVariancePopulation_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 7\nInvalid argument types for function '*': (ARRAY, ARRAY)"),
            one("meta::pure::functions::math::tests::variance::testVarianceSample_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 7\nInvalid argument types for function '*': (ARRAY, ARRAY)"),
            one("meta::pure::functions::math::tests::variance::testVariance_Population_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 7\nInvalid argument types for function '*': (ARRAY, ARRAY)"),
            one("meta::pure::functions::math::tests::variance::testVariance_Sample_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 7\nInvalid argument types for function '*': (ARRAY, ARRAY)"),

            // In
            one("meta::pure::functions::collection::tests::in::testInIsEmpty_Function_1__Boolean_1_", "class org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_LazyImpl cannot be cast to class java.lang.Boolean (org.finos.legend.pure.generated.Root_meta_pure_metamodel_type_Class_LazyImpl is in unnamed module of loader 'app'; java.lang.Boolean is in module java.base of loader 'bootstrap')"),
            one("meta::pure::functions::collection::tests::in::testInNonPrimitive_Function_1__Boolean_1_", "Can't find a match for function 'meta::pure::functions::collection::in(Firm[*],Firm[*])'"),
            one("meta::pure::functions::collection::tests::in::testInPrimitive_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error:\nCan not convert parameter 'CAST('2014-02-01' AS DATE)' of type [DATE] into expected type [BOOLEAN]")
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
