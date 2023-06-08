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

import io.dropwizard.testing.junit.ResourceTestRule;
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
import org.junit.Test;

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
        LegendTdsTestClient client = new LegendTdsTestClient(resources.target("sql/v1/execution/executeQueryString").request());
        LegendSessionFactory legendSessionFactory = new LegendSessionFactory(client);

        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(0);

        testPostgresServer = new TestPostgresServer(serverConfig, legendSessionFactory, (user, connectionProperties) -> new NoPasswordAuthenticationMethod(new AnonymousIdentityProvider()));
        testPostgresServer.startUp();
    }

    @Test
    public void testMetadata() throws SQLException
    {
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                "dummy", "dummy");
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM service.\"/personService\"");
             ResultSet resultSet = statement.executeQuery()
        )
        {
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            Assert.assertEquals(2, resultSetMetaData.getColumnCount());
            Assert.assertEquals("Id", resultSetMetaData.getColumnName(1));
            Assert.assertEquals("Name", resultSetMetaData.getColumnName(2));
            Assert.assertEquals("int4", resultSetMetaData.getColumnTypeName(1));
            Assert.assertEquals("varchar", resultSetMetaData.getColumnTypeName(2));
        }
    }

    @Test
    public void testParameterMetadata() throws SQLException
    {
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                "dummy", "dummy");
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM service.\"/personService\"")
        )
        {
            ParameterMetaData parameterMetaData = statement.getParameterMetaData();
            Assert.assertEquals(0, parameterMetaData.getParameterCount());
        }
    }

    @Test
    public void testNumberOfRows() throws SQLException
    {
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
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
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
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
    public void testSelectWithoutTable() throws SQLException
    {
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
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
    public void testInformationSchema() throws SQLException
    {
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
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
            Assert.assertEquals(3, rows);
        }
    }

    @Test
    public void testPgCatalog() throws SQLException
    {
        try (Connection connection = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
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

    @AfterClass
    public static void tearDown()
    {
        testPostgresServer.stopListening();
        testPostgresServer.shutDown();
    }
}