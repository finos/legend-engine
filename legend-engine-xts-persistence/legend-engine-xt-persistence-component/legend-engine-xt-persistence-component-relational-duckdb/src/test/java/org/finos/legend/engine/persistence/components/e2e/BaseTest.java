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

package org.finos.legend.engine.persistence.components.e2e;

import org.finos.legend.engine.persistence.components.common.DatasetFilter;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DerivedDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.duckdb.DuckDBSink;
import org.finos.legend.engine.persistence.components.relational.executor.RelationalExecutor;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.util.SchemaEvolutionCapability;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.File;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BaseTest
{
    public static final String TEST_SCHEMA = "TEST";
    public static final String TEST_DATABASE = "TEST_DB";
    protected static final String JDBC_URL = "jdbc:duckdb:" + TEST_DATABASE;
    protected static final String USER_NAME = "sa";
    protected static final String PASSWORD = "";
    public static JdbcHelper duckDBSink;

    protected final ZonedDateTime fixedExecutionZonedDateTime1 = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    protected final Clock fixedClock_2000_01_01 = Clock.fixed(fixedExecutionZonedDateTime1.toInstant(), ZoneOffset.UTC);

    protected ZonedDateTime fixedExecutionZonedDateTime2 = ZonedDateTime.of(2000, 1, 2, 0, 0, 0, 123456000, ZoneOffset.UTC);
    protected Clock fixedClock_2000_01_02 = Clock.fixed(fixedExecutionZonedDateTime2.toInstant(), ZoneOffset.UTC);

    protected final ZonedDateTime fixedExecutionZonedDateTime3 = ZonedDateTime.of(2000, 1, 3, 0, 0, 0, 0, ZoneOffset.UTC);
    protected final Clock fixedClock_2000_01_03 = Clock.fixed(fixedExecutionZonedDateTime3.toInstant(), ZoneOffset.UTC);

    protected RelationalExecutor executor = new RelationalExecutor(DuckDBSink.get(), duckDBSink);

    @BeforeAll
    public static void initialize()
    {
        duckDBSink = JdbcHelper.of(DuckDBSink.createConnection(USER_NAME, PASSWORD, JDBC_URL));
        // Closing connection pool created by other tests.
        duckDBSink.close();
        duckDBSink = JdbcHelper.of(DuckDBSink.createConnection(USER_NAME, PASSWORD, JDBC_URL));
    }

    @AfterAll
    public static void cleanUp()
    {
        duckDBSink.close();
    }

    @BeforeEach
    public void setUp()
    {
        String createSchemaSql = "CREATE SCHEMA IF NOT EXISTS {TEST_SCHEMA_NAME}"
            .replace("{TEST_SCHEMA_NAME}", TEST_SCHEMA);
        duckDBSink.executeStatement(createSchemaSql);
    }

    @AfterEach
    public void tearDown()
    {
        String dropSchemaSql = "DROP SCHEMA \"{TEST_SCHEMA_NAME}\" CASCADE"
            .replace("{TEST_SCHEMA_NAME}", TEST_SCHEMA);
        duckDBSink.executeStatement(dropSchemaSql);

        // This is for tables that we do not specify the schema name
        String dropSchemaSql2 = "DROP TABLE IF EXISTS \"batch_metadata\" CASCADE";
        String dropSchemaSql3 = "DROP TABLE IF EXISTS \"BATCH_METADATA\" CASCADE";
        duckDBSink.executeStatements(Arrays.asList(dropSchemaSql2, dropSchemaSql3));
    }

    protected void createStagingTableWithoutPks(DatasetDefinition stagingTable) throws Exception
    {
        List<Field> fieldsWithoutPk = stagingTable.schema().fields().stream().map(field -> field.withPrimaryKey(false)).collect(Collectors.toList());
        stagingTable = stagingTable.withSchema(stagingTable.schema().withFields(fieldsWithoutPk));
        RelationalTransformer transformer = new RelationalTransformer(DuckDBSink.get());
        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(stagingTable, true);
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        executor.executePhysicalPlan(tableCreationPhysicalPlan);
    }

    protected void createStagingTable(DatasetDefinition stagingTable) throws Exception
    {
        RelationalTransformer transformer = new RelationalTransformer(DuckDBSink.get());
        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(stagingTable, true);
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        executor.executePhysicalPlan(tableCreationPhysicalPlan);
    }

    protected void createStagingTable(DerivedDataset stagingTable) throws Exception
    {
        RelationalTransformer transformer = new RelationalTransformer(DuckDBSink.get());
        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(stagingTable, true);
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        executor.executePhysicalPlan(tableCreationPhysicalPlan);
    }

    protected void createTempTable(DatasetDefinition tempTable) throws Exception
    {
        RelationalTransformer transformer = new RelationalTransformer(DuckDBSink.get());
        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(tempTable, true);
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        executor.executePhysicalPlan(tableCreationPhysicalPlan);
    }

    protected IngestorResult executePlansAndVerifyResults(IngestMode ingestMode, PlannerOptions options, Datasets datasets, String[] schema, String expectedDataPath, Map<String, Object> expectedStats, String orderByClause) throws Exception
    {
        return executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPath, expectedStats, Clock.systemUTC(), orderByClause);
    }

    protected IngestorResult executePlansAndVerifyResults(IngestMode ingestMode, PlannerOptions options, Datasets datasets, String[] schema, String expectedDataPath, Map<String, Object> expectedStats, Set<SchemaEvolutionCapability> userCapabilitySet, Clock executionTimestampClock, String orderByClause) throws Exception
    {
        return executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPath, expectedStats, executionTimestampClock, userCapabilitySet, false, orderByClause);
    }

    private void verifyLatestStagingFilters(RelationalIngestor ingestor, Datasets datasets) throws Exception
    {
        List<DatasetFilter> filters = ingestor.getLatestStagingFilters(JdbcConnection.of(duckDBSink.connection()), datasets);
        DerivedDataset derivedDataset = (DerivedDataset) datasets.stagingDataset();
        List<DatasetFilter> expectedFilters = new ArrayList<>(derivedDataset.datasetFilters());
        Assertions.assertEquals(filters.size(), expectedFilters.size());

        Comparator comparator = Comparator.comparing(DatasetFilter::fieldName).thenComparing(DatasetFilter::filterType);
        Collections.sort(filters, comparator);
        Collections.sort(expectedFilters, comparator);
        for (int i = 0; i < filters.size(); i++)
        {
            Assertions.assertEquals(expectedFilters.get(i).fieldName(), filters.get(i).fieldName());
            Assertions.assertEquals(expectedFilters.get(i).filterType(), filters.get(i).filterType());
            Assertions.assertEquals(expectedFilters.get(i).value(), filters.get(i).value());
        }
    }

    protected IngestorResult executePlansAndVerifyResults(IngestMode ingestMode, PlannerOptions options, Datasets datasets,
                                                          String[] schema, String expectedDataPath, Map<String, Object> expectedStats,
                                                          Clock executionTimestampClock, Set<SchemaEvolutionCapability> userCapabilitySet,
                                                          boolean verifyStagingFilters, String orderByClause) throws Exception
    {
        // Execute physical plans
        RelationalIngestor ingestor = RelationalIngestor.builder()
            .ingestMode(ingestMode)
            .relationalSink(DuckDBSink.get())
            .executionTimestampClock(executionTimestampClock)
            .cleanupStagingData(options.cleanupStagingData())
            .collectStatistics(options.collectStatistics())
            .enableSchemaEvolution(options.enableSchemaEvolution())
            .schemaEvolutionCapabilitySet(userCapabilitySet)
            .enableConcurrentSafety(true)
            .build();
        return executePlansAndVerifyResults(ingestor, datasets, schema, expectedDataPath, expectedStats, verifyStagingFilters, orderByClause);
    }

    protected IngestorResult executePlansAndVerifyResults(RelationalIngestor ingestor, Datasets datasets, String[] schema,
                                                          String expectedDataPath, Map<String, Object> expectedStats,
                                                          boolean verifyStagingFilters, String orderByClause) throws Exception
    {
        // Execute physical plans
        IngestorResult result = ingestor.performFullIngestion(JdbcConnection.of(duckDBSink.connection()), datasets).get(0);

        Map<StatisticName, Object> actualStats = result.statisticByName();

        // Verify the database data
        List<Map<String, Object>> tableData = duckDBSink.executeQuery("select * from \"TEST\".\"main\"" + orderByClause);
        TestUtils.assertFileAndTableDataEquals(schema, expectedDataPath, tableData);

        // Verify statistics
        Assertions.assertEquals(expectedStats.size(), actualStats.size());
        for (String statistic : expectedStats.keySet())
        {
            Assertions.assertEquals(expectedStats.get(statistic).toString(), actualStats.get(StatisticName.valueOf(statistic)).toString());
        }

        // Verify StagingFilters
        if (verifyStagingFilters)
        {
            verifyLatestStagingFilters(ingestor, datasets);
        }

        // Return result (including updated datasets)
        return result;
    }

    protected IngestorResult executePlansAndVerifyResultsWithStagingFilters(IngestMode ingestMode, PlannerOptions options, Datasets datasets, String[] schema, String expectedDataPath, Map<String, Object> expectedStats, Clock executionTimestampClock, String orderByClause) throws Exception
    {
        return executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPath, expectedStats, executionTimestampClock, Collections.emptySet(), true, orderByClause);
    }

    protected IngestorResult executePlansAndVerifyResults(IngestMode ingestMode, PlannerOptions options, Datasets datasets, String[] schema, String expectedDataPath, Map<String, Object> expectedStats, Clock executionTimestampClock, String orderByClause) throws Exception
    {
        return executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPath, expectedStats, executionTimestampClock, Collections.emptySet(), false, orderByClause);
    }

    protected List<IngestorResult> executePlansAndVerifyResultsWithSpecifiedDataSplits(IngestMode ingestMode, PlannerOptions options, Datasets datasets, String[] schema, String expectedDataPath, List<Map<String, Object>> expectedStats, List<DataSplitRange> dataSplitRanges, String orderByClause) throws Exception
    {
        return executePlansAndVerifyResultsWithSpecifiedDataSplits(ingestMode, options, datasets, schema, expectedDataPath, expectedStats, dataSplitRanges, Clock.systemUTC(), orderByClause);
    }

    protected List<IngestorResult> executePlansAndVerifyResultsWithSpecifiedDataSplits(IngestMode ingestMode, PlannerOptions options, Datasets datasets, String[] schema, String expectedDataPath, List<Map<String, Object>> expectedStats, List<DataSplitRange> dataSplitRanges, Clock executionTimestampClock, String orderByClause) throws Exception
    {
        RelationalIngestor ingestor = RelationalIngestor.builder()
            .ingestMode(ingestMode)
            .relationalSink(DuckDBSink.get())
            .executionTimestampClock(executionTimestampClock)
            .cleanupStagingData(options.cleanupStagingData())
            .collectStatistics(options.collectStatistics())
            .enableSchemaEvolution(options.enableSchemaEvolution())
            .build();

        List<IngestorResult> results = ingestor.performFullIngestionWithDataSplits(JdbcConnection.of(duckDBSink.connection()), datasets, dataSplitRanges);

        List<Map<String, Object>> tableData = duckDBSink.executeQuery("select * from \"TEST\".\"main\"" + orderByClause);
        TestUtils.assertFileAndTableDataEquals(schema, expectedDataPath, tableData);

        for (int i = 0; i < results.size(); i++)
        {
            Map<StatisticName, Object> actualStats = results.get(i).statisticByName();
            Assertions.assertEquals(expectedStats.get(i).size(), actualStats.size());
            for (String statistic : expectedStats.get(i).keySet())
            {
                Assertions.assertEquals(expectedStats.get(i).get(statistic).toString(), actualStats.get(StatisticName.valueOf(statistic)).toString());
            }
        }
        return results;
    }

    protected List<IngestorResult> executePlansAndVerifyResultsWithDerivedDataSplits(IngestMode ingestMode, PlannerOptions options, Datasets datasets, String[] schema, String expectedDataPath, List<Map<String, Object>> expectedStats, Clock executionTimestampClock, String orderByClause) throws Exception
    {
        RelationalIngestor ingestor = RelationalIngestor.builder()
            .ingestMode(ingestMode)
            .relationalSink(DuckDBSink.get())
            .executionTimestampClock(executionTimestampClock)
            .cleanupStagingData(options.cleanupStagingData())
            .collectStatistics(options.collectStatistics())
            .enableSchemaEvolution(options.enableSchemaEvolution())
            .build();

        List<IngestorResult> results = ingestor.performFullIngestion(JdbcConnection.of(duckDBSink.connection()), datasets);

        List<Map<String, Object>> tableData = duckDBSink.executeQuery("select * from \"TEST\".\"main\"" + orderByClause);
        TestUtils.assertFileAndTableDataEquals(schema, expectedDataPath, tableData);

        for (int i = 0; i < results.size(); i++)
        {
            Map<StatisticName, Object> actualStats = results.get(i).statisticByName();
            Assertions.assertEquals(expectedStats.get(i).size(), actualStats.size());
            for (String statistic : expectedStats.get(i).keySet())
            {
                Assertions.assertEquals(expectedStats.get(i).get(statistic).toString(), actualStats.get(StatisticName.valueOf(statistic)).toString());
            }
        }
        return results;
    }

    protected Map<String, Object> createExpectedStatsMap(int incomingRecordCount, int rowsDeleted, int rowsInserted, int rowsUpdated, int rowsTerminated)
    {
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), incomingRecordCount);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), rowsDeleted);
        expectedStats.put(StatisticName.ROWS_INSERTED.name(), rowsInserted);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), rowsTerminated);
        expectedStats.put(StatisticName.ROWS_UPDATED.name(), rowsUpdated);
        return expectedStats;
    }

    public IngestorResult executePlansAndVerifyForCaseConversion(IngestMode ingestMode, PlannerOptions options, Datasets datasets, String[] schema, String expectedDataPath, Map<String, Object> expectedStats, String orderByClause) throws Exception
    {
        return executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPath, expectedStats, Clock.systemUTC(), orderByClause);
    }

    public IngestorResult executePlansAndVerifyForCaseConversion(IngestMode ingestMode, PlannerOptions options, Datasets datasets, String[] schema, String expectedDataPath, Map<String, Object> expectedStats, Clock executionTimestampClock, String orderByClause) throws Exception
    {
        RelationalIngestor ingestor = RelationalIngestor.builder()
            .ingestMode(ingestMode)
            .relationalSink(DuckDBSink.get())
            .executionTimestampClock(executionTimestampClock)
            .cleanupStagingData(options.cleanupStagingData())
            .collectStatistics(options.collectStatistics())
            .enableSchemaEvolution(options.enableSchemaEvolution())
            .schemaEvolutionCapabilitySet(Collections.emptySet())
            .caseConversion(CaseConversion.TO_UPPER)
            .build();
        return executePlansAndVerifyForCaseConversion(ingestor, datasets, schema, expectedDataPath, expectedStats, orderByClause);
    }

    public IngestorResult executePlansAndVerifyForCaseConversion(RelationalIngestor ingestor, Datasets datasets, String[] schema, String expectedDataPath, Map<String, Object> expectedStats, String orderByClause) throws Exception
    {
        Executor executor = ingestor.initExecutor(JdbcConnection.of(duckDBSink.connection()));

        ingestor.initDatasets(datasets);
        ingestor.create();
        ingestor.evolve();

        executor.begin();
        IngestorResult result = ingestor.ingest().get(0);
        // Do more stuff if needed
        executor.commit();

        Map<StatisticName, Object> actualStats = result.statisticByName();

        // Verify the database data
        List<Map<String, Object>> tableData = duckDBSink.executeQuery("select * from \"TEST\".\"MAIN\"" + orderByClause);

        TestUtils.assertFileAndTableDataEquals(schema, expectedDataPath, tableData);

        // Verify statistics
        Assertions.assertEquals(expectedStats.size(), actualStats.size());
        for (String statistic : expectedStats.keySet())
        {
            Assertions.assertEquals(expectedStats.get(statistic).toString(), actualStats.get(StatisticName.valueOf(statistic)).toString());
        }

        // Return result (including updated datasets)
        return result;
    }

    protected void loadBasicStagingData(String path)
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "COPY \"TEST\".\"staging\"" +
            "(\"id\", \"name\", \"income\", \"start_time\", \"expiry_date\", \"digest\")" +
            " FROM '" + path + "' CSV";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadBasicStagingDataInUpperCase(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"STAGING\";" +
                "INSERT INTO \"TEST\".\"STAGING\"(ID, NAME, INCOME, START_TIME ,EXPIRY_DATE, DIGEST) " +
                "SELECT CONVERT( \"ID\",INT ), \"NAME\", CONVERT( \"INCOME\", BIGINT), CONVERT( \"START_TIME\", DATETIME), CONVERT( \"EXPIRY_DATE\", DATE), DIGEST" +
                " FROM CSVREAD( '" + path + "', 'ID, NAME, INCOME, START_TIME, EXPIRY_DATE, DIGEST', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadBasicStagingDataWithLessColumnsThanMain(String path)
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "COPY \"TEST\".\"staging\"" +
            "(\"id\", \"name\", \"income\", \"start_time\", \"digest\")" +
            " FROM '" + path + "' CSV";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadBasicStagingDataWithDataSplit(String path)
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "COPY \"TEST\".\"staging\"" +
            "(\"id\", \"name\", \"income\", \"start_time\", \"expiry_date\", \"digest\", \"data_split\")" +
            " FROM '" + path + "' CSV";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataWithNoPk(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(name, income, expiry_date) " +
            "SELECT \"name\", CONVERT( \"income\", BIGINT), CONVERT( \"expiry_date\", DATE)" +
            " FROM CSVREAD( '" + path + "', 'name, income, expiry_date', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataWithNoPkInUpperCase(String path)
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"STAGING\";" +
            "COPY \"TEST\".\"STAGING\"" +
            "(\"NAME\", \"INCOME\", \"EXPIRY_DATE\")" +
            " FROM '" + path + "' CSV";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataForWithPartition(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(date, entity, price, volume, digest) " +
            "SELECT CONVERT( \"date\",DATE ), \"entity\", CONVERT( \"price\", DECIMAL(20,2)), CONVERT( \"volume\", BIGINT), \"digest\"" +
            " FROM CSVREAD( '" + path + "', 'date, entity, price, volume, digest', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataForWithMultiPartition(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
                "INSERT INTO \"TEST\".\"staging\"(date, accountNum, dimension, balance, digest) " +
                "SELECT CONVERT( \"date\",DATE ), \"accountNum\", \"dimension\", CONVERT( \"balance\", BIGINT), \"digest\"" +
                " FROM CSVREAD( '" + path + "', 'date, accountNum, dimension, balance, digest', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataForWithPartitionWithVersion(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(date, entity, price, volume, digest, version) " +
            "SELECT CONVERT( \"date\",DATE ), \"entity\", CONVERT( \"price\", DECIMAL(20,2)), CONVERT( \"volume\", BIGINT), \"digest\", CONVERT( \"version\",INT)" +
            " FROM CSVREAD( '" + path + "', 'date, entity, price, volume, digest, version', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataForWithPartitionWithVersionInUpperCase(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"STAGING\";" +
            "INSERT INTO \"TEST\".\"STAGING\"(DATE, ENTITY, PRICE, VOLUME, DIGEST, VERSION) " +
            "SELECT CONVERT( \"DATE\",DATE ), \"ENTITY\", CONVERT( \"PRICE\", DECIMAL(20,2)), CONVERT( \"VOLUME\", BIGINT), \"DIGEST\", CONVERT( \"VERSION\",INT)" +
            " FROM CSVREAD( '" + path + "', 'DATE, ENTITY, PRICE, VOLUME, DIGEST, VERSION', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataWithDeleteInd(String path)
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "COPY \"TEST\".\"staging\"" +
            "(\"id\", \"name\", \"income\", \"start_time\", \"expiry_date\", \"digest\", \"delete_indicator\")" +
            " FROM '" + path + "' CSV";
        duckDBSink.executeStatement(loadSql);
    }

    public static void loadDedupAndVersioningStagingDataWithoutVersion(String path)
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "COPY \"TEST\".\"staging\"" +
            "(\"id\", \"name\", \"income\", \"expiry_date\", \"digest\")" +
            " FROM '" + path + "' CSV";
        duckDBSink.executeStatement(loadSql);
    }

    public static void loadDedupAndVersioningStagingDataWithVersion(String path)
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "COPY \"TEST\".\"staging\"" +
            "(\"id\", \"name\", \"version\", \"income\", \"expiry_date\", \"digest\")" +
            " FROM '" + path + "' CSV";
        duckDBSink.executeStatement(loadSql);
    }

    public static void loadDedupAndVersioningStagingDataWithVersionAndBatch(String path)
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "COPY \"TEST\".\"staging\"" +
            "(\"id\", \"name\", \"version\", \"income\", \"expiry_date\", \"digest\", \"batch\")" +
            " FROM '" + path + "' CSV";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataWithVersion(String path)
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "COPY \"TEST\".\"staging\"" +
            "(\"id\", \"name\", \"income\", \"start_time\", \"expiry_date\", \"digest\", \"version\")" +
            " FROM '" + path + "' CSV";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataWithVersionInUpperCase(String path)
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"STAGING\";" +
            "COPY \"TEST\".\"STAGING\"" +
            "(\"ID\", \"NAME\", \"INCOME\", \"START_TIME\", \"EXPIRY_DATE\", \"DIGEST\", \"VERSION\")" +
            " FROM '" + path + "' CSV";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataWithVersionWithoutDigest(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(id, name, income, start_time ,expiry_date, version) " +
            "SELECT CONVERT( \"id\",INT ), \"name\", CONVERT( \"income\", BIGINT), CONVERT( \"start_time\", DATETIME), CONVERT( \"expiry_date\", DATE), CONVERT( \"version\",INT)" +
            " FROM CSVREAD( '" + path + "', 'id, name, income, start_time, expiry_date, version', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataWithFilter(String path)
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "COPY \"TEST\".\"staging\"" +
            "(\"id\", \"name\", \"income\", \"start_time\", \"expiry_date\", \"digest\", \"batch\")" +
            " FROM '" + path + "' CSV";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataWithFilterWithVersion(String path)
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "COPY \"TEST\".\"staging\"" +
            "(\"id\", \"name\", \"income\", \"start_time\", \"expiry_date\", \"digest\", \"version\", \"batch\")" +
            " FROM '" + path + "' CSV";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataWithFilterWithVersionInUpperCase(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"STAGING\";" +
            "INSERT INTO \"TEST\".\"STAGING\"(ID, NAME, INCOME, START_TIME ,EXPIRY_DATE, DIGEST, VERSION, BATCH) " +
            "SELECT CONVERT( \"ID\",INT ), \"NAME\", CONVERT( \"INCOME\", BIGINT), CONVERT( \"START_TIME\", DATETIME), CONVERT( \"EXPIRY_DATE\", DATE), DIGEST, CONVERT( \"VERSION\",INT), CONVERT( \"BATCH\",INT)" +
            " FROM CSVREAD( '" + path + "', 'ID, NAME, INCOME, START_TIME, EXPIRY_DATE, DIGEST, VERSION, BATCH', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataForBitemp(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(key1, key2, value1, date_in, date_out, digest) " +
            "SELECT \"key1\", \"key2\", CONVERT( \"value1\", INT), CONVERT( \"date_in\", DATETIME), CONVERT( \"date_out\", DATETIME) ,  \"digest\"" +
            " FROM CSVREAD( '" + path + "', 'key1, key2, value1, date_in, date_out, digest', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataForBitempValidityFromTimeOnly(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(key1, key2, value1, date_in, digest) " +
            "SELECT \"key1\", \"key2\", CONVERT( \"value1\", INT), CONVERT( \"date_in\", DATETIME),  \"digest\"" +
            " FROM CSVREAD( '" + path + "', 'key1, key2, value1, date_in, digest', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataForBitempWithDeleteInd(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(key1, key2, value1, date_in, date_out, digest, delete_indicator) " +
            "SELECT \"key1\", \"key2\", CONVERT( \"value1\", INT), CONVERT( \"date_in\", DATETIME), CONVERT( \"date_out\", DATETIME) ,  \"digest\", CONVERT( \"delete_indicator\", INT)" +
            " FROM CSVREAD( '" + path + "', 'key1, key2, value1, date_in, date_out, digest, delete_indicator', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataForBitemporalFromOnly(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(index, datetime, balance, digest) " +
            "SELECT CONVERT( \"index\", INT), CONVERT( \"datetime\", DATETIME), CONVERT( \"balance\", BIGINT), \"digest\"" +
            " FROM CSVREAD( '" + path + "', 'index, datetime, balance, digest', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataForBitemporalFromOnlyWithUpperCase(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"STAGING\";" +
                "INSERT INTO \"TEST\".\"STAGING\"(INDEX, DATETIME, BALANCE, DIGEST) " +
                "SELECT CONVERT( \"INDEX\", INT), CONVERT( \"DATETIME\", DATETIME), CONVERT( \"BALANCE\", BIGINT), \"DIGEST\"" +
                " FROM CSVREAD( '" + path + "', 'INDEX, DATETIME, BALANCE, DIGEST', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataForBitemporalFromOnlyWithDeleteInd(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(index, datetime, balance, digest, delete_indicator) " +
            "SELECT CONVERT( \"index\", INT), CONVERT( \"datetime\", DATETIME), CONVERT( \"balance\", BIGINT), \"digest\", \"delete_indicator\"" +
            " FROM CSVREAD( '" + path + "', 'index, datetime, balance, digest, delete_indicator', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataForBitemporalFromOnlyWithVersionWithDataSplit(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(index, datetime, balance, digest, version, data_split) " +
            "SELECT CONVERT( \"index\", INT), CONVERT( \"datetime\", DATETIME), CONVERT( \"balance\", BIGINT), \"digest\", CONVERT( \"version\", BIGINT), CONVERT( \"data_split\", BIGINT)" +
            " FROM CSVREAD( '" + path + "', 'index, datetime, balance, digest, version, data_split', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataForBitemporalFromOnlyWithDeleteIndWithVersionWithDataSplit(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(index, datetime, balance, digest, version, delete_indicator, data_split) " +
            "SELECT CONVERT( \"index\", INT), CONVERT( \"datetime\", DATETIME), CONVERT( \"balance\", BIGINT), \"digest\", CONVERT( \"version\", BIGINT), \"delete_indicator\", CONVERT( \"data_split\", BIGINT)" +
            " FROM CSVREAD( '" + path + "', 'index, datetime, balance, digest, version, delete_indicator, data_split', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataForIntIncome(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(id, name, income, start_time ,expiry_date, digest) " +
            "SELECT CONVERT( \"id\",INT ), \"name\", CONVERT( \"income\", INT), CONVERT( \"start_time\", DATETIME), CONVERT( \"expiry_date\", DATE), digest" +
            " FROM CSVREAD( '" + path + "', 'id, name, income, start_time, expiry_date, digest', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataForDecimalIncome(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(id, name, income, start_time ,expiry_date, digest) " +
            "SELECT CONVERT( \"id\",INT ), \"name\", CONVERT( \"income\", DECIMAL), CONVERT( \"start_time\", DATETIME), CONVERT( \"expiry_date\", DATE), digest" +
            " FROM CSVREAD( '" + path + "', 'id, name, income, start_time, expiry_date, digest', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected void loadStagingDataForWithoutName(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(id, income, start_time ,expiry_date, digest) " +
            "SELECT CONVERT( \"id\",INT ), CONVERT( \"income\", BIGINT), CONVERT( \"start_time\", DATETIME), CONVERT( \"expiry_date\", DATE), digest" +
            " FROM CSVREAD( '" + path + "', 'id, income, start_time, expiry_date, digest', NULL )";
        duckDBSink.executeStatement(loadSql);
    }

    protected static void validateFileExists(String path)
    {
        File f = new File(path);
        if (!f.exists())
        {
            throw new IllegalStateException("File does not exist : " + path);
        }
    }
}
