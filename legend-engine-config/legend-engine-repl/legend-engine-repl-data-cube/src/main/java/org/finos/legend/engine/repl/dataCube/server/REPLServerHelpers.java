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
import org.finos.legend.engine.language.pure.grammar.to.HelperValueSpecificationGrammarComposer;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.result.builder.tds.TDSBuilder;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.PackageableConnection;
import org.finos.legend.engine.protocol.pure.m3.function.Function;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.PackageableRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.SectionIndex;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Database;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Schema;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.Table;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.ValueSpecification;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.AppliedFunction;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.ClassInstance;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.relation.ColSpec;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.classInstance.relation.ColSpecArray;
import org.finos.legend.engine.protocol.pure.dsl.store.valuespecification.constant.classInstance.RelationStoreAccessor;
import org.finos.legend.engine.protocol.pure.m3.valuespecification.constant.PackageableElementPtr;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.core.legend.LegendInterface;
import org.finos.legend.engine.repl.dataCube.server.model.DataCubeQueryColumn;
import org.finos.legend.engine.repl.shared.ExecutionHelper;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
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
    public static void handleJSONResponse(HttpExchange exchange, int responseCode, String response, REPLServerState state)
    {
        handleResponse(exchange, responseCode, response, state, "application/json");
    }

    public static void handleTextResponse(HttpExchange exchange, int responseCode, String response, REPLServerState state)
    {
        handleResponse(exchange, responseCode, response, state, "text/plain");
    }

    private static void handleResponse(HttpExchange exchange, int responseCode, String response, REPLServerState state, String contentType)
    {
        try
        {
            OutputStream os = exchange.getResponseBody();
            byte[] byteResponse = response != null ? response.getBytes(StandardCharsets.UTF_8) : new byte[0];
            if (contentType != null)
            {
                exchange.getResponseHeaders().add("Content-Type", contentType);
            }
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
        private String query;
        private Map<String, ?> queryConfiguration;
        private Map<String, ?> querySource;

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
            String runtimePath = null;
            String mappingPath = null;
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
                        if (runtimePath != null && !runtimePath.equals(newRuntime))
                        {
                            throw new RuntimeException("Can't launch DataCube. Source query contains multiple different ->from(), only one is expected");
                        }
                        runtimePath = newRuntime;
                    }
                    else if (fn.parameters.size() == 3)
                    {
                        // TODO: verify the type of the element (i.e. Mapping & Runtime)
                        String newMapping = ((PackageableElementPtr) fn.parameters.get(1)).fullPath;
                        String newRuntime = ((PackageableElementPtr) fn.parameters.get(2)).fullPath;
                        if ((mappingPath != null && !mappingPath.equals(newMapping)) || (runtimePath != null && !runtimePath.equals(newRuntime)))
                        {
                            throw new RuntimeException("Can't launch DataCube. Source query contains multiple different ->from(), only one is expected");
                        }
                        mappingPath = newMapping;
                        runtimePath = newRuntime;
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

            // Build the minimal PMCD needed to persist to run the query
            // NOTE: the ONLY use case we want to support right now is when user uses a single DB with relation store accessor
            // with a single connection in a single runtime, no mapping, no join, etc.
            // Those cases would be too complex to handle and result in too big of a PMCD to persist.
            boolean isLocal = false;
            boolean isPersistenceSupported = false;
            PureModelContextData model = null;
            PackageableRuntime runtime = null;
            if (runtimePath != null)
            {
                String _runtimePath = runtimePath;
                runtime = (PackageableRuntime) ListIterate.select(pureModelContextData.getElements(), e -> e.getPath().equals(_runtimePath)).getOnly();
            }
            Database database = null;
            if (currentExpression instanceof ClassInstance && ((ClassInstance) currentExpression).value instanceof RelationStoreAccessor)
            {
                RelationStoreAccessor accessor = (RelationStoreAccessor) ((ClassInstance) currentExpression).value;

                if (accessor.path.size() <= 1)
                {
                    throw new EngineException("Error in the accessor definition. Please provide a table.", accessor.sourceInformation, EngineErrorType.COMPILATION);
                }
                String schemaName = (accessor.path.size() == 3) ? accessor.path.get(1) : "default";
                String tableName = (accessor.path.size() == 3) ? accessor.path.get(2) : accessor.path.get(1);

                // clone the database, only extract the schema and table that we need
                Database _database = (Database) ListIterate.select(pureModelContextData.getElements(), e -> e.getPath().equals(accessor.path.get(0))).getOnly();
                Schema _schema = ListIterate.select(_database.schemas, s -> s.name.equals(schemaName)).getOnly();
                Table _table = ListIterate.select(_schema.tables, t -> t.name.equals(tableName)).getOnly();
                database = new Database();
                database.name = _database.name;
                database._package = _database._package;
                Schema schema = new Schema();
                schema.name = _schema.name;
                Table table = new Table();
                table.name = _table.name;
                table.columns = _table.columns;
                schema.tables = Lists.mutable.with(table);
                database.schemas = Lists.mutable.with(schema);
            }
            PackageableConnection connection = null;
            if (runtimePath != null)
            {
                String _runtime = runtimePath;
                PackageableRuntime rt = (PackageableRuntime) ListIterate.select(pureModelContextData.getElements(), e -> e.getPath().equals(_runtime)).getOnly();
                if (rt != null && rt.runtimeValue.connections.size() == 1 && rt.runtimeValue.connections.get(0).storeConnections.size() == 1)
                {
                    Connection conn = rt.runtimeValue.connections.get(0).storeConnections.get(0).connection;
                    if (conn instanceof ConnectionPointer)
                    {
                        PackageableConnection _connection = (PackageableConnection) ListIterate.select(pureModelContextData.getElements(), e -> e.getPath().equals(((ConnectionPointer) conn).connection)).getOnly();
                        if (_connection.connectionValue instanceof RelationalDatabaseConnection)
                        {
                            connection = _connection;
                            isLocal = DatabaseType.DuckDB.equals(((RelationalDatabaseConnection) _connection.connectionValue).databaseType);
                        }
                    }
                }
            }
            // The only case we want to support persisting the model is when we have a single DB and connection
            if (database != null && connection != null && runtime != null && mappingPath == null)
            {
                model = PureModelContextData.newBuilder()
                        .withSerializer(pureModelContextData.serializer)
                        .withElements(Lists.mutable.with(database, connection, runtime))
                        .build();
                try
                {
                    this.legendInterface.compile(model);
                    model = this.legendInterface.parse(this.legendInterface.render(model), false);
                    model = PureModelContextData.newBuilder().withSerializer(model.serializer).withElements(ListIterate.reject(model.getElements(), el -> el instanceof SectionIndex)).build();
                    isPersistenceSupported = true;
                }
                catch (Exception e)
                {
                    this.client.printDebug("Error while compiling persistent model: " + e.getMessage());
                    // something was wrong with the assembled model, reset it
                    model = null;
                }
            }

            Map<String, Object> source = Maps.mutable.empty();
            source.put("_type", "repl");
            source.put("query", currentExpression.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().build()));
            source.put("runtime", runtimePath);
            source.put("model", model);
            // some extra analytics metadata which would not be part of the persistent query
            source.put("columns", columns);
            source.put("mapping", mappingPath);
            source.put("timestamp", this.startTime);
            source.put("isLocal", isLocal);
            source.put("isPersistenceSupported", isPersistenceSupported);
            this.querySource = source;

            // -------------------- CONFIGURATION --------------------
            this.queryConfiguration = null; // initially, the config is not initialized

            // -------------------- QUERY --------------------
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
            this.query = partialFn.accept(DEPRECATED_PureGrammarComposerCore.Builder.newInstance().build());
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
            this.initialize(pureModelContextData, ListIterate.collect(RelationTypeHelper.convert(relationType).columns, col -> new DataCubeQueryColumn(col.name, HelperValueSpecificationGrammarComposer.printGenericType(col.genericType, DEPRECATED_PureGrammarComposerCore.Builder.newInstance().build()))));
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

        public String getQuery()
        {
            return this.query;
        }

        public Map<String, ?> getQueryConfiguration()
        {
            return this.queryConfiguration;
        }

        public Map<String, ?> getQuerySource()
        {
            return this.querySource;
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
