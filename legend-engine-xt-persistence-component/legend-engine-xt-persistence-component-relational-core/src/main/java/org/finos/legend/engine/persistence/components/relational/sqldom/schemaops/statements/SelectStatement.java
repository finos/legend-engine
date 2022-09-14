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

package org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.quantifiers.Quantifier;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.Condition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.select.SelectExpression;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.TableLike;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Expression;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Function;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.ObjectValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.SelectValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.StringValue;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.COMMA;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class SelectStatement extends SelectExpression implements DMLStatement
{
    private Quantifier quantifier;
    private final List<Value> selectItems;
    private Long selectItemsSize;
    private final List<TableLike> tables;
    private Condition condition;
    private final List<Value> groupByFields;

    public SelectStatement()
    {
        this.selectItems = new ArrayList<>();
        this.tables = new ArrayList<>();
        this.groupByFields = new ArrayList<>();
    }

    public SelectStatement(Quantifier quantifier, List<Value> selectItems, List<TableLike> tables, Condition condition, List<Value> groupByFields)
    {
        this.quantifier = quantifier;
        this.selectItems = selectItems;
        this.tables = tables;
        this.condition = condition;
        this.groupByFields = groupByFields;
    }

    /*
     SELECT GENERIC PLAN:

    SELECT [ DISTINCT | ALL ] SelectItem [ , SelectItem ]* [AS alias]
    FROM clause
    [ WHERE clause ]
    [ GROUP BY clause ]
    [ HAVING clause ]
    [ ORDER BY clause ]
    [ result offset clause ]
    [ fetch first clause ]
     */

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();

        if (getAlias() != null)
        {
            builder.append(OPEN_PARENTHESIS);
        }
        builder.append(Clause.SELECT.get());

        // Add quantifier
        if (quantifier != null)
        {
            builder.append(WHITE_SPACE);
            quantifier.genSql(builder);
        }

        // Add SelectItems
        SqlGen.genSqlList(builder, selectItems, WHITE_SPACE, COMMA);

        // Add FROM Clause
        if (!tables.isEmpty())
        {
            builder.append(WHITE_SPACE);
            builder.append(Clause.FROM.get());
            SqlGen.genSqlList(builder, tables, WHITE_SPACE, COMMA);
        }

        // Add where clause
        if (condition != null)
        {
            builder.append(WHITE_SPACE + Clause.WHERE.get() + WHITE_SPACE);
            condition.genSql(builder);
        }

        // Add group by clause
        if (groupByFields != null && groupByFields.size() > 0)
        {
            builder.append(WHITE_SPACE);
            builder.append(Clause.GROUP_BY.get());
            builder.append(WHITE_SPACE);
            // Add groupBy Fields
            for (int i = 0; i < groupByFields.size(); i++)
            {
                groupByFields.get(i).genSqlWithoutAlias(builder);
                if (i < (groupByFields.size() - 1))
                {
                    builder.append(COMMA + WHITE_SPACE);
                }
            }
        }

        if (getAlias() != null)
        {
            builder.append(CLOSING_PARENTHESIS);
            super.genSql(builder);
        }

    }

    @Override
    public void push(Object node)
    {
        if (node instanceof TableLike)
        {
            tables.add((TableLike) node);
        }
        else if (node instanceof Condition)
        {
            condition = (Condition) node;
        }
        else if (node instanceof Quantifier)
        {
            quantifier = (Quantifier) node;
        }
        else if (node instanceof Function)
        {
            selectItems.add((Function) node);
        }
        else if (node instanceof ObjectValue)
        {
            selectItems.add((ObjectValue) node);
        }
        else if (node instanceof SelectValue)
        {
            selectItems.add((SelectValue) node);
        }
        else if (node instanceof StringValue)
        {
            selectItems.add((StringValue) node);
        }
        else if (node instanceof Expression)
        {
            selectItems.add((Expression) node);
        }
        else if (node instanceof Field)
        {
            if (selectItems.size() < selectItemsSize)
            {
                selectItems.add((Field) node);
            }
            else
            {
                groupByFields.add((Field) node);
            }
        }
        else if (node instanceof Value)
        {
            selectItems.add((Value) node);
        }
    }

    void validate() throws SqlDomException
    {
        if (selectItems == null || selectItems.isEmpty())
        {
            throw new SqlDomException("selectItems is mandatory for Select Statement");
        }
    }

    public void setSelectItemsSize(Long selectItemsSize)
    {
        this.selectItemsSize = selectItemsSize;
    }
}
