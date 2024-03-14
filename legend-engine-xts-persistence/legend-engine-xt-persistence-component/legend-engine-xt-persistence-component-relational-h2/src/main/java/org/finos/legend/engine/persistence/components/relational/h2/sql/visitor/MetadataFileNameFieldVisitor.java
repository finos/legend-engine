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

package org.finos.legend.engine.persistence.components.relational.h2.sql.visitor;

import org.finos.legend.engine.persistence.components.logicalplan.values.MetadataFileNameField;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.visitors.StringValueVisitor;
import org.finos.legend.engine.persistence.components.relational.h2.logicalplan.datasets.H2StagedFilesDatasetProperties;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

public class MetadataFileNameFieldVisitor implements LogicalPlanVisitor<MetadataFileNameField>
{
    @Override
    public VisitorResult visit(PhysicalPlanNode prev, MetadataFileNameField current, VisitorContext context)
    {
        if (!(current.stagedFilesDatasetProperties() instanceof H2StagedFilesDatasetProperties))
        {
            throw new IllegalStateException("Only H2StagedFilesDatasetProperties are supported for H2 Sink");
        }
        H2StagedFilesDatasetProperties datasetProperties = (H2StagedFilesDatasetProperties) current.stagedFilesDatasetProperties();

        StringValue stringValue = StringValue.of(datasetProperties.filePaths().get(0));
        return new StringValueVisitor().visit(prev, stringValue, context);
    }
}
