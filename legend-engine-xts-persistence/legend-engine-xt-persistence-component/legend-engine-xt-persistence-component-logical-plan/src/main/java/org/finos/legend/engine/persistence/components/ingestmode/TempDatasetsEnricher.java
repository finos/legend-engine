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
import org.finos.legend.engine.persistence.components.ingestmode.merge.MergeStrategyVisitors;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
import org.finos.legend.engine.persistence.components.util.LogicalPlanUtils;

import java.util.Optional;

public class TempDatasetsEnricher implements IngestModeVisitor<Datasets>
{
    Datasets datasets;

    public TempDatasetsEnricher(Datasets datasets)
    {
        this.datasets = datasets;
    }


    @Override
    public Datasets visitAppendOnly(AppendOnlyAbstract appendOnly)
    {
        return datasets;
    }

    @Override
    public Datasets visitNontemporalSnapshot(NontemporalSnapshotAbstract nontemporalSnapshot)
    {
        return datasets;
    }

    @Override
    public Datasets visitNontemporalDelta(NontemporalDeltaAbstract nontemporalDelta)
    {
        return datasets;
    }

    @Override
    public Datasets visitUnitemporalSnapshot(UnitemporalSnapshotAbstract unitemporalSnapshot)
    {
        return datasets;
    }

    @Override
    public Datasets visitUnitemporalDelta(UnitemporalDeltaAbstract unitemporalDelta)
    {
        return datasets;
    }

    @Override
    public Datasets visitBitemporalSnapshot(BitemporalSnapshotAbstract bitemporalSnapshot)
    {
        return datasets;
    }

    @Override
    public Datasets visitBitemporalDelta(BitemporalDeltaAbstract bitemporalDelta)
    {
        Datasets enrichedDatasets = datasets;
        if (bitemporalDelta.validityMilestoning().validityDerivation() instanceof SourceSpecifiesFromDateTime)
        {
            Optional<String> deleteIndicatorField = bitemporalDelta.mergeStrategy().accept(MergeStrategyVisitors.EXTRACT_DELETE_FIELD);
            enrichedDatasets = enrichedDatasets.withTempDataset(LogicalPlanUtils.getTempDataset(enrichedDatasets));
            if (deleteIndicatorField.isPresent())
            {
                enrichedDatasets = enrichedDatasets.withTempDatasetWithDeleteIndicator(LogicalPlanUtils.getTempDatasetWithDeleteIndicator(enrichedDatasets, deleteIndicatorField.get()));
            }
        }

        return enrichedDatasets;
    }

    @Override
    public Datasets visitBulkLoad(BulkLoadAbstract bulkLoad)
    {
        return datasets;
    }
}
