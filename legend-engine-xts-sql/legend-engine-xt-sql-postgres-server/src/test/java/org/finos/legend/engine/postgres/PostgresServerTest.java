// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.postgres;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.postgres.config.ServerConfig;
import org.finos.legend.engine.postgres.handler.legend.LegendTdsTestClient;
import org.finos.legend.engine.postgres.protocol.sql.SQLManager;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.sql.LegendExecutionService;
import org.finos.legend.engine.postgres.protocol.wire.auth.identity.AnonymousIdentityProvider;
import org.finos.legend.engine.postgres.protocol.wire.auth.method.NoPasswordAuthenticationMethod;
import org.finos.legend.engine.postgres.protocol.wire.serialization.Messages;
import org.finos.legend.engine.query.sql.api.execute.SqlExecuteTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.postgresql.PGProperty;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

@org.junit.jupiter.api.extension.ExtendWith(DropwizardExtensionsSupport.class)
public class PostgresServerTest
{
    public static final ResourceExtension resources = SqlExecuteTest.getResourceTestRule();
    private static TestPostgresServer testPostgresServer;

    static
    {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    @BeforeAll
    public static void setUp()
    {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(0);
        serverConfig.setHttpPort(0);

        testPostgresServer = new TestPostgresServer(serverConfig,
                new SQLManager(Lists.mutable.with(new LegendExecutionService(new LegendTdsTestClient(resources)))),
                (user, connectionProperties) -> new NoPasswordAuthenticationMethod(new AnonymousIdentityProvider()),
                new Messages(Throwable::getMessage));
        testPostgresServer.startUp();
    }

    @Test
    public void testMetadata() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM service.\"/personService\"")
        )
        {
            ResultSetMetaData resultSetMetaData = statement.getMetaData();
            Assertions.assertEquals(5, resultSetMetaData.getColumnCount());
            Assertions.assertEquals("Id", resultSetMetaData.getColumnName(1));
            Assertions.assertEquals("Name", resultSetMetaData.getColumnName(2));
            Assertions.assertEquals("Employee Type", resultSetMetaData.getColumnName(3));
            Assertions.assertEquals("Full Name", resultSetMetaData.getColumnName(4));
            Assertions.assertEquals("Derived Name", resultSetMetaData.getColumnName(5));
            Assertions.assertEquals("int8", resultSetMetaData.getColumnTypeName(1));
            Assertions.assertEquals("varchar", resultSetMetaData.getColumnTypeName(2));
            Assertions.assertEquals("varchar", resultSetMetaData.getColumnTypeName(3));
            Assertions.assertEquals("varchar", resultSetMetaData.getColumnTypeName(4));
            Assertions.assertEquals("varchar", resultSetMetaData.getColumnTypeName(5));
        }
    }

    @Test
    public void testParameterMetadata() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM service.\"/personService\"")
        )
        {
            ParameterMetaData parameterMetaData = statement.getParameterMetaData();
            Assertions.assertEquals(0, parameterMetaData.getParameterCount());
        }
    }

    @Test
    public void testMultipleParameterMetadata() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM pg_catalog.pg_tablespace where 1 = ? and 2 = ?")
        )
        {
            statement.setInt(1, 2);
            statement.setInt(2, 2);
            ParameterMetaData parameterMetaData = statement.getParameterMetaData();
            ResultSet resultSet = statement.executeQuery();

            int rows = 0;
            while (resultSet.next())
            {
                rows++;
            }
            Assertions.assertEquals(0, rows);
            Assertions.assertEquals(2, parameterMetaData.getParameterCount());
        }
    }

    @Test
    public void testNumberOfRows() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM service.\"/personService\"");
                ResultSet resultSet = statement.executeQuery()
        )
        {
            int rows = 0;
            while (resultSet.next())
            {
                rows++;
            }
            Assertions.assertEquals(4, rows);
        }
    }

    @Test
    public void testSimpleQuery() throws SQLException
    {
        Properties info = new Properties();
        PGProperty.USER.set(info, "dummy");
        PGProperty.PASSWORD.set(info, "dummy");
        PGProperty.PREFER_QUERY_MODE.set(info, "simple");
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres", info);
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM service.\"/personService\"");
                ResultSet resultSet = statement.executeQuery()
        )
        {
            int rows = 0;
            while (resultSet.next())
            {
                rows++;
            }
            Assertions.assertEquals(4, rows);
        }
    }

    @Test
    public void concurrentConnectionTest() throws SQLException
    {
        try (
                Connection connection1 = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                Connection connection2 = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement1 = connection1.prepareStatement("SELECT * FROM service.\"/personService\"");
                PreparedStatement statement2 = connection2.prepareStatement("SELECT * FROM service.\"/personService\"");
                ResultSet resultSet1 = statement1.executeQuery();
                ResultSet resultSet2 = statement2.executeQuery()
        )
        {
            int rows1 = 0;
            while (resultSet1.next())
            {
                rows1++;
            }
            Assertions.assertEquals(4, rows1);
            int rows2 = 0;
            while (resultSet2.next())
            {
                rows2++;
            }
            Assertions.assertEquals(4, rows2);
        }
    }

    @Test
    public void testSuccessAfterFailure() throws SQLException
    {
        Properties info = new Properties();
        PGProperty.USER.set(info, "dummy");
        PGProperty.PASSWORD.set(info, "dummy");
        PGProperty.LOG_SERVER_ERROR_DETAIL.set(info, "false");

        try (
                Connection connection1 = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        info);
                PreparedStatement statement1 = connection1.prepareStatement("SELECT * FROM service.\"/personServiceNonExistent\"");
                PreparedStatement statement2 = connection1.prepareStatement("SELECT * FROM service.\"/personService\"")
        )
        {

            PSQLException exception = Assertions.assertThrows(PSQLException.class, statement1::executeQuery);
            Assertions.assertEquals("ERROR: IllegalArgumentException: No Service found for pattern '/personServiceNonExistent'", exception.getMessage());
            int rows2 = 0;
            ResultSet resultSet2 = statement2.executeQuery();
            while (resultSet2.next())
            {
                rows2++;
            }
            Assertions.assertEquals(4, rows2);
        }
    }

    @Test
    public void testSuccessAfterFailureInSimple() throws SQLException
    {
        Properties info = new Properties();
        PGProperty.USER.set(info, "dummy");
        PGProperty.PASSWORD.set(info, "dummy");
        PGProperty.PREFER_QUERY_MODE.set(info, "simple");
        PGProperty.LOG_SERVER_ERROR_DETAIL.set(info, "false");

        try (
                Connection connection1 = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        info);
                PreparedStatement statement1 = connection1.prepareStatement("SELECT * FROM service.\"/personServiceNonExistent\"");
                PreparedStatement statement2 = connection1.prepareStatement("SELECT * FROM service.\"/personService\"")
        )
        {

            PSQLException exception = Assertions.assertThrows(PSQLException.class, statement1::executeQuery);
            Assertions.assertEquals("ERROR: IllegalArgumentException: No Service found for pattern '/personServiceNonExistent'", exception.getMessage());
            int rows2 = 0;
            ResultSet resultSet2 = statement2.executeQuery();
            while (resultSet2.next())
            {
                rows2++;
            }
            Assertions.assertEquals(4, rows2);
        }
    }

    @Test
    public void testTableFunctionSyntax() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM service('/personService')");
                ResultSet resultSet = statement.executeQuery()
        )
        {
            int rows = 0;
            while (resultSet.next())
            {
                rows++;
            }
            Assertions.assertEquals(4, rows);
        }
    }

    @Test
    public void testAutoCommitOff() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy")
        )
        {
            connection.setAutoCommit(false);
            Assertions.assertFalse(connection.getAutoCommit());
        }
    }

    @Test
    public void testFetchSizePreparedStatement() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy")
        )
        {
            connection.setAutoCommit(false);
            PreparedStatement statement = connection.prepareStatement("SELECT * FROM service('/personService')");
            statement.setFetchSize(1);
            ResultSet resultSet = statement.executeQuery();
            int rows = 0;
            while (resultSet.next())
            {
                rows++;
            }
            Assertions.assertEquals(4, rows);
        }
    }

    @Test
    public void testTableFunctionwithDecimal() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM service('/personRatings')");
                ResultSet resultSet = statement.executeQuery()
        )
        {
            int rows = 0;
            while (resultSet.next())
            {
                rows++;
            }
            Assertions.assertEquals(4, rows);
        }
    }

    @Test
    public void testSelectWithoutTable() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT 1");
                ResultSet resultSet = statement.executeQuery()
        )
        {
            int rows = 0;
            while (resultSet.next())
            {
                rows++;
            }
            Assertions.assertEquals(1, rows);
        }
    }

    /**
     * This query format is used by the postgres jdbc driver
     */
    @Test
    public void testShowTxn() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SHOW TRANSACTION ISOLATION LEVEL");
                ResultSet resultSet = statement.executeQuery()
        )
        {
            int rows = 0;
            while (resultSet.next())
            {
                rows++;
            }
            Assertions.assertEquals(1, rows);
        }
    }

    /**
     * This query format is used by the postgres odbc driver
     */
    @Test
    public void testShowTxnOdbc() throws SQLException
    {
        String sql = "SHOW transaction_isolation";
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement preparedStatement = connection.prepareStatement(sql);
                ResultSet psResultSet = preparedStatement.executeQuery();
                Statement statement = connection.createStatement()
        )
        {
            int psRows = 0;
            while (psResultSet.next())
            {
                psRows++;
            }
            Assertions.assertEquals(1, psRows);

            Assertions.assertTrue(statement.execute(sql));
            ResultSet statementResultSet = statement.getResultSet();
            int statementRows = 0;
            while (statementResultSet.next())
            {
                statementRows++;
            }
            Assertions.assertEquals(1, statementRows);
        }
    }

    @Test
    public void testHikariConnection() throws SQLException
    {
        HikariConfig jdbcConfig = new HikariConfig();
        jdbcConfig.setJdbcUrl("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres");
        jdbcConfig.setUsername("dummy");
        try (
                HikariDataSource dataSource = new HikariDataSource(jdbcConfig);
                Connection connection = dataSource.getConnection();
                PreparedStatement preparedStatement = connection.prepareStatement("SELECT 1");
                ResultSet resultSet = preparedStatement.executeQuery()
        )
        {
            int rows = 0;
            while (resultSet.next())
            {
                rows++;
            }
            Assertions.assertEquals(1, rows);
        }
    }

    @Test
    public void testInformationSchema() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM information_schema.schemata");
                ResultSet resultSet = statement.executeQuery()
        )
        {
            int rows = 0;
            while (resultSet.next())
            {
                rows++;
            }
            Assertions.assertEquals(6, rows);
        }
    }

    @Test
    public void testPgCatalog() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM pg_catalog.pg_tablespace");
                ResultSet resultSet = statement.executeQuery()
        )
        {
            int rows = 0;
            while (resultSet.next())
            {
                rows++;
            }
            Assertions.assertEquals(2, rows);
        }
    }

    @Test
    public void testPgType() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT oid, typbasetype FROM pg_type");
                ResultSet resultSet = statement.executeQuery()
        )
        {
            int rows = 0;
            while (resultSet.next())
            {
                rows++;
            }
            Assertions.assertEquals(375, rows);
        }
    }

    @Test
    public void testConnectionIsValid() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy")
        )
        {
            // This triggers an empty query and expects an empty response
            boolean isValid = connection.isValid(1);
            Assertions.assertTrue(isValid);
        }
    }

    @Test
    public void testEmptyQuery() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("")
        )
        {
            int rowCount = statement.executeUpdate();
            Assertions.assertEquals(0, rowCount);
        }
    }

    @Test
    public void testUnknownServiceInExecution() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT blah FROM service('/blah')")
        )
        {
            PSQLException exception = Assertions.assertThrows(PSQLException.class, statement::executeQuery);
            ServerErrorMessage serverErrorMessage = exception.getServerErrorMessage();
            Assertions.assertNotNull(serverErrorMessage);
            Assertions.assertEquals("IllegalArgumentException: No Service found for pattern '/blah'", serverErrorMessage.getMessage());
        }
    }

    @Test
    public void testUnknownColumnInExecution() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT \"some_random_column_name\" FROM service('/personService')")
        )
        {
            PSQLException exception = Assertions.assertThrows(PSQLException.class, statement::executeQuery);
            ServerErrorMessage serverErrorMessage = exception.getServerErrorMessage();
            Assertions.assertNotNull(serverErrorMessage);
            Assertions.assertNotNull(serverErrorMessage.getMessage());
            Assertions.assertTrue(serverErrorMessage.getMessage().endsWith("\"no column found named: 'some_random_column_name'. Available columns: [Id, Name, Employee Type, Full Name, Derived Name]\""));
        }
    }

    @Test
    public void testUnknownServiceInSchema() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT blah FROM service('/blah')")
        )
        {
            PSQLException exception = Assertions.assertThrows(PSQLException.class, statement::getMetaData);
            ServerErrorMessage serverErrorMessage = exception.getServerErrorMessage();
            Assertions.assertNotNull(serverErrorMessage);
            Assertions.assertEquals("IllegalArgumentException: No Service found for pattern '/blah'", serverErrorMessage.getMessage());
        }
    }

    @Test
    public void testUnknownColumnInSchema() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT \"some_random_column_name\" FROM service('/personService')")
        )
        {
            PSQLException exception = Assertions.assertThrows(PSQLException.class, statement::getMetaData);
            ServerErrorMessage serverErrorMessage = exception.getServerErrorMessage();
            Assertions.assertNotNull(serverErrorMessage);
            Assertions.assertNotNull(serverErrorMessage.getMessage());
            Assertions.assertTrue(serverErrorMessage.getMessage().endsWith("\"no column found named: 'some_random_column_name'. Available columns: [Id, Name, Employee Type, Full Name, Derived Name]\""));
        }
    }

    @Test
    @Disabled
    public void testLotsOfConnectionsBadConnectionManagementPreparedStatement() throws SQLException
    {
        for (int i = 0; i < 500; i++)
        {
            //deliberately not closing connections or statement to simulate bad connection management
            Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                    "dummy", "dummy");

            PreparedStatement statement = connection.prepareStatement("SELECT * FROM pg_catalog.pg_tablespace");

            int numberOfColumns = statement.getMetaData().getColumnCount();
            Assertions.assertEquals(4, numberOfColumns);

            //we do twice to ensure reuse works as expected
            testLotsOfConnections(statement.executeQuery());
            testLotsOfConnections(statement.executeQuery());
        }
    }


    @Test
    @Disabled
    public void testLotsOfConnectionsGoodConnectionManagementPreparedStatement() throws SQLException
    {
        for (int i = 0; i < 500; i++)
        {
            try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");

                PreparedStatement statement = connection.prepareStatement("SELECT * FROM pg_catalog.pg_tablespace");
            )
            {
                int numberOfColumns = statement.getMetaData().getColumnCount();
                Assertions.assertEquals(4, numberOfColumns);

                //we do twice to ensure reuse works as expected
                testLotsOfConnections(statement.executeQuery());
                testLotsOfConnections(statement.executeQuery());
            }
        }
    }

    @Test
    @Disabled
    public void testLotsOfConnectionsBadConnectionManagementStatement() throws SQLException
    {
        for (int i = 0; i < 500; i++)
        {
            //deliberately not closing connections or statement to simulate bad connection management
            Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                    "dummy", "dummy");

            Statement statement = connection.createStatement();

            //we do twice to ensure reuse works as expected
            testLotsOfConnections(statement.executeQuery("SELECT * FROM pg_catalog.pg_tablespace"));
            testLotsOfConnections(statement.executeQuery("SELECT * FROM pg_catalog.pg_tablespace"));
        }
    }


    @Test
    @Disabled
    public void testLotsOfConnectionsGoodConnectionManagementStatement() throws SQLException
    {
        for (int i = 0; i < 500; i++)
        {
            try (
                    Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                            "dummy", "dummy");

                    Statement statement = connection.createStatement();
            )
            {
                //we do twice to ensure reuse works as expected
                testLotsOfConnections(statement.executeQuery("SELECT * FROM pg_catalog.pg_tablespace"));
                testLotsOfConnections(statement.executeQuery("SELECT * FROM pg_catalog.pg_tablespace"));
            }
        }
    }

    @Test
    @Disabled
    public void testLotsOfConcurrentConnectionsGoodConnectionManagementStatement() throws SQLException
    {
        for (int i = 0; i < 500; i++)
        {
            try (
                    Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                            "dummy", "dummy");

                    Statement statement = connection.createStatement();
            )
            {
                //we do twice to ensure reuse works as expected
                testLotsOfConnections(statement.executeQuery("SELECT * FROM pg_catalog.pg_tablespace"));
                testLotsOfConnections(statement.executeQuery("SELECT * FROM pg_catalog.pg_tablespace"));
            }
        }
    }

    @Test
    @Disabled
    public void testLotsOfConcurrentConnectionsBadConnectionManagementStatement() throws SQLException
    {
        List<Connection> connections = Lists.mutable.empty();
        for (int i = 0; i < 500; i++)
        {
            //deliberately not closing connections or statement to simulate bad connection management
            connections.add(DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                    "dummy", "dummy"));
        }

        for (Connection connection : connections)
        {
            Statement statement = connection.createStatement();
            testLotsOfConnections(statement.executeQuery("SELECT * FROM pg_catalog.pg_tablespace"));
        }
    }

    @Test
    @Disabled
    public void testLotsOfConcurrentConnectionsBadConnectionManagementPreparedStatement() throws SQLException
    {
        List<Connection> connections = Lists.mutable.empty();
        for (int i = 0; i < 500; i++)
        {
            //deliberately not closing connections or statement to simulate bad connection management
            connections.add(DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                    "dummy", "dummy"));

        }

        for (Connection connection : connections)
        {
            Statement statement = connection.createStatement();
            testLotsOfConnections(statement.executeQuery("SELECT * FROM pg_catalog.pg_tablespace"));
        }
    }

    private void testLotsOfConnections(ResultSet resultSet)
    {
        try
        {
            int rows = 0;
            while (resultSet.next())
            {
                rows++;
            }
            Assertions.assertEquals(2, rows);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }

    }

    @AfterAll
    public static void tearDown()
    {
        testPostgresServer.stopListening();
        testPostgresServer.shutDown();
    }
}
