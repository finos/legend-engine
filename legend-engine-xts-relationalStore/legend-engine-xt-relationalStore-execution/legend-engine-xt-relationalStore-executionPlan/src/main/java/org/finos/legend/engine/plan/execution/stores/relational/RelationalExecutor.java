// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational;

import freemarker.template.Configuration;
import freemarker.template.Template;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.Iterate;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeClassResultHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeResultHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.ExecutionNodeTDSResultHelper;
import org.finos.legend.engine.plan.execution.nodes.helpers.freemarker.FreeMarkerExecutor;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.finos.legend.engine.plan.execution.result.ConstantResult;
import org.finos.legend.engine.plan.execution.result.Result;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.stores.StoreType;
import org.finos.legend.engine.plan.execution.stores.relational.activity.RelationalExecutionActivity;
import org.finos.legend.engine.plan.execution.stores.relational.blockConnection.BlockConnection;
import org.finos.legend.engine.plan.execution.stores.relational.config.RelationalExecutionConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.relational.result.DeferredRelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.PreparedTempTableResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.RealizedRelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.ResultInterpreterExtension;
import org.finos.legend.engine.plan.execution.stores.relational.result.ResultInterpreterExtensionLoader;
import org.finos.legend.engine.plan.execution.stores.relational.result.SQLExecutionResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.SQLUpdateResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.VoidRelationalResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalSaveNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.slf4j.Logger;

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class RelationalExecutor
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(RelationalExecutor.class);

    public static final String DEFAULT_DB_TIME_ZONE = "GMT";

    private final ConnectionManagerSelector connectionManager;
    private final RelationalExecutionConfiguration relationalExecutionConfiguration;
    private MutableList<Function2<ExecutionState, List<Map<String, Object>>, Result>> resultInterpreterExtensions;

    private static final MutableMap<String, String> DATA_TYPE_RELATIONAL_TYPE_MAP = Maps.mutable.empty();

    static
    {
        DATA_TYPE_RELATIONAL_TYPE_MAP.put("Integer", "INT");
        DATA_TYPE_RELATIONAL_TYPE_MAP.put("Float", "FLOAT");
        DATA_TYPE_RELATIONAL_TYPE_MAP.put("Number", "FLOAT");
        DATA_TYPE_RELATIONAL_TYPE_MAP.put("String", "VARCHAR(1000)");
        DATA_TYPE_RELATIONAL_TYPE_MAP.put("Date", "TIMESTAMP");
        DATA_TYPE_RELATIONAL_TYPE_MAP.put("DateTime", "TIMESTAMP");
        DATA_TYPE_RELATIONAL_TYPE_MAP.put("StrictDate", "DATE");
        DATA_TYPE_RELATIONAL_TYPE_MAP.put("Boolean", "BIT");
    }

    private Optional<DatabaseAuthenticationFlowProvider> flowProviderHolder;

    public RelationalExecutor(TemporaryTestDbConfiguration temporarytestdb, RelationalExecutionConfiguration relationalExecutionConfiguration)
    {
        this(temporarytestdb, relationalExecutionConfiguration, Optional.empty());
    }

    public RelationalExecutor(TemporaryTestDbConfiguration temporarytestdb, RelationalExecutionConfiguration relationalExecutionConfiguration, Optional<DatabaseAuthenticationFlowProvider> flowProviderHolder)
    {
        this.flowProviderHolder = flowProviderHolder;
        this.connectionManager = new ConnectionManagerSelector(temporarytestdb, relationalExecutionConfiguration.oauthProfiles, flowProviderHolder);
        this.relationalExecutionConfiguration = relationalExecutionConfiguration;
        this.resultInterpreterExtensions = Iterate.addAllTo(ResultInterpreterExtensionLoader.extensions(), Lists.mutable.empty()).collect(ResultInterpreterExtension::additionalResultBuilder);
    }

    public RelationalExecutionConfiguration getRelationalExecutionConfiguration()
    {
        return this.relationalExecutionConfiguration;
    }

    public ConnectionManagerSelector getConnectionManager()
    {
        return this.connectionManager;
    }

    public Result execute(RelationalExecutionNode node, Identity identity, ExecutionState executionState)
    {
        Connection connectionManagerConnection;
        String databaseTimeZone = node.getDatabaseTimeZone() == null ? DEFAULT_DB_TIME_ZONE : node.getDatabaseTimeZone();
        String databaseTypeName = node.getDatabaseTypeName();
        List<String> tempTableList = new FastList<>();

        connectionManagerConnection = getConnection(node, identity, ((RelationalStoreExecutionState) executionState.getStoreExecutionState(StoreType.Relational)));
        Span span = GlobalTracer.get().activeSpan();
        if (span != null)
        {
            span.log("Connection acquired");
        }

        this.prepareForSQLExecution(node.sqlQuery, node.sqlComment, connectionManagerConnection, databaseTimeZone, databaseTypeName, tempTableList, identity, executionState, true);

        if (executionState.inAllocation)
        {
            if ((ExecutionNodeTDSResultHelper.isResultTDS(node) || (ExecutionNodeResultHelper.isResultSizeRangeSet(node) && !ExecutionNodeResultHelper.isSingleRecordResult(node))) && !executionState.realizeInMemory)
            {
                return new RelationalResult(executionState.activities, node, node.resultColumns, databaseTypeName, databaseTimeZone, connectionManagerConnection, identity, tempTableList, executionState.topSpan, executionState.getRequestContext(), executionState.logSQLWithParamValues());
            }
            else if (node.isResultVoid())
            {
                return new VoidRelationalResult(executionState.activities, node, connectionManagerConnection, identity, executionState.logSQLWithParamValues());
            }
            else
            {
                // Refactor and clean up the flush to Constant
                RelationalResult result = new RelationalResult(executionState.activities, node, node.resultColumns, databaseTypeName, databaseTimeZone, connectionManagerConnection, identity, tempTableList, executionState.topSpan, executionState.getRequestContext(), executionState.logSQLWithParamValues());

                if (node.isResultPrimitiveType())
                {
                    try
                    {
                        if (result.resultSet.next())
                        {
                            MutableList<Function<Object, Object>> transformers = result.getTransformers();
                            Object convertedValue = transformers.get(0).valueOf(result.resultSet.getObject(1));
                            return new ConstantResult(convertedValue);
                        }
                        else
                        {
                            throw new RuntimeException("Result set is empty for allocation node");
                        }
                    }
                    catch (SQLException e)
                    {
                        throw new RuntimeException(e);
                    }
                }
                else
                {
                    try
                    {
                        RealizedRelationalResult realizedRelationalResult = (RealizedRelationalResult) result.realizeInMemory();
                        List<Map<String, Object>> rowValueMaps = realizedRelationalResult.getRowValueMaps(false);
                        Result res = evaluateAdditionalExtractors(this.resultInterpreterExtensions, executionState, rowValueMaps);
                        if (res != null)
                        {
                            return res;
                        }
                        else if (ExecutionNodeClassResultHelper.isClassResult(node) && rowValueMaps.size() == 1)
                        {
                            return new ConstantResult(rowValueMaps.get(0));
                        }
                        else
                        {
                            return new ConstantResult(rowValueMaps);
                        }
                    }
                    catch (Exception e)
                    {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        else if (node.isResultVoid())
        {
            return new VoidRelationalResult(executionState.activities, node, connectionManagerConnection, identity, executionState.logSQLWithParamValues());
        }
        else
        {
            return new RelationalResult(executionState.activities, node, node.resultColumns, databaseTypeName, databaseTimeZone, connectionManagerConnection, identity, tempTableList, executionState.topSpan, executionState.getRequestContext(), executionState.logSQLWithParamValues());
        }
    }

    public static Result evaluateAdditionalExtractors(MutableList<Function2<ExecutionState, List<Map<String, Object>>, Result>> resultInterpreterExtensions, ExecutionState executionState, List<Map<String, Object>> rowValueMaps)
    {
        for (Function2<ExecutionState, List<Map<String, Object>>, Result> func : resultInterpreterExtensions)
        {
            Result res = func.value(executionState, rowValueMaps);
            if (res != null)
            {
                return res;
            }
        }
        return null;
    }

    public Result execute(SQLExecutionNode node, Identity identity, ExecutionState executionState)
    {
        Connection connectionManagerConnection;
        String databaseTimeZone = node.getDatabaseTimeZone() == null ? DEFAULT_DB_TIME_ZONE : node.getDatabaseTimeZone();
        String databaseType = node.getDatabaseTypeName();
        List<String> tempTableList = FastList.newList();

        Span span = GlobalTracer.get().activeSpan();

        connectionManagerConnection = getConnection(node, identity, (RelationalStoreExecutionState) executionState.getStoreExecutionState(StoreType.Relational));
        if (span != null)
        {
            span.log("Connection acquired");
        }

        this.prepareForSQLExecution(node.sqlQuery, node.sqlComment, connectionManagerConnection, databaseTimeZone, databaseType, tempTableList, identity, executionState, true);
        
        if (node.isResultVoid())
        {
            return new VoidRelationalResult(executionState.activities, node, connectionManagerConnection, identity, executionState.logSQLWithParamValues());
        }

        if (node.isMutationSQL)
        {
            return new SQLUpdateResult(executionState.activities, databaseType, connectionManagerConnection, node.connection, identity, tempTableList, executionState.getRequestContext());
        }

        return new SQLExecutionResult(executionState.activities, node, databaseType, databaseTimeZone, connectionManagerConnection, identity, tempTableList, executionState.topSpan, executionState.getRequestContext(), executionState.logSQLWithParamValues());
    }

    public SQLUpdateResult execute(RelationalSaveNode node, Identity identity, ExecutionState executionState)
    {
        Connection connectionManagerConnection;
        String databaseTimeZone = node.getDatabaseTimeZone() == null ? DEFAULT_DB_TIME_ZONE : node.getDatabaseTimeZone();
        String databaseType = node.getDatabaseTypeName();
        List<String> tempTableList = FastList.newList();

        Span span = GlobalTracer.get().activeSpan();
        connectionManagerConnection = this.getConnection(node.connection, node.onConnectionCloseRollbackQuery, node.onConnectionCloseCommitQuery, identity, (RelationalStoreExecutionState) executionState.getStoreExecutionState(StoreType.Relational));
        if (span != null)
        {
            span.log("Connection acquired");
        }

        this.prepareForSQLExecution(node.sqlQuery, node.sqlComment, connectionManagerConnection, databaseTimeZone, databaseType, tempTableList, identity, executionState, false);

        return new SQLUpdateResult(executionState.activities, databaseType, connectionManagerConnection, node.connection, identity, tempTableList, executionState.getRequestContext());
    }

    private void prepareForSQLExecution(String sqlQuery, String sqlComment, Connection connection, String databaseTimeZone, String databaseTypeName, List<String> tempTableList, Identity identity, ExecutionState executionState, boolean shouldLogSQL)
    {
        DatabaseManager databaseManager = DatabaseManager.fromString(databaseTypeName);
        RelationalDatabaseCommands relationalDatabaseCommands = databaseManager.relationalDatabaseSupport();
        for (Map.Entry<String, Result> var : executionState.getResults().entrySet())
        {
            Result result = var.getValue();

            if (result instanceof DeferredRelationalResult && sqlQuery.contains("(${" + var.getKey() + "})"))
            {
                DeferredRelationalResult deferredRelationalResult = (DeferredRelationalResult) result;
                result = deferredRelationalResult.evaluate();
                executionState.activities.addAll(result.activities);
            }

            if (result instanceof StreamingResult && sqlQuery.contains("(${" + var.getKey() + "})"))
            {
                String tableName = relationalDatabaseCommands.processTempTableName(var.getKey());
                if (!tempTableList.contains(tableName))
                {
                    this.prepareTempTable(connection, (StreamingResult) result, tableName, databaseTypeName, databaseTimeZone, tempTableList);
                    tempTableList.add(tableName);
                }
                sqlQuery = sqlQuery.replace("(${" + var.getKey() + "})", tableName);
            }
            else if (result instanceof PreparedTempTableResult && sqlQuery.contains("(${" + var.getKey() + "})"))
            {
                sqlQuery = sqlQuery.replace("(${" + var.getKey() + "})", ((PreparedTempTableResult) result).getTempTableName());
            }
            else if (result instanceof RelationalResult && (sqlQuery.contains("inFilterClause_" + var.getKey() + "})") || sqlQuery.contains("${" + var.getKey() + "}")))
            {
                boolean isResultSetClosed = false;
                try
                {
                    isResultSetClosed = ((RelationalResult) result).resultSet.isClosed();
                }
                catch (SQLException ignored)
                {
                }

                if (((RelationalResult) result).columnCount == 1 && !isResultSetClosed)
                {
                    RealizedRelationalResult realizedRelationalResult = (RealizedRelationalResult) result.realizeInMemory();
                    List<Map<String, Object>> rowValueMaps = realizedRelationalResult.getRowValueMaps(false);
                    executionState.addResult(var.getKey(), new ConstantResult(rowValueMaps.stream().flatMap(map -> map.values().stream()).collect(Collectors.toList())));
                }
            }
        }

        if (sqlQuery == null)
        {
            throw new RuntimeException("Relational execution not supported on external server");
        }

        try
        {
            sqlComment = sqlComment != null ? FreeMarkerExecutor.process(sqlComment, executionState, databaseTypeName, databaseTimeZone) : null;
            RelationalStoreExecutionState relationalStoreExecutionState = (RelationalStoreExecutionState) executionState.getStoreExecutionState(StoreType.Relational);
            sqlQuery = relationalStoreExecutionState.ignoreFreeMarkerProcessing() ? sqlQuery : FreeMarkerExecutor.process(sqlQuery, executionState, databaseTypeName, databaseTimeZone);
            Span span = GlobalTracer.get().activeSpan();
            if (span != null && shouldLogSQL && executionState.logSQLWithParamValues())
            {
                span.setTag("generatedSQL", sqlQuery);
            }
        }
        catch (Exception e)
        {
            throw new IllegalStateException("Reprocessing sql failed with vars " + executionState.getResults().keySet(), e);
        }

        if (executionState.logSQLWithParamValues())
        {
            LOGGER.info(new LogInfo(identity.getName(), LoggingEventType.EXECUTION_RELATIONAL_REPROCESS_SQL, "Reprocessing sql with vars " + executionState.getResults().keySet() + ": " + sqlQuery).toString());
        }

        executionState.activities.add(new RelationalExecutionActivity(sqlQuery, sqlComment));
    }

    private void prepareTempTable(Connection connectionManagerConnection, StreamingResult res, String tempTableName, String databaseTypeName, String databaseTimeZone, List<String> tempTableList)
    {
        DatabaseManager databaseManager = DatabaseManager.fromString(databaseTypeName);
        try (Scope ignored = GlobalTracer.get().buildSpan("create temp table").withTag("tempTableName", tempTableName).withTag("databaseType", databaseTypeName).startActive(true))
        {
            databaseManager.relationalDatabaseSupport().accept(RelationalDatabaseCommandsVisitorBuilder.getStreamResultToTempTableVisitor(relationalExecutionConfiguration, connectionManagerConnection, res, tempTableName, databaseTimeZone));
        }
        catch (Exception e)
        {
            try
            {
                if (!tempTableList.isEmpty())
                {
                    try (Statement statement = connectionManagerConnection.createStatement())
                    {
                        tempTableList.forEach((Consumer<? super String>) table ->
                        {
                            try
                            {
                                statement.execute(databaseManager.relationalDatabaseSupport().dropTempTable(table));
                            }
                            catch (Exception ignored)
                            {
                            }
                        });
                    }
                }
                connectionManagerConnection.close();
                throw new RuntimeException(e);
            }
            catch (Exception ex)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private Connection getConnection(RelationalExecutionNode node, Identity identity, RelationalStoreExecutionState executionState)
    {
        return this.getConnection(node.connection, node.onConnectionCloseRollbackQuery, node.onConnectionCloseCommitQuery, identity, executionState);
    }

    private Connection getConnection(SQLExecutionNode node, Identity identity, RelationalStoreExecutionState executionState)
    {
        return this.getConnection(node.connection, node.onConnectionCloseRollbackQuery, node.onConnectionCloseCommitQuery, identity, executionState);
    }

    private Connection getConnection(DatabaseConnection databaseConnection, String onConnectionCloseRollbackQuery, String onConnectionCloseCommitQuery, Identity identity, RelationalStoreExecutionState executionState)
    {
        if (executionState.retainConnection())
        {
            BlockConnection blockConnection = executionState.getBlockConnectionContext().getBlockConnection(executionState, databaseConnection, identity);
            if (onConnectionCloseRollbackQuery != null)
            {
                blockConnection.addRollbackQuery(onConnectionCloseRollbackQuery);
            }
            if (onConnectionCloseCommitQuery != null)
            {
                blockConnection.addCommitQuery(onConnectionCloseCommitQuery);
            }
            return blockConnection;
        }
        return executionState.getRelationalExecutor().getConnectionManager().getDatabaseConnection(identity, databaseConnection, executionState.getRuntimeContext());
    }

    public static String process(String query, Map<?, ?> vars, String templateFunctions)
    {
        String result = "";
        try
        {
            Configuration cfg = new Configuration();
            cfg.setNumberFormat("computer");
            Template t = new Template("sqlTemplate", new StringReader(templateFunctions + "\n" + query), cfg);
            StringWriter stringWriter = new StringWriter();
            t.process(vars, stringWriter);
            result = stringWriter.toString();
        }
        catch (Exception ignored)
        {
        }
        return result;
    }

    public static String getRelationalTypeFromDataType(String dataType)
    {
        return DATA_TYPE_RELATIONAL_TYPE_MAP.get(dataType);
    }
}
