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

package org.finos.legend.engine.postgres.protocol.sql.handler.jdbc;

import org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.catalog.CatalogManager;
import org.finos.legend.engine.postgres.protocol.sql.handler.jdbc.catalog.SQLRewrite;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.regular.PostgresStatement;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.result.PostgresResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.function.Supplier;

public class JDBCPostgresStatement implements PostgresStatement, AutoCloseable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JDBCPostgresPreparedStatement.class);

    private final Supplier<Connection> connectionSupplier;
    private final SQLRewrite sqlRewrite;

    private Connection connection;
    private Statement postgresStatement;

    public JDBCPostgresStatement(Supplier<Connection> connectionSupplier, SQLRewrite sqlRewrite) throws SQLException
    {
        this.connectionSupplier = connectionSupplier;
        this.sqlRewrite = sqlRewrite;
    }

    @Override
    public boolean execute(String query) throws Exception
    {
        try
        {
            ensureStatement();
            return postgresStatement.execute(CatalogManager.reprocessQuery(query, sqlRewrite));
        }
        catch (Exception e)
        {
            closeConnectionAndStatement();
            throw e;
        }
    }

    @Override
    public PostgresResultSet getResultSet() throws Exception
    {
        return new JDBCPostgresResultSet(postgresStatement.getResultSet(), this::closeConnectionAndStatement);
    }

    @Override
    public void close() throws Exception
    {
        closeConnectionAndStatement();
    }

    private synchronized void closeConnectionAndStatement()
    {
        try
        {
            if (postgresStatement != null)
            {
                postgresStatement.close();
            }
        }
        catch (SQLException e)
        {
            LOGGER.info("Error closing Statement", e);
        }

        try
        {
            if (connection != null && !connection.isClosed())
            {
                connection.close();
            }
        }
        catch (SQLException e)
        {
            LOGGER.info("Error closing Statement", e);
        }
    }

    private synchronized void ensureStatement() throws SQLException
    {
        if (postgresStatement == null || connection == null || connection.isClosed())
        {
            this.connection = connectionSupplier.get();
            this.postgresStatement = connection.createStatement();

        }
    }

}
