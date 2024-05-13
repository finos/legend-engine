// Copyright 2023 Goldman Sachs
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

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.Resources;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.BulkLoad;
import org.finos.legend.engine.persistence.components.ingestmode.audit.AuditingVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.digest.DigestGenerationHandler;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.And;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.IsNull;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Not;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Or;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.ExternalDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesSelection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Delete;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Drop;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.values.BulkLoadBatchStatusValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.logicalplan.values.All;
import org.finos.legend.engine.persistence.components.logicalplan.values.MetadataFileNameField;
import org.finos.legend.engine.persistence.components.logicalplan.values.MetadataRowNumberField;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDataset;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Copy;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.logicalplan.values.TryCastFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.util.ValidationCategory;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;
import org.finos.legend.engine.persistence.components.util.TableNameGenUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_INSERTED;
import static org.finos.legend.engine.persistence.components.util.TableNameGenUtils.TEMP_DATASET_ALIAS;
import static org.finos.legend.engine.persistence.components.util.TableNameGenUtils.TEMP_DATASET_QUALIFIER;

class BulkLoadPlanner extends Planner
{

    private boolean transformWhileCopy;
    private Dataset externalDataset;
    private Dataset validationDataset;
    private StagedFilesDataset stagedFilesDataset;

    private static final String FILE = "legend_persistence_file";
    private static final String ROW_NUMBER = "legend_persistence_row_number";

    BulkLoadPlanner(Datasets datasets, BulkLoad ingestMode, PlannerOptions plannerOptions, Set<Capability> capabilities)
    {
        super(datasets, ingestMode, plannerOptions, capabilities);

        // validation
        validateNoPrimaryKeysInStageAndMain();
        if (!(datasets.stagingDataset() instanceof StagedFilesDataset))
        {
            throw new IllegalArgumentException("Only StagedFilesDataset are allowed under Bulk Load");
        }

        stagedFilesDataset = (StagedFilesDataset) datasets.stagingDataset();

        transformWhileCopy = capabilities.contains(Capability.TRANSFORM_WHILE_COPY);
        if (!transformWhileCopy)
        {
            String externalDatasetName = TableNameGenUtils.generateTableName(datasets.mainDataset().datasetReference().name().orElseThrow((IllegalStateException::new)), TEMP_DATASET_QUALIFIER, options().ingestRunId());
            externalDataset = ExternalDataset.builder()
                .stagedFilesDataset(stagedFilesDataset)
                .database(datasets.mainDataset().datasetReference().database())
                .group(datasets.mainDataset().datasetReference().group())
                .name(externalDatasetName)
                .alias(TEMP_DATASET_ALIAS)
                .build();
        }

        if (capabilities.contains(Capability.DRY_RUN))
        {
            validationDataset = stagedFilesDataset.stagedFilesDatasetProperties().validationModeSupported() ? getValidationModeDataset() : getGenericValidationDataset();
        }
    }

    private void validateNoPrimaryKeysInStageAndMain()
    {
        List<String> primaryKeysFromMain = mainDataset().schema().fields().stream().filter(Field::primaryKey).map(Field::name).collect(Collectors.toList());
        validatePrimaryKeysIsEmpty(primaryKeysFromMain);

        List<String> primaryKeysFromStage = stagingDataset().schema().fields().stream().filter(Field::primaryKey).map(Field::name).collect(Collectors.toList());
        validatePrimaryKeysIsEmpty(primaryKeysFromStage);
    }

    @Override
    protected BulkLoad ingestMode()
    {
        return (BulkLoad) super.ingestMode();
    }

    @Override
    public LogicalPlan buildLogicalPlanForIngest(Resources resources)
    {
        if (transformWhileCopy)
        {
            return buildLogicalPlanForTransformWhileCopy(resources);
        }
        else
        {
            return buildLogicalPlanForCopyAndTransform(resources);
        }
    }

