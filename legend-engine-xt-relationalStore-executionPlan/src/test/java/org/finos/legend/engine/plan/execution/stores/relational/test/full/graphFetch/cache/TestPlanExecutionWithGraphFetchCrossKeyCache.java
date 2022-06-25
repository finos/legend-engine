// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.full.graphFetch.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.io.input.ClassLoaderObjectInputStream;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.Compiler;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.HelperValueSpecificationBuilder;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.PureModel;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.plan.execution.PlanExecutionContext;
import org.finos.legend.engine.plan.execution.cache.ExecutionCache;
import org.finos.legend.engine.plan.execution.cache.ExecutionCacheBuilder;
import org.finos.legend.engine.plan.execution.cache.ExecutionCacheStats;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCache;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheByTargetCrossKeys;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheKey;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCrossAssociationKeys;
import org.finos.legend.engine.plan.execution.nodes.helpers.platform.JavaHelper;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamToPureFormatSerializer;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;
import org.finos.legend.engine.plan.execution.stores.relational.connection.AlloyTestServer;
import org.finos.legend.engine.plan.generation.PlanGenerator;
import org.finos.legend.engine.plan.generation.transformers.LegendPlanTransformers;
import org.finos.legend.engine.plan.platform.PlanPlatform;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.domain.Function;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.finos.legend.engine.shared.javaCompiler.EngineJavaCompiler;
import org.finos.legend.engine.shared.javaCompiler.JavaCompileException;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.finos.legend.pure.generated.core_relational_relational_extensions_extension.Root_meta_relational_extension_relationalExtensions__Extension_MANY_;

public class TestPlanExecutionWithGraphFetchCrossKeyCache extends AlloyTestServer
{

    private static final String LOGICAL_MODEL = "###Pure\n" +
            "Class test::Person\n" +
            "{\n" +
            "  fullName: String[1];\n" +
            "}\n" +
            "\n" +
            "Class test::Firm\n" +
            "{\n" +
            "  name: String[1];\n" +
            "}\n" +
            "\n" +
            "Class test::Address\n" +
            "{\n" +
            "  name: String[1];\n" +
            "}\n" +
            "\n" +
            "Association test::Person_Firm\n" +
            "{\n" +
            "  employees: test::Person[*];\n" +
            "  firm: test::Firm[0..1];\n" +
            "}\n" +
            "\n" +
            "Association test::Person_Address\n" +
            "{\n" +
            "  persons: test::Person[*];\n" +
            "  address: test::Address[0..1];  \n" +
            "}\n" +
            "\n" +
            "Association test::Firm_Address\n" +
            "{\n" +
            "  firms: test::Firm[*];\n" +
            "  address: test::Address[0..1];  \n" +
            "}\n\n\n";

    private static final String STORE_MODEL = "###Relational\n" +
            "Database test::DB1\n" +
            "(\n" +
            "  Table personTable (\n" +
            "    fullName VARCHAR(100) PRIMARY KEY,\n" +
            "    firmName VARCHAR(100),\n" +
            "    addressName VARCHAR(100)\n" +
            "  )\n" +
            ")\n" +
            "\n" +
            "###Relational\n" +
            "Database test::DB2\n" +
            "(\n" +
            "  Table firmTable (\n" +
            "    name VARCHAR(100) PRIMARY KEY,\n" +
            "    addressName VARCHAR(100)\n" +
            "  )\n" +
            ")\n" +
            "\n" +
            "###Relational\n" +
            "Database test::DB3\n" +
            "(\n" +
            "  Table addressTable (\n" +
            "    name VARCHAR(100) PRIMARY KEY\n" +
            "  )\n" +
            ")\n\n\n";

