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

package org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.values.WindowFunction;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.List;

public class WindowFunctionVisitor implements LogicalPlanVisitor<WindowFunction>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, WindowFunction current, VisitorContext context)
    {
        org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.WindowFunction windowFunction =
            new org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.WindowFunction(
                current.alias().orElse(null),
                context.quoteIdentifier());
        for (Optimizer optimizer : context.optimizers())
        {
            windowFunction = (org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.WindowFunction) optimizer.optimize(windowFunction);
        }
        prev.push(windowFunction);

        List<LogicalPlanNode> logicalPlanNodeList = new ArrayList<>();
        logicalPlanNodeList.add(current.windowFunction());
        if (current.orderByFields() != null)
        {
            logicalPlanNodeList.addAll(current.orderByFields());
        }
        if (current.partitionByFields() != null)
        {
            logicalPlanNodeList.addAll(current.partitionByFields());
        }
        return new VisitorResult(windowFunction, logicalPlanNodeList);
    }
}
