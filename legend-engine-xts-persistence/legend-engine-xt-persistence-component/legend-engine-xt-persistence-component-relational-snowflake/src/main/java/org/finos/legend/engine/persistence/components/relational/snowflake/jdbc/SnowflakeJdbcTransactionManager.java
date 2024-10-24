// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.snowflake.jdbc;

import net.snowflake.client.jdbc.SnowflakeResultSet;
import org.finos.legend.engine.persistence.components.executor.TabularData;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcTransactionManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SnowflakeJdbcTransactionManager extends JdbcTransactionManager
{
    public SnowflakeJdbcTransactionManager(Connection connection) throws SQLException
    {
        super(connection);
    }

    public TabularData convertResultSetToTabularData(String sql) throws SQLException
    {
        List<Map<String, Object>> resultList = new ArrayList<>();
        Optional<String> queryId;
        try (ResultSet resultSet = this.statement.executeQuery(sql))
        {
            while (resultSet.next())
            {
                extractResults(resultList, resultSet);
            }
            queryId = Optional.ofNullable(resultSet.unwrap(SnowflakeResultSet.class).getQueryID());
        }
        return TabularData.builder()
                .addAllData(resultList)
                .queryId(queryId)
                .build();
    }

    public TabularData convertResultSetToTabularData(String sql, int rows) throws SQLException
    {
        List<Map<String, Object>> resultList = new ArrayList<>();
        Optional<String> queryId;
        try (ResultSet resultSet = this.statement.executeQuery(sql))
        {
            int iter = 0;
            while (resultSet.next() && iter < rows)
            {
                iter++;
                extractResults(resultList, resultSet);
            }
            queryId = Optional.ofNullable(resultSet.unwrap(SnowflakeResultSet.class).getQueryID());
        }
        return TabularData.builder()
                .addAllData(resultList)
                .queryId(queryId)
                .build();
    }
}