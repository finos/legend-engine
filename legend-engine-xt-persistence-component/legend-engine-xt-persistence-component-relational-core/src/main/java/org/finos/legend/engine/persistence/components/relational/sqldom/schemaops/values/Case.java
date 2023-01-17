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
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.Pair;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.Condition;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;


public class Case extends Value
{

    private List<Pair<Condition, Value>> conditionedValues;
    private Value elseValue;

    public Case(String quoteIdentifier)
    {
        super(quoteIdentifier);
        this.conditionedValues = new ArrayList<>();
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
        builder.append(OPEN_PARENTHESIS);
        builder.append(Clause.CASE);

        for (Pair<Condition, Value> pair : conditionedValues)
        {
            builder.append(WHITE_SPACE);
            builder.append(Clause.WHEN);
            builder.append(WHITE_SPACE);
            pair.getKey().genSql(builder);
            builder.append(WHITE_SPACE);
            builder.append(Clause.THEN);
            builder.append(WHITE_SPACE);
            pair.getValue().genSqlWithoutAlias(builder);
        }

        builder.append(WHITE_SPACE);
        builder.append(Clause.ELSE);
        builder.append(WHITE_SPACE);
        elseValue.genSqlWithoutAlias(builder);
        builder.append(WHITE_SPACE);

        builder.append(Clause.END);
        builder.append(CLOSING_PARENTHESIS);
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof Pair)
        {
            conditionedValues.add((Pair) node);
        }
        else if (node instanceof Value)
        {
            elseValue = (Value) node;
        }
    }
}

