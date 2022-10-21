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

package org.finos.legend.engine.testable.persistence.mapper;

import java.util.Optional;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.audit.Auditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.JsonExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.NontemporalDelta;

import static org.finos.legend.engine.testable.persistence.mapper.IngestModeMapper.DIGEST_FIELD_DEFAULT;
import static org.finos.legend.engine.testable.persistence.mapper.IngestModeMapper.STAGING_SUFFIX;
import static org.finos.legend.engine.testable.persistence.mapper.IngestModeMapper.isFieldNamePresent;

public class NontemporalDeltaMapper
{
    public static org.finos.legend.engine.persistence.components.ingestmode.NontemporalDelta from(NontemporalDelta nontemporalDelta)
    {
        return org.finos.legend.engine.persistence.components.ingestmode.NontemporalDelta.builder()
                .digestField(DIGEST_FIELD_DEFAULT)
                .auditing(nontemporalDelta.auditing.accept(MappingVisitors.MAP_TO_COMPONENT_AUDITING))
                .mergeStrategy(nontemporalDelta.mergeStrategy.accept(MappingVisitors.MAP_TO_COMPONENT_MERGE_STRATEGY))
                .build();
    }

    public static Datasets enrichAndDeriveDatasets(NontemporalDelta nontemporalDelta, Dataset mainDataset, String testData)
    {
        Auditing auditing = nontemporalDelta.auditing.accept(MappingVisitors.MAP_TO_COMPONENT_AUDITING);
        MergeStrategy mergeStrategy = nontemporalDelta.mergeStrategy.accept(MappingVisitors.MAP_TO_COMPONENT_MERGE_STRATEGY);

        DatasetDefinition.Builder datasetDefinitionBuilder = DatasetDefinition.builder()
                .name(mainDataset.datasetReference().name().get())
                .database(mainDataset.datasetReference().database())
                .group(mainDataset.datasetReference().group())
                .alias(mainDataset.datasetReference().alias().orElse(null));

        SchemaDefinition.Builder schemaDefinitionBuilder = SchemaDefinition.builder()
                .addAllFields(mainDataset.schema().fields())
                .addAllIndexes(mainDataset.schema().indexes())
                .shardSpecification(mainDataset.schema().shardSpecification())
                .columnStoreSpecification(mainDataset.schema().columnStoreSpecification());

        SchemaDefinition.Builder stagingSchemaDefinitionBuilder = SchemaDefinition.builder()
                .addAllFields(mainDataset.schema().fields())
                .addAllIndexes(mainDataset.schema().indexes())
                .shardSpecification(mainDataset.schema().shardSpecification())
                .columnStoreSpecification(mainDataset.schema().columnStoreSpecification());

        // if DeleteIndicatorMergeStrategy -> user provided DELETED field addition
        if (mergeStrategy instanceof DeleteIndicatorMergeStrategy)
        {
            if (!isFieldNamePresent(mainDataset, ((DeleteIndicatorMergeStrategy) mergeStrategy).deleteField()))
            {
                Field deleted = Field.builder()
                        .name(((DeleteIndicatorMergeStrategy) mergeStrategy).deleteField())
                        .type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty()))
                        .defaultValue(((DeleteIndicatorMergeStrategy) mergeStrategy).deleteValues())
                        .build();
                stagingSchemaDefinitionBuilder.addFields(deleted);
            }
        }

        JsonExternalDatasetReference stagingDataset = JsonExternalDatasetReference.builder()
                .name(mainDataset.datasetReference().name().get() + STAGING_SUFFIX)
                .database(mainDataset.datasetReference().database())
                .group(mainDataset.datasetReference().group())
                .alias(mainDataset.datasetReference().alias().isPresent() ? mainDataset.datasetReference().alias().get() + STAGING_SUFFIX : null)
                .schema(stagingSchemaDefinitionBuilder.build())
                .data(testData)
                .build();

        // DIGEST field addition
        if (!isFieldNamePresent(mainDataset, DIGEST_FIELD_DEFAULT))
        {
            Field digest = Field.builder()
                    .name(DIGEST_FIELD_DEFAULT)
                    .type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty()))
                    .build();
            schemaDefinitionBuilder.addFields(digest);
        }

        // if DateTimeAuditing -> user provided BATCH_TIME_IN field addition
        if (auditing instanceof DateTimeAuditing)
        {
            if (!isFieldNamePresent(mainDataset, ((DateTimeAuditing) auditing).dateTimeField()))
            {
                Field batchTimeIn = Field.builder()
                        .name(((DateTimeAuditing) auditing).dateTimeField())
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .build();
                schemaDefinitionBuilder.addFields(batchTimeIn);
            }
        }

        mainDataset = datasetDefinitionBuilder.schema(schemaDefinitionBuilder.build()).build();

        return Datasets.of(mainDataset, stagingDataset);
    }
}
