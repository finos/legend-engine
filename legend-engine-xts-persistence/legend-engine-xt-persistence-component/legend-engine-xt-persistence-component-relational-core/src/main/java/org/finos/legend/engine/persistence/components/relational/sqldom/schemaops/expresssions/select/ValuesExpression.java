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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.select;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.COMMA;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class ValuesExpression extends SelectExpression
{
    private final List<List<Value>> values;
    private int columnCount = 0;
    private int rowIndex = 0;
    private int colIndex = 0;

    public ValuesExpression()
    {
        this.values = new ArrayList<>();
    }

    public ValuesExpression withColumnCount(int columnCount)
    {
        return new ValuesExpression(columnCount, rowIndex, colIndex, values);
    }

    public ValuesExpression(List<List<Value>> values)
    {
        this.values = values;
    }

    public ValuesExpression(int columnCount, int rowIndex, int colIndex, List<List<Value>> values)
    {
        this.columnCount = columnCount;
        this.rowIndex = rowIndex;
        this.colIndex = colIndex;

        this.values = values;
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        builder.append(Clause.VALUES);
        builder.append(WHITE_SPACE);

        for (int i = 0; i < values.size(); i++)
        {
            builder.append(OPEN_PARENTHESIS);
            for (int j = 0; j < values.get(i).size(); j++)
            {
                Value val = values.get(i).get(j);
                val.genSql(builder);
                if (j < (values.get(i).size() - 1))
                {
                    builder.append(COMMA);
                }
            }
            builder.append(CLOSING_PARENTHESIS);
            if (i < (values.size() - 1))
            {
                builder.append(COMMA);
            }
        }
    }

    @Override
    public void push(Object node)
    {
        // Values
        if (colIndex == 0)
        {
            List<Value> row = new ArrayList<>();
            values.add(row);
        }
        if (node instanceof Value)
        {
            List<Value> row = values.get(rowIndex);
            row.add((Value) node);
            colIndex++;
        }
        if (colIndex == columnCount)
        {
            colIndex = 0;
            rowIndex++;
        }
    }

    void validate() throws SqlDomException
    {
        if (values == null || values.size() == 0)
        {
            throw new SqlDomException("values is empty");
        }
    }
}
