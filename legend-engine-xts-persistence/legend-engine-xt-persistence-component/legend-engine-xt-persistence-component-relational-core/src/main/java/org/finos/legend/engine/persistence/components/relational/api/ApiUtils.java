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

package org.finos.legend.engine.persistence.components.relational.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.persistence.components.common.DatasetFilter;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.FilterType;
import org.finos.legend.engine.persistence.components.common.OptimizationFilter;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.ingestmode.*;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetsCaseConverter;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.planner.Planner;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.sql.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.BulkLoadMetadataDataset;
import org.finos.legend.engine.persistence.components.util.LockInfoDataset;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;

import java.util.*;

import static org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory.MAX_OF_FIELD;
import static org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory.MIN_OF_FIELD;

public class ApiUtils
{
    private static final String LOCK_INFO_DATASET_SUFFIX = "_legend_persistence_lock";

    public static Dataset deriveMainDatasetFromStaging(Datasets datasets, IngestMode ingestMode)
    {
        Dataset mainDataset = datasets.mainDataset();
        List<Field> mainDatasetFields = mainDataset.schema().fields();
        if (mainDatasetFields == null || mainDatasetFields.isEmpty())
        {
            mainDataset = ingestMode.accept(new DeriveMainDatasetSchemaFromStaging(datasets.mainDataset(), datasets.stagingDataset()));
        }
        return mainDataset;
    }

    public static Datasets enrichAndApplyCase(Datasets datasets, CaseConversion caseConversion)
    {
        DatasetsCaseConverter converter = new DatasetsCaseConverter();
        MetadataDataset metadataDataset = datasets.metadataDataset().orElse(MetadataDataset.builder().build());
        BulkLoadMetadataDataset bulkLoadMetadataDataset = datasets.bulkLoadMetadataDataset().orElse(BulkLoadMetadataDataset.builder().build());
        LockInfoDataset lockInfoDataset = getLockInfoDataset(datasets);
        Datasets enrichedDatasets = datasets
                .withMetadataDataset(metadataDataset)
                .withLockInfoDataset(lockInfoDataset)
                .withBulkLoadMetadataDataset(bulkLoadMetadataDataset);
        if (caseConversion == CaseConversion.TO_UPPER)
        {
            return converter.applyCase(enrichedDatasets, String::toUpperCase);
        }
        if (caseConversion == CaseConversion.TO_LOWER)
        {
            return converter.applyCase(enrichedDatasets, String::toLowerCase);
        }
        return enrichedDatasets;
    }

    public static IngestMode applyCase(IngestMode ingestMode, CaseConversion caseConversion)
    {
        if (caseConversion == CaseConversion.TO_UPPER)
        {
            return ingestMode.accept(new IngestModeCaseConverter(String::toUpperCase));
        }
        if (caseConversion == CaseConversion.TO_LOWER)
        {
            return ingestMode.accept(new IngestModeCaseConverter(String::toLowerCase));
        }
        return ingestMode;
    }

    private static LockInfoDataset getLockInfoDataset(Datasets datasets)
    {
        Dataset main = datasets.mainDataset();
        LockInfoDataset lockInfoDataset;
        if (datasets.lockInfoDataset().isPresent())
        {
            lockInfoDataset = datasets.lockInfoDataset().get();
        }
        else
        {
            String datasetName = main.datasetReference().name().orElseThrow(IllegalStateException::new);
            String lockDatasetName = datasetName + LOCK_INFO_DATASET_SUFFIX;
            lockInfoDataset = LockInfoDataset.builder()
                    .database(main.datasetReference().database())
                    .group(main.datasetReference().group())
                    .name(lockDatasetName)
                    .build();
        }
        return lockInfoDataset;
    }

    public static Optional<Long> getNextBatchId(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor,
                                          Transformer<SqlGen, SqlPlan> transformer, IngestMode ingestMode)
    {
        if (ingestMode.accept(IngestModeVisitors.IS_INGEST_MODE_TEMPORAL) || ingestMode instanceof BulkLoad)
        {
            LogicalPlan logicalPlanForNextBatchId = LogicalPlanFactory.getLogicalPlanForNextBatchId(datasets, ingestMode);
            List<TabularData> tabularData = executor.executePhysicalPlanAndGetResults(transformer.generatePhysicalPlan(logicalPlanForNextBatchId));
            Optional<Object> nextBatchId = getFirstColumnValue(getFirstRowForFirstResult(tabularData));
            if (nextBatchId.isPresent())
            {
                return retrieveValueAsLong(nextBatchId.get());
            }
        }
        return Optional.empty();
    }

    public static Optional<Map<OptimizationFilter, Pair<Object, Object>>> getOptimizationFilterBounds(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor,
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
}
