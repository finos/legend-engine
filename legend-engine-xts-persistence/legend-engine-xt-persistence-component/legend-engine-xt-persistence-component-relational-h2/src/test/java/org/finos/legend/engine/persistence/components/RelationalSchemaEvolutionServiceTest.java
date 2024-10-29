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

package org.finos.legend.engine.persistence.components;

import org.finos.legend.engine.persistence.components.ingestmode.AppendOnly;
import org.finos.legend.engine.persistence.components.ingestmode.audit.DateTimeAuditing;
import org.finos.legend.engine.persistence.components.ingestmode.deduplication.FilterDuplicates;
import org.finos.legend.engine.persistence.components.ingestmode.digest.UserProvidedDigestGenStrategy;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.*;
import org.finos.legend.engine.persistence.components.relational.CaseConversion;
import org.finos.legend.engine.persistence.components.relational.api.RelationalSchemaEvolutionService;
import org.finos.legend.engine.persistence.components.relational.api.SchemaEvolutionServiceResult;
import org.finos.legend.engine.persistence.components.relational.api.SchemaEvolutionStatus;
import org.finos.legend.engine.persistence.components.relational.h2.H2Sink;
import org.finos.legend.engine.persistence.components.relational.jdbc.JdbcConnection;
import org.finos.legend.engine.persistence.components.schemaevolution.IncompatibleSchemaChangeException;
import org.finos.legend.engine.persistence.components.util.SchemaEvolutionCapability;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.finos.legend.engine.persistence.components.TestUtils.batchIdName;
import static org.finos.legend.engine.persistence.components.TestUtils.batchUpdateTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.digestName;
import static org.finos.legend.engine.persistence.components.TestUtils.expiryDateName;
import static org.finos.legend.engine.persistence.components.TestUtils.getColumnDataTypeFromTable;
import static org.finos.legend.engine.persistence.components.TestUtils.getColumnDataTypeLengthFromTable;
import static org.finos.legend.engine.persistence.components.TestUtils.getColumnsFromTable;
import static org.finos.legend.engine.persistence.components.TestUtils.getIsColumnNullableFromTable;
import static org.finos.legend.engine.persistence.components.TestUtils.idName;
import static org.finos.legend.engine.persistence.components.TestUtils.incomeName;
import static org.finos.legend.engine.persistence.components.TestUtils.mainTableName;
import static org.finos.legend.engine.persistence.components.TestUtils.nameName;
import static org.finos.legend.engine.persistence.components.TestUtils.startTimeName;
import static org.finos.legend.engine.persistence.components.TestUtils.testDatabaseName;
import static org.finos.legend.engine.persistence.components.TestUtils.testSchemaName;

class RelationalSchemaEvolutionServiceTest extends BaseTest
{
    @Test
    void testAddColumn() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getSchemaEvolutionAddColumnMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Create main table with old schema
        createTempTable(mainTable);

        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        RelationalSchemaEvolutionService evolutionService = RelationalSchemaEvolutionService.builder()
            .relationalSink(H2Sink.get())
            .schemaEvolutionCapabilitySet(schemaEvolutionCapabilitySet)
            .ingestMode(ingestMode)
            .build();

        SchemaEvolutionServiceResult result = evolutionService.evolve(mainTable.datasetReference(), stagingTable.schema(), JdbcConnection.of(h2Sink.connection()));

