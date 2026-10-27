// Copyright 2026 Goldman Sachs
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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.dropwizard.testing.junit5.ResourceExtension;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.postgres.config.SSLConfig;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.postgresql.util.PSQLException;
import org.slf4j.bridge.SLF4JBridgeHandler;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

@ExtendWith(DropwizardExtensionsSupport.class)
public class PostgresServerSslTest
{
    private static final String KEYSTORE_PASSWORD = "test-password";
    public static final ResourceExtension resources = SqlExecuteTest.getResourceTestRule();
    private static TestPostgresServer sslOptionalServer;
    private static TestPostgresServer sslRequiredServer;
    private static TestPostgresServer mutualTlsServer;
    private static Path clientCertPath;
    private static Path clientKeyPath;

    static
    {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    @BeforeAll
    public static void setUp() throws Exception
    {
        Path keyStorePath = Files.createTempFile("legend-pg-test-keystore", ".p12");
        writeSelfSignedKeyStore(keyStorePath);
        keyStorePath.toFile().deleteOnExit();
        sslOptionalServer = startServer(sslConfig(keyStorePath, false));
        sslRequiredServer = startServer(sslConfig(keyStorePath, true));

        SelfSignedCertificate clientCert = new SelfSignedCertificate();
        clientCertPath = clientCert.certificate().toPath();
        clientKeyPath = writeClientKeyAsDer(clientCert);
        Path trustStorePath = writeClientTrustStore(clientCert.cert());
        mutualTlsServer = startServer(mutualTlsConfig(keyStorePath, trustStorePath));
    }

    private static Path writeClientKeyAsDer(SelfSignedCertificate clientCert) throws Exception
    {
        Path path = Files.createTempFile("legend-pg-test-client-key", ".pk8");
        Files.write(path, clientCert.key().getEncoded());
        path.toFile().deleteOnExit();
        return path;
    }

    private static Path writeClientTrustStore(Certificate clientCertificate) throws Exception
    {
        KeyStore trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(null, null);
        trustStore.setCertificateEntry("legend-client", clientCertificate);
        Path path = Files.createTempFile("legend-pg-test-truststore", ".p12");
        try (OutputStream out = Files.newOutputStream(path))
        {
            trustStore.store(out, KEYSTORE_PASSWORD.toCharArray());
        }
        path.toFile().deleteOnExit();
        return path;
    }

    private static void writeSelfSignedKeyStore(Path path) throws Exception
    {
        SelfSignedCertificate cert = new SelfSignedCertificate();
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, null);
        keyStore.setKeyEntry("legend-pg", cert.key(), KEYSTORE_PASSWORD.toCharArray(),
                new Certificate[]{cert.cert()});
        try (OutputStream out = Files.newOutputStream(path))
        {
            keyStore.store(out, KEYSTORE_PASSWORD.toCharArray());
        }
    }

    private static SSLConfig sslConfig(Path keyStorePath, boolean requireSsl) throws Exception
    {
        String json = "{"
                + "\"keyStorePath\":\"" + keyStorePath.toString().replace("\\", "\\\\") + "\","
                + "\"keyStorePassword\":\"" + KEYSTORE_PASSWORD + "\","
                + "\"keyStoreType\":\"PKCS12\","
                + "\"needClientAuth\":false,"
                + "\"requireSsl\":" + requireSsl
                + "}";
        return new ObjectMapper().readValue(json, SSLConfig.class);
    }

    private static SSLConfig mutualTlsConfig(Path keyStorePath, Path trustStorePath) throws Exception
    {
        String json = "{"
                + "\"keyStorePath\":\"" + keyStorePath.toString().replace("\\", "\\\\") + "\","
                + "\"keyStorePassword\":\"" + KEYSTORE_PASSWORD + "\","
                + "\"keyStoreType\":\"PKCS12\","
                + "\"trustStorePath\":\"" + trustStorePath.toString().replace("\\", "\\\\") + "\","
                + "\"trustStorePassword\":\"" + KEYSTORE_PASSWORD + "\","
                + "\"trustStoreType\":\"PKCS12\","
                + "\"needClientAuth\":true,"
                + "\"requireSsl\":true"
                + "}";
        return new ObjectMapper().readValue(json, SSLConfig.class);
    }

