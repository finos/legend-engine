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

import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DerivedDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class DerivedDatasetVisitor implements LogicalPlanVisitor<DerivedDataset>
{
    @Override
    public VisitorResult visit(PhysicalPlanNode prev, DerivedDataset current, VisitorContext context)
    {
        Condition filterCondition = LogicalPlanUtils.getDatasetFilterCondition(current);
        List<Value> allColumns = new ArrayList<>(current.schemaReference().fieldValues());
        Selection selection = Selection.builder()
                .source(current.datasetReference())
                .addAllFields(allColumns)
                .condition(filterCondition)
                .alias(current.datasetReference().alias())
                .build();
        return new SelectionVisitor().visit(prev, selection, context);
    }
}