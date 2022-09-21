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

package org.finos.legend.engine.plan.execution.stores.service.features.batching;

import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestSuite;
import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.buildPlanForQuery;
import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.executePlan;

public class TestServiceStoreBatching extends ServiceStoreTestSuite
{
    private static String pureGrammar;

    @BeforeClass
    public static void setup()
    {
        setupServer("features/batching");

        String serviceStoreConnection =
                "###Connection\n" +
                        "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                        "{\n" +
                        "    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                        "    baseUrl : 'http://127.0.0.1:" + getPort() + "';\n" +
                        "}";
        pureGrammar = ServiceStoreTestUtils.readGrammarFromPureFile("/features/batching/testGrammar.pure") + "\n\n" + serviceStoreConnection;
    }

    @Test
    public void serviceStoreBatching()
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
                "         }#, 100)\n" +
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

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query);

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"s_tradeId\":\"1\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"30:100\",\"s_product\":{\"s_productId\":\"30:100\",\"s_productName\":\"Product 30\",\"s_description\":\"Product 30 description\"}},{\"s_tradeId\":\"2\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"31:200\",\"s_product\":{\"s_productId\":\"31:200\",\"s_productName\":\"Product 31\",\"s_description\":\"Product 31 description\"}},{\"s_tradeId\":\"3\",\"s_traderDetails\":\"abc:F_Name_2:L_Name_2\",\"s_tradeDetails\":\"32:300\",\"s_product\":{\"s_productId\":\"32:300\",\"s_productName\":\"Product 32\",\"s_description\":\"Product 32 description\"}},{\"s_tradeId\":\"4\",\"s_traderDetails\":\"abc:F_Name_2:L_Name_2\",\"s_tradeDetails\":\"33:400\",\"s_product\":{\"s_productId\":\"33:400\",\"s_productName\":\"Product 33\",\"s_description\":\"Product 33 description\"}},{\"s_tradeId\":\"5\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"34:100\",\"s_product\":{\"s_productId\":\"34:100\",\"s_productName\":\"Product 34\",\"s_description\":\"Product 34 description\"}},{\"s_tradeId\":\"6\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"35:200\",\"s_product\":{\"s_productId\":\"35:200\",\"s_productName\":\"Product 35\",\"s_description\":\"Product 35 description\"}},{\"s_tradeId\":\"7\",\"s_traderDetails\":\"abc:F_Name_2:L_Name_2\",\"s_tradeDetails\":\"36:300\",\"s_product\":{\"s_productId\":\"36:300\",\"s_productName\":\"Product 36\",\"s_description\":\"Product 36 description\"}},{\"s_tradeId\":\"8\",\"s_traderDetails\":\"abc:F_Name_2:L_Name_2\",\"s_tradeDetails\":\"37:400\",\"s_product\":{\"s_productId\":\"37:400\",\"s_productName\":\"Product 37\",\"s_description\":\"Product 37 description\"}},{\"s_tradeId\":\"9\",\"s_traderDetails\":\"abc:F_Name_2:L_Name_2\",\"s_tradeDetails\":\"38:300\",\"s_product\":{\"s_productId\":\"38:300\",\"s_productName\":\"Product 38\",\"s_description\":\"Product 38 description\"}},{\"s_tradeId\":\"10\",\"s_traderDetails\":\"abc:F_Name_2:L_Name_2\",\"s_tradeDetails\":\"39:400\",\"s_product\":{\"s_productId\":\"39:400\",\"s_productName\":\"Product 39\",\"s_description\":\"Product 39 description\"}},{\"s_tradeId\":\"11\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"35:200\",\"s_product\":{\"s_productId\":\"35:200\",\"s_productName\":\"Product 35\",\"s_description\":\"Product 35 description\"}}]}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan));
    }

    @Test
    public void serviceStoreBatchingWithM2MChaining()
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
                "         }#, 100)\n" +
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

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query);

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"tradeId\":\"1\",\"quantity\":100,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"30\",\"productName\":\"Product 30\",\"description\":\"Product 30 description\"}},{\"tradeId\":\"2\",\"quantity\":200,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"31\",\"productName\":\"Product 31\",\"description\":\"Product 31 description\"}},{\"tradeId\":\"3\",\"quantity\":300,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"32\",\"productName\":\"Product 32\",\"description\":\"Product 32 description\"}},{\"tradeId\":\"4\",\"quantity\":400,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"33\",\"productName\":\"Product 33\",\"description\":\"Product 33 description\"}},{\"tradeId\":\"5\",\"quantity\":100,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"34\",\"productName\":\"Product 34\",\"description\":\"Product 34 description\"}},{\"tradeId\":\"6\",\"quantity\":200,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"35\",\"productName\":\"Product 35\",\"description\":\"Product 35 description\"}},{\"tradeId\":\"7\",\"quantity\":300,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"36\",\"productName\":\"Product 36\",\"description\":\"Product 36 description\"}},{\"tradeId\":\"8\",\"quantity\":400,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"37\",\"productName\":\"Product 37\",\"description\":\"Product 37 description\"}},{\"tradeId\":\"9\",\"quantity\":300,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"38\",\"productName\":\"Product 38\",\"description\":\"Product 38 description\"}},{\"tradeId\":\"10\",\"quantity\":400,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"39\",\"productName\":\"Product 39\",\"description\":\"Product 39 description\"}},{\"tradeId\":\"11\",\"quantity\":200,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"35\",\"productName\":\"Product 35\",\"description\":\"Product 35 description\"}}]}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan));
    }

    @Test
    public void serviceStoreBatchingWithMultiplePropertiesInCrossRelationship()
    {
        String serviceStoreConnection =
                "###Connection\n" +
                        "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                        "{\n" +
                        "    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                        "    baseUrl : 'http://127.0.0.1:" + getPort() + "';\n" +
                        "}";
        String pureGrammar = ServiceStoreTestUtils.readGrammarFromPureFile("/features/batching/testGrammar2.pure") + "\n\n" + serviceStoreConnection;

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
                "         }#, 100)\n" +
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

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query, "meta::external::store::service::showcase::mapping::ServiceStoreMapping", "meta::external::store::service::showcase::runtime::ServiceStoreRuntime");

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"tradeId\":\"1\",\"quantity\":\"100\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"30\",\"productName\":\"Product 30\",\"description\":\"Product 30 description\"}},{\"tradeId\":\"2\",\"quantity\":\"200\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"31\",\"productName\":\"Product 31\",\"description\":\"Product 31 description\"}},{\"tradeId\":\"3\",\"quantity\":\"300\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"32\",\"productName\":\"Product 32\",\"description\":\"Product 32 description\"}},{\"tradeId\":\"4\",\"quantity\":\"400\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"33\",\"productName\":\"Product 33\",\"description\":\"Product 33 description\"}},{\"tradeId\":\"5\",\"quantity\":\"100\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"34\",\"productName\":\"Product 34\",\"description\":\"Product 34 description\"}},{\"tradeId\":\"6\",\"quantity\":\"200\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"35\",\"productName\":\"Product 35\",\"description\":\"Product 35 description\"}},{\"tradeId\":\"7\",\"quantity\":\"300\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"36\",\"productName\":\"Product 36\",\"description\":\"Product 36 description\"}},{\"tradeId\":\"8\",\"quantity\":\"400\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"37\",\"productName\":\"Product 37\",\"description\":\"Product 37 description\"}},{\"tradeId\":\"9\",\"quantity\":\"300\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"38\",\"productName\":\"Product 38\",\"description\":\"Product 38 description\"}},{\"tradeId\":\"10\",\"quantity\":\"400\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"39\",\"productName\":\"Product 39\",\"description\":\"Product 39 description\"}},{\"tradeId\":\"11\",\"quantity\":\"200\",\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"35\",\"productName\":\"Product 35\",\"description\":\"Product 35 description\"}}]}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan));
    }


}
