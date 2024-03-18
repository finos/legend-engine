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
import org.finos.legend.engine.postgres.SessionsFactory;
import org.finos.legend.engine.postgres.auth.AuthenticationMethodType;
import org.finos.legend.engine.postgres.auth.IdentityType;
import org.junit.Assert;
import org.junit.Test;

public class TestServerConfig
{
    static ObjectMapper objectMapper = new ObjectMapper();


    @Test
    public void loadJDBCConfig() throws Exception
    {
        File file = new File("src/test/resources/JDBCConfig.json");
        FileInputStream inputStream = new FileInputStream(file);
        ServerConfig serverConfig = objectMapper.readValue(inputStream, ServerConfig.class);
        Assert.assertEquals(JDBCHandlerConfig.class, serverConfig.getHandler().getClass());
        Assert.assertEquals(9998L, serverConfig.getPort().longValue());
        Assert.assertEquals(AuthenticationMethodType.NO_PASSWORD, serverConfig.getAuthenticationMethod());
        Assert.assertEquals(IdentityType.ANONYMOUS, serverConfig.getIdentityType());
        JDBCHandlerConfig jdbcHandlerConfig = (JDBCHandlerConfig) serverConfig.getHandler();
        jdbcHandlerConfig.buildSessionsFactory();
        Assert.assertEquals("jdbc:postgresql://localhost:5432/postgres", jdbcHandlerConfig.getConnectionString());
        Assert.assertEquals("password", jdbcHandlerConfig.getPassword());
        Assert.assertEquals("postgres", jdbcHandlerConfig.getUser());
    }

    @Test
    public void loadLegendConfig() throws Exception
    {
        File file = new File("src/test/resources/legendConfig.json");
        FileInputStream inputStream = new FileInputStream(file);
        ServerConfig serverConfig = objectMapper.readValue(inputStream, ServerConfig.class);
        Assert.assertEquals(LegendHandlerConfig.class, serverConfig.getHandler().getClass());
        Assert.assertEquals(9998L, serverConfig.getPort().longValue());
        Assert.assertEquals(AuthenticationMethodType.NO_PASSWORD, serverConfig.getAuthenticationMethod());
        Assert.assertEquals(IdentityType.ANONYMOUS, serverConfig.getIdentityType());
        LegendHandlerConfig legendHandlerConfig = (LegendHandlerConfig) serverConfig.getHandler();
        legendHandlerConfig.buildSessionsFactory();
        Assert.assertEquals("http", legendHandlerConfig.getProtocol());
        Assert.assertEquals("localhost", legendHandlerConfig.getHost());
        Assert.assertEquals("6300", legendHandlerConfig.getPort());
    }
}
