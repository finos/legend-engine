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

package org.finos.legend.engine.persistence.components.relational.api;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType;
import org.finos.legend.engine.persistence.components.common.Resources;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.planner.Planner;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.planner.Planners;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.schemaevolution.SchemaEvolution;
import org.finos.legend.engine.persistence.components.schemaevolution.SchemaEvolutionResult;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.MetadataUtils;
import org.finos.legend.engine.persistence.components.util.SchemaEvolutionCapability;
import org.finos.legend.engine.persistence.components.util.ValidationCategory;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Immutable
@Style(
    typeAbstract = "*Abstract",
    typeImmutable = "*",
    jdkOnly = true,
    optionalAcceptNullable = true,
    strictBuilder = true
)
public abstract class RelationalGeneratorAbstract
{

    public static final String BULK_LOAD_BATCH_STATUS_PATTERN = "{BULK_LOAD_BATCH_STATUS_PLACEHOLDER}";

    //---------- FLAGS ----------

    @Default
    public boolean cleanupStagingData()
    {
        return true;
    }

    @Default
    public boolean collectStatistics()
    {
        return false;
    }

    @Default
    public boolean enableSchemaEvolution()
    {
        return false;
    }

    @Default
    public CaseConversion caseConversion()
    {
        return CaseConversion.NONE;
    }

    @Default
    public Clock executionTimestampClock()
    {
        return Clock.systemUTC();
    }

    @Default
    public boolean createStagingDataset()
    {
        return false;
    }

    @Default
    public boolean enableConcurrentSafety()
    {
        return false;
    }

    public abstract Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet();

    public abstract Optional<String> batchStartTimestampPattern();

    public abstract Optional<String> batchEndTimestampPattern();

    public abstract Optional<String> batchIdPattern();

    public abstract Optional<Long> infiniteBatchIdValue();

    public abstract Map<String, Object> additionalMetadata();

    public abstract Optional<String> bulkLoadEventIdValue();

    @Default
    public String bulkLoadBatchStatusPattern()
    {
        return BULK_LOAD_BATCH_STATUS_PATTERN;
    }

    @Default
    public String batchSuccessStatusValue()
    {
        return MetadataUtils.MetaTableStatus.DONE.toString();
    }

    @Default
    public int sampleRowCount()
    {
        return 20;
    }

    @Default
    public String ingestRunId()
    {
        return UUID.randomUUID().toString();
    }

    //---------- FIELDS ----------

    public abstract IngestMode ingestMode();

    public abstract RelationalSink relationalSink();

    @Derived
    protected PlannerOptions plannerOptions()
    {
        return PlannerOptions.builder()
            .cleanupStagingData(cleanupStagingData())
            .collectStatistics(collectStatistics())
            .enableSchemaEvolution(enableSchemaEvolution())
            .createStagingDataset(createStagingDataset())
            .enableConcurrentSafety(enableConcurrentSafety())
            .putAllAdditionalMetadata(additionalMetadata())
            .bulkLoadEventIdValue(bulkLoadEventIdValue())
            .batchSuccessStatusValue(batchSuccessStatusValue())
            .sampleRowCount(sampleRowCount())
            .ingestRunId(ingestRunId())
            .build();
    }

    @Derived
    protected TransformOptions transformOptions()
    {
        TransformOptions.Builder builder = TransformOptions.builder()
            .executionTimestampClock(executionTimestampClock())
            .batchStartTimestampPattern(batchStartTimestampPattern())
            .batchEndTimestampPattern(batchEndTimestampPattern())
            .infiniteBatchIdValue(infiniteBatchIdValue())
            .bulkLoadBatchStatusPattern(bulkLoadBatchStatusPattern())
            .batchIdPattern(batchIdPattern());

        relationalSink().optimizerForCaseConversion(caseConversion()).ifPresent(builder::addOptimizers);

        return builder.build();
    }

    // ---------- API ----------

    public GeneratorResult generateOperations(Datasets datasets)
    {
        return generateOperations(datasets, Resources.builder().build());
    }

    public GeneratorResult generateOperationsForEmptyBatch(Datasets datasets)
    {
        return generateOperations(datasets, Resources.builder().stagingDataSetEmpty(true).build());
    }

    public List<GeneratorResult> generateOperationsWithDataSplits(Datasets datasets, List<DataSplitRange> dataSplitRanges)
    {
        GeneratorResult generatorResult = generateOperations(datasets);
        return dataSplitRanges.stream()
            .map(generatorResult::withIngestDataSplitRange)
            .collect(Collectors.toList());
    }

    //TODO: generateOperationsWithDataSplitsForEmptyBatch


    // ---------- UTILITY METHODS ----------

