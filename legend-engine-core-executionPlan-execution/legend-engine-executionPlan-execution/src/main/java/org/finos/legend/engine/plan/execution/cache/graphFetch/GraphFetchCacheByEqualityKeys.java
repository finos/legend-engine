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

import java.util.Objects;

public class GraphFetchCacheByEqualityKeys implements GraphFetchCache
{
    private final String mappingId;
    private final String instanceSetId;
    private final ExecutionCache<GraphFetchCacheKey, Object> cache;
    private String subTree;

    public GraphFetchCacheByEqualityKeys(String mappingId, String instanceSetId, ExecutionCache<GraphFetchCacheKey, Object> cache)
    {
        Objects.requireNonNull(mappingId, "Mapping ID should be non null");
        Objects.requireNonNull(instanceSetId, "Instance Set ID should be non null");
        Objects.requireNonNull(cache, "Cache should be non null");

        this.mappingId = mappingId;
        this.instanceSetId = instanceSetId;
        this.cache = cache;
    }

    public String getMappingId()
    {
        return this.mappingId;
    }

    public String getInstanceSetId()
    {
        return this.instanceSetId;
    }

    public void setSubTree(String subTree)
    {
        this.subTree = subTree;
    }

    public String getSubTree()
    {
        return this.subTree;
    }

    public boolean isCacheUtilized()
    {
        return this.subTree != null;
    }

    @Override
    public ExecutionCache<GraphFetchCacheKey, Object> getExecutionCache()
    {
        return this.cache;
    }

    @Override
    public boolean isValidForPlan(SingleExecutionPlan plan)
    {
        return true;
    }
}
