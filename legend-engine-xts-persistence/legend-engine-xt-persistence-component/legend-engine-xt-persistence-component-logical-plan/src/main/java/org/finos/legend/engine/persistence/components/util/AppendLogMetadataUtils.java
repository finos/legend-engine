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

import java.util.ArrayList;
import java.util.List;

public class AppendLogMetadataUtils
{
    private final AppendLogMetadataDataset appendLogMetadataDataset;
    private final Dataset dataset;

    public AppendLogMetadataUtils(AppendLogMetadataDataset appendLogMetadataDataset)
    {
        this.appendLogMetadataDataset = appendLogMetadataDataset;
        this.dataset = appendLogMetadataDataset.get();
    }

    /*
    INSERT INTO batch_metadata ("batchIdField", "tableNameField", "batchStartTimeField", "batchEndTimeField",
     "batchStatusField","batchSourceInfoField")
    (SELECT '<batch_id>','<table_name>','{BATCH_START_TIMESTAMP_PLACEHOLDER}','{BATCH_END_TIMESTAMP_PLACEHOLDER}',
    '<batch_status>','<batch_source_info>');
     */
    public Insert insertMetaData(StringValue batchIdValue, StringValue appendLogTableName,
                                 BatchStartTimestamp batchStartTimestamp, BatchEndTimestamp batchEndTimestamp,
                                 StringValue batchStatusValue, StringValue batchSourceInfoValue)
    {
        DatasetReference metaTableRef = this.dataset.datasetReference();
        FieldValue batchId = FieldValue.builder().datasetRef(metaTableRef).fieldName(appendLogMetadataDataset.batchIdField()).build();
        FieldValue tableName = FieldValue.builder().datasetRef(metaTableRef).fieldName(appendLogMetadataDataset.tableNameField()).build();

        FieldValue batchStartTs = FieldValue.builder().datasetRef(metaTableRef).fieldName(appendLogMetadataDataset.batchStartTimeField()).build();
        FieldValue batchEndTs = FieldValue.builder().datasetRef(metaTableRef).fieldName(appendLogMetadataDataset.batchEndTimeField()).build();

        FieldValue batchStatus = FieldValue.builder().datasetRef(metaTableRef).fieldName(appendLogMetadataDataset.batchStatusField()).build();
        FieldValue batchSourceInfo = FieldValue.builder().datasetRef(metaTableRef).fieldName(appendLogMetadataDataset.batchSourceInfoField()).build();

        List<Value> metaInsertFields = new ArrayList<>();
        List<Value> metaSelectFields = new ArrayList<>();

        metaInsertFields.add(batchId);
        metaSelectFields.add(batchIdValue);

        metaInsertFields.add(tableName);
        metaSelectFields.add(appendLogTableName);

        metaInsertFields.add(batchStartTs);
        metaSelectFields.add(batchStartTimestamp);

        metaInsertFields.add(batchEndTs);
        metaSelectFields.add(batchEndTimestamp);

        metaInsertFields.add(batchStatus);
        metaSelectFields.add(batchStatusValue);

        metaInsertFields.add(batchSourceInfo);
        metaSelectFields.add(ParseJsonFunction.builder().jsonString(batchSourceInfoValue).build());

        return Insert.of(dataset, Selection.builder().addAllFields(metaSelectFields).build(), metaInsertFields);
    }
}
