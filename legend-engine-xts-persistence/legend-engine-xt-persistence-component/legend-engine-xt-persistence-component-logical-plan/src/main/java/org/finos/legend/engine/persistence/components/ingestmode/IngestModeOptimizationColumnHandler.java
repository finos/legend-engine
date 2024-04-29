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

package org.finos.legend.engine.persistence.components.ingestmode;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.OptimizationFilter;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.SUPPORTED_DATA_TYPES_FOR_OPTIMIZATION_COLUMNS;
import static org.finos.legend.engine.persistence.components.util.LogicalPlanUtils.findCommonPrimaryFieldsBetweenMainAndStaging;

public class IngestModeOptimizationColumnHandler implements IngestModeVisitor<IngestMode>
{
    Datasets datasets;

    public IngestModeOptimizationColumnHandler(Datasets datasets)
    {
        this.datasets = datasets;
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


    private List<OptimizationFilter> deriveOptimizationFilters(UnitemporalDeltaAbstract unitemporalDelta)
    {
        List<OptimizationFilter> optimizationFilters = unitemporalDelta.optimizationFilters();
        if (optimizationFilters == null || optimizationFilters.isEmpty())
        {
            List<Field> primaryKeys = findCommonPrimaryFieldsBetweenMainAndStaging(datasets.mainDataset(), datasets.stagingDataset());
            List<Field> comparablePrimaryKeys = primaryKeys.stream().filter(field -> SUPPORTED_DATA_TYPES_FOR_OPTIMIZATION_COLUMNS.contains(field.type().dataType())).collect(Collectors.toList());
            optimizationFilters = new ArrayList<>();
            for (Field field : comparablePrimaryKeys)
            {
                OptimizationFilter filter = OptimizationFilter.of(field.name(), field.name().toUpperCase() + "_LOWER", field.name().toUpperCase() + "_UPPER");
                optimizationFilters.add(filter);
            }
        }
        return optimizationFilters;
    }
}
