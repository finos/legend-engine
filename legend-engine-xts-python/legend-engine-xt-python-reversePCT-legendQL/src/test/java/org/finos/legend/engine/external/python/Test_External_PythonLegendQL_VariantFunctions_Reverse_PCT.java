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

package org.finos.legend.engine.external.python;

import junit.framework.Test;
import org.eclipse.collections.api.RichIterable;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.code.core.VariantCodeRepositoryProvider;
import org.finos.legend.pure.generated.Root_meta_pure_test_pct_reversePCT_framework_ReversesForSource;
import org.finos.legend.pure.generated.core_external_python_reverse_pct_legend_ql_pythonReversePCTLegendQLApi;
import org.finos.legend.pure.m3.pct.reports.config.PCTReportConfiguration;
import org.finos.legend.pure.m3.pct.reports.config.exclusion.ExclusionSpecification;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.shared.model.ReportScope;
import org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled;

import static org.finos.legend.pure.runtime.java.compiled.testHelper.PureTestBuilderCompiled.getClassLoaderExecutionSupport;

public class Test_External_PythonLegendQL_VariantFunctions_Reverse_PCT extends PCTReportConfiguration
{
    private static final ReportScope reportScope = VariantCodeRepositoryProvider.variantFunctions;
    private static final Adapter adapter = Core_External_PythonLegendQL_ReversePCTReportProvider.LegendQLAdapter;
    private static final String platform = "compiled";
    private static final RichIterable<? extends Root_meta_pure_test_pct_reversePCT_framework_ReversesForSource> reverseInfo = core_external_python_reverse_pct_legend_ql_pythonReversePCTLegendQLApi.Root_meta_external_python_reversePCT_legendQL_pythonLegendQLReversesVariant__ReversesForSource_MANY_(getClassLoaderExecutionSupport(Thread.currentThread().getContextClassLoader()));
    private static final MutableList<ExclusionSpecification> expectedFailures = PythonLegendQLReversePCTHelper.build(reverseInfo);

    static
    {
        expectedFailures.addAll(PythonLegendQLReversePCTHelper.getMissingExpectedFailures(PureTestBuilderCompiled.buildPCTTestSuite(reportScope, expectedFailures, adapter), reverseInfo));
    }

    public static Test suite()
    {
        return PythonLegendQLReversePCTHelper.buildSuite(reportScope, adapter, expectedFailures);
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