    /*
    ------------------
    Validation Mode Logic:
    ------------------
    COPY INTO temp_table (data_columns)
    SELECT data_columns from staging
    WITH VALIDATION_MODE = true

    ------------------
    Generic Approach Logic:
    ------------------
    modified_data_columns: nullable data_columns with String datatype
    meta_columns: file_name, row_number

    COPY INTO temp_table (modified_data_columns, meta_columns)
    SELECT modified_data_columns, meta_columns from staging
     */
    @Override
    public LogicalPlan buildLogicalPlanForDryRun(Resources resources)
    {
        if (!capabilities.contains(Capability.DRY_RUN))
        {
            return LogicalPlan.of(Collections.emptyList());
        }

        List<Operation> operations = new ArrayList<>();

        if (stagedFilesDataset.stagedFilesDatasetProperties().validationModeSupported())
        {
            Copy copy = Copy.builder()
                .targetDataset(validationDataset)
                .sourceDataset(stagedFilesDataset.datasetReference().withAlias(""))
                .stagedFilesDatasetProperties(stagedFilesDataset.stagedFilesDatasetProperties())
                .validationMode(true)
                .build();
            operations.add(copy);
        }
        else
        {
            operations.add(Delete.builder().dataset(validationDataset).build());

            List<Value> fieldsToSelect = LogicalPlanUtils.extractStagedFilesFieldValuesWithVarCharType(stagingDataset());
            fieldsToSelect.add(MetadataFileNameField.builder().stagedFilesDatasetProperties(stagedFilesDataset.stagedFilesDatasetProperties()).build());
            fieldsToSelect.add(MetadataRowNumberField.builder().stagedFilesDatasetProperties(stagedFilesDataset.stagedFilesDatasetProperties()).build());

            List<Value> fieldsToInsert = new ArrayList<>(stagingDataset().schemaReference().fieldValues());
            fieldsToInsert.add(FieldValue.builder().fieldName(FILE).datasetRef(stagingDataset().datasetReference()).build());
            fieldsToInsert.add(FieldValue.builder().fieldName(ROW_NUMBER).datasetRef(stagingDataset().datasetReference()).build());

            Dataset selectStage = StagedFilesSelection.builder().source(stagedFilesDataset).addAllFields(fieldsToSelect).build();

            Copy copy = Copy.builder()
                .targetDataset(validationDataset)
                .sourceDataset(selectStage)
                .addAllFields(fieldsToInsert)
                .stagedFilesDatasetProperties(stagedFilesDataset.stagedFilesDatasetProperties())
                .validationMode(false)
                .build();
            operations.add(copy);
        }
        return LogicalPlan.of(operations);
    }

    /*
    ------------------
    Validation Mode Logic:
    ------------------
    NOT APPLICABLE

    ------------------
    Generic Approach Logic:
    ------------------
    For null values:
    SELECT * FROM temp_table WHERE
        (non_nullable_data_column_1 = NULL
        OR non_nullable_data_column_2 = NULL
        OR ...)

    For datatype conversion:
    SELECT * FROM temp_table WHERE (non_string_data_column_1 != NULL AND TRY_CAST(non_string_data_column_1 AS datatype) = NULL)
    SELECT * FROM temp_table WHERE (non_string_data_column_2 != NULL AND TRY_CAST(non_string_data_column_2 AS datatype) = NULL)
    ...
     */
    public Map<ValidationCategory, List<Pair<Set<FieldValue>, LogicalPlan>>> buildLogicalPlanForDryRunValidation(Resources resources)
    {
        if (!capabilities.contains(Capability.DRY_RUN) || stagedFilesDataset.stagedFilesDatasetProperties().validationModeSupported())
        {
            return Collections.emptyMap();
        }
        Map<ValidationCategory, List<Pair<Set<FieldValue>, LogicalPlan>>> validationMap = new HashMap<>();
        List<Field> fieldsToCheckForNull = stagingDataset().schema().fields().stream().filter(field -> !field.nullable()).collect(Collectors.toList());
        List<Field> fieldsToCheckForDatatype = stagingDataset().schema().fields().stream().filter(field -> !DataType.isStringDatatype(field.type().dataType())).collect(Collectors.toList());

        if (!fieldsToCheckForNull.isEmpty())
        {
            Selection queryForNull = Selection.builder()
                .source(validationDataset)
                .condition(Or.of(fieldsToCheckForNull.stream().map(field ->
                        IsNull.of(FieldValue.builder().fieldName(field.name()).datasetRef(validationDataset.datasetReference()).build()))
                    .collect(Collectors.toList())))
                .limit(options().sampleRowCount())
                .build();

            validationMap.put(ValidationCategory.NULL_VALUE,
                Collections.singletonList(Tuples.pair(fieldsToCheckForNull.stream().map(field -> FieldValue.builder().fieldName(field.name()).datasetRef(validationDataset.datasetReference()).build()).collect(Collectors.toSet()),
                    LogicalPlan.of(Collections.singletonList(queryForNull)))));
        }

        if (!fieldsToCheckForDatatype.isEmpty())
        {
            validationMap.put(ValidationCategory.TYPE_CONVERSION, new ArrayList<>());

            for (Field fieldToCheckForDatatype : fieldsToCheckForDatatype)
            {
                Selection queryForDatatype = Selection.builder()
                    .source(validationDataset)
                    .condition(And.builder()
                        .addConditions(Not.of(IsNull.of(FieldValue.builder().fieldName(fieldToCheckForDatatype.name()).datasetRef(validationDataset.datasetReference()).build())))
                        .addConditions(IsNull.of(TryCastFunction.of(FieldValue.builder().fieldName(fieldToCheckForDatatype.name()).datasetRef(validationDataset.datasetReference()).build(), fieldToCheckForDatatype.type())))
                        .build())
                    .limit(options().sampleRowCount())
                    .build();

                validationMap.get(ValidationCategory.TYPE_CONVERSION).add(Tuples.pair(Stream.of(fieldToCheckForDatatype).map(field -> FieldValue.builder().fieldName(field.name()).datasetRef(validationDataset.datasetReference()).build()).collect(Collectors.toSet()),
                    LogicalPlan.of(Collections.singletonList(queryForDatatype))));
            }
        }

        return validationMap;
    }

