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

import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamToPureFormatSerializer;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.javaCompiler.EngineJavaCompiler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class SnowflakeM2MUdfPlanExecutor
{
    public static String executeSnowflakeM2MUdfPlanWithArg(SingleExecutionPlan singleExecutionPlan, EngineJavaCompiler engineJavaCompiler, String parameter, Object arg)
    {
        Map<String, Object> parameters = Maps.mutable.empty();
        parameters.put(parameter,arg);
        PlanExecutor.ExecuteArgs executeArgs = PlanExecutor.ExecuteArgs.newArgs().withPlan(singleExecutionPlan).withParams(parameters).build();
        PlanExecutor executor = PlanExecutor.newPlanExecutorBuilder().isJavaCompilationAllowed(false).withAvailableStoreExecutors().build();
        try (JavaHelper.ThreadContextClassLoaderScope scope = engineJavaCompiler == null ? null : JavaHelper.withCurrentThreadContextClassLoader(engineJavaCompiler.getClassLoader()))
        {
            JsonStreamingResult jsonResult = (JsonStreamingResult) executor.executeWithArgs(executeArgs);
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
}
