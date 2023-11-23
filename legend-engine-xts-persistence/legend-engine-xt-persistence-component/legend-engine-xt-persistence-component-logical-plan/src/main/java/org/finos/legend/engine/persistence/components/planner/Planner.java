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

import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.Resources;
import org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorStatistics;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.audit.AuditingVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditingAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditingAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.DeduplicationVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.*;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.operations.*;
import org.finos.legend.engine.persistence.components.logicalplan.values.*;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.ObjectValue;
import org.finos.legend.engine.persistence.components.util.BulkLoadMetadataDataset;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.finos.legend.engine.persistence.components.util.LockInfoUtils;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Collections;
import java.util.HashMap;

import static org.finos.legend.engine.persistence.components.common.StatisticName.INCOMING_RECORD_COUNT;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_DELETED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_INSERTED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_TERMINATED;
import static org.finos.legend.engine.persistence.components.common.StatisticName.ROWS_UPDATED;

import static org.finos.legend.engine.persistence.components.ingestmode.deduplication.DatasetDeduplicationHandler.COUNT;
import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.SUPPORTED_DATA_TYPES_FOR_VERSIONING_COLUMNS;
import static org.immutables.value.Value.Default;
import static org.immutables.value.Value.Immutable;
import static org.immutables.value.Value.Style;

public abstract class Planner
{
    @Immutable
    @Style(
        typeAbstract = "*Abstract",
        typeImmutable = "*",
        jdkOnly = true,
        optionalAcceptNullable = true,
        strictBuilder = true
    )
    public interface PlannerOptionsAbstract
    {
        @Default
        default boolean cleanupStagingData()
        {
            return true;
        }

        @Default
        default boolean collectStatistics()
        {
            return false;
        }

        @Default
        default boolean enableSchemaEvolution()
        {
            return false;
        }

        @Default
        default boolean createStagingDataset()
        {
            return false;
        }

        @Default
        default boolean enableConcurrentSafety()
        {
            return false;
        }

        Optional<String> bulkLoadTaskIdValue();
    }

    private final Datasets datasets;
    private final IngestMode ingestMode;
    private final PlannerOptions plannerOptions;
    protected final Set<Capability> capabilities;
    protected final List<String> primaryKeys;
    private final Optional<Dataset> tempStagingDataset;
    private final Optional<Dataset> tempStagingDatasetWithoutPks;
    private final Dataset effectiveStagingDataset;
    protected final boolean isTempTableNeededForStaging;

    Planner(Datasets datasets, IngestMode ingestMode, PlannerOptions plannerOptions, Set<Capability> capabilities)
    {
        this.datasets = datasets;
        this.ingestMode = ingestMode;
        this.plannerOptions = plannerOptions == null ? PlannerOptions.builder().build() : plannerOptions;
        this.isTempTableNeededForStaging = LogicalPlanUtils.isTempTableNeededForStaging(ingestMode);
        this.tempStagingDataset = getTempStagingDataset();
        this.tempStagingDatasetWithoutPks = getTempStagingDatasetWithoutPks();
        this.effectiveStagingDataset = isTempTableNeededForStaging ? tempStagingDataset() : originalStagingDataset();
        this.capabilities = capabilities;
        this.primaryKeys = findCommonPrimaryKeysBetweenMainAndStaging();

        // Validation
        // 1. MaxVersion & AllVersion strategies must have primary keys
        ingestMode.versioningStrategy().accept(new ValidatePrimaryKeysForVersioningStrategy(primaryKeys, this::validatePrimaryKeysNotEmpty));
        // 2. Validate if the versioningField is comparable if a versioningStrategy is present
        validateVersioningField(ingestMode().versioningStrategy(), stagingDataset());
    }

    private Optional<Dataset> getTempStagingDataset()
    {
        Optional<Dataset> tempStagingDataset = Optional.empty();
        if (isTempTableNeededForStaging)
        {
            tempStagingDataset = Optional.of(LogicalPlanUtils.getTempStagingDatasetDefinition(originalStagingDataset(), ingestMode));
        }
        return tempStagingDataset;
    }

