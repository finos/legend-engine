// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.postgres.handler.legend;

import java.sql.Types;
import java.util.List;
import org.finos.legend.engine.postgres.handler.PostgresResultSetMetaData;
import static org.finos.legend.engine.postgres.handler.legend.LegendDataType.*;

public class LegendResultSetMetaData implements PostgresResultSetMetaData
{

    private List<LegendColumn> legendColumns;

    public LegendResultSetMetaData(List<LegendColumn> legendColumns)
    {
        this.legendColumns = legendColumns;
    }

    @Override
    public int getColumnCount() throws Exception
    {
        return legendColumns.size();
    }

    @Override
    public String getColumnName(int i) throws Exception
    {
        return getColumnPrivate(i).getName();
    }

    private LegendColumn getColumnPrivate(int i)
    {
        return legendColumns.get(i - 1);
    }

    @Override
    public int getColumnType(int i) throws Exception
    {
        String legendType = getColumnPrivate(i).getType();
        switch (legendType)
        {
            case STRICT_DATE:
                return Types.DATE;
            case DATE:
            case DATE_TIME:
                return Types.TIMESTAMP;
            case INTEGER:
                return Types.BIGINT;
            case FLOAT:
            case NUMBER:
                return Types.DOUBLE;
            case BOOLEAN:
                return Types.BOOLEAN;
            default:
                return Types.VARCHAR;
        }

    }

    @Override
    public int getScale(int i) throws Exception
    {
        return 0;
    }
}
