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
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deletestrategy.DeleteAllStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.DeleteTargetData;
import org.finos.legend.engine.persistence.components.ingestmode.emptyhandling.NoOp;
import org.finos.legend.engine.persistence.components.ingestmode.partitioning.Partitioning;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.NoVersioningStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.util.MetadataDataset;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.finos.legend.engine.persistence.components.TestUtils.*;

class UnitemporalSnapshotWithBatchIdTest extends BaseTest
{
    private final String basePathForInput = "src/test/resources/data/unitemporal-snapshot-milestoning/input/batch_id_based/";
    private final String basePathForExpected = "src/test/resources/data/unitemporal-snapshot-milestoning/expected/batch_id_based/";

    private static final String suffixForDeletePartitionTable = "_deleted_partitions";

    /*
    Scenario: Test milestoning Logic without Partition when staging table pre populated
    Empty batch handling - DeleteTargetData
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicWithoutPartition() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();
        MetadataDataset metadataDataset = TestUtils.getMetadataDataset();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .emptyDatasetHandling(DeleteTargetData.builder().build())
            .deduplicationStrategy(FailOnDuplicates.builder().build())
            .versioningStrategy(NoVersioningStrategy.builder().failOnDuplicatePrimaryKeys(true).build())
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
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
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
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);

        // ------------ Perform unitemporal snapshot milestoning Pass3 (Empty Batch) ------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "without_partition/expected_pass3.csv";
        // 1. Load Staging table
        loadBasicStagingData(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 4);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic with Partition when staging table pre populated
    Empty Batch Handling : DeleteTargetData
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicWithPartition() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getEntityPriceIdBasedMainTable();
        DatasetDefinition stagingTable = TestUtils.getEntityPriceStagingTable();
        MetadataDataset metadataDataset = TestUtils.getMetadataDataset();

        String[] schema = new String[]{dateName, entityName, priceName, volumeName, digestName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Collections.singletonList(dateName)).build())
            .emptyDatasetHandling(DeleteTargetData.builder().build())
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
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform unitemporal snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "with_partition/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_partition/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataForWithPartition(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 2, 1, 1);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);

        // ------------ Perform unitemporal snapshot milestoning Pass3 (Empty Batch) ------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_partition/expected_pass3.csv";
        // 1. Load Staging table
        loadStagingDataForWithPartition(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic with Partition filters when staging table pre populated
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicWithPartitionFilter() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getEntityPriceIdBasedMainTable();
        DatasetDefinition stagingTable = TestUtils.getEntityPriceStagingTable();
        MetadataDataset metadataDataset = TestUtils.getMetadataDataset();

        String[] schema = new String[]{dateName, entityName, priceName, volumeName, digestName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Collections.singletonList(dateName)).putAllPartitionValuesByField(partitionFilter).build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_partition_filter/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_partition_filter/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForWithPartition(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(6, 0, 6, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform unitemporal snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "with_partition_filter/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_partition_filter/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataForWithPartition(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 2, 1, 4);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);


        // ------------ Perform unitemporal snapshot milestoning Pass3 (Empty Batch - No Op) ------------------------
        IngestMode ingestModeWithNoOpBatchHandling = ingestMode.withEmptyDatasetHandling(NoOp.builder().build());

        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_partition_filter/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForWithPartition(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestModeWithNoOpBatchHandling, options, datasets, schema, expectedDataPass3, expectedStats);

        // ------------ Perform unitemporal snapshot milestoning Pass3 (Empty Batch - Delete target Data) ------------------------
        IngestMode ingestModeWithDeleteTargetData = ingestMode.withEmptyDatasetHandling(DeleteTargetData.builder().build());
        dataPass3 = "src/test/resources/data/empty_file.csv";
        expectedDataPass3 = basePathForExpected + "with_partition_filter/expected_pass3.csv";
        // 1. Load Staging table
        loadStagingDataForWithPartition(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 3);
        executePlansAndVerifyResults(ingestModeWithDeleteTargetData, options, datasets, schema, expectedDataPass3, expectedStats);
    }

    @Test
    void testUnitemporalSnapshotMilestoningLogicWithMultiplePartitionValues() throws Exception
    {
        DatasetDefinition mainTable = DatasetDefinition.builder()
                .group(testSchemaName).name(mainTableName)
                .schema(SchemaDefinition.builder()
                        .addFields(date)
                        .addFields(accountNum)
                        .addFields(dimension)
                        .addFields(balance)
                        .addFields(digest)
                        .addFields(batchIdIn)
                        .addFields(batchIdOut)
                        .build()).build();

        DatasetDefinition stagingTable = DatasetDefinition.builder()
                .group(testSchemaName).name(stagingTableName)
                .schema(SchemaDefinition.builder()
                        .addFields(date)
                        .addFields(accountNum)
                        .addFields(dimension)
                        .addFields(balance)
                        .addFields(digest)
                        .build()).build();

        String[] schema = new String[]{dateName, accountNumName, dimensionName, balanceName, digestName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);


        List<Map<String, Object>> partitionSpecList = new ArrayList<>();
        addPartitionSpec(partitionSpecList, "2024-01-01", "ACCOUNT_1");
        addPartitionSpec(partitionSpecList, "2024-01-01", "ACCOUNT_2");
        addPartitionSpec(partitionSpecList, "2024-01-02", "ACCOUNT_1");
        addPartitionSpec(partitionSpecList, "2024-01-02", "ACCOUNT_2");
        addPartitionSpec(partitionSpecList, "2024-01-03", "ACCOUNT_1");
        addPartitionSpec(partitionSpecList, "2024-01-03", "ACCOUNT_2");

        Partitioning partition = Partitioning.builder().addAllPartitionFields(Arrays.asList(dateName, accountNumName)).addAllPartitionSpecList(partitionSpecList).build();

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .build())
                .partitioningStrategy(partition)
                .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_multi_values_partition/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_multi_values_partition/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForWithMultiPartition(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(15, 0, 15, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform unitemporal snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "with_multi_values_partition/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_multi_values_partition/expected_pass2.csv";


        partitionSpecList = new ArrayList<>();
        addPartitionSpec(partitionSpecList, "2024-01-01", "ACCOUNT_1");
        addPartitionSpec(partitionSpecList, "2024-01-01", "ACCOUNT_3");
        addPartitionSpec(partitionSpecList, "2024-01-02", "ACCOUNT_2");
        addPartitionSpec(partitionSpecList, "2024-01-04", "ACCOUNT_1");
        ingestMode = ingestMode.withPartitioningStrategy(partition.withPartitionSpecList(partitionSpecList));

        // 1. Load staging table
        loadStagingDataForWithMultiPartition(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(5, 0, 2, 2, 3);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);

        // ------------ Perform unitemporal snapshot milestoning Pass3 (Empty Batch - No Op) ------------------------
        IngestMode ingestModeWithNoOpBatchHandling = ingestMode.withEmptyDatasetHandling(NoOp.builder().build());

        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_multi_values_partition/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForWithMultiPartition(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestModeWithNoOpBatchHandling, options, datasets, schema, expectedDataPass3, expectedStats);

        // ------------ Perform unitemporal snapshot milestoning Pass3 (Empty Batch - Delete target Data) ------------------------

        partitionSpecList = new ArrayList<>();
        addPartitionSpec(partitionSpecList, "2024-01-01", "ACCOUNT_1");
        addPartitionSpec(partitionSpecList, "2024-01-02", "ACCOUNT_2");
        IngestMode ingestModeWithDeleteTargetData = ingestMode.withPartitioningStrategy(partition.withPartitionSpecList(partitionSpecList)).withEmptyDatasetHandling(DeleteTargetData.builder().build());
        dataPass3 = "src/test/resources/data/empty_file.csv";
        expectedDataPass3 = basePathForExpected + "with_multi_values_partition/expected_pass3.csv";
        // 1. Load Staging table
        loadStagingDataForWithMultiPartition(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 3);
        executePlansAndVerifyResults(ingestModeWithDeleteTargetData, options, datasets, schema, expectedDataPass3, expectedStats);
    }

    @Test
    void testUnitemporalSnapshotMilestoningLogicWithDerivedPartitionSpec() throws Exception
    {
        DatasetDefinition mainTable = DatasetDefinition.builder()
                .group(testSchemaName).name(mainTableName)
                .schema(SchemaDefinition.builder()
                        .addFields(date)
                        .addFields(accountNum)
                        .addFields(dimension)
                        .addFields(balance)
                        .addFields(digest)
                        .addFields(batchIdIn)
                        .addFields(batchIdOut)
                        .build()).build();

        DatasetDefinition stagingTable = DatasetDefinition.builder()
                .group(testSchemaName).name(stagingTableName)
                .schema(SchemaDefinition.builder()
                        .addFields(date)
                        .addFields(accountNum)
                        .addFields(dimension)
                        .addFields(balance)
                        .addFields(digest)
                        .build()).build();

        String[] schema = new String[]{dateName, accountNumName, dimensionName, balanceName, digestName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .build())
                .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Arrays.asList(dateName, accountNumName)).derivePartitionSpec(true).build())
                .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_multi_values_partition/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_multi_values_partition/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForWithMultiPartition(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(15, 0, 15, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform unitemporal snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "with_multi_values_partition/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_multi_values_partition/expected_pass2.csv";

        // 1. Load staging table
        loadStagingDataForWithMultiPartition(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(5, 0, 2, 2, 3);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);

        // ------------ Perform unitemporal snapshot milestoning Pass3 (Empty Batch - No Op) ------------------------
        IngestMode ingestModeWithNoOpBatchHandling = ingestMode.withEmptyDatasetHandling(NoOp.builder().build());

        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_multi_values_partition/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForWithMultiPartition(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestModeWithNoOpBatchHandling, options, datasets, schema, expectedDataPass3, expectedStats);

        // ------------ Perform unitemporal snapshot milestoning Pass 3 (Delete Partition file) ------------------------

        IngestMode ingestModeWithDerivePartition = ingestMode.withEmptyDatasetHandling(DeleteTargetData.builder().build());

        String dataPass4 = "src/test/resources/data/empty_file.csv";
        String deletePartitionFile = basePathForInput + "with_multi_values_partition/delete_partition.csv";
        String expectedDataPass4 = basePathForExpected + "with_multi_values_partition/expected_pass3.csv";

        DatasetDefinition deletePartitionMainDataset = DatasetDefinition.builder()
                .database(testDatabaseName)
                .group(testSchemaName)
                .name(mainTableName + suffixForDeletePartitionTable)
                .schema(SchemaDefinition.builder()
                        .addFields(dateNonPk)
                        .addFields(accountNum)
                        .addFields(batchId)
                        .build())
                .build();

        datasets = datasets.withDeletePartitionDataset(deletePartitionMainDataset);

        // 1. Create and Load delete partition table
        createStagingTable(deletePartitionMainDataset);
        loadDeletePartitionDataWithMultiPartitionKeys(deletePartitionFile);

        // 1. Load Staging table
        loadStagingDataForWithMultiPartition(dataPass4);

        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 3);
        executePlansAndVerifyResults(ingestModeWithDerivePartition, options, datasets, schema, expectedDataPass4, expectedStats);
    }


    private static void addPartitionSpec(List<Map<String, Object>> partitionSpecList, String date, String accountNum)
    {
        partitionSpecList.add(new HashMap<String,Object>()
        {
            {
            put(dateName, date);
            put(accountNumName, accountNum);
            }
        });
    }

    /*
    Scenario: Test milestoning Logic when staging data comes from CSV and has less columns than main dataset
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicWithLessColumnsInStaging() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalIdBasedMainTable();
        String dataPass1 = basePathForInput + "less_columns_in_staging/staging_data_pass1.csv";
        Dataset stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass1);
        MetadataDataset metadataDataset = TestUtils.getMetadataDataset();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName};

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning Pass1 ------------------------
        String expectedDataPass1 = basePathForExpected + "less_columns_in_staging/expected_pass1.csv";
        // Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform unitemporal snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "less_columns_in_staging/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "less_columns_in_staging/expected_pass2.csv";
        stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass2);

        // Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass2, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging table is pre populated and
    staging table is cleaned up in the end
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicWithPartitionWithCleanStagingData() throws Exception
    {

        DatasetDefinition mainTable = TestUtils.getEntityPriceIdBasedMainTable();
        DatasetDefinition stagingTable = TestUtils.getEntityPriceStagingTable();
        MetadataDataset metadataDataset = TestUtils.getMetadataDataset();

        String[] schema = new String[]{dateName, entityName, priceName, volumeName, digestName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .partitioningStrategy(Partitioning.builder().addAllPartitionFields(Collections.singletonList(dateName)).build())
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
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 0);
    }


    /*
    Scenario: Test milestoning Logic without Partition and delete all strategy when staging table pre populated
    Empty batch handling - DeleteTargetData
    */
    @Test
    void testUnitemporalSnapshotMilestoningLogicWithoutPartitionAndDeleteAll() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalSnapshot ingestMode = UnitemporalSnapshot.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .build())
                .emptyDatasetHandling(DeleteTargetData.builder().build())
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .deleteStrategy(DeleteAllStrategy.builder().build())
                .versioningStrategy(NoVersioningStrategy.builder().failOnDuplicatePrimaryKeys(true).build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform unitemporal snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "without_partition_delete_all/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "without_partition_delete_all/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform unitemporal snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "without_partition_delete_all/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "without_partition_delete_all/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(3, 0, 1, 2, 1);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);

        // ------------ Perform unitemporal snapshot milestoning Pass3 (Empty Batch) ------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "without_partition_delete_all/expected_pass3.csv";
        // 1. Load Staging table
        loadBasicStagingData(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 3);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);
    }
}
