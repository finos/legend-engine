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

package org.finos.legend.engine.plan.execution.stores.relational.test.snowflake.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegrationLoader;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.pure.code.core.CoreScenarioQuantCodeRepositoryProvider;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreRelationalSnowflakeCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Relational_Snowflake_ScenarioQuantFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreScenarioQuantCodeRepositoryProvider.scenario_Quant_Functions;
    private static final Adapter adapter = CoreRelationalSnowflakeCodeRepositoryProvider.snowflakeAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
                one("meta::external::scenario::quant::gap::testGapAnalysis_Function_1__Boolean_1_", "net.snowflake.client.jdbc.SnowflakeSQLException: SQL compilation error: error line 1 at position 597\nSliding window frame unsupported for function LAG"),
                one("meta::external::scenario::quant::vwap::testMonthlyVWAP_Function_1__Boolean_1_", "\"#TDS\n   symbol:String[0..1],month:Date[0..1],closeByVolSum:Float[0..1],volumeSum:Integer[0..1],vwap:Float[0..1]\n   AAPL,2026-01-01T00:00:00.000+0000,168177755000.0,872700000,192.71\n   AAPL,2026-02-01T00:00:00.000+0000,227944933333.33,1097000000,207.79\n   AAPL,2026-03-01T00:00:00.000+0000,140986233333.33,637000000,221.33\n#\n is not equivalent to:\n#TDS\n   symbol:String[0..1],month:DateTime[0..1],closeByVolSum:Number[0..1],volumeSum:Integer[0..1],vwap:Float[0..1]\n   AAPL,2026-01-01T00:00:00.000+0000,168177754999.87,872700000,192.71\n   AAPL,2026-02-01T00:00:00.000+0000,227944933332.55,1097000000,207.79\n   AAPL,2026-03-01T00:00:00.000+0000,140986233333.05,637000000,221.33\n#\"")
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
