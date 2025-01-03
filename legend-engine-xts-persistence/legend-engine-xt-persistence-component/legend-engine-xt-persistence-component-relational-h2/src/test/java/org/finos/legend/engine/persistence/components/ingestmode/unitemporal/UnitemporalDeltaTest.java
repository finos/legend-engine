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
import org.finos.legend.engine.persistence.components.common.OptimizationFilter;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.DigestBasedResolver;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.NoVersioningStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersionColumnBasedResolver;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersionComparator;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DerivedDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FilteredDataset;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.finos.legend.engine.persistence.components.TestUtils.batchIdInName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchIdOutName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchTimeInName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchTimeOutName;
import static org.finos.legend.engine.persistence.components.TestUtils.deleteIndicatorName;
import static org.finos.legend.engine.persistence.components.TestUtils.deleteIndicatorValues;
import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.TestUtils.startTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.versionName;

class UnitemporalDeltaTest extends BaseTest
{
    private final String basePathForInput = "src/test/resources/data/unitemporal-incremental-milestoning/input/batch_id_and_time_based/";
    private final String basePathForExpected = "src/test/resources/data/unitemporal-incremental-milestoning/expected/batch_id_and_time_based/";

    /*
    Scenario: Test milestoning Logic when staging table pre populated
    */
    @Test
    void testMilestoning() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
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

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "without_delete_ind/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "without_delete_ind/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "without_delete_ind/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "without_delete_ind/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(3, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "without_delete_ind/expected_pass3.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging table pre populated and delete indicator is present
    */
    @Test
    void testMilestoningWithDeleteIndicator() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithDeleteIndicator();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTableWithoutPks(stagingTable);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorName)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                .build())
            .versioningStrategy(NoVersioningStrategy.builder().failOnDuplicatePrimaryKeys(true).build())
            .deduplicationStrategy(FailOnDuplicates.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_delete_ind/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_delete_ind/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithDeleteInd(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(4, 0, 4, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "with_delete_ind/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_delete_ind/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithDeleteInd(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(5, 0, 1, 1, 2);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_delete_ind/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataWithDeleteInd(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);

        // ------------ Perform Pass4 (Duplicate PKs) -------------------------
        String dataPass4 = basePathForInput + "with_delete_ind/staging_data_pass3.csv";
        // 1. Load staging table
        loadStagingDataWithDeleteInd(dataPass4);
        try
        {
            executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);
            Assertions.fail("Should not succeed");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Encountered multiple rows with duplicate primary keys, Failing the batch as Fail on Duplicate Primary Keys is selected", e.getMessage());
        }
    }

    @Test
    void testMilestoningWithOptimizationFilters() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTableWithExpiryDatePk();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .addOptimizationFilters(OptimizationFilter.of(expiryDateName))
            .addOptimizationFilters(OptimizationFilter.of(idName))
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_optimization_filter/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_optimization_filter/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "with_optimization_filter/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_optimization_filter/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(3, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_optimization_filter/expected_pass3.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);

        // ------------ Perform Pass4 with lower bound equals upper bound -------------------------
        String dataPass4 = basePathForInput + "with_optimization_filter/staging_data_pass4.csv";
        String expectedDataPass4 = basePathForExpected + "with_optimization_filter/expected_pass4.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass4);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1, 0, 0, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass4, expectedStats, fixedClock_2000_01_01);
    }

    @Test
    void testMilestoningWithMaxVersionGreaterThanDoNotPerform() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalMainTableWithVersionWithoutDigest();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithVersionWithoutDigest();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, versionName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN))
                .performStageVersioning(false)
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_max_versioning/greater_than/without_dedup/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_max_versioning/greater_than/without_dedup/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersionWithoutDigest(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "with_max_versioning/greater_than/without_dedup/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_max_versioning/greater_than/without_dedup/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersionWithoutDigest(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 1, 2, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_max_versioning/greater_than/without_dedup/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataWithVersionWithoutDigest(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);
    }

    @Test
    void testMilestoningWithMaxVersionGreaterThanEqualToDoNotPerform() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalMainTableWithVersion();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithVersion();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN_EQUAL_TO))
                .performStageVersioning(false)
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_max_versioning/greater_than_equal_to/without_dedup/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_max_versioning/greater_than_equal_to/without_dedup/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "with_max_versioning/greater_than_equal_to/without_dedup/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_max_versioning/greater_than_equal_to/without_dedup/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 1, 3, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_max_versioning/greater_than_equal_to/without_dedup/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);
    }

    @Test
    void testMilestoningWithFilterDuplicatesMaxVersioningGreaterThan() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalMainTableWithVersion();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithVersion();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTableWithoutPks(stagingTable);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN))
                .performStageVersioning(true)
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_max_versioning/greater_than/with_dedup/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_max_versioning/greater_than/with_dedup/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(6, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 6);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "with_max_versioning/greater_than/with_dedup/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_max_versioning/greater_than/with_dedup/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(10, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_max_versioning/greater_than/with_dedup/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);
    }

    @Test
    void testMilestoningWithFailOnDuplicatesMaxVersioningGreaterThanEqualTo() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalMainTableWithVersion();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithVersion();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTableWithoutPks(stagingTable);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN_EQUAL_TO))
                .performStageVersioning(true)
                .build())
            .deduplicationStrategy(FailOnDuplicates.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_max_versioning/greater_than_equal_to/with_dedup/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_max_versioning/greater_than_equal_to/with_dedup/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "with_max_versioning/greater_than_equal_to/with_dedup/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_max_versioning/greater_than_equal_to/with_dedup/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(10, 0, 1, 3, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_max_versioning/greater_than_equal_to/with_dedup/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);

        // ------------ Perform Pass4 (Fail on Dups) -------------------------
        String dataPass4 = basePathForInput + "with_max_versioning/greater_than_equal_to/with_dedup/staging_data_pass4.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass4);
        // 2. Execute plans and verify results
        try
        {
            executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);
            Assertions.fail("Should not succeed");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy", e.getMessage());
        }
    }

    @Test
    void testMilestoningWithFilterStagingTable() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DerivedDataset stagingTable = TestUtils.getDerivedStagingTableWithFilter();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        DatasetDefinition stagingTableForDB = TestUtils.getStagingTableWithFilterForDB();
        createStagingTable(stagingTableForDB);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
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

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_staging_filter/with_no_versioning/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_staging_filter/with_no_versioning/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithFilter(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        IngestorResult result = executePlansAndVerifyResultsWithStagingFilters(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
        Assertions.assertEquals(Optional.of(1), result.batchId());
        Assertions.assertEquals("2000-01-01 00:00:00.000000", result.ingestionTimestampUTC());

        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 6);

        // ------------ Perform Pass2 ------------------------
        // 0. Create new filter
        datasets = Datasets.of(mainTable, TestUtils.getStagingTableWithFilterSecondPass());
        String dataPass2 = basePathForInput + "with_staging_filter/with_no_versioning/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_staging_filter/with_no_versioning/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithFilter(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(3, 0, 1, 1, 0);
        result = executePlansAndVerifyResultsWithStagingFilters(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);
        Assertions.assertEquals(Optional.of(2), result.batchId());
        Assertions.assertEquals("2000-01-01 00:00:00.000000", result.ingestionTimestampUTC());

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_staging_filter/with_no_versioning/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataWithFilter(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        result = executePlansAndVerifyResultsWithStagingFilters(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_01);
        Assertions.assertEquals(Optional.of(3), result.batchId());
        Assertions.assertEquals("2000-01-01 00:00:00.000000", result.ingestionTimestampUTC());
    }

    @Test
    void testMilestoningWithFilterDupsMaxVersionGreaterThanWithStagingFilters() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalMainTableWithVersion();
        DerivedDataset stagingTable = TestUtils.getDerivedStagingTableWithFilterWithVersion();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        DatasetDefinition stagingTableForDB = TestUtils.getStagingTableWithFilterWithVersionForDB();
        createStagingTableWithoutPks(stagingTableForDB);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN))
                .performStageVersioning(false)
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_staging_filter/with_max_versioning/greater_than/without_dedup/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_staging_filter/with_max_versioning/greater_than/without_dedup/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(6, 0, 3, 0, 0);
        executePlansAndVerifyResultsWithStagingFilters(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 9);

        // ------------ Perform Pass2 ------------------------
        // 0. Create new filter
        datasets = Datasets.of(mainTable, TestUtils.getStagingTableWithFilterWithVersionSecondPass());
        String dataPass2 = basePathForInput + "with_staging_filter/with_max_versioning/greater_than/without_dedup/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_staging_filter/with_max_versioning/greater_than/without_dedup/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 1, 1, 0);
        executePlansAndVerifyResultsWithStagingFilters(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_staging_filter/with_max_versioning/greater_than/without_dedup/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResultsWithStagingFilters(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_01);
    }

    @Test
    void testMilestoningWithFailOnDupsMaxVersionGreaterThanEqualToWithStagingFilters() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalMainTableWithVersion();
        DerivedDataset stagingTable = TestUtils.getDerivedStagingTableWithFilterWithVersion();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        DatasetDefinition stagingTableForDB = TestUtils.getStagingTableWithFilterWithVersionForDB();
        createStagingTableWithoutPks(stagingTableForDB);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN_EQUAL_TO))
                .performStageVersioning(false)
                .build())
            .deduplicationStrategy(FailOnDuplicates.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_staging_filter/with_max_versioning/greater_than_equal_to/without_dedup/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_staging_filter/with_max_versioning/greater_than_equal_to/without_dedup/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResultsWithStagingFilters(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 6);

        // ------------ Perform Pass2 ------------------------
        // 0. Create new filter
        datasets = Datasets.of(mainTable, TestUtils.getStagingTableWithFilterWithVersionSecondPass());
        String dataPass2 = basePathForInput + "with_staging_filter/with_max_versioning/greater_than_equal_to/without_dedup/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_staging_filter/with_max_versioning/greater_than_equal_to/without_dedup/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 1, 2, 0);
        executePlansAndVerifyResultsWithStagingFilters(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_staging_filter/with_max_versioning/greater_than_equal_to/without_dedup/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResultsWithStagingFilters(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass4 Fail on Dups -------------------------
        String dataPass4 = basePathForInput + "with_staging_filter/with_max_versioning/greater_than_equal_to/without_dedup/staging_data_pass4.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass4);
        // 2. Execute plans and verify results
        try
        {
            executePlansAndVerifyResultsWithStagingFilters(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_01);
            Assertions.fail("Should not succeed");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy", e.getMessage());
        }
    }

    @Test
    void testMilestoningWithFilterStagingTableWithMaxVersioningGreaterThan() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalMainTableWithVersion();
        DerivedDataset stagingTable = TestUtils.getDerivedStagingTableWithFilterWithVersion();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        DatasetDefinition stagingTableForDB = TestUtils.getStagingTableWithFilterWithVersionForDB();
        createStagingTable(stagingTableForDB);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN))
                .performStageVersioning(true)
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_staging_filter/with_max_versioning/greater_than/with_dedup/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_staging_filter/with_max_versioning/greater_than/with_dedup/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResultsWithStagingFilters(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 6);

        // ------------ Perform Pass2 ------------------------
        // 0. Create new filter
        datasets = Datasets.of(mainTable, TestUtils.getStagingTableWithFilterWithVersionSecondPass());
        String dataPass2 = basePathForInput + "with_staging_filter/with_max_versioning/greater_than/with_dedup/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_staging_filter/with_max_versioning/greater_than/with_dedup/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(9, 0, 1, 1, 0);
        executePlansAndVerifyResultsWithStagingFilters(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_staging_filter/with_max_versioning/greater_than/with_dedup/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResultsWithStagingFilters(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_01);
    }

    @Test
    void testMilestoningWithFilterDupsMaxVersioningDigestBasedWithStagingFilters() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalMainTableWithVersion();
        DerivedDataset stagingTable = TestUtils.getDerivedStagingTableWithFilterWithVersion();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        DatasetDefinition stagingTableForDB = TestUtils.getStagingTableWithFilterWithVersionForDB();
        createStagingTable(stagingTableForDB);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(DigestBasedResolver.INSTANCE)
                .performStageVersioning(true)
                .build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_staging_filter/with_max_versioning/digest_based/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_staging_filter/with_max_versioning/digest_based/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResultsWithStagingFilters(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 6);

        // ------------ Perform Pass2 ------------------------
        // 0. Create new filter
        datasets = Datasets.of(mainTable, TestUtils.getStagingTableWithFilterWithVersionSecondPass());
        String dataPass2 = basePathForInput + "with_staging_filter/with_max_versioning/digest_based/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_staging_filter/with_max_versioning/digest_based/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(9, 0, 1, 2, 0);
        executePlansAndVerifyResultsWithStagingFilters(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_staging_filter/with_max_versioning/digest_based/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResultsWithStagingFilters(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_01);
    }

    @Test
    void testMilestoningWithFilterStagingTableWithMaxVersioningGreaterThanWithDedupWithUpperCase() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalMainTableWithVersion();
        DerivedDataset stagingTable = TestUtils.getDerivedStagingTableWithFilterWithVersion();

        String[] schema = new String[]{idName.toUpperCase(), nameName.toUpperCase(), incomeName.toUpperCase(), startTimeName.toUpperCase(), expiryDateName.toUpperCase(), digestName.toUpperCase(), versionName.toUpperCase(), batchIdInName.toUpperCase(), batchIdOutName.toUpperCase(), batchTimeInName.toUpperCase(), batchTimeOutName.toUpperCase()};

        // Create staging table
        h2Sink.executeStatement("CREATE TABLE IF NOT EXISTS \"TEST\".\"STAGING\"(\"ID\" INTEGER NOT NULL,\"NAME\" VARCHAR(64) NOT NULL,\"INCOME\" BIGINT,\"START_TIME\" TIMESTAMP NOT NULL,\"EXPIRY_DATE\" DATE,\"DIGEST\" VARCHAR,\"VERSION\" INT,\"BATCH\" INT,PRIMARY KEY (\"ID\", \"START_TIME\", \"VERSION\", \"BATCH\"))");

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN))
                .performStageVersioning(true)
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_staging_filter/with_max_versioning/greater_than/with_dedup/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_staging_filter/with_max_versioning/greater_than/with_dedup/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersionInUpperCase(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass2 ------------------------
        // 0. Create new filter
        datasets = Datasets.of(mainTable, TestUtils.getStagingTableWithFilterWithVersionSecondPass());
        String dataPass2 = basePathForInput + "with_staging_filter/with_max_versioning/greater_than/with_dedup/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_staging_filter/with_max_versioning/greater_than/with_dedup/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersionInUpperCase(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(9, 0, 1, 1, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_staging_filter/with_max_versioning/greater_than/with_dedup/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersionInUpperCase(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);
    }

    @Test
    void testMilestoningWithMaxVersioningGreaterThanWithDedupWithFilteredDatasetWithUpperCase() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalMainTableWithVersion();
        FilteredDataset stagingTable = TestUtils.getFilteredStagingTableWithVersion();

        String[] schema = new String[]{idName.toUpperCase(), nameName.toUpperCase(), incomeName.toUpperCase(), startTimeName.toUpperCase(), expiryDateName.toUpperCase(), digestName.toUpperCase(), versionName.toUpperCase(), batchIdInName.toUpperCase(), batchIdOutName.toUpperCase(), batchTimeInName.toUpperCase(), batchTimeOutName.toUpperCase()};

        // Create staging table
        h2Sink.executeStatement("CREATE TABLE IF NOT EXISTS \"TEST\".\"STAGING\"(\"ID\" INTEGER NOT NULL,\"NAME\" VARCHAR(64) NOT NULL,\"INCOME\" BIGINT,\"START_TIME\" TIMESTAMP NOT NULL,\"EXPIRY_DATE\" DATE,\"DIGEST\" VARCHAR,\"VERSION\" INT,\"BATCH\" INT,PRIMARY KEY (\"ID\", \"START_TIME\", \"VERSION\", \"BATCH\"))");

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN))
                .performStageVersioning(true)
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "with_staging_filter/with_max_versioning/greater_than/with_dedup/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_staging_filter/with_max_versioning/greater_than/with_dedup/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersionInUpperCase(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass2 ------------------------
        // 0. Create new filter
        datasets = Datasets.of(mainTable, TestUtils.getFilteredStagingTableWithVersionSecondPass());
        String dataPass2 = basePathForInput + "with_staging_filter/with_max_versioning/greater_than/with_dedup/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "with_staging_filter/with_max_versioning/greater_than/with_dedup/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersionInUpperCase(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(9, 0, 1, 1, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "with_staging_filter/with_max_versioning/greater_than/with_dedup/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersionInUpperCase(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);
    }

    @Test
    void testMilestoningWithMaxVersioningFail() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalMainTableWithVersionWithoutDigest();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithVersionWithoutDigest();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, versionName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(nameName)
                .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN))
                .performStageVersioning(false)
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String dataPass1 = basePathForInput + "with_max_versioning/greater_than/without_dedup/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_max_versioning/greater_than/without_dedup/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersionWithoutDigest(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);

        try
        {
            executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Versioning field's data type [VARCHAR] is not supported", e.getMessage());
        }
    }

    /*
    Scenario: Test milestoning Logic when staging data comes from CSV and has less columns than main dataset
    */
    @Test
    void testMilestoningWithLessColumnsInStaging() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalMainTable();
        String dataPass1 = basePathForInput + "less_columns_in_staging/staging_data_pass1.csv";
        Dataset stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass1);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
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

        // ------------ Perform Pass1 ------------------------
        String expectedDataPass1 = basePathForExpected + "less_columns_in_staging/expected_pass1.csv";
        // Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "less_columns_in_staging/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "less_columns_in_staging/expected_pass2.csv";
        stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass2);
        // Execute plans and verify results
        expectedStats = createExpectedStatsMap(3, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = "src/test/resources/data/empty_file.csv";
        String expectedDataPass3 = basePathForExpected + "less_columns_in_staging/expected_pass3.csv";
        stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass3);

        // Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass3, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging table is pre populated and
    staging table is cleaned up in the end
    */
    @Test
    void testMilestoningWithDeleteIndicatorWithCleanStagingDataWithoutStatCollection() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithDeleteIndicator();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorName)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass With Clean Staging Table ------------------------
        String dataPass1 = basePathForInput + "with_delete_ind/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_delete_ind/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithDeleteInd(dataPass1);
        // 2. Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, new HashMap<>(), fixedClock_2000_01_01);
        // 3. Assert that the staging table is truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 0);
    }

    @Test
    void testMilestoningWithDeleteIndicatorWithoutCleanStagingDataWithStatCollection() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithDeleteIndicator();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorName)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass With Clean Staging Table ------------------------
        String dataPass1 = basePathForInput + "with_delete_ind/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "with_delete_ind/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithDeleteInd(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(4, 0, 4, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
        // 3. Assert that the staging table is truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 4);
    }
}
