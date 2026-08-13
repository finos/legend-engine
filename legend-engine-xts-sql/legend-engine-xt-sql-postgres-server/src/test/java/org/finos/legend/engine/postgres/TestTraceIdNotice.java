// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.postgres;

import io.dropwizard.testing.junit.ResourceTestRule;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.postgres.config.ServerConfig;
import org.finos.legend.engine.postgres.handler.legend.LegendTdsTestClient;
import org.finos.legend.engine.postgres.protocol.sql.SQLManager;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.sql.LegendExecutionService;
import org.finos.legend.engine.postgres.protocol.wire.auth.identity.AnonymousIdentityProvider;
import org.finos.legend.engine.postgres.protocol.wire.auth.method.NoPasswordAuthenticationMethod;
import org.finos.legend.engine.postgres.protocol.wire.serialization.Messages;
import org.finos.legend.engine.query.sql.api.execute.SqlExecuteTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.postgresql.PGProperty;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Properties;

public class TestTraceIdNotice
{
    private static OpenTelemetrySdk openTelemetrySdk;

    static
    {
        // Register a real OpenTelemetry SDK so that spans produce valid trace ids.
        // This must happen before OpenTelemetryUtil class is loaded.
        // Reset first in case another test in the same JVM already registered a global instance.
        GlobalOpenTelemetry.resetForTest();
        openTelemetrySdk = OpenTelemetrySdk.builder()
                .setTracerProvider(SdkTracerProvider.builder().build())
                .buildAndRegisterGlobal();
    }

    @ClassRule
    public static final ResourceTestRule resources = SqlExecuteTest.getResourceTestRule();
    private static TestPostgresServer testPostgresServer;

    @BeforeClass
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

    @AfterClass
    public static void tearDown()
    {
        if (testPostgresServer != null)
        {
            testPostgresServer.stopListening();
            testPostgresServer.shutDown();
        }
        if (openTelemetrySdk != null)
        {
            openTelemetrySdk.close();
            GlobalOpenTelemetry.resetForTest();
        }
    }

    @Test
    public void testTraceIdNoticeSentOnSimpleQuery() throws SQLException
    {
        Properties info = new Properties();
        PGProperty.USER.set(info, "dummy");
        PGProperty.PASSWORD.set(info, "dummy");
        PGProperty.PREFER_QUERY_MODE.set(info, "simple");

        try (
                Connection connection = DriverManager.getConnection(
                        "jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres", info);
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT * FROM service.\"/personService\"")
        )
        {
            // Consume result set
            while (resultSet.next())
            {
                // no-op
            }

            SQLWarning warning = statement.getWarnings();
            Assert.assertNotNull("Expected a notice with the trace id", warning);
            Assert.assertTrue("Notice should contain traceId",
                    warning.getMessage().contains("traceId:"));
        }
    }

    @Test
    public void testTraceIdNoticeSentOnExtendedQuery() throws SQLException
    {
        try (
                Connection connection = DriverManager.getConnection(
                        "jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres",
                        "dummy", "dummy");
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM service.\"/personService\"");
                ResultSet resultSet = statement.executeQuery()
        )
        {
            while (resultSet.next())
            {
                // no-op
            }

            SQLWarning warning = statement.getWarnings();
            Assert.assertNotNull("Expected a notice with the trace id", warning);
            Assert.assertTrue("Notice should contain traceId",
                    warning.getMessage().contains("traceId:"));
        }
    }

    @Test
    public void testTraceIdNoticeSentOnFailure() throws SQLException
    {
        Properties info = new Properties();
        PGProperty.USER.set(info, "dummy");
        PGProperty.PASSWORD.set(info, "dummy");
        PGProperty.PREFER_QUERY_MODE.set(info, "simple");
        PGProperty.LOG_SERVER_ERROR_DETAIL.set(info, "false");

        try (
                Connection connection = DriverManager.getConnection(
                        "jdbc:postgresql://127.0.0.1:" + testPostgresServer.getLocalAddress().getPort() + "/postgres", info);
                Statement statement = connection.createStatement()
        )
        {
            try
            {
                statement.executeQuery("SELECT * FROM service.\"/nonExistentService\"");
                Assert.fail("Expected an exception");
            }
            catch (SQLException e)
            {
                // Expected — the query fails
            }

            SQLWarning warning = statement.getWarnings();
            Assert.assertNotNull("Expected a notice with trace id even on failure", warning);
            Assert.assertTrue("Notice should contain traceId",
                    warning.getMessage().contains("traceId:"));
        }
    }
}

