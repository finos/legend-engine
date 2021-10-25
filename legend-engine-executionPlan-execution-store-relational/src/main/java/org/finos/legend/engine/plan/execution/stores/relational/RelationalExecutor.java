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
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutionState;
import org.finos.legend.engine.plan.execution.stores.relational.result.PreparedTempTableResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.RealizedRelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.ResultInterpreterExtension;
import org.finos.legend.engine.plan.execution.stores.relational.result.SQLExecutionResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.VoidRelationalResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.ExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.pac4j.core.profile.CommonProfile;
import org.slf4j.Logger;

import java.io.StringReader;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.function.Consumer;

public class RelationalExecutor
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger("Alloy Execution Server");

    public static final String DEFAULT_DB_TIME_ZONE = "GMT";

    private final ConnectionManagerSelector connectionManager;
    private final RelationalExecutionConfiguration relationalExecutionConfiguration;
    private final RelationalExecutorInfo relationalExecutorInfo;
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

    public RelationalExecutor(TemporaryTestDbConfiguration temporarytestdb, RelationalExecutionConfiguration relationalExecutionConfiguration)
    {
        this.relationalExecutorInfo = new RelationalExecutorInfo();
        this.connectionManager = new ConnectionManagerSelector(temporarytestdb, relationalExecutionConfiguration.oauthProfiles, this.relationalExecutorInfo);
        this.relationalExecutionConfiguration = relationalExecutionConfiguration;
        this.resultInterpreterExtensions = Iterate.addAllTo(ServiceLoader.load(ResultInterpreterExtension.class), Lists.mutable.empty()).collect(ResultInterpreterExtension::additionalResultBuilder);
    }

    public RelationalExecutorInfo getRelationalExecutorInfo()
    {
        return this.relationalExecutorInfo;
    }

    public RelationalExecutionConfiguration getRelationalExecutionConfiguration()
    {
        return this.relationalExecutionConfiguration;
    }

    public ConnectionManagerSelector getConnectionManager()
    {
        return this.connectionManager;
    }

    public Result execute(RelationalExecutionNode node, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        Connection connectionManagerConnection;
        String databaseTimeZone = node.getDatabaseTimeZone() == null ? DEFAULT_DB_TIME_ZONE : node.getDatabaseTimeZone();
        String databaseTypeName = node.getDatabaseTypeName();
        List<String> tempTableList = new FastList<>();

        connectionManagerConnection = getConnection(node, profiles, ((RelationalStoreExecutionState) executionState.getStoreExecutionState(StoreType.Relational)));
        Span span = GlobalTracer.get().activeSpan();
        if (span != null)
        {
            span.log("Connection acquired");
        }

        this.prepareForSQLExecution(node, connectionManagerConnection, databaseTimeZone, databaseTypeName, tempTableList, profiles, executionState);

        if (executionState.inAllocation)
        {
            if ((ExecutionNodeTDSResultHelper.isResultTDS(node) || (ExecutionNodeResultHelper.isResultSizeRangeSet(node) && !ExecutionNodeResultHelper.isSingleRecordResult(node))) && !executionState.transformAllocation)
            {
                return new RelationalResult(executionState.activities, node, node.resultColumns, databaseTypeName, databaseTimeZone, connectionManagerConnection, profiles, tempTableList, executionState.topSpan);
            }
            else if (node.isResultVoid())
            {
                return new VoidRelationalResult(executionState.activities, connectionManagerConnection, profiles);
            }
            else
            {
                // Refactor and clean up the flush to Constant
                RelationalResult result = new RelationalResult(executionState.activities, node, node.resultColumns, databaseTypeName, databaseTimeZone, connectionManagerConnection, profiles, tempTableList, executionState.topSpan);

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
            return new VoidRelationalResult(executionState.activities, connectionManagerConnection, profiles);
        }
        else
        {
            return new RelationalResult(executionState.activities, node, node.resultColumns, databaseTypeName, databaseTimeZone, connectionManagerConnection, profiles, tempTableList, executionState.topSpan);
        }
    }

    public static Result evaluateAdditionalExtractors(MutableList<Function2<ExecutionState, List<Map<String, Object>>,Result>> resultInterpreterExtensions, ExecutionState executionState, List<Map<String, Object>> rowValueMaps)
    {
        for (Function2<ExecutionState, List<Map<String, Object>>,Result> func : resultInterpreterExtensions)
        {
            Result res = func.value(executionState, rowValueMaps);
            if (res != null)
            {
                return res;
            }
        }
        return null;
    }

    public Result execute(SQLExecutionNode node, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        Connection connectionManagerConnection;
        String databaseTimeZone = node.getDatabaseTimeZone() == null ? DEFAULT_DB_TIME_ZONE : node.getDatabaseTimeZone();
        String databaseType = node.getDatabaseTypeName();
        List<String> tempTableList = FastList.newList();

        Span span = GlobalTracer.get().activeSpan();
        connectionManagerConnection = getConnection(node, profiles, (RelationalStoreExecutionState) executionState.getStoreExecutionState(StoreType.Relational));
        if (span != null)
        {
            span.log("Connection acquired");
        }

        this.prepareForSQLExecution(node, connectionManagerConnection, databaseTimeZone, databaseType, tempTableList, profiles, executionState);

        if (node.isResultVoid())
        {
            return new VoidRelationalResult(executionState.activities, connectionManagerConnection, profiles);
        }

        return new SQLExecutionResult(executionState.activities, node, databaseType, databaseTimeZone, connectionManagerConnection, profiles, tempTableList, executionState.topSpan);
    }

    private void prepareForSQLExecution(ExecutionNode node, Connection connection, String databaseTimeZone, String databaseTypeName, List<String> tempTableList, MutableList<CommonProfile> profiles, ExecutionState executionState)
    {
        String sqlQuery;

        sqlQuery = node instanceof RelationalExecutionNode ? ((RelationalExecutionNode) node).sqlQuery() : ((SQLExecutionNode) node).sqlQuery();

        DatabaseManager databaseManager = DatabaseManager.fromString(databaseTypeName);
        for (Map.Entry<String, Result> var : executionState.getResults().entrySet())
        {
            if (var.getValue() instanceof StreamingResult && sqlQuery.contains("(${" + var.getKey() + "})"))
            {
                String tableName = databaseManager.relationalDatabaseSupport().processTempTableName(var.getKey());
                this.prepareTempTable(connection, (StreamingResult) var.getValue(), tableName, databaseTypeName, databaseTimeZone, tempTableList);
                tempTableList.add(tableName);
                sqlQuery = sqlQuery.replace("(${" + var.getKey() + "})", tableName);
            }
            else if (var.getValue() instanceof PreparedTempTableResult && sqlQuery.contains("(${" + var.getKey() + "})"))
            {
                sqlQuery = sqlQuery.replace("(${" + var.getKey() + "})", ((PreparedTempTableResult) var.getValue()).getTempTableName());
            }
        }

        if (sqlQuery == null)
        {
            throw new RuntimeException("Relational execution not supported on external server");
        }

        try
        {
            sqlQuery = FreeMarkerExecutor.process(sqlQuery, executionState, databaseTypeName, databaseTimeZone);
            Span span = GlobalTracer.get().activeSpan();
            if (span != null)
            {
                span.setTag("generatedSQL", sqlQuery);
            }
        }
        catch (Exception e)
        {
            LOGGER.info("Exception while reprocessing SQL Query. Detail: " + e.getMessage() + ".");
        }

        LOGGER.info(new LogInfo(profiles, LoggingEventType.EXECUTION_RELATIONAL_REPROCESS_SQL, "Reprocessing sql with vars [" + executionState.getResults().keySet() + "]: " + sqlQuery).toString());

        executionState.activities.add(new RelationalExecutionActivity(sqlQuery));
    }

    private void prepareTempTable(Connection connectionManagerConnection, StreamingResult res, String tempTableName, String databaseTypeName, String databaseTimeZone, List<String> tempTableList)
    {
        DatabaseManager databaseManager = DatabaseManager.fromString(databaseTypeName);
        try
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
                        tempTableList.forEach((Consumer<? super String>) table -> {
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

    private Connection getConnection(RelationalExecutionNode node, MutableList<CommonProfile> profiles, RelationalStoreExecutionState executionState)
    {
        return this.getConnection(node.connection, node.onConnectionCloseRollbackQuery, node.onConnectionCloseCommitQuery, profiles, executionState);
    }

    private Connection getConnection(SQLExecutionNode node, MutableList<CommonProfile> profiles, RelationalStoreExecutionState executionState)
    {
        return this.getConnection(node.connection, node.onConnectionCloseRollbackQuery, node.onConnectionCloseCommitQuery, profiles, executionState);
    }

    private Connection getConnection(DatabaseConnection databaseConnection, String onConnectionCloseRollbackQuery, String onConnectionCloseCommitQuery, MutableList<CommonProfile> profiles, RelationalStoreExecutionState executionState)
    {
        if (executionState.retainConnection())
        {
            BlockConnection blockConnection = executionState.getBlockConnectionContext().getBlockConnection(executionState, databaseConnection, profiles);
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
        return executionState.getRelationalExecutor().getConnectionManager().getDatabaseConnection(profiles, databaseConnection);
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
        catch (Exception e)
        {
        }
        return result;
    }

    public static String getRelationalTypeFromDataType(String dataType)
    {
        return DATA_TYPE_RELATIONAL_TYPE_MAP.get(dataType);
    }
}