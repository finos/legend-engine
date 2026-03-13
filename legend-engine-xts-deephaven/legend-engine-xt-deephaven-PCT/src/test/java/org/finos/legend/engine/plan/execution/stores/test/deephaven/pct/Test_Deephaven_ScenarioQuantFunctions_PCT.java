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

package org.finos.legend.engine.plan.execution.stores.test.deephaven.pct;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.deephaven.test.TestDeephavenConnectionIntegrationLoader;
import org.finos.legend.engine.pure.code.core.CoreScenarioQuantCodeRepositoryProvider;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.finos.legend.pure.code.core.CoreDeephavenPCTCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.engine.test.shared.framework.PureTestHelperFramework.wrapSuite;

public class Test_Deephaven_ScenarioQuantFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreScenarioQuantCodeRepositoryProvider.scenario_Quant_Functions;
    private static final Adapter adapter = CoreDeephavenPCTCodeRepositoryProvider.deephavenAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            one("meta::external::scenario::quant::gap::testGapAnalysis_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::project_Relation_1__FuncColSpecArray_1__Relation_1_\""),
            one("meta::external::scenario::quant::maxDrawDown::testMaxDrawDown_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::groupBy_Relation_1__ColSpec_1__AggColSpec_1__Relation_1_\""),
            one("meta::external::scenario::quant::sma::testSimpleMovingAverage5Days_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::project_Relation_1__FuncColSpecArray_1__Relation_1_\""),
            one("meta::external::scenario::quant::volatility::close::testAnnualizedRolling10DaysVolatility_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::project_Relation_1__FuncColSpecArray_1__Relation_1_\""),
            one("meta::external::scenario::quant::vwap::testMonthlyVWAP_Function_1__Boolean_1_", "\"function not supported yet: meta::pure::functions::relation::project_Relation_1__FuncColSpecArray_1__Relation_1_\"")
    );

    public static Test suite()
    {
        return wrapSuite(
                () -> true,
                () -> PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter),
                () -> false,
                Lists.mutable.with((TestServerResource) TestDeephavenConnectionIntegrationLoader.extensions().getFirst())
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
