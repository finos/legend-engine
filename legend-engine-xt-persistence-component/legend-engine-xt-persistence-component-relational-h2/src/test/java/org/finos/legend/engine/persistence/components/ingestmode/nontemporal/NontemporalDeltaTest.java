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

import java.util.Arrays;
import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.TestUtils.batchUpdateTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.deleteIndicatorName;
import static org.finos.legend.engine.persistence.components.TestUtils.deleteIndicatorValues;
import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.TestUtils.startTimeName;

class NontemporalDeltaTest extends BaseTest
{
    private final String basePath = "src/test/resources/data/incremental-delta-milestoning/";

    /*
    Scenario: Test milestoning Logic when staging table pre populated
     */
    @Test
    void testMilestoningStagingTablePrePopulated() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
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

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String dataPass1 = basePath + "input/vanilla_case/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/vanilla_case/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform incremental (delta) milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/vanilla_case/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/vanilla_case/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass2);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging data comes from CSV and has less columns than main dataset
    */
    @Test
    void testIncrementalDeltaMilestoningLogicWithLessColumnsInStaging() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        String dataPass1 = basePath + "input/less_columns_in_staging/data_pass1.csv";
        Dataset stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass1);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestName)
            .auditing(NoAuditing.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String expectedDataPass1 = basePath + "expected/less_columns_in_staging/expected_pass1.csv";
        // Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform incremental (delta) milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/less_columns_in_staging/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/less_columns_in_staging/expected_pass2.csv";
        stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass2);
        // Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass2, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging table is pre populated and
    staging table is cleaned up in the end
    */
    @Test
    void testGeneratePhysicalPlanWithCleanStagingData() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestName)
            .auditing(NoAuditing.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(true).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform incremental (delta) milestoning With Clean Staging Table ------------------------
        String dataPass1 = basePath + "input/vanilla_case/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/vanilla_case/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 0);
    }

    /*
    Scenario: Test milestoning Logic when staging table is pre populated
    and isUpdateBatchTimeEnabled is enabled
    */
    @Test
    void testGeneratePhysicalPlanWithUpdateTimestampColumn() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getMainTableWithbatchUpdateTimeField();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        NontemporalDelta ingestMode = NontemporalDelta.builder()
            .digestField(digestName)
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String dataPass1 = basePath + "input/with_update_timestamp_field/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/with_update_timestamp_field/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 2);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);
    }

    /*
    Scenario: Test milestoning Logic when staging table pre populated + delete indicator
     */
    @Test
    void testMilestoningStagingTablePrePopulatedWithDeleteIndicator() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
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
        Datasets datasets = Datasets.builder().mainDataset(mainTable).stagingDataset(stagingTable).build();

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform incremental (delta) milestoning Pass1 ------------------------
        String dataPass1 = basePath + "input/with_delete_indicator/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/with_delete_indicator/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithDeleteInd(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);

        // ------------ Perform incremental (delta) milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/with_delete_indicator/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/with_delete_indicator/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithDeleteInd(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 1);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);
        // 3. Assert that the staging table is NOT truncated
        stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 3);
    }
}
