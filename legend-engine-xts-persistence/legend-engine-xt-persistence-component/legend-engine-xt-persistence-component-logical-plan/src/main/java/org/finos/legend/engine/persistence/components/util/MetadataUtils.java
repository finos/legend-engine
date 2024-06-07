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

package org.finos.legend.engine.persistence.components.util;

import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchEndTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchIdValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.DiffBinaryValueOperator;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.ParseJsonFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.logicalplan.values.NumericalValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.SumBinaryValueOperator;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.And;
import org.finos.legend.engine.persistence.components.logicalplan.values.SelectValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MetadataUtils
{
    public enum MetaTableStatus
    {
        INITIALIZED,
        DONE
    }

    public static final String BATCH_SOURCE_INFO_FILE_PATHS = "file_paths";
    public static final String BATCH_SOURCE_INFO_FILE_PATTERNS = "file_patterns";
    public static final String BATCH_SOURCE_INFO_STAGING_FILTERS = "staging_filters";
    public static final String BATCH_STATISTICS_PATTERN = "{BATCH_STATISTICS_PLACEHOLDER}";

    private final MetadataDataset dataset;
    private final Dataset metaDataset;

    public MetadataUtils(MetadataDataset dataset)
    {
        this.dataset = dataset;
        this.metaDataset = dataset.get();
    }

    /*
    (SELECT COALESCE(MAX("table_batch_id"),0)+1 FROM batch_metadata WHERE "table_name" = mainTableName)-1";
    */
    public Value getPrevBatchId(StringValue mainTableName)
    {
        BatchIdValue maxBatchId = getBatchId(mainTableName);
        return DiffBinaryValueOperator.of(maxBatchId, NumericalValue.of(1L));
    }

    /*
    SELECT COALESCE(MAX("table_batch_id"),0)+1 FROM batch_metadata WHERE "table_name" = mainTableName
    */
    public BatchIdValue getBatchId(StringValue mainTableName)
    {
        FieldValue tableNameFieldValue = FieldValue.builder().datasetRef(metaDataset.datasetReference()).fieldName(dataset.tableNameField()).build();
        FunctionImpl tableNameInUpperCase = FunctionImpl.builder().functionName(FunctionName.UPPER).addValue(tableNameFieldValue).build();
        StringValue mainTableNameInUpperCase = StringValue.builder().value(mainTableName.value().map(field -> field.toUpperCase()))
                .alias(mainTableName.alias()).build();
        Condition whereCondition = Equals.of(tableNameInUpperCase, mainTableNameInUpperCase);
        FieldValue tableBatchIdFieldValue = FieldValue.builder().datasetRef(metaDataset.datasetReference()).fieldName(dataset.tableBatchIdField()).build();
        FunctionImpl maxBatchId = FunctionImpl.builder().functionName(FunctionName.MAX).addValue(tableBatchIdFieldValue).build();
        FunctionImpl coalesce = FunctionImpl.builder().functionName(FunctionName.COALESCE).addValue(maxBatchId, NumericalValue.of(0L)).build();

        return BatchIdValue.of(Selection.builder()
            .source(metaDataset)
            .condition(whereCondition)
            .addFields(SumBinaryValueOperator.of(coalesce, NumericalValue.of(1L)))
            .build());
    }

    public Insert insertMetaData(StringValue mainTableName, BatchStartTimestamp batchStartTimestamp, BatchEndTimestamp batchEndTimestamp, Value batchStatus)
    {
        return insertMetaData(mainTableName, batchStartTimestamp, batchEndTimestamp, batchStatus, Optional.empty(), Optional.empty(), Optional.empty(), false);
    }

    /*
    INSERT INTO batch_metadata ("table_name", "table_batch_id", "batch_start_ts_utc", "batch_end_ts_utc", "batch_status")
    (SELECT 'main',
    (SELECT COALESCE(MAX("table_batch_id"),0)+1 FROM batch_metadata WHERE "table_name" = 'main'),
    '{BATCH_START_TIMESTAMP_PLACEHOLDER}',
    '{BATCH_END_TIMESTAMP_PLACEHOLDER}',
    'DONE');
     */
    public Insert insertMetaData(StringValue mainTableName, BatchStartTimestamp batchStartTimestamp, BatchEndTimestamp batchEndTimestamp, Value batchStatusValue, Optional<String> ingestRequestId, Optional<StringValue> batchSourceInfoValue, Optional<StringValue> additionalMetadataValue, boolean writeStatistics)
    {
        DatasetReference metaTableRef = this.metaDataset.datasetReference();
        FieldValue tableName = FieldValue.builder().datasetRef(metaTableRef).fieldName(dataset.tableNameField()).build();
        FieldValue batchId = FieldValue.builder().datasetRef(metaTableRef).fieldName(dataset.tableBatchIdField()).build();
        FieldValue startTs = FieldValue.builder().datasetRef(metaTableRef).fieldName(dataset.batchStartTimeField()).build();
        FieldValue endTs = FieldValue.builder().datasetRef(metaTableRef).fieldName(dataset.batchEndTimeField()).build();
        FieldValue batchStatus = FieldValue.builder().datasetRef(metaTableRef).fieldName(dataset.batchStatusField()).build();

        List<Value> metaInsertFields = new ArrayList<>();
        metaInsertFields.add(tableName);
        metaInsertFields.add(batchId);
        metaInsertFields.add(startTs);
        metaInsertFields.add(endTs);
        metaInsertFields.add(batchStatus);
        if (ingestRequestId.isPresent())
        {
            FieldValue ingestRequestIdField = FieldValue.builder().datasetRef(metaTableRef).fieldName(dataset.ingestRequestIdField()).build();
            metaInsertFields.add(ingestRequestIdField);
        }
        if (batchSourceInfoValue.isPresent())
        {
            FieldValue batchSourceInfo = FieldValue.builder().datasetRef(metaTableRef).fieldName(dataset.batchSourceInfoField()).build();
            metaInsertFields.add(batchSourceInfo);
        }
        if (additionalMetadataValue.isPresent())
        {
            FieldValue additionalMetadata = FieldValue.builder().datasetRef(metaTableRef).fieldName(dataset.additionalMetadataField()).build();
            metaInsertFields.add(additionalMetadata);
        }
        if (writeStatistics)
        {
            FieldValue batchStatistics = FieldValue.builder().datasetRef(metaTableRef).fieldName(dataset.batchStatisticsField()).build();
            metaInsertFields.add(batchStatistics);
        }

        List<Value> metaSelectFields = new ArrayList<>();
        metaSelectFields.add(mainTableName);
        metaSelectFields.add(getBatchId(mainTableName));
        metaSelectFields.add(batchStartTimestamp);
        metaSelectFields.add(batchEndTimestamp);
        metaSelectFields.add(batchStatusValue);
        if (ingestRequestId.isPresent())
        {
            metaSelectFields.add(StringValue.of(ingestRequestId.get()));
        }
        if (batchSourceInfoValue.isPresent())
        {
            ParseJsonFunction batchSourceInfoJson = ParseJsonFunction.builder().jsonString(batchSourceInfoValue.get()).build();
            metaSelectFields.add(batchSourceInfoJson);
        }
        if (additionalMetadataValue.isPresent())
        {
            ParseJsonFunction additionalMetadataInfoJson = ParseJsonFunction.builder().jsonString(additionalMetadataValue.get()).build();
            metaSelectFields.add(additionalMetadataInfoJson);
        }
        if (writeStatistics)
        {
            ParseJsonFunction batchStatisticsJson = ParseJsonFunction.builder().jsonString(StringValue.of(BATCH_STATISTICS_PATTERN)).build();
            metaSelectFields.add(batchStatisticsJson);
        }

        return Insert.of(metaDataset, Selection.builder().addAllFields(metaSelectFields).build(), metaInsertFields);
    }


    /*
    SELECT STAGING_FILTERS FROM <batch_metadata> WHERE
        TABLE_NAME = <mainTableName> AND
        TABLE_BATCH_ID = (SELECT MAX(TABLE_BATCH_ID) from <batch_metadata> where TABLE_NAME = <mainTableName>)
    LIMIT 1
    */
    public Selection getLatestStagingFilters(StringValue mainTableName)
    {
        FieldValue stagingFiltersField = FieldValue.builder().datasetRef(metaDataset.datasetReference()).fieldName(dataset.batchSourceInfoField()).build();
        FieldValue tableNameField = FieldValue.builder().datasetRef(metaDataset.datasetReference()).fieldName(dataset.tableNameField()).build();
        FunctionImpl tableNameInUpperCase = FunctionImpl.builder().functionName(FunctionName.UPPER).addValue(tableNameField).build();
        StringValue mainTableNameInUpperCase = StringValue.builder().value(mainTableName.value().map(field -> field.toUpperCase()))
                .alias(mainTableName.alias()).build();

        Condition tableNameEqualsCondition = Equals.of(tableNameInUpperCase, mainTableNameInUpperCase);

        FieldValue tableBatchIdField = FieldValue.builder().datasetRef(metaDataset.datasetReference()).fieldName(dataset.tableBatchIdField()).build();
        FunctionImpl maxBatchId = FunctionImpl.builder().functionName(FunctionName.MAX).addValue(tableBatchIdField).build();
        SelectValue maxBatchIdValue = SelectValue.of(Selection.builder().source(metaDataset.datasetReference()).condition(tableNameEqualsCondition).addFields(maxBatchId).build());
        Condition maxBatchIdCondition = Equals.of(tableBatchIdField, maxBatchIdValue);

        List<Condition> conditions = new ArrayList<>();
        conditions.add(tableNameEqualsCondition);
        conditions.add(maxBatchIdCondition);

        return Selection.builder()
                .source(metaDataset)
                .condition(And.of(conditions))
                .addFields(stagingFiltersField)
                .limit(1)
                .build();
    }
}
