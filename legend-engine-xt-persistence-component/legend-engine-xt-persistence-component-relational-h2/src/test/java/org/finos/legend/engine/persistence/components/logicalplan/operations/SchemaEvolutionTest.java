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

package org.finos.legend.engine.persistence.components.logicalplan.operations;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.common.StatisticName;
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.schemaevolution.IncompatibleSchemaChangeException;
import org.finos.legend.engine.persistence.components.util.SchemaEvolutionCapability;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.finos.legend.engine.persistence.components.TestUtils.assertTableColumnsEquals;
import static org.finos.legend.engine.persistence.components.TestUtils.assertUpdatedDataset;
import static org.finos.legend.engine.persistence.components.TestUtils.createDatasetWithUpdatedField;
import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.TestUtils.getCheckDataTypeFromTableSql;
import static org.finos.legend.engine.persistence.components.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.TestUtils.mainTableName;
import static org.finos.legend.engine.persistence.components.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.TestUtils.nameWithMoreLength;
import static org.finos.legend.engine.persistence.components.TestUtils.startTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.testDatabaseName;
import static org.finos.legend.engine.persistence.components.TestUtils.testSchemaName;

class SchemaEvolutionTest extends BaseTest
{
    private String basePathForInput = "src/test/resources/data/schema-evolution/input/";
    private String basePathForExpected = "src/test/resources/data/schema-evolution/expected/";

    @Test
    void testAddColumn() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getSchemaEvolutionBeforeAddMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestName)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(NoAuditing.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).enableSchemaEvolution(true).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "add_data_pass.csv";
        String expectedDataPass1 = basePathForExpected + "add_expected_pass.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        expectedStats.put(StatisticName.ROWS_UPDATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, schemaEvolutionCapabilitySet);
        // 3. Verify schema changes in database
        List<Map<String, Object>> actualTableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
        assertTableColumnsEquals(Arrays.asList(schema), actualTableData);
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(stagingTable, result.updatedDatasets().mainDataset());
    }

    @Test
    void testImplicitDataTypeChange() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBasicMainTable();
        DatasetDefinition stagingTable = TestUtils.getSchemaEvolutionImplicitChangeStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestField(digestName)
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(NoAuditing.builder().build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).enableSchemaEvolution(true).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_SIZE_CHANGE);
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "implicit_data_type_change_data_pass.csv";
        String expectedDataPass1 = basePathForExpected + "implicit_data_type_change_expected_pass.csv";
        // 1. Load staging table
        loadStagingDataForImplicitTypeChange(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        expectedStats.put(StatisticName.ROWS_UPDATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);
        IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, schemaEvolutionCapabilitySet);
        // 3. Verify schema changes in database
        List<Map<String, Object>> actualTableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
        assertTableColumnsEquals(Arrays.asList(schema), actualTableData);
        Assertions.assertEquals("BIGINT", getCheckDataTypeFromTableSql(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, incomeName));
        Assertions.assertEquals("VARCHAR", getCheckDataTypeFromTableSql(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, nameName));
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(createDatasetWithUpdatedField(mainTable, nameWithMoreLength), result.updatedDatasets().mainDataset());
    }

    @Test
    void testSchemaEvolutionFailPKTypeDifferent() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getSchemaEvolutionPKTypeDifferentMainTable();
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
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_CONVERSION);
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName};

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "explicit_data_type_change_data_pass.csv";
        String expectedDataPass1 = basePathForExpected + "explicit_data_type_change_expected_pass.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = new HashMap<>();
        expectedStats.put(StatisticName.INCOMING_RECORD_COUNT.name(), 3);
        expectedStats.put(StatisticName.ROWS_DELETED.name(), 0);
        expectedStats.put(StatisticName.ROWS_UPDATED.name(), 0);
        expectedStats.put(StatisticName.ROWS_TERMINATED.name(), 0);

        try
        {
            IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, schemaEvolutionCapabilitySet);
            Assertions.fail("Exception was not thrown");
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Primary keys for main table has changed which is not allowed ", e.getMessage());
        }
    }
}