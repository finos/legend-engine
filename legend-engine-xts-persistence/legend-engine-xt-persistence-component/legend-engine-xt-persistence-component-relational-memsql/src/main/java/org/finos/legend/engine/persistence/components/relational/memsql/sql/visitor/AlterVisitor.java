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

package org.finos.legend.engine.persistence.components.relational.memsql.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.ColumnStoreSpecification;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Alter;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Update;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Pair;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.memsql.sqldom.schemaops.statements.AlterTable;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.AlterOperation;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class AlterVisitor implements LogicalPlanVisitor<Alter>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, Alter current, VisitorContext context)
    {
        boolean isColumnStore = current.dataset().schema().columnStoreSpecification().map(ColumnStoreSpecification::columnStore).orElse(false);

        if (!isColumnStore || current.operation() != Alter.AlterOperation.CHANGE_DATATYPE)
        {
            AlterTable alterTable = new AlterTable(AlterOperation.valueOf(current.operation().name()));
            for (Optimizer optimizer : context.optimizers())
            {
                alterTable = (AlterTable) optimizer.optimize(alterTable);
            }
            prev.push(alterTable);

            List<LogicalPlanNode> logicalPlanNodeList = new ArrayList<>();
            logicalPlanNodeList.add(current.dataset());
            logicalPlanNodeList.add(current.columnDetails());
            current.newColumn().ifPresent(logicalPlanNodeList::add);

            return new VisitorResult(alterTable, logicalPlanNodeList);
        }
        else
        {
            LogicalPlanNode[] logicalPlanNodes = new LogicalPlanNode[]{};
            List<Operation> operationList = new ArrayList<>();

            // Get a new column name
            int count = 1;
            String originalName = current.columnDetails().name();
            String newName = originalName + count;
            Set<String> existingNames = new HashSet<>();
            for (Field field : current.dataset().schema().fields())
            {
                existingNames.add(field.name());
            }
            while (existingNames.contains(newName))
            {
                newName = originalName + (++count);
            }

            Field newColumn = current.columnDetails().withName(newName);

            // Add new column
            Alter addNewColumnOperation = Alter.of(current.dataset(), Alter.AlterOperation.ADD, newColumn, Optional.empty());
            operationList.add(addNewColumnOperation);

            // Copy the old column values to the new column
            Update copyColumnValuesOperation = Update.builder()
                .dataset(current.dataset())
                .addKeyValuePairs(Pair.of(FieldValue.builder().fieldName(newName).build(), FieldValue.builder().fieldName(originalName).build()))
                .build();
            operationList.add(copyColumnValuesOperation);

            // Drop the old column
            Alter dropColumnOperation = Alter.of(current.dataset(), Alter.AlterOperation.DROP, current.columnDetails(), Optional.empty());
            operationList.add(dropColumnOperation);

            // Rename the new column to the old column name
            Alter renameColumnOperation = Alter.of(current.dataset(), Alter.AlterOperation.RENAME_COLUMN, newColumn, current.columnDetails());
            operationList.add(renameColumnOperation);

            Operation[] operations = new Operation[]{};
            return new VisitorResult(null, logicalPlanNodes, operationList.toArray(operations));
        }
    }
}
