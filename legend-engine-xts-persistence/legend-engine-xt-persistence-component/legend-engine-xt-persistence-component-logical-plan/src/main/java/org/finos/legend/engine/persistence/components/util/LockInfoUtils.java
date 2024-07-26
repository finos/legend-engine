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

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
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

    public Insert initializeLockInfo(BatchStartTimestamp batchStartTimestamp)
    {
        DatasetReference metaTableRef = this.dataset.datasetReference();
        FieldValue insertTimeField = FieldValue.builder().datasetRef(metaTableRef).fieldName(lockInfoDataset.insertTimeField()).build();
        List<Value> insertFields = Arrays.asList(insertTimeField);
        List<Value> selectFields = Arrays.asList(batchStartTimestamp);
        Condition condition = Not.of(Exists.of(Selection.builder().addFields(All.INSTANCE).source(dataset).build()));
        return Insert.of(dataset, Selection.builder().addAllFields(selectFields).condition(condition).build(), insertFields);
    }

    public Insert initializeLockInfoForMultiIngest(BatchStartTimestamp batchStartTimestamp)
    {
        DatasetReference metaTableRef = this.dataset.datasetReference();
        FieldValue insertTimeField = FieldValue.builder().datasetRef(metaTableRef).fieldName(lockInfoDataset.insertTimeField()).build();
        FieldValue batchIdField = FieldValue.builder().datasetRef(metaTableRef).fieldName(lockInfoDataset.batchIdField()).build();
        List<Value> insertFields = Arrays.asList(insertTimeField, batchIdField);
        List<Value> selectFields = Arrays.asList(batchStartTimestamp, NumericalValue.of(0L));
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

    public Update updateLockInfoForMultiIngest(long batchId, BatchStartTimestamp batchStartTimestamp)
    {
        List<Pair<FieldValue, Value>> keyValuePairs = new ArrayList<>();
        FieldValue batchIdField = FieldValue.builder().datasetRef(this.dataset.datasetReference()).fieldName(lockInfoDataset.batchIdField()).build();
        FunctionImpl coalesce = FunctionImpl.builder().functionName(FunctionName.COALESCE).addValue(batchIdField, NumericalValue.of(batchId)).build();
        SelectValue batchIdValue = SelectValue.of(Selection.builder().source(dataset).addFields(SumBinaryValueOperator.of(coalesce, NumericalValue.of(1L))).build());
        keyValuePairs.add(Pair.of(FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName(lockInfoDataset.lastUsedTimeField()).build(), batchStartTimestamp));
        keyValuePairs.add(Pair.of(FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName(lockInfoDataset.batchIdField()).build(), batchIdValue));
        Update update = Update.builder().dataset(dataset).addAllKeyValuePairs(keyValuePairs).build();
        return update;
    }

    public LogicalPlan getLogicalPlanForBatchIdValue()
    {
        FieldValue batchIdField = FieldValue.builder().datasetRef(this.dataset.datasetReference()).fieldName(lockInfoDataset.batchIdField()).build();
        Selection selection = Selection.builder().source(dataset).addFields(batchIdField).build();
        return LogicalPlan.builder().addOps(selection).build();
    }

}
