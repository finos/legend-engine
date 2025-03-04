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
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnlyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalDeltaAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalSnapshotAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.BulkLoadAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.IngestModeVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.NoOpAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalDeltaAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshotAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDeltaAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshotAbstract;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.transformer.Transformer;
import org.finos.legend.engine.persistence.components.util.PlaceholderValue;

import java.util.Map;

public class MultiDatasetIngestModeRefresher implements IngestModeVisitor<IngestMode>
{
    private Datasets datasets;
    private Executor<SqlGen, TabularData, SqlPlan> executor;
    Transformer<SqlGen, SqlPlan> transformer;
    Map<String, PlaceholderValue> placeHolderKeyValues;

    public MultiDatasetIngestModeRefresher(Datasets datasets, Executor<SqlGen, TabularData, SqlPlan> executor, Transformer<SqlGen, SqlPlan> transformer, Map<String, PlaceholderValue> placeHolderKeyValues)
    {
        this.datasets = datasets;
        this.executor = executor;
        this.transformer = transformer;
        this.placeHolderKeyValues = placeHolderKeyValues;
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
        return new UnitemporalSnapshotOptimizer((UnitemporalSnapshot) unitemporalSnapshot, datasets, executor, transformer, placeHolderKeyValues).optimize();
    }

    @Override
    public IngestMode visitUnitemporalDelta(UnitemporalDeltaAbstract unitemporalDelta)
    {
        return unitemporalDelta;
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
}
