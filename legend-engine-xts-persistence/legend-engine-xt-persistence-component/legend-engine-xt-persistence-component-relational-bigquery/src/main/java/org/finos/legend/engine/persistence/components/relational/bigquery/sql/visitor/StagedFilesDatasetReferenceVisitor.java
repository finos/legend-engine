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

import org.finos.legend.engine.persistence.components.common.AvroFileFormatAbstract;
import org.finos.legend.engine.persistence.components.common.CsvFileFormatAbstract;
import org.finos.legend.engine.persistence.components.common.FileFormat;
import org.finos.legend.engine.persistence.components.common.FileFormatVisitor;
import org.finos.legend.engine.persistence.components.common.JsonFileFormatAbstract;
import org.finos.legend.engine.persistence.components.common.ParquetFileFormatAbstract;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDatasetReference;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlanNode;
import org.finos.legend.engine.persistence.components.relational.bigquery.logicalplan.datasets.BigQueryStagedFilesDatasetProperties;
import org.finos.legend.engine.persistence.components.transformer.LogicalPlanVisitor;
import org.finos.legend.engine.persistence.components.transformer.VisitorContext;

import java.util.HashMap;
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

        Map<String, Object> loadOptionsMap = new HashMap<>();
        FileFormat fileFormat = datasetProperties.fileFormat();
        loadOptionsMap.put("format", fileFormat.getFormatName());
        fileFormat.accept(new RetrieveLoadOptions(loadOptionsMap));
        prev.push(loadOptionsMap);

        prev.push(datasetProperties.files());

        return new VisitorResult(null);
    }

    private static class RetrieveLoadOptions implements FileFormatVisitor<Void>
    {
        private Map<String, Object> loadOptionsMap;

        RetrieveLoadOptions(Map<String, Object> loadOptionsMap)
        {
            this.loadOptionsMap = loadOptionsMap;
        }

        @Override
        public Void visitCsvFileFormat(CsvFileFormatAbstract csvFileFormat)
        {
            csvFileFormat.fieldDelimiter().ifPresent(property -> loadOptionsMap.put("field_delimiter", property));
            csvFileFormat.encoding().ifPresent(property -> loadOptionsMap.put("encoding", property));
            csvFileFormat.nullMarker().ifPresent(property -> loadOptionsMap.put("null_marker", property));
            csvFileFormat.quote().ifPresent(property -> loadOptionsMap.put("quote", property));
            csvFileFormat.skipLeadingRows().ifPresent(property -> loadOptionsMap.put("skip_leading_rows", property));
            csvFileFormat.maxBadRecords().ifPresent(property -> loadOptionsMap.put("max_bad_records", property));
            csvFileFormat.compression().ifPresent(property -> loadOptionsMap.put("compression", property));
            return null;
        }

        @Override
        public Void visitJsonFileFormat(JsonFileFormatAbstract jsonFileFormat)
        {
            jsonFileFormat.maxBadRecords().ifPresent(property -> loadOptionsMap.put("max_bad_records", property));
            jsonFileFormat.compression().ifPresent(property -> loadOptionsMap.put("compression", property));
            return null;
        }

        @Override
        public Void visitAvroFileFormat(AvroFileFormatAbstract avroFileFormat)
        {
            return null;
        }

        @Override
        public Void visitParquetFileFormat(ParquetFileFormatAbstract parquetFileFormat)
        {
            return null;
        }
    }
}
