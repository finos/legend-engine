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

import org.finos.legend.engine.postgres.protocol.wire.session.statements.result.PostgresResultSet;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.result.PostgresResultSetMetaData;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Date;

class JDBCPostgresResultSet implements PostgresResultSet
{

    private final ResultSet resultSet;

    public JDBCPostgresResultSet(ResultSet resultSet)
    {
        this.resultSet = resultSet;
    }

    @Override
    public PostgresResultSetMetaData getMetaData() throws Exception
    {
        return new JDBCPostgresResultSetMetaData(resultSet.getMetaData());
    }

    @Override
    public Object getObject(int i) throws Exception
    {
        Object value = resultSet.getObject(i);
        if (value != null && resultSet.getMetaData().getColumnType(i) == Types.TIMESTAMP)
        {
            return ((Timestamp) value).toInstant().toEpochMilli();
        }
        if (value != null && resultSet.getMetaData().getColumnType(i) == Types.DATE)
        {
            return ((Date) value).toInstant().toEpochMilli();
        }
        return value;
    }

    @Override
    public boolean next() throws Exception
    {
        return resultSet.next();
    }

    @Override
    public void close() throws Exception
    {
        if (resultSet != null)
        {
            resultSet.close();
        }
    }
}
