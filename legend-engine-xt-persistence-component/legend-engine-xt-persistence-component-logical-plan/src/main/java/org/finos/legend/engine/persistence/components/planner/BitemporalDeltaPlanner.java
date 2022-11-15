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
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategyVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.And;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Exists;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.GreaterThan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.IsNull;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.LessThan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.LessThanEqualTo;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Not;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.NotEquals;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Or;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Join;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.JoinOperation;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Drop;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Delete;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Update;
import org.finos.legend.engine.persistence.components.logicalplan.operations.UpdateAbstract;
import org.finos.legend.engine.persistence.components.logicalplan.values.Case;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.logicalplan.values.NumericalValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Pair;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class BitemporalDeltaPlanner extends BitemporalPlanner
{
    private static final String VALID_DATE_TIME_FROM_NAME = "start_date";
    private static final String VALID_DATE_TIME_THRU_NAME = "end_date";
    private static final String LEFT_DATASET_IN_JOIN_ALIAS = "x";
    private static final String RIGHT_DATASET_IN_JOIN_ALIAS = "y";
    private static final String TEMP_DATASET_BASE_NAME = "temp";
    private static final String TEMP_DATASET_WITH_DELETE_INDICATOR_BASE_NAME = "tempWithDeleteIndicator";
    private static final String STAGE_DATASET_WITHOUT_DUPLICATES_BASE_NAME = "stageWithoutDuplicates";

    private final Optional<String> deleteIndicatorField;
    private final List<Object> deleteIndicatorValues;

    private final Optional<Condition> deleteIndicatorIsNotSetCondition;
    private final Optional<Condition> deleteIndicatorIsSetCondition;
    private final Optional<Condition> dataSplitInRangeCondition;

    // TODO: optional? final?
    private Dataset stagingDataset;
    private Dataset tempDataset;
    private Dataset tempDatasetWithDeleteIndicator;

    private FieldValue sourceValidDatetimeFrom;
    private FieldValue targetValidDatetimeFrom;
    private FieldValue targetValidDatetimeThru;
    private FieldValue digest;

    private FieldValue validDateTimeFrom;
    private FieldValue validDateTimeThru;

    private List<FieldValue> primaryKeyFields;
    private List<FieldValue> primaryKeyFieldsAndFromFieldFromStage;
    private List<FieldValue> primaryKeyFieldsAndFromFieldFromMain;
    private List<FieldValue> primaryKeyFieldsAndFromFieldForSelection;
    private List<FieldValue> dataFields;

    BitemporalDeltaPlanner(Datasets datasets, BitemporalDelta ingestMode, PlannerOptions plannerOptions)
    {
        super(datasets, ingestMode, plannerOptions);

        if (ingestMode().validityMilestoning().validityDerivation() instanceof SourceSpecifiesFromDateTime && ingestMode().deduplicationStrategy() instanceof FilterDuplicates)
        {
            this.stagingDataset = getStagingDatasetWithoutDuplicates(datasets);
        }
        else
        {
            this.stagingDataset = stagingDataset();
        }

        this.deleteIndicatorField = ingestMode.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_DELETE_FIELD);
        this.deleteIndicatorValues = ingestMode.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_DELETE_VALUES);

        this.deleteIndicatorIsNotSetCondition = deleteIndicatorField.map(field -> LogicalPlanUtils.getDeleteIndicatorIsNotSetCondition(stagingDataset, field, deleteIndicatorValues));
        this.deleteIndicatorIsSetCondition = deleteIndicatorField.map(field -> LogicalPlanUtils.getDeleteIndicatorIsSetCondition(stagingDataset, field, deleteIndicatorValues));
        this.dataSplitInRangeCondition = ingestMode.dataSplitField().map(field -> LogicalPlanUtils.getDataSplitInRangeCondition(stagingDataset, field));

        if (ingestMode().validityMilestoning().validityDerivation() instanceof SourceSpecifiesFromDateTime)
        {
            this.tempDataset = getTempDataset(datasets);

            this.sourceValidDatetimeFrom = FieldValue.builder().fieldName(ingestMode.validityMilestoning().validityDerivation().accept(BitemporalPlanner.EXTRACT_SOURCE_VALID_DATE_TIME_FROM)).alias(VALID_DATE_TIME_FROM_NAME).build();
            this.targetValidDatetimeFrom = FieldValue.builder().fieldName(ingestMode.validityMilestoning().accept(BitemporalPlanner.EXTRACT_TARGET_VALID_DATE_TIME_FROM)).alias(VALID_DATE_TIME_FROM_NAME).build();
            this.targetValidDatetimeThru = FieldValue.builder().fieldName(ingestMode.validityMilestoning().accept(BitemporalPlanner.EXTRACT_TARGET_VALID_DATE_TIME_THRU)).alias(VALID_DATE_TIME_THRU_NAME).build();

            this.validDateTimeFrom = FieldValue.builder().fieldName(VALID_DATE_TIME_FROM_NAME).build();
            this.validDateTimeThru = FieldValue.builder().fieldName(VALID_DATE_TIME_THRU_NAME).build();

            this.dataFields = stagingDataset.schemaReference().fieldValues().stream().map(field -> FieldValue.builder().fieldName(field.fieldName()).build()).collect(Collectors.toList());
            this.dataFields.removeIf(field -> field.fieldName().equals(ingestMode.digestField()));
            this.dataFields.removeIf(field -> field.fieldName().equals(sourceValidDatetimeFrom.fieldName()));

            this.primaryKeys.removeIf(fieldName -> fieldName.equals(sourceValidDatetimeFrom.fieldName()));
            this.primaryKeysMatchCondition = LogicalPlanUtils.getPrimaryKeyMatchCondition(mainDataset(), stagingDataset, primaryKeys.toArray(new String[0]));

            this.primaryKeyFields = new ArrayList<>();
            for (String pkName : primaryKeys)
            {
                this.primaryKeyFields.add(FieldValue.builder().fieldName(pkName).build());
                this.dataFields.removeIf(field -> field.fieldName().equals(pkName));
            }

            this.primaryKeyFieldsAndFromFieldFromStage = new ArrayList<>();
            this.primaryKeyFieldsAndFromFieldFromStage.addAll(primaryKeyFields);
            this.primaryKeyFieldsAndFromFieldFromStage.add(sourceValidDatetimeFrom);

            this.primaryKeyFieldsAndFromFieldFromMain = new ArrayList<>();
            this.primaryKeyFieldsAndFromFieldFromMain.addAll(primaryKeyFields);
            this.primaryKeyFieldsAndFromFieldFromMain.add(targetValidDatetimeFrom);

            this.primaryKeyFieldsAndFromFieldForSelection = new ArrayList<>();
            this.primaryKeyFieldsAndFromFieldForSelection.addAll(primaryKeyFields);
            this.primaryKeyFieldsAndFromFieldForSelection.add(validDateTimeFrom);

            this.digest = FieldValue.builder().fieldName(ingestMode.digestField()).build();

            if (deleteIndicatorField.isPresent())
            {
                this.tempDatasetWithDeleteIndicator = getTempDatasetWithDeleteIndicator(datasets);
                this.dataFields.removeIf(field -> field.fieldName().equals(deleteIndicatorField.get()));
            }

            if (ingestMode.dataSplitField().isPresent())
            {
                this.dataFields.removeIf(field -> field.fieldName().equals(ingestMode.dataSplitField().get()));
            }
        }
    }

    private Dataset getStagingDatasetWithoutDuplicates(Datasets datasets)
    {
        return datasets.stagingDatasetWithoutDuplicates().orElse(DatasetDefinition.builder()
            .schema(stagingDataset().schema())
            .database(stagingDataset().datasetReference().database())
            .group(stagingDataset().datasetReference().group())
            .name(LogicalPlanUtils.generateTableNameWithSuffix(stagingDataset().datasetReference().name().orElseThrow((IllegalStateException::new)), STAGE_DATASET_WITHOUT_DUPLICATES_BASE_NAME))
            .alias(STAGE_DATASET_WITHOUT_DUPLICATES_BASE_NAME)
            .build());
    }

    private Dataset getTempDataset(Datasets datasets)
    {
        return datasets.tempDataset().orElse(DatasetDefinition.builder()
            .schema(mainDataset().schema())
            .database(mainDataset().datasetReference().database())
            .group(mainDataset().datasetReference().group())
            .name(LogicalPlanUtils.generateTableNameWithSuffix(mainDataset().datasetReference().name().orElseThrow((IllegalStateException::new)), TEMP_DATASET_BASE_NAME))
            .alias(TEMP_DATASET_BASE_NAME)
            .build());
    }

    private Dataset getTempDatasetWithDeleteIndicator(Datasets datasets)
    {
        if (datasets.tempDatasetWithDeleteIndicator().isPresent())
        {
            return datasets.tempDatasetWithDeleteIndicator().get();
        }
        else
        {
            Field deleteIndicator = Field.builder().name(deleteIndicatorField.orElseThrow((IllegalStateException::new))).type(FieldType.of(DataType.BOOLEAN, Optional.empty(), Optional.empty())).build();
            List<Field> mainFieldsPlusDeleteIndicator = new ArrayList<>(mainDataset().schema().fields());
            mainFieldsPlusDeleteIndicator.add(deleteIndicator);
            return DatasetDefinition.builder()
                .schema(mainDataset().schema().withFields(mainFieldsPlusDeleteIndicator))
                .database(mainDataset().datasetReference().database())
                .group(mainDataset().datasetReference().group())
                .name(LogicalPlanUtils.generateTableNameWithSuffix(mainDataset().datasetReference().name().orElseThrow((IllegalStateException::new)), TEMP_DATASET_WITH_DELETE_INDICATOR_BASE_NAME))
                .alias(TEMP_DATASET_WITH_DELETE_INDICATOR_BASE_NAME)
                .build();
        }
    }

    @Override
    protected BitemporalDelta ingestMode()
    {
        return (BitemporalDelta) super.ingestMode();
    }

    @Override
    public LogicalPlan buildLogicalPlanForIngest(Resources resources, Set<Capability> capabilities)
    {
        List<Operation> operations = new ArrayList<>();

        if (ingestMode().validityMilestoning().validityDerivation() instanceof SourceSpecifiesFromAndThruDateTime)
        {
            // Op 1: Milestone Records in main table
            operations.add(getMilestoningLogic());
            // Op 2: Insert records in main table
            operations.add(getUpsertLogic());
        }
        else
        {
            if (ingestMode().deduplicationStrategy() instanceof FilterDuplicates)
            {
                // Op 0: Insert records from stage table to stage without duplicates table
                operations.add(getStageToStageWithoutDuplicates());
            }
            // Op 1: Insert records from stage table to temp table
            operations.add(getStageToTemp());
            // Op 2: Insert records from main table to temp table
            operations.add(getMainToTemp());
            // Op 3: Milestone records in main table
            operations.add(getUpdateMain(tempDataset));
            // Op 4: Insert records from temp table to main table
            operations.add(getTempToMain());
            if (deleteIndicatorField.isPresent())
            {
                // Op 5: Insert records from main table to temp table for deletion
                operations.add(getMainToTempForDeletion());
                // Op 6: Milestone records in main table for deletion
                operations.add(getUpdateMain(tempDatasetWithDeleteIndicator));
                // Op 7: Insert records from temp table to main table for deletion
                operations.add(getTempToMainForDeletion());
            }
            // Op 8: Cleanup temp tables
            operations.add(Delete.builder().dataset(tempDataset).build());
            if (deleteIndicatorField.isPresent())
            {
                operations.add(Delete.builder().dataset(tempDatasetWithDeleteIndicator).build());
            }
            if (ingestMode().deduplicationStrategy() instanceof FilterDuplicates)
            {
                operations.add(Delete.builder().dataset(stagingDataset).build());
            }
        }
        return LogicalPlan.of(operations);
    }

    @Override
    public LogicalPlan buildLogicalPlanForPreActions(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();

        operations.add(Create.of(true, mainDataset()));
        operations.add(Create.of(true, metadataDataset().orElseThrow(IllegalStateException::new).get()));

        if (ingestMode().validityMilestoning().validityDerivation() instanceof SourceSpecifiesFromDateTime)
        {
            operations.add(Create.of(true, tempDataset));
            if (deleteIndicatorField.isPresent())
            {
                operations.add(Create.of(true, tempDatasetWithDeleteIndicator));
            }
            if (ingestMode().deduplicationStrategy() instanceof FilterDuplicates)
            {
                operations.add(Create.of(true, stagingDataset));
            }
        }

        return LogicalPlan.of(operations);
    }

    /*
    ------------------
    Upsert Logic:
    ------------------
    staging_columns: columns coming from staging
    special_columns: batch_id_in, batch_id_out, validityFromTarget, validityThroughTarget

    INSERT INTO main_table (staging_columns, special_columns)
    SELECT {SELECT_LOGIC} from staging
    WHERE NOT EXISTS
    (batch_id_out = INF) and (DIGEST match) and (PKs match)
     */
    private Insert getUpsertLogic()
    {
        Condition notExistsCondition = Not.of(Exists.of(
            Selection.builder()
                .source(mainDataset())
                .condition(And.builder().addConditions(openRecordCondition, digestMatchCondition, primaryKeysMatchCondition).build())
                .addAllFields(LogicalPlanUtils.ALL_COLUMNS())
                .build()));

        Condition selectCondition = notExistsCondition;
        if (ingestMode().dataSplitField().isPresent())
        {
            selectCondition = And.builder().addConditions(selectCondition, dataSplitInRangeCondition.orElseThrow(IllegalStateException::new)).build();
        }
        if (deleteIndicatorField.isPresent())
        {
            selectCondition = And.builder().addConditions(selectCondition, deleteIndicatorIsNotSetCondition.orElseThrow(IllegalStateException::new)).build();
        }

        List<Value> fieldsToSelect = fieldsToSelect();
        List<Value> fieldsToInsert = fieldsToInsert();

        deleteIndicatorField.ifPresent(deleteIndicatorField ->
        {
            LogicalPlanUtils.removeField(fieldsToSelect, deleteIndicatorField);
            LogicalPlanUtils.removeField(fieldsToInsert, deleteIndicatorField);
        });
        if (ingestMode().dataSplitField().isPresent())
        {
            LogicalPlanUtils.removeField(fieldsToSelect, ingestMode().dataSplitField().get());
            LogicalPlanUtils.removeField(fieldsToInsert, ingestMode().dataSplitField().get());
        }

        Dataset selectStage = Selection.builder().source(stagingDataset()).condition(selectCondition).addAllFields(fieldsToSelect).build();

        return Insert.of(mainDataset(), selectStage, fieldsToInsert);
    }

    /*
    ------------------
    Milestoning Logic:
    ------------------
    UPDATE main_table (batch_id_out = {TABLE_BATCH_ID} - 1)
    WHERE
    (batch_id_out = INF) and
    EXISTS (select * from staging where (data split in range) and (PKs match) and ((digest does not match) or (delete indicator is present)))
    */
    private Update getMilestoningLogic()
    {
        List<Pair<FieldValue, Value>> updatePairs = keyValuesForMilestoningUpdate();

        Condition digestCondition = deleteIndicatorField.isPresent()
            ? Or.builder().addConditions(digestDoesNotMatchCondition, deleteIndicatorIsSetCondition.orElseThrow(IllegalStateException::new)).build()
            : digestDoesNotMatchCondition;

        Condition selectCondition = dataSplitInRangeCondition.isPresent()
            ? And.builder().addConditions(dataSplitInRangeCondition.get(), primaryKeysMatchCondition, digestCondition).build()
            : And.builder().addConditions(primaryKeysMatchCondition, digestCondition).build();

        Condition existsCondition = Exists.of(Selection.builder()
            .source(stagingDataset())
            .condition(selectCondition)
            .addAllFields(LogicalPlanUtils.ALL_COLUMNS())
            .build());

        Condition milestoningCondition = And.builder().addConditions(openRecordCondition, existsCondition).build();

        return UpdateAbstract.of(mainDataset(), updatePairs, milestoningCondition);
    }

    /*
    ------------------
    Stage to Stage without Duplicates Logic:
    ------------------
    INSERT INTO stage_without_duplicates (ALL_FIELDS_FROM_STAGE)
        SELECT ALL_FIELDS_FROM_STAGE FROM stage WHERE NOT EXISTS
            (SELECT * FROM main WHERE stage.digest = main.digest AND openRecordCondition)
    */
    private Insert getStageToStageWithoutDuplicates()
    {
        Condition digestMatchAndOpenCondition = And.builder().addConditions(digestMatchCondition, openRecordCondition).build();
        Condition whereNotExists = Not.of(Exists.of(Selection.builder().source(mainDataset()).addAllFields(LogicalPlanUtils.ALL_COLUMNS()).condition(digestMatchAndOpenCondition).build()));
        List<Value> stageFields = new ArrayList<>(stagingDataset().schemaReference().fieldValues());
        Selection select = Selection.builder().source(stagingDataset()).addAllFields(stageFields).condition(whereNotExists).build();
        return Insert.builder().sourceDataset(select).targetDataset(stagingDataset).addAllFields(stageFields).build();
    }

    /*
    -------------------
    Stage to Temp Logic:
    -------------------
    INSERT INTO temp (PK_FIELDS, DATA_FIELDS, DIGEST, FROM_FIELD, THRU_FIELD, BATCH_IN, BATCH_OUT)
    SELECT PK_FIELDS, DATA_FIELDS, DIGEST, FROM_FIELD, y.end_date as THRU_FIELD, <batch_id> as BATCH_IN, INF as BATCH_OUT
    FROM
    -------------------------------------------------- THIRD JOIN -------------------------------------------------
    (SELECT PK_FIELDS, DATA_FIELDS, DIGEST, FROM_FIELD FROM stage) x
    LEFT JOIN
      -------------------------------------------------- SECOND JOIN -------------------------------------------------
      (SELECT PK_FIELDS, FROM_FIELD, COALESCE(MIN(y.start_date), MIN(x.end_date)) AS end_date
      FROM
         -------------------------------------------------- FIRST JOIN -------------------------------------------------
         (SELECT PK_FIELDS, FROM_FIELD, COALESCE(MIN(y.start_date), INF) AS end_date
         FROM
                (SELECT PK_FIELDS, FROM_FIELD FROM stage) x
                LEFT JOIN
                (SELECT PK_FIELDS, FROM_FIELD FROM main WHERE openRecordCondition) y
                ON PKS_MATCH AND x.start_date < y.start_date GROUP BY PK_FIELDS, FROM_FIELD
          ) x
          ----------------------------------------END OF FIRST JOIN--------------------------------------------------
       LEFT JOIN
       (SELECT PK_FIELDS, FROM_FIELD FROM stage) y
       ON PKS_MATCH  AND y.start_date > x.start_date  AND y.start_date < x.end_date GROUP BY PK_FIELDS, FROM_FIELD
       ) y
       -------------------------------------------END OF SECOND JOIN----------------------------------------------------
    ON PKS_MATCH AND FROM_FIELD_MATCH
    ----------------------------------------------END OF THIRD JOIN-----------------------------------------------------
   */
    private Insert getStageToTemp()
    {
        // FIRST JOIN between stage X and MAIN Y
        Selection selectX1;
        if (deleteIndicatorField.isPresent() && ingestMode().dataSplitField().isPresent())
        {
            selectX1 = Selection.builder().source(stagingDataset).addAllFields(primaryKeyFieldsAndFromFieldFromStage).condition(And.builder().addConditions(deleteIndicatorIsNotSetCondition.orElseThrow(IllegalStateException::new), dataSplitInRangeCondition.orElseThrow(IllegalStateException::new)).build()).alias(LEFT_DATASET_IN_JOIN_ALIAS).build();
        }
        else if (deleteIndicatorField.isPresent() && !ingestMode().dataSplitField().isPresent())
        {
            selectX1 = Selection.builder().source(stagingDataset).addAllFields(primaryKeyFieldsAndFromFieldFromStage).condition(deleteIndicatorIsNotSetCondition).alias(LEFT_DATASET_IN_JOIN_ALIAS).build();
        }
        else if (!deleteIndicatorField.isPresent() && ingestMode().dataSplitField().isPresent())
        {
            selectX1 = Selection.builder().source(stagingDataset).addAllFields(primaryKeyFieldsAndFromFieldFromStage).condition(dataSplitInRangeCondition).alias(LEFT_DATASET_IN_JOIN_ALIAS).build();
        }
        else
        {
            selectX1 = Selection.builder().source(stagingDataset).addAllFields(primaryKeyFieldsAndFromFieldFromStage).alias(LEFT_DATASET_IN_JOIN_ALIAS).build();
        }
        Selection selectY1 = Selection.builder().source(mainDataset()).condition(openRecordCondition).addAllFields(primaryKeyFieldsAndFromFieldFromMain).alias(RIGHT_DATASET_IN_JOIN_ALIAS).build();

        Condition x1AndY1PkMatchCondition = LogicalPlanUtils.getPrimaryKeyMatchCondition(selectX1, selectY1, primaryKeys.toArray(new String[0]));
        Condition x1FromLessThanY1From = LessThan.of(validDateTimeFrom.withDatasetRef(selectX1.datasetReference()), validDateTimeFrom.withDatasetRef(selectY1.datasetReference()));
        Condition joinXY1Condition = And.builder().addConditions(x1AndY1PkMatchCondition, x1FromLessThanY1From).build();
        Join joinXY1 = Join.of(selectX1, selectY1, joinXY1Condition, JoinOperation.LEFT_OUTER_JOIN);

        List<Value> selectXY1Fields = primaryKeyFieldsAndFromFieldForSelection.stream().map(field -> field.withDatasetRef(selectX1.datasetReference())).collect(Collectors.toList());
        selectXY1Fields.add(FunctionImpl.builder()
            .functionName(FunctionName.COALESCE)
            .addValue(FunctionImpl.builder().functionName(FunctionName.MIN).addValue(validDateTimeFrom.withDatasetRef(selectY1.datasetReference())).build(), LogicalPlanUtils.INFINITE_BATCH_TIME())
            .alias(VALID_DATE_TIME_THRU_NAME)
            .build());

        List<Value> selectXY1GroupByFields = primaryKeyFieldsAndFromFieldForSelection.stream().map(field -> field.withDatasetRef(selectX1.datasetReference())).collect(Collectors.toList());
        Selection selectXY1 = Selection.builder().source(joinXY1).addAllFields(selectXY1Fields).groupByFields(selectXY1GroupByFields).build();

        // SECOND JOIN between X and Y
        Selection selectX2 = selectXY1.withAlias(LEFT_DATASET_IN_JOIN_ALIAS);
        Selection selectY2;
        if (deleteIndicatorField.isPresent() && ingestMode().dataSplitField().isPresent())
        {
            selectY2 = Selection.builder().source(stagingDataset).addAllFields(primaryKeyFieldsAndFromFieldFromStage).condition(And.builder().addConditions(deleteIndicatorIsNotSetCondition.orElseThrow(IllegalStateException::new), dataSplitInRangeCondition.orElseThrow(IllegalStateException::new)).build()).alias(RIGHT_DATASET_IN_JOIN_ALIAS).build();
        }
        else if (deleteIndicatorField.isPresent() && !ingestMode().dataSplitField().isPresent())
        {
            selectY2 = Selection.builder().source(stagingDataset).addAllFields(primaryKeyFieldsAndFromFieldFromStage).condition(deleteIndicatorIsNotSetCondition).alias(RIGHT_DATASET_IN_JOIN_ALIAS).build();
        }
        else if (!deleteIndicatorField.isPresent() && ingestMode().dataSplitField().isPresent())
        {
            selectY2 = Selection.builder().source(stagingDataset).addAllFields(primaryKeyFieldsAndFromFieldFromStage).condition(dataSplitInRangeCondition).alias(RIGHT_DATASET_IN_JOIN_ALIAS).build();
        }
        else
        {
            selectY2 = Selection.builder().source(stagingDataset).addAllFields(primaryKeyFieldsAndFromFieldFromStage).alias(RIGHT_DATASET_IN_JOIN_ALIAS).build();
        }

        Condition x2AndY2PkMatchCondition = LogicalPlanUtils.getPrimaryKeyMatchCondition(selectX2, selectY2, primaryKeys.toArray(new String[0]));
        Condition y2FromGreaterThanX2From = GreaterThan.of(validDateTimeFrom.withDatasetRef(selectY2.datasetReference()), validDateTimeFrom.withDatasetRef(selectX2.datasetReference()));
        Condition y2FromLessThanX2Through = LessThan.of(validDateTimeFrom.withDatasetRef(selectY2.datasetReference()), validDateTimeThru.withDatasetRef(selectX2.datasetReference()));
        Condition joinXY2Condition = And.builder().addConditions(x2AndY2PkMatchCondition, y2FromGreaterThanX2From, y2FromLessThanX2Through).build();
        Join joinXY2 = Join.of(selectX2, selectY2, joinXY2Condition, JoinOperation.LEFT_OUTER_JOIN);

        List<Value> selectXY2Fields = primaryKeyFieldsAndFromFieldForSelection.stream().map(field -> field.withDatasetRef(selectX2.datasetReference())).collect(Collectors.toList());
        selectXY2Fields.add(FunctionImpl.builder()
            .functionName(FunctionName.COALESCE)
            .addValue(
                FunctionImpl.builder().functionName(FunctionName.MIN).addValue(validDateTimeFrom.withDatasetRef(selectY2.datasetReference())).build(),
                FunctionImpl.builder().functionName(FunctionName.MIN).addValue(validDateTimeThru.withDatasetRef(selectX2.datasetReference())).build())
            .alias(VALID_DATE_TIME_THRU_NAME)
            .build());

        List<Value> selectXY2GroupByFields = primaryKeyFieldsAndFromFieldForSelection.stream().map(field -> field.withDatasetRef(selectX2.datasetReference())).collect(Collectors.toList());
        Selection selectXY2 = Selection.builder().source(joinXY2).addAllFields(selectXY2Fields).groupByFields(selectXY2GroupByFields).build();

        // THIRD JOIN between X and Y
        Selection selectX3;
        if (deleteIndicatorField.isPresent() && ingestMode().dataSplitField().isPresent())
        {
            selectX3 = Selection.builder().source(stagingDataset).addAllFields(stagingDataset.schemaReference().fieldValues()).condition(And.builder().addConditions(deleteIndicatorIsNotSetCondition.orElseThrow(IllegalStateException::new), dataSplitInRangeCondition.orElseThrow(IllegalStateException::new)).build()).alias(LEFT_DATASET_IN_JOIN_ALIAS).build();
        }
        else if (deleteIndicatorField.isPresent() && !ingestMode().dataSplitField().isPresent())
        {
            selectX3 = Selection.builder().source(stagingDataset).addAllFields(stagingDataset.schemaReference().fieldValues()).condition(deleteIndicatorIsNotSetCondition).alias(LEFT_DATASET_IN_JOIN_ALIAS).build();
        }
        else if (!deleteIndicatorField.isPresent() && ingestMode().dataSplitField().isPresent())
        {
            selectX3 = Selection.builder().source(stagingDataset).addAllFields(stagingDataset.schemaReference().fieldValues()).condition(dataSplitInRangeCondition).alias(LEFT_DATASET_IN_JOIN_ALIAS).build();
        }
        else
        {
            selectX3 = Selection.builder().source(stagingDataset).addAllFields(stagingDataset.schemaReference().fieldValues()).alias(LEFT_DATASET_IN_JOIN_ALIAS).build();
        }

        Selection selectY3 = selectXY2.withAlias(RIGHT_DATASET_IN_JOIN_ALIAS);

        Condition x3AndY3PkMatchCondition = LogicalPlanUtils.getPrimaryKeyMatchCondition(selectX3, selectY3, primaryKeys.toArray(new String[0]));
        Condition x3FromEqualsY3From = Equals.of(sourceValidDatetimeFrom.withDatasetRef(selectX3.datasetReference()), validDateTimeFrom.withDatasetRef(selectY3.datasetReference()));
        Condition joinXY3Condition = And.builder().addConditions(x3AndY3PkMatchCondition, x3FromEqualsY3From).build();
        Join joinXY3 = Join.of(selectX3, selectY3, joinXY3Condition, JoinOperation.LEFT_OUTER_JOIN);

        List<Value> selectXY3Fields = new ArrayList<>();
        selectXY3Fields.addAll(primaryKeyFields.stream().map(field -> field.withDatasetRef(selectX3.datasetReference())).collect(Collectors.toList()));
        selectXY3Fields.addAll(dataFields.stream().map(field -> field.withDatasetRef(selectX3.datasetReference())).collect(Collectors.toList()));
        selectXY3Fields.add(digest.withDatasetRef(selectX3.datasetReference()));
        selectXY3Fields.add(sourceValidDatetimeFrom.withDatasetRef(selectX3.datasetReference()));
        selectXY3Fields.add(validDateTimeThru.withDatasetRef(selectY3.datasetReference()));
        selectXY3Fields.addAll(transactionMilestoningFieldValues());

        Selection selectXY3 = Selection.builder().source(joinXY3).addAllFields(selectXY3Fields).build();

        // Final insertion into temp table
        List<Value> insertFields = new ArrayList<>();
        insertFields.addAll(primaryKeyFields);
        insertFields.addAll(dataFields);
        insertFields.add(digest);
        insertFields.add(targetValidDatetimeFrom);
        insertFields.add(targetValidDatetimeThru);
        insertFields.addAll(transactionMilestoningFields());

        return Insert.builder().targetDataset(tempDataset).sourceDataset(selectXY3).addAllFields(insertFields).build();
    }

    /*
    -------------------
    Main to Temp Logic:
    -------------------
    INSERT INTO temp (PK_FIELDS, DATA_FIELDS, DIGEST, FROM_FIELD, THRU_FIELD, BATCH_IN, BATCH_OUT)
    SELECT PK_FIELDS, DATA_FIELDS, DIGEST, FROM_FIELD, y.end_date as THRU_FIELD, <batch_id> as BATCH_IN, INF as BATCH_OUT
    FROM
    -------------------------------------------------- THIRD JOIN ------------------------------------------------------
    (SELECT PK_FIELDS, DATA_FIELDS, DIGEST, FROM_FIELD FROM main WHERE batch_id_out = INF) x
    JOIN
      -------------------------------------------------- SECOND JOIN ---------------------------------------------------
      (SELECT PK_FIELDS, FROM_FIELD, THRU_FIELD
      FROM
         -------------------------------------------------- FIRST JOIN -------------------------------------------------
          (SELECT PK_FIELDS, FROM_FIELD, MIN(y.start_date) as THRU_FIELD
          FROM
          (SELECT PK_FIELDS, FROM_FIELD, THRU_FIELD FROM main WHERE batch_id_out = INF) x
          JOIN
          (SELECT PK_FIELDS, FROM_FIELD FROM stage) y
          ON PKS_MATCH AND y.start_date > x.start_date  AND y.start_date < x.end_date GROUP BY PK_FIELDS, FROM_FIELD
          ) x
          ----------------------------------------END OF FIRST JOIN-----------------------------------------------------
       WHERE NOT EXISTS
       (SELECT gs_loan_number, start_date FROM stage
       WHERE x.gs_loan_number = y.gs_loan_number AND x.start_date = y.start_date)
       ) y
       -------------------------------------------END OF SECOND JOIN----------------------------------------------------
    ON PKS_MATCH AND FROM_FIELD_MATCH
    ----------------------------------------------END OF THIRD JOIN-----------------------------------------------------
    */
    private Insert getMainToTemp()
    {
        // FIRST JOIN between X and Y
        List<Value> selectX1Fields = new ArrayList<>();
        selectX1Fields.addAll(primaryKeyFieldsAndFromFieldFromMain);
        selectX1Fields.add(targetValidDatetimeThru.withAlias(VALID_DATE_TIME_THRU_NAME));

        Selection selectX1 = Selection.builder().source(mainDataset()).addAllFields(selectX1Fields).condition(openRecordCondition).alias(LEFT_DATASET_IN_JOIN_ALIAS).build();
        Selection selectY1;
        if (deleteIndicatorField.isPresent() && ingestMode().dataSplitField().isPresent())
        {
            selectY1 = Selection.builder().source(stagingDataset).addAllFields(primaryKeyFieldsAndFromFieldFromStage).condition(And.builder().addConditions(deleteIndicatorIsNotSetCondition.orElseThrow(IllegalStateException::new), dataSplitInRangeCondition.orElseThrow(IllegalStateException::new)).build()).alias(RIGHT_DATASET_IN_JOIN_ALIAS).build();
        }
        else if (deleteIndicatorField.isPresent() && !ingestMode().dataSplitField().isPresent())
        {
            selectY1 = Selection.builder().source(stagingDataset).addAllFields(primaryKeyFieldsAndFromFieldFromStage).condition(deleteIndicatorIsNotSetCondition).alias(RIGHT_DATASET_IN_JOIN_ALIAS).build();
        }
        else if (!deleteIndicatorField.isPresent() && ingestMode().dataSplitField().isPresent())
        {
            selectY1 = Selection.builder().source(stagingDataset).addAllFields(primaryKeyFieldsAndFromFieldFromStage).condition(dataSplitInRangeCondition).alias(RIGHT_DATASET_IN_JOIN_ALIAS).build();
        }
        else
        {
            selectY1 = Selection.builder().source(stagingDataset).addAllFields(primaryKeyFieldsAndFromFieldFromStage).alias(RIGHT_DATASET_IN_JOIN_ALIAS).build();
        }

        Condition x1AndY1PkMatchCondition = LogicalPlanUtils.getPrimaryKeyMatchCondition(selectX1, selectY1, primaryKeys.toArray(new String[0]));
        Condition y1FromGreaterThanX1From = GreaterThan.of(validDateTimeFrom.withDatasetRef(selectY1.datasetReference()), validDateTimeFrom.withDatasetRef(selectX1.datasetReference()));
        Condition y1FromLessThanX1Through = LessThan.of(validDateTimeFrom.withDatasetRef(selectY1.datasetReference()), validDateTimeThru.withDatasetRef(selectX1.datasetReference()));
        Condition joinXY1Condition = And.builder().addConditions(x1AndY1PkMatchCondition, y1FromGreaterThanX1From, y1FromLessThanX1Through).build();
        Join joinXY1 = Join.of(selectX1, selectY1, joinXY1Condition, JoinOperation.INNER_JOIN);

        List<Value> selectXY1Fields = primaryKeyFieldsAndFromFieldForSelection.stream().map(field -> field.withDatasetRef(selectX1.datasetReference())).collect(Collectors.toList());
        selectXY1Fields.add(FunctionImpl.builder().functionName(FunctionName.MIN).addValue(validDateTimeFrom.withDatasetRef(selectY1.datasetReference())).alias(VALID_DATE_TIME_THRU_NAME).build());

        List<Value> selectXY1GroupByFields = primaryKeyFieldsAndFromFieldForSelection.stream().map(field -> field.withDatasetRef(selectX1.datasetReference())).collect(Collectors.toList());

        Selection selectXY1 = Selection.builder().source(joinXY1).addAllFields(selectXY1Fields).groupByFields(selectXY1GroupByFields).build();

        // SECOND JOIN between X and Y
        Selection selectX2 = selectXY1.withAlias(LEFT_DATASET_IN_JOIN_ALIAS);

        Condition x2AndStagePkMatchCondition = LogicalPlanUtils.getPrimaryKeyMatchCondition(selectX2, stagingDataset, primaryKeys.toArray(new String[0]));
        Condition x2FromEqualsStageFrom = Equals.of(validDateTimeFrom.withDatasetRef(selectX2.datasetReference()), sourceValidDatetimeFrom.withDatasetRef(stagingDataset.datasetReference()));
        Condition selectFromStageCondition = And.builder().addConditions(x2AndStagePkMatchCondition, x2FromEqualsStageFrom).build();
        if (deleteIndicatorField.isPresent())
        {
            selectFromStageCondition = And.builder().addConditions(selectFromStageCondition, deleteIndicatorIsNotSetCondition.orElseThrow(IllegalStateException::new)).build();
        }
        if (ingestMode().dataSplitField().isPresent())
        {
            selectFromStageCondition = And.builder().addConditions(selectFromStageCondition, dataSplitInRangeCondition.orElseThrow(IllegalStateException::new)).build();
        }
        Condition whereNotExists = Not.builder().condition(Exists.of(Selection.builder().source(stagingDataset).addAllFields(primaryKeyFieldsAndFromFieldFromStage).condition(selectFromStageCondition).build())).build();

        List<Value> selectXY2Fields = primaryKeyFieldsAndFromFieldForSelection.stream().map(field -> field.withDatasetRef(selectX2.datasetReference())).collect(Collectors.toList());
        selectXY2Fields.add(validDateTimeThru.withDatasetRef(selectX2.datasetReference()).withAlias(VALID_DATE_TIME_THRU_NAME));

        Selection selectXY2 = Selection.builder().source(selectX2).addAllFields(selectXY2Fields).condition(whereNotExists).build();

        // THIRD JOIN between X and Y
        Selection selectX3 = Selection.builder().source(mainDataset()).addAllFields(mainDataset().schemaReference().fieldValues()).condition(openRecordCondition).alias(LEFT_DATASET_IN_JOIN_ALIAS).build();
        Selection selectY3 = selectXY2.withAlias(RIGHT_DATASET_IN_JOIN_ALIAS);

        Condition x3AndY3PkMatchCondition = LogicalPlanUtils.getPrimaryKeyMatchCondition(selectX3, selectY3, primaryKeys.toArray(new String[0]));
        Condition x3FromEqualsY3From = Equals.of(targetValidDatetimeFrom.withDatasetRef(selectX3.datasetReference()), validDateTimeFrom.withDatasetRef(selectY3.datasetReference()));
        Condition joinXY3Condition = And.builder().addConditions(x3AndY3PkMatchCondition, x3FromEqualsY3From).build();
        Join joinXY3 = Join.of(selectX3, selectY3, joinXY3Condition, JoinOperation.INNER_JOIN);

        List<Value> selectXY3Fields = new ArrayList<>();
        selectXY3Fields.addAll(primaryKeyFields.stream().map(field -> field.withDatasetRef(selectX3.datasetReference())).collect(Collectors.toList()));
        selectXY3Fields.addAll(dataFields.stream().map(field -> field.withDatasetRef(selectX3.datasetReference())).collect(Collectors.toList()));
        selectXY3Fields.add(digest.withDatasetRef(selectX3.datasetReference()));
        selectXY3Fields.add(targetValidDatetimeFrom.withDatasetRef(selectX3.datasetReference()));
        selectXY3Fields.add(validDateTimeThru.withDatasetRef(selectY3.datasetReference()));
        selectXY3Fields.addAll(transactionMilestoningFieldValues());

        Selection selectXY3 = Selection.builder().source(joinXY3).addAllFields(selectXY3Fields).build();

        // Final insertion into temp table
        List<Value> insertFields = new ArrayList<>();
        insertFields.addAll(primaryKeyFields);
        insertFields.addAll(dataFields);
        insertFields.add(digest);
        insertFields.add(targetValidDatetimeFrom);
        insertFields.add(targetValidDatetimeThru);
        insertFields.addAll(transactionMilestoningFields());

        return Insert.builder().targetDataset(tempDataset).sourceDataset(selectXY3).addAllFields(insertFields).build();
    }

    /*
    ------------------
    Update main table Logic:
    ------------------
    UPDATE main x SET batch_id_out = <BATCH_ID>
        WHERE EXISTS
        (SELECT * FROM temp y WHERE
        PKS_MATCH AND FROM_MATCH)
        AND <Record is open>
    */
    private Update getUpdateMain(Dataset tempDataset)
    {
        Condition pKMatchCondition = LogicalPlanUtils.getPrimaryKeyMatchCondition(mainDataset(), tempDataset, primaryKeys.toArray(new String[0]));
        Condition fromMatchCondition = Equals.of(targetValidDatetimeFrom.withDatasetRef(mainDataset().datasetReference()), targetValidDatetimeFrom.withDatasetRef(tempDataset.datasetReference()));
        Condition selectionCondition = And.builder().addConditions(pKMatchCondition, fromMatchCondition).build();

        Selection select = Selection.builder().source(tempDataset).addAllFields(LogicalPlanUtils.ALL_COLUMNS()).condition(selectionCondition).build();

        Condition existsCondition = Exists.of(select);
        Condition updateCondition = And.builder().addConditions(existsCondition, openRecordCondition).build();

        return Update.builder().dataset(mainDataset()).addAllKeyValuePairs(keyValuesForMilestoningUpdate()).whereCondition(updateCondition).build();
    }

    /*
    ------------------
    Temp to Main Logic:
    ------------------
    INSERT INTO main (ALL_FIELDS_FROM_TEMP)
        SELECT ALL_FIELDS_FROM_TEMP FROM temp
    */
    private Insert getTempToMain()
    {
        List<Value> tempFields = new ArrayList<>(tempDataset.schemaReference().fieldValues());
        Selection select = Selection.builder().source(tempDataset).addAllFields(tempFields).build();
        return Insert.builder().sourceDataset(select).targetDataset(mainDataset()).addAllFields(tempFields).build();
    }

    /*
    -------------------
    Main to Temp (for Deletion) Logic:
    -------------------
    INSERT INTO tempWithDeleteIndicator (PK_FIELDS, DATA_FIELDS, DIGEST, FROM_FIELD, THRU_FIELD, BATCH_IN, BATCH_OUT, DELETE_INDICATOR)
    SELECT PK_FIELDS, DATA_FIELDS, DIGEST, FROM_FIELD, x.end_date as THRU_FIELD, <batch_id> as BATCH_IN, INF as BATCH_OUT, CASE WHEN y.delete_indicator IS NULL THEN 0 ELSE 1 END
    FROM
    (SELECT * FROM main WHERE BATCH_OUT = INF
    AND EXISTS
        (SELECT * FROM stage WHERE PKS_MATCH AND (main.start_date = start_date OR main.end_date = start_date) AND stage.delete_indicator = 1) x
        LEFT JOIN
        (SELECT * FROM stage) y
        ON PKS_MATCH AND x.start_date = y.start_date
    */
    private Insert getMainToTempForDeletion()
    {
        Condition mainFromEqualsStageFrom = Equals.of(targetValidDatetimeFrom.withDatasetRef(mainDataset().datasetReference()), sourceValidDatetimeFrom.withDatasetRef(stagingDataset.datasetReference()));
        Condition mainThroughEqualsStageFrom = Equals.of(targetValidDatetimeThru.withDatasetRef(mainDataset().datasetReference()), sourceValidDatetimeFrom.withDatasetRef(stagingDataset.datasetReference()));
        Condition innerSelectCondition = And.builder().addConditions(primaryKeysMatchCondition, Or.builder().addConditions(mainFromEqualsStageFrom, mainThroughEqualsStageFrom).build(), deleteIndicatorIsSetCondition.orElseThrow(IllegalStateException::new)).build();

        Selection innerSelect;
        if (ingestMode().dataSplitField().isPresent())
        {
            innerSelect = Selection.builder().source(stagingDataset).addAllFields(LogicalPlanUtils.ALL_COLUMNS()).condition(And.builder().addConditions(innerSelectCondition, dataSplitInRangeCondition.orElseThrow(IllegalStateException::new)).build()).build();
        }
        else
        {
            innerSelect = Selection.builder().source(stagingDataset).addAllFields(LogicalPlanUtils.ALL_COLUMNS()).condition(innerSelectCondition).build();
        }

        Condition selectXCondition = And.builder().addConditions(openRecordCondition, Exists.of(innerSelect)).build();
        Selection selectX = Selection.builder().source(mainDataset()).addAllFields(LogicalPlanUtils.ALL_COLUMNS()).condition(selectXCondition).alias(LEFT_DATASET_IN_JOIN_ALIAS).build();

        Selection selectY;
        if (ingestMode().dataSplitField().isPresent())
        {
            selectY = Selection.builder().source(stagingDataset).addAllFields(LogicalPlanUtils.ALL_COLUMNS()).condition(dataSplitInRangeCondition).alias(RIGHT_DATASET_IN_JOIN_ALIAS).build();
        }
        else
        {
            selectY = Selection.builder().source(stagingDataset).addAllFields(LogicalPlanUtils.ALL_COLUMNS()).alias(RIGHT_DATASET_IN_JOIN_ALIAS).build();
        }

        Condition xAndYPkMatchCondition = LogicalPlanUtils.getPrimaryKeyMatchCondition(selectX, selectY, primaryKeys.toArray(new String[0]));
        Condition xFromEqualsYFrom = Equals.of(targetValidDatetimeFrom.withDatasetRef(selectX.datasetReference()), sourceValidDatetimeFrom.withDatasetRef(selectY.datasetReference()));
        Condition joinXYCondition = And.builder().addConditions(xAndYPkMatchCondition, xFromEqualsYFrom).build();

        Join joinXY = Join.of(selectX, selectY, joinXYCondition, JoinOperation.LEFT_OUTER_JOIN);

        FieldValue deleteIndicator = FieldValue.builder().fieldName(deleteIndicatorField.orElseThrow(IllegalStateException::new)).build();
        FieldValue yDeleteIndicator = deleteIndicator.withDatasetRef(selectY.datasetReference());

        List<Value> selectXYFields = new ArrayList<>();
        selectXYFields.addAll(primaryKeyFields.stream().map(field -> field.withDatasetRef(selectX.datasetReference())).collect(Collectors.toList()));
        selectXYFields.addAll(dataFields.stream().map(field -> field.withDatasetRef(selectX.datasetReference())).collect(Collectors.toList()));
        selectXYFields.add(digest.withDatasetRef(selectX.datasetReference()));
        selectXYFields.add(targetValidDatetimeFrom.withDatasetRef(selectX.datasetReference()));
        selectXYFields.add(targetValidDatetimeThru.withDatasetRef(selectX.datasetReference()));
        selectXYFields.addAll(transactionMilestoningFieldValues());
        selectXYFields.add(Case.builder()
            .addConditionValueList(Pair.of(IsNull.of(yDeleteIndicator), NumericalValue.of(0L)))
            .elseValue(NumericalValue.of(1L)).build());

        Selection selectXY = Selection.builder().source(joinXY).addAllFields(selectXYFields).build();

        List<Value> insertFields = new ArrayList<>();
        insertFields.addAll(primaryKeyFields);
        insertFields.addAll(dataFields);
        insertFields.add(digest);
        insertFields.add(targetValidDatetimeFrom);
        insertFields.add(targetValidDatetimeThru);
        insertFields.addAll(transactionMilestoningFields());
        insertFields.add(deleteIndicator);

        return Insert.builder().targetDataset(tempDatasetWithDeleteIndicator).sourceDataset(selectXY).addAllFields(insertFields).build();
    }

    /*
    -------------------
    Temp to Main (for Deletion) Logic:
    -------------------
    INSERT INTO main (PK_FIELDS, DATA_FIELDS, DIGEST, FROM_FIELD, THRU_FIELD, BATCH_IN, BATCH_OUT)
    SELECT PK_FIELDS, DATA_FIELDS, DIGEST, FROM_FIELD, MAX(y.end_date) as THRU_FIELD, BATCH_IN, BATCH_OUT
    FROM
    -------------------------------------------------- SECOND JOIN -------------------------------------------------
    (SELECT PK_FIELDS, DATA_FIELDS, DIGEST, FROM_FIELD, COALESCE(MIN(y.start_date, INF)) as THRU_FIELD, BATCH_IN, BATCH_OUT
    FROM
      -------------------------------------------------- FIRST JOIN -------------------------------------------------
      tempWithDeleteIndicator AS x
      LEFT JOIN
      tempWithDeleteIndicator AS y
      ON PKS_MATCH AND y.start_date > x.start_date AND y.delete_indicator = 0
      WHERE x.delete_indicator = 0
      GROUP BY PK_FIELDS, DATA_FIELDS, DIGEST, FROM_FIELD, BATCH_IN, BATCH_OUT
      ) x
       ------------------------------------------- END OF FIRST JOIN----------------------------------------------------
    LEFT JOIN
    tempWithDeleteIndicator AS y
    ON PKS_MATCH AND y.end_date > x.start_date AND y.end_date <= x.end_date AND y.delete_indicator <> 0
    GROUP BY PK_FIELDS, DATA_FIELDS, DIGEST, FROM_FIELD, BATCH_IN, BATCH_OUT
    ---------------------------------------------- END OF SECOND JOIN-----------------------------------------------------
    */
    private Insert getTempToMainForDeletion()
    {
        // FIRST JOIN between X and Y
        Dataset tempX1 = tempDatasetWithDeleteIndicator.datasetReference().withAlias(LEFT_DATASET_IN_JOIN_ALIAS);
        Dataset tempY1 = tempDatasetWithDeleteIndicator.datasetReference().withAlias(RIGHT_DATASET_IN_JOIN_ALIAS);

        Condition x1AndY1PkMatchCondition = LogicalPlanUtils.getPrimaryKeyMatchCondition(tempX1, tempY1, primaryKeys.toArray(new String[0]));
        Condition y1FromGreaterThanX1From = GreaterThan.of(targetValidDatetimeFrom.withDatasetRef(tempY1.datasetReference()), targetValidDatetimeFrom.withDatasetRef(tempX1.datasetReference()));
        Condition y1DeleteIndicatorIsZero = Equals.of(FieldValue.builder().fieldName(deleteIndicatorField.orElseThrow(IllegalStateException::new)).datasetRef(tempY1.datasetReference()).build(), NumericalValue.of(0L));
        Condition joinXY1Condition = And.builder().addConditions(x1AndY1PkMatchCondition, y1FromGreaterThanX1From, y1DeleteIndicatorIsZero).build();
        Join joinXY1 = Join.of(tempX1, tempY1, joinXY1Condition, JoinOperation.LEFT_OUTER_JOIN);

        List<Value> selectXY1Fields = new ArrayList<>();
        selectXY1Fields.addAll(primaryKeyFields.stream().map(field -> field.withDatasetRef(tempX1.datasetReference())).collect(Collectors.toList()));
        selectXY1Fields.addAll(dataFields.stream().map(field -> field.withDatasetRef(tempX1.datasetReference())).collect(Collectors.toList()));
        selectXY1Fields.add(digest.withDatasetRef(tempX1.datasetReference()));
        selectXY1Fields.add(targetValidDatetimeFrom.withDatasetRef(tempX1.datasetReference()).withAlias(VALID_DATE_TIME_FROM_NAME));
        selectXY1Fields.add(FunctionImpl.builder().functionName(FunctionName.COALESCE).addValue(FunctionImpl.builder().functionName(FunctionName.MIN).addValue(targetValidDatetimeFrom.withDatasetRef(tempY1.datasetReference())).build(), LogicalPlanUtils.INFINITE_BATCH_TIME()).alias(VALID_DATE_TIME_THRU_NAME).build());
        selectXY1Fields.addAll(transactionMilestoningFields().stream().map(field -> field.withDatasetRef(tempX1.datasetReference())).collect(Collectors.toList()));

        List<Value> selectXY1GroupByFields = new ArrayList<>();
        selectXY1GroupByFields.addAll(primaryKeyFields.stream().map(field -> field.withDatasetRef(tempX1.datasetReference())).collect(Collectors.toList()));
        selectXY1GroupByFields.addAll(dataFields.stream().map(field -> field.withDatasetRef(tempX1.datasetReference())).collect(Collectors.toList()));
        selectXY1GroupByFields.add(digest.withDatasetRef(tempX1.datasetReference()));
        selectXY1GroupByFields.add(targetValidDatetimeFrom.withDatasetRef(tempX1.datasetReference()));
        selectXY1GroupByFields.addAll(transactionMilestoningFields().stream().map(field -> field.withDatasetRef(tempX1.datasetReference())).collect(Collectors.toList()));

        Condition selectXY1Condition = Equals.of(FieldValue.builder().fieldName(deleteIndicatorField.orElseThrow(IllegalStateException::new)).datasetRef(tempX1.datasetReference()).build(), NumericalValue.of(0L));

        Selection selectXY1 = Selection.builder().source(joinXY1).addAllFields(selectXY1Fields).condition(selectXY1Condition).groupByFields(selectXY1GroupByFields).alias(LEFT_DATASET_IN_JOIN_ALIAS).build();

        // Second Left Join to extract the correct end_date
        Selection selectX2 = selectXY1;
        Dataset tempY2 = tempDatasetWithDeleteIndicator.datasetReference().withAlias(RIGHT_DATASET_IN_JOIN_ALIAS);

        Condition x2AndY2PkMatchCondition = LogicalPlanUtils.getPrimaryKeyMatchCondition(selectX2, tempY2, primaryKeys.toArray(new String[0]));
        Condition y2ThroughGreaterThanX2From = GreaterThan.of(targetValidDatetimeThru.withDatasetRef(tempY2.datasetReference()), validDateTimeFrom.withDatasetRef(selectX2.datasetReference()));
        Condition y2ThroughLessThanEqualsToX2Through = LessThanEqualTo.of(targetValidDatetimeThru.withDatasetRef(tempY2.datasetReference()), validDateTimeThru.withDatasetRef(selectX2.datasetReference()));
        Condition y2DeleteIndicatorIsNotZero = NotEquals.of(FieldValue.builder().fieldName(deleteIndicatorField.get()).datasetRef(tempY2.datasetReference()).build(), NumericalValue.of(0L));
        Condition joinXY2Condition = And.builder().addConditions(x2AndY2PkMatchCondition, y2ThroughGreaterThanX2From, y2ThroughLessThanEqualsToX2Through, y2DeleteIndicatorIsNotZero).build();
        Join joinXY2 = Join.of(selectX2, tempY2, joinXY2Condition, JoinOperation.LEFT_OUTER_JOIN);

        List<Value> selectXY2Fields = new ArrayList<>();
        selectXY2Fields.addAll(primaryKeyFields.stream().map(field -> field.withDatasetRef(selectX2.datasetReference())).collect(Collectors.toList()));
        selectXY2Fields.addAll(dataFields.stream().map(field -> field.withDatasetRef(selectX2.datasetReference())).collect(Collectors.toList()));
        selectXY2Fields.add(digest.withDatasetRef(selectX2.datasetReference()));
        selectXY2Fields.add(validDateTimeFrom.withDatasetRef(selectX2.datasetReference()).withAlias(VALID_DATE_TIME_FROM_NAME));
        selectXY2Fields.add(FunctionImpl.builder().functionName(FunctionName.MAX).addValue(targetValidDatetimeThru.withDatasetRef(tempY2.datasetReference())).alias(VALID_DATE_TIME_THRU_NAME).build());
        selectXY2Fields.addAll(transactionMilestoningFields().stream().map(field -> field.withDatasetRef(selectX2.datasetReference())).collect(Collectors.toList()));

        List<Value> selectXY2GroupByFields = new ArrayList<>();
        selectXY2GroupByFields.addAll(primaryKeyFields.stream().map(field -> field.withDatasetRef(selectX2.datasetReference())).collect(Collectors.toList()));
        selectXY2GroupByFields.addAll(dataFields.stream().map(field -> field.withDatasetRef(selectX2.datasetReference())).collect(Collectors.toList()));
        selectXY2GroupByFields.add(digest.withDatasetRef(selectX2.datasetReference()));
        selectXY2GroupByFields.add(validDateTimeFrom.withDatasetRef(selectX2.datasetReference()));
        selectXY2GroupByFields.addAll(transactionMilestoningFields().stream().map(field -> field.withDatasetRef(selectX2.datasetReference())).collect(Collectors.toList()));

        Selection selectXY2 = Selection.builder().source(joinXY2).addAllFields(selectXY2Fields).groupByFields(selectXY2GroupByFields).build();

        // Final insertion into main table
        List<Value> insertFields = new ArrayList<>();
        insertFields.addAll(primaryKeyFields);
        insertFields.addAll(dataFields);
        insertFields.add(digest);
        insertFields.add(targetValidDatetimeFrom);
        insertFields.add(targetValidDatetimeThru);
        insertFields.addAll(transactionMilestoningFields());

        return Insert.builder().targetDataset(mainDataset()).sourceDataset(selectXY2).addAllFields(insertFields).build();
    }
}