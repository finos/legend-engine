// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.inMemory.autoMapping;

import junit.framework.Assert;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamToPureFormatSerializer;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.finos.legend.engine.plan.execution.stores.inMemory.utils.TestUtils.buildPlanForQuery;
import static org.finos.legend.engine.plan.execution.stores.inMemory.utils.TestUtils.readGrammarFromPureFile;

public class TestComplexPropertyMappingWithoutExplicitClassMapping
{
    private final PlanExecutor planExecutor = PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build();

    @Test
    public void testComplexPropertyMappingWithoutExplicitClassMapping()
    {
        String pureGrammar = readGrammarFromPureFile("/autoMapping/complexPropertyAutoMappingTest.pure");
        String query = "###Pure\n" +
                "function showcase::query(): Any[1]\n" +
                "{\n" +
                "   {|test::legend::autoMapping::model::Person.all()\n" +
                "       ->graphFetch(#{\n" +
                "           test::legend::autoMapping::model::Person {\n" +
                "               firstName,\n" +
                "               lastName,\n" +
                "               firm {\n" +
                "                   id\n" +
                "               },\n" +
                "               addresses {\n" +
                "                   street\n" +
                "               }\n" +
                "           }\n" +
                "         }#)" +
                "       ->serialize(#{\n" +
                "           test::legend::autoMapping::model::Person {\n" +
                "               firstName,\n" +
                "               lastName,\n" +
                "               firm {\n" +
                "                   id\n" +
                "               },\n" +
                "               addresses {\n" +
                "                   street\n" +
                "               }\n" +
                "           }\n" +
                "        }#)};\n" +
                "}\n";

        SingleExecutionPlan plan = buildPlanForQuery(pureGrammar + "\n\n" + query, "test::legend::autoMapping::mapping::TestComplexTypeAutoMapping", "test::legend::autoMapping::runtime::TestComplexTypeAutoMapping");
        InputStream personStream = new ByteArrayInputStream("{\"fullName\":\"John Doe\", \"firm\" : {\"id\" : \"Firm A\", \"address\" : {\"street\": \"A\"}}, \"address1\": {\"street\": \"Person Street A\"}, \"address2\": {\"street\": \"Person Street B\"}}".getBytes(StandardCharsets.UTF_8));
        PlanExecutor.ExecuteArgs executeArgs = PlanExecutor.ExecuteArgs.newArgs()
                .withPlan(plan)
                .withInputAsStream(personStream)
                .build();

        String expected = "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"firm\":{\"id\":\"Firm A\"},\"addresses\":[{\"street\":\"Person Street A\"},{\"street\":\"Person Street B\"}]}";

        JsonStreamingResult result = ((JsonStreamingResult) planExecutor.executeWithArgs(executeArgs));
        Assert.assertEquals(expected, result.flush(new JsonStreamToPureFormatSerializer(result)));
    }
}
