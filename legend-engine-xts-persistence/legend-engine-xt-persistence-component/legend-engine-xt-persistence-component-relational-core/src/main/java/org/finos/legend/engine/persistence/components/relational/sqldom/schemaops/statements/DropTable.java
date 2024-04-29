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
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.TableConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.modifiers.TableModifier;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class DropTable implements DDLStatement
{
    private Table table;
    private List<TableModifier> modifiers = new ArrayList<>();
    private List<TableConstraint> constraints = new ArrayList<>();

    public DropTable()
    {
    }

    public DropTable(Table table, List<TableModifier> modifiers, List<TableConstraint> constraints)
    {
        this.table = table;
        this.modifiers = modifiers;
        this.constraints = constraints;
    }

    /*
     DROP GENERIC PLAN:
     DROP TABLE [modifiers] NAME [constraints]
     */

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        builder.append(Clause.DROP.get());
        builder.append(WHITE_SPACE);
        builder.append(Clause.TABLE.get());

        // modifiers
        SqlGen.genSqlList(builder, modifiers, WHITE_SPACE, WHITE_SPACE);

        // Table name
        builder.append(WHITE_SPACE);
        table.genSqlWithoutAlias(builder);

        // constraints
        SqlGen.genSqlList(builder, constraints, WHITE_SPACE, WHITE_SPACE);
    }


    @Override
    public void push(Object node)
    {
        if (node instanceof Table)
        {
            table = ((Table) node);
        }
        else if (node instanceof TableModifier)
        {
            modifiers.add((TableModifier) node);
        }
        else if (node instanceof TableConstraint)
        {
            constraints.add((TableConstraint) node);
        }
    }

    void validate() throws SqlDomException
    {
        if (table == null)
        {
            throw new SqlDomException("Table is mandatory for Drop Table Command");
        }
    }
}
