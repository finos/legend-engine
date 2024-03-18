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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.io.File;
import org.finos.legend.engine.postgres.auth.AuthenticationMethod;
import org.finos.legend.engine.postgres.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

public class PostgresServerLauncher
{
    private String configPath;

    public PostgresServerLauncher(String configPath)
    {
        this.configPath = configPath;
    }

    public void launch() throws Exception
    {
        //TODO ADD CLI

        ObjectMapper objectMapper = new ObjectMapper();
        ServerConfig serverConfig = objectMapper.readValue(new File(configPath), ServerConfig.class);
        /*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
          System property must be set before any logger instance is created.
          Do NOT create static logger in this class.
        */
        if (serverConfig.getLogConfigFile() != null)
        {
            System.setProperty("logback.configurationFile", serverConfig.getLogConfigFile());
        }
        if (serverConfig.getGss() != null)
        {
            System.setProperty("java.security.krb5.conf", serverConfig.getGss().getKerberosConfigFile());
        }

        //Log config has been initiated. We can create a logger now.
        Logger logger = LoggerFactory.getLogger(PostgresServerLauncher.class);

        // install jul to slf4j bridge
        SLF4JBridgeHandler.install();

        SessionsFactory sessionFactory = serverConfig.buildSessionFactory();
        AuthenticationMethod authenticationMethod = serverConfig.buildAuthenticationMethod();

        logger.info("Starting server in port: " + serverConfig.getPort());

        new PostgresServer(serverConfig, sessionFactory, (user, connectionProperties) -> authenticationMethod).run();
    }

    public static void main(String[] args) throws Exception
    {
        String configPath = args[0];
        new PostgresServerLauncher(configPath).launch();
    }
}
