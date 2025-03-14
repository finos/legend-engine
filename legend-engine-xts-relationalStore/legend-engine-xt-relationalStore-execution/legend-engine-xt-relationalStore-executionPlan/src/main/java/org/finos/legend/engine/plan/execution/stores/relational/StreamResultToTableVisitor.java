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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import io.opentracing.Scope;
import io.opentracing.util.GlobalTracer;
import org.finos.legend.engine.plan.execution.result.StreamingResult;
import org.finos.legend.engine.plan.execution.stores.relational.config.RelationalExecutionConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.Column;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.IngestionMethod;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommandsVisitor;
import org.finos.legend.engine.plan.execution.stores.relational.result.TempTableStreamingResult;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.operational.logs.LogInfo;
import org.finos.legend.engine.shared.core.operational.logs.LoggingEventType;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

public class StreamResultToTableVisitor implements RelationalDatabaseCommandsVisitor<Boolean>
{
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(StreamResultToTableVisitor.class);

    public RelationalExecutionConfiguration config;
    public Connection connection;
    public StreamingResult result;
    public String tableName;
    public String databaseTimeZone;
    public IngestionMethod ingestionMethod;

    public StreamResultToTableVisitor(RelationalExecutionConfiguration config, Connection connection, StreamingResult result, String tableName, String databaseTimeZone)
    {
        this.config = config;
        this.connection = connection;
        this.result = result;
        this.tableName = tableName;
        this.databaseTimeZone = databaseTimeZone;
        this.ingestionMethod = IngestionMethod.DIRECT_INSERT;
    }

    @Override
    public Boolean visit(RelationalDatabaseCommands databaseCommands)
    {
        return this.streamResultToTable(databaseCommands);
    }

    public Boolean streamResultToTable(RelationalDatabaseCommands dbCommands)
    {
        try (Statement statement = connection.createStatement())
        {
            statement.execute(dbCommands.dropTempTable(tableName));

            if (result instanceof TempTableStreamingResult)
            {
                TempTableStreamingResult tempTableStreamingResult = (TempTableStreamingResult) result;

                // Extract columns from the temp table metadata
                List<Column> columns = tempTableStreamingResult.tempTableColumnMetaData.stream()
                        .map(c -> new Column(c.column.label, c.column.dataType))
                        .collect(Collectors.toList());

                // Create the table
                String createTableSQL = dbCommands.createTempTable(tableName, columns);
                checkedExecute(statement, createTableSQL);

                // Stream data directly to the table
                streamTempTableResultToTable(tempTableStreamingResult, statement, columns);
            }
            else
            {
                throw new RuntimeException("Result not supported yet: " + result.getClass().getName());
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

        return true;
    }


    private void streamTempTableResultToTable(TempTableStreamingResult tempTableStreamingResult, Statement statement, List<Column> columns) throws SQLException
    {
        String columnMetadata = "(" + columns.stream().map(c -> c.name).collect(Collectors.joining(", ")) + ")";
        // Stream objects directly to the table
        tempTableStreamingResult.inputStream.forEach(obj ->
        {
            try
            {
                StringBuilder insertSQL = new StringBuilder("INSERT INTO " + tableName + columnMetadata + " VALUES (");

                // Extract values from the object based on column names
                for (int i = 0; i < columns.size(); i++)
                {
                    Object value = extractValueFromObject(obj, columns.get(i).name);
                    insertSQL.append(formatValueForInsert(value));

                    if (i < columns.size() - 1)
                    {
                        insertSQL.append(", ");
                    }
                }

                insertSQL.append(")");
                checkedExecute(statement, insertSQL.toString());
            }
            catch (Exception e)
            {
                throw new RuntimeException("Error streaming temp table result to table", e);
            }
        });
    }

    private Object extractValueFromObject(Object obj, String fieldName)
    {
        try
        {
            if (obj instanceof ObjectNode)
            {
                return ((com.fasterxml.jackson.databind.node.ObjectNode) obj).get(fieldName);
            }
            throw new UnsupportedOperationException("Unsupported Type" + obj.getClass().getName());
        }
        catch (Exception e)
        {
            throw new RuntimeException("Could not extract field " + fieldName + " from object", e);
        }
    }

    private String formatValueForInsert(Object value)
    {
        if (value == null)
        {
            return "NULL";
        }

        // TODO: Use ResultNormalizer to properly format values for SQL and mantain type integrity
        //Object normalizedValue = ResultNormalizer.normalizeToSql(value, this.databaseTimeZone);
        if (value instanceof TextNode && !((TextNode) value).canConvertToLong())
        {
            return "\'" + ((TextNode) value).asText() + "\'";
        }
        else
        {
            value.toString();
        }
        throw new UnsupportedOperationException("Unsupported Type" + value.getClass().getName());

    }

    private static boolean checkedExecute(Statement statement, String sql)
    {
        try (Scope ignored = GlobalTracer.get().buildSpan("temp table sql execution").withTag("sql", sql).startActive(true))
        {
            LOGGER.info(new LogInfo(Identity.getAnonymousIdentity().getName(), LoggingEventType.EXECUTION_RELATIONAL_COMMIT, sql, 0.0d).toString());
            return statement.execute(sql);
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }
}
