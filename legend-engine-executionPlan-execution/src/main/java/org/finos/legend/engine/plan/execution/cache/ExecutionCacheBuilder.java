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

package org.finos.legend.engine.plan.execution.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheStats;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheByEqualityKeys;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheKey;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

public class ExecutionCacheBuilder
{
    public static <K, V> ExecutionCache<K, V> fromGuavaCache(Cache<K, V> guavaCache)
    {
        return buildExecutionCacheFromGuavaCache(guavaCache);
    }

    public static GraphFetchCacheByEqualityKeys buildGraphFetchCacheByEqualityKeysFromExecutionCache(ExecutionCache<GraphFetchCacheKey, Object> executionCache, String mappingId, String instanceSetId)
    {
        return new GraphFetchCacheByEqualityKeys(mappingId, instanceSetId, executionCache);
    }

    public static GraphFetchCacheByEqualityKeys buildGraphFetchCacheByEqualityKeysFromGuavaCache(Cache<GraphFetchCacheKey, Object> guavaCache, String mappingId, String instanceSetId)
    {
        ExecutionCache<GraphFetchCacheKey, Object> executionCache = buildExecutionCacheFromGuavaCache(guavaCache);
        return new GraphFetchCacheByEqualityKeys(mappingId, instanceSetId, executionCache);
    }

    private static <K, V> ExecutionCache<K, V> buildExecutionCacheFromGuavaCache(Cache<K, V> guavaCache)
    {
        return new ExecutionCache<K, V>()
        {
            @Override
            public V getIfPresent(K key)
            {
                return guavaCache.getIfPresent(key);
            }

            @Override
            public V get(K key, Callable<? extends V> valueLoader)
            {
                try
                {
                    return guavaCache.get(key, valueLoader);
                }
                catch (ExecutionException e)
                {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public Map<? extends K, ? extends V> getAllPresent(Iterable<? extends K> keys)
            {
                return guavaCache.getAllPresent(keys);
            }

            @Override
            public void put(K key, V value)
            {
                guavaCache.put(key, value);
            }

            @Override
            public void putAll(Map<? extends K, ? extends V> keyValues)
            {
                guavaCache.putAll(keyValues);
            }

            @Override
            public void invalidate(K key)
            {
                guavaCache.invalidate(key);
            }

            @Override
            public void invalidateAll(Iterable<? extends K> keys)
            {
                guavaCache.invalidateAll(keys);
            }

            @Override
            public void invalidateAll()
            {
                guavaCache.invalidateAll();
            }

            @Override
            public long estimatedSize()
            {
                return guavaCache.size();
            }

            @Override
            public ExecutionCacheStats stats()
            {
                CacheStats guavaCacheStats = guavaCache.stats();
                return buildExecutionCacheStatsFromGuavaCacheStats(guavaCacheStats);
            }
        };
    }

    private static ExecutionCacheStats buildExecutionCacheStatsFromGuavaCacheStats(CacheStats guavaCacheStats)
    {
        return new ExecutionCacheStats()
        {
            @Override
            public long requestCount()
            {
                return guavaCacheStats.requestCount();
            }

            @Override
            public long hitCount()
            {
                return guavaCacheStats.hitCount();
            }

            @Override
            public long missCount()
            {
                return guavaCacheStats.missCount();
            }

            @Override
            public long loadCount()
            {
                return guavaCacheStats.loadCount();
            }

            @Override
            public long loadSuccessCount()
            {
                return guavaCacheStats.loadSuccessCount();
            }

            @Override
            public long loadFailureCount()
            {
                return guavaCacheStats.loadExceptionCount();
            }

            @Override
            public long evictionCount()
            {
                return guavaCacheStats.evictionCount();
            }

            @Override
            public double hitRate()
            {
                return guavaCacheStats.hitRate();
            }

            @Override
            public double missRate()
            {
                return guavaCacheStats.missRate();
            }

            @Override
            public double loadFailureRate()
            {
                return guavaCacheStats.loadExceptionRate();
            }

            @Override
            public double averageLoadPenalty()
            {
                return guavaCacheStats.averageLoadPenalty();
            }

            @Override
            public long totalLoadTime()
            {
                return guavaCacheStats.totalLoadTime();
            }

            @Override
            public String toString()
            {
                return this.buildStatsString();
            }
        };
    }
}
