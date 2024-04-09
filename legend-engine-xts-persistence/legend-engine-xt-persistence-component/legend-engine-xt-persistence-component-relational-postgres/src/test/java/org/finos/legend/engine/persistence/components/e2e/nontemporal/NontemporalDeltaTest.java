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

package org.finos.legend.engine.persistence.components.e2e.nontemporal;

import org.finos.legend.engine.persistence.components.common.DatasetFilter;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.FilterType;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.e2e.BaseTest;
import org.finos.legend.engine.persistence.components.e2e.TestUtils;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.DigestBasedResolver;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersionColumnBasedResolver;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.VersionComparator;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DerivedDataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FilteredDataset;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.NumericalValue;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.e2e.TestUtils.batchIdName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.batchName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.batchUpdateTimeName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.dataSplitName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.deleteIndicatorName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.deleteIndicatorValues;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.getDedupAndVersioningSchemaWithVersion;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.getDedupAndVersioningSchemaWithVersionAndBatch;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.stagingTableName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.startTimeName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.testSchemaName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.versionName;

class NontemporalDeltaTest extends BaseTest
{
    private final String basePathForInput = "data/incremental-delta-milestoning/";
    private final String basePathForExpected = "src/test/resources/data/incremental-delta-milestoning/";


    /*
    Scenarios:
    1. Auditing is not enabled
    2. Staging data imported from CSV and has lesser columns than main dataset
    3. Staging data cleanup
    4. Auditing is not enabled
    5. Data Splits enabled
    */

