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
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Delete;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.finos.legend.engine.persistence.components.common.StatisticName.INCOMING_RECORD_COUNT;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_DELETED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_INSERTED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_TERMINATED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_UPDATED;

class NontemporalSnapshotPlanner extends Planner
{
    NontemporalSnapshotPlanner(Datasets datasets, NontemporalSnapshot ingestMode, PlannerOptions plannerOptions)
    {
        super(datasets, ingestMode, plannerOptions);
    }

    @Override
    protected NontemporalSnapshot ingestMode()
    {
        return (NontemporalSnapshot) super.ingestMode();
    }

    @Override
    public LogicalPlan buildLogicalPlanForIngest(Resources resources, Set<Capability> capabilities)
    {
        Selection selectStaging = Selection.builder()
            .source(stagingDataset())
            .addAllFields(LogicalPlanUtils.ALL_COLUMNS())
            .build();

        List<Value> stagingFields = new ArrayList<>(stagingDataset().schemaReference().fieldValues());

        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            List<Value> selectFields = new ArrayList<>(stagingDataset().schemaReference().fieldValues());
            selectFields.add(BatchStartTimestamp.INSTANCE);
            selectStaging = selectStaging.withFields(selectFields);

            String auditField = ingestMode().auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).orElseThrow(IllegalStateException::new);
            stagingFields.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(auditField).build());
        }

        List<Operation> operations = new ArrayList<>();
        // Step 1: Delete all rows from existing table
        operations.add(Delete.builder().dataset(mainDataset()).build());
        // Step 2: Insert new dataset
        operations.add(Insert.of(mainDataset(), selectStaging, stagingFields));

        return LogicalPlan.of(operations);
    }

    @Override
    public LogicalPlan buildLogicalPlanForPreActions(Resources resources)
    {
        return LogicalPlan.builder().addOps(Create.of(true, mainDataset())).build();
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

    @Override
    public Map<StatisticName, LogicalPlan> buildLogicalPlanForPostRunStatistics(Resources resources)
    {
        Map<StatisticName, LogicalPlan> postRunStatisticsResult = new HashMap<>();

        if (options().collectStatistics())
        {
            //Incoming dataset record count
            postRunStatisticsResult.put(
                INCOMING_RECORD_COUNT,
                LogicalPlan.builder().addOps(LogicalPlanUtils.getRecordCount(stagingDataset(), INCOMING_RECORD_COUNT.get())).build());

            //Rows terminated = Rows invalidated in Sink - Rows updated
            postRunStatisticsResult.put(
                ROWS_TERMINATED,
                LogicalPlanFactory.getLogicalPlanForConstantStats(ROWS_TERMINATED.get(), 0L));

            //Rows inserted (no previous active row with same primary key) = Rows added in sink - rows updated
            postRunStatisticsResult.put(
                ROWS_INSERTED,
                LogicalPlan.builder().addOps(LogicalPlanUtils.getRecordCount(mainDataset(), ROWS_INSERTED.get())).build());

            //Rows updated (when it is invalidated and a new row for same primary keys is added)
            postRunStatisticsResult.put(
                ROWS_UPDATED,
                LogicalPlanFactory.getLogicalPlanForConstantStats(ROWS_UPDATED.get(), 0L));
        }

        return postRunStatisticsResult;
    }
}
