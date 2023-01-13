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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.FunctionName;

import java.util.List;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.COMMA;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;

public class Function extends Value
{
    private FunctionName name;
    private List<Value> values;

    public Function(FunctionName name, List<Value> values, String quoteIdentifier)
    {
        super(quoteIdentifier);
        this.name = name;
        this.values = values;
    }

    public Function(FunctionName name, List<Value> values, String alias, String quoteIdentifier)
    {
        super(alias, quoteIdentifier);
        this.name = name;
        this.values = values;
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        genSqlWithoutAlias(builder);
        super.genSql(builder);
    }

    @Override
    public void genSqlWithoutAlias(StringBuilder builder) throws SqlDomException
    {
        builder.append(name.get());
        builder.append(OPEN_PARENTHESIS);
        if (values != null)
        {
            for (int ctr = 0; ctr < values.size(); ctr++)
            {
                values.get(ctr).genSqlWithoutAlias(builder);
                if (ctr < (values.size() - 1))
                {
                    builder.append(COMMA);
                }
            }
        }
        builder.append(CLOSING_PARENTHESIS);
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof FunctionName)
        {
            name = (FunctionName) node;
        }
        else if (node instanceof Value)
        {
            values.add((Value) node);
        }
    }
}