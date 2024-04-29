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

package org.finos.legend.engine.persistence.components.relational.memsql.sqldom.schemaops.statements;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Join;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.Pair;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.Condition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.TableLike;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.DMLStatement;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.ASSIGNMENT_OPERATOR;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.COMMA;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class UpdateStatement implements DMLStatement
{
    private TableLike table;
    private TableLike joinTable;
    private List<Pair<Field, Value>> setPairs = new ArrayList<>();
    private Condition whereCondition;
    private Condition joinCondition;

    /*
     UpdateStatement GENERIC PLAN:
    UPDATE table-Name [[AS] correlation-Name]
    SET column-Name = Value [ , column-Name = Value} ]*
    [WHERE clause]

     UpdateStatement with INNER JOIN clause:
    UPDATE table-name
    INNER JOIN table-reference
    ON join-condition
    SET column-Name = Value [ , column-Name = Value ]*
    [WHERE clause]
     */

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        builder.append(Clause.UPDATE.get());

        // Add table name
        builder.append(WHITE_SPACE);
        table.genSql(builder);

        builder.append(WHITE_SPACE);

        // If JOIN
        if (joinTable != null)
        {
            builder.append(Join.INNER_JOIN.get() + WHITE_SPACE);
            joinTable.genSql(builder);
            builder.append(WHITE_SPACE);
            builder.append(Clause.ON.get() + WHITE_SPACE);
            joinCondition.genSql(builder);
            builder.append(WHITE_SPACE);
        }

        // SET
        builder.append(Clause.SET.get() + WHITE_SPACE);
        for (int ctr = 0; ctr < setPairs.size(); ctr++)
        {
            setPairs.get(ctr).getKey().genSql(builder);
            builder.append(WHITE_SPACE);
            builder.append(ASSIGNMENT_OPERATOR);
            builder.append(WHITE_SPACE);
            setPairs.get(ctr).getValue().genSql(builder);
            if (ctr < (setPairs.size() - 1))
            {
                builder.append(COMMA);
            }
        }

        if (whereCondition != null)
        {
            builder.append(WHITE_SPACE + Clause.WHERE.get() + WHITE_SPACE);
            whereCondition.genSql(builder);
        }
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof TableLike)
        {
            if (table == null)
            {
                table = (TableLike) node;
            }
            else
            {
                joinTable = (TableLike) node;
            }
        }
        else if (node instanceof Condition)
        {
            if (joinTable != null && joinCondition == null)
            {
                joinCondition = (Condition) node;
            }
            else
            {
                whereCondition = (Condition) node;
            }
        }
        else if (node instanceof Pair)
        {
            setPairs.add((Pair) node);
        }
    }

    void validate() throws SqlDomException
    {
        if (joinTable != null && joinCondition == null)
        {
            throw new SqlDomException("Join Condition must not be null when Join Table is non-null");
        }
        if (joinCondition != null && joinTable == null)
        {
            throw new SqlDomException("Join Table must not be null when Join Condition is non-null");
        }
    }
}