    private Optional<Dataset> getTempStagingDatasetWithoutPks()
    {
        Optional<Dataset> tempStagingDatasetWithoutPks = Optional.empty();
        if (isTempTableNeededForStaging)
        {
            tempStagingDatasetWithoutPks = Optional.of(LogicalPlanUtils.getTempStagingDatasetWithoutPks(tempStagingDataset()));
        }
        return tempStagingDatasetWithoutPks;
    }

    private List<String> findCommonPrimaryKeysBetweenMainAndStaging()
    {
        Set<String> primaryKeysFromMain = mainDataset().schema().fields().stream().filter(Field::primaryKey).map(Field::name).collect(Collectors.toSet());
        return stagingDataset().schema().fields().stream().filter(field -> field.primaryKey() && primaryKeysFromMain.contains(field.name())).map(Field::name).collect(Collectors.toList());
    }

    protected Dataset mainDataset()
    {
        return datasets.mainDataset();
    }

    public Dataset stagingDataset()
    {
        return effectiveStagingDataset;
    }

    protected Dataset originalStagingDataset()
    {
        return datasets.stagingDataset();
    }

    protected Dataset tempStagingDataset()
    {
        return tempStagingDataset.orElseThrow(IllegalStateException::new);
    }

    protected Dataset tempStagingDatasetWithoutPks()
    {
        return tempStagingDatasetWithoutPks.orElseThrow(IllegalStateException::new);
    }

    protected List<Value> getDataFields()
    {
        List<Value> dataFields = new ArrayList<>(stagingDataset().schemaReference().fieldValues());
        Optional<String> dedupField = ingestMode.deduplicationStrategy().accept(DeduplicationVisitors.EXTRACT_DEDUP_FIELD);

        if (ingestMode().dataSplitField().isPresent())
        {
            LogicalPlanUtils.removeField(dataFields, ingestMode().dataSplitField().get());
        }
        if (dedupField.isPresent())
        {
            LogicalPlanUtils.removeField(dataFields, dedupField.get());
        }
        return dataFields;
    }

    protected Optional<MetadataDataset> metadataDataset()
    {
        return datasets.metadataDataset();
    }

    protected Optional<BulkLoadMetadataDataset> bulkLoadMetadataDataset()
    {
        return datasets.bulkLoadMetadataDataset();
    }

    protected Optional<LockInfoDataset> lockInfoDataset()
    {
        return datasets.lockInfoDataset();
    }

    protected IngestMode ingestMode()
    {
        return ingestMode;
    }

    protected PlannerOptions options()
    {
        return plannerOptions;
    }

    public abstract LogicalPlan buildLogicalPlanForIngest(Resources resources);

    public LogicalPlan buildLogicalPlanForMetadataIngest(Resources resources)
    {
        return null;
    }

    public LogicalPlan buildLogicalPlanForInitializeLock(Resources resources)
    {
        if (options().enableConcurrentSafety())
        {
            LockInfoUtils lockInfoUtils = new LockInfoUtils(datasets.lockInfoDataset().orElseThrow(IllegalStateException::new));
            return LogicalPlan.of(Collections.singleton(lockInfoUtils.initializeLockInfo(mainDataset().datasetReference().name().orElseThrow(IllegalStateException::new), BatchStartTimestampAbstract.INSTANCE)));
        }
        return null;
    }

    public LogicalPlan buildLogicalPlanForAcquireLock(Resources resources)
    {
        if (options().enableConcurrentSafety())
        {
            LockInfoUtils lockInfoUtils = new LockInfoUtils(datasets.lockInfoDataset().orElseThrow(IllegalStateException::new));
            return LogicalPlan.of(Collections.singleton(lockInfoUtils.updateLockInfo(BatchStartTimestampAbstract.INSTANCE)));
        }
        return null;
    }

    public LogicalPlan buildLogicalPlanForPreActions(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();
        operations.add(Create.of(true, mainDataset()));
        if (options().createStagingDataset())
        {
            operations.add(Create.of(true, originalStagingDataset()));
        }
        if (options().enableConcurrentSafety())
        {
            operations.add(Create.of(true, lockInfoDataset().orElseThrow(IllegalStateException::new).get()));
        }
        if (isTempTableNeededForStaging)
        {
            operations.add(Create.of(true, tempStagingDatasetWithoutPks()));
        }
        return LogicalPlan.of(operations);
    }

