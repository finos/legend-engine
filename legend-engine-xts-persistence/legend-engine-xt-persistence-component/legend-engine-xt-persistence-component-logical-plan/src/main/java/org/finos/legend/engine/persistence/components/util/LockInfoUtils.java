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
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Exists;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Not;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Update;
import org.finos.legend.engine.persistence.components.logicalplan.values.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LockInfoUtils
{
    private final LockInfoDataset lockInfoDataset;
    private final Dataset dataset;

    public LockInfoUtils(LockInfoDataset lockInfoDataset)
    {
        this.lockInfoDataset = lockInfoDataset;
        this.dataset = lockInfoDataset.get();
    }

    public Insert initializeLockInfo(String tableName, BatchStartTimestamp batchStartTimestamp)
    {
        DatasetReference metaTableRef = this.dataset.datasetReference();
        FieldValue insertTimeField = FieldValue.builder().datasetRef(metaTableRef).fieldName(lockInfoDataset.insertTimeField()).build();
        FieldValue tableNameField = FieldValue.builder().datasetRef(metaTableRef).fieldName(lockInfoDataset.tableNameField()).build();
        List<Value> insertFields = Arrays.asList(insertTimeField, tableNameField);
        List<Value> selectFields = Arrays.asList(batchStartTimestamp, StringValue.of(tableName));
        Condition condition = Not.of(Exists.of(Selection.builder().addFields(All.INSTANCE).source(dataset).build()));
        return Insert.of(dataset, Selection.builder().addAllFields(selectFields).condition(condition).build(), insertFields);
    }

    public Update updateLockInfo(BatchStartTimestamp batchStartTimestamp)
    {
        List<Pair<FieldValue, Value>> keyValuePairs = new ArrayList<>();
        keyValuePairs.add(Pair.of(FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName(lockInfoDataset.lastUsedTimeField()).build(), batchStartTimestamp));
        Update update = Update.builder().dataset(dataset).addAllKeyValuePairs(keyValuePairs).build();
        return update;
    }

}
