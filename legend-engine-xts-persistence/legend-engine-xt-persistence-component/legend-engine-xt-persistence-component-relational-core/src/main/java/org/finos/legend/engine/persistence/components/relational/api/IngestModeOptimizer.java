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
import org.finos.legend.engine.persistence.components.common.OptimizationFilter;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.ingestmode.*;
import org.finos.legend.engine.persistence.components.ingestmode.partitioning.Partitioning;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.executor.TabularData;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.transformer.Transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.relational.api.utils.IngestionUtils.*;
import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.SUPPORTED_DATA_TYPES_FOR_OPTIMIZATION_COLUMNS;
import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.findCommonPrimaryFieldsBetweenMainAndStaging;

public class IngestModeOptimizer implements IngestModeVisitor<IngestMode>
{
    private Datasets datasets;
    private Executor<SqlGen, TabularData, SqlPlan> executor;
    Transformer<SqlGen, SqlPlan> transformer;

    public IngestModeOptimizer(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor, Transformer<SqlGen, SqlPlan> transformer)
    {
        this.datasets = datasets;
        this.executor = executor;
        this.transformer = transformer;
    }

    @Override
    public IngestMode visitAppendOnly(AppendOnlyAbstract appendOnly)
    {
        return appendOnly;
    }

    @Override
    public IngestMode visitNontemporalSnapshot(NontemporalSnapshotAbstract nontemporalSnapshot)
    {
        return nontemporalSnapshot;
    }

    @Override
    public IngestMode visitNontemporalDelta(NontemporalDeltaAbstract nontemporalDelta)
    {
        return nontemporalDelta;
    }

    @Override
    public IngestMode visitUnitemporalSnapshot(UnitemporalSnapshotAbstract unitemporalSnapshot)
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
                            .maxPartitionSpecFilters(partition.maxPartitionSpecFilters()).build())
                        .build();
            }
        }
        return unitemporalSnapshot;
    }

    @Override
    public IngestMode visitUnitemporalDelta(UnitemporalDeltaAbstract unitemporalDelta)
    {
        return UnitemporalDelta
                .builder()
                .digestField(unitemporalDelta.digestField())
                .addAllOptimizationFilters(deriveOptimizationFilters(unitemporalDelta))
                .transactionMilestoning(unitemporalDelta.transactionMilestoning())
                .mergeStrategy(unitemporalDelta.mergeStrategy())
                .versioningStrategy(unitemporalDelta.versioningStrategy())
                .deduplicationStrategy(unitemporalDelta.deduplicationStrategy())
                .build();
    }

    @Override
    public IngestMode visitBitemporalSnapshot(BitemporalSnapshotAbstract bitemporalSnapshot)
    {
        return bitemporalSnapshot;
    }

    @Override
    public IngestMode visitBitemporalDelta(BitemporalDeltaAbstract bitemporalDelta)
    {
        return bitemporalDelta;
    }

    @Override
    public IngestMode visitBulkLoad(BulkLoadAbstract bulkLoad)
    {
        return bulkLoad;
    }

    @Override
    public IngestMode visitNoOp(NoOpAbstract noOpAbstract)
    {
        return noOpAbstract;
    }

    private List<Map<String, Object>> derivePartitionSpecList(List<String> partitionFields, Long maxAllowedPartitionSpecFilters)
    {
        List<Map<String, Object>> partitionSpecList = new ArrayList<>();

        // select count_distinct_approx (pk1, pk2, ...) from staging_table with staging_filters
        LogicalPlan logicalPlanForApproxDistinctCount = LogicalPlanFactory.getLogicalPlanForApproxDistinctCount(datasets.stagingDataset(), partitionFields);
        List<TabularData> approxCountResult = executor.executePhysicalPlanAndGetResults(transformer.generatePhysicalPlan(logicalPlanForApproxDistinctCount));
        Optional<Object> obj = getFirstColumnValue(getFirstRowForFirstResult(approxCountResult));
        Optional<Long> maxDuplicatesValue = retrieveValueAsLong(obj.orElse(null));
        if (maxDuplicatesValue.isPresent() && maxDuplicatesValue.get() > maxAllowedPartitionSpecFilters)
        {
            return partitionSpecList;
        }

        // Select distinct pk1, pk2, ... from staging_table with staging_filters
        LogicalPlan logicalPlanForPartitionSpec = LogicalPlanFactory.getLogicalPlanForDistinctValues(datasets.stagingDataset(), partitionFields);
        List<TabularData> partitionSpecResult = executor.executePhysicalPlanAndGetResults(transformer.generatePhysicalPlan(logicalPlanForPartitionSpec));

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


    private List<OptimizationFilter> deriveOptimizationFilters(UnitemporalDeltaAbstract unitemporalDelta)
    {
        List<OptimizationFilter> optimizationFilters = new ArrayList<>();
        List<Field> primaryKeys = findCommonPrimaryFieldsBetweenMainAndStaging(datasets.mainDataset(), datasets.stagingDataset());
        List<Field> comparablePrimaryKeys = primaryKeys.stream().filter(field -> SUPPORTED_DATA_TYPES_FOR_OPTIMIZATION_COLUMNS.contains(field.type().dataType())).collect(Collectors.toList());
        for (Field field : comparablePrimaryKeys)
        {
            OptimizationFilter filter = OptimizationFilter.of(field.name());
            optimizationFilters.add(filter);
        }
        return optimizationFilters;
    }
}
