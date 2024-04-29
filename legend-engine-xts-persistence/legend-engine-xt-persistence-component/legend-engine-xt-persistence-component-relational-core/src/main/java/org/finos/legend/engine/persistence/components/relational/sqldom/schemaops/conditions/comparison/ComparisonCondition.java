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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.comparison;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Operator;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.Condition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;

public class ComparisonCondition extends Condition
{
    protected Value left;
    protected Value right;
    protected Operator operator;

    public ComparisonCondition()
    {
    }

    public ComparisonCondition(Value left, Value right, Operator operator)
    {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        left.genSqlWithoutAlias(builder);
        builder.append(String.format(" %s ", operator.get()));
        right.genSqlWithoutAlias(builder);
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
}
