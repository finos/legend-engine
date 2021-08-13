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

import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.Relational;
import org.finos.legend.engine.plan.execution.PlanExecutionContext;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.cache.ExecutionCacheBuilder;
import org.finos.legend.engine.plan.execution.cache.graphFetch.GraphFetchCacheByEqualityKeys;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamToJsonDefaultSerializer;
import org.finos.legend.engine.plan.execution.result.json.JsonStreamingResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.SingleExecutionPlan;
import org.finos.legend.engine.shared.core.ObjectMapperFactory;
import org.finos.legend.engine.shared.javaCompiler.JavaCompileException;
import org.h2.tools.Server;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static org.finos.legend.engine.plan.execution.stores.relational.TestExecutionScope.buildTestExecutor;

public class TestPlanExecutionWithGraphFetchEqualityCache
{
    private static final ObjectMapper objectMapper = ObjectMapperFactory.getNewStandardObjectMapperWithPureProtocolExtensionSupports();
    private final int port = DynamicPortGenerator.generatePort();
    private Server tcpServer;
    private PlanExecutor planExecutor;

    @Before
    public void setUp()
    {
        try
        {
            Class.forName("org.h2.Driver");
            Enumeration<Driver> e = DriverManager.getDrivers();
            MutableList<Driver> found = Lists.mutable.empty();
            while (e.hasMoreElements())
            {
                Driver d = e.nextElement();
                if (!d.getClass().getName().equals("org.h2.Driver"))
                {
                    found.add(d);
                }
            }

            found.forEach((Procedure<Driver>) c -> {
                try
                {
                    DriverManager.deregisterDriver(c);
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            });

            tcpServer = AlloyH2Server.startServer(port);
            this.insertData();

            planExecutor = PlanExecutor.newPlanExecutor(Relational.build(this.port));
        }
        catch (Exception e)
        {
        }
    }

    @After
    public void teardown()
    {
        if (tcpServer.isRunning(true))
        {
            tcpServer.stop();
        }
    }

    private static SingleExecutionPlan readPlan(String resourcePath) throws IOException
    {
        return objectMapper.readValue(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource(resourcePath)), SingleExecutionPlan.class);
    }

    @Test
    public void testSimpleCacheWithNoDepth() throws IOException, JavaCompileException
    {
        SingleExecutionPlan plan = readPlan("org/finos/legend/engine/plan/execution/stores/relational/test/cache/graphFetch//equalityCachePlanWithNoDepth.json");
        Map<String, ?> params = org.eclipse.collections.api.factory.Maps.mutable.of("count", 1);

        GraphFetchCacheByEqualityKeys personCache = ExecutionCacheBuilder.buildGraphFetchCacheByEqualityKeysFromGuavaCache(
                CacheBuilder.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build(),
                "meta::relational::tests::simpleRelationalMappingInc",
                "meta_pure_tests_model_simple_Person"
        );

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"firstName\":\"Anthony\",\"lastName\":\"Allen\"}}";
        String expectedSubTree = "{firstName,lastName}";

        assertCachingForAllObjects(plan, params, personCache, expectedRes, expectedSubTree, 1);
    }

    @Test
    public void testTimeToLive() throws IOException, JavaCompileException, InterruptedException
    {
        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"firstName\":\"Anthony\",\"lastName\":\"Allen\"}}";

        SingleExecutionPlan plan = readPlan("org/finos/legend/engine/plan/execution/stores/relational/test/cache/graphFetch//equalityCachePlanWithNoDepth.json");

        GraphFetchCacheByEqualityKeys personCache = ExecutionCacheBuilder.buildGraphFetchCacheByEqualityKeysFromGuavaCache(
                CacheBuilder.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.SECONDS).build(),
                "meta::relational::tests::simpleRelationalMappingInc",
                "meta_pure_tests_model_simple_Person"
        );

        PlanExecutionContext planExecutionContext = new PlanExecutionContext(plan, personCache);
        Map<String, ?> params = org.eclipse.collections.api.factory.Maps.mutable.of("count", 1);

        JsonStreamingResult result = (JsonStreamingResult) planExecutor.execute(plan, params, null, planExecutionContext);
        String res = result.flush(new JsonStreamToJsonDefaultSerializer(result));
        Assert.assertEquals(expectedRes, res);

        result = (JsonStreamingResult) planExecutor.execute(plan, params, null, planExecutionContext);
        res = result.flush(new JsonStreamToJsonDefaultSerializer(result));
        Assert.assertEquals(expectedRes, res);

