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

package org.finos.legend.engine.postgres.e2e;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.finos.legend.engine.language.pure.grammar.test.GrammarParseTestUtils;
import org.finos.legend.engine.language.pure.modelManager.ModelManager;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.generation.extension.PlanGeneratorExtension;
import org.finos.legend.engine.postgres.config.ServerConfig;
import org.finos.legend.engine.postgres.e2e.coverage.FailureDetailReport;
import org.finos.legend.engine.postgres.e2e.coverage.FunctionCatalogExtractor;
import org.finos.legend.engine.postgres.e2e.coverage.FunctionCoverageMapper;
import org.finos.legend.engine.postgres.e2e.coverage.FunctionCoverageReport;
import org.finos.legend.engine.postgres.e2e.coverage.HtmlReportGenerator;
import org.finos.legend.engine.postgres.e2e.coverage.StructuralParityReport;
import org.finos.legend.engine.postgres.protocol.sql.SQLManager;
import org.finos.legend.engine.postgres.protocol.sql.handler.legend.bridge.sql.LegendExecutionService;
import org.finos.legend.engine.postgres.protocol.wire.auth.identity.AnonymousIdentityProvider;
import org.finos.legend.engine.postgres.protocol.wire.auth.method.NoPasswordAuthenticationMethod;
import org.finos.legend.engine.postgres.protocol.wire.serialization.Messages;
import org.finos.legend.engine.protocol.pure.v1.model.context.PureModelContextData;
import org.finos.legend.engine.pure.code.core.PureCoreExtensionLoader;
import org.finos.legend.engine.query.sql.api.CatchAllExceptionMapper;
import org.finos.legend.engine.query.sql.api.MockPac4jFeature;
import org.finos.legend.engine.query.sql.api.execute.SqlExecute;
import org.finos.legend.engine.shared.core.deployment.DeploymentMode;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.postgresql.ds.PGSimpleDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

/**
 * Main Postgres Parity Test Suite.
 * Runs SQL tests against both direct Postgres and Legend SQL server (via wire protocol),
 * comparing results for both TDS and Relation execution paths.
 */

@Testcontainers
public class TestPostgresParity
{
    private static final Logger LOGGER = LoggerFactory.getLogger(TestPostgresParity.class);

