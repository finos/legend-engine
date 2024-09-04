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
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.AllowDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UDFBasedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UserProvidedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.AllVersionsStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.DigestBasedResolver;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.MaxVersionStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.NoVersioningStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FilteredDataset;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.api.GeneratorResult;
import org.finos.legend.engine.persistence.components.relational.api.RelationalGenerator;
import org.finos.legend.engine.persistence.components.relational.api.RelationalIngestor;
import org.finos.legend.engine.persistence.components.relational.h2.H2DigestUtil;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.TestUtils.batchIdName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchUpdateTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.dataSplitName;
import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.digestUDF;
import static org.finos.legend.engine.persistence.components.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.TestUtils.startTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.versionName;

class AppendOnlyTest extends BaseTest
{
    private final String basePath = "src/test/resources/data/incremental-append-milestoning/";
    /*
    Scenarios:
    1) With Auditing, NoVersion, Filter Duplicates, true     - tested (perform deduplication, auditing, filter existing)
    2) No Auditing, NoVersion, Allow Duplicates, false       - tested (the most basic case)
    3) With Auditing, MaxVersion, Filter Duplicates, true    - tested (perform deduplication and versioning, auditing, filter existing)
    4) With Auditing, MaxVersion, Filter Duplicates, false   - tested (perform deduplication and versioning, auditing)
    5) With Auditing, AllVersion, Filter Duplicates, true    - tested (perform deduplication and versioning, data split, auditing, filter existing)
    6) With Auditing, AllVersion, Filter Duplicates, false   - tested (perform deduplication and versioning, data split, auditing)

    Other enrichment tests:
    1) Staging data is imported along with Digest field population
    2) Staging has lesser columns than main dataset
    3) Do no create table
    */

    /*
    Scenario: Test Append Only vanilla case + staging table is cleaned up in the end with upper case (2)
    */
    @Test
    void testAppendOnlyVanillaUpperCase() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithNoPks();

