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

import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDatasetReference;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.duckdb.logicalplan.datasets.DuckDBStagedFilesDatasetProperties;
import org.finos.legend.engine.persistence.components.relational.duckdb.sql.DuckDBDataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.duckdb.sqldom.schemaops.expressions.table.FileRead;
import org.finos.legend.engine.persistence.components.relational.sql.DataTypeMapping;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.stream.Collectors;


public class StagedFilesDatasetReferenceVisitor implements LogicalPlanVisitor<StagedFilesDatasetReference>
{
    @Override
    public VisitorResult visit(PhysicalPlanNode prev, StagedFilesDatasetReference current, VisitorContext context)
    {
        if (!(current.properties() instanceof DuckDBStagedFilesDatasetProperties))
        {
            throw new IllegalStateException("Only DuckDBStagedFilesDatasetProperties are supported for Duck DB Sink");
        }
        DuckDBStagedFilesDatasetProperties datasetProperties = (DuckDBStagedFilesDatasetProperties) current.properties();

        DataTypeMapping dataTypeMapping = new DuckDBDataTypeMapping();
        FileRead fileRead = new FileRead(FileRead.FileType.valueOf(datasetProperties.fileFormat().name()),
            datasetProperties.filePaths().size() > 0 ? datasetProperties.filePaths() : datasetProperties.filePatterns(),
            current.columns().stream().map(Field::name).collect(Collectors.toList()),
            current.columns().stream().map(field -> dataTypeMapping.getDataType(field.type())).collect(Collectors.toList()),
            datasetProperties.loadOptions());

        prev.push(fileRead);
        return new VisitorResult(null);
    }
}
