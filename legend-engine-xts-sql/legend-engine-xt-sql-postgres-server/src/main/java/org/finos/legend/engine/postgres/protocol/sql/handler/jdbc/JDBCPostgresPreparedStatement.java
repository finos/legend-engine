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

import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;

class JDBCPostgresPreparedStatement implements PostgresPreparedStatement
{

    private final PreparedStatement preparedStatement;
    private boolean isExecuted = false;

    public JDBCPostgresPreparedStatement(PreparedStatement preparedStatement)
    {
        this.preparedStatement = preparedStatement;
    }

    @Override
    public void setObject(int i, Object o) throws Exception
    {
        preparedStatement.setObject(i, o);
    }

    @Override
    public PostgresResultSetMetaData getMetaData() throws Exception
    {
        return new JDBCPostgresResultSetMetaData(preparedStatement.getMetaData());
    }

    @Override
    public ParameterMetaData getParameterMetaData() throws Exception
    {
        return preparedStatement.getParameterMetaData();
    }

    @Override
    public void close() throws Exception
    {
        preparedStatement.close();
    }

    @Override
    public void setMaxRows(int maxRows) throws Exception
    {
        preparedStatement.setMaxRows(maxRows);
    }

    @Override
    public int getMaxRows() throws Exception
    {
        return preparedStatement.getMaxRows();
    }

    @Override
    public boolean isExecuted()
    {
        return isExecuted;
    }

    @Override
    public boolean execute() throws Exception
    {
        isExecuted = true;
        return preparedStatement.execute();
    }

    @Override
    public PostgresResultSet getResultSet() throws Exception
    {
        return new JDBCPostgresResultSet(preparedStatement.getResultSet());
    }
}
