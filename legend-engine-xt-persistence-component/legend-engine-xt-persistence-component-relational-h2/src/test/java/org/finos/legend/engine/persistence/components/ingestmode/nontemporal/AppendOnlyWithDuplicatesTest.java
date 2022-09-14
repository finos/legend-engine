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
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.AllowDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FailOnDuplicates;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.CsvExternalDatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.TestUtils.getSchemaWithNoPKs;
import static org.finos.legend.engine.persistence.components.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.TestUtils.startTimeName;

class AppendOnlyWithDuplicatesTest extends BaseTest
{
    private final String basePath = "src/test/resources/data/incremental-append-milestoning/";

    /*
    Scenario: ALLOW_DUPLICATES validation with primary keys not empty
    */
    @Test
    void testIncrementalAppendAllowDuplicatesPKsNotEmpty() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        try
        {
            AppendOnly ingestMode = AppendOnly.builder()
                .digestField(digestName)
                .addKeyFields(idName, startTimeName)
                .deduplicationStrategy(AllowDuplicates.builder().build())
                .auditing(NoAuditing.builder().build())
                .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build AppendOnly, [keyFields] must be empty since [deduplicationStrategy] is set to allow duplicates", e.getMessage());
        }
    }

    /*
   Scenario: Test milestoning Logic when staging table pre populated
   Duplicates are allowed, no pks in dataset
   */
    @Test
    void testMilestoningWithAllowDuplicates() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicTableWithNoPks();
        String dataPass1 = basePath + "input/allow_duplicates/data_pass1.csv";
        Dataset stagingTable = CsvExternalDatasetReference.builder().schema(getSchemaWithNoPKs()).csvDataPath(dataPass1).build();

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestName)
            .deduplicationStrategy(AllowDuplicates.builder().build())
            .auditing(NoAuditing.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{nameName, incomeName, expiryDateName};

        // ------------ Perform incremental (append) milestoning Pass1 ------------------------
        String expectedDataPass1 = basePath + "expected/allow_duplicates/expected_pass1.csv";
        // Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_INSERTED.name(), 3);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform incremental (append) milestoning Pass2 ------------------------
        String dataPass2 = basePath + "input/allow_duplicates/data_pass2.csv";
        stagingTable = CsvExternalDatasetReference.builder().schema(getSchemaWithNoPKs()).csvDataPath(dataPass1).build();
        String expectedDataPass2 = basePath + "expected/allow_duplicates/expected_pass2.csv";
        // Execute plans and verify results
        executePlansAndVerifyResults(ingestMode, options, datasets.withStagingDataset(stagingTable), schema, expectedDataPass2, expectedStats);
    }

    /*
    Scenario: FAIL_ON_DUPLICATES validation with primary keys empty
    */
    @Test
    void testMilestoningWithFailOnDuplicatesValidation() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicTableWithNoPks();
        String dataPass1 = basePath + "input/allow_duplicates/data_pass1.csv";
        Dataset stagingTable = CsvExternalDatasetReference.builder().schema(getSchemaWithNoPKs()).csvDataPath(dataPass1).build();

        // Generate the milestoning object
        try
        {
            AppendOnly ingestMode = AppendOnly.builder()
                .digestField(digestName)
                .deduplicationStrategy(FailOnDuplicates.builder().build())
                .auditing(NoAuditing.builder().build())
                .build();

            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertEquals("Cannot build AppendOnly, [keyFields] must contain at least one element since [deduplicationStrategy] is set to fail on duplicates", e.getMessage());
        }
    }

    /*
    Scenario: Test milestoning Logic when staging table pre populated
    FAIL_ON_DUPLICATES strategy will cause the test to fail
    */
    @Test
    void testMilestoningWithFailOnDuplicates() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestName)
            .addKeyFields(idName, startTimeName)
            .deduplicationStrategy(FailOnDuplicates.builder().build())
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
        try
        {
            executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);
            Assertions.fail("Exception was not thrown");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e.getMessage().contains("Unique index or primary key violation"));
        }
    }
}