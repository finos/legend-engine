// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.service.advanced;

import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils;
import org.finos.legend.engine.plan.execution.stores.service.utils.TestServer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.buildPlanForQuery;
import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.executePlan;

public class TestServiceStoreAdvanced
{
    private static final String SERVICE_STORE;
    private static final String SERVICE_STORE_MAPPING;
    private static final String MODELS;
    private static final String SERVICE_STORE_CONNECTION;

    private static final int PORT;
    private static final String SERVER_RESOURCE_FILE_PATH;

    private static TestServer server;

    static
    {
        PORT = DynamicPortGenerator.generatePort();
        SERVER_RESOURCE_FILE_PATH = "/advanced/pathContent.json";

        SERVICE_STORE = ServiceStoreTestUtils.readGrammarFromPureFile("/advanced/serviceStore.pure");
        SERVICE_STORE_CONNECTION =
                "###Connection\n" +
                        "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                        "{\n" +
                        "    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                        "    baseUrl : 'http://127.0.0.1:" + PORT + "';\n" +
                        "}";
        SERVICE_STORE_MAPPING = ServiceStoreTestUtils.readGrammarFromPureFile("/advanced/mapping.pure");
        MODELS = ServiceStoreTestUtils.readGrammarFromPureFile("/advanced/model.pure");
    }

    @BeforeClass
    public static void setup() throws Exception
    {
        server = new TestServer(PORT, SERVER_RESOURCE_FILE_PATH);
    }

    @AfterClass
    public static void teardown() throws Exception
    {
        server.shutDown();
    }

    @Test
    public void serviceStoreBatchingWithMultiplePropertiesInCrossRelationship()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::Trade.all()\n" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::Trade {\n" +
                "               tradeId,\n" +
                "               quantity,\n" +
                "               trader {\n" +
                "                   kerberos,\n" +
                "                   firstName,\n" +
                "                   lastName\n" +
                "               },\n" +
                "               product {\n" +
                "                   productId,\n" +
                "                   productName,\n" +
                "                   description\n" +
                "               }\n" +
                "           }\n" +
                "         }#, 10)\n" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::Trade {\n" +
                "               tradeId,\n" +
                "               quantity,\n" +
                "               trader {\n" +
                "                   kerberos,\n" +
                "                   firstName,\n" +
                "                   lastName\n" +
                "               },\n" +
                "               product {\n" +
                "                   productId,\n" +
                "                   productName,\n" +
                "                   description\n" +
                "               }\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(SERVICE_STORE + "\n\n" + SERVICE_STORE_CONNECTION + "\n\n" + SERVICE_STORE_MAPPING + "\n\n" + MODELS + "\n\n" + query);

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"tradeId\":\"1\",\"quantity\":\"100\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"30\",\"productName\":\"Product 30\",\"description\":\"Product 30 description\"}},{\"tradeId\":\"2\",\"quantity\":\"200\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"31\",\"productName\":\"Product 31\",\"description\":\"Product 31 description\"}},{\"tradeId\":\"3\",\"quantity\":\"300\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"32\",\"productName\":\"Product 32\",\"description\":\"Product 32 description\"}},{\"tradeId\":\"4\",\"quantity\":\"400\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"33\",\"productName\":\"Product 33\",\"description\":\"Product 33 description\"}},{\"tradeId\":\"5\",\"quantity\":\"100\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"34\",\"productName\":\"Product 34\",\"description\":\"Product 34 description\"}},{\"tradeId\":\"6\",\"quantity\":\"200\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"35\",\"productName\":\"Product 35\",\"description\":\"Product 35 description\"}},{\"tradeId\":\"7\",\"quantity\":\"300\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"36\",\"productName\":\"Product 36\",\"description\":\"Product 36 description\"}},{\"tradeId\":\"8\",\"quantity\":\"400\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"37\",\"productName\":\"Product 37\",\"description\":\"Product 37 description\"}},{\"tradeId\":\"9\",\"quantity\":\"300\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"38\",\"productName\":\"Product 38\",\"description\":\"Product 38 description\"}},{\"tradeId\":\"10\",\"quantity\":\"400\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"39\",\"productName\":\"Product 39\",\"description\":\"Product 39 description\"}}]}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan));
    }
}
