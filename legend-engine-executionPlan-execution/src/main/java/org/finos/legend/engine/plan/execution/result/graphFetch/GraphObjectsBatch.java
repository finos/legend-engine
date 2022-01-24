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

package org.finos.legend.engine.plan.execution.result.graphFetch;

import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.cache.ExecutionCache;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheKey;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GraphObjectsBatch
{
    private final long graphFetchBatchMemoryLimit;
    protected final long batchIndex;
    protected Map<Integer, List<?>> nodeObjects;
    protected Map<Integer, ExecutionCache<GraphFetchCacheKey, List<Object>>> xStorePropertyCaches;
    protected long totalObjectMemoryUtilization;
    protected long rowCount;

    @Deprecated
    public GraphObjectsBatch(long batchIndex)
    {
        this(batchIndex, PlanExecutor.DEFAULT_GRAPH_FETCH_BATCH_MEMORY_LIMIT);
    }

    public GraphObjectsBatch(long batchIndex, long graphFetchBatchMemoryLimit)
    {
        this.graphFetchBatchMemoryLimit = graphFetchBatchMemoryLimit;
        this.batchIndex = batchIndex;
        this.nodeObjects = new HashMap<>();
        this.xStorePropertyCaches = new HashMap<>();
        this.totalObjectMemoryUtilization = 0;
        this.rowCount = 0;
    }

    public GraphObjectsBatch(GraphObjectsBatch other)
    {
        this.graphFetchBatchMemoryLimit = other.graphFetchBatchMemoryLimit;
        this.batchIndex = other.batchIndex;
        this.nodeObjects = other.nodeObjects;
        this.xStorePropertyCaches = other.xStorePropertyCaches;
        this.totalObjectMemoryUtilization = other.totalObjectMemoryUtilization;
        this.rowCount = other.rowCount;
    }

    public long getBatchIndex()
    {
        return this.batchIndex;
    }

    public void setObjectsForNodeIndex(int index, List<?> objects)
    {
        this.nodeObjects.put(index, objects);
    }

    public List<?> getObjectsForNodeIndex(int index)
    {
        return this.nodeObjects.get(index);
    }

    public void setXStorePropertyCachesForNodeIndex(int index, ExecutionCache<GraphFetchCacheKey, List<Object>> cache)
    {
        this.xStorePropertyCaches.put(index, cache);
    }

    public ExecutionCache<GraphFetchCacheKey, List<Object>> getXStorePropertyCacheForNodeIndex(int index)
    {
        return this.xStorePropertyCaches.get(index);
    }

    public long getRowCount()
    {
        return this.rowCount;
    }

    public void incrementRowCount()
    {
        this.rowCount++;
    }

    public void addObjectMemoryUtilization(long memoryBytes)
    {
        this.totalObjectMemoryUtilization += memoryBytes;
        if (this.totalObjectMemoryUtilization > this.graphFetchBatchMemoryLimit)
        {
            throw new RuntimeException("Maximum memory reached when processing the graphFetch. Try reducing batch size of graphFetch fetch operation.");
        }
    }

    public long getTotalObjectMemoryUtilization()
    {
        return this.totalObjectMemoryUtilization;
    }
}
