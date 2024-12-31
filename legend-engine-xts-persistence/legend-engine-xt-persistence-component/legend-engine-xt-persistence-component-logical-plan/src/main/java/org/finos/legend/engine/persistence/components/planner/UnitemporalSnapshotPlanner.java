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
import org.finos.legend.engine.persistence.components.exception.EmptyBatchException;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.deletestrategy.DeleteAllStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.DeleteTargetDataAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.EmptyDatasetHandlingVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.FailEmptyBatchAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.NoOpAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.partitioning.*;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.And;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Exists;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.In;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Not;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Update;
import org.finos.legend.engine.persistence.components.logicalplan.operations.UpdateAbstract;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Pair;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;

import java.util.*;
import java.util.stream.Collectors;

class UnitemporalSnapshotPlanner extends UnitemporalPlanner
{
    private Optional<Partitioning> partitioning = Optional.empty();

    UnitemporalSnapshotPlanner(Datasets datasets, UnitemporalSnapshot ingestMode, PlannerOptions plannerOptions, Set<Capability> capabilities)
    {
        super(datasets, ingestMode, plannerOptions, capabilities);

        if (ingestMode().partitioningStrategy() instanceof Partitioning)
        {
            partitioning = Optional.of((Partitioning) ingestMode().partitioningStrategy());
        }

        // validate all partitionFields must be present in staging dataset
        ingestMode.partitioningStrategy().accept(new PartitioningStrategyVisitor<Void>()
         {
             @Override
             public Void visitPartitioning(PartitioningAbstract partitionStrategy)
             {
                 List<String> fieldNames = stagingDataset().schema().fields().stream().map(Field::name).collect(Collectors.toList());
                 partitionStrategy.partitionFields().forEach(field -> validateExistence(
                         fieldNames,
                         field,
                         "Field [" + field + "] from partitionFields not present in incoming dataset"));
                 return null;
             }

             @Override
             public Void visitNoPartitioning(NoPartitioningAbstract noPartitionStrategy)
             {
                 return null;
             }
         });
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

     Partition with Delete All :

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
    )

     */
    protected Insert sqlToUpsertRows()
    {
        List<Value> dataFields = getDataFields();
        List<Value> fieldsToSelect = new ArrayList<>(dataFields);
        List<Value> milestoneUpdateValues = transactionMilestoningFieldValues();
        fieldsToSelect.addAll(milestoneUpdateValues);
        List<Value> fieldsToInsert = new ArrayList<>(dataFields);
        fieldsToInsert.addAll(transactionMilestoningFields());

        if (partitioning.isPresent() && partitioning.get().deleteStrategy() instanceof DeleteAllStrategy)
        {
            Dataset selectStage = Selection.builder().source(stagingDataset()).addAllFields(fieldsToSelect).build();
            return Insert.of(mainDataset(), selectStage, fieldsToInsert);
        }

        List<Condition> whereClauseForNotInSink = new ArrayList<>((Arrays.asList(openRecordCondition)));

        if (partitioning.isPresent())
        {
            Partitioning partition = partitioning.get();
            // if partitionValuesByField provided, add inCondition
            if (!partition.partitionValuesByField().isEmpty())
            {
                whereClauseForNotInSink.add(LogicalPlanUtils.getPartitionColumnValueMatchInCondition(mainDataset(), partition.partitionValuesByField()));
            }
            else if (!partition.partitionSpecList().isEmpty())
            {
                whereClauseForNotInSink.add(LogicalPlanUtils.getPartitionSpecMatchCondition(mainDataset(), partition.partitionSpecList()));
            }
            else
            {
                whereClauseForNotInSink.add(LogicalPlanUtils.getPartitionColumnsMatchCondition(mainDataset(), stagingDataset(), partition.partitionFields().toArray(new String[0])));
            }
        }

        Condition notInSinkCondition = Not.of(In.of(
                FieldValue.builder().datasetRef(stagingDataset().datasetReference()).fieldName(ingestMode().digestField().orElseThrow(IllegalStateException::new)).build(),
                Selection.builder()
                        .source(mainDataset())
                        .condition(And.of(whereClauseForNotInSink))
                        .addFields(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(ingestMode().digestField().orElseThrow(IllegalStateException::new)).build())
                        .build()));

        Dataset selectStage = Selection.builder().source(stagingDataset()).condition(notInSinkCondition).addAllFields(fieldsToSelect).build();

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

        Partition with Delete All:

      update "table_name" as sink
        set
        sink."batch_id_out"  = <<BATCH_ID>> - 1
      where
        sink."batch_id_out" = 999999999 and
       exists
        (
        sink.partitionColumns = stage.partitionColumns
        )

     */
    protected Update getSqlToMilestoneRows(List<Pair<FieldValue, Value>> values)
    {
        List<Condition> whereClause = new ArrayList<>(Arrays.asList(openRecordCondition));

        if (!(partitioning.isPresent() && partitioning.get().deleteStrategy() instanceof DeleteAllStrategy))
        {
            Condition notExistsWhereClause = Not.of(Exists.of(
                    Selection.builder()
                            .source(stagingDataset())
                            .condition(And.builder().addConditions(primaryKeysMatchCondition, digestMatchCondition).build())
                            .addAllFields(LogicalPlanUtils.ALL_COLUMNS())
                            .build()));
            whereClause.add(notExistsWhereClause);
        }

        if (partitioning.isPresent())
        {
            Partitioning partition = partitioning.get();

            if (!partition.partitionValuesByField().isEmpty())
            {
                whereClause.add(LogicalPlanUtils.getPartitionColumnValueMatchInCondition(mainDataset(), partition.partitionValuesByField()));
            }
            else if (!partition.partitionSpecList().isEmpty())
            {
                whereClause.add(LogicalPlanUtils.getPartitionSpecMatchCondition(mainDataset(), partition.partitionSpecList()));
            }
            else
            {
                Condition partitionColumnCondition = Exists.of(
                    Selection.builder()
                        .source(stagingDataset())
                        .condition(LogicalPlanUtils.getPartitionColumnsMatchCondition(mainDataset(), stagingDataset(), partition.partitionFields().toArray(new String[0])))
                        .addAllFields(LogicalPlanUtils.ALL_COLUMNS())
                        .build());
                whereClause.add(partitionColumnCondition);
            }
        }

        return UpdateAbstract.of(mainDataset(), values, And.of(whereClause));
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

        if (partitioning.isPresent())
        {
            Partitioning partition = partitioning.get();

            if (!(partition.partitionValuesByField().isEmpty()))
            {
                conditions.add(LogicalPlanUtils.getPartitionColumnValueMatchInCondition(mainDataset(), partition.partitionValuesByField()));
            }
            else if (!partition.partitionSpecList().isEmpty())
            {
                conditions.add(LogicalPlanUtils.getPartitionSpecMatchCondition(mainDataset(), partition.partitionSpecList()));
            }
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
            if (partitioning.isPresent() && partitioning.get().partitionValuesByField().isEmpty() && partitioning.get().partitionSpecList().isEmpty())
            {
                return LogicalPlan.of(operations);
            }
            operations.add(sqlToMilestoneAllRows(keyValuePairs));
            return LogicalPlan.of(operations);
        }

        @Override
        public LogicalPlan visitFailEmptyBatch(FailEmptyBatchAbstract failEmptyBatchAbstract)
        {
            throw new EmptyBatchException("Encountered an Empty Batch, FailEmptyBatch is enabled, so failing the batch!");
        }
    }
}
