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

package org.finos.legend.engine.repl.relational.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.DEPRECATED_PureGrammarComposerCore;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.builder.tds.TDSBuilder;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.application.AppliedFunction;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.ClassInstance;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.PackageableElementPtr;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.relation.ColSpec;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.raw.classInstance.relation.ColSpecArray;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.commands.Execute;
import org.finos.legend.engine.repl.core.legend.LegendInterface;
import org.finos.legend.engine.repl.relational.server.model.DataCubeQuery;
import org.finos.legend.engine.repl.relational.server.model.DataCubeQueryColumn;
import org.finos.legend.engine.repl.relational.server.model.DataCubeQuerySourceREPLExecutedQuery;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.finos.legend.engine.repl.core.Helpers.REPL_RUN_FUNCTION_QUALIFIED_PATH;

public class REPLServerHelpers
{
    public static void handleResponse(HttpExchange exchange, int responseCode, String response, REPLServerState state)
    {
        try
        {
            OutputStream os = exchange.getResponseBody();
            byte[] byteResponse = response != null ? response.getBytes(StandardCharsets.UTF_8) : new byte[0];
            exchange.sendResponseHeaders(responseCode, byteResponse.length);
            os.write(byteResponse);
            os.close();
        }
        catch (IOException e)
        {
            state.log(e.getMessage());
        }
    }

    public static Map<String, String> getQueryParams(HttpExchange exchange)
    {
        String query = exchange.getRequestURI().getQuery();
        Map<String, String> result = new HashMap<>();
        if (query == null)
        {
            return result;
        }
        for (String param : query.split("&"))
        {
            String[] entry = param.split("=");
            if (entry.length > 1)
            {
                result.put(entry[0], entry[1]);
            }
            else
            {
                result.put(entry[0], "");
            }
        }
        return result;
    }

    public static class REPLServerState
    {
        public final ObjectMapper objectMapper;
        public final PlanExecutor planExecutor;
        public final LegendInterface legendInterface;
        public Long startTime;

        private Client client;
        private PureModelContextData currentPureModelContextData;
        private DataCubeQuery query;

        public REPLServerState(ObjectMapper objectMapper, PlanExecutor planExecutor, LegendInterface legendInterface)
        {
            this.objectMapper = objectMapper;
            this.planExecutor = planExecutor;
            this.legendInterface = legendInterface;
        }

        public void initializeWithREPLExecutedQuery(Execute.ExecuteResult executeResult)
        {
            this.currentPureModelContextData = executeResult.pureModelContextData;

            this.startTime = System.currentTimeMillis();
            this.query = new DataCubeQuery();
            this.query.name = "New Report";
            this.query.configuration = null; // initially, the config is not initialized

            // process source
            DataCubeQuerySourceREPLExecutedQuery source = new DataCubeQuerySourceREPLExecutedQuery();

            if (!(executeResult.result instanceof RelationalResult) || !(((RelationalResult) executeResult.result).builder instanceof TDSBuilder))
            {
                throw new RuntimeException("Can't initialize DataCube. Last executed query did not produce a TDS (i.e. data-grid), try a different query...");
            }

            RelationType relationType;
            try
            {
                relationType = (RelationType) executeResult.pureModel.getConcreteFunctionDefinition(REPL_RUN_FUNCTION_QUALIFIED_PATH, null)._expressionSequence().getLast()._genericType()._typeArguments().getFirst()._rawType();
            }
            catch (Exception e)
            {
                throw new RuntimeException("Can't initialize DataCube. Last executed query must return a relation type, try a different query...");
            }

            RelationalResult result = (RelationalResult) executeResult.result;
            source.columns = ListIterate.collect(((TDSBuilder) result.builder).columns, col -> new DataCubeQueryColumn(col.name, col.type));

            // try to extract the runtime for the query
            // remove any usage of multiple from(), only add one to the end
            // TODO: we might need to account for other variants of ->from(), such as when mapping is specified
            Function function = (Function) ListIterate.select(executeResult.pureModelContextData.getElements(), e -> e.getPath().equals(REPL_RUN_FUNCTION_QUALIFIED_PATH)).getFirst();
            String runtime = null;
            MutableList<AppliedFunction> fns = Lists.mutable.empty();
            ValueSpecification currentExpression = function.body.get(0);
            while (currentExpression instanceof AppliedFunction)
            {
                AppliedFunction fn = (AppliedFunction) currentExpression;
                if (fn.function.equals("from"))
                {
                    String newRuntime = ((PackageableElementPtr) fn.parameters.get(1)).fullPath;
                    if (runtime != null && !runtime.equals(newRuntime))
                    {
                        throw new RuntimeException("Can't initialize DataCube. Source query contains multiple different ->from(), only one is expected");
                    }
                    else
                    {
                        runtime = newRuntime;
                    }
                }
                else
                {
                    fns.add(fn);
                }
                currentExpression = fn.parameters.get(0);
            }
            for (AppliedFunction fn : fns)
            {
                fn.parameters.set(0, currentExpression);
                currentExpression = fn;
            }

            this.query.partialQuery = "";
            source.query = currentExpression.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().build());
            source.runtime = runtime;
            this.query.source = source;

