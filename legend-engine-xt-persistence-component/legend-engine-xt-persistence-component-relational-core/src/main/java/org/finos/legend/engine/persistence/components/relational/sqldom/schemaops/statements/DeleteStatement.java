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
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.Condition;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class DeleteStatement implements DMLStatement
{
    private Table table;
    private Condition condition;

    public DeleteStatement()
    {
    }

    public DeleteStatement(Table table, Condition condition)
    {
        this.table = table;
        this.condition = condition;
    }

    /*
             DELETE GENERIC PLAN:
             DELETE FROM table-Name [[AS] correlation-Name] [WHERE clause]
             */
    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        builder.append(Clause.DELETE_FROM.get());

        // Table name
        builder.append(WHITE_SPACE);
        table.genSql(builder);

        // Add where Clause
        if (condition != null)
        {
            builder.append(WHITE_SPACE + Clause.WHERE.get() + WHITE_SPACE);
            condition.genSql(builder);
        }
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof Table)
        {
            table = (Table) node;
        }
        else if (node instanceof Condition)
        {
            condition = (Condition) node;
        }
    }

    void validate() throws SqlDomException
    {
        // table is mandatory
        if (table == null)
        {
            throw new SqlDomException("Table is mandatory for Delete Table Command");
        }
    }
}