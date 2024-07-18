// Copyright 2024 Goldman Sachs
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

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.Resources;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.IngestModeOptimizationColumnHandler;
import org.finos.legend.engine.persistence.components.ingestmode.IngestModeVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.TempDatasetsEnricher;
import org.finos.legend.engine.persistence.components.planner.Planner;
import org.finos.legend.engine.persistence.components.planner.Planners;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.finos.legend.engine.persistence.components.util.PlaceholderValue;
import org.finos.legend.engine.persistence.components.util.SqlLogging;
import org.immutables.value.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.finos.legend.engine.persistence.components.relational.api.ApiUtils.datasetEmpty;
import static org.finos.legend.engine.persistence.components.relational.api.ApiUtils.ADDITIONAL_METADATA_KEY_PATTERN;
import static org.finos.legend.engine.persistence.components.relational.api.ApiUtils.ADDITIONAL_METADATA_VALUE_PATTERN;
import static org.finos.legend.engine.persistence.components.relational.api.ApiUtils.BATCH_END_TS_PATTERN;
import static org.finos.legend.engine.persistence.components.relational.api.ApiUtils.BATCH_ID_PATTERN;
import static org.finos.legend.engine.persistence.components.relational.api.ApiUtils.BATCH_START_TS_PATTERN;
import static org.finos.legend.engine.persistence.components.transformer.Transformer.TransformOptionsAbstract.DATE_TIME_FORMATTER;

@Value.Immutable
@Value.Style(
        typeAbstract = "*Abstract",
        typeImmutable = "*",
        jdkOnly = true,
        optionalAcceptNullable = true,
        strictBuilder = true
)
public abstract class RelationalMultiDatasetIngestorAbstract
{

    private static final Logger LOGGER = LoggerFactory.getLogger(RelationalMultiDatasetIngestorAbstract.class);

    //-------------------- FIELDS --------------------

    public abstract RelationalSink relationalSink();

    abstract LockInfoDataset lockInfoDataset();

    //-------------------- FLAGS --------------------

    @Value.Default
    public boolean cleanupStagingData()
    {
        return true;
    }

    @Value.Default
    public boolean collectStatistics()
    {
        return true;
    }

    @Value.Default
    public CaseConversion caseConversion()
    {
        return CaseConversion.NONE;
    }

    @Value.Default
    public Clock executionTimestampClock()
    {
        return Clock.systemUTC();
    }

    public abstract Map<String, Object> additionalMetadata();

    public abstract Optional<String> ingestRequestId();

    @Value.Default
    public boolean enableIdempotencyCheck()
    {
        return false;
    }

    @Value.Default
    public SqlLogging sqlLogging()
    {
        return SqlLogging.DISABLED;
    }

    @Value.Default
    public int sampleDataErrorRowCount()
    {
        return 20;
    }

    @Value.Derived
    protected TransformOptions transformOptions()
    {
        TransformOptions.Builder builder = TransformOptions.builder()
                .executionTimestampClock(executionTimestampClock())
                .batchIdPattern(BATCH_ID_PATTERN);
        relationalSink().optimizerForCaseConversion(caseConversion()).ifPresent(builder::addOptimizers);
        return builder.build();
    }

    //------------------- Private Fields -------------------
    private Executor<SqlGen, TabularData, SqlPlan> executor;

    private Transformer<SqlGen, SqlPlan> transformer;

    private Map<IngestStage, IngestStageMetadata> ingestStageMetadataMap; // TODO: Review if we really need a map, or a list will do

    //-------------------- APIs --------------------

    public Executor init(List<DatasetIngestDetails> datasetIngestDetails, RelationalConnection connection)
    {
        // 1. Initialize the executor
        Executor executor = initExecutor(connection);

        // 2. Initialize the ingestStageMetadataMap
        initIngestStageMetadataMap(datasetIngestDetails);

        return executor;
    }

    public void create()
    {
        // 1. Validate initialization has been performed
        validateInitialization();

        // 2. Create all datasets needed for all stages
        createAllDatasets();

        // 3. Initialize the lock which will exist for all stages
        initializeLock();
    }

