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
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.sqldom.common.FunctionName;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.List;

public class FunctionVisitor implements LogicalPlanVisitor<FunctionImpl>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, FunctionImpl current, VisitorContext context)
    {
        org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Function function =
            new org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.values.Function(
                FunctionName.fromName(current.functionName().get()),
                new ArrayList<>(),
                current.alias().orElse(null));

        prev.push(function);

        if (current.value() != null)
        {
            List<LogicalPlanNode> logicalPlanNodeList = new ArrayList<>(current.value());
            return new VisitorResult(function, logicalPlanNodeList);
        }
        return new VisitorResult(null);
    }
}
