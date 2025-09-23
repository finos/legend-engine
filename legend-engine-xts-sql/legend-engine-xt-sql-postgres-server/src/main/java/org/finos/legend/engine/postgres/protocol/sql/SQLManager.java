// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.postgres.protocol.sql;

import org.finos.legend.engine.language.sql.grammar.from.SQLGrammarParser;
import org.finos.legend.engine.postgres.PostgresServerException;
import org.finos.legend.engine.postgres.protocol.sql.dispatcher.ExecutionType;
import org.finos.legend.engine.postgres.protocol.sql.dispatcher.StatementDispatcherVisitor;
import org.finos.legend.engine.postgres.protocol.sql.handler.empty.EmptyPreparedStatement;
import org.finos.legend.engine.postgres.protocol.sql.handler.empty.EmptyStatement;
import org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.JDBCSessionHandler;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.LegendExecutionService;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.LegendSessionHandler;
import org.finos.legend.engine.postgres.protocol.sql.handler.txn.TxnIsolationPreparedStatement;
import org.finos.legend.engine.postgres.protocol.sql.handler.txn.TxnIsolationStatement;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.prepared.PostgresPreparedStatement;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.regular.PostgresStatement;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.h2.tools.Server;

import java.io.File;
import java.nio.file.Files;
import java.sql.SQLException;

public class SQLManager
{
    JDBCSessionHandler metadataJDBCSessionHandler;
    LegendExecutionService client;

    public SQLManager(LegendExecutionService client)
    {
        try
        {
            File h2ServerTempDir = Files.createTempDirectory("legendSqlH2Server").toFile();
            h2ServerTempDir.deleteOnExit();
            String h2ServerTempDirPath = h2ServerTempDir.getAbsolutePath();
            Server h2PgServer = Server.createPgServer("-baseDir", h2ServerTempDirPath, "-ifNotExists");
            h2PgServer.start();
            String url = "jdbc:postgresql://localhost:" + h2PgServer.getPort() + "/legendSQLMetadata;" +
                    "INIT=CREATE SCHEMA service\\;CREATE TABLE service.emptytable(id varchar(10));";
            this.metadataJDBCSessionHandler = new JDBCSessionHandler(url, "sa", "");
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        this.client = client;
    }

    public PostgresPreparedStatement buildPreparedStatement(String query, Identity identity)
    {
        try
        {
            if (query != null)
            {
                switch (getType(query))
                {
                    case Empty:
                        return new EmptyPreparedStatement();
                    case Legend:
                        return new LegendSessionHandler(client, identity).prepareStatement(query);
                    case Metadata:
                        return metadataJDBCSessionHandler.prepareStatement(query);
                    case TX:
                        return new TxnIsolationPreparedStatement();
                }
            }
        }
        catch (SQLException e)
        {
            throw new PostgresServerException(e);
        }
        return null;
    }

    public PostgresStatement buildStatement(String query, Identity identity)
    {
        try
        {
            if (query != null)
            {
                switch (getType(query))
                {
                    case Empty:
                        return new EmptyStatement();
                    case Legend:
                        return new LegendSessionHandler(client, identity).createStatement();
                    case Metadata:
                        return metadataJDBCSessionHandler.createStatement();
                    case TX:
                        return new TxnIsolationStatement();
                }
            }
        }
        catch (SQLException e)
        {
            throw new PostgresServerException(e);
        }
        return null;
    }

    private static ExecutionType getType(String query)
    {
        return query.isEmpty() ? ExecutionType.Empty : SQLGrammarParser.getSqlBaseParser(query, "query").singleStatement().accept(new StatementDispatcherVisitor());
    }
}
