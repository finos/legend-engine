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

import org.finos.legend.engine.persistence.components.IncrementalClock;
import org.finos.legend.engine.persistence.components.e2e.BaseTest;
import org.finos.legend.engine.persistence.components.e2e.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.AllowDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UserProvidedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.DigestBasedResolver;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.NoVersioningStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FilteredDataset;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.relational.postgres.PostgresSink;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.e2e.TestUtils.batchIdName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.batchUpdateTimeName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.dataSplitName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.startTimeName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.versionName;

class AppendOnlyTest extends BaseTest
{
    private final String basePathForInput = "data/incremental-append-milestoning/";
    private final String basePathForExpected = "src/test/resources/data/incremental-append-milestoning/";

    /*
    Scenario: Test Append Only vanilla case + staging table is cleaned up in the end with upper case
    */
    @Test
    void testAppendOnlyVanillaUpperCase() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithNoPks();

        // Create staging table
        postgresSink.executeStatement("CREATE TABLE IF NOT EXISTS \"TEST\".\"STAGING\"(\"NAME\" VARCHAR(64) NOT NULL,\"INCOME\" BIGINT,\"EXPIRY_DATE\" DATE)");

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(AllowDuplicates.builder().build())
            .versioningStrategy(NoVersioningStrategy.builder().build())
            .auditing(NoAuditing.builder().build())
            .filterExistingRecords(false)
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(true).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{nameName.toUpperCase(), incomeName.toUpperCase(), expiryDateName.toUpperCase(), batchIdName.toUpperCase()};

