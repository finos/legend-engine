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
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.PlaceholderValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.finos.legend.engine.persistence.components.relational.api.utils.IngestionUtils.getFirstColumnValue;
import static org.finos.legend.engine.persistence.components.relational.api.utils.IngestionUtils.getFirstRowForFirstResult;
import static org.finos.legend.engine.persistence.components.relational.api.utils.IngestionUtils.retrieveValueAsLong;

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
                    .partitioningStrategy(Partitioning.builder()
                        .addAllPartitionFields(partition.partitionFields())
                        .addAllPartitionSpecList(derivePartitionSpecList(partition.partitionFields(), partition.maxPartitionSpecFilters()))
                        .derivePartitionSpec(partition.derivePartitionSpec())
                        .maxPartitionSpecFilters(partition.maxPartitionSpecFilters())
                        .deleteStrategy(partition.deleteStrategy())
                        .build())
                    .build();
            }
        }
        return unitemporalSnapshot;
    }

    private List<Map<String, Object>> derivePartitionSpecList(List<String> partitionFields, Long maxAllowedPartitionSpecFilters)
    {
        List<Map<String, Object>> partitionSpecList = new ArrayList<>();

        // select count_distinct_approx (pk1, pk2, ...) from staging_table with staging_filters
        LogicalPlan logicalPlanForApproxDistinctCount = LogicalPlanFactory.getLogicalPlanForApproxDistinctCount(datasets.stagingDataset(), partitionFields);
        List<TabularData> approxCountResult = executor.executePhysicalPlanAndGetResults(transformer.generatePhysicalPlan(logicalPlanForApproxDistinctCount), placeHolderKeyValues);
        Optional<Object> obj = getFirstColumnValue(getFirstRowForFirstResult(approxCountResult));
        Optional<Long> maxDuplicatesValue = retrieveValueAsLong(obj.orElse(null));
        if (maxDuplicatesValue.isPresent() && maxDuplicatesValue.get() > maxAllowedPartitionSpecFilters)
        {
            return partitionSpecList;
        }

        // Select distinct pk1, pk2, ... from staging_table with staging_filters
        LogicalPlan logicalPlanForPartitionSpec = LogicalPlanFactory.getLogicalPlanForDistinctValues(datasets.stagingDataset(), partitionFields);
        List<TabularData> partitionSpecResult = executor.executePhysicalPlanAndGetResults(transformer.generatePhysicalPlan(logicalPlanForPartitionSpec), placeHolderKeyValues);

        if (!partitionSpecResult.isEmpty())
        {
            List<Map<String, Object>> partitionSpecRows = partitionSpecResult.get(0).data();
            for (Map<String, Object> partitionSpec: partitionSpecRows)
            {
                partitionSpecList.add(partitionSpec);
            }
        }

        return partitionSpecList;
    }
}