    GeneratorResult generateOperations(Datasets datasets, Resources resources)
    {
        IngestMode ingestModeWithCaseConversion = ApiUtils.applyCase(ingestMode(), caseConversion());
        Datasets datasetsWithCaseConversion = ApiUtils.enrichAndApplyCase(datasets, caseConversion());
        Dataset enrichedMainDataset = ApiUtils.deriveMainDatasetFromStaging(datasetsWithCaseConversion, ingestModeWithCaseConversion);
        Datasets enrichedDatasets = datasetsWithCaseConversion.withMainDataset(enrichedMainDataset);
        Planner planner = Planners.get(enrichedDatasets, ingestModeWithCaseConversion, plannerOptions(), relationalSink().capabilities());
        return generateOperations(enrichedDatasets, resources, planner, ingestModeWithCaseConversion);
    }

    GeneratorResult generateOperations(Datasets datasets, Resources resources, Planner planner, IngestMode ingestMode)
    {
        Transformer<SqlGen, SqlPlan> transformer = new RelationalTransformer(relationalSink(), transformOptions());

        // pre-run statistics
        Map<StatisticName, LogicalPlan> preIngestStatisticsLogicalPlan = planner.buildLogicalPlanForPreRunStatistics(resources);
        Map<StatisticName, SqlPlan> preIngestStatisticsSqlPlan = new HashMap<>();
        for (StatisticName statistic : preIngestStatisticsLogicalPlan.keySet())
        {
            preIngestStatisticsSqlPlan.put(statistic, transformer.generatePhysicalPlan(preIngestStatisticsLogicalPlan.get(statistic)));
        }

        // pre-actions
        LogicalPlan preActionsLogicalPlan = planner.buildLogicalPlanForPreActions(resources);
        SqlPlan preActionsSqlPlan = transformer.generatePhysicalPlan(preActionsLogicalPlan);

        // dry-run pre-actions
        LogicalPlan dryRunPreActionsLogicalPlan = planner.buildLogicalPlanForDryRunPreActions(resources);
        SqlPlan dryRunPreActionsSqlPlan = transformer.generatePhysicalPlan(dryRunPreActionsLogicalPlan);

        // initialize-lock
        LogicalPlan initializeLockLogicalPlan = planner.buildLogicalPlanForInitializeLock(resources);
        Optional<SqlPlan> initializeLockSqlPlan = Optional.empty();
        if (initializeLockLogicalPlan != null)
        {
            initializeLockSqlPlan = Optional.of(transformer.generatePhysicalPlan(initializeLockLogicalPlan));
        }

        // acquire-lock
        LogicalPlan acquireLockLogicalPlan = planner.buildLogicalPlanForAcquireLock(resources);
        Optional<SqlPlan> acquireLockSqlPlan = Optional.empty();
        if (acquireLockLogicalPlan != null)
        {
            acquireLockSqlPlan = Optional.of(transformer.generatePhysicalPlan(acquireLockLogicalPlan));
        }

        // schema evolution
        Optional<SqlPlan> schemaEvolutionSqlPlan = Optional.empty();
        Optional<Dataset> schemaEvolutionDataset = Optional.empty();
        if (enableSchemaEvolution())
        {
            // Get logical plan and physical plan for schema evolution and update datasets
            SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink(), ingestMode, schemaEvolutionCapabilitySet());
            SchemaEvolutionResult schemaEvolutionResult = schemaEvolution.buildLogicalPlanForSchemaEvolution(datasets.mainDataset(), datasets.stagingDataset().schema());
            LogicalPlan schemaEvolutionLogicalPlan = schemaEvolutionResult.logicalPlan();

            schemaEvolutionSqlPlan = Optional.of(transformer.generatePhysicalPlan(schemaEvolutionLogicalPlan));
            schemaEvolutionDataset = Optional.of(schemaEvolutionResult.evolvedDataset());

            // update main dataset with evolved schema and re-initialize planner
            planner = Planners.get(datasets.withMainDataset(schemaEvolutionDataset.get()), ingestMode, plannerOptions(), relationalSink().capabilities());
        }

        // deduplication and versioning
        LogicalPlan deduplicationAndVersioningLogicalPlan = planner.buildLogicalPlanForDeduplicationAndVersioning(resources);
        Optional<SqlPlan> deduplicationAndVersioningSqlPlan = Optional.empty();
        if (deduplicationAndVersioningLogicalPlan != null)
        {
            deduplicationAndVersioningSqlPlan = Optional.of(transformer.generatePhysicalPlan(deduplicationAndVersioningLogicalPlan));
        }

        Map<DedupAndVersionErrorSqlType, LogicalPlan> deduplicationAndVersioningErrorChecksLogicalPlan = planner.buildLogicalPlanForDeduplicationAndVersioningErrorChecks(resources);
        Map<DedupAndVersionErrorSqlType, SqlPlan> deduplicationAndVersioningErrorChecksSqlPlan = new HashMap<>();
        for (DedupAndVersionErrorSqlType statistic : deduplicationAndVersioningErrorChecksLogicalPlan.keySet())
        {
            deduplicationAndVersioningErrorChecksSqlPlan.put(statistic, transformer.generatePhysicalPlan(deduplicationAndVersioningErrorChecksLogicalPlan.get(statistic)));
        }

