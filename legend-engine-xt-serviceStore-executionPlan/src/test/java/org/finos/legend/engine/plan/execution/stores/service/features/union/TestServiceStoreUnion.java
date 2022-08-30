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

package org.finos.legend.engine.plan.execution.stores.service.features.union;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.concurrent.ConcurrentExecutionNodeExecutorPool;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamToJsonDefaultSerializer;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.plan.execution.stores.inMemory.plugin.InMemory;
import org.finos.legend.engine.plan.execution.stores.service.plugin.ServiceStore;
import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestSuite;
import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.server.pac4j.kerberos.KerberosProfile;
import org.finos.legend.server.pac4j.kerberos.LocalCredentials;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.buildPlanForQuery;
import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.executePlan;

public class TestServiceStoreUnion extends ServiceStoreTestSuite
{
    private static String pureGrammar;

    @BeforeClass
    public static void setup()
    {
        setupServer("features/union");

        String serviceStoreConnection =
                "###Connection\n" +
                        "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                        "{\n" +
                        "    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                        "    baseUrl : 'http://127.0.0.1:" + getPort() + "';\n" +
                        "}\n\n" +
                        "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection2\n" +
                        "{\n" +
                        "    store   : meta::external::store::service::showcase::store::EmployeeServiceStore;\n" +
                        "    baseUrl : 'http://127.0.0.1:" + getPort() + "';\n" +
                        "}\n\n" +
                        "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection3\n" +
                        "{\n" +
                        "    store   : meta::external::store::service::showcase::store::EmployeeServiceStore1;\n" +
                        "    baseUrl : 'http://127.0.0.1:" + getPort() + "';\n" +
                        "}\n\n" +
                        "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection4\n" +
                        "{\n" +
                        "    store   : meta::external::store::service::showcase::store::EmployeeServiceStore2;\n" +
                        "    baseUrl : 'http://127.0.0.1:" + getPort() + "';\n" +
                        "}\n\n" +
                        "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection5\n" +
                        "{\n" +
                        "    store   : meta::external::store::service::showcase::store::EmployeeServiceStore3;\n" +
                        "    baseUrl : 'http://127.0.0.1:" + getPort() + "';\n" +
                        "}\n\n" +
                        "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection6\n" +
                        "{\n" +
                        "    store   : meta::external::store::service::showcase::store::EmployeeServiceStore4;\n" +
                        "    baseUrl : 'http://127.0.0.1:" + getPort() + "';\n" +
                        "}\n\n" +
                        "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection7\n" +
                        "{\n" +
                        "    store   : meta::external::store::service::showcase::store::EmployeeServiceStore5;\n" +
                        "    baseUrl : 'http://127.0.0.1:" + getPort() + "';\n" +
                        "}\n\n";
        pureGrammar = ServiceStoreTestUtils.readGrammarFromPureFile("/features/union/testGrammar.pure") + "\n\n" + serviceStoreConnection;
    }

    @Test
    public void serviceStoreUnionExample()
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

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query);

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"s_tradeId\":\"1\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"30:100\"},{\"s_tradeId\":\"2\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"31:200\"},{\"s_tradeId\":\"3\",\"s_traderDetails\":\"abc:F_Name_2:L_Name_2\",\"s_tradeDetails\":\"30:300\"},{\"s_tradeId\":\"4\",\"s_traderDetails\":\"abc:F_Name_2:L_Name_2\",\"s_tradeDetails\":\"31:400\"},{\"s_tradeId\":\"5\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"40:100\"},{\"s_tradeId\":\"6\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"41:200\"},{\"s_tradeId\":\"7\",\"s_traderDetails\":\"abc:F_Name_2:L_Name_2\",\"s_tradeDetails\":\"40:300\"},{\"s_tradeId\":\"8\",\"s_traderDetails\":\"abc:F_Name_2:L_Name_2\",\"s_tradeDetails\":\"41:400\"}]}";