    static
    {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static E2eTestPostgresServer legendServer;
    private static DirectPostgresRunner directRunner;
    private static ParityReport report;
    private static YamlStatusUpdater statusUpdater;
    private static Map<String, List<FunctionCatalogExtractor.PgFunction>> functionCatalog;
    private static Set<String> knownTables;
    private static ResourceTestRule resourceTestRule;
    private static Connection legendConnection;

    static final String[] TEST_FILES = {
            "parity-tests/schema.yaml",
            "parity-tests/smoke_tests.yaml",
            "parity-tests/functions/math_functions.yaml",
            "parity-tests/functions/string_functions.yaml",
            "parity-tests/functions/binary_functions.yaml",
            "parity-tests/functions/pattern_matching.yaml",
            "parity-tests/functions/formatting_functions.yaml",
            "parity-tests/functions/datetime_functions.yaml",
            "parity-tests/functions/conditional_functions.yaml",
            "parity-tests/functions/json_functions.yaml",
            "parity-tests/functions/array_functions.yaml",
            "parity-tests/functions/aggregate_functions.yaml",
            "parity-tests/functions/window_functions.yaml",
            "parity-tests/functions/network_functions.yaml",
            "parity-tests/functions/system_functions.yaml",
            "parity-tests/functions/sequence_functions.yaml",
            "parity-tests/functions/set_returning_functions.yaml",
            "parity-tests/structural/joins.yaml",
            "parity-tests/structural/unions.yaml",
            "parity-tests/structural/subqueries.yaml",
            "parity-tests/structural/ctes.yaml",
            "parity-tests/structural/order_limit_offset.yaml",
            "parity-tests/structural/group_by.yaml",
            "parity-tests/structural/distinct.yaml",
            "parity-tests/structural/null_semantics.yaml",
            "parity-tests/structural/type_casting.yaml",
            "parity-tests/structural/case_expressions.yaml",
            "parity-tests/structural/where_predicates.yaml",
            "parity-tests/structural/aliases.yaml",
            "parity-tests/structural/having.yaml",
            "parity-tests/structural/lateral_joins.yaml",
            "parity-tests/structural/boolean_logic.yaml",
            "parity-tests/structural/select_star.yaml",
            "parity-tests/structural/multiple_schemas.yaml",
            "parity-tests/structural/json_operators.yaml",
            "parity-tests/structural/interval_arithmetic.yaml",
            "parity-tests/window_frames/frame_types.yaml",
            "parity-tests/window_frames/partition_ordering.yaml",
            "parity-tests/window_frames/frame_exclusion.yaml",
            "parity-tests/window_frames/named_windows.yaml",
            "parity-tests/compositions/agg_window_mix.yaml",
            "parity-tests/compositions/multi_join_agg.yaml",
            "parity-tests/compositions/stress_queries.yaml",
            "parity-tests/compositions/nested_subqueries.yaml",
            "parity-tests/compositions/window_over_agg.yaml"
    };

    @BeforeAll
    static void setUp() throws Exception
    {
        PGSimpleDataSource pgDataSource = new PGSimpleDataSource();
        pgDataSource.setUrl(postgres.getJdbcUrl());
        pgDataSource.setUser(postgres.getUsername());
        pgDataSource.setPassword(postgres.getPassword());
        directRunner = new DirectPostgresRunner(pgDataSource);
        report = new ParityReport();
        statusUpdater = new YamlStatusUpdater();
        knownTables = new HashSet<>();
        Set<String> allIds = new LinkedHashSet<>();
        List<String> duplicateIds = new ArrayList<>();
        for (String testFile : TEST_FILES)
        {
            TestCaseLoader.TestFile file = TestCaseLoader.load(testFile);
            if (file.schema != null)
            {
                SchemaManager schemaManager = new SchemaManager(pgDataSource);
                schemaManager.createSchema(file.schema);
                for (TestCaseLoader.TableDef table : file.schema.tables)
                {
                    knownTables.add(table.name.toLowerCase());
                }
            }
            if (file.tests != null)
            {
                for (TestCaseLoader.TestCase tc : file.tests)
                {
                    if (!allIds.add(tc.id))
                    {
                        duplicateIds.add(tc.id + " (in " + testFile + ")");
                    }
                }
            }
        }
        if (!duplicateIds.isEmpty())
        {
            throw new IllegalStateException("Duplicate test IDs found (must be globally unique):\n  "
                    + String.join("\n  ", duplicateIds));
        }
        try (Connection conn = pgDataSource.getConnection();
             Statement stmt = conn.createStatement())
        {
            stmt.execute("CREATE SEQUENCE IF NOT EXISTS test_seq START 1");
        }
        PureModelContextData modelData = GrammarParseTestUtils.loadPureModelContextFromResource(
                "e2e-model.pure", TestPostgresParity.class);
        registerVault();
        E2eTestSourceProvider sourceProvider = new E2eTestSourceProvider(
                modelData,
                postgres.getHost(),
                postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                postgres.getDatabaseName(),
                postgres.getUsername(),
                postgres.getPassword()
        );
        ModelManager modelManager = new ModelManager(DeploymentMode.TEST);
        PlanExecutor executor = PlanExecutor.newPlanExecutorWithAvailableStoreExecutors();
        MutableList<PlanGeneratorExtension> generatorExtensions = Lists.mutable.withAll(ServiceLoader.load(PlanGeneratorExtension.class));
        SqlExecute sqlExecute = new SqlExecute(
                modelManager,
                executor,
                (pm) -> PureCoreExtensionLoader.extensions().flatCollect(g -> g.extraPureCoreExtensions(pm.getExecutionSupport())),
                FastList.newListWith(sourceProvider),
                generatorExtensions.flatCollect(PlanGeneratorExtension::getExtraPlanTransformers)
        );
        resourceTestRule = ResourceTestRule.builder()
                .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
                .addResource(sqlExecute)
                .addResource(new MockPac4jFeature())
                .addResource(new CatchAllExceptionMapper())
                .bootstrapLogging(false)
                .build();
        // Dropwizard 1.3.x only provides ResourceTestRule (JUnit 4 @Rule).
        // Since we use JUnit 5, we must reflectively invoke the lifecycle methods.
        startResourceTestRule(resourceTestRule);
        ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(0);
        serverConfig.setHttpPort(0);
        legendServer = new E2eTestPostgresServer(
                serverConfig,
                new SQLManager(Lists.mutable.with(new LegendExecutionService(new E2eLegendTestClient(resourceTestRule)))),
                (user, connectionProperties) -> new NoPasswordAuthenticationMethod(new AnonymousIdentityProvider()),
                new Messages(Throwable::getMessage)
        );
        legendServer.startUp();
        int port = legendServer.getLocalAddress().getPort();
        legendConnection = DriverManager.getConnection(
                "jdbc:postgresql://127.0.0.1:" + port + "/postgres", "dummy", "dummy");
        try
        {
            FunctionCatalogExtractor extractor = new FunctionCatalogExtractor(pgDataSource);
            functionCatalog = extractor.extractCatalog();
        }
        catch (Exception e)
        {
            LOGGER.warn("Failed to extract function catalog: {}", e.getMessage());
            functionCatalog = new LinkedHashMap<>();
        }
    }

    @AfterAll
    static void tearDown() throws Exception
    {
        report.printSummary();
        report.writeJsonReport(new File("target/parity-report.json"));
        if (Boolean.getBoolean("parity.updateStatus"))
        {
            File resourceDir = new File("src/test/resources");
            if (!resourceDir.isDirectory())
            {
                resourceDir = new File("legend-engine-xts-sql/legend-engine-xt-sql-e2e-tests/src/test/resources");
            }
            statusUpdater.updateFiles(resourceDir, TEST_FILES);
        }
        generateFunctionCoverageReport();
        if (legendConnection != null)
        {
            try
            {
                legendConnection.close();
            }
            catch (Exception e)
            {
                LOGGER.debug("Error closing Legend connection", e);
            }
        }
        if (legendServer != null)
        {
            legendServer.shutDown();
        }
        stopResourceTestRule(resourceTestRule);
        if (Boolean.getBoolean("parity.failOnError") && report.hasFailures())
        {
            throw new AssertionError("Parity tests have failures. See target/parity-report.json for details.");
        }
    }

    @TestFactory
    Collection<DynamicTest> parityTests()
    {
        List<DynamicTest> tests = new ArrayList<>();
        for (String testFile : TEST_FILES)
        {
            TestCaseLoader.TestFile file = TestCaseLoader.load(testFile);
            if (file.tests == null || file.tests.isEmpty())
            {
                continue;
            }
            String category = testFile.replace("parity-tests/", "").replace(".yaml", "");
            for (TestCaseLoader.TestCase tc : file.tests)
            {
                tests.add(DynamicTest.dynamicTest(tc.id + " [TDS]", () -> runTest(tc, category, "TDS")));
                tests.add(DynamicTest.dynamicTest(tc.id + " [Relation]", () -> runTest(tc, category, "Relation")));
            }
        }
        return tests;
    }

    private void runTest(TestCaseLoader.TestCase tc, String category, String path)
    {
        if (tc.skip != null)
        {
            report.record(new ParityReport.TestResult(tc.id, category, path, "SKIP", tc.sql, null, tc.skip, null));
            statusUpdater.recordResult(tc.id, path, "SKIP");
            return;
        }
        ResultMatrix expected;
        try
        {
            expected = directRunner.execute(tc.sql);
        }
        catch (Exception e)
        {
            report.record(new ParityReport.TestResult(tc.id, category, path, "BUG", tc.sql, null, e.getMessage(), null));
            statusUpdater.recordResult(tc.id, path, "BUG");
            return;
        }
        String rewrittenSql;
        try
        {
            if (Boolean.TRUE.equals(tc.join_func))
            {
                String prefix = "TDS".equals(path) ? "tds" : "rel";
                rewrittenSql = "SELECT * FROM func('e2e::" + prefix + "_person_with_dept') ORDER BY \"name\"";
            }
            else
            {
                String prefix = "TDS".equals(path) ? "tds" : "rel";
                AstFromRewriter rewriter = new AstFromRewriter(prefix, knownTables);
                rewrittenSql = rewriter.hasTableReferences(tc.sql) ? rewriter.rewrite(tc.sql) : tc.sql;
            }
        }
        catch (Exception e)
        {
            report.record(new ParityReport.TestResult(tc.id, category, path, "ERROR", tc.sql, null, "Rewrite failed: " + e.getMessage(), null));
            statusUpdater.recordResult(tc.id, path, "ERROR");
            assertNoRegression(tc, path, "ERROR");
            return;
        }
        ResultMatrix actual;
        try
        {
            actual = executeLegendQuery(rewrittenSql);
        }
        catch (Exception e)
        {
            report.record(new ParityReport.TestResult(tc.id, category, path, "ERROR", tc.sql, rewrittenSql, e.getMessage(), null));
            statusUpdater.recordResult(tc.id, path, "ERROR");
            assertNoRegression(tc, path, "ERROR");
            return;
        }
        boolean hasOrderBy = tc.sql.toUpperCase().contains("ORDER BY");
        ResultMatrix expectedCmp = hasOrderBy ? expected : expected.sorted();
        ResultMatrix actualCmp = hasOrderBy ? actual : actual.sorted();
        ResultComparator.ComparisonResult comparison = ResultComparator.compare(expectedCmp, actualCmp);
        if (comparison.isMatch())
        {
            report.record(new ParityReport.TestResult(tc.id, category, path, "PASS", tc.sql, rewrittenSql, null, null));
            statusUpdater.recordResult(tc.id, path, "PASS");
            assertNoRegression(tc, path, "PASS");
        }
        else
        {
            report.record(new ParityReport.TestResult(tc.id, category, path, "FAIL", tc.sql, rewrittenSql, null, comparison.getDiffs(), expectedCmp, actualCmp));
            statusUpdater.recordResult(tc.id, path, "FAIL");
            assertNoRegression(tc, path, "FAIL");
        }
    }

    private void assertNoRegression(TestCaseLoader.TestCase tc, String path, String actualStatus)
    {
        String expectedStatus = "TDS".equals(path) ? tc.expected_tds_status : tc.expected_rel_status;
        if (expectedStatus == null || expectedStatus.equals(actualStatus))
        {
            return;
        }
        if ("PASS".equals(expectedStatus) && !"PASS".equals(actualStatus))
        {
            throw new AssertionError("REGRESSION: " + tc.id + "|" + path + " was expected PASS, now " + actualStatus +
                    ". If this is intentional, update the test's expected_" + path.toLowerCase() + "_status in the YAML file.");
        }
        if (!"PASS".equals(expectedStatus) && "PASS".equals(actualStatus) && !Boolean.getBoolean("parity.ignoreFixDetection"))
        {
            throw new AssertionError("FIX DETECTED: " + tc.id + "|" + path + " was expected " + expectedStatus +
                    ", now PASS. Update the test's expected_" + path.toLowerCase() + "_status in the YAML file to PASS.");
        }
    }

    private ResultMatrix executeLegendQuery(String sql) throws Exception
    {
        try (Statement stmt = legendConnection.createStatement();
             ResultSet rs = stmt.executeQuery(sql))
        {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= colCount; i++)
            {
                columnNames.add(meta.getColumnLabel(i));
            }
            List<List<Object>> rows = new ArrayList<>();
            while (rs.next())
            {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= colCount; i++)
                {
                    row.add(rs.getObject(i));
                }
                rows.add(row);
            }
            return new ResultMatrix(columnNames, rows);
        }
    }

    private static void registerVault()
    {
        java.util.Properties vaultProps = new java.util.Properties();
        vaultProps.put("e2e.user", postgres.getUsername());
        vaultProps.put("e2e.password", postgres.getPassword());
        Vault.INSTANCE.registerImplementation(new PropertiesVaultImplementation(vaultProps));
    }

    /**
     * Starts a Dropwizard 1.3.x ResourceTestRule in a JUnit 5 context.
     * ResourceTestRule is a JUnit 4 @Rule; its lifecycle must be driven reflectively.
     */
    private static void startResourceTestRule(ResourceTestRule rule) throws Exception
    {
        java.lang.reflect.Field resourceField = ResourceTestRule.class.getDeclaredField("resource");
        resourceField.setAccessible(true);
        Object resource = resourceField.get(rule);
        resource.getClass().getMethod("before").invoke(resource);
    }

    private static void stopResourceTestRule(ResourceTestRule rule)
    {
        if (rule == null)
        {
            return;
        }
        try
        {
            java.lang.reflect.Field resourceField = ResourceTestRule.class.getDeclaredField("resource");
            resourceField.setAccessible(true);
            Object resource = resourceField.get(rule);
            resource.getClass().getMethod("after").invoke(resource);
        }
        catch (Exception e)
        {
            LOGGER.debug("Error stopping ResourceTestRule", e);
        }
    }

    private static void generateFunctionCoverageReport()
    {
        try
        {
            List<TestCaseLoader.TestCase> allTestCases = new ArrayList<>();
            for (String testFile : TEST_FILES)
            {
                TestCaseLoader.TestFile file = TestCaseLoader.load(testFile);
                if (file.tests != null)
                {
                    allTestCases.addAll(file.tests);
                }
            }
            if (functionCatalog != null && !functionCatalog.isEmpty())
            {
                FunctionCoverageMapper mapper = new FunctionCoverageMapper();
                mapper.mapCoverage(functionCatalog, new File("target/parity-report.json"), allTestCases);
                FunctionCoverageReport coverageReport = new FunctionCoverageReport();
                coverageReport.generate(functionCatalog, "target", new File("target/parity-report.json"));
            }
            else
            {
                LOGGER.warn("No function catalog available, skipping function coverage report");
            }
            StructuralParityReport structuralReport = new StructuralParityReport();
            structuralReport.generate(allTestCases, new File("target/parity-report.json"), "target");

            // Failure detail report (full result set comparisons)
            FailureDetailReport failureDetailReport = new FailureDetailReport();
            failureDetailReport.generate(report.getResults(), "target");

            // Generate HTML versions of the markdown reports
            new HtmlReportGenerator().generateAll("target");
        }
        catch (Exception e)
        {
            LOGGER.error("Failed to generate coverage reports", e);
        }
    }
}