    /**
     * Ingest multi datasets in a transaction. Each dataset can have multiple stages
     * @return List of IngestStageResults
     */
    public List<DatasetIngestResults> ingest()
    {
        // 1. Validate initialization has been performed
        validateInitialization();

        // 2. Acquire lock for all ingest stages
        acquireLock();

        // 3. Perform idempotency check
        List<DatasetIngestResults> result = verifyIfRequestAlreadyProcessedPreviously();

        if (result.isEmpty())
        {
            // 4. Perform ingestion
            result = performIngestionForAllStages();
            LOGGER.info("Ingestion completed");
        }

        return result;
    }


    //-------------------- Helper Methods --------------------
    private Executor initExecutor(RelationalConnection connection)
    {
        LOGGER.info("Invoked initExecutor method, will initialize the executor");
        executor = relationalSink().getRelationalExecutor(connection);
        executor.setSqlLogging(sqlLogging());
        return executor;
    }

    private void initIngestStageMetadataMap(List<DatasetIngestDetails> datasetIngestDetails)
    {
        ingestStageMetadataMap = new LinkedHashMap<>();

        for (DatasetIngestDetails details : datasetIngestDetails)
        {
            MetadataDataset metadataDataset = details.metadataDataset();
            for (IngestStage ingestStage : details.ingestStages())
            {
                // 1. Enrich the ingest mode with case conversion
                IngestMode enrichedIngestMode = ApiUtils.applyCase(ingestStage.ingestMode(), caseConversion());

                // 2. Build datasets with main, staging and metadata
                Datasets enrichedDatasets = Datasets.builder()
                    .stagingDataset(ingestStage.stagingDataset())
                    .mainDataset(ApiUtils.deriveMainDatasetFromStaging(ingestStage.mainDataset(), ingestStage.stagingDataset(), enrichedIngestMode))
                    .metadataDataset(metadataDataset)
                    .build();

                // 3. Enrich the datasets with case conversion
                enrichedDatasets = ApiUtils.enrichAndApplyCase(enrichedDatasets, caseConversion());

                // 4. Add optimization columns if needed
                enrichedIngestMode = enrichedIngestMode.accept(new IngestModeOptimizationColumnHandler(enrichedDatasets));

                // 5. Add temp datasets
                enrichedDatasets = enrichedIngestMode.accept(new TempDatasetsEnricher(enrichedDatasets));

                // 6. Use a placeholder for additional metadata
                Map<String, Object> placeholderAdditionalMetadata = new HashMap<>();
                if (!additionalMetadata().isEmpty())
                {
                    placeholderAdditionalMetadata = Collections.singletonMap(ADDITIONAL_METADATA_KEY_PATTERN, ADDITIONAL_METADATA_VALUE_PATTERN);
                }

                // 7. Create the generator
                RelationalGenerator generator = RelationalGenerator.builder()
                    .ingestMode(enrichedIngestMode)
                    .relationalSink(relationalSink())
                    .cleanupStagingData(cleanupStagingData())
                    .collectStatistics(collectStatistics())
                    .writeStatistics(collectStatistics()) // Collecting statistics will imply writing it into the batch metadata table
                    .skipMainAndMetadataDatasetCreation(true)
                    .enableConcurrentSafety(true)
                    .caseConversion(caseConversion())
                    .executionTimestampClock(executionTimestampClock())
                    .batchStartTimestampPattern(BATCH_START_TS_PATTERN)
                    .batchEndTimestampPattern(BATCH_END_TS_PATTERN)
                    .batchIdPattern(BATCH_ID_PATTERN)
                    .putAllAdditionalMetadata(placeholderAdditionalMetadata)
                    .ingestRequestId(ingestRequestId())
                    .batchSuccessStatusValue("SUCCESS") // TODO: ??
                    .sampleRowCount(sampleDataErrorRowCount())
                    .ingestRunId(ingestStage.getRunId())
                    .build();

                // 8. Create the planner
                Planner planner = Planners.get(enrichedDatasets, enrichedIngestMode, generator.plannerOptions(), relationalSink().capabilities());

                ingestStageMetadataMap.put(ingestStage,
                    IngestStageMetadata.builder()
                        .datasets(enrichedDatasets)
                        .ingestMode(enrichedIngestMode)
                        .relationalGenerator(generator)
                        .planner(planner)
                        .build());
            }
        }
    }

