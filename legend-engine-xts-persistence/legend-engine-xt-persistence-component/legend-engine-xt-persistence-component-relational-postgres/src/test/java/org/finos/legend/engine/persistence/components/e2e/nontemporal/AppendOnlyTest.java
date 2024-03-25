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

import org.finos.legend.engine.persistence.components.e2e.BaseTest;
import org.finos.legend.engine.persistence.components.e2e.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.AllowDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UserProvidedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.versioning.NoVersioningStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FilteredDataset;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.e2e.TestUtils.batchIdName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.e2e.TestUtils.nameName;

class AppendOnlyTest extends BaseTest
{
    private final String basePathForInput = "data/incremental-append-milestoning/";
    private final String basePathForExpected = "src/test/resources/data/incremental-append-milestoning/";

    /*
    Scenario: Test Append Only vanilla case + staging table is cleaned up in the end with upper case (2)
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
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
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
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);
        // 3. Assert that the staging table is truncated
        stagingTableList = postgresSink.executeQuery("select * from \"TEST\".\"STAGING\"");
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
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform incremental (append) milestoning With Clean Staging Table ------------------------
        String dataPass2 = basePathForInput + "input/with_staging_filter/data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "expected/with_staging_filter/expected_pass2.csv";
        // 1. Load staging table
        loadStagingDataWithNoPkInUpperCase(dataPass2);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(2, 0, 2, 0, 0);
        executePlansAndVerifyForCaseConversion(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);
    }
}