    private static final String MAPPING = "###Mapping\n" +
            "Mapping test::Map\n" +
            "(\n" +
            "  test::Person : Relational {\n" +
            "    +firmName : String[0..1] : [test::DB1]personTable.firmName,\n" +
            "    +addressName : String[0..1] : [test::DB1]personTable.addressName, \n" +
            "    fullName: [test::DB1]personTable.fullName\n" +
            "  }\n" +
            "\n" +
            "  test::Firm : Relational {\n" +
            "    +addressName : String[0..1] : [test::DB2]firmTable.addressName, \n" +
            "    name: [test::DB2]firmTable.name\n" +
            "  }\n" +
            "\n" +
            "  test::Address : Relational {\n" +
            "    name: [test::DB3]addressTable.name\n" +
            "  }\n" +
            "\n" +
            "  test::Person_Firm : XStore {\n" +
            "    employees[test_Firm, test_Person]: $this.name == $that.firmName,\n" +
            "    firm[test_Person, test_Firm]: $this.firmName == $that.name\n" +
            "  }\n" +
            "\n" +
            "  test::Person_Address : XStore {\n" +
            "    persons[test_Address, test_Person]: $this.name == $that.addressName,\n" +
            "    address[test_Person, test_Address]: $this.addressName == $that.name\n" +
            "  }\n" +
            "\n" +
            "  test::Firm_Address : XStore {\n" +
            "    firms[test_Address, test_Firm]: $this.name == $that.addressName,\n" +
            "    address[test_Firm, test_Address]: $this.addressName == $that.name\n" +
            "  }\n" +
            ")\n\n\n";

    private static final String RUNTIME = "###Runtime\n" +
            "Runtime test::Runtime\n" +
            "{\n" +
            "  mappings:\n" +
            "  [\n" +
            "    test::Map\n" +
            "  ];\n" +
            "  connections:\n" +
            "  [\n" +
            "    test::DB1:\n" +
            "    [\n" +
            "      c1: #{\n" +
            "        RelationalDatabaseConnection\n" +
            "        {\n" +
            "          type: H2;\n" +
            "          specification: LocalH2 {};\n" +
            "          auth: DefaultH2;\n" +
            "        }\n" +
            "      }#\n" +
            "    ],\n" +
            "    test::DB2:\n" +
            "    [\n" +
            "      c2: #{\n" +
            "        RelationalDatabaseConnection\n" +
            "        {\n" +
            "          type: H2;\n" +
            "          specification: LocalH2 {};\n" +
            "          auth: DefaultH2;\n" +
            "        }\n" +
            "      }#\n" +
            "    ],\n" +
            "    test::DB3:\n" +
            "    [\n" +
            "      c3: #{\n" +
            "        RelationalDatabaseConnection\n" +
            "        {\n" +
            "          type: H2;\n" +
            "          specification: LocalH2 {};\n" +
            "          auth: DefaultH2;\n" +
            "        }\n" +
            "      }#\n" +
            "    ]\n" +
            "  ];\n" +
            "}\n";


