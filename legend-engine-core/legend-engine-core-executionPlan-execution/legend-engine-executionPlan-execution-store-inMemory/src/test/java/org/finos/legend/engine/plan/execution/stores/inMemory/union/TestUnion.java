//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.inMemory.union;

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.concurrent.TestConcurrentExecutionNodeExecution;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamToPureFormatSerializer;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.url.NamedInputStream;
import org.finos.legend.engine.shared.core.url.NamedInputStreamProvider;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.finos.legend.engine.plan.execution.stores.inMemory.utils.TestUtils.buildPlanForQuery;
import static org.finos.legend.engine.plan.execution.stores.inMemory.utils.TestUtils.readGrammarFromPureFile;

public class TestUnion
{
    @Test
    public void testM2MUnion()
    {
        String pureGrammar = readGrammarFromPureFile("/union/unionTest.pure");
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|demo::domain::Trade.all()\n" +
                "       ->graphFetch(#{\n" +
                "           demo::domain::Trade {\n" +
                "               tradeId,\n" +
                "               quantity,\n" +
                "               product {\n" +
                "                   productId,\n" +
                "                   productName,\n" +
                "                   description\n" +
                "               },\n" +
                "               trader {\n" +
                "                   kerberos,\n" +
                "                   firstName,\n" +
                "                   lastName\n" +
                "               }\n" +
                "           }\n" +
                "         }#)" +
                "       ->serialize(#{\n" +
                "           demo::domain::Trade {\n" +
                "               tradeId,\n" +
                "               quantity,\n" +
                "               product {\n" +
                "                   productId,\n" +
                "                   productName,\n" +
                "                   description\n" +
                "               },\n" +
                "               trader {\n" +
                "                   kerberos,\n" +
                "                   firstName,\n" +
                "                   lastName\n" +
                "               }\n" +
                "           }\n" +
                "        }#)};\n" +
                "}\n";

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query, "demo::mapping::Mapping", "demo::runtime::Runtime");
        InputStream sTrade1Stream = new ByteArrayInputStream("[  {    \"s_trade1Id\": 1,    \"s_trader1Details\": \"abc:F_Name_1:L_Name_1\",    \"s_trade1Details\": \"30:100:Product A\"  },  {    \"s_trade1Id\": 2,    \"s_trader1Details\": \"abc:F_Name_1:L_Name_1\",    \"s_trade1Details\": \"31:200:Product B\"  },  {    \"s_trade1Id\": 3,    \"s_trader1Details\": \"abc:F_Name_2:L_Name_2\",    \"s_trade1Details\": \"30:300:Product A\"  },  {    \"s_trade1Id\": 4,    \"s_trader1Details\": \"abc:F_Name_2:L_Name_2\",    \"s_trade1Details\": \"31:400:Product B\"  }]".getBytes(StandardCharsets.UTF_8));
        InputStream sTrade2Stream = new ByteArrayInputStream("[  {    \"s_trade2Id\": 5,    \"s_trader2Details\": \"abc:F_Name_1:L_Name_1\",    \"s_trade2Details\": \"30:100:Product A:Desc A\"  },  {    \"s_trade2Id\": 6,    \"s_trader2Details\": \"abc:F_Name_1:L_Name_1\",    \"s_trade2Details\": \"31:200:Product B:Desc B\"  },  {    \"s_trade2Id\": 7,    \"s_trader2Details\": \"abc:F_Name_2:L_Name_2\",    \"s_trade2Details\": \"30:300:Product A:Desc C\"  },  {    \"s_trade2Id\": 8,    \"s_trader2Details\": \"abc:F_Name_2:L_Name_2\",    \"s_trade2Details\": \"31:400:Product B:Desc D\"  }]".getBytes(StandardCharsets.UTF_8));
        List<NamedInputStream> namedInputStreamList = Lists.mutable.with(new NamedInputStream("s_trade1", sTrade1Stream), new NamedInputStream("s_trade2", sTrade2Stream));
        NamedInputStreamProvider namedInputStreamProvider = new NamedInputStreamProvider(namedInputStreamList);
        PlanExecutor.ExecuteArgs executeArgs = PlanExecutor.ExecuteArgs.newArgs()
                                                                    .withPlan(plan)
                                                                    .withInputAsStreamProvider(namedInputStreamProvider)
                                                                    .build();

        String expected = "[{\"tradeId\":1,\"quantity\":100,\"product\":{\"productId\":\"30\",\"productName\":\"Product A\",\"description\":\"\"},\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"}},{\"tradeId\":2,\"quantity\":200,\"product\":{\"productId\":\"31\",\"productName\":\"Product B\",\"description\":\"\"},\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"}},{\"tradeId\":3,\"quantity\":300,\"product\":{\"productId\":\"30\",\"productName\":\"Product A\",\"description\":\"\"},\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"}},{\"tradeId\":4,\"quantity\":400,\"product\":{\"productId\":\"31\",\"productName\":\"Product B\",\"description\":\"\"},\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"}},{\"tradeId\":5,\"quantity\":100,\"product\":{\"productId\":\"30\",\"productName\":\"Product A\",\"description\":\"Desc A\"},\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"}},{\"tradeId\":6,\"quantity\":200,\"product\":{\"productId\":\"31\",\"productName\":\"Product B\",\"description\":\"Desc B\"},\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_1\",\"lastName\":\"L_Name_1\"}},{\"tradeId\":7,\"quantity\":300,\"product\":{\"productId\":\"30\",\"productName\":\"Product A\",\"description\":\"Desc C\"},\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"}},{\"tradeId\":8,\"quantity\":400,\"product\":{\"productId\":\"31\",\"productName\":\"Product B\",\"description\":\"Desc D\"},\"trader\":{\"kerberos\":\"abc\",\"firstName\":\"F_Name_2\",\"lastName\":\"L_Name_2\"}}]";

        assertResults(TestConcurrentExecutionNodeExecution.executePlanConcurrently(executeArgs, 10, 1, 1, "[Running, pool size = 2, active threads = 0, queued tasks = 0, completed tasks = 2]"), expected);
    }

    private void assertResults(List<Result> results, String expectedResult)
    {
        for (Result result : results)
        {
            JsonStreamingResult jsonStreamingResult = (JsonStreamingResult) result;
            Assert.assertEquals(expectedResult, jsonStreamingResult.flush(new JsonStreamToPureFormatSerializer(jsonStreamingResult)));
        }
    }
}
