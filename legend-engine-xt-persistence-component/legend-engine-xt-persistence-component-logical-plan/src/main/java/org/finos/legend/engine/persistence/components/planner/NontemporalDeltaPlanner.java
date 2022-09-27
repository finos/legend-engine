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
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.audit.AuditingVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategyVisitors;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.And;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Exists;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Not;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Merge;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Update;
import org.finos.legend.engine.persistence.components.logicalplan.operations.UpdateAbstract;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Pair;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.common.StatisticName.INCOMING_RECORD_COUNT;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_DELETED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_TERMINATED;

class NontemporalDeltaPlanner extends Planner
{

    private final Optional<String> deleteIndicatorField;
    private final List<Object> deleteIndicatorValues;

    private BatchStartTimestamp batchStartTimestamp;

    NontemporalDeltaPlanner(Datasets datasets, NontemporalDelta ingestMode, PlannerOptions plannerOptions)
    {
        super(datasets, ingestMode, plannerOptions);

        // validate
        validatePrimaryKeysNotEmpty(primaryKeys);

        this.deleteIndicatorField = ingestMode.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_DELETE_FIELD);
        this.deleteIndicatorValues = ingestMode.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_DELETE_VALUES);

        this.batchStartTimestamp = BatchStartTimestamp.INSTANCE;
    }

    @Override
    protected NontemporalDelta ingestMode()
    {
        return (NontemporalDelta) super.ingestMode();
    }

    @Override
    public LogicalPlan buildLogicalPlanForIngest(Resources resources, Set<Capability> capabilities)
    {
        Condition pkMatchCondition = LogicalPlanUtils.getPrimaryKeyMatchCondition(mainDataset(), stagingDataset(), primaryKeys.toArray(new String[0]));
        Condition digestDoesNotMatchCondition = LogicalPlanUtils.getDigestDoesNotMatchCondition(mainDataset(), stagingDataset(), ingestMode().digestField());
        Condition digestMatchCondition = LogicalPlanUtils.getDigestMatchCondition(mainDataset(), stagingDataset(), ingestMode().digestField());

        List<Operation> operations = new ArrayList<>();
        // Op1: Merge data from staging to main
        if (capabilities.contains(Capability.MERGE))
        {
            Merge merge = getMergeOperation(pkMatchCondition, digestDoesNotMatchCondition);
            operations.add(merge);
        }
        else
        {
            Insert insert = getInsertOperation(pkMatchCondition, digestMatchCondition);
            Update update = getUpdateOperation(pkMatchCondition, digestDoesNotMatchCondition);
            operations.add(update);
            operations.add(insert);
        }

        return LogicalPlan.of(operations);
    }

    /*
        MERGE INTO main_table
        USING (staging_columns)
        ON pks_match AND
        WHEN MATCHED AND disgest_not_match THEN
        UPDATE SET column_assignment
        WHEN NOT MATCHED THEN
        INSERT (staging_columns)
        VALUES (staging_values)
    */
    private Merge getMergeOperation(Condition pkMatchCondition, Condition digestDoesNotMatchCondition)
    {
        List<Pair<FieldValue, Value>> keyValuePairs = stagingDataset().schemaReference().fieldValues()
            .stream()
            .map(field -> Pair.<FieldValue, Value>of(
                FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(field.fieldName()).build(),
                FieldValue.builder().datasetRef(stagingDataset().datasetReference()).fieldName(field.fieldName()).build()))
            .collect(Collectors.toList());

        Merge merge = Merge.builder()
            .dataset(mainDataset())
            .usingDataset(stagingDataset())
            .addAllMatchedKeyValuePairs(keyValuePairs)
            .addAllUnmatchedKeyValuePairs(keyValuePairs)
            .onCondition(pkMatchCondition)
            .matchedCondition(digestDoesNotMatchCondition)
            .build();

        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            String auditField = ingestMode().auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).orElseThrow(IllegalStateException::new);
            keyValuePairs.add(Pair.of(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(auditField).build(), batchStartTimestamp));
            merge = merge.withUnmatchedKeyValuePairs(keyValuePairs);
        }

        return merge;
    }

    /*
    UPDATE main_table
    SET
    sink.COL1 = (SELECT stage.COL1 FROM staging as stage WHERE (PKs match) AND (DIGEST does not match)),
    sink.COL2 = (SELECT stage.COL2 FROM staging as stage WHERE (PKs match) AND (DIGEST does not match)),
    ..
    WHERE EXISTS
    (SELECT * FROM staging_table WHERE (PKs match) AND (DIGEST does not match))
     */
    private Update getUpdateOperation(Condition pkMatchCondition, Condition digestDoesNotMatchCondition)
    {
        Condition joinCondition = And.builder().addConditions(pkMatchCondition, digestDoesNotMatchCondition).build();

        List<Pair<FieldValue, Value>> keyValuePairs = stagingDataset().schemaReference().fieldValues().stream().map(
            field -> Pair.<FieldValue, Value>of(
                FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(field.fieldName()).build(),
                FieldValue.builder().datasetRef(stagingDataset().datasetReference()).fieldName(field.fieldName()).build())).collect(Collectors.toList());

        Update update = UpdateAbstract.of(mainDataset(), stagingDataset(), keyValuePairs, joinCondition);
        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            String auditField = ingestMode().auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).orElseThrow(IllegalStateException::new);

            keyValuePairs.add(Pair.of(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(auditField).build(), batchStartTimestamp));
            update = update.withKeyValuePairs(keyValuePairs);
        }
        return update;
    }

    /*
    insert into main_table (staging_columns)
    (select staging_columns from stage_table
    where not exists
    (select * from main_table where digest_match and pks match )
    */
    private Insert getInsertOperation(Condition pkMatchCondition, Condition digestMatchCondition)
    {

        List<Value> stagingFields = new ArrayList<>(stagingDataset().schemaReference().fieldValues());

        Condition notExistInSinkCondition = Not.of(Exists.of(
            Selection.builder()
                .source(mainDataset())
                .condition(And.builder().addConditions(pkMatchCondition, digestMatchCondition).build())
                .addAllFields(LogicalPlanUtils.ALL_COLUMNS())
                .build())
        );

        List<Value> fieldsToSelect = new ArrayList<>(stagingDataset().schemaReference().fieldValues());
        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            String auditField = ingestMode().auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).orElseThrow(IllegalStateException::new);
            stagingFields.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(auditField).build());
            fieldsToSelect.add(batchStartTimestamp);
        }
        else
        {
            fieldsToSelect = LogicalPlanUtils.ALL_COLUMNS();
        }
        Dataset selectStage = Selection.builder().source(stagingDataset()).condition(notExistInSinkCondition).addAllFields(fieldsToSelect).build();
        return Insert.of(mainDataset(), selectStage, stagingFields);
    }

    @Override
    public LogicalPlan buildLogicalPlanForPreActions(Resources resources)
    {
        return LogicalPlan.builder().addOps(Create.of(true, mainDataset())).build();
    }

    @Override
    public Map<StatisticName, LogicalPlan> buildLogicalPlanForPreRunStatistics(Resources resources)
    {
        return Collections.emptyMap();
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

            if (ingestMode().auditing().accept(AUDIT_ENABLED))
            {
                //Rows terminated = Rows invalidated in Sink - Rows updated
                postRunStatisticsResult.put(
                    ROWS_TERMINATED,
                    LogicalPlanFactory.getLogicalPlanForConstantStats(ROWS_TERMINATED.get(), 0L));

                //Rows inserted (no previous active row with same primary key) = Rows added in sink - rows updated
                //todo :no way to differentiate b/w updated and inserted row
                //Rows updated (when it is invalidated and a new row for same primary keys is added)
                //Rows Deleted = rows removed(hard-deleted) from sink table
                postRunStatisticsResult.put(
                    ROWS_DELETED,
                    LogicalPlanFactory.getLogicalPlanForConstantStats(ROWS_DELETED.get(), 0L));
            }
        }
        return postRunStatisticsResult;
    }
}
