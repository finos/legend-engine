// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.e2e;

import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromAndThruDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Disabled
public class BitempDeltaTest extends BigQueryEndToEndTest
{
    private final String basePathForInput = "src/test/resources/data/bitemporal-incremental-milestoning/input/batch_id_based/";
    private final String basePathForExpected = "src/test/resources/data/bitemporal-incremental-milestoning/expected/batch_id_based/";

    /*
    Scenario: Test milestoning Logic when staging table pre populated
    */
    @Test
    void testMilestoningSourceSpecifiesFromAndThrough() throws Exception
    {
        DatasetDefinition mainTable = getDefaultMainTable();
        DatasetDefinition stagingTable = getBitemporalStagingTable();

        String[] schema = new String[] {key1Name, key2Name, valueName, fromName, throughName, digestName, batchIdInName, batchIdOutName};

        // Create staging table
        createTable(stagingTable);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .build())
                .validityMilestoning(ValidDateTime.builder()
                        .dateTimeFromName(fromName)
                        .dateTimeThruName(throughName)
                        .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                                .sourceDateTimeFromField(dateInName)
                                .sourceDateTimeThruField(dateOutName)
                                .build())
                        .build())
                .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from_and_through/without_delete_ind/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from_and_through/without_delete_ind/expected_pass1.csv";
        // 1. Load Staging table
        loadData(dataPass1, stagingTable, 1);
        // 2. Execute Plan and Verify Results
        Map<String, Object> expectedStats = createExpectedStatsMap(5, 0, 5, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);
        // 3. Assert that the staging table is NOT truncated
        List<Map<String, Object>> stagingTableList = runQuery("select * from \"TEST\".\"staging\"");
        Assertions.assertEquals(stagingTableList.size(), 5);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from_and_through/without_delete_ind/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "source_specifies_from_and_through/without_delete_ind/expected_pass2.csv";
        // 1. Load Staging table
        loadData(dataPass2, stagingTable, 1);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(4, 0, 0, 2, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = basePathForInput + "source_specifies_from_and_through/without_delete_ind/staging_data_pass3.csv";
        String expectedDataPass3 = basePathForExpected + "source_specifies_from_and_through/without_delete_ind/expected_pass3.csv";
        // 1. Load staging table
        loadData(dataPass3, stagingTable, 1);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);
    }

    /*
    Scenario: Test milestoning Logic when staging table pre populated and delete indicator is present
    */
    @Test
    void testMilestoningSourceSpecifiesFromAndThroughWithDeleteIndicator() throws Exception
    {

        DatasetDefinition mainTable = this.getBitemporalMainTable();
        DatasetDefinition stagingTable = getBitemporalStagingTableWithDeleteIndicator();

        String[] schema = new String[] {key1Name, key2Name, valueName, fromName, throughName, digestName, batchIdInName, batchIdOutName};

        // Create staging table
        createTable(stagingTable);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
                .digestField(digestName)
                .transactionMilestoning(BatchId.builder()
                        .batchIdInName(batchIdInName)
                        .batchIdOutName(batchIdOutName)
                        .build())
                .validityMilestoning(ValidDateTime.builder()
                        .dateTimeFromName(fromName)
                        .dateTimeThruName(throughName)
                        .validityDerivation(SourceSpecifiesFromAndThruDateTime.builder()
                                .sourceDateTimeFromField(dateInName)
                                .sourceDateTimeThruField(dateOutName)
                                .build())
                        .build())
                .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                        .deleteField(deleteIndicatorName)
                        .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                        .build())
                .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "source_specifies_from_and_through/with_delete_ind/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "source_specifies_from_and_through/with_delete_ind/expected_pass1.csv";
        // 1. Load Staging table
        loadData(dataPass1, stagingTable, 1);
        // 2. Execute Plan and Verify Results
        Map<String, Object> expectedStats = createExpectedStatsMap(5, 0, 5, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "source_specifies_from_and_through/with_delete_ind/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "source_specifies_from_and_through/with_delete_ind/expected_pass2.csv";
        // 1. Load Staging table
        loadData(dataPass2, stagingTable, 1);
        // 2. Execute Plan and Verify Results
        expectedStats = createExpectedStatsMap(4, 0, 0, 2, 1);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = basePathForInput + "source_specifies_from_and_through/with_delete_ind/staging_data_pass3.csv";
        String expectedDataPass3 = basePathForExpected + "source_specifies_from_and_through/with_delete_ind/expected_pass3.csv";
        // 1. Load staging table
        loadData(dataPass3, stagingTable, 1);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats);
    }
}
