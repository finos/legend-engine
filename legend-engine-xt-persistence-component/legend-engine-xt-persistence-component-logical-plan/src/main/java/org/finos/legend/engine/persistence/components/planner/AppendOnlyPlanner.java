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
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.audit.AuditingVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.AllowDuplicatesAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.DeduplicationStrategyVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.DeduplicationStrategyVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicatesAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicatesAbstract;
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
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
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

import static org.finos.legend.engine.persistence.components.common.StatisticName.INCOMING_RECORD_COUNT;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_DELETED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_INSERTED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_TERMINATED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_UPDATED;
import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.ALL_COLUMNS;
import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.getDigestMatchCondition;
import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.getPrimaryKeyMatchCondition;

class AppendOnlyPlanner extends Planner
{
    private final Optional<Condition> dataSplitInRangeCondition;

    AppendOnlyPlanner(Datasets datasets, AppendOnly ingestMode, PlannerOptions plannerOptions)
    {
        super(datasets, ingestMode, plannerOptions);

        // validate
        ingestMode.deduplicationStrategy().accept(new DeduplicationStrategyVisitor<Void>()
        {
            @Override
            public Void visitAllowDuplicates(AllowDuplicatesAbstract allowDuplicates)
            {
                validatePrimaryKeysIsEmpty(primaryKeys);
                return null;
            }

            @Override
            public Void visitFilterDuplicates(FilterDuplicatesAbstract filterDuplicates)
            {
                validatePrimaryKeysNotEmpty(primaryKeys);
                return null;
            }

            @Override
            public Void visitFailOnDuplicates(FailOnDuplicatesAbstract failOnDuplicates)
            {
                validatePrimaryKeysNotEmpty(primaryKeys);
                return null;
            }
        });

        this.dataSplitInRangeCondition = ingestMode.dataSplitField().map(field -> LogicalPlanUtils.getDataSplitInRangeCondition(stagingDataset(), field));
    }

    @Override
    protected AppendOnly ingestMode()
    {
        return (AppendOnly) super.ingestMode();
    }

    @Override
    public LogicalPlan buildLogicalPlanForIngest(Resources resources, Set<Capability> capabilities)
    {
        List<Value> fieldsToSelect = new ArrayList<>(stagingDataset().schemaReference().fieldValues());
        List<Value> fieldsToInsert = new ArrayList<>(stagingDataset().schemaReference().fieldValues());

        if (ingestMode().dataSplitField().isPresent())
        {
            LogicalPlanUtils.removeField(fieldsToSelect, ingestMode().dataSplitField().get());
            LogicalPlanUtils.removeField(fieldsToInsert, ingestMode().dataSplitField().get());
        }

        if (ingestMode().auditing().accept(AUDIT_ENABLED))
        {
            BatchStartTimestamp batchStartTimestamp = BatchStartTimestamp.INSTANCE;
            fieldsToSelect.add(batchStartTimestamp);

            String auditField = ingestMode().auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).orElseThrow(IllegalStateException::new);
            fieldsToInsert.add(FieldValue.builder().datasetRef(mainDataset().datasetReference()).fieldName(auditField).build());
        }
        else if (!ingestMode().dataSplitField().isPresent())
        {
            fieldsToSelect = LogicalPlanUtils.ALL_COLUMNS();
        }

        Dataset selectStage;
        if (ingestMode().deduplicationStrategy().accept(DeduplicationStrategyVisitors.IS_FILTER_DUPLICATES))
        {
            Condition notExistInSinkCondition = Not.of(Exists.of(Selection.builder()
                .source(mainDataset())
                .condition(And.builder()
                    .addConditions(
                        getPrimaryKeyMatchCondition(mainDataset(), stagingDataset(), primaryKeys.toArray(new String[0])),
                        getDigestMatchCondition(mainDataset(), stagingDataset(), ingestMode().digestField().orElseThrow(IllegalStateException::new)))
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

            selectStage = Selection.builder().source(stagingDataset()).condition(selectCondition).addAllFields(fieldsToSelect).build();
        }
        else
        {
            if (ingestMode().dataSplitField().isPresent())
            {
                selectStage = Selection.builder().source(stagingDataset()).condition(dataSplitInRangeCondition).addAllFields(fieldsToSelect).build();
            }
            else
            {
                selectStage = Selection.builder().source(stagingDataset()).addAllFields(fieldsToSelect).build();
            }
        }

        List<Operation> operations = new ArrayList<>();
        // Op1: insert from staging to main
        operations.add(Insert.of(mainDataset(), selectStage, fieldsToInsert));
        return LogicalPlan.of(operations);
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

            if (ingestMode().deduplicationStrategy().accept(DeduplicationStrategyVisitors.IS_ALLOW_DUPLICATES))
            {
                postRunStatisticsResult.put(
                    ROWS_INSERTED,
                    LogicalPlan.builder().addOps(LogicalPlanUtils.getRecordCount(stagingDataset(), ROWS_INSERTED.get())).build());
            }
            else if (ingestMode().auditing().accept(AUDIT_ENABLED))
            {
                //Rows terminated = Rows invalidated in Sink - Rows updated
                postRunStatisticsResult.put(
                    ROWS_TERMINATED,
                    LogicalPlanFactory.getLogicalPlanForConstantStats(ROWS_TERMINATED.get(), 0L));

                //Rows inserted (no previous active row with same primary key) = Rows added in sink - rows updated
                String auditField = ingestMode().auditing().accept(AuditingVisitors.EXTRACT_AUDIT_FIELD).orElseThrow(IllegalStateException::new);
                postRunStatisticsResult.put(
                    ROWS_INSERTED,
                    LogicalPlan.builder()
                        .addOps(LogicalPlanUtils.getRowsBasedOnLatestTimestamp(mainDataset(), auditField, ROWS_INSERTED.get()))
                        .build());

                //Rows updated (when it is invalidated and a new row for same primary keys is added)
                postRunStatisticsResult.put(
                    ROWS_UPDATED,
                    LogicalPlanFactory.getLogicalPlanForConstantStats(ROWS_UPDATED.get(), 0L));

                //Rows Deleted = rows removed (hard-deleted) from sink table
                postRunStatisticsResult.put(
                    ROWS_DELETED,
                    LogicalPlanFactory.getLogicalPlanForConstantStats(ROWS_DELETED.get(), 0L));
            }
        }

        return postRunStatisticsResult;
    }
}