        // Create staging table
        h2Sink.executeStatement("CREATE TABLE IF NOT EXISTS \"TEST\".\"STAGING\"(\"NAME\" VARCHAR(64) NOT NULL,\"INCOME\" BIGINT,\"EXPIRY_DATE\" DATE)");

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
        String dataPass1 = basePath + "input/vanilla_case/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/vanilla_case/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithNoPkInUpperCase(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"STAGING\"");
        Assertions.assertEquals(stagingTableList.size(), 0);

        // ------------ Perform incremental (append) milestoning With Clean Staging Table ------------------------
        String dataPass2 = basePath + "input/vanilla_case/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/vanilla_case/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithNoPkInUpperCase(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);
        // 3. Assert that the staging table is truncated
        stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"STAGING\"");
        Assertions.assertEquals(stagingTableList.size(), 0);
    }

    /*
    Scenario: Test Append Only vanilla case + staging table is cleaned up in the end with upper case (2)
    */
    @Test
    void testAppendOnlyVanillaUpperCaseWithFilteredDataset() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        FilteredDataset stagingTable = TestUtils.getFilteredStagingTableWithComplexFilter();

        // Create staging table
        h2Sink.executeStatement("CREATE TABLE IF NOT EXISTS \"TEST\".\"STAGING\"(\"NAME\" VARCHAR(64) NOT NULL,\"INCOME\" BIGINT,\"EXPIRY_DATE\" DATE)");

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
        String dataPass1 = basePath + "input/with_staging_filter/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/with_staging_filter/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithNoPkInUpperCase(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(2, 0, 2, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform incremental (append) milestoning With Clean Staging Table ------------------------
        String dataPass2 = basePath + "input/with_staging_filter/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/with_staging_filter/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithNoPkInUpperCase(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(2, 0, 2, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);
    }

    /*
    Scenario: Test Append Only with auditing, no versioning, filter duplicates and filter existing records (1)
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
        String dataPass1 = basePath + "input/auditing_no_version_filter_dup_filter_existing/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/auditing_no_version_filter_dup_filter_existing/expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/auditing_no_version_filter_dup_filter_existing/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/auditing_no_version_filter_dup_filter_existing/expected_pass2.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 2, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_02);
    }

    /*
    Scenario: Test Append Only with auditing, max version, filter duplicates and filter existing records with upper case (3)
    */
    @Test
    void testAppendOnlyWithAuditingMaxVersionFilterDuplicatesFilterExistingRecordsUpperCase() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithNonPkVersion();

        // Create staging table
        h2Sink.executeStatement("CREATE TABLE IF NOT EXISTS \"TEST\".\"STAGING\"(\"ID\" INTEGER NOT NULL,\"NAME\" VARCHAR(64) NOT NULL,\"INCOME\" BIGINT,\"START_TIME\" TIMESTAMP NOT NULL,\"EXPIRY_DATE\" DATE,\"DIGEST\" VARCHAR,\"VERSION\" INT)");

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
        String dataPass1 = basePath + "input/auditing_max_version_filter_dup_filter_existing/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/auditing_max_version_filter_dup_filter_existing/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersionInUpperCase(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(4, 0, 3, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/auditing_max_version_filter_dup_filter_existing/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/auditing_max_version_filter_dup_filter_existing/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersionInUpperCase(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 2, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_02);
    }

    /*
    Scenario: Test Append Only with auditing, max version, filter duplicates and no filter existing records (4)
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
        String dataPass1 = basePath + "input/auditing_max_version_filter_dup_no_filter_existing/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/auditing_max_version_filter_dup_no_filter_existing/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(4, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/auditing_max_version_filter_dup_no_filter_existing/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/auditing_max_version_filter_dup_no_filter_existing/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(4, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_02);
    }

    /*
    Scenario: Test Append Only with auditing, all version, filter duplicates and filter existing records (5)
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
        String dataPass1 = basePath + "input/auditing_all_version_filter_dup_filter_existing/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/auditing_all_version_filter_dup_filter_existing/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass1);
        // 2. Execute plans and verify results
        List<Map<String, Object>> expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats1 = createExpectedStatsMap(3, 0, 3, 0, 0);
        Map<String, Object> expectedStats2 = createExpectedStatsMap(1, 0, 1, 0, 0);
        expectedStatsList.add(expectedStats1);
        expectedStatsList.add(expectedStats2);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStatsList, incrementalClock);

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/auditing_all_version_filter_dup_filter_existing/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/auditing_all_version_filter_dup_filter_existing/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStatsList = new ArrayList<>();
        expectedStats1 = createExpectedStatsMap(4, 0, 2, 0, 0);
        expectedStatsList.add(expectedStats1);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass2, expectedStatsList, incrementalClock);
    }

    /*
    Scenario: Test Append Only with auditing, all version, filter duplicates and no filter existing records (6)
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
        String dataPass1 = basePath + "input/auditing_all_version_filter_dup_no_filter_existing/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/auditing_all_version_filter_dup_no_filter_existing/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass1);
        // 2. Execute plans and verify results
        List<Map<String, Object>> expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats1 = createExpectedStatsMap(3, 0, 3, 0, 0);
        Map<String, Object> expectedStats2 = createExpectedStatsMap(1, 0, 1, 0, 0);
        expectedStatsList.add(expectedStats1);
        expectedStatsList.add(expectedStats2);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStatsList, incrementalClock);

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String dataPass2 = basePath + "input/auditing_all_version_filter_dup_no_filter_existing/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/auditing_all_version_filter_dup_no_filter_existing/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersion(dataPass2);
        // 2. Execute plans and verify results
        expectedStatsList = new ArrayList<>();
        expectedStats1 = createExpectedStatsMap(4, 0, 3, 0, 0);
        expectedStatsList.add(expectedStats1);
        expectedStatsList.add(expectedStats2);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass2, expectedStatsList, incrementalClock);
    }

    /*
    Scenario: Scenario: Test Append Only vanilla case + staging table is cleaned up in the end with upper case with UDF based digest generation
    */
    @Test
    void testAppendOnlyWithUDFDigestGeneration() throws Exception
    {
        // Register UDF
        H2DigestUtil.registerMD5Udf(h2Sink, digestUDF);

        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithNoPks();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // Create staging table
        h2Sink.executeStatement("CREATE TABLE IF NOT EXISTS \"TEST\".\"STAGING\"(\"NAME\" VARCHAR(64) NOT NULL,\"INCOME\" BIGINT,\"EXPIRY_DATE\" DATE)");

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .deduplicationStrategy(AllowDuplicates.builder().build())
            .versioningStrategy(NoVersioningStrategy.builder().build())
            .auditing(NoAuditing.builder().build())
            .filterExistingRecords(false)
            .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestUdfName(digestUDF).digestField(digestName).build())
            .build();

        // Verify SQLs using generator
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(H2Sink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .caseConversion(CaseConversion.TO_UPPER)
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"TEST\".\"MAIN\"" +
            "(\"NAME\" VARCHAR(64) NOT NULL,\"INCOME\" BIGINT,\"EXPIRY_DATE\" DATE,\"DIGEST\" VARCHAR,\"BATCH_ID\" INTEGER)";

        String expectedIngestSql = "INSERT INTO \"TEST\".\"MAIN\" " +
            "(\"NAME\", \"INCOME\", \"EXPIRY_DATE\", \"DIGEST\", \"BATCH_ID\") " +
            "(SELECT staging.\"NAME\" as \"NAME\",staging.\"INCOME\" as \"INCOME\",staging.\"EXPIRY_DATE\" as \"EXPIRY_DATE\"," +
            "LAKEHOUSE_MD5(ARRAY['EXPIRY_DATE',CONVERT(staging.\"EXPIRY_DATE\",VARCHAR),'INCOME',CONVERT(staging.\"INCOME\",VARCHAR),'NAME',CONVERT(staging.\"NAME\",VARCHAR)])," +
            "(SELECT COALESCE(MAX(BATCH_METADATA.\"TABLE_BATCH_ID\"),0)+1 FROM BATCH_METADATA as BATCH_METADATA WHERE UPPER(BATCH_METADATA.\"TABLE_NAME\") = 'MAIN') " +
            "FROM \"TEST\".\"STAGING\" as staging)";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));


        // Verify execution using ingestor
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        String[] schema = new String[]{nameName.toUpperCase(), incomeName.toUpperCase(), expiryDateName.toUpperCase(), digestName.toUpperCase(), batchIdName.toUpperCase()};

        // ------------ Perform incremental (append) milestoning With Clean Staging Table ------------------------
        String dataPass1 = basePath + "input/digest_generation/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/digest_generation/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithNoPkInUpperCase(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is truncated
        List<Map<String, Object>> stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"STAGING\"");
        Assertions.assertEquals(stagingTableList.size(), 0);

        // ------------ Perform incremental (append) milestoning With Clean Staging Table ------------------------
        String dataPass2 = basePath + "input/digest_generation/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/digest_generation/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithNoPkInUpperCase(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);
        // 3. Assert that the staging table is truncated
        stagingTableList = h2Sink.executeQuery("select * from \"TEST\".\"STAGING\"");
        Assertions.assertEquals(stagingTableList.size(), 0);
    }

    /*
    Scenario: Test Append Only with auditing, all version, filter duplicates and no filter existing records with UDF based digest generation
    */
    @Test
    void testAppendOnlyWithUDFDigestGenerationWithFieldsToExclude() throws Exception
    {
        // Register UDF
        H2DigestUtil.registerMD5Udf(h2Sink, digestUDF);

        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithNonPkVersionWithoutDigest();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IncrementalClock incrementalClock = new IncrementalClock(fixedExecutionZonedDateTime1.toInstant(), ZoneOffset.UTC, 1000);

        // Create staging table
        createStagingTableWithoutPks(stagingTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .versioningStrategy(AllVersionsStrategy.builder()
                .versioningField(versionName)
                .dataSplitFieldName(dataSplitName)
                .mergeDataVersionResolver(DigestBasedResolver.INSTANCE)
                .performStageVersioning(true)
                .build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .filterExistingRecords(false)
            .digestGenStrategy(UDFBasedDigestGenStrategy.builder().digestUdfName(digestUDF).digestField(digestName).addAllFieldsToExcludeFromDigest(Arrays.asList(versionName)).build())
            .build();

        // Verify SQLs using generator
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(H2Sink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .ingestRunId("075605e3-bada-47d7-9ae9-7138f392fe22")
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"TEST\".\"main\"" +
            "(\"id\" INTEGER NOT NULL,\"name\" VARCHAR(64) NOT NULL,\"income\" BIGINT,\"start_time\" TIMESTAMP NOT NULL,\"expiry_date\" DATE,\"version\" INTEGER,\"batch_update_time\" TIMESTAMP NOT NULL,\"digest\" VARCHAR,\"batch_id\" INTEGER,PRIMARY KEY (\"id\", \"start_time\", \"batch_update_time\"))";

        String expectedIngestSql = "INSERT INTO \"TEST\".\"main\" " +
            "(\"id\", \"name\", \"income\", \"start_time\", \"expiry_date\", \"version\", \"digest\", \"batch_update_time\", \"batch_id\") " +
            "(SELECT staging.\"id\" as \"id\",staging.\"name\" as \"name\",staging.\"income\" as \"income\",staging.\"start_time\" as \"start_time\",staging.\"expiry_date\" as \"expiry_date\",staging.\"version\" as \"version\"," +
            "LAKEHOUSE_MD5(ARRAY['expiry_date',CONVERT(staging.\"expiry_date\",VARCHAR),'id',CONVERT(staging.\"id\",VARCHAR),'income',CONVERT(staging.\"income\",VARCHAR),'name',CONVERT(staging.\"name\",VARCHAR),'start_time',CONVERT(staging.\"start_time\",VARCHAR)])," +
            "'2000-01-01 00:00:00.000000'," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
            "FROM \"TEST\".\"staging_temp_staging_lp_yosulf\" as staging WHERE (staging.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (staging.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));


        // Verify execution using ingestor
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, versionName, digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String dataPass1 = basePath + "input/digest_generation_with_fields_to_exclude/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/digest_generation_with_fields_to_exclude/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersionWithoutDigest(dataPass1);
        // 2. Execute plans and verify results
        List<Map<String, Object>> expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats1 = createExpectedStatsMap(3, 0, 3, 0, 0);
        Map<String, Object> expectedStats2 = createExpectedStatsMap(1, 0, 1, 0, 0);
        expectedStatsList.add(expectedStats1);
        expectedStatsList.add(expectedStats2);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStatsList, incrementalClock);

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/digest_generation_with_fields_to_exclude/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/digest_generation_with_fields_to_exclude/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersionWithoutDigest(dataPass2);
        // 2. Execute plans and verify results
        expectedStatsList = new ArrayList<>();
        expectedStats1 = createExpectedStatsMap(4, 0, 3, 0, 0);
        expectedStatsList.add(expectedStats1);
        expectedStatsList.add(expectedStats2);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass2, expectedStatsList, incrementalClock);
    }

    /*
   Scenario: Test Append Only with auditing, all version, filter duplicates and no filter existing records with UDF based digest generation with type conversion UDFs
   */
    @Test
    void testAppendOnlyWithUDFDigestGenerationWithFieldsToExcludeAndTypeConversionUdfs() throws Exception
    {
        // Register UDF
        H2DigestUtil.registerMD5Udf(h2Sink, digestUDF);

        // Register a dummy String to String type conversion UDF
        h2Sink.executeStatement("CREATE ALIAS StringToString AS '" +
            "String stringToString(String value)\n" +
            "    {\n" +
            "        return value;\n" +
            "    }'");

        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        DatasetDefinition stagingTable = TestUtils.getStagingTableWithNonPkVersionWithoutDigest();
        Datasets datasets = Datasets.of(mainTable, stagingTable);
        IncrementalClock incrementalClock = new IncrementalClock(fixedExecutionZonedDateTime1.toInstant(), ZoneOffset.UTC, 1000);

        // Create staging table
        createStagingTableWithoutPks(stagingTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .versioningStrategy(AllVersionsStrategy.builder()
                .versioningField(versionName)
                .dataSplitFieldName(dataSplitName)
                .mergeDataVersionResolver(DigestBasedResolver.INSTANCE)
                .performStageVersioning(true)
                .build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .filterExistingRecords(false)
            .digestGenStrategy(UDFBasedDigestGenStrategy.builder()
                .digestUdfName(digestUDF)
                .putAllTypeConversionUdfNames(Collections.singletonMap(DataType.VARCHAR, "StringToString"))
                .digestField(digestName)
                .addAllFieldsToExcludeFromDigest(Collections.singletonList(versionName))
                .build())
            .build();

        // Verify SQLs using generator
        RelationalGenerator generator = RelationalGenerator.builder()
            .ingestMode(ingestMode)
            .relationalSink(H2Sink.get())
            .collectStatistics(true)
            .executionTimestampClock(fixedClock_2000_01_01)
            .ingestRunId("075605e3-bada-47d7-9ae9-7138f392fe22")
            .build();

        GeneratorResult operations = generator.generateOperations(datasets);

        List<String> preActionsSql = operations.preActionsSql();
        List<String> ingestSql = operations.ingestSql();

        String expectedCreateTableSql = "CREATE TABLE IF NOT EXISTS \"TEST\".\"main\"" +
            "(\"id\" INTEGER NOT NULL,\"name\" VARCHAR(64) NOT NULL,\"income\" BIGINT,\"start_time\" TIMESTAMP NOT NULL,\"expiry_date\" DATE,\"version\" INTEGER,\"batch_update_time\" TIMESTAMP NOT NULL,\"digest\" VARCHAR,\"batch_id\" INTEGER,PRIMARY KEY (\"id\", \"start_time\", \"batch_update_time\"))";

        String expectedIngestSql = "INSERT INTO \"TEST\".\"main\" " +
            "(\"id\", \"name\", \"income\", \"start_time\", \"expiry_date\", \"version\", \"digest\", \"batch_update_time\", \"batch_id\") " +
            "(SELECT staging.\"id\" as \"id\",staging.\"name\" as \"name\",staging.\"income\" as \"income\",staging.\"start_time\" as \"start_time\",staging.\"expiry_date\" as \"expiry_date\",staging.\"version\" as \"version\"," +
            "LAKEHOUSE_MD5(ARRAY['expiry_date',CONVERT(staging.\"expiry_date\",VARCHAR),'id',CONVERT(staging.\"id\",VARCHAR),'income',CONVERT(staging.\"income\",VARCHAR),'name',StringToString(staging.\"name\"),'start_time',CONVERT(staging.\"start_time\",VARCHAR)])," +
            "'2000-01-01 00:00:00.000000'," +
            "(SELECT COALESCE(MAX(batch_metadata.\"table_batch_id\"),0)+1 FROM batch_metadata as batch_metadata WHERE UPPER(batch_metadata.\"table_name\") = 'MAIN') " +
            "FROM \"TEST\".\"staging_temp_staging_lp_yosulf\" as staging WHERE (staging.\"data_split\" >= '{DATA_SPLIT_LOWER_BOUND_PLACEHOLDER}') AND (staging.\"data_split\" <= '{DATA_SPLIT_UPPER_BOUND_PLACEHOLDER}'))";

        Assertions.assertEquals(expectedCreateTableSql, preActionsSql.get(0));
        Assertions.assertEquals(expectedIngestSql, ingestSql.get(0));


        // Verify execution using ingestor
        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, versionName, digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String dataPass1 = basePath + "input/digest_generation_with_fields_to_exclude/data_pass1.csv";
        String expectedDataPass1 = basePath + "expected/digest_generation_with_fields_to_exclude/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataWithVersionWithoutDigest(dataPass1);
        // 2. Execute plans and verify results
        List<Map<String, Object>> expectedStatsList = new ArrayList<>();
        Map<String, Object> expectedStats1 = createExpectedStatsMap(3, 0, 3, 0, 0);
        Map<String, Object> expectedStats2 = createExpectedStatsMap(1, 0, 1, 0, 0);
        expectedStatsList.add(expectedStats1);
        expectedStatsList.add(expectedStats2);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass1, expectedStatsList, incrementalClock);

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/digest_generation_with_fields_to_exclude/data_pass2.csv";
        String expectedDataPass2 = basePath + "expected/digest_generation_with_fields_to_exclude/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithVersionWithoutDigest(dataPass2);
        // 2. Execute plans and verify results
        expectedStatsList = new ArrayList<>();
        expectedStats1 = createExpectedStatsMap(4, 0, 3, 0, 0);
        expectedStatsList.add(expectedStats1);
        expectedStatsList.add(expectedStats2);
        executePlansAndVerifyResultsWithDerivedDataSplits(ingestMode, options, datasets, schema, expectedDataPass2, expectedStatsList, incrementalClock);
    }

    /*
    Scenario: test Append Only with auditing, no version, allow duplicates and filter existing records when staging data is imported along with digest field population
    */
    @Test
    void testAppendOnlyWithStagingDataImportedWithPopulateDigest() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        String dataPass1 = basePath + "input/import_with_populate_digest/data_pass1.json";
        Dataset stagingTable = TestUtils.getJsonDatasetWithoutDigestReferenceTable(dataPass1);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(AllowDuplicates.builder().build())
            .versioningStrategy(NoVersioningStrategy.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .filterExistingRecords(true)
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String expectedDataPass1 = basePath + "expected/import_with_populate_digest/expected_pass1.csv";
        // Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(5, 0, 5, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/import_with_populate_digest/data_pass2.json";
        stagingTable = TestUtils.getJsonDatasetWithoutDigestReferenceTable(dataPass2);
        String expectedDataPass2 = basePath + "expected/import_with_populate_digest/expected_pass2.csv";
        // Execute plans and verify results
        expectedStats = createExpectedStatsMap(2, 0, 1, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass2, expectedStats, fixedClock_2000_01_02);
    }

    /*
    Scenario: Test Append Only with auditing, no version, allow duplicates and no filter existing records when staging has lesser columns than main
    */
    @Test
    void testAppendOnlyWithLessColumnsInStaging() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getDefaultMainTable();
        String dataPass1 = basePath + "input/less_columns_in_staging/data_pass1.csv";
        Dataset stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass1);

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
        String expectedDataPass1 = basePath + "expected/less_columns_in_staging/expected_pass1.csv";
        // Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/less_columns_in_staging/data_pass2.csv";
        stagingTable = TestUtils.getCsvDatasetRefWithLessColumnsThanMain(dataPass2);
        String expectedDataPass2 = basePath + "expected/less_columns_in_staging/expected_pass2.csv";
        // Execute plans and verify results
        expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass2, expectedStats, fixedClock_2000_01_02);
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
                .relationalSink(H2Sink.get())
                .createDatasets(false)
                .build();
        try
        {
            ingestor.performFullIngestion(JdbcConnection.of(h2Sink.connection()), datasets);
            Assertions.fail("Should not be successful");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e.getMessage().contains("Table \"batch_metadata\" not found"));
        }
    }
}
