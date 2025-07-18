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
import org.finos.legend.engine.persistence.components.ingestmode.BitemporalDelta;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UserProvidedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.merge.DeleteIndicatorMergeStrategy;
import org.finos.legend.engine.persistence.components.ingestmode.transactionmilestoning.BatchId;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.ValidDateTime;
import org.finos.legend.engine.persistence.components.ingestmode.validitymilestoning.derivation.SourceSpecifiesFromDateTime;
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

    // Add column
    // income BIGINT
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
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(1, result.schemaEvolutionSql().get().size());
        Assertions.assertEquals("ALTER TABLE \"TEST\".\"main\" ADD COLUMN \"income\" BIGINT", result.schemaEvolutionSql().get().get(0));

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "add_column_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "add_column_expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadBasicStagingData(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1, 0, 1, 0, 0);
        result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_02);
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.expectedMainTableSchema(), result.updatedDatasets().mainDataset());
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(0, result.schemaEvolutionSql().get().size());
    }

    // Alter column
    // income: INTEGER -> BIGINT
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
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(1, result.schemaEvolutionSql().get().size());
        Assertions.assertEquals("ALTER TABLE \"TEST\".\"main\" ALTER COLUMN \"income\" BIGINT", result.schemaEvolutionSql().get().get(0));

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "data_type_conversion_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "data_type_conversion_expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadBasicStagingData(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1,0,1,0,0);
        result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_03);
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.expectedMainTableSchema(), result.updatedDatasets().mainDataset());
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(0, result.schemaEvolutionSql().get().size());
    }

    // Alter column
    // name: VARCHAR(64) NOT NULL -> VARCHAR(256) NOT NULL
    // income: BIGINT -> INTEGER
    @Test
    void testDataTypeSizeChange() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getMainTableWithBatchUpdateTimeField();
        DatasetDefinition stagingTable = TestUtils.getSchemaEvolutionDataTypeLengthIncrementStagingTable();

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
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_LENGTH_CHANGE);
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
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(1, result.schemaEvolutionSql().get().size());
        Assertions.assertEquals("ALTER TABLE \"TEST\".\"main\" ALTER COLUMN \"name\" VARCHAR(256) NOT NULL", result.schemaEvolutionSql().get().get(0));

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "datatype_type_size_change_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "datatype_type_size_change_expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadStagingDataForIntIncome(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1,0,1,0,0);
        result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_03);
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.expectedMainTableSchemaWithLengthEvolution(), result.updatedDatasets().mainDataset());
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(0, result.schemaEvolutionSql().get().size());
    }

    // Alter column
    // name: VARCHAR(64) NOT NULL -> VARCHAR(64)
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
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(1, result.schemaEvolutionSql().get().size());
        Assertions.assertEquals("ALTER TABLE \"TEST\".\"main\" ALTER COLUMN \"name\" SET NULL", result.schemaEvolutionSql().get().get(0));

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "column_nullability_change_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "column_nullability_change_expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadBasicStagingData(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1,0,1,0,0);
        result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_03);
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(createDatasetWithUpdatedField(TestUtils.expectedMainTableSchema(), name.withNullable(true)), result.updatedDatasets().mainDataset());
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(0, result.schemaEvolutionSql().get().size());
    }

    // Alter column
    // income: INTEGER NOT NULL -> BIGINT
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
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(2, result.schemaEvolutionSql().get().size());
        Assertions.assertEquals("ALTER TABLE \"TEST\".\"main\" ALTER COLUMN \"income\" BIGINT", result.schemaEvolutionSql().get().get(0));
        Assertions.assertEquals("ALTER TABLE \"TEST\".\"main\" ALTER COLUMN \"income\" SET NULL", result.schemaEvolutionSql().get().get(1));

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "data_type_conversion_and_column_nullability_change_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "data_type_conversion_and_column_nullability_change_expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadBasicStagingData(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1,0,1,0,0);
        result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_03);
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.expectedMainTableSchema(), result.updatedDatasets().mainDataset());
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(0, result.schemaEvolutionSql().get().size());
    }

    // Alter column
    // income: BIGINT -> DECIMAL(10, 2)
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
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_LENGTH_CHANGE);
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_SCALE_CHANGE);
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
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(1, result.schemaEvolutionSql().get().size());
        Assertions.assertEquals("ALTER TABLE \"TEST\".\"main\" ALTER COLUMN \"income\" DECIMAL(10,2)", result.schemaEvolutionSql().get().get(0));

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "data_type_conversion_and_data_type_size_change_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "data_type_conversion_and_data_type_size_change_expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadStagingDataForDecimalIncome(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1,0,1,0,0);
        result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_03);
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.expectedMainTableSchemaWithDatatypeChange(), result.updatedDatasets().mainDataset());
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(0, result.schemaEvolutionSql().get().size());
    }

    // Alter column
    // char_5: CHAR(5) -> VARCHAR
    // char: CHAR -> VARCHAR(10)
    // char_100: CHAR(100) -> VARCHAR(1000)
    @Test
    void testDataTypeConversionAndDataTypeSizeChangeExplicit() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getMainTableForExplicit();
        DatasetDefinition stagingTable = TestUtils.getStagingTableForExplicit();

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
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_LENGTH_CHANGE);
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, "char_5", "char", "char_100", digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform Pass1 (Schema Evolution) ------------------------
        String dataPass1 = basePathForInput + "explicit_data_type_conversion_and_data_type_size_change_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "explicit_data_type_conversion_and_data_type_size_change_expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForExplicit(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3,0,3,0,0);
        IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_01);
        // 3. Verify schema changes in database
        List<Map<String, Object>> actualTableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
        assertTableColumnsEquals(Arrays.asList(schema), actualTableData);
        Assertions.assertEquals("VARCHAR", getColumnDataTypeFromTable(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, "char_5"));
        Assertions.assertEquals(1000000000, getColumnDataTypeLengthFromTable(h2Sink, mainTableName, "char_5"));
        Assertions.assertEquals("VARCHAR", getColumnDataTypeFromTable(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, "char"));
        Assertions.assertEquals(10, getColumnDataTypeLengthFromTable(h2Sink, mainTableName, "char"));
        Assertions.assertEquals("VARCHAR", getColumnDataTypeFromTable(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, "char_100"));
        Assertions.assertEquals(1000, getColumnDataTypeLengthFromTable(h2Sink, mainTableName, "char_100"));
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.getExpectedMainTableForExplicit(), result.updatedDatasets().mainDataset());
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(3, result.schemaEvolutionSql().get().size());
        Assertions.assertEquals("ALTER TABLE \"TEST\".\"main\" ALTER COLUMN \"char_5\" VARCHAR(1000000000)", result.schemaEvolutionSql().get().get(0));
        Assertions.assertEquals("ALTER TABLE \"TEST\".\"main\" ALTER COLUMN \"char\" VARCHAR(10)", result.schemaEvolutionSql().get().get(1));
        Assertions.assertEquals("ALTER TABLE \"TEST\".\"main\" ALTER COLUMN \"char_100\" VARCHAR(1000)", result.schemaEvolutionSql().get().get(2));

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "explicit_data_type_conversion_and_data_type_size_change_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "explicit_data_type_conversion_and_data_type_size_change_expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadStagingDataForExplicit(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1,0,1,0,0);
        result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_03);
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.getExpectedMainTableForExplicit(), result.updatedDatasets().mainDataset());
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(0, result.schemaEvolutionSql().get().size());
    }

    // Alter column
    // decimal_10_2: DECIMAL(10, 2) -> INTEGER
    // varchar_10: VARCHAR(10) -> STRING
    // another_varchar_10: VARCHAR(10) -> STRING(20)
    @Test
    void testDataTypeConversionAndDataTypeSizeChangeImplicit() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getMainTableForImplicit();
        DatasetDefinition stagingTable = TestUtils.getStagingTableForImplicit();

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
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_LENGTH_CHANGE);
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_SCALE_CHANGE);
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, "decimal_10_2", "varchar_10", "another_varchar_10", digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform Pass1 (Schema Evolution) ------------------------
        String dataPass1 = basePathForInput + "implicit_data_type_conversion_and_data_type_size_change_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "implicit_data_type_conversion_and_data_type_size_change_expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForImplicit(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3,0,3,0,0);
        IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_01);
        // 3. Verify schema changes in database
        List<Map<String, Object>> actualTableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
        assertTableColumnsEquals(Arrays.asList(schema), actualTableData);
        Assertions.assertEquals("DECIMAL", getColumnDataTypeFromTable(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, "decimal_10_2"));
        Assertions.assertEquals(10, getColumnDataTypeLengthFromTable(h2Sink, mainTableName, "decimal_10_2"));
        Assertions.assertEquals(2, getColumnDataTypeScaleFromTable(h2Sink, mainTableName, "decimal_10_2"));
        Assertions.assertEquals("VARCHAR", getColumnDataTypeFromTable(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, "varchar_10"));
        Assertions.assertEquals(1000000000, getColumnDataTypeLengthFromTable(h2Sink, mainTableName, "varchar_10"));
        Assertions.assertEquals("VARCHAR", getColumnDataTypeFromTable(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, "another_varchar_10"));
        Assertions.assertEquals(20, getColumnDataTypeLengthFromTable(h2Sink, mainTableName, "another_varchar_10"));
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.getExpectedMainTableForImplicit(), result.updatedDatasets().mainDataset());
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(2, result.schemaEvolutionSql().get().size());
        Assertions.assertEquals("ALTER TABLE \"TEST\".\"main\" ALTER COLUMN \"varchar_10\" VARCHAR(1000000000)", result.schemaEvolutionSql().get().get(0));
        Assertions.assertEquals("ALTER TABLE \"TEST\".\"main\" ALTER COLUMN \"another_varchar_10\" VARCHAR(20)", result.schemaEvolutionSql().get().get(1));

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "implicit_data_type_conversion_and_data_type_size_change_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "implicit_data_type_conversion_and_data_type_size_change_expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadStagingDataForImplicit(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1,0,1,0,0);
        result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_03);
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.getExpectedMainTableForImplicit(), result.updatedDatasets().mainDataset());
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(0, result.schemaEvolutionSql().get().size());
    }

    // Alter column
    // varchar: VARCHAR -> STRING(10)
    @Test
    void testDataTypeConversionAndDataTypeSizeChangeImplicitFailBecauseLengthDecrement() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getMainTableForImplicitDecrement();
        DatasetDefinition stagingTable = TestUtils.getStagingTableForImplicitDecrement();

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
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_LENGTH_CHANGE);
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_SCALE_CHANGE);
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, "varchar", digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform Pass1 (Schema Evolution) ------------------------
        String dataPass1 = basePathForInput + "implicit_data_type_conversion_and_data_type_size_change_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "implicit_data_type_conversion_and_data_type_size_change_expected_pass1.csv";
        // Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3,0,3,0,0);

        try
        {
            IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_03);
            Assertions.fail("Exception was not thrown");
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Data type size is decremented from \"1000000000\" to \"10\" for column \"varchar\"", e.getMessage());
        }
    }


    // Alter column
    // varchar_64: VARCHAR(64) -> VARCHAR
    @Test
    void testDataTypeConversionAndDataTypeSizeChangeSameType() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getMainTableForSameType();
        DatasetDefinition stagingTable = TestUtils.getStagingTableForSameType();

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
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_LENGTH_CHANGE);
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_SCALE_CHANGE);
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, "varchar_64", digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform Pass1 (Schema Evolution) ------------------------
        String dataPass1 = basePathForInput + "same_data_type_conversion_and_data_type_size_change_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "same_data_type_conversion_and_data_type_size_change_expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForSame(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3,0,3,0,0);
        IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_01);
        // 3. Verify schema changes in database
        List<Map<String, Object>> actualTableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
        assertTableColumnsEquals(Arrays.asList(schema), actualTableData);
        Assertions.assertEquals("VARCHAR", getColumnDataTypeFromTable(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, "varchar_64"));
        Assertions.assertEquals(1000000000, getColumnDataTypeLengthFromTable(h2Sink, mainTableName, "varchar_64"));
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.getExpectedMainTableForSameType(), result.updatedDatasets().mainDataset());
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(1, result.schemaEvolutionSql().get().size());
        Assertions.assertEquals("ALTER TABLE \"TEST\".\"main\" ALTER COLUMN \"varchar_64\" VARCHAR(1000000000)", result.schemaEvolutionSql().get().get(0));

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "same_data_type_conversion_and_data_type_size_change_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "same_data_type_conversion_and_data_type_size_change_expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadStagingDataForSame(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1,0,1,0,0);
        result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_03);
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.getExpectedMainTableForSameType(), result.updatedDatasets().mainDataset());
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(0, result.schemaEvolutionSql().get().size());
    }

    // Alter column
    // varchar: VARCHAR -> VARCHAR(64)
    @Test
    void testDataTypeConversionAndDataTypeSizeChangeSameTypeFailBecauseLengthDecrement() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getMainTableForSameTypeDecrement();
        DatasetDefinition stagingTable = TestUtils.getStagingTableForSameTypeDecrement();

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
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_LENGTH_CHANGE);
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_SCALE_CHANGE);
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[]{idName, nameName, "varchar", digestName, batchUpdateTimeName, batchIdName};

        // ------------ Perform Pass1 (Schema Evolution) ------------------------
        String dataPass1 = basePathForInput + "same_data_type_conversion_and_data_type_size_change_data_pass1.csv";
        String expectedDataPass1 = basePathForExpected + "same_data_type_conversion_and_data_type_size_change_expected_pass1.csv";
        // Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(3,0,3,0,0);
        try
        {
            IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_03);
            Assertions.fail("Exception was not thrown");
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Data type size is decremented from \"1000000000\" to \"64\" for column \"varchar\"", e.getMessage());
        }
    }

    // Missing column
    // name
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
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ALLOW_MISSING_COLUMNS);
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
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(1, result.schemaEvolutionSql().get().size());
        Assertions.assertEquals("ALTER TABLE \"TEST\".\"main\" ALTER COLUMN \"name\" SET NULL", result.schemaEvolutionSql().get().get(0));

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = basePathForInput + "make_main_column_nullable_data_pass2.csv";
        String expectedDataPass2 = basePathForExpected + "make_main_column_nullable_expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadStagingDataForWithoutName(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1,0,1,0,0);
        result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_03);
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(createDatasetWithUpdatedField(TestUtils.expectedMainTableSchema(), name.withNullable(true)), result.updatedDatasets().mainDataset());
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(0, result.schemaEvolutionSql().get().size());
    }

    // Add column
    // balance BIGINT
    @Test
    void testBitempDeltaAddColumn() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBitemporalFromOnlyMainTableIdBasedSchemaEvolutionAddColumn();
        DatasetDefinition stagingTable = TestUtils.getBitemporalFromOnlyStagingTableIdBased();

        // Create staging table
        createStagingTable(stagingTable);

        //Create main table with Old schema
        createTempTable(mainTable);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(startDateTimeName)
                .dateTimeThruName(endDateTimeName)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(dateTimeName)
                    .build())
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).enableSchemaEvolution(true).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[] {indexName, balanceName, digestName, startDateTimeName, endDateTimeName, batchIdInName, batchIdOutName};

        // ------------ Perform Pass1 (Schema Evolution) ------------------------
        String dataPass1 = "src/test/resources/data/bitemporal-incremental-milestoning/input/batch_id_based/" + "source_specifies_from/without_delete_ind/set_1/staging_data_pass1.csv";
        String expectedDataPass1 = "src/test/resources/data/bitemporal-incremental-milestoning/expected/batch_id_based/" + "source_specifies_from/without_delete_ind/set_1/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForBitemporalFromOnly(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(1, 0, 1, 0, 0);
        IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_01);
        // 3. Verify schema changes in database
        List<Map<String, Object>> actualTableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
        assertTableColumnsEquals(Arrays.asList(schema), actualTableData);
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.getExpectedBitemporalFromOnlyMainTableIdBased(), result.updatedDatasets().mainDataset());
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(1, result.schemaEvolutionSql().get().size());
        Assertions.assertEquals("ALTER TABLE \"TEST\".\"main\" ADD COLUMN \"balance\" BIGINT", result.schemaEvolutionSql().get().get(0));

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = "src/test/resources/data/bitemporal-incremental-milestoning/input/batch_id_based/" + "source_specifies_from/without_delete_ind/set_1/staging_data_pass2.csv";
        String expectedDataPass2 = "src/test/resources/data/bitemporal-incremental-milestoning/expected/batch_id_based/" + "source_specifies_from/without_delete_ind/set_1/expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadStagingDataForBitemporalFromOnly(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1, 0, 1, 1, 0);
        result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_02);
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.getExpectedBitemporalFromOnlyMainTableIdBased(), result.updatedDatasets().mainDataset());
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(0, result.schemaEvolutionSql().get().size());
    }

    // Add column
    // balance BIGINT
    // delete_indicator VARCHAR (ignored)
    @Test
    void testBitempDeltaWithDeleteIndicatorAddColumn() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getBitemporalFromOnlyMainTableIdBasedSchemaEvolutionAddColumn();
        DatasetDefinition stagingTable = TestUtils.getBitemporalFromOnlyStagingTableWithDeleteIndicatorIdBased();

        // Create staging table
        createStagingTable(stagingTable);

        //Create main table with Old schema
        createTempTable(mainTable);

        BitemporalDelta ingestMode = BitemporalDelta.builder()
            .digestField(digestName)
            .transactionMilestoning(BatchId.builder()
                .batchIdInName(batchIdInName)
                .batchIdOutName(batchIdOutName)
                .build())
            .validityMilestoning(ValidDateTime.builder()
                .dateTimeFromName(startDateTimeName)
                .dateTimeThruName(endDateTimeName)
                .validityDerivation(SourceSpecifiesFromDateTime.builder()
                    .sourceDateTimeFromField(dateTimeName)
                    .build())
                .build())
            .mergeStrategy(DeleteIndicatorMergeStrategy.builder()
                .deleteField(deleteIndicatorName)
                .addAllDeleteValues(Arrays.asList(deleteIndicatorValues))
                .build())
            .build();

        PlannerOptions options = PlannerOptions.builder().cleanupStagingData(false).collectStatistics(true).enableSchemaEvolution(true).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);
        Datasets datasets = Datasets.of(mainTable, stagingTable);

        String[] schema = new String[] {indexName, balanceName, digestName, startDateTimeName, endDateTimeName, batchIdInName, batchIdOutName};

        // ------------ Perform Pass1 (Schema Evolution) ------------------------
        String dataPass1 = "src/test/resources/data/bitemporal-incremental-milestoning/input/batch_id_based/" + "source_specifies_from/with_delete_ind/set_1/staging_data_pass1.csv";
        String expectedDataPass1 = "src/test/resources/data/bitemporal-incremental-milestoning/expected/batch_id_based/" + "source_specifies_from/with_delete_ind/set_1/expected_pass1.csv";
        // 1. Load staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteInd(dataPass1);
        // 2. Execute plans and verify results
        Map<String, Object> expectedStats = createExpectedStatsMap(1, 0, 1, 0, 0);
        IngestorResult result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass1, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_01);
        // 3. Verify schema changes in database
        List<Map<String, Object>> actualTableData = h2Sink.executeQuery("select * from \"TEST\".\"main\"");
        assertTableColumnsEquals(Arrays.asList(schema), actualTableData);
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.getExpectedBitemporalFromOnlyMainTableIdBased(), result.updatedDatasets().mainDataset());
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(1, result.schemaEvolutionSql().get().size());
        Assertions.assertEquals("ALTER TABLE \"TEST\".\"main\" ADD COLUMN \"balance\" BIGINT", result.schemaEvolutionSql().get().get(0));

        // ------------ Perform Pass2 ------------------------
        String dataPass2 = "src/test/resources/data/bitemporal-incremental-milestoning/input/batch_id_based/" + "source_specifies_from/with_delete_ind/set_1/staging_data_pass2.csv";
        String expectedDataPass2 = "src/test/resources/data/bitemporal-incremental-milestoning/expected/batch_id_based/" + "source_specifies_from/with_delete_ind/set_1/expected_pass2.csv";
        // 1. Update datasets
        datasets = result.updatedDatasets();
        // 2. Load staging table
        loadStagingDataForBitemporalFromOnlyWithDeleteInd(dataPass2);
        // 3. Execute plans and verify results
        expectedStats = createExpectedStatsMap(1, 0, 1, 1, 0);
        result = executePlansAndVerifyResults(ingestMode, options, datasets, schema, expectedDataPass2, expectedStats, schemaEvolutionCapabilitySet, fixedClock_2000_01_02);
        // 4. Verify schema changes in model objects
        assertUpdatedDataset(TestUtils.getExpectedBitemporalFromOnlyMainTableIdBased(), result.updatedDatasets().mainDataset());
        // 5. Verify schema evolution SQLs
        Assertions.assertEquals(0, result.schemaEvolutionSql().get().size());
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