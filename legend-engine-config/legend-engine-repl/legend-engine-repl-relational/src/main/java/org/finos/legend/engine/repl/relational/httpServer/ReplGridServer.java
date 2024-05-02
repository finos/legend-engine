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

package org.finos.legend.engine.repl.relational.httpServer;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposer;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.repl.autocomplete.Completer;
import org.finos.legend.engine.repl.autocomplete.CompletionResult;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.legend.LegendInterface;
import org.finos.legend.engine.repl.relational.autocomplete.RelationalCompleterExtension;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.engine.shared.core.operational.errorManagement.ExceptionError;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ReplGridServer
{
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final PlanExecutor planExecutor = PlanExecutor.newPlanExecutorBuilder().withAvailableStoreExecutors().build();
    private PureModelContextData currentPMCD;
    private Client client;
    private int port;

    public ReplGridServer(Client client)
    {
        this.client = client;
    }

    private String getReplUrl()
    {
        String proxyUrl = System.getenv("VSCODE_PROXY_URI");
        if (proxyUrl != null)
        {
            return proxyUrl.replace("{{port}}", this.port + "") + "repl/";
        }
        return "http://localhost:" + this.port + "/repl/";
    }

    public String getGridUrl()
    {
        return getReplUrl() + "grid";
    }

    public static class GridServerResult
    {
        private final String currentQuery;
        private final String result;

        public GridServerResult(@JsonProperty("currentQuery") String currentQuery, @JsonProperty("result") String result)
        {
            this.currentQuery = currentQuery;
            this.result = result;
        }

        public String getResult()
        {
            return this.result;
        }

        public String getCurrentQuery()
        {
            return this.currentQuery;
        }
    }

    public void updateGridState(PureModelContextData pmcd)
    {
        this.currentPMCD = pmcd;
    }

    public void initializeServer() throws Exception
    {
        InetSocketAddress serverPortAddress = new InetSocketAddress(0);

        HttpServer server = HttpServer.create(serverPortAddress, 0);

        server.createContext("/licenseKey", new HttpHandler()
        {
            @Override
            public void handle(HttpExchange exchange) throws IOException
            {
                if ("GET".equals(exchange.getRequestMethod()))
                {
                    try
                    {
                        String licenseKey = System.getProperty("legend.repl.grid.licenseKey") == null ? "" : System.getProperty("legend.repl.grid.licenseKey");
                        String key = objectMapper.writeValueAsString(licenseKey);
                        handleResponse(exchange, 200, key);
                    }
                    catch (Exception e)
                    {
                        OutputStream os = exchange.getResponseBody();
                        exchange.sendResponseHeaders(500, e.getMessage().length());
                        os.write(e.getMessage().getBytes(StandardCharsets.UTF_8));
                        os.close();
                    }

                }
            }
        });

        server.createContext("/repl/", new HttpHandler()
        {
            @Override
            public void handle(HttpExchange exchange) throws IOException
            {
                if ("GET".equals(exchange.getRequestMethod()))
                {
                    try
                    {
                        String[] path = exchange.getRequestURI().getPath().split("/repl/");
                        String resourcePath = "/web-content/package/dist/repl/" + (path[1].equals("grid") ? "index.html" : path[1]);
                        OutputStream os = exchange.getResponseBody();
                        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(ReplGridServer.class.getResourceAsStream(resourcePath))))
                        {
                            String content = bufferedReader.lines().collect(Collectors.joining("\n"));
                            if (resourcePath.endsWith(".html") && resourcePath.startsWith("index"))
                            {
                                content = content.replace("/repl/", new URI(getReplUrl()).getPath());
                            }
                            else if (resourcePath.endsWith(".js"))
                            {
                                exchange.getResponseHeaders().add("Content-Type", "text/javascript; charset=utf-8");
                            }
                            else if (resourcePath.endsWith(".css"))
                            {
                                exchange.getResponseHeaders().add("Content-Type", "text/css; charset=utf-8");
                            }
                            byte[] response = content.getBytes(StandardCharsets.UTF_8);
                            exchange.sendResponseHeaders(200, response.length);
                            os.write(response);
                        }
                        catch (Exception e)
                        {
                            handleResponse(exchange, 500, e.getMessage());
                        }
                        finally
                        {
                            os.close();
                        }
                    }
                    catch (Exception e)
                    {
                        handleResponse(exchange, 500, e.getMessage());
                    }

                }
            }
        });

        server.createContext("/initialLambda", new HttpHandler()
        {
            @Override
            public void handle(HttpExchange exchange) throws IOException
            {
                if ("GET".equals(exchange.getRequestMethod()))
                {
                    try
                    {
                        Function func = (Function) currentPMCD.getElements().stream().filter(e -> e.getPath().equals("a::b::c::d__Any_MANY_")).collect(Collectors.toList()).get(0);
                        Lambda lambda = new Lambda();
                        lambda.body = func.body;
                        String response = objectMapper.writeValueAsString(lambda);
                        handleResponse(exchange, 200, response);
                    }
                    catch (Exception e)
                    {
                        handleResponse(exchange, 500, e.getMessage());
                    }
                }
            }
        });

        server.createContext("/executeLambda", new HttpHandler()
        {
            @Override
            public void handle(HttpExchange exchange) throws IOException
            {
                if ("POST".equals(exchange.getRequestMethod()))
                {
                    try
                    {
                        InputStreamReader inputStreamReader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                        BufferedReader bufferReader = new BufferedReader(inputStreamReader);
                        String requestBody = bufferReader.lines().collect(Collectors.joining());
                        AppliedFunction body = (AppliedFunction) PureGrammarParser.newInstance().parseValueSpecification(requestBody, "", 0, 0, true);
                        List<ValueSpecification> newBody = Lists.mutable.of(body);
                        if (checkIfPaginationIsEnabled(exchange.getRequestURI().getQuery()))
                        {
                            applySliceFunction(newBody);
                        }
                        Function func = (Function) currentPMCD.getElements().stream().filter(e -> e.getPath().equals("a::b::c::d__Any_MANY_")).collect(Collectors.toList()).get(0);
                        func.body = newBody;
                        String response = executeLambda(client.getLegendInterface(), currentPMCD, func, newBody.get(0));
                        handleResponse(exchange, 200, response);
                    }
                    catch (Exception e)
                    {
                        handleResponse(exchange, 500, e.getMessage());
                    }
                }
            }
        });

        server.createContext("/typeahead", new HttpHandler()
        {
            @Override
            public void handle(HttpExchange exchange) throws IOException
            {
                if ("POST".equals(exchange.getRequestMethod()))
                {
                    try
                    {
                        InputStreamReader inputStreamReader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                        BufferedReader bufferReader = new BufferedReader(inputStreamReader);
                        String requestBody = bufferReader.lines().collect(Collectors.joining());
                        String buildCodeContext = PureGrammarComposer.newInstance(PureGrammarComposerContext.Builder.newInstance().build()).renderPureModelContextData(currentPMCD);
                        CompletionResult result  = new Completer(buildCodeContext, Lists.mutable.with(new RelationalCompleterExtension())).complete(requestBody);
                        if (result.getEngineException() != null)
                        {
                            handleResponse(exchange, 500, result.getEngineException().toPretty());
                        }
                        else
                        {
                            handleResponse(exchange, 200, objectMapper.writeValueAsString(result.getCompletion()));
                        }
                    }
                    catch (Exception e)
                    {
                        handleResponse(exchange, 500, e.getMessage());
                    }
                }
            }
        });

        server.createContext("/parseQuery", new HttpHandler()
        {
            @Override
            public void handle(HttpExchange exchange) throws IOException
            {
                if ("POST".equals(exchange.getRequestMethod()))
                {
                    try
                    {
                        InputStreamReader inputStreamReader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                        BufferedReader bufferReader = new BufferedReader(inputStreamReader);
                        String requestBody = bufferReader.lines().collect(Collectors.joining("\n"));
                        PureGrammarParser.newInstance().parseValueSpecification(requestBody, "", 0, 0, true);
                        exchange.sendResponseHeaders(200, -1);
                    }
                    catch (Exception e)
                    {
                        handleResponse(exchange, 400, objectMapper.writeValueAsString(new ExceptionError(-1, e)));
                    }
                }
            }
        });

        server.createContext("/gridResult", new HttpHandler()
        {
            @Override
            public void handle(HttpExchange exchange) throws IOException
            {
                if ("GET".equals(exchange.getRequestMethod()))
                {
                    ValueSpecification funcBody = null;
                    Function func = null;
                    try
                    {
                        func = (Function) currentPMCD.getElements().stream().filter(e -> e.getPath().equals("a::b::c::d__Any_MANY_")).collect(Collectors.toList()).get(0);
                        funcBody = func.body.get(0);

                        List<ValueSpecification> newBody = Lists.mutable.of(funcBody);
                        if (checkIfPaginationIsEnabled(exchange.getRequestURI().getQuery()))
                        {
                            applySliceFunction(newBody);
                        }
                        func.body = newBody;

                        String response = executeLambda(client.getLegendInterface(), currentPMCD, func, funcBody);
                        handleResponse(exchange, 200, response);
                    }
                    catch (Exception e)
                    {
                        System.out.println(e.getMessage());
                        if (func != null)
                        {
                            func.body = Lists.mutable.of(funcBody);
                        }
                        handleResponse(exchange, 500, e.getMessage());
                    }

                }
                else if ("POST".equals(exchange.getRequestMethod()))
                {
                    ValueSpecification funcBody = null;
                    Function func = null;
                    try
                    {
                        InputStreamReader inputStreamReader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                        BufferedReader bufferReader = new BufferedReader(inputStreamReader);
                        String requestBody = bufferReader.lines().collect(Collectors.joining());
                        Lambda request = objectMapper.readValue(requestBody, Lambda.class);
                        func = (Function) currentPMCD.getElements().stream().filter(e -> e.getPath().equals("a::b::c::d__Any_MANY_")).collect(Collectors.toList()).get(0);
                        funcBody = func.body.get(0);
                        func.body = request.body;
                        String response = executeLambda(client.getLegendInterface(), currentPMCD, func, funcBody);
                        handleResponse(exchange, 200, response);
                    }
                    catch (Exception e)
                    {
                        System.out.println(e.getMessage());
                        if (func != null)
                        {
                            func.body = Lists.mutable.of(funcBody);
                        }
                        handleResponse(exchange, 500, e.getMessage());
                    }
                }
            }
        });

        server.setExecutor(null);
        server.start();
        serverPortAddress = server.getAddress();
        this.port = serverPortAddress.getPort();
        System.out.println("REPL Grid Server has started at port " + serverPortAddress.getPort());
    }

    public static String executeLambda(LegendInterface legendInterface, PureModelContextData currentRequestPMCD, Function func, ValueSpecification funcBody) throws IOException
    {
        Lambda lambda = new Lambda();
        lambda.body = func.body;
        String lambdaString = lambda.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().withRenderStyle(RenderStyle.PRETTY).build());
        PureModel pureModel = legendInterface.compile(currentRequestPMCD);
        RichIterable<? extends Root_meta_pure_extension_Extension> extensions = PureCoreExtensionLoader.extensions().flatCollect(e -> e.extraPureCoreExtensions(pureModel.getExecutionSupport()));

        // Plan
        Root_meta_pure_executionPlan_ExecutionPlan plan = legendInterface.generatePlan(pureModel, false);
        String planStr = PlanGenerator.serializeToJSON(plan, "vX_X_X", pureModel, extensions, LegendPlanTransformers.transformers);

        // Execute
        Result res =  planExecutor.execute(planStr);
        func.body = Lists.mutable.of(funcBody);
        if (res instanceof RelationalResult)
        {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ((RelationalResult) res).getSerializer(SerializationFormat.DEFAULT).stream(byteArrayOutputStream);
            GridServerResult result = new GridServerResult(lambdaString, byteArrayOutputStream.toString());
            return objectMapper.writeValueAsString(result);
        }
        throw new RuntimeException("Expected return type of Lambda execution is RelationalResult, but returned " + res.getClass().getName());
    }

    private void handleResponse(HttpExchange exchange, int responseCode, String response)
    {
        try
        {
            OutputStream os = exchange.getResponseBody();
            byte[] byteResponse = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(responseCode, byteResponse.length);
            os.write(byteResponse);
            os.close();
        }
        catch (IOException e)
        {
            System.out.println(e.getMessage());
        }
    }

    public static void applySliceFunction(List<ValueSpecification> body)
    {
        CInteger startValue = new CInteger(0);
        CInteger endValue = new CInteger(100);
        ValueSpecification currentExpression = body.get(0);
        while (currentExpression instanceof AppliedFunction)
        {
            if (((AppliedFunction) currentExpression).function.equals("from"))
            {
                ValueSpecification childExpression = ((AppliedFunction) currentExpression).parameters.get(0);
                if (childExpression instanceof  AppliedFunction && ((AppliedFunction) childExpression).function.equals("slice"))
                {
                    ((AppliedFunction) childExpression).parameters = Lists.mutable.of(((AppliedFunction) childExpression).parameters.get(0), startValue, endValue);
                    break;
                }
                AppliedFunction sliceFunction = new AppliedFunction();
                sliceFunction.function = "slice";
                sliceFunction.parameters = Lists.mutable.of(((AppliedFunction) currentExpression).parameters.get(0), startValue, endValue);
                ((AppliedFunction) currentExpression).parameters.set(0, sliceFunction);
                break;
            }
            currentExpression = ((AppliedFunction) currentExpression).parameters.get(0);
        }
    }

    private static boolean checkIfPaginationIsEnabled(String queryParamsString)
    {
        Map<String, String> queryParams = new HashMap<>();
        for (String param : queryParamsString.split("&"))
        {
            String[] entry = param.split("=");
            if (entry.length > 1)
            {
                queryParams.put(entry[0], entry[1]);
            }
            else
            {
                queryParams.put(entry[0], "");
            }
        }
        return queryParams.get("isPaginationEnabled").equals("true") ? true : false;
    }
}
