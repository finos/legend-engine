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

package org.finos.legend.engine.persistence.components.relational.api.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.persistence.components.common.DatasetFilter;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType;
import org.finos.legend.engine.persistence.components.common.FilterType;
import org.finos.legend.engine.persistence.components.common.OptimizationFilter;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.ingestmode.*;
import org.finos.legend.engine.persistence.components.ingestmode.BulkLoad;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.DatasetDeduplicationHandler;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.DeriveDataErrorRowsLogicalPlan;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.DeriveDuplicatePkRowsLogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.planner.Planner;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.api.*;
import org.finos.legend.engine.persistence.components.relational.exception.DataQualityException;
import org.finos.legend.engine.persistence.components.exception.JsonReadOrWriteException;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.finos.legend.engine.persistence.components.util.MetadataUtils;
import org.finos.legend.engine.persistence.components.util.PlaceholderValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType.DATA_ERROR_ROWS;
import static org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType.DUPLICATE_ROWS;
import static org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType.MAX_DATA_ERRORS;
import static org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType.MAX_DUPLICATES;
import static org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType.MAX_PK_DUPLICATES;
import static org.finos.legend.engine.persistence.components.common.DedupAndVersionErrorSqlType.PK_DUPLICATE_ROWS;
import static org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory.MAX_OF_FIELD;
import static org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory.MIN_OF_FIELD;
import static org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory.TABLE_IS_NON_EMPTY;
import static org.finos.legend.engine.persistence.components.relational.api.DataErrorAbstract.NUM_DATA_VERSION_ERRORS;
import static org.finos.legend.engine.persistence.components.relational.api.DataErrorAbstract.NUM_DUPLICATES;
import static org.finos.legend.engine.persistence.components.relational.api.DataErrorAbstract.NUM_PK_DUPLICATES;
import static org.finos.legend.engine.persistence.components.relational.api.RelationalGeneratorAbstract.BULK_LOAD_BATCH_STATUS_PATTERN;
import static org.finos.legend.engine.persistence.components.relational.api.utils.ApiUtils.convertCase;
import static org.finos.legend.engine.persistence.components.transformer.Transformer.TransformOptionsAbstract.DATE_TIME_FORMATTER;
import static org.finos.legend.engine.persistence.components.util.MetadataUtils.BATCH_SOURCE_INFO_STAGING_FILTERS;

public class IngestionUtils
{
    public static final String BATCH_ID_PATTERN = "{NEXT_BATCH_ID_PATTERN}";
    public static final String BATCH_START_TS_PATTERN = "{BATCH_START_TIMESTAMP_PLACEHOLDER}";
    public static final String BATCH_END_TS_PATTERN = "{BATCH_END_TIMESTAMP_PLACEHOLDER}";
    public static final String ADDITIONAL_METADATA_KEY_PATTERN = "{ADDITIONAL_METADATA_KEY_PLACEHOLDER}";
    public static final String ADDITIONAL_METADATA_VALUE_PATTERN = "{ADDITIONAL_METADATA_VALUE_PLACEHOLDER}";
    public static final String ADDITIONAL_METADATA_PLACEHOLDER_PATTERN = "{\"" + ADDITIONAL_METADATA_KEY_PATTERN + "\":\"" + ADDITIONAL_METADATA_VALUE_PATTERN + "\"}";
    private static final String SINGLE_QUOTE = "'";
    private static final Logger LOGGER = LoggerFactory.getLogger(IngestionUtils.class);

