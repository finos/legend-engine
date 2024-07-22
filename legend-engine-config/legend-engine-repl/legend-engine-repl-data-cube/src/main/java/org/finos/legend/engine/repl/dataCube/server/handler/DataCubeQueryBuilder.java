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
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.repl.autocomplete.CompletionResult;
import org.finos.legend.engine.repl.dataCube.server.DataCubeHelpers;
import org.finos.legend.engine.repl.dataCube.server.model.*;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.finos.legend.engine.repl.dataCube.server.REPLServerHelpers.*;

public class DataCubeQueryBuilder
{
    public static class ParseQuery implements DataCubeServerHandler
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
                        DataCubeParseQueryInput input = state.objectMapper.readValue(requestBody, DataCubeParseQueryInput.class);
                        ValueSpecification result = DataCubeHelpers.parseQuery(input.code, input.returnSourceInformation);
                        handleResponse(exchange, 200, state.objectMapper.writeValueAsString(result), state);
                    }
                    catch (Exception e)
                    {
                        handleResponse(exchange, 400, e instanceof EngineException ? state.objectMapper.writeValueAsString(e) : e.getMessage(), state);
                    }
                }
            };
        }
    }

    public static class GetQueryCode implements DataCubeServerHandler
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
                        DataCubeGetQueryCodeInput input = state.objectMapper.readValue(requestBody, DataCubeGetQueryCodeInput.class);
                        handleResponse(exchange, 200, DataCubeHelpers.getQueryCode(input.query, input.pretty), state);
                    }
                    catch (Exception e)
                    {
                        handleResponse(exchange, 400, e.getMessage(), state);
                    }
                }
            };
        }
    }

    public static class GetQueryCodeBatch implements DataCubeServerHandler
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
                        DataCubeGetQueryCodeBatchInput input = state.objectMapper.readValue(requestBody, DataCubeGetQueryCodeBatchInput.class);
                        DataCubeGetQueryCodeBatchResult result = new DataCubeGetQueryCodeBatchResult();
                        MapAdapter.adapt(input.queries).forEachKeyValue((key, value) ->
                        {
                            try
                            {
                                result.queries.put(key, DataCubeHelpers.getQueryCode(value, input.pretty));
                            }
                            catch (Exception e)
                            {
                                result.queries.put(key, null);
                            }
                        });
                        handleResponse(exchange, 200, state.objectMapper.writeValueAsString(result), state);
                    }
                    catch (Exception e)
                    {
                        handleResponse(exchange, 400, e.getMessage(), state);
                    }
                }
            };
        }
    }

    public static class QueryTypeahead implements DataCubeServerHandler
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
                        DataCubeQueryTypeaheadInput input = state.objectMapper.readValue(requestBody, DataCubeQueryTypeaheadInput.class);
                        PureModelContextData data = state.getCurrentPureModelContextData();
                        CompletionResult result = DataCubeHelpers.getCodeTypeahead(input.code, input.isPartial, data, state.client.getCompleterExtensions());
                        handleResponse(exchange, 200, state.objectMapper.writeValueAsString(result.getCompletion()), state);
                    }
                    catch (Exception e)
                    {
                        handleResponse(exchange, 500, e.getMessage(), state);
                    }
                }
            };
        }
    }

    public static class GetRelationReturnType implements DataCubeServerHandler
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
                        DataCubeGetQueryRelationReturnTypeInput input = state.objectMapper.readValue(requestBody, DataCubeGetQueryRelationReturnTypeInput.class);
                        Lambda lambda = input.query; // if no lambda is specified, we're executing the initial query
                        PureModelContextData data = DataCubeHelpers.injectNewFunction(state.getCurrentPureModelContextData(), lambda).getOne();
                        handleResponse(exchange, 200, state.objectMapper.writeValueAsString(DataCubeHelpers.getRelationReturnType(state.legendInterface, data)), state);
                    }
                    catch (Exception e)
                    {
                        handleResponse(exchange, 500, e instanceof EngineException ? state.objectMapper.writeValueAsString(e) : e.getMessage(), state);
                    }
                }
            };
        }
    }

    public static class GetBaseQuery implements DataCubeServerHandler
    {
        @Override
        public HttpHandler getHandler(REPLServerState state)
        {
            return exchange ->
            {
                if ("GET".equals(exchange.getRequestMethod()))
                {
                    try
                    {
                        DataCubeQuery query = state.getQuery();
                        if (query != null)
                        {
                            DataCubeGetBaseQueryResult result = new DataCubeGetBaseQueryResult();
                            result.timestamp = state.startTime;
                            result.query = query;
                            result.partialQuery = DataCubeHelpers.parseQuery(query.partialQuery, false);
                            result.sourceQuery = DataCubeHelpers.parseQuery(query.source.query, false);
                            handleResponse(exchange, 200, state.objectMapper.writeValueAsString(result), state);
                        }
                        else
                        {
                            throw new RuntimeException("DataCube base query has not been set!");
                        }
                    }
                    catch (Exception e)
                    {
                        handleResponse(exchange, 500, e.getMessage(), state);
                    }
                }
            };
        }
    }
}
