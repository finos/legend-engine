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
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.logicalplan.values.NumericalValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.SumBinaryValueOperator;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;

import java.util.ArrayList;
import java.util.List;

public class MetadataUtils
{
    public enum MetaTableStatus
    {
        INITIALIZED,
        DONE
    }

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
        Condition whereCondition = Equals.of(tableNameFieldValue, mainTableName);

        FieldValue tableBatchIdFieldValue = FieldValue.builder().datasetRef(metaDataset.datasetReference()).fieldName(dataset.tableBatchIdField()).build();
        FunctionImpl maxBatchId = FunctionImpl.builder().functionName(FunctionName.MAX).addValue(tableBatchIdFieldValue).build();
        FunctionImpl coalesce = FunctionImpl.builder().functionName(FunctionName.COALESCE).addValue(maxBatchId, NumericalValue.of(0L)).build();

        return BatchIdValue.of(Selection.builder()
            .source(metaDataset)
            .condition(whereCondition)
            .addFields(SumBinaryValueOperator.of(coalesce, NumericalValue.of(1L)))
            .build());
    }

    /*
    INSERT INTO batch_metadata ("table_name", "table_batch_id", "batch_start_ts_utc", "batch_end_ts_utc", "batch_status")
    (SELECT 'main',
    (SELECT COALESCE(MAX("table_batch_id"),0)+1 FROM batch_metadata WHERE "table_name" = 'main'),
    '{BATCH_START_TIMESTAMP_PLACEHOLDER}',
    '{BATCH_END_TIMESTAMP_PLACEHOLDER}',
    'DONE');
     */
    public Insert insertMetaData(StringValue mainTableName, BatchStartTimestamp batchStartTimestamp, BatchEndTimestamp batchEndTimestamp)
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

        StringValue status = StringValue.of(MetaTableStatus.DONE.toString());
        List<Value> metaSelectFields = new ArrayList<>();
        metaSelectFields.add(mainTableName);
        metaSelectFields.add(getBatchId(mainTableName));
        metaSelectFields.add(batchStartTimestamp);
        metaSelectFields.add(batchEndTimestamp);
        metaSelectFields.add(status);
        return Insert.of(metaDataset, Selection.builder().addAllFields(metaSelectFields).build(), metaInsertFields);
    }
}
