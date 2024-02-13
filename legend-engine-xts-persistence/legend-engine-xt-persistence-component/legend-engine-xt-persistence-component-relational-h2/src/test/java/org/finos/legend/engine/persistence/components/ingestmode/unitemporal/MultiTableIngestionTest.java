// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.ingestmode.unitemporal;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.executor.Executor;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.TestUtils.*;

public class MultiTableIngestionTest extends BaseTest
{

    private final String basePathForInput = "src/test/resources/data/unitemporal-incremental-milestoning/input/batch_id_and_time_based/";
    private final String basePathForExpected = "src/test/resources/data/unitemporal-incremental-milestoning/expected/batch_id_and_time_based/";

    String[] datsetSchema1 = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

    String[] datsetSchema2 = new String[]{idName, nameName, ratingName, startTimeName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

    private SchemaDefinition stagingDataset2Schema =
            SchemaDefinition.builder()
                 .addFields(id)
                 .addFields(name)
                .addFields(rating)
                .addFields(startTime)
                .addFields(digest)
                .build();

    DatasetDefinition stagingTable1 = DatasetDefinition.builder()
            .group(testSchemaName)
            .name("staging1")
            .schema(getStagingSchema())
            .build();

    DatasetDefinition stagingTable2 = DatasetDefinition.builder()
            .group(testSchemaName)
            .name("staging2")
            .schema(stagingDataset2Schema)
            .build();

    Dataset mainTable1 = DatasetDefinition.builder()
            .group(testSchemaName)
            .name("main1")
            .schema(SchemaDefinition.builder().build())
            .build();

    Dataset mainTable2 = DatasetDefinition.builder()
            .group(testSchemaName)
            .name("main2")
            .schema(SchemaDefinition.builder().build())
            .build();

    @Test
    public void testMultiTableIngestionSuccessCase() throws Exception
    {
        // Create staging tables
        createStagingTable(stagingTable1);
        createStagingTable(stagingTable2);

        Datasets datasets1 = Datasets.of(mainTable1, stagingTable1);
        Datasets datasets2 = Datasets.of(mainTable2, stagingTable2);

        UnitemporalDelta ingestMode = org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .dateTimeInName(batchTimeInName)
                        .dateTimeOutName(batchTimeOutName)
                        .build())
                .build();
        RelationalIngestor ingestor = RelationalIngestor.builder()
                .ingestMode(ingestMode)
                .relationalSink(H2Sink.get())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(true)
                .collectStatistics(true)
                .enableSchemaEvolution(false)
                .build();

        Executor executor = ingestor.initExecutor(JdbcConnection.of(h2Sink.connection()));

        // Create Main tables
        ingestor.initDatasets(datasets1);
        ingestor.create();

        ingestor.initDatasets(datasets2);
        ingestor.create();

        // Pass 1:
        String dataset1Path = basePathForInput + "multi_table_ingestion/staging_dataset1_pass1.csv";
        String dataset2Path = basePathForInput + "multi_table_ingestion/staging_dataset2_pass1.csv";
        String expectedDataset1Path = basePathForExpected + "multi_table_ingestion/expected_dataset1_pass1.csv";
        String expectedDataset2Path = basePathForExpected + "multi_table_ingestion/expected_dataset2_pass1.csv";
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);

        loadStagingDataset1(dataset1Path);
        loadStagingDataset2(dataset2Path);

        List<IngestorResult> result = ingestMultiTables(executor, ingestor, datasets1, datasets2);
        verifyResults(1, datsetSchema1, expectedDataset1Path, "main1", result.get(0), expectedStats);
        verifyResults(1, datsetSchema2, expectedDataset2Path, "main2", result.get(1), expectedStats);


        // Pass 2:
       dataset1Path = basePathForInput + "multi_table_ingestion/staging_dataset1_pass2.csv";
       dataset2Path = basePathForInput + "multi_table_ingestion/staging_dataset2_pass2.csv";
       expectedDataset1Path = basePathForExpected + "multi_table_ingestion/expected_dataset1_pass2.csv";
       expectedDataset2Path = basePathForExpected + "multi_table_ingestion/expected_dataset2_pass2.csv";
       expectedStats = createExpectedStatsMap(3, 0, 1, 1, 0);

       loadStagingDataset1(dataset1Path);
       loadStagingDataset2(dataset2Path);

       result = ingestMultiTables(executor, ingestor, datasets1, datasets2);
       verifyResults(2, datsetSchema1, expectedDataset1Path, "main1", result.get(0), expectedStats);
       verifyResults(2, datsetSchema2, expectedDataset2Path, "main2", result.get(1), expectedStats);

