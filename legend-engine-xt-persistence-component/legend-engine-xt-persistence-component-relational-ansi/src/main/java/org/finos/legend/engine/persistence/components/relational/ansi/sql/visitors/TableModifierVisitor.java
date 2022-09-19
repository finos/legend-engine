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

import org.finos.legend.engine.persistence.components.logicalplan.modifiers.IfExistsTableModifier;
import org.finos.legend.engine.persistence.components.logicalplan.modifiers.IfNotExistsTableModifier;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.sqldom.modifiers.TableModifier;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

public class TableModifierVisitor implements LogicalPlanVisitor<org.finos.legend.engine.persistence.components.logicalplan.modifiers.TableModifier>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, org.finos.legend.engine.persistence.components.logicalplan.modifiers.TableModifier current, VisitorContext context)
    {
        TableModifier tableModifier;
        if (current instanceof IfExistsTableModifier)
        {
            tableModifier = new org.finos.legend.engine.persistence.components.relational.sqldom.modifiers.IfExistsTableModifier();
        }
        else if (current instanceof IfNotExistsTableModifier)
        {
            tableModifier = new org.finos.legend.engine.persistence.components.relational.sqldom.modifiers.IfNotExistsTableModifier();
        }
        else
        {
            throw new IllegalArgumentException("Unrecognized table modifier '" + current.getClass().getName() + "'");
        }

        prev.push(tableModifier);
        return new VisitorResult(tableModifier);
    }
}
