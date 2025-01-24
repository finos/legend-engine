// Copyright 2025 Goldman Sachs
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

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.values.DistinctFunction;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.List;

public class DistinctFunctionVisitor implements LogicalPlanVisitor<DistinctFunction>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, DistinctFunction current, VisitorContext context)
    {
        org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.values.DistinctFunction distinctFunction =
            new org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.values.DistinctFunction(context.quoteIdentifier());

        for (Optimizer optimizer : context.optimizers())
        {
            distinctFunction = (org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.values.DistinctFunction) optimizer.optimize(distinctFunction);
        }
        prev.push(distinctFunction);

        if (current.values() != null)
        {
            List<LogicalPlanNode> logicalPlanNodeList = new ArrayList<>(current.values());
            return new VisitorResult(distinctFunction, logicalPlanNodeList);
        }

        return new VisitorResult(null);
    }
}