        Assert.assertEquals(expectedRes, executePlan(plan));
    }

    @Test
    public void serviceStoreUnionWithNestedTree()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::S_Product.all()\n" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Product {\n" +
                "               s_productId,\n" +
                "               s_productName,\n" +
                "               s_description,\n" +
                "               s_synonyms {\n" +
                "                   s_name,\n" +
                "                   s_type\n" +
                "               }\n" +
                "           }\n" +
                "         }#)\n" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Product {\n" +
                "               s_productId,\n" +
                "               s_productName,\n" +
                "               s_description,\n" +
                "               s_synonyms {\n" +
                "                   s_name,\n" +
                "                   s_type\n" +
                "               }\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query);

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"s_productId\":\"30\",\"s_productName\":\"Product 30\",\"s_description\":\"Product 30 description\",\"s_synonyms\":[{\"s_name\":\"product 30 synonym 1\",\"s_type\":\"isin\"},{\"s_name\":\"product 30 synonym 2\",\"s_type\":\"cusip\"}]},{\"s_productId\":\"31\",\"s_productName\":\"Product 31\",\"s_description\":\"Product 31 description\",\"s_synonyms\":[{\"s_name\":\"product 31 synonym 1\",\"s_type\":\"isin\"},{\"s_name\":\"product 31 synonym 2\",\"s_type\":\"cusip\"}]},{\"s_productId\":\"40\",\"s_productName\":\"Product 40\",\"s_description\":\"Product 40 description\",\"s_synonyms\":[{\"s_name\":\"product 40 synonym 1\",\"s_type\":\"isin\"},{\"s_name\":\"product 40 synonym 2\",\"s_type\":\"cusip\"}]},{\"s_productId\":\"41\",\"s_productName\":\"Product 41\",\"s_description\":\"Product 41 description\",\"s_synonyms\":[{\"s_name\":\"product 41 synonym 1\",\"s_type\":\"isin\"},{\"s_name\":\"product 41 synonym 2\",\"s_type\":\"cusip\"}]}]}";

        Assert.assertEquals(expectedRes, executePlan(plan));
    }

    @Test
    public void serviceStoreUnionWithFilter()
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
                "               s_description,\n" +
                "               s_synonyms {\n" +
                "                   s_name,\n" +
                "                   s_type\n" +
                "               }\n" +
                "           }\n" +
                "         }#)\n" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::S_Product {\n" +
                "               s_productId,\n" +
                "               s_productName,\n" +
                "               s_description,\n" +
                "               s_synonyms {\n" +
                "                   s_name,\n" +
                "                   s_type\n" +
                "               }\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query);

        String expectedRes1 = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"s_productId\":\"31\",\"s_productName\":\"Product 31\",\"s_description\":\"Product 31 description\",\"s_synonyms\":[{\"s_name\":\"product 31 synonym 1\",\"s_type\":\"isin\"},{\"s_name\":\"product 31 synonym 2\",\"s_type\":\"cusip\"}]}}";

        Assert.assertEquals(expectedRes1, executePlan(plan, Maps.mutable.with("productId", "31")));

        String expectedRes2 = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"s_productId\":\"41\",\"s_productName\":\"Product 41\",\"s_description\":\"Product 41 description\",\"s_synonyms\":[{\"s_name\":\"product 41 synonym 1\",\"s_type\":\"isin\"},{\"s_name\":\"product 41 synonym 2\",\"s_type\":\"cusip\"}]}}";

        Assert.assertEquals(expectedRes2, executePlan(plan, Maps.mutable.with("productId", "41")));
    }

    @Test
    public void serviceStoreMultipleUnionElementsExample()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::Person.all()" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::Person {\n" +
                "               firstName,\n" +
                "               lastName,\n" +
                "               firmId\n" +
                "           }\n" +
                "         }#)" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::Person {\n" +
                "               firstName,\n" +
                "               lastName,\n" +
                "               firmId\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query);

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"firstName\":\"FirstName ServiceStore\",\"lastName\":\"LastName ServiceStore\",\"firmId\":\"ServiceStore\"},{\"firstName\":\"FirstName Model\",\"lastName\":\"LastName Model\",\"firmId\":\"Model\"}]}";

        Assert.assertEquals(expectedRes, executePlan(plan));
    }

    @Test
    public void serviceStoreUnionWithMultipleMappings()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::Person.all()" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::Person {\n" +
                "               firstName,\n" +
                "               lastName,\n" +
                "               firmId\n" +
                "           }\n" +
                "         }#)" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::Person {\n" +
                "               firstName,\n" +
                "               lastName,\n" +
                "               firmId\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query, "meta::external::store::service::showcase::mapping::ServiceStoreMapping2", "meta::external::store::service::showcase::runtime::ServiceStoreRuntime2");

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"firstName\":\"FirstName ServiceStore\",\"lastName\":\"LastName ServiceStore\",\"firmId\":\"ServiceStore\"},{\"firstName\":\"FirstName ServiceStore\",\"lastName\":\"LastName ServiceStore\",\"firmId\":\"ServiceStore\"},{\"firstName\":\"FirstName ServiceStore\",\"lastName\":\"LastName ServiceStore\",\"firmId\":\"ServiceStore\"},{\"firstName\":\"FirstName ServiceStore\",\"lastName\":\"LastName ServiceStore\",\"firmId\":\"ServiceStore\"},{\"firstName\":\"FirstName ServiceStore\",\"lastName\":\"LastName ServiceStore\",\"firmId\":\"ServiceStore\"}]}";

        Assert.assertEquals(expectedRes, executePlan(plan));
    }

    @Test
    public void serviceStoreUnionWithMultipleMappingsAndConcurrentExecution()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::Person.all()" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::Person {\n" +
                "               firstName,\n" +
                "               lastName,\n" +
                "               firmId\n" +
                "           }\n" +
                "         }#)" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::Person {\n" +
                "               firstName,\n" +
                "               lastName,\n" +
                "               firmId\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query, "meta::external::store::service::showcase::mapping::ServiceStoreMapping2", "meta::external::store::service::showcase::runtime::ServiceStoreRuntime2");

        ConcurrentExecutionNodeExecutorPool concurrentExecutionNodeExecutorPool = ConcurrentExecutionNodeExecutorPool.getOrInitializeExecutorPool(10);
        PlanExecutor planExecutor = PlanExecutor.newPlanExecutor(true, Lists.mutable.with(InMemory.build(), ServiceStore.build()), concurrentExecutionNodeExecutorPool, PlanExecutor.DEFAULT_GRAPH_FETCH_BATCH_MEMORY_LIMIT);
        PlanExecutor.ExecuteArgs executeArgs = PlanExecutor.ExecuteArgs.newArgs()
                .withPlan(plan)
                .withProfiles(Lists.mutable.with(new KerberosProfile(LocalCredentials.INSTANCE)))
                .build();
        JsonStreamingResult result = (JsonStreamingResult) planExecutor.executeWithArgs(executeArgs);

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"firstName\":\"FirstName ServiceStore\",\"lastName\":\"LastName ServiceStore\",\"firmId\":\"ServiceStore\"},{\"firstName\":\"FirstName ServiceStore\",\"lastName\":\"LastName ServiceStore\",\"firmId\":\"ServiceStore\"},{\"firstName\":\"FirstName ServiceStore\",\"lastName\":\"LastName ServiceStore\",\"firmId\":\"ServiceStore\"},{\"firstName\":\"FirstName ServiceStore\",\"lastName\":\"LastName ServiceStore\",\"firmId\":\"ServiceStore\"},{\"firstName\":\"FirstName ServiceStore\",\"lastName\":\"LastName ServiceStore\",\"firmId\":\"ServiceStore\"}]}";
        Assert.assertEquals(expectedRes, result.flush(new JsonStreamToJsonDefaultSerializer(result)));
        Assert.assertTrue(concurrentExecutionNodeExecutorPool.toString().contains("[Running, pool size = 5, active threads = 0, queued tasks = 0, completed tasks = 5]"));
    }
}