            // build the partial query
            // NOTE: for this, the initial query is going to be a select all
            AppliedFunction partialFn = new AppliedFunction();
            partialFn.function = "select";
            ColSpecArray colSpecArray = new ColSpecArray();
            colSpecArray.colSpecs = ListIterate.collect(source.columns, col ->
            {
                ColSpec colSpec = new ColSpec();
                colSpec.name = col.name;
                return colSpec;
            });
            partialFn.parameters = Lists.mutable.with(new ClassInstance("colSpecArray", colSpecArray, null));
            this.query.partialQuery = partialFn.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().build());

            // build the full query
            AppliedFunction fullFn = new AppliedFunction();
            fullFn.function = "from";
            fullFn.parameters = Lists.mutable.with(partialFn, new PackageableElementPtr(runtime));
            partialFn.parameters = Lists.mutable.with(currentExpression).withAll(partialFn.parameters);
            this.query.query = fullFn.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().build());
        }

        public PureModelContextData getCurrentPureModelContextData()
        {
            PureModelContextData data = this.currentPureModelContextData;
            if (data == null)
            {
                throw new RuntimeException("Can't retrieve current graph data. Try to load or run a query in REPL before launching DataCube...");
            }
            return data;
        }

        public DataCubeQuery getQuery()
        {
            return this.query;
        }

        public void setClient(Client client)
        {
            this.client = client;
        }

        public void log(String message)
        {
            if (this.client != null)
            {
                this.client.getTerminal().writer().println(message);
            }
        }
    }

    public interface DataCubeServerHandler
    {
        HttpHandler getHandler(REPLServerState state);
    }

    public static class DEV__CORSFilter extends Filter
    {
        private final MutableList<String> allowedOrigins;

        DEV__CORSFilter(MutableList<String> allowedOrigins)
        {
            super();
            if (allowedOrigins.isEmpty())
            {
                throw new IllegalArgumentException("Can't configure CORS filter: Allowed origins cannot be empty");
            }
            this.allowedOrigins = allowedOrigins;
        }

        @Override
        public void doFilter(HttpExchange exchange, Chain chain) throws IOException
        {
            Headers headers = exchange.getResponseHeaders();
            headers.add("Access-Control-Allow-Origin", allowedOrigins.makeString(","));
            headers.add("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
            headers.add("Access-Control-Allow-Credentials", "true");
            headers.add("chainPreflight", "false");
            headers.add("Access-Control-Allow-Headers", "Access-Control-Allow-Headers, Origin, Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS"))
            {
                exchange.sendResponseHeaders(204, -1);
            }
            else
            {
                chain.doFilter(exchange);
            }
        }

        @Override
        public String description()
        {
            return "CORSFilter";
        }
    }
}
