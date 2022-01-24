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

package org.finos.legend.engine.plan.execution.stores.service.batching;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils;
import org.finos.legend.engine.plan.execution.stores.service.utils.TestServer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.buildPlanForQuery;
import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.executePlan;

public class TestServiceStoreBatching
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
        SERVER_RESOURCE_FILE_PATH = "/batching/pathContent.json";

        SERVICE_STORE = ServiceStoreTestUtils.readGrammarFromPureFile("/batching/serviceStore.pure");
        SERVICE_STORE_CONNECTION =
                "###Connection\n" +
                        "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                        "{\n" +
                        "    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                        "    baseUrl : 'http://127.0.0.1:" + PORT + "';\n" +
                        "}";
        SERVICE_STORE_MAPPING = ServiceStoreTestUtils.readGrammarFromPureFile("/batching/mapping.pure");
        MODELS = ServiceStoreTestUtils.readGrammarFromPureFile("/batching/model.pure");
    }

    @BeforeClass
    public static void setup() throws Exception
    {
        server = new TestServer(PORT, SERVER_RESOURCE_FILE_PATH, Collections.singletonList(getCustomHandler()));
    }

    private static AbstractHandler getCustomHandler()
    {
        String basePath = "/products/getProductsById";

        ContextHandler contextHandler = new ContextHandler(basePath + "/*");
        AbstractHandler handler = new AbstractHandler()
        {
            @Override
            public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException
            {
                String url = request.getRequestURL().toString();
                String path = url.substring(url.indexOf(basePath) + basePath.length() + 1);

                List<String> paramsList = Arrays.asList(java.net.URLDecoder.decode(path, StandardCharsets.UTF_8.name()).split(","));
                Set<String> params = new HashSet(paramsList);
                String content;

                Set<String> p1 = new HashSet(Arrays.asList("30,31,32,33,34,35,36,37,38,39".split(",")));
                Set<String> p2 = new HashSet(Arrays.asList("30:100,31:200,32:300,33:400,34:100,35:200,36:300,37:400,38:300,39:400".split(",")));
                if (p1.equals(params) && paramsList.size() == 10)
                {
                    content = "s_productId,s_productName,s_description\n30,Product 30,Product 30 description\n31,Product 31,Product 31 description\n32,Product 32,Product 32 description\n33,Product 33,Product 33 description\n34,Product 34,Product 34 description\n35,Product 35,Product 35 description\n36,Product 36,Product 36 description\n37,Product 37,Product 37 description\n38,Product 38,Product 38 description\n39,Product 39,Product 39 description\n40,Product 40,Product 40 description";
                }
                else if(p2.equals(params) && paramsList.size() == 10)
                {
                    content = "s_productId,s_productName,s_description\n30:100,Product 30,Product 30 description\n31:200,Product 31,Product 31 description\n32:300,Product 32,Product 32 description\n33:400,Product 33,Product 33 description\n34:100,Product 34,Product 34 description\n35:200,Product 35,Product 35 description\n36:300,Product 36,Product 36 description\n37:400,Product 37,Product 37 description\n38:300,Product 38,Product 38 description\n39:400,Product 39,Product 39 description";
                }
                else
                {
                    throw new RuntimeException("Unexpected parameters");
                }

                OutputStream stream = httpServletResponse.getOutputStream();
                stream.write(content.getBytes());
                stream.flush();
            }
        };

        contextHandler.setHandler(handler);
        return contextHandler;
    }

    @AfterClass
    public static void teardown() throws Exception
    {
        server.shutDown();
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

        SingleExecutionPlan plan = buildPlanForQuery(SERVICE_STORE + "\n\n" + SERVICE_STORE_CONNECTION + "\n\n" + SERVICE_STORE_MAPPING + "\n\n" + MODELS + "\n\n" + query);

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

        SingleExecutionPlan plan = buildPlanForQuery(SERVICE_STORE + "\n\n" + SERVICE_STORE_CONNECTION + "\n\n" + SERVICE_STORE_MAPPING + "\n\n" + MODELS + "\n\n" + query);

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"tradeId\":\"1\",\"quantity\":100,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"30\",\"productName\":\"Product 30\",\"description\":\"Product 30 description\"}},{\"tradeId\":\"2\",\"quantity\":200,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"31\",\"productName\":\"Product 31\",\"description\":\"Product 31 description\"}},{\"tradeId\":\"3\",\"quantity\":300,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"32\",\"productName\":\"Product 32\",\"description\":\"Product 32 description\"}},{\"tradeId\":\"4\",\"quantity\":400,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"33\",\"productName\":\"Product 33\",\"description\":\"Product 33 description\"}},{\"tradeId\":\"5\",\"quantity\":100,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"34\",\"productName\":\"Product 34\",\"description\":\"Product 34 description\"}},{\"tradeId\":\"6\",\"quantity\":200,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"35\",\"productName\":\"Product 35\",\"description\":\"Product 35 description\"}},{\"tradeId\":\"7\",\"quantity\":300,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"36\",\"productName\":\"Product 36\",\"description\":\"Product 36 description\"}},{\"tradeId\":\"8\",\"quantity\":400,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"37\",\"productName\":\"Product 37\",\"description\":\"Product 37 description\"}},{\"tradeId\":\"9\",\"quantity\":300,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"38\",\"productName\":\"Product 38\",\"description\":\"Product 38 description\"}},{\"tradeId\":\"10\",\"quantity\":400,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"},\"product\":{\"productId\":\"39\",\"productName\":\"Product 39\",\"description\":\"Product 39 description\"}},{\"tradeId\":\"11\",\"quantity\":200,\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"},\"product\":{\"productId\":\"35\",\"productName\":\"Product 35\",\"description\":\"Product 35 description\"}}]}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan));
    }
}
