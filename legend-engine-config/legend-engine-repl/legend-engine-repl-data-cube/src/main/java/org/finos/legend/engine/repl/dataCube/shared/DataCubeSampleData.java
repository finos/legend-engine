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

package org.finos.legend.engine.repl.dataCube.shared;

import org.apache.commons.io.IOUtils;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseConnection;
import org.finos.legend.engine.repl.client.Client;
import org.finos.legend.engine.repl.dataCube.commands.DataCube;
import org.finos.legend.engine.repl.relational.schema.Table;
import org.finos.legend.engine.repl.relational.shared.ConnectionHelper;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Objects;

import static org.finos.legend.engine.repl.relational.schema.MetadataReader.getTables;

public class DataCubeSampleData
{
    public static final DataCubeSampleData SPORT = new DataCubeSampleData("sport", "sample__sport", "org/finos/legend/engine/repl/dataCube/walkthrough/sport-data.csv", Lists.mutable.with("Athlete", "Age", "Country", "Year", "Date", "Sport", "Gold", "Silver", "Bronze"));
    public static final DataCubeSampleData TREE = new DataCubeSampleData("tree", "sample__tree", "org/finos/legend/engine/repl/dataCube/walkthrough/tree-data.csv", Lists.mutable.with("city", "country", "year", "tree"));

    public final String name;
    public final String tableName;
    public final String csvFilePath;
    public final MutableList<String> expectedColumns;

    public DataCubeSampleData(String name, String tableName, String csvFilePath, MutableList<String> expectedColumns)
    {
        this.name = name;
        this.tableName = tableName;
        this.csvFilePath = csvFilePath;
        this.expectedColumns = expectedColumns;
    }

    public void load(Client client)
    {
        DatabaseConnection databaseConnection = ConnectionHelper.getDatabaseConnection(client.getModelState().parse(), DataCube.getLocalConnectionPath());
        try (
                InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(this.csvFilePath);
                Connection connection = ConnectionHelper.getConnection(databaseConnection, client.getPlanExecutor());
                Statement statement = connection.createStatement())
        {
            MutableList<Table> tables = getTables(connection);
            if (tables.anySatisfy(t -> t.name.equals(this.tableName)))
            {
                statement.executeUpdate(DatabaseManager.fromString(databaseConnection.type.name()).relationalDatabaseSupport().dropTable(tableName));
            }
            Path tempFile = Files.createTempFile("sample-data" + this.name, ".csv");
            FileOutputStream fos = new FileOutputStream(tempFile.toFile());
            IOUtils.copy(Objects.requireNonNull(inputStream, "Can't extract sample data '" + this.name + "' from " + this.csvFilePath), fos);
            statement.executeUpdate(DatabaseManager.fromString(databaseConnection.type.name()).relationalDatabaseSupport().load(tableName, tempFile.toString()));

            // post check
            tables = getTables(connection);
            Table table = tables.detect(t -> t.name.equals(this.tableName));
            if (!Arrays.equals(table.columns.collect(column -> column.name).toArray(), this.expectedColumns.toArray()))
            {
                throw new RuntimeException("Sample data '" + this.name + "' does not have the expected columns " + this.expectedColumns.makeString("(", ",", ")") + " (got: " + table.columns.collect(column -> column.name).makeString(",") + ")");
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
