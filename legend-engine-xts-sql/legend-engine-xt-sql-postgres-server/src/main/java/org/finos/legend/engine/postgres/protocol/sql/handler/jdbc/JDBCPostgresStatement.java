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

import java.sql.SQLException;
import java.sql.Statement;

public class JDBCPostgresStatement implements PostgresStatement, AutoCloseable
{
    private final Statement postgresStatement;
    private final SQLRewrite sqlRewrite;

    public JDBCPostgresStatement(Statement postgresStatement, SQLRewrite sqlRewrite) throws SQLException
    {
        this.postgresStatement = postgresStatement;
        this.sqlRewrite = sqlRewrite;
    }

    @Override
    public boolean execute(String query) throws Exception
    {
        return postgresStatement.execute(CatalogManager.reprocessQuery(query, sqlRewrite));
    }

    @Override
    public PostgresResultSet getResultSet() throws Exception
    {
        return new JDBCPostgresResultSet(postgresStatement.getResultSet());
    }

    @Override
    public void close() throws Exception
    {
        if (postgresStatement != null)
        {
            postgresStatement.close();
        }
    }

}
