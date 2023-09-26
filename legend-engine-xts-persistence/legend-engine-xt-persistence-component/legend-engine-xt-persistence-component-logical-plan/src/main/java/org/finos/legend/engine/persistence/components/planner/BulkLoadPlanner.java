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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.Resources;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.BulkLoad;
import org.finos.legend.engine.persistence.components.ingestmode.audit.AuditingVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.digest.DigestGenStrategyVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.digest.NoDigestGenStrategyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UDFBasedDigestGenStrategyAbstract;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesSelection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Delete;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Drop;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.logicalplan.values.All;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.StagedFilesDatasetProperties;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Copy;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.values.DigestUdf;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.BulkLoadBatchIdValue;
import org.finos.legend.engine.persistence.components.util.BulkLoadMetadataDataset;
import org.finos.legend.engine.persistence.components.util.BulkLoadMetadataUtils;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_INSERTED;
import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.TEMP_DATASET_BASE_NAME;
import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.UNDERSCORE;

class BulkLoadPlanner extends Planner
{

    private boolean allowExtraFieldsWhileCopying;
    private Dataset tempDataset;
    private StagedFilesDataset stagedFilesDataset;
    private BulkLoadMetadataDataset bulkLoadMetadataDataset;

    BulkLoadPlanner(Datasets datasets, BulkLoad ingestMode, PlannerOptions plannerOptions, Set<Capability> capabilities)
    {
        super(datasets, ingestMode, plannerOptions, capabilities);

        // validation
        if (!(datasets.stagingDataset() instanceof StagedFilesDataset))
        {
            throw new IllegalArgumentException("Only StagedFilesDataset are allowed under Bulk Load");
        }

        stagedFilesDataset = (StagedFilesDataset) datasets.stagingDataset();
        bulkLoadMetadataDataset = bulkLoadMetadataDataset().orElseThrow(IllegalStateException::new);

        allowExtraFieldsWhileCopying = capabilities.contains(Capability.ALLOW_EXTRA_FIELDS_WHILE_COPYING);
        if (!allowExtraFieldsWhileCopying)
        {
            tempDataset = datasets.tempDataset().orElse(DatasetDefinition.builder()
                .schema(datasets.stagingDataset().schema())
                .database(datasets.mainDataset().datasetReference().database())
                .group(datasets.mainDataset().datasetReference().group())
                .name(datasets.mainDataset().datasetReference().name().orElseThrow((IllegalStateException::new)) + UNDERSCORE + TEMP_DATASET_BASE_NAME)
                .alias(TEMP_DATASET_BASE_NAME)
                .build());
        }
    }

    @Override
    protected BulkLoad ingestMode()
    {
        return (BulkLoad) super.ingestMode();
    }

    @Override
    public LogicalPlan buildLogicalPlanForIngest(Resources resources)
    {
        if (allowExtraFieldsWhileCopying)
        {
            return buildLogicalPlanForIngestUsingCopy(resources);
        }
        else
        {
            return buildLogicalPlanForIngestUsingCopyAndInsert(resources);
        }
    }

    private LogicalPlan buildLogicalPlanForIngestUsingCopy(Resources resources)
    {
        List<Value> fieldsToSelect = LogicalPlanUtils.extractStagedFilesFieldValues(stagingDataset());
        List<Value> fieldsToInsert = new ArrayList<>(stagingDataset().schemaReference().fieldValues());

        // Add digest
        ingestMode().digestGenStrategy().accept(new DigestGeneration(mainDataset(), stagingDataset(), fieldsToSelect, fieldsToInsert));

        // Add batch_id field
        fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(ingestMode().batchIdField()).build());
        fieldsToSelect.add(BulkLoadBatchIdValue.INSTANCE);

