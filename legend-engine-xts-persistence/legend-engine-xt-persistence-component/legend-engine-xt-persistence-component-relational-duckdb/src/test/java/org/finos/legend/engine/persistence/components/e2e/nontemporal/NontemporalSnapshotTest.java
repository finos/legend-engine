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

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.e2e.BaseTest;
import org.finos.legend.engine.persistence.components.e2e.TestUtils;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.NoVersioningStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FilteredDataset;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.e2e.TestUtils.batchIdName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.batchUpdateTimeName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.getDedupAndVersioningSchemaWithVersion;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.getDedupAndVersioningSchemaWithoutVersion;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.stagingTableName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.startTimeName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.testSchemaName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.versionName;

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
    7. With Auditing, Max Version, Filter Duplicates
    8. With Auditing, No Version, Fail on Duplicates
     */

    @Test
    void test() throws Exception
    {
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();
        createStagingTable(stagingTable);

        duckDBSink.executeStatement("CREATE TABLE IF NOT EXISTS \"TEST\".\"main\"(\"id\" INTEGER NOT NULL,\"name\" VARCHAR(64) NOT NULL,\"income\" BIGINT,\"start_time\" TIMESTAMP NOT NULL,\"expiry_date\" DATE,\"digest\" VARCHAR,\"batch_id\" INTEGER,PRIMARY KEY (\"id\", \"start_time\"))");
        duckDBSink.executeStatement("CREATE TABLE IF NOT EXISTS batch_metadata(\"table_name\" VARCHAR(255),\"batch_start_ts_utc\" TIMESTAMP,\"batch_end_ts_utc\" TIMESTAMP,\"batch_status\" VARCHAR(32),\"table_batch_id\" INTEGER,\"batch_source_info\" JSON,\"additional_metadata\" JSON)");

        String dataPass1 = basePath + "input/vanilla_case/data_pass1.csv";
        loadBasicStagingData(dataPass1);

        duckDBSink.executeStatement("DELETE FROM \"TEST\".\"main\" as main");
        duckDBSink.executeStatement("INSERT INTO \"TEST\".\"main\" (\"id\", \"name\", \"income\", \"start_time\", \"expiry_date\", \"digest\", \"batch_id\") (SELECT staging.\"id\" as \"id\",staging.\"name\" as \"name\",staging.\"income\" as \"income\",staging.\"start_time\" as \"start_time\",staging.\"expiry_date\" as \"expiry_date\",staging.\"digest\" as \"digest\",1 FROM \"TEST\".\"staging\" as staging)");
        duckDBSink.executeStatement("INSERT INTO batch_metadata (\"table_name\", \"table_batch_id\", \"batch_start_ts_utc\", \"batch_end_ts_utc\", \"batch_status\") (SELECT 'main',1,'2024-05-24 06:08:29.590999','2024-05-24 06:08:29.599921','DONE')\n");
        List<Map<String, Object>> result = duckDBSink.executeQuery("SELECT * FROM \"TEST\".\"main\"");

        String dataPass2 = basePath + "input/vanilla_case/data_pass2.csv";
        loadBasicStagingData(dataPass2);

        duckDBSink.executeStatement("DELETE FROM \"TEST\".\"main\" as main");
        duckDBSink.executeStatement("INSERT INTO \"TEST\".\"main\" (\"id\", \"name\", \"income\", \"start_time\", \"expiry_date\", \"digest\", \"batch_id\") (SELECT staging.\"id\" as \"id\",staging.\"name\" as \"name\",staging.\"income\" as \"income\",staging.\"start_time\" as \"start_time\",staging.\"expiry_date\" as \"expiry_date\",staging.\"digest\" as \"digest\",2 FROM \"TEST\".\"staging\" as staging)");
        result = duckDBSink.executeQuery("SELECT * FROM \"TEST\".\"main\"");
        System.out.println("I'm here");
    }
    /*
    Scenario: Test Nontemporal Snapshot with no auditing
     */
    @Test
    void testNontemporalSnapshotNoAuditing() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdName};

        // ------------ Perform snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePath + "input/vanilla_case/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/vanilla_case/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(5, 0, 5, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\"");
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = duckDBSink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 5);
        List<Map<String, Object>> mainTableList = duckDBSink.executeQuery("select * from \"TEST\".\"main\"");

        // ------------ Perform snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/vanilla_case/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/vanilla_case/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass2);
        // 2. Execute plans and verify results
        expectedStats.clear();
        expectedStats = createExpectedStatsMap(6, 5, 6, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, " order by \"id\"");
    }

    /*
    Scenario: Test Nontemporal Snapshot with no auditing
     */
    @Test
    void testNontemporalSnapshotNoAuditingWithFilteredDataset() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        FilteredDataset stagingTable = TestUtils.getFilteredStagingTable();

        // Create staging table
        DatasetDefinition stagingTableForDB = TestUtils.getStagingTableWithFilterForDB();
        createStagingTable(stagingTableForDB);

        // Generate the milestoning object
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdName};

        // ------------ Perform snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePath + "input/with_staging_filter/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/with_staging_filter/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithFilter(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(5, 0, 5, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\"");

        // ------------ Perform snapshot milestoning Pass2 ------------------------
        // 0. Create new filter
        datasets = Datasets.of(mainTable, TestUtils.getFilteredStagingTableSecondPass());
        String dataPass2 = basePath + "input/with_staging_filter/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/with_staging_filter/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithFilter(dataPass2);
        // 2. Execute plans and verify results
        expectedStats.clear();
        expectedStats = createExpectedStatsMap(3, 5, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, " order by \"id\"");
    }

    /*
    Scenario: Test Nontemporal Snapshot when auditing is enabled
    */
    @Test
    void testNontemporalSnapshotWithAuditing() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getMainTableWithBatchUpdateTimeField();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder()
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String dataPass1 = basePath + "input/with_update_timestamp_field/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/with_update_timestamp_field/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01, " order by \"id\"");
    }

    /*
    Scenario: Test Nontemporal Snapshot when staging data comes from CSV and has lesser columns than main dataset
    */
    @Test
    void testNontemporalSnapshotWithLessColumnsInStaging() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        DatasetDefinition stagingTable = TestUtils.getDatasetWithLessColumnsThanMain();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdName};

        // ------------ Perform snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePath + "input/less_columns_in_staging/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/less_columns_in_staging/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingDataWithLessColumnsThanMain(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(5, 0, 5, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\"");

        // ------------ Perform snapshot milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/less_columns_in_staging/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/less_columns_in_staging/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingDataWithLessColumnsThanMain(dataPass2);
        // 2. Execute plans and verify results
        expectedStats.clear();
        expectedStats = createExpectedStatsMap(6, 5, 6, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, " order by \"id\"");
    }

    /*
    Scenario: Test Nontemporal Snapshot when staging table is cleaned up in the end
     */
    @Test
    void testNontemporalSnapshotWithCleanStagingData() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(true).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdName};

        // ------------ Perform snapshot milestoning With Clean Staging Table ------------------------
        String dataPass1 = basePath + "input/vanilla_case/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/vanilla_case/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(5, 0, 5, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\"");
        // 3. Assert that the staging table is truncated
        List<Map<String, Object>> stagingTableList = duckDBSink.executeQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 0);
    }

    /*
    Scenario: Test Nontemporal Snapshot when MaxVersion and FilterDuplicates are enabled
    */
    @Test
    void testNontemporalSnapshotWithMaxVersionAndFilterDuplicates() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getDedupAndVersioningSchemaWithVersion)
            .build();

        // Create staging table
        createStagingTableWithoutPks(stagingTable);

        // Generate the milestoning object
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder()
                .auditing(NoAuditing.builder().build())
                .versioningStrategy(MaxVersionStrategy.builder().versioningField("version").build())
                .deduplicationStrategy(FilterDuplicates.builder().build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, versionName, incomeName, expiryDateName, digestName, batchIdName};

        // ------------ Perform snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePath + "input/dedup_and_versioning/data2_with_dups_no_data_error.csv";
        String expectedDataPass1 = basePath + "expected/max_version_filter_duplicates/expected_pass1.csv";
        // 1. Load staging table
        loadDedupAndVersioningStagingDataWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(6, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\"");

        // ------------ Perform snapshot milestoning Pass2 ------------------------
        // Throw Data Error
        String dataPass2 = basePath + "input/dedup_and_versioning/data3_with_dups_and_data_error.csv";
        // 1. Load staging table
        loadDedupAndVersioningStagingDataWithVersion(dataPass2);
        // 2. Execute plans and verify results
        try
        {
            executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\"");
            Assertions.fail("Should not succeed");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Encountered Data errors (same PK, same version but different data), hence failing the batch", e.getMessage());
        }
    }

    /*
    Scenario: Test Nontemporal Snapshot when No Version and FailOnDuplicates
    */
    @Test
    void testNontemporalSnapshotWithFailOnDupsNoVersioning() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = DatasetDefinition.builder()
            .group(testSchemaName)
            .name(stagingTableName)
            .schema(getDedupAndVersioningSchemaWithoutVersion)
            .build();

        // Create staging table
        createStagingTableWithoutPks(stagingTable);

        // Generate the milestoning object
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder()
                .auditing(NoAuditing.builder().build())
                .versioningStrategy(NoVersioningStrategy.builder().build())
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, expiryDateName, digestName, batchIdName};

        // ------------ Perform snapshot milestoning Pass1 ------------------------
        String dataPass1 = basePath + "input/dedup_and_versioning/data5_without_dups.csv";
        String expectedDataPass1 = basePath + "expected/no_versioning_fail_on_duplicates/expected_pass1.csv";
        // 1. Load staging table
        loadDedupAndVersioningStagingDataWithoutVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\"");

        // ------------ Perform snapshot milestoning Pass2 ------------------------
        // Throw Data Error
        String dataPass2 = basePath + "input/dedup_and_versioning/data1_with_dups.csv";
        // 1. Load staging table
        loadDedupAndVersioningStagingDataWithoutVersion(dataPass2);
        // 2. Execute plans and verify results
        try
        {
            executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"id\"");
            Assertions.fail("Should not succeed");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Encountered Duplicates, Failing the batch as Fail on Duplicates is set as Deduplication strategy", e.getMessage());
        }
    }
}
