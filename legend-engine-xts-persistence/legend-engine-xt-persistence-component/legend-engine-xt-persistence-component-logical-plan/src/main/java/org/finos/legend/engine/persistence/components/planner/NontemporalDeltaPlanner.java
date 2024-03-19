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

import java.util.HashMap;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.Resources;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.audit.AuditingVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersioningConditionVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategyVisitors;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.And;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Exists;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Not;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Delete;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Merge;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Update;
import org.finos.legend.engine.persistence.components.logicalplan.operations.UpdateAbstract;
import org.finos.legend.engine.persistence.components.logicalplan.values.*;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchIdValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_DELETED;

class NontemporalDeltaPlanner extends Planner
{
    private final Condition pkMatchCondition;
    private final Condition digestMatchCondition;
    private final Condition versioningCondition;

    private final Optional<String> deleteIndicatorField;
    private final List<Object> deleteIndicatorValues;

    private final Optional<Condition> deleteIndicatorIsNotSetCondition;
    private final Optional<Condition> deleteIndicatorIsSetCondition;
    private final BatchStartTimestamp batchStartTimestamp;
    private final BatchIdValue batchIdValue;

    private final Optional<Condition> dataSplitInRangeCondition;
    private List<Value> dataFields;

    NontemporalDeltaPlanner(Datasets datasets, NontemporalDelta ingestMode, PlannerOptions plannerOptions, Set<Capability> capabilities)
    {
        super(datasets, ingestMode, plannerOptions, capabilities);

        // validate
        validatePrimaryKeysNotEmpty(primaryKeys);

        this.pkMatchCondition = LogicalPlanUtils.getPrimaryKeyMatchCondition(mainDataset(), stagingDataset(), primaryKeys.toArray(new String[0]));
        this.digestMatchCondition = LogicalPlanUtils.getDigestMatchCondition(mainDataset(), stagingDataset(), ingestMode().digestField());
        this.versioningCondition = ingestMode().versioningStrategy()
            .accept(new VersioningConditionVisitor(mainDataset(), stagingDataset(), false, ingestMode().digestField()));

        this.deleteIndicatorField = ingestMode.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_DELETE_FIELD);
        this.deleteIndicatorValues = ingestMode.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_DELETE_VALUES);

        this.deleteIndicatorIsNotSetCondition = deleteIndicatorField.map(field -> LogicalPlanUtils.getDeleteIndicatorIsNotSetCondition(stagingDataset(), field, deleteIndicatorValues));
        this.deleteIndicatorIsSetCondition = deleteIndicatorField.map(field -> LogicalPlanUtils.getDeleteIndicatorIsSetCondition(stagingDataset(), field, deleteIndicatorValues));
        this.batchStartTimestamp = BatchStartTimestamp.INSTANCE;
        this.batchIdValue = metadataUtils.getBatchId(StringValue.of(mainDataset().datasetReference().name().orElseThrow(IllegalStateException::new)));
        this.dataSplitInRangeCondition = ingestMode.dataSplitField().map(field -> LogicalPlanUtils.getDataSplitInRangeCondition(stagingDataset(), field));
        this.dataFields = getDataFields();
    }

    @Override
    protected NontemporalDelta ingestMode()
    {
        return (NontemporalDelta) super.ingestMode();
    }

    @Override
    public LogicalPlan buildLogicalPlanForIngest(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();
        // Op1: Merge data from staging to main
        if (capabilities.contains(Capability.MERGE))
        {
            Merge merge = getMergeOperation();
            operations.add(merge);
        }
        else
        {
            Insert insert = getInsertOperation();
            Update update = getUpdateOperation();
            operations.add(update);
            operations.add(insert);
        }

        // Op2: Delete data from main table if delete indicator is present & match
        if (this.deleteIndicatorField.isPresent() && this.deleteIndicatorIsSetCondition.isPresent())
        {
            Delete delete = getDeleteOperation();
            operations.add(delete);
        }

        return LogicalPlan.of(operations);
    }

    /*
        DELETE FROM \"main\"_table
        WHERE EXIST (SELECT * FROM \"staging\"_table WHERE pk_match AND digest_match AND staging.delete_indicator_is_match)
     */
    private Delete getDeleteOperation()
    {
        return Delete.builder()
                .dataset(mainDataset())
                .condition(Exists.builder()
                    .source(Selection.builder()
                        .source(stagingDataset())
                        .addFields(All.INSTANCE)
                        .condition(And.builder().addConditions(this.pkMatchCondition, this.digestMatchCondition, this.deleteIndicatorIsSetCondition.get()).build())
                        .build())
                    .build())
                .build();
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
    private Merge getMergeOperation()
    {
        List<Pair<FieldValue, Value>> keyValuePairs = getKeyValuePairs();
        Dataset stagingDataset = stagingDataset();

        if (ingestMode().dataSplitField().isPresent())
        {
            stagingDataset = Selection.builder().source(stagingDataset).condition(this.dataSplitInRangeCondition).addAllFields(dataFields).alias(stagingDataset().datasetReference().alias()).build();
        }

        Condition versioningCondition;
        if (this.deleteIndicatorIsNotSetCondition.isPresent())
        {
            versioningCondition = And.builder().addConditions(this.versioningCondition, this.deleteIndicatorIsNotSetCondition.get()).build();
        }
        else
        {
            versioningCondition = this.versioningCondition;
        }

        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            String auditField = ingestMode().auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).orElseThrow(IllegalStateException::new);
            keyValuePairs.add(Pair.of(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(auditField).build(), batchStartTimestamp));
        }

        // Add batch_id field
        keyValuePairs.add(Pair.of(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(ingestMode().batchIdField()).build(), batchIdValue));

        Merge merge = Merge.builder()
            .dataset(mainDataset())
            .usingDataset(stagingDataset)
            .addAllMatchedKeyValuePairs(keyValuePairs)
            .addAllUnmatchedKeyValuePairs(keyValuePairs)
            .onCondition(this.pkMatchCondition)
            .matchedCondition(versioningCondition)
            .notMatchedCondition(this.deleteIndicatorIsNotSetCondition)
            .build();

        return merge;
    }

    /*
    UPDATE main_table
    SET
    sink.COL1 = (SELECT stage.COL1 FROM \"staging\" as stage WHERE (PKs match) AND (DIGEST does not match)),
    sink.COL2 = (SELECT stage.COL2 FROM \"staging\" as stage WHERE (PKs match) AND (DIGEST does not match)),
    ..
    WHERE EXISTS
    (SELECT * FROM \"staging\"_table WHERE (PKs match) AND (DIGEST does not match) AND DATA_SPLIT_CONDITION)
     */
    private Update getUpdateOperation()
    {
        Condition joinCondition;
        if (this.deleteIndicatorIsNotSetCondition.isPresent())
        {
            joinCondition = And.builder().addConditions(this.pkMatchCondition, this.versioningCondition, this.deleteIndicatorIsNotSetCondition.get()).build();
        }
        else
        {
            joinCondition = And.builder().addConditions(this.pkMatchCondition, this.versioningCondition).build();
        }
        Dataset stagingDataset = stagingDataset();
        List<Pair<FieldValue, Value>> keyValuePairs = getKeyValuePairs();

        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            String auditField = ingestMode().auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).orElseThrow(IllegalStateException::new);
            keyValuePairs.add(Pair.of(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(auditField).build(), this.batchStartTimestamp));
        }

        // Add batch_id field
        keyValuePairs.add(Pair.of(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(ingestMode().batchIdField()).build(), this.batchIdValue));

        if (ingestMode().dataSplitField().isPresent())
        {
            stagingDataset = Selection.builder().source(stagingDataset).condition(this.dataSplitInRangeCondition).addAllFields(LogicalPlanUtils.ALL_COLUMNS()).alias(stagingDataset().datasetReference().alias()).build();
        }
        Update update = UpdateAbstract.of(mainDataset(), stagingDataset, keyValuePairs, joinCondition);

        return update;
    }

    private List<Pair<FieldValue, Value>> getKeyValuePairs()
    {
        List<Value> fieldsToSelect = new ArrayList<>(dataFields);
        if (deleteIndicatorField.isPresent())
        {
            LogicalPlanUtils.removeField(fieldsToSelect, deleteIndicatorField.get());
        }
        List<Pair<FieldValue, Value>> keyValuePairs = fieldsToSelect
            .stream()
            .map(field -> Pair.<FieldValue, Value>of(
                    FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(((FieldValue) field).fieldName()).build(),
                    FieldValue.builder().datasetRef(stagingDataset().datasetReference()).fieldName(((FieldValue) field).fieldName()).build()))
            .collect(Collectors.toList());
        return keyValuePairs;
    }

    /*
    insert into main_table (staging_columns)
    (select staging_columns from stage_table
    where not exists
    (select * from main_table where pks match)
    */
    private Insert getInsertOperation()
    {
        List<Value> fieldsToSelect = new ArrayList<>(dataFields);
        List<Value> fieldsToInsert = new ArrayList<>(dataFields);
        if (deleteIndicatorField.isPresent())
        {
            LogicalPlanUtils.removeField(fieldsToSelect, deleteIndicatorField.get());
            LogicalPlanUtils.removeField(fieldsToInsert, deleteIndicatorField.get());
        }

        Condition notExistInSinkCondition = Not.of(Exists.of(
            Selection.builder()
                .source(mainDataset())
                .condition(this.pkMatchCondition)
                .addAllFields(LogicalPlanUtils.ALL_COLUMNS())
                .build())
        );

        Condition selectCondition = notExistInSinkCondition;
        if (ingestMode().dataSplitField().isPresent())
        {
            selectCondition = And.builder().addConditions(this.dataSplitInRangeCondition.get(), selectCondition).build();
        }
        if (deleteIndicatorIsNotSetCondition.isPresent())
        {
            selectCondition = And.builder().addConditions(this.deleteIndicatorIsNotSetCondition.get(), selectCondition).build();
        }

        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            String auditField = ingestMode().auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).orElseThrow(IllegalStateException::new);
            fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(auditField).build());
            fieldsToSelect.add(this.batchStartTimestamp);
        }

        // Add batch_id field
        fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(ingestMode().batchIdField()).build());
        fieldsToSelect.add(this.batchIdValue);

        Dataset selectStage = Selection.builder().source(stagingDataset()).condition(selectCondition).addAllFields(fieldsToSelect).build();
        return Insert.of(mainDataset(), selectStage, fieldsToInsert);
    }

    public Optional<Condition> getDataSplitInRangeConditionForStatistics()
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
    List<String> getDigestOrRemainingColumns()
    {
        return Arrays.asList(ingestMode().digestField());
    }

    @Override
    protected void addPostRunStatsForRowsDeleted(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        if (!this.deleteIndicatorField.isPresent() || !this.deleteIndicatorIsSetCondition.isPresent())
        {
            super.addPostRunStatsForRowsDeleted(postRunStatisticsResult);
        }
    }

    @Override
    protected void addPreRunStatsForRowsDeleted(Map<StatisticName, LogicalPlan> preRunStatisticsResult)
    {
        if (this.deleteIndicatorField.isPresent() && this.deleteIndicatorIsSetCondition.isPresent())
        {
            // Rows Deleted = rows removed (hard-deleted) from sink table
            LogicalPlan rowsDeletedCountPlan = LogicalPlan.builder().addOps(LogicalPlanUtils
                .getRecordCount(mainDataset(),
                        ROWS_DELETED.get(),
                        Optional.of(Exists.builder()
                                .source(Selection.builder()
                                        .source(stagingDataset())
                                        .addFields(All.INSTANCE)
                                        .condition(And.builder().addConditions(this.pkMatchCondition, this.digestMatchCondition, this.deleteIndicatorIsSetCondition.get()).build())
                                        .build())
                                .build()))).build();

            preRunStatisticsResult.put(ROWS_DELETED, rowsDeletedCountPlan);
        }
    }
}