    private void validateInitialization()
    {
        // Validation: init() must have been invoked
        if (executor == null || ingestStageMetadataMap == null)
        {
            throw new IllegalStateException("Initialization not done, call init() before invoking this method!");
        }
    }

    private void createAllDatasets()
    {
        LOGGER.info("Creating the datasets");
        for (IngestStage ingestStage : ingestStageMetadataMap.keySet())
        {
            IngestStageMetadata ingestStageMetadata = ingestStageMetadataMap.get(ingestStage);
            GeneratorResult generatorResult = ingestStageMetadata.relationalGenerator().generateOperationsForCreate(Resources.builder().build(), ingestStageMetadata.planner(), transformer);
            executor.executePhysicalPlan(generatorResult.preActionsSqlPlan());
        }
    }

    private void initializeLock()
    {
        LOGGER.info("Concurrent safety is enabled, Initializing lock");
        Map<String, PlaceholderValue> placeHolderKeyValues = new HashMap<>();
        placeHolderKeyValues.put(BATCH_START_TS_PATTERN, PlaceholderValue.of(LocalDateTime.now(executionTimestampClock()).format(DATE_TIME_FORMATTER), false));
        try
        {
            // TODO: Instead of using the generator/planner to do this, we should build the logical plan here
            // executor.executePhysicalPlan(generatorResult.initializeLockSqlPlan().orElseThrow(IllegalStateException::new), placeHolderKeyValues);
        }
        catch (Exception e)
        {
            // Ignore this exception
            // In race condition: multiple jobs will try to insert same row
        }
    }

    private void acquireLock()
    {
        LOGGER.info("Concurrent safety is enabled, Acquiring lock");
        Map<String, PlaceholderValue> placeHolderKeyValues = new HashMap<>();
        placeHolderKeyValues.put(BATCH_START_TS_PATTERN, PlaceholderValue.of(LocalDateTime.now(executionTimestampClock()).format(DATE_TIME_FORMATTER), false));
        // TODO: Instead of using the generator/planner to do this, we should build the logical plan here
        //executor.executePhysicalPlan(generatorResult.acquireLockSqlPlan().orElseThrow(IllegalStateException::new), placeHolderKeyValues);
    }

    private List<org.finos.legend.engine.persistence.components.relational.api.DatasetIngestResults> verifyIfRequestAlreadyProcessedPreviously()
    {
        // TODO
        return null;
    }

    private List<DatasetIngestResults> performIngestionForAllStages()
    {
        for (IngestStage ingestStage : ingestStageMetadataMap.keySet())
        {
            IngestStageMetadata ingestStageMetadata = ingestStageMetadataMap.get(ingestStage);

            IngestMode enrichedIngestMode = ingestStageMetadata.ingestMode();
            Datasets enrichedDatasets = ingestStageMetadata.datasets();
            RelationalGenerator generator = ingestStageMetadata.relationalGenerator();
            Planner planner = ingestStageMetadata.planner();

            // 1. Check if staging dataset is empty
            Resources.Builder resourcesBuilder = Resources.builder();
            if (enrichedIngestMode.accept(IngestModeVisitors.NEED_TO_CHECK_STAGING_EMPTY) && executor.datasetExists(enrichedDatasets.stagingDataset()))
            {
                boolean isStagingDatasetEmpty = datasetEmpty(enrichedDatasets.stagingDataset(), transformer, executor);
                LOGGER.info(String.format("Checking if staging dataset is empty : {%s}", isStagingDatasetEmpty));
                resourcesBuilder.stagingDataSetEmpty(isStagingDatasetEmpty);
            }

            // 2. Generate SQLs
            GeneratorResult generatorResult = generator.generateOperationsForIngest(resourcesBuilder.build(), planner, transformer);

            // 3. Perform deduplication and versioning
            if (generatorResult.deduplicationAndVersioningSqlPlan().isPresent())
            {
                ApiUtils.dedupAndVersion(executor, generatorResult, enrichedDatasets, caseConversion());
            }
        }

        return null;
    }
}
