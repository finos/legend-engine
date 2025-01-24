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
import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.impl.map.mutable.MapAdapter;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.m3.relation.RelationType;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.repl.autocomplete.CompletionResult;
import org.finos.legend.engine.repl.dataCube.server.DataCubeHelpers;
import org.finos.legend.engine.repl.dataCube.server.model.*;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

import static org.finos.legend.engine.repl.dataCube.server.REPLServerHelpers.*;
import static org.finos.legend.engine.repl.shared.ExecutionHelper.REPL_RUN_FUNCTION_QUALIFIED_PATH;
import static org.finos.legend.engine.repl.shared.ExecutionHelper.REPL_RUN_FUNCTION_SIGNATURE;

public class DataCubeQueryBuilder
{
    public static class ParseValueSpecification implements DataCubeServerHandler
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
                        handleJSONResponse(exchange, 200, state.objectMapper.writeValueAsString(result), state);
                    }
                    catch (Exception e)
                    {
                        if (e instanceof EngineException)
                        {
                            handleJSONResponse(exchange, 400, state.objectMapper.writeValueAsString(new DataCubeQueryBuilderError((EngineException) e)), state);
                        }
                        else
                        {
                            handleTextResponse(exchange, 500, e.getMessage(), state);
                        }
                    }
                }
            };
        }
    }

    public static class GetValueSpecificationCode implements DataCubeServerHandler
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
                        DataCubeGetValueSpecificationCodeInput input = state.objectMapper.readValue(requestBody, DataCubeGetValueSpecificationCodeInput.class);
                        handleTextResponse(exchange, 200, DataCubeHelpers.getQueryCode(input.value, input.pretty), state);
                    }
                    catch (Exception e)
                    {
                        handleTextResponse(exchange, 400, e.getMessage(), state);
                    }
                }
            };
        }
    }

    public static class GetValueSpecificationCodeBatch implements DataCubeServerHandler
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
                        DataCubeGetValueSpecificationCodeBatchInput input = state.objectMapper.readValue(requestBody, DataCubeGetValueSpecificationCodeBatchInput.class);
                        DataCubeGetQueryCodeBatchResult result = new DataCubeGetQueryCodeBatchResult();
                        MapAdapter.adapt(input.values).forEachKeyValue((key, value) ->
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
                        handleJSONResponse(exchange, 200, state.objectMapper.writeValueAsString(result), state);
                    }
                    catch (Exception e)
                    {
                        handleTextResponse(exchange, 400, e.getMessage(), state);
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
                        CompletionResult result = DataCubeHelpers.getCodeTypeahead(input.code, input.baseQuery, input.model != null ? input.model : state.getCurrentPureModelContextData(), state.client.getCompleterExtensions(), state.legendInterface);
                        handleJSONResponse(exchange, 200, state.objectMapper.writeValueAsString(result.getCompletion()), state);
                    }
                    catch (Exception e)
                    {
                        handleTextResponse(exchange, 500, e.getMessage(), state);
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
                        handleJSONResponse(exchange, 200, state.objectMapper.writeValueAsString(DataCubeHelpers.getRelationReturnType(state.legendInterface, input.query, input.model != null ? input.model : state.getCurrentPureModelContextData())), state);
                    }
                    catch (Exception e)
                    {
                        if (e instanceof EngineException)
                        {
                            handleJSONResponse(exchange, 500, state.objectMapper.writeValueAsString(new DataCubeQueryBuilderError((EngineException) e)), state);
                        }
                        else
                        {
                            handleTextResponse(exchange, 500, e.getMessage(), state);
                        }
                    }
                }
            };
        }
    }

    public static class GetQueryCodeRelationReturnType implements DataCubeServerHandler
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
                        DataCubeGetQueryCodeRelationReturnTypeInput input = state.objectMapper.readValue(requestBody, DataCubeGetQueryCodeRelationReturnTypeInput.class);

                        String graphCode = "";
                        PureModelContextData currentData = input.model != null ? input.model : state.getCurrentPureModelContextData();
                        PureModelContextData newData = PureModelContextData.newBuilder()
                                .withOrigin(currentData.getOrigin())
                                .withSerializer(currentData.getSerializer())
                                .withElements(ListIterate.select(currentData.getElements(), el -> !el.getPath().equals(REPL_RUN_FUNCTION_QUALIFIED_PATH)))
                                .build();
                        graphCode += PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build()).renderPureModelContextData(newData);
                        graphCode += "\n###Pure\n";
                        graphCode += "function " + REPL_RUN_FUNCTION_SIGNATURE + "{\n";
                        graphCode += DataCubeHelpers.getQueryCode(input.baseQuery.body.get(0), false) + "\n";
                        int lineOffset = StringUtils.countMatches(graphCode, "\n");
                        graphCode += input.code;
                        graphCode += "\n}";

                        try
                        {
                            PureModelContextData data = PureGrammarParser.newInstance().parseModel(graphCode);
                            RelationType relationType = DataCubeHelpers.getRelationReturnType(state.legendInterface, data);
                            handleJSONResponse(exchange, 200, state.objectMapper.writeValueAsString(relationType), state);
                        }
                        catch (EngineException e)
                        {
                            SourceInformation sourceInformation = e.getSourceInformation();
                            sourceInformation.startLine -= lineOffset;
                            sourceInformation.endLine -= lineOffset;
                            handleJSONResponse(exchange, 400, state.objectMapper.writeValueAsString(new DataCubeQueryBuilderError(new EngineException(e.getMessage(), sourceInformation, e.getErrorType()))), state);
                        }
                    }
                    catch (Exception e)
                    {
                        if (e instanceof EngineException)
                        {
                            handleJSONResponse(exchange, 500, state.objectMapper.writeValueAsString(new DataCubeQueryBuilderError((EngineException) e)), state);
                        }
                        else
                        {
                            handleTextResponse(exchange, 500, e.getMessage(), state);
                        }
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
                        String query = state.getQuery();
                        Map<String, ?> configuration = state.getQueryConfiguration();
                        Map<String, ?> source = state.getQuerySource();
                        if (query != null)
                        {
                            DataCubeGetBaseQueryResult result = new DataCubeGetBaseQueryResult();
                            result.query = query;
                            result.configuration = configuration;
                            result.source = source;
                            handleJSONResponse(exchange, 200, state.objectMapper.writeValueAsString(result), state);
                        }
                        else
                        {
                            throw new RuntimeException("DataCube base query has not been set!");
                        }
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
