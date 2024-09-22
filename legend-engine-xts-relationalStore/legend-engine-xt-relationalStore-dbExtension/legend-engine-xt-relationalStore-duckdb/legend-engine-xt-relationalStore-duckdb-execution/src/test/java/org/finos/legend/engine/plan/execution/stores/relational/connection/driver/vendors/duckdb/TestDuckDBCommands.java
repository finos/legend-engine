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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.duckdb;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.result.serialization.TemporaryFile;
import org.finos.legend.engine.plan.execution.stores.relational.activity.RelationalExecutionActivity;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.Column;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.result.RelationalResult;
import org.finos.legend.engine.plan.execution.stores.relational.result.SQLExecutionResult;
import org.finos.legend.engine.plan.execution.stores.relational.serialization.RelationalResultToCSVSerializer;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.RelationalTdsInstantiationExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.nodes.SQLExecutionNode;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.DataTypeResultType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DuckDBDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import javax.security.auth.Subject;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestDuckDBCommands
{
    @Rule
    public final TemporaryFolder TEMPORARY_FOLDER = new TemporaryFolder();

    private static final DuckDBCommands DUCK_DB_COMMANDS = new DuckDBCommands();
    private static final ConnectionManagerSelector CONNECTION_MANAGER_SELECTOR = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList(), Optional.empty());

    @AfterClass
    public static void tearDown() throws IOException
    {
        ConnectionStateManager.getInstance().close();
    }

    @Test
    public void testLoadCommand() throws Exception
    {
        try (
                Connection connection = CONNECTION_MANAGER_SELECTOR.getDatabaseConnection((Subject) null, this.testDuckDBConnection());
                Statement statement = connection.createStatement()
        )
        {
            String loadSql = DUCK_DB_COMMANDS.load("load_command_test_table", Objects.requireNonNull(TestDuckDBCommands.class.getClassLoader().getResource("personTable.csv")).getFile());
            statement.execute(loadSql);
            try (ResultSet rs = statement.executeQuery("select * from load_command_test_table"))
            {
                assertOnColumnCountAndColumnTypes(rs.getMetaData(), 4, "(id:BIGINT)|(firstName:VARCHAR)|(lastName:VARCHAR)|(age:BIGINT)");
                assertOnResultSetCSV(
                        rs,
                        "1,Peter,Smith,23\r\n" +
                        "2,John,Johnson,22\r\n" +
                        "3,John,Hill,12\r\n" +
                        "4,Anthony,Allen,22\r\n" +
                        "5,Fabrice,Roberts,34\r\n" +
                        "6,Oliver,Hill,32\r\n" +
                        "7,David,Harris,35\r\n"
                );
            }
        }
    }

    @Test
    public void testLoadCommandWithProvidedTypes() throws Exception
    {
        try (
                Connection connection = CONNECTION_MANAGER_SELECTOR.getDatabaseConnection((Subject) null, this.testDuckDBConnection());
                Statement statement = connection.createStatement()
        )
        {
            List<Column> columns = Arrays.asList(
                    new Column("id", "INT"),
                    new Column("firstName", "VARCHAR"),
                    new Column("lastName", "VARCHAR(32)"),
                    new Column("age", "DOUBLE")
            );
            String loadSql = DUCK_DB_COMMANDS.load("load_command_with_types_test_table", Objects.requireNonNull(TestDuckDBCommands.class.getClassLoader().getResource("personTable.csv")).getFile(), columns);
            statement.execute(loadSql);
            try (ResultSet rs = statement.executeQuery("select * from load_command_with_types_test_table"))
            {
                assertOnColumnCountAndColumnTypes(rs.getMetaData(), 4, "(id:INTEGER)|(firstName:VARCHAR)|(lastName:VARCHAR)|(age:DOUBLE)");
                assertOnResultSetCSV(
                        rs,
                        "1,Peter,Smith,23.0\r\n" +
                        "2,John,Johnson,22.0\r\n" +
                        "3,John,Hill,12.0\r\n" +
                        "4,Anthony,Allen,22.0\r\n" +
                        "5,Fabrice,Roberts,34.0\r\n" +
                        "6,Oliver,Hill,32.0\r\n" +
                        "7,David,Harris,35.0\r\n"
                );
            }
        }
    }

    @Test
    public void testLoadCommandWithProvidedTypesFromRelationalResult() throws Exception
    {
        RelationalResult relationalResult;
        try (TemporaryFile tempFile = new TemporaryFile(TEMPORARY_FOLDER.getRoot().getAbsolutePath()))
        {
            RelationalDatabaseConnection duckDbConnection = this.testDuckDBConnection();
            try (
                    Connection connection = CONNECTION_MANAGER_SELECTOR.getDatabaseConnection((Subject) null, duckDbConnection);
                    Statement statement = connection.createStatement()
            )
            {
                statement.execute(DUCK_DB_COMMANDS.load("load_command_with_types_test_table_1", Objects.requireNonNull(TestDuckDBCommands.class.getClassLoader().getResource("personTable.csv")).getFile()));
                SQLExecutionNode sqlExecutionNode = new SQLExecutionNode();
                sqlExecutionNode.isResultColumnsDynamic = true;
                sqlExecutionNode.connection = duckDbConnection;
                RelationalTdsInstantiationExecutionNode tdsNode = new RelationalTdsInstantiationExecutionNode();
                tdsNode.resultType = new DataTypeResultType();
                SQLExecutionResult sqlExecutionResult = new SQLExecutionResult(
                        Lists.mutable.of(new RelationalExecutionActivity("select firstName as \"First Name\", avg(age) as \"Average Age\" from load_command_with_types_test_table_1 group by firstName", "")),
                        sqlExecutionNode,
                        "DuckDB",
                        "UTC",
                        connection,
                        Identity.getAnonymousIdentity(),
                        Lists.mutable.empty(),
                        null
                );
                relationalResult = new RelationalResult(sqlExecutionResult, tdsNode);
                RelationalResultToCSVSerializer csvSerializer = new RelationalResultToCSVSerializer(relationalResult, true);
                tempFile.writeFile(csvSerializer);
                relationalResult.close();
            }

            try (
                    Connection connection = CONNECTION_MANAGER_SELECTOR.getDatabaseConnection((Subject) null, duckDbConnection);
                    Statement statement = connection.createStatement()
            )
            {
                String loadSql = DUCK_DB_COMMANDS.load("load_command_with_types_test_table_2", tempFile.path.toString(), relationalResult.getResultSetColumns());
                statement.execute(loadSql);
                try (ResultSet rs = statement.executeQuery("select * from load_command_with_types_test_table_2 order by \"Average Age\""))
                {
                    assertOnColumnCountAndColumnTypes(rs.getMetaData(), 2, "(First Name:VARCHAR)|(Average Age:DOUBLE)");
                    assertOnResultSetCSV(
                            rs,
                            "John,17.0\r\n" +
                            "Anthony,22.0\r\n" +
                            "Peter,23.0\r\n" +
                            "Oliver,32.0\r\n" +
                            "Fabrice,34.0\r\n" +
                            "David,35.0\r\n"
                    );
                }
            }
        }
    }

    private static void assertOnColumnCountAndColumnTypes(ResultSetMetaData resultSetMetaData, int expectedColumnCount, String expectedColumnTypes) throws SQLException
    {
        int columnCount = resultSetMetaData.getColumnCount();
        Assert.assertEquals(expectedColumnCount, columnCount);

        String columnsAndTypes = IntStream.range(1, expectedColumnCount + 1).mapToObj(i ->
        {
            try
            {
                return "(" + resultSetMetaData.getColumnLabel(i) + ":" + resultSetMetaData.getColumnTypeName(i) + ")";
            }
            catch (SQLException e)
            {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.joining("|"));
        Assert.assertEquals(expectedColumnTypes, columnsAndTypes);
    }

    private static void assertOnResultSetCSV(ResultSet resultSet, String expectedResult) throws SQLException, IOException
    {
        StringBuilder sb = new StringBuilder();
        try (CSVPrinter csvPrinter = new CSVPrinter(sb, CSVFormat.DEFAULT))
        {
            csvPrinter.printRecords(resultSet);
        }
        Assert.assertEquals(expectedResult, sb.toString());
    }

    private RelationalDatabaseConnection testDuckDBConnection() throws Exception
    {
        DuckDBDatasourceSpecification duckDBDatasourceSpecification = new DuckDBDatasourceSpecification();
        duckDBDatasourceSpecification.path = TEMPORARY_FOLDER.newFolder("duckDB").getAbsolutePath() + "/duck_db_file";
        TestDatabaseAuthenticationStrategy testDatabaseAuthSpec = new TestDatabaseAuthenticationStrategy();
        return new RelationalDatabaseConnection(duckDBDatasourceSpecification, testDatabaseAuthSpec, DatabaseType.DuckDB);
    }
}
