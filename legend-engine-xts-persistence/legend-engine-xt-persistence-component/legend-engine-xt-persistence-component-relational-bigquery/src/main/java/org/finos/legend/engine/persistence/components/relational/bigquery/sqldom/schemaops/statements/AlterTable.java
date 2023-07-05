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

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.AlterOperation;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.column.NotNullColumnConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.Column;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.DDLStatement;

import static org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause.COLUMN;
import static org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause.DROP;
import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class AlterTable implements DDLStatement
{
    private final AlterOperation operation;
    private Table table;
    private Column columnToAlter;

    public AlterTable(AlterOperation operation)
    {
        this.operation = operation;
    }

    public Table getTable()
    {
        return table;
    }

    /*
    Add column:
        ALTER TABLE [TABLE_NAME] ADD COLUMN [NEW_COLUMN_NAME] [COLUMN_DATATYPE]
    Drop Not Null:
        ALTER TABLE [TABLE_NAME] ALTER COLUMN [COLUMN_NAME] DROP NOT NULL
    Change Data type:
        ALTER TABLE [TABLE_NAME] ALTER COLUMN [COLUMN_NAME] SET DATA TYPE column_schema
        https://cloud.google.com/bigquery/docs/reference/standard-sql/data-definition-language#alter_column_set_data_type_statement
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
        if (operation.getParent() == null)
        {
            builder.append(operation.name());
        }
        else
        {
            builder.append(operation.getParent().name());
        }
        builder.append(WHITE_SPACE);
        builder.append(COLUMN);
        builder.append(WHITE_SPACE);

        switch (operation)
        {
            case ADD:
                columnToAlter.genSql(builder);
                break;
            case CHANGE_DATATYPE:
                columnToAlter.genSqlWithNameOnly(builder);
                builder.append(WHITE_SPACE);
                builder.append(Clause.SET.get());
                builder.append(WHITE_SPACE);
                builder.append(Clause.DATA_TYPE.get());
                builder.append(WHITE_SPACE);
                columnToAlter.genSqlWithTypeOnly(builder);
                break;
            case NULLABLE_COLUMN:
                columnToAlter.genSqlWithNameOnly(builder);
                builder.append(WHITE_SPACE);
                builder.append(DROP);
                builder.append(WHITE_SPACE);
                NotNullColumnConstraint notNullColumnConstraint = new NotNullColumnConstraint();
                notNullColumnConstraint.genSql(builder);
                break;
            default:
                throw new SqlDomException("Alter operation " + operation.name() + " not supported");
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
            columnToAlter = (Column) node;
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
