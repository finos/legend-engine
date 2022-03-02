package org.finos.legend.engine.plan.execution.stores.service.showcase.post;

import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestSuite;
import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.buildPlanForQuery;
import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.executePlan;

public class ServiceStorePostShowcaseTest extends ServiceStoreTestSuite
{
    private static String pureGrammar;

    @BeforeClass
    public static void setup()
    {
        setupServer("showcase/post");

        String serviceStoreConnection =
                "###Connection\n" +
                        "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                        "{\n" +
                        "    store   : meta::external::store::service::showcase::store::ServiceStoreWithPostServices;\n" +
                        "    baseUrl : 'http://127.0.0.1:" + getPort() + "';\n" +
                        "}";
        pureGrammar = ServiceStoreTestUtils.readGrammarFromPureFile("/showcase/post/testGrammar.pure") + "\n\n" + serviceStoreConnection;
    }
    
    @Test
    public void serviceStorePostExampleWithStaticBody()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::Product.all()" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::Product {\n" +
                "               productId,\n" +
                "               productName,\n" +
                "               description\n" +
                "           }\n" +
                "         }#)" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::Product {\n" +
                "               productId,\n" +
                "               productName,\n" +
                "               description\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query);

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"productId\":\"30\",\"productName\":\"Product 30\",\"description\":\"Product 30 description\"}}";

        Assert.assertEquals(expectedRes, executePlan(plan));
    }

    @Test
    public void serviceStorePostExampleWithStaticList()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::Firm.all()" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::Firm {\n" +
                "               legalName,\n" +
                "               addresses\n" +
                "               {\n" +
                "                   street,\n" +
                "                   city,\n" +
                "                   country\n" +
                "               }\n" +
                "           }\n" +
                "         }#)" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::Firm {\n" +
                "               legalName,\n" +
                "               addresses\n" +
                "               {\n" +
                "                   street,\n" +
                "                   city,\n" +
                "                   country\n" +
                "               }\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query);

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"legalName\":\"Firm A\",\"addresses\":[{\"street\":\"street A 1\",\"city\":\"city A\",\"country\":\"country A\"},{\"street\":\"street A 2\",\"city\":\"city A\",\"country\":\"country A\"}]},{\"legalName\":\"Firm B\",\"addresses\":[{\"street\":\"street B 1\",\"city\":\"city B\",\"country\":\"country B\"},{\"street\":\"street B 2\",\"city\":\"city B\",\"country\":\"country B\"}]},{\"legalName\":\"Firm C\",\"addresses\":[]},{\"legalName\":\"Firm D\",\"addresses\":[{\"street\":\"street D\",\"city\":\"city D\",\"country\":\"country D\"}]}]}";

        Assert.assertEquals(expectedRes, executePlan(plan));
    }

    @Test
    public void serviceStorePostExampleWithFilter()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::Product.all()\n" +
                "       ->filter(p | $p.productName == '38142Y716')" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::Product {\n" +
                "               productId,\n" +
                "               productName,\n" +
                "               description\n" +
                "           }\n" +
                "         }#)" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::Product {\n" +
                "               productId,\n" +
                "               productName,\n" +
                "               description\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query);

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"productId\":\"30\",\"productName\":\"Product 30\",\"description\":\"Product 30 description\"}}";

        Assert.assertEquals(expectedRes, executePlan(plan));
    }

    @Test
    public void serviceStorePostExampleWithParameterizedQuery()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {name:String[1]|meta::external::store::service::showcase::domain::Firm.all()" +
                "       ->filter(p | $p.legalName == $name)" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::Firm {\n" +
                "               legalName,\n" +
                "               addresses\n" +
                "               {\n" +
                "                   street,\n" +
                "                   city,\n" +
                "                   country\n" +
                "               }\n" +
                "           }\n" +
                "         }#)" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::Firm {\n" +
                "               legalName,\n" +
                "               addresses\n" +
                "               {\n" +
                "                   street,\n" +
                "                   city,\n" +
                "                   country\n" +
                "               }\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query);

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"legalName\":\"Firm A\",\"addresses\":[{\"street\":\"street A 1\",\"city\":\"city A\",\"country\":\"country A\"},{\"street\":\"street A 2\",\"city\":\"city A\",\"country\":\"country A\"}]}}";

        Assert.assertEquals(expectedRes, executePlan(plan, Maps.mutable.with("name", "Firm A")));

        String expectedRes1 = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"legalName\":\"Firm C\",\"addresses\":[]}}";

        Assert.assertEquals(expectedRes1, executePlan(plan, Maps.mutable.with("name", "Firm C")));
    }

    @Test
    public void serviceStorePostExampleWithNesting()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::Trade.all()\n" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::Trade {\n" +
                "               id,\n" +
                "               quantity,\n" +
                "               trader\n" +
                "               {\n" +
                "                   kerberos\n," +
                "                   id" +
                "               }\n" +
                "           }\n" +
                "         }#)" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::Trade {\n" +
                "               id,\n" +
                "               quantity,\n" +
                "               trader\n" +
                "               {\n" +
                "                   kerberos\n," +
                "                   id" +
                "               }\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query);

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"id\":\"1\",\"quantity\":100,\"trader\":{\"kerberos\":\"abc\",\"id\":1234}}}";

        Assert.assertEquals(expectedRes, executePlan(plan));
    }

    @Test
    public void serviceStorePostExampleWithCrossStore()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::Firm.all()" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::Firm {\n" +
                "               legalName,\n" +
                "               addresses\n" +
                "               {\n" +
                "                   street,\n" +
                "                   city,\n" +
                "                   country\n" +
                "               },\n" +
                "               employees\n" +
                "               {\n" +
                "                   firstName,\n" +
                "                   lastName,\n" +
                "                   kerberos,\n" +
                "                   designation,\n" +
                "                   id,\n" +
                "                   age,\n" +
                "                   addresses\n" +
                "                   {\n" +
                "                       street,\n" +
                "                       city,\n" +
                "                       country\n" +
                "                   }\n" +
                "               }\n" +
                "           }\n" +
                "         }#)" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::Firm {\n" +
                "               legalName,\n" +
                "               addresses\n" +
                "               {\n" +
                "                   street,\n" +
                "                   city,\n" +
                "                   country\n" +
                "               },\n" +
                "               employees\n" +
                "               {\n" +
                "                   firstName,\n" +
                "                   lastName,\n" +
                "                   kerberos,\n" +
                "                   designation,\n" +
                "                   id,\n" +
                "                   age,\n" +
                "                   addresses\n" +
                "                   {\n" +
                "                       street,\n" +
                "                       city,\n" +
                "                       country\n" +
                "                   }\n" +
                "               }\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query);

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"legalName\":\"Firm A\",\"addresses\":[{\"street\":\"street A 1\",\"city\":\"city A\",\"country\":\"country A\"},{\"street\":\"street A 2\",\"city\":\"city A\",\"country\":\"country A\"}],\"employees\":[{\"firstName\":\"Person A1 firstName\",\"lastName\":\"Person A1 lastName\",\"kerberos\":\"Person A1 kerberos\",\"designation\":\"Person A1 designation\",\"id\":\"Person A1 id\",\"age\":12,\"addresses\":[{\"street\":\"Person A1 street 1\",\"city\":\"Person A1 city\",\"country\":\"Person A1 country\"},{\"street\":\"Person A1 street 2\",\"city\":\"Person A1 city\",\"country\":\"Person A1 country\"}]}]},{\"legalName\":\"Firm B\",\"addresses\":[{\"street\":\"street B 1\",\"city\":\"city B\",\"country\":\"country B\"},{\"street\":\"street B 2\",\"city\":\"city B\",\"country\":\"country B\"}],\"employees\":[{\"firstName\":\"Person B1 firstName\",\"lastName\":\"Person B1 lastName\",\"kerberos\":\"Person B1 kerberos\",\"designation\":\"Person B1 designation\",\"id\":\"Person B1 id\",\"age\":12,\"addresses\":[{\"street\":\"Person B1 street 1\",\"city\":\"Person B1 city\",\"country\":\"Person B1 country\"},{\"street\":\"Person B1 street 2\",\"city\":\"Person B1 city\",\"country\":\"Person B1 country\"}]}]},{\"legalName\":\"Firm C\",\"addresses\":[],\"employees\":[{\"firstName\":\"Person C1 firstName\",\"lastName\":\"Person C1 lastName\",\"kerberos\":\"Person C1 kerberos\",\"designation\":\"Person C1 designation\",\"id\":\"Person C1 id\",\"age\":12,\"addresses\":[{\"street\":\"Person C1 street 1\",\"city\":\"Person C1 city\",\"country\":\"Person C1 country\"},{\"street\":\"Person C1 street 2\",\"city\":\"Person C1 city\",\"country\":\"Person C1 country\"}]}]},{\"legalName\":\"Firm D\",\"addresses\":[{\"street\":\"street D\",\"city\":\"city D\",\"country\":\"country D\"}],\"employees\":[{\"firstName\":\"Person D1 firstName\",\"lastName\":\"Person D1 lastName\",\"kerberos\":\"Person D1 kerberos\",\"designation\":\"Person D1 designation\",\"id\":\"Person D1 id\",\"age\":12,\"addresses\":[{\"street\":\"Person D1 street 1\",\"city\":\"Person D1 city\",\"country\":\"Person D1 country\"},{\"street\":\"Person D1 street 2\",\"city\":\"Person D1 city\",\"country\":\"Person D1 country\"}]}]}]}";

        Assert.assertEquals(expectedRes, executePlan(plan));
    }

    @Test
    public void serviceStorePostExampleWithListProperties()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {firstName:String[1], lastName:String[1], id:String[1], designation:String[1]|meta::external::store::service::showcase::domain::Employee.all()\n" +
                "       ->filter(e | $e.firstName == $firstName && $e.lastName == $lastName && $e.designation == $designation && $e.id == $id)\n" +
                "       ->graphFetch(#{\n" +
                "           meta::external::store::service::showcase::domain::Employee {\n" +
                "               firstName,\n" +
                "               lastName,\n" +
                "               kerberos,\n" +
                "               designation,\n" +
                "               id,\n" +
                "               age,\n" +
                "               addresses\n" +
                "               {\n" +
                "                   street,\n" +
                "                   city,\n" +
                "                   country\n" +
                "               }\n" +
                "           }\n" +
                "         }#)" +
                "       ->serialize(#{\n" +
                "           meta::external::store::service::showcase::domain::Employee {\n" +
                "               firstName,\n" +
                "               lastName,\n" +
                "               kerberos,\n" +
                "               designation,\n" +
                "               id,\n" +
                "               age,\n" +
                "               addresses\n" +
                "               {\n" +
                "                   street,\n" +
                "                   city,\n" +
                "                   country\n" +
                "               }\n" +
                "           }\n" +
                "        }#)};\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query);

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"firstName\":\"firstName A\",\"lastName\":\"lastName A\",\"kerberos\":\"kerberos A\",\"designation\":\"designation A\",\"id\":\"id A\",\"age\":35,\"addresses\":[{\"street\":\"street 1\",\"city\":\"city\",\"country\":\"country\"},{\"street\":\"street 2\",\"city\":\"city\",\"country\":\"country\"}]}}";

        Assert.assertEquals(expectedRes, executePlan(plan, Maps.mutable.with("firstName", "firstName A", "lastName", "lastName A", "designation", "designation A", "id", "id A")));
    }
}
