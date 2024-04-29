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

package org.finos.legend.engine.persistence.components.logicalplan.operations;

import org.finos.legend.engine.persistence.components.BaseTest;
import org.finos.legend.engine.persistence.components.TestUtils;
import org.finos.legend.engine.persistence.components.common.Datasets;
import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UserProvidedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.planner.PlannerOptions;
import org.finos.legend.engine.persistence.components.relational.api.IngestorResult;
import org.finos.legend.engine.persistence.components.schemaevolution.IncompatibleSchemaChangeException;
import org.finos.legend.engine.persistence.components.util.SchemaEvolutionCapability;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.finos.legend.engine.persistence.components.TestUtils.*;

class SchemaEvolutionTest extends BaseTest
{
    private String basePathForInput = "src/test/resources/data/schema-evolution/input/";
    private String basePathForExpected = "src/test/resources/data/schema-evolution/expected/";

    @Test
    void testAddColumn() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getSchemaEvolutionAddColumnMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        //Create main table with Old schema
        createTempTable(mainTable);

        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).enableSchemaEvolution(true).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform Pass1 (Schema Evolution) ------------------------
        String dataPass1 = basePathForInput + "add_column_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "add_column_expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_01);
        // 3. Verify schema changes in database
        List<Map<String, Object>> actualTableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
        assertTableColumnsEquals(Arrays.asList(schema), actualTableData);
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.expectedMainTableSchema(), result.updatedDatasets().mainDataset());

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "add_column_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "add_column_expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadBasicStagingData(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1, 0, 1, 0, 0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_02);
    }

    @Test
    void testDataTypeConversion() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getSchemaEvolutionDataTypeConversionMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        //Create main table with Old schema
        createTempTable(mainTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).enableSchemaEvolution(true).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_CONVERSION);
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform Pass1 (Schema Evolution) ------------------------
        String dataPass1 = basePathForInput + "data_type_conversion_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "data_type_conversion_expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3, 0, 3, 0, 0);
        IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_01);
        // 3. Verify schema changes in database
        List<Map<String, Object>> actualTableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
        assertTableColumnsEquals(Arrays.asList(schema), actualTableData);
        Assertions.assertEquals("BIGINT", getColumnDataTypeFromTable(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, incomeName));
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.expectedMainTableSchema(), result.updatedDatasets().mainDataset());

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "data_type_conversion_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "data_type_conversion_expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadBasicStagingData(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1,0,1,0,0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_03);
    }

    @Test
    void testDataTypeSizeChange() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getMainTableWithBatchUpdateTimeField();
        DatasetDefinition stagingTable = TestUtils.getSchemaEvolutionDataTypeSizeChangeStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        //Create main table with Old schema
        createTempTable(mainTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).enableSchemaEvolution(true).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_SIZE_CHANGE);
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform Pass1 (Schema Evolution) ------------------------
        String dataPass1 = basePathForInput + "datatype_type_size_change_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "datatype_type_size_change_expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForIntIncome(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3,0,3,0,0);
        IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_01);
        // 3. Verify schema changes in database
        List<Map<String, Object>> actualTableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
        assertTableColumnsEquals(Arrays.asList(schema), actualTableData);
        Assertions.assertEquals("BIGINT", getColumnDataTypeFromTable(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, incomeName));
        Assertions.assertEquals("VARCHAR", getColumnDataTypeFromTable(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, nameName));
        Assertions.assertEquals(256, getColumnDataTypeLengthFromTable(h2Sink, mainTableName, nameName));
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.expectedMainTableSchemaWithLengthEvolution(), result.updatedDatasets().mainDataset());

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "datatype_type_size_change_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "datatype_type_size_change_expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadStagingDataForIntIncome(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1,0,1,0,0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_03);
    }

    @Test
    void testColumnNullabilityChange() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getMainTableWithBatchUpdateTimeField();
        DatasetDefinition stagingTable = TestUtils.getSchemaEvolutionColumnNullabilityChangeStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        //Create main table with Old schema
        createTempTable(mainTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).enableSchemaEvolution(true).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.COLUMN_NULLABILITY_CHANGE);
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform Pass1 (Schema Evolution) ------------------------
        String dataPass1 = basePathForInput + "column_nullability_change_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "column_nullability_change_expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3,0,3,0,0);
        IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_01);
        // 3. Verify schema changes in database
        List<Map<String, Object>> actualTableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
        assertTableColumnsEquals(Arrays.asList(schema), actualTableData);
        Assertions.assertEquals("YES", getIsColumnNullableFromTable(h2Sink, mainTableName, nameName));
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(createDatasetWithUpdatedField(TestUtils.expectedMainTableSchema(), name.withNullable(true)), result.updatedDatasets().mainDataset());

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "column_nullability_change_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "column_nullability_change_expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadBasicStagingData(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1,0,1,0,0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_03);
    }

    @Test
    void testDataTypeConversionAndColumnNullabilityChange() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getSchemaEvolutionDataTypeConversionAndColumnNullabilityChangeMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        //Create main table with Old schema
        createTempTable(mainTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).enableSchemaEvolution(true).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_CONVERSION);
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.COLUMN_NULLABILITY_CHANGE);
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform Pass1 (Schema Evolution) ------------------------
        String dataPass1 = basePathForInput + "data_type_conversion_and_column_nullability_change_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "data_type_conversion_and_column_nullability_change_expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3,0,3,0,0);
        IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_01);
        // 3. Verify schema changes in database
        List<Map<String, Object>> actualTableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
        assertTableColumnsEquals(Arrays.asList(schema), actualTableData);
        Assertions.assertEquals("BIGINT", getColumnDataTypeFromTable(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, incomeName));
        Assertions.assertEquals("YES", getIsColumnNullableFromTable(h2Sink, mainTableName, incomeName));
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.expectedMainTableSchema(), result.updatedDatasets().mainDataset());

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "data_type_conversion_and_column_nullability_change_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "data_type_conversion_and_column_nullability_change_expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadBasicStagingData(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1,0,1,0,0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_03);
    }

    @Test
    void testDataTypeConversionAndDataTypeSizeChange() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getMainTableWithBatchUpdateTimeField();
        DatasetDefinition stagingTable = TestUtils.getSchemaEvolutionDataTypeConversionAndDataTypeSizeChangeStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        //Create main table with Old schema
        createTempTable(mainTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).enableSchemaEvolution(true).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_CONVERSION);
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_SIZE_CHANGE);
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform Pass1 (Schema Evolution) ------------------------
        String dataPass1 = basePathForInput + "data_type_conversion_and_data_type_size_change_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "data_type_conversion_and_data_type_size_change_expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForDecimalIncome(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3,0,3,0,0);
        IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_01);
        // 3. Verify schema changes in database
        List<Map<String, Object>> actualTableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
        assertTableColumnsEquals(Arrays.asList(schema), actualTableData);
        Assertions.assertEquals("DECIMAL", getColumnDataTypeFromTable(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, incomeName));
        Assertions.assertEquals(10, getColumnDataTypeLengthFromTable(h2Sink, mainTableName, incomeName));
        Assertions.assertEquals(2, getColumnDataTypeScaleFromTable(h2Sink, mainTableName, incomeName));
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.expectedMainTableSchemaWithDatatypeChange(), result.updatedDatasets().mainDataset());

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "data_type_conversion_and_data_type_size_change_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "data_type_conversion_and_data_type_size_change_expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadStagingDataForDecimalIncome(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1,0,1,0,0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_03);
    }

    @Test
    void testMakeMainColumnNullable() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getMainTableWithBatchUpdateTimeField();
        DatasetDefinition stagingTable = TestUtils.getSchemaEvolutionMakeMainColumnNullableStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        //Create main table with Old schema
        createTempTable(mainTable);

        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).enableSchemaEvolution(true).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.COLUMN_NULLABILITY_CHANGE);
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform Pass1 (Schema Evolution) ------------------------
        String dataPass1 = basePathForInput + "make_main_column_nullable_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "make_main_column_nullable_expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForWithoutName(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3,0,3,0,0);
        IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_01);
        // 3. Verify schema changes in database
        List<Map<String, Object>> actualTableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
        assertTableColumnsEquals(Arrays.asList(schema), actualTableData);
        Assertions.assertEquals("YES", getIsColumnNullableFromTable(h2Sink, mainTableName, nameName));
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(createDatasetWithUpdatedField(TestUtils.expectedMainTableSchema(), name.withNullable(true)), result.updatedDatasets().mainDataset());

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "make_main_column_nullable_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "make_main_column_nullable_expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadStagingDataForWithoutName(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1,0,1,0,0);
        executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_03);
    }

    @Test
    void testSchemaEvolutionFailPKTypeDifferent() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getSchemaEvolutionPKTypeDifferentMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        //Create main table with Old schema
        createTempTable(mainTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).enableSchemaEvolution(true).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_CONVERSION);
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName};

        // ------------ Perform Pass1 ------------------------
        String dataPass1 = basePathForInput + "data_type_conversion_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "data_type_conversion_expected_pass1.csv";
        // 1. Load staging table
        loadBasicStagingData(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3,0,3,0,0);

        try
        {
            IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_03);
            Assertions.fail("Exception was not thrown");
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Primary keys for main table has changed which is not allowed", e.getMessage());
        }
    }
}