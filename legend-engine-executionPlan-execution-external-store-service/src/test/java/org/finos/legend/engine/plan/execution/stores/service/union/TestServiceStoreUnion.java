package org.finos.legend.engine.plan.execution.stores.service.union;

import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils;
import org.finos.legend.engine.plan.execution.stores.service.utils.TestServer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.junit.*;

import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.buildPlanForQuery;
import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.executePlan;

public class TestServiceStoreUnion
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
        SERVER_RESOURCE_FILE_PATH = "/union/pathContent.json";

        SERVICE_STORE = ServiceStoreTestUtils.readGrammarFromPureFile("/union/serviceStore.pure");
        SERVICE_STORE_CONNECTION =
                "###Connection\n" +
                        "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                        "{\n" +
                        "    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                        "    baseUrl : 'http://127.0.0.1:" + PORT + "';\n" +
                        "}";
        SERVICE_STORE_MAPPING = ServiceStoreTestUtils.readGrammarFromPureFile("/union/mapping.pure");
        MODELS = ServiceStoreTestUtils.readGrammarFromPureFile("/union/model.pure");
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

        SingleExecutionPlan plan = buildPlanForQuery(SERVICE_STORE + "\n\n" + SERVICE_STORE_CONNECTION + "\n\n" + SERVICE_STORE_MAPPING + "\n\n" + MODELS + "\n\n" + query);

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

        SingleExecutionPlan plan = buildPlanForQuery(SERVICE_STORE + "\n\n" + SERVICE_STORE_CONNECTION + "\n\n" + SERVICE_STORE_MAPPING + "\n\n" + MODELS + "\n\n" + query);

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

        SingleExecutionPlan plan = buildPlanForQuery(SERVICE_STORE + "\n\n" + SERVICE_STORE_CONNECTION + "\n\n" + SERVICE_STORE_MAPPING + "\n\n" + MODELS + "\n\n" + query);

        String expectedRes1 = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"s_productId\":\"31\",\"s_productName\":\"Product 31\",\"s_description\":\"Product 31 description\",\"s_synonyms\":[{\"s_name\":\"product 31 synonym 1\",\"s_type\":\"isin\"},{\"s_name\":\"product 31 synonym 2\",\"s_type\":\"cusip\"}]}}";

        Assert.assertEquals(expectedRes1, executePlan(plan, Maps.mutable.with("productId", "31")));

        String expectedRes2 = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"s_productId\":\"41\",\"s_productName\":\"Product 41\",\"s_description\":\"Product 41 description\",\"s_synonyms\":[{\"s_name\":\"product 41 synonym 1\",\"s_type\":\"isin\"},{\"s_name\":\"product 41 synonym 2\",\"s_type\":\"cusip\"}]}}";

        Assert.assertEquals(expectedRes2, executePlan(plan, Maps.mutable.with("productId", "41")));
    }
}
