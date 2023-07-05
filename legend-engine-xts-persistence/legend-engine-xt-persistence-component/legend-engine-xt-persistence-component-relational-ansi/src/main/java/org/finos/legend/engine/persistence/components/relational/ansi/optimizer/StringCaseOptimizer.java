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
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Value;

import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class StringCaseOptimizer implements CaseConversionOptimizer
{
    protected Function<String, String> strategy;

    public StringCaseOptimizer(Function<String, String> strategy)
    {
        this.strategy = strategy;
    }

    @Override
    public PhysicalPlanNode optimize(PhysicalPlanNode component)
    {
        if (component instanceof Field)
        {
            Field field = (Field) component;
            field.setName(applyCase(field.getName()));
            field.setAlias(applyCase(field.getAlias()));
            return field;
        }
        else if (component instanceof Value)
        {
            Value value = (Value) component;
            value.setAlias(applyCase(value.getAlias()));
        }
        else if (component instanceof Column)
        {
            Column column = (Column) component;
            column.setColumnName(applyCase(column.getColumnName()));
            return column;
        }
        else if (component instanceof Table)
        {
            Table table = (Table) component;
            applyCase(table);
            return table;
        }
        else if (component instanceof TruncateTable)
        {
            TruncateTable truncateTable = (TruncateTable) component;
            if (truncateTable.getTable() != null)
            {
                Table table = truncateTable.getTable();
                applyCase(table);
            }
            return truncateTable;
        }
        else if (component instanceof ForeignKeyTableConstraint)
        {
            ForeignKeyTableConstraint foreignKeyTableConstraint = (ForeignKeyTableConstraint) component;
            return foreignKeyTableConstraint.withColumnNames(foreignKeyTableConstraint.getColumnNames().stream()
                    .map(strategy)
                    .collect(Collectors.toList()));
        }
        else if (component instanceof PrimaryKeyTableConstraint)
        {
            PrimaryKeyTableConstraint primaryKeyTableConstraint = (PrimaryKeyTableConstraint) component;
            return primaryKeyTableConstraint.withColumnNames(primaryKeyTableConstraint.getColumnNames().stream()
                    .map(strategy)
                    .collect(Collectors.toList()));
        }
        else if (component instanceof TableIndexConstraint)
        {
            TableIndexConstraint tableIndexConstraint = (TableIndexConstraint) component;
            return tableIndexConstraint.withColumnNamesAndIndexName(
                    tableIndexConstraint.getColumnNames()
                            .stream()
                            .map(strategy)
                            .collect(Collectors.toList()),
                    applyCase(tableIndexConstraint.getIndexName()));
        }
        else if (component instanceof UniqueTableConstraint)
        {
            UniqueTableConstraint uniqueTableConstraint = (UniqueTableConstraint) component;
            return uniqueTableConstraint.withColumnNames(uniqueTableConstraint.getColumnNames().stream()
                    .map(strategy)
                    .collect(Collectors.toList()));
        }
        else if (component instanceof ShowCommand)
        {
            ShowCommand command = (ShowCommand) component;
            command.setSchemaName(applyCase(command.getSchemaName()));
            return command;
        }
        return component;
    }

    protected void applyCase(Table table)
    {
        table.setDb(applyCase(table.getDb()));
        table.setSchema(applyCase(table.getSchema()));
        table.setTable(applyCase(table.getTable()));
    }

    protected String applyCase(String field)
    {
        return field != null ? this.strategy.apply(field) : field;
    }
}
