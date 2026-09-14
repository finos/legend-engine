//  Copyright 2022 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake;

import net.snowflake.client.api.connection.SnowflakeConnection;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.QuoteMode;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.ImmutableMap;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.Column;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.IngestionMethod;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommandsVisitor;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SnowflakeCommands extends RelationalDatabaseCommands
{
    private static final ImmutableMap<String, String> columnTypeToSqlTextMap = Maps.immutable.of("BIT", "BOOLEAN");
    public static final CSVFormat TEMP_FILE_CSV_FORMAT = CSVFormat.DEFAULT.withQuoteMode(QuoteMode.ALL_NON_NULL);

    @Override
    public CSVFormat getCsvFormatForTempFile()
    {
        return TEMP_FILE_CSV_FORMAT;
    }

    @Override
    public String processTempTableName(String tempTableName)
    {
        return "LEGEND_TEMP_DB.LEGEND_TEMP_SCHEMA." + tempTableName;
    }

    public String tempStageName()
    {
        return "LEGEND_TEMP_DB.LEGEND_TEMP_SCHEMA.LEGEND_TEMP_STAGE";
    }

    @Override
    public String dropTempTable(String tableName)
    {
        return "Drop table if exists " + tableName;
    }

    @Override
    public List<String> createAndLoadTempTable(String tableName, List<Column> columns, String optionalCSVFileLocation)
    {
        throw new UnsupportedOperationException("Snowflake should use input stream to temp table load. This method should not be triggered for Snowflake.");
    }

    @Override
    public String createTempTable(String tableName, List<Column> columns)
    {
        return "CREATE TEMPORARY TABLE " + tableName + "(" + columns.stream().map(c -> c.name + " " + columnTypeToSqlTextMap.getIfAbsentValue(c.type, c.type)).collect(Collectors.joining(", ")) + ");";
    }

    @Override
    public String getSemiStructuredInsertStatement(String tableName, String columnName)
    {
        return "INSERT INTO " + tableName + " (" + columnName + ") SELECT parse_json(?)";
    }

    @Override
    public IngestionMethod getDefaultIngestionMethod()
    {
        return IngestionMethod.CLIENT_STREAM;
    }

    public boolean supportsHeaderOnCsvFile()
    {
        return false;
    }

    @Override
    public <T> T accept(RelationalDatabaseCommandsVisitor<T> visitor)
    {
        return visitor.visit(this);
    }

    /**
     * Streaming data to session-scoped temp table without creating file on local file system.
     * Source rows are consumed from {@code csvInputStream} and pushed straight to a Snowflake internal stage via the JDBC driver's {@code SnowflakeConnection.uploadStream} method.
     */
    @Override
    public void ingestFromStream(Connection connection, String tableName, List<Column> columns, InputStream csvInputStream) throws Exception
    {
        String stageFileName = "legend_stream_" + UUID.randomUUID().toString().replace("-", "") + ".csv";
        String stagedObjectName = stageFileName + ".gz";
        String createTable = "CREATE TEMPORARY TABLE " + tableName + " " + columns.stream().map(c -> c.name + " " + columnTypeToSqlTextMap.getIfAbsentValue(c.type, c.type)).collect(Collectors.joining(",", "(", ")"));
        String createStage = "CREATE OR REPLACE TEMPORARY STAGE " + tempStageName();
        String copyInto = "COPY INTO " + tableName + " FROM @" + tempStageName() + "/" + stagedObjectName + " file_format = (type = CSV field_optionally_enclosed_by= '\"')" + " ON_ERROR = ABORT_STATEMENT";
        String dropStage = "DROP STAGE " + tempStageName();

        try (Statement statement = connection.createStatement())
        {
            statement.execute(dropTempTable(tableName));
            statement.execute(createTable);
            statement.execute(createStage);
        }

        try
        {
            SnowflakeConnection snowflakeConnection = connection.unwrap(SnowflakeConnection.class);
            snowflakeConnection.uploadStream(tempStageName(), stageFileName, csvInputStream);

            try (Statement copyStatement = connection.createStatement())
            {
                copyStatement.execute(copyInto);
            }
        }
        finally
        {
            try (Statement dropStatement = connection.createStatement())
            {
                dropStatement.execute(dropStage);
            }
            catch (Exception dropEx)
            {
                //the stage is session-scoped and auto-drops with the session anyway so ignoring this exception to not mask exception with copy
            }
        }
    }
}
