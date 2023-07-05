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
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Operator;

public class Expression extends Value
{
    private Value left;
    private Value right;
    private Operator operator;

    public Expression(Operator operator, String quoteIdentifier)
    {
        super(quoteIdentifier);
        this.operator = operator;
    }

    public Expression(Operator operator, String alias, String quoteIdentifier)
    {
        super(alias, quoteIdentifier);
        this.operator = operator;
    }

    public Expression(Value left, Value right, Operator operator, String quoteIdentifier)
    {
        super(quoteIdentifier);
        this.left = left;
        this.operator = operator;
        this.right = right;
    }

    public Expression(Value left, Value right, Operator operator, String alias, String quoteIdentifier)
    {
        super(alias, quoteIdentifier);
        this.left = left;
        this.operator = operator;
        this.right = right;
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
        validate();
        left.genSql(builder);
        builder.append(operator.get());
        right.genSql(builder);
    }

    void validate() throws SqlDomException
    {
        if (left == null)
        {
            throw new SqlDomException("Left value is null");
        }
        if (right == null)
        {
            throw new SqlDomException("Right value is null");
        }
    }

    @Override
    public void push(Object node)
    {
        if (left == null)
        {
            left = (Value) node;
        }
        else
        {
            right = (Value) node;
        }
    }
}
