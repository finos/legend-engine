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

package org.finos.legend.engine.language.bigqueryFunc.api;

import com.google.cloud.bigquery.*;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.functionActivator.api.FunctionActivatorAPI;
import org.finos.legend.engine.functionActivator.api.input.FunctionActivatorInput;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.ServiceLoader;

public class TestBigQueryFunctionService
{
    private final FunctionActivatorAPI api = new FunctionActivatorAPI(new ModelManager(DeploymentMode.TEST), (PureModel pureModel) -> Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class)).flatCollect(e -> e.getExtraExtensions(pureModel)));

    public static void main(String[] args)
    {
        String datasetName = "my_dataset";
        String tableFunctionName = "my_table_function";
        BigQuery bigQuery = BigQueryOptions.newBuilder().setProjectId("peppy-aquifer-391118").build().getService();

        RoutineId routineId = RoutineId.of("peppy-aquifer-391118", datasetName, tableFunctionName);

        RoutineInfo routineInfo =
                RoutineInfo
                        .newBuilder(routineId)
                        .setRoutineType("TABLE_VALUED_FUNCTION")
                        .setLanguage("SQL")
                        .setBody("select * from my_dataset.employees")
                        .build();
        try
        {
            bigQuery.create(routineInfo);
            System.out.println("Created table function!");
        }
        catch (BigQueryException e)
        {
            System.out.println(e);
        }
    }

    @Test
    public void testDeployment()
    {
        String val =
"###Relational\n" +
        "Database com::gs::EmployeeDatabase\n" +
        "(\n" +
        "  Schema my_dataset\n" +
        "  (\n" +
        "    Table employees\n" +
        "    (\n" +
        "      employee_id VARCHAR(200) PRIMARY KEY,\n" +
        "      first_name VARCHAR(200),\n" +
        "      last_name VARCHAR(200),\n" +
        "      start_date DATE\n" +
        "    )\n" +
        "  )\n" +
        ")\n" +
                        "\n" +
                        "\n" +
                        "###Pure\n" +
                        "Class com::gs::Employee\n" +
                        "{\n" +
                        "  firstName: String[1];\n" +
                        "  lastName: String[1];\n" +
                        "  startDate: Date[1];\n" +
                        "}\n" +
                        "\n" +
                        "function com::gs::GetEmployees(): com::gs::Employee[*]\n" +
                        "{\n" +
                        "  com::gs::Employee.all()->from(\n" +
                        "    com::gs::EmployeeMapping,\n" +
                        "    com::gs::BigQueryRuntime\n" +
                        "  )\n" +
                        "}\n" +
                        "\n" +
                        "\n" +
                        "###Mapping\n" +
                                "Mapping com::gs::EmployeeMapping\n" +
                                "(\n" +
                                "  com::gs::Employee: Relational\n" +
                                "  {\n" +
                                "    ~primaryKey\n" +
                                "    (\n" +
                                "      [com::gs::EmployeeDatabase]my_dataset.employees.employee_id\n" +
                                "    )\n" +
                                "    ~mainTable [com::gs::EmployeeDatabase]my_dataset.employees\n" +
                                "    firstName: [com::gs::EmployeeDatabase]my_dataset.employees.first_name,\n" +
                                "    lastName: [com::gs::EmployeeDatabase]my_dataset.employees.last_name,\n" +
                                "    startDate: [com::gs::EmployeeDatabase]my_dataset.employees.start_date\n" +
                                "  }\n" +
                                ")\n" +
                        "\n" +
                        "\n" +
                        "###Connection\n" +
                        "RelationalDatabaseConnection com::gs::BigQueryConnection\n" +
                        "{\n" +
                        "  store: com::gs::EmployeeDatabase;\n" +
                        "  type: BigQuery;\n" +
                        "  specification: BigQuery\n" +
                        "  {\n" +
                        "    projectId: 'peppy-aquifer-391118';\n" +
                        "    defaultDataset: 'my_dataset';\n" +
                        "  };\n" +
                        "  auth: GCPApplicationDefaultCredentials;\n" +
                        "}\n" +
                        "\n" +
                        "\n" +
                        "###Runtime\n" +
                        "Runtime com::gs::BigQueryRuntime\n" +
                        "{\n" +
                        "  mappings:\n" +
                        "  [\n" +
                        "    com::gs::EmployeeMapping\n" +
                        "  ];\n" +
                        "  connections:\n" +
                        "  [\n" +
                        "    com::gs::EmployeeDatabase:\n" +
                        "    [\n" +
                        "      connection: com::gs::BigQueryConnection\n" +
                        "    ]\n" +
                        "  ];\n" +
                        "}\n" +
                        "###BigQuery\n" +
                        "BigQueryFunction com::gs::BigQueryFunction\n" +
                        "{\n" +
                        "    functionName: 'test_function';\n" +
                        "    description: 'test';\n" +
                        "    function: com::gs::GetEmployees():Employee[*];\n" +
                        "}\n" +
                        "";


        Response response = api.publishToSandbox(new FunctionActivatorInput("vX_X_X", "com::gs::BigQueryFunction", PureGrammarParser.newInstance().parseModel(val)), null);
        Assert.assertEquals("[]", response.getEntity().toString());
    }
}
