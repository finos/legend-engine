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

import org.finos.legend.engine.postgres.protocol.wire.session.statements.prepared.PostgresPreparedStatement;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.result.PostgresResultSet;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.result.PostgresResultSetMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class JDBCPostgresPreparedStatement implements PostgresPreparedStatement, AutoCloseable
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JDBCPostgresPreparedStatement.class);

    private final Supplier<Connection> connectionSupplier;
    private final String query;

    private Connection connection;
    private PreparedStatement preparedStatement;
    private boolean isExecuted = false;
    private int maxRows = 0;

    // Store parameters to replay them when PreparedStatement is recreated
    private final Map<Integer, Object> parameters = new HashMap<>();

    public JDBCPostgresPreparedStatement(Supplier<Connection> connectionSupplier, String query) throws SQLException
    {
        this.connectionSupplier = connectionSupplier;
        this.query = query;
    }

    @Override
    public void setObject(int i, Object o) throws Exception
    {
        // Store parameter for replay if PreparedStatement is recreated
        parameters.put(i, o);
    }

    @Override
    public PostgresResultSetMetaData getMetaData() throws Exception
    {
        ensurePreparedStatement();
        try
        {
            return new JDBCPostgresResultSetMetaData(preparedStatement.getMetaData());
        }
        finally
        {
            // Close connection after metadata retrieval - it will be re-acquired if execute() is called
            closeConnectionAndStatement();
        }
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws Exception
    {
        ensurePreparedStatement();
        try
        {
            return preparedStatement.getParameterMetaData();
        }
        finally
        {
            // Close connection after metadata retrieval - it will be re-acquired if execute() is called
            closeConnectionAndStatement();
        }
    }

    @Override
    public void close() throws Exception
    {
        closeConnectionAndStatement();
    }

    @Override
    public void setMaxRows(int maxRows) throws Exception
    {
        this.maxRows = maxRows;
    }

    @Override
    public int getMaxRows() throws Exception
    {
        return maxRows;
    }

    @Override
    public boolean isExecuted()
    {
        return isExecuted;
    }

    @Override
    public boolean execute() throws Exception
    {
        try
        {
            ensurePreparedStatement();
            isExecuted = true;
            return preparedStatement.execute();
        }
        catch (Exception e)
        {
            // If execution fails, return connection immediately
            closeConnectionAndStatement();
            throw e;
        }
    }

    @Override
    public PostgresResultSet getResultSet() throws Exception
    {
        return new JDBCPostgresResultSet(preparedStatement.getResultSet(), this::closeConnectionAndStatement);
    }

    private synchronized void ensurePreparedStatement() throws SQLException
    {
        if (preparedStatement == null || connection == null || connection.isClosed())
        {
            this.connection = connectionSupplier.get();
            this.preparedStatement = connection.prepareStatement(query);

            // Replay stored parameters on the new PreparedStatement
            for (Map.Entry<Integer, Object> entry : parameters.entrySet())
            {
                preparedStatement.setObject(entry.getKey(), entry.getValue());
            }

            // Replay maxRows setting
            if (maxRows > 0)
            {
                preparedStatement.setMaxRows(maxRows);
            }
        }
    }

    private synchronized void closeConnectionAndStatement()
    {
        try
        {
            if (preparedStatement != null)
            {
                preparedStatement.close();
                preparedStatement = null;
            }
        }
        catch (SQLException e)
        {
            LOGGER.info("Error closing PreparedStatement", e);
        }

        try
        {
            if (connection != null && !connection.isClosed())
            {
                connection.close();
                connection = null;
            }
        }
        catch (SQLException e)
        {
            LOGGER.info("Error closing Connection", e);
        }
    }
}
