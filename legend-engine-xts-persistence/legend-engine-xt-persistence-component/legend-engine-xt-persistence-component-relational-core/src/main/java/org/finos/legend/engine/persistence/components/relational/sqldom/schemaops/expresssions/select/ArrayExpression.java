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
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.ObjectValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.StringValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.COMMA;

public class ArrayExpression<K extends Value> extends SelectExpression
{
    private final List<Value> values;

    public ArrayExpression()
    {
        this.values = new ArrayList<>();
    }

    public ArrayExpression(List<Value> values)
    {
        this.values = values;
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        for (int i = 0; i < values.size(); i++)
        {
            Value val = values.get(i);
            val.genSql(builder);
            if (i < (values.size() - 1))
            {
                builder.append(COMMA);
            }
        }
    }

    void validate() throws SqlDomException
    {
        if (values.size() == 0)
        {
            throw new SqlDomException("values is empty");
        }
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof StringValue)
        {
            values.add((StringValue) node);
        }
        else if (node instanceof ObjectValue)
        {
            values.add(((ObjectValue) node));
        }
    }
}

