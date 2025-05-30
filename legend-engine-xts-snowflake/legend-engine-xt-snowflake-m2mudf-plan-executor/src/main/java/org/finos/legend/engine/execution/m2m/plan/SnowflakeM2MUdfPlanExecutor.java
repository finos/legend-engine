// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.execution.m2m.plan;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamToPureFormatSerializer;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.Variable;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.FunctionParametersValidationNode;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class SnowflakeM2MUdfPlanExecutor
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    public static String executeWithArgs(String plan, Object... args)
    {
        try
        {
            Map<String, Object> parameters = Maps.mutable.empty();
            SingleExecutionPlan singleExecutionPlan = objectMapper.readValue(plan, SingleExecutionPlan.class);
            if (singleExecutionPlan != null && singleExecutionPlan.rootExecutionNode != null)
            {
                List<ExecutionNode> executionNodes = singleExecutionPlan.rootExecutionNode.executionNodes;
                if (executionNodes != null && !executionNodes.isEmpty())
                {
                    if (executionNodes.get(0) instanceof FunctionParametersValidationNode)
                    {
                        List<Variable> functionParameters = ((FunctionParametersValidationNode) executionNodes.get(0)).functionParameters;
                        if (functionParameters.size() != args.length)
                        {
                            throw new EngineException("Number of function parameters does not match number of arguments");
                        }
                        int i = 0;
                        for (Variable parameter : functionParameters)
                        {
                            parameters.put(parameter.name, args[i]);
                            i++;
                        }
                    }
                }
            }
            PlanExecutor.ExecuteArgs executeArgs = PlanExecutor.ExecuteArgs.newArgs().withPlan(singleExecutionPlan).withParams(parameters).build();
            PlanExecutor executor = PlanExecutor.newPlanExecutorBuilder().isJavaCompilationAllowed(true).withAvailableStoreExecutors().build();
            try (JsonStreamingResult jsonResult = (JsonStreamingResult) executor.executeWithArgs(executeArgs))
            {
                JsonStreamToPureFormatSerializer resultStream = (JsonStreamToPureFormatSerializer) jsonResult.getSerializer(SerializationFormat.PURE);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                resultStream.stream(out);
                return out.toString();
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
