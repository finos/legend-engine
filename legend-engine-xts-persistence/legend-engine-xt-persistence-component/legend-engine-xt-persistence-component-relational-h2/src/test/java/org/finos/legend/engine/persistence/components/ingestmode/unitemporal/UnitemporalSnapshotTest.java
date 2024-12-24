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
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.FailEmptyBatch;
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.NoOp;
import org.finos.legend.engine.persistence.components.ingestmode.partitioning.Partitioning;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.DigestBasedResolver;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.NoVersioningStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FilteredDataset;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.util.DeleteStrategy;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.TestUtils.*;

class UnitemporalSnapshotTest extends BaseTest
{
    private final String basePathForInput = "src/test/resources/data/unitemporal-snapshot-milestoning/input/batch_id_and_time_based/";
    private final String basePathForExpected = "src/test/resources/data/unitemporal-snapshot-milestoning/expected/batch_id_and_time_based/";

    /*
    Scenario: Test milestoning Logic without Partition when staging table pre populated
    Empty batch handling - default
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicWithoutPartition() throws Exception
    {
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

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "without_partition/no_version/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "without_partition/no_version/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform unitemporal snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "without_partition/no_version/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "without_partition/no_version/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform unitemporal snapshot milestoning Pass3 (Empty Batch) ------------------------

        options = options.withCleanupStagingData(true);

        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "without_partition/no_version/expected_pass3.csv";
        // 1. Load Staging table
        loadBasicStagingData(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 4);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_01);
    }

    @Test
    void testUnitemporalSnapshotMilestoningLogicWithoutPartitionWithCaseConversion() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        String[] schema = new String[]{idName.toUpperCase(), nameName.toUpperCase(), incomeName.toUpperCase(), startTimeName.toUpperCase(), expiryDateName.toUpperCase(), digestName.toUpperCase(), batchIdInName.toUpperCase(), batchIdOutName.toUpperCase()};

        // Create staging table
        h2Sink.executeStatement("CREATE TABLE IF NOT EXISTS \"TEST\".\"STAGING\"(\"ID\" INTEGER NOT NULL,\"NAME\" VARCHAR(64) NOT NULL,\"INCOME\" BIGINT,\"START_TIME\" TIMESTAMP NOT NULL,\"EXPIRY_DATE\" DATE,\"DIGEST\" VARCHAR,PRIMARY KEY (\"ID\", \"START_TIME\"))");

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .dateTimeInName(batchTimeInName)
                        .dateTimeOutName(batchTimeOutName)
                        .build())
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .versioningStrategy(NoVersioningStrategy.builder().failOnDuplicatePrimaryKeys(true).build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "without_partition/no_version/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "without_partition/no_version/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingDataInUpperCase(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"STAGING\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform unitemporal snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "without_partition/no_version/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "without_partition/no_version/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingDataInUpperCase(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 1, 1, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform unitemporal snapshot milestoning Pass3 (Empty Batch) Empty Data Handling = Fail ------------------------
        UnitemporalSnapshot ingestModeWithFailOnEmptyBatchStrategy = UnitemporalSnapshot.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .dateTimeInName(batchTimeInName)
                        .dateTimeOutName(batchTimeOutName)
                        .build())
                .emptyDatasetHandling(FailEmptyBatch.builder().build())
                .build();

        options = options.withCleanupStagingData(true);

        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "without_partition/no_version/expected_pass3.csv";
        // 1. Load Staging table
        loadBasicStagingDataInUpperCase(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 4);
        try
        {
            executePlansAndVerifyForCaseConversion(ingestModeWithFailOnEmptyBatchStrategy, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_01);
            Assertions.fail("Exception should be thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Encountered an Empty Batch, FailEmptyBatch is enabled, so failing the batch!", e.getMessage());
        }

        // ------------ Perform unitemporal snapshot milestoning Pass5 (Empty Batch) Empty Data Handling = Skip ------------------------
        UnitemporalSnapshot ingestModeWithSkipEmptyBatchStrategy = UnitemporalSnapshot.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .dateTimeInName(batchTimeInName)
                        .dateTimeOutName(batchTimeOutName)
                        .build())
                .emptyDatasetHandling(NoOp.builder().build())
                .build();

        options = options.withCleanupStagingData(true);

        dataPass3 = "src/test/resources/data/empty_file.csv";
        expectedDataPass3 = basePathForExpected + "without_partition/no_version/expected_pass2.csv";
        // 1. Load Staging table
        loadBasicStagingDataInUpperCase(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestModeWithSkipEmptyBatchStrategy, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_01);


        // ------------ Perform unitemporal snapshot milestoning Pass6 (Empty Batch) Empty Data Handling = Skip ------------------------
        options = options.withCleanupStagingData(true);

        dataPass3 = "src/test/resources/data/empty_file.csv";
        expectedDataPass3 = basePathForExpected + "without_partition/no_version/expected_pass4.csv";
        // 1. Load Staging table
        loadBasicStagingDataInUpperCase(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 4);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_01);
    }


    /*
    Scenario: Test milestoning Logic with Partition when staging table pre populated
    Empty Batch Handling : Default
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicWithPartition() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getEntityPriceMainTable();
        DatasetDefinition stagingTable = TestUtils.getEntityPriceStagingTable();
        MetadataDataset metadataDataset = TestUtils.getMetadataDataset();

        String[] schema = new String[]{dateName, entityName, priceName, volumeName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

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
            .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Collections.singletonList(dateName)).build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_partition/no_version/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_partition/no_version/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForWithPartition(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(6, 0, 6, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform unitemporal snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "with_partition/no_version/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_partition/no_version/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataForWithPartition(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(3, 0, 1, 1, 1);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform unitemporal snapshot milestoning Pass3 (Empty Batch) ------------------------

        options = options.withCleanupStagingData(true);

        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_partition/no_version/expected_pass3.csv";
        // 1. Load Staging table
        loadStagingDataForWithPartition(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_01);
    }

    /*
    Scenario: Test milestoning Logic when staging data comes from CSV and has less columns than main dataset
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicWithLessColumnsInStaging() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalMainTable();
        String dataPass1 = basePathForInput + "less_columns_in_staging/staging_data_pass1.csv";
        Dataset stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass1);
        MetadataDataset metadataDataset = TestUtils.getMetadataDataset();
        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning Pass1 ------------------------
        String expectedDataPass1 = basePathForExpected + "less_columns_in_staging/expected_pass1.csv";
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform unitemporal snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "less_columns_in_staging/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "less_columns_in_staging/expected_pass2.csv";
        stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass2);

        // Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);
    }

    /*
    Scenario: Test milestoning Logic with Partition when staging table pre populated
    Staging table should be cleaned up after the test
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicWithPartitionWithCleanStagingDataWithoutStatCollection() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getEntityPriceMainTable();
        DatasetDefinition stagingTable = TestUtils.getEntityPriceStagingTable();
        MetadataDataset metadataDataset = TestUtils.getMetadataDataset();

        String[] schema = new String[]{dateName, entityName, priceName, volumeName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

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
            .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Collections.singletonList(dateName)).build())
            .build();

        PlannerOptions options = PlannerOptions.builder().build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning With Clean Staging Table ------------------------
        String dataPass1 = basePathForInput + "with_partition/no_version/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_partition/no_version/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForWithPartition(dataPass1);
        // 2. Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, new HashMap<>(), fixedClock_2000_01_01);
        // 3. Assert that the staging table is truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 0);
    }

    @Test
    void testUnitemporalSnapshotMilestoningLogicWithPartitionWithoutCleanStagingDataWithStatCollection() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getEntityPriceMainTable();
        DatasetDefinition stagingTable = TestUtils.getEntityPriceStagingTable();
        MetadataDataset metadataDataset = TestUtils.getMetadataDataset();

        String[] schema = new String[]{dateName, entityName, priceName, volumeName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

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
            .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Collections.singletonList(dateName)).build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning With Clean Staging Table ------------------------
        String dataPass1 = basePathForInput + "with_partition/no_version/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_partition/no_version/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForWithPartition(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(6, 0, 6, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
        // 3. Assert that the staging table is truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 6);
    }

    /*
    Scenario: Test milestoning Logic with max version and without Partition when staging table pre populated
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicMaxVersionWithoutPartitionAllowDuplicates() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithNonPkVersion();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTableWithoutPks(stagingTable);

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(DigestBasedResolver.builder().build())
                .performStageVersioning(true)
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "without_partition/max_version/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "without_partition/max_version/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(6, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 6);

        // ------------ Perform unitemporal snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "without_partition/max_version/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "without_partition/max_version/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 1, 2, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform unitemporal snapshot milestoning Pass3 (Empty Batch) ------------------------
        options = options.withCleanupStagingData(true);
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "without_partition/max_version/expected_pass3.csv";
        // 1. Load Staging table
        loadStagingDataWithVersion(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 4);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_01);
    }

    /*
    Scenario: Test milestoning Logic with max version and with Partition when staging table pre populated
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicMaxVersionWithPartitionFilterDuplicates() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getEntityPriceWithVersionStagingTable();

        String[] schema = new String[]{dateName, entityName, priceName, volumeName, digestName, versionName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTableWithoutPks(stagingTable);

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Collections.singletonList(dateName)).build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(DigestBasedResolver.builder().build())
                .performStageVersioning(true)
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_partition/max_version/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_partition/max_version/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForWithPartitionWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(9, 0, 6, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform unitemporal snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "with_partition/max_version/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_partition/max_version/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataForWithPartitionWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 1, 1, 1);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform unitemporal snapshot milestoning Pass3 (Empty Batch) ------------------------
        options = options.withCleanupStagingData(true);
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_partition/max_version/expected_pass3.csv";
        // 1. Load Staging table
        loadStagingDataForWithPartitionWithVersion(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_01);
    }

    /*
    Scenario: Test milestoning Logic with max version and with Partition when staging table pre populated
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicMaxVersionWithPartitionFilterDuplicatesWithFilteredDataset() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        FilteredDataset stagingTable = TestUtils.getEntityPriceWithVersionFilteredStagingTable();

        String[] schema = new String[]{dateName, entityName, priceName, volumeName, digestName, versionName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTableWithoutPks(TestUtils.getEntityPriceWithVersionStagingTable());

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Collections.singletonList(dateName)).build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(DigestBasedResolver.builder().build())
                .performStageVersioning(true)
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).cleanupStagingData(false).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_staging_filter/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_staging_filter/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForWithPartitionWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(6, 0, 4, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform unitemporal snapshot milestoning Pass2 ------------------------
        // 0. Create new filter
        datasets = Datasets.of(mainTable, TestUtils.getEntityPriceWithVersionFilteredStagingTableSecondPass());
        String dataPass2 = basePathForInput + "with_staging_filter/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_staging_filter/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataForWithPartitionWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1, 0, 1, 0, 2);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform unitemporal snapshot milestoning Pass3 (Empty Batch) ------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_staging_filter/expected_pass3.csv";
        // 1. Load Staging table
        loadStagingDataForWithPartitionWithVersion(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_01);
    }

    /*
    Scenario: Test milestoning Logic with max version and with Partition when staging table pre populated with upper case
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicMaxVersionWithPartitionFilterDuplicatesUpperCase() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getEntityPriceWithVersionStagingTable();

        String[] schema = new String[]{dateName.toUpperCase(), entityName.toUpperCase(), priceName.toUpperCase(), volumeName.toUpperCase(), digestName.toUpperCase(), versionName.toUpperCase(), batchIdInName.toUpperCase(), batchIdOutName.toUpperCase(), batchTimeInName.toUpperCase(), batchTimeOutName.toUpperCase()};

        // Create staging table
        h2Sink.executeStatement("CREATE TABLE IF NOT EXISTS \"TEST\".\"STAGING\"(\"DATE\" DATE NOT NULL,\"ENTITY\" VARCHAR NOT NULL,\"PRICE\" DECIMAL(20,2),\"VOLUME\" BIGINT,\"DIGEST\" VARCHAR,\"VERSION\" INTEGER)");

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Collections.singletonList(dateName)).build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(DigestBasedResolver.builder().build())
                .performStageVersioning(true)
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_partition/max_version/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_partition/max_version/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForWithPartitionWithVersionInUpperCase(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(9, 0, 6, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform unitemporal snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "with_partition/max_version/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_partition/max_version/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataForWithPartitionWithVersionInUpperCase(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 1, 1, 1);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform unitemporal snapshot milestoning Pass3 (Empty Batch) ------------------------
        options = options.withCleanupStagingData(true);
        String dataPass3 = "src/test/resources/data/empty_file.csv";;
        String expectedDataPass3 = basePathForExpected + "with_partition/max_version/expected_pass3.csv";
        // 1. Load Staging table
        loadStagingDataForWithPartitionWithVersionInUpperCase(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_01);
    }

    /*
    Scenario: Test milestoning Logic with Partition, without digest when staging table pre populated
    Empty Batch Handling : Default
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicWithPartitionNoDigest() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getEntityPriceMainTableWithoutDigest();
        DatasetDefinition stagingTable = TestUtils.getEntityPriceStagingTableWithoutDigest();
        MetadataDataset metadataDataset = TestUtils.getMetadataDataset();

        String[] schema = new String[]{dateName, entityName, priceName, volumeName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .transactionMilestoning(BatchIdAndDateTime.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .dateTimeInName(batchTimeInName)
                        .dateTimeOutName(batchTimeOutName)
                        .build())
                .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Collections.singletonList(dateName)).deleteStrategy(DeleteStrategy.DELETE_ALL).build())
                .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_partition/no_version_no_digest/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_partition/no_version_no_digest/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForWithPartitionWithoutDigest(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(6, 0, 6, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform unitemporal snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "with_partition/no_version_no_digest/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_partition/no_version_no_digest/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataForWithPartitionWithoutDigest(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(3, 0, 1, 2, 1);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform unitemporal snapshot milestoning Pass3 (Empty Batch) ------------------------

        options = options.withCleanupStagingData(true);

        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_partition/no_version_no_digest/expected_pass3.csv";
        // 1. Load Staging table
        loadStagingDataForWithPartitionWithoutDigest(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_01);
    }
}
