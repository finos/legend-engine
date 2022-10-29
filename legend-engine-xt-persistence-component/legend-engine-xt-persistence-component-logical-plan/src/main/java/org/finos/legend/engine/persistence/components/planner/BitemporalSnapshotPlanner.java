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
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalSnapshot;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
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
import org.finos.legend.engine.persistence.components.logicalplan.values.DiffBinaryValueOperator;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Pair;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.common.StatisticName.INCOMING_RECORD_COUNT;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_DELETED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_INSERTED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_TERMINATED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_UPDATED;

class BitemporalSnapshotPlanner extends BitemporalPlanner
{
    BitemporalSnapshotPlanner(Datasets datasets, BitemporalSnapshot ingestMode, PlannerOptions plannerOptions)
    {
        super(datasets, ingestMode, plannerOptions);

        // validate

        if (ingestMode.partitioned())
        {
            List<String> fieldNames = stagingDataset().schema().fields().stream().map(Field::name).collect(Collectors.toList());
            ingestMode.partitionValuesByField().keySet().forEach(field -> validateExistence(
                fieldNames,
                field,
                "Field [" + field + "] from partitionValuesByField not present in incoming dataset"));
        }
    }

    protected BitemporalSnapshot ingestMode()
    {
        return (BitemporalSnapshot) super.ingestMode();
    }

    @Override
    public LogicalPlan buildLogicalPlanForIngest(Resources resources, Set<Capability> capabilities)
    {
        List<Pair<FieldValue, Value>> keyValuePairs = keyValuesForMilestoningUpdate();

        List<Operation> operations = new ArrayList<>();
        if (resources.stagingDataSetEmpty())
        {
            // Step 1: Milestone all Records in main table
            operations.add(sqlToMilestoneAllRows(keyValuePairs));
        }
        else
        {
            // Step 1: Milestone Records in main table
            operations.add(getSqlToMilestoneRows(keyValuePairs));
            // Step 2: Insert records in main table
            operations.add(sqlToUpsertRows());
        }
        return LogicalPlan.of(operations);
    }

    @Override
    public LogicalPlan buildLogicalPlanForPreActions(Resources resources)
    {
        return LogicalPlan.builder()
            .addOps(
                Create.of(true, mainDataset()),
                Create.of(true, metadataDataset().orElseThrow(IllegalStateException::new).get()))
            .build();
    }

    /*
    insert into main_table
    (
       select
            (
            select fields from stage
            )
            {TABLE_BATCH_ID} as "batch_id_in",
            999999999 as "batch_id_out",
            {validityFromReference} as "validityFromTarget",
            {validityThroughReference} as "validityThroughTarget",
        from
            stage
        where
            stage."digest" not in
            (
                select digest from main_table
                where sink."batch_id_out" = 999999999
            )
     )
     */
    protected Insert sqlToUpsertRows()
    {
        Condition notInSinkCondition = Not.of(In.of(
            FieldValue.builder().datasetRef(stagingDataset().datasetReference()).fieldName(ingestMode().digestField()).build(),
            Selection.builder()
                .source(mainDataset())
                .condition(openRecordCondition)
                .addFields(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(ingestMode().digestField()).build())
                .build()));

        List<Value> fieldsToSelect = fieldsToSelect();
        Dataset selectStage = Selection.builder().source(stagingDataset()).condition(notInSinkCondition).addAllFields(fieldsToSelect).build();

        List<Value> fieldsToInsert = fieldsToInsert();
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
        sink."digest" <> stage."digest" and sink.primaryKeys = stage.primaryKeys
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
                Condition partitionColumnCondition = Exists.of(Selection.builder()
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
        "batch_id_out" = {TABLE_BATCH_ID} - 1
   where "batch_id_out" = {MAX_BATCH_ID_VALUE}
   */
    protected Update sqlToMilestoneAllRows(List<Pair<FieldValue, Value>> values)
    {
        return UpdateAbstract.of(mainDataset(), values, openRecordCondition);
    }
}
