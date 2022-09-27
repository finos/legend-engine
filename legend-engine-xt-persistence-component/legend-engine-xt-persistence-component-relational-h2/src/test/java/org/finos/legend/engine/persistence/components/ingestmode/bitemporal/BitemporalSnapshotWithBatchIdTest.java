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

package org.finos.legend.engine.persistence.components.ingestmode.bitemporal;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.TestUtils.batchIdInName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchIdOutName;
import static org.finos.legend.engine.persistence.components.TestUtils.dateInName;
import static org.finos.legend.engine.persistence.components.TestUtils.dateOutName;
import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.key1Name;
import static org.finos.legend.engine.persistence.components.TestUtils.key2Name;
import static org.finos.legend.engine.persistence.components.TestUtils.lakeFromName;
import static org.finos.legend.engine.persistence.components.TestUtils.lakeThroughName;
import static org.finos.legend.engine.persistence.components.TestUtils.partitionFilter;
import static org.finos.legend.engine.persistence.components.TestUtils.valueName;

// todo: stats collection is turned off for now
@Disabled
class BitemporalSnapshotWithBatchIdTest extends BaseTest
{
    private final String basePathForInput = "src/test/resources/data/bitemporal-snapshot-milestoning/input/batch_id_based/";
    private final String basePathForExpected = "src/test/resources/data/bitemporal-snapshot-milestoning/expected/batch_id_based/";

    /*
    Scenario: Test milestoning Logic without Partition when staging table pre populated
    */
    @Test
    void testBitemporalSnapshotMilestoningLogicWithoutPartition() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBitemporalMainTable();
        DatasetDefinition stagingTable = TestUtils.getBitemporalStagingTable();

        String[] schema = new String[]{key1Name, key2Name, valueName, dateInName, dateOutName, digestName, batchIdInName, batchIdOutName, lakeFromName, lakeThroughName};

