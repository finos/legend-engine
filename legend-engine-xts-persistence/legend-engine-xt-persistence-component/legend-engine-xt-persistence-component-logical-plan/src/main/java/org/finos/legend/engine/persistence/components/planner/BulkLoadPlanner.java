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

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.Resources;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.BulkLoad;
import org.finos.legend.engine.persistence.components.ingestmode.audit.AuditingVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.digest.DigestGenerationHandler;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.ExternalDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetAdditionalProperties;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.TableType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesSelection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Drop;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.values.BulkLoadBatchStatusValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.logicalplan.values.All;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDataset;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Copy;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;
import org.finos.legend.engine.persistence.components.util.TableNameGenUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_INSERTED;
import static org.finos.legend.engine.persistence.components.util.TableNameGenUtils.TEMP_DATASET_ALIAS;
import static org.finos.legend.engine.persistence.components.util.TableNameGenUtils.TEMP_DATASET_QUALIFIER;

class BulkLoadPlanner extends Planner
{

    private boolean transformWhileCopy;
    private Dataset externalDataset;
    private StagedFilesDataset stagedFilesDataset;

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

    @Override
    public LogicalPlan buildLogicalPlanForDryRun(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();
        if (capabilities.contains(Capability.DRY_RUN) && stagedFilesDataset.stagedFilesDatasetProperties().dryRunSupported())
        {
            Dataset validationDataset = getValidationDataset();
            Copy copy = Copy.builder()
                    .targetDataset(validationDataset)
                    .sourceDataset(stagedFilesDataset.datasetReference().withAlias(""))
                    .stagedFilesDatasetProperties(stagedFilesDataset.stagedFilesDatasetProperties())
                    .dryRun(true)
                    .build();
            operations.add(copy);
        }
        return LogicalPlan.of(operations);
    }

    private LogicalPlan buildLogicalPlanForTransformWhileCopy(Resources resources)
    {
        List<Value> fieldsToSelect = LogicalPlanUtils.extractStagedFilesFieldValues(stagingDataset());
        List<Value> fieldsToInsert = new ArrayList<>(stagingDataset().schemaReference().fieldValues());

        // Add digest
        ingestMode().digestGenStrategy().accept(new DigestGenerationHandler(mainDataset(), fieldsToSelect, fieldsToInsert));

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
        ingestMode().digestGenStrategy().accept(new DigestGenerationHandler(mainDataset(), fieldsToSelect, fieldsToInsertIntoMain));

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
        operations.add(Create.of(true, mainDataset()));
        operations.add(Create.of(true, metadataDataset().orElseThrow(IllegalStateException::new).get()));
        if (!transformWhileCopy)
        {
            operations.add(Create.of(false, externalDataset));
        }
        return LogicalPlan.of(operations);
    }

    @Override
    public LogicalPlan buildLogicalPlanForDryRunPreActions(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();
        if (capabilities.contains(Capability.DRY_RUN) && stagedFilesDataset.stagedFilesDatasetProperties().dryRunSupported())
        {
            operations.add(Create.of(true, getValidationDataset()));
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

    private Dataset getValidationDataset()
    {
        String tableName = mainDataset().datasetReference().name().orElseThrow((IllegalStateException::new));
        String validationDatasetName = TableNameGenUtils.generateTableName(tableName, "validation", options().ingestRunId());
        return DatasetDefinition.builder()
                .schema(stagedFilesDataset.schema())
                .database(mainDataset().datasetReference().database())
                .group(mainDataset().datasetReference().group())
                .name(validationDatasetName)
                .datasetAdditionalProperties(DatasetAdditionalProperties.builder().tableType(TableType.TEMPORARY).build())
                .build();
    }
}
