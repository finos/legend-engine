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
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.DeleteTargetDataAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.EmptyDatasetHandlingVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.FailEmptyBatchAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.NoOpAbstract;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.And;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Exists;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.In;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Not;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Update;
import org.finos.legend.engine.persistence.components.logicalplan.operations.UpdateAbstract;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Pair;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class UnitemporalSnapshotPlanner extends UnitemporalPlanner
{
    UnitemporalSnapshotPlanner(Datasets datasets, UnitemporalSnapshot ingestMode, PlannerOptions plannerOptions, Set<Capability> capabilities)
    {
        super(datasets, ingestMode, plannerOptions, capabilities);

        // validate
        if (ingestMode.partitioned())
        {
            List<String> fieldNames = stagingDataset().schema().fields().stream().map(Field::name).collect(Collectors.toList());
            // All partitionFields must be present in staging dataset
            ingestMode.partitionFields().forEach(field -> validateExistence(
                    fieldNames,
                    field,
                    "Field [" + field + "] from partitionFields not present in incoming dataset"));
        }
    }

    @Override
    protected UnitemporalSnapshot ingestMode()
    {
        return (UnitemporalSnapshot) super.ingestMode();
    }

    @Override
    public LogicalPlan buildLogicalPlanForIngest(Resources resources)
    {
        List<Pair<FieldValue, Value>> keyValuePairs = keyValuesForMilestoningUpdate();

        if (resources.stagingDataSetEmpty())
        {
            // Empty Dataset handling
            return ingestMode().emptyDatasetHandling().accept(new EmptyDatasetHandler(keyValuePairs));
        }
        else
        {
            List<Operation> operations = new ArrayList<>();
            // Step 1: Milestone Records in main table
            operations.add(getSqlToMilestoneRows(keyValuePairs));
            // Step 2: Insert records in main table
            operations.add(sqlToUpsertRows());
            return LogicalPlan.of(operations);
        }
    }

    @Override
    public LogicalPlan buildLogicalPlanForPreActions(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();
        operations.add(Create.of(true, mainDataset()));
        if (options().createStagingDataset())
        {
            operations.add(Create.of(true, stagingDataset()));
        }
        operations.add(Create.of(true, metadataDataset().orElseThrow(IllegalStateException::new).get()));
        if (options().enableConcurrentSafety())
        {
            operations.add(Create.of(true, lockInfoDataset().orElseThrow(IllegalStateException::new).get()));
        }
        return LogicalPlan.of(operations);
    }

    /*
    insert into main_table
    (
       select
            {TABLE_BATCH_ID} as "batch_id_in",
            999999999 as "batch_id_out",
            {BATCH_TIME} as "batch_time_in_utc",
            '9999-12-31 23:59:59' as "batch_time_out_utc",
            (
            select fields from stage
            )
        from
            stage
        where
            stage."digest" not in
            (
                select digest from main_table
                where sink."batch_id_out" = 999999999
                  [ and {MILESTONING_PARTITION_COLUMN_EQUALITY} ]
            )
     )
     */
    protected Insert sqlToUpsertRows()
    {
        List<Condition> whereClauseForNotInSink = new ArrayList<>((Arrays.asList(openRecordCondition)));

        if (ingestMode().partitioned())
        {
            // if partitionValuesByField provided, add inCondition
            if (!ingestMode().partitionValuesByField().isEmpty())
            {
                whereClauseForNotInSink.add(LogicalPlanUtils.getPartitionColumnValueMatchInCondition(mainDataset(), ingestMode().partitionValuesByField()));
            }
            else
            {
                whereClauseForNotInSink.add(LogicalPlanUtils.getPartitionColumnsMatchCondition(mainDataset(), stagingDataset(), ingestMode().partitionFields().toArray(new String[0])));
            }
        }

        Condition notInSinkCondition = Not.of(In.of(
            FieldValue.builder().datasetRef(stagingDataset().datasetReference()).fieldName(ingestMode().digestField()).build(),
            Selection.builder()
                .source(mainDataset())
                .condition(And.of(whereClauseForNotInSink))
                .addFields(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(ingestMode().digestField()).build())
                .build()));

        List<Value> fieldsToSelect = new ArrayList<>(stagingDataset().schemaReference().fieldValues());
        List<Value> milestoneUpdateValues = transactionMilestoningFieldValues();
        fieldsToSelect.addAll(milestoneUpdateValues);
        Dataset selectStage = Selection.builder().source(stagingDataset()).condition(notInSinkCondition).addAllFields(fieldsToSelect).build();

        List<Value> fieldsToInsert = new ArrayList<>(stagingDataset().schemaReference().fieldValues());
        fieldsToInsert.addAll(transactionMilestoningFields());

        return Insert.of(mainDataset(), selectStage, fieldsToInsert);
    }

    /*
    Non-Partition :

    update "table_name" as sink
       set
       sink."batch_id_out"  = <<BATCH_ID>> - 1
    where
       sink."batch_id_out" = 999999999 and
       not exists
       (
        sink."digest" = stage."digest" and sink.primaryKeys = stage.primaryKeys
       )

      Partition :

      update "table_name" as sink
        set
        sink."batch_id_out"  = <<BATCH_ID>> - 1
      where
        sink."batch_id_out" = 999999999 and
        not exists
        (
        sink."digest" = stage."digest" and sink.primaryKeys = stage.primaryKeys
        ) and
        exists
        (
        sink.partitionColumns = stage.partitionColumns
        )

     */
    protected Update getSqlToMilestoneRows(List<Pair<FieldValue, Value>> values)
    {
        Condition notExistsWhereClause = Not.of(Exists.of(
            Selection.builder()
                .source(stagingDataset())
                .condition(And.builder().addConditions(primaryKeysMatchCondition, digestMatchCondition).build())
                .addAllFields(LogicalPlanUtils.ALL_COLUMNS())
                .build()));

        List<Condition> whereClauseForPartition = new ArrayList<>((Arrays.asList(openRecordCondition, notExistsWhereClause)));

        if (ingestMode().partitioned())
        {
            if (ingestMode().partitionValuesByField().isEmpty())
            {
                Condition partitionColumnCondition = Exists.of(
                    Selection.builder()
                        .source(stagingDataset())
                        .condition(LogicalPlanUtils.getPartitionColumnsMatchCondition(mainDataset(), stagingDataset(), ingestMode().partitionFields().toArray(new String[0])))
                        .addAllFields(LogicalPlanUtils.ALL_COLUMNS())
                        .build());
                whereClauseForPartition.add(partitionColumnCondition);
            }
            else
            {
                whereClauseForPartition.add(LogicalPlanUtils.getPartitionColumnValueMatchInCondition(mainDataset(), ingestMode().partitionValuesByField()));
            }
        }

        return UpdateAbstract.of(mainDataset(), values, And.of(whereClauseForPartition));
    }

    /*
   update {FULLY_QUALIFIED_SINK_TABLE_NAME} set
        "batch_id_out" = {TABLE_BATCH_ID} - 1,
         "batch_out_time" = {BATCH_TIME}"
   where "batch_id_out" = {MAX_BATCH_ID_VALUE}
   // OPTIONAL : when partition values are provided
   and sink.partition_key in [VALUE1, VALUE2, ...]
   */
    protected Update sqlToMilestoneAllRows(List<Pair<FieldValue, Value>> values)
    {
        List<Condition> conditions = new ArrayList<>();
        conditions.add(openRecordCondition);

        // Handle Partition Values
        if (ingestMode().partitioned() && !(ingestMode().partitionValuesByField().isEmpty()))
        {
            conditions.add(LogicalPlanUtils.getPartitionColumnValueMatchInCondition(mainDataset(), ingestMode().partitionValuesByField()));
        }
        return UpdateAbstract.of(mainDataset(), values, And.of(conditions));
    }


    private class EmptyDatasetHandler implements EmptyDatasetHandlingVisitor<LogicalPlan>
    {
        List<Pair<FieldValue, Value>> keyValuePairs;

        public EmptyDatasetHandler(List<Pair<FieldValue, Value>> keyValuePairs)
        {
            this.keyValuePairs = keyValuePairs;
        }

        @Override
        public LogicalPlan visitNoOp(NoOpAbstract noOpAbstract)
        {
            List<Operation> operations = new ArrayList<>();
            return LogicalPlan.of(operations);
        }

        @Override
        public LogicalPlan visitDeleteTargetData(DeleteTargetDataAbstract deleteTargetDataAbstract)
        {
            List<Operation> operations = new ArrayList<>();
            if (ingestMode().partitioned() && ingestMode().partitionValuesByField().isEmpty())
            {
                return LogicalPlan.of(operations);
            }
            operations.add(sqlToMilestoneAllRows(keyValuePairs));
            return LogicalPlan.of(operations);
        }

        @Override
        public LogicalPlan visitFailEmptyBatch(FailEmptyBatchAbstract failEmptyBatchAbstract)
        {
            throw new RuntimeException("Encountered an Empty Batch, FailEmptyBatch is enabled, so failing the batch!");
        }
    }
}
