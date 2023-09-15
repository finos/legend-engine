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

import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchEndTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.logicalplan.values.ParseJsonFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.BulkLoadBatchIdValue;
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
        metaSelectFields.add(BulkLoadBatchIdValue.INSTANCE);

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
