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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.logical;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.Condition;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;

public class OrCondition extends Condition
{
    private static final String EMPTY = "";

    private final List<Condition> conditions;

    public OrCondition()
    {
        this.conditions = new ArrayList<>();
    }

    public OrCondition(List<Condition> conditions)
    {
        this.conditions = conditions;
    }

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        if (conditions.size() == 1)
        {
            conditions.get(0).genSql(builder);
        }
        else
        {
            builder.append(OPEN_PARENTHESIS);
            SqlGen.genSqlList(builder, conditions, EMPTY, ") OR (");
            builder.append(CLOSING_PARENTHESIS);
        }
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof Condition)
        {
            conditions.add((Condition) node);
        }
    }

    void validate() throws SqlDomException
    {
        if (conditions == null || conditions.isEmpty())
        {
            throw new SqlDomException("conditions is empty");
        }
    }
}
