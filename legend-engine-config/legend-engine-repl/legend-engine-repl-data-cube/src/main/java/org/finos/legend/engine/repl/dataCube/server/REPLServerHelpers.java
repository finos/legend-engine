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

package org.finos.legend.engine.repl.dataCube.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.RelationTypeHelper;
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
import org.finos.legend.engine.repl.core.legend.LegendInterface;
import org.finos.legend.engine.repl.dataCube.server.model.DataCubeQuery;
import org.finos.legend.engine.repl.dataCube.server.model.DataCubeQueryColumn;
import org.finos.legend.engine.repl.shared.ExecutionHelper;
import org.finos.legend.pure.m3.coreinstance.meta.pure.metamodel.relation.RelationType;
import org.finos.legend.pure.m3.navigation.M3Paths;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.repl.shared.ExecutionHelper.REPL_RUN_FUNCTION_QUALIFIED_PATH;

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
            state.client.printError(e.getMessage());
        }
    }

    public static class REPLServerState
    {
        public final Client client;
        public final ObjectMapper objectMapper;
        public final PlanExecutor planExecutor;
        public final LegendInterface legendInterface;
        public Long startTime;

        private PureModelContextData currentPureModelContextData;
        private DataCubeQuery query;
        private Map<String, ?> source;

        public REPLServerState(Client client, ObjectMapper objectMapper, PlanExecutor planExecutor, LegendInterface legendInterface)
        {
            this.client = client;
            this.objectMapper = objectMapper;
            this.planExecutor = planExecutor;
            this.legendInterface = legendInterface;
        }

        private void initialize(PureModelContextData pureModelContextData, List<DataCubeQueryColumn> columns)
        {
            this.currentPureModelContextData = pureModelContextData;
            this.startTime = System.currentTimeMillis();

            // -------------------- SOURCE --------------------
            // try to extract the runtime for the query
            // remove any usage of multiple from(), only add one to the end
            // TODO: we might need to account for other variants of ->from(), such as when mapping is specified
            Function function = (Function) ListIterate.select(pureModelContextData.getElements(), e -> e.getPath().equals(REPL_RUN_FUNCTION_QUALIFIED_PATH)).getFirst();
            String runtime = null;
            String mapping = null;
            Deque<AppliedFunction> fns = new LinkedList<>();
            ValueSpecification currentExpression = function.body.get(0);
            while (currentExpression instanceof AppliedFunction)
            {
                AppliedFunction fn = (AppliedFunction) currentExpression;
                if (fn.function.equals("from"))
                {
                    if (fn.parameters.size() == 2)
                    {
                        // TODO: verify the type of the element (i.e. Runtime)
                        String newRuntime = ((PackageableElementPtr) fn.parameters.get(1)).fullPath;
                        if (runtime != null && !runtime.equals(newRuntime))
                        {
                            throw new RuntimeException("Can't launch DataCube. Source query contains multiple different ->from(), only one is expected");
                        }
                        runtime = newRuntime;
                    }
                    else if (fn.parameters.size() == 3)
                    {
                        // TODO: verify the type of the element (i.e. Mapping & Runtime)
                        String newMapping = ((PackageableElementPtr) fn.parameters.get(1)).fullPath;
                        String newRuntime = ((PackageableElementPtr) fn.parameters.get(2)).fullPath;
                        if ((mapping != null && !mapping.equals(newMapping)) || (runtime != null && !runtime.equals(newRuntime)))
                        {
                            throw new RuntimeException("Can't launch DataCube. Source query contains multiple different ->from(), only one is expected");
                        }
                        mapping = newMapping;
                        runtime = newRuntime;
                    }
                }
                else
                {
                    fns.addFirst(fn);
                }
                currentExpression = fn.parameters.get(0);
            }
            for (AppliedFunction fn : fns)
            {
                fn.parameters.set(0, currentExpression);
                currentExpression = fn;
            }
            Map<String, Object> source = Maps.mutable.empty();
            source.put("_type", "repl");
            source.put("timestamp", this.startTime);
            source.put("query", currentExpression.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().build()));
            source.put("runtime", runtime);
            source.put("mapping", mapping);
            this.source = source;

            // -------------------- QUERY --------------------
            DataCubeQuery query = new DataCubeQuery();
            query.configuration = null; // initially, the config is not initialized
            // NOTE: for this, the initial query is going to be a select all
            AppliedFunction partialFn = new AppliedFunction();
            partialFn.function = "select";
            ColSpecArray colSpecArray = new ColSpecArray();
            colSpecArray.colSpecs = ListIterate.collect(columns, col ->
            {
                ColSpec colSpec = new ColSpec();
                colSpec.name = col.name;
                return colSpec;
            });
            partialFn.parameters = Lists.mutable.with(new ClassInstance("colSpecArray", colSpecArray, null));
            query.query = partialFn.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().build());
            this.query = query;
        }

        public void initializeFromTable(PureModelContextData pureModelContextData)
        {
            PureModel pureModel = client.getLegendInterface().compile(pureModelContextData);
            RelationType<?> relationType;
            try
            {
                relationType = (RelationType) pureModel.getConcreteFunctionDefinition(REPL_RUN_FUNCTION_QUALIFIED_PATH, null)._expressionSequence().getLast()._genericType()._typeArguments().getFirst()._rawType();
            }
            catch (Exception e)
            {
                throw new RuntimeException("Can't launch DataCube: expected to get a relation type");
            }
            this.initialize(pureModelContextData, ListIterate.collect(RelationTypeHelper.convert(relationType).columns, col -> new DataCubeQueryColumn(col.name, col.type)));
        }

        public void initializeWithREPLExecutedQuery(ExecutionHelper.ExecuteResultSummary executeResultSummary)
        {
            if (!(executeResultSummary.result instanceof RelationalResult) || !(((RelationalResult) executeResultSummary.result).builder instanceof TDSBuilder))
            {
                throw new RuntimeException("Can't launch DataCube: last executed query did not produce a TDS (i.e. data-grid), try a different query...");
            }

            RelationType relationType;
            try
            {
                relationType = (RelationType) executeResultSummary.pureModel.getConcreteFunctionDefinition(REPL_RUN_FUNCTION_QUALIFIED_PATH, null)._expressionSequence().getLast()._genericType()._typeArguments().getFirst()._rawType();
            }
            catch (Exception e)
            {
                throw new RuntimeException("Can't launch DataCube: last executed query must return a relation type, try a different query...");
            }

            boolean isDynamic = false;
            try
            {
                SQLExecutionNode sqlExecutionNode = ((SQLExecutionNode) executeResultSummary.plan.rootExecutionNode.executionNodes.get(0));
                if (sqlExecutionNode.isResultColumnsDynamic != null)
                {
                    isDynamic = sqlExecutionNode.isResultColumnsDynamic;
                }
            }
            catch (Exception e)
            {
                // do nothing
            }
            if (isDynamic)
            {
                throw new RuntimeException("Can't launch DataCube: last executed query produced dynamic result, try casting the result with cast(@" + M3Paths.Relation + "<(...)>) syntax or use 'cache' command to dump the data out to a table and query against that table instead...");
            }

            RelationalResult result = (RelationalResult) executeResultSummary.result;
            this.initialize(executeResultSummary.pureModelContextData, ListIterate.collect(((TDSBuilder) result.builder).columns, col -> new DataCubeQueryColumn(col.name, col.type)));
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

        public Map<String, ?> getSource()
        {
            return this.source;
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
