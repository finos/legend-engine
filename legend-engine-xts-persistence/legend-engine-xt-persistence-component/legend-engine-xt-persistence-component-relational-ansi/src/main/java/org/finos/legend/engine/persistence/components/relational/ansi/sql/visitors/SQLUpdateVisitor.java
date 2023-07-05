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
import org.finos.legend.engine.persistence.components.logicalplan.conditions.And;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Exists;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Update;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Pair;
import org.finos.legend.engine.persistence.components.logicalplan.values.SelectValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.UpdateStatement;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;

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

        // If the update has a join
        if (current.joinDataset().isPresent())
        {
            Dataset joinDataset = current.joinDataset().get();
            Condition joinCondition = current.joinCondition().orElse(null);
            Selection selection;
            if (joinDataset instanceof Selection)
            {
                selection = (Selection) joinDataset;
                if (selection.condition().isPresent())
                {
                    joinCondition = And.builder().addConditions(joinCondition, selection.condition().get()).build();
                }
                selection = Selection.builder().source(selection.source()).condition(joinCondition).addAllFields(selection.fields()).build();
            }
            else
            {
                selection = Selection.builder().source(joinDataset).condition(joinCondition).addAllFields(LogicalPlanUtils.ALL_COLUMNS()).build();
            }

            // Push the where condition
            Condition joinConditionInWhereClause = Exists.of(selection);
            if (current.whereCondition().isPresent())
            {
                // AND both the WHERE and JOIN conditions
                logicalPlanNodeList.add(And.builder().addConditions(current.whereCondition().get(), joinConditionInWhereClause).build());
            }
            else
            {
                // Just push the JOIN condition to the WHERE clause
                logicalPlanNodeList.add(joinConditionInWhereClause);
            }
            for (Pair<FieldValue, Value> field : current.keyValuePairs())
            {
                if (field.value() instanceof FieldValue)
                {
                    Dataset selectionSource = joinDataset;
                    if (joinDataset instanceof Selection)
                    {
                        if (selection.source().isPresent())
                        {
                            selectionSource = selection.source().get();
                        }
                    }
                    logicalPlanNodeList.add(Pair.of(
                        field.key(),
                        SelectValue.of(Selection.builder().source(selectionSource).condition(joinCondition).addFields(field.value()).build())));
                    // todo: handle binary operators
                }
                else
                {
                    logicalPlanNodeList.add(field);
                }
            }
        }
        else
        {
            if (current.whereCondition().isPresent())
            {
                logicalPlanNodeList.add(current.whereCondition().get());
            }
            logicalPlanNodeList.addAll(current.keyValuePairs());
        }

        return new VisitorResult(updateStatement, logicalPlanNodeList);
    }
}
