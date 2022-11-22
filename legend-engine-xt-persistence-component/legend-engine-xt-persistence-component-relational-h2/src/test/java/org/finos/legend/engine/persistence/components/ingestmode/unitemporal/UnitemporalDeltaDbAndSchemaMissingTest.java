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

package org.finos.legend.engine.persistence.components.ingestmode.unitemporal;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.UnitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchIdAndDateTime;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.finos.legend.engine.persistence.components.TestUtils.batchIdInName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchIdOutName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchTimeInName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchTimeOutName;
import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.TestUtils.startTimeName;

class UnitemporalDeltaDbAndSchemaMissingTest extends BaseTest
{
    private final String basePathForInput = "src/test/resources/data/unitemporal-incremental-milestoning/input/batch_id_and_time_based/";
    private final String basePathForExpected = "src/test/resources/data/unitemporal-incremental-milestoning/expected/batch_id_and_time_based/";

    /*
    Scenario: Test milestoning Logic when db and schema are missing in dataset definition
    */
    @Test
    void testMilestoningDbAndSchemaMissing() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalMainTable();
        mainTable = mainTable.withDatabase(Optional.empty());
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();
        stagingTable = stagingTable.withDatabase(Optional.empty());
        testMilestoning(mainTable, stagingTable);
    }

    /*
    Scenario: Test milestoning Logic when db and schema are both present in the dataset definition
    */
    @Test
    void testMilestoningDbAndSchemaBothPresent() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getUnitemporalMainTable();
        mainTable = mainTable.withDatabase(TEST_DATABASE)
            .withGroup(TEST_SCHEMA);

        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();
        stagingTable.withDatabase(TEST_DATABASE)
            .withGroup(TEST_SCHEMA);

        testMilestoning(mainTable, stagingTable);
    }

    void testMilestoning(DatasetDefinition mainTable, DatasetDefinition stagingTable) throws Exception
    {
        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchIdInName, batchIdOutName, batchTimeInName, batchTimeOutName};

        // Create staging table
        createStagingTable(stagingTable);

        UnitemporalDelta ingestMode = UnitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchIdAndDateTime.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .dateTimeInName(batchTimeInName)
                .dateTimeOutName(batchTimeOutName)
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().collectStatistics(true).build();
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String mainTableFullyQualifiedName = getFullyQualifiedTableName(mainTable);
        String stagingTableFullyQualifiedName = getFullyQualifiedTableName(stagingTable);

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "without_delete_ind/staging_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "without_delete_ind/expected_pass1.csv";
        // 1. Load staging table
        loadStagingData(dataPass1, stagingTableFullyQualifiedName);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "without_delete_ind/staging_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "without_delete_ind/expected_pass2.csv";
        // 1. Load staging table
        loadStagingData(dataPass2, stagingTableFullyQualifiedName);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(3, 0, 1, 1, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, fixedClock_2000_01_01);

        // ------------ Perform Pass3 empty batch (No Impact) -------------------------
        String dataPass3 = basePathForInput + "without_delete_ind/staging_data_pass3.csv";
        String expectedDataPass3 = basePathForExpected + "without_delete_ind/expected_pass3.csv";
        // 1. Load staging table
        loadStagingData(dataPass3, stagingTableFullyQualifiedName);
        // 2. Execute plans and verify results
        expectedStats = createExpectedStatsMap(0, 0, 0, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass3, expectedStats, fixedClock_2000_01_01);
    }

    private void loadStagingData(String path, String tableName) throws Exception
    {
        validateFileExists(path);
        String loadSql = "TRUNCATE TABLE %s;" +
            "INSERT INTO %s(id, name, income, start_time ,expiry_date, digest) " +
            "SELECT CONVERT( \"id\",INT ), \"name\", CONVERT( \"income\", INT), CONVERT( \"start_time\", DATETIME), CONVERT( \"expiry_date\", DATE) ,  \"digest\"" +
            " FROM CSVREAD( '%s', 'id, name, income, start_time, expiry_date, digest', NULL )";

        loadSql = String.format(loadSql, tableName, tableName, path);
        h2Sink.executeStatement(loadSql);
    }

    private static String getFullyQualifiedTableName(DatasetDefinition dataset)
    {
        Optional<String> db = dataset.database();
        Optional<String> schema = dataset.group();
        String table = dataset.name();
        StringBuilder builder = new StringBuilder();
        if (db.isPresent() || schema.isPresent())
        {
            db.ifPresent(s -> builder.append(String.format("%s.", SqlGenUtils.getQuotedField(s))));
            schema.ifPresent(s -> builder.append(String.format("%s.", SqlGenUtils.getQuotedField(s))));
            builder.append(SqlGenUtils.getQuotedField(table));
        }
        else
        {
            builder.append(table);
        }
        return builder.toString();
    }
}
