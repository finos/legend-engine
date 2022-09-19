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

package org.finos.legend.engine.persistence.components.planner;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnlyAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalDeltaAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalSnapshotAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.IngestModeVisitor;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalDeltaAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshotAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDeltaAbstract;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshotAbstract;

public class Planners
{
    private Planners()
    {
    }

    public static Planner get(Datasets datasets, IngestMode ingestMode)
    {
        return ingestMode.accept(new PlannerFactory(datasets, PlannerOptions.builder().build()));
    }

    public static Planner get(Datasets datasets, IngestMode ingestMode, PlannerOptions plannerOptions)
    {
        return ingestMode.accept(new PlannerFactory(datasets, plannerOptions));
    }

    static class PlannerFactory implements IngestModeVisitor<Planner>
    {
        private final Datasets datasets;
        private final PlannerOptions plannerOptions;

        PlannerFactory(Datasets datasets, PlannerOptions plannerOptions)
        {
            this.datasets = datasets;
            this.plannerOptions = plannerOptions;
        }

        @Override
        public Planner visitAppendOnly(AppendOnlyAbstract appendOnly)
        {
            return new AppendOnlyPlanner(datasets, (AppendOnly) appendOnly, plannerOptions);
        }

        @Override
        public Planner visitNontemporalSnapshot(NontemporalSnapshotAbstract nontemporalSnapshot)
        {
            return new NontemporalSnapshotPlanner(datasets, (NontemporalSnapshot) nontemporalSnapshot, plannerOptions);
        }

        @Override
        public Planner visitNontemporalDelta(NontemporalDeltaAbstract nontemporalDelta)
        {
            return new NontemporalDeltaPlanner(datasets, (NontemporalDelta) nontemporalDelta, plannerOptions);
        }

        @Override
        public Planner visitUnitemporalSnapshot(UnitemporalSnapshotAbstract unitemporalSnapshot)
        {
            return new UnitemporalSnapshotPlanner(datasets, (UnitemporalSnapshot) unitemporalSnapshot, plannerOptions);
        }

        @Override
        public Planner visitUnitemporalDelta(UnitemporalDeltaAbstract unitemporalDelta)
        {
            return new UnitemporalDeltaPlanner(datasets, (UnitemporalDelta) unitemporalDelta, plannerOptions);
        }

        @Override
        public Planner visitBitemporalSnapshot(BitemporalSnapshotAbstract bitemporalSnapshot)
        {
            return new BitemporalSnapshotPlanner(datasets, (BitemporalSnapshot) bitemporalSnapshot, plannerOptions);
        }

        @Override
        public Planner visitBitemporalDelta(BitemporalDeltaAbstract bitemporalDelta)
        {
            return new BitemporalDeltaPlanner(datasets, (BitemporalDelta) bitemporalDelta, plannerOptions);
        }
    }
}
