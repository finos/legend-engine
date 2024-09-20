// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.executor;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface RelationalTransactionManager
{
    void close() throws SQLException;

    void beginTransaction() throws SQLException;

    void commitTransaction() throws SQLException;

    void revertTransaction() throws SQLException;

    boolean executeInCurrentTransaction(String sql) throws SQLException;

    List<Map<String, Object>> convertResultSetToList(String sql) throws SQLException;

    List<Map<String, Object>> convertResultSetToList(String sql, int rows) throws SQLException;

    default TabularData convertResultSetToTabularData(String sql) throws SQLException
    {
        return TabularData.builder()
            .addAllData(convertResultSetToList(sql))
            .build();
    }

    default TabularData convertResultSetToTabularData(String sql, int rows) throws SQLException
    {
        return TabularData.builder()
            .addAllData(convertResultSetToList(sql, rows))
            .build();
    }
}
