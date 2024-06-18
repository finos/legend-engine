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

package org.finos.legend.pure.code.core;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.pure.m3.pct.shared.provider.PCTReportProvider;
import org.finos.legend.pure.m3.pct.reports.model.AdapterReport;
import org.finos.legend.pure.m3.pct.functions.model.Functions;

public class Unclassified_Functions_PCTReportProvider implements PCTReportProvider
{
    @Override
    public MutableList<Functions> getFunctions()
    {
        try
        {
            return org.eclipse.collections.api.factory.Lists.mutable.with(
                    JsonMapper.builder().build().readValue(
                            Unclassified_Functions_PCTReportProvider.class.getResourceAsStream("/pct-reports/FUNCTIONS_unclassified.json"),
                            Functions.class
                    )
            );
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public MutableList<AdapterReport> getAdapterReports()
    {
        return Lists.mutable.empty();
    }
}
