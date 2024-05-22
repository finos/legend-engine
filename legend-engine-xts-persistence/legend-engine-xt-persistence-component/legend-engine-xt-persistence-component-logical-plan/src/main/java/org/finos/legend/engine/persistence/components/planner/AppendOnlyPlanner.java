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

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.Resources;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.audit.AuditingVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.audit.AuditingVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditingAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditingAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.digest.DigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.digest.DigestGenerationHandler;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UserProvidedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.And;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Exists;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Not;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Insert;
import org.finos.legend.engine.persistence.components.logicalplan.values.All;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_INSERTED;
import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.ALL_COLUMNS;
import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.getDigestMatchCondition;
import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.getPrimaryKeyMatchCondition;

class AppendOnlyPlanner extends Planner
{
    private final Optional<Condition> dataSplitInRangeCondition;
    private final Optional<String> userProvidedDigest;

    AppendOnlyPlanner(Datasets datasets, AppendOnly ingestMode, PlannerOptions plannerOptions, Set<Capability> capabilities)
    {
        super(datasets, ingestMode, plannerOptions, capabilities);

        // Retrieve user provided digest if present
        DigestGenStrategy digestGenStrategy = ingestMode().digestGenStrategy();
        if (digestGenStrategy instanceof UserProvidedDigestGenStrategy)
        {
            userProvidedDigest = Optional.of(((UserProvidedDigestGenStrategy) digestGenStrategy).digestField());
        }
        else
        {
            userProvidedDigest = Optional.empty();
        }

        // Validation
        // 1. If primary keys are present, then auditing must be turned on and the auditing column must be one of the primary keys
        if (!primaryKeys.isEmpty())
        {
            ingestMode.auditing().accept(new ValidateAuditingForPrimaryKeys(mainDataset()));
        }

        // 2. For filterExistingRecords, we must have (user provided) digest and primary keys to filter them
        if (ingestMode.filterExistingRecords())
        {
            if (!userProvidedDigest.isPresent() || primaryKeys.isEmpty())
            {
                throw new IllegalStateException("Primary keys and digest are mandatory for filterExistingRecords");
            }
        }

        this.dataSplitInRangeCondition = ingestMode.dataSplitField().map(field -> LogicalPlanUtils.getDataSplitInRangeCondition(stagingDataset(), field));
    }

    @Override
    protected AppendOnly ingestMode()
    {
        return (AppendOnly) super.ingestMode();
    }

