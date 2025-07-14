// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.result;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

public class SQLResultDBColumnsMetaData
{
    private final List<SQLResultColumn> sqlResultColumns;
    private final List<Pair<Integer, String>> dbMetaDataType;
    private final boolean[] timeStampColumns;
    private final boolean[] dateColumns;
    private final boolean[] variantColumns;

    SQLResultDBColumnsMetaData(List<SQLResultColumn> resultColumns, ResultSetMetaData rsMetaData) throws SQLException
    {
        int size = resultColumns.size();
        this.sqlResultColumns = resultColumns;
        this.dbMetaDataType = Lists.multiReader.ofInitialCapacity(size);
        this.timeStampColumns = new boolean[size];
        this.dateColumns = new boolean[size];
        this.variantColumns = new boolean[size];

        for (int i = 1; i <= size; i++)
        {

            this.dbMetaDataType.add(Tuples.pair(rsMetaData.getColumnType(i), rsMetaData.getColumnTypeName(i)));
            if (columnIsOfType(i, Types.TIMESTAMP, "TIMESTAMP"))
            {
                timeStampColumns[i - 1] = true;

            }
            else if (columnIsOfType(i, Types.DATE, "DATE"))
            {
                dateColumns[i - 1] = true;

            }
            // Variant types are not standardized across databases, so we check for common types
            else if (
                    columnIsOfType(i, Types.JAVA_OBJECT, "SEMISTRUCTURED")
                            || columnIsOfType(i, "JSON", "SEMISTRUCTURED") // duckdb
                            || columnIsOfType(i, "VARIANT", "SEMISTRUCTURED") // snowflake
                            || columnIsOfType(i, "ARRAY", "ARRAY") // snowflake
                            || columnIsOfType(i, "OBJECT", "OBJECT") // snowflake
                            || columnIsOfType(i, "STRUCT", "OBJECT") // snowflake
                            || columnIsOfType(i, Types.ARRAY, "ARRAY")
                            || columnIsOfType(i, Types.STRUCT, "OBJECT")
            )
            {
                variantColumns[i - 1] = true;
            }
        }
    }

    boolean isTimestampColumn(int index)
    {
        return timeStampColumns[index - 1];
    }


    boolean isDateColumn(int index)
    {
        return dateColumns[index - 1];
    }

    boolean isVariantColumn(int index)
    {
        return variantColumns[index - 1];
    }

    private boolean columnIsOfType(int index, String dbColumnTypeName, String alloyColumnType)
    {
        int zeroBasedIndex = index - 1;
        SQLResultColumn sqlResultColumn = this.sqlResultColumns.get(zeroBasedIndex);
        return dbColumnTypeName.equals(this.dbMetaDataType.get(zeroBasedIndex).getTwo()) || alloyColumnType.equals(sqlResultColumn.dataType);
    }

    private boolean columnIsOfType(int index, int dbColumnType, String alloyColumnType)
    {
        int zeroBasedIndex = index - 1;
        SQLResultColumn sqlResultColumn = this.sqlResultColumns.get(zeroBasedIndex);
        return this.dbMetaDataType.get(zeroBasedIndex).getOne() == dbColumnType || alloyColumnType.equals(sqlResultColumn.dataType);
    }
}
