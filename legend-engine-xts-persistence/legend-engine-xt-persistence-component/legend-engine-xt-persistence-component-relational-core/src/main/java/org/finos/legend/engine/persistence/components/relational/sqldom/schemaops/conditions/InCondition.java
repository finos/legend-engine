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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.select.SelectExpression;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class InCondition extends Condition
{
    private Value value;
    private SelectExpression expression;

    public InCondition()
    {
    }

    public InCondition(Value value, SelectExpression expression)
    {
        this.value = value;
        this.expression = expression;
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        value.genSqlWithoutAlias(builder);
        builder.append(WHITE_SPACE);
        builder.append(Clause.IN.get());
        builder.append(WHITE_SPACE);
        builder.append(OPEN_PARENTHESIS);
        expression.genSql(builder);
        builder.append(CLOSING_PARENTHESIS);
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof SelectExpression)
        {
            expression = (SelectExpression) node;
        }
        if (node instanceof Value)
        {
            value = (Value) node;
        }
    }

    void validate() throws SqlDomException
    {
        if (value == null)
        {
            throw new SqlDomException("value is null");
        }
        if (expression == null)
        {
            throw new SqlDomException("Expression is null");
        }
    }

}
