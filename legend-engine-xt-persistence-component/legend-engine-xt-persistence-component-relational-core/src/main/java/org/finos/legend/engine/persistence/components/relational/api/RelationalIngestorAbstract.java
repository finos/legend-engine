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
import org.finos.legend.engine.persistence.components.common.OptimizationFilter;
import org.finos.legend.engine.persistence.components.common.Resources;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.executor.DigestInfo;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.importer.Importer;
import org.finos.legend.engine.persistence.components.importer.Importers;
import org.finos.legend.engine.persistence.components.ingestmode.DeriveMainDatasetSchemaFromStaging;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.IngestModeOptimizationColumnHandler;
import org.finos.legend.engine.persistence.components.ingestmode.IngestModeVisitors;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.ExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.planner.Planner;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.planner.Planners;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.executor.RelationalExecutor;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;
import org.finos.legend.engine.persistence.components.util.SchemaEvolutionCapability;
import org.immutables.value.Value.Default;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

import java.sql.Connection;
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
    private static final String BATCH_ID_PATTERN = "{NEXT_BATCH_ID_PATTERN}";

    private static final String BATCH_START_TS_PATTERN = "{BATCH_START_TIMESTAMP_PLACEHOLDER}";

    //---------- FLAGS ----------

    @Default
    public boolean cleanupStagingData()
    {
        return true;
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
    public Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet()
    {
        return Collections.emptySet();
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

    // ---------- API ----------

    public IngestorResult ingest(Connection connection, Datasets datasets)
    {
        return ingest(connection, datasets, null).stream().findFirst().orElseThrow(IllegalStateException::new);
    }

    public List<IngestorResult> ingestWithDataSplits(Connection connection, Datasets datasets, List<DataSplitRange> dataSplitRanges)
    {
        // Provide the default dataSplit ranges if missing
        if (dataSplitRanges == null || dataSplitRanges.isEmpty())
        {
            dataSplitRanges = Arrays.asList(DataSplitRange.of(1,1));
        }
        return ingest(connection, datasets, dataSplitRanges);
    }

    // ---------- UTILITY METHODS ----------

    private List<IngestorResult> ingest(Connection connection, Datasets datasets, List<DataSplitRange> dataSplitRanges)
    {
        IngestMode enrichedIngestMode = ApiUtils.applyCase(ingestMode(), caseConversion());
        Datasets enrichedDatasets = ApiUtils.applyCase(datasets, caseConversion());

        Transformer<SqlGen, SqlPlan> transformer = new RelationalTransformer(relationalSink(), transformOptions());
        Executor<SqlGen, TabularData, SqlPlan> executor = new RelationalExecutor(relationalSink(), JdbcHelper.of(connection));

        Resources.Builder resourcesBuilder = Resources.builder();
        Datasets updatedDatasets = enrichedDatasets;

        // import external dataset reference
        if (updatedDatasets.stagingDataset() instanceof ExternalDatasetReference)
        {
            // update staging dataset reference to imported dataset
            updatedDatasets = importExternalDataset(enrichedIngestMode, updatedDatasets, transformer, executor);
            resourcesBuilder.externalDatasetImported(true);
        }

        // Check if staging dataset is empty
        if (ingestMode().accept(IngestModeVisitors.NEED_TO_CHECK_STAGING_EMPTY) && executor.datasetExists(updatedDatasets.stagingDataset()))
        {
            resourcesBuilder.stagingDataSetEmpty(datasetEmpty(updatedDatasets.stagingDataset(), transformer, executor));
        }

        boolean mainDatasetExists = executor.datasetExists(updatedDatasets.mainDataset());
        if (mainDatasetExists)
        {
            updatedDatasets = updatedDatasets.withMainDataset(constructDatasetFromDatabase(executor, updatedDatasets.mainDataset()));
        }
        else
        {
            updatedDatasets = updatedDatasets.withMainDataset(ApiUtils.deriveMainDatasetFromStaging(updatedDatasets, enrichedIngestMode));
        }

        // Add Optimization Columns if needed
        enrichedIngestMode = enrichedIngestMode.accept(new IngestModeOptimizationColumnHandler(updatedDatasets));

        // generate sql plans
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(enrichedIngestMode)
            .relationalSink(relationalSink())
            .cleanupStagingData(cleanupStagingData())
            .collectStatistics(collectStatistics())
            .enableSchemaEvolution(enableSchemaEvolution())
            .addAllSchemaEvolutionCapabilitySet(schemaEvolutionCapabilitySet())
            .caseConversion(caseConversion())
            .executionTimestampClock(executionTimestampClock())
            .batchStartTimestampPattern(BATCH_START_TS_PATTERN)
            .batchIdPattern(BATCH_ID_PATTERN)
            .build();

        Planner planner = Planners.get(updatedDatasets, enrichedIngestMode, plannerOptions());
        GeneratorResult generatorResult = generator.generateOperations(updatedDatasets, resourcesBuilder.build(), planner, enrichedIngestMode);

        // Create tables
        executor.executePhysicalPlan(generatorResult.preActionsSqlPlan());
        // The below boolean is created before the execution of pre-actions, hence it represents whether the main table has already existed before that
        if (mainDatasetExists)
        {
            // Perform schema evolution
            if (generatorResult.schemaEvolutionDataset().isPresent())
            {
                updatedDatasets = updatedDatasets.withMainDataset(generatorResult.schemaEvolutionDataset().get());
                generatorResult.schemaEvolutionSqlPlan().ifPresent(executor::executePhysicalPlan);
            }
        }
        // Perform Ingestion
        List<IngestorResult> result = performIngestion(updatedDatasets, transformer, planner, executor, generatorResult, dataSplitRanges, enrichedIngestMode);
        return result;
    }

    private List<IngestorResult> performIngestion(Datasets datasets, Transformer<SqlGen, SqlPlan> transformer, Planner planner, Executor<SqlGen,
        TabularData, SqlPlan> executor, GeneratorResult generatorResult, List<DataSplitRange> dataSplitRanges, IngestMode ingestMode)
    {
        try
        {
            List<IngestorResult> results = new ArrayList<>();
            executor.begin();
            int dataSplitIndex = 0;
            int dataSplitsCount = (dataSplitRanges == null || dataSplitRanges.isEmpty()) ? 0 : dataSplitRanges.size();
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
                    .build();
                results.add(result);
                dataSplitIndex++;
            }
            while (planner.dataSplitExecutionSupported() && dataSplitIndex < dataSplitsCount);
            // Clean up
            executor.executePhysicalPlan(generatorResult.postActionsSqlPlan());
            executor.commit();
            return results;
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
            executor.executePhysicalPlan(generatorResult.metadataIngestSqlPlan().get(), placeHolderKeyValues);
        }
        return statisticsResultMap;
    }

    private Datasets importExternalDataset(IngestMode ingestMode, Datasets datasets, Transformer<SqlGen, SqlPlan> transformer, Executor<SqlGen, TabularData, SqlPlan> executor)
    {
        ExternalDatasetReference externalDatasetReference = (ExternalDatasetReference) datasets.stagingDataset();
        DatasetReference mainDataSetReference = datasets.mainDataset().datasetReference();

        externalDatasetReference = externalDatasetReference
            .withName(externalDatasetReference.name().isPresent() ? externalDatasetReference.name().get() : LogicalPlanUtils.generateTableNameWithSuffix(mainDataSetReference.name().orElseThrow(IllegalStateException::new), STAGING))
            .withDatabase(externalDatasetReference.database().isPresent() ? externalDatasetReference.database().get() : mainDataSetReference.database().orElse(null))
            .withGroup(externalDatasetReference.group().isPresent() ? externalDatasetReference.group().get() : mainDataSetReference.group().orElse(null))
            .withAlias(externalDatasetReference.alias().isPresent() ? externalDatasetReference.alias().get() : mainDataSetReference.alias().orElseThrow(RuntimeException::new) + UNDERSCORE + STAGING);

        // TODO : Auto infer schema in future

        // Prepare DigestInfo
        boolean hasDigestField = ingestMode.accept(IngestModeVisitors.DIGEST_REQUIRED);
        Optional<String> digestFieldOptional = ingestMode.accept(IngestModeVisitors.EXTRACT_DIGEST_FIELD);
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
        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(extractedStagingDatasetDefinition, false);
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        executor.executePhysicalPlan(tableCreationPhysicalPlan);

        // Load staging data
        Set<String> metaFields = ingestMode.accept(IngestModeVisitors.META_FIELDS_TO_EXCLUDE_FROM_DIGEST);
        DigestInfo digestInfo = DigestInfo.builder().populateDigest(populateDigest).digestField(digestFieldOptional.orElse(null)).addAllMetaFields(metaFields).build();
        Importer importer = Importers.forExternalDatasetReference(externalDatasetReference, transformer, executor);
        importer.importData(externalDatasetReference, digestInfo);

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

    private Dataset constructDatasetFromDatabase(Executor<SqlGen, TabularData, SqlPlan> executor, Dataset dataset)
    {
        String tableName = dataset.datasetReference().name().orElseThrow(IllegalStateException::new);
        String schemaName = dataset.datasetReference().group().orElse(null);
        String databaseName = dataset.datasetReference().database().orElse(null);
        return executor.constructDatasetFromDatabase(tableName, schemaName, databaseName);
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
        if (ingestMode.accept(IngestModeVisitors.IS_INGEST_MODE_TEMPORAL))
        {
            LogicalPlan logicalPlanForNextBatchId = LogicalPlanFactory.getLogicalPlanForNextBatchId(datasets);
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
}
