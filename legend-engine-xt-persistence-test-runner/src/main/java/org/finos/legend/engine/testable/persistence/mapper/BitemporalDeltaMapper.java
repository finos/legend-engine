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
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionMilestoning;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidityMilestoning;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.ValidityDerivation;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.JsonExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.persister.ingestmode.delta.BitemporalDelta;

import static org.finos.legend.engine.testable.persistence.mapper.IngestModeMapper.DIGEST_FIELD_DEFAULT;
import static org.finos.legend.engine.testable.persistence.mapper.IngestModeMapper.STAGING_SUFFIX;
import static org.finos.legend.engine.testable.persistence.mapper.IngestModeMapper.isFieldNamePresent;

public class BitemporalDeltaMapper
{
    public static org.finos.legend.engine.persistence.components.ingestmode.BitemporalDelta from(BitemporalDelta bitemporalDelta)
    {
        return org.finos.legend.engine.persistence.components.ingestmode.BitemporalDelta.builder()
                .digestField(DIGEST_FIELD_DEFAULT)
                .mergeStrategy(bitemporalDelta.mergeStrategy.accept(MappingVisitors.MAP_TO_COMPONENT_MERGE_STRATEGY))
                .transactionMilestoning(bitemporalDelta.transactionMilestoning.accept(MappingVisitors.MAP_TO_COMPONENT_TRANSACTION_MILESTONING))
                .validityMilestoning(bitemporalDelta.validityMilestoning.accept(MappingVisitors.MAP_TO_COMPONENT_VALIDITY_MILESTONING))
                .build();
    }

