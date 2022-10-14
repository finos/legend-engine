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

package org.finos.legend.engine.persistence.components.ingestmode.nontemporal;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.IncrementalClock;
import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.TestUtils.batchUpdateTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.TestUtils.startTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.dataSplitName;

class AppendOnlyTest extends BaseTest
{
    private final String basePath = "src/test/resources/data/incremental-append-milestoning/";

    /*
    Scenario: Test milestoning Logic when staging table pre populated
    */
    @Test
    void testIncrementalAppendMilestoningLogic() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestName)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(NoAuditing.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String dataPass1 = basePath + "input/vanilla_case/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/vanilla_case/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        expectedStats.put(StatisticName.ROWS_UPDATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/vanilla_case/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/vanilla_case/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass2);
        // 2. Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging table pre populated
    */
    @Test
    void testIncrementalAppendWithStagingDataImportedWithPopulateDigest() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        String dataPass1 = "src/test/resources/data/import-data/data_pass1.json";
        Dataset stagingTable = TestUtils.getJsonDatasetWithoutDigestReferenceTable(dataPass1);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestName)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(NoAuditing.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String expectedDataPass1 = "src/test/resources/data/import-data/data_expected_with_digest_pass1.csv";
        // Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 5);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        expectedStats.put(StatisticName.ROWS_UPDATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);

        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = "src/test/resources/data/import-data/data_pass2.json";
        stagingTable = TestUtils.getJsonDatasetWithoutDigestReferenceTable(dataPass2);
        String expectedDataPass2 = "src/test/resources/data/import-data/data_expected_with_digest_pass2.csv";
        // Execute plans and verify results
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 2);
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass2, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging data comes from CSV and has less columns than main dataset
    */
    @Test
    void testIncrementalAppendMilestoningLogicWithLessColumnsInStaging() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        String dataPass1 = basePath + "input/less_columns_in_staging/data_pass1.csv";
        Dataset stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass1);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestName)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(NoAuditing.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String expectedDataPass1 = basePath + "expected/less_columns_in_staging/expected_pass1.csv";
        // Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        expectedStats.put(StatisticName.ROWS_UPDATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);

        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/less_columns_in_staging/data_pass2.csv";
        stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass2);
        String expectedDataPass2 = basePath + "expected/less_columns_in_staging/expected_pass2.csv";
        // Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass2, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging table is pre populated and
    staging table is cleaned up in the end
    */
    @Test
    void testIncrementalAppendMilestoningLogicWithCleanStagingData() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestName)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(NoAuditing.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(true).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform incremental (append) milestoning With Clean Staging Table ------------------------
        String dataPass1 = basePath + "input/vanilla_case/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/vanilla_case/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        expectedStats.put(StatisticName.ROWS_UPDATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);

        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 0);
    }

    /*
    Scenario: Test milestoning Logic when staging data comes from CSV
    and isUpdateBatchTimeEnabled is enabled
    */
    @Test
    void testIncrementalAppendMilestoningLogicWithUpdateTimestampField() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getMainTableWithbatchUpdateTimeField();
        String dataPass1 = basePath + "input/with_update_timestamp_field/data_pass1.csv";
        Dataset stagingTable = TestUtils.getBasicCsvDatasetReferenceTable(dataPass1);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestName)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String expectedDataPass1 = basePath + "expected/with_update_timestamp_field/expected_pass1.csv";
        // Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
    }

    /*
    Scenario: Test milestoning logic when staging table is pre populated and
    Staging table has extra column which should be handled invoke schema evolution
    */
    @Test
    void testIncrementalAppendMilestoningLogicWithAddColumnEvolution() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getSchemaEvolMainTableWithMissingColumn();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestName)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(NoAuditing.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).enableSchemaEvolution(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String dataPass1 = basePath + "input/vanilla_case/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/vanilla_case/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        expectedStats.put(StatisticName.ROWS_UPDATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/vanilla_case/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/vanilla_case/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass2);
        // 2. Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, result.updatedDatasets(), schema, expectedDataPass2, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging table is pre populated and
    Test implicit data change schema evolution
    */
    @Test
    void testIncrementalAppendMilestoningLogicWithImplicitDataTypeChange() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableForImplicitSchemaEvolution();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestName)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(NoAuditing.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).enableSchemaEvolution(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String dataPass1 = basePath + "input/vanilla_case/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/vanilla_case/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        expectedStats.put(StatisticName.ROWS_UPDATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);

        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/vanilla_case/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/vanilla_case/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass2);
        // 2. Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);
    }

    @Test
    void testAppendOnlyWithFilterDuplicatesAuditEnabledWithDataSplits() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getMainTableWithbatchUpdateTimeField();
        String dataPass1 = basePath + "input/with_data_splits/data_pass1.csv";
        Dataset stagingTable = TestUtils.getBasicCsvDatasetReferenceTableWithDataSplits(dataPass1);
        IncrementalClock incrementalClock = new IncrementalClock(fixedExecutionZonedDateTime1.toInstant(), ZoneOffset.UTC, 1000);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
                .digestField(digestName)
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
                .dataSplitField(dataSplitName)
                .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{batchUpdateTimeName, idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String expectedDataPass1 = basePath + "expected/with_data_splits/expected_pass1.csv";
        // Execute plans and verify results
        List<DataSplitRange> dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(1, 1));
        dataSplitRanges.add(DataSplitRange.of(2, 2));
        List<Map<String, Object>> expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats1 = createExpectedStatsMap(3, 0, 3, 0, 0);
        Map<String, Object> expectedStats2 = createExpectedStatsMap(2, 0, 2, 0, 0);

        expectedStatsList.add(expectedStats1);
        expectedStatsList.add(expectedStats2);

        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStatsList, dataSplitRanges, incrementalClock);
    }

}
