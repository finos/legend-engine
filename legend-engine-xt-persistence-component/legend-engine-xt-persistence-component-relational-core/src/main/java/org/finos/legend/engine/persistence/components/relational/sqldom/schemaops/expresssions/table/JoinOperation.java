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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Join;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.Condition;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class JoinOperation extends TableLike
{
    private TableLike left;
    private TableLike right;
    private Join joinType;
    private Condition joinCondition;

    public JoinOperation(Join joinType)
    {
        this.joinType = joinType;
    }

    public JoinOperation(TableLike left, TableLike right, Join joinType, Condition joinCondition)
    {
        this.left = left;
        this.right = right;
        this.joinType = joinType;
        this.joinCondition = joinCondition;
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();

        left.genSql(builder);
        builder.append(WHITE_SPACE).append(joinType.get()).append(WHITE_SPACE);
        right.genSql(builder);
        builder.append(" ON ");
        joinCondition.genSql(builder);
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof Condition)
        {
            joinCondition = (Condition) node;
        }
        else if (node instanceof TableLike)
        {
            if (left == null)
            {
                left = (TableLike) node;
            }
            else
            {
                right = (TableLike) node;
            }
        }
    }

    void validate() throws SqlDomException
    {
        if (left == null)
        {
            throw new SqlDomException("left is null");
        }
        if (right == null)
        {
            throw new SqlDomException("right is null");
        }
        if (joinCondition == null)
        {
            throw new SqlDomException("joinCondition is null");
        }
    }
}