    public static List<IngestorResult> verifyIfRequestAlreadyProcessedPreviously(SchemaEvolutionResult schemaEvolutionResult, Datasets enrichedDatasets,
                                                                                 Optional<String> ingestRequestId, Transformer<SqlGen, SqlPlan> transformer,
                                                                                 Executor<SqlGen, TabularData, SqlPlan> executor, String batchSuccessStatusValue)
    {
        LOGGER.info("Perform Idempotency Check");
        List<IngestorResult> result = new ArrayList<>();

        LogicalPlan logicalPlan = LogicalPlanFactory.getLogicalPlanForBatchMetaRowsWithExistingIngestRequestId(
            enrichedDatasets.metadataDataset().orElseThrow(IllegalStateException::new),
            ingestRequestId.orElseThrow(IllegalStateException::new),
            enrichedDatasets.mainDataset().datasetReference().name().orElseThrow(IllegalStateException::new));
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<TabularData> tabularDataList = executor.executePhysicalPlanAndGetResults(physicalPlan);
        MetadataDataset metadataDataset = enrichedDatasets.metadataDataset().get();
        if (!tabularDataList.isEmpty())
        {
            List<Map<String, Object>> metadataResults = tabularDataList.get(0).getData();
            for (Map<String, Object> metadata: metadataResults)
            {
                Timestamp ingestionTimestampUTC = (Timestamp) metadata.get(metadataDataset.batchStartTimeField());
                Timestamp ingestionEndTimestampUTC = (Timestamp) metadata.get(metadataDataset.batchEndTimeField());
                String batchStatus = String.valueOf(metadata.get(metadataDataset.batchStatusField()));
                batchStatus = batchStatus.equalsIgnoreCase(batchSuccessStatusValue)  ? IngestStatus.SUCCEEDED.name() : batchStatus;
                IngestorResult ingestorResult = IngestorResult.builder()
                    .batchId(retrieveValueAsLong(metadata.get(metadataDataset.tableBatchIdField())).orElseThrow(IllegalStateException::new).intValue())
                    .putAllStatisticByName(readValueAsMap(String.valueOf(metadata.get(metadataDataset.batchStatisticsField()))))
                    .status(IngestStatus.valueOf(batchStatus))
                    .updatedDatasets(enrichedDatasets)
                    .schemaEvolutionSql(schemaEvolutionResult.schemaEvolutionSql())
                    .ingestionTimestampUTC(ingestionTimestampUTC.toLocalDateTime().format(DATE_TIME_FORMATTER))
                    .ingestionEndTimestampUTC(ingestionEndTimestampUTC.toLocalDateTime().format(DATE_TIME_FORMATTER))
                    .previouslyProcessed(true)
                    .build();
                result.add(ingestorResult);
            }
        }

        return result;
    }

    public static List<DataError> performDryRun(IngestMode enrichedIngestMode, Datasets enrichedDatasets, GeneratorResult generatorResult,
                                                Transformer<SqlGen, SqlPlan> transformer, Executor<SqlGen, TabularData, SqlPlan> executor,
                                                RelationalSink relationalSink, int sampleRowCount, CaseConversion caseConversion)
    {
        if (enrichedIngestMode instanceof BulkLoad)
        {
            executor.executePhysicalPlan(generatorResult.dryRunPreActionsSqlPlan().orElseThrow(IllegalStateException::new));
            List<DataError> results = relationalSink.performDryRun(enrichedDatasets, transformer, executor, generatorResult.dryRunSqlPlan().orElseThrow(IllegalStateException::new), generatorResult.dryRunValidationSqlPlan(), sampleRowCount, caseConversion);
            executor.executePhysicalPlan(generatorResult.dryRunPostCleanupSqlPlan().orElseThrow(IllegalStateException::new));
            return results;
        }
        else
        {
            throw new UnsupportedOperationException("Dry Run not supported for this ingest Mode : " + enrichedIngestMode.getClass().getSimpleName());
        }
    }