    @Override
    public LogicalPlan buildLogicalPlanForIngest(Resources resources)
    {
        Pair<List<Value>, List<FieldType>> dataFieldsWithTypes = getDataFieldsWithTypes();
        List<Value> dataFields = dataFieldsWithTypes.getOne();
        List<Value> fieldsToSelect = new ArrayList<>(dataFields);
        List<Value> fieldsToInsert = new ArrayList<>(dataFields);

        // Add digest generation (if applicable)
        ingestMode().digestGenStrategy().accept(new DigestGenerationHandler(mainDataset(), fieldsToSelect, fieldsToInsert, dataFieldsWithTypes.getTwo()));

        // Add auditing (if applicable)
        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            BatchStartTimestamp batchStartTimestamp = BatchStartTimestamp.INSTANCE;
            fieldsToSelect.add(batchStartTimestamp);

            String auditField = ingestMode().auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).orElseThrow(IllegalStateException::new);
            fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(auditField).build());
        }

        // Add batch_id field
        fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(ingestMode().batchIdField()).build());
        fieldsToSelect.add(metadataUtils.getBatchId(StringValue.of(mainDataset().datasetReference().name().orElseThrow(IllegalStateException::new))));

        Dataset selectStage = ingestMode().filterExistingRecords() ? getSelectStageWithFilterExistingRecords(fieldsToSelect) : getSelectStage(fieldsToSelect);

        return LogicalPlan.of(Collections.singletonList(Insert.of(mainDataset(), selectStage, fieldsToInsert)));
    }

    @Override
    List<String> getDigestOrRemainingColumns()
    {
        List<String> remainingCols = new ArrayList<>();
        if (userProvidedDigest.isPresent())
        {
            remainingCols = Arrays.asList(userProvidedDigest.get());
        }
        else if (!primaryKeys.isEmpty())
        {
            remainingCols = getNonPKNonVersionDataFields();
        }
        return remainingCols;
    }

    private Dataset getSelectStage(List<Value> fieldsToSelect)
    {
        if (ingestMode().dataSplitField().isPresent())
        {
            return Selection.builder().source(stagingDataset()).condition(dataSplitInRangeCondition).addAllFields(fieldsToSelect).build();
        }
        else
        {
            return Selection.builder().source(stagingDataset()).addAllFields(fieldsToSelect).build();
        }
    }

    private Dataset getSelectStageWithFilterExistingRecords(List<Value> fieldsToSelect)
    {
        Condition notExistInSinkCondition = Not.of(Exists.of(Selection.builder()
            .source(mainDataset())
            .condition(And.builder()
                .addConditions(
                    getPrimaryKeyMatchCondition(mainDataset(), stagingDataset(), primaryKeys.toArray(new String[0])),
                    getDigestMatchCondition(mainDataset(), stagingDataset(), userProvidedDigest.orElseThrow(IllegalStateException::new)))
                .build())
            .addAllFields(ALL_COLUMNS())
            .build()));

        Condition selectCondition;
        if (ingestMode().dataSplitField().isPresent())
        {
            selectCondition = And.builder().addConditions(dataSplitInRangeCondition.orElseThrow(IllegalStateException::new), notExistInSinkCondition).build();
        }
        else
        {
            selectCondition = notExistInSinkCondition;
        }

        return Selection.builder().source(stagingDataset()).condition(selectCondition).addAllFields(fieldsToSelect).build();
    }

    protected void addPostRunStatsForRowsInserted(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        // Rows inserted = rows in main with batch_id column equals latest batch id value
        List<Value> fields = Collections.singletonList(FunctionImpl.builder().functionName(FunctionName.COUNT).addValue(All.INSTANCE).alias(ROWS_INSERTED.get()).build());
        Condition condition = LogicalPlanUtils.getBatchIdEqualityCondition(mainDataset(), metadataUtils.getBatchId(mainTableName), ingestMode().batchIdField());
        Selection selection = Selection.builder().source(mainDataset().datasetReference()).condition(condition).addAllFields(fields).build();

        postRunStatisticsResult.put(ROWS_INSERTED, LogicalPlan.builder().addOps(selection).build());
    }

    public Optional<Condition> getDataSplitInRangeConditionForStatistics()
    {
        return dataSplitInRangeCondition;
    }

    static class ValidateAuditingForPrimaryKeys implements AuditingVisitor<Void>
    {
        final Dataset mainDataset;

        ValidateAuditingForPrimaryKeys(Dataset mainDataset)
        {
            this.mainDataset = mainDataset;
        }

        @Override
        public Void visitNoAuditing(NoAuditingAbstract noAuditing)
        {
            throw new IllegalStateException("NoAuditing not allowed when there are primary keys");
        }

        @Override
        public Void visitDateTimeAuditing(DateTimeAuditingAbstract dateTimeAuditing)
        {
            Field dateTimeAuditingField = mainDataset.schema().fields().stream()
                .filter(field -> field.name().equalsIgnoreCase(dateTimeAuditing.dateTimeField()))
                .findFirst().orElseThrow(() -> new IllegalStateException("dateTimeField is mandatory Field for dateTimeAuditing mode"));
            if (!dateTimeAuditingField.primaryKey())
            {
                throw new IllegalStateException("auditing dateTimeField must be a primary key when there are other primary keys");
            }
            return null;
        }
    }
}
