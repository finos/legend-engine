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

package org.finos.legend.engine.plan.execution.stores.service.features.flags;

import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestSuite;
import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.buildPlanForQuery;
import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.executePlan;

public class TestServiceStoreParameterFlags extends ServiceStoreTestSuite
{
    private static String pureGrammar;

    @BeforeClass
    public static void setup()
    {
        setupServer("features/flags");

        String serviceStoreConnection =
                "###Connection\n" +
                        "ServiceStoreConnection meta::external::store::service::showcase::connection::serviceStoreConnection\n" +
                        "{\n" +
                        "    store   : meta::external::store::service::showcase::store::TradeProductServiceStore;\n" +
                        "    baseUrl : 'http://127.0.0.1:" + getPort() + "';\n" +
                        "}";
        pureGrammar = ServiceStoreTestUtils.readGrammarFromPureFile("/features/flags/testGrammar.pure") + "\n\n" + serviceStoreConnection;
    }

    @Test
    public void serviceStoreFilterExampleWithSpecialCharactersInQueryParamValue()
    {
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|meta::external::store::service::showcase::domain::S_Trade.all()" +
                "       ->filter(p | $p.s_traderDetails == 'abc:F_Name_1:L_Name_1')\n" +
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

        String expectedResWithEmptyList = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"s_tradeId\":\"1\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"30:100\"},{\"s_tradeId\":\"2\",\"s_traderDetails\":\"abc:F_Name_1:L_Name_1\",\"s_tradeDetails\":\"30:400\"}]}";

        Assert.assertEquals(expectedResWithEmptyList, executePlan(plan));
    }
}