        // Pass 3:
        dataset1Path = "src/test/resources/data/empty_file.csv";
        dataset2Path = "src/test/resources/data/empty_file.csv";
        expectedDataset1Path = basePathForExpected + "multi_table_ingestion/expected_dataset1_pass3.csv";
        expectedDataset2Path = basePathForExpected + "multi_table_ingestion/expected_dataset2_pass3.csv";
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);

        loadStagingDataset1(dataset1Path);
        loadStagingDataset2(dataset2Path);

        result = ingestMultiTables(executor, ingestor, datasets1, datasets2);
        verifyResults(3, datsetSchema1, expectedDataset1Path, "main1", result.get(0), expectedStats);
        verifyResults(3, datsetSchema2, expectedDataset2Path, "main2", result.get(1), expectedStats);

        // Verify if additional query data was written
        List<Map<String, Object>> tableData = h2Sink.executeQuery(String.format("select * from batch_metadata where table_name = 'new_test_table'"));
        Assertions.assertEquals(3, tableData.size());
    }

    private List<IngestorResult> ingestMultiTables(Executor executor, RelationalIngestor ingestor, Datasets... allDatasets)
    {
        List<IngestorResult> multiTableIngestionResult = new ArrayList<>();
        try
        {
            executor.begin();
            for (Datasets datasets: allDatasets)
            {
                ingestor.initDatasets(datasets);
                IngestorResult result = ingestor.ingest().get(0);
                multiTableIngestionResult.add(result);
            }

            // Show how we can add more to same tx
            executor.getRelationalExecutionHelper().executeStatement("insert into batch_metadata(table_name, batch_status) values ('new_test_table', 'test_tx')");
            executor.commit();
        }
        catch (Exception e)
        {
            executor.revert();
            throw e;
        }
        finally
        {
            executor.close();
        }
        return multiTableIngestionResult;
    }

    @Test
    public void testMultiTableIngestionWithFailedTx() throws Exception
    {
        // Create staging tables
        createStagingTable(stagingTable1);
        createStagingTable(stagingTable2);

        Datasets datasets1 = Datasets.of(mainTable1, stagingTable1);
        Datasets datasets2 = Datasets.of(mainTable2, stagingTable2);

        UnitemporalDelta ingestMode = org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .dateTimeInName(batchTimeInName)
                        .dateTimeOutName(batchTimeOutName)
                        .build())
                .build();
        RelationalIngestor ingestor = RelationalIngestor.builder()
                .ingestMode(ingestMode)
                .relationalSink(H2Sink.get())
                .executionTimestampClock(fixedClock_2000_01_01)
                .cleanupStagingData(true)
                .collectStatistics(true)
                .enableSchemaEvolution(false)
                .build();

        Executor executor = ingestor.initExecutor(JdbcConnection.of(h2Sink.connection()));

        // Create Main tables
        ingestor.initDatasets(datasets1);
        ingestor.create();
        ingestor.initDatasets(datasets2);
        ingestor.create();

        // Pass 1:
        String dataset1Path = basePathForInput + "multi_table_ingestion/staging_dataset1_pass1.csv";
        String dataset2Path = basePathForInput + "multi_table_ingestion/staging_dataset2_pass1.csv";

        loadStagingDataset1(dataset1Path);
        loadStagingDataset2(dataset2Path);
        try
        {
            List<IngestorResult> result = ingestMultiTablesWithBadQuery(executor, ingestor, datasets1, datasets2);
            Assertions.fail("Should not reach here");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e.getMessage().contains("Column \"unknown_column\" not found"));
            // Verify that no data was written in the two datasets
            List<Map<String, Object>> tableData1 = h2Sink.executeQuery(String.format("select * from \"TEST\".\"main1\""));
            List<Map<String, Object>> tableData2 = h2Sink.executeQuery(String.format("select * from \"TEST\".\"main2\""));
            Assertions.assertTrue(tableData1.isEmpty());
            Assertions.assertTrue(tableData2.isEmpty());
        }
    }


        private List<IngestorResult> ingestMultiTablesWithBadQuery(Executor executor, RelationalIngestor ingestor, Datasets... allDatasets)
    {
        List<IngestorResult> multiTableIngestionResult = new ArrayList<>();
        try
        {
            executor.begin();
            for (Datasets datasets: allDatasets)
            {
                ingestor.initDatasets(datasets);
                IngestorResult result = ingestor.ingest().get(0);
                multiTableIngestionResult.add(result);
            }

            // A bad query should revert the complete transaction
            executor.getRelationalExecutionHelper().executeStatement("insert into batch_metadata(table_name, batch_status, unknown_column) values ('new_test_table', 'test_tx', 'XYZ')");
            executor.commit();
        }
        catch (Exception e)
        {
            executor.revert();
            throw e;
        }
        finally
        {
            executor.close();
        }
        return multiTableIngestionResult;
    }


    private void loadStagingDataset1(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging1\";" +
                "INSERT INTO \"TEST\".\"staging1\"(id, name, income, start_time ,expiry_date, digest) " +
                "SELECT CONVERT( \"id\",INT ), \"name\", CONVERT( \"income\", BIGINT), CONVERT( \"start_time\", DATETIME), CONVERT( \"expiry_date\", DATE), digest" +
                " FROM CSVREAD( '" + path + "', 'id, name, income, start_time, expiry_date, digest', NULL )";
        h2Sink.executeStatement(loadSql);
    }

    private void loadStagingDataset2(String path) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE \"TEST\".\"staging2\";" +
                "INSERT INTO \"TEST\".\"staging2\"(id, name, rating, start_time , digest) " +
                "SELECT CONVERT( \"id\",INT ), \"name\", CONVERT( \"rating\", INT), CONVERT( \"start_time\", DATETIME), digest" +
                " FROM CSVREAD( '" + path + "', 'id, name, rating, start_time, digest', NULL )";
        h2Sink.executeStatement(loadSql);
    }


    public static void verifyResults(int batchId, String[] schema, String expectedDataPath, String tableName, IngestorResult result, Map<String, Object> expectedStats) throws IOException
    {
        Assertions.assertEquals(batchId, result.batchId().get());
        Assertions.assertEquals("2000-01-01 00:00:00.000000", result.ingestionTimestampUTC());
        List<Map<String, Object>> tableData = h2Sink.executeQuery(String.format("select * from \"TEST\".\"%s\"", tableName));
        TestUtils.assertFileAndTableDataEquals(schema, expectedDataPath, tableData);
        Map<StatisticName, Object> actualStats = result.statisticByName();
        for (String statistic : expectedStats.keySet())
        {
            Assertions.assertEquals(expectedStats.get(statistic).toString(), actualStats.get(StatisticName.valueOf(statistic)).toString());
        }
    }


}
