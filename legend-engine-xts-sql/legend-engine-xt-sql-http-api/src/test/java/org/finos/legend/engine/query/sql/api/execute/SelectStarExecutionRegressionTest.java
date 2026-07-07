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
import java.util.HashSet;
import java.util.Set;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Regression tests for SELECT * Query Optimization.
 * These tests verify that:
 * - SELECT * queries (with no modifications) can use pre-generated execution plans,
 *   skipping SQL-to-Pure transformation and plan generation.
 * - Parameterized SELECT * queries correctly extract service parameters and pass
 *   them to the pre-generated plan with proper type conversion (Date, Enum, String[], Integer, Float, Decimal, Boolean).
 * - The optimized path produces identical results to the standard path (regression tests).
 * Test data (from proj-1.pure H2 setup):
 *   Alice:    id=101, ratings=9.1, salary=50000.00, type=Type1, start_date=2023-08-24
 *   Bob:      id=102, ratings=9.2, salary=60000.50, type=Type2, start_date=2022-08-24
 *   Curtis:   id=103, ratings=9.3, salary=75000.75, type=Type2, start_date=2022-07-24
 *   Danielle: id=104, ratings=9.4, salary=80000.25, type=Type1, start_date=2022-07-23
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
    public static final ResourceTestRule resources = buildResources(true);

    public static ResourceTestRule buildResources(boolean enablePreGeneratedPlans)
    {
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
        PlanExecutor executor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();
        MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));

        TestSQLSourceProvider testSQLSourceProvider = new TestSQLSourceProvider(enablePreGeneratedPlans);
        LOGGER.info("Building resources: preGeneratedPlans={}, planCount={}", testSQLSourceProvider.isPreGeneratedPlansEnabled(), testSQLSourceProvider.getPreGeneratedPlanCount());

        SqlExecute sqlExecute = new SqlExecute(modelManager, executor, (pm) -> PureCoreExtensionLoader.extensions().flatCollect(g -> g.extraPureCoreExtensions(pm.getExecutionSupport())), FastList.newListWith(testSQLSourceProvider), generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers));

        return ResourceTestRule.builder()
                .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
                .addResource(sqlExecute)
                .addResource(new MockPac4jFeature())
                .addResource(new CatchAllExceptionMapper())
                .bootstrapLogging(false)
                .build();
    }

    // ==================== HELPER METHODS ====================

    private TDSExecuteResult execute(String sql) throws JsonProcessingException
    {
        String response = resources.target("sql/v1/execution/execute")
                .request()
                .post(Entity.json(new SQLQueryInput(null, sql, FastList.newList())))
                .readEntity(String.class);
        TDSExecuteResult result = OM.readValue(response, TDSExecuteResult.class);
        assertNotNull("Result should not be null for: " + sql, result);
        assertNotNull("Result.result should not be null for: " + sql + "\nRaw response: " + response, result.result);
        return result;
    }

    private int getColumnIndex(TDSExecuteResult result, String columnName)
    {
        int index = result.result.columns.indexOf(columnName);
        assertTrue("Column '" + columnName + "' not found. Available: " + result.result.columns, index >= 0);
        return index;
    }

    private Set<Object> getColumnValues(TDSExecuteResult result, String columnName)
    {
        int colIndex = getColumnIndex(result, columnName);
        return result.result.rows.stream()
                .map(row -> row.values.get(colIndex))
                .collect(Collectors.toSet());
    }

    private void assertReturnsNames(String sql, String... expectedNames) throws JsonProcessingException
    {
        TDSExecuteResult result = execute(sql);
        assertEquals("Row count mismatch for: " + sql, expectedNames.length, result.result.rows.size());
        Set<Object> actualNames = getColumnValues(result, "Name");
        Set<Object> expected = new HashSet<>();
        for (String name : expectedNames)
        {
            expected.add(name);
        }
        assertEquals("Names mismatch for: " + sql, expected, actualNames);
    }

    private void assertReturnsEmpty(String sql) throws JsonProcessingException
    {
        TDSExecuteResult result = execute(sql);
        assertTrue("Expected no rows for: " + sql, result.result.rows.isEmpty());
    }

    private void assertReturnsRows(String sql) throws JsonProcessingException
    {
        TDSExecuteResult result = execute(sql);
        assertFalse("Expected rows for: " + sql, result.result.rows.isEmpty());
    }

    private void assertOptimizedMatchesStandard(String selectStarSql, String standardSql) throws JsonProcessingException
    {
        TDSExecuteResult selectStarResult = execute(selectStarSql);
        TDSExecuteResult standardResult = execute(standardSql);

        assertEquals("Row count should match between optimized and standard paths",
                selectStarResult.result.rows.size(),
                standardResult.result.rows.size());

        for (String col : standardResult.result.columns)
        {
            assertTrue("SELECT * should contain column '" + col + "'",
                    selectStarResult.result.columns.contains(col));
        }

        if (standardResult.result.columns.contains("Name") && selectStarResult.result.columns.contains("Name"))
        {
            Set<Object> starNames = getColumnValues(selectStarResult, "Name");
            Set<Object> stdNames = getColumnValues(standardResult, "Name");
            assertEquals("Names should match between optimized and standard paths", starNames, stdNames);
        }
    }

    // ==================== TESTING FOR VARIOUS DATATYPE PARAMETERS ====================

    // ---- DateTime format tests (biggest risk: requires getPlanParameters Pure pipeline) ----

    @Test
    public void testDateTimeParameter_IsoWithT() throws JsonProcessingException
    {
        // Format: yyyy-MM-ddTHH:mm:ss (ISO 8601 with T separator)
        assertReturnsNames("SELECT * FROM service('/personServiceByDateTimeExact', dateTime => '2023-08-24T12:00:00')", "Alice");
        assertReturnsNames("SELECT * FROM service('/personServiceByDateTimeExact', dateTime => '2022-08-24T15:30:00')", "Bob");
        assertReturnsNames("SELECT * FROM service('/personServiceByDateTimeExact', dateTime => '2022-07-23T18:45:30')", "Danielle");
    }

    @Test
    public void testDateTimeParameter_SpaceSeparator() throws JsonProcessingException
    {
        // Format: yyyy-MM-dd HH:mm:ss (space separator instead of T)
        assertReturnsNames("SELECT * FROM service('/personServiceByDateTimeExact', dateTime => '2023-08-24 12:00:00')", "Alice");
        assertReturnsNames("SELECT * FROM service('/personServiceByDateTimeExact', dateTime => '2022-08-24 15:30:00')", "Bob");
        assertReturnsNames("SELECT * FROM service('/personServiceByDateTimeExact', dateTime => '2022-07-23 18:45:30')", "Danielle");
    }

    @Test
    public void testDateTimeParameter_IsoWithMilliseconds() throws JsonProcessingException
    {
        // Format: yyyy-MM-ddTHH:mm:ss.SSS (ISO 8601 with milliseconds)
        assertReturnsNames("SELECT * FROM service('/personServiceByDateTimeExact', dateTime => '2023-08-24T12:00:00.000')", "Alice");
        assertReturnsNames("SELECT * FROM service('/personServiceByDateTimeExact', dateTime => '2022-07-23T18:45:30.000')", "Danielle");
        assertReturnsEmpty("SELECT * FROM service('/personServiceByDateTimeExact', dateTime => '2023-08-24T12:00:00.001')");
    }

    @Test
    public void testDateTimeParameter_SpaceWithMilliseconds() throws JsonProcessingException
    {
        // Format: yyyy-MM-dd HH:mm:ss.SSS (space separator with milliseconds)
        assertReturnsNames("SELECT * FROM service('/personServiceByDateTimeExact', dateTime => '2023-08-24 12:00:00.000')", "Alice");
        assertReturnsNames("SELECT * FROM service('/personServiceByDateTimeExact', dateTime => '2022-07-23 18:45:30.000')", "Danielle");
        assertReturnsEmpty("SELECT * FROM service('/personServiceByDateTimeExact', dateTime => '2022-07-23 18:45:30.001')");
    }

    @Test
    public void testDateTimeParameter_RangeWithMultipleFormats() throws JsonProcessingException
    {
        // Verify >= comparison works correctly with all DateTime formats
        // sinceDateTime >= '2022-08-24T15:30:00' should return Alice (2023-08-24 12:00:00), Bob (2022-08-24 15:30:00)
        assertReturnsNames("SELECT * FROM service('/personServiceByDateTimeRange', sinceDateTime => '2022-08-24T15:30:00')", "Alice", "Bob");
        assertReturnsNames("SELECT * FROM service('/personServiceByDateTimeRange', sinceDateTime => '2022-08-24 15:30:00')", "Alice", "Bob");
        assertReturnsNames("SELECT * FROM service('/personServiceByDateTimeRange', sinceDateTime => '2022-08-24T15:30:00.000')", "Alice", "Bob");
        assertReturnsNames("SELECT * FROM service('/personServiceByDateTimeRange', sinceDateTime => '2022-08-24 15:30:00.000')", "Alice", "Bob");
    }

    @Test
    public void testDateTimeParameter_NoMatch() throws JsonProcessingException
    {
        // Future date — no data should match
        assertReturnsEmpty("SELECT * FROM service('/personServiceByDateTimeExact', dateTime => '2026-06-09T22:10:11.000')");
        assertReturnsEmpty("SELECT * FROM service('/personServiceByDateTimeExact', dateTime => '2026-06-09 22:10:11.000')");
        assertReturnsEmpty("SELECT * FROM service('/personServiceByDateTimeRange', sinceDateTime => '2026-06-09T22:10:11.000')");
        assertReturnsEmpty("SELECT * FROM service('/personServiceByDateTimeRange', sinceDateTime => '2026-06-09 22:10:11.000')");
    }

    @Test
    public void testDateTimeParameter_FormatConsistency() throws JsonProcessingException
    {
        // Same logical DateTime in all four formats must produce identical results
        TDSExecuteResult isoWithT = execute("SELECT * FROM service('/personServiceByDateTimeRange', sinceDateTime => '2022-07-24T09:15:30')");
        TDSExecuteResult spaceNoMs = execute("SELECT * FROM service('/personServiceByDateTimeRange', sinceDateTime => '2022-07-24 09:15:30')");
        TDSExecuteResult isoWithMs = execute("SELECT * FROM service('/personServiceByDateTimeRange', sinceDateTime => '2022-07-24T09:15:30.000')");
        TDSExecuteResult spaceWithMs = execute("SELECT * FROM service('/personServiceByDateTimeRange', sinceDateTime => '2022-07-24 09:15:30.000')");

        assertEquals("ISO with T and space separator must return same row count",
                isoWithT.result.rows.size(), spaceNoMs.result.rows.size());
        assertEquals("ISO with T and ISO with millis must return same row count",
                isoWithT.result.rows.size(), isoWithMs.result.rows.size());
        assertEquals("ISO with T and space with millis must return same row count",
                isoWithT.result.rows.size(), spaceWithMs.result.rows.size());

        Set<Object> namesT = getColumnValues(isoWithT, "Name");
        Set<Object> namesSpace = getColumnValues(spaceNoMs, "Name");
        Set<Object> namesMs = getColumnValues(isoWithMs, "Name");
        Set<Object> namesSpaceMs = getColumnValues(spaceWithMs, "Name");

        assertEquals("All four DateTime formats must produce identical results", namesT, namesSpace);
        assertEquals("All four DateTime formats must produce identical results", namesT, namesMs);
        assertEquals("All four DateTime formats must produce identical results", namesT, namesSpaceMs);
    }

    // ---- Other datatype parameter tests ----

    @Test
    public void testDateParameter() throws JsonProcessingException
    {
        assertReturnsNames("SELECT * FROM service('/personServiceForStartDate/{date}', date => '2023-08-24')", "Alice");
        assertReturnsNames("SELECT * FROM service('/personServiceForStartDate/{date}', date => '2022-07-23')", "Danielle");
        assertReturnsEmpty("SELECT * FROM service('/personServiceForStartDate/{date}', date => '1999-01-01')");
    }

    @Test
    public void testStrictDateParameter() throws JsonProcessingException
    {
        // asOfDate >= 2022-08-01: Alice (2023-08-24), Bob (2022-08-24)
        assertReturnsNames("SELECT * FROM service('/personServiceByStrictDate', asOfDate => '2022-08-01')", "Alice", "Bob");
    }

    @Test
    public void testEnumParameter() throws JsonProcessingException
    {
        assertReturnsNames("SELECT * FROM service('/personServiceForStartDate/{date}', date => '2023-08-24', type => 'Type1')", "Alice");
        assertReturnsEmpty("SELECT * FROM service('/personServiceForStartDate/{date}', date => '2023-08-24', type => 'Type2')");
    }

    @Test
    public void testStringArrayParameter() throws JsonProcessingException
    {
        assertReturnsNames("SELECT * FROM service('/personServiceForNames', names => ARRAY['Alice', 'Bob'])", "Alice", "Bob");
        assertReturnsNames("SELECT * FROM service('/personServiceForNames', names => ARRAY['Curtis'])", "Curtis");
        assertReturnsNames("SELECT * FROM service('/personServiceForNames', names => ARRAY['Alice', 'Bob', 'Curtis', 'Danielle'])", "Alice", "Bob", "Curtis", "Danielle");
    }

    @Test
    public void testIntegerParameter() throws JsonProcessingException
    {
        assertReturnsNames("SELECT * FROM service('/personServiceById', minId => 103)", "Curtis", "Danielle");
        assertReturnsNames("SELECT * FROM service('/personServiceById', minId => 100)", "Alice", "Bob", "Curtis", "Danielle");
        assertReturnsEmpty("SELECT * FROM service('/personServiceById', minId => 999)");
        assertReturnsNames("SELECT * FROM service('/personServiceById', minId => '103')", "Curtis", "Danielle");
    }

    @Test
    public void testStringParameter() throws JsonProcessingException
    {
        assertReturnsNames("SELECT * FROM service('/personServiceByName', name => 'Alice')", "Alice");
        assertReturnsEmpty("SELECT * FROM service('/personServiceByName', name => 'Wendy')");
    }

    @Test
    public void testBooleanParameter() throws JsonProcessingException
    {
        assertReturnsNames("SELECT * FROM service('/personServiceByHighRating', highOnly => true)", "Curtis", "Danielle");
        assertReturnsNames("SELECT * FROM service('/personServiceByHighRating', highOnly => false)", "Alice", "Bob", "Curtis", "Danielle");
        assertReturnsNames("SELECT * FROM service('/personServiceByHighRating', highOnly => 'true')", "Curtis", "Danielle");
    }

    @Test
    public void testFloatParameter() throws JsonProcessingException
    {
        assertReturnsNames("SELECT * FROM service('/personServiceByRating', minRating => 9.25)", "Curtis", "Danielle");
        assertReturnsNames("SELECT * FROM service('/personServiceByRating', minRating => 9.1)", "Alice", "Bob", "Curtis", "Danielle");
        assertReturnsNames("SELECT * FROM service('/personServiceByRating', minRating => '9.25')", "Curtis", "Danielle");
    }

    @Test
    public void testDecimalParameter() throws JsonProcessingException
    {
        assertReturnsNames("SELECT * FROM service('/personServiceBySalary', minSalary => '70000.00')", "Curtis", "Danielle");
        assertReturnsNames("SELECT * FROM service('/personServiceBySalary', minSalary => '50000.00')", "Alice", "Bob", "Curtis", "Danielle");
        assertReturnsEmpty("SELECT * FROM service('/personServiceBySalary', minSalary => '100000.00')");
    }

    @Test
    public void testMultipleParameters() throws JsonProcessingException
    {
        assertReturnsNames("SELECT * FROM service('/personServiceMultiParam', minId => 101, type => 'Type1')", "Alice", "Danielle");
        assertReturnsNames("SELECT * FROM service('/personServiceMultiParam', minId => 104, type => 'Type1')", "Danielle");
    }

    // ==================== NESTED QUERIES ====================
    @Test
    public void testNestedQueries() throws JsonProcessingException
    {
        // Nested SELECT * with parameters
        assertReturnsNames("SELECT * FROM (SELECT * FROM service('/personServiceById', minId => 103))", "Curtis", "Danielle");
        assertReturnsNames("SELECT * FROM service('/personServiceById', minId => 102) AS t", "Bob", "Curtis", "Danielle");
    }

    // ==================== MULTI-EXECUTION SERVICE ====================
    @Test
    public void testMultiExecService_Env1() throws JsonProcessingException
    {
        // Multi-execution service with env key — resolves CompositeExecutionPlan to the sub-plan for 'env1'
        assertReturnsNames("SELECT * FROM service('/personServiceMultiExec/{env}', env => 'env1', minId => 103)", "Curtis", "Danielle");
        assertReturnsNames("SELECT * FROM service('/personServiceMultiExec/{env}', env => 'env1', minId => 101)", "Alice", "Bob", "Curtis", "Danielle");
    }

    @Test
    public void testMultiExecService_Env2() throws JsonProcessingException
    {
        // Same query routed to 'env2' — both envs point to the same DB so results are identical
        assertReturnsNames("SELECT * FROM service('/personServiceMultiExec/{env}', env => 'env2', minId => 103)", "Curtis", "Danielle");
        assertReturnsNames("SELECT * FROM service('/personServiceMultiExec/{env}', env => 'env2', minId => 101)", "Alice", "Bob", "Curtis", "Danielle");
    }

    @Test
    public void testMultiExecService_ConsistentAcrossKeys() throws JsonProcessingException
    {
        // Verify both execution keys produce identical results (same underlying DB)
        TDSExecuteResult env1Result = execute("SELECT * FROM service('/personServiceMultiExec/{env}', env => 'env1', minId => 102)");
        TDSExecuteResult env2Result = execute("SELECT * FROM service('/personServiceMultiExec/{env}', env => 'env2', minId => 102)");

        assertEquals("Both execution keys should return same row count",
                env1Result.result.rows.size(), env2Result.result.rows.size());

        Set<Object> env1Names = getColumnValues(env1Result, "Name");
        Set<Object> env2Names = getColumnValues(env2Result, "Name");
        assertEquals("Both execution keys should return same names", env1Names, env2Names);
    }

    // ==================== BASIC SERVICE (NO PARAMETERS) ====================
    @Test
    public void testBasicService() throws JsonProcessingException
    {
        assertReturnsRows("SELECT * FROM service('/testService')");
        assertReturnsRows("SELECT * FROM (SELECT * FROM service('/testService'))");

        // Schema validation
        TDSExecuteResult result = execute("SELECT * FROM service('/personServiceById', minId => 101)");
        assertFalse("Columns should not be empty", result.result.columns.isEmpty());
    }

    // ==================== STANDARD PATH FALLBACK ====================
    @Test
    public void testStandardPathFallback() throws JsonProcessingException
    {
        // These disqualify SELECT * optimization and fall back to standard path
        assertReturnsRows("SELECT * FROM service('/testService') WHERE Id > 0");
        assertReturnsRows("SELECT Name FROM service('/testService')");
        assertReturnsRows("SELECT * FROM service('/testService') ORDER BY Name");
    }

    // ==================== SELECT * VS STANDARD PATH REGRESSION ====================
    @Test
    public void testOptimizedMatchesStandardPath() throws JsonProcessingException
    {
        assertOptimizedMatchesStandard("SELECT * FROM service('/testService')","SELECT Name, Id FROM service('/testService')");
        assertOptimizedMatchesStandard("SELECT * FROM service('/personServiceById', minId => 103)","SELECT Name, Id FROM service('/personServiceById', minId => 103)");
    }

    // ==================== OPTIONAL PARAMETER HANDLING ====================
    @Test
    public void testOptionalParameter_Omitted() throws JsonProcessingException
    {
        assertReturnsRows("SELECT * FROM service('/personServiceForStartDate/{date}', date => '2023-08-24')");
    }
}