    public LogicalPlan buildLogicalPlanForDeduplicationAndVersioning(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();
        if (isTempTableNeededForStaging)
        {
            operations.add(Delete.builder().dataset(tempStagingDataset()).build());
            Dataset dedupAndVersionedDataset = LogicalPlanUtils.getDedupedAndVersionedDataset(ingestMode.deduplicationStrategy(), ingestMode.versioningStrategy(), originalStagingDataset(), primaryKeys);
            List<Value> fieldsToInsert = new ArrayList<>(dedupAndVersionedDataset.schemaReference().fieldValues());
            operations.add(Insert.of(tempStagingDataset(), dedupAndVersionedDataset, fieldsToInsert));
        }
        return LogicalPlan.of(operations);
    }

    public LogicalPlan buildLogicalPlanForPostActions(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();
        if (plannerOptions.cleanupStagingData())
        {
            operations.add(Delete.builder().dataset(originalStagingDataset()).build());
        }
        return LogicalPlan.of(operations);
    }

    // Introduce a flag
    public LogicalPlan buildLogicalPlanForPostCleanup(Resources resources)
    {
        List<Operation> operations = new ArrayList<>();
        // Drop table
        if (resources.externalDatasetImported())
        {
            operations.add(Drop.of(true, originalStagingDataset(), true));
        }
        if (isTempTableNeededForStaging)
        {
            operations.add(Drop.of(true, tempStagingDataset(), true));
        }
        return LogicalPlan.of(operations);
    }

    public Map<StatisticName, LogicalPlan> buildLogicalPlanForPreRunStatistics(Resources resources)
    {
        return Collections.emptyMap();
    }

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
            addPostRunStatsForRowsDeleted(postRunStatisticsResult);
        }

        return postRunStatisticsResult;
    }

    public Map<DedupAndVersionErrorStatistics, LogicalPlan> buildLogicalPlanForDeduplicationAndVersioningErrorChecks(Resources resources)
    {
        Map<DedupAndVersionErrorStatistics, LogicalPlan> dedupAndVersioningErrorChecks = new HashMap<>();
        addMaxDuplicatesErrorCheck(dedupAndVersioningErrorChecks);
        addDataErrorCheck(dedupAndVersioningErrorChecks);
        return dedupAndVersioningErrorChecks;
    }

    protected void addMaxDuplicatesErrorCheck(Map<DedupAndVersionErrorStatistics, LogicalPlan> dedupAndVersioningErrorChecks)
    {
        if (ingestMode.deduplicationStrategy() instanceof FailOnDuplicates)
        {
            FunctionImpl maxCount = FunctionImpl.builder()
                    .functionName(FunctionName.MAX)
                    .addValue(FieldValue.builder().datasetRef(tempStagingDataset().datasetReference()).fieldName(COUNT).build())
                    .alias(DedupAndVersionErrorStatistics.MAX_DUPLICATES.name())
                    .build();
            Selection selectMaxDupsCount = Selection.builder()
                    .source(tempStagingDataset())
                    .addFields(maxCount)
                    .build();
            LogicalPlan maxDuplicatesCountPlan = LogicalPlan.builder().addOps(selectMaxDupsCount).build();
            dedupAndVersioningErrorChecks.put(DedupAndVersionErrorStatistics.MAX_DUPLICATES, maxDuplicatesCountPlan);
        }
    }

    protected void addDataErrorCheck(Map<DedupAndVersionErrorStatistics, LogicalPlan> dedupAndVersioningErrorChecks)
    {
        List<String> remainingColumns = getDigestOrRemainingColumns();
        if (ingestMode.versioningStrategy().accept(VersioningVisitors.IS_TEMP_TABLE_NEEDED))
        {
            LogicalPlan logicalPlan = ingestMode.versioningStrategy().accept(new DeriveDataErrorCheckLogicalPlan(primaryKeys, remainingColumns, tempStagingDataset()));
            if (logicalPlan != null)
            {
                dedupAndVersioningErrorChecks.put(DedupAndVersionErrorStatistics.MAX_DATA_ERRORS, logicalPlan);
            }
        }
    }

    abstract List<String> getDigestOrRemainingColumns();

    protected void validatePrimaryKeysNotEmpty(List<String> primaryKeys)
    {
        if (primaryKeys.isEmpty())
        {
            throw new IllegalStateException("Primary key list must not be empty");
        }
    }

    protected void validatePrimaryKeysIsEmpty(List<String> primaryKeys)
    {
        if (!primaryKeys.isEmpty())
        {
            throw new IllegalStateException("Primary key list must be empty");
        }
    }

    public boolean dataSplitExecutionSupported()
    {
        return true;
    }

    public Optional<Condition> getDataSplitInRangeConditionForStatistics()
    {
        return Optional.empty();
    }

    protected void addPreRunStatsForRowsDeleted(Map<StatisticName, LogicalPlan> preRunStatisticsResult)
    {
        LogicalPlan rowsDeletedCountPlan = LogicalPlanFactory.getLogicalPlanForConstantStats(ROWS_DELETED.get(), 0L);
        preRunStatisticsResult.put(ROWS_DELETED, rowsDeletedCountPlan);
    }

    protected void addPostRunStatsForIncomingRecords(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        Optional<Condition> filterCondition = Optional.empty();
        Value countIncomingRecords = FunctionImpl.builder().functionName(FunctionName.COUNT).alias(INCOMING_RECORD_COUNT.get()).addValue(All.INSTANCE).build();
        Dataset dataset = originalStagingDataset();

        // If data splits are present
        if (ingestMode.dataSplitField().isPresent())
        {
            dataset = stagingDataset();
            filterCondition = getDataSplitInRangeConditionForStatistics();
            Optional<String> duplicateCountFieldName = ingestMode.deduplicationStrategy().accept(DeduplicationVisitors.EXTRACT_DEDUP_FIELD);
            // If deduplication has been performed
            if (duplicateCountFieldName.isPresent())
            {
                FieldValue duplicateCountField = FieldValue.builder().fieldName(duplicateCountFieldName.get()).datasetRef(dataset.datasetReference()).build();
                FunctionImpl sumOfDuplicateFieldCount = FunctionImpl.builder().functionName(FunctionName.SUM).addValue(duplicateCountField).build();
                countIncomingRecords = FunctionImpl.builder().functionName(FunctionName.COALESCE).alias(INCOMING_RECORD_COUNT.get()).addValue(sumOfDuplicateFieldCount, ObjectValue.of(0)).build();
            }
        }

        LogicalPlan incomingRecordCountPlan = LogicalPlan.builder()
                .addOps(Selection.builder()
                        .source(dataset)
                        .addFields(countIncomingRecords)
                        .condition(filterCondition)
                        .build())
                .build();
        postRunStatisticsResult.put(INCOMING_RECORD_COUNT, incomingRecordCountPlan);
    }

    protected void addPostRunStatsForRowsTerminated(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        LogicalPlan rowsTerminatedCountPlan = LogicalPlanFactory.getLogicalPlanForConstantStats(ROWS_TERMINATED.get(), 0L);
        postRunStatisticsResult.put(ROWS_TERMINATED, rowsTerminatedCountPlan);
    }

    protected void addPostRunStatsForRowsUpdated(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        LogicalPlan rowsUpdatedCountPlan = LogicalPlanFactory.getLogicalPlanForConstantStats(ROWS_UPDATED.get(), 0L);
        postRunStatisticsResult.put(ROWS_UPDATED, rowsUpdatedCountPlan);
    }

    protected void addPostRunStatsForRowsInserted(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        LogicalPlan rowsInsertedCountPlan = LogicalPlan.builder().addOps(LogicalPlanUtils.getRecordCount(mainDataset(), ROWS_INSERTED.get())).build();
        postRunStatisticsResult.put(ROWS_INSERTED, rowsInsertedCountPlan);
    }

    protected void addPostRunStatsForRowsDeleted(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        LogicalPlan rowsDeletedCountPlan = LogicalPlanFactory.getLogicalPlanForConstantStats(ROWS_DELETED.get(), 0L);
        postRunStatisticsResult.put(ROWS_DELETED, rowsDeletedCountPlan);
    }

    protected List<String> getNonPKNonVersionDataFields()
    {
        List<String> nonPkDataFields = stagingDataset().schemaReference().fieldValues().stream()
                .map(fieldValue -> fieldValue.fieldName())
                .filter(fieldName -> !primaryKeys.contains(fieldName))
                .collect(Collectors.toList());
        Optional<String> dedupField = ingestMode.deduplicationStrategy().accept(DeduplicationVisitors.EXTRACT_DEDUP_FIELD);
        Optional<String> versioningField = ingestMode.versioningStrategy().accept(VersioningVisitors.EXTRACT_VERSIONING_FIELD);
        nonPkDataFields.removeIf(field -> ingestMode().dataSplitField().isPresent() && field.equals(ingestMode().dataSplitField().get()));
        nonPkDataFields.removeIf(field -> dedupField.isPresent() && field.equals(dedupField.get()));
        nonPkDataFields.removeIf(field -> versioningField.isPresent() && field.equals(versioningField.get()));
        return nonPkDataFields;
    }

    protected void validateVersioningField(VersioningStrategy versioningStrategy, Dataset dataset)
    {
        Optional<String> versioningField = versioningStrategy.accept(VersioningVisitors.EXTRACT_VERSIONING_FIELD);
        if (versioningField.isPresent())
        {
            Field filterField = dataset.schema().fields().stream()
                    .filter(field -> field.name().equals(versioningField.get()))
                    .findFirst().orElseThrow(() -> new IllegalStateException(String.format("Versioning field [%s] not found in Staging Schema", versioningField.get())));
            if (!SUPPORTED_DATA_TYPES_FOR_VERSIONING_COLUMNS.contains(filterField.type().dataType()))
            {
                throw new IllegalStateException(String.format("Versioning field's data type [%s] is not supported", filterField.type().dataType()));
            }
        }
    }

    // auditing visitor

    protected static final AuditEnabled AUDIT_ENABLED = new AuditEnabled();

    static class AuditEnabled implements AuditingVisitor<Boolean>
    {
        private AuditEnabled()
        {
        }

        @Override
        public Boolean visitNoAuditing(NoAuditingAbstract noAuditing)
        {
            return false;
        }

        @Override
        public Boolean visitDateTimeAuditing(DateTimeAuditingAbstract dateTimeAuditing)
        {
            return true;
        }
    }

    static class ValidatePrimaryKeysForVersioningStrategy implements VersioningStrategyVisitor<Void>
    {
        final List<String> primaryKeys;
        final Consumer<List<String>> validatePrimaryKeysNotEmpty;

        ValidatePrimaryKeysForVersioningStrategy(List<String> primaryKeys, Consumer<List<String>> validatePrimaryKeysNotEmpty)
        {
            this.primaryKeys = primaryKeys;
            this.validatePrimaryKeysNotEmpty = validatePrimaryKeysNotEmpty;
        }

        @Override
        public Void visitNoVersioningStrategy(NoVersioningStrategyAbstract noVersioningStrategy)
        {
            return null;
        }

        @Override
        public Void visitMaxVersionStrategy(MaxVersionStrategyAbstract maxVersionStrategy)
        {
            validatePrimaryKeysNotEmpty.accept(primaryKeys);
            if (primaryKeys.contains(maxVersionStrategy.versioningField()))
            {
                throw new IllegalStateException("Versioning field cannot be a primary key");
            }
            return null;
        }

        @Override
        public Void visitAllVersionsStrategy(AllVersionsStrategyAbstract allVersionsStrategyAbstract)
        {
            validatePrimaryKeysNotEmpty.accept(primaryKeys);
            if (primaryKeys.contains(allVersionsStrategyAbstract.versioningField()))
            {
                throw new IllegalStateException("Versioning field cannot be a primary key");
            }
            return null;
        }
    }
}
