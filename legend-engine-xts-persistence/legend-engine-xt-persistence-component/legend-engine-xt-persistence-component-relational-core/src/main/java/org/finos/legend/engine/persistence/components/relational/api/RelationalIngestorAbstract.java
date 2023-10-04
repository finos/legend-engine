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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.OptimizationFilter;
import org.finos.legend.engine.persistence.components.common.Resources;
import org.finos.legend.engine.persistence.components.common.DatasetFilter;
import org.finos.legend.engine.persistence.components.common.FilterType;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.executor.DigestInfo;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.importer.Importer;
import org.finos.legend.engine.persistence.components.importer.Importers;
import org.finos.legend.engine.persistence.components.ingestmode.DeriveMainDatasetSchemaFromStaging;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.IngestModeOptimizationColumnHandler;
import org.finos.legend.engine.persistence.components.ingestmode.IngestModeVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.BulkLoad;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.ExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.ingestmode.TempDatasetsEnricher;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.planner.Planner;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.planner.Planners;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.finos.legend.engine.persistence.components.util.MetadataUtils;
import org.finos.legend.engine.persistence.components.util.SchemaEvolutionCapability;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory.MAX_OF_FIELD;
import static org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory.MIN_OF_FIELD;
import static org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory.TABLE_IS_NON_EMPTY;
import static org.finos.legend.engine.persistence.components.relational.api.RelationalGeneratorAbstract.BULK_LOAD_BATCH_STATUS_PATTERN;
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
    private static final String SINGLE_QUOTE = "'";

    public static final String BATCH_ID_PATTERN = "{NEXT_BATCH_ID_PATTERN}";
    public static final String BATCH_START_TS_PATTERN = "{BATCH_START_TIMESTAMP_PLACEHOLDER}";
    private static final String BATCH_END_TS_PATTERN = "{BATCH_END_TIMESTAMP_PLACEHOLDER}";

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
    public boolean createStagingDataset()
    {
        return false;
    }

    @Default
    public boolean collectStatistics()
    {
        return true;
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
    public boolean enableConcurrentSafety()
    {
        return false;
    }

    @Default
    public Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet()
    {
        return Collections.emptySet();
    }

    //---------- FIELDS ----------

    public abstract IngestMode ingestMode();

    public abstract RelationalSink relationalSink();

    public abstract Optional<String> bulkLoadTaskIdValue();

    @Derived
    protected PlannerOptions plannerOptions()
    {
        return PlannerOptions.builder()
            .cleanupStagingData(cleanupStagingData())
            .collectStatistics(collectStatistics())
            .enableSchemaEvolution(enableSchemaEvolution())
            .createStagingDataset(createStagingDataset())
            .enableConcurrentSafety(enableConcurrentSafety())
            .bulkLoadTaskIdValue(bulkLoadTaskIdValue())
            .build();
    }

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


    // ---------- API ----------

    /*
    - Initializes executor
    - @return : The methods returns the Executor to the caller enabling them to handle their own transaction
    */
    public Executor init(RelationalConnection connection)
    {
        LOGGER.info("Invoked init method, will initialize the executor");
        this.executor = relationalSink().getRelationalExecutor(connection);
        return executor;
    }

    /*
    - Create Datasets
    */
    public Datasets create(Datasets datasets)
    {
        LOGGER.info("Invoked create method, will create the datasets");
        init(datasets);
        createAllDatasets();
        initializeLock();
        return this.enrichedDatasets;
    }

    /*
    - Evolve Schema of Target table based on schema changes in staging table
    */
    public Datasets evolve(Datasets datasets)
    {
        LOGGER.info("Invoked evolve method, will evolve the schema");
        init(datasets);
        evolveSchema();
        return this.enrichedDatasets;
    }

    /*
    - Perform ingestion from staging to main dataset based on the Ingest mode, executes in current transaction
    */
    public IngestorResult ingest(Datasets datasets)
    {
        LOGGER.info("Invoked ingest method, will perform the ingestion");
        init(datasets);
        IngestorResult result = ingest(Arrays.asList()).stream().findFirst().orElseThrow(IllegalStateException::new);
        LOGGER.info("Ingestion completed");
        return result;
    }

    /*
    - Perform cleanup of temporary tables
    */
    public Datasets cleanUp(Datasets datasets)
    {
        LOGGER.info("Invoked cleanUp method, will delete the temporary resources");
        init(datasets);
        postCleanup();
        return this.enrichedDatasets;
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
    public IngestorResult performFullIngestion(RelationalConnection connection, Datasets datasets)
    {
        LOGGER.info("Invoked performFullIngestion method");
        return performFullIngestion(connection, datasets, null).stream().findFirst().orElseThrow(IllegalStateException::new);
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
        return extractDatasetFilters(metadataDataset, executor, physicalPlan);
    }

    // ---------- UTILITY METHODS ----------

    private void evolveSchema()
    {
        if (mainDatasetExists && generatorResult.schemaEvolutionDataset().isPresent())
        {
            LOGGER.info("SchemaEvolution is enabled, evolving the schema");
            enrichedDatasets = enrichedDatasets.withMainDataset(generatorResult.schemaEvolutionDataset().get());
            generatorResult.schemaEvolutionSqlPlan().ifPresent(executor::executePhysicalPlan);
        }
    }

    private void createAllDatasets()
    {
        LOGGER.info("Creating the datasets");
        executor.executePhysicalPlan(generatorResult.preActionsSqlPlan());
    }

    private void initializeLock()
    {
        if (enableConcurrentSafety())
        {
            LOGGER.info("Concurrent safety is enabled, Initializing lock");
            Map<String, String> placeHolderKeyValues = new HashMap<>();
            placeHolderKeyValues.put(BATCH_START_TS_PATTERN, LocalDateTime.now(executionTimestampClock()).format(DATE_TIME_FORMATTER));
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
            Map<String, String> placeHolderKeyValues = new HashMap<>();
            placeHolderKeyValues.put(BATCH_START_TS_PATTERN, LocalDateTime.now(executionTimestampClock()).format(DATE_TIME_FORMATTER));
            executor.executePhysicalPlan(generatorResult.acquireLockSqlPlan().orElseThrow(IllegalStateException::new), placeHolderKeyValues);
        }
    }

    private void postCleanup()
    {
        if (generatorResult.postCleanupSqlPlan().isPresent())
        {
            LOGGER.info("Executing Post Clean up");
            executor.executePhysicalPlan(generatorResult.postCleanupSqlPlan().get());
        }
    }

    private List<IngestorResult> ingest(List<DataSplitRange> dataSplitRanges)
    {
        if (enrichedIngestMode instanceof BulkLoad)
        {
            LOGGER.info("Starting Bulk Load");
            return performBulkLoad(enrichedDatasets, transformer, planner, executor, generatorResult, enrichedIngestMode);
        }
        else
        {
            LOGGER.info(String.format("Starting Ingestion with IngestMode: {%s}", enrichedIngestMode.getClass().getSimpleName()));
            return performIngestion(enrichedDatasets, transformer, planner, executor, generatorResult, dataSplitRanges, enrichedIngestMode);
        }
    }

    private List<IngestorResult> performFullIngestion(RelationalConnection connection, Datasets datasets, List<DataSplitRange> dataSplitRanges)
    {
        // 1. init
        init(connection);
        init(datasets);

        // 2. Create Datasets
        if (createDatasets())
        {
            createAllDatasets();
            initializeLock();
        }

        // Evolve Schema
        evolveSchema();

        // Perform Ingestion
        List<IngestorResult> result;
        try
        {
            executor.begin();
            result = ingest(dataSplitRanges);
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
        }

        // post Cleanup
        postCleanup();
        LOGGER.info("Ingestion completed");
        return result;
    }


    private void init(Datasets datasets)
    {
        LOGGER.info("Initializing Datasets");
        // Validation: init(Connection) must have been invoked
        if (this.executor == null)
        {
            throw new IllegalStateException("Executor not initialized, call init(Connection) before invoking this method!");
        }
        // 1. Case handling
        enrichedIngestMode = ApiUtils.applyCase(ingestMode(), caseConversion());
        enrichedDatasets = ApiUtils.enrichAndApplyCase(datasets, caseConversion());

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
            boolean isStagingDatasetEmpty = datasetEmpty(enrichedDatasets.stagingDataset(), transformer, executor);
            LOGGER.info(String.format("Checking if staging dataset is empty : {%s}", isStagingDatasetEmpty));
            resourcesBuilder.stagingDataSetEmpty(isStagingDatasetEmpty);
        }

        // 5. Check if main Dataset Exists
        mainDatasetExists = executor.datasetExists(enrichedDatasets.mainDataset());
        if (mainDatasetExists && enableSchemaEvolution())
        {
            enrichedDatasets = enrichedDatasets.withMainDataset(executor.constructDatasetFromDatabase(enrichedDatasets.mainDataset()));
        }
        else
        {
            enrichedDatasets = enrichedDatasets.withMainDataset(ApiUtils.deriveMainDatasetFromStaging(enrichedDatasets, enrichedIngestMode));
        }

        // 6. Add Optimization Columns if needed
        enrichedIngestMode = enrichedIngestMode.accept(new IngestModeOptimizationColumnHandler(enrichedDatasets));

        // 7. Enrich temp Datasets
        enrichedDatasets = enrichedIngestMode.accept(new TempDatasetsEnricher(enrichedDatasets));

        // 8. generate sql plans
        RelationalGenerator generator = RelationalGenerator.builder()
                .ingestMode(enrichedIngestMode)
                .relationalSink(relationalSink())
                .cleanupStagingData(cleanupStagingData())
                .collectStatistics(collectStatistics())
                .createStagingDataset(createStagingDataset())
                .enableSchemaEvolution(enableSchemaEvolution())
                .addAllSchemaEvolutionCapabilitySet(schemaEvolutionCapabilitySet())
                .caseConversion(caseConversion())
                .executionTimestampClock(executionTimestampClock())
                .batchStartTimestampPattern(BATCH_START_TS_PATTERN)
                .batchEndTimestampPattern(BATCH_END_TS_PATTERN)
                .batchIdPattern(BATCH_ID_PATTERN)
                .bulkLoadTaskIdValue(bulkLoadTaskIdValue())
                .build();

        planner = Planners.get(enrichedDatasets, enrichedIngestMode, plannerOptions(), relationalSink().capabilities());
        generatorResult = generator.generateOperations(enrichedDatasets, resourcesBuilder.build(), planner, enrichedIngestMode);
    }

    private List<IngestorResult> performIngestion(Datasets datasets, Transformer<SqlGen, SqlPlan> transformer, Planner planner, Executor<SqlGen,
        TabularData, SqlPlan> executor, GeneratorResult generatorResult, List<DataSplitRange> dataSplitRanges, IngestMode ingestMode)
    {
         List<IngestorResult> results = new ArrayList<>();
         int dataSplitIndex = 0;
         int dataSplitsCount = (dataSplitRanges == null || dataSplitRanges.isEmpty()) ? 0 : dataSplitRanges.size();
         acquireLock();
         do
         {
             Optional<DataSplitRange> dataSplitRange = Optional.ofNullable(dataSplitsCount == 0 ? null : dataSplitRanges.get(dataSplitIndex));
             // Extract the Placeholders values
             Map<String, String> placeHolderKeyValues = extractPlaceHolderKeyValues(datasets, executor, planner, transformer, ingestMode, dataSplitRange);
             // Load main table, extract stats and update metadata table
             Map<StatisticName, Object> statisticsResultMap = loadData(executor, generatorResult, placeHolderKeyValues);
             IngestorResult result = IngestorResult.builder()
                 .putAllStatisticByName(statisticsResultMap)
                 .updatedDatasets(datasets)
                 .batchId(Optional.ofNullable(placeHolderKeyValues.containsKey(BATCH_ID_PATTERN) ? Integer.valueOf(placeHolderKeyValues.get(BATCH_ID_PATTERN)) : null))
                 .dataSplitRange(dataSplitRange)
                 .schemaEvolutionSql(generatorResult.schemaEvolutionSql())
                 .status(IngestStatus.SUCCEEDED)
                 .ingestionTimestampUTC(placeHolderKeyValues.get(BATCH_START_TS_PATTERN))
                 .build();
             results.add(result);
             dataSplitIndex++;
         }
         while (planner.dataSplitExecutionSupported() && dataSplitIndex < dataSplitsCount);
         // Clean up
         executor.executePhysicalPlan(generatorResult.postActionsSqlPlan());
         return results;
    }

    private Map<StatisticName, Object> loadData(Executor<SqlGen, TabularData, SqlPlan> executor, GeneratorResult generatorResult, Map<String, String> placeHolderKeyValues)
    {
        // Extract preIngest Statistics
        Map<StatisticName, Object> statisticsResultMap = new HashMap<>(
            executeStatisticsPhysicalPlan(executor, generatorResult.preIngestStatisticsSqlPlan(), placeHolderKeyValues));
        // Execute ingest SqlPlan
        executor.executePhysicalPlan(generatorResult.ingestSqlPlan(), placeHolderKeyValues);
        // Extract postIngest Statistics
        statisticsResultMap.putAll(
            executeStatisticsPhysicalPlan(executor, generatorResult.postIngestStatisticsSqlPlan(), placeHolderKeyValues));
        // Execute metadata ingest SqlPlan
        if (generatorResult.metadataIngestSqlPlan().isPresent())
        {
            // add batchEndTimestamp
            placeHolderKeyValues.put(BATCH_END_TS_PATTERN, LocalDateTime.now(executionTimestampClock()).format(DATE_TIME_FORMATTER));
            executor.executePhysicalPlan(generatorResult.metadataIngestSqlPlan().get(), placeHolderKeyValues);
        }
        return statisticsResultMap;
    }


    private List<IngestorResult> performBulkLoad(Datasets datasets, Transformer<SqlGen, SqlPlan> transformer, Planner planner, Executor<SqlGen, TabularData, SqlPlan> executor, GeneratorResult generatorResult, IngestMode ingestMode)
    {
        List<IngestorResult> results = new ArrayList<>();
        Map<String, String> placeHolderKeyValues = extractPlaceHolderKeyValues(datasets, executor, planner, transformer, ingestMode, Optional.empty());

        // Execute ingest SqlPlan
        IngestorResult result = relationalSink().performBulkLoad(datasets, executor, generatorResult.ingestSqlPlan(), generatorResult.postIngestStatisticsSqlPlan(), placeHolderKeyValues);
        // Execute metadata ingest SqlPlan
        if (generatorResult.metadataIngestSqlPlan().isPresent())
        {
            // add batchEndTimestamp
            placeHolderKeyValues.put(BATCH_END_TS_PATTERN, LocalDateTime.now(executionTimestampClock()).format(DATE_TIME_FORMATTER));
            placeHolderKeyValues.put(BULK_LOAD_BATCH_STATUS_PATTERN, result.status().name());
            executor.executePhysicalPlan(generatorResult.metadataIngestSqlPlan().get(), placeHolderKeyValues);
        }
        results.add(result);
        // Clean up
        executor.executePhysicalPlan(generatorResult.postActionsSqlPlan());

        return results;
    }


    private Datasets importExternalDataset(Datasets datasets)
    {
        LOGGER.info("Importing External Dataset");
        ExternalDatasetReference externalDatasetReference = (ExternalDatasetReference) datasets.stagingDataset();
        DatasetReference mainDataSetReference = datasets.mainDataset().datasetReference();

        externalDatasetReference = externalDatasetReference
            .withName(externalDatasetReference.name().isPresent() ? externalDatasetReference.name().get() : LogicalPlanUtils.generateTableNameWithSuffix(mainDataSetReference.name().orElseThrow(IllegalStateException::new), STAGING))
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

    private boolean datasetEmpty(Dataset dataset, Transformer<SqlGen, SqlPlan> transformer, Executor<SqlGen, TabularData, SqlPlan> executor)
    {
        LogicalPlan checkIsDatasetEmptyLogicalPlan = LogicalPlanFactory.getLogicalPlanForIsDatasetEmpty(dataset);
        SqlPlan physicalPlanForCheckIsDataSetEmpty = transformer.generatePhysicalPlan(checkIsDatasetEmptyLogicalPlan);
        List<TabularData> results = executor.executePhysicalPlanAndGetResults(physicalPlanForCheckIsDataSetEmpty);

        String value = String.valueOf(results.stream()
            .findFirst()
            .map(TabularData::getData)
            .flatMap(t -> t.stream().findFirst())
            .map(Map::values)
            .flatMap(t -> t.stream().findFirst())
            .orElseThrow(IllegalStateException::new));
        return !value.equals(TABLE_IS_NON_EMPTY);
    }

    private Map<StatisticName, Object> executeStatisticsPhysicalPlan(Executor<SqlGen, TabularData, SqlPlan> executor,
                                                                     Map<StatisticName, SqlPlan> statisticsSqlPlan,
                                                                     Map<String, String> placeHolderKeyValues)
    {
        return statisticsSqlPlan.keySet()
            .stream()
            .collect(Collectors.toMap(
                k -> k,
                k -> executor.executePhysicalPlanAndGetResults(statisticsSqlPlan.get(k), placeHolderKeyValues)
                    .stream()
                    .findFirst()
                    .map(TabularData::getData)
                    .flatMap(t -> t.stream().findFirst())
                    .map(Map::values)
                    .flatMap(t -> t.stream().findFirst())
                    .orElseThrow(IllegalStateException::new)));
    }

    private Map<String, String> extractPlaceHolderKeyValues(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor,
                                                            Planner planner, Transformer<SqlGen, SqlPlan> transformer, IngestMode ingestMode,
                                                            Optional<DataSplitRange> dataSplitRange)
    {
        Map<String, String> placeHolderKeyValues = new HashMap<>();
        Optional<Long> nextBatchId = getNextBatchId(datasets, executor, transformer, ingestMode);
        Optional<Map<OptimizationFilter, Pair<Object, Object>>> optimizationFilters = getOptimizationFilterBounds(datasets, executor, transformer, ingestMode);
        if (nextBatchId.isPresent())
        {
            LOGGER.info(String.format("Obtained the next Batch id: %s", nextBatchId.get()));
            placeHolderKeyValues.put(BATCH_ID_PATTERN, nextBatchId.get().toString());
        }
        if (optimizationFilters.isPresent())
        {
            for (OptimizationFilter filter : optimizationFilters.get().keySet())
            {
                Object lowerBound = optimizationFilters.get().get(filter).getOne();
                Object upperBound = optimizationFilters.get().get(filter).getTwo();
                if (lowerBound instanceof Date)
                {
                    placeHolderKeyValues.put(filter.lowerBoundPattern(), lowerBound.toString());
                    placeHolderKeyValues.put(filter.upperBoundPattern(), upperBound.toString());
                }
                else if (lowerBound instanceof Number)
                {
                    placeHolderKeyValues.put(SINGLE_QUOTE + filter.lowerBoundPattern() + SINGLE_QUOTE, lowerBound.toString());
                    placeHolderKeyValues.put(SINGLE_QUOTE + filter.upperBoundPattern() + SINGLE_QUOTE, upperBound.toString());
                }
                else
                {
                    throw new IllegalStateException("Unexpected data type for optimization filter");
                }
            }
        }
        if (planner.dataSplitExecutionSupported() && dataSplitRange.isPresent())
        {
            placeHolderKeyValues.put(SINGLE_QUOTE + LogicalPlanUtils.DATA_SPLIT_LOWER_BOUND_PLACEHOLDER + SINGLE_QUOTE, String.valueOf(dataSplitRange.get().lowerBound()));
            placeHolderKeyValues.put(SINGLE_QUOTE + LogicalPlanUtils.DATA_SPLIT_UPPER_BOUND_PLACEHOLDER + SINGLE_QUOTE, String.valueOf(dataSplitRange.get().upperBound()));
        }
        placeHolderKeyValues.put(BATCH_START_TS_PATTERN, LocalDateTime.now(executionTimestampClock()).format(DATE_TIME_FORMATTER));
        return placeHolderKeyValues;
    }

    private Optional<Long> getNextBatchId(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor,
                                             Transformer<SqlGen, SqlPlan> transformer, IngestMode ingestMode)
    {
        if (ingestMode.accept(IngestModeVisitors.IS_INGEST_MODE_TEMPORAL) || ingestMode instanceof BulkLoad)
        {
            LogicalPlan logicalPlanForNextBatchId = LogicalPlanFactory.getLogicalPlanForNextBatchId(datasets, ingestMode);
            List<TabularData> tabularData = executor.executePhysicalPlanAndGetResults(transformer.generatePhysicalPlan(logicalPlanForNextBatchId));
            Optional<Object> nextBatchId = Optional.ofNullable(tabularData.stream()
                .findFirst()
                .map(TabularData::getData)
                .flatMap(t -> t.stream().findFirst())
                .map(Map::values)
                .flatMap(t -> t.stream().findFirst())
                .orElseThrow(IllegalStateException::new));
            if (nextBatchId.isPresent())
            {
                if (nextBatchId.get() instanceof Integer)
                {
                    return Optional.of(Long.valueOf((Integer) nextBatchId.get()));
                }
                if (nextBatchId.get() instanceof Long)
                {
                    return Optional.of((Long) nextBatchId.get());
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Map<OptimizationFilter, Pair<Object, Object>>> getOptimizationFilterBounds(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor,
                                                                                        Transformer<SqlGen, SqlPlan> transformer, IngestMode ingestMode)
    {
        List<OptimizationFilter> filters = ingestMode.accept(IngestModeVisitors.RETRIEVE_OPTIMIZATION_FILTERS);
        if (!filters.isEmpty())
        {
            Map<OptimizationFilter, Pair<Object, Object>> map = new HashMap<>();
            for (OptimizationFilter filter : filters)
            {
                LogicalPlan logicalPlanForMinAndMaxForField = LogicalPlanFactory.getLogicalPlanForMinAndMaxForField(datasets.stagingDataset(), filter.fieldName());
                List<TabularData> tabularData = executor.executePhysicalPlanAndGetResults(transformer.generatePhysicalPlan(logicalPlanForMinAndMaxForField));
                Map<String, Object> resultMap = tabularData.stream()
                    .findFirst()
                    .map(TabularData::getData)
                    .flatMap(t -> t.stream().findFirst())
                    .orElseThrow(IllegalStateException::new);
                // Put into map only when not null
                Object lower = resultMap.get(MIN_OF_FIELD);
                Object upper = resultMap.get(MAX_OF_FIELD);
                if (lower != null && upper != null)
                {
                    map.put(filter, Tuples.pair(lower, upper));
                }
            }
            return Optional.of(map);
        }
        return Optional.empty();
    }

    private List<DatasetFilter> extractDatasetFilters(MetadataDataset metadataDataset, Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan physicalPlan) throws JsonProcessingException
    {
        List<DatasetFilter> datasetFilters = new ArrayList<>();
        List<TabularData> results = executor.executePhysicalPlanAndGetResults(physicalPlan);
        Optional<String> stagingFilters = results.stream()
                .findFirst()
                .map(TabularData::getData)
                .flatMap(t -> t.stream().findFirst())
                .map(stringObjectMap -> (String) stringObjectMap.get(metadataDataset.stagingFiltersField()));

        // Convert map of Filters to List of Filters
        if (stagingFilters.isPresent())
        {
            Map<String, Map<String, Object>> datasetFiltersMap = new ObjectMapper().readValue(stagingFilters.get(), new TypeReference<Map<String, Map<String, Object>>>() {});
            for (Map.Entry<String, Map<String, Object>> filtersMapEntry : datasetFiltersMap.entrySet())
            {
                for (Map.Entry<String, Object> filterEntry : filtersMapEntry.getValue().entrySet())
                {
                    DatasetFilter datasetFilter = DatasetFilter.of(filtersMapEntry.getKey(), FilterType.fromName(filterEntry.getKey()), filterEntry.getValue());
                    datasetFilters.add(datasetFilter);
                }
            }
        }
        return datasetFilters;
    }

}