    public static List<IngestorResult> performIngestion(Datasets datasets, Transformer<SqlGen, SqlPlan> transformer, Planner planner, Executor<SqlGen,
        TabularData, SqlPlan> executor, GeneratorResult generatorResult, List<DataSplitRange> dataSplitRanges, IngestMode ingestMode,
        SchemaEvolutionResult schemaEvolutionResult, Map<String, Object> additionalMetadata, Clock executionTimestampClock, Optional<Long> batchId)
    {
        List<IngestorResult> results = new ArrayList<>();
        int dataSplitIndex = 0;
        int dataSplitsCount = (dataSplitRanges == null || dataSplitRanges.isEmpty()) ? 0 : dataSplitRanges.size();
        do
        {
            Optional<DataSplitRange> dataSplitRange = Optional.ofNullable(dataSplitsCount == 0 ? null : dataSplitRanges.get(dataSplitIndex));
            // Extract the Placeholders values
            Map<String, PlaceholderValue> placeHolderKeyValues = extractPlaceHolderKeyValues(datasets, executor, planner, transformer, ingestMode, dataSplitRange, additionalMetadata, executionTimestampClock, batchId);
            // Load main table, extract stats and update metadata table
            Map<StatisticName, Object> statisticsResultMap = loadData(executor, generatorResult, placeHolderKeyValues, executionTimestampClock);
            IngestorResult result = IngestorResult.builder()
                .putAllStatisticByName(statisticsResultMap)
                .updatedDatasets(datasets)
                .batchId(Optional.ofNullable(placeHolderKeyValues.containsKey(BATCH_ID_PATTERN) ? Integer.valueOf(placeHolderKeyValues.get(BATCH_ID_PATTERN).value()) : null))
                .dataSplitRange(dataSplitRange)
                .schemaEvolutionSql(schemaEvolutionResult.schemaEvolutionSql())
                .status(IngestStatus.SUCCEEDED)
                .ingestionTimestampUTC(placeHolderKeyValues.get(BATCH_START_TS_PATTERN).value())
                .ingestionEndTimestampUTC(placeHolderKeyValues.get(BATCH_END_TS_PATTERN).value())
                .build();
            results.add(result);
            dataSplitIndex++;
        }
        while (planner.dataSplitExecutionSupported() && dataSplitIndex < dataSplitsCount);
        // Clean up
        executor.executePhysicalPlan(generatorResult.postActionsSqlPlan());
        return results;
    }

