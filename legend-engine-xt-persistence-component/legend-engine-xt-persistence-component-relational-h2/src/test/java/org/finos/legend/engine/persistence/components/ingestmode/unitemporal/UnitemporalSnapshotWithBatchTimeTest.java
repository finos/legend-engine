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

package org.finos.legend.engine.persistence.components.ingestmode.unitemporal;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.TransactionDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.TestUtils.batchTimeInName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchTimeOutName;
import static org.finos.legend.engine.persistence.components.TestUtils.closePriceName;
import static org.finos.legend.engine.persistence.components.TestUtils.dateName;
import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.TestUtils.startTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.tickerName;
import static org.finos.legend.engine.persistence.components.TestUtils.volumeName;

class UnitemporalSnapshotWithBatchTimeTest extends BaseTest
{
    private final String basePathForInput = "src/test/resources/data/unitemporal-snapshot-milestoning/input/time_based/";
    private final String basePathForExpected = "src/test/resources/data/unitemporal-snapshot-milestoning/expected/time_based/";

    /*
    Scenario: Test milestoning Logic without Partition when staging table pre populated
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicWithoutPartition() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalTimeBasedMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();
        MetadataDataset metadataDataset = TestUtils.getMetadataDataset();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestName)
            .addAllKeyFields(Arrays.asList(idName, startTimeName))
            .transactionMilestoning(TransactionDateTime.builder()
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "without_partition/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "without_partition/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform unitemporal snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "without_partition/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "without_partition/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_02);

        // ------------ Perform unitemporal snapshot milestoning Pass3 (Empty Batch) ------------------------
        String dataPass3 = basePathForInput + "without_partition/staging_data_pass3.csv";
        String expectedDataPass3 = basePathForExpected + "without_partition/expected_pass3.csv";
        // 1. Load Staging table
        loadBasicStagingData(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 4);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_03);
    }

    /*
    Scenario: Test milestoning Logic with Partition when staging table pre populated
    This test case is adapted from
    https://confluence.site.gs.com/display/DIO/Alloy+Streaming+-+Milestoning+Schemes#AlloyStreamingMilestoningSchemes-Withpartitioning
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicWithPartition() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getTickerPriceTimeBasedMainTable();
        DatasetDefinition stagingTable = TestUtils.getTickerPriceStagingTable();
        MetadataDataset metadataDataset = TestUtils.getMetadataDataset();

        String[] schema = new String[]{dateName, tickerName, closePriceName, volumeName, digestName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestName)
            .addAllKeyFields(Arrays.asList(dateName, tickerName))
            .transactionMilestoning(TransactionDateTime.builder()
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .addAllPartitionFields(Collections.singletonList(dateName))
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_partition/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_partition/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForWithPartition(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(6, 0, 6, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform unitemporal snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "with_partition/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_partition/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataForWithPartition(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(3, 0, 1, 1, 1);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_02);

        // ------------ Perform unitemporal snapshot milestoning Pass3 (Empty Batch) ------------------------
        String dataPass3 = basePathForInput + "with_partition/staging_data_pass3.csv";
        String expectedDataPass3 = basePathForExpected + "with_partition/expected_pass3.csv";
        // 1. Load Staging table
        loadStagingDataForWithPartition(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 6);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_03);
    }

    /*
    Scenario: Test milestoning Logic when staging data comes from CSV and has less columns than main dataset
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicWithLessColumnsInStaging() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalTimeBasedMainTable();
        String dataPass1 = basePathForInput + "less_columns_in_staging/staging_data_pass1.csv";
        Dataset stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass1);
        MetadataDataset metadataDataset = TestUtils.getMetadataDataset();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchTimeInName, batchTimeOutName};

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestName)
            .addAllKeyFields(Arrays.asList(idName, startTimeName))
            .transactionMilestoning(TransactionDateTime.builder()
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning Pass1 ------------------------
        String expectedDataPass1 = basePathForExpected + "less_columns_in_staging/expected_pass1.csv";
        // Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform unitemporal snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "less_columns_in_staging/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "less_columns_in_staging/expected_pass2.csv";
        stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass2);

        // Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass2, expectedStats, fixedClock_2000_01_02);
    }

    /*
    Scenario: Test milestoning Logic with Partition when staging table pre populated
    Staging table should be cleaned up after the test
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicWithPartitionWithCleanStagingData() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getTickerPriceTimeBasedMainTable();
        DatasetDefinition stagingTable = TestUtils.getTickerPriceStagingTable();
        MetadataDataset metadataDataset = TestUtils.getMetadataDataset();

        String[] schema = new String[]{dateName, tickerName, closePriceName, volumeName, digestName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestName)
            .addAllKeyFields(Arrays.asList(dateName, tickerName))
            .transactionMilestoning(TransactionDateTime.builder()
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .addAllPartitionFields(Collections.singletonList(dateName))
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(true).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning With Clean Staging Table ------------------------
        String dataPass1 = basePathForInput + "with_partition/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_partition/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForWithPartition(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(6, 0, 6, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
        // 3. Assert that the staging table is truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 0);
    }
}
