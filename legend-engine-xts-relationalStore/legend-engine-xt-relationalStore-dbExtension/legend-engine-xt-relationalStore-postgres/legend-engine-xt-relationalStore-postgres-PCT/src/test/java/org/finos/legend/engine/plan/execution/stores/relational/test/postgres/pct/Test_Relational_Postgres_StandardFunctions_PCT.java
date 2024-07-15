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

package org.finos.legend.engine.plan.execution.stores.relational.test.postgres.pct;

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

public class Test_Relational_Postgres_StandardFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreStandardFunctionsCodeRepositoryProvider.standardFunctions;
    private static final Adapter adapter = CoreExternalTestConnectionCodeRepositoryProvider.postgresAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            // StD Dev
            one("meta::pure::functions::math::tests::stdDev::testFloatStdDev_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),
            one("meta::pure::functions::math::tests::stdDev::testIntStdDev_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),
            one("meta::pure::functions::math::tests::stdDev::testMixedStdDev_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),
            one("meta::pure::functions::math::tests::stdDev::testNegativeNumberStdDev_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),
            one("meta::pure::functions::math::tests::stdDev::testPopulationStandardDeviation_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),

            // Variance
            one("meta::pure::functions::math::tests::variance::testVariancePopulation_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),
            one("meta::pure::functions::math::tests::variance::testVarianceSample_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),
            one("meta::pure::functions::math::tests::variance::testVariance_Population_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\""),
            one("meta::pure::functions::math::tests::variance::testVariance_Sample_Function_1__Boolean_1_", "\"Unexpected error executing function with params [Anonymous_Lambda]\"")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestConnectionIntegrationLoader.extensions().select(c -> c.getDatabaseType() == DatabaseType.Postgres).getFirst())
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
