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

package org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.operations.Show;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.statements.ShowCommand;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.ShowType;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

public class ShowVisitor implements LogicalPlanVisitor<Show>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, Show current, VisitorContext context)
    {
        ShowCommand command;
        if (current.dataset().datasetReference().database().isPresent() && current.dataset().datasetReference().group().isPresent())
        {
            command = new ShowCommand(
                ShowType.valueOf(current.operation().name()),
                current.dataset().datasetReference().database().get(),
                current.dataset().datasetReference().group().get(),
                current.dataset().datasetReference().name().orElseThrow(IllegalStateException::new));
        }
        else if (current.dataset().datasetReference().group().isPresent())
        {
            command = new ShowCommand(
                ShowType.valueOf(current.operation().name()),
                null,
                current.dataset().datasetReference().group().get(),
                current.dataset().datasetReference().name().orElseThrow(IllegalStateException::new));
        }
        else
        {
            command = new ShowCommand(
                ShowType.valueOf(current.operation().name()),
                null,
                null,
                current.dataset().datasetReference().name().orElseThrow(IllegalStateException::new));
        }
        for (Optimizer optimizer : context.optimizers())
        {
            command = (ShowCommand) optimizer.optimize(command);
        }

        prev.push(command);
        return new VisitorResult(null);
    }
}