    /*
    Scenario: Test NonTemporal Delta when Auditing is not enabled
     */
    @Test
    void testNonTemporalDeltaWithNoAuditing() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestName)
                .auditing(NoAuditing.builder().build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/vanilla_case/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/vanilla_case/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\", \"income\"");
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = postgresSink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform incremental (delta) milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "input/vanilla_case/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/vanilla_case/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass2);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, " order by \"id\", \"income\"");
    }

    /*
    Scenario: Test NonTemporal Delta when delete indicator is present
     */
    @Test
    void testNonTemporalDeltaWithDeleteIndicator() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithDeleteIndicator();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestName)
                .auditing(NoAuditing.builder().build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                        .deleteField(deleteIndicatorName)
                        .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                        .build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/with_delete_indicator/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/with_delete_indicator/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithDeleteInd(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\", \"income\"");
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = postgresSink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform incremental (delta) milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "input/with_delete_indicator/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/with_delete_indicator/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithDeleteInd(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 4);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 1);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, " order by \"id\", \"income\"");
        // 3. Assert that the staging table is NOT truncated
        stagingTableList = postgresSink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 4);

        // ------------ Perform incremental (delta) milestoning Pass3 ------------------------
        String dataPass3 = basePathForInput + "input/with_delete_indicator/data_pass3.csv";
        String expectedDataPass3 = basePathForExpected + "expected/with_delete_indicator/expected_pass3.csv";
        // 1. Load staging table
        loadStagingDataWithDeleteInd(dataPass3);
        // 2. Execute plans and verify results
        expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 1);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, " order by \"id\", \"income\"");
        // 3. Assert that the staging table is NOT truncated
        stagingTableList = postgresSink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 1);
    }

    /*
    Scenario: Test NonTemporal Delta when staging data comes from CSV and has lesser columns than main dataset
    */
    @Test
    void testNonTemporalDeltaWithLessColumnsInStaging() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        DatasetDefinition stagingTable = TestUtils.getDatasetWithLessColumnsThanMain();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestName)
                .auditing(NoAuditing.builder().build())
                .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/less_columns_in_staging/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/less_columns_in_staging/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingDataWithColumnsThanMain(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\", \"income\"");

        // ------------ Perform incremental (delta) milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "input/less_columns_in_staging/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/less_columns_in_staging/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingDataWithColumnsThanMain(dataPass2);
        // 2. Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, " order by \"id\", \"income\"");
    }

    /*
    Scenario: Test NonTemporal Delta when staging table is cleaned up in the end
    */
    @Test
    void testNonTemporalDeltaWithCleanStagingDataWithFailOnDups() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTableWithoutPks(stagingTable);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestName)
                .auditing(NoAuditing.builder().build())
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(true).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdName};

        // ------------ Perform incremental (delta) milestoning With Clean Staging Table ------------------------
        String dataPass1 = basePathForInput + "input/vanilla_case/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/vanilla_case/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\", \"income\"");
        // 3. Assert that the staging table is truncated
        List<Map<String, Object>> stagingTableList = postgresSink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 0);

        // ------------ Perform incremental (delta) milestoning Fail on Dups ------------------------
        String dataPass2 = basePathForInput + "input/with_duplicates/data_pass1.csv";
        loadBasicStagingData(dataPass2);
        try
        {
            executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\", \"income\"");
            Assertions.fail("Should not Succeed");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy", e.getMessage());
        }
    }

    /*
    Scenario: Test NonTemporal Delta when Auditing is enabled
    */
    @Test
    void testNonTemporalDeltaWithAuditingFilterDuplicates() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTableWithoutPks(stagingTable);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestName)
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/with_update_timestamp_field/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/with_update_timestamp_field/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 5);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01, " order by \"id\", \"income\"");
    }

    /*
    Scenario: Test NonTemporal Delta when Data splits are enabled
    */
    @Test
    void testNonTemporalDeltaNoAuditingWithAllVersionDoNotPerform() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getDatasetWithDataSplits();

        // Create staging table
        createStagingTableWithoutPks(stagingTable);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestName)
                .versioningStrategy(AllVersionsStrategy.builder()
                        .versioningField(expiryDateName)
                        .dataSplitFieldName(dataSplitName)
                        .performStageVersioning(false)
                        .mergeDataVersionResolver(DigestBasedResolver.INSTANCE).build())
                .auditing(NoAuditing.builder().build())
                .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/with_data_splits/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/with_data_splits/expected_pass1.csv";

        // 1. Load staging table
        loadBasicStagingDataWithDataSplit(dataPass1);
        // 2. Execute plans and verify results
        List<DataSplitRange> dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(1, 1));
        dataSplitRanges.add(DataSplitRange.of(2, 2));
        List<Map<String, Object>> expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats1 = new HashMap<>();
        expectedStats1.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats1.put(StatisticName.ROWS_DELETED.name(), 0);
        expectedStats1.put(StatisticName.ROWS_TERMINATED.name(), 0);

        Map<String, Object> expectedStats2 = new HashMap<>();
        expectedStats2.put(StatisticName.INCOMING_RECORD_COUNT.name(), 2);
        expectedStats2.put(StatisticName.ROWS_DELETED.name(), 0);
        expectedStats2.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStatsList.add(expectedStats1);
        expectedStatsList.add(expectedStats2);

        executePlansAndVerifyResultsWithSpecifiedDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStatsList, dataSplitRanges, " order by \"id\", \"income\"");
    }

    @Test
    void testNonTemporalDeltaWithMaxVersioningGreaterThan() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTableWithVersion();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithVersion();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestName)
                .auditing(NoAuditing.builder().build())
                .versioningStrategy(MaxVersionStrategy.builder()
                        .versioningField(versionName)
                        .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN))
                        .performStageVersioning(false)
                        .build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchIdName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/with_max_versioning/greater_than/without_dedup/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/with_max_versioning/greater_than/without_dedup/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\", \"income\"");
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = postgresSink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform incremental (delta) milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "input/with_max_versioning/greater_than/without_dedup/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/with_max_versioning/greater_than/without_dedup/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 4);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, " order by \"id\", \"income\"");
    }

    @Test
    void testNonTemporalDeltaWithMaxVersioningGreaterThanEqualTo() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTableWithVersion();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithVersion();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestName)
                .auditing(NoAuditing.builder().build())
                .versioningStrategy(MaxVersionStrategy.builder()
                        .versioningField(versionName)
                        .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN_EQUAL_TO))
                        .performStageVersioning(false)
                        .build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchIdName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/with_max_versioning/greater_than_equal_to/without_dedup/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/with_max_versioning/greater_than_equal_to/without_dedup/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\", \"income\"");
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = postgresSink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform incremental (delta) milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "input/with_max_versioning/greater_than_equal_to/without_dedup/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/with_max_versioning/greater_than_equal_to/without_dedup/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 4);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, " order by \"id\", \"income\"");
    }

    @Test
    void testNonTemporalDeltaWithMaxVersioningGreaterThanWithDedup() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTableWithVersion();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithVersion();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestName)
                .auditing(NoAuditing.builder().build())
                .versioningStrategy(MaxVersionStrategy.builder()
                        .versioningField(versionName)
                        .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN))
                        .performStageVersioning(true)
                        .build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchIdName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/with_max_versioning/greater_than/with_dedup/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/with_max_versioning/greater_than/with_dedup/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\", \"income\"");
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = postgresSink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform incremental (delta) milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "input/with_max_versioning/greater_than/with_dedup/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/with_max_versioning/greater_than/with_dedup/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 10);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, " order by \"id\", \"income\"");
    }

    @Test
    void testNonTemporalDeltaWithMaxVersioningGreaterThanEqualToWithDedup() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTableWithVersion();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithVersion();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestName)
                .auditing(NoAuditing.builder().build())
                .versioningStrategy(MaxVersionStrategy.builder()
                        .versioningField(versionName)
                        .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN_EQUAL_TO))
                        .performStageVersioning(true)
                        .build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchIdName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/with_max_versioning/greater_than_equal_to/with_dedup/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/with_max_versioning/greater_than_equal_to/with_dedup/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\", \"income\"");
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = postgresSink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform incremental (delta) milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "input/with_max_versioning/greater_than_equal_to/with_dedup/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/with_max_versioning/greater_than_equal_to/with_dedup/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 10);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, " order by \"id\", \"income\"");
    }

    @Test
    void testNonTemporalDeltaWithFilterStagingTable() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DerivedDataset stagingTable = TestUtils.getDerivedStagingTableWithFilter();

        // Create staging table
        DatasetDefinition stagingTableForDB = TestUtils.getStagingTableWithFilterForDB();
        createStagingTable(stagingTableForDB);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestName)
                .auditing(NoAuditing.builder().build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/with_staging_filter/with_no_versioning/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/with_staging_filter/with_no_versioning/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithFilter(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\", \"income\"");
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = postgresSink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 6);

        // ------------ Perform incremental (delta) milestoning Pass2 ------------------------
        // 0. Create new filter
        datasets = Datasets.of(mainTable, TestUtils.getStagingTableWithFilterSecondPass());
        String dataPass2 = basePathForInput + "input/with_staging_filter/with_no_versioning/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/with_staging_filter/with_no_versioning/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithFilter(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, " order by \"id\", \"income\"");
    }

    @Test
    void testNonTemporalDeltaWithFilterStagingTableWithMaxVersioningGreaterThan() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTableWithVersion();
        DerivedDataset stagingTable = TestUtils.getDerivedStagingTableWithFilterWithVersion();

        // Create staging table
        DatasetDefinition stagingTableForDB = TestUtils.getStagingTableWithFilterWithVersionForDB();
        createStagingTable(stagingTableForDB);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestName)
                .auditing(NoAuditing.builder().build())
                .versioningStrategy(MaxVersionStrategy.builder()
                        .versioningField(versionName)
                        .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN))
                        .performStageVersioning(false)
                        .build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchIdName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/with_staging_filter/with_max_versioning/greater_than/without_dedup/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/with_staging_filter/with_max_versioning/greater_than/without_dedup/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\", \"income\"");
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = postgresSink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 6);

        // ------------ Perform incremental (delta) milestoning Pass2 ------------------------
        // 0. Create new filter
        datasets = Datasets.of(mainTable, TestUtils.getStagingTableWithFilterWithVersionSecondPass());
        String dataPass2 = basePathForInput + "input/with_staging_filter/with_max_versioning/greater_than/without_dedup/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/with_staging_filter/with_max_versioning/greater_than/without_dedup/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 4);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, " order by \"id\", \"income\"");
    }

    @Test
    void testNonTemporalDeltaWithFilterStagingTableWithMaxVersioningGreaterThanEqualTo() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTableWithVersion();
        DerivedDataset stagingTable = TestUtils.getDerivedStagingTableWithFilterWithVersion();

        // Create staging table
        DatasetDefinition stagingTableForDB = TestUtils.getStagingTableWithFilterWithVersionForDB();
        createStagingTable(stagingTableForDB);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestName)
                .auditing(NoAuditing.builder().build())
                .versioningStrategy(MaxVersionStrategy.builder()
                        .versioningField(versionName)
                        .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN_EQUAL_TO))
                        .performStageVersioning(false)
                        .build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchIdName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/with_staging_filter/with_max_versioning/greater_than_equal_to/without_dedup/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/with_staging_filter/with_max_versioning/greater_than_equal_to/without_dedup/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\", \"income\"");
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = postgresSink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 6);

        // ------------ Perform incremental (delta) milestoning Pass2 ------------------------
        // 0. Create new filter
        datasets = Datasets.of(mainTable, TestUtils.getStagingTableWithFilterWithVersionSecondPass());
        String dataPass2 = basePathForInput + "input/with_staging_filter/with_max_versioning/greater_than_equal_to/without_dedup/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/with_staging_filter/with_max_versioning/greater_than_equal_to/without_dedup/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 4);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, " order by \"id\", \"income\"");
    }

    @Test
    void testNonTemporalDeltaWithFilterStagingTableWithMaxVersioningGreaterThanWithDedup() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTableWithVersion();
        DerivedDataset stagingTable = TestUtils.getDerivedStagingTableWithFilterWithVersion();

        // Create staging table
        DatasetDefinition stagingTableForDB = TestUtils.getStagingTableWithFilterWithVersionForDB();
        createStagingTableWithoutPks(stagingTableForDB);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestName)
                .auditing(NoAuditing.builder().build())
                .versioningStrategy(MaxVersionStrategy.builder()
                        .versioningField(versionName)
                        .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN))
                        .performStageVersioning(true)
                        .build())
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchIdName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/with_staging_filter/with_max_versioning/greater_than/with_dedup/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/with_staging_filter/with_max_versioning/greater_than/with_dedup/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\", \"income\"");
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = postgresSink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 6);

        // ------------ Perform incremental (delta) milestoning Pass2 ------------------------
        // 0. Create new filter
        datasets = Datasets.of(mainTable, TestUtils.getStagingTableWithFilterWithVersionSecondPass());
        String dataPass2 = basePathForInput + "input/with_staging_filter/with_max_versioning/greater_than/with_dedup/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/with_staging_filter/with_max_versioning/greater_than/with_dedup/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 10);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, " order by \"id\", \"income\"");


        // ------------ Perform incremental (delta) milestoning Pass3 Fail on Dups ------------------------
        // 0. Create new filter
        datasets = Datasets.of(mainTable, TestUtils.getStagingTableWithFilterWithVersionSecondPass());
        String dataPass3 = basePathForInput + "input/with_staging_filter/with_max_versioning/greater_than/with_dedup/data_pass3.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass3);
        try
        {
            executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, " order by \"id\", \"income\"");
            Assertions.fail("Should not Succeed");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy", e.getMessage());
        }
    }

    @Test
    void testNonTemporalDeltaWithFilterStagingTableWithFilterDupsMaxVersioningGreaterThanEqualTo() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTableWithVersion();
        DerivedDataset stagingTable = TestUtils.getDerivedStagingTableWithFilterWithVersion();

        // Create staging table
        DatasetDefinition stagingTableForDB = TestUtils.getStagingTableWithFilterWithVersionForDB();
        createStagingTableWithoutPks(stagingTableForDB);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestName)
                .auditing(NoAuditing.builder().build())
                .versioningStrategy(MaxVersionStrategy.builder()
                        .versioningField(versionName)
                        .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN_EQUAL_TO))
                        .performStageVersioning(true)
                        .build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchIdName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/with_staging_filter/with_max_versioning/greater_than_equal_to/with_dedup/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/with_staging_filter/with_max_versioning/greater_than_equal_to/with_dedup/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 4);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\", \"income\"");
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = postgresSink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 7);

        // ------------ Perform incremental (delta) milestoning Pass2 ------------------------
        // 0. Create new filter
        datasets = Datasets.of(mainTable, TestUtils.getStagingTableWithFilterWithVersionSecondPass());
        String dataPass2 = basePathForInput + "input/with_staging_filter/with_max_versioning/greater_than_equal_to/with_dedup/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/with_staging_filter/with_max_versioning/greater_than_equal_to/with_dedup/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithFilterWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 12);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, " order by \"id\", \"income\"");
    }

    @Test
    void testNonTemporalDeltaWithAllVersionGreaterThanAndStagingFilters() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingDataset = DatasetDefinition.builder()
                .group(testSchemaName)
                .name(stagingTableName)
                .schema(getDedupAndVersioningSchemaWithVersionAndBatch)
                .build();

        createStagingTableWithoutPks(stagingDataset);

        DerivedDataset stagingTable = DerivedDataset.builder()
                .group(testSchemaName)
                .name(stagingTableName)
                .schema(getDedupAndVersioningSchemaWithVersion)
                .addDatasetFilters(DatasetFilter.of(batchName, FilterType.EQUAL_TO, 1))
                .build();

        String path = basePathForInput + "input/with_staging_filter/with_all_version/greater_than/data1.csv";
        loadStagingDataWithVersionAndBatch(path);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestName)
                .auditing(NoAuditing.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder()
                        .versioningField(versionName)
                        .mergeDataVersionResolver(VersionColumnBasedResolver.of(VersionComparator.GREATER_THAN_EQUAL_TO))
                        .performStageVersioning(true)
                        .build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, versionName, incomeName, expiryDateName, digestName, batchIdName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String expectedDataPass1 = basePathForExpected + "expected/with_staging_filter/with_all_version/greater_than/expected_pass1.csv";
        // 2. Execute plans and verify results
        List<Map<String, Object>> expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats1  = new HashMap<>();
        expectedStats1.put(StatisticName.INCOMING_RECORD_COUNT.name(), 4);
        expectedStats1.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats1.put(StatisticName.ROWS_DELETED.name(), 0);
        Map<String, Object> expectedStats2  = new HashMap<>();
        expectedStats2.put(StatisticName.INCOMING_RECORD_COUNT.name(), 1);
        expectedStats2.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats2.put(StatisticName.ROWS_DELETED.name(), 0);
        Map<String, Object> expectedStats3  = new HashMap<>();
        expectedStats3.put(StatisticName.INCOMING_RECORD_COUNT.name(), 1);
        expectedStats3.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats3.put(StatisticName.ROWS_DELETED.name(), 0);
        expectedStatsList.add(expectedStats1);
        expectedStatsList.add(expectedStats2);
        expectedStatsList.add(expectedStats3);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStatsList, fixedClock_2000_01_01, " order by \"id\", \"income\"");

        // ------------ Perform incremental (delta) milestoning Pass2 Fail on Duplicates ------------------------
        ingestMode = ingestMode.withDeduplicationStrategy(FailOnDuplicates.builder().build());
        stagingTable = stagingTable.withDatasetFilters(DatasetFilter.of("batch", FilterType.EQUAL_TO, 2));
        datasets = Datasets.of(mainTable, stagingTable);
        try
        {
            executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStatsList, fixedClock_2000_01_01, " order by \"id\", \"income\"");
            Assertions.fail("Should not succeed");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy", e.getMessage());
        }

        // ------------ Perform incremental (delta) milestoning Pass2 Filter Duplicates ------------------------
        String expectedDataPass2 = basePathForExpected + "expected/with_staging_filter/with_all_version/greater_than/expected_pass2.csv";
        expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats4  = new HashMap<>();
        expectedStats4.put(StatisticName.INCOMING_RECORD_COUNT.name(), 4);
        expectedStats4.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats4.put(StatisticName.ROWS_DELETED.name(), 0);
        expectedStatsList.add(expectedStats4);

        ingestMode = ingestMode.withDeduplicationStrategy(FilterDuplicates.builder().build());
        stagingTable = stagingTable.withDatasetFilters(DatasetFilter.of("batch", FilterType.EQUAL_TO, 2));
        datasets = Datasets.of(mainTable, stagingTable);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass2, expectedStatsList, fixedClock_2000_01_01, " order by \"id\", \"income\"");
    }

    @Test
    void testNonTemporalDeltaWithAllVersionDigestBasedAndStagingFilters() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingDataset = DatasetDefinition.builder()
                .group(testSchemaName)
                .name(stagingTableName)
                .schema(getDedupAndVersioningSchemaWithVersionAndBatch)
                .build();

        createStagingTableWithoutPks(stagingDataset);
        DerivedDataset stagingTable = DerivedDataset.builder()
                .group(testSchemaName)
                .name(stagingTableName)
                .schema(getDedupAndVersioningSchemaWithVersion)
                .addDatasetFilters(DatasetFilter.of("batch", FilterType.EQUAL_TO, 1))
                .build();
        String path = basePathForInput + "input/with_staging_filter/with_all_version/digest_based/data1.csv";
        loadStagingDataWithVersionAndBatch(path);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
                .digestField(digestName)
                .auditing(NoAuditing.builder().build())
                .versioningStrategy(AllVersionsStrategy.builder()
                        .versioningField(versionName)
                        .mergeDataVersionResolver(DigestBasedResolver.INSTANCE)
                        .performStageVersioning(true)
                        .build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, versionName, incomeName, expiryDateName, digestName, batchIdName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String expectedDataPass1 = basePathForExpected + "expected/with_staging_filter/with_all_version/digest_based/expected_pass1.csv";
        // 2. Execute plans and verify results
        List<Map<String, Object>> expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats1  = new HashMap<>();
        expectedStats1.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats1.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats1.put(StatisticName.ROWS_DELETED.name(), 0);
        Map<String, Object> expectedStats2  = new HashMap<>();
        expectedStats2.put(StatisticName.INCOMING_RECORD_COUNT.name(), 1);
        expectedStats2.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats2.put(StatisticName.ROWS_DELETED.name(), 0);
        Map<String, Object> expectedStats3  = new HashMap<>();
        expectedStats3.put(StatisticName.INCOMING_RECORD_COUNT.name(), 1);
        expectedStats3.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats3.put(StatisticName.ROWS_DELETED.name(), 0);
        expectedStatsList.add(expectedStats1);
        expectedStatsList.add(expectedStats2);
        expectedStatsList.add(expectedStats3);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStatsList, fixedClock_2000_01_01, " order by \"id\", \"income\"");

        // ------------ Perform incremental (delta) milestoning Pass2 Filter Duplicates ------------------------
        String expectedDataPass2 = basePathForExpected + "expected/with_staging_filter/with_all_version/digest_based/expected_pass2.csv";
        expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats4  = new HashMap<>();
        expectedStats4.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats4.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats4.put(StatisticName.ROWS_DELETED.name(), 0);
        expectedStatsList.add(expectedStats4);
        stagingTable = stagingTable.withDatasetFilters(DatasetFilter.of("batch", FilterType.EQUAL_TO, 2));
        datasets = Datasets.of(mainTable, stagingTable);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass2, expectedStatsList, fixedClock_2000_01_01, " order by \"id\", \"income\"");
    }

    @Test
    void testNonTemporalDeltaWithAllVersionDigestBasedAndFilteredDataset() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingDataset = DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getDedupAndVersioningSchemaWithVersionAndBatch)
            .build();

        createStagingTableWithoutPks(stagingDataset);
        FilteredDataset stagingTable = FilteredDataset.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getDedupAndVersioningSchemaWithVersion)
            .filter(Equals.of(FieldValue.builder()
                .fieldName(batchName)
                .datasetRefAlias(stagingTableName)
                .build(), NumericalValue.of(1L)))
            .build();
        String path = basePathForInput + "input/with_staging_filter/with_all_version/digest_based/data1.csv";
        loadStagingDataWithVersionAndBatch(path);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestName)
            .auditing(NoAuditing.builder().build())
            .versioningStrategy(AllVersionsStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(DigestBasedResolver.INSTANCE)
                .performStageVersioning(true)
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, versionName, incomeName, expiryDateName, digestName, batchIdName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String expectedDataPass1 = basePathForExpected + "expected/with_staging_filter/with_all_version/digest_based/expected_pass1.csv";
        // 2. Execute plans and verify results
        List<Map<String, Object>> expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats1  = new HashMap<>();
        expectedStats1.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats1.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats1.put(StatisticName.ROWS_DELETED.name(), 0);
        Map<String, Object> expectedStats2  = new HashMap<>();
        expectedStats2.put(StatisticName.INCOMING_RECORD_COUNT.name(), 1);
        expectedStats2.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats2.put(StatisticName.ROWS_DELETED.name(), 0);
        Map<String, Object> expectedStats3  = new HashMap<>();
        expectedStats3.put(StatisticName.INCOMING_RECORD_COUNT.name(), 1);
        expectedStats3.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats3.put(StatisticName.ROWS_DELETED.name(), 0);
        expectedStatsList.add(expectedStats1);
        expectedStatsList.add(expectedStats2);
        expectedStatsList.add(expectedStats3);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStatsList, fixedClock_2000_01_01, " order by \"id\", \"income\"");

        // ------------ Perform incremental (delta) milestoning Pass2 Filter Duplicates ------------------------
        String expectedDataPass2 = basePathForExpected + "expected/with_staging_filter/with_all_version/digest_based/expected_pass2.csv";
        expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats4  = new HashMap<>();
        expectedStats4.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats4.put(StatisticName.ROWS_TERMINATED.name(), 0);
        expectedStats4.put(StatisticName.ROWS_DELETED.name(), 0);
        expectedStatsList.add(expectedStats4);
        stagingTable = stagingTable.withFilter(Equals.of(FieldValue.builder()
            .fieldName(batchName)
            .datasetRefAlias(stagingTableName)
            .build(), NumericalValue.of(2L)));
        datasets = Datasets.of(mainTable, stagingTable);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass2, expectedStatsList, fixedClock_2000_01_01, " order by \"id\", \"income\"");
    }
}