// Copyright 2026 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package org.finos.legend.engine.query.sql.api.execute;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.query.sql.api.CatchAllExceptionMapper;
import org.finos.legend.engine.query.sql.api.MockPac4jFeature;
import org.finos.legend.engine.query.sql.api.TestSQLSourceProvider;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.ClassRule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Entity;
import java.util.ServiceLoader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * Integration tests for SELECT * Query Optimization.
 * These tests verify that:
 * - SELECT * queries (with no modifications) can use pre-generated execution plans,
 * skipping SQL-to-Pure transformation and plan generation for simple SELECT * queries.
 */
public class SelectStarExecutionRegressionTest
{
    private static final Logger LOGGER = LoggerFactory.getLogger(SelectStarExecutionRegressionTest.class);
    private static final ObjectMapper OM = new ObjectMapper();

    static
    {
        System.setProperty(TestProperties.CONTAINER_PORT, "0");
    }

    @ClassRule
    public static final ResourceTestRule resources = getResourceTestRule();

    public static ResourceTestRule getResourceTestRule()
    {
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
        PlanExecutor executor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();
        MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));

        TestSQLSourceProvider testSQLSourceProvider = new TestSQLSourceProvider(true);
        LOGGER.info("Pre-generated plans enabled: {}, count: {}", testSQLSourceProvider.isPreGeneratedPlansEnabled(), testSQLSourceProvider.getPreGeneratedPlanCount());

        SqlExecute sqlExecute = new SqlExecute(modelManager, executor, (pm) -> PureCoreExtensionLoader.extensions().flatCollect(g -> g.extraPureCoreExtensions(pm.getExecutionSupport())), FastList.newListWith(testSQLSourceProvider), generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers));

        return ResourceTestRule.builder()
                .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
                .addResource(sqlExecute)
                .addResource(new MockPac4jFeature())
                .addResource(new CatchAllExceptionMapper())
                .bootstrapLogging(false)
                .build();
    }

    private String executeQuery(String sql)
    {
        return resources.target("sql/v1/execution/execute")
                .request()
                .post(Entity.json(new SQLQueryInput(null, sql, FastList.newList())))
                .readEntity(String.class);
    }

    // ==================== SELECT * QUERY TESTS ====================

    @Test
    public void testSelectStar_SimpleSelectAll() throws JsonProcessingException
    {
        String sql = "SELECT * FROM service('/testService')";
        String result = executeQuery(sql);

        TDSExecuteResult tdsResult = OM.readValue(result, TDSExecuteResult.class);
        assertFalse("Should return rows", tdsResult.result.rows.isEmpty());
    }

    @Test
    public void testSelectStar_NestedSelectAll() throws JsonProcessingException
    {
        String sql = "SELECT * FROM (SELECT * FROM service('/testService'))";
        String result = executeQuery(sql);

        TDSExecuteResult tdsResult = OM.readValue(result, TDSExecuteResult.class);
        assertFalse("Should return rows", tdsResult.result.rows.isEmpty());
    }

    @Test
    public void testSelectStar_WithTableAlias() throws JsonProcessingException
    {
        String sql = "SELECT * FROM service('/testService') AS t";
        String result = executeQuery(sql);

        TDSExecuteResult tdsResult = OM.readValue(result, TDSExecuteResult.class);
        assertFalse("Should return rows", tdsResult.result.rows.isEmpty());
    }

    // ==================== STANDARD PATH TESTS ====================

    @Test
    public void testStandard_SelectWithWhereClause() throws JsonProcessingException
    {
        String sql = "SELECT * FROM service('/testService') WHERE Id > 0";
        String result = executeQuery(sql);

        TDSExecuteResult tdsResult = OM.readValue(result, TDSExecuteResult.class);
        assertNotNull("Should return result", tdsResult.result);
    }

    @Test
    public void testStandard_SelectSpecificColumns() throws JsonProcessingException
    {
        String sql = "SELECT Name FROM service('/testService')";
        String result = executeQuery(sql);

        TDSExecuteResult tdsResult = OM.readValue(result, TDSExecuteResult.class);
        assertFalse("Should return rows", tdsResult.result.rows.isEmpty());
    }

    @Test
    public void testStandard_SelectWithOrderBy() throws JsonProcessingException
    {
        String sql = "SELECT * FROM service('/testService') ORDER BY Name";
        String result = executeQuery(sql);

        TDSExecuteResult tdsResult = OM.readValue(result, TDSExecuteResult.class);
        assertFalse("Should return rows", tdsResult.result.rows.isEmpty());
    }

    @Test
    public void testStandard_SelectWithLimit() throws JsonProcessingException
    {
        String sql = "SELECT * FROM service('/testService') LIMIT 10";
        String result = executeQuery(sql);

        TDSExecuteResult tdsResult = OM.readValue(result, TDSExecuteResult.class);
        assertNotNull("Should return result", tdsResult.result);
    }

    @Test
    public void testStandard_SelectWithGroupBy() throws JsonProcessingException
    {
        String sql = "SELECT Name, count(*) FROM service('/testService') GROUP BY Name";
        String result = executeQuery(sql);

        TDSExecuteResult tdsResult = OM.readValue(result, TDSExecuteResult.class);
        assertNotNull("Should return result", tdsResult.result);
    }

    @Test
    public void testStandard_NestedSelectWithWhere() throws JsonProcessingException
    {
        String sql = "SELECT * FROM (SELECT * FROM service('/testService') WHERE Id > 0)";
        String result = executeQuery(sql);

        TDSExecuteResult tdsResult = OM.readValue(result, TDSExecuteResult.class);
        assertNotNull("Should return result", tdsResult.result);
    }

    @Test
    public void testStandard_SelectDistinct() throws JsonProcessingException
    {
        String sql = "SELECT DISTINCT Name FROM service('/testService')";
        String result = executeQuery(sql);

        TDSExecuteResult tdsResult = OM.readValue(result, TDSExecuteResult.class);
        assertNotNull("Should return result", tdsResult.result);
    }
}