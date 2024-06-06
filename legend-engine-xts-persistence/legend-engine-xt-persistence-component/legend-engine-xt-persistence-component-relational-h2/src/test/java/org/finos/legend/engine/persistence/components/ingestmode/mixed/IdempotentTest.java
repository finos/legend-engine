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

package org.finos.legend.engine.persistence.components.ingestmode.mixed;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.TestUtils.*;

public class IdempotentTest extends BaseTest
{
    @Test
    public void testIdempotentRequestWithPerformFullIngestion() throws Exception
    {
        String basePathForInput = "src/test/resources/data/unitemporal-incremental-milestoning/input/batch_id_and_time_based/";
        String basePathForExpected = "src/test/resources/data/unitemporal-incremental-milestoning/expected/batch_id_and_time_based/";
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTable(stagingTable);

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalIngestor ingestor = RelationalIngestor.builder()
                .ingestMode(getUnitemporalDelta())
                .relationalSink(H2Sink.get())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(true)
                .collectStatistics(true)
                .enableSchemaEvolution(false)
                .enableConcurrentSafety(true)
                .writeStatistics(true)
                .ingestRequestId("REQUEST1")
                .enableIdempotencyCheck(true)
                .build();

        // Pass 1
        String dataPass1 = basePathForInput + "without_delete_ind/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "without_delete_ind/expected_pass1.csv";
        loadBasicStagingData(dataPass1);
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        IngestorResult result = ingestor.performFullIngestion(JdbcConnection.of(h2Sink.connection()), datasets).get(0);
        verifyResults(1, schema, expectedDataPass1, "main", result, expectedStats, false);

        // Pass 1: duplicate Request
        result = ingestor.performFullIngestion(JdbcConnection.of(h2Sink.connection()), datasets).get(0);
        verifyResults(1, schema, expectedDataPass1, "main", result, expectedStats, true);

        // Pass 2
        String dataPass2 = basePathForInput + "without_delete_ind/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "without_delete_ind/expected_pass2.csv";
        loadBasicStagingData(dataPass2);
        expectedStats = createExpectedStatsMap(3, 0, 1, 1, 0);
        ingestor = ingestor.withIngestRequestId("REQUEST2");
        result = ingestor.performFullIngestion(JdbcConnection.of(h2Sink.connection()), datasets).get(0);
        verifyResults(2, schema, expectedDataPass2, "main", result, expectedStats, false);

        // Pass 2: duplicate request
        result = ingestor.performFullIngestion(JdbcConnection.of(h2Sink.connection()), datasets).get(0);
        verifyResults(2, schema, expectedDataPass2, "main", result, expectedStats, true);
    }

    @Test
    public void testIdempotentRequestWithIngestApi() throws Exception
    {
        String basePathForInput = "src/test/resources/data/unitemporal-snapshot-milestoning/input/batch_id_and_time_based/";
        String basePathForExpected = "src/test/resources/data/unitemporal-snapshot-milestoning/expected/batch_id_and_time_based/";
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();
        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .dateTimeInName(batchTimeInName)
                        .dateTimeOutName(batchTimeOutName)
                        .build())
                .build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        RelationalIngestor ingestor = RelationalIngestor.builder()
                .ingestMode(getUnitemporalDelta())
                .relationalSink(H2Sink.get())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(true)
                .collectStatistics(true)
                .enableSchemaEvolution(false)
                .enableConcurrentSafety(true)
                .writeStatistics(true)
                .ingestRequestId("REQUEST1")
                .enableIdempotencyCheck(true)
                .build();

        // Pass 1
        String dataPass1 = basePathForInput + "without_partition/no_version/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "without_partition/no_version/expected_pass1.csv";
        loadBasicStagingData(dataPass1);
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        IngestorResult result = ingest(ingestor, datasets);
        verifyResults(1, schema, expectedDataPass1, "main", result, expectedStats, false);

        // Pass 1: duplicate Request
        result = ingest(ingestor, datasets);
        verifyResults(1, schema, expectedDataPass1, "main", result, expectedStats, true);

