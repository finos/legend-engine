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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Collectors;

import javax.sql.DataSource;

/**
 * Creates tables and inserts seed data in Postgres from YAML schema definitions.
 */
public class SchemaManager
{
    private final DataSource dataSource;

    public SchemaManager(DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public void createSchema(TestCaseLoader.Schema schema) throws SQLException
    {
        try (Connection conn = dataSource.getConnection())
        {
            for (TestCaseLoader.TableDef table : schema.tables)
            {
                createTable(conn, table);
                insertRows(conn, table);
            }
        }
    }

    private void createTable(Connection conn, TestCaseLoader.TableDef table) throws SQLException
    {
        StringBuilder ddl = new StringBuilder();
        ddl.append("CREATE TABLE IF NOT EXISTS ").append(table.name).append(" (");
        ddl.append(table.columns.stream()
                .map(c -> c.name + " " + c.type)
                .collect(Collectors.joining(", ")));
        ddl.append(")");

        try (Statement stmt = conn.createStatement())
        {
            stmt.execute("DROP TABLE IF EXISTS " + table.name + " CASCADE");
            stmt.execute(ddl.toString());
        }
    }

    private void insertRows(Connection conn, TestCaseLoader.TableDef table) throws SQLException
    {
        if (table.rows == null || table.rows.isEmpty())
        {
            return;
        }

        String placeholders = table.columns.stream().map(c -> "?").collect(Collectors.joining(", "));
        String sql = "INSERT INTO " + table.name + " VALUES (" + placeholders + ")";

        try (PreparedStatement ps = conn.prepareStatement(sql))
        {
            for (List<Object> row : table.rows)
            {
                for (int i = 0; i < row.size(); i++)
                {
                    Object val = row.get(i);
                    String colType = table.columns.get(i).type.toUpperCase();
                    if (val == null)
                    {
                        ps.setNull(i + 1, java.sql.Types.NULL);
                    }
                    else if (val instanceof Integer)
                    {
                        ps.setInt(i + 1, (Integer) val);
                    }
                    else if (val instanceof Long)
                    {
                        ps.setLong(i + 1, (Long) val);
                    }
                    else if (val instanceof Double)
                    {
                        ps.setDouble(i + 1, (Double) val);
                    }
                    else if (val instanceof Boolean)
                    {
                        ps.setBoolean(i + 1, (Boolean) val);
                    }
                    else if (colType.contains("DATE") && !colType.contains("TIMESTAMP"))
                    {
                        ps.setDate(i + 1, java.sql.Date.valueOf(val.toString()));
                    }
                    else if (colType.contains("TIMESTAMP"))
                    {
                        ps.setTimestamp(i + 1, java.sql.Timestamp.valueOf(val.toString()));
                    }
                    else if (colType.contains("BOOLEAN") || colType.contains("BIT"))
                    {
                        ps.setBoolean(i + 1, Boolean.parseBoolean(val.toString()));
                    }
                    else if (colType.contains("JSON") || colType.contains("JSONB"))
                    {
                        org.postgresql.util.PGobject pgObj = new org.postgresql.util.PGobject();
                        pgObj.setType(colType.contains("JSONB") ? "jsonb" : "json");
                        pgObj.setValue(val.toString());
                        ps.setObject(i + 1, pgObj);
                    }
                    else if (colType.contains("[]") || colType.toUpperCase().startsWith("ARRAY"))
                    {
                        // Expect val to be a list (from YAML) or a Postgres array literal string
                        if (val instanceof List)
                        {
                            String baseType = colType.replace("[]", "").trim();
                            java.sql.Array sqlArray = conn.createArrayOf(baseType, ((List<?>) val).toArray());
                            ps.setArray(i + 1, sqlArray);
                        }
                        else
                        {
                            ps.setString(i + 1, val.toString());
                        }
                    }
                    else
                    {
                        ps.setString(i + 1, val.toString());
                    }
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}


