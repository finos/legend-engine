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

package org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.operations.Truncate;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.TruncateTable;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.Collections;

public class TruncateVisitor implements LogicalPlanVisitor<Truncate>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, Truncate current, VisitorContext context)
    {
        // TODO For partitioned Tables, if the table requires a partition filter, Truncate will fail.
        // This should be the approach in that case:
        // 1. UPDATE the table to remove the partition filter requirement
        // 2. Truncate table
        // 3. UPDATE the table to add the partition filter requirement

        TruncateTable truncateTable = new TruncateTable();
        for (Optimizer optimizer : context.optimizers())
        {
            truncateTable = (TruncateTable) optimizer.optimize(truncateTable);
        }
        prev.push(truncateTable);

        return new VisitorResult(truncateTable, Collections.singletonList(current.dataset()));
    }
}