        // Pass 2
        String dataPass2 = basePathForInput + "without_partition/no_version/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "without_partition/no_version/expected_pass2.csv";
        loadBasicStagingData(dataPass2);
        expectedStats = createExpectedStatsMap(4, 0, 1, 1, 0);
        ingestor = ingestor.withIngestRequestId("REQUEST2");
        result = ingest(ingestor, datasets);
        verifyResults(2, schema, expectedDataPass2, "main", result, expectedStats, false);

        // Pass 2: duplicate request
        result = ingest(ingestor, datasets);
        verifyResults(2, schema, expectedDataPass2, "main", result, expectedStats, true);
    }

    private IngestorResult ingest(RelationalIngestor ingestor, Datasets datasets)
    {
        Executor executor = ingestor.initExecutor(JdbcConnection.of(h2Sink.connection()));
        ingestor.initDatasets(datasets);
        ingestor.create();
        ingestor.evolve();
        executor.begin();
        IngestorResult result = ingestor.ingest().get(0);
        // Do more stuff if needed
        executor.commit();
        return result;
    }


    @Test
    public void testIdempotentRequestMissingIngestRequestId()
    {
        try
        {
            RelationalIngestor ingestor = RelationalIngestor.builder()
                    .ingestMode(getUnitemporalDelta())
                    .relationalSink(H2Sink.get())
                    .executionTimestampClock(fixedClock_2000_01_01)
                    .cleanupStagingData(true)
                    .collectStatistics(true)
                    .enableSchemaEvolution(false)
                    .enableConcurrentSafety(true)
                    .writeStatistics(true)
                    .enableIdempotencyCheck(true)
                    .build();
            Assertions.fail("Should not happen");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("If IdempotencyCheck is enabled, concurrentSafety must be enabled and IngestRequestId must be present", e.getMessage());
        }
    }

    @Test
    public void testIdempotentRequestConcurrentSafetyNotEnabledValidationFailure()
    {
        try
        {
            RelationalIngestor ingestor = RelationalIngestor.builder()
                    .ingestMode(getUnitemporalDelta())
                    .relationalSink(H2Sink.get())
                    .executionTimestampClock(fixedClock_2000_01_01)
                    .cleanupStagingData(true)
                    .collectStatistics(true)
                    .enableSchemaEvolution(false)
                    .writeStatistics(true)
                    .ingestRequestId("123456789")
                    .enableIdempotencyCheck(true)
                    .build();
            Assertions.fail("Should not happen");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("If IdempotencyCheck is enabled, concurrentSafety must be enabled and IngestRequestId must be present", e.getMessage());
        }
    }

    private static UnitemporalDelta getUnitemporalDelta()
    {
        UnitemporalDelta unitemporalDelta = UnitemporalDelta.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .dateTimeInName(batchTimeInName)
                        .dateTimeOutName(batchTimeOutName)
                        .build())
                .build();
        return unitemporalDelta;
    }

    public static void verifyResults(int batchId, String[] schema, String expectedDataPath, String tableName, IngestorResult result, Map<String, Object> expectedStats, boolean previouslyProcessed) throws IOException
    {
        Assertions.assertEquals(batchId, result.batchId().get());
        Assertions.assertEquals("2000-01-01 00:00:00.000000", result.ingestionTimestampUTC());
        List<Map<String, Object>> tableData = h2Sink.executeQuery(String.format("select * from \"TEST\".\"%s\"", tableName));
        TestUtils.assertFileAndTableDataEquals(schema, expectedDataPath, tableData);
        Map<StatisticName, Object> actualStats = result.statisticByName();

        String statsStoredStr = (String) h2Sink.executeQuery("select * from \"batch_metadata\" where \"table_batch_id\" = select max(\"table_batch_id\") from \"batch_metadata\"").get(0).get("batch_statistics");
        Map<String, Object> statsStored = new ObjectMapper().readValue(statsStoredStr, Map.class);

        for (String statistic : expectedStats.keySet())
        {
            Assertions.assertEquals(expectedStats.get(statistic).toString(), actualStats.get(StatisticName.valueOf(statistic)).toString());
            // Assert expected stats and saved stats
            Assertions.assertEquals(expectedStats.get(statistic).toString(), statsStored.get(StatisticName.valueOf(statistic).toString()).toString());
        }

        Assertions.assertEquals(previouslyProcessed, result.previouslyProcessed());
    }


}