        Assert.assertEquals(2, personCache.getExecutionCache().stats().requestCount());
        Assert.assertEquals(1, personCache.getExecutionCache().stats().hitCount());
        Assert.assertEquals(1, personCache.getExecutionCache().stats().missCount());

        Thread.sleep(11000);

        result = (JsonStreamingResult) planExecutor.execute(plan, params, null, planExecutionContext);
        res = result.flush(new JsonStreamToJsonDefaultSerializer(result));
        Assert.assertEquals(expectedRes, res);

        Assert.assertEquals(3, personCache.getExecutionCache().stats().requestCount());
        Assert.assertEquals(1, personCache.getExecutionCache().stats().hitCount());
        Assert.assertEquals(2, personCache.getExecutionCache().stats().missCount());

        result = (JsonStreamingResult) planExecutor.execute(plan, params, null, planExecutionContext);
        res = result.flush(new JsonStreamToJsonDefaultSerializer(result));
        Assert.assertEquals(expectedRes, res);

        Assert.assertEquals(4, personCache.getExecutionCache().stats().requestCount());
        Assert.assertEquals(2, personCache.getExecutionCache().stats().hitCount());
        Assert.assertEquals(2, personCache.getExecutionCache().stats().missCount());
    }

    @Test
    public void testSimpleCacheWithToOneComplexProperties() throws IOException, JavaCompileException
    {
        SingleExecutionPlan plan = readPlan("org/finos/legend/engine/plan/execution/stores/relational/test/cache/graphFetch//equalityCachePlanWithToOneComplexProperties.json");
        Map<String, ?> params = org.eclipse.collections.api.factory.Maps.mutable.of("count", 1);

        GraphFetchCacheByEqualityKeys personCache = ExecutionCacheBuilder.buildGraphFetchCacheByEqualityKeysFromGuavaCache(
                CacheBuilder.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build(),
                "meta::relational::tests::simpleRelationalMappingInc",
                "meta_pure_tests_model_simple_Person"
        );

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"firstName\":\"Anthony\",\"lastName\":\"Allen\",\"firm\":{\"legalName\":\"FirmA\",\"address\":{\"name\":\"New York\"}}}}";
        String expectedSubTree = "{firstName,lastName,firm{legalName,address{name}}}";

        assertCachingForAllObjects(plan, params, personCache, expectedRes, expectedSubTree, 1);
    }

    @Test
    public void testSimpleCacheWithToOneComplexPropertiesManyObjects() throws IOException, JavaCompileException
    {
        SingleExecutionPlan plan = readPlan("org/finos/legend/engine/plan/execution/stores/relational/test/cache/graphFetch//equalityCachePlanWithToOneComplexProperties.json");
        Map<String, ?> params = org.eclipse.collections.api.factory.Maps.mutable.of("count", 100);

        GraphFetchCacheByEqualityKeys personCache = ExecutionCacheBuilder.buildGraphFetchCacheByEqualityKeysFromGuavaCache(
                CacheBuilder.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build(),
                "meta::relational::tests::simpleRelationalMappingInc",
                "meta_pure_tests_model_simple_Person"
        );

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":[{\"firstName\":\"Anthony\",\"lastName\":\"Allen\",\"firm\":{\"legalName\":\"FirmA\",\"address\":{\"name\":\"New York\"}}},{\"firstName\":\"Olivier\",\"lastName\":\"Doe\",\"firm\":{\"legalName\":\"FirmC\",\"address\":{\"name\":\"Tokyo\"}}},{\"firstName\":\"David\",\"lastName\":\"Harris\",\"firm\":{\"legalName\":\"FirmD\",\"address\":{\"name\":\"Mountain View\"}}},{\"firstName\":\"John\",\"lastName\":\"Hill\",\"firm\":{\"legalName\":\"FirmA\",\"address\":{\"name\":\"New York\"}}},{\"firstName\":\"John\",\"lastName\":\"Johnson\",\"firm\":{\"legalName\":\"FirmA\",\"address\":{\"name\":\"New York\"}}},{\"firstName\":\"Fabrice\",\"lastName\":\"Roberts\",\"firm\":{\"legalName\":\"FirmB\",\"address\":{\"name\":\"Cupertino\"}}},{\"firstName\":\"Peter\",\"lastName\":\"Smith\",\"firm\":{\"legalName\":\"FirmA\",\"address\":{\"name\":\"New York\"}}}]}";
        String expectedSubTree = "{firstName,lastName,firm{legalName,address{name}}}";

        assertCachingForAllObjects(plan, params, personCache, expectedRes, expectedSubTree, 7);
    }

    @Test
    public void testSimpleCacheWithToManyComplexProperties() throws IOException, JavaCompileException
    {
        SingleExecutionPlan plan = readPlan("org/finos/legend/engine/plan/execution/stores/relational/test/cache/graphFetch//equalityCachePlanWithToManyComplexProperties.json");
        Map<String, ?> params = Collections.emptyMap();

        GraphFetchCacheByEqualityKeys firmCache = ExecutionCacheBuilder.buildGraphFetchCacheByEqualityKeysFromGuavaCache(
                CacheBuilder.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build(),
                "meta::relational::tests::simpleRelationalMappingInc",
                "meta_pure_tests_model_simple_Firm"
        );

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"legalName\":\"FirmA\",\"employees\":[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"},{\"firstName\":\"John\",\"lastName\":\"Hill\"},{\"firstName\":\"Anthony\",\"lastName\":\"Allen\"}]}}";
        String expectedSubTree = "{legalName,employees{firstName,lastName}}";

        assertCachingForAllObjects(plan, params, firmCache, expectedRes, expectedSubTree, 1);
    }

    @Test
    public void testUnUtilizedCache() throws IOException, JavaCompileException
    {
        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"legalName\":\"FirmA\",\"employees\":[{\"firstName\":\"Peter\",\"lastName\":\"Smith\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\"},{\"firstName\":\"John\",\"lastName\":\"Hill\"},{\"firstName\":\"Anthony\",\"lastName\":\"Allen\"}]}}";

        SingleExecutionPlan plan = readPlan("org/finos/legend/engine/plan/execution/stores/relational/test/cache/graphFetch//equalityCachePlanWithToManyComplexProperties.json");

        GraphFetchCacheByEqualityKeys personCache = ExecutionCacheBuilder.buildGraphFetchCacheByEqualityKeysFromGuavaCache(
                CacheBuilder.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build(),
                "meta::relational::tests::simpleRelationalMappingInc",
                "meta_pure_tests_model_simple_Person"
        );

        PlanExecutionContext planExecutionContext = new PlanExecutionContext(plan, personCache);

        JsonStreamingResult result = (JsonStreamingResult) planExecutor.execute(plan, Collections.emptyMap(), null, planExecutionContext);
        String res = result.flush(new JsonStreamToJsonDefaultSerializer(result));
        Assert.assertEquals(expectedRes, res);

        Assert.assertFalse(personCache.isCacheUtilized());
        Assert.assertEquals(0, personCache.getExecutionCache().estimatedSize());
        Assert.assertEquals(0, personCache.getExecutionCache().stats().requestCount());
        Assert.assertEquals(0, personCache.getExecutionCache().stats().hitCount());
        Assert.assertEquals(0, personCache.getExecutionCache().stats().missCount());
    }

    @Test
    public void testSimpleCacheWithPrimitiveQualifiers() throws IOException, JavaCompileException
    {
        SingleExecutionPlan plan = readPlan("org/finos/legend/engine/plan/execution/stores/relational/test/cache/graphFetch//equalityCachePlanWithPrimitiveQualifiers.json");
        Map<String, ?> params = Collections.emptyMap();

        GraphFetchCacheByEqualityKeys firmCache = ExecutionCacheBuilder.buildGraphFetchCacheByEqualityKeysFromGuavaCache(
                CacheBuilder.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build(),
                "meta::relational::tests::simpleRelationalMappingInc",
                "meta_pure_tests_model_simple_Firm"
        );

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"legalName\":\"FirmA\",\"averageEmployeesAge()\":39.5,\"employees\":[{\"firstName\":\"Peter\",\"lastName\":\"Smith\",\"fullName(true)\":\"Smith, Peter\"},{\"firstName\":\"John\",\"lastName\":\"Johnson\",\"fullName(true)\":\"Johnson, John\"},{\"firstName\":\"John\",\"lastName\":\"Hill\",\"fullName(true)\":\"Hill, John\"},{\"firstName\":\"Anthony\",\"lastName\":\"Allen\",\"fullName(true)\":\"Allen, Anthony\"}]}}";
        String expectedSubTree = "{legalName,averageEmployeesAge,employees{firstName,lastName,fullName([true])}}";

        assertCachingForAllObjects(plan, params, firmCache, expectedRes, expectedSubTree, 1);
    }

    @Test
    public void testSimpleCacheWithComplexQualifiers() throws IOException, JavaCompileException
    {
        SingleExecutionPlan plan = readPlan("org/finos/legend/engine/plan/execution/stores/relational/test/cache/graphFetch/equalityCachePlanWithComplexQualifiers.json");
        Map<String, ?> params = Collections.emptyMap();

        GraphFetchCacheByEqualityKeys firmCache = ExecutionCacheBuilder.buildGraphFetchCacheByEqualityKeysFromGuavaCache(
                CacheBuilder.newBuilder().recordStats().expireAfterWrite(10, TimeUnit.MINUTES).build(),
                "meta::relational::tests::simpleRelationalMappingInc",
                "meta_pure_tests_model_simple_Firm"
        );

        String expectedRes = "{\"builder\":{\"_type\":\"json\"},\"values\":{\"legalName\":\"FirmA\",\"employeeByLastName('Smith')\":{\"firstName\":\"Peter\",\"lastName\":\"Smith\",\"address\":{\"name\":\"Hoboken\"}},\"employeesByCityOrManager('New York', '')\":[{\"firstName\":\"John\",\"lastName\":\"Johnson\",\"address\":{\"name\":\"New York\"}},{\"firstName\":\"John\",\"lastName\":\"Hill\",\"address\":{\"name\":\"New York\"}},{\"firstName\":\"Anthony\",\"lastName\":\"Allen\",\"address\":{\"name\":\"New York\"}}]}}";
        String expectedSubTree = "{legalName,employeeByLastName(['Smith']){firstName,lastName,address{name}},employeesByCityOrManager(['New York'],['']){firstName,lastName,address{name}}}";

        assertCachingForAllObjects(plan, params, firmCache, expectedRes, expectedSubTree, 1);
    }

    private void assertCachingForAllObjects(
            SingleExecutionPlan plan, Map<String, ?> params, GraphFetchCacheByEqualityKeys cache,
            String expectedRes, String expectedSubTree, int objectCount
    ) throws JavaCompileException
    {
        PlanExecutionContext planExecutionContext = new PlanExecutionContext(plan, cache);

        JsonStreamingResult result = (JsonStreamingResult) planExecutor.execute(plan, params, null, planExecutionContext);
        String res = result.flush(new JsonStreamToJsonDefaultSerializer(result));
        Assert.assertEquals(expectedRes, res);

        Assert.assertTrue(cache.isCacheUtilized());
        Assert.assertEquals(expectedSubTree, cache.getSubTree());
        Assert.assertEquals(objectCount, cache.getExecutionCache().estimatedSize());
        Assert.assertEquals(objectCount, cache.getExecutionCache().stats().requestCount());
        Assert.assertEquals(0, cache.getExecutionCache().stats().hitCount());
        Assert.assertEquals(objectCount, cache.getExecutionCache().stats().missCount());

        result = (JsonStreamingResult) planExecutor.execute(plan, params, null, planExecutionContext);
        res = result.flush(new JsonStreamToJsonDefaultSerializer(result));
        Assert.assertEquals(expectedRes, res);

        Assert.assertEquals(objectCount, cache.getExecutionCache().estimatedSize());
        Assert.assertEquals(2 * objectCount, cache.getExecutionCache().stats().requestCount());
        Assert.assertEquals(objectCount, cache.getExecutionCache().stats().hitCount());
        Assert.assertEquals(objectCount, cache.getExecutionCache().stats().missCount());

        for (int i = 0; i < 100; ++i)
        {
            result = (JsonStreamingResult) planExecutor.execute(plan, params, null, planExecutionContext);
            res = result.flush(new JsonStreamToJsonDefaultSerializer(result));
            Assert.assertEquals(expectedRes, res);
        }

        Assert.assertEquals(objectCount, cache.getExecutionCache().estimatedSize());
        Assert.assertEquals(102 * objectCount, cache.getExecutionCache().stats().requestCount());
        Assert.assertEquals(101 * objectCount, cache.getExecutionCache().stats().hitCount());
        Assert.assertEquals(objectCount, cache.getExecutionCache().stats().missCount());
    }

    private void insertData()
    {
        Connection c = null;
        Statement s = null;
        try
        {
            c = buildTestExecutor(port).getConnectionManager().getTestDatabaseConnection();
            s = c.createStatement();

            s.execute("Drop table if exists PersonNameParameter;");
            s.execute("Create Table PersonNameParameter(id INT, lastNameFirst VARCHAR(200), title VARCHAR(200));");
            s.execute("insert into PersonNameParameter (id, lastNameFirst, title) values (1, true, 'eee');");
            s.execute("Drop table if exists PersonTable;");
            s.execute("Create Table PersonTable(id INT, firstName VARCHAR(200), lastName VARCHAR(200), age INT, addressId INT, firmId INT, managerId INT);");
            s.execute("insert into PersonTable (id, firstName, lastName, age, addressId, firmId, managerId) values (1, 'Peter', 'Smith',23, 1,1,2);");
            s.execute("insert into PersonTable (id, firstName, lastName, age, addressId, firmId, managerId) values (2, 'John', 'Johnson',22, 2,1,4);");
            s.execute("insert into PersonTable (id, firstName, lastName, age, addressId, firmId, managerId) values (3, 'John', 'Hill',12, 3,1,2);");
            s.execute("insert into PersonTable (id, firstName, lastName, age, addressId, firmId, managerId) values (4, 'Anthony', 'Allen',22, 4,1,null);");
            s.execute("insert into PersonTable (id, firstName, lastName, age, addressId, firmId, managerId) values (5, 'Fabrice', 'Roberts',34, 5,2,null);");
            s.execute("insert into PersonTable (id, firstName, lastName, age, addressId, firmId, managerId) values (6, 'Olivier', 'Doe',32, 6,3,null);");
            s.execute("insert into PersonTable (id, firstName, lastName, age, addressId, firmId, managerId) values (7, 'David', 'Harris',35, 7,4,null);");
            s.execute("Drop table if exists InteractionTable;");
            s.execute("Create Table InteractionTable(id VARCHAR(200), sourceId INT, targetId INT, time INT, active VARCHAR(1));");
            s.execute("insert into InteractionTable (id, sourceId, targetId, time, active) values (1, 1, 2, 4, 'Y');");
            s.execute("insert into InteractionTable (id, sourceId, targetId, time, active) values (2, 1, 2, 6, 'N');");
            s.execute("insert into InteractionTable (id, sourceId, targetId, time, active) values (2, 1, 3, 12, 'N');");
            s.execute("insert into InteractionTable (id, sourceId, targetId, time, active) values (2, 1, 4, 14, 'Y');");
            s.execute("insert into InteractionTable (id, sourceId, targetId, time, active) values (3, 4, 5, 3, 'N');");
            s.execute("insert into InteractionTable (id, sourceId, targetId, time, active) values (3, 4, 6, 23, 'Y');");
            s.execute("insert into InteractionTable (id, sourceId, targetId, time, active) values (4, 3, 6, 11, 'N');");
            s.execute("insert into InteractionTable (id, sourceId, targetId, time, active) values (5, 3, 7, 33, 'Y');");
            s.execute("insert into InteractionTable (id, sourceId, targetId, time, active) values (6, 4, 1, 44, 'Y');");
            s.execute("insert into InteractionTable (id, sourceId, targetId, time, active) values (6, 4, 3, 55, 'N');");
            s.execute("insert into InteractionTable (id, sourceId, targetId, time, active) values (6, 5, 4, 22, 'Y');");
            s.execute("insert into InteractionTable (id, sourceId, targetId, time, active) values (6, 5, 6, 33, 'Y');");
            s.execute("insert into InteractionTable (id, sourceId, targetId, time, active) values (7, 4, 1, 14, 'N');");
            s.execute("insert into InteractionTable (id, sourceId, targetId, time, active) values (7, 4, 2, 11, 'Y');");
            s.execute("Drop table if exists FirmTable;");
            s.execute("Create Table FirmTable(id INT, legalName VARCHAR(200), addressId INT, ceoId INT);");
            s.execute("insert into FirmTable (id, legalName, addressId, ceoId) values (1, 'FirmA', 8, 1);");
            s.execute("insert into FirmTable (id, legalName, addressId, ceoId) values (2, 'FirmB', 9, 5);");
            s.execute("insert into FirmTable (id, legalName, addressId, ceoId) values (3, 'FirmC', 10, 3);");
            s.execute("insert into FirmTable (id, legalName, addressId, ceoId) values (4, 'FirmD', 11, 7);");
            s.execute("Drop table if exists addressTable;");
            s.execute("Create table addressTable(id INT, type BIT, name VARCHAR(200), street VARCHAR(200), comments VARCHAR(200));");
            s.execute("insert into addressTable (id, type, name, street, comments) values (1,1,'Hoboken', null, 'A comment with a % in the middle');");
            s.execute("insert into addressTable (id, type, name, street, comments) values (2,1,'New York', null, 'A comment with a _ in the middle');");
            s.execute("insert into addressTable (id, type, name, street) values (3,1,'New York', null);");
            s.execute("insert into addressTable (id, type, name, street) values (4,1,'New York', null);");
            s.execute("insert into addressTable (id, type, name, street) values (5,1,'San Fransisco', null);");
            s.execute("insert into addressTable (id, type, name, street) values (6,1,'Hong Kong', null);");
            s.execute("insert into addressTable (id, type, name, street) values (7,1,'New York', null);");
            s.execute("insert into addressTable (id, type, name, street) values (8,1,'New York', 'West Street');");
            s.execute("insert into addressTable (id, type, name, street) values (9,1,'Cupertino', 'Infinite Loop');");
            s.execute("insert into addressTable (id, type, name, street) values (10,1,'Tokyo', null);");
            s.execute("insert into addressTable (id, type, name, street) values (11,1,'Mountain View', null);");
            s.execute("Drop table if exists LocationTable;");
            s.execute("Create Table LocationTable(id INT, personId INT, place VARCHAR(200),date DATE);");
            s.execute("insert into LocationTable (id, personId, place, date) values (1, 1,'New York','2014-12-01');");
            s.execute("insert into LocationTable (id, personId, place, date) values (2, 1,'Hoboken','2014-12-01');");
            s.execute("insert into LocationTable (id, personId, place, date) values (3, 2,'New York','2014-12-01');");
            s.execute("insert into LocationTable (id, personId, place, date) values (4, 2,'Hampton','2014-12-01');");
            s.execute("insert into LocationTable (id, personId, place, date) values (5, 3,'New York','2014-12-01');");
            s.execute("insert into LocationTable (id, personId, place, date) values (6, 3,'Jersey City','2014-12-01');");
            s.execute("insert into LocationTable (id, personId, place, date) values (7, 4,'New York','2014-12-01');");
            s.execute("insert into LocationTable (id, personId, place, date) values (8, 4,'Jersey City','2014-12-01');");
            s.execute("insert into LocationTable (id, personId, place, date) values (9, 5,'San Fransisco','2014-12-01');");
            s.execute("insert into LocationTable (id, personId, place, date) values (10, 5,'Paris','2014-12-01');");
            s.execute("insert into LocationTable (id, personId, place, date) values (11, 6,'Hong Kong','2014-12-01');");
            s.execute("insert into LocationTable (id, personId, place, date) values (12, 6,'London','2014-12-01');");
            s.execute("insert into LocationTable (id, personId, place, date) values (13, 7,'New York','2014-12-01');");
            s.execute("Drop table if exists placeOfInterestTable;");
            s.execute("Create Table placeOfInterestTable(id INT, locationId INT, name VARCHAR(200));");
            s.execute("insert into  placeOfInterestTable (id, locationId, name) values (1, 1,'Statue of Liberty');");
            s.execute("insert into  placeOfInterestTable (id, locationId, name) values (2, 1,'Columbus Park');");
            s.execute("insert into  placeOfInterestTable (id, locationId, name) values (3, 2,'Broadway');");
            s.execute("insert into  placeOfInterestTable (id, locationId, name) values (4, 2,'Hoboken City Hall');");
            s.execute("insert into  placeOfInterestTable (id, locationId, name) values (5, 3,'Empire State Building');");
            s.execute("Drop schema if exists productSchema cascade;");
            s.execute("Create schema productSchema;");
            s.execute("Drop table if exists productSchema.ProductTable;");
            s.execute("Create Table productSchema.ProductTable(id INT, name VARCHAR(200));");
            s.execute("insert into productSchema.ProductTable (id, name) values (1, 'FirmA');");
            s.execute("insert into productSchema.ProductTable (id, name) values (2, 'FirmB');");
            s.execute("insert into productSchema.ProductTable (id, name) values (3, 'FirmD');");
            s.execute("insert into productSchema.ProductTable (id, name) values (4, 'FirmE');");
            s.execute("Drop table if exists productSchema.SynonymTable;");
            s.execute("Create Table productSchema.SynonymTable(id INT, prodid INT, type VARCHAR(200), name VARCHAR(200));");
            s.execute("insert into productSchema.SynonymTable (id, prodid, type, name) values (1, 1, 'CUSIP', '38142Y716');");
            s.execute("insert into productSchema.SynonymTable (id, prodid, type, name) values (2, 1, 'ISIN', 'US38141G1040');");
            s.execute("insert into productSchema.SynonymTable (id, prodid, type, name) values (3, 2, 'CUSIP', '037833100');");
            s.execute("insert into productSchema.SynonymTable (id, prodid, type, name) values (4, 2, 'ISIN', 'US0378331005');");
            s.execute("insert into productSchema.SynonymTable (id, prodid, type, name) values (5, 3, 'CUSIP', '38259P706');");
            s.execute("insert into productSchema.SynonymTable (id, prodid, type, name) values (6, 3, 'ISIN', 'US38259P5089');");
            s.execute("Drop table if exists accountTable;");
            s.execute("Create Table accountTable(ID INT, name VARCHAR(200), createDate DATE);");
            s.execute("insert into accountTable (ID, name, createDate) values (1, 'Account 1', '2013-12-01');");
            s.execute("insert into accountTable (ID, name, createDate) values (2, 'Account 2', '2013-12-02');");
            s.execute("Drop table if exists orderTable;");
            s.execute("Create Table orderTable(id INT, prodid INT, accountId INT, quantity FLOAT, orderDate DATE, settlementDateTime TIMESTAMP);");
            s.execute("insert into orderTable (id, prodid, accountId, quantity, orderDate, settlementDateTime) values (1, 1, 1, 25, '2014-12-01', '2014-12-02 21:00:00');");
            s.execute("insert into orderTable (id, prodid, accountId, quantity, orderDate, settlementDateTime) values (2, 1, 2, 320, '2014-12-01', '2014-12-02 21:00:00');");
            s.execute("insert into orderTable (id, prodid, accountId, quantity, orderDate, settlementDateTime) values (3, 2, 1, 11, '2014-12-01', '2014-12-02 21:00:00');");
            s.execute("insert into orderTable (id, prodid, accountId, quantity, orderDate, settlementDateTime) values (4, 1, 2, 300, '2014-12-02', '2014-12-03 21:00:00');");
            s.execute("Drop table if exists tradeTable;");
            s.execute("Create Table tradeTable(id INT, prodid INT, accountId INT, quantity FLOAT, tradeDate DATE, settlementDateTime TIMESTAMP(9));");
            s.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate, settlementDateTime) values (1, 1, 1, 25, '2014-12-01', '2014-12-02 21:00:00');");
            s.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate, settlementDateTime) values (2, 1, 2, 320, '2014-12-01','2014-12-02 21:00:00');");
            s.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate, settlementDateTime) values (3, 2, 1, 11, '2014-12-01', '2014-12-02 21:00:00');");
            s.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate, settlementDateTime) values (4, 2, 2, 23, '2014-12-02', '2014-12-03 21:00:00');");
            s.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate, settlementDateTime) values (5, 2, 1, 32, '2014-12-02', '2014-12-03 21:00:00');");
            s.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate, settlementDateTime) values (6, 3, 1, 27, '2014-12-03', '2014-12-04 21:00:00');");
            s.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate, settlementDateTime) values (7, 3, 1, 44, '2014-12-03', '2014-12-04 15:22:23.123456789');");
            s.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate, settlementDateTime) values (8, 3, 2, 22, '2014-12-04', '2014-12-05 21:00:00');");
            s.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate, settlementDateTime) values (9, 3, 2, 45, '2014-12-04', '2014-12-05 21:00:00');");
            s.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate) values (10, 3, 2, 38, '2014-12-04');");
            s.execute("insert into tradeTable (id, prodid, accountId, quantity, tradeDate) values (11, -3, -4, 5, '2014-12-05');");
            s.execute("Drop table if exists orderPnlTable;");
            s.execute("Create Table orderPnlTable(ORDER_ID INT, pnl FLOAT);");
            s.execute("insert into orderPnlTable (ORDER_ID, pnl) values (1, 100);");
            s.execute("insert into orderPnlTable (ORDER_ID, pnl) values (2, 200);");
            s.execute("insert into orderPnlTable (ORDER_ID, pnl) values (3, 0);");
            s.execute("insert into orderPnlTable (ORDER_ID, pnl) values (4, 150);");
            s.execute("Drop table if exists salesPersonTable;");
            s.execute("Create Table salesPersonTable(PERSON_ID INT, ACCOUNT_ID INT, NAME VARCHAR(200));");
            s.execute("insert into salesPersonTable (person_id, account_id, name) values (1, 1, 'Peter Smith');");
            s.execute("insert into salesPersonTable (person_id, account_id, name) values (2, 2, 'John Johnson');");
            s.execute("Drop table if exists tradeEventTable;");
            s.execute("Create Table tradeEventTable(event_id INT, trade_id INT, eventType VARCHAR(10), eventDate DATE, person_id INT);");
            s.execute("insert into tradeEventTable (event_id, trade_id, eventType, eventDate, person_id) values (1, 1, 'New', '2014-12-01', 1);");
            s.execute("insert into tradeEventTable (event_id, trade_id, eventType, eventDate, person_id) values (2, 1, 'Correct', '2014-12-02', 2);");
            s.execute("insert into tradeEventTable (event_id, trade_id, eventType, eventDate, person_id) values (3, 1, 'Settle', '2014-12-03', 3);");
            s.execute("insert into tradeEventTable (event_id, trade_id, eventType, eventDate, person_id) values (4, 6, 'New', '2014-12-03', 4);");
            s.execute("insert into tradeEventTable (event_id, trade_id, eventType, eventDate, person_id) values (5, 6, 'Cancel', '2014-12-04', 5);");
            s.execute("Drop table if exists ingest_metadata;");
            s.execute("Create Table ingest_metadata(ingest_id INT PRIMARY KEY, dataSource_name  VARCHAR(100), dataset_name VARCHAR(100), partition_name VARCHAR(100), table_name_suffix VARCHAR(255), is_active Integer);");
            s.execute("insert into ingest_metadata (ingest_id, dataSource_name, dataset_name, partition_name, table_name_suffix, is_active) values (1, 'dataSource1', 'dataSet1', 'p1','tableNameSuffix',1);");
            s.execute("Drop schema if exists schemaA cascade;");
            s.execute("create schema schemaA;");
            s.execute("Drop table if exists schemaA.firmSet;");
            s.execute("Create Table schemaA.firmSet(id INT, name VARCHAR(200));");
            s.execute("insert into schemaA.firmSet (id, name) values (1, 'FirmA');");
            s.execute("insert into schemaA.firmSet (id, name) values (2, 'FirmB');");
            s.execute("Drop table if exists schemaA.personset;");
            s.execute("Create Table schemaA.personset(id INT, lastName VARCHAR(200), FirmID INT, firstName VARCHAR(200));");
            s.execute("insert into schemaA.personset(id, lastname, FirmID, firstName) values (3, 'Doe', 1, 'John');");
            s.execute("Drop schema if exists schemaB cascade;");
            s.execute("create schema schemaB;");
            s.execute("Drop table if exists schemaB.PERSONSET;");
            s.execute("Create Table schemaB.PERSONSET (ID INT,  age INT);");
            s.execute("insert into schemaB.PERSONSET (ID, age) values (1, 17);");
            s.execute("insert into schemaB.PERSONSET (ID, age) values (2,  20);");
            s.execute("insert into schemaB.PERSONSET (ID, age) values (3,  23);");
            s.execute("Drop table if exists otherNamesTable;");
            s.execute("Create Table otherNamesTable (PERSON_ID INT, OTHER_NAME VARCHAR(200));");
            s.execute("insert into otherNamesTable (PERSON_ID, OTHER_NAME) values (1, 'abc');");
            s.execute("insert into otherNamesTable (PERSON_ID, OTHER_NAME) values (1, 'def');");
            s.execute("insert into otherNamesTable (PERSON_ID, OTHER_NAME) values (1, 'ghi');");
            s.execute("insert into otherNamesTable (PERSON_ID, OTHER_NAME) values (2, 'jkl');");
            s.execute("insert into otherNamesTable (PERSON_ID, OTHER_NAME) values (2, 'mno');");
            System.out.println("finished inserts");
        }
        catch (Exception e)
        {
        }
        finally
        {
            try
            {
                if (s != null)
                {
                    s.close();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            try
            {
                if (c != null)
                {
                    c.close();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
