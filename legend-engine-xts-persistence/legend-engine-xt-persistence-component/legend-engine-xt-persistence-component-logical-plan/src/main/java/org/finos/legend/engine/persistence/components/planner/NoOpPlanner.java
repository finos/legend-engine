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

package org.finos.legend.engine.persistence.components.planner;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.Resources;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.NoOp;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Create;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.util.Capability;

import java.util.*;

class NoOpPlanner extends Planner
{
    NoOpPlanner(Datasets datasets, NoOp ingestMode, PlannerOptions plannerOptions, Set<Capability> capabilities)
    {
        super(datasets, ingestMode, plannerOptions, capabilities);
    }

    @Override
    protected NoOp ingestMode()
    {
        return (NoOp) super.ingestMode();
    }

    public LogicalPlan buildLogicalPlanForPreActions()
    {
        List<Operation> operations = new ArrayList<>();
        if (!options().skipMainAndMetadataDatasetCreation())
        {
            operations.add(Create.of(true, metadataDataset().orElseThrow(IllegalStateException::new).get()));
        }
        if (options().enableConcurrentSafety())
        {
            operations.add(Create.of(true, lockInfoDataset().orElseThrow(IllegalStateException::new).get()));
        }
        return LogicalPlan.of(operations);
    }

    @Override
    public LogicalPlan buildLogicalPlanForIngest(Resources resources)
    {
        return LogicalPlan.of(Collections.emptyList());
    }

    @Override
    protected void addPostRunStatsForIncomingRecords(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        // Not supported
    }

    protected void addPostRunStatsForRowsTerminated(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        // Not supported
    }

    protected void addPostRunStatsForRowsUpdated(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        // Not supported
    }

    protected void addPostRunStatsForRowsInserted(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        // Not supported
    }

    protected void addPostRunStatsForRowsDeleted(Map<StatisticName, LogicalPlan> postRunStatisticsResult)
    {
        // Not supported
    }

    public LogicalPlan buildLogicalPlanForPostActions(Resources resources)
    {
        return LogicalPlan.of(Collections.emptyList());
    }


    @Override
    List<String> getDigestOrRemainingColumns()
    {
        return Collections.emptyList();
    }

    @Override
    public boolean dataSplitExecutionSupported()
    {
        return false;
    }
}
