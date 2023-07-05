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
import org.finos.legend.engine.persistence.components.relational.sqldom.common.AlterOperation;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.NullColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.Column;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.DDLStatement;

import static org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause.COLUMN;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class AlterTable implements DDLStatement
{
    private final AlterOperation operation;
    private Table table;
    private Column columnToAlter;
    private Column newColumn;

    public AlterTable(AlterOperation operation)
    {
        this.operation = operation;
    }

    public Table getTable()
    {
        return table;
    }

    /*
        ALTER [ONLINE] TABLE tbl_name ADD COLUMN col_name { column_definition }

        ALTER [ONLINE] TABLE tbl_name MODIFY COLUMN col_name
        { column_definition }
        | data_type [NULL]

        ALTER TABLE tbl_name DROP COLUMN col_name

        ALTER TABLE tbl_name CHANGE old_col_name new_col_name
         */
    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        builder.append(Clause.ALTER.get());

        builder.append(WHITE_SPACE + Clause.TABLE.get());

        // Table name
        builder.append(WHITE_SPACE);
        table.genSqlWithoutAlias(builder);

        // Operation
        builder.append(WHITE_SPACE);
        if (operation == AlterOperation.RENAME_COLUMN)
        {
            builder.append(org.finos.legend.engine.persistence.components.relational.memsql.sqldom.common.Clause.CHANGE.get());
        }
        else
        {
            if (operation == AlterOperation.CHANGE_DATATYPE || operation == AlterOperation.NULLABLE_COLUMN)
            {
                builder.append(org.finos.legend.engine.persistence.components.relational.memsql.sqldom.common.Clause.MODIFY.get());
            }
            else
            {
                builder.append(operation.name());
            }
            builder.append(WHITE_SPACE);
            builder.append(COLUMN);
        }
        builder.append(WHITE_SPACE);

        // Operation parameters
        switch (operation)
        {
            case ADD:
            case CHANGE_DATATYPE:
                columnToAlter.genSql(builder);
                break;
            case NULLABLE_COLUMN:
                columnToAlter.genSqlWithNameOnly(builder);
                builder.append(WHITE_SPACE);
                columnToAlter.getDataType().genSql(builder);
                builder.append(WHITE_SPACE);
                NullColumnConstraint nullColumnConstraint = new NullColumnConstraint();
                nullColumnConstraint.genSql(builder);
                break;
            case DROP:
                columnToAlter.genSqlWithNameOnly(builder);
                break;
            case RENAME_COLUMN:
                columnToAlter.genSqlWithNameOnly(builder);
                builder.append(WHITE_SPACE);
                newColumn.genSqlWithNameOnly(builder);
                break;
        }
    }

    @Override
    public void push(Object node)
    {
        if (node instanceof Table)
        {
            table = (Table) node;
        }
        else if (node instanceof Column)
        {
            if (columnToAlter == null)
            {
                columnToAlter = (Column) node;
            }
            else
            {
                newColumn = (Column) node;
            }
        }
    }

    void validate() throws SqlDomException
    {
        if (table == null)
        {
            throw new SqlDomException("Table is mandatory for Alter Table Command");
        }
        if (columnToAlter == null)
        {
            throw new SqlDomException("Columns details is mandatory for Alter Table Command");
        }
    }
}
