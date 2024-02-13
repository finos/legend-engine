// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.schemaops.statements;

import org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.schemaops.expressions.table.StagedFilesTable;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.DDLStatement;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause.LOAD_DATA;
import static org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause.OVERWRITE;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.COMMA;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.EMPTY;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class CopyStatement implements DDLStatement
{
    private Table table;
    private StagedFilesTable stagedFilesTable;
    private List<Value> columns;

    public CopyStatement()
    {
        columns = new ArrayList<>();
    }

    /*
     Copy GENERIC PLAN for Big Query:
        LOAD DATA OVERWRITE table_name
        (COLUMN_LIST)
        FROM FILES (LOAD_OPTIONS)
     */
    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        builder.append(LOAD_DATA.get());
        builder.append(WHITE_SPACE);
        builder.append(OVERWRITE.get());
        builder.append(WHITE_SPACE);
        table.genSqlWithoutAlias(builder);
        builder.append(WHITE_SPACE);

        builder.append(OPEN_PARENTHESIS);
        SqlGen.genSqlList(builder, columns, EMPTY, COMMA);
        builder.append(CLOSING_PARENTHESIS);

        builder.append(WHITE_SPACE + Clause.FROM.get() + WHITE_SPACE + Clause.FILES + WHITE_SPACE);
        stagedFilesTable.genSql(builder);
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof Table)
        {
            table = (Table) node;
        }
        else if (node instanceof StagedFilesTable)
        {
            stagedFilesTable = (StagedFilesTable) node;
        }
        else if (node instanceof Value)
        {
            columns.add((Value) node);
        }
    }

    void validate() throws SqlDomException
    {
        if (stagedFilesTable == null)
        {
            throw new SqlDomException("stagedFilesTable is mandatory for Copy Table Command");
        }

        if (table == null)
        {
            throw new SqlDomException("table is mandatory for Copy Table Command");
        }
    }
}
