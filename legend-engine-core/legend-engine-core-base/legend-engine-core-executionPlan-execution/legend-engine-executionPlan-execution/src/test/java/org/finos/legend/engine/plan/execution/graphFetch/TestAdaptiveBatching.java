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


package org.finos.legend.engine.plan.execution.graphFetch;

import org.eclipse.collections.impl.factory.Maps;
import org.finos.legend.engine.plan.execution.nodes.state.ExecutionState;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;

public class TestAdaptiveBatching
{
    // Tests the return value when previous batch stats are null
    @Test
    public void testAdaptiveBatchingWithNoStats()
    {
        ExecutionState fakeExecutionState = new ExecutionState(Maps.mutable.empty(), Collections.emptyList(), Collections.emptyList());
        Assert.assertEquals(64, AdaptiveBatching.getAdaptiveBatchSize(fakeExecutionState));
    }

    // Tests the increase in batch size when far off from the soft limit
    @Test
    public void testIncreaseInBatchSizeFarFromSoftLimit()
    {
        GraphFetchExecutionConfiguration graphFetchExecutionConfiguration = new GraphFetchExecutionConfiguration(GraphFetchExecutionConfiguration.DEFAULT_BATCH_MEMORY_LIMIT, GraphFetchExecutionConfiguration.DEFAULT_SOFT_MEMORY_LIMIT_PERCENTAGE, true, GraphFetchExecutionConfiguration.DEFAULT_BATCH_SIZE);
        ExecutionState fakeExecutionState = new ExecutionState(Maps.mutable.empty(), Collections.emptyList(), Collections.emptyList(), true, graphFetchExecutionConfiguration);
        fakeExecutionState.adaptiveGraphBatchStats = new AdaptiveGraphBatchStats(0, 64);
        Assert.assertEquals(74, AdaptiveBatching.getAdaptiveBatchSize(fakeExecutionState));
        Assert.assertEquals(174, AdaptiveBatching.getAdaptiveBatchSize(fakeExecutionState));
        Assert.assertEquals(1174, AdaptiveBatching.getAdaptiveBatchSize(fakeExecutionState));
        Assert.assertEquals(11174, AdaptiveBatching.getAdaptiveBatchSize(fakeExecutionState));
    }

    // Tests the increase in batch size when near the soft limit
    @Test
    public void testIncreaseInBatchSizeNearSoftLimit()
    {
        GraphFetchExecutionConfiguration graphFetchExecutionConfiguration = new GraphFetchExecutionConfiguration(200, GraphFetchExecutionConfiguration.DEFAULT_SOFT_MEMORY_LIMIT_PERCENTAGE, true, GraphFetchExecutionConfiguration.DEFAULT_BATCH_SIZE);
        ExecutionState fakeExecutionState = new ExecutionState(Maps.mutable.empty(), Collections.emptyList(), Collections.emptyList(), true, graphFetchExecutionConfiguration);
        fakeExecutionState.adaptiveGraphBatchStats = new AdaptiveGraphBatchStats(49, 1);

        Assert.assertEquals(1, AdaptiveBatching.getAdaptiveBatchSize(fakeExecutionState));
        Assert.assertEquals(1, AdaptiveBatching.getAdaptiveBatchSize(fakeExecutionState));
    }

    // Tests exponential decrease in batch size
    @Test
    public void testExponentialDecreaseInBatchSize()
    {
        GraphFetchExecutionConfiguration graphFetchExecutionConfiguration = new GraphFetchExecutionConfiguration(200, GraphFetchExecutionConfiguration.DEFAULT_SOFT_MEMORY_LIMIT_PERCENTAGE, true, GraphFetchExecutionConfiguration.DEFAULT_BATCH_SIZE);
        ExecutionState fakeExecutionState = new ExecutionState(Maps.mutable.empty(), Collections.emptyList(), Collections.emptyList(), true, graphFetchExecutionConfiguration);
        fakeExecutionState.adaptiveGraphBatchStats = new AdaptiveGraphBatchStats(400, 4);

        Assert.assertEquals(2, AdaptiveBatching.getAdaptiveBatchSize(fakeExecutionState));
    }

    // Tests ideal decrease in batch size
    @Test
    public void testIdealDecreaseInBatchSize()
    {
        GraphFetchExecutionConfiguration graphFetchExecutionConfiguration = new GraphFetchExecutionConfiguration(200, GraphFetchExecutionConfiguration.DEFAULT_SOFT_MEMORY_LIMIT_PERCENTAGE, true, GraphFetchExecutionConfiguration.DEFAULT_BATCH_SIZE);
        ExecutionState fakeExecutionState = new ExecutionState(Maps.mutable.empty(), Collections.emptyList(), Collections.emptyList(), true, graphFetchExecutionConfiguration);
        fakeExecutionState.adaptiveGraphBatchStats = new AdaptiveGraphBatchStats(120, 4);

        Assert.assertEquals(3, AdaptiveBatching.getAdaptiveBatchSize(fakeExecutionState));
    }
}
