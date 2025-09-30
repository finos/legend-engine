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

import org.finos.legend.engine.postgres.protocol.sql.handler.SessionHandler;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.prepared.PostgresPreparedStatement;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.regular.PostgresStatement;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class JDBCSessionHandler implements SessionHandler
{
    private Connection connection;
    private final String connectionString;
    private final String user;
    private final String password;

    public JDBCSessionHandler(String connectionString, String user, String password)
    {
        this.connectionString = connectionString;
        this.user = user;
        this.password = password;
    }

    @Override
    public PostgresPreparedStatement prepareStatement(String query) throws SQLException
    {
        return new JDBCPostgresPreparedStatement(getConnection().prepareStatement(query.replaceAll("\\$\\d+", "?")));
    }

    @Override
    public PostgresStatement createStatement() throws SQLException
    {
        return new JDBCPostgresStatement(getConnection().createStatement());
    }

    private Connection getConnection() throws SQLException
    {
        if (connection == null)
        {
            this.connection = DriverManager.getConnection(connectionString, user, password);
        }
        return connection;
    }
}
