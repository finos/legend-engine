// Copyright 2024 Goldman Sachs
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

import java.util.ArrayList;
import java.util.List;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FunctionalDataset;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.expresssions.table.TableFunction;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

public class FunctionalDatasetVisitor implements LogicalPlanVisitor<FunctionalDataset>
{
    @Override
    public VisitorResult visit(PhysicalPlanNode prev, FunctionalDataset current, VisitorContext context)
    {
        TableFunction tableFunction = new TableFunction(
            current.database().orElse(null),
            current.group().orElse(null),
            current.name().orElseThrow(IllegalStateException::new),
            new ArrayList<>(),
            context.quoteIdentifier()
        );

        prev.push(tableFunction);

        if (current.value() != null)
        {
            List<LogicalPlanNode> logicalPlanNodeList = new ArrayList<>(current.value());
            return new VisitorResult(tableFunction, logicalPlanNodeList);
        }
        return new VisitorResult(null);
    }
}
