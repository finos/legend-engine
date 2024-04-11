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

package org.finos.legend.engine.repl.relational.schema;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.JDBCType;
import java.sql.ResultSet;

public class MetadataReader
{
    public static MutableList<Table> getTables(Connection connection)
    {
        try
        {
            MutableList<Table> tables = Lists.mutable.empty();
            DatabaseMetaData metaData = connection.getMetaData();

            try (ResultSet res = metaData.getTables(null, null, null, null))
            {
                while (res.next())
                {
                    if (!"INFORMATION_SCHEMA".equals(res.getString("TABLE_SCHEM")))
                    {
                        MutableList<Column> cols = Lists.mutable.empty();
                        try (ResultSet columns = metaData.getColumns(null, null, res.getString("TABLE_NAME"), null))
                        {
                            while (columns.next())
                            {
                                String columnName = columns.getString("COLUMN_NAME");
                                cols.add(new Column(columnName, getType(columns)));
                            }
                        }
                        tables.add(new Table(res.getString("TABLE_SCHEM"), res.getString("TABLE_NAME"), cols));
                    }
                }
            }
            return tables;
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    public static String getType(ResultSet colInfo) throws Exception
    {
        int datatype = colInfo.getInt("DATA_TYPE");
        int size = colInfo.getInt("COLUMN_SIZE");
        String name = JDBCType.valueOf(datatype).getName();
        String newType;
        switch (name)
        {
            case "BOOLEAN":
                newType = "Bit";
                break;
            case "VARCHAR":
                newType = "VARCHAR(" + size + ")";
                break;
            default:
                newType = name;
        }
        return newType;
    }


//    public static void printSchema(ResultSet res) throws Exception
//    {
//        ResultSetMetaData m = res.getMetaData();
//        for (int i = 1; i <= m.getColumnCount(); i++)
//        {
//            System.out.println("-" + m.getColumnName(i));
//        }
//    }
}
