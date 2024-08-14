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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.postgres.auth.AnonymousIdentityProvider;
import org.finos.legend.engine.postgres.auth.NoPasswordAuthenticationMethod;
import org.finos.legend.engine.postgres.config.ServerConfig;
import org.finos.legend.engine.postgres.handler.legend.LegendExecutionService;
import org.finos.legend.engine.postgres.handler.legend.LegendSessionFactory;
import org.finos.legend.engine.postgres.handler.legend.LegendTdsTestClient;
import org.finos.legend.engine.query.sql.api.execute.SqlExecuteTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;

public class PostgresServerTest
{
    @ClassRule
    public static final ResourceTestRule resources;
    private static TestPostgresServer testPostgresServer;

    static
    {
        Pair<PureModel, ResourceTestRule> pureModelResourceTestRulePair = SqlExecuteTest.getPureModelResourceTestRulePair();
        resources = pureModelResourceTestRulePair.getTwo();
    }

    @BeforeClass
    public static void setUp()
    {
        LegendTdsTestClient client = new LegendTdsTestClient(resources);
        LegendSessionFactory legendSessionFactory = new LegendSessionFactory(new LegendExecutionService(client));

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(0);

        testPostgresServer = new TestPostgresServer(serverConfig, legendSessionFactory,
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
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM service.\"/personService\"");
        )
        {
            ResultSetMetaData resultSetMetaData = statement.getMetaData();
            Assert.assertEquals(5, resultSetMetaData.getColumnCount());
            Assert.assertEquals("Id", resultSetMetaData.getColumnName(1));
            Assert.assertEquals("Name", resultSetMetaData.getColumnName(2));
            Assert.assertEquals("Employee Type", resultSetMetaData.getColumnName(3));
            Assert.assertEquals("Full Name", resultSetMetaData.getColumnName(4));
            Assert.assertEquals("Derived Name", resultSetMetaData.getColumnName(5));
            Assert.assertEquals("int8", resultSetMetaData.getColumnTypeName(1));
            Assert.assertEquals("varchar", resultSetMetaData.getColumnTypeName(2));
            Assert.assertEquals("varchar", resultSetMetaData.getColumnTypeName(3));
            Assert.assertEquals("varchar", resultSetMetaData.getColumnTypeName(4));
            Assert.assertEquals("varchar", resultSetMetaData.getColumnTypeName(5));
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
            Assert.assertEquals(0, parameterMetaData.getParameterCount());
        }
    }

    /**
     * Verify that schema created as part of the metadata H2 DB creation exits
     * @throws SQLException on errors
     */
    @Test
    public void testInitSchemaCreation() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("select nspname,relname from pg_catalog.pg_namespace n " +
                        "inner join pg_catalog.pg_class c on n.oid = c.relnamespace where nspname = 'service' and c.relname= 'emptytable'");
                ResultSet resultSet = statement.executeQuery()
        )
        {
            int rows = 0;
            while (resultSet.next())
            {
                rows++;
            }
            Assert.assertEquals(1, rows);
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
            Assert.assertEquals(4, rows);
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
            Assert.assertEquals(4, rows);
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
            Assert.assertEquals(4, rows);
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
            Assert.assertEquals(1, rows);
        }
    }

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
            Assert.assertEquals(1, rows);
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
            Assert.assertEquals(1, rows);
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
            Assert.assertEquals(4, rows);
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
            Assert.assertEquals(1, rows);
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
            Assert.assertTrue(isValid);
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
            Assert.assertEquals(0, rowCount);
        }
    }

    @Test
    public void testUnknownServiceInExecution() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT blah FROM service('/blah')");
        )
        {
            PSQLException exception = Assert.assertThrows(PSQLException.class, statement::executeQuery);
            ServerErrorMessage serverErrorMessage = exception.getServerErrorMessage();
            Assert.assertNotNull(serverErrorMessage);
            Assert.assertEquals("IllegalArgumentException: No Service found for pattern '/blah'", serverErrorMessage.getMessage());
        }
    }

    @Test
    public void testUnknownColumnInExecution() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT \"some_random_column_name\" FROM service('/personService')");
        )
        {
            PSQLException exception = Assert.assertThrows(PSQLException.class, statement::executeQuery);
            ServerErrorMessage serverErrorMessage = exception.getServerErrorMessage();
            Assert.assertNotNull(serverErrorMessage);
            Assert.assertNotNull(serverErrorMessage.getMessage());
            Assert.assertTrue(serverErrorMessage.getMessage().endsWith("\"no column found named some_random_column_name\""));
        }
    }

    @Test
    public void testUnknownServiceInSchema() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT blah FROM service('/blah')");
        )
        {
            PSQLException exception = Assert.assertThrows(PSQLException.class, statement::getMetaData);
            ServerErrorMessage serverErrorMessage = exception.getServerErrorMessage();
            Assert.assertNotNull(serverErrorMessage);
            Assert.assertEquals("IllegalArgumentException: No Service found for pattern '/blah'", serverErrorMessage.getMessage());
        }
    }

    @Test
    public void testUnknownColumnInSchema() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT \"some_random_column_name\" FROM service('/personService')");
        )
        {
            PSQLException exception = Assert.assertThrows(PSQLException.class, statement::getMetaData);
            ServerErrorMessage serverErrorMessage = exception.getServerErrorMessage();
            Assert.assertNotNull(serverErrorMessage);
            Assert.assertNotNull(serverErrorMessage.getMessage());
            Assert.assertTrue(serverErrorMessage.getMessage().endsWith("\"no column found named some_random_column_name\""));
        }
    }

    @AfterClass
    public static void tearDown()
    {
        testPostgresServer.stopListening();
        testPostgresServer.shutDown();
    }
}
