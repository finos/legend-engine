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
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.JsonExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.snapshot.NontemporalSnapshot;
import static org.finos.legend.engine.testable.persistence.mapper.IngestModeMapper.STAGING_SUFFIX;
import static org.finos.legend.engine.testable.persistence.mapper.IngestModeMapper.isFieldNamePresent;

public class NontemporalSnapshotMapper
{
    public static org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot from(NontemporalSnapshot nontemporalSnapshot)
    {
        return org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot.builder()
                .auditing(nontemporalSnapshot.auditing.accept(MappingVisitors.MAP_TO_COMPONENT_AUDITING))
                .build();
    }

    public static Datasets enrichAndDeriveDatasets(NontemporalSnapshot nontemporalSnapshot, Dataset mainDataset, String testData)
    {
        Auditing auditing = nontemporalSnapshot.auditing.accept(MappingVisitors.MAP_TO_COMPONENT_AUDITING);

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

        JsonExternalDatasetReference stagingDataset = JsonExternalDatasetReference.builder()
                .name(mainDataset.datasetReference().name().get() + STAGING_SUFFIX)
                .database(mainDataset.datasetReference().database())
                .group(mainDataset.datasetReference().group())
                .alias(mainDataset.datasetReference().alias().isPresent() ? mainDataset.datasetReference().alias().get() + STAGING_SUFFIX : null)
                .schema(schemaDefinitionBuilder.build())
                .data(testData)
                .build();

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
