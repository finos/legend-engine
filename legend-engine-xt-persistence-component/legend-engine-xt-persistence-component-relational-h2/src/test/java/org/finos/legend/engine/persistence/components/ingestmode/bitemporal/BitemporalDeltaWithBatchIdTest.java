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
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.TestUtils.batchIdInName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchIdOutName;
import static org.finos.legend.engine.persistence.components.TestUtils.dataSplitName;
import static org.finos.legend.engine.persistence.components.TestUtils.dateInName;
import static org.finos.legend.engine.persistence.components.TestUtils.dateOutName;
import static org.finos.legend.engine.persistence.components.TestUtils.deleteIndicatorName;
import static org.finos.legend.engine.persistence.components.TestUtils.deleteIndicatorValues;
import static org.finos.legend.engine.persistence.components.TestUtils.deleteIndicatorValuesEdgeCase;
import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.key1Name;
import static org.finos.legend.engine.persistence.components.TestUtils.key2Name;
import static org.finos.legend.engine.persistence.components.TestUtils.fromName;
import static org.finos.legend.engine.persistence.components.TestUtils.throughName;
import static org.finos.legend.engine.persistence.components.TestUtils.balanceName;
import static org.finos.legend.engine.persistence.components.TestUtils.dateTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.endDateTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.indexName;
import static org.finos.legend.engine.persistence.components.TestUtils.startDateTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.valueName;

class BitemporalDeltaWithBatchIdTest extends BaseTest
{
    private final String basePathForInput = "src/test/resources/data/bitemporal-incremental-milestoning/input/batch_id_based/";
    private final String basePathForExpected = "src/test/resources/data/bitemporal-incremental-milestoning/expected/batch_id_based/";

    /*
    Scenario: Test milestoning Logic when staging table pre populated
    */
    @Test
    void testMilestoningSourceSpecifiesFromAndThrough() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBitemporalStagingTable();

