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

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.model.result.SQLResultColumn;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

public class SQLResultDBColumnsMetaData
{
    private final List<SQLResultColumn> sqlResultColumns;
    private final List<Integer> dbMetaDataType;
    private final boolean[] timeStampColumns;
    private final boolean[] dateColumns;

    SQLResultDBColumnsMetaData(List<SQLResultColumn> resultColumns, ResultSetMetaData rsMetaData) throws SQLException
    {
        int size = resultColumns.size();
        this.sqlResultColumns = resultColumns;
        this.dbMetaDataType = Lists.multiReader.ofInitialCapacity(size);
        this.timeStampColumns = new boolean[size];
        this.dateColumns = new boolean[size];


        for (int i = 1; i <= size; i++)
        {

            this.dbMetaDataType.add(rsMetaData.getColumnType(i));
            if (columnIsOfType(i, Types.TIMESTAMP, "TIMESTAMP"))
            {
                timeStampColumns[i - 1] = true;

            }
            else if (columnIsOfType(i, Types.DATE, "DATE"))
            {
                dateColumns[i - 1] = true;

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

    private boolean columnIsOfType(int index, int dbColumnType, String alloyColumnType)
    {
        int zeroBasedIndex = index - 1;
        SQLResultColumn sqlResultColumn = this.sqlResultColumns.get(zeroBasedIndex);
        return this.dbMetaDataType.get(zeroBasedIndex) == dbColumnType || sqlResultColumn.dataType != null && sqlResultColumn.dataType.equals(alloyColumnType);
    }
}
