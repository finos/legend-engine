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

package org.finos.legend.engine.plan.execution.stores.service.showcase;

import org.eclipse.collections.api.factory.Maps;
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

public class ServiceStoreShowcaseTest
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
        SERVER_RESOURCE_FILE_PATH = "/showcase/pathContent.json";

        SERVICE_STORE = ServiceStoreTestUtils.readGrammarFromPureFile("/showcase/serviceStore.pure");
        SERVICE_STORE_CONNECTION =
                "###Connection\n" +
                "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                "{\n" +
                "    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                "    baseUrl : 'http://127.0.0.1:" + PORT + "';\n" +
                "}";
        SERVICE_STORE_MAPPING = ServiceStoreTestUtils.readGrammarFromPureFile("/showcase/mapping.pure");
        MODELS = ServiceStoreTestUtils.readGrammarFromPureFile("/showcase/model.pure");
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
    public void serviceStoreSimpleExample()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::S_Trade.all()" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Trade {\n" +
                "               s_tradeId,\n" +
                "               s_traderDetails,\n" +
                "               s_tradeDetails\n" +
                "           }\n" +
                "         }#)" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Trade {\n" +
                "               s_tradeId,\n" +
                "               s_traderDetails,\n" +
                "               s_tradeDetails\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(SERVICE_STORE + "\n\n" + SERVICE_STORE_CONNECTION + "\n\n" + SERVICE_STORE_MAPPING + "\n\n" + MODELS + "\n\n" + query);

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"s_tradeId\":\"1\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"30:100\"},{\"s_tradeId\":\"2\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"31:200\"},{\"s_tradeId\":\"3\",\"s_traderDetails\":\"abc:F_Name_2:L_Name_2\",\"s_tradeDetails\":\"30:300\"},{\"s_tradeId\":\"4\",\"s_traderDetails\":\"abc:F_Name_2:L_Name_2\",\"s_tradeDetails\":\"31:400\"}]}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan));
    }

    @Test
    public void serviceStoreConstantMappingExample()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::S_Product.all()\n" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Product {\n" +
                "               s_productId,\n" +
                "               s_productName,\n" +
                "               s_description\n" +
                "           }\n" +
                "         }#)\n" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Product {\n" +
                "               s_productId,\n" +
                "               s_productName,\n" +
                "               s_description\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(SERVICE_STORE + "\n\n" + SERVICE_STORE_CONNECTION + "\n\n" + SERVICE_STORE_MAPPING + "\n\n" + MODELS + "\n\n" + query);

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"s_productId\":\"30\",\"s_productName\":\"Product 30\",\"s_description\":\"Product 30 description\"}}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan));
    }

    @Test
    public void serviceStoreFilterExample()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::S_Product.all()\n" +
                "       ->filter(p | $p.s_productId == '30')\n" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Product {\n" +
                "               s_productId,\n" +
                "               s_productName,\n" +
                "               s_description\n" +
                "           }\n" +
                "         }#)\n" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Product {\n" +
                "               s_productId,\n" +
                "               s_productName,\n" +
                "               s_description\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(SERVICE_STORE + "\n\n" + SERVICE_STORE_CONNECTION + "\n\n" + SERVICE_STORE_MAPPING + "\n\n" + MODELS + "\n\n" + query);

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"s_productId\":\"30\",\"s_productName\":\"Product 30\",\"s_description\":\"Product 30 description\"}}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan));
    }

    @Test
    public void serviceStoreFilterWithConstantMappingExample()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::S_Product.all()\n" +
                "       ->filter(p | $p.s_description == 'product 30 description')\n" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Product {\n" +
                "               s_productId,\n" +
                "               s_productName,\n" +
                "               s_description\n" +
                "           }\n" +
                "         }#)\n" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Product {\n" +
                "               s_productId,\n" +
                "               s_productName,\n" +
                "               s_description\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(SERVICE_STORE + "\n\n" + SERVICE_STORE_CONNECTION + "\n\n" + SERVICE_STORE_MAPPING + "\n\n" + MODELS + "\n\n" + query);

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"s_productId\":\"30\",\"s_productName\":\"Product 30\",\"s_description\":\"Product 30 description\"}}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan));
    }

    @Test
    public void serviceStoreFilterOnParamExample()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {productId:String[1]|meta::external::store::service::showcase::domain::S_Product.all()\n" +
                "       ->filter(p | $p.s_productId == $productId)\n" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Product {\n" +
                "               s_productId,\n" +
                "               s_productName,\n" +
                "               s_description\n" +
                "           }\n" +
                "         }#)\n" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Product {\n" +
                "               s_productId,\n" +
                "               s_productName,\n" +
                "               s_description\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(SERVICE_STORE + "\n\n" + SERVICE_STORE_CONNECTION + "\n\n" + SERVICE_STORE_MAPPING + "\n\n" + MODELS + "\n\n" + query);

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"s_productId\":\"31\",\"s_productName\":\"Product 31\",\"s_description\":\"Product 31 description\"}}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan, Maps.mutable.with("productId", "31")));
    }

    @Test
    public void serviceStoreFilterOnMultiplePropertiesExample()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::S_Product.all()\n" +
                "       ->filter(p | $p.s_productName == 'product 30' && $p.s_description == 'product 30 description')\n" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Product {\n" +
                "               s_productId,\n" +
                "               s_productName,\n" +
                "               s_description\n" +
                "           }\n" +
                "         }#)\n" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Product {\n" +
                "               s_productId,\n" +
                "               s_productName,\n" +
                "               s_description\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(SERVICE_STORE + "\n\n" + SERVICE_STORE_CONNECTION + "\n\n" + SERVICE_STORE_MAPPING + "\n\n" + MODELS + "\n\n" + query);

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"s_productId\":\"30\",\"s_productName\":\"Product 30\",\"s_description\":\"Product 30 description\"}}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan));
    }

    @Test
    public void serviceStoreCrossStoreExample()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::S_Trade.all()\n" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Trade {\n" +
                "               s_tradeId,\n" +
                "               s_traderDetails,\n" +
                "               s_tradeDetails,\n" +
                "               s_product {\n" +
                "                   s_productId,\n" +
                "                   s_productName,\n" +
                "                   s_description\n" +
                "               }\n" +
                "           }\n" +
                "         }#)\n" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Trade {\n" +
                "               s_tradeId,\n" +
                "               s_traderDetails,\n" +
                "               s_tradeDetails,\n" +
                "               s_product {\n" +
                "                   s_productId,\n" +
                "                   s_productName,\n" +
                "                   s_description\n" +
                "               }\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(SERVICE_STORE + "\n\n" + SERVICE_STORE_CONNECTION + "\n\n" + SERVICE_STORE_MAPPING + "\n\n" + MODELS + "\n\n" + query);

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"s_tradeId\":\"1\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"30:100\",\"s_product\":{\"s_productId\":\"30\",\"s_productName\":\"Product 30\",\"s_description\":\"Product 30 description\"}},{\"s_tradeId\":\"2\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"31:200\",\"s_product\":{\"s_productId\":\"31\",\"s_productName\":\"Product 31\",\"s_description\":\"Product 31 description\"}},{\"s_tradeId\":\"3\",\"s_traderDetails\":\"abc:F_Name_2:L_Name_2\",\"s_tradeDetails\":\"30:300\",\"s_product\":{\"s_productId\":\"30\",\"s_productName\":\"Product 30\",\"s_description\":\"Product 30 description\"}},{\"s_tradeId\":\"4\",\"s_traderDetails\":\"abc:F_Name_2:L_Name_2\",\"s_tradeDetails\":\"31:400\",\"s_product\":{\"s_productId\":\"31\",\"s_productName\":\"Product 31\",\"s_description\":\"Product 31 description\"}}]}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan));
    }

    @Test
    public void serviceStoreM2MChainingExample()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::Trade.all()\n" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::Trade {\n" +
                "               tradeId,\n" +
                "               quantity\n" +
                "           }\n" +
                "         }#)\n" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::Trade {\n" +
                "               tradeId,\n" +
                "               quantity\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(SERVICE_STORE + "\n\n" + SERVICE_STORE_CONNECTION + "\n\n" + SERVICE_STORE_MAPPING + "\n\n" + MODELS + "\n\n" + query);

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"tradeId\":\"1\",\"quantity\":100},{\"tradeId\":\"2\",\"quantity\":200},{\"tradeId\":\"3\",\"quantity\":300},{\"tradeId\":\"4\",\"quantity\":400}]}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan));
    }

    @Test
    public void serviceStoreM2MChainingWithFilterExample()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::Product.all()\n" +
                "       ->filter(p | $p.productId == '30')\n" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::Product {\n" +
                "               productId,\n" +
                "               productName,\n" +
                "               description\n" +
                "           }\n" +
                "         }#)\n" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::Product {\n" +
                "               productId,\n" +
                "               productName,\n" +
                "               description\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(SERVICE_STORE + "\n\n" + SERVICE_STORE_CONNECTION + "\n\n" + SERVICE_STORE_MAPPING + "\n\n" + MODELS + "\n\n" + query);

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"productId\":\"30\",\"productName\":\"Product 30\",\"description\":\"Product 30 description\"}}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan));
    }

    @Test
    public void serviceStoreM2MChainingWithFilterOnParamExample()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {prodId:String[1]|meta::external::store::service::showcase::domain::Product.all()\n" +
                "       ->filter(p | $p.productId == $prodId)\n" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::Product {\n" +
                "               productId,\n" +
                "               productName,\n" +
                "               description\n" +
                "           }\n" +
                "         }#)\n" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::Product {\n" +
                "               productId,\n" +
                "               productName,\n" +
                "               description\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(SERVICE_STORE + "\n\n" + SERVICE_STORE_CONNECTION + "\n\n" + SERVICE_STORE_MAPPING + "\n\n" + MODELS + "\n\n" + query);

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"productId\":\"31\",\"productName\":\"Product 31\",\"description\":\"Product 31 description\"}}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan, Maps.mutable.with("prodId", "31")));
    }

    @Test
    public void serviceStoreM2MChainingWithComplexPropertyExample()
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
                "               }\n" +
                "           }\n" +
                "         }#)\n" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::Trade {\n" +
                "               tradeId,\n" +
                "               quantity,\n" +
                "               trader {\n" +
                "                   kerberos,\n" +
                "                   firstName,\n" +
                "                   lastName\n" +
                "               }\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(SERVICE_STORE + "\n\n" + SERVICE_STORE_CONNECTION + "\n\n" + SERVICE_STORE_MAPPING + "\n\n" + MODELS + "\n\n" + query);

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"tradeId\":\"1\",\"quantity\":100,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"}},{\"tradeId\":\"2\",\"quantity\":200,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"}},{\"tradeId\":\"3\",\"quantity\":300,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"}},{\"tradeId\":\"4\",\"quantity\":400,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"}}]}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan));
    }

    @Test
    public void serviceStoreM2MChainingWithCrossStoreExample()
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
                "         }#)\n" +
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

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"tradeId\":\"1\",\"quantity\":100,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"30\",\"productName\":\"Product 30\",\"description\":\"Product 30 description\"}},{\"tradeId\":\"2\",\"quantity\":200,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"31\",\"productName\":\"Product 31\",\"description\":\"Product 31 description\"}},{\"tradeId\":\"3\",\"quantity\":300,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"30\",\"productName\":\"Product 30\",\"description\":\"Product 30 description\"}},{\"tradeId\":\"4\",\"quantity\":400,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"31\",\"productName\":\"Product 31\",\"description\":\"Product 31 description\"}}]}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan));
    }
}
