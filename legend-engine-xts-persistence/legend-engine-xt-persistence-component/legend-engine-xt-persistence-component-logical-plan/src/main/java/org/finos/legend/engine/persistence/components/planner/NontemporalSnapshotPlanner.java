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

package org.finos.legend.engine.persistence.components.planner;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.Resources;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.audit.AuditingVisitors;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Delete;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_DELETED;

class NontemporalSnapshotPlanner extends Planner
{
    NontemporalSnapshotPlanner(Datasets datasets, NontemporalSnapshot ingestMode, PlannerOptions plannerOptions, Set<Capability> capabilities)
    {
        super(datasets, ingestMode, plannerOptions, capabilities);
    }

    @Override
    protected NontemporalSnapshot ingestMode()
    {
        return (NontemporalSnapshot) super.ingestMode();
    }

    @Override
    public LogicalPlan buildLogicalPlanForIngest(Resources resources)
    {
        List<Value> dataFields = getDataFields();
        List<Value> fieldsToSelect = new ArrayList<>(dataFields);
        List<Value> fieldsToInsert = new ArrayList<>(dataFields);

        // If audit is enabled, add audit column to select and insert fields
        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            fieldsToSelect.add(BatchStartTimestamp.INSTANCE);
            String auditField = ingestMode().auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).orElseThrow(IllegalStateException::new);
            fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(auditField).build());
        }

        // Add batch_id field
        fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(ingestMode().batchIdField()).build());
        fieldsToSelect.add(metadataUtils.getBatchId(StringValue.of(mainDataset().datasetReference().name().orElseThrow(IllegalStateException::new))));

        Selection selectStaging = Selection.builder().source(stagingDataset()).addAllFields(fieldsToSelect).build();

        List<Operation> operations = new ArrayList<>();
        // Step 1: Delete all rows from existing table
        operations.add(Delete.builder().dataset(mainDataset()).build());
        // Step 2: Insert new dataset
        operations.add(Insert.of(mainDataset(), selectStaging, fieldsToInsert));

        return LogicalPlan.of(operations);
    }

    @Override
    public Map<StatisticName, LogicalPlan> buildLogicalPlanForPreRunStatistics(Resources resources)
    {
        Map<StatisticName, LogicalPlan> preRunStatisticsResult = new HashMap<>();
        if (options().collectStatistics())
        {
            //Rows Deleted = rows removed(hard-deleted) from sink table)
            preRunStatisticsResult.put(ROWS_DELETED,
                LogicalPlan.builder().addOps(LogicalPlanUtils.getRecordCount(mainDataset(), ROWS_DELETED.get())).build());
        }
        return preRunStatisticsResult;
    }

    protected void addPostRunStatsForRowsDeleted(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
    }

    @Override
    List<String> getDigestOrRemainingColumns()
    {
        List<String> remainingCols = new ArrayList<>();
        if (!primaryKeys.isEmpty())
        {
            remainingCols = getNonPKNonVersionDataFields();
        }
        return remainingCols;
    }

    @Override
    public boolean dataSplitExecutionSupported()
    {
        return false;
    }
}
