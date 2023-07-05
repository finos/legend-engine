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
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.Pair;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.Condition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.TableLike;
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
    private final List<Pair<Field, Value>> setPairs;
    private Condition condition;

    public UpdateStatement()
    {
        this.setPairs = new ArrayList<>();
    }

    public UpdateStatement(TableLike table, List<Pair<Field, Value>> setPairs, Condition condition)
    {
        this.table = table;
        this.setPairs = setPairs;
        this.condition = condition;
    }

    /*
     UpdateStatement GENERIC PLAN:
    UPDATE table-Name [[AS] correlation-Name]
    SET column-Name = Value [ , column-Name = Value} ]*
    [WHERE clause]
     */

    /*
    Different DBMS support different flavour of update for join support
    https://stackoverflow.com/questions/1293330/how-can-i-do-an-update-statement-with-join-in-sql-server
     */

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        builder.append(Clause.UPDATE.get());

        // Add table name
        builder.append(WHITE_SPACE);
        table.genSql(builder);

        builder.append(WHITE_SPACE);
        builder.append(Clause.SET.get());

        // Add set Values with Assignment operator
        builder.append(WHITE_SPACE);
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

        // Add where Clause
        if (condition != null)
        {
            builder.append(WHITE_SPACE).append(Clause.WHERE.get()).append(WHITE_SPACE);
            condition.genSql(builder);
        }
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof TableLike)
        {
            table = (TableLike) node;
        }
        else if (node instanceof Condition)
        {
            condition = (Condition) node;
        }
        else if (node instanceof Pair)
        {
            setPairs.add((Pair) node);
        }
    }
}
