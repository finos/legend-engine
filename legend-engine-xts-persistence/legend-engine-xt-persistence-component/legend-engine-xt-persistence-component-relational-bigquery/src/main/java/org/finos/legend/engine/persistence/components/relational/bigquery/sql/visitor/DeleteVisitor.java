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

package org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Delete;
import org.finos.legend.engine.persistence.components.logicalplan.values.ObjectValue;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.DeleteStatement;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.List;

public class DeleteVisitor implements LogicalPlanVisitor<Delete>
{

    /*
    DELETE always needs A WHERE CLAUSE IN BigQuery
    If the condition is not provided, default condition used: 1 = 1
    */

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, Delete current, VisitorContext context)
    {
        Condition condition = current.condition().orElseGet(() -> Equals.of(ObjectValue.of(1), ObjectValue.of(1)));
        DeleteStatement deleteStatement = new DeleteStatement();
        prev.push(deleteStatement);

        List<LogicalPlanNode> logicalPlanNodeList = new ArrayList<>();
        logicalPlanNodeList.add(current.dataset());
        logicalPlanNodeList.add(condition);

        return new VisitorResult(deleteStatement, logicalPlanNodeList);
    }
}
