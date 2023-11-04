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

package org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.statements;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.DMLStatement;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.SelectStatement;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.COMMA;

public class CopyStatement implements DMLStatement
{
    private Table table;
    private final List<Field> columns;
    private SelectStatement selectStatement;

    public CopyStatement()
    {
        columns = new ArrayList<>();
    }

    public CopyStatement(Table table, List<Field> columns, SelectStatement selectStatement)
    {
        this.table = table;
        this.columns = columns;
        this.selectStatement = selectStatement;
    }

    /*
     Copy GENERIC PLAN for Snowflake:
        COPY INTO [<namespace>.]<table_name> (COLUMN_LIST)
        FROM
        ( SELECT [<alias>.]$<file_col_num>[:<element>] [ , [<alias>.]$<file_col_num>[:<element>] , ...  ]
            FROM { <internal_location> | <external_location> }
        [ ( FILE_FORMAT => '<namespace>.<named_file_format>', PATTERN => '<regex_pattern>' ) ] [ <alias> ] )
        on_error = 'ABORT_STATEMENT'
     */

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        builder.append("COPY INTO ");

        // Add table name
        table.genSqlWithoutAlias(builder);
        builder.append(WHITE_SPACE);

        // Add column names
        if (columns != null && columns.size() > 0)
        {
            builder.append(OPEN_PARENTHESIS);
            for (int i = 0; i < columns.size(); i++)
            {
                columns.get(i).genSqlWithNameOnly(builder);
                if (i < (columns.size() - 1))
                {
                    builder.append(COMMA + WHITE_SPACE);
                }
            }
            builder.append(CLOSING_PARENTHESIS);
        }

        builder.append(WHITE_SPACE + Clause.FROM.get() + WHITE_SPACE);

        builder.append(OPEN_PARENTHESIS);
        selectStatement.genSql(builder);
        builder.append(CLOSING_PARENTHESIS);

        builder.append(WHITE_SPACE);
        builder.append("on_error = 'ABORT_STATEMENT'");
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof Table)
        {
            table = (Table) node;
        }
        else if (node instanceof Field)
        {
            columns.add((Field) node);
        }
        else if (node instanceof SelectStatement)
        {
            selectStatement = (SelectStatement) node;
        }
    }

    void validate() throws SqlDomException
    {
        if (selectStatement == null)
        {
            throw new SqlDomException("selectStatement is mandatory for Copy Table Command");
        }

        if (table == null)
        {
            throw new SqlDomException("table is mandatory for Copy Table Command");
        }
    }
}
