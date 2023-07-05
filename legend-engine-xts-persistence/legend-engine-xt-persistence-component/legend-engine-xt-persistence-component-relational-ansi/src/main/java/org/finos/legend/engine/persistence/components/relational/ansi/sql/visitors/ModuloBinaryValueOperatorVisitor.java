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

package org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors;

import org.finos.legend.engine.persistence.components.logicalplan.values.ModuloBinaryValueOperator;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.Operator;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Expression;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.Arrays;

public class ModuloBinaryValueOperatorVisitor implements LogicalPlanVisitor<ModuloBinaryValueOperator>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, ModuloBinaryValueOperator current, VisitorContext context)
    {
        Expression e = new Expression(Operator.MOD, current.alias().orElse(null), context.quoteIdentifier());
        for (Optimizer optimizer : context.optimizers())
        {
            e = (Expression) optimizer.optimize(e);
        }
        prev.push(e);
        return new VisitorResult(e, Arrays.asList(current.left(), current.right()));
    }
}
