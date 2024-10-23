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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.finos.legend.engine.persistence.components.common.*;
import org.finos.legend.engine.persistence.components.executor.DigestInfo;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.importer.Importer;
import org.finos.legend.engine.persistence.components.importer.Importers;
import org.finos.legend.engine.persistence.components.ingestmode.*;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.*;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.planner.Planner;
import org.finos.legend.engine.persistence.components.planner.Planners;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.api.utils.ApiUtils;
import org.finos.legend.engine.persistence.components.relational.api.utils.IngestionUtils;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.schemaevolution.SchemaEvolution;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.finos.legend.engine.persistence.components.util.MetadataUtils;
import org.finos.legend.engine.persistence.components.util.PlaceholderValue;
import org.finos.legend.engine.persistence.components.util.TableNameGenUtils;
import org.finos.legend.engine.persistence.components.util.SchemaEvolutionCapability;
import org.finos.legend.engine.persistence.components.util.SqlLogging;
import org.immutables.value.Value;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;

import static org.finos.legend.engine.persistence.components.relational.api.utils.IngestionUtils.ADDITIONAL_METADATA_KEY_PATTERN;
import static org.finos.legend.engine.persistence.components.relational.api.utils.IngestionUtils.ADDITIONAL_METADATA_VALUE_PATTERN;
import static org.finos.legend.engine.persistence.components.relational.api.utils.IngestionUtils.BATCH_END_TS_PATTERN;
import static org.finos.legend.engine.persistence.components.relational.api.utils.IngestionUtils.BATCH_ID_PATTERN;
import static org.finos.legend.engine.persistence.components.relational.api.utils.IngestionUtils.BATCH_START_TS_PATTERN;
import static org.finos.legend.engine.persistence.components.transformer.Transformer.TransformOptionsAbstract.DATE_TIME_FORMATTER;

@Immutable
@Style(
    typeAbstract = "*Abstract",
    typeImmutable = "*",
    jdkOnly = true,
    optionalAcceptNullable = true,
    strictBuilder = true
)
public abstract class RelationalIngestorAbstract
{
    private static final String STAGING = "staging";
    private static final String UNDERSCORE = "_";
    private static final Logger LOGGER = LoggerFactory.getLogger(RelationalIngestor.class);

    //---------- FLAGS ----------

    @Default
    public boolean cleanupStagingData()
    {
        return true;
    }

    @Default
    public boolean createDatasets()
    {
        return true;
    }

    @Default
    public boolean skipMainAndMetadataDatasetCreation()
    {
        return false;
    }

    @Default
    public boolean collectStatistics()
    {
        return true;
    }

    @Default
    public boolean writeStatistics()
    {
        return false;
    }

    @Default
    public boolean enableSchemaEvolution()
    {
        return false;
    }

    @Default
    public boolean enableSchemaEvolutionForMetadataDatasets()
    {
        return true;
    }

    @Default
    public boolean ignoreCaseForSchemaEvolution()
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
    public boolean enableConcurrentSafety()
    {
        return false;
    }

    @Default
    public Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet()
    {
        return Collections.emptySet();
    }

    public abstract Map<String, Object> additionalMetadata();

    public abstract Optional<String> ingestRequestId();

    @Default
    public boolean enableIdempotencyCheck()
    {
        return false;
    }

