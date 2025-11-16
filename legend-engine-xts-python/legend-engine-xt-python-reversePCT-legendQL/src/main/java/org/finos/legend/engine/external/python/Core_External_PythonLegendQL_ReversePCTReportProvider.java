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

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.pct.functions.model.Functions;
import org.finos.legend.pure.m3.pct.reports.model.Adapter;
import org.finos.legend.pure.m3.pct.reports.model.AdapterReport;
import org.finos.legend.pure.m3.pct.shared.provider.PCTReportProvider;
import org.finos.legend.pure.m3.pct.shared.provider.PCTReportProviderTool;

public class Core_External_PythonLegendQL_ReversePCTReportProvider implements PCTReportProvider
{
    public static final Adapter LegendQLAdapter = new Adapter(
            "PythonLegendQL",
            "Reverse_PCT",
            "meta::external::python::reversePCT::legendQL::pythonLegendQLReversePCTAdapter_Function_1__X_o_"
    );

    @Override
    public MutableList<Functions> getFunctions()
    {
        return Lists.mutable.empty();
    }

    @Override
    public MutableList<AdapterReport> getAdapterReports()
    {
        return PCTReportProviderTool.load(Core_External_PythonLegendQL_ReversePCTReportProvider.class.getClassLoader(), AdapterReport.class,
                "pct-reports/ADAPTER_standard_compiled_PythonLegendQL.json",
                "pct-reports/ADAPTER_essential_compiled_PythonLegendQL.json",
                "pct-reports/ADAPTER_grammar_compiled_PythonLegendQL.json",
                "pct-reports/ADAPTER_relation_compiled_PythonLegendQL.json",
                "pct-reports/ADAPTER_variant_compiled_PythonLegendQL.json",
                "pct-reports/ADAPTER_unclassified_compiled_PythonLegendQL.json"
        );
    }
}
