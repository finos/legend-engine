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

import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.conditions.logical.OrCondition;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

public class OrVisitor implements LogicalPlanVisitor<org.finos.legend.engine.persistence.components.logicalplan.conditions.Or>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, org.finos.legend.engine.persistence.components.logicalplan.conditions.Or current, VisitorContext context)
    {
        OrCondition orCondition = new OrCondition();
        prev.push(orCondition);

        return new VisitorResult(orCondition, current.conditions());
    }
}