        // ------------ Perform incremental (append) milestoning With Clean Staging Table ------------------------
        String dataPass1 = basePathForInput + "input/vanilla_case/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/vanilla_case/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithNoPkInUpperCase(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"BATCH_ID\", \"INCOME\"");
        // 3. Assert that the staging table is truncated
        List<Map<String, Object>> stagingTableList = postgresSink.executeQuery("select * from \"TEST\".\"STAGING\"");
        Assertions.assertEquals(stagingTableList.size(), 0);

        // ------------ Perform incremental (append) milestoning With Clean Staging Table ------------------------
        String dataPass2 = basePathForInput + "input/vanilla_case/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/vanilla_case/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithNoPkInUpperCase(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, " order by \"BATCH_ID\", \"INCOME\"");
        // 3. Assert that the staging table is truncated
        stagingTableList = postgresSink.executeQuery("select * from \"TEST\".\"STAGING\"");
        Assertions.assertEquals(stagingTableList.size(), 0);
    }

    /*
    Scenario: Test Append Only vanilla case + staging table is cleaned up in the end with upper case
    */
    @Test
    void testAppendOnlyVanillaUpperCaseWithFilteredDataset() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        FilteredDataset stagingTable = TestUtils.getFilteredStagingTableWithComplexFilter();

        // Create staging table
        postgresSink.executeStatement("CREATE TABLE IF NOT EXISTS \"TEST\".\"STAGING\"(\"NAME\" VARCHAR(64) NOT NULL,\"INCOME\" BIGINT,\"EXPIRY_DATE\" DATE)");

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(AllowDuplicates.builder().build())
            .versioningStrategy(NoVersioningStrategy.builder().build())
            .auditing(NoAuditing.builder().build())
            .filterExistingRecords(false)
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{nameName.toUpperCase(), incomeName.toUpperCase(), expiryDateName.toUpperCase(), batchIdName.toUpperCase()};

        // ------------ Perform incremental (append) milestoning With Clean Staging Table ------------------------
        String dataPass1 = basePathForInput + "input/with_staging_filter/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/with_staging_filter/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithNoPkInUpperCase(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(2, 0, 2, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, " order by \"BATCH_ID\", \"INCOME\"");

        // ------------ Perform incremental (append) milestoning With Clean Staging Table ------------------------
        String dataPass2 = basePathForInput + "input/with_staging_filter/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/with_staging_filter/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithNoPkInUpperCase(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(2, 0, 2, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, " order by \"BATCH_ID\", \"INCOME\"");
    }

    /*
    Scenario: Test Append Only with auditing, no versioning, filter duplicates and filter existing records
    */
    @Test
    void testAppendOnlyWithAuditingNoVersioningFilterDuplicatesFilterExistingRecords() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTableWithoutPks(stagingTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .versioningStrategy(NoVersioningStrategy.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .filterExistingRecords(true)
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/auditing_no_version_filter_dup_filter_existing/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/auditing_no_version_filter_dup_filter_existing/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01, " order by \"batch_id\", \"income\"");

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "input/auditing_no_version_filter_dup_filter_existing/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/auditing_no_version_filter_dup_filter_existing/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 2, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_02, " order by \"batch_id\", \"income\"");
    }

    /*
    Scenario: Test Append Only with auditing, max version, filter duplicates and filter existing records with upper case
    */
    @Test
    void testAppendOnlyWithAuditingMaxVersionFilterDuplicatesFilterExistingRecordsUpperCase() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithNonPkVersion();

        // Create staging table
        postgresSink.executeStatement("CREATE TABLE IF NOT EXISTS \"TEST\".\"STAGING\"(\"ID\" INTEGER NOT NULL,\"NAME\" VARCHAR(64) NOT NULL,\"INCOME\" BIGINT,\"START_TIME\" TIMESTAMP NOT NULL,\"EXPIRY_DATE\" DATE,\"DIGEST\" VARCHAR,\"VERSION\" INT)");

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(DigestBasedResolver.INSTANCE)
                .performStageVersioning(true)
                .build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .filterExistingRecords(true)
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName.toUpperCase(), nameName.toUpperCase(), incomeName.toUpperCase(), startTimeName.toUpperCase(), expiryDateName.toUpperCase(), digestName.toUpperCase(), versionName.toUpperCase(), batchUpdateTimeName.toUpperCase(), batchIdName.toUpperCase()};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/auditing_max_version_filter_dup_filter_existing/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/auditing_max_version_filter_dup_filter_existing/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersionInUpperCase(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(4, 0, 3, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01, " order by \"BATCH_ID\", \"INCOME\"");

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "input/auditing_max_version_filter_dup_filter_existing/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/auditing_max_version_filter_dup_filter_existing/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersionInUpperCase(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 2, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_02, " order by \"BATCH_ID\", \"INCOME\"");
    }

    /*
    Scenario: Test Append Only with auditing, max version, filter duplicates and no filter existing records
    */
    @Test
    void testAppendOnlyWithAuditingMaxVersionFilterDuplicatesNoFilterExistingRecords() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithNonPkVersion();

        // Create staging table
        createStagingTableWithoutPks(stagingTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .versioningStrategy(MaxVersionStrategy.builder()
                .versioningField(versionName)
                .mergeDataVersionResolver(DigestBasedResolver.INSTANCE)
                .performStageVersioning(true)
                .build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .filterExistingRecords(false)
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchUpdateTimeName, batchIdName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/auditing_max_version_filter_dup_no_filter_existing/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/auditing_max_version_filter_dup_no_filter_existing/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(4, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01, " order by \"batch_id\", \"income\"");

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "input/auditing_max_version_filter_dup_no_filter_existing/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/auditing_max_version_filter_dup_no_filter_existing/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_02, " order by \"batch_id\", \"income\"");
    }

    /*
    Scenario: Test Append Only with auditing, all version, filter duplicates and filter existing records
    */
    @Test
    void testAppendOnlyWithAuditingAllVersionFilterDuplicatesFilterExistingRecords() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithNonPkVersion();
        IncrementalClock incrementalClock = new IncrementalClock(fixedExecutionZonedDateTime1.toInstant(), ZoneOffset.UTC, 1000);

        // Create staging table
        createStagingTableWithoutPks(stagingTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .versioningStrategy(AllVersionsStrategy.builder()
                .versioningField(versionName)
                .dataSplitFieldName(dataSplitName)
                .mergeDataVersionResolver(DigestBasedResolver.INSTANCE)
                .performStageVersioning(true)
                .build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .filterExistingRecords(true)
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchUpdateTimeName, batchIdName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/auditing_all_version_filter_dup_filter_existing/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/auditing_all_version_filter_dup_filter_existing/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass1);
        // 2. Execute plans and verify results
        List<Map<String, Object>> expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats1 = createExpectedStatsMap(3, 0, 3, 0, 0);
        Map<String, Object> expectedStats2 = createExpectedStatsMap(1, 0, 1, 0, 0);
        expectedStatsList.add(expectedStats1);
        expectedStatsList.add(expectedStats2);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStatsList, incrementalClock, " order by \"batch_id\", \"income\"");

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "input/auditing_all_version_filter_dup_filter_existing/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/auditing_all_version_filter_dup_filter_existing/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStatsList = new ArrayList<>();
        expectedStats1 = createExpectedStatsMap(4, 0, 2, 0, 0);
        expectedStatsList.add(expectedStats1);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass2, expectedStatsList, incrementalClock, " order by \"batch_id\", \"income\"");
    }

    /*
    Scenario: Test Append Only with auditing, all version, filter duplicates and no filter existing records
    */
    @Test
    void testAppendOnlyWithAuditingAllVersionFilterDuplicatesNoFilterExistingRecords() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithNonPkVersion();
        IncrementalClock incrementalClock = new IncrementalClock(fixedExecutionZonedDateTime1.toInstant(), ZoneOffset.UTC, 1000);

        // Create staging table
        createStagingTableWithoutPks(stagingTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .versioningStrategy(AllVersionsStrategy.builder()
                .versioningField(versionName)
                .dataSplitFieldName(dataSplitName)
                .mergeDataVersionResolver(DigestBasedResolver.INSTANCE)
                .performStageVersioning(true)
                .build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .filterExistingRecords(false)
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, versionName, batchUpdateTimeName, batchIdName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/auditing_all_version_filter_dup_no_filter_existing/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/auditing_all_version_filter_dup_no_filter_existing/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass1);
        // 2. Execute plans and verify results
        List<Map<String, Object>> expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats1 = createExpectedStatsMap(3, 0, 3, 0, 0);
        Map<String, Object> expectedStats2 = createExpectedStatsMap(1, 0, 1, 0, 0);
        expectedStatsList.add(expectedStats1);
        expectedStatsList.add(expectedStats2);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStatsList, incrementalClock, " order by \"batch_id\", \"income\"");

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String dataPass2 = basePathForInput + "input/auditing_all_version_filter_dup_no_filter_existing/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/auditing_all_version_filter_dup_no_filter_existing/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStatsList = new ArrayList<>();
        expectedStats1 = createExpectedStatsMap(4, 0, 3, 0, 0);
        expectedStatsList.add(expectedStats1);
        expectedStatsList.add(expectedStats2);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass2, expectedStatsList, incrementalClock, " order by \"batch_id\", \"income\"");
    }

    /*
    Scenario: Test Append Only with auditing, no version, allow duplicates and no filter existing records when staging has lesser columns than main
    */
    @Test
    void testAppendOnlyWithLessColumnsInStaging() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getDatasetWithLessColumnsThanMain();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(AllowDuplicates.builder().build())
            .versioningStrategy(NoVersioningStrategy.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .filterExistingRecords(false)
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String dataPass1 = basePathForInput + "input/less_columns_in_staging/data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "expected/less_columns_in_staging/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingDataWithColumnsThanMain(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01, " order by \"batch_id\", \"income\"");

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = basePathForInput + "input/less_columns_in_staging/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/less_columns_in_staging/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingDataWithColumnsThanMain(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_02, " order by \"batch_id\", \"income\"");
    }

    /*
    Scenario: Test Append Only vanilla case with do not create table
    */
    @Test
    void testAppendOnlyDoNotCreateTables() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithNoPks();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(AllowDuplicates.builder().build())
            .versioningStrategy(NoVersioningStrategy.builder().build())
            .auditing(NoAuditing.builder().build())
            .filterExistingRecords(false)
            .build();

        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        RelationalIngestor ingestor = RelationalIngestor.builder()
            .ingestMode(ingestMode)
            .relationalSink(PostgresSink.get())
            .createDatasets(false)
            .build();
        try
        {
            ingestor.performFullIngestion(JdbcConnection.of(postgresSink.connection()), datasets);
            Assertions.fail("Should not be successful");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e.getMessage().contains("relation \"batch_metadata\" does not exist"));
        }
    }
}
