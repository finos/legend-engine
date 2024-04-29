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

import org.finos.legend.engine.plan.execution.cache.ExecutionCache;
import org.finos.legend.engine.plan.execution.cache.ExecutionCacheStats;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheKey;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class GraphObjectsBatch
{
    private static final ExecutionCache<GraphFetchCacheKey, List<Object>> nullCache = new ExecutionCache<GraphFetchCacheKey, List<Object>>()
    {
        @Override
        public List<Object> get(GraphFetchCacheKey key, Callable<? extends List<Object>> valueLoader)
        {
            throw new UnsupportedOperationException("no operations can be performed on null Cache");
        }

        @Override
        public List<Object> getIfPresent(GraphFetchCacheKey key)
        {
            throw new UnsupportedOperationException("no operations can be performed on null Cache");
        }

        @Override
        public Map<? extends GraphFetchCacheKey, ? extends List<Object>> getAllPresent(Iterable<? extends GraphFetchCacheKey> keys)
        {
            throw new UnsupportedOperationException("no operations can be performed on null Cache");
        }

        @Override
        public void put(GraphFetchCacheKey key, List<Object> value)
        {
            throw new UnsupportedOperationException("no operations can be performed on null Cache");
        }

        @Override
        public void putAll(Map<? extends GraphFetchCacheKey, ? extends List<Object>> keyValues)
        {
            throw new UnsupportedOperationException("no operations can be performed on null Cache");
        }

        @Override
        public void invalidate(GraphFetchCacheKey key)
        {
            throw new UnsupportedOperationException("no operations can be performed on null Cache");
        }

        @Override
        public void invalidateAll(Iterable<? extends GraphFetchCacheKey> keys)
        {
            throw new UnsupportedOperationException("no operations can be performed on null Cache");
        }

        @Override
        public void invalidateAll()
        {
            throw new UnsupportedOperationException("no operations can be performed on null Cache");
        }

        @Override
        public long estimatedSize()
        {
            throw new UnsupportedOperationException("no operations can be performed on null Cache");
        }

        @Override
        public ExecutionCacheStats stats()
        {
            throw new UnsupportedOperationException("no operations can be performed on null Cache");
        }
    };

    private final long graphFetchBatchMemoryLimit;
    protected final long batchIndex;
    protected ConcurrentMap<Integer, List<?>> nodeObjects;
    protected ConcurrentMap<Integer, ExecutionCache<GraphFetchCacheKey, List<Object>>> xStorePropertyCaches;
    protected AtomicLong totalObjectMemoryUtilization;
    protected AtomicLong rowCount;

    public GraphObjectsBatch(long batchIndex, long graphFetchBatchMemoryLimit)
    {
        this.graphFetchBatchMemoryLimit = graphFetchBatchMemoryLimit;
        this.batchIndex = batchIndex;
        this.nodeObjects = new ConcurrentHashMap<>();
        this.xStorePropertyCaches = new ConcurrentHashMap<>();
        this.totalObjectMemoryUtilization = new AtomicLong(0);
        this.rowCount = new AtomicLong(0);
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
        if (cache == null)
        {
            // concurrentMap doesn't support nulls as keys/values, so we put in a proxy instead.
            this.xStorePropertyCaches.put(index, nullCache);
            return;
        }
        this.xStorePropertyCaches.put(index, cache);
    }

    public ExecutionCache<GraphFetchCacheKey, List<Object>> getXStorePropertyCacheForNodeIndex(int index)
    {
        ExecutionCache<GraphFetchCacheKey, List<Object>> cache = this.xStorePropertyCaches.get(index);
        if (nullCache.equals(cache))
        {
            return null;
        }
        return cache;
    }

    public long getRowCount()
    {
        return this.rowCount.get();
    }

    public void incrementRowCount()
    {
        this.rowCount.incrementAndGet();
    }

    public void addObjectMemoryUtilization(long memoryBytes)
    {
        this.totalObjectMemoryUtilization.addAndGet(memoryBytes);
        if (this.totalObjectMemoryUtilization.get() > this.graphFetchBatchMemoryLimit)
        {
            throw new RuntimeException("Maximum memory reached when processing the graphFetch. Try reducing batch size of graphFetch fetch operation.");
        }
    }

    public long getTotalObjectMemoryUtilization()
    {
        return this.totalObjectMemoryUtilization.get();
    }
}