    private static Map<StatisticName, Object> loadData(Executor<SqlGen, TabularData, SqlPlan> executor, GeneratorResult generatorResult, Map<String, PlaceholderValue> placeHolderKeyValues, Clock executionTimestampClock)
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
        // add batchEndTimestamp
        placeHolderKeyValues.put(BATCH_END_TS_PATTERN, PlaceholderValue.of(LocalDateTime.now(executionTimestampClock).format(DATE_TIME_FORMATTER), false));
        placeHolderKeyValues.put(MetadataUtils.BATCH_STATISTICS_PATTERN, PlaceholderValue.of(writeValueAsString(statisticsResultMap), false));
        executor.executePhysicalPlan(generatorResult.metadataIngestSqlPlan(), placeHolderKeyValues);
        return statisticsResultMap;
    }

    private static Map<StatisticName, Object> executeStatisticsPhysicalPlan(Executor<SqlGen, TabularData, SqlPlan> executor,
                                                                     Map<StatisticName, SqlPlan> statisticsSqlPlan,
                                                                     Map<String, PlaceholderValue> placeHolderKeyValues)
    {
        Map<StatisticName, Object> results = new HashMap<>();
        for (Map.Entry<StatisticName, SqlPlan> entry: statisticsSqlPlan.entrySet())
        {
            List<TabularData> result = executor.executePhysicalPlanAndGetResults(entry.getValue(), placeHolderKeyValues);
            Optional<Object> obj = getFirstColumnValue(getFirstRowForFirstResult(result));
            Object value = obj.orElse(null);
            results.put(entry.getKey(), value);
        }
        return results;
    }

    public static List<IngestorResult> performBulkLoad(Datasets datasets, Transformer<SqlGen, SqlPlan> transformer, Planner planner,
                                                Executor<SqlGen, TabularData, SqlPlan> executor, GeneratorResult generatorResult,
                                                IngestMode ingestMode, SchemaEvolutionResult schemaEvolutionResult,
                                                Map<String, Object> additionalMetadata, Clock executionTimestampClock,
                                                RelationalSink relationalSink, Optional<Long> batchId)
    {
        List<IngestorResult> results = new ArrayList<>();
        Map<String, PlaceholderValue> placeHolderKeyValues = extractPlaceHolderKeyValues(datasets, executor, planner, transformer, ingestMode, Optional.empty(), additionalMetadata, executionTimestampClock, batchId);

        // Execute ingest SqlPlan
        IngestorResult result = relationalSink.performBulkLoad(datasets, executor, generatorResult.ingestSqlPlan(), generatorResult.postIngestStatisticsSqlPlan(), placeHolderKeyValues, executionTimestampClock);
        if (schemaEvolutionResult != null && !schemaEvolutionResult.schemaEvolutionSql().isEmpty())
        {
            result = result.withSchemaEvolutionSql(schemaEvolutionResult.schemaEvolutionSql());
        }
        // Execute metadata ingest SqlPlan
        // add batchEndTimestamp
        placeHolderKeyValues.put(BATCH_END_TS_PATTERN, PlaceholderValue.of(result.ingestionEndTimestampUTC(), false));
        placeHolderKeyValues.put(BULK_LOAD_BATCH_STATUS_PATTERN, PlaceholderValue.of(result.status().name(), false));
        placeHolderKeyValues.put(MetadataUtils.BATCH_STATISTICS_PATTERN, PlaceholderValue.of(writeValueAsString(result.statisticByName()), false));
        executor.executePhysicalPlan(generatorResult.metadataIngestSqlPlan(), placeHolderKeyValues);
        results.add(result);
        // Clean up
        executor.executePhysicalPlan(generatorResult.postActionsSqlPlan());

        return results;
    }

    private static Map<String, PlaceholderValue> extractPlaceHolderKeyValues(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor,
                                                                      Planner planner, Transformer<SqlGen, SqlPlan> transformer, IngestMode ingestMode,
                                                                      Optional<DataSplitRange> dataSplitRange, Map<String, Object> additionalMetadata,
                                                                      Clock executionTimestampClock, Optional<Long> nextBatchId)
    {
        Map<String, PlaceholderValue> placeHolderKeyValues = new HashMap<>();

        // Handle batch ID
        if (!nextBatchId.isPresent())
        {
            nextBatchId = IngestionUtils.getNextBatchId(datasets, executor, transformer);
        }
        if (nextBatchId.isPresent())
        {
            LOGGER.info(String.format("Obtained the next Batch id: %s", nextBatchId.get()));
            placeHolderKeyValues.put(BATCH_ID_PATTERN, PlaceholderValue.of(nextBatchId.get().toString(), false));
        }

        // Handle optimization filters
        Optional<Map<OptimizationFilter, Pair<Object, Object>>> optimizationFilters = IngestionUtils.getOptimizationFilterBounds(datasets, executor, transformer, ingestMode, placeHolderKeyValues);
        if (optimizationFilters.isPresent())
        {
            for (OptimizationFilter filter : optimizationFilters.get().keySet())
            {
                Object lowerBound = optimizationFilters.get().get(filter).getOne();
                Object upperBound = optimizationFilters.get().get(filter).getTwo();
                if (lowerBound instanceof Number)
                {
                    placeHolderKeyValues.put(SINGLE_QUOTE + filter.lowerBoundPattern() + SINGLE_QUOTE, PlaceholderValue.of(lowerBound.toString(), true));
                    placeHolderKeyValues.put(SINGLE_QUOTE + filter.upperBoundPattern() + SINGLE_QUOTE, PlaceholderValue.of(upperBound.toString(), true));
                }
                else
                {
                    placeHolderKeyValues.put(filter.lowerBoundPattern(), PlaceholderValue.of(lowerBound.toString(), true));
                    placeHolderKeyValues.put(filter.upperBoundPattern(), PlaceholderValue.of(upperBound.toString(), true));
                }
            }
        }

        // Handle data splits
        if (planner.dataSplitExecutionSupported() && dataSplitRange.isPresent())
        {
            placeHolderKeyValues.put(SINGLE_QUOTE + LogicalPlanUtils.DATA_SPLIT_LOWER_BOUND_PLACEHOLDER + SINGLE_QUOTE, PlaceholderValue.of(String.valueOf(dataSplitRange.get().lowerBound()), false));
            placeHolderKeyValues.put(SINGLE_QUOTE + LogicalPlanUtils.DATA_SPLIT_UPPER_BOUND_PLACEHOLDER + SINGLE_QUOTE, PlaceholderValue.of(String.valueOf(dataSplitRange.get().upperBound()), false));
        }

        // Handle additional metadata
        try
        {
            ObjectMapper objectMapper = new ObjectMapper();
            String additionalMetadataString = objectMapper.writeValueAsString(additionalMetadata);
            placeHolderKeyValues.put(ADDITIONAL_METADATA_PLACEHOLDER_PATTERN, PlaceholderValue.of(additionalMetadataString, true));
        }
        catch (JsonProcessingException e)
        {
            throw new JsonReadOrWriteException("Unable to parse additional metadata", e);
        }

        // Handle batch timestamp
        placeHolderKeyValues.put(BATCH_START_TS_PATTERN, PlaceholderValue.of(LocalDateTime.now(executionTimestampClock).format(DATE_TIME_FORMATTER), false));

        return placeHolderKeyValues;
    }

    public static Optional<Long> getNextBatchId(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor,
                                          Transformer<SqlGen, SqlPlan> transformer)
    {
        LogicalPlan logicalPlanForNextBatchId = LogicalPlanFactory.getLogicalPlanForNextBatchId(datasets);
        List<TabularData> tabularData = executor.executePhysicalPlanAndGetResults(transformer.generatePhysicalPlan(logicalPlanForNextBatchId));
        Optional<Object> nextBatchId = getFirstColumnValue(getFirstRowForFirstResult(tabularData));
        if (nextBatchId.isPresent())
        {
            return retrieveValueAsLong(nextBatchId.get());
        }
        return Optional.empty();
    }

    public static long getNextBatchIdFromLockTable(LogicalPlan plan, Executor<SqlGen, TabularData, SqlPlan> executor,
                                                   Transformer<SqlGen, SqlPlan> transformer)
    {
        List<TabularData> tabularData = executor.executePhysicalPlanAndGetResults(transformer.generatePhysicalPlan(plan));
        Optional<Object> nextBatchId = getFirstColumnValue(getFirstRowForFirstResult(tabularData));
        if (nextBatchId.isPresent())
        {
            return retrieveValueAsLong(nextBatchId.get()).orElseThrow(IllegalStateException::new);
        }
        else
        {
            throw new IllegalStateException("Lock Table is not properly initialized");
        }
    }

    public static Optional<Map<OptimizationFilter, Pair<Object, Object>>> getOptimizationFilterBounds(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor,
                                                                                                      Transformer<SqlGen, SqlPlan> transformer, IngestMode ingestMode,
                                                                                                      Map<String, PlaceholderValue> placeHolderKeyValues)
    {
        List<OptimizationFilter> filters = ingestMode.accept(IngestModeVisitors.RETRIEVE_OPTIMIZATION_FILTERS);
        if (!filters.isEmpty())
        {
            Map<OptimizationFilter, Pair<Object, Object>> map = new HashMap<>();
            for (OptimizationFilter filter : filters)
            {
                LogicalPlan logicalPlanForMinAndMaxForField = LogicalPlanFactory.getLogicalPlanForMinAndMaxForField(datasets.stagingDataset(), filter.fieldName());
                List<TabularData> tabularData = executor.executePhysicalPlanAndGetResults(transformer.generatePhysicalPlan(logicalPlanForMinAndMaxForField), placeHolderKeyValues);
                Map<String, Object> resultMap = getFirstRowForFirstResult(tabularData);
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

    public static List<DatasetFilter> extractDatasetFilters(MetadataDataset metadataDataset, Executor<SqlGen, TabularData, SqlPlan> executor, SqlPlan physicalPlan) throws JsonProcessingException
    {
        List<DatasetFilter> datasetFilters = new ArrayList<>();
        List<TabularData> results = executor.executePhysicalPlanAndGetResults(physicalPlan);
        Optional<String> stagingFilters = results.stream()
                .findFirst()
                .map(TabularData::getData)
                .flatMap(t -> t.stream().findFirst())
                .map(stringObjectMap -> String.valueOf(stringObjectMap.get(metadataDataset.batchSourceInfoField())));

        // Convert map of Filters to List of Filters
        if (stagingFilters.isPresent())
        {
            Map<String, Map<String, Map<String, Object>>> datasetFiltersMap = new ObjectMapper().readValue(stagingFilters.get(), new TypeReference<Map<String, Map<String, Map<String, Object>>>>() {});
            for (Map.Entry<String, Map<String, Object>> filtersMapEntry : datasetFiltersMap.get(BATCH_SOURCE_INFO_STAGING_FILTERS).entrySet())
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

    public static List<DataSplitRange> getDataSplitRanges(Executor<SqlGen, TabularData, SqlPlan> executor, Planner planner,
                                                          Transformer<SqlGen, SqlPlan> transformer, IngestMode ingestMode)
    {
        List<DataSplitRange> dataSplitRanges = new ArrayList<>();
        if (ingestMode.versioningStrategy() instanceof AllVersionsStrategy)
        {
            Dataset stagingDataset = planner.stagingDataset();
            String dataSplitField = ingestMode.dataSplitField().get();
            LogicalPlan logicalPlanForMaxOfField = LogicalPlanFactory.getLogicalPlanForMaxOfField(stagingDataset, dataSplitField);
            List<TabularData> tabularData = executor.executePhysicalPlanAndGetResults(transformer.generatePhysicalPlan(logicalPlanForMaxOfField));
            Map<String, Object> row = getFirstRowForFirstResult(tabularData);
            Long maxDataSplit = retrieveValueAsLong(row.get(MAX_OF_FIELD)).orElseThrow(IllegalStateException::new);
            for (int i = 1; i <= maxDataSplit; i++)
            {
                dataSplitRanges.add(DataSplitRange.of(i, i));
            }
        }
        return dataSplitRanges;
    }

    public static boolean datasetEmpty(Dataset dataset, Transformer<SqlGen, SqlPlan> transformer, Executor<SqlGen, TabularData, SqlPlan> executor, Map<String, PlaceholderValue> placeHolderKeyValues)
    {
        LogicalPlan checkIsDatasetEmptyLogicalPlan = LogicalPlanFactory.getLogicalPlanForIsDatasetEmpty(dataset);
        SqlPlan physicalPlanForCheckIsDataSetEmpty = transformer.generatePhysicalPlan(checkIsDatasetEmptyLogicalPlan);
        List<TabularData> results = executor.executePhysicalPlanAndGetResults(physicalPlanForCheckIsDataSetEmpty, placeHolderKeyValues);
        Optional<Object> obj = getFirstColumnValue(getFirstRowForFirstResult(results));
        String value = String.valueOf(obj.orElseThrow(IllegalStateException::new));
        return !value.equals(TABLE_IS_NON_EMPTY);
    }

    public static void dedupAndVersion(Executor<SqlGen, TabularData, SqlPlan> executor, GeneratorResult generatorResult, Datasets enrichedDatasets, CaseConversion caseConversion, Map<String, PlaceholderValue> placeHolderKeyValues)
    {
        executor.executePhysicalPlan(generatorResult.deduplicationAndVersioningSqlPlan().get(), placeHolderKeyValues);

        Map<DedupAndVersionErrorSqlType, SqlPlan> dedupAndVersionErrorSqlTypeSqlPlanMap = generatorResult.deduplicationAndVersioningErrorChecksSqlPlan();

        // Error Check for Duplicates: if Dedup = fail on dups, Fail the job if count > 1
        if (dedupAndVersionErrorSqlTypeSqlPlanMap.containsKey(MAX_DUPLICATES))
        {
            List<TabularData> result = executor.executePhysicalPlanAndGetResults(dedupAndVersionErrorSqlTypeSqlPlanMap.get(MAX_DUPLICATES), placeHolderKeyValues);
            Optional<Object> obj = getFirstColumnValue(getFirstRowForFirstResult(result));
            Optional<Long> maxDuplicatesValue = retrieveValueAsLong(obj.orElse(null));
            if (maxDuplicatesValue.isPresent() && maxDuplicatesValue.get() > 1)
            {
                // Find the duplicate rows
                TabularData duplicateRows = executor.executePhysicalPlanAndGetResults(dedupAndVersionErrorSqlTypeSqlPlanMap.get(DUPLICATE_ROWS), placeHolderKeyValues).get(0);
                String errorMessage = "Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy";
                LOGGER.error(errorMessage);
                List<DataError> dataErrors = constructDataQualityErrors(enrichedDatasets.stagingDataset(), duplicateRows.getData(),
                    ErrorCategory.DUPLICATES, caseConversion, DatasetDeduplicationHandler.COUNT, NUM_DUPLICATES);
                throw new DataQualityException(errorMessage, dataErrors);
            }
        }

        // Error Check for PK Duplicates: if versioning = No Versioning (fail on pk dups), Fail the job if count > 1
        if (dedupAndVersionErrorSqlTypeSqlPlanMap.containsKey(MAX_PK_DUPLICATES))
        {
            List<TabularData> result = executor.executePhysicalPlanAndGetResults(dedupAndVersionErrorSqlTypeSqlPlanMap.get(MAX_PK_DUPLICATES), placeHolderKeyValues);
            Optional<Object> obj = getFirstColumnValue(getFirstRowForFirstResult(result));
            Optional<Long> maxPkDuplicatesValue = retrieveValueAsLong(obj.orElse(null));
            if (maxPkDuplicatesValue.isPresent() && maxPkDuplicatesValue.get() > 1)
            {
                // Find the pk-duplicate rows
                TabularData duplicatePkRows = executor.executePhysicalPlanAndGetResults(dedupAndVersionErrorSqlTypeSqlPlanMap.get(PK_DUPLICATE_ROWS), placeHolderKeyValues).get(0);
                String errorMessage = "Encountered multiple rows with duplicate primary keys, Failing the batch as Fail on Duplicate Primary Keys is selected";
                LOGGER.error(errorMessage);
                List<DataError> dataErrors = IngestionUtils.constructDataQualityErrors(enrichedDatasets.stagingDataset(), duplicatePkRows.getData(),
                    ErrorCategory.DUPLICATE_PRIMARY_KEYS, caseConversion, DeriveDuplicatePkRowsLogicalPlan.DUPLICATE_PK_COUNT, NUM_PK_DUPLICATES);
                throw new DataQualityException(errorMessage, dataErrors);
            }
        }

        // Error Check for Data Error: If versioning = Max Version/ All Versioning, Check for data error
        if (dedupAndVersionErrorSqlTypeSqlPlanMap.containsKey(MAX_DATA_ERRORS))
        {
            List<TabularData> result = executor.executePhysicalPlanAndGetResults(dedupAndVersionErrorSqlTypeSqlPlanMap.get(MAX_DATA_ERRORS), placeHolderKeyValues);
            Optional<Object> obj = getFirstColumnValue(getFirstRowForFirstResult(result));
            Optional<Long> maxDataErrorsValue = retrieveValueAsLong(obj.orElse(null));
            if (maxDataErrorsValue.isPresent() && maxDataErrorsValue.get() > 1)
            {
                // Find the data errors
                TabularData errors = executor.executePhysicalPlanAndGetResults(dedupAndVersionErrorSqlTypeSqlPlanMap.get(DATA_ERROR_ROWS), placeHolderKeyValues).get(0);
                String errorMessage = "Encountered Data errors (same PK, same version but different data), hence failing the batch";
                LOGGER.error(errorMessage);
                List<DataError> dataErrors = IngestionUtils.constructDataQualityErrors(enrichedDatasets.stagingDataset(), errors.getData(),
                    ErrorCategory.DATA_VERSION_ERROR, caseConversion, DeriveDataErrorRowsLogicalPlan.DATA_VERSION_ERROR_COUNT, NUM_DATA_VERSION_ERRORS);
                throw new DataQualityException(errorMessage, dataErrors);
            }
        }
    }

    private static String writeValueAsString(Map<StatisticName, Object> statisticByName)
    {
        try
        {
            return new ObjectMapper().writeValueAsString(statisticByName);
        }
        catch (JsonProcessingException e)
        {
            throw new JsonReadOrWriteException(e.getMessage(), e);
        }
    }

    public static Optional<Long> retrieveValueAsLong(Object obj)
    {
        if (obj instanceof Integer)
        {
            return Optional.of(Long.valueOf((Integer) obj));
        }
        else if (obj instanceof Long)
        {
            return Optional.of((Long) obj);
        }
        return Optional.empty();
    }

    private static Map<StatisticName, Object> readValueAsMap(String batchStatistics)
    {
        try
        {
            return new ObjectMapper().readValue(batchStatistics, new TypeReference<HashMap<StatisticName, Object>>() {});
        }
        catch (JsonProcessingException e)
        {
            throw new JsonReadOrWriteException(e.getMessage(), e);
        }
    }

    public static Map<String, Object> getFirstRowForFirstResult(List<TabularData> tabularData)
    {
        Map<String, Object> resultMap = tabularData.stream()
                .findFirst()
                .map(TabularData::getData)
                .flatMap(t -> t.stream().findFirst())
                .orElse(Collections.emptyMap());
        return resultMap;
    }

    public static Optional<Object> getFirstColumnValue(Map<String, Object> row)
    {
        Optional<Object> object = Optional.empty();
        if (!row.isEmpty())
        {
            String key = row.keySet().stream().findFirst().orElseThrow(IllegalStateException::new);
            object = Optional.ofNullable(row.get(key));
        }
        return object;
    }

    public static List<DataError> constructDataQualityErrors(Dataset stagingDataset, List<Map<String, Object>> dataErrors,
                                                             ErrorCategory errorCategory, CaseConversion caseConversion, String errorField, String errorDetailsKey)
    {
        List<DataError> dataErrorList = new ArrayList<>();
        List<String> allFields = stagingDataset.schemaReference().fieldValues().stream().map(FieldValue::fieldName).collect(Collectors.toList());
        String caseCorrectedErrorField = convertCase(caseConversion, errorField);

        for (Map<String, Object> dataError: dataErrors)
        {
            dataErrorList.add(DataError.builder()
                    .errorMessage(errorCategory.getDefaultErrorMessage())
                    .errorCategory(errorCategory)
                    .errorRecord(buildErrorRecord(allFields, dataError))
                    .putAllErrorDetails(buildErrorDetails(dataError, caseCorrectedErrorField, errorDetailsKey))
                    .build());
        }
        return dataErrorList;
    }

    private static Map<String, Object> buildErrorDetails(Map<String, Object> dataError, String errorField, String errorDetailsKey)
    {
        Map<String, Object> errorDetails = new HashMap<>();
        Object errorDetailsValue = dataError.get(errorField);
        errorDetails.put(errorDetailsKey, errorDetailsValue);
        return errorDetails;
    }

    public static String buildErrorRecord(List<String> allColumns, Map<String, Object> row)
    {
        Map<String, Object> errorRecordMap = new HashMap<>();

        for (String column : allColumns)
        {
            if (row.containsKey(column))
            {
                errorRecordMap.put(column, row.get(column));
            }
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try
        {
            return objectMapper.writeValueAsString(errorRecordMap);
        }
        catch (JsonProcessingException e)
        {
            throw new JsonReadOrWriteException(e.getMessage(), e);
        }
    }

    public static String removeLineBreaks(String str)
    {
        return str.replaceAll("\n", " ").replaceAll("\r", " ");
    }

    public static Optional<String> findToken(String message, String regex, int group)
    {
        Optional<String> token = Optional.empty();
        Matcher matcher = Pattern.compile(regex).matcher(message);
        if (matcher.find())
        {
            token = Optional.of(matcher.group(group));
        }
        return token;
    }

}
