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

package org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.result;

import java.sql.Types;
import java.util.List;

import com.google.common.base.Function;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.LegendColumn;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.statement.TypeConversion;
import org.finos.legend.engine.postgres.protocol.wire.session.statements.result.PostgresResultSetMetaData;

public class LegendResultSetMetaData implements PostgresResultSetMetaData
{
    private List<LegendColumn> legendColumns;
    private List<Integer> columnTypes;

    public LegendResultSetMetaData(List<LegendColumn> legendColumns)
    {
        this.legendColumns = legendColumns;
        this.columnTypes = ListIterate.collect(legendColumns, c ->
        {
            Function<Integer, Integer> ifNullReturnsVarchar = (v -> v == null ? Types.VARCHAR : v);
            return ifNullReturnsVarchar.apply(c.getLinearizedInheritances().isEmpty() ?
                    TypeConversion._typeConversions.get(c.getType()) :
                    TypeConversion._typeConversions.get(ListIterate.detect(c.getLinearizedInheritances(), v -> TypeConversion._typeConversions.get(v) != null))
            );
        });
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
        return  columnTypes.get(i - 1);
    }

    @Override
    public int getScale(int i) throws Exception
    {
        return 0;
    }

    @Override
    public String getColumnTypeName(int i) throws Exception
    {
        return null;
    }
}
