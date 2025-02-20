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

package org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor;

import java.util.List;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Update;
import org.finos.legend.engine.persistence.components.logicalplan.values.ObjectValue;

public class SQLUpdateVisitor extends org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.SQLUpdateVisitor
{

    /*
    UODATE always needs A WHERE CLAUSE IN BigQuery
    If the condition is not provided, default condition used: 1 = 1
    */
    @Override
    public void visitWhereCondition(Update current, List<LogicalPlanNode> logicalPlanNodeList)
    {
        if (current.whereCondition().isPresent())
        {
            super.visitWhereCondition(current, logicalPlanNodeList);
        }
        else
        {
            Condition condition = Equals.of(ObjectValue.of(1), ObjectValue.of(1));
            logicalPlanNodeList.add(condition);
        }
    }
}
