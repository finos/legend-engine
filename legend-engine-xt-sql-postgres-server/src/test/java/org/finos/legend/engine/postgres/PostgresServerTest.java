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

import com.gs.tablasco.TableVerifier;
import com.gs.tablasco.verify.ResultSetTable;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.postgres.auth.AnonymousIdentityProvider;
import org.finos.legend.engine.postgres.auth.NoPasswordAuthenticationMethod;
import org.finos.legend.engine.postgres.config.ServerConfig;
import org.finos.legend.engine.postgres.handler.legend.LegendSessionFactory;
import org.finos.legend.engine.postgres.handler.legend.LegendTdsTestClient;
import org.finos.legend.engine.query.sql.api.execute.SqlExecuteTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Enclosed.class)
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
        LegendSessionFactory legendSessionFactory = new LegendSessionFactory(client);

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(0);

        testPostgresServer = new TestPostgresServer(serverConfig, legendSessionFactory, (user, connectionProperties) -> new NoPasswordAuthenticationMethod(new AnonymousIdentityProvider()));
        testPostgresServer.startUp();
    }

    @RunWith(Parameterized.class)
    public static class PostgresServerVerifyResultSetTest
    {
        @Rule
        public TableVerifier verifier = new TableVerifier()
                .withMavenDirectoryStrategy()
//                uncomment line below when adding new tests, to create the 'expected' files
//                .withRebase()
                ;

        @Parameterized.Parameter
        public String query;

        @Parameterized.Parameters
        public static Iterable<String> queries()
        {
            return Lists.immutable.of(
                    "SELECT 1",
                    "SELECT * FROM service.\"/personService\"",
                    "SELECT * FROM service('/personService')",
                    "SELECT * FROM information_schema.schemata",
                    "SELECT * FROM pg_catalog.pg_tablespace",
                    "SHOW TRANSACTION ISOLATION LEVEL"
            );
        }

        @Test
        public void queryTest() throws SQLException
        {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()
            )
            {
                verifier.verify(query, ResultSetTable.create(resultSet));
            }
        }
    }

    public static class PostgresServerOtherFeaturesTest
    {

        @Test
        public void testMetadata() throws SQLException
        {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM service.\"/personService\"");
                 ResultSet resultSet = statement.executeQuery()
            )
            {
                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                Assert.assertEquals(3, resultSetMetaData.getColumnCount());
                Assert.assertEquals("Id", resultSetMetaData.getColumnName(1));
                Assert.assertEquals("Name", resultSetMetaData.getColumnName(2));
                Assert.assertEquals("Employee Type", resultSetMetaData.getColumnName(3));
                Assert.assertEquals("int4", resultSetMetaData.getColumnTypeName(1));
                Assert.assertEquals("varchar", resultSetMetaData.getColumnTypeName(2));
                Assert.assertEquals("varchar", resultSetMetaData.getColumnTypeName(3));
            }
        }

        @Test
        public void testParameterMetadata() throws SQLException
        {
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT * FROM service.\"/personService\"")
            )
            {
                ParameterMetaData parameterMetaData = statement.getParameterMetaData();
                Assert.assertEquals(0, parameterMetaData.getParameterCount());
            }
        }

        @Test
        public void testConnectionIsValid() throws SQLException
        {
            try (Connection connection = getConnection()
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
            try (Connection connection = getConnection();
                 PreparedStatement statement = connection.prepareStatement("")
            )
            {
                int rowCount = statement.executeUpdate();
                Assert.assertEquals(0, rowCount);
            }
        }

        @Test
        public void testHikariConnection() throws SQLException
        {
            HikariConfig jdbcConfig = new HikariConfig();
            jdbcConfig.setJdbcUrl("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres");
            jdbcConfig.setUsername("dummy");
            try (HikariDataSource dataSource = new HikariDataSource(jdbcConfig);
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
    }

    private static Connection getConnection() throws SQLException
    {
        return DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                "dummy", "dummy");
    }

    @AfterClass
    public static void tearDown()
    {
        testPostgresServer.stopListening();
        testPostgresServer.shutDown();
    }
}