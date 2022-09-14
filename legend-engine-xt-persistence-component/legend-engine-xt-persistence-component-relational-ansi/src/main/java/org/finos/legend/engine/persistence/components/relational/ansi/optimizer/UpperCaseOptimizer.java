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

package org.finos.legend.engine.persistence.components.relational.ansi.optimizer;

import org.finos.legend.engine.persistence.components.optimizer.CaseConversionOptimizer;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.ForeignKeyTableConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.PrimaryKeyTableConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.TableIndexConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.UniqueTableConstraint;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.Column;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.Table;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.ShowCommand;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.TruncateTable;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Field;

import java.util.stream.Collectors;

public class UpperCaseOptimizer implements CaseConversionOptimizer
{

    @Override
    public PhysicalPlanNode optimize(PhysicalPlanNode component)
    {
        if (component instanceof Field)
        {
            Field field = (Field) component;
            field.setName(field.getName() != null ? field.getName().toUpperCase() : field.getName());
            field.setAlias(field.getAlias() != null ? field.getAlias().toUpperCase() : field.getAlias());
            return field;
        }
        else if (component instanceof Column)
        {
            Column column = (Column) component;
            column.setColumnName(column.getColumnName() != null ? column.getColumnName().toUpperCase() : column.getColumnName());
            return column;
        }
        else if (component instanceof Table)
        {
            Table table = (Table) component;
            table.setDb(table.getDb() != null ? table.getDb().toUpperCase() : table.getDb());
            table.setSchema(table.getSchema() != null ? table.getSchema().toUpperCase() : table.getSchema());
            table.setTable(table.getTable() != null ? table.getTable().toUpperCase() : table.getTable());
            table.setAlias(table.getAlias() != null ? table.getAlias().toUpperCase() : table.getAlias());
            return table;
        }
        else if (component instanceof TruncateTable)
        {
            TruncateTable truncateTable = (TruncateTable) component;
            if (truncateTable.getTable() != null)
            {
                Table table = truncateTable.getTable();
                table.setDb(table.getDb() != null ? table.getDb().toUpperCase() : table.getDb());
                table.setSchema(table.getSchema() != null ? table.getSchema().toUpperCase() : table.getSchema());
                table.setTable(table.getTable() != null ? table.getTable().toUpperCase() : table.getTable());
                table.setAlias(table.getAlias() != null ? table.getAlias().toUpperCase() : table.getAlias());
            }
            return truncateTable;
        }
        else if (component instanceof ForeignKeyTableConstraint)
        {
            ForeignKeyTableConstraint foreignKeyTableConstraint = (ForeignKeyTableConstraint) component;
            return foreignKeyTableConstraint.withColumnNames(foreignKeyTableConstraint.getColumnNames().stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList()));
        }
        else if (component instanceof PrimaryKeyTableConstraint)
        {
            PrimaryKeyTableConstraint primaryKeyTableConstraint = (PrimaryKeyTableConstraint) component;
            return primaryKeyTableConstraint.withColumnNames(primaryKeyTableConstraint.getColumnNames().stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList()));
        }
        else if (component instanceof TableIndexConstraint)
        {
            TableIndexConstraint tableIndexConstraint = (TableIndexConstraint) component;
            return tableIndexConstraint.withColumnNamesAndIndexName(
                tableIndexConstraint.getColumnNames()
                    .stream()
                    .map(String::toUpperCase)
                    .collect(Collectors.toList()),
                tableIndexConstraint.getIndexName() == null
                    ? null
                    : tableIndexConstraint.getIndexName().toUpperCase());
        }
        else if (component instanceof UniqueTableConstraint)
        {
            UniqueTableConstraint uniqueTableConstraint = (UniqueTableConstraint) component;
            return uniqueTableConstraint.withColumnNames(uniqueTableConstraint.getColumnNames().stream()
                .map(String::toUpperCase)
                .collect(Collectors.toList()));
        }
        else if (component instanceof ShowCommand)
        {
            ShowCommand command = (ShowCommand) component;
            command.setSchemaName(command.getSchemaName() != null ? command.getSchemaName().toUpperCase() : command.getSchemaName());
            return command;
        }
        return component;
    }
}
