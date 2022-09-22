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

package org.finos.legend.engine.persistence.components.relational.snowflake.optmizer;

import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.statements.AlterTable;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.statements.ShowCommand;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;

public class LowerCaseOptimizer extends org.finos.legend.engine.persistence.components.relational.ansi.optimizer.LowerCaseOptimizer
{
    @Override
    public PhysicalPlanNode optimize(PhysicalPlanNode component)
    {
        component = super.optimize(component);
        if (component instanceof AlterTable)
        {
            AlterTable alterTable = (AlterTable) component;
            if (alterTable.getTable() != null)
            {
                Table table = alterTable.getTable();
                table.setDb(table.getDb() != null ? table.getDb().toLowerCase() : table.getDb());
                table.setSchema(table.getSchema() != null ? table.getSchema().toLowerCase() : table.getSchema());
                table.setTable(table.getTable() != null ? table.getTable().toLowerCase() : table.getTable());
                table.setAlias(table.getAlias() != null ? table.getAlias().toLowerCase() : table.getAlias());
            }
            return alterTable;
        }
        else if (component instanceof ShowCommand)
        {
            ShowCommand command = (ShowCommand) component;
            command.setSchemaName(command.getSchemaName() != null ? command.getSchemaName().toLowerCase() : command.getSchemaName());
            command.setDatabaseName(command.getDatabaseName() != null ? command.getDatabaseName().toLowerCase() : command.getDatabaseName());
            command.setTableName(command.getTableName() != null ? command.getTableName().toLowerCase() : command.getTableName());
            return command;
        }
        return component;
    }
}
