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
import org.finos.legend.engine.persistence.components.logicalplan.operations.Merge;
import org.finos.legend.engine.persistence.components.logicalplan.values.NumericalValue;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.MergeStatement;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.List;

public class SQLMergeVisitor implements LogicalPlanVisitor<Merge>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, Merge current, VisitorContext context)
    {
        MergeStatement mergeStatement = new MergeStatement();
        prev.push(mergeStatement);

        List<LogicalPlanNode> logicalPlanNodeList = new ArrayList<>();
        logicalPlanNodeList.add(current.dataset());
        logicalPlanNodeList.add(current.usingDataset());

        current.onCondition().ifPresent(logicalPlanNodeList::add);
        current.matchedCondition().ifPresent(logicalPlanNodeList::add);

        logicalPlanNodeList.add(NumericalValue.of((long) current.matchedKeyValuePairs().size()));
        logicalPlanNodeList.addAll(current.matchedKeyValuePairs());
        logicalPlanNodeList.addAll(current.unmatchedKeyValuePairs());

        return new VisitorResult(mergeStatement, logicalPlanNodeList);
    }
}