        // ingest
        LogicalPlan ingestLogicalPlan = planner.buildLogicalPlanForIngest(resources);
        SqlPlan ingestSqlPlan = transformer.generatePhysicalPlan(ingestLogicalPlan);

        // dry-run
        LogicalPlan dryRunLogicalPlan = planner.buildLogicalPlanForDryRun(resources);
        SqlPlan dryRunSqlPlan = transformer.generatePhysicalPlan(dryRunLogicalPlan);

        // dry-run validations
        Map<ValidationCategory, List<Pair<Set<FieldValue>, LogicalPlan>>> dryRunValidationLogicalPlan = planner.buildLogicalPlanForDryRunValidation(resources);
        Map<ValidationCategory, List<Pair<Set<FieldValue>, SqlPlan>>> dryRunValidationSqlPlan = new HashMap<>();
        for (ValidationCategory validationCategory : dryRunValidationLogicalPlan.keySet())
        {
            dryRunValidationSqlPlan.put(validationCategory, new ArrayList<>());
            for (Pair<Set<FieldValue>, LogicalPlan> pair : dryRunValidationLogicalPlan.get(validationCategory))
            {
                SqlPlan sqlplan = transformer.generatePhysicalPlan(pair.getTwo());
                dryRunValidationSqlPlan.get(validationCategory).add(Tuples.pair(pair.getOne(), sqlplan));
            }
        }

        // metadata ingest
        LogicalPlan metaDataIngestLogicalPlan = planner.buildLogicalPlanForMetadataIngest(resources);
        SqlPlan metaDataIngestSqlPlan = transformer.generatePhysicalPlan(metaDataIngestLogicalPlan);

        // post-actions
        LogicalPlan postActionsLogicalPlan = planner.buildLogicalPlanForPostActions(resources);
        SqlPlan postActionsSqlPlan = transformer.generatePhysicalPlan(postActionsLogicalPlan);

        // post-cleanup
        LogicalPlan postCleanupLogicalPlan = planner.buildLogicalPlanForPostCleanup(resources);
        Optional<SqlPlan> postCleanupSqlPlan = Optional.empty();
        if (postCleanupLogicalPlan != null)
        {
            postCleanupSqlPlan = Optional.of(transformer.generatePhysicalPlan(postCleanupLogicalPlan));
        }

        // dry-run post-cleanup
        LogicalPlan dryRunPostCleanupLogicalPlan = planner.buildLogicalPlanForDryRunPostCleanup(resources);
        SqlPlan dryRunPostCleanupSqlPlan = transformer.generatePhysicalPlan(dryRunPostCleanupLogicalPlan);

        // post-run statistics
        Map<StatisticName, LogicalPlan> postIngestStatisticsLogicalPlan = planner.buildLogicalPlanForPostRunStatistics(resources);
        Map<StatisticName, SqlPlan> postIngestStatisticsSqlPlan = new HashMap<>();
        for (StatisticName statistic : postIngestStatisticsLogicalPlan.keySet())
        {
            postIngestStatisticsSqlPlan.put(statistic, transformer.generatePhysicalPlan(postIngestStatisticsLogicalPlan.get(statistic)));
        }

        return GeneratorResult.builder()
            .preActionsSqlPlan(preActionsSqlPlan)
            .dryRunPreActionsSqlPlan(dryRunPreActionsSqlPlan)
            .initializeLockSqlPlan(initializeLockSqlPlan)
            .acquireLockSqlPlan(acquireLockSqlPlan)
            .schemaEvolutionSqlPlan(schemaEvolutionSqlPlan)
            .schemaEvolutionDataset(schemaEvolutionDataset)
            .ingestSqlPlan(ingestSqlPlan)
            .dryRunSqlPlan(dryRunSqlPlan)
            .putAllDryRunValidationSqlPlan(dryRunValidationSqlPlan)
            .postActionsSqlPlan(postActionsSqlPlan)
            .postCleanupSqlPlan(postCleanupSqlPlan)
            .dryRunPostCleanupSqlPlan(dryRunPostCleanupSqlPlan)
            .metadataIngestSqlPlan(metaDataIngestSqlPlan)
            .deduplicationAndVersioningSqlPlan(deduplicationAndVersioningSqlPlan)
            .putAllDeduplicationAndVersioningErrorChecksSqlPlan(deduplicationAndVersioningErrorChecksSqlPlan)
            .putAllPreIngestStatisticsSqlPlan(preIngestStatisticsSqlPlan)
            .putAllPostIngestStatisticsSqlPlan(postIngestStatisticsSqlPlan)
            .build();
    }
}