        String[] schema = new String[] {key1Name, key2Name, valueName, fromName, throughName, digestName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(fromName)
                .dateTimeThruName(throughName)
                .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                    .sourceDateTimeFromField(dateInName)
                    .sourceDateTimeThruField(dateOutName)
                    .build())
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from_and_through/without_delete_ind/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from_and_through/without_delete_ind/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemp(dataPass1);
        // 2. Execute Plan and Verify Results
        Map<String, Object> expectedStats = createExpectedStatsMap(5, 0, 5, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 5);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from_and_through/without_delete_ind/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "source_specifies_from_and_through/without_delete_ind/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForBitemp(dataPass2);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(4, 0, 0, 2, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = basePathForInput + "source_specifies_from_and_through/without_delete_ind/staging_data_pass3.csv";
        String expectedDataPass3 = basePathForExpected + "source_specifies_from_and_through/without_delete_ind/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataForBitemp(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging table pre populated and delete indicator is present
    */
    @Test
    void testMilestoningSourceSpecifiesFromAndThroughWithDeleteIndicator() throws Exception
    {

        DatasetDefinition mainTable = TestUtils.getBitemporalMainTable();
        DatasetDefinition stagingTable = TestUtils.getBitemporalStagingTableWithDeleteIndicator();

        String[] schema = new String[] {key1Name, key2Name, valueName, fromName, throughName, digestName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(fromName)
                .dateTimeThruName(throughName)
                .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                    .sourceDateTimeFromField(dateInName)
                    .sourceDateTimeThruField(dateOutName)
                    .build())
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorName)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from_and_through/with_delete_ind/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from_and_through/with_delete_ind/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitempWithDeleteInd(dataPass1);
        // 2. Execute Plan and Verify Results
        Map<String, Object> expectedStats = createExpectedStatsMap(5, 0, 5, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from_and_through/with_delete_ind/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "source_specifies_from_and_through/with_delete_ind/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForBitempWithDeleteInd(dataPass2);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(4, 0, 0, 2, 1);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = basePathForInput + "source_specifies_from_and_through/with_delete_ind/staging_data_pass3.csv";
        String expectedDataPass3 = basePathForExpected + "source_specifies_from_and_through/with_delete_ind/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataForBitempWithDeleteInd(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging data comes from CSV and has less columns than main dataset
    */
    @Test
    void testMilestoningSourceSpecifiesFromAndThroughWithLessColumnsInStaging() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBitemporalMainTable();
        String dataPass1 = basePathForInput + "source_specifies_from_and_through/less_columns_in_staging/staging_data_pass1.csv";
        Dataset stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMainForBitemp(dataPass1);

        String[] schema = new String[] {key1Name, key2Name, valueName, fromName, throughName, digestName, batchIdInName, batchIdOutName};

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(fromName)
                .dateTimeThruName(throughName)
                .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                    .sourceDateTimeFromField(dateInName)
                    .sourceDateTimeThruField(dateOutName)
                    .build())
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String expectedDataPass1 = basePathForExpected + "source_specifies_from_and_through/less_columns_in_staging/expected_pass1.csv";
        // Execute Plan and Verify Results
        Map<String, Object> expectedStats = createExpectedStatsMap(5, 0, 5, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from_and_through/less_columns_in_staging/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "source_specifies_from_and_through/less_columns_in_staging/expected_pass2.csv";
        stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMainForBitemp(dataPass2);
        // Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(4, 0, 0, 2, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass2, expectedStats);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = basePathForInput + "source_specifies_from_and_through/less_columns_in_staging/staging_data_pass3.csv";
        String expectedDataPass3 = basePathForExpected + "source_specifies_from_and_through/less_columns_in_staging/expected_pass3.csv";
        stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMainForBitemp(dataPass3);
        // Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass3, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging table is pre populated and staging table is cleaned up in the end
    */
    @Test
    void testMilestoningSourceSpecifiesFromAndThroughWithDeleteIndicatorWithCleanStagingData() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBitemporalStagingTableWithDeleteIndicator();

        String[] schema = new String[] {key1Name, key2Name, valueName, fromName, throughName, digestName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(fromName)
                .dateTimeThruName(throughName)
                .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                    .sourceDateTimeFromField(dateInName)
                    .sourceDateTimeThruField(dateOutName)
                    .build())
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorName)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(true).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from_and_through/with_delete_ind/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from_and_through/with_delete_ind/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitempWithDeleteInd(dataPass1);
        // 2. Execute Plan and Verify Results
        Map<String, Object> expectedStats = createExpectedStatsMap(5, 0, 5, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 0);
    }

    /*
    Scenario: Test milestoning Logic with only validity from time specified when staging table pre populated
    */
    @Test
    void testMilestoningSourceSpecifiesFromSet1() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBitemporalFromOnlyMainTableIdBased();
        DatasetDefinition stagingTable = TestUtils.getBitemporalFromOnlyStagingTableIdBased();
        DatasetDefinition tempTable = TestUtils.getBitemporalFromOnlyTempTableIdBased();

        String[] schema = new String[] {indexName, balanceName, digestName, startDateTimeName, endDateTimeName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);
        // Create temp table
        createTempTable(tempTable);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(startDateTimeName)
                .dateTimeThruName(endDateTimeName)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(dateTimeName)
                    .build())
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).tempDataset(tempTable).build();

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from/without_delete_ind/set_1/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from/without_delete_ind/set_1/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnly(dataPass1);
        // 2. Execute Plan and Verify Results
        Map<String, Object> expectedStats = createExpectedStatsMap(1, 0, 1, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from/without_delete_ind/set_1/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "source_specifies_from/without_delete_ind/set_1/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnly(dataPass2);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(1, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);

        // ------------ Perform Pass3 -------------------------
        String dataPass3 = basePathForInput + "source_specifies_from/without_delete_ind/set_1/staging_data_pass3.csv";
        String expectedDataPass3 = basePathForExpected + "source_specifies_from/without_delete_ind/set_1/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataForBitemporalFromOnly(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);

        // ------------ Perform Pass4 -------------------------
        String dataPass4 = basePathForInput + "source_specifies_from/without_delete_ind/set_1/staging_data_pass4.csv";
        String expectedDataPass4 = basePathForExpected + "source_specifies_from/without_delete_ind/set_1/expected_pass4.csv";
        // 1. Load staging table
        loadStagingDataForBitemporalFromOnly(dataPass4);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass4, expectedStats);

        // ------------ Perform Pass5 -------------------------
        String dataPass5 = basePathForInput + "source_specifies_from/without_delete_ind/set_1/staging_data_pass5.csv";
        String expectedDataPass5 = basePathForExpected + "source_specifies_from/without_delete_ind/set_1/expected_pass5.csv";
        // 1. Load staging table
        loadStagingDataForBitemporalFromOnly(dataPass5);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1, 0, 0, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass5, expectedStats);

        // ------------ Perform Pass6 (identical records) -------------------------
        String dataPass6 = basePathForInput + "source_specifies_from/without_delete_ind/set_1/staging_data_pass6.csv";
        String expectedDataPass6 = basePathForExpected + "source_specifies_from/without_delete_ind/set_1/expected_pass6.csv";
        // 1. Load staging table
        loadStagingDataForBitemporalFromOnly(dataPass6);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1, 0, 0, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass6, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic with only validity from time specified when staging table pre populated and Upper case Optimizer
    */
    @Test
    void testMilestoningSourceSpecifiesFromSet1WithUpperCaseOptimizer() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBitemporalFromOnlyMainTableIdBased();
        DatasetDefinition stagingTable = TestUtils.getBitemporalFromOnlyStagingTableIdBased();

        String[] schema = new String[]{indexName.toUpperCase(), balanceName.toUpperCase(), digestName.toUpperCase(),
                startDateTimeName.toUpperCase(), endDateTimeName.toUpperCase(), batchIdInName.toUpperCase(), batchIdOutName.toUpperCase()};

        // Create staging table
        h2Sink.executeStatement("CREATE TABLE IF NOT EXISTS \"TEST\".\"STAGING\"(\"INDEX\" INTEGER NOT NULL,\"DATETIME\" TIMESTAMP NOT NULL,\"BALANCE\" BIGINT,\"DIGEST\" VARCHAR,PRIMARY KEY (\"INDEX\", \"DATETIME\"))");

        BitemporalDelta ingestMode = BitemporalDelta.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .build())
                .validityMilestoning(ValidDateTime.builder()
                        .dateTimeFromName(startDateTimeName)
                        .dateTimeThruName(endDateTimeName)
                        .validityDerivation(SourceSpecifiesFromDateTime.builder()
                                .sourceDateTimeFromField(dateTimeName)
                                .build())
                        .build())
                .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).build();

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from/without_delete_ind/set_1/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from/without_delete_ind/set_1/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithUpperCase(dataPass1);
        // 2. Execute Plan and Verify Results
        Map<String, Object> expectedStats = createExpectedStatsMap(1, 0, 1, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from/without_delete_ind/set_1/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "source_specifies_from/without_delete_ind/set_1/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithUpperCase(dataPass2);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(1, 0, 1, 1, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);
    }


    /*
    Scenario: Test milestoning Logic with only validity from time specified when staging table pre populated
    */
    @Test
    void testMilestoningSourceSpecifiesFromSet2() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBitemporalFromOnlyStagingTableIdBased();
        DatasetDefinition tempTable = TestUtils.getBitemporalFromOnlyTempTableIdBased();

        String[] schema = new String[] {indexName, balanceName, digestName, startDateTimeName, endDateTimeName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);
        // Create temp table
        createTempTable(tempTable);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(startDateTimeName)
                .dateTimeThruName(endDateTimeName)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(dateTimeName)
                    .build())
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).tempDataset(tempTable).build();

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from/without_delete_ind/set_2/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from/without_delete_ind/set_2/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnly(dataPass1);
        // 2. Execute Plan and Verify Results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from/without_delete_ind/set_2/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "source_specifies_from/without_delete_ind/set_2/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnly(dataPass2);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(8, 0, 6, 3, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic with only validity from time specified when staging table pre populated
    */
    @Test
    void testMilestoningSourceSpecifiesFromSet3WithDataSplit() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBitemporalFromOnlyStagingTableWithDataSplitIdBased();
        DatasetDefinition tempTable = TestUtils.getBitemporalFromOnlyTempTableIdBased();

        String[] schema = new String[] {indexName, balanceName, digestName, startDateTimeName, endDateTimeName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);
        // Create temp table
        createTempTable(tempTable);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .dataSplitField(dataSplitName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(startDateTimeName)
                .dateTimeThruName(endDateTimeName)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(dateTimeName)
                    .build())
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).tempDataset(tempTable).build();

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from/without_delete_ind/set_3_with_data_split/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from/without_delete_ind/set_3_with_data_split/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDataSplit(dataPass1);
        // 2. Execute Plan and Verify Results
        List<DataSplitRange> dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(1, 1));
        List<Map<String, Object>> expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(2, 0, 2, 0, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, dataSplitRanges);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from/without_delete_ind/set_3_with_data_split/staging_data_pass2.csv";
        String expectedDataPass4 = basePathForExpected + "source_specifies_from/without_delete_ind/set_3_with_data_split/expected_pass4.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDataSplit(dataPass2);
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(1, 1));
        dataSplitRanges.add(DataSplitRange.of(2, 3));
        dataSplitRanges.add(DataSplitRange.of(50, 100));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 1, 1, 0));
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 1, 0));
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 1, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass4, expectedStats, dataSplitRanges);

        // ------------ Perform Pass3 (identical records) ------------------------
        String dataPass3 = basePathForInput + "source_specifies_from/without_delete_ind/set_3_with_data_split/staging_data_pass3.csv";
        String expectedDataPass6 = basePathForExpected + "source_specifies_from/without_delete_ind/set_3_with_data_split/expected_pass6.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDataSplit(dataPass3);
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(1, 1));
        dataSplitRanges.add(DataSplitRange.of(2, 2));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 1, 0));
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 1, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass6, expectedStats, dataSplitRanges);
    }

    /*
    Scenario: Test milestoning Logic with only validity from time specified when staging table pre populated
    */
    @Test
    void testMilestoningSourceSpecifiesFromSet3WithDataSplitMultiPasses() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBitemporalFromOnlyStagingTableWithDataSplitIdBased();
        DatasetDefinition tempTable = TestUtils.getBitemporalFromOnlyTempTableIdBased();

        String[] schema = new String[] {indexName, balanceName, digestName, startDateTimeName, endDateTimeName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);
        // Create temp table
        createTempTable(tempTable);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .dataSplitField(dataSplitName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(startDateTimeName)
                .dateTimeThruName(endDateTimeName)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(dateTimeName)
                    .build())
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).tempDataset(tempTable).build();

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from/without_delete_ind/set_3_with_data_split/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from/without_delete_ind/set_3_with_data_split/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDataSplit(dataPass1);
        // 2. Execute Plan and Verify Results
        List<DataSplitRange> dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(1, 1));
        List<Map<String, Object>> expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(2, 0, 2, 0, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, dataSplitRanges);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from/without_delete_ind/set_3_with_data_split/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "source_specifies_from/without_delete_ind/set_3_with_data_split/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDataSplit(dataPass2);
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(1, 1));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 1, 1, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, dataSplitRanges);

        // ------------ Perform Pass3 ------------------------
        String expectedDataPass3 = basePathForExpected + "source_specifies_from/without_delete_ind/set_3_with_data_split/expected_pass3.csv";
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(2, 3));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 1, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, dataSplitRanges);

        // ------------ Perform Pass4 ------------------------
        String expectedDataPass4 = basePathForExpected + "source_specifies_from/without_delete_ind/set_3_with_data_split/expected_pass4.csv";
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(50, 100));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 1, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass4, expectedStats, dataSplitRanges);

        // ------------ Perform Pass5 (identical records) ------------------------
        String dataPass3 = basePathForInput + "source_specifies_from/without_delete_ind/set_3_with_data_split/staging_data_pass3.csv";
        String expectedDataPass5 = basePathForExpected + "source_specifies_from/without_delete_ind/set_3_with_data_split/expected_pass5.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDataSplit(dataPass3);
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(1, 1));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 1, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass5, expectedStats, dataSplitRanges);

        // ------------ Perform Pass6 (identical records) ------------------------
        String expectedDataPass6 = basePathForExpected + "source_specifies_from/without_delete_ind/set_3_with_data_split/expected_pass6.csv";
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(2, 2));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 1, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass6, expectedStats, dataSplitRanges);
    }

    /*
    Scenario: Test milestoning Logic with only validity from time specified when staging table pre populated
    */
    @Test
    void testMilestoningSourceSpecifiesFromSet4FilterDuplicates() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBitemporalFromOnlyMainTableIdBased();
        DatasetDefinition stagingTable = TestUtils.getBitemporalFromOnlyStagingTableIdBased();
        DatasetDefinition tempTable = TestUtils.getBitemporalFromOnlyTempTableIdBased();
        DatasetDefinition stagingTableWithoutDuplicates = TestUtils.getBitemporalFromOnlyStagingTableWithoutDuplicatesIdBased();

        String[] schema = new String[] {indexName, balanceName, digestName, startDateTimeName, endDateTimeName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);
        // Create temp table
        createTempTable(tempTable);
        // Create staging table without duplicates
        createStagingTable(stagingTableWithoutDuplicates);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(startDateTimeName)
                .dateTimeThruName(endDateTimeName)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(dateTimeName)
                    .build())
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).tempDataset(tempTable).stagingDatasetWithoutDuplicates(stagingTableWithoutDuplicates).build();

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from/without_delete_ind/set_4_filter_duplicates/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from/without_delete_ind/set_4_filter_duplicates/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnly(dataPass1);
        // 2. Execute Plan and Verify Results
        Map<String, Object> expectedStats = createExpectedStatsMap(1, 0, 1, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from/without_delete_ind/set_4_filter_duplicates/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "source_specifies_from/without_delete_ind/set_4_filter_duplicates/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnly(dataPass2);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(1, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);

        // ------------ Perform Pass3 -------------------------
        String dataPass3 = basePathForInput + "source_specifies_from/without_delete_ind/set_4_filter_duplicates/staging_data_pass3.csv";
        String expectedDataPass3 = basePathForExpected + "source_specifies_from/without_delete_ind/set_4_filter_duplicates/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataForBitemporalFromOnly(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);

        // ------------ Perform Pass4 -------------------------
        String dataPass4 = basePathForInput + "source_specifies_from/without_delete_ind/set_4_filter_duplicates/staging_data_pass4.csv";
        String expectedDataPass4 = basePathForExpected + "source_specifies_from/without_delete_ind/set_4_filter_duplicates/expected_pass4.csv";
        // 1. Load staging table
        loadStagingDataForBitemporalFromOnly(dataPass4);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass4, expectedStats);

        // ------------ Perform Pass5 -------------------------
        String dataPass5 = basePathForInput + "source_specifies_from/without_delete_ind/set_4_filter_duplicates/staging_data_pass5.csv";
        String expectedDataPass5 = basePathForExpected + "source_specifies_from/without_delete_ind/set_4_filter_duplicates/expected_pass5.csv";
        // 1. Load staging table
        loadStagingDataForBitemporalFromOnly(dataPass5);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1, 0, 0, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass5, expectedStats);

        // ------------ Perform Pass6 (identical records) -------------------------
        String dataPass6 = basePathForInput + "source_specifies_from/without_delete_ind/set_4_filter_duplicates/staging_data_pass6.csv";
        String expectedDataPass6 = basePathForExpected + "source_specifies_from/without_delete_ind/set_4_filter_duplicates/expected_pass6.csv";
        // 1. Load staging table
        loadStagingDataForBitemporalFromOnly(dataPass6);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass6, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic with only validity from time specified when staging table pre populated
    */
    @Test
    void testMilestoningSourceSpecifiesFromSet5WithDataSplitFilterDuplicates() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBitemporalFromOnlyMainTableIdBased();
        DatasetDefinition stagingTable = TestUtils.getBitemporalFromOnlyStagingTableWithDataSplitIdBased();
        DatasetDefinition tempTable = TestUtils.getBitemporalFromOnlyTempTableIdBased();

        String[] schema = new String[] {indexName, balanceName, digestName, startDateTimeName, endDateTimeName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);
        // Create temp table
        createTempTable(tempTable);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .dataSplitField(dataSplitName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(startDateTimeName)
                .dateTimeThruName(endDateTimeName)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(dateTimeName)
                    .build())
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).tempDataset(tempTable).build();

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from/without_delete_ind/set_5_with_data_split_filter_duplicates/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from/without_delete_ind/set_5_with_data_split_filter_duplicates/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDataSplit(dataPass1);
        // 2. Execute Plan and Verify Results
        List<DataSplitRange> dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(1, 1));
        List<Map<String, Object>> expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(2, 0, 2, 0, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, dataSplitRanges);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from/without_delete_ind/set_5_with_data_split_filter_duplicates/staging_data_pass2.csv";
        String expectedDataPass4 = basePathForExpected + "source_specifies_from/without_delete_ind/set_5_with_data_split_filter_duplicates/expected_pass4.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDataSplit(dataPass2);
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(1, 1));
        dataSplitRanges.add(DataSplitRange.of(2, 3));
        dataSplitRanges.add(DataSplitRange.of(50, 100));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 1, 1, 0));
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 1, 0));
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 1, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass4, expectedStats, dataSplitRanges);

        // ------------ Perform Pass3 (identical records) ------------------------
        String dataPass3 = basePathForInput + "source_specifies_from/without_delete_ind/set_5_with_data_split_filter_duplicates/staging_data_pass3.csv";
        String expectedDataPass6 = basePathForExpected + "source_specifies_from/without_delete_ind/set_5_with_data_split_filter_duplicates/expected_pass6.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDataSplit(dataPass3);
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(1, 1));
        dataSplitRanges.add(DataSplitRange.of(2, 2));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 0, 0));
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 0, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass6, expectedStats, dataSplitRanges);
    }

    /*
    Scenario: Test milestoning Logic with only validity from time specified when staging table pre populated
    */
    @Test
    void testMilestoningSourceSpecifiesFromSet5WithDataSplitFilterDuplicatesMultiPasses() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBitemporalFromOnlyStagingTableWithDataSplitIdBased();
        DatasetDefinition tempTable = TestUtils.getBitemporalFromOnlyTempTableIdBased();

        String[] schema = new String[] {indexName, balanceName, digestName, startDateTimeName, endDateTimeName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);
        // Create temp table
        createTempTable(tempTable);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .dataSplitField(dataSplitName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(startDateTimeName)
                .dateTimeThruName(endDateTimeName)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(dateTimeName)
                    .build())
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).tempDataset(tempTable).build();

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from/without_delete_ind/set_5_with_data_split_filter_duplicates/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from/without_delete_ind/set_5_with_data_split_filter_duplicates/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDataSplit(dataPass1);
        // 2. Execute Plan and Verify Results
        List<DataSplitRange> dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(1, 1));
        List<Map<String, Object>> expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(2, 0, 2, 0, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, dataSplitRanges);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from/without_delete_ind/set_5_with_data_split_filter_duplicates/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "source_specifies_from/without_delete_ind/set_5_with_data_split_filter_duplicates/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDataSplit(dataPass2);
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(1, 1));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 1, 1, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, dataSplitRanges);

        // ------------ Perform Pass3 ------------------------
        String expectedDataPass3 = basePathForExpected + "source_specifies_from/without_delete_ind/set_5_with_data_split_filter_duplicates/expected_pass3.csv";
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(2, 3));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 1, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, dataSplitRanges);

        // ------------ Perform Pass4 ------------------------
        String expectedDataPass4 = basePathForExpected + "source_specifies_from/without_delete_ind/set_5_with_data_split_filter_duplicates/expected_pass4.csv";
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(50, 100));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 1, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass4, expectedStats, dataSplitRanges);

        // ------------ Perform Pass5 (identical records) ------------------------
        String dataPass3 = basePathForInput + "source_specifies_from/without_delete_ind/set_5_with_data_split_filter_duplicates/staging_data_pass3.csv";
        String expectedDataPass5 = basePathForExpected + "source_specifies_from/without_delete_ind/set_5_with_data_split_filter_duplicates/expected_pass5.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDataSplit(dataPass3);
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(1, 1));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 0, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass5, expectedStats, dataSplitRanges);

        // ------------ Perform Pass6 (identical records) ------------------------
        String expectedDataPass6 = basePathForExpected + "source_specifies_from/without_delete_ind/set_5_with_data_split_filter_duplicates/expected_pass6.csv";
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(2, 2));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 0, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass6, expectedStats, dataSplitRanges);
    }

    /*
    Scenario: Test milestoning Logic with only validity from time specified with delete indicator when staging table pre populated
    */
    @Test
    void testMilestoningSourceSpecifiesFromWithDeleteIndicatorSet1() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBitemporalFromOnlyMainTableIdBased();
        DatasetDefinition stagingTable = TestUtils.getBitemporalFromOnlyStagingTableWithDeleteIndicatorIdBased();
        DatasetDefinition tempTable = TestUtils.getBitemporalFromOnlyTempTableIdBased();
        DatasetDefinition tempTableWithDeleteIndicator = TestUtils.getBitemporalFromOnlyTempTableWithDeleteIndicatorIdBased();

        String[] schema = new String[] {indexName, balanceName, digestName, startDateTimeName, endDateTimeName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);
        // Create temp table
        createTempTable(tempTable);
        createTempTable(tempTableWithDeleteIndicator);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(startDateTimeName)
                .dateTimeThruName(endDateTimeName)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(dateTimeName)
                    .build())
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorName)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).tempDataset(tempTable).tempDatasetWithDeleteIndicator(tempTableWithDeleteIndicator).build();

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from/with_delete_ind/set_1/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from/with_delete_ind/set_1/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteInd(dataPass1);
        // 2. Execute Plan and Verify Results
        Map<String, Object> expectedStats = createExpectedStatsMap(1, 0, 1, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from/with_delete_ind/set_1/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "source_specifies_from/with_delete_ind/set_1/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteInd(dataPass2);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(1, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);

        // ------------ Perform Pass3 ------------------------
        String dataPass3 = basePathForInput + "source_specifies_from/with_delete_ind/set_1/staging_data_pass3.csv";
        String expectedDataPass3 = basePathForExpected + "source_specifies_from/with_delete_ind/set_1/expected_pass3.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteInd(dataPass3);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(1, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);

        // ------------ Perform Pass4 ------------------------
        String dataPass4 = basePathForInput + "source_specifies_from/with_delete_ind/set_1/staging_data_pass4.csv";
        String expectedDataPass4 = basePathForExpected + "source_specifies_from/with_delete_ind/set_1/expected_pass4.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteInd(dataPass4);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(1, 0, 0, 1, 1);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass4, expectedStats);

        // ------------ Perform Pass5 ------------------------
        String dataPass5 = basePathForInput + "source_specifies_from/with_delete_ind/set_1/staging_data_pass5.csv";
        String expectedDataPass5 = basePathForExpected + "source_specifies_from/with_delete_ind/set_1/expected_pass5.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteInd(dataPass5);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(1, 0, 0, 0, 1);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass5, expectedStats);

        // ------------ Perform Pass6 (identical records) ------------------------
        String dataPass6 = basePathForInput + "source_specifies_from/with_delete_ind/set_1/staging_data_pass6.csv";
        String expectedDataPass6 = basePathForExpected + "source_specifies_from/with_delete_ind/set_1/expected_pass6.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteInd(dataPass6);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(1, 0, 0, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass6, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic with only validity from time specified with delete indicator when staging table pre populated
    */
    @Test
    void testMilestoningSourceSpecifiesFromWithDeleteIndicatorSet2() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBitemporalFromOnlyStagingTableWithDeleteIndicatorIdBased();
        DatasetDefinition tempTable = TestUtils.getBitemporalFromOnlyTempTableIdBased();
        DatasetDefinition tempTableWithDeleteIndicator = TestUtils.getBitemporalFromOnlyTempTableWithDeleteIndicatorIdBased();

        String[] schema = new String[] {indexName, balanceName, digestName, startDateTimeName, endDateTimeName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);
        // Create temp table
        createTempTable(tempTable);
        createTempTable(tempTableWithDeleteIndicator);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(startDateTimeName)
                .dateTimeThruName(endDateTimeName)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(dateTimeName)
                    .build())
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorName)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).tempDataset(tempTable).tempDatasetWithDeleteIndicator(tempTableWithDeleteIndicator).build();

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from/with_delete_ind/set_2/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from/with_delete_ind/set_2/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteInd(dataPass1);
        // 2. Execute Plan and Verify Results
        Map<String, Object> expectedStats = createExpectedStatsMap(9, 0, 9, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from/with_delete_ind/set_2/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "source_specifies_from/with_delete_ind/set_2/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteInd(dataPass2);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(5, 0, 0, 2, 4);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic with only validity from time specified when staging table pre populated
    */
    @Test
    void testMilestoningSourceSpecifiesFromWithDeleteIndicatorSet3WithDataSplit() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBitemporalFromOnlyMainTableIdBased();
        DatasetDefinition stagingTable = TestUtils.getBitemporalFromOnlyStagingTableWithDeleteIndicatorWithDataSplitIdBased();

        String[] schema = new String[] {indexName, balanceName, digestName, startDateTimeName, endDateTimeName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .dataSplitField(dataSplitName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(startDateTimeName)
                .dateTimeThruName(endDateTimeName)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(dateTimeName)
                    .build())
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorName)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValuesEdgeCase))
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).build();

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from/with_delete_ind/set_3_with_data_split/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from/with_delete_ind/set_3_with_data_split/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteIndWithDataSplit(dataPass1);
        // 2. Execute Plan and Verify Results
        List<DataSplitRange> dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(5, 5));
        List<Map<String, Object>> expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(2, 0, 2, 0, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, dataSplitRanges);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from/with_delete_ind/set_3_with_data_split/staging_data_pass2.csv";
        String expectedDataPass3 = basePathForExpected + "source_specifies_from/with_delete_ind/set_3_with_data_split/expected_pass3.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteIndWithDataSplit(dataPass2);
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(0, 1));
        dataSplitRanges.add(DataSplitRange.of(2, 2));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 1, 1, 0));
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 1, 1));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, dataSplitRanges);

        // ------------ Perform Pass3 (identical records) ------------------------
        String dataPass3 = basePathForInput + "source_specifies_from/with_delete_ind/set_3_with_data_split/staging_data_pass3.csv";
        String expectedDataPass4 = basePathForExpected + "source_specifies_from/with_delete_ind/set_3_with_data_split/expected_pass4.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteIndWithDataSplit(dataPass3);
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(70, 70));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(2, 0, 0, 2, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass4, expectedStats, dataSplitRanges);
    }

    /*
    Scenario: Test milestoning Logic with only validity from time specified when staging table pre populated
    */
    @Test
    void testMilestoningSourceSpecifiesFromWithDeleteIndicatorSet3WithDataSplitWithMultiplePasses() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBitemporalFromOnlyStagingTableWithDeleteIndicatorWithDataSplitIdBased();

        String[] schema = new String[] {indexName, balanceName, digestName, startDateTimeName, endDateTimeName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .dataSplitField(dataSplitName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(startDateTimeName)
                .dateTimeThruName(endDateTimeName)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(dateTimeName)
                    .build())
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorName)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValuesEdgeCase))
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).build();

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from/with_delete_ind/set_3_with_data_split/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from/with_delete_ind/set_3_with_data_split/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteIndWithDataSplit(dataPass1);
        // 2. Execute Plan and Verify Results
        List<DataSplitRange> dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(5, 5));
        List<Map<String, Object>> expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(2, 0, 2, 0, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, dataSplitRanges);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from/with_delete_ind/set_3_with_data_split/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "source_specifies_from/with_delete_ind/set_3_with_data_split/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteIndWithDataSplit(dataPass2);
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(0, 1));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 1, 1, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, dataSplitRanges);

        // ------------ Perform Pass3 ------------------------
        String expectedDataPass3 = basePathForExpected + "source_specifies_from/with_delete_ind/set_3_with_data_split/expected_pass3.csv";
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(2, 2));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 1, 1));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, dataSplitRanges);

        // ------------ Perform Pass4 (identical records) ------------------------
        String dataPass3 = basePathForInput + "source_specifies_from/with_delete_ind/set_3_with_data_split/staging_data_pass3.csv";
        String expectedDataPass4 = basePathForExpected + "source_specifies_from/with_delete_ind/set_3_with_data_split/expected_pass4.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteIndWithDataSplit(dataPass3);
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(70, 71));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(2, 0, 0, 2, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass4, expectedStats, dataSplitRanges);
    }

    /*
    Scenario: Test milestoning Logic with only validity from time specified with delete indicator when staging table pre populated
    */
    @Test
    void testMilestoningSourceSpecifiesFromWithDeleteIndicatorSet4FilterDuplicates() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBitemporalFromOnlyMainTableIdBased();
        DatasetDefinition stagingTable = TestUtils.getBitemporalFromOnlyStagingTableWithDeleteIndicatorIdBased();
        DatasetDefinition tempTable = TestUtils.getBitemporalFromOnlyTempTableIdBased();
        DatasetDefinition tempTableWithDeleteIndicator = TestUtils.getBitemporalFromOnlyTempTableWithDeleteIndicatorIdBased();

        String[] schema = new String[] {indexName, balanceName, digestName, startDateTimeName, endDateTimeName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);
        // Create temp table
        createTempTable(tempTable);
        createTempTable(tempTableWithDeleteIndicator);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(startDateTimeName)
                .dateTimeThruName(endDateTimeName)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(dateTimeName)
                    .build())
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorName)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).tempDataset(tempTable).tempDatasetWithDeleteIndicator(tempTableWithDeleteIndicator).build();

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from/with_delete_ind/set_4_filter_duplicates/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from/with_delete_ind/set_4_filter_duplicates/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteInd(dataPass1);
        // 2. Execute Plan and Verify Results
        Map<String, Object> expectedStats = createExpectedStatsMap(1, 0, 1, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from/with_delete_ind/set_4_filter_duplicates/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "source_specifies_from/with_delete_ind/set_4_filter_duplicates/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteInd(dataPass2);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(1, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);

        // ------------ Perform Pass3 ------------------------
        String dataPass3 = basePathForInput + "source_specifies_from/with_delete_ind/set_4_filter_duplicates/staging_data_pass3.csv";
        String expectedDataPass3 = basePathForExpected + "source_specifies_from/with_delete_ind/set_4_filter_duplicates/expected_pass3.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteInd(dataPass3);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(1, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);

        // ------------ Perform Pass4 ------------------------
        String dataPass4 = basePathForInput + "source_specifies_from/with_delete_ind/set_4_filter_duplicates/staging_data_pass4.csv";
        String expectedDataPass4 = basePathForExpected + "source_specifies_from/with_delete_ind/set_4_filter_duplicates/expected_pass4.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteInd(dataPass4);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(1, 0, 0, 1, 1);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass4, expectedStats);

        // ------------ Perform Pass5 ------------------------
        String dataPass5 = basePathForInput + "source_specifies_from/with_delete_ind/set_4_filter_duplicates/staging_data_pass5.csv";
        String expectedDataPass5 = basePathForExpected + "source_specifies_from/with_delete_ind/set_4_filter_duplicates/expected_pass5.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteInd(dataPass5);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(1, 0, 0, 0, 1);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass5, expectedStats);

        // ------------ Perform Pass6 (identical records) ------------------------
        String dataPass6 = basePathForInput + "source_specifies_from/with_delete_ind/set_4_filter_duplicates/staging_data_pass6.csv";
        String expectedDataPass6 = basePathForExpected + "source_specifies_from/with_delete_ind/set_4_filter_duplicates/expected_pass6.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteInd(dataPass6);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(1, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass6, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic with only validity from time specified when staging table pre populated
    */
    @Test
    void testMilestoningSourceSpecifiesFromWithDeleteIndicatorSet5WithDataSplitFilterDuplicates() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBitemporalFromOnlyStagingTableWithDeleteIndicatorWithDataSplitIdBased();
        DatasetDefinition stagingTableWithoutDuplicates = TestUtils.getBitemporalFromOnlyStagingTableWithoutDuplicatesWithDeleteIndicatorWithDataSplitIdBased();

        String[] schema = new String[] {indexName, balanceName, digestName, startDateTimeName, endDateTimeName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);
        // Create staging table without duplicates
        createStagingTable(stagingTableWithoutDuplicates);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .dataSplitField(dataSplitName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(startDateTimeName)
                .dateTimeThruName(endDateTimeName)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(dateTimeName)
                    .build())
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorName)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValuesEdgeCase))
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).stagingDatasetWithoutDuplicates(stagingTableWithoutDuplicates).build();

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from/with_delete_ind/set_5_with_data_split_filter_duplicates/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from/with_delete_ind/set_5_with_data_split_filter_duplicates/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteIndWithDataSplit(dataPass1);
        // 2. Execute Plan and Verify Results
        List<DataSplitRange> dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(5, 5));
        List<Map<String, Object>> expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(2, 0, 2, 0, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, dataSplitRanges);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from/with_delete_ind/set_5_with_data_split_filter_duplicates/staging_data_pass2.csv";
        String expectedDataPass3 = basePathForExpected + "source_specifies_from/with_delete_ind/set_5_with_data_split_filter_duplicates/expected_pass3.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteIndWithDataSplit(dataPass2);
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(0, 1));
        dataSplitRanges.add(DataSplitRange.of(2, 2));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 1, 1, 0));
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 1, 1));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, dataSplitRanges);

        // ------------ Perform Pass3 (identical records) ------------------------
        String dataPass3 = basePathForInput + "source_specifies_from/with_delete_ind/set_5_with_data_split_filter_duplicates/staging_data_pass3.csv";
        String expectedDataPass4 = basePathForExpected + "source_specifies_from/with_delete_ind/set_5_with_data_split_filter_duplicates/expected_pass4.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteIndWithDataSplit(dataPass3);
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(5, 100));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(2, 0, 0, 0, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass4, expectedStats, dataSplitRanges);
    }

    /*
    Scenario: Test milestoning Logic with only validity from time specified when staging table pre populated
    */
    @Test
    void testMilestoningSourceSpecifiesFromWithDeleteIndicatorSet5WithDataSplitFilterDuplicatesWithMultiplePasses() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBitemporalFromOnlyMainTableIdBased();
        DatasetDefinition stagingTable = TestUtils.getBitemporalFromOnlyStagingTableWithDeleteIndicatorWithDataSplitIdBased();
        DatasetDefinition stagingTableWithoutDuplicates = TestUtils.getBitemporalFromOnlyStagingTableWithoutDuplicatesWithDeleteIndicatorWithDataSplitIdBased();

        String[] schema = new String[] {indexName, balanceName, digestName, startDateTimeName, endDateTimeName, batchIdInName, batchIdOutName};

        // Create staging table
        createStagingTable(stagingTable);
        // Create staging table without duplicates
        createStagingTable(stagingTableWithoutDuplicates);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .dataSplitField(dataSplitName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(startDateTimeName)
                .dateTimeThruName(endDateTimeName)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(dateTimeName)
                    .build())
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorName)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValuesEdgeCase))
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).stagingDatasetWithoutDuplicates(stagingTableWithoutDuplicates).build();

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from/with_delete_ind/set_5_with_data_split_filter_duplicates/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from/with_delete_ind/set_5_with_data_split_filter_duplicates/expected_pass1.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteIndWithDataSplit(dataPass1);
        // 2. Execute Plan and Verify Results
        List<DataSplitRange> dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(5, 5));
        List<Map<String, Object>> expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(2, 0, 2, 0, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, dataSplitRanges);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from/with_delete_ind/set_5_with_data_split_filter_duplicates/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "source_specifies_from/with_delete_ind/set_5_with_data_split_filter_duplicates/expected_pass2.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteIndWithDataSplit(dataPass2);
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(0, 1));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 1, 1, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, dataSplitRanges);

        // ------------ Perform Pass3 ------------------------
        String expectedDataPass3 = basePathForExpected + "source_specifies_from/with_delete_ind/set_5_with_data_split_filter_duplicates/expected_pass3.csv";
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(2, 2));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(1, 0, 0, 1, 1));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, dataSplitRanges);

        // ------------ Perform Pass4 (identical records) ------------------------
        String dataPass3 = basePathForInput + "source_specifies_from/with_delete_ind/set_5_with_data_split_filter_duplicates/staging_data_pass3.csv";
        String expectedDataPass4 = basePathForExpected + "source_specifies_from/with_delete_ind/set_5_with_data_split_filter_duplicates/expected_pass4.csv";
        // 1. Load Staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteIndWithDataSplit(dataPass3);
        // 2. Execute Plan and Verify Results
        dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(0, 100));
        expectedStats = new ArrayList<>();
        expectedStats.add(createExpectedStatsMap(2, 0, 0, 0, 0));
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass4, expectedStats, dataSplitRanges);
    }
}
