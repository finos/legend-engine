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

import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.optimizer.Optimizer;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.NumericalValue;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

public class StringValueVisitor implements LogicalPlanVisitor<StringValue>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, StringValue current, VisitorContext context)
    {
        org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.StringValue value = new org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.StringValue(current.value().orElse(null), current.alias().orElse(null), context.quoteIdentifier());
        for (Optimizer optimizer : context.optimizers())
        {
            value = (org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.StringValue) optimizer.optimize(value);
        }
        prev.push(value);
        return new VisitorResult();
    }
}
