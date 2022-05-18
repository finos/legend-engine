// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.constraint.pure.api;


import org.finos.legend.engine.constraint.pure.api.model.RelationalValidationInput;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.generation.extension.LegendPlanGeneratorExtension;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;

public class TestValidate {

    @Test
    public void testValidate  ()
    {
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel ( "###Connection\n" +
                "RelationalDatabaseConnection test::H2DemoConnection\n" +
                "{\n" +
                "  store: test::H2DemoDataBase;\n" +
                "  type: H2;\n" +
                "  specification: LocalH2\n" +
                "  {\n" +
                "    testDataSetupSqls: [\n" +
                "      'Drop table if exists CountryTable;\\r\\nDrop table if exists FirmTable;\\r\\nDrop table if exists EmployeeTable;\\r\\nCreate Table FirmTable(id INT, legal_name VARCHAR(200));\\r\\nCreate Table CountryTable (id INT, country_name VARCHAR(200));\\r\\nCreate Table EmployeeTable (id INT, firm_id INT, full_name VARCHAR(200), country_id INT);\\r\\nInsert into FirmTable (id, legal_name) values (111,\\'FINOS\\');\\r\\nInsert into FirmTable (id, legal_name) values (222,\\'LINUX_FOUNDATION\\');\\r\\nInsert into CountryTable (id, country_name) values (1,\\'USA\\');\\r\\nInsert into CountryTable (id, country_name) values (2,\\'UK\\');\\r\\nInsert into EmployeeTable (id, firm_id, full_name, country_id) values (1,111,\\'Person A\\',1);\\r\\nInsert into EmployeeTable (id, firm_id, full_name, country_id) values (2,111,\\'Person B\\',2);\\r\\nInsert into EmployeeTable (id, firm_id, full_name, country_id) values (3,111,\\'Person C\\',1);\\r\\nInsert into EmployeeTable (id, firm_id, full_name, country_id) values (4,222,\\'Person D\\',1);\\r\\nInsert into EmployeeTable (id, firm_id, full_name, country_id) values (5,222,\\'Person E\\',1);\\r\\nInsert into EmployeeTable (id, firm_id, full_name, country_id) values (6,222,\\'Person F\\',2);'\n" +
                "      ];\n" +
                "  };\n" +
                "  auth: DefaultH2;\n" +
                "}\n" +
                "###Relational \n" +
                "Database test::H2DemoDataBase\n" +
                "(\n" +
                "  Table FirmTable\n" +
                "  (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    legal_name VARCHAR(200)\n" +
                "  )\n" +
                "  Table EmployeeTable\n" +
                "  (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    firm_id INTEGER,\n" +
                "    full_name VARCHAR(200),\n" +
                "    country_id INTEGER\n" +
                "  )\n" +
                "  Table CountryTable\n" +
                "  (\n" +
                "    id INTEGER PRIMARY KEY,\n" +
                "    country_name VARCHAR(200)\n" +
                "  )\n" +
                "\n" +
                "  Join FirmEmployee(EmployeeTable.firm_id = FirmTable.id)\n" +
                "  Join EmployeeCountry(EmployeeTable.country_id = CountryTable.id)\n" +
                ")\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::RelationalToModelMapping\n" +
                "(\n" +
                "  *test::Employee: Relational\n" +
                "  {\n" +
                "    ~primaryKey\n" +
                "    (\n" +
                "      [test::H2DemoDataBase]EmployeeTable.id\n" +
                "    )\n" +
                "    ~mainTable [test::H2DemoDataBase]EmployeeTable\n" +
                "    fullName: [test::H2DemoDataBase]EmployeeTable.full_name\n" +
                "  }\n" +
                "  *test::Firm: Relational\n" +
                "  {\n" +
                "    ~primaryKey\n" +
                "    (\n" +
                "      [test::H2DemoDataBase]FirmTable.id\n" +
                "    )\n" +
                "    ~mainTable [test::H2DemoDataBase]FirmTable\n" +
                "    legalName: [test::H2DemoDataBase]FirmTable.legal_name,\n" +
                "    employees[test_Employee]: [test::H2DemoDataBase]@FirmEmployee\n" +
                "  })\n" +
                "  \n" +
                "  \n" +
                "###Pure\n" +
                "Class test::Firm\n" +
                "[\n" +
                "  longLegalName: $this.legalName->length() <= 5\n" +
                "]\n" +
                "{\n" +
                "  legalName: String[1];\n" +
                "  employees: test::Employee[*];\n" +
                "  employeeCount() {$this.employees->count()}: Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class test::Employee\n" +
                "[isNotEmptyString:$this.fullName!='']\n" +
                "{\n" +
                "  fullName: String[1];\n" +
                "}\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime test::H2DemoRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::RelationalToModelMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    test::H2DemoDataBase:\n" +
                "    [\n" +
                "      connection_1: test::H2DemoConnection\n" +
                "    ]\n" +
                "  ];\n" +
                "}");

        RelationalValidationInput validation = new RelationalValidationInput();
        validation.clientVersion = "vX_X_X";
        validation._class  ="test::Firm";
        validation.mapping = "test::RelationalToModelMapping";
        validation.runtime = "test::H2DemoRuntime";
        validation.model = contextData;
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
        PlanExecutor executor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();
        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(getClass().getClassLoader(), new java.lang.Class<?>[]{HttpServletRequest.class}, new ReflectiveInvocationHandler(new Request()));
        LegendPlanGeneratorExtension ex = new LegendPlanGeneratorExtension();

        Validate validate = new Validate(modelManager,executor, (PureModel pm)  -> ex.getExtraRouterExtensions(pm), LegendPlanTransformers.transformers);
        Response response = validate.validate(request, validation, SerializationFormat.defaultFormat, null, null);
        try {
           String r = responseAsString(response);
           assertEquals("{\"builder\": {\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"CONSTRAINT_ID\",\"type\":\"String\"},{\"name\":\"ENFORCEMENT_LEVEL\",\"type\":\"String\"},{\"name\":\"MESSAGE\",\"type\":\"String\"}]}, \"activities\": [{\"_type\":\"relational\",\"sql\":\"select 'longLegalName' as \\\"CONSTRAINT_ID\\\", 'Error' as \\\"ENFORCEMENT_LEVEL\\\", '' as \\\"MESSAGE\\\" from FirmTable as \\\"root\\\" where not char_length(\\\"root\\\".legal_name) <= 5\"}], \"result\" : {\"columns\" : [\"CONSTRAINT_ID\",\"ENFORCEMENT_LEVEL\",\"MESSAGE\"], \"rows\" : [{\"values\": [\"longLegalName\",\"Error\",\"\"]}]}}",r);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private String responseAsString(Response response) throws IOException
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Object r = response.getEntity();
        if(r instanceof StreamingOutput ) ((StreamingOutput) r).write(baos) ;
        return baos.toString("UTF-8");
    }

    private static class ReflectiveInvocationHandler implements InvocationHandler
    {
        private final Object[] delegates;

        private ReflectiveInvocationHandler(Object... delegates)
        {
            this.delegates = delegates;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
        {
            for (Object delegate : delegates)
            {
                try
                {
                    return delegate.getClass().getMethod(method.getName(), method.getParameterTypes()).invoke(delegate, args);
                }
                catch (NoSuchMethodException e)
                {
                    // The loop will complete if all delegates fail
                }
            }
            throw new UnsupportedOperationException("Method not simulated: " + method);
        }
    }

    private static class Request
    {
        @SuppressWarnings("unused")
        public String getRemoteUser()
        {
            return "someone";
        }
    }
}
