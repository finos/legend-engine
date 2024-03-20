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

package org.finos.legend.engine.postgres.handler.legend;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.finos.legend.engine.postgres.Session;
import org.finos.legend.engine.postgres.SessionsFactory;
import org.finos.legend.engine.postgres.handler.PostgresPreparedStatement;
import org.finos.legend.engine.postgres.handler.PostgresStatement;
import org.finos.legend.engine.postgres.handler.SessionHandler;
import org.finos.legend.engine.postgres.handler.jdbc.JDBCSessionFactory;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.h2.tools.Server;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;

public class LegendSessionFactory implements SessionsFactory
{

    private final LegendExecutionService legendExecutionClient;
    private final JDBCSessionFactory.JDBCSessionHandler metadataSessionHandler;
    private  ExecutorService executorService = Executors.newCachedThreadPool();

    public LegendSessionFactory(LegendExecutionService legendExecutionClient)
    {
        this.legendExecutionClient = legendExecutionClient;
        try
        {
            File h2ServerTempDir = Files.createTempDirectory("legendSqlH2Server").toFile();
            h2ServerTempDir.deleteOnExit();
            String h2ServerTempDirPath = h2ServerTempDir.getAbsolutePath();
            Server h2PgServer = Server.createPgServer("-baseDir", h2ServerTempDirPath, "-ifNotExists");
            h2PgServer.start();
            String url = "jdbc:postgresql://localhost:" + h2PgServer.getPort() + "/legendSQLMetadata;" +
                    "INIT=CREATE SCHEMA service\\;CREATE TABLE service.emptytable(id varchar(10));";
            this.metadataSessionHandler = new JDBCSessionFactory.JDBCSessionHandler(url, "sa", "");
        }
        catch (IOException | SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Session createSession(String defaultSchema, Identity identity)
    {
        return new Session(new LegendSessionHandler(legendExecutionClient, identity), metadataSessionHandler, executorService, identity);
    }

    private static class LegendSessionHandler implements SessionHandler
    {
        private final LegendExecutionService legendExecutionClient;
        private final Identity identity;

        public LegendSessionHandler(LegendExecutionService legendExecutionClient, Identity identity)
        {
            this.legendExecutionClient = legendExecutionClient;
            this.identity = identity;
        }

        @Override
        public PostgresPreparedStatement prepareStatement(String query)
        {
            return new LegendPreparedStatement(query, legendExecutionClient, identity);
        }

        @Override
        public PostgresStatement createStatement()
        {
            return new LegendStatement(legendExecutionClient, identity);
        }
    }
}