    @Default
    public SqlLogging sqlLogging()
    {
        return SqlLogging.DISABLED;
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

    @Derived
    public String getRunId()
    {
        return UUID.randomUUID().toString();
    }

    @Value.Check
    void validate()
    {
        // If IdempotencyCheck is enabled, concurrentSafety must be enabled and IngestRequestId must be present
        if (enableIdempotencyCheck() && (!enableConcurrentSafety() || !ingestRequestId().isPresent()))
        {
            throw new IllegalStateException("If IdempotencyCheck is enabled, concurrentSafety must be enabled and IngestRequestId must be present");
        }

        if (!relationalSink().isIngestModeSupported(ingestMode()))
        {
            throw new UnsupportedOperationException("Unsupported ingest mode");
        }
    }

    //---------- FIELDS ----------

    public abstract IngestMode ingestMode();

    public abstract RelationalSink relationalSink();

    @Derived
    protected TransformOptions transformOptions()
    {
        TransformOptions.Builder builder = TransformOptions.builder()
            .executionTimestampClock(executionTimestampClock())
            .batchIdPattern(BATCH_ID_PATTERN);

        relationalSink().optimizerForCaseConversion(caseConversion()).ifPresent(builder::addOptimizers);

        return builder.build();
    }

    // ---------- Private Fields ----------
    private IngestMode enrichedIngestMode;
    private Datasets enrichedDatasets;
    private Transformer<SqlGen, SqlPlan> transformer;
    private Executor<SqlGen, TabularData, SqlPlan> executor;
    private Resources.Builder resourcesBuilder;
    private GeneratorResult generatorResult;
    boolean mainDatasetExists;
    private Planner planner;
    private boolean datasetsInitialized = false;

    // ---------- API ----------

    /*
    - Get Executor
    - @return : The methods returns the Executor to the caller enabling them to handle their own transaction
    */
    public static Executor getExecutor(RelationalSink relationalSink, RelationalConnection connection)
    {
        LOGGER.info("Invoked getExecutor method");
        return relationalSink.getRelationalExecutor(connection);
    }

    /*
    - Initializes executor
    - @return : The methods returns the Executor to the caller enabling them to handle their own transaction
    */
    public Executor initExecutor(RelationalConnection connection)
    {
        LOGGER.info("Invoked initExecutor method, will initialize the executor");
        this.executor = relationalSink().getRelationalExecutor(connection);
        this.executor.setSqlLogging(sqlLogging());
        return executor;
    }

    /*
    - Initializes executor
    */
    public void initExecutor(Executor executor)
    {
        LOGGER.info("Invoked initExecutor method, will initialize the executor");
        this.executor = executor;
        this.executor.setSqlLogging(sqlLogging());
    }

    /*
    - Initializes Datasets
     */
    public Datasets initDatasets(Datasets datasets)
    {
        return enrichDatasetsAndGenerateOperations(datasets);
    }

    /*
    - Create Datasets
    */
    public void create()
    {
        LOGGER.info("Invoked create method, will create the datasets");
        validateDatasetsInitialization();
        createAllDatasets();
        initializeLock();
    }

    public void create(Datasets datasets)
    {
        LOGGER.info("Invoked create(datasets) method, will initialize and create the datasets");
        enrichDatasetsAndGenerateOperationsForCreate(datasets);
        createAllDatasets();
        initializeLock();
    }

    /*
    - Evolve Schema of Target table based on schema changes in staging table
    */
    public SchemaEvolutionResult evolve()
    {
        LOGGER.info("Invoked evolve method, will evolve the schema");
        validateDatasetsInitialization();
        List<String> schemaEvolutionSql = new ArrayList<>();
        schemaEvolutionSql.addAll(evolveMetadataDatasetSchema());
        schemaEvolutionSql.addAll(evolveMainDatasetSchema());
        SchemaEvolutionResult schemaEvolveResult = SchemaEvolutionResult.builder().updatedDatasets(enrichedDatasets).addAllSchemaEvolutionSql(schemaEvolutionSql).build();
        return schemaEvolveResult;
    }

    /*
    - Perform dry run of Ingestion - only supported for Bulk Load
    */
    public DryRunResult dryRun()
    {
        LOGGER.info("Invoked dryRun method, will perform the dryRun");
        validateDatasetsInitialization();
        List<DataError> dataErrors = IngestionUtils.performDryRun(enrichedIngestMode, enrichedDatasets, generatorResult, transformer, executor, relationalSink(), sampleRowCount(), caseConversion());
        IngestStatus ingestStatus = dataErrors.isEmpty() ? IngestStatus.SUCCEEDED : IngestStatus.FAILED;
        DryRunResult dryRunResult = DryRunResult.builder().status(ingestStatus).addAllErrorRecords(dataErrors).build();
        LOGGER.info("DryRun completed");
        return dryRunResult;
    }


    /*
    - Perform ingestion from staging to main dataset based on the Ingest mode, executes in current transaction
    */
    public List<IngestorResult> ingest()
    {
        LOGGER.info("Invoked ingest method, will perform the ingestion");
        validateDatasetsInitialization();
        SchemaEvolutionResult schemaEvolutionResult = SchemaEvolutionResult.builder().updatedDatasets(enrichedDatasets).build();
        acquireLock();

        // idempotency Check
        List<IngestorResult> result = verifyIfRequestAlreadyProcessedPreviously(schemaEvolutionResult);
        if (result.isEmpty())
        {
            dedupAndVersion();
            List<DataSplitRange> dataSplitRanges = IngestionUtils.getDataSplitRanges(executor, planner, transformer, ingestMode());
            result = ingest(dataSplitRanges, schemaEvolutionResult);
            LOGGER.info("Ingestion completed");
        }
        return result;
    }

    /*
    - Perform cleanup of temporary tables
    */
    public void cleanUp()
    {
        LOGGER.info("Invoked cleanUp method, will delete the temporary resources");
        validateDatasetsInitialization();
        postCleanup();
    }

    /*
    Perform full ingestion from Staging to Target table based on the Ingest mode
    Full Ingestion covers:
    1. Export external dataset
    2. Create tables
    3. Evolves Schema
    4. Ingestion from staging to main dataset in a transaction
    5. Clean up of temporary tables
     */
    public List<IngestorResult> performFullIngestion(RelationalConnection connection, Datasets datasets)
    {
        LOGGER.info("Invoked performFullIngestion method");
        return performFullIngestion(connection, datasets, new ArrayList<>());
    }

    /*
    Perform ingestion from Staging to Target table based on the Ingest mode, handling different datasplits in the staging data
    Full Ingestion covers:
    1. Export external dataset
    2. Create tables
    3. Evolves Schema
    4. Ingestion from staging to main dataset in a transaction
    */
    public List<IngestorResult> performFullIngestionWithDataSplits(RelationalConnection connection, Datasets datasets, List<DataSplitRange> dataSplitRanges)
    {
        LOGGER.info("Invoked performFullIngestionWithDataSplits method");
        // Provide the default dataSplit ranges if missing
        if (dataSplitRanges == null || dataSplitRanges.isEmpty())
        {
            dataSplitRanges = Arrays.asList(DataSplitRange.of(1,1));
        }
        return performFullIngestion(connection, datasets, dataSplitRanges);
    }

    /*
    Get the latest staging filters stored in the metadata tables for a dataset
    */
    public List<DatasetFilter> getLatestStagingFilters(RelationalConnection connection, Datasets datasets) throws JsonProcessingException
    {
        LOGGER.info("Invoked getLatestStagingFilters method");
        MetadataDataset metadataDataset = datasets.metadataDataset().isPresent()
                ? datasets.metadataDataset().get()
                : MetadataDataset.builder().build();
        MetadataUtils store = new MetadataUtils(metadataDataset);
        String tableName = datasets.mainDataset().datasetReference().name().orElseThrow(IllegalStateException::new);
        Selection selection = store.getLatestStagingFilters(StringValue.of(tableName));

        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(selection).build();
        Transformer<SqlGen, SqlPlan> transformer = new RelationalTransformer(relationalSink(), transformOptions());
        Executor<SqlGen, TabularData, SqlPlan> executor = relationalSink().getRelationalExecutor(connection);
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        return IngestionUtils.extractDatasetFilters(metadataDataset, executor, physicalPlan);
    }

    // ---------- UTILITY METHODS ----------

    private void validateDatasetsInitialization()
    {
        // Validation: initExecutor must have been invoked
        if (this.executor == null)
        {
            throw new IllegalStateException("Executor not initialized, call initExecutor before invoking this method!");
        }
        // Validation: initDatasets must have been invoked
        if (!this.datasetsInitialized)
        {
            throw new IllegalStateException("Datasets not initialized, call initDatasets before invoking this method!");
        }
    }

    private List<String> evolveMainDatasetSchema()
    {
        List<String> schemaEvolutionSql = new ArrayList<>();
        if (mainDatasetExists && generatorResult.schemaEvolutionDataset().isPresent())
        {
            LOGGER.info("SchemaEvolution is enabled, evolving the schema");
            enrichedDatasets = enrichedDatasets.withMainDataset(generatorResult.schemaEvolutionDataset().get());
            Optional<SqlPlan> schemaEvolutionSqlPlan = generatorResult.schemaEvolutionSqlPlan();
            if (schemaEvolutionSqlPlan.isPresent() && !schemaEvolutionSqlPlan.get().getSqlList().isEmpty())
            {
                executor.executePhysicalPlan(schemaEvolutionSqlPlan.get());
                schemaEvolutionSql = schemaEvolutionSqlPlan.get().getSqlList();
            }
        }
        return schemaEvolutionSql;
    }

    private List<String> evolveMetadataDatasetSchema()
    {
        List<String> schemaEvolutionSql = new ArrayList<>();
        if (enableSchemaEvolutionForMetadataDatasets())
        {
            LOGGER.info("SchemaEvolution for Metadata dataset is enabled, evolving the schema");
            MetadataDataset metadataDataset = enrichedDatasets.metadataDataset().isPresent()
                    ? enrichedDatasets.metadataDataset().get() : MetadataDataset.builder().build();

            Dataset desiredMetadataDataset = metadataDataset.get();
            Dataset existingMetadataDataset = null;

            boolean metadataDatasetExists = executor.datasetExists(desiredMetadataDataset);
            if (metadataDatasetExists)
            {
                existingMetadataDataset = executor.constructDatasetFromDatabase(desiredMetadataDataset);
                Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
                schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);
                SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink(), this.ingestMode(), schemaEvolutionCapabilitySet, true);
                org.finos.legend.engine.persistence.components.schemaevolution.SchemaEvolutionResult schemaEvolutionResult = schemaEvolution.buildLogicalPlanForSchemaEvolution(existingMetadataDataset, desiredMetadataDataset.schema());
                LogicalPlan schemaEvolutionLogicalPlan = schemaEvolutionResult.logicalPlan();
                Optional<SqlPlan> schemaEvolutionSqlPlan = Optional.of(transformer.generatePhysicalPlan(schemaEvolutionLogicalPlan));
                if (schemaEvolutionSqlPlan.isPresent() && !schemaEvolutionSqlPlan.get().getSqlList().isEmpty())
                {
                    executor.executePhysicalPlan(schemaEvolutionSqlPlan.get());
                    schemaEvolutionSql = schemaEvolutionSqlPlan.get().getSqlList();
                }
            }
        }
        return schemaEvolutionSql;
    }

    private void createAllDatasets()
    {
        LOGGER.info("Creating the datasets");
        executor.executePhysicalPlan(generatorResult.preActionsSqlPlan());
    }

    public void dedupAndVersion()
    {
        if (generatorResult.deduplicationAndVersioningSqlPlan().isPresent())
        {
            LOGGER.info("Executing Deduplication and Versioning");
            IngestionUtils.dedupAndVersion(executor, generatorResult, enrichedDatasets, caseConversion(), new HashMap<>());
        }
    }

    private void initializeLock()
    {
        if (enableConcurrentSafety())
        {
            LOGGER.info("Concurrent safety is enabled, Initializing lock");
            Map<String, PlaceholderValue> placeHolderKeyValues = new HashMap<>();
            placeHolderKeyValues.put(BATCH_START_TS_PATTERN, PlaceholderValue.of(LocalDateTime.now(executionTimestampClock()).format(DATE_TIME_FORMATTER), false));
            try
            {
                executor.executePhysicalPlan(generatorResult.initializeLockSqlPlan().orElseThrow(IllegalStateException::new), placeHolderKeyValues);
            }
            catch (Exception e)
            {
                // Ignore this exception
                // In race condition: multiple jobs will try to insert same row
            }
        }
    }

    private void acquireLock()
    {
        if (enableConcurrentSafety())
        {
            LOGGER.info("Concurrent safety is enabled, Acquiring lock");
            Map<String, PlaceholderValue> placeHolderKeyValues = new HashMap<>();
            placeHolderKeyValues.put(BATCH_START_TS_PATTERN, PlaceholderValue.of(LocalDateTime.now(executionTimestampClock()).format(DATE_TIME_FORMATTER), false));
            executor.executePhysicalPlan(generatorResult.acquireLockSqlPlan().orElseThrow(IllegalStateException::new), placeHolderKeyValues);
        }
    }

    private List<IngestorResult> verifyIfRequestAlreadyProcessedPreviously(SchemaEvolutionResult schemaEvolutionResult)
    {
        if (enableConcurrentSafety() && enableIdempotencyCheck())
        {
            return IngestionUtils.verifyIfRequestAlreadyProcessedPreviously(schemaEvolutionResult, enrichedDatasets, ingestRequestId(), transformer, executor, batchSuccessStatusValue());
        }
        return new ArrayList<>();
    }

    private void postCleanup()
    {
        if (generatorResult.postCleanupSqlPlan().isPresent())
        {
            LOGGER.info("Executing Post Clean up");
            executor.executePhysicalPlan(generatorResult.postCleanupSqlPlan().get());
        }
    }

    private List<IngestorResult> ingest(List<DataSplitRange> dataSplitRanges, SchemaEvolutionResult schemaEvolutionResult)
    {
        if (enrichedIngestMode instanceof BulkLoad)
        {
            LOGGER.info("Starting Bulk Load");
            return IngestionUtils.performBulkLoad(enrichedDatasets, transformer, planner, executor, generatorResult, enrichedIngestMode, schemaEvolutionResult, additionalMetadata(), executionTimestampClock(), relationalSink(), Optional.empty());
        }
        else
        {
            LOGGER.info(String.format("Starting Ingestion with IngestMode: {%s}", enrichedIngestMode.getClass().getSimpleName()));
            return IngestionUtils.performIngestion(enrichedDatasets, transformer, planner, executor, generatorResult, dataSplitRanges, enrichedIngestMode, schemaEvolutionResult, additionalMetadata(), executionTimestampClock(), Optional.empty());
        }
    }

    private List<IngestorResult> performFullIngestion(RelationalConnection connection, Datasets datasets, List<DataSplitRange> dataSplitRanges)
    {
        // 1. init
        initExecutor(connection);
        initDatasets(datasets);

        // 2. Create Datasets
        if (createDatasets())
        {
            createAllDatasets();
            initializeLock();
        }

        // Evolve Schema
        SchemaEvolutionResult schemaEvolutionResult = evolve();
        List<IngestorResult> result;

        try
        {
            executor.begin();
            acquireLock();

            // idempotency Check
            result = verifyIfRequestAlreadyProcessedPreviously(schemaEvolutionResult);
            if (result.isEmpty())
            {
                // Dedup and Version
                dedupAndVersion();
                // Find the data split ranges based on the result of dedup and versioning
                if (dataSplitRanges.isEmpty())
                {
                    dataSplitRanges = IngestionUtils.getDataSplitRanges(executor, planner, transformer, ingestMode());
                }

                // Perform Ingestion
                result = ingest(dataSplitRanges, schemaEvolutionResult);
            }
            executor.commit();
        }
        catch (Exception e)
        {
            executor.revert();
            throw e;
        }
        finally
        {
            executor.close();
            // post Cleanup
            postCleanup();
        }

        LOGGER.info("Ingestion completed");
        return result;
    }

    private Datasets enrichDatasetsAndGenerateOperationsForCreate(Datasets datasets)
    {
        LOGGER.info("Initializing Datasets for create()");

        // Validation: init(Connection) must have been invoked
        if (this.executor == null)
        {
            throw new IllegalStateException("Executor not initialized, call init(Connection) before invoking this method!");
        }

        // 1. Case handling
        enrichedIngestMode = ApiUtils.applyCase(ingestMode(), caseConversion());
        enrichedDatasets = ApiUtils.enrichAndApplyCase(datasets, caseConversion(), enableConcurrentSafety());

        // 2. Initialize transformer
        transformer = new RelationalTransformer(relationalSink(), transformOptions());
        resourcesBuilder = Resources.builder();

        // 3. Derive main dataset schema
        if (enableSchemaEvolution())
        {
            if (executor.datasetExists(enrichedDatasets.mainDataset()))
            {
                enrichedDatasets = enrichedDatasets.withMainDataset(executor.constructDatasetFromDatabase(enrichedDatasets.mainDataset()));
                mainDatasetExists = true;
            }
            else
            {
                enrichedDatasets = enrichedDatasets.withMainDataset(ApiUtils.deriveMainDatasetFromStaging(enrichedDatasets.mainDataset(), enrichedDatasets.stagingDataset(), enrichedIngestMode));
            }
        }
        else
        {
            enrichedDatasets = enrichedDatasets.withMainDataset(ApiUtils.deriveMainDatasetFromStaging(enrichedDatasets.mainDataset(), enrichedDatasets.stagingDataset(), enrichedIngestMode));
        }

        // 4. generate sql plans
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(enrichedIngestMode)
            .relationalSink(relationalSink())
            .cleanupStagingData(cleanupStagingData())
            .collectStatistics(collectStatistics())
            .writeStatistics(writeStatistics())
            .skipMainAndMetadataDatasetCreation(skipMainAndMetadataDatasetCreation())
            .enableSchemaEvolution(enableSchemaEvolution())
            .addAllSchemaEvolutionCapabilitySet(schemaEvolutionCapabilitySet())
            .ignoreCaseForSchemaEvolution(ignoreCaseForSchemaEvolution())
            .enableConcurrentSafety(enableConcurrentSafety())
            .caseConversion(caseConversion())
            .executionTimestampClock(executionTimestampClock())
            .batchStartTimestampPattern(BATCH_START_TS_PATTERN)
            .batchEndTimestampPattern(BATCH_END_TS_PATTERN)
            .batchIdPattern(BATCH_ID_PATTERN)
            .ingestRequestId(ingestRequestId())
            .batchSuccessStatusValue(batchSuccessStatusValue())
            .sampleRowCount(sampleRowCount())
            .ingestRunId(getRunId())
            .build();

        planner = Planners.get(enrichedDatasets, enrichedIngestMode, generator.plannerOptions(), relationalSink().capabilities());
        generatorResult = generator.generateOperationsForCreate(planner);

        return enrichedDatasets;
    }

    private Datasets enrichDatasetsAndGenerateOperations(Datasets datasets)
    {
        LOGGER.info("Initializing Datasets");
        // Validation: init(Connection) must have been invoked
        if (this.executor == null)
        {
            throw new IllegalStateException("Executor not initialized, call init(Connection) before invoking this method!");
        }

        // 1. Case handling
        enrichedIngestMode = ApiUtils.applyCase(ingestMode(), caseConversion());
        enrichedDatasets = ApiUtils.enrichAndApplyCase(datasets, caseConversion(), enableConcurrentSafety());

        // 2. Initialize transformer
        transformer = new RelationalTransformer(relationalSink(), transformOptions());
        resourcesBuilder = Resources.builder();

        // 3. import external dataset
        if (enrichedDatasets.stagingDataset() instanceof ExternalDatasetReference)
        {
            enrichedDatasets = importExternalDataset(enrichedDatasets);
        }

        // 4. Check if staging dataset is empty
        if (ingestMode().accept(IngestModeVisitors.NEED_TO_CHECK_STAGING_EMPTY) && executor.datasetExists(enrichedDatasets.stagingDataset()))
        {
            boolean isStagingDatasetEmpty = IngestionUtils.datasetEmpty(enrichedDatasets.stagingDataset(), transformer, executor, new HashMap<>());
            LOGGER.info(String.format("Checking if staging dataset is empty : {%s}", isStagingDatasetEmpty));
            resourcesBuilder.stagingDataSetEmpty(isStagingDatasetEmpty);
        }

        // 5. Derive main dataset schema
        if (enableSchemaEvolution())
        {
            if (executor.datasetExists(enrichedDatasets.mainDataset()))
            {
                enrichedDatasets = enrichedDatasets.withMainDataset(executor.constructDatasetFromDatabase(enrichedDatasets.mainDataset()));
                mainDatasetExists = true;
            }
            else
            {
                enrichedDatasets = enrichedDatasets.withMainDataset(ApiUtils.deriveMainDatasetFromStaging(enrichedDatasets.mainDataset(), enrichedDatasets.stagingDataset(), enrichedIngestMode));
            }
        }
        else
        {
            enrichedDatasets = enrichedDatasets.withMainDataset(ApiUtils.deriveMainDatasetFromStaging(enrichedDatasets.mainDataset(), enrichedDatasets.stagingDataset(), enrichedIngestMode));
        }

        // 6. Add Optimization Columns if needed
        enrichedIngestMode = enrichedIngestMode.accept(new IngestModeOptimizer(enrichedDatasets, executor, transformer));

        // 7. Use a placeholder for additional metadata
        Map<String, Object> placeholderAdditionalMetadata = new HashMap<>();
        if (!additionalMetadata().isEmpty())
        {
            placeholderAdditionalMetadata = Collections.singletonMap(ADDITIONAL_METADATA_KEY_PATTERN, ADDITIONAL_METADATA_VALUE_PATTERN);
        }

        // 8. generate sql plans
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(enrichedIngestMode)
                .relationalSink(relationalSink())
                .cleanupStagingData(cleanupStagingData())
                .collectStatistics(collectStatistics())
                .writeStatistics(writeStatistics())
                .skipMainAndMetadataDatasetCreation(skipMainAndMetadataDatasetCreation())
                .enableSchemaEvolution(enableSchemaEvolution())
                .addAllSchemaEvolutionCapabilitySet(schemaEvolutionCapabilitySet())
                .ignoreCaseForSchemaEvolution(ignoreCaseForSchemaEvolution())
                .enableConcurrentSafety(enableConcurrentSafety())
                .caseConversion(caseConversion())
                .executionTimestampClock(executionTimestampClock())
                .batchStartTimestampPattern(BATCH_START_TS_PATTERN)
                .batchEndTimestampPattern(BATCH_END_TS_PATTERN)
                .batchIdPattern(BATCH_ID_PATTERN)
                .putAllAdditionalMetadata(placeholderAdditionalMetadata)
                .ingestRequestId(ingestRequestId())
                .batchSuccessStatusValue(batchSuccessStatusValue())
                .sampleRowCount(sampleRowCount())
                .ingestRunId(getRunId())
                .build();

        planner = Planners.get(enrichedDatasets, enrichedIngestMode, generator.plannerOptions(), relationalSink().capabilities());
        generatorResult = generator.generateOperations(enrichedDatasets, resourcesBuilder.build(), planner, enrichedIngestMode);
        datasetsInitialized = true;
        return enrichedDatasets;
    }

    private Datasets importExternalDataset(Datasets datasets)
    {
        LOGGER.info("Importing External Dataset");
        ExternalDatasetReference externalDatasetReference = (ExternalDatasetReference) datasets.stagingDataset();
        DatasetReference mainDataSetReference = datasets.mainDataset().datasetReference();

        externalDatasetReference = externalDatasetReference
            .withName(externalDatasetReference.name().isPresent() ? externalDatasetReference.name().get() : TableNameGenUtils.generateTableName(mainDataSetReference.name().orElseThrow(IllegalStateException::new), STAGING, getRunId()))
            .withDatabase(externalDatasetReference.database().isPresent() ? externalDatasetReference.database().get() : mainDataSetReference.database().orElse(null))
            .withGroup(externalDatasetReference.group().isPresent() ? externalDatasetReference.group().get() : mainDataSetReference.group().orElse(null))
            .withAlias(externalDatasetReference.alias().isPresent() ? externalDatasetReference.alias().get() : mainDataSetReference.alias().orElseThrow(RuntimeException::new) + UNDERSCORE + STAGING);

        // TODO : Auto infer schema in future

        // Prepare DigestInfo
        boolean hasDigestField = enrichedIngestMode.accept(IngestModeVisitors.DIGEST_REQUIRED);
        Optional<String> digestFieldOptional = enrichedIngestMode.accept(IngestModeVisitors.EXTRACT_DIGEST_FIELD);
        boolean populateDigest = hasDigestField && externalDatasetReference.schema().fields().stream().noneMatch(field -> field.name().equalsIgnoreCase(digestFieldOptional.orElseThrow(IllegalStateException::new)));

        if (populateDigest)
        {
            List<Field> fields = new ArrayList<>(externalDatasetReference.schema().fields());
            DeriveMainDatasetSchemaFromStaging.addDigestField(fields, digestFieldOptional.get());
            externalDatasetReference = externalDatasetReference.withSchema(externalDatasetReference.schema().withFields(fields));
        }

        Dataset extractedStagingDatasetDefinition = externalDatasetReference.getDatasetDefinition();
        Datasets updatedDatasets = datasets.withStagingDataset(extractedStagingDatasetDefinition);

        // Create staging table
        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(extractedStagingDatasetDefinition, true);
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        executor.executePhysicalPlan(tableCreationPhysicalPlan);

        // Load staging data
        Set<String> metaFields = enrichedIngestMode.accept(IngestModeVisitors.META_FIELDS_TO_EXCLUDE_FROM_DIGEST);
        DigestInfo digestInfo = DigestInfo.builder().populateDigest(populateDigest).digestField(digestFieldOptional.orElse(null)).addAllMetaFields(metaFields).build();
        Importer importer = Importers.forExternalDatasetReference(externalDatasetReference, transformer, executor);
        importer.importData(externalDatasetReference, digestInfo);
        resourcesBuilder.externalDatasetImported(true);

        return updatedDatasets;
    }
}
