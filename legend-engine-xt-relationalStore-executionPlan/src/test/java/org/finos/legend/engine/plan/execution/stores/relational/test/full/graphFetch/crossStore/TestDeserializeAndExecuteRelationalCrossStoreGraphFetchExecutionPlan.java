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

package org.finos.legend.engine.plan.execution.stores.relational.test.full.graphFetch.crossStore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.PlanExecutionContext;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.cache.ExecutionCache;
import org.finos.legend.engine.plan.execution.cache.ExecutionCacheBuilder;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCache;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheByTargetCrossKeys;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheKey;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCrossAssociationKeys;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamToPureFormatSerializer;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.javaCompiler.JavaCompileException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TestDeserializeAndExecuteRelationalCrossStoreGraphFetchExecutionPlan
{
    private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();

    @Test
    public void testDeserializeAndExecutePlansWithJavaSourceCode() throws IOException
    {
        List<String> sourceCodePlanResources = Collections.singletonList(
                "org/finos/legend/engine/plan/execution/stores/relational/test/full/graphFetch/crossStore/withSourceCode/plan1.json"
        );

        for (String planWithSourceCode: sourceCodePlanResources)
        {
            SingleExecutionPlan plan = readPlan(planWithSourceCode);
            Assert.assertEquals(getExpectedResult(), executePlan(plan));
            Assert.assertEquals(getExpectedResult(), executePlan(plan));
        }
    }

    @Test
    public void testDeserializeAndExecutePlansWithJavaSourceCodeWithCache() throws IOException, JavaCompileException
    {
        List<String> sourceCodePlanResources = Collections.singletonList(
                "org/finos/legend/engine/plan/execution/stores/relational/test/full/graphFetch/crossStore/withSourceCode/plan1.json"
        );

        for (String planWithSourceCode: sourceCodePlanResources)
        {
            SingleExecutionPlan plan = readPlan(planWithSourceCode);
            GraphFetchCacheByTargetCrossKeys firmCache = getFirmEmptyCache(plan);
            List<GraphFetchCacheByTargetCrossKeys> addressCaches = getSharedAddressCaches(plan);
            PlanExecutionContext context = new PlanExecutionContext(plan, Lists.mutable.of((GraphFetchCache) firmCache).withAll(addressCaches));

            Assert.assertEquals(getExpectedResult(), executePlan(plan, context));
            assertCacheStats(firmCache.getExecutionCache(), 3, 5, 2, 3);
            assertCacheStats(addressCaches.get(0).getExecutionCache(), 5, 7, 2, 5);

            Assert.assertEquals(getExpectedResult(), executePlan(plan, context));
            assertCacheStats(firmCache.getExecutionCache(), 3, 10, 7, 3);
            assertCacheStats(addressCaches.get(0).getExecutionCache(), 5, 12, 7, 5);
        }
    }

    @Test
    public void testDeserializeAndExecutePlansWithJavaByteCode() throws IOException
    {
        List<String> byteCodePlanResources = Collections.singletonList(
                "org/finos/legend/engine/plan/execution/stores/relational/test/full/graphFetch/crossStore/withByteCode/plan1.json"
        );

        for (String planWithByteCode: byteCodePlanResources)
        {
            SingleExecutionPlan plan = readPlan(planWithByteCode);
            Assert.assertEquals(getExpectedResult(), executePlan(plan));
            Assert.assertEquals(getExpectedResult(), executePlan(plan));
        }
    }

    @Test
    public void testDeserializeAndExecutePlansWithJavaByteCodeWithCache() throws IOException, JavaCompileException
    {
        List<String> byteCodePlanResources = Collections.singletonList(
                "org/finos/legend/engine/plan/execution/stores/relational/test/full/graphFetch/crossStore/withByteCode/plan1.json"
        );

        for (String planWithByteCode: byteCodePlanResources)
        {
            SingleExecutionPlan plan = readPlan(planWithByteCode);
            GraphFetchCacheByTargetCrossKeys firmCache = getFirmEmptyCache(plan);
            List<GraphFetchCacheByTargetCrossKeys> addressCaches = getSharedAddressCaches(plan);
            PlanExecutionContext context = new PlanExecutionContext(plan, Lists.mutable.of((GraphFetchCache) firmCache).withAll(addressCaches));

            Assert.assertEquals(getExpectedResult(), executePlan(plan, context));
            assertCacheStats(firmCache.getExecutionCache(), 3, 5, 2, 3);
            assertCacheStats(addressCaches.get(0).getExecutionCache(), 5, 7, 2, 5);

            Assert.assertEquals(getExpectedResult(), executePlan(plan, context));
            assertCacheStats(firmCache.getExecutionCache(), 3, 10, 7, 3);
            assertCacheStats(addressCaches.get(0).getExecutionCache(), 5, 12, 7, 5);
        }
    }

    private String getExpectedResult()
    {
        return "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\"}},\"address\":{\"name\":\"A1\"}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\",\"address\":{\"name\":\"A3\"}},\"address\":{\"name\":\"A2\"}}," +
                "{\"fullName\":\"P3\",\"firm\":null,\"address\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null,\"address\":{\"name\":\"A3\"}}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\"}},\"address\":{\"name\":\"A1\"}}" +
                "]";
    }

    private SingleExecutionPlan readPlan(String resourcePath) throws IOException
    {
        return OBJECT_MAPPER.readValue(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(resourcePath)), SingleExecutionPlan.class);
    }

    private String executePlan(SingleExecutionPlan plan)
    {
        return executePlan(plan, null);
    }

    private String executePlan(SingleExecutionPlan plan, PlanExecutionContext planExecutionContext)
    {
        JsonStreamingResult result = (JsonStreamingResult) PlanExecutor.newPlanExecutorWithAvailableStoreExecutors().executeWithArgs(PlanExecutor.ExecuteArgs.newArgs().withPlan(plan).withPlanExecutionContext(planExecutionContext).build());
        return result.flush(new JsonStreamToPureFormatSerializer(result));
    }

    private GraphFetchCacheByTargetCrossKeys getFirmEmptyCache(SingleExecutionPlan plan)
    {
        return ExecutionCacheBuilder.buildGraphFetchCacheByTargetCrossKeysFromGuavaCache(
                CacheBuilder.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build(),
                GraphFetchCrossAssociationKeys.graphFetchCrossAssociationKeysForPlan(plan).stream().filter(x -> x.getName().equals("<default, root.firm>")).findFirst().orElse(null)
        );
    }

    private List<GraphFetchCacheByTargetCrossKeys> getSharedAddressCaches(SingleExecutionPlan plan)
    {
        Cache<GraphFetchCacheKey, List<Object>> cache = CacheBuilder.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build();
        return Arrays.asList(
                ExecutionCacheBuilder.buildGraphFetchCacheByTargetCrossKeysFromGuavaCache(
                        cache,
                        Objects.requireNonNull(GraphFetchCrossAssociationKeys.graphFetchCrossAssociationKeysForPlan(plan).stream().filter(x -> x.getName().equals("<default, root.firm.address>")).findFirst().orElse(null))
                ),
                ExecutionCacheBuilder.buildGraphFetchCacheByTargetCrossKeysFromGuavaCache(
                        cache,
                        Objects.requireNonNull(GraphFetchCrossAssociationKeys.graphFetchCrossAssociationKeysForPlan(plan).stream().filter(x -> x.getName().equals("<default, root.address>")).findFirst().orElse(null))
                )
        );
    }

    private void assertCacheStats(ExecutionCache<?, ?> cache, int estimatedSize, int requestCount, int hitCount, int missCount)
    {
        Assert.assertEquals(estimatedSize, cache.estimatedSize());
        Assert.assertEquals(requestCount, cache.stats().requestCount());
        Assert.assertEquals(hitCount, cache.stats().hitCount());
        Assert.assertEquals(missCount, cache.stats().missCount());
    }
}
