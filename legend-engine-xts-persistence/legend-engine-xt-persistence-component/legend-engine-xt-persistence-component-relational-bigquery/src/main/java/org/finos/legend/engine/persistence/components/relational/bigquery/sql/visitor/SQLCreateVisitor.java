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
import org.finos.legend.engine.persistence.components.logicalplan.datasets.ExternalDataset;
import org.finos.legend.engine.persistence.components.logicalplan.modifiers.IfNotExistsTableModifier;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.schemaops.statements.CreateTable;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.ArrayList;
import java.util.List;

public class SQLCreateVisitor implements LogicalPlanVisitor<Create>
{

    @Override
    public VisitorResult visit(PhysicalPlanNode prev, Create current, VisitorContext context)
    {
        if (current.dataset() instanceof ExternalDataset)
        {
            return new CreateExternalDatasetVisitor().visit(prev, current, context);
        }

        CreateTable createTable = new CreateTable();
        prev.push(createTable);

        List<LogicalPlanNode> logicalPlanNodes = new ArrayList<>();
        logicalPlanNodes.add(current.dataset().datasetReference());
        logicalPlanNodes.add(current.dataset().schema());

        if (current.ifNotExists())
        {
            logicalPlanNodes.add(IfNotExistsTableModifier.INSTANCE);
        }

        // Add Partition Keys
        if (current.dataset().schema().partitionKeys() != null && !current.dataset().schema().partitionKeys().isEmpty())
        {
            logicalPlanNodes.addAll(current.dataset().schema().partitionKeys());
        }

        // Add Clustering Keys
        if (current.dataset().schema().clusterKeys() != null && !current.dataset().schema().clusterKeys().isEmpty())
        {
            logicalPlanNodes.addAll(current.dataset().schema().clusterKeys());
        }

        return new VisitorResult(createTable, logicalPlanNodes);
    }
}