    private static TestPostgresServer startServer(SSLConfig ssl)
    {
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(0);
        serverConfig.setHttpPort(0);
        serverConfig.setSsl(ssl);
        TestPostgresServer server = new TestPostgresServer(serverConfig,
                new SQLManager(Lists.mutable.with(new LegendExecutionService(new LegendTdsTestClient(resources)))),
                (user, connectionProperties) -> new NoPasswordAuthenticationMethod(new AnonymousIdentityProvider()),
                new Messages(Throwable::getMessage));
        server.startUp();
        return server;
    }

    private static Connection connect(TestPostgresServer server, String sslMode) throws SQLException
    {
        Properties info = new Properties();
        info.setProperty("user", "dummy");
        info.setProperty("password", "dummy");
        info.setProperty("sslmode", sslMode);
        String url = "jdbc:postgresql://127.0.0.1:" + server.getLocalAddress().getPort() + "/postgres";
        return DriverManager.getConnection(url, info);
    }

    private static Connection connectWithClientCert(TestPostgresServer server, Path certPath, Path keyPath) throws SQLException
    {
        Properties info = new Properties();
        info.setProperty("user", "dummy");
        info.setProperty("password", "dummy");
        info.setProperty("sslmode", "require");
        info.setProperty("sslcert", certPath.toString());
        info.setProperty("sslkey", keyPath.toString());
        String url = "jdbc:postgresql://127.0.0.1:" + server.getLocalAddress().getPort() + "/postgres";
        return DriverManager.getConnection(url, info);
    }

    private static int queryRowCount(Connection connection) throws SQLException
    {
        try (
                PreparedStatement statement = connection.prepareStatement("SELECT * FROM service.\"/personService\"");
                ResultSet resultSet = statement.executeQuery()
        )
        {
            int rows = 0;
            while (resultSet.next())
            {
                rows++;
            }
            return rows;
        }
    }

    @Test
    public void plaintextClientSucceedsWhenSslOptional() throws SQLException
    {
        try (Connection connection = connect(sslOptionalServer, "disable"))
        {
            Assertions.assertEquals(4, queryRowCount(connection));
        }
    }

    @Test
    public void tlsClientSucceedsWhenSslOptional() throws SQLException
    {
        try (Connection connection = connect(sslOptionalServer, "require"))
        {
            Assertions.assertEquals(4, queryRowCount(connection));
        }
    }

    @Test
    public void tlsClientSucceedsWhenSslRequired() throws SQLException
    {
        try (Connection connection = connect(sslRequiredServer, "require"))
        {
            Assertions.assertEquals(4, queryRowCount(connection));
        }
    }

    @Test
    public void plaintextClientRejectedWhenSslRequired()
    {
        PSQLException exception = Assertions.assertThrows(PSQLException.class, () -> connect(sslRequiredServer, "disable"));
        Assertions.assertTrue(exception.getMessage().contains("SSL connection is required"),
                "expected an SSL-required failure, got: " + exception.getMessage());
    }

    @Test
    public void mutualTlsClientWithTrustedCertSucceeds() throws SQLException
    {
        try (Connection connection = connectWithClientCert(mutualTlsServer, clientCertPath, clientKeyPath))
        {
            Assertions.assertEquals(4, queryRowCount(connection));
        }
    }

    @Test
    public void mutualTlsClientWithoutCertRejected()
    {
        SQLException exception = Assertions.assertThrows(SQLException.class, () -> connect(mutualTlsServer, "require"));
        String rootCause = rootCauseMessage(exception);
        String message = rootCause.toLowerCase();
        Assertions.assertFalse(message.contains("ssl connection is required"),
                "mTLS rejection should fail at the TLS handshake, not the plaintext-required check: " + rootCause);
        Assertions.assertTrue(
                message.contains("certificate")
                        || message.contains("handshake")
                        || message.contains("received fatal alert")
                        || message.contains("reset")
                        || message.contains("broken pipe")
                        || message.contains("eof"),
                "expected a TLS handshake-level rejection, got: " + rootCause);
    }

    private static String rootCauseMessage(Throwable throwable)
    {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause)
        {
            cause = cause.getCause();
        }
        return cause.getMessage() == null ? "" : cause.getMessage();
    }

    @AfterAll
    public static void tearDown()
    {
        if (sslOptionalServer != null)
        {
            sslOptionalServer.stopListening();
            sslOptionalServer.shutDown();
        }
        if (sslRequiredServer != null)
        {
            sslRequiredServer.stopListening();
            sslRequiredServer.shutDown();
        }
        if (mutualTlsServer != null)
        {
            mutualTlsServer.stopListening();
            mutualTlsServer.shutDown();
        }
    }
}
