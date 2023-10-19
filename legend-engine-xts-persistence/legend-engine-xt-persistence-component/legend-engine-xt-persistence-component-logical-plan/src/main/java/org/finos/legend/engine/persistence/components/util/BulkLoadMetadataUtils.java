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

package org.finos.legend.engine.persistence.components.util;

import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchIdValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.logicalplan.values.NumericalValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchEndTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.SumBinaryValueOperator;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.logicalplan.values.ParseJsonFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.BulkLoadBatchStatusValue;

import java.util.ArrayList;
import java.util.List;

public class BulkLoadMetadataUtils
{
    private final BulkLoadMetadataDataset bulkLoadMetadataDataset;
    private final Dataset dataset;

    public BulkLoadMetadataUtils(BulkLoadMetadataDataset bulkLoadMetadataDataset)
    {
        this.bulkLoadMetadataDataset = bulkLoadMetadataDataset;
        this.dataset = bulkLoadMetadataDataset.get();
    }

    /*
    SELECT COALESCE(MAX("table_batch_id"),0)+1 FROM batch_metadata WHERE "table_name" = mainTableName
    */
    public BatchIdValue getBatchId(StringValue mainTableName)
    {
        FieldValue tableNameFieldValue = FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName(bulkLoadMetadataDataset.tableNameField()).build();
        FunctionImpl tableNameInUpperCase = FunctionImpl.builder().functionName(FunctionName.UPPER).addValue(tableNameFieldValue).build();
        StringValue mainTableNameInUpperCase = StringValue.builder().value(mainTableName.value().map(field -> field.toUpperCase()))
            .alias(mainTableName.alias()).build();
        Condition whereCondition = Equals.of(tableNameInUpperCase, mainTableNameInUpperCase);
        FieldValue tableBatchIdFieldValue = FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName(bulkLoadMetadataDataset.batchIdField()).build();
        FunctionImpl maxBatchId = FunctionImpl.builder().functionName(FunctionName.MAX).addValue(tableBatchIdFieldValue).build();
        FunctionImpl coalesce = FunctionImpl.builder().functionName(FunctionName.COALESCE).addValue(maxBatchId, NumericalValue.of(0L)).build();

        return BatchIdValue.of(Selection.builder()
            .source(dataset)
            .condition(whereCondition)
            .addFields(SumBinaryValueOperator.of(coalesce, NumericalValue.of(1L)))
            .build());
    }

    /*
    INSERT INTO batch_metadata ("batchIdField", "tableNameField", "batchStartTimeField", "batchEndTimeField",
     "batchStatusField","batchSourceInfoField")
    (SELECT '<batch_id>','<table_name>','{BATCH_START_TIMESTAMP_PLACEHOLDER}','{BATCH_END_TIMESTAMP_PLACEHOLDER}',
    '<batch_status>','<batch_source_info>');
     */
    public Insert insertMetaData(StringValue tableNameValue, StringValue batchSourceInfoValue)
    {
        DatasetReference metaTableRef = this.dataset.datasetReference();
        FieldValue batchId = FieldValue.builder().datasetRef(metaTableRef).fieldName(bulkLoadMetadataDataset.batchIdField()).build();
        FieldValue tableName = FieldValue.builder().datasetRef(metaTableRef).fieldName(bulkLoadMetadataDataset.tableNameField()).build();

        FieldValue batchStartTs = FieldValue.builder().datasetRef(metaTableRef).fieldName(bulkLoadMetadataDataset.batchStartTimeField()).build();
        FieldValue batchEndTs = FieldValue.builder().datasetRef(metaTableRef).fieldName(bulkLoadMetadataDataset.batchEndTimeField()).build();

        FieldValue batchStatus = FieldValue.builder().datasetRef(metaTableRef).fieldName(bulkLoadMetadataDataset.batchStatusField()).build();
        FieldValue batchSourceInfo = FieldValue.builder().datasetRef(metaTableRef).fieldName(bulkLoadMetadataDataset.batchSourceInfoField()).build();

        List<Value> metaInsertFields = new ArrayList<>();
        List<Value> metaSelectFields = new ArrayList<>();

        metaInsertFields.add(batchId);
        metaSelectFields.add(getBatchId(tableNameValue));

        metaInsertFields.add(tableName);
        metaSelectFields.add(tableNameValue);

        metaInsertFields.add(batchStartTs);
        metaSelectFields.add(BatchStartTimestamp.INSTANCE);

        metaInsertFields.add(batchEndTs);
        metaSelectFields.add(BatchEndTimestamp.INSTANCE);

        metaInsertFields.add(batchStatus);
        metaSelectFields.add(BulkLoadBatchStatusValue.INSTANCE);

        metaInsertFields.add(batchSourceInfo);
        metaSelectFields.add(ParseJsonFunction.builder().jsonString(batchSourceInfoValue).build());

        return Insert.of(dataset, Selection.builder().addAllFields(metaSelectFields).build(), metaInsertFields);
    }
}
