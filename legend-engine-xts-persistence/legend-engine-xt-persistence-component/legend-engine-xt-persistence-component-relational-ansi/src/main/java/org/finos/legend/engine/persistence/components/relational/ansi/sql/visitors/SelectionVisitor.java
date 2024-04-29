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
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DerivedDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FilteredDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.sqldom.schemaops.statements.SelectStatement;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;

import java.util.ArrayList;
import java.util.List;

public class SelectionVisitor implements LogicalPlanVisitor<Selection>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, Selection current, VisitorContext context)
    {
        SelectStatement selectStatement = new SelectStatement();
        current.alias().ifPresent(selectStatement::setAlias);
        prev.push(selectStatement);

        List<LogicalPlanNode> logicalPlanNodeList = new ArrayList<>();
        List<Condition> whereConditions = new ArrayList<>();
        current.condition().ifPresent(whereConditions::add);

        if (current.source().isPresent())
        {
            Dataset dataset = current.source().get();
            /* Optimize Scenarios where using Derived Dataset:
                Convert unnecessary inner queries like this
                select id from (select * from table where condition)
                    to
                select id from table where condition
            */
            if (dataset instanceof DerivedDataset)
            {
                DerivedDataset derivedDataset = (DerivedDataset) dataset;
                Condition filterCondition = LogicalPlanUtils.getDatasetFilterCondition(derivedDataset);
                whereConditions.add(filterCondition);
                logicalPlanNodeList.add(derivedDataset.datasetReference());
            }
            else if (dataset instanceof FilteredDataset)
            {
                FilteredDataset filteredDataset = (FilteredDataset) dataset;
                Condition filterCondition = filteredDataset.filter();
                whereConditions.add(filterCondition);
                logicalPlanNodeList.add(filteredDataset.datasetReference());
            }
            else
            {
                logicalPlanNodeList.add(dataset);
            }
        }

        if (current.fields().isEmpty())
        {
            current.source().map(Dataset::schemaReference).ifPresent(logicalPlanNodeList::add);
            selectStatement.setSelectItemsSize((long) current.source().get().schemaReference().fieldValues().size());
        }
        else
        {
            logicalPlanNodeList.addAll(current.fields());
            selectStatement.setSelectItemsSize((long) current.fields().size());
        }

        if (current.limit().isPresent())
        {
            selectStatement.setLimit(current.limit().get());
        }

        if (!whereConditions.isEmpty())
        {
            selectStatement.setHasWhereCondition(true);
            logicalPlanNodeList.add(And.of(whereConditions));
        }

        current.groupByFields().ifPresent(logicalPlanNodeList::addAll);
        current.havingCondition().ifPresent(logicalPlanNodeList::add);
        current.quantifier().ifPresent(logicalPlanNodeList::add);

        return new VisitorResult(selectStatement, logicalPlanNodeList);
    }
}
