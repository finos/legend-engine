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
import org.finos.legend.engine.persistence.components.logicalplan.conditions.And;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.LessThan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Not;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Exists;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Delete;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
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
import java.util.Optional;

import static org.finos.legend.engine.persistence.components.common.StatisticName.INCOMING_RECORD_COUNT;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_DELETED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_INSERTED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_TERMINATED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_UPDATED;
import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.ALL_COLUMNS;
import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.getPrimaryKeyMatchCondition;

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
        Dataset stagingDataset = stagingDataset();
        List<Value> fieldsToSelect = new ArrayList<>(stagingDataset().schemaReference().fieldValues());
        List<Value> fieldsToInsert = new ArrayList<>(stagingDataset().schemaReference().fieldValues());
        Optional<Condition> selectCondition = Optional.empty();

        // If data splits is enabled, add the condition to pick only the latest data split
        if (ingestMode().dataSplitField().isPresent())
        {
            String dataSplitField = ingestMode().dataSplitField().get();
            LogicalPlanUtils.removeField(fieldsToSelect, dataSplitField);
            LogicalPlanUtils.removeField(fieldsToInsert, dataSplitField);
            DatasetReference stagingRight = stagingDataset.datasetReference().withAlias("stage_right");
            FieldValue dataSplitLeft = FieldValue.builder()
                .fieldName(dataSplitField)
                .datasetRef(stagingDataset.datasetReference())
                .build();
            FieldValue dataSplitRight = dataSplitLeft.withDatasetRef(stagingRight.datasetReference());
            selectCondition = Optional.of(Not.of(Exists.of(Selection.builder()
                    .source(stagingRight)
                    .condition(And.builder()
                            .addConditions(
                                    LessThan.of(dataSplitLeft, dataSplitRight),
                                    getPrimaryKeyMatchCondition(stagingDataset, stagingRight, primaryKeys.toArray(new String[0])))
                            .build())
                    .addAllFields(ALL_COLUMNS())
                    .build())));
        }
        // If audit is enabled, add audit column to select and insert fields
        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            fieldsToSelect.add(BatchStartTimestamp.INSTANCE);
            String auditField = ingestMode().auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).orElseThrow(IllegalStateException::new);
            fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(auditField).build());
        }
        else if (!ingestMode().dataSplitField().isPresent())
        {
            fieldsToSelect = LogicalPlanUtils.ALL_COLUMNS();
        }

        Selection selectStaging = Selection.builder()
                .source(stagingDataset)
                .addAllFields(fieldsToSelect)
                .condition(selectCondition)
                .build();

        List<Operation> operations = new ArrayList<>();
        // Step 1: Delete all rows from existing table
        operations.add(Delete.builder().dataset(mainDataset()).build());
        // Step 2: Insert new dataset
        operations.add(Insert.of(mainDataset(), selectStaging, fieldsToInsert));

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

    protected void addPostRunStatsForRowsDeleted(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
    }

    @Override
    public boolean dataSplitExecutionSupported()
    {
        return false;
    }

}
