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

import org.finos.legend.engine.persistence.components.common.FileFormatType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDatasetReference;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.bigquery.logicalplan.datasets.BigQueryStagedFilesDatasetProperties;
import org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.schemaops.expressions.table.StagedFilesTable;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class StagedFilesDatasetReferenceVisitor implements LogicalPlanVisitor<StagedFilesDatasetReference>
{
    @Override
    public VisitorResult visit(PhysicalPlanNode prev, StagedFilesDatasetReference current, VisitorContext context)
    {
        if (!(current.properties() instanceof BigQueryStagedFilesDatasetProperties))
        {
            throw new IllegalStateException("Only BigQueryStagedFilesDatasetProperties are supported for BigQuery Sink");
        }
        BigQueryStagedFilesDatasetProperties datasetProperties = (BigQueryStagedFilesDatasetProperties) current.properties();

        FileFormatType fileFormatType = datasetProperties.fileFormat();
        Map<String, Object> loadOptionsMap = new HashMap<>(datasetProperties.loadOptions());
        loadOptionsMap.put("format", fileFormatType.name());
        List<String> uris = datasetProperties.filePaths().isEmpty() ? datasetProperties.filePatterns() : datasetProperties.filePaths();
        StagedFilesTable stagedFilesTable = new StagedFilesTable(uris, loadOptionsMap);
        prev.push(stagedFilesTable);

        return new VisitorResult(null);
    }
}