    public static Datasets enrichAndDeriveDatasets(BitemporalDelta bitemporalDelta, Dataset mainDataset, String testData)
    {
        TransactionMilestoning transactionMilestoning = bitemporalDelta.transactionMilestoning.accept(MappingVisitors.MAP_TO_COMPONENT_TRANSACTION_MILESTONING);
        ValidityMilestoning validityMilestoning = bitemporalDelta.validityMilestoning.accept(MappingVisitors.MAP_TO_COMPONENT_VALIDITY_MILESTONING);
        ValidityDerivation validityMilestoningDerication = validityMilestoning.validityDerivation();
        MergeStrategy mergeStrategy = bitemporalDelta.mergeStrategy.accept(MappingVisitors.MAP_TO_COMPONENT_MERGE_STRATEGY);

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

        // if SourceSpecifiesFromDateTime based validityMilestoningDerivation -> user provided SOURCE_FROM field addition
        if (validityMilestoningDerication instanceof SourceSpecifiesFromDateTime)
        {
            if (!isFieldNamePresent(mainDataset, ((SourceSpecifiesFromDateTime) validityMilestoningDerication).sourceDateTimeFromField()))
            {
                Field sourceDateTimeFrom = Field.builder()
                        .name(((SourceSpecifiesFromDateTime) validityMilestoningDerication).sourceDateTimeFromField())
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(true)
                        .build();
                schemaDefinitionBuilder.addFields(sourceDateTimeFrom);
                stagingSchemaDefinitionBuilder.addFields(sourceDateTimeFrom);
            }
        }

        // if SourceSpecifiesFromDateTime based validityMilestoningDerivation -> user provided SOURCE_FROM SOURCE_THRU fields addition
        if (validityMilestoningDerication instanceof SourceSpecifiesFromAndThruDateTime)
        {
            if (!isFieldNamePresent(mainDataset, ((SourceSpecifiesFromAndThruDateTime) validityMilestoningDerication).sourceDateTimeFromField()))
            {
                Field sourceDateTimeFrom = Field.builder()
                        .name(((SourceSpecifiesFromAndThruDateTime) validityMilestoningDerication).sourceDateTimeFromField())
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(true)
                        .build();
                schemaDefinitionBuilder.addFields(sourceDateTimeFrom);
                stagingSchemaDefinitionBuilder.addFields(sourceDateTimeFrom);
            }
            if (!isFieldNamePresent(mainDataset, ((SourceSpecifiesFromAndThruDateTime) validityMilestoningDerication).sourceDateTimeThruField()))
            {
                Field sourceDateTimeThru = Field.builder()
                        .name(((SourceSpecifiesFromAndThruDateTime) validityMilestoningDerication).sourceDateTimeThruField())
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(false)
                        .build();
                schemaDefinitionBuilder.addFields(sourceDateTimeThru);
                stagingSchemaDefinitionBuilder.addFields(sourceDateTimeThru);
            }
        }

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

        // if BatchId based transactionMilestoning -> user provided BATCH_IN BATCH_OUT fields addition
        if (transactionMilestoning instanceof BatchId)
        {
            if (!isFieldNamePresent(mainDataset, ((BatchId) transactionMilestoning).batchIdInName()))
            {
                Field batchIdIn = Field.builder()
                        .name(((BatchId) transactionMilestoning).batchIdInName())
                        .type(FieldType.of(DataType.INTEGER, Optional.empty(), Optional.empty()))
                        .primaryKey(true)
                        .build();
                schemaDefinitionBuilder.addFields(batchIdIn);
            }
            if (!isFieldNamePresent(mainDataset, ((BatchId) transactionMilestoning).batchIdOutName()))
            {
                Field batchIdOut = Field.builder()
                        .name(((BatchId) transactionMilestoning).batchIdOutName())
                        .type(FieldType.of(DataType.INTEGER, Optional.empty(), Optional.empty()))
                        .primaryKey(false)
                        .build();
                schemaDefinitionBuilder.addFields(batchIdOut);
            }
        }

        // if TransactionDateTime based transactionMilestoning -> user provided IN_Z OUT_Z fields addition
        if (transactionMilestoning instanceof TransactionDateTime)
        {
            if (!isFieldNamePresent(mainDataset, ((TransactionDateTime) transactionMilestoning).dateTimeInName()))
            {
                Field dateTimeIn = Field.builder()
                        .name(((TransactionDateTime) transactionMilestoning).dateTimeInName())
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(true)
                        .build();
                schemaDefinitionBuilder.addFields(dateTimeIn);
            }
            if (!isFieldNamePresent(mainDataset, ((TransactionDateTime) transactionMilestoning).dateTimeOutName()))
            {
                Field dateTimeOut = Field.builder()
                        .name(((TransactionDateTime) transactionMilestoning).dateTimeOutName())
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(false)
                        .build();
                schemaDefinitionBuilder.addFields(dateTimeOut);
            }
        }

        // if TransactionDateTime based transactionMilestoning -> user provided IN_Z OUT_Z fields addition
        if (transactionMilestoning instanceof BatchIdAndDateTime)
        {
            if (!isFieldNamePresent(mainDataset, ((BatchIdAndDateTime) transactionMilestoning).batchIdInName()))
            {
                Field batchIdIn = Field.builder()
                        .name(((BatchIdAndDateTime) transactionMilestoning).batchIdInName())
                        .type(FieldType.of(DataType.INTEGER, Optional.empty(), Optional.empty()))
                        .primaryKey(true)
                        .build();
                schemaDefinitionBuilder.addFields(batchIdIn);
            }
            if (!isFieldNamePresent(mainDataset, ((BatchIdAndDateTime) transactionMilestoning).batchIdOutName()))
            {
                Field batchIdOut = Field.builder()
                        .name(((BatchIdAndDateTime) transactionMilestoning).batchIdOutName())
                        .type(FieldType.of(DataType.INTEGER, Optional.empty(), Optional.empty()))
                        .primaryKey(false)
                        .build();
                schemaDefinitionBuilder.addFields(batchIdOut);
            }
            if (!isFieldNamePresent(mainDataset, ((BatchIdAndDateTime) transactionMilestoning).dateTimeInName()))
            {
                Field dateTimeIn = Field.builder()
                        .name(((BatchIdAndDateTime) transactionMilestoning).dateTimeInName())
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(false)
                        .build();
                schemaDefinitionBuilder.addFields(dateTimeIn);
            }
            if (!isFieldNamePresent(mainDataset, ((BatchIdAndDateTime) transactionMilestoning).dateTimeOutName()))
            {
                Field dateTimeOut = Field.builder()
                        .name(((BatchIdAndDateTime) transactionMilestoning).dateTimeOutName())
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(false)
                        .build();
                schemaDefinitionBuilder.addFields(dateTimeOut);
            }
        }

        // if ValidDateTime based validityMilestoning -> user provided FROM_Z THRU_Z fields addition
        if (validityMilestoning instanceof ValidDateTime)
        {
            if (!isFieldNamePresent(mainDataset, ((ValidDateTime) validityMilestoning).dateTimeFromName()))
            {
                Field dateTimeFrom = Field.builder()
                        .name(((ValidDateTime) validityMilestoning).dateTimeFromName())
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(true)
                        .build();
                schemaDefinitionBuilder.addFields(dateTimeFrom);
            }
            if (!isFieldNamePresent(mainDataset, ((ValidDateTime) validityMilestoning).dateTimeThruName()))
            {
                Field dateTimeThru = Field.builder()
                        .name(((ValidDateTime) validityMilestoning).dateTimeThruName())
                        .type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty()))
                        .primaryKey(false)
                        .build();
                schemaDefinitionBuilder.addFields(dateTimeThru);
            }
        }

        mainDataset = datasetDefinitionBuilder.schema(schemaDefinitionBuilder.build()).build();

        return Datasets.of(mainDataset, stagingDataset);
    }
}
