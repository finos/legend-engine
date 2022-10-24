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
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTimeAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionMilestoned;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionMilestoningVisitor;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.And;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Exists;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.values.All;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchEndTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.BatchStartTimestamp;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.logicalplan.values.Pair;
import org.finos.legend.engine.persistence.components.logicalplan.values.SelectValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Value;
import org.finos.legend.engine.persistence.components.logicalplan.values.DiffBinaryValueOperator;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.finos.legend.engine.persistence.components.util.MetadataUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_INSERTED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_UPDATED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_TERMINATED;

abstract class UnitemporalPlanner extends Planner
{
    protected final MetadataUtils metadataUtils;
    protected final StringValue mainTableName;
    protected final BatchStartTimestamp batchStartTimestamp;
    protected final BatchEndTimestamp batchEndTimestamp;
    protected final Condition openRecordCondition;
    protected final Condition digestMatchCondition;
    protected final Condition digestDoesNotMatchCondition;

    protected Condition primaryKeysMatchCondition;

    UnitemporalPlanner(Datasets datasets, TransactionMilestoned transactionMilestoned, PlannerOptions plannerOptions)
    {
        super(datasets.metadataDataset().isPresent()
                ? datasets
                : datasets.withMetadataDataset(MetadataDataset.builder().build()),
            transactionMilestoned,
            plannerOptions);

        // validate
        validatePrimaryKeysNotEmpty(primaryKeys);
        validatePrimaryKey(datasets.mainDataset().schema().fields(), transactionMilestoned.transactionMilestoning().accept(FIELD_INCLUDED_IN_PRIMARY_KEY));

        // initialize parameters
        this.metadataUtils = new MetadataUtils(metadataDataset().orElseThrow(IllegalStateException::new));
        this.mainTableName = StringValue.of(mainDataset().datasetReference().name().orElseThrow(IllegalStateException::new));
        this.batchStartTimestamp = BatchStartTimestamp.INSTANCE;
        this.batchEndTimestamp = BatchEndTimestamp.INSTANCE;
        this.openRecordCondition = transactionMilestoned.transactionMilestoning().accept(new DetermineOpenRecordCondition(mainDataset()));
        this.digestMatchCondition = LogicalPlanUtils.getDigestMatchCondition(mainDataset(), stagingDataset(), transactionMilestoned.digestField());
        this.primaryKeysMatchCondition = LogicalPlanUtils.getPrimaryKeyMatchCondition(mainDataset(), stagingDataset(), primaryKeys.toArray(new String[0]));
        this.digestDoesNotMatchCondition = LogicalPlanUtils.getDigestDoesNotMatchCondition(mainDataset(), stagingDataset(), transactionMilestoned.digestField());
    }

    @Override
    protected TransactionMilestoned ingestMode()
    {
        return (TransactionMilestoned) super.ingestMode();
    }

    @Override
    public LogicalPlan buildLogicalPlanForMetadataIngest(Resources resources)
    {
        return LogicalPlan.of(Arrays.asList(metadataUtils.insertMetaData(mainTableName, batchStartTimestamp, batchEndTimestamp)));
    }

    protected void validatePrimaryKey(List<Field> fields, String targetFieldName)
    {
        Field targetField = fields.stream().filter(field -> field.name().equals(targetFieldName)).findFirst().orElse(null);
        if (targetField == null)
        {
            throw new IllegalArgumentException(String.format("Field \"%s\" does not exist", targetFieldName));
        }
        if (!targetField.primaryKey())
        {
            throw new IllegalArgumentException(String.format("Field \"%s\" must be a primary key", targetFieldName));
        }
    }

