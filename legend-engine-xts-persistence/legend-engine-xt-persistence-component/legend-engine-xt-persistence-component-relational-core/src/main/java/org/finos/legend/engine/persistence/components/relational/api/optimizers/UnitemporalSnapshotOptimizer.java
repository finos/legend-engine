// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.api.optimizers;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.executor.TabularData;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.partitioning.Partitioning;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.PlaceholderValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.finos.legend.engine.persistence.components.relational.api.utils.IngestionUtils.*;
import static org.finos.legend.engine.persistence.components.relational.api.utils.PartitionExtractorUtils.extractPartitions;

public class UnitemporalSnapshotOptimizer
{
    private UnitemporalSnapshot unitemporalSnapshot;
    private Datasets datasets;
    private Executor<SqlGen, TabularData, SqlPlan> executor;
    Transformer<SqlGen, SqlPlan> transformer;
    Map<String, PlaceholderValue> placeHolderKeyValues;

    public UnitemporalSnapshotOptimizer(UnitemporalSnapshot unitemporalSnapshot, Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor, Transformer<SqlGen, SqlPlan> transformer, Map<String, PlaceholderValue> placeHolderKeyValues)
    {
        this.unitemporalSnapshot = unitemporalSnapshot;
        this.datasets = datasets;
        this.executor = executor;
        this.transformer = transformer;
        this.placeHolderKeyValues = placeHolderKeyValues;
    }

    public UnitemporalSnapshot optimize()
    {
        if (unitemporalSnapshot.partitioningStrategy().isPartitioned())
        {
            Partitioning partition = (Partitioning) unitemporalSnapshot.partitioningStrategy();
            if (!partition.partitionFields().isEmpty() && partition.derivePartitionSpec())
            {
                return UnitemporalSnapshot
                    .builder()
                    .versioningStrategy(unitemporalSnapshot.versioningStrategy())
                    .deduplicationStrategy(unitemporalSnapshot.deduplicationStrategy())
                    .digestField(unitemporalSnapshot.digestField())
                    .transactionMilestoning(unitemporalSnapshot.transactionMilestoning())
                    .emptyDatasetHandling(unitemporalSnapshot.emptyDatasetHandling())
                    .deleteStrategy(unitemporalSnapshot.deleteStrategy())
                    .partitioningStrategy(Partitioning.builder()
                        .addAllPartitionFields(partition.partitionFields())
                        .addAllPartitionSpecList(derivePartitionSpecList(partition.partitionFields(), partition.maxPartitionSpecFilters()))
                        .derivePartitionSpec(partition.derivePartitionSpec())
                        .maxPartitionSpecFilters(partition.maxPartitionSpecFilters())
                        .build())
                    .build();
            }
        }
        return unitemporalSnapshot;
    }

    private List<Map<String, Object>> derivePartitionSpecList(List<String> partitionFields, Long maxAllowedPartitionSpecFilters)
    {
        Optional<Long> stagingDatasetPartitionCount = fetchApproximatePartitionCount(datasets.stagingDataset(), partitionFields);
        boolean shouldUsePartitionSpec = shouldUsePartitionSpec(stagingDatasetPartitionCount, maxAllowedPartitionSpecFilters);

        if (!shouldUsePartitionSpec)
        {
            return new ArrayList<>();
        }

        return extractPartitions(executor, transformer, datasets.stagingDataset(), partitionFields, placeHolderKeyValues);
    }

    private boolean shouldUsePartitionSpec(Optional<Long> approxCountValue, Long maxAllowedPartitionSpecFilters)
    {
        return approxCountValue.isPresent() && approxCountValue.get() <= maxAllowedPartitionSpecFilters;
    }

    private Optional<Long> fetchApproximatePartitionCount(Dataset dataset, List<String> partitionFields)
    {
        // select count_distinct_approx (pk1, pk2, ...) from staging_table with staging_filters
        LogicalPlan logicalPlanForApproxDistinctCount = LogicalPlanFactory.getLogicalPlanForApproxDistinctCount(dataset, partitionFields);
        List<TabularData> approxCountResult = executor.executePhysicalPlanAndGetResults(transformer.generatePhysicalPlan(logicalPlanForApproxDistinctCount), placeHolderKeyValues);
        Optional<Object> obj = getFirstColumnValue(getFirstRowForFirstResult(approxCountResult));
        return retrieveValueAsLong(obj.orElse(null));
    }
}
