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
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.select.SelectExpression;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.CLOSING_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.COMMA;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.OPEN_PARENTHESIS;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class InsertStatement implements DMLStatement
{
    private Table table;
    private final List<Field> columns;
    private SelectExpression selectExpression;

    public InsertStatement()
    {
        columns = new ArrayList<>();
    }

    public InsertStatement(Table table, List<Field> columns, SelectExpression selectExpression)
    {
        this.table = table;
        this.columns = columns;
        this.selectExpression = selectExpression;
    }

    /*
     InsertQuery GENERIC PLAN:
     INSERT INTO table-Name [ (Simple-column-Name [ , Simple-column-Name]* ) ]
     {Select Query | Values Expression}
      1. INSERT INTO TABLE_NAME (column1, column2, column3,...columnN)  VALUES (value1, value2, value3,...valueN);
      2. INSERT INTO first_table_name [(column1, column2, ... columnN)] SELECT column1, column2, ...columnN  FROM second_table_name [WHERE condition];
     */

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        builder.append(Clause.INSERT_INTO.get());
        builder.append(WHITE_SPACE);

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

        builder.append(WHITE_SPACE);
        builder.append(OPEN_PARENTHESIS);
        selectExpression.genSql(builder);
        builder.append(CLOSING_PARENTHESIS);
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
        else if (node instanceof SelectExpression)
        {
            selectExpression = (SelectExpression) node;
        }
    }

    void validate() throws SqlDomException
    {
        if (selectExpression == null)
        {
            throw new SqlDomException("selectExpression is mandatory for Select Statement");
        }

        if (table == null)
        {
            throw new SqlDomException("table is mandatory for Create Table Command");
        }
    }
}
