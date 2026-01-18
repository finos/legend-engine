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

package org.finos.legend.engine.pure.code.core.java.binding;

import junit.framework.Test;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.pure.code.core.CoreScenarioQuantCodeRepositoryProvider;
import org.finos.legend.pure.code.core.CoreJavaPlatformBindingCodeRepositoryProvider;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

public class Test_JAVA_ScenarioQuantFunctions_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = CoreScenarioQuantCodeRepositoryProvider.scenario_Quant_Functions;
    private static final Adapter adapter = CoreJavaPlatformBindingCodeRepositoryProvider.javaAdapter;
    private static final String platform = "compiled";
    private static final MutableList<ExclusionSpecification> expectedFailures = Lists.mutable.with(
            one("meta::external::scenario::quant::return::logReturn::testMinuteLogReturn_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::external::scenario::quant::gap::testGapAnalysis_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::external::scenario::quant::maxDrawDown::testMaxDrawDown_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::external::scenario::quant::sma::testSimpleMovingAverage5Days_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::external::scenario::quant::volatility::close::testAnnualizedRolling10DaysVolatility_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\""),
            one("meta::external::scenario::quant::vwap::testMonthlyVWAP_Function_1__Boolean_1_", "\"Instance of type 'meta::pure::metamodel::relation::TDS' can't be translated\"")
     );

    public static Test suite()
    {
        return PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter);
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
