// Copyright 2024 Goldman Sachs
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

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.pct.functions.model.Functions;
import org.finos.legend.pure.m3.pct.reports.model.AdapterReport;
import org.finos.legend.pure.m3.pct.shared.provider.PCTReportProvider;

public class Core_JavaBinding_PCTReportProvider implements PCTReportProvider
{
    @Override
    public MutableList<Functions> getFunctions()
    {
        return Lists.mutable.empty();
    }

    @Override
    public MutableList<AdapterReport> getAdapterReports()
    {
        try
        {
            return Lists.mutable.with(
                    JsonMapper.builder().build().readValue(
                            Core_JavaBinding_PCTReportProvider.class.getResourceAsStream("/pct-reports/ADAPTER_standard_compiled_Java.json"),
                            AdapterReport.class
                    ),
                    JsonMapper.builder().build().readValue(
                            Core_JavaBinding_PCTReportProvider.class.getResourceAsStream("/pct-reports/ADAPTER_essential_compiled_Java.json"),
                            AdapterReport.class
                    ),
                    JsonMapper.builder().build().readValue(
                            Core_JavaBinding_PCTReportProvider.class.getResourceAsStream("/pct-reports/ADAPTER_grammar_compiled_Java.json"),
                            AdapterReport.class
                    ),
                    JsonMapper.builder().build().readValue(
                            Core_JavaBinding_PCTReportProvider.class.getResourceAsStream("/pct-reports/ADAPTER_relation_compiled_Java.json"),
                            AdapterReport.class
                    )
            );
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
