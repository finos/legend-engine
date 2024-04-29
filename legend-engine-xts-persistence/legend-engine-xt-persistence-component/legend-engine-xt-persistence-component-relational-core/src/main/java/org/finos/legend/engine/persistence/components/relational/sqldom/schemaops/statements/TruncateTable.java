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
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class TruncateTable implements DDLStatement
{
    private Table table;

    public TruncateTable()
    {
    }

    public TruncateTable(Table table)
    {
        this.table = table;
    }

    public Table getTable()
    {
        return table;
    }

    /*
     TRUNCATE GENERIC PLAN:
     TRUNCATE TABLE NAME
     */

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        builder.append(Clause.TRUNCATE.get());
        builder.append(WHITE_SPACE);
        builder.append(Clause.TABLE.get());

        // Table name
        builder.append(WHITE_SPACE);
        table.genSqlWithoutAlias(builder);
    }


    @Override
    public void push(Object node)
    {
        if (node instanceof Table)
        {
            table = ((Table) node);
        }
    }

    void validate() throws SqlDomException
    {
        if (table == null)
        {
            throw new SqlDomException("Table is mandatory for Truncate Table Command");
        }
    }
}