    private LogicalPlan buildLogicalPlanForTransformWhileCopy(Resources resources)
    {
        List<Value> fieldsToSelect = LogicalPlanUtils.extractStagedFilesFieldValues(stagingDataset());
        List<Value> fieldsToInsert = new ArrayList<>(stagingDataset().schemaReference().fieldValues());

        // Add digest
        List<FieldType> fieldTypes = stagingDataset().schema().fields().stream().map(field -> field.type()).collect(Collectors.toList());
        ingestMode().digestGenStrategy().accept(new DigestGenerationHandler(mainDataset(), fieldsToSelect, fieldsToInsert, fieldTypes));

        // Add batch_id field
        fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(ingestMode().batchIdField()).build());
        fieldsToSelect.add(metadataUtils.getBatchId(StringValue.of(mainDataset().datasetReference().name().orElseThrow(IllegalStateException::new))));

        // Add auditing
        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            addAuditing(fieldsToInsert, fieldsToSelect);
        }

        Dataset selectStage = StagedFilesSelection.builder().source(stagedFilesDataset).addAllFields(fieldsToSelect).build();
        return LogicalPlan.of(Collections.singletonList(
                Copy.builder()
                        .targetDataset(mainDataset())
                        .sourceDataset(selectStage)
                        .addAllFields(fieldsToInsert)
                        .stagedFilesDatasetProperties(stagedFilesDataset.stagedFilesDatasetProperties())
                        .build()));
    }

    private LogicalPlan buildLogicalPlanForCopyAndTransform(Resources resources)
    {
        List<Value> fieldsToSelect = new ArrayList<>(externalDataset.schemaReference().fieldValues());
        List<Value> fieldsToInsertIntoMain = new ArrayList<>(externalDataset.schemaReference().fieldValues());

        // Add digest
        List<FieldType> fieldTypes = externalDataset.schema().fields().stream().map(field -> field.type()).collect(Collectors.toList());
        ingestMode().digestGenStrategy().accept(new DigestGenerationHandler(mainDataset(), fieldsToSelect, fieldsToInsertIntoMain, fieldTypes));

        // Add batch_id field
        fieldsToInsertIntoMain.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(ingestMode().batchIdField()).build());
        fieldsToSelect.add(metadataUtils.getBatchId(StringValue.of(mainDataset().datasetReference().name().orElseThrow(IllegalStateException::new))));

        // Add auditing
        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            addAuditing(fieldsToInsertIntoMain, fieldsToSelect);
        }

        return LogicalPlan.of(Collections.singletonList(Insert.of(mainDataset(), Selection.builder().source(externalDataset).addAllFields(fieldsToSelect).build(), fieldsToInsertIntoMain)));
    }

    private void addAuditing(List<Value> fieldsToInsert, List<Value> fieldsToSelect)
    {
        BatchStartTimestamp batchStartTimestamp = BatchStartTimestamp.INSTANCE;
        String auditField = ingestMode().auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).orElseThrow(IllegalStateException::new);
        fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(auditField).build());
        fieldsToSelect.add(batchStartTimestamp);
    }

    @Override
    public LogicalPlan buildLogicalPlanForPreActions(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();
        if (!options().skipMainAndMetadataDatasetCreation())
        {
            operations.add(Create.of(true, mainDataset()));
            operations.add(Create.of(true, metadataDataset().orElseThrow(IllegalStateException::new).get()));
        }
        if (!transformWhileCopy)
        {
            operations.add(Create.of(false, externalDataset));
        }
        if (options().enableConcurrentSafety())
        {
            operations.add(Create.of(true, lockInfoDataset().orElseThrow(IllegalStateException::new).get()));
        }
        return LogicalPlan.of(operations);
    }

    @Override
    public LogicalPlan buildLogicalPlanForDryRunPreActions(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();
        if (capabilities.contains(Capability.DRY_RUN))
        {
            operations.add(Create.of(true, validationDataset));
        }
        return LogicalPlan.of(operations);
    }

    @Override
    public LogicalPlan buildLogicalPlanForPostActions(Resources resources)
    {
        // there is no need to delete from the temp table for big query because we always use "create or replace"
        List<Operation> operations = new ArrayList<>();
        return LogicalPlan.of(operations);
    }

    @Override
    public LogicalPlan buildLogicalPlanForPostCleanup(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();
        if (!transformWhileCopy)
        {
            operations.add(Drop.of(true, externalDataset, false));
        }
        return LogicalPlan.of(operations);
    }

    @Override
    public LogicalPlan buildLogicalPlanForDryRunPostCleanup(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();
        if (capabilities.contains(Capability.DRY_RUN))
        {
            operations.add(Drop.of(true, validationDataset, false));
        }
        return LogicalPlan.of(operations);
    }

    @Override
    List<String> getDigestOrRemainingColumns()
    {
        return Collections.emptyList();
    }

    @Override
    public LogicalPlan buildLogicalPlanForMetadataIngest(Resources resources)
    {
        // Save file paths/patterns and event id into batch_source_info column
        Map<String, Object> batchSourceInfoMap = LogicalPlanUtils.jsonifyBulkLoadSourceInfo(stagedFilesDataset.stagedFilesDatasetProperties(), options().bulkLoadEventIdValue());
        Optional<StringValue> batchSourceInfo = LogicalPlanUtils.getStringValueFromMap(batchSourceInfoMap);

        // Save additional metadata into additional_metadata column
        Optional<StringValue> additionalMetadata = LogicalPlanUtils.getStringValueFromMap(options().additionalMetadata());

        return LogicalPlan.of(Arrays.asList(metadataUtils.insertMetaData(mainTableName, batchStartTimestamp, batchEndTimestamp, BulkLoadBatchStatusValue.INSTANCE, batchSourceInfo, additionalMetadata)));
    }

    @Override
    public void addPostRunStatsForRowsInserted(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        // Rows inserted = rows in main with batch_id column equals latest batch id value
        List<Value> fields = Collections.singletonList(FunctionImpl.builder().functionName(FunctionName.COUNT).addValue(All.INSTANCE).alias(ROWS_INSERTED.get()).build());
        Condition condition = LogicalPlanUtils.getBatchIdEqualityCondition(mainDataset(), metadataUtils.getBatchId(mainTableName), ingestMode().batchIdField());
        Selection selection = Selection.builder().source(mainDataset().datasetReference()).condition(condition).addAllFields(fields).build();

        postRunStatisticsResult.put(ROWS_INSERTED, LogicalPlan.builder().addOps(selection).build());
    }

    @Override
    protected void addPostRunStatsForIncomingRecords(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        // Not supported at the moment
    }

    @Override
    protected void addPostRunStatsForRowsUpdated(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        // Not supported at the moment
    }

    @Override
    protected void addPostRunStatsForRowsTerminated(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        // Not supported at the moment
    }

    @Override
    protected void addPostRunStatsForRowsDeleted(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        // Not supported at the moment
    }

    private Dataset getValidationModeDataset()
    {
        String tableName = mainDataset().datasetReference().name().orElseThrow((IllegalStateException::new));
        String validationDatasetName = TableNameGenUtils.generateTableName(tableName, "validation", options().ingestRunId());
        return DatasetDefinition.builder()
                .schema(stagedFilesDataset.schema())
                .database(mainDataset().datasetReference().database())
                .group(mainDataset().datasetReference().group())
                .name(validationDatasetName)
                .build();
    }

    private Dataset getGenericValidationDataset()
    {
        String tableName = mainDataset().datasetReference().name().orElseThrow((IllegalStateException::new));
        String validationDatasetName = TableNameGenUtils.generateTableName(tableName, "validation", options().ingestRunId());

        List<Field> fields = stagedFilesDataset.schema().fields().stream().map(field -> field.withType(FieldType.builder().dataType(DataType.VARCHAR).build()).withNullable(true)).collect(Collectors.toList());
        fields.add(Field.builder().name(FILE).type(FieldType.builder().dataType(DataType.VARCHAR).build()).build());
        fields.add(Field.builder().name(ROW_NUMBER).type(FieldType.builder().dataType(DataType.BIGINT).build()).build());

        return DatasetDefinition.builder()
            .schema(stagedFilesDataset.schema().withFields(fields))
            .database(mainDataset().datasetReference().database())
            .group(mainDataset().datasetReference().group())
            .name(validationDatasetName)
            .build();
    }
}
