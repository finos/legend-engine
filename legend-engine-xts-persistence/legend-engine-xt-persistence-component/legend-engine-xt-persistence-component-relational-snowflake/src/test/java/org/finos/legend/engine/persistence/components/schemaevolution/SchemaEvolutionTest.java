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

package org.finos.legend.engine.persistence.components.schemaevolution;

import org.finos.legend.engine.persistence.components.IngestModeTest;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.*;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.ClusterKey;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.optimizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.snowflake.SnowflakeSink;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.sink.Sink;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.finos.legend.engine.persistence.components.util.SchemaEvolutionCapability;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SchemaEvolutionTest extends IngestModeTest
{
    private RelationalSink relationalSink = SnowflakeSink.get();

    private Field id = Field.builder().name("id").type(FieldType.of(DataType.BIGINT, Optional.empty(), Optional.empty())).primaryKey(true).build();
    private Field name = Field.builder().name("name").type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).primaryKey(true).build();
    private Field amount = Field.builder().name("amount").type(FieldType.of(DataType.DOUBLE, Optional.empty(), Optional.empty())).build();
    private Field bizDate = Field.builder().name("biz_date").type(FieldType.of(DataType.DATE, Optional.empty(), Optional.empty())).build();
    private Field decimalCol = Field.builder().name("decimal_col").type(FieldType.of(DataType.DECIMAL, 10, 5)).build();
    private Field numericId = Field.builder().name("id").type(FieldType.of(DataType.NUMERIC, Optional.empty(), Optional.empty())).primaryKey(true).build();
    private Field floatAmount = Field.builder().name("amount").type(FieldType.of(DataType.FLOAT, Optional.empty(), Optional.empty())).build();
    private Field decimalColModified = Field.builder().name("decimal_col").type(FieldType.of(DataType.NUMERIC, 20, 5)).build();
    private Field descriptionWithI6MBLength = Field.builder().name("description").type(FieldType.of(DataType.VARCHAR, 16777216, null)).build();
    private Field descriptionWith128MBLength = Field.builder().name("description").type(FieldType.of(DataType.VARCHAR, 134217728, null)).build();

    private SchemaDefinition baseTableSchema = SchemaDefinition.builder()
        .addFields(id)
        .addFields(name)
        .addFields(amount)
        .addFields(bizDate)
        .addFields(decimalCol)
        .build();

    private SchemaDefinition stagingTableImplicitDatatypeChange = SchemaDefinition.builder()
        .addFields(numericId)
        .addFields(name)
        .addFields(floatAmount)
        .addFields(bizDate)
        .addFields(decimalCol)
        .build();

    private SchemaDefinition stagingTableImplicitDatatypeChangeAndLengthChange = SchemaDefinition.builder()
        .addFields(numericId)
        .addFields(name)
        .addFields(floatAmount)
        .addFields(bizDate)
        .addFields(decimalColModified)
        .build();

    private String expectedAlterDecimalLength = "ALTER TABLE \"mydb\".\"main\" ALTER COLUMN \"decimal_col\" NUMBER(20,5)";

    // Data sizing change in main table column and data_type_size_change capability allowed
    // Alter column
    // description: VARCHAR -> VARCHAR(64)
    @Test
    void testSnapshotMilestoningWithColumnLengthChangeEvolution()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchemaWithDataLengthChange)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableEvolvedLength)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_LENGTH_CHANGE);
        SchemaEvolution schemaEvolution = new SchemaEvolution(getRelationSink(false), ingestMode, schemaEvolutionCapabilitySet, true);

        IncompatibleSchemaChangeException exception = assertThrows(IncompatibleSchemaChangeException.class,
                () -> schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema()));

        Assertions.assertEquals("Data type size is decremented from \"16777216\" to \"64\" for column \"description\"", exception.getMessage());
    }

    // Data sizing change (length) in main table column and data_type_size_change capability allowed with upper case optimizer enabled
    // Alter column
    // DESCRIPTION: VARCHAR -> VARCHAR(64)
    @Test
    void testSnapshotMilestoningWithColumnLengthChangeEvolutionWithUpperCase()
    {
        RelationalTransformer transformer = new RelationalTransformer(relationalSink, TransformOptions.builder().addOptimizers(new UpperCaseOptimizer()).build());

        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchemaWithDataLengthChange)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableEvolvedLength)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_LENGTH_CHANGE);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, true);

        IncompatibleSchemaChangeException exception = assertThrows(IncompatibleSchemaChangeException.class,
                () -> schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema()));

        Assertions.assertEquals("Data type size is decremented from \"16777216\" to \"64\" for column \"description\"", exception.getMessage());
    }

    //Data sizing (length) changes but user capability doesn't allow it --> throws exception
    // Alter column
    // description: VARCHAR -> VARCHAR(64)
    @Test
    void testSnapshotMilestoningWithColumnLengthChangeEvolutionAndUserProvidedSchemaEvolutionCapability()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchemaWithDataLengthChange)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableEvolvedLength)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, true);

        IncompatibleSchemaChangeException exception = assertThrows(IncompatibleSchemaChangeException.class,
                () -> schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema()));

        Assertions.assertEquals("Data type size is decremented from \"16777216\" to \"64\" for column \"description\"", exception.getMessage());
    }

    // Data sizing change (scale) in main table column and data_type_size_change capability allowed
    // Alter column
    // decimal_col: DECIMAL(10, 0) -> DECIMAL(10, 2)
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testSnapshotMilestoningWithColumnScaleChangeEvolution(boolean icebergSink)
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchemaWithDataScaleChange)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableEvolvedScale)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_LENGTH_CHANGE);
        SchemaEvolution schemaEvolution = new SchemaEvolution(getRelationSink(icebergSink), ingestMode, schemaEvolutionCapabilitySet, false);

        IncompatibleSchemaChangeException exception = assertThrows(IncompatibleSchemaChangeException.class,
                () -> schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema()));

        Assertions.assertEquals("Data type scale changes couldn't be performed on column \"decimal_col\" since sink/user capability does not allow it", exception.getMessage());
    }

    // Implicit data type conversion is automatically handled by DB. No additional alter statement generated
    // Alter column
    // id: BIGINT -> NUMERIC
    // amount: DOUBLE -> FLOAT
    @Test
    void testSnapshotMilestoningWithImplicitDataTypeEvolution()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchema)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableImplicitDatatypeChange)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, Collections.emptySet(), true);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(0, sqlsForSchemaEvolution.size());
    }

    // Implicit data type conversion is automatically handled by DB. Alter statement is generated for length change
    // Alter column
    // id: BIGINT -> NUMERIC
    // amount: DOUBLE -> FLOAT
    // decimal_col: DECIMAL(10, 5) -> DECIMAL(20, 5)
    @Test
    void testSnapshotMilestoningWithImplicitDataTypeEvolutionAndLengthEvolution()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchema)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(stagingTableImplicitDatatypeChangeAndLengthChange)
            .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_LENGTH_CHANGE);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, true);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(expectedAlterDecimalLength, sqlsForSchemaEvolution.get(0));
    }

    // Add column
    // biz_date: DATE
    @Test
    void testSnapshotMilestoningWithAddColumnAndIgnoreCase()
    {
        RelationalTransformer transformer = new RelationalTransformer(relationalSink, TransformOptions.builder().build());

        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableShortenedSchema)
                .datasetAdditionalProperties(DatasetAdditionalProperties.builder().tableOrigin(TableOrigin.ICEBERG).build())
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(baseTableSchemaWithSomeColumnsInUpperCase)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, true);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        // Use the planner utils to return the sql
        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
        Assertions.assertEquals(1, sqlsForSchemaEvolution.size());
        String expectedSchemaEvolutionAddColumn = "ALTER ICEBERG TABLE \"mydb\".\"main\" ADD COLUMN \"biz_date\" DATE";
        Assertions.assertEquals(expectedSchemaEvolutionAddColumn, sqlsForSchemaEvolution.get(0));
    }

    // Alter column
    // amount: DOUBLE NOT NULL -> DOUBLE
    // biz_date: DATE NOT NULL -> DATE
    @Test
    void testSnapshotMilestoningWithAlterNullabilityAndUserCapability()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchemaWithNonNullableColumn)
                .datasetAdditionalProperties(DatasetAdditionalProperties.builder().tableOrigin(TableOrigin.ICEBERG).build())
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableNullableChange)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.COLUMN_NULLABILITY_CHANGE);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, false);
        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(2, sqlsForSchemaEvolution.size());
        String expectedSchemaImplicitNullabilityChange = "ALTER ICEBERG TABLE \"mydb\".\"main\" ALTER COLUMN \"amount\" DROP NOT NULL";
        String expectedSchemaNullabilityChange = "ALTER ICEBERG TABLE \"mydb\".\"main\" ALTER COLUMN \"biz_date\" DROP NOT NULL";
        Assertions.assertEquals(expectedSchemaImplicitNullabilityChange, sqlsForSchemaEvolution.get(0));
        Assertions.assertEquals(expectedSchemaNullabilityChange, sqlsForSchemaEvolution.get(1));
    }

    // Data type change required in main table column (float --> double) and data_type_conversion capability allowed
    // Alter column
    // amount: FLOAT -> DOUBLE
    @Test
    void testSnapshotMilestoningWithNonBreakingDataTypeEvolution()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableExplicitDatatypeChangeSchema)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableNonBreakingDatatypeChange)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_CONVERSION);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, true);

        IncompatibleSchemaChangeException exception = assertThrows(IncompatibleSchemaChangeException.class,
                () -> schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema()));
        Assertions.assertEquals("Explicit data type conversion from \"FLOAT\" to \"DOUBLE\" couldn't be performed since sink/user capability does not allow it", exception.getMessage());

    }

    //Alter varchar column to increase to max length supported by sink for iceberg
    @Test
    void testIcebergVarcharColumnLengthChangeEvolution()
    {
        SchemaDefinition.Builder schemaDefinitionBuilder = SchemaDefinition.builder()
                .addFields(id)
                .addFields(name)
                .addFields(amount)
                .addFields(bizDate);

        Dataset existingDatasetWith16MBVarcharCol = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(schemaDefinitionBuilder
                        .addFields(descriptionWithI6MBLength)
                        .build())
                .datasetAdditionalProperties(DatasetAdditionalProperties.builder().tableOrigin(TableOrigin.ICEBERG).build())
                .build();

        Dataset mainTableCreatedWithMaxLength = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(schemaDefinitionBuilder
                        .addFields(descriptionWith128MBLength)
                        .build())
                .datasetAdditionalProperties(DatasetAdditionalProperties.builder().tableOrigin(TableOrigin.ICEBERG).build())
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(baseTableSchemaWithDataLengthChange)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_LENGTH_CHANGE);

        SchemaEvolution schemaEvolution = new SchemaEvolution(getRelationSink(true), ingestMode, schemaEvolutionCapabilitySet, true);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(existingDatasetWith16MBVarcharCol, stagingTable.schema());
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        // Use the planner utils to return the sql
        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
        Assertions.assertEquals(1, sqlsForSchemaEvolution.size());
        String expectedSchemaEvolutionAddColumn = "ALTER ICEBERG TABLE \"mydb\".\"main\" ALTER COLUMN \"description\" VARCHAR";
        Assertions.assertEquals(expectedSchemaEvolutionAddColumn, sqlsForSchemaEvolution.get(0));

        result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTableCreatedWithMaxLength, stagingTable.schema());
        transformer = new RelationalTransformer(relationalSink);
        physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
        Assertions.assertEquals(1, sqlsForSchemaEvolution.size());
        expectedSchemaEvolutionAddColumn = "ALTER ICEBERG TABLE \"mydb\".\"main\" ALTER COLUMN \"description\" VARCHAR";
        Assertions.assertEquals(expectedSchemaEvolutionAddColumn, sqlsForSchemaEvolution.get(0));
    }

    void testAlterClusterKey()
    {
        RelationalTransformer transformer = new RelationalTransformer(relationalSink, TransformOptions.builder().build());
        Dataset datasetWithoutClusterKey = DatasetDefinition.builder()
                .database(mainDbName)
                .name(mainTableName)
                .alias(mainTableAlias)
                .schema(baseTableSchema)
                .build();
        Dataset datasetWithOneClusterKey = DatasetDefinition.builder()
                .database(mainDbName)
                .name(mainTableName)
                .alias(mainTableAlias)
                .schema(baseTableSchema.withClusterKeys(ClusterKey.of(FieldValue.builder().fieldName("id").build())))
                .build();
        Dataset datasetWithTwoClusterKey = DatasetDefinition.builder()
                .database(mainDbName)
                .name(mainTableName)
                .alias(mainTableAlias)
                .schema(baseTableSchema.withClusterKeys(ClusterKey.of(FieldValue.builder().fieldName("id").build()), ClusterKey.of(FieldValue.builder().fieldName("name").build())))
                .build();
        
        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, Collections.emptySet(), false);

        //Add cluster key
        List<Operation> addClusterKeyOperations = schemaEvolution.buildLogicalPlanForSchemaEvolution(datasetWithoutClusterKey, datasetWithOneClusterKey.schema()).logicalPlan().ops();
        assertEquals(1, addClusterKeyOperations.size());
        SqlPlan addClusterKeySqlPlan = transformer.generatePhysicalPlan(LogicalPlan.of(addClusterKeyOperations));
        assertEquals("ALTER TABLE \"mydb\".\"main\" CLUSTER BY (\"id\")", addClusterKeySqlPlan.getSqlList().get(0));

        //Add another cluster key
        List<Operation> addAnotherClusterKeyOperations = schemaEvolution.buildLogicalPlanForSchemaEvolution(datasetWithOneClusterKey, datasetWithTwoClusterKey.schema()).logicalPlan().ops();
        assertEquals(1, addAnotherClusterKeyOperations.size());
        SqlPlan addAnotherClusterKeySqlPlan = transformer.generatePhysicalPlan(LogicalPlan.of(addAnotherClusterKeyOperations));
        assertEquals("ALTER TABLE \"mydb\".\"main\" CLUSTER BY (\"id\", \"name\")", addAnotherClusterKeySqlPlan.getSqlList().get(0));

        // Change cluster key back to one cluster key
        List<Operation> changeClusterKeyOperations = schemaEvolution.buildLogicalPlanForSchemaEvolution(datasetWithTwoClusterKey, datasetWithOneClusterKey.schema()).logicalPlan().ops();
        assertEquals(1, changeClusterKeyOperations.size());
        SqlPlan changeClusterKeySqlPlan = transformer.generatePhysicalPlan(LogicalPlan.of(changeClusterKeyOperations));
        assertEquals("ALTER TABLE \"mydb\".\"main\" CLUSTER BY (\"id\")", changeClusterKeySqlPlan.getSqlList().get(0));

        // Remove cluster key
        List<Operation> removeClusterKeyOperations = schemaEvolution.buildLogicalPlanForSchemaEvolution(datasetWithOneClusterKey, datasetWithoutClusterKey.schema()).logicalPlan().ops();
        assertEquals(1, removeClusterKeyOperations.size());
        SqlPlan removeClusterKeySqlPlan = transformer.generatePhysicalPlan(LogicalPlan.of(removeClusterKeyOperations));
        assertEquals("ALTER TABLE \"mydb\".\"main\" DROP CLUSTERING KEY", removeClusterKeySqlPlan.getSqlList().get(0));
}

    private Sink getRelationSink(boolean icebergSink)
    {
        if (icebergSink)
        {
            return SnowflakeSink.getIcebergInstance();
        }
        return SnowflakeSink.get();
    }
}
