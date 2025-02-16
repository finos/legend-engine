// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.repl.dataCube.server.handler;

import com.sun.net.httpserver.HttpHandler;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.m3.function.LambdaFunction;
import org.finos.legend.engine.repl.dataCube.server.DataCubeHelpers;
import org.finos.legend.engine.repl.dataCube.server.model.DataCubeExecutionInput;
import org.finos.legend.engine.repl.dataCube.server.model.DataCubeExecutionResult;
import org.finos.legend.engine.repl.dataCube.server.model.DataCubeGetExecutionPlanInput;
import org.finos.legend.engine.repl.dataCube.server.model.DataCubeGetExecutionPlanResult;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.finos.legend.engine.repl.dataCube.server.DataCubeHelpers.executeQuery;
import static org.finos.legend.engine.repl.dataCube.server.DataCubeHelpers.getExecutionPlan;
import static org.finos.legend.engine.repl.dataCube.server.REPLServerHelpers.*;

public class DataCubeQueryExecutor
{
    public static class ExecuteQuery implements DataCubeServerHandler
    {
        @Override
        public HttpHandler getHandler(REPLServerState state)
        {
            return exchange ->
            {
                if ("POST".equals(exchange.getRequestMethod()))
                {
                    try
                    {
                        InputStreamReader inputStreamReader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                        BufferedReader bufferReader = new BufferedReader(inputStreamReader);
                        String requestBody = bufferReader.lines().collect(Collectors.joining());
                        DataCubeExecutionInput input = state.objectMapper.readValue(requestBody, DataCubeExecutionInput.class);
                        boolean debug = input.debug != null && input.debug;
                        LambdaFunction lambda = input.query;
                        PureModelContextData data = DataCubeHelpers.injectNewFunction(input.model != null ? input.model : state.getCurrentPureModelContextData(), lambda).getOne();
                        DataCubeExecutionResult result = executeQuery(state.client, state.legendInterface, state.planExecutor, data, debug);
                        handleJSONResponse(exchange, 200, state.objectMapper.writeValueAsString(result), state);
                    }
                    catch (Exception e)
                    {
                        handleTextResponse(exchange, 500, e.getMessage(), state);
                    }
                }
            };
        }
    }

    public static class GetExecutionPlan implements DataCubeServerHandler
    {
        @Override
        public HttpHandler getHandler(REPLServerState state)
        {
            return exchange ->
            {
                if ("POST".equals(exchange.getRequestMethod()))
                {
                    try
                    {
                        InputStreamReader inputStreamReader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                        BufferedReader bufferReader = new BufferedReader(inputStreamReader);
                        String requestBody = bufferReader.lines().collect(Collectors.joining());
                        DataCubeGetExecutionPlanInput input = state.objectMapper.readValue(requestBody, DataCubeGetExecutionPlanInput.class);
                        boolean debug = input.debug != null && input.debug;
                        LambdaFunction lambda = input.query;
                        PureModelContextData model = DataCubeHelpers.injectNewFunction(input.model != null ? input.model : state.getCurrentPureModelContextData(), lambda).getOne();
                        DataCubeGetExecutionPlanResult result = getExecutionPlan(state.client, state.legendInterface, model, debug);
                        handleJSONResponse(exchange, 200, state.objectMapper.writeValueAsString(result), state);
                    }
                    catch (Exception e)
                    {
                        handleTextResponse(exchange, 500, e.getMessage(), state);
                    }
                }
            };
        }
    }
}
