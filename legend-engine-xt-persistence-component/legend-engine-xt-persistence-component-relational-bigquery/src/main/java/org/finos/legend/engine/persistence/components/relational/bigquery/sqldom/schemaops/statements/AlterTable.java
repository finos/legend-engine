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


    @Override
    public void genSql(StringBuilder builder) throws SqlDomException
    {
        // TODO
    }

    @Override
    public void push(Object node)
    {
        // TODO
    }

    void validate() throws SqlDomException
    {
        // TODO
    }
}
