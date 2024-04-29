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

package org.finos.legend.engine.persistence.components.logicalplan.datasets;

import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class DatasetCaseConverter
{
    public Dataset applyCaseOnDataset(Dataset dataset, Function<String, String> strategy)
    {
        Optional<String> newName = dataset.datasetReference().name().map(strategy);
        Optional<String> newSchemaName = dataset.datasetReference().group().map(strategy);
        Optional<String> newDatabaseName = dataset.datasetReference().database().map(strategy);

        List<Field> newDatasetFields = new ArrayList<>();
        for (Field field : dataset.schema().fields())
        {
            Field newField = field.withName(strategy.apply(field.name()));
            newDatasetFields.add(newField);
        }

        List<Index> newDatasetIndices = new ArrayList<>();
        for (Index index : dataset.schema().indexes())
        {
            List<String> indexColumnNames = new ArrayList<>();
            for (String columnName : index.columns())
            {
                String newColumnName = strategy.apply(columnName);
                indexColumnNames.add(newColumnName);
            }
            Index newIndex = index.withIndexName(strategy.apply(index.indexName())).withColumns(indexColumnNames);
            newDatasetIndices.add(newIndex);
        }

        ColumnStoreSpecification newColumnStoreSpecification = null;
        if (dataset.schema().columnStoreSpecification().isPresent())
        {
            ColumnStoreSpecification columnStoreSpecification = dataset.schema().columnStoreSpecification().get();
            List<Field> newColumnStoreKeys = new ArrayList<>();
            for (Field field : columnStoreSpecification.columnStoreKeys())
            {
                Field newField = field.withName(strategy.apply(field.name()));
                newColumnStoreKeys.add(newField);
            }
            newColumnStoreSpecification = columnStoreSpecification.withColumnStoreKeys(newColumnStoreKeys);
        }

        ShardSpecification newShardSpecification = null;
        if (dataset.schema().shardSpecification().isPresent())
        {
           ShardSpecification shardSpecification = dataset.schema().shardSpecification().get();
            List<Field> newShardKeys = new ArrayList<>();
            for (Field field : shardSpecification.shardKeys())
            {
                Field newField = field.withName(strategy.apply(field.name()));
                newShardKeys.add(newField);
            }
            newShardSpecification = shardSpecification.withShardKeys(newShardKeys);
        }

        SchemaDefinition schemaDefinition = SchemaDefinition.builder()
            .addAllFields(newDatasetFields)
            .addAllIndexes(newDatasetIndices)
            .columnStoreSpecification(newColumnStoreSpecification)
            .shardSpecification(newShardSpecification)
            .build();

        if (dataset instanceof DatasetDefinition)
        {
            DatasetDefinition datasetDefinition = DatasetDefinition.builder()
                    .name(newName.orElseThrow(IllegalStateException::new))
                    .group(newSchemaName)
                    .database(newDatabaseName)
                    .schema(schemaDefinition)
                    .datasetAdditionalProperties(dataset.datasetAdditionalProperties())
                    .build();

            if (dataset.datasetReference().alias().isPresent())
            {
                datasetDefinition = datasetDefinition.withAlias(dataset.datasetReference().alias().get());
            }
            return datasetDefinition;
        }

        if (dataset instanceof DerivedDataset)
        {
            DerivedDataset derivedDataset = DerivedDataset.builder()
                    .name(newName.orElseThrow(IllegalStateException::new))
                    .group(newSchemaName)
                    .database(newDatabaseName)
                    .schema(schemaDefinition)
                    .addAllDatasetFilters(((DerivedDataset) dataset).datasetFilters())
                    .datasetAdditionalProperties(dataset.datasetAdditionalProperties())
                    .build();

            if (dataset.datasetReference().alias().isPresent())
            {
                derivedDataset = derivedDataset.withAlias(dataset.datasetReference().alias().get());
            }
            return derivedDataset;
        }

        if (dataset instanceof FilteredDataset)
        {
            FilteredDataset filteredDataset = FilteredDataset.builder()
                .name(newName.orElseThrow(IllegalStateException::new))
                .group(newSchemaName)
                .database(newDatabaseName)
                .schema(schemaDefinition)
                .filter(((FilteredDataset) dataset).filter())
                .datasetAdditionalProperties(dataset.datasetAdditionalProperties())
                .build();

            if (dataset.datasetReference().alias().isPresent())
            {
                filteredDataset = filteredDataset.withAlias(dataset.datasetReference().alias().get());
            }
            return filteredDataset;
        }

        if (dataset instanceof StagedFilesDataset)
        {
            StagedFilesDataset stagedFilesDataset = StagedFilesDataset.builder()
                    .schema(schemaDefinition)
                    .stagedFilesDatasetProperties(((StagedFilesDataset) dataset).stagedFilesDatasetProperties())
                    .datasetAdditionalProperties(dataset.datasetAdditionalProperties())
                    .build();

            if (dataset.datasetReference().alias().isPresent())
            {
                stagedFilesDataset = stagedFilesDataset.withAlias(dataset.datasetReference().alias().get());
            }
            return stagedFilesDataset;
        }

        throw new UnsupportedOperationException("Unsupported Dataset Conversion");
    }

    public MetadataDataset applyCaseOnMetadataDataset(MetadataDataset metadataDataset, Function<String, String> strategy)
    {
        return MetadataDataset.builder()
                .metadataDatasetDatabaseName(metadataDataset.metadataDatasetDatabaseName().map(strategy))
                .metadataDatasetGroupName(metadataDataset.metadataDatasetGroupName().map(strategy))
                .metadataDatasetName(strategy.apply(metadataDataset.metadataDatasetName()))
                .tableNameField(strategy.apply(metadataDataset.tableNameField()))
                .batchStartTimeField(strategy.apply(metadataDataset.batchStartTimeField()))
                .batchEndTimeField(strategy.apply(metadataDataset.batchEndTimeField()))
                .batchStatusField(strategy.apply(metadataDataset.batchStatusField()))
                .tableBatchIdField(strategy.apply(metadataDataset.tableBatchIdField()))
                .batchSourceInfoField(strategy.apply(metadataDataset.batchSourceInfoField()))
                .additionalMetadataField(strategy.apply(metadataDataset.additionalMetadataField()))
                .build();
    }

    public LockInfoDataset applyCaseOnLockInfoDataset(LockInfoDataset lockInfoDataset, Function<String, String> strategy)
    {
        return LockInfoDataset.builder()
                .database(lockInfoDataset.database().map(strategy))
                .group(lockInfoDataset.group().map(strategy))
                .name(strategy.apply(lockInfoDataset.name()))
                .insertTimeField(strategy.apply(lockInfoDataset.insertTimeField()))
                .lastUsedTimeField(strategy.apply(lockInfoDataset.lastUsedTimeField()))
                .tableNameField(strategy.apply(lockInfoDataset.tableNameField()))
                .build();
    }
}
