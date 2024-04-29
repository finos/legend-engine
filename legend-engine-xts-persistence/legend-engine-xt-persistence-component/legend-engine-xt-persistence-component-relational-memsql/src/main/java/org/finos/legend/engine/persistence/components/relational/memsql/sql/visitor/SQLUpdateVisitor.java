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

package org.finos.legend.engine.persistence.components.relational.memsql.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Update;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.memsql.sqldom.schemaops.statements.UpdateStatement;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.List;

public class SQLUpdateVisitor implements LogicalPlanVisitor<Update>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, Update current, VisitorContext context)
    {
        UpdateStatement updateStatement = new UpdateStatement();
        prev.push(updateStatement);

        List<LogicalPlanNode> logicalPlanNodeList = new ArrayList<>();
        logicalPlanNodeList.add(current.dataset());

        current.joinDataset().ifPresent(joinDataset ->
        {
            logicalPlanNodeList.add(joinDataset);
            logicalPlanNodeList.add(current.joinCondition().get());
        });

        current.whereCondition().ifPresent(logicalPlanNodeList::add);
        logicalPlanNodeList.addAll(current.keyValuePairs());

        return new VisitorResult(updateStatement, logicalPlanNodeList);
    }
}
