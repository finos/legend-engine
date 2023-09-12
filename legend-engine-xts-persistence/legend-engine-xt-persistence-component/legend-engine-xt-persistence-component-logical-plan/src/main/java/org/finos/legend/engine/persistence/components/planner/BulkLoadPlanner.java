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
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
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
import org.finos.legend.engine.persistence.components.logicalplan.values.DigestUdf;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;

import java.util.*;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.common.StatisticName.INCOMING_RECORD_COUNT;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_INSERTED;

class BulkLoadPlanner extends Planner
{

    private StagedFilesDataset stagedFilesDataset;

    BulkLoadPlanner(Datasets datasets, BulkLoad ingestMode, PlannerOptions plannerOptions)
    {
        super(datasets, ingestMode, plannerOptions);

        // validation
        if (!(datasets.stagingDataset() instanceof StagedFilesDataset))
        {
            throw new IllegalArgumentException("Only StagedFilesDataset are allowed under Bulk Load");
        }

        stagedFilesDataset = (StagedFilesDataset) datasets.stagingDataset();
    }

    @Override
    protected BulkLoad ingestMode()
    {
        return (BulkLoad) super.ingestMode();
    }

    @Override
    public LogicalPlan buildLogicalPlanForIngest(Resources resources, Set<Capability> capabilities)
    {
        List<Value> fieldsToSelect = LogicalPlanUtils.extractStagedFilesFieldValues(stagingDataset());
        List<Value> fieldsToInsert = new ArrayList<>(stagingDataset().schemaReference().fieldValues());

        // Digest Generation
        if (ingestMode().generateDigest())
        {
            Value digestValue = DigestUdf
                    .builder()
                    .udfName(ingestMode().digestUdfName().orElseThrow(IllegalStateException::new))
                    .addAllFieldNames(stagingDataset().schemaReference().fieldValues().stream().map(fieldValue -> fieldValue.fieldName()).collect(Collectors.toList()))
                    .addAllValues(fieldsToSelect)
                    .build();
            String digestField = ingestMode().digestField().orElseThrow(IllegalStateException::new);
            fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(digestField).build());
            fieldsToSelect.add(digestValue);
        }

        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            BatchStartTimestamp batchStartTimestamp = BatchStartTimestamp.INSTANCE;
            fieldsToSelect.add(batchStartTimestamp);
            String auditField = ingestMode().auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).orElseThrow(IllegalStateException::new);
            fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(auditField).build());
        }

        if (ingestMode().lineageField().isPresent())
        {
            fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(ingestMode().lineageField().get()).build());
            List<String> files = stagedFilesDataset.stagedFilesDatasetProperties().files();
            String lineageValue = String.join(",", files);
            fieldsToSelect.add(StringValue.of(lineageValue));
        }


        Dataset selectStage = Selection.builder().source(stagingDataset()).addAllFields(fieldsToSelect).build();
        return LogicalPlan.of(Collections.singletonList(Copy.of(mainDataset(), selectStage, fieldsToInsert)));
    }

    @Override
    public LogicalPlan buildLogicalPlanForPreActions(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();
        operations.add(Create.of(true, mainDataset()));
        if (options().createStagingDataset())
        {
            // TODO: Check if Create Stage is needed
        }
        return LogicalPlan.of(operations);
    }

    @Override
    public LogicalPlan buildLogicalPlanForPostActions(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();
        return LogicalPlan.of(operations);
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
        // Only supported if Audit enabled
        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            // Rows inserted = rows in main with audit column equals latest timestamp
            String auditField = ingestMode().auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).orElseThrow(IllegalStateException::new);
            postRunStatisticsResult.put(INCOMING_RECORD_COUNT, LogicalPlan.builder()
                    .addOps(getRowsBasedOnAppendTimestamp(mainDataset(), auditField, INCOMING_RECORD_COUNT.get()))
                    .build());
        }
    }

    private Selection getRowsBasedOnAppendTimestamp(Dataset dataset, String field, String alias)
    {
        FieldValue fieldValue = FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName(field).build();
        Equals condition = Equals.of(fieldValue, BatchStartTimestamp.INSTANCE);
        FunctionImpl countFunction = FunctionImpl.builder().functionName(FunctionName.COUNT).addValue(All.INSTANCE).alias(alias).build();
        return Selection.builder().source(dataset.datasetReference()).condition(condition).addFields(countFunction).build();
    }
}
