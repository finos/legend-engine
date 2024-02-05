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

package org.finos.legend.engine.persistence.components.relational.bigquery.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanNode;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.ExternalDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.schemaops.statements.CreateExternalTable;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CreateExternalDatasetVisitor implements LogicalPlanVisitor<Create>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, Create current, VisitorContext context)
    {
        ExternalDataset externalDataset = (ExternalDataset) current.dataset();

        CreateExternalTable createExternalTable = new CreateExternalTable();
        prev.push(createExternalTable);

        List<LogicalPlanNode> logicalPlanNodes = new ArrayList<>();
        logicalPlanNodes.add(externalDataset);
        logicalPlanNodes.add(externalDataset.stagedFilesDataset());

        // Trim type parameters as Big Query external tables do not allow them
        List<Field> fields = externalDataset.stagedFilesDataset().schema().fields();
        List<Field> trimmedFields = fields.stream().map(field -> field.withType(field.type().withLength(Optional.empty()).withScale(Optional.empty()))).collect(Collectors.toList());
        logicalPlanNodes.add(externalDataset.stagedFilesDataset().schema().withFields(trimmedFields));

        return new VisitorResult(createExternalTable, logicalPlanNodes);
    }
}
