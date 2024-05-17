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

package org.finos.legend.engine.plan.execution.stores.relational.test.duckdb.integration;

import com.fasterxml.jackson.databind.json.JsonMapper;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.pure.m3.pct.PCTReportProvider;
import org.finos.legend.pure.m3.pct.model.Report;

public class Core_Relational_DuckDB_PCTReportProvider implements PCTReportProvider
{
    @Override
    public MutableList<Report> getReports()
    {
        try
        {
            return Lists.mutable.with(
                    JsonMapper.builder().build().readValue(
                            Core_Relational_DuckDB_PCTReportProvider.class.getResourceAsStream("/pct-reports/base_compiled_testAdapterForRelationalWithDuckDBExecution_Function_1__X_o_.json"),
                            Report.class
                    ),
                    JsonMapper.builder().build().readValue(
                            Core_Relational_DuckDB_PCTReportProvider.class.getResourceAsStream("/pct-reports/basic_compiled_testAdapterForRelationalWithDuckDBExecution_Function_1__X_o_.json"),
                            Report.class
                    ),
                    JsonMapper.builder().build().readValue(
                            Core_Relational_DuckDB_PCTReportProvider.class.getResourceAsStream("/pct-reports/grammar_compiled_testAdapterForRelationalWithDuckDBExecution_Function_1__X_o_.json"),
                            Report.class
                    ),
                    JsonMapper.builder().build().readValue(
                            Core_Relational_DuckDB_PCTReportProvider.class.getResourceAsStream("/pct-reports/relation_compiled_testAdapterForRelationalWithDuckDBExecution_Function_1__X_o_.json"),
                            Report.class
                    )
            );
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
