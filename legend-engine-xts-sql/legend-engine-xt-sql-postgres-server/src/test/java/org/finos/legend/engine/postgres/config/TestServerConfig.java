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

package org.finos.legend.engine.postgres.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;
import org.finos.legend.engine.postgres.PostgresServerLauncher;
import org.finos.legend.engine.postgres.protocol.wire.auth.method.AuthenticationMethodType;
import org.finos.legend.engine.postgres.protocol.wire.auth.identity.IdentityType;
import org.junit.Assert;
import org.junit.Test;

public class TestServerConfig
{
    static ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void loadJDBCConfig() throws Exception
    {
        File file = new File("src/test/resources/JDBCConfig.json");

        try (FileInputStream inputStream = new FileInputStream(file))
        {
            ServerConfig serverConfig = objectMapper.readValue(inputStream, ServerConfig.class);
            Assert.assertEquals(JDBCHandlerConfig.class, serverConfig.getHandler().getClass());
            Assert.assertEquals(9998L, serverConfig.getPort().longValue());
            Assert.assertEquals(AuthenticationMethodType.NO_PASSWORD, serverConfig.getAuthenticationMethod());
            Assert.assertEquals(IdentityType.ANONYMOUS, serverConfig.getIdentityType());
            JDBCHandlerConfig jdbcHandlerConfig = (JDBCHandlerConfig) serverConfig.getHandler();
            Assert.assertEquals("jdbc:postgresql://localhost:5432/postgres", jdbcHandlerConfig.getConnectionString());
            Assert.assertEquals("password", jdbcHandlerConfig.getPassword());
            Assert.assertEquals("postgres", jdbcHandlerConfig.getUser());
        }
    }

    @Test
    public void loadLegendConfig() throws Exception
    {
        File file = new File("src/test/resources/legendConfig.json");

        try (FileInputStream inputStream = new FileInputStream(file))
        {
            ServerConfig serverConfig = objectMapper.readValue(inputStream, ServerConfig.class);
            Assert.assertEquals(LegendHandlerConfig.class, serverConfig.getHandler().getClass());
            Assert.assertEquals(9998L, serverConfig.getPort().longValue());
            Assert.assertEquals(AuthenticationMethodType.NO_PASSWORD, serverConfig.getAuthenticationMethod());
            Assert.assertEquals(IdentityType.ANONYMOUS, serverConfig.getIdentityType());
            LegendHandlerConfig legendHandlerConfig = (LegendHandlerConfig) serverConfig.getHandler();
            Assert.assertEquals("http", legendHandlerConfig.getProtocol());
            Assert.assertEquals("localhost", legendHandlerConfig.getHost());
            Assert.assertEquals("6300", legendHandlerConfig.getPort());
        }
    }

    @Test
    public void loadSslConfig() throws Exception
    {
        File file = new File("src/test/resources/sslConfig.json");

        try (FileInputStream inputStream = new FileInputStream(file))
        {
            ServerConfig serverConfig = objectMapper.readValue(inputStream, ServerConfig.class);
            SSLConfig ssl = serverConfig.getSsl();
            Assert.assertNotNull(ssl);
            Assert.assertEquals("keystore.p12", ssl.getKeyStorePath());
            Assert.assertEquals("${KEYSTORE_PASSWORD}", ssl.getKeyStorePassword());
            Assert.assertEquals("PKCS12", ssl.getKeyStoreType());
            Assert.assertEquals("truststore.p12", ssl.getTrustStorePath());
            Assert.assertFalse(ssl.isNeedClientAuth());
            Assert.assertFalse(ssl.isWantClientAuth());
            Assert.assertTrue(ssl.isRequireSsl());
            Assert.assertEquals(Arrays.asList("TLSv1.3", "TLSv1.2"), ssl.getSupportedProtocols());
            Assert.assertEquals(Arrays.asList("TLS_AES_256_GCM_SHA384", "TLS_AES_128_GCM_SHA256"), ssl.getSupportedCipherSuites());
        }
    }

    @Test
    public void loadConfigWithoutSsl() throws Exception
    {
        File file = new File("src/test/resources/legendConfig.json");

        try (FileInputStream inputStream = new FileInputStream(file))
        {
            ServerConfig serverConfig = objectMapper.readValue(inputStream, ServerConfig.class);
            Assert.assertNull(serverConfig.getSsl());
        }
    }

    @Test
    public void resolvesLiteralPassword()
    {
        Assert.assertEquals("literal", PostgresServerLauncher.resolveEnv("literal"));
        Assert.assertNull(PostgresServerLauncher.resolveEnv(null));
    }

    @Test
    public void missingEnvVarFailsClosed()
    {
        try
        {
            PostgresServerLauncher.resolveEnv("${A_DEFINITELY_UNSET_ENV_VAR_FOR_TEST}");
            Assert.fail("expected failure for unset env var");
        }
        catch (IllegalStateException e)
        {
            Assert.assertTrue(e.getMessage().contains("A_DEFINITELY_UNSET_ENV_VAR_FOR_TEST"));
        }
    }
}
