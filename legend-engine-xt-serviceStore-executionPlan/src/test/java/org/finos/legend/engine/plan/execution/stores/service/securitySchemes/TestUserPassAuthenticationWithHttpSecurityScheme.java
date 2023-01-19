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
package org.finos.legend.engine.plan.execution.stores.service.securitySchemes;

import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestSuite;
import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.buildPlanForQuery;
import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.executePlan;

public class TestUserPassAuthenticationWithHttpSecurityScheme extends ServiceStoreTestSuite
{

    private static String pureGrammar;

    @BeforeClass
    public static void setup()
    {
        setupServer("securitySchemes");

        String serviceStore =
                "###ServiceStore\n" +
                        "ServiceStore meta::external::store::service::showcase::store::TradeProductServiceStore\n" +
                        "(\n" +
                        "   description : 'Showcase Service Store';\n" +
                        "   securitySchemes : {\n" +
                        "       http : Http\n" +
                        "               {\n" +
                        "                   scheme : 'basic';\n" +
                        "               }\n" +
                        "   };\n" +
                        "   ServiceGroup TradeServices\n" +
                        "   (\n" +
                        "      path : '/trades';\n" +
                        "\n" +
                        "      Service AllTradeService\n" +
                        "            (\n" +
                        "               path : '/allTradesService';\n" +
                        "               method : GET;\n" +
                        "               security : [http];\n" +
                        "               response : [meta::external::store::service::showcase::domain::S_Trade <- meta::external::store::service::showcase::store::tradeServiceStoreSchemaBinding];\n" +
                        "            )\n" +
                        "   )  \n" +
                        ")";

        String serviceStoreConnection = "###Connection\n" +
                "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                "{\n" +
                "    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                "    baseUrl : 'http://127.0.0.1:port';\n" +
                "    auth    : {\n" +
                "                 http  : UserPassword\n" +
                "                 {\n" +
                "                    username : 'username';\n" +
                "                    password : SystemPropertiesSecret\n" +
                "                    {\n" +
                "                        systemPropertyName : 'property1';\n" +
                "                    }\n" +
                "                 }\n" +
                "            };\n" +
                "}";

        pureGrammar = serviceStore + "\n\n" + serviceStoreConnection.replace("port", String.valueOf(getPort())) + "\n\n" + ServiceStoreTestUtils.readGrammarFromPureFile("/securitySchemes/testGrammar.pure");

    }

    @Test
    public void testAuthentication()
    {
        // Set the value of the password in the system properties
        System.setProperty("property1", "password");
        try
        {
            SingleExecutionPlan plan = buildPlanForQuery(pureGrammar);
            String result = executePlan(plan);
            Assert.assertEquals("{\"builder\":{\"_type\":\"json\"},\"values\":[{\"s_tradeId\":\"1\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"30:100\"},{\"s_tradeId\":\"2\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"31:200\"},{\"s_tradeId\":\"3\",\"s_traderDetails\":\"abc:F_Name_2:L_Name_2\",\"s_tradeDetails\":\"30:300\"},{\"s_tradeId\":\"4\",\"s_traderDetails\":\"abc:F_Name_2:L_Name_2\",\"s_tradeDetails\":\"31:400\"}]}", result);
        }
        finally
        {
            System.clearProperty("reference1");
        }
    }
}
