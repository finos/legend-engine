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

package org.finos.legend.engine.plan.execution.stores.service.features.pathOffset;

import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestSuite;
import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.buildPlanForQuery;
import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.executePlan;

public class TestServiceStorePathOffset extends ServiceStoreTestSuite
{
    private static String pureGrammar;

    @BeforeClass
    public static void setup()
    {
        setupServer("features/pathOffset");

        String serviceStoreConnection =
                "###Connection\n" +
                        "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                        "{\n" +
                        "    store   : meta::external::store::service::showcase::store::EmployeesServiceStore;\n" +
                        "    baseUrl : 'http://127.0.0.1:" + getPort() + "';\n" +
                        "}";
        pureGrammar = ServiceStoreTestUtils.readGrammarFromPureFile("/features/pathOffset/testGrammar.pure") + "\n\n" + serviceStoreConnection;
    }

    @Test
    public void serviceStoreSimpleExample()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::Person.all()" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::Person {\n" +
                "               firstName,\n" +
                "               lastName,\n" +
                "               middleName\n" +
                "           }\n" +
                "         }#)" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::Person {\n" +
                "               firstName,\n" +
                "               lastName,\n" +
                "               middleName\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query);

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"firstName\":\"Nancy\",\"lastName\":\"Fraher\",\"middleName\":null},{\"firstName\":\"Jason\",\"lastName\":\"Schlichting\",\"middleName\":null},{\"firstName\":\"Jason2\",\"lastName\":\"Schlichting\",\"middleName\":null}]}";

        Assert.assertEquals(expectedRes, executePlan(plan));
    }

    @Test
    public void serviceStoreExampleWithNesting()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::Firm.all()" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::Firm {\n" +
                "               firmName,\n" +
                "               address{\n" +
                "                   street\n" +
                "               }\n" +
                "           }\n" +
                "         }#)" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::Firm {\n" +
                "               firmName,\n" +
                "               address{\n" +
                "                   street\n" +
                "               }\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query);

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"firmName\":\"Firm A\",\"address\":[{\"street\":\"Firm A address\"}]},{\"firmName\":\"Firm B\",\"address\":[{\"street\":\"Firm B address 1\"},{\"street\":\"Firm B address 2\"}]},{\"firmName\":\"Firm c\",\"address\":[]}]}";

        Assert.assertEquals(expectedRes, executePlan(plan));
    }

    @Test
    public void serviceStoreExampleWithCrossStore()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::Firm.all()" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::Firm {\n" +
                "               firmName,\n" +
                "               address{\n" +
                "                   street\n" +
                "               },\n" +
                "               employees{" +
                "                   firstName,\n" +
                "                   lastName,\n" +
                "                   middleName\n" +
                "               }\n" +
                "           }\n" +
                "         }#)" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::Firm {\n" +
                "               firmName,\n" +
                "               address{\n" +
                "                   street\n" +
                "               },\n" +
                "               employees{" +
                "                   firstName,\n" +
                "                   lastName,\n" +
                "                   middleName\n" +
                "               }\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query);

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"firmName\":\"Firm A\",\"address\":[{\"street\":\"Firm A address\"}],\"employees\":[{\"firstName\":\"Nancy\",\"lastName\":\"Fraher\",\"middleName\":null}]},{\"firmName\":\"Firm B\",\"address\":[{\"street\":\"Firm B address 1\"},{\"street\":\"Firm B address 2\"}],\"employees\":[{\"firstName\":\"Jason\",\"lastName\":\"Schlichting\",\"middleName\":null}]},{\"firmName\":\"Firm c\",\"address\":[],\"employees\":[{\"firstName\":\"Jason2\",\"lastName\":\"Schlichting\",\"middleName\":null}]}]}";

        Assert.assertEquals(expectedRes, executePlan(plan));
    }
}