    protected void validateExistence(List<String> values, String valueToCheck, String errorMessage)
    {
        if (!values.contains(valueToCheck))
        {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    protected List<Pair<FieldValue, Value>> keyValuesForMilestoningUpdate()
    {
        return ingestMode().transactionMilestoning().accept(new DetermineMilestoningUpdateKeyValues(mainDataset(), metadataUtils, batchStartTimestamp));
    }

    protected List<FieldValue> transactionMilestoningFields()
    {
        return ingestMode().transactionMilestoning().accept(new DetermineTransactionMilestoningFields(mainDataset()));
    }

    protected List<Value> transactionMilestoningFieldValues()
    {
        return ingestMode().transactionMilestoning().accept(new DetermineTransactionMilestoningValues(metadataUtils, mainTableName, batchStartTimestamp));
    }

    protected SelectValue getRowsUpdated()
    {
        Selection selection = getRowsUpdated(null);
        return SelectValue.of(selection);
    }

    protected Selection getRowsUpdated(String alias)
    {
        Dataset sink2 = DatasetDefinition.builder()
            .database(mainDataset().datasetReference().database())
            .name(mainDataset().datasetReference().name().orElseThrow(IllegalStateException::new))
            .alias("sink2")
            .group(mainDataset().datasetReference().group())
            .schema(mainDataset().schema())
            .build();

        Condition primaryKeysMatchCondition = LogicalPlanUtils.getPrimaryKeyMatchCondition(sink2, mainDataset(), primaryKeys.toArray(new String[0]));
        Condition inCondition = ingestMode().transactionMilestoning().accept(new DetermineRowsAddedInSinkCondition(sink2, mainTableName, metadataUtils, batchStartTimestamp));
        Condition existsCondition = Exists.of(Selection.builder()
            .source(sink2)
            .condition(And.builder().addConditions(primaryKeysMatchCondition, inCondition).build())
            .addAllFields(LogicalPlanUtils.ALL_COLUMNS())
            .build());

        Condition outCondition = ingestMode().transactionMilestoning().accept(new DetermineRowsInvalidatedInSink(mainDataset(), mainTableName, metadataUtils, batchStartTimestamp));
        Condition whereCondition = And.builder().addConditions(outCondition, existsCondition).build();

        List<Value> fields = Collections.singletonList(FunctionImpl.builder().functionName(FunctionName.COUNT).addValue(All.INSTANCE).alias(alias).build());
        return Selection.builder().source(mainDataset().datasetReference()).condition(whereCondition).addAllFields(fields).build();
    }


    protected SelectValue getRowsAddedInSink()
    {
        List<Value> fields = Collections.singletonList(FunctionImpl.builder().functionName(FunctionName.COUNT).addValue(All.INSTANCE).build());
        Condition condition = ingestMode().transactionMilestoning().accept(new DetermineRowsAddedInSinkCondition(mainDataset(), mainTableName, metadataUtils, batchStartTimestamp));
        return SelectValue.of(Selection.builder().source(mainDataset().datasetReference()).condition(condition).addAllFields(fields).build());
    }

    protected SelectValue getRowsInvalidatedInSink()
    {
        List<Value> fields = Collections.singletonList(FunctionImpl.builder().functionName(FunctionName.COUNT).addValue(All.INSTANCE).build());
        Condition condition = ingestMode().transactionMilestoning().accept(new DetermineRowsInvalidatedInSink(mainDataset(), mainTableName, metadataUtils, batchStartTimestamp));
        return SelectValue.of(Selection.builder().source(mainDataset().datasetReference()).condition(condition).addAllFields(fields).build());
    }

    protected Selection getRowsInvalidatedInSink(String alias)
    {
        List<Value> fields = Collections.singletonList(FunctionImpl.builder().functionName(FunctionName.COUNT).addValue(All.INSTANCE).alias(alias).build());
        Condition condition = ingestMode().transactionMilestoning().accept(new DetermineRowsInvalidatedInSink(mainDataset(), mainTableName, metadataUtils, batchStartTimestamp));
        return Selection.builder().source(mainDataset().datasetReference()).condition(condition).addAllFields(fields).build();
    }

    // transaction milestoning visitors

    static class DetermineOpenRecordCondition implements TransactionMilestoningVisitor<Condition>
    {
        private final Dataset mainDataset;

        private DetermineOpenRecordCondition(Dataset mainDataset)
        {
            this.mainDataset = mainDataset;
        }

        @Override
        public Condition visitBatchId(BatchIdAbstract batchId)
        {
            return LogicalPlanUtils.getBatchIdEqualsInfiniteCondition(mainDataset, batchId.batchIdOutName());
        }

        @Override
        public Condition visitDateTime(TransactionDateTimeAbstract transactionDateTime)
        {
            return LogicalPlanUtils.getBatchTimeEqualsInfiniteCondition(mainDataset, transactionDateTime.dateTimeOutName());
        }

        @Override
        public Condition visitBatchIdAndDateTime(BatchIdAndDateTimeAbstract batchIdAndDateTime)
        {
            return LogicalPlanUtils.getBatchIdEqualsInfiniteCondition(mainDataset, batchIdAndDateTime.batchIdOutName());
        }
    }

    static class DetermineMilestoningUpdateKeyValues implements TransactionMilestoningVisitor<List<Pair<FieldValue, Value>>>
    {
        private final Dataset mainDataset;
        private final MetadataUtils metadataUtils;
        private final BatchStartTimestamp batchStartTimestamp;

        private DetermineMilestoningUpdateKeyValues(Dataset mainDataset, MetadataUtils metadataUtils, BatchStartTimestamp batchStartTimestamp)
        {
            this.mainDataset = mainDataset;
            this.metadataUtils = metadataUtils;
            this.batchStartTimestamp = batchStartTimestamp;
        }

        @Override
        public List<Pair<FieldValue, Value>> visitBatchId(BatchIdAbstract batchId)
        {
            List<Pair<FieldValue, Value>> keyValuesForUpdate = new ArrayList<>(1);

            keyValuesForUpdate.add(Pair.of(
                FieldValue.builder().datasetRef(mainDataset.datasetReference()).fieldName(batchId.batchIdOutName()).build(),
                metadataUtils.getPrevBatchId(StringValue.of(mainDataset.datasetReference().name().orElseThrow(IllegalStateException::new)))));

            return keyValuesForUpdate;
        }

        @Override
        public List<Pair<FieldValue, Value>> visitDateTime(TransactionDateTimeAbstract transactionDateTime)
        {
            List<Pair<FieldValue, Value>> keyValuesForUpdate = new ArrayList<>(1);

            keyValuesForUpdate.add(Pair.of(
                FieldValue.builder().datasetRef(mainDataset.datasetReference()).fieldName(transactionDateTime.dateTimeOutName()).build(),
                batchStartTimestamp));

            return keyValuesForUpdate;
        }

        @Override
        public List<Pair<FieldValue, Value>> visitBatchIdAndDateTime(BatchIdAndDateTimeAbstract batchIdAndDateTime)
        {
            List<Pair<FieldValue, Value>> keyValuesForUpdate = new ArrayList<>(2);

            keyValuesForUpdate.add(Pair.of(
                FieldValue.builder().datasetRef(mainDataset.datasetReference()).fieldName(batchIdAndDateTime.batchIdOutName()).build(),
                metadataUtils.getPrevBatchId(StringValue.of(mainDataset.datasetReference().name().orElseThrow(IllegalStateException::new)))
            ));

            keyValuesForUpdate.add(Pair.of(
                FieldValue.builder().datasetRef(mainDataset.datasetReference()).fieldName(batchIdAndDateTime.dateTimeOutName()).build(),
                batchStartTimestamp));

            return keyValuesForUpdate;
        }
    }

    static class DetermineTransactionMilestoningFields implements TransactionMilestoningVisitor<List<FieldValue>>
    {
        private final Dataset mainDataset;

        private DetermineTransactionMilestoningFields(Dataset mainDataset)
        {
            this.mainDataset = mainDataset;
        }

        @Override
        public List<FieldValue> visitBatchId(BatchIdAbstract batchId)
        {
            List<FieldValue> fields = new ArrayList<>(2);
            fields.add(FieldValue.builder().datasetRef(mainDataset.datasetReference()).fieldName(batchId.batchIdInName()).build());
            fields.add(FieldValue.builder().datasetRef(mainDataset.datasetReference()).fieldName(batchId.batchIdOutName()).build());
            return fields;
        }

        @Override
        public List<FieldValue> visitDateTime(TransactionDateTimeAbstract transactionDateTime)
        {
            List<FieldValue> fields = new ArrayList<>(2);
            fields.add(FieldValue.builder().datasetRef(mainDataset.datasetReference()).fieldName(transactionDateTime.dateTimeInName()).build());
            fields.add(FieldValue.builder().datasetRef(mainDataset.datasetReference()).fieldName(transactionDateTime.dateTimeOutName()).build());
            return fields;
        }

        @Override
        public List<FieldValue> visitBatchIdAndDateTime(BatchIdAndDateTimeAbstract batchIdAndDateTime)
        {
            List<FieldValue> fields = new ArrayList<>(4);
            fields.add(FieldValue.builder().datasetRef(mainDataset.datasetReference()).fieldName(batchIdAndDateTime.batchIdInName()).build());
            fields.add(FieldValue.builder().datasetRef(mainDataset.datasetReference()).fieldName(batchIdAndDateTime.batchIdOutName()).build());
            fields.add(FieldValue.builder().datasetRef(mainDataset.datasetReference()).fieldName(batchIdAndDateTime.dateTimeInName()).build());
            fields.add(FieldValue.builder().datasetRef(mainDataset.datasetReference()).fieldName(batchIdAndDateTime.dateTimeOutName()).build());
            return fields;
        }
    }

    static class DetermineTransactionMilestoningValues implements TransactionMilestoningVisitor<List<Value>>
    {
        private final MetadataUtils metadataUtils;
        private final StringValue mainTableName;
        private final BatchStartTimestamp batchStartTimestamp;

        private DetermineTransactionMilestoningValues(MetadataUtils metadataUtils, StringValue mainTableName, BatchStartTimestamp batchStartTimestamp)
        {
            this.metadataUtils = metadataUtils;
            this.mainTableName = mainTableName;
            this.batchStartTimestamp = batchStartTimestamp;
        }

        @Override
        public List<Value> visitBatchId(BatchIdAbstract batchId)
        {
            List<Value> values = new ArrayList<>(2);
            values.add(metadataUtils.getBatchId(mainTableName));
            values.add(LogicalPlanUtils.INFINITE_BATCH_ID());
            return values;
        }

        @Override
        public List<Value> visitDateTime(TransactionDateTimeAbstract transactionDateTime)
        {
            List<Value> values = new ArrayList<>(2);
            values.add(batchStartTimestamp);
            values.add(LogicalPlanUtils.INFINITE_BATCH_TIME());
            return values;
        }

        @Override
        public List<Value> visitBatchIdAndDateTime(BatchIdAndDateTimeAbstract batchIdAndDateTime)
        {
            List<Value> values = new ArrayList<>(4);
            values.add(metadataUtils.getBatchId(mainTableName));
            values.add(LogicalPlanUtils.INFINITE_BATCH_ID());
            values.add(batchStartTimestamp);
            values.add(LogicalPlanUtils.INFINITE_BATCH_TIME());
            return values;
        }
    }

    /*
    Rows Added in Condition:
    batch_id_in = batchId
    or batch_time_in = batchTimeStamp
     */
    static class DetermineRowsAddedInSinkCondition implements TransactionMilestoningVisitor<Condition>
    {
        private final Dataset mainDataset;
        private final StringValue mainTableName;
        private final MetadataUtils metadataUtils;
        private final BatchStartTimestamp batchStartTimestamp;

        private DetermineRowsAddedInSinkCondition(Dataset mainDataset, StringValue mainTableName, MetadataUtils metadataUtils, BatchStartTimestamp batchStartTimestamp)
        {
            this.mainDataset = mainDataset;
            this.mainTableName = mainTableName;
            this.metadataUtils = metadataUtils;
            this.batchStartTimestamp = batchStartTimestamp;
        }

        @Override
        public Condition visitBatchId(BatchIdAbstract batchId)
        {
            return LogicalPlanUtils.getBatchIdEqualityCondition(mainDataset, metadataUtils.getBatchId(mainTableName), batchId.batchIdInName());
        }

        @Override
        public Condition visitDateTime(TransactionDateTimeAbstract transactionDateTime)
        {
            return LogicalPlanUtils.getBatchTimeEqualityCondition(mainDataset, batchStartTimestamp, transactionDateTime.dateTimeInName());
        }

        @Override
        public Condition visitBatchIdAndDateTime(BatchIdAndDateTimeAbstract batchIdAndDateTime)
        {
            return LogicalPlanUtils.getBatchIdEqualityCondition(mainDataset, metadataUtils.getBatchId(mainTableName), batchIdAndDateTime.batchIdInName());
        }
    }

    /*
    Rows Invalidated in Condition:
    batch_id_out = previous batchId
    or date_time_out = current batchTimeStamp
    */
    static class DetermineRowsInvalidatedInSink implements TransactionMilestoningVisitor<Condition>
    {
        private final Dataset mainDataset;
        private final StringValue mainTableName;
        private final MetadataUtils metadataUtils;
        private final BatchStartTimestamp batchStartTimestamp;

        private DetermineRowsInvalidatedInSink(Dataset mainDataset, StringValue mainTableName, MetadataUtils metadataUtils, BatchStartTimestamp batchStartTimestamp)
        {
            this.mainDataset = mainDataset;
            this.mainTableName = mainTableName;
            this.metadataUtils = metadataUtils;
            this.batchStartTimestamp = batchStartTimestamp;
        }

        @Override
        public Condition visitBatchId(BatchIdAbstract batchId)
        {
            return LogicalPlanUtils.getBatchIdEqualityCondition(mainDataset, metadataUtils.getPrevBatchId(mainTableName), batchId.batchIdOutName());
        }

        @Override
        public Condition visitDateTime(TransactionDateTimeAbstract transactionDateTime)
        {
            return LogicalPlanUtils.getBatchTimeEqualityCondition(mainDataset, batchStartTimestamp, transactionDateTime.dateTimeOutName());
        }

        @Override
        public Condition visitBatchIdAndDateTime(BatchIdAndDateTimeAbstract batchIdAndDateTime)
        {
            return LogicalPlanUtils.getBatchIdEqualityCondition(mainDataset, metadataUtils.getPrevBatchId(mainTableName), batchIdAndDateTime.batchIdOutName());
        }
    }

    protected static final FieldIncludedInPrimaryKey FIELD_INCLUDED_IN_PRIMARY_KEY = new FieldIncludedInPrimaryKey();

    static class FieldIncludedInPrimaryKey implements TransactionMilestoningVisitor<String>
    {
        private FieldIncludedInPrimaryKey()
        {
        }

        @Override
        public String visitBatchId(BatchIdAbstract batchId)
        {
            return batchId.batchIdInName();
        }

        @Override
        public String visitDateTime(TransactionDateTimeAbstract transactionDateTime)
        {
            return transactionDateTime.dateTimeInName();
        }

        @Override
        public String visitBatchIdAndDateTime(BatchIdAndDateTimeAbstract batchIdAndDateTime)
        {
            return batchIdAndDateTime.batchIdInName();
        }
    }

    // Stats related methods
    @Override
    protected void addPostRunStatsForRowsTerminated(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        //Rows terminated = Rows invalidated in Sink - Rows updated
        LogicalPlan rowsTerminatedCountPlan = LogicalPlan.builder()
                .addOps(Selection.builder()
                        .addFields(DiffBinaryValueOperator.of(getRowsInvalidatedInSink(), getRowsUpdated()).withAlias(ROWS_TERMINATED.get()))
                        .build())
                .build();
        postRunStatisticsResult.put(ROWS_TERMINATED, rowsTerminatedCountPlan);
    }

    @Override
    protected void addPostRunStatsForRowsUpdated(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        //Rows updated (when it is invalidated and a new row for same primary keys is added)
        LogicalPlan rowsUpdatedCountPlan = LogicalPlan.builder().addOps(getRowsUpdated(ROWS_UPDATED.get())).build();
        postRunStatisticsResult.put(ROWS_UPDATED, rowsUpdatedCountPlan);
    }

    @Override
    protected void addPostRunStatsForRowsInserted(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        //Rows inserted (no previous active row with same primary key) = Rows added in sink - rows updated
        LogicalPlan rowsInsertedCountPlan = LogicalPlan.builder()
                .addOps(Selection.builder()
                        .addFields(DiffBinaryValueOperator.of(getRowsAddedInSink(), getRowsUpdated()).withAlias(ROWS_INSERTED.get()))
                        .build())
                .build();
        postRunStatisticsResult.put(ROWS_INSERTED, rowsInsertedCountPlan);
    }
}
