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

package org.finos.legend.engine.persistence.components;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.executor.RelationalExecutor;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcHelper;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseTest
{
    public static final String TEST_SCHEMA = "TEST";
    public static final String TEST_DATABASE = "TEST_DB";
    private static final String H2_JDBC_URL = "jdbc:h2:mem:" + TEST_DATABASE +
        ";DATABASE_TO_UPPER=false;mode=mysql;LOCK_TIMEOUT=10000;BUILTIN_ALIAS_OVERRIDE=TRUE";
    private static final String H2_USER_NAME = "sa";
    private static final String H2_PASSWORD = "";
    public static JdbcHelper h2Sink;

    protected final ZonedDateTime fixedExecutionZonedDateTime1 = ZonedDateTime.of(2000, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
    protected final Clock fixedClock_2000_01_01 = Clock.fixed(fixedExecutionZonedDateTime1.toInstant(), ZoneOffset.UTC);

    protected ZonedDateTime fixedExecutionZonedDateTime2 = ZonedDateTime.of(2000, 1, 2, 0, 0, 0, 0, ZoneOffset.UTC);
    protected Clock fixedClock_2000_01_02 = Clock.fixed(fixedExecutionZonedDateTime2.toInstant(), ZoneOffset.UTC);

    protected final ZonedDateTime fixedExecutionZonedDateTime3 = ZonedDateTime.of(2000, 1, 3, 0, 0, 0, 0, ZoneOffset.UTC);
    protected final Clock fixedClock_2000_01_03 = Clock.fixed(fixedExecutionZonedDateTime3.toInstant(), ZoneOffset.UTC);

    protected RelationalExecutor executor = new RelationalExecutor(H2Sink.get(), h2Sink);

    @BeforeAll
    public static void initialize()
    {
        h2Sink = JdbcHelper.of(H2Sink.createConnection(H2_USER_NAME, H2_PASSWORD, H2_JDBC_URL));
        // Closing connection pool created by other tests.
        h2Sink.close();
        h2Sink = JdbcHelper.of(H2Sink.createConnection(H2_USER_NAME, H2_PASSWORD, H2_JDBC_URL));
    }

    @AfterAll
    public static void cleanUp()
    {
        h2Sink.close();
    }

    @BeforeEach
    public void setUp() throws Exception
    {
        String createSchemaSql = "CREATE SCHEMA IF NOT EXISTS {TEST_SCHEMA_NAME} AUTHORIZATION {USER_NAME}"
            .replace("{TEST_SCHEMA_NAME}", TEST_SCHEMA)
            .replace("{USER_NAME}", H2_USER_NAME);
        h2Sink.executeStatement(createSchemaSql);
    }

    @AfterEach
    public void tearDown() throws Exception
    {
        h2Sink.executeStatement("DROP ALL OBJECTS");
    }

    protected void createStagingTable(DatasetDefinition stagingTable) throws Exception
    {
        RelationalTransformer transformer = new RelationalTransformer(H2Sink.get());
        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(stagingTable, true);
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        executor.executePhysicalPlan(tableCreationPhysicalPlan);
    }

    protected void createTempTable(DatasetDefinition tempTable) throws Exception
    {
        RelationalTransformer transformer = new RelationalTransformer(H2Sink.get());
        LogicalPlan tableCreationPlan = LogicalPlanFactory.getDatasetCreationPlan(tempTable, true);
        SqlPlan tableCreationPhysicalPlan = transformer.generatePhysicalPlan(tableCreationPlan);
        executor.executePhysicalPlan(tableCreationPhysicalPlan);
    }

    protected IngestorResult executePlansAndVerifyResults(IngestMode ingestMode, PlannerOptions options, Datasets datasets, String[] schema, String expectedDataPath, Map<String, Object> expectedStats) throws Exception
    {
        return executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPath, expectedStats, Clock.systemUTC());
    }

    protected IngestorResult executePlansAndVerifyResults(IngestMode ingestMode, PlannerOptions options, Datasets datasets, String[] schema, String expectedDataPath, Map<String, Object> expectedStats, Clock executionTimestampClock) throws Exception
    {
        // Execute physical plans
        RelationalIngestor ingestor = RelationalIngestor.builder()
            .ingestMode(ingestMode)
            .relationalSink(H2Sink.get())
            .executionTimestampClock(executionTimestampClock)
            .cleanupStagingData(options.cleanupStagingData())
            .collectStatistics(options.collectStatistics())
            .enableSchemaEvolution(options.enableSchemaEvolution())
            .build();
        IngestorResult result = ingestor.ingest(h2Sink.connection(), datasets);

        Map<StatisticName, Object> actualStats = result.statisticByName();

        // Verify the database data
        List<Map<String, Object>> tableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
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

    protected List<IngestorResult> executePlansAndVerifyResultsWithDataSplits(IngestMode ingestMode, PlannerOptions options, Datasets datasets, String[] schema, String expectedDataPath, List<Map<String, Object>> expectedStats, List<DataSplitRange> dataSplitRanges) throws Exception
    {
        return executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPath, expectedStats, dataSplitRanges, Clock.systemUTC());
    }

    protected List<IngestorResult> executePlansAndVerifyResultsWithDataSplits(IngestMode ingestMode, PlannerOptions options, Datasets datasets, String[] schema, String expectedDataPath, List<Map<String, Object>> expectedStats, List<DataSplitRange> dataSplitRanges, Clock executionTimestampClock) throws Exception
    {
        RelationalIngestor ingestor = RelationalIngestor.builder()
            .ingestMode(ingestMode)
            .relationalSink(H2Sink.get())
            .executionTimestampClock(executionTimestampClock)
            .cleanupStagingData(options.cleanupStagingData())
            .collectStatistics(options.collectStatistics())
            .enableSchemaEvolution(options.enableSchemaEvolution())
            .build();

        List<IngestorResult> results = ingestor.ingestWithDataSplits(h2Sink.connection(), datasets, dataSplitRanges);

        List<Map<String, Object>> tableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
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

    protected void loadBasicStagingData(String path) throws Exception
    {
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(id, name, income, start_time ,expiry_date, digest) " +
            "SELECT CONVERT( \"id\",INT ), \"name\", CONVERT( \"income\", INT), CONVERT( \"start_time\", DATETIME), CONVERT( \"expiry_date\", DATE), digest" +
            " FROM CSVREAD( '" + path + "', 'id, name, income, start_time, expiry_date, digest', NULL )";
        h2Sink.executeStatement(loadSql);
    }

    protected void loadStagingDataForWithPartition(String path) throws Exception
    {
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(date, ticker, close_price, volume, digest) " +
            "SELECT CONVERT( \"date\",DATE ), \"ticker\", CONVERT( \"close_price\", DECIMAL(20,2)), CONVERT( \"volume\", INT), \"digest\"" +
            " FROM CSVREAD( '" + path + "', 'date, ticker, close_price, volume, digest', NULL )";
        h2Sink.executeStatement(loadSql);
    }

    protected void loadStagingDataWithDeleteInd(String path) throws Exception
    {
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(id, name, income, start_time ,expiry_date, digest, delete_indicator) " +
            "SELECT CONVERT( \"id\",INT ), \"name\", CONVERT( \"income\", INT), CONVERT( \"start_time\", DATETIME), CONVERT( \"expiry_date\", DATE) ,  \"digest\", \"delete_indicator\"" +
            " FROM CSVREAD( '" + path + "', 'id, name, income, start_time, expiry_date, digest, delete_indicator', NULL )";
        h2Sink.executeStatement(loadSql);
    }

    protected void loadStagingDataForBitemp(String path) throws Exception
    {
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(key1, key2, value1, date_in, date_out, digest) " +
            "SELECT \"key1\", \"key2\", CONVERT( \"value1\", INT), CONVERT( \"date_in\", DATETIME), CONVERT( \"date_out\", DATETIME) ,  \"digest\"" +
            " FROM CSVREAD( '" + path + "', 'key1, key2, value1, date_in, date_out, digest', NULL )";
        h2Sink.executeStatement(loadSql);
    }

    protected void loadStagingDataForBitempValidityFromTimeOnly(String path) throws Exception
    {
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(key1, key2, value1, date_in, digest) " +
            "SELECT \"key1\", \"key2\", CONVERT( \"value1\", INT), CONVERT( \"date_in\", DATETIME),  \"digest\"" +
            " FROM CSVREAD( '" + path + "', 'key1, key2, value1, date_in, digest', NULL )";
        h2Sink.executeStatement(loadSql);
    }

    protected void loadStagingDataForBitempWithDeleteInd(String path) throws Exception
    {
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(key1, key2, value1, date_in, date_out, digest, delete_indicator) " +
            "SELECT \"key1\", \"key2\", CONVERT( \"value1\", INT), CONVERT( \"date_in\", DATETIME), CONVERT( \"date_out\", DATETIME) ,  \"digest\", CONVERT( \"delete_indicator\", INT)" +
            " FROM CSVREAD( '" + path + "', 'key1, key2, value1, date_in, date_out, digest, delete_indicator', NULL )";
        h2Sink.executeStatement(loadSql);
    }

    protected void loadStagingDataForLoans(String path) throws Exception
    {
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(gs_loan_id, datetime, balance, digest) " +
            "SELECT CONVERT( \"gs_loan_id\", INT), CONVERT( \"datetime\", DATETIME), CONVERT( \"balance\", BIGINT), \"digest\"" +
            " FROM CSVREAD( '" + path + "', 'gs_loan_id, datetime, balance, digest', NULL )";
        h2Sink.executeStatement(loadSql);
    }

    protected void loadStagingDataForLoansWithDeleteInd(String path) throws Exception
    {
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(gs_loan_id, datetime, balance, digest, delete_indicator) " +
            "SELECT CONVERT( \"gs_loan_id\", INT), CONVERT( \"datetime\", DATETIME), CONVERT( \"balance\", BIGINT), \"digest\", \"delete_indicator\"" +
            " FROM CSVREAD( '" + path + "', 'gs_loan_id, datetime, balance, digest, delete_indicator', NULL )";
        h2Sink.executeStatement(loadSql);
    }

    protected void loadStagingDataForLoansWithDataSplit(String path) throws Exception
    {
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(gs_loan_id, datetime, balance, digest, data_split) " +
            "SELECT CONVERT( \"gs_loan_id\", INT), CONVERT( \"datetime\", DATETIME), CONVERT( \"balance\", BIGINT), \"digest\", CONVERT( \"data_split\", BIGINT)" +
            " FROM CSVREAD( '" + path + "', 'gs_loan_id, datetime, balance, digest, data_split', NULL )";
        h2Sink.executeStatement(loadSql);
    }

    protected void loadStagingDataForLoansWithDeleteIndWithDataSplit(String path) throws Exception
    {
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging\";" +
            "INSERT INTO \"TEST\".\"staging\"(gs_loan_id, datetime, balance, digest, delete_indicator, data_split) " +
            "SELECT CONVERT( \"gs_loan_id\", INT), CONVERT( \"datetime\", DATETIME), CONVERT( \"balance\", BIGINT), \"digest\", \"delete_indicator\", CONVERT( \"data_split\", BIGINT)" +
            " FROM CSVREAD( '" + path + "', 'gs_loan_id, datetime, balance, digest, delete_indicator, data_split', NULL )";
        h2Sink.executeStatement(loadSql);
    }
}
