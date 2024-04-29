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

import org.finos.legend.engine.persistence.components.logicalplan.constraints.CascadeTableConstraint;
import org.finos.legend.engine.persistence.components.logicalplan.constraints.TableConstraint;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

public class TableConstraintVisitor implements LogicalPlanVisitor<TableConstraint>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, TableConstraint current, VisitorContext context)
    {
        org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.TableConstraint tableConstraint;
        if (current instanceof CascadeTableConstraint)
        {
            tableConstraint = new org.finos.legend.engine.persistence.components.relational.sqldom.constraints.table.CascadeTableConstraint();
        }
        else
        {
            throw new IllegalArgumentException("Unrecognized table constraint '" + current.getClass().getName() + "'");
        }

        prev.push(tableConstraint);
        return new VisitorResult(tableConstraint);
    }
}
