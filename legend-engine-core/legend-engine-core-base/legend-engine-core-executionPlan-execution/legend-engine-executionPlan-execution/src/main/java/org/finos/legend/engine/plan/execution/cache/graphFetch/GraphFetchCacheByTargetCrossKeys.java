// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.cache.graphFetch;

import org.finos.legend.engine.plan.execution.cache.ExecutionCache;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;

import java.util.List;

public class GraphFetchCacheByTargetCrossKeys implements GraphFetchCache
{
    private final GraphFetchCrossAssociationKeys graphFetchCrossAssociationKeys;
    private final ExecutionCache<GraphFetchCacheKey, List<Object>> cache;

    public GraphFetchCacheByTargetCrossKeys(GraphFetchCrossAssociationKeys graphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>> cache)
    {
        this.graphFetchCrossAssociationKeys = graphFetchCrossAssociationKeys;
        this.cache = cache;
    }

    public GraphFetchCrossAssociationKeys getGraphFetchCrossAssociationKeys()
    {
        return this.graphFetchCrossAssociationKeys;
    }

    @Override
    public ExecutionCache<GraphFetchCacheKey, List<Object>> getExecutionCache()
    {
        return this.cache;
    }

    @Override
    public boolean isValidForPlan(SingleExecutionPlan plan)
    {
        return this.graphFetchCrossAssociationKeys.getPlan().equals(plan);
    }
}