        List<String> actualSchema = getColumnsFromTable(h2Sink.connection(), null, testSchemaName, mainTableName);
        List<String> expectedSchema = Arrays.asList(schema);
        Assertions.assertTrue(actualSchema.size() == expectedSchema.size() && actualSchema.containsAll(expectedSchema) && expectedSchema.containsAll(actualSchema));
        Assertions.assertEquals(SchemaEvolutionStatus.SUCCEEDED, result.status());
        Assertions.assertEquals(Arrays.asList("ALTER TABLE \"TEST\".\"main\" ADD COLUMN \"income\" BIGINT"), result.executedSchemaEvolutionSqls());
    }

    @Test
    void testAddColumnUpperCase() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getSchemaEvolutionAddColumnMainTableUpperCase(); // This is only used to create a database table in upper case
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();
        DatasetReference mainTableDatasetReference = DatasetReferenceImpl.builder().group(testSchemaName)
                .name(mainTableName)
                .datasetAdditionalProperties(DatasetAdditionalProperties.builder().tableOrigin(TableOrigin.ICEBERG).build())
                .build(); // This is the model user has

        Assertions.assertEquals(DatasetAdditionalProperties.builder().tableOrigin(TableOrigin.ICEBERG).build(),
                mainTableDatasetReference.datasetAdditionalProperties().get());

        // Create staging table
        createStagingTable(stagingTable);

        // Create main table with old schema
        createTempTable(mainTable);

        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);

        String[] schema = new String[]{idName.toUpperCase(), nameName.toUpperCase(), incomeName.toUpperCase(), startTimeName.toUpperCase(), expiryDateName.toUpperCase(), digestName.toUpperCase(), batchUpdateTimeName.toUpperCase(), batchIdName.toUpperCase()};

        RelationalSchemaEvolutionService evolutionService = RelationalSchemaEvolutionService.builder()
            .relationalSink(H2Sink.get())
            .schemaEvolutionCapabilitySet(schemaEvolutionCapabilitySet)
            .ingestMode(ingestMode)
            .caseConversion(CaseConversion.TO_UPPER)
            .build();

        SchemaEvolutionServiceResult result = evolutionService.evolve(mainTableDatasetReference, stagingTable.schema(), JdbcConnection.of(h2Sink.connection()));

        List<String> actualSchema = getColumnsFromTable(h2Sink.connection(), null, testSchemaName.toUpperCase(), mainTableName.toUpperCase());
        List<String> expectedSchema = Arrays.asList(schema);
        Assertions.assertTrue(actualSchema.size() == expectedSchema.size() && actualSchema.containsAll(expectedSchema) && expectedSchema.containsAll(actualSchema));
        Assertions.assertEquals(SchemaEvolutionStatus.SUCCEEDED, result.status());
        Assertions.assertEquals(Arrays.asList("ALTER TABLE \"TEST\".\"MAIN\" ADD COLUMN \"INCOME\" BIGINT"), result.executedSchemaEvolutionSqls());
    }

    @Test
    void testDataTypeConversion() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getSchemaEvolutionDataTypeConversionMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Create main table with old schema
        createTempTable(mainTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_CONVERSION);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        RelationalSchemaEvolutionService evolutionService = RelationalSchemaEvolutionService.builder()
            .relationalSink(H2Sink.get())
            .schemaEvolutionCapabilitySet(schemaEvolutionCapabilitySet)
            .ingestMode(ingestMode)
            .build();

        SchemaEvolutionServiceResult result = evolutionService.evolve(mainTable.datasetReference(), stagingTable.schema(), JdbcConnection.of(h2Sink.connection()));

        List<String> actualSchema = getColumnsFromTable(h2Sink.connection(), null, testSchemaName, mainTableName);
        List<String> expectedSchema = Arrays.asList(schema);
        Assertions.assertTrue(actualSchema.size() == expectedSchema.size() && actualSchema.containsAll(expectedSchema) && expectedSchema.containsAll(actualSchema));
        Assertions.assertEquals("BIGINT", getColumnDataTypeFromTable(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, incomeName));
        Assertions.assertEquals(SchemaEvolutionStatus.SUCCEEDED, result.status());
        Assertions.assertEquals(Arrays.asList("ALTER TABLE \"TEST\".\"main\" ALTER COLUMN \"income\" BIGINT"), result.executedSchemaEvolutionSqls());
    }

    @Test
    void testDataTypeSizeChange() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getMainTableWithBatchUpdateTimeField();
        DatasetDefinition stagingTable = TestUtils.getSchemaEvolutionDataTypeSizeChangeStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Create main table with old schema
        createTempTable(mainTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_LENGTH_CHANGE);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        RelationalSchemaEvolutionService evolutionService = RelationalSchemaEvolutionService.builder()
            .relationalSink(H2Sink.get())
            .schemaEvolutionCapabilitySet(schemaEvolutionCapabilitySet)
            .ingestMode(ingestMode)
            .build();

        SchemaEvolutionServiceResult result = evolutionService.evolve(mainTable.datasetReference(), stagingTable.schema(), JdbcConnection.of(h2Sink.connection()));

        List<String> actualSchema = getColumnsFromTable(h2Sink.connection(), null, testSchemaName, mainTableName);
        List<String> expectedSchema = Arrays.asList(schema);
        Assertions.assertTrue(actualSchema.size() == expectedSchema.size() && actualSchema.containsAll(expectedSchema) && expectedSchema.containsAll(actualSchema));
        Assertions.assertEquals("BIGINT", getColumnDataTypeFromTable(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, incomeName));
        Assertions.assertEquals("VARCHAR", getColumnDataTypeFromTable(h2Sink.connection(), testDatabaseName, testSchemaName, mainTableName, nameName));
        Assertions.assertEquals(256, getColumnDataTypeLengthFromTable(h2Sink, mainTableName, nameName));
        Assertions.assertEquals(SchemaEvolutionStatus.SUCCEEDED, result.status());
        Assertions.assertEquals(Arrays.asList("ALTER TABLE \"TEST\".\"main\" ALTER COLUMN \"name\" VARCHAR(256) NOT NULL"), result.executedSchemaEvolutionSqls());
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

        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.COLUMN_NULLABILITY_CHANGE);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        RelationalSchemaEvolutionService evolutionService = RelationalSchemaEvolutionService.builder()
            .relationalSink(H2Sink.get())
            .schemaEvolutionCapabilitySet(schemaEvolutionCapabilitySet)
            .ingestMode(ingestMode)
            .build();

        SchemaEvolutionServiceResult result = evolutionService.evolve(mainTable.datasetReference(), stagingTable.schema(), JdbcConnection.of(h2Sink.connection()));

        List<String> actualSchema = getColumnsFromTable(h2Sink.connection(), null, testSchemaName, mainTableName);
        List<String> expectedSchema = Arrays.asList(schema);
        Assertions.assertTrue(actualSchema.size() == expectedSchema.size() && actualSchema.containsAll(expectedSchema) && expectedSchema.containsAll(actualSchema));
        Assertions.assertEquals("YES", getIsColumnNullableFromTable(h2Sink, mainTableName, nameName));
        Assertions.assertEquals(SchemaEvolutionStatus.SUCCEEDED, result.status());
        Assertions.assertEquals(Arrays.asList("ALTER TABLE \"TEST\".\"main\" ALTER COLUMN \"name\" SET NULL"), result.executedSchemaEvolutionSqls());
    }

    @Test
    void testMakeMainColumnNullable() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getMainTableWithBatchUpdateTimeField();
        DatasetDefinition stagingTable = TestUtils.getSchemaEvolutionMakeMainColumnNullableStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Create main table with old schema
        createTempTable(mainTable);

        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.COLUMN_NULLABILITY_CHANGE);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        RelationalSchemaEvolutionService evolutionService = RelationalSchemaEvolutionService.builder()
            .relationalSink(H2Sink.get())
            .schemaEvolutionCapabilitySet(schemaEvolutionCapabilitySet)
            .ingestMode(ingestMode)
            .build();

        SchemaEvolutionServiceResult result = evolutionService.evolve(mainTable.datasetReference(), stagingTable.schema(), JdbcConnection.of(h2Sink.connection()));

        List<String> actualSchema = getColumnsFromTable(h2Sink.connection(), null, testSchemaName, mainTableName);
        List<String> expectedSchema = Arrays.asList(schema);
        Assertions.assertTrue(actualSchema.size() == expectedSchema.size() && actualSchema.containsAll(expectedSchema) && expectedSchema.containsAll(actualSchema));
        Assertions.assertEquals("YES", getIsColumnNullableFromTable(h2Sink, mainTableName, nameName));
        Assertions.assertEquals(SchemaEvolutionStatus.SUCCEEDED, result.status());
        Assertions.assertEquals(Arrays.asList("ALTER TABLE \"TEST\".\"main\" ALTER COLUMN \"name\" SET NULL"), result.executedSchemaEvolutionSqls());
    }

    @Test
    void testAddColumnAndNullabilityChange() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getSchemaEvolutionAddColumnMainTable();
        DatasetDefinition stagingTable = TestUtils.getSchemaEvolutionColumnNullabilityChangeStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Create main table with old schema
        createTempTable(mainTable);

        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.COLUMN_NULLABILITY_CHANGE);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        RelationalSchemaEvolutionService evolutionService = RelationalSchemaEvolutionService.builder()
            .relationalSink(H2Sink.get())
            .schemaEvolutionCapabilitySet(schemaEvolutionCapabilitySet)
            .ingestMode(ingestMode)
            .build();

        SchemaEvolutionServiceResult result = evolutionService.evolve(mainTable.datasetReference(), stagingTable.schema(), JdbcConnection.of(h2Sink.connection()));

        List<String> actualSchema = getColumnsFromTable(h2Sink.connection(), null, testSchemaName, mainTableName);
        List<String> expectedSchema = Arrays.asList(schema);
        Assertions.assertTrue(actualSchema.size() == expectedSchema.size() && actualSchema.containsAll(expectedSchema) && expectedSchema.containsAll(actualSchema));
        Assertions.assertEquals("YES", getIsColumnNullableFromTable(h2Sink, mainTableName, nameName));
        Assertions.assertEquals(SchemaEvolutionStatus.SUCCEEDED, result.status());
        Assertions.assertEquals(Arrays.asList("ALTER TABLE \"TEST\".\"main\" ALTER COLUMN \"name\" SET NULL", "ALTER TABLE \"TEST\".\"main\" ADD COLUMN \"income\" BIGINT"), result.executedSchemaEvolutionSqls());
    }

    @Test
    void testSchemaEvolutionFailPKTypeDifferent() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getSchemaEvolutionPKTypeDifferentMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        // Create main table with pld schema
        createTempTable(mainTable);

        // Generate the milestoning object
        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_CONVERSION);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName};

        RelationalSchemaEvolutionService evolutionService = RelationalSchemaEvolutionService.builder()
            .relationalSink(H2Sink.get())
            .schemaEvolutionCapabilitySet(schemaEvolutionCapabilitySet)
            .ingestMode(ingestMode)
            .build();

        try
        {
            evolutionService.evolve(mainTable.datasetReference(), stagingTable.schema(), JdbcConnection.of(h2Sink.connection()));
            Assertions.fail("Exception was not thrown");
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Primary keys for main table has changed which is not allowed", e.getMessage());
        }
    }

    @Test
    void testSchemaEvolutionDatasetNotFound() throws Exception
    {
        DatasetDefinition mainTable = TestUtils.getSchemaEvolutionAddColumnMainTable();
        DatasetDefinition stagingTable = TestUtils.getBasicStagingTable();

        // Create staging table
        createStagingTable(stagingTable);

        AppendOnly ingestMode = AppendOnly.builder()
            .digestGenStrategy(UserProvidedDigestGenStrategy.builder().digestField(digestName).build())
            .deduplicationStrategy(FilterDuplicates.builder().build())
            .auditing(DateTimeAuditing.builder().dateTimeField(batchUpdateTimeName).build())
            .build();

        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);

        String[] schema = new String[]{idName, nameName, incomeName, startTimeName, expiryDateName, digestName, batchUpdateTimeName, batchIdName};

        RelationalSchemaEvolutionService evolutionService = RelationalSchemaEvolutionService.builder()
            .relationalSink(H2Sink.get())
            .schemaEvolutionCapabilitySet(schemaEvolutionCapabilitySet)
            .ingestMode(ingestMode)
            .build();

        SchemaEvolutionServiceResult result = evolutionService.evolve(mainTable.datasetReference(), stagingTable.schema(), JdbcConnection.of(h2Sink.connection()));

        Assertions.assertEquals(SchemaEvolutionStatus.FAILED, result.status());
        Assertions.assertEquals("Dataset is not found: main", result.message().get());
        Assertions.assertTrue(result.executedSchemaEvolutionSqls().isEmpty());
    }
}