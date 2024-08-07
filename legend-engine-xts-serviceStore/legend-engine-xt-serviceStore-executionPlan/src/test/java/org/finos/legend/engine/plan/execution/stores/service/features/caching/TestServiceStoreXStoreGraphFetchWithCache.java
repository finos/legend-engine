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

package org.finos.legend.engine.plan.execution.stores.service.features.caching;

import com.google.common.cache.CacheBuilder;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.engine.plan.execution.PlanExecutionContext;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.cache.ExecutionCache;
import org.finos.legend.engine.plan.execution.cache.ExecutionCacheBuilder;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCache;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheKey;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCrossAssociationKeys;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamToJsonDefaultSerializer;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;

import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestSuite;
import org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;

import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.javaCompiler.EngineJavaCompiler;
import org.finos.legend.engine.shared.javaCompiler.JavaCompileException;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import net.javacrumbs.jsonunit.JsonAssert;

import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.buildPlanForQuery;
import static org.finos.legend.engine.plan.execution.stores.service.utils.ServiceStoreTestUtils.executePlan;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestServiceStoreXStoreGraphFetchWithCache extends ServiceStoreTestSuite
{
    private static String pureGrammar;
    private static String mapping;
    private static String runtime;
    private static String jsonPrepend;

    @BeforeClass
    public static void setup()
    {
        setupServer("features/caching");
        String serviceUrl = "'http://127.0.0.1:" + getPort() + "';\n";
        // there is only one mapping and runtime definition needed for this test
        mapping = "test::serviceStore::caching::Map";
        runtime = "test::serviceStore::caching::Runtime";
        // used to enable jsonAsserts on plan execution results
        jsonPrepend = "{\"builder\":{\"_type\":\"json\"}, \"values\":";

        String serviceStoreConnection =
                "###Connection\n" +
                        "ServiceStoreConnection test::serviceStore::caching::connection::PersonConnection\n" +
                        "{\n" +
                        "    store   : test::serviceStore::caching::store::PersonService;\n" +
                        "    baseUrl : " + serviceUrl +
                        "}" +
                        "ServiceStoreConnection test::serviceStore::caching::connection::FirmConnection\n" +
                        "{\n" +
                        "    store   : test::serviceStore::caching::store::FirmService;\n" +
                        "    baseUrl : " + serviceUrl +
                        "}" +
                        "ServiceStoreConnection test::serviceStore::caching::connection::AddressConnection\n" +
                        "{\n" +
                        "    store   : test::serviceStore::caching::store::AddressService;\n" +
                        "    baseUrl : " + serviceUrl +
                        "}" +
                        "ServiceStoreConnection test::serviceStore::caching::connection::StreetConnection\n" +
                        "{\n" +
                        "    store   : test::serviceStore::caching::store::StreetService;\n" +
                        "    baseUrl : " + serviceUrl +
                        "}";
        pureGrammar = ServiceStoreTestUtils.readGrammarFromPureFile("/features/caching/xStorePropertyAccessServices.pure") +
                "\n\n" + serviceStoreConnection;
    }

    /**
     * A GraphFetch query which touches one field from one service (no cross-store access)
     */
    @Test
    public void XStoreServiceWithNoCrossPropertyAccess()
    {
        String query = "###Pure\n" +
                "function testfetch1::query(): Any[1]\n" +
                "{\n" +
                "  {|test::Person.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::Person {\n" +
                "        fullName\n" +
                "      }\n" +
                "    }#, 1)\n" +
                "    ->serialize(#{\n" +
                "      test::Person {\n" +
                "        fullName\n" +
                "      }\n" +
                "    }#)};\n" +
                "}";
        SingleExecutionPlan fetchPlan = buildPlanForQuery(pureGrammar + "\n\n" + query, mapping, runtime);
        String expectedJson = jsonPrepend + "[{\"fullName\":\"P1\"},{\"fullName\":\"P2\"},{\"fullName\":\"P3\"},{\"fullName\":\"P4\"},{\"fullName\":\"P5\"}]}";
        JsonAssert.assertJsonEquals(expectedJson, executePlan(fetchPlan));

        // since this is not Cross-Store, check that the GraphFetchCrossAssociationKeys are empty
        List<GraphFetchCrossAssociationKeys> gfxks = GraphFetchCrossAssociationKeys.graphFetchCrossAssociationKeysForPlan(fetchPlan);
        Assert.assertEquals(Collections.emptyList(), gfxks);
    }

    /**
     * A GraphFetch query which touches one property each from two services (one cross-store access)
     * Fetch2 tests 2:1 multiplicity
     */
    @Test
    public void XStoreServiceWithSingleCrossPropertyAccess() throws JavaCompileException
    {
        String query = "###Pure\n" +
                "function testfetch2::query(): Any[1]\n" +
                "{\n" +
                "  {|test::Person.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name\n" +
                "        }\n" +
                "      }\n" +
                "    }#, 1)\n" +
                "    ->serialize(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name\n" +
                "        }\n" +
                "      }\n" +
                "    }#)};\n" +
                "}";

        String expectedJson = jsonPrepend + "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\"}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\"}}," +
                "{\"fullName\":\"P3\",\"firm\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\"}}" +
                "]}";

        SingleExecutionPlan fetchPlan = buildPlanForQuery(pureGrammar + "\n\n" + query, mapping, runtime);
        List<GraphFetchCrossAssociationKeys> gfxks = GraphFetchCrossAssociationKeys.graphFetchCrossAssociationKeysForPlan(fetchPlan);
        Assert.assertEquals(1, gfxks.size());
        Assert.assertEquals("<default, root.firm@firm_set>", gfxks.get(0).getName());

        ExecutionCache<GraphFetchCacheKey, List<Object>> firmCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        Map<GraphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>>> cacheConfig = Maps.mutable.of(gfxks.get(0), firmCache);
        PlanExecutorHelper planExecutorHelper = new PlanExecutorHelper(fetchPlan, cacheConfig);

        JsonAssert.assertJsonEquals(expectedJson, planExecutorHelper.executePlan());
        assertCacheStats(firmCache, 3, 5, 2, 3);
        JsonAssert.assertJsonEquals(expectedJson, planExecutorHelper.executePlan());
        assertCacheStats(firmCache, 3, 10, 7, 3);
    }

    /**
     * A GraphFetch query which touches the same property across two services (one cross-store access)
     * Fetch2 was testing 2:1 multiplicity; this case tests 1:2 multiplicity
     */
    @Test
    public void XStoreServiceWithToManyCrossPropertyAccess() throws JavaCompileException
    {
        String query = "###Pure\n" +
                "function testfetch3::query(): Any[1]\n" +
                "{\n" +
                "  {|test::Address.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::Address {\n" +
                "        name,\n" +
                "        persons {\n" +
                "          fullName\n" +
                "        }\n" +
                "      }\n" +
                "    }#, 1)\n" +
                "    ->serialize(#{\n" +
                "      test::Address {\n" +
                "        name,\n" +
                "        persons {\n" +
                "          fullName\n" +
                "        }\n" +
                "      }\n" +
                "    }#)};\n" +
                "}";

        String expectedJson = jsonPrepend + "[" +
                "{\"name\":\"A1\",\"persons\":[{\"fullName\":\"P1\"},{\"fullName\":\"P5\"}]}," +
                "{\"name\":\"A2\",\"persons\":[{\"fullName\":\"P2\"}]}," +
                "{\"name\":\"A3\",\"persons\":[{\"fullName\":\"P4\"}]}," +
                "{\"name\":\"A4\",\"persons\":[]}," +
                "{\"name\":\"A5\",\"persons\":[]}" +
                "]}";

        SingleExecutionPlan fetchPlan = buildPlanForQuery(pureGrammar + "\n\n" + query, mapping, runtime);
        List<GraphFetchCrossAssociationKeys> gfxks = GraphFetchCrossAssociationKeys.graphFetchCrossAssociationKeysForPlan(fetchPlan);

        Assert.assertEquals(1, gfxks.size());
        Assert.assertEquals("<default, root.persons@person_set>", gfxks.get(0).getName());

        ExecutionCache<GraphFetchCacheKey, List<Object>> personCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        Map<GraphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>>> cacheConfig = Maps.mutable.of(gfxks.get(0), personCache);
        PlanExecutorHelper planExecutorHelper = new PlanExecutorHelper(fetchPlan, cacheConfig);

        JsonAssert.assertJsonEquals(expectedJson, planExecutorHelper.executePlan());
        assertCacheStats(personCache, 5, 5, 0, 5);
        JsonAssert.assertJsonEquals(expectedJson, planExecutorHelper.executePlan());
        assertCacheStats(personCache, 5, 10, 5, 5);
    }

    /**
     * A GraphFetch query which requires fields from three services
     */
    @Test
    public void XStoreServiceWithMultiCrossPropertyAccess() throws JavaCompileException
    {
        String query = "###Pure\n" +
                "function testfetch4::query(): Any[1]\n" +
                "{\n" +
                "  {|test::Person.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name\n" +
                "        },\n" +
                "        address {\n" +
                "          name\n" +
                "        }\n" +
                "      }\n" +
                "    }#, 1)\n" +
                "    ->serialize(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name\n" +
                "        },\n" +
                "        address {\n" +
                "          name\n" +
                "        }\n" +
                "      }\n" +
                "    }#)};\n" +
                "}";

        String expectedJson = jsonPrepend + "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\"},\"address\":{\"name\":\"A1\"}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\"},\"address\":{\"name\":\"A2\"}}," +
                "{\"fullName\":\"P3\",\"firm\":null,\"address\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null,\"address\":{\"name\":\"A3\"}}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\"},\"address\":{\"name\":\"A1\"}}" +
                "]}";

        SingleExecutionPlan fetchPlan = buildPlanForQuery(pureGrammar + "\n\n" + query, mapping, runtime);
        List<GraphFetchCrossAssociationKeys> gfxks = GraphFetchCrossAssociationKeys.graphFetchCrossAssociationKeysForPlan(fetchPlan);

        Assert.assertEquals(2, gfxks.size());
        Assert.assertEquals(Sets.mutable.of("<default, root.firm@firm_set>", "<default, root.address@address_set>"), gfxks.stream().map(GraphFetchCrossAssociationKeys::getName).collect(Collectors.toSet()));

        ExecutionCache<GraphFetchCacheKey, List<Object>> firmCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        ExecutionCache<GraphFetchCacheKey, List<Object>> addressCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        Map<GraphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>>> cacheConfig = Maps.mutable.of(
                gfxks.stream().filter(x -> "<default, root.firm@firm_set>".equals(x.getName())).findFirst().orElse(null), firmCache,
                gfxks.stream().filter(x -> "<default, root.address@address_set>".equals(x.getName())).findFirst().orElse(null), addressCache
        );
        PlanExecutorHelper planExecutorHelper = new PlanExecutorHelper(fetchPlan, cacheConfig);

        JsonAssert.assertJsonEquals(expectedJson, planExecutorHelper.executePlan());
        assertCacheStats(firmCache, 3, 5, 2, 3);
        assertCacheStats(addressCache, 4, 5, 1, 4);
        JsonAssert.assertJsonEquals(expectedJson, planExecutorHelper.executePlan());
        assertCacheStats(firmCache, 3, 10, 7, 3);
        assertCacheStats(addressCache, 4, 10, 6, 4);
    }

    /**
     * This test is currently Ignored because ServiceStore does not yet support shared caches.
     * Shared caches are not currently supported as the attemptAddingChildToParent in the generated java code
     * casts using the Class, leading to a type mismatch. Casting would need to use the Interface instead.
     * A GraphFetch query uses all 3 services and is nested 3 levels with shared cache
     */
    @Ignore
    @Test
    public void XStoreServiceWithDeepCrossPropertyAccessSharedCache() throws JavaCompileException
    {
        String query = "###Pure\n" +
                "function testfetch5::query(): Any[1]\n" +
                "{\n" +
                "  {|test::Person.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name,\n" +
                "          address {\n" +
                "            name\n" +
                "          }\n" +
                "        },\n" +
                "        address {\n" +
                "          name\n" +
                "        }\n" +
                "      }\n" +
                "    }#, 1)\n" +
                "    ->serialize(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name,\n" +
                "          address {\n" +
                "            name\n" +
                "          }\n" +
                "        },\n" +
                "        address {\n" +
                "          name\n" +
                "        }\n" +
                "      }\n" +
                "    }#)};\n" +
                "}";

        String expectedJson = jsonPrepend + "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\"}},\"address\":{\"name\":\"A1\"}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\",\"address\":{\"name\":\"A3\"}},\"address\":{\"name\":\"A2\"}}," +
                "{\"fullName\":\"P3\",\"firm\":null,\"address\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null,\"address\":{\"name\":\"A3\"}}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\"}},\"address\":{\"name\":\"A1\"}}" +
                "]}";

        SingleExecutionPlan fetchPlan = buildPlanForQuery(pureGrammar + "\n\n" + query, mapping, runtime);
        List<GraphFetchCrossAssociationKeys> gfxks = GraphFetchCrossAssociationKeys.graphFetchCrossAssociationKeysForPlan(fetchPlan);

        Assert.assertEquals(3, gfxks.size());
        Assert.assertEquals(Sets.mutable.of("<default, root.firm@firm_set>", "<default, root.address@address_set>", "<default, root.firm@firm_set.address@address_set>"), gfxks.stream().map(GraphFetchCrossAssociationKeys::getName).collect(Collectors.toSet()));

        ExecutionCache<GraphFetchCacheKey, List<Object>> firmCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        ExecutionCache<GraphFetchCacheKey, List<Object>> addressCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        Map<GraphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>>> cacheConfig = Maps.mutable.of(
                gfxks.stream().filter(x -> "<default, root.firm@firm_set>".equals(x.getName())).findFirst().orElse(null), firmCache,
                gfxks.stream().filter(x -> "<default, root.address@address_set>".equals(x.getName())).findFirst().orElse(null), addressCache,
                gfxks.stream().filter(x -> "<default, root.firm@firm_set.address@address_set>".equals(x.getName())).findFirst().orElse(null), addressCache
        );
        PlanExecutorHelper planExecutorHelper = new PlanExecutorHelper(fetchPlan, cacheConfig);

        JsonAssert.assertJsonEquals(expectedJson, planExecutorHelper.executePlan());
        assertCacheStats(firmCache, 3, 5, 2, 3);
        assertCacheStats(addressCache, 5, 7, 2, 5);
        JsonAssert.assertJsonEquals(expectedJson, planExecutorHelper.executePlan());
        assertCacheStats(firmCache, 3, 10, 7, 3);
        assertCacheStats(addressCache, 5, 12, 7, 5);
    }

    /**
     * A GraphFetch query uses all 3 services and is nested 3 levels without shared cache
     * @throws JavaCompileException if plan compilation error
     */
    @Test
    public void XStoreServiceWithDeepCrossPropertyAccessNoSharedCache() throws JavaCompileException
    {
        String query = "###Pure\n" +
                "function testfetch6::query(): Any[1]\n" +
                "{\n" +
                "  {|test::Person.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name,\n" +
                "          address {\n" +
                "            name\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }#, 1)\n" +
                "    ->serialize(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name,\n" +
                "          address {\n" +
                "            name\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }#)};\n" +
                "}";

        String expectedJson = jsonPrepend + "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\"}}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\",\"address\":{\"name\":\"A3\"}}}," +
                "{\"fullName\":\"P3\",\"firm\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\"}}}" +
                "]}";

        SingleExecutionPlan fetchPlan = buildPlanForQuery(pureGrammar + "\n\n" + query, mapping, runtime);
        List<GraphFetchCrossAssociationKeys> gfxks = GraphFetchCrossAssociationKeys.graphFetchCrossAssociationKeysForPlan(fetchPlan);

        Assert.assertEquals(2, gfxks.size());
        Assert.assertEquals(Sets.mutable.of("<default, root.firm@firm_set>", "<default, root.firm@firm_set.address@address_set>"), gfxks.stream().map(GraphFetchCrossAssociationKeys::getName).collect(Collectors.toSet()));

        ExecutionCache<GraphFetchCacheKey, List<Object>> firmCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        ExecutionCache<GraphFetchCacheKey, List<Object>> addressCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        Map<GraphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>>> cacheConfig = Maps.mutable.of(
                gfxks.stream().filter(x -> "<default, root.firm@firm_set>".equals(x.getName())).findFirst().orElse(null), firmCache,
                gfxks.stream().filter(x -> "<default, root.firm@firm_set.address@address_set>".equals(x.getName())).findFirst().orElse(null), addressCache
        );
        PlanExecutorHelper planExecutorHelper = new PlanExecutorHelper(fetchPlan, cacheConfig);

        JsonAssert.assertJsonEquals(expectedJson, planExecutorHelper.executePlan());
        assertCacheStats(firmCache, 3, 5, 2, 3);
        assertCacheStats(addressCache, 2, 2, 0, 2);
        JsonAssert.assertJsonEquals(expectedJson, planExecutorHelper.executePlan());
        assertCacheStats(firmCache, 3, 10, 7, 3);
        assertCacheStats(addressCache, 2, 2, 0, 2);
    }

    /**
     * A GraphFetch query uses all 3 services and is nested 3 levels without shared cache
     * This test tests source-level batching for large batch size
     * @throws JavaCompileException if plan compilation error
     */
    @Test
    public void XStoreServiceDeepCrossNoSharedCacheLargeSourceBatch() throws JavaCompileException
    {
        // large batch size with deep cross property
        String query = "###Pure\n" +
                "function testfetch6::query(): Any[1]\n" +
                "{\n" +
                "  {|test::Person.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name,\n" +
                "          address {\n" +
                "            name\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }#, 100)\n" +
                "    ->serialize(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name,\n" +
                "          address {\n" +
                "            name\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }#)};\n" +
                "}";

        String expectedJson = jsonPrepend + "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\"}}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\",\"address\":{\"name\":\"A3\"}}}," +
                "{\"fullName\":\"P3\",\"firm\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\"}}}" +
                "]}";

        SingleExecutionPlan fetchPlan = buildPlanForQuery(pureGrammar + "\n\n" + query, mapping, runtime);
        List<GraphFetchCrossAssociationKeys> gfxks = GraphFetchCrossAssociationKeys.graphFetchCrossAssociationKeysForPlan(fetchPlan);

        Assert.assertEquals(2, gfxks.size());
        Assert.assertEquals(Sets.mutable.of("<default, root.firm@firm_set>", "<default, root.firm@firm_set.address@address_set>"), gfxks.stream().map(GraphFetchCrossAssociationKeys::getName).collect(Collectors.toSet()));

        ExecutionCache<GraphFetchCacheKey, List<Object>> firmCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        ExecutionCache<GraphFetchCacheKey, List<Object>> addressCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        Map<GraphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>>> cacheConfig = Maps.mutable.of(
                gfxks.stream().filter(x -> "<default, root.firm@firm_set>".equals(x.getName())).findFirst().orElse(null), firmCache,
                gfxks.stream().filter(x -> "<default, root.firm@firm_set.address@address_set>".equals(x.getName())).findFirst().orElse(null), addressCache
        );
        PlanExecutorHelper planExecutorHelper = new PlanExecutorHelper(fetchPlan, cacheConfig);

        JsonAssert.assertJsonEquals(expectedJson, planExecutorHelper.executePlan());
        assertCacheStats(firmCache, 3, 3, 0, 3);
        assertCacheStats(addressCache, 2, 2, 0, 2);
        JsonAssert.assertJsonEquals(expectedJson, planExecutorHelper.executePlan());
        assertCacheStats(firmCache, 3, 6, 3, 3);
        assertCacheStats(addressCache, 2, 2, 0, 2);
    }

    /**
     * A GraphFetch query uses all 3 services without shared cache
     * This test tests source-level batching for small batch size >1
     * this would catch cases where cache is improperly set
     * (e.g. during first small batch cache is improperly set, would break during 2nd batch)
     * @throws JavaCompileException if plan compilation error
     */
    @Test
    public void XStoreServiceMultiCrossSmallSourceBatch() throws JavaCompileException
    {
        String query = "###Pure\n" +
                "function testfetch4::query(): Any[1]\n" +
                "{\n" +
                "  {|test::Person.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name\n" +
                "        },\n" +
                "        address {\n" +
                "          name\n" +
                "        }\n" +
                "      }\n" +
                "    }#, 3)\n" +
                "    ->serialize(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name\n" +
                "        },\n" +
                "        address {\n" +
                "          name\n" +
                "        }\n" +
                "      }\n" +
                "    }#)};\n" +
                "}";

        String expectedJson = jsonPrepend + "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\"},\"address\":{\"name\":\"A1\"}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\"},\"address\":{\"name\":\"A2\"}}," +
                "{\"fullName\":\"P3\",\"firm\":null,\"address\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null,\"address\":{\"name\":\"A3\"}}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\"},\"address\":{\"name\":\"A1\"}}" +
                "]}";

        SingleExecutionPlan fetchPlan = buildPlanForQuery(pureGrammar + "\n\n" + query, mapping, runtime);
        List<GraphFetchCrossAssociationKeys> gfxks = GraphFetchCrossAssociationKeys.graphFetchCrossAssociationKeysForPlan(fetchPlan);

        Assert.assertEquals(2, gfxks.size());
        Assert.assertEquals(Sets.mutable.of("<default, root.firm@firm_set>", "<default, root.address@address_set>"), gfxks.stream().map(GraphFetchCrossAssociationKeys::getName).collect(Collectors.toSet()));

        ExecutionCache<GraphFetchCacheKey, List<Object>> firmCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        ExecutionCache<GraphFetchCacheKey, List<Object>> addressCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        Map<GraphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>>> cacheConfig = Maps.mutable.of(
                gfxks.stream().filter(x -> "<default, root.firm@firm_set>".equals(x.getName())).findFirst().orElse(null), firmCache,
                gfxks.stream().filter(x -> "<default, root.address@address_set>".equals(x.getName())).findFirst().orElse(null), addressCache
        );
        PlanExecutorHelper planExecutorHelper = new PlanExecutorHelper(fetchPlan, cacheConfig);

        JsonAssert.assertJsonEquals(expectedJson, planExecutorHelper.executePlan());
        assertCacheStats(firmCache, 3, 5, 2, 3);
        assertCacheStats(addressCache, 4, 5, 1, 4);
        JsonAssert.assertJsonEquals(expectedJson, planExecutorHelper.executePlan());
        assertCacheStats(firmCache, 3, 10, 7, 3);
        assertCacheStats(addressCache, 4, 10, 6, 4);
    }

    /**
     * A GraphFetch query uses all 4 services and is nested 4 levels without shared cache
     * @throws JavaCompileException if plan compilation error
     */
    @Test
    public void XStoreServiceWithFourDeepXPropertyAccessNoSharedCache() throws JavaCompileException
    {
        String query = "###Pure\n" +
                "function testfetch6::query(): Any[1]\n" +
                "{\n" +
                "  {|test::Person.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name,\n" +
                "          address {\n" +
                "            name,\n" +
                "            street {\n" +
                "               streetId\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }#, 1)\n" +
                "    ->serialize(#{\n" +
                "      test::Person {\n" +
                "        fullName,\n" +
                "        firm {\n" +
                "          name,\n" +
                "          address {\n" +
                "            name,\n" +
                "            street {\n" +
                "               streetId\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }#)};\n" +
                "}";

        String expectedJson = jsonPrepend + "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\",\"street\":{\"streetId\":\"S2\"}}}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\",\"address\":{\"name\":\"A3\",\"street\":null}}}," +
                "{\"fullName\":\"P3\",\"firm\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\",\"street\":{\"streetId\":\"S2\"}}}}" +
                "]}";

        SingleExecutionPlan fetchPlan = buildPlanForQuery(pureGrammar + "\n\n" + query, mapping, runtime);
        List<GraphFetchCrossAssociationKeys> gfxks = GraphFetchCrossAssociationKeys.graphFetchCrossAssociationKeysForPlan(fetchPlan);

        Assert.assertEquals(3, gfxks.size());
        Assert.assertEquals(Sets.mutable.of("<default, root.firm@firm_set>", "<default, root.firm@firm_set.address@address_set>", "<default, root.firm@firm_set.address@address_set.street@street_set>"), gfxks.stream().map(GraphFetchCrossAssociationKeys::getName).collect(Collectors.toSet()));

        ExecutionCache<GraphFetchCacheKey, List<Object>> firmCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        ExecutionCache<GraphFetchCacheKey, List<Object>> addressCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        ExecutionCache<GraphFetchCacheKey, List<Object>> streetCache = ExecutionCacheBuilder.buildExecutionCacheFromGuavaCache(CacheBuilder.newBuilder().recordStats().build());
        Map<GraphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>>> cacheConfig = Maps.mutable.of(
                gfxks.stream().filter(x -> "<default, root.firm@firm_set>".equals(x.getName())).findFirst().orElse(null), firmCache,
                gfxks.stream().filter(x -> "<default, root.firm@firm_set.address@address_set>".equals(x.getName())).findFirst().orElse(null), addressCache,
                gfxks.stream().filter(x -> "<default, root.firm@firm_set.address@address_set.street@street_set>".equals(x.getName())).findFirst().orElse(null), streetCache
        );
        PlanExecutorHelper planExecutorHelper = new PlanExecutorHelper(fetchPlan, cacheConfig);

        JsonAssert.assertJsonEquals(expectedJson, planExecutorHelper.executePlan());
        assertCacheStats(firmCache, 3, 5, 2, 3);
        assertCacheStats(addressCache, 2, 2, 0, 2);
        assertCacheStats(streetCache, 2, 2, 0, 2);
        JsonAssert.assertJsonEquals(expectedJson, planExecutorHelper.executePlan());
        assertCacheStats(firmCache, 3, 10, 7, 3);
        assertCacheStats(addressCache, 2, 2, 0, 2);
        assertCacheStats(streetCache, 2, 2, 0, 2);
    }

    /**
     * Helper for executing the test plans via PlanExecutionContext
     * ServiceRunner normally takes care of creating the PlanExecutionContext in an end-to-end run
     * This set of tests is specific to the ServiceStore cross-store cache functionality
     * end-to-end tests already exist (tested in TestServiceRunner.java)
     * This helper is modeled after AbstractXStoreServiceRunner in TestServiceRunner.java
     * We do not directly import from the pure.dsl.service.execution package as that package has a dependency on relational
     * It is more efficient for the build for this package to depend directly on  legend.engine.plan.execution
     */
    private static class PlanExecutorHelper
    {
        SingleExecutionPlan plan;
        Identity identity;
        EngineJavaCompiler compiler;
        PlanExecutor executor;
        // Substitute for OperationalContext
        Map<GraphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>>> cacheConfig;
        Map<String, ?> params;


        private PlanExecutorHelper(SingleExecutionPlan plan, Map<GraphFetchCrossAssociationKeys, ExecutionCache<GraphFetchCacheKey, List<Object>>> cacheConfig) throws JavaCompileException
        {
            this.plan = plan;
            this.identity = Identity.getAnonymousIdentity();
            this.compiler = JavaHelper.compilePlan(plan, identity);
            // no params used by these tests
            this.params = Collections.emptyMap();
            this.executor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors(false);
            this.cacheConfig = cacheConfig;
        }

        /**
         * ultimately, should call this.run.newExecutionBuilder.executeToStream
         */
        public String executePlan()
        {
            try (JavaHelper.ThreadContextClassLoaderScope ignored = JavaHelper.withCurrentThreadContextClassLoader(compiler.getClassLoader()))
            {
                List<GraphFetchCache> graphFetchCaches = cacheConfig
                        .entrySet()
                        .stream()
                        .map(e -> ExecutionCacheBuilder.buildGraphFetchCacheByTargetCrossKeysFromExecutionCache(e.getValue(), e.getKey()))
                        .collect(Collectors.toList());

                PlanExecutionContext planExecutionContext = new PlanExecutionContext(graphFetchCaches);

                JsonStreamingResult result = (JsonStreamingResult) this.executor.execute(this.plan, this.params, null, planExecutionContext);
                return result.flush(new JsonStreamToJsonDefaultSerializer(result));
            }
        }
    }

    private static void assertCacheStats(ExecutionCache<?, ?> cache, int estimatedSize, int requestCount, int hitCount, int missCount)
    {
        Assert.assertEquals(estimatedSize, cache.estimatedSize());
        Assert.assertEquals(requestCount, cache.stats().requestCount());
        Assert.assertEquals(hitCount, cache.stats().hitCount());
        Assert.assertEquals(missCount, cache.stats().missCount());
    }
}