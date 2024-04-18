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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.RichIterable;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Multiplicity;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.CInteger;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.Lambda;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.legend.LegendInterface;
import org.finos.legend.engine.shared.core.api.grammar.RenderStyle;
import org.finos.legend.pure.generated.Root_meta_pure_executionPlan_ExecutionPlan;
import org.finos.legend.pure.generated.Root_meta_pure_extension_Extension;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    public String getGridUrl()
    {
        return "http://localhost:" + this.port + "/repl/grid";
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
            public void handle(HttpExchange exchange)
            {
                if ("GET".equals(exchange.getRequestMethod()))
                {
                    try
                    {
                        String[] path = exchange.getRequestURI().getPath().split("/repl/");
                        Path currentPath = Paths.get("").toAbsolutePath();
                        byte[] response = Files.readAllBytes(Paths.get(currentPath.toString() + "/legend-engine-config/legend-engine-repl/legend-engine-repl-relational/target/web-content/package/dist/repl/" + (path[1].equals("grid") ? "index.html" : path[1])));
                        exchange.sendResponseHeaders(200, response.length);
                        OutputStream os = exchange.getResponseBody();
                        os.write(response);
                        os.close();
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
            public void handle(HttpExchange exchange)
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

        server.createContext("/gridResult", new HttpHandler()
        {
            @Override
            public void handle(HttpExchange exchange)
            {
                if ("GET".equals(exchange.getRequestMethod()))
                {
                    ValueSpecification funcBody = null;
                    Function func = null;
                    try
                    {
                        func = (Function) currentPMCD.getElements().stream().filter(e -> e.getPath().equals("a::b::c::d__Any_MANY_")).collect(Collectors.toList()).get(0);
                        funcBody = func.body.get(0);

                        AppliedFunction sliceFunction = new AppliedFunction();
                        sliceFunction.function = "slice";
                        sliceFunction.parameters = Lists.mutable.of(funcBody);
                        sliceFunction.multiplicity = Multiplicity.PURE_MANY;
                        sliceFunction.parameters.add(new CInteger(0));
                        sliceFunction.parameters.add(new CInteger(100));
                        func.body = Lists.mutable.of(sliceFunction);

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
}