    @Test
    public void testCrossCacheWithNoPropertyAccess() throws JavaCompileException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): String[1]\n" +
                "{\n" +
                "  test::Person.all()\n" +
                "    ->graphFetch(#{\n" +
                "      test::Person {\n" +
                "        fullName\n" +
                "      }\n" +
                "    }#, 1)\n" +
                "    ->serialize(#{\n" +
                "      test::Person {\n" +
                "        fullName\n" +
                "      }\n" +
                "    }#)\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction);
        GraphFetchCacheByTargetCrossKeys firmCache = getFirmEmptyCache(plan);
        PlanExecutionContext context = new PlanExecutionContext(plan, firmCache);

        String expectedRes = "[" +
                "{\"fullName\":\"P1\"}," +
                "{\"fullName\":\"P2\"}," +
                "{\"fullName\":\"P3\"}," +
                "{\"fullName\":\"P4\"}," +
                "{\"fullName\":\"P5\"}" +
                "]";

        Assert.assertEquals(expectedRes, executePlan(plan, context));
        assertCacheStats(firmCache.getExecutionCache(), 0, 0, 0, 0);
    }

    @Test
    public void testSingleCrossPropertyCache() throws JavaCompileException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): String[1]\n" +
                "{\n" +
                "  test::Person.all()\n" +
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
                "    }#)\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction);
        GraphFetchCacheByTargetCrossKeys firmCache = getFirmEmptyCache(plan);
        PlanExecutionContext context = new PlanExecutionContext(plan, firmCache);

        String expectedRes = "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\"}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\"}}," +
                "{\"fullName\":\"P3\",\"firm\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\"}}" +
                "]";

        Assert.assertEquals(expectedRes, executePlan(plan, context));
        assertCacheStats(firmCache.getExecutionCache(), 3, 5, 2, 3);

        Assert.assertEquals(expectedRes, executePlan(plan, context));
        assertCacheStats(firmCache.getExecutionCache(), 3, 10, 7, 3);
    }

    @Test
    public void testToManyCrossPropertyCache() throws JavaCompileException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): String[1]\n" +
                "{\n" +
                "  test::Address.all()\n" +
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
                "    }#)\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction);
        GraphFetchCacheByTargetCrossKeys personCache = getPersonCache(plan);
        PlanExecutionContext context = new PlanExecutionContext(plan, personCache);

        String expectedRes = "[" +
                "{\"name\":\"A1\",\"persons\":[{\"fullName\":\"P1\"},{\"fullName\":\"P5\"}]}," +
                "{\"name\":\"A2\",\"persons\":[{\"fullName\":\"P2\"}]}," +
                "{\"name\":\"A3\",\"persons\":[{\"fullName\":\"P4\"}]}," +
                "{\"name\":\"A4\",\"persons\":[]}," +
                "{\"name\":\"A5\",\"persons\":[]}" +
                "]";

        Assert.assertEquals(expectedRes, executePlan(plan, context));
        assertCacheStats(personCache.getExecutionCache(), 5, 5, 0, 5);

        Assert.assertEquals(expectedRes, executePlan(plan, context));
        assertCacheStats(personCache.getExecutionCache(), 5, 10, 5, 5);
    }

    @Test
    public void testMultiCrossPropertyCaches() throws JavaCompileException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): String[1]\n" +
                "{\n" +
                "  test::Person.all()\n" +
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
                "    }#)\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction);
        GraphFetchCacheByTargetCrossKeys firmCache = getFirmEmptyCache(plan);
        GraphFetchCacheByTargetCrossKeys addressCache = getAddressCache(plan);
        PlanExecutionContext context = new PlanExecutionContext(plan, firmCache, addressCache);

        String expectedRes = "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\"},\"address\":{\"name\":\"A1\"}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\"},\"address\":{\"name\":\"A2\"}}," +
                "{\"fullName\":\"P3\",\"firm\":null,\"address\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null,\"address\":{\"name\":\"A3\"}}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\"},\"address\":{\"name\":\"A1\"}}" +
                "]";

        Assert.assertEquals(expectedRes, executePlan(plan, context));
        assertCacheStats(firmCache.getExecutionCache(), 3, 5, 2, 3);
        assertCacheStats(addressCache.getExecutionCache(), 4, 5, 1, 4);

        Assert.assertEquals(expectedRes, executePlan(plan, context));
        assertCacheStats(firmCache.getExecutionCache(), 3, 10, 7, 3);
        assertCacheStats(addressCache.getExecutionCache(), 4, 10, 6, 4);
    }

    @Test
    public void testDeepCrossPropertyCachesShared() throws JavaCompileException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): String[1]\n" +
                "{\n" +
                "  test::Person.all()\n" +
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
                "    }#)\n" +
                "}";

        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction);
        GraphFetchCacheByTargetCrossKeys firmCache = getFirmEmptyCache(plan);
        List<GraphFetchCacheByTargetCrossKeys> addressCaches = getSharedAddressCaches(plan);
        PlanExecutionContext context = new PlanExecutionContext(plan, Lists.mutable.of((GraphFetchCache) firmCache).withAll(addressCaches));

        String expectedRes = "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\"}},\"address\":{\"name\":\"A1\"}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\",\"address\":{\"name\":\"A3\"}},\"address\":{\"name\":\"A2\"}}," +
                "{\"fullName\":\"P3\",\"firm\":null,\"address\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null,\"address\":{\"name\":\"A3\"}}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\"}},\"address\":{\"name\":\"A1\"}}" +
                "]";

        Assert.assertEquals(expectedRes, executePlan(plan, context));
        assertCacheStats(firmCache.getExecutionCache(), 3, 5, 2, 3);
        assertCacheStats(addressCaches.get(0).getExecutionCache(), 5, 7, 2, 5);

        Assert.assertEquals(expectedRes, executePlan(plan, context));
        assertCacheStats(firmCache.getExecutionCache(), 3, 10, 7, 3);
        assertCacheStats(addressCaches.get(0).getExecutionCache(), 5, 12, 7, 5);
    }

    @Test
    public void testCrossPropertyCachingWithSerializableCache() throws JavaCompileException, IOException, ClassNotFoundException
    {
        String fetchFunction = "###Pure\n" +
                "function test::fetch(): String[1]\n" +
                "{\n" +
                "  test::Person.all()\n" +
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
                "    }#)\n" +
                "}";

        String expectedRes = "[" +
                "{\"fullName\":\"P1\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\"}}}," +
                "{\"fullName\":\"P2\",\"firm\":{\"name\":\"F2\",\"address\":{\"name\":\"A3\"}}}," +
                "{\"fullName\":\"P3\",\"firm\":null}," +
                "{\"fullName\":\"P4\",\"firm\":null}," +
                "{\"fullName\":\"P5\",\"firm\":{\"name\":\"F1\",\"address\":{\"name\":\"A4\"}}}" +
                "]";

        SingleExecutionPlan plan = buildPlanForFetchFunction(fetchFunction);
        EngineJavaCompiler compiler = JavaHelper.compilePlan(plan, null);
        GraphFetchCrossAssociationKeys graphFetchCrossAssociationKeys = GraphFetchCrossAssociationKeys.graphFetchCrossAssociationKeysForPlan(plan).stream().filter(x -> x.getName().equals("<default, root.firm>")).findFirst().orElse(null);

        try (JavaHelper.ThreadContextClassLoaderScope ignored = JavaHelper.withCurrentThreadContextClassLoader(compiler.getClassLoader()))
        {
            Map<GraphFetchCacheKey, List<Object>> firmMap1 = Maps.mutable.empty();
            GraphFetchCacheByTargetCrossKeys firmCache1 = ExecutionCacheBuilder.buildGraphFetchCacheByTargetCrossKeysFromExecutionCache(buildExecutionCacheFromMap(firmMap1), graphFetchCrossAssociationKeys);
            Assert.assertEquals(expectedRes, executePlan(plan, new PlanExecutionContext(firmCache1)));
            assertCacheStats(firmCache1.getExecutionCache(), 3, 5, 2, 3);

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(firmMap1);
            objectOutputStream.flush();

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
            ClassLoaderObjectInputStream objectInputStream = new ClassLoaderObjectInputStream(compiler.getClassLoader(), byteArrayInputStream);
            Object o = objectInputStream.readObject();

            @SuppressWarnings("unchecked") Map<GraphFetchCacheKey, List<Object>> firmMap2 = (Map<GraphFetchCacheKey, List<Object>>) o;
            GraphFetchCacheByTargetCrossKeys firmCache2 = ExecutionCacheBuilder.buildGraphFetchCacheByTargetCrossKeysFromExecutionCache(buildExecutionCacheFromMap(firmMap2), graphFetchCrossAssociationKeys);
            Assert.assertEquals(expectedRes, executePlan(plan, new PlanExecutionContext(firmCache2)));
            assertCacheStats(firmCache2.getExecutionCache(), 3, 5, 5, 0);
        }
    }

    private GraphFetchCacheByTargetCrossKeys getFirmEmptyCache(SingleExecutionPlan plan)
    {
        return ExecutionCacheBuilder.buildGraphFetchCacheByTargetCrossKeysFromGuavaCache(
                CacheBuilder.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build(),
                GraphFetchCrossAssociationKeys.graphFetchCrossAssociationKeysForPlan(plan).stream().filter(x -> x.getName().equals("<default, root.firm>")).findFirst().orElse(null)
        );
    }

    private GraphFetchCacheByTargetCrossKeys getPersonCache(SingleExecutionPlan plan)
    {
        return ExecutionCacheBuilder.buildGraphFetchCacheByTargetCrossKeysFromGuavaCache(
                CacheBuilder.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build(),
                Objects.requireNonNull(GraphFetchCrossAssociationKeys.graphFetchCrossAssociationKeysForPlan(plan).stream().filter(x -> x.getName().equals("<default, root.persons>")).findFirst().orElse(null))
        );
    }

    private GraphFetchCacheByTargetCrossKeys getAddressCache(SingleExecutionPlan plan)
    {
        return ExecutionCacheBuilder.buildGraphFetchCacheByTargetCrossKeysFromGuavaCache(
                CacheBuilder.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build(),
                Objects.requireNonNull(GraphFetchCrossAssociationKeys.graphFetchCrossAssociationKeysForPlan(plan).stream().filter(x -> x.getName().equals("<default, root.address>")).findFirst().orElse(null))
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

    private String executePlan(SingleExecutionPlan plan, PlanExecutionContext context)
    {
        JsonStreamingResult result = (JsonStreamingResult) planExecutor.execute(plan, Collections.emptyMap(), null, context);
        return result.flush(new JsonStreamToPureFormatSerializer(result));
    }

    private void assertCacheStats(ExecutionCache<?, ?> cache, int estimatedSize, int requestCount, int hitCount, int missCount)
    {
        Assert.assertEquals(estimatedSize, cache.estimatedSize());
        Assert.assertEquals(requestCount, cache.stats().requestCount());
        Assert.assertEquals(hitCount, cache.stats().hitCount());
        Assert.assertEquals(missCount, cache.stats().missCount());
    }

    private SingleExecutionPlan buildPlanForFetchFunction(String fetchFunction)
    {
        PureModelContextData contextData = PureGrammarParser.newInstance().parseModel(LOGICAL_MODEL + STORE_MODEL + MAPPING + RUNTIME + fetchFunction);
        PureModel pureModel = Compiler.compile(contextData, null, null);

        List<ValueSpecification> fetchFunctionExpressions = contextData.getElementsOfType(Function.class).get(0).body;

        return PlanGenerator.generateExecutionPlan(
                HelperValueSpecificationBuilder.buildLambda(fetchFunctionExpressions, Collections.emptyList(), new CompileContext.Builder(pureModel).build()),
                pureModel.getMapping("test::Map"),
                pureModel.getRuntime("test::Runtime"),
                null,
                pureModel,
                "vX_X_X",
                PlanPlatform.JAVA,
                null,
                Root_meta_relational_extension_relationalExtensions__Extension_MANY_(pureModel.getExecutionSupport()),
                LegendPlanTransformers.transformers
        );
    }

    @Override
    protected void insertTestData(Statement s) throws SQLException
    {
        s.execute("Create Schema default;");
        s.execute("Drop table if exists personTable;");
        s.execute("Create Table personTable(fullName VARCHAR(100) NOT NULL,firmName VARCHAR(100) NULL,addressName VARCHAR(100) NULL, PRIMARY KEY(fullName));");
        s.execute("Drop table if exists firmTable;");
        s.execute("Create Table firmTable(name VARCHAR(100) NOT NULL,addressName VARCHAR(100) NULL, PRIMARY KEY(name));");
        s.execute("Drop table if exists addressTable;");
        s.execute("Create Table addressTable(name VARCHAR(100) NOT NULL, PRIMARY KEY(name));");
        s.execute("insert into personTable (fullName,firmName,addressName) values ('P1','F1','A1');");
        s.execute("insert into personTable (fullName,firmName,addressName) values ('P2','F2','A2');");
        s.execute("insert into personTable (fullName,firmName,addressName) values ('P3',null,null);");
        s.execute("insert into personTable (fullName,firmName,addressName) values ('P4',null,'A3');");
        s.execute("insert into personTable (fullName,firmName,addressName) values ('P5','F1','A1');");
        s.execute("insert into firmTable (name,addressName) values ('F1','A4');");
        s.execute("insert into firmTable (name,addressName) values ('F2','A3');");
        s.execute("insert into firmTable (name,addressName) values ('F3','A3');");
        s.execute("insert into firmTable (name,addressName) values ('F4',null);");
        s.execute("insert into addressTable (name) values ('A1');");
        s.execute("insert into addressTable (name) values ('A2');");
        s.execute("insert into addressTable (name) values ('A3');");
        s.execute("insert into addressTable (name) values ('A4');");
        s.execute("insert into addressTable (name) values ('A5');");
    }

    private <K, V> ExecutionCache<K, V> buildExecutionCacheFromMap(Map<K, V> map)
    {
        Map<String, Integer> stats = Maps.mutable.of("requestCount", 0, "hitCount", 0, "missCount", 0, "size", 0);

        return new ExecutionCache<K, V>()
        {
            @Override
            public V get(K key, Callable<? extends V> valueLoader)
            {
                throw new UnsupportedOperationException("Not supported!");
            }

            @Override
            public V getIfPresent(K key)
            {
                V v = map.get(key);
                stats.put("requestCount", stats.get("requestCount") + 1);
                if (v != null)
                {
                    stats.put("hitCount", stats.get("hitCount") + 1);
                }
                else
                {
                    stats.put("missCount", stats.get("missCount") + 1);
                }
                return v;
            }

            @Override
            public Map<? extends K, ? extends V> getAllPresent(Iterable<? extends K> keys)
            {
                throw new UnsupportedOperationException("Not supported!");
            }

            @Override
            public void put(K key, V value)
            {
                map.put(key, value);
                stats.put("size", map.size());
            }

            @Override
            public void putAll(Map<? extends K, ? extends V> keyValues)
            {
                map.putAll(keyValues);
                stats.put("size", map.size());
            }

            @Override
            public void invalidate(K key)
            {
                throw new UnsupportedOperationException("Not supported!");
            }

            @Override
            public void invalidateAll(Iterable<? extends K> keys)
            {
                throw new UnsupportedOperationException("Not supported!");
            }

            @Override
            public void invalidateAll()
            {
                throw new UnsupportedOperationException("Not supported!");
            }

            @Override
            public long estimatedSize()
            {
                return map.size();
            }

            @Override
            public ExecutionCacheStats stats()
            {
                return new ExecutionCacheStats()
                {
                    @Override
                    public long requestCount()
                    {
                        return stats.get("requestCount");
                    }

                    @Override
                    public long hitCount()
                    {
                        return stats.get("hitCount");
                    }

                    @Override
                    public long missCount()
                    {
                        return stats.get("missCount");
                    }

                    @Override
                    public long loadCount()
                    {
                        throw new UnsupportedOperationException("Not supported!");
                    }

                    @Override
                    public long loadSuccessCount()
                    {
                        throw new UnsupportedOperationException("Not supported!");
                    }

                    @Override
                    public long loadFailureCount()
                    {
                        throw new UnsupportedOperationException("Not supported!");
                    }

                    @Override
                    public long evictionCount()
                    {
                        throw new UnsupportedOperationException("Not supported!");
                    }

                    @Override
                    public double hitRate()
                    {
                        return hitCount() / (requestCount() * 1.0);
                    }

                    @Override
                    public double missRate()
                    {
                        return missCount() / (requestCount() * 1.0);
                    }

                    @Override
                    public double loadFailureRate()
                    {
                        throw new UnsupportedOperationException("Not supported!");
                    }

                    @Override
                    public double averageLoadPenalty()
                    {
                        throw new UnsupportedOperationException("Not supported!");
                    }

                    @Override
                    public long totalLoadTime()
                    {
                        throw new UnsupportedOperationException("Not supported!");
                    }
                };
            }
        };
    }
}
