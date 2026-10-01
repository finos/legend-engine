// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.postgres.e2e;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

/**
 * Executes SQL directly against a Postgres database via JDBC and returns a ResultMatrix.
 */
public class DirectPostgresRunner
{
    private final DataSource dataSource;

    public DirectPostgresRunner(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public ResultMatrix execute(String sql) throws Exception
    {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql))
        {
            return fromResultSet(rs);
        }
    }

    private ResultMatrix fromResultSet(ResultSet rs) throws Exception
    {
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        List<String> columnNames = new ArrayList<>();
        for (int i = 1; i <= colCount; i++)
        {
            columnNames.add(meta.getColumnLabel(i));
        }

        List<List<Object>> rows = new ArrayList<>();
        while (rs.next())
        {
            List<Object> row = new ArrayList<>();
            for (int i = 1; i <= colCount; i++)
            {
                Object val = rs.getObject(i);
                row.add(val);
            }
            rows.add(row);
        }

        return new ResultMatrix(columnNames, rows);
    }
}