        // Add auditing
        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            addAuditing(fieldsToInsert, fieldsToSelect);
        }

        Dataset selectStage = StagedFilesSelection.builder().source(stagedFilesDataset).addAllFields(fieldsToSelect).build();
        return LogicalPlan.of(Collections.singletonList(Copy.of(mainDataset(), selectStage, fieldsToInsert)));
    }

    private LogicalPlan buildLogicalPlanForIngestUsingCopyAndInsert(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();


        // Operation 1: Copy into a temp table
        List<Value> fieldsToSelectFromStage = LogicalPlanUtils.extractStagedFilesFieldValues(stagingDataset());
        Dataset selectStage = StagedFilesSelection.builder().source(stagedFilesDataset).addAllFields(fieldsToSelectFromStage).build();
        operations.add(Copy.of(tempDataset, selectStage, fieldsToSelectFromStage));


        // Operation 2: Transfer from temp table into target table, adding extra columns at the same time
        List<Value> fieldsToSelectFromTemp = new ArrayList<>(tempDataset.schemaReference().fieldValues());
        List<Value> fieldsToInsertIntoMain = new ArrayList<>(tempDataset.schemaReference().fieldValues());

        // Add digest
        ingestMode().digestGenStrategy().accept(new DigestGeneration(mainDataset(), stagingDataset(), tempDataset, fieldsToSelectFromTemp, fieldsToInsertIntoMain));

        // Add batch_id field
        fieldsToInsertIntoMain.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(ingestMode().batchIdField()).build());
        fieldsToSelectFromTemp.add(BulkLoadBatchIdValue.INSTANCE);

        // Add auditing
        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            addAuditing(fieldsToInsertIntoMain, fieldsToSelectFromTemp);
        }

        operations.add(Insert.of(mainDataset(), Selection.builder().source(tempDataset).addAllFields(fieldsToSelectFromTemp).build(), fieldsToInsertIntoMain));


        return LogicalPlan.of(operations);
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
        operations.add(Create.of(true, bulkLoadMetadataDataset.get()));
        if (!allowExtraFieldsWhileCopying)
        {
            operations.add(Create.of(true, tempDataset));
        }
        return LogicalPlan.of(operations);
    }

    @Override
    public LogicalPlan buildLogicalPlanForPostActions(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();
        if (!allowExtraFieldsWhileCopying)
        {
            operations.add(Delete.builder().dataset(tempDataset).build());
        }
        return LogicalPlan.of(operations);
    }

    @Override
    public LogicalPlan buildLogicalPlanForPostCleanup(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();
        if (!allowExtraFieldsWhileCopying)
        {
            operations.add(Drop.of(true, tempDataset, true));
        }
        return LogicalPlan.of(operations);
    }

    @Override
    public LogicalPlan buildLogicalPlanForMetadataIngest(Resources resources)
    {
        BulkLoadMetadataUtils bulkLoadMetadataUtils = new BulkLoadMetadataUtils(bulkLoadMetadataDataset);
        String batchSourceInfo = jsonifyBatchSourceInfo(stagedFilesDataset.stagedFilesDatasetProperties());
        StringValue datasetName = StringValue.of(mainDataset().datasetReference().name());
        Insert insertMetaData = bulkLoadMetadataUtils.insertMetaData(datasetName, StringValue.of(batchSourceInfo));
        return LogicalPlan.of(Arrays.asList(insertMetaData));
    }

    @Override
    public void addPostRunStatsForRowsInserted(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        // Only supported if Audit enabled
        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            // Rows inserted = rows in main with audit column equals latest timestamp
            String auditField = ingestMode().auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).orElseThrow(IllegalStateException::new);
            postRunStatisticsResult.put(ROWS_INSERTED, LogicalPlan.builder()
                    .addOps(getRowsBasedOnAppendTimestamp(mainDataset(), auditField, ROWS_INSERTED.get()))
                    .build());
        }
    }

    @Override
    protected void addPostRunStatsForIncomingRecords(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        // Not supported at the moment
    }

    private Selection getRowsBasedOnAppendTimestamp(Dataset dataset, String field, String alias)
    {
        FieldValue fieldValue = FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName(field).build();
        Equals condition = Equals.of(fieldValue, BatchStartTimestamp.INSTANCE);
        FunctionImpl countFunction = FunctionImpl.builder().functionName(FunctionName.COUNT).addValue(All.INSTANCE).alias(alias).build();
        return Selection.builder().source(dataset.datasetReference()).condition(condition).addFields(countFunction).build();
    }

    private String jsonifyBatchSourceInfo(StagedFilesDatasetProperties stagedFilesDatasetProperties)
    {
        List<String> files = stagedFilesDatasetProperties.files();
        Map<String, Object> batchSourceMap = new HashMap();
        batchSourceMap.put("files", files);
        ObjectMapper objectMapper = new ObjectMapper();
        try
        {
            return objectMapper.writeValueAsString(batchSourceMap);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }


    static class DigestGeneration implements DigestGenStrategyVisitor<Void>
    {
        private List<Value> fieldsToSelect;
        private List<Value> fieldsToInsert;
        private Dataset stagingDataset;
        private Dataset mainDataset;
        private Optional<Dataset> tempDataset;

        public DigestGeneration(Dataset mainDataset, Dataset stagingDataset, List<Value> fieldsToSelect, List<Value> fieldsToInsert)
        {
            this.mainDataset = mainDataset;
            this.stagingDataset = stagingDataset;
            this.tempDataset = Optional.empty();
            this.fieldsToSelect = fieldsToSelect;
            this.fieldsToInsert = fieldsToInsert;
        }

        public DigestGeneration(Dataset mainDataset, Dataset stagingDataset, Dataset tempDataset, List<Value> fieldsToSelect, List<Value> fieldsToInsert)
        {
            this.mainDataset = mainDataset;
            this.stagingDataset = stagingDataset;
            this.tempDataset = Optional.of(tempDataset);
            this.fieldsToSelect = fieldsToSelect;
            this.fieldsToInsert = fieldsToInsert;
        }

        @Override
        public Void visitNoDigestGenStrategy(NoDigestGenStrategyAbstract noDigestGenStrategy)
        {
            return null;
        }

        @Override
        public Void visitUDFBasedDigestGenStrategy(UDFBasedDigestGenStrategyAbstract udfBasedDigestGenStrategy)
        {
            DigestUdf.Builder digestValueBuilder = DigestUdf
                    .builder()
                    .udfName(udfBasedDigestGenStrategy.digestUdfName())
                    .addAllFieldNames(stagingDataset.schemaReference().fieldValues().stream().map(fieldValue -> fieldValue.fieldName()).collect(Collectors.toList()))
                    .addAllValues(fieldsToSelect);
            tempDataset.ifPresent(digestValueBuilder::dataset);
            Value digestValue = digestValueBuilder.build();
            String digestField = udfBasedDigestGenStrategy.digestField();
            fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset.datasetReference()).fieldName(digestField).build());
            fieldsToSelect.add(digestValue);
            return null;
        }
    }

}
