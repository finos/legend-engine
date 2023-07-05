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

package org.finos.legend.engine.plan.execution.cache.test;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.eclipse.collections.api.factory.Maps;
import org.finos.legend.engine.plan.execution.cache.ExecutionCache;
import org.finos.legend.engine.plan.execution.cache.ExecutionCacheBuilder;
import org.finos.legend.engine.plan.execution.cache.ExecutionCacheStats;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class TestExecutionCache
{
    @Test
    public void testExecutionCacheCreationFromGuavaCache()
    {
        Cache<Integer, String> guavaCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();
        ExecutionCache<Integer, String> executionCache = ExecutionCacheBuilder.fromGuavaCache(guavaCache);
        Assert.assertNotNull(executionCache);
    }

    @Test
    public void testExecutionCacheFromGuavaCacheGet()
    {
        Cache<Integer, String> guavaCache = CacheBuilder.newBuilder().recordStats().expireAfterWrite(1, TimeUnit.MINUTES).build();
        ExecutionCache<Integer, String> executionCache = ExecutionCacheBuilder.fromGuavaCache(guavaCache);

        executionCache.get(1, () ->
        {
            Thread.sleep(1000);
            return "Value 1";
        });
        Assert.assertEquals("Value 1", executionCache.getIfPresent(1));
        Assert.assertEquals(1, executionCache.getAllPresent(Collections.singletonList(1)).size());

        executionCache.get(2, () ->
        {
            Thread.sleep(1000);
            return "Value 2";
        });
        Assert.assertEquals("Value 2", executionCache.getIfPresent(2));
        Assert.assertEquals(2, executionCache.getAllPresent(Arrays.asList(1, 2)).size());

        executionCache.get(3, () ->
        {
            Thread.sleep(1000);
            return "Value 3";
        });

        Assert.assertEquals(3, executionCache.estimatedSize());

        Assert.assertEquals(8, executionCache.stats().requestCount());
        Assert.assertEquals(5, executionCache.stats().hitCount());
        Assert.assertEquals(3, executionCache.stats().missCount());
        Assert.assertEquals(3, executionCache.stats().loadCount());
        Assert.assertEquals(3, executionCache.stats().loadSuccessCount());
        Assert.assertEquals(0, executionCache.stats().loadFailureCount());
        Assert.assertTrue(3000 <= executionCache.stats().totalLoadTime());
        Assert.assertTrue(1000 <= executionCache.stats().averageLoadPenalty());
    }

    @Test
    public void testExecutionCacheFromGuavaCachePut()
    {
        Cache<Integer, String> guavaCache = CacheBuilder.newBuilder().recordStats().expireAfterWrite(1, TimeUnit.MINUTES).build();
        ExecutionCache<Integer, String> executionCache = ExecutionCacheBuilder.fromGuavaCache(guavaCache);

        executionCache.put(1, "Value 1");
        Assert.assertEquals("Value 1", executionCache.getIfPresent(1));
        Assert.assertEquals(1, executionCache.getAllPresent(Collections.singletonList(1)).size());

        executionCache.putAll(Maps.mutable.with(2, "Value 2", 3, "Value 3"));
        Assert.assertEquals("Value 2", executionCache.getIfPresent(2));
        Assert.assertEquals(2, executionCache.getAllPresent(Arrays.asList(1, 2)).size());
        Assert.assertEquals(3, executionCache.estimatedSize());

        Assert.assertEquals(5, executionCache.stats().requestCount());
        Assert.assertEquals(5, executionCache.stats().hitCount());
        Assert.assertEquals(0, executionCache.stats().missCount());
        Assert.assertEquals(0, executionCache.stats().loadCount());
        Assert.assertEquals(0, executionCache.stats().loadSuccessCount());
        Assert.assertEquals(0, executionCache.stats().loadFailureCount());
        Assert.assertEquals(0, executionCache.stats().totalLoadTime());
        Assert.assertEquals(0, executionCache.stats().averageLoadPenalty(), 0.0);
    }

    @Test
    public void testExecutionCacheFromGuavaCacheInvalidate()
    {
        Cache<Integer, String> guavaCache = CacheBuilder.newBuilder().recordStats().expireAfterWrite(1, TimeUnit.MINUTES).build();
        ExecutionCache<Integer, String> executionCache = ExecutionCacheBuilder.fromGuavaCache(guavaCache);

        executionCache.put(1, "Value 1");
        executionCache.putAll(Maps.mutable.with(2, "Value 2", 3, "Value 3", 4, "Value 4", 5, "Value 5"));
        Assert.assertEquals(5, executionCache.estimatedSize());

        executionCache.invalidate(1);
        Assert.assertEquals(4, executionCache.estimatedSize());
        Assert.assertEquals(4, executionCache.getAllPresent(Arrays.asList(2, 3, 4, 5)).size());

        executionCache.invalidateAll(Arrays.asList(2, 3));
        Assert.assertEquals(2, executionCache.estimatedSize());
        Assert.assertEquals(2, executionCache.getAllPresent(Arrays.asList(4, 5)).size());


        executionCache.invalidateAll();
        Assert.assertEquals(0, executionCache.estimatedSize());
        Assert.assertEquals(0, executionCache.getAllPresent(Arrays.asList(1, 2, 3, 4, 5)).size());
    }

    @Test
    public void testExecutionCacheFromGuavaCacheStatsString()
    {
        Cache<Integer, String> guavaCache = CacheBuilder.newBuilder().recordStats().expireAfterWrite(1, TimeUnit.MINUTES).build();
        ExecutionCache<Integer, String> executionCache = ExecutionCacheBuilder.fromGuavaCache(guavaCache);

        executionCache.get(1, () ->
        {
            Thread.sleep(1000);
            return "Value 1";
        });
        Assert.assertEquals("Value 1", executionCache.getIfPresent(1));
        Assert.assertEquals(1, executionCache.getAllPresent(Collections.singletonList(1)).size());

        executionCache.get(2, () ->
        {
            Thread.sleep(1000);
            return "Value 2";
        });
        Assert.assertEquals("Value 2", executionCache.getIfPresent(2));
        Assert.assertEquals(2, executionCache.getAllPresent(Arrays.asList(1, 2)).size());

        executionCache.get(3, () ->
        {
            Thread.sleep(1000);
            return "Value 3";
        });

        ExecutionCacheStats stats = executionCache.stats();
        String expected = "ExecutionCacheStats(\n" +
                "   requestCount = " + stats.requestCount() + ",\n" +
                "   hitCount = " + stats.hitCount() + ",\n" +
                "   missCount = " + stats.missCount() + ",\n" +
                "   loadCount = " + stats.loadCount() + ",\n" +
                "   loadSuccessCount = " + stats.loadSuccessCount() + ",\n" +
                "   loadFailureCount = " + stats.loadFailureCount() + ",\n" +
                "   evictionCount = " + stats.evictionCount() + "\n" +
                "   hitRate = " + stats.hitRate() + ",\n" +
                "   missRate = " + stats.missRate() + ",\n" +
                "   loadFailureRate = " + stats.loadFailureRate() + ",\n" +
                "   averageLoadPenalty = " + stats.averageLoadPenalty() + ",\n" +
                "   totalLoadTime = " + stats.totalLoadTime() + ",\n" +
                ")";

        Assert.assertEquals(expected, stats.toString());
    }

    @Test
    public void testExecutionCacheFromGuavaCacheTTL() throws InterruptedException
    {
        Cache<Integer, String> guavaCache = CacheBuilder.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.SECONDS).build();
        ExecutionCache<Integer, String> executionCache = ExecutionCacheBuilder.fromGuavaCache(guavaCache);

        executionCache.get(1, () -> "Value 1");
        Assert.assertEquals(1, executionCache.estimatedSize());
        Assert.assertEquals("Value 1", executionCache.getIfPresent(1));

        Thread.sleep(5000);

        executionCache.get(2, () -> "Value 2");
        Assert.assertEquals(2, executionCache.estimatedSize());
        Assert.assertEquals("Value 2", executionCache.getIfPresent(2));

        Thread.sleep(6000);

        Assert.assertNull(executionCache.getIfPresent(1));
        Assert.assertEquals("Value 2", executionCache.getIfPresent(2));

        executionCache.get(3, () -> "Value 3");
        Assert.assertEquals("Value 3", executionCache.getIfPresent(3));

        Thread.sleep(5000);

        Assert.assertNull(executionCache.getIfPresent(1));
        Assert.assertNull(executionCache.getIfPresent(2));
        Assert.assertEquals("Value 3", executionCache.getIfPresent(3));

        Thread.sleep(6000);

        Assert.assertNull(executionCache.getIfPresent(1));
        Assert.assertNull(executionCache.getIfPresent(2));
        Assert.assertNull(executionCache.getIfPresent(3));
        Assert.assertEquals(3, executionCache.stats().evictionCount());
    }
}