        // Create staging table
        createStagingTable(stagingTable);
        BitemporalSnapshot ingestMode = BitemporalSnapshot.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(lakeFromName)
                .dateTimeThruName(lakeThroughName)
                .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                    .sourceDateTimeFromField(dateInName)
                    .sourceDateTimeThruField(dateOutName)
                    .build())
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(false).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "without_partition/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "without_partition/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemp(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 5);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "without_partition/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "without_partition/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForBitemp(dataPass2);
        // 2. Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);

        // ------------ Perform Pass3 ------------------------
        String dataPass3 = basePathForInput + "without_partition/staging_data_pass3.csv";
        String expectedDataPass3 = basePathForExpected + "without_partition/expected_pass3.csv";
        // 1. Load Staging table
        loadStagingDataForBitemp(dataPass3);
        // 2. Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);

        // ------------ Perform bitemporal snapshot milestoning Pass4 (Empty Batch) ------------------------
        String dataPass4 = basePathForInput + "without_partition/staging_data_pass4.csv";
        String expectedDataPass4 = basePathForExpected + "without_partition/expected_pass4.csv";
        // 1. Load Staging table
        loadStagingDataForBitemp(dataPass4);
        // 2. Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass4, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic without Partition with only validity from time specified when staging table pre populated
    */
    @Test
    void testBitemporalSnapshotMilestoningLogicHasFromTimeOnly() throws Exception
    {

        DatasetDefinition mainTable = TestUtils.getBitemporalFromTimeOnlyMainTable();
        DatasetDefinition stagingTable = TestUtils.getBitemporalFromTimeOnlyStagingTable();

        String[] schema = new String[]{key1Name, key2Name, valueName, dateInName, digestName, batchIdInName, batchIdOutName, lakeFromName, lakeThroughName};

        // Create staging table
        createStagingTable(stagingTable);

        BitemporalSnapshot ingestMode = BitemporalSnapshot.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(lakeFromName)
                .dateTimeThruName(lakeThroughName)
                .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                    .sourceDateTimeFromField(dateInName)
                    .sourceDateTimeThruField(dateOutName)
                    .build())
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(false).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "has_from_time_only/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "has_from_time_only/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitempValidityFromTimeOnly(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "has_from_time_only/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "has_from_time_only/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForBitempValidityFromTimeOnly(dataPass2);
        // 2. Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);

        // ------------ Perform Pass3 ------------------------
        String dataPass3 = basePathForInput + "has_from_time_only/staging_data_pass3.csv";
        String expectedDataPass3 = basePathForExpected + "has_from_time_only/expected_pass3.csv";
        // 1. Load Staging table
        loadStagingDataForBitempValidityFromTimeOnly(dataPass3);
        // 2. Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);

        // ------------ Perform bitemporal snapshot milestoning Pass4 (Empty Batch) ------------------------
        String dataPass4 = basePathForInput + "has_from_time_only/staging_data_pass4.csv";
        String expectedDataPass4 = basePathForExpected + "has_from_time_only/expected_pass4.csv";
        // 1. Load Staging table
        loadStagingDataForBitemp(dataPass4);
        // 2. Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass4, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic with Partition when staging table pre populated
    */
    @Test
    void testBitemporalSnapshotMilestoningLogicWithPartition() throws Exception
    {

        DatasetDefinition mainTable = TestUtils.getBitemporalMainTable();
        DatasetDefinition stagingTable = TestUtils.getBitemporalStagingTable();

        String[] schema = new String[]{key1Name, key2Name, valueName, dateInName, dateOutName, digestName, batchIdInName, batchIdOutName, lakeFromName, lakeThroughName};

        // Create staging table
        createStagingTable(stagingTable);

        BitemporalSnapshot ingestMode = BitemporalSnapshot.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(lakeFromName)
                .dateTimeThruName(lakeThroughName)
                .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                    .sourceDateTimeFromField(dateInName)
                    .sourceDateTimeThruField(dateOutName)
                    .build())
                .build())
            .addAllPartitionFields(Arrays.asList(key1Name, key2Name))
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(false).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_partition/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_partition/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemp(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "with_partition/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_partition/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForBitemp(dataPass2);
        // 2. Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);

        // ------------ Perform Pass3 ------------------------
        String dataPass3 = basePathForInput + "with_partition/staging_data_pass3.csv";
        String expectedDataPass3 = basePathForExpected + "with_partition/expected_pass3.csv";
        // 1. Load Staging table
        loadStagingDataForBitemp(dataPass3);
        // 2. Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);

        // ------------ Perform bitemporal snapshot milestoning Pass4 (Empty Batch) ------------------------
        String dataPass4 = basePathForInput + "with_partition/staging_data_pass4.csv";
        String expectedDataPass4 = basePathForExpected + "with_partition/expected_pass4.csv";
        // 1. Load Staging table
        loadStagingDataForBitemp(dataPass4);
        // 2. Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass4, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging data comes from CSV and has less columns than main dataset
    */
    @Test
    void testBitemporalSnapshotMilestoningLogicWithLessColumnsInStaging() throws Exception
    {

        DatasetDefinition mainTable = TestUtils.getBitemporalMainTable();
        String dataPass1 = basePathForInput + "less_columns_in_staging/staging_data_pass1.csv";
        Dataset stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMainForBitemp(dataPass1);

        String[] schema = new String[]{key1Name, key2Name, valueName, dateInName, dateOutName, digestName, batchIdInName, batchIdOutName, lakeFromName, lakeThroughName};

        BitemporalSnapshot ingestMode = BitemporalSnapshot.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(lakeFromName)
                .dateTimeThruName(lakeThroughName)
                .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                    .sourceDateTimeFromField(dateInName)
                    .sourceDateTimeThruField(dateOutName)
                    .build())
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(false).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String expectedDataPass1 = basePathForExpected + "less_columns_in_staging/expected_pass1.csv";
        // Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "less_columns_in_staging/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "less_columns_in_staging/expected_pass2.csv";
        stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMainForBitemp(dataPass2);
        // Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass2, expectedStats);

        // ------------ Perform Pass3 ------------------------
        String dataPass3 = basePathForInput + "less_columns_in_staging/staging_data_pass3.csv";
        String expectedDataPass3 = basePathForExpected + "less_columns_in_staging/expected_pass3.csv";
        stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMainForBitemp(dataPass3);
        // Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass3, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging table is pre populated and staging table is cleaned up in the end
    */
    @Test
    void testBitemporalSnapshotMilestoningLogicWithPartitionWithcleanStagingData() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBitemporalMainTable();
        DatasetDefinition stagingTable = TestUtils.getBitemporalStagingTable();

        String[] schema = new String[]{key1Name, key2Name, valueName, dateInName, dateOutName, digestName, batchIdInName, batchIdOutName, lakeFromName, lakeThroughName};

        // Create staging table
        createStagingTable(stagingTable);

        BitemporalSnapshot ingestMode = BitemporalSnapshot.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(lakeFromName)
                .dateTimeThruName(lakeThroughName)
                .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                    .sourceDateTimeFromField(dateInName)
                    .sourceDateTimeThruField(dateOutName)
                    .build())
                .build())
            .addAllPartitionFields(Arrays.asList(key1Name, key2Name))
            .putAllPartitionValuesByField(partitionFilter)
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_partition/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_partition/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemp(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 0);
    }
}
