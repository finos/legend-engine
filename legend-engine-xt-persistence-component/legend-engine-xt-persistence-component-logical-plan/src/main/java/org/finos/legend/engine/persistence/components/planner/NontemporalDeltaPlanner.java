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

import java.util.Collections;
import java.util.HashMap;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.Resources;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.audit.AuditingVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategyVisitors;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.And;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Exists;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Not;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Or;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Delete;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_DELETED;

class NontemporalDeltaPlanner extends Planner
{
    private Condition pkMatchCondition;

    private final Optional<String> deleteIndicatorField;
    private final List<Object> deleteIndicatorValues;

    private final Optional<Condition> deleteIndicatorIsNotSetCondition;
    private Optional<Condition> deleteIndicatorIsSetCondition;

    private BatchStartTimestamp batchStartTimestamp;

    private final Optional<Condition> dataSplitInRangeCondition;

    NontemporalDeltaPlanner(Datasets datasets, NontemporalDelta ingestMode, PlannerOptions plannerOptions)
    {
        super(datasets, ingestMode, plannerOptions);

        // validate
        validatePrimaryKeysNotEmpty(primaryKeys);
        this.pkMatchCondition = LogicalPlanUtils.getPrimaryKeyMatchCondition(mainDataset(), stagingDataset(), primaryKeys.toArray(new String[0]));

        this.deleteIndicatorField = ingestMode.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_DELETE_FIELD);
        this.deleteIndicatorValues = ingestMode.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_DELETE_VALUES);

        this.deleteIndicatorIsNotSetCondition = deleteIndicatorField.map(field -> LogicalPlanUtils.getDeleteIndicatorIsNotSetCondition(stagingDataset(), field, deleteIndicatorValues));
        this.deleteIndicatorIsSetCondition = deleteIndicatorField.map(field -> LogicalPlanUtils.getDeleteIndicatorIsSetCondition(stagingDataset(), field, deleteIndicatorValues));

        this.batchStartTimestamp = BatchStartTimestamp.INSTANCE;

        this.dataSplitInRangeCondition = ingestMode.dataSplitField().map(field -> LogicalPlanUtils.getDataSplitInRangeCondition(stagingDataset(), field));
    }

    @Override
    protected NontemporalDelta ingestMode()
    {
        return (NontemporalDelta) super.ingestMode();
    }

    @Override
    public LogicalPlan buildLogicalPlanForIngest(Resources resources, Set<Capability> capabilities)
    {
        Condition digestDoesNotMatchCondition = LogicalPlanUtils.getDigestDoesNotMatchCondition(mainDataset(), stagingDataset(), ingestMode().digestField());
        Condition digestMatchCondition = LogicalPlanUtils.getDigestMatchCondition(mainDataset(), stagingDataset(), ingestMode().digestField());

        List<Operation> operations = new ArrayList<>();
        // Op1: Merge data from staging to main
        if (capabilities.contains(Capability.MERGE))
        {
            Merge merge = getMergeOperation(digestDoesNotMatchCondition);
            operations.add(merge);
        }
        else
        {
            Insert insert = getInsertOperation(digestMatchCondition);
            Update update = getUpdateOperation(digestDoesNotMatchCondition);
            operations.add(update);
            operations.add(insert);
        }

        // Op2: Delete data from main table if delete indicator is present & match
        if (this.deleteIndicatorField.isPresent() && this.deleteIndicatorIsSetCondition.isPresent())
        {
            Delete delete = getDeleteOperation(digestMatchCondition);
            operations.add(delete);
        }

        return LogicalPlan.of(operations);
    }

    /*
        DELETE FROM main_table WHERE EXIST (SELECT * FROM staging_table WHERE pk_match AND digest_match AND staging.delete_indicator_is_match)
     */
    private Delete getDeleteOperation(Condition digestMatchCondition)
    {
        List<Value> stagingFields = stagingDataset().schemaReference().fieldValues()
                .stream()
                .filter(field -> this.deleteIndicatorField.isPresent() ? !field.fieldName().equals(this.deleteIndicatorField.get()) : !field.fieldName().isEmpty())
                .collect(Collectors.toList());

        Delete delete = Delete.builder()
                .dataset(mainDataset())
                .condition(Exists.builder()
                    .source(Selection.builder()
                        .source(stagingDataset())
                        .addAllFields(stagingFields)
                        .condition(And.builder().addConditions(this.pkMatchCondition, digestMatchCondition, this.deleteIndicatorIsSetCondition.get()).build())
                        .build())
                    .build())
                .build();

        return delete;
    }

    /*
        MERGE INTO main_table
        USING (staging_columns)
        ON pks_match AND
        WHEN MATCHED AND ((DIGEST does not match) or (delete indicator NOT match)) THEN
        UPDATE SET column_assignment
        WHEN NOT MATCHED THEN
        INSERT (staging_columns)
        VALUES (staging_values)
    */
    private Merge getMergeOperation(Condition digestDoesNotMatchCondition)
    {
        List<Pair<FieldValue, Value>> keyValuePairs = stagingDataset().schemaReference().fieldValues()
            .stream()
            .filter(field -> this.deleteIndicatorField.isPresent() ? !field.fieldName().equals(this.deleteIndicatorField.get()) : !field.fieldName().isEmpty())
            .map(field -> Pair.<FieldValue, Value>of(
                FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(field.fieldName()).build(),
                FieldValue.builder().datasetRef(stagingDataset().datasetReference()).fieldName(field.fieldName()).build()))
            .collect(Collectors.toList());

        Condition digestCondition;
        Dataset stagingDataset = stagingDataset();

        if (ingestMode().dataSplitField().isPresent())
        {
            keyValuePairs.removeIf(field -> field.key().fieldName().equals(ingestMode().dataSplitField().get()));
            List<Value> fieldsToSelect = new ArrayList<>(stagingDataset().schemaReference().fieldValues());
            LogicalPlanUtils.removeField(fieldsToSelect, ingestMode().dataSplitField().get());
            stagingDataset = Selection.builder().source(stagingDataset()).condition(dataSplitInRangeCondition).addAllFields(fieldsToSelect).alias(stagingDataset().datasetReference().alias()).build();
        }

        if (deleteIndicatorIsNotSetCondition.isPresent())
        {
            digestCondition = Or.builder().addConditions(digestDoesNotMatchCondition, deleteIndicatorIsNotSetCondition.get()).build();
        }
        else
        {
            digestCondition = digestDoesNotMatchCondition;
        }

        Merge merge = Merge.builder()
            .dataset(mainDataset())
            .usingDataset(stagingDataset)
            .addAllMatchedKeyValuePairs(keyValuePairs)
            .addAllUnmatchedKeyValuePairs(keyValuePairs)
            .onCondition(this.pkMatchCondition)
            .matchedCondition(digestCondition)
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
    (SELECT * FROM staging_table WHERE (PKs match) AND (DIGEST does not match) AND DATA_SPLIT_CONDITION)
     */
    private Update getUpdateOperation(Condition digestDoesNotMatchCondition)
    {
        Condition joinCondition = And.builder().addConditions(this.pkMatchCondition, digestDoesNotMatchCondition).build();
        Dataset stagingDataset = stagingDataset();

        List<Pair<FieldValue, Value>> keyValuePairs = stagingDataset().schemaReference().fieldValues()
            .stream()
            .filter(field -> this.deleteIndicatorField.isPresent() ? !field.fieldName().equals(this.deleteIndicatorField.get()) : !field.fieldName().isEmpty())
            .map(field -> Pair.<FieldValue, Value>of(
                    FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(field.fieldName()).build(),
                    FieldValue.builder().datasetRef(stagingDataset().datasetReference()).fieldName(field.fieldName()).build()))
            .collect(Collectors.toList());

        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            String auditField = ingestMode().auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).orElseThrow(IllegalStateException::new);
            keyValuePairs.add(Pair.of(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(auditField).build(), batchStartTimestamp));
        }

        if (ingestMode().dataSplitField().isPresent())
        {
            keyValuePairs.removeIf(field -> field.key().fieldName().equals(ingestMode().dataSplitField().get()));
            stagingDataset = Selection.builder().source(stagingDataset()).condition(dataSplitInRangeCondition).addAllFields(LogicalPlanUtils.ALL_COLUMNS()).alias(stagingDataset().datasetReference().alias()).build();
        }
        Update update = UpdateAbstract.of(mainDataset(), stagingDataset, keyValuePairs, joinCondition);

        return update;
    }

    /*
    insert into main_table (staging_columns)
    (select staging_columns from stage_table
    where not exists
    (select * from main_table where digest_match and pks match )
    */
    private Insert getInsertOperation(Condition digestMatchCondition)
    {
        List<Value> fieldsToInsert = stagingDataset().schemaReference().fieldValues()
                .stream()
                .filter(field -> this.deleteIndicatorField.isPresent() ? !field.fieldName().equals(this.deleteIndicatorField.get()) : !field.fieldName().isEmpty())
                .collect(Collectors.toList());

        List<Value> fieldsToSelect = stagingDataset().schemaReference().fieldValues()
                .stream()
                .filter(field -> this.deleteIndicatorField.isPresent() ? !field.fieldName().equals(this.deleteIndicatorField.get()) : !field.fieldName().isEmpty())
                .collect(Collectors.toList());

        Condition notExistInSinkCondition = Not.of(Exists.of(
            Selection.builder()
                .source(mainDataset())
                .condition(And.builder().addConditions(this.pkMatchCondition, digestMatchCondition).build())
                .addAllFields(LogicalPlanUtils.ALL_COLUMNS())
                .build())
        );

        Condition selectCondition = notExistInSinkCondition;
        if (ingestMode().dataSplitField().isPresent())
        {
            LogicalPlanUtils.removeField(fieldsToSelect, ingestMode().dataSplitField().get());
            LogicalPlanUtils.removeField(fieldsToInsert, ingestMode().dataSplitField().get());
            selectCondition = And.builder().addConditions(dataSplitInRangeCondition.get(), notExistInSinkCondition).build();
        }

        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            String auditField = ingestMode().auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).orElseThrow(IllegalStateException::new);
            fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(auditField).build());
            fieldsToSelect.add(batchStartTimestamp);
        }
        else if (!ingestMode().dataSplitField().isPresent())
        {
            fieldsToSelect = LogicalPlanUtils.ALL_COLUMNS();
        }
        Dataset selectStage = Selection.builder().source(stagingDataset()).condition(selectCondition).addAllFields(fieldsToSelect).build();
        return Insert.of(mainDataset(), selectStage, fieldsToInsert);
    }

    @Override
    public LogicalPlan buildLogicalPlanForPreActions(Resources resources)
    {
        return LogicalPlan.builder().addOps(Create.of(true, mainDataset())).build();
    }

    public Optional<Condition> getDataSplitInRangeCondition()
    {
        return dataSplitInRangeCondition;
    }

    // stats related
    @Override
    protected void addPostRunStatsForRowsUpdated(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        // Not supported at the moment
    }

    @Override
    protected void addPostRunStatsForRowsInserted(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        // Not supported at the moment
    }

    @Override
    public Map<StatisticName, LogicalPlan> buildLogicalPlanForPreRunStatistics(Resources resources)
    {
        Map<StatisticName, LogicalPlan> preRunStatisticsResult = new HashMap<>();
        if (options().collectStatistics())
        {
            //Rows deleted
            addPreRunStatsForRowsDeleted(preRunStatisticsResult);
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
            addPostRunStatsForIncomingRecords(postRunStatisticsResult);
            //Rows terminated
            addPostRunStatsForRowsTerminated(postRunStatisticsResult);
            //Rows inserted
            addPostRunStatsForRowsInserted(postRunStatisticsResult);
            //Rows updated
            addPostRunStatsForRowsUpdated(postRunStatisticsResult);
            //Rows deleted
            if (!this.deleteIndicatorField.isPresent() || !this.deleteIndicatorIsSetCondition.isPresent())
            {
                addPostRunStatsForRowsDeleted(postRunStatisticsResult);
            }
        }

        return postRunStatisticsResult;
    }

    @Override
    protected void addPreRunStatsForRowsDeleted(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        if (this.deleteIndicatorField.isPresent() && this.deleteIndicatorIsSetCondition.isPresent())
        {
            Condition digestMatchCondition = LogicalPlanUtils.getDigestMatchCondition(mainDataset(), stagingDataset(), ingestMode().digestField());

            // Rows Deleted = rows removed (hard-deleted) from sink table
            LogicalPlan rowsDeletedCountPlan = LogicalPlan.builder().addOps(LogicalPlanUtils
                .getRecordCount(
                    stagingDataset(),
                    ROWS_DELETED.get(),
                    Optional.of(And.builder().addConditions(this.deleteIndicatorIsSetCondition.get(),
                        Exists.builder()
                            .source(Selection.builder()
                                .source(mainDataset())
                                .condition(And.builder().addConditions(this.pkMatchCondition, digestMatchCondition).build())
                                .build())
                            .build()).build()))).build();

            postRunStatisticsResult.put(ROWS_DELETED, rowsDeletedCountPlan);
        }
        else
        {
            super.addPreRunStatsForRowsDeleted(postRunStatisticsResult);
        }
    }
}
