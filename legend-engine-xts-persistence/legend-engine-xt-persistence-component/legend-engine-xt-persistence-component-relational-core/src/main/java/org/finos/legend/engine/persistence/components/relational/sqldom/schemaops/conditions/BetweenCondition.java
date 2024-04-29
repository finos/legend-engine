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
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;

public class BetweenCondition extends Condition
{
    private final Value value;
    private final Value left;
    private final Value right;

    public BetweenCondition(Value value, Value left, Value right)
    {
        this.value = value;
        this.left = left;
        this.right = right;
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        value.genSqlWithoutAlias(builder);
        builder.append(" BETWEEN ");
        left.genSql(builder);
        builder.append(" AND ");
        right.genSql(builder);
    }

    void validate() throws SqlDomException
    {
        if (value == null)
        {
            throw new SqlDomException("value is null");
        }
        if (left == null)
        {
            throw new SqlDomException("Left value is null");
        }
        if (right == null)
        {
            throw new SqlDomException("Right value is null");
        }
    }

}
