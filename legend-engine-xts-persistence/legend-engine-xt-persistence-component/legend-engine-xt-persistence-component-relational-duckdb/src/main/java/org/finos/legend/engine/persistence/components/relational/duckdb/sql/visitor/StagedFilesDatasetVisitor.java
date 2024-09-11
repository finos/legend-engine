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

package org.finos.legend.engine.persistence.components.relational.duckdb.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesSelection;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;

import java.util.List;

public class StagedFilesDatasetVisitor implements LogicalPlanVisitor<StagedFilesDataset>
{
    @Override
    public VisitorResult visit(PhysicalPlanNode prev, StagedFilesDataset current, VisitorContext context)
    {
        List<Value> allColumns = LogicalPlanUtils.extractStagedFilesFieldValues(current);
        StagedFilesSelection selection = StagedFilesSelection.builder()
            .source(current)
            .addAllFields(allColumns)
            .alias(current.datasetReference().alias())
            .build();
        return new StagedFilesSelectionVisitor().visit(prev, selection, context);
    }
}
