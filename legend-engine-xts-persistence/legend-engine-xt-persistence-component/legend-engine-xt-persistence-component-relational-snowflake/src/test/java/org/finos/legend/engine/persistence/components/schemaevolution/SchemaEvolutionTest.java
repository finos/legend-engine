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
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetAdditionalProperties;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.TableOrigin;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.optimizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.snowflake.SnowflakeSink;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.finos.legend.engine.persistence.components.util.SchemaEvolutionCapability;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, true);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(expectedSchemaEvolutionModifySize, sqlsForSchemaEvolution.get(0));
    }

    // Data sizing change (length) in main table column and data_type_size_change capability allowed with upper case optimizer enabled
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

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(expectedSchemaEvolutionModifySizeWithUpperCase, sqlsForSchemaEvolution.get(0));
    }

    //Data sizing (length) changes but user capability doesn't allow it --> throws exception
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

        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
            RelationalTransformer transformer = new RelationalTransformer(relationalSink);
            SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

            List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
            Assertions.assertEquals(expectedSchemaEvolutionModifySize, sqlsForSchemaEvolution.get(0));
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Data type length changes couldn't be performed on column \"description\" since sink/user capability does not allow it", e.getMessage());
        }
    }

    // Data sizing change (scale) in main table column and data_type_size_change capability allowed
    @Test
    void testSnapshotMilestoningWithColumnScaleChangeEvolution()
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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, false);

        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
            RelationalTransformer transformer = new RelationalTransformer(relationalSink);
            SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

            List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
            Assertions.assertEquals(expectedSchemaEvolutionModifyScale, sqlsForSchemaEvolution.get(0));
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Data type scale changes couldn't be performed on column \"decimal_col\" since sink/user capability does not allow it", e.getMessage());
        }
    }

    // Implicit data type conversion is automatically handled by DB. No additional alter statement generated
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

        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
            RelationalTransformer transformer = new RelationalTransformer(relationalSink);
            SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

            List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
            Assertions.assertEquals(expectedSchemaNonBreakingChange, sqlsForSchemaEvolution.get(0));
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Explicit data type conversion from \"FLOAT\" to \"DOUBLE\" couldn't be performed since sink/user capability does not allow it", e.getMessage());
        }
    }
}
