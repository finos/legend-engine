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

package org.finos.legend.engine.persistence.components.relational.h2.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.values.ToArrayFunction;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.List;

public class ToArrayFunctionVisitor implements LogicalPlanVisitor<ToArrayFunction>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, ToArrayFunction current, VisitorContext context)
    {
        org.finos.legend.engine.persistence.components.relational.h2.sqldom.schemaops.values.ToArrayFunction function =
            new org.finos.legend.engine.persistence.components.relational.h2.sqldom.schemaops.values.ToArrayFunction(
                current.alias().orElse(null),
                context.quoteIdentifier()
            );

        for (Optimizer optimizer : context.optimizers())
        {
            function = (org.finos.legend.engine.persistence.components.relational.h2.sqldom.schemaops.values.ToArrayFunction) optimizer.optimize(function);
        }
        prev.push(function);

        if (current.values() != null)
        {
            List<LogicalPlanNode> logicalPlanNodeList = new ArrayList<>();
            logicalPlanNodeList.addAll(current.values());
            return new VisitorResult(function, logicalPlanNodeList);
        }
        return new VisitorResult(null);
    }
}
