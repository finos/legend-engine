// Copyright 2022 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.persistence.components.relational.jdbc;

import org.postgresql.util.PGobject;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdbcTransactionManager
{
    protected final Statement statement;
    private Connection connection;
    private boolean previousAutoCommit;

    public JdbcTransactionManager(Connection connection) throws SQLException
    {
        this.connection = connection;
        this.statement = this.connection.createStatement();
        this.previousAutoCommit = this.connection.getAutoCommit();
    }

    public void close() throws SQLException
    {
        if (this.statement != null)
        {
            this.statement.close();
        }
    }

    public void beginTransaction() throws SQLException
    {
        this.connection.setAutoCommit(false);
    }

    public void commitTransaction() throws SQLException
    {
        this.connection.commit();
        this.setAutoCommitToPreviousState();
    }

    public void revertTransaction() throws SQLException
    {
        if (!this.connection.getAutoCommit())
        {
            this.connection.rollback();
        }
        this.setAutoCommitToPreviousState();
    }

    private void setAutoCommitToPreviousState() throws SQLException
    {
        this.connection.setAutoCommit(this.previousAutoCommit);
    }

    public boolean executeInCurrentTransaction(String sql) throws SQLException
    {
        return this.statement.execute(sql);
    }

    // todo: find a better way to return both the data and schema
    public List<Map<String, Object>> convertResultSetToList(String sql)
    {
        try
        {
            List<Map<String, Object>> resultList = new ArrayList<>();
            try (ResultSet resultSet = this.statement.executeQuery(sql))
            {
                while (resultSet.next())
                {
                    extractResults(resultList, resultSet);
                }
            }
            return resultList;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    public List<Map<String, Object>> convertResultSetToList(String sql, int rows)
    {
        try
        {
            List<Map<String, Object>> resultList = new ArrayList<>();
            try (ResultSet resultSet = this.statement.executeQuery(sql))
            {
                int iter = 0;
                while (resultSet.next() && iter < rows)
                {
                    iter++;
                    extractResults(resultList, resultSet);
                }
            }
            return resultList;
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static void extractResults(List<Map<String, Object>> resultList, ResultSet resultSet) throws SQLException
    {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = resultSet.getMetaData().getColumnCount();
        Map<String, Object> row = new HashMap<>();
        for (int i = 1; i <= columnCount; i++)
        {
            Object value = resultSet.getObject(i);
            if (metaData.getColumnTypeName(i).equalsIgnoreCase("JSON") && value instanceof byte[])
            {
                value = new String((byte[]) value, StandardCharsets.UTF_8);
            }
            if (metaData.getColumnTypeName(i).equalsIgnoreCase("JSON") && value instanceof PGobject)
            {
                value = ((PGobject) value).getValue();
            }
            row.put(metaData.getColumnName(i), value);
        }
        resultList.add(row);
    }
}
