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
import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.relational.api.DataSplitRange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.TestUtils.batchUpdateTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.dataSplitName;
import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.TestUtils.startTimeName;

class NontemporalSnapshotTest extends BaseTest
{
    private final String basePath = "src/test/resources/data/snapshot-milestoning/";

    /*
    Scenarios:
    1. No Auditing
    2. With Auditing
    3. No Auditing & import external JSON dataset
    4. No Auditing & import external CSV dataset
    5. Staging has lesser columns than main dataset
    6. Staging data cleanup
    7. Data Splits enabled
     */

    /*
    Scenario: Test Nontemporal Snapshot with no auditing
     */
    @Test
    void testNontemporalSnapshotNoAuditing() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePath + "input/vanilla_case/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/vanilla_case/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results

        Map<String, Object> expectedStats = createExpectedStatsMap(5, 0, 5, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 5);

        // ------------ Perform snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/vanilla_case/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/vanilla_case/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass2);
        // 2. Execute plans and verify results
        expectedStats.clear();
        expectedStats = createExpectedStatsMap(6, 5, 6, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);
    }

    /*
    Scenario: Test Nontemporal Snapshot when auditing is enabled
    */
    @Test
    void testNontemporalSnapshotWithAuditing() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getMainTableWithBatchUpdateTimeField();
        String dataPass1 = basePath + "input/with_update_timestamp_field/data_pass1.csv";
        Dataset stagingTable = TestUtils.getBasicCsvDatasetReferenceTable(dataPass1);

        // Generate the milestoning object
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder()
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
    Scenario: Test Nontemporal Snapshot when staging data comes from JSON and auditing is disabled
     */
    @Test
    void testNontemporalSnapshotImportExternalJson() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        String dataPass1 = basePath + "input/vanilla_case/data_pass1.json";
        Dataset stagingTable = TestUtils.getBasicJsonDatasetReferenceTable(dataPass1);

        // Generate the milestoning object
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform snapshot milestoning Pass1 ------------------------
        String expectedDataPass1 = basePath + "expected/vanilla_case/expected_pass1.csv";
        Map<String, Object> expectedStats = createExpectedStatsMap(5, 0, 5, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/vanilla_case/data_pass2.json";
        stagingTable = TestUtils.getBasicJsonDatasetReferenceTable(dataPass2);

        String expectedDataPass2 = basePath + "expected/vanilla_case/expected_pass2.csv";
        expectedStats.clear();
        expectedStats = createExpectedStatsMap(6, 5, 6, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass2, expectedStats);
    }

    /*
    Scenario: Test Nontemporal Snapshot when staging data comes from CSV and auditing is disabled
    */
    @Test
    void testNontemporalSnapshotImportExternalCsv() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        String dataPass1 = basePath + "input/vanilla_case/data_pass1.csv";
        Dataset stagingTable = TestUtils.getBasicCsvDatasetReferenceTable(dataPass1);

        // Generate the milestoning object
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform snapshot milestoning Pass1 ------------------------
        String expectedDataPass1 = basePath + "expected/vanilla_case/expected_pass1.csv";
        Map<String, Object> expectedStats = createExpectedStatsMap(5, 0, 5, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/vanilla_case/data_pass2.csv";
        stagingTable = TestUtils.getBasicCsvDatasetReferenceTable(dataPass2);

        String expectedDataPass2 = basePath + "expected/vanilla_case/expected_pass2.csv";
        expectedStats.clear();
        expectedStats = createExpectedStatsMap(6, 5, 6, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass2, expectedStats);
    }

    /*
    Scenario: Test Nontemporal Snapshot when staging data comes from CSV and has lesser columns than main dataset
    */
    @Test
    void testNontemporalSnapshotWithLessColumnsInStaging() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        String dataPass1 = basePath + "input/less_columns_in_staging/data_pass1.csv";
        Dataset stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass1);

        // Generate the milestoning object
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform snapshot milestoning Pass1 ------------------------
        String expectedDataPass1 = basePath + "expected/less_columns_in_staging/expected_pass1.csv";
        // Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(5, 0, 5, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/less_columns_in_staging/data_pass2.csv";
        stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass2);
        String expectedDataPass2 = basePath + "expected/less_columns_in_staging/expected_pass2.csv";
        // Execute plans and verify results
        expectedStats.clear();
        expectedStats = createExpectedStatsMap(6, 5, 6, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass2, expectedStats);
    }

    /*
    Scenario: Test Nontemporal Snapshot when staging table is cleaned up in the end
     */
    @Test
    void testNontemporalSnapshotWithCleanStagingData() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(true).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform snapshot milestoning With Clean Staging Table ------------------------
        String dataPass1 = basePath + "input/vanilla_case/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/vanilla_case/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(5, 0, 5, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 0);
    }

    /*
    Scenario: Test Nontemporal Snapshot when data splits are enabled
    */
    @Test
    void testNontemporalSnapshotWithDataSplits() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        String dataPass1 = basePath + "input/with_data_splits/data_pass1.csv";
        Dataset stagingTable = TestUtils.getBasicCsvDatasetReferenceTableWithDataSplits(dataPass1);

        // Generate the milestoning object
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder()
                .auditing(NoAuditing.builder().build())
                .dataSplitField(dataSplitName)
                .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String expectedDataPass1 = basePath + "expected/with_data_splits/expected_pass1.csv";
        // Execute plans and verify results
        List<DataSplitRange> dataSplitRanges = new ArrayList<>();
        dataSplitRanges.add(DataSplitRange.of(1, 1));
        dataSplitRanges.add(DataSplitRange.of(2, 2));
        dataSplitRanges.add(DataSplitRange.of(3, 3));

        List<Map<String, Object>> expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats = createExpectedStatsMap(5, 0, 3, 0, 0);
        expectedStatsList.add(expectedStats);
        executePlansAndVerifyResultsWithDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStatsList, dataSplitRanges);
    }

}
