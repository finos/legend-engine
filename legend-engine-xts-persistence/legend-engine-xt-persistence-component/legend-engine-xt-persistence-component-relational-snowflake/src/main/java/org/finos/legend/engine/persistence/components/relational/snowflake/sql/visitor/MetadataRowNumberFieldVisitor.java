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

package org.finos.legend.engine.persistence.components.relational.snowflake.sql.visitor;

import org.finos.legend.engine.persistence.components.common.FileFormatType;
import org.finos.legend.engine.persistence.components.logicalplan.values.MetadataRowNumberField;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.snowflake.logicalplan.datasets.SnowflakeStagedFilesDatasetProperties;
import org.finos.legend.engine.persistence.components.relational.snowflake.logicalplan.datasets.StandardFileFormat;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schemaops.values.MetadataRowNumberValue;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

public class MetadataRowNumberFieldVisitor implements LogicalPlanVisitor<MetadataRowNumberField>
{
    @Override
    public VisitorResult visit(PhysicalPlanNode prev, MetadataRowNumberField current, VisitorContext context)
    {
        if (!(current.stagedFilesDatasetProperties() instanceof SnowflakeStagedFilesDatasetProperties))
        {
            throw new IllegalStateException("Only SnowflakeStagedFilesDatasetProperties are supported for Snowflake Sink");
        }
        SnowflakeStagedFilesDatasetProperties datasetProperties = (SnowflakeStagedFilesDatasetProperties) current.stagedFilesDatasetProperties();

        int startingRowNumber = 1;
        if (datasetProperties.fileFormat().isPresent() && datasetProperties.fileFormat().get() instanceof StandardFileFormat)
        {
            StandardFileFormat standardFileFormat = (StandardFileFormat) datasetProperties.fileFormat().get();
            startingRowNumber = standardFileFormat.formatType().equals(FileFormatType.AVRO) ? 0 : 1;
        }
        MetadataRowNumberValue rowNumberColumn = new MetadataRowNumberValue(context.quoteIdentifier(), startingRowNumber);
        prev.push(rowNumberColumn);
        return new VisitorResult(null);
    }
}
