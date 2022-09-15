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
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Clause;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.ShowType;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;

import static org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils.WHITE_SPACE;

public class ShowCommand implements SqlGen
{
    private final ShowType operation;
    private String databaseName;
    private String schemaName;
    private String tableName;

    public ShowCommand(ShowType operation, String databaseName, String schemaName, String tableName)
    {
        this.operation = operation;
        this.databaseName = databaseName;
        this.schemaName = schemaName;
        this.tableName = tableName;
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    public void setDatabaseName(String databaseName)
    {
        this.databaseName = databaseName;
    }

    public String getSchemaName()
    {
        return schemaName;
    }

    public void setSchemaName(String schemaName)
    {
        this.schemaName = schemaName;
    }

    public String getTableName()
    {
        return tableName;
    }

    public void setTableName(String tableName)
    {
        this.tableName = tableName;
    }

    /*
    SHOW [FULL] COLUMNS
    {FROM | IN} tbl_name
    [{FROM | IN} db_name]
    [LIKE 'pattern' | WHERE expr]

    SHOW [FULL] [TEMPORARY] TABLES
    [{FROM | IN} db_name]
    [[EXTENDED] LIKE pattern | WHERE TABLE_TYPE {= | !=} {'VIEW' | 'BASE TABLE'}]

    SHOW SCHEMAS
    [LIKE 'pattern']
     */

    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        validate();
        builder.append(Clause.SHOW.get());
        builder.append(WHITE_SPACE + operation.name());

        if (operation == ShowType.COLUMNS)
        {
            builder.append(WHITE_SPACE + Clause.FROM.get());
            builder.append(WHITE_SPACE + tableName);
            if ((databaseName != null && !databaseName.isEmpty()) && (schemaName != null && !schemaName.isEmpty()))
            {
                builder.append(WHITE_SPACE);
                builder.append(Clause.IN.get() + WHITE_SPACE);
                builder.append(databaseName + SqlGenUtils.DOT + schemaName);
            }
            else if (schemaName != null && !schemaName.isEmpty())
            {
                builder.append(WHITE_SPACE);
                builder.append(Clause.IN.get() + WHITE_SPACE);
                builder.append(schemaName);
            }
        }
        else if (operation == ShowType.TABLES)
        {
            if ((databaseName != null && !databaseName.isEmpty()) && (schemaName != null && !schemaName.isEmpty()))
            {
                builder.append(WHITE_SPACE);
                builder.append(Clause.FROM.get() + WHITE_SPACE);
                builder.append(databaseName + SqlGenUtils.DOT + schemaName);
            }
            else if (schemaName != null && !schemaName.isEmpty())
            {
                builder.append(WHITE_SPACE);
                builder.append(Clause.FROM.get() + WHITE_SPACE);
                builder.append(schemaName);
            }
            builder.append(WHITE_SPACE + Clause.LIKE.get());
            builder.append(WHITE_SPACE);
            builder.append(SqlGenUtils.singleQuote(tableName));
        }
    }

    void validate() throws SqlDomException
    {
        if (operation == null)
        {
            throw new SqlDomException("Operation is mandatory for Show Command");
        }
        if (tableName == null || tableName.isEmpty())
        {
            throw new SqlDomException("Table name is mandatory for Show Command");
        }
    }
}
