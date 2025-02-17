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

package org.finos.legend.engine.persistence.components.schemaevolution;

import org.finos.legend.engine.persistence.components.IngestModeTest;
import org.finos.legend.engine.persistence.components.ingestmode.IngestMode;
import org.finos.legend.engine.persistence.components.ingestmode.NontemporalSnapshot;
import org.finos.legend.engine.persistence.components.ingestmode.audit.NoAuditing;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Dataset;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.RelationalSink;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.ansi.optimizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.ansi.sql.AnsiDatatypeToDefaultSizeMapping;
import org.finos.legend.engine.persistence.components.relational.sqldom.utils.SqlGenUtils;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.scenarios.BitemporalDeltaSourceSpecifiesFromAndThroughScenarios;
import org.finos.legend.engine.persistence.components.scenarios.BitemporalDeltaSourceSpecifiesFromOnlyScenarios;
import org.finos.legend.engine.persistence.components.scenarios.TestScenario;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.finos.legend.engine.persistence.components.util.Capability;
import org.finos.legend.engine.persistence.components.util.SchemaEvolutionCapability;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SchemaEvolutionTest extends IngestModeTest
{
    static class TestSink extends AnsiSqlSink
    {
        TestSink()
        {
            super(
                    new HashSet<>(Arrays.asList(
                            Capability.ADD_COLUMN,
                            Capability.DATA_TYPE_LENGTH_CHANGE,
                            Capability.DATA_TYPE_SCALE_CHANGE,
                            Capability.IMPLICIT_DATA_TYPE_CONVERSION,
                            Capability.EXPLICIT_DATA_TYPE_CONVERSION)),
                    Collections.singletonMap(DataType.DOUBLE, EnumSet.of(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.FLOAT, DataType.REAL)),
                    Collections.singletonMap(DataType.FLOAT, EnumSet.of(DataType.DOUBLE)),
                    new AnsiDatatypeToDefaultSizeMapping(),
                    SqlGenUtils.QUOTE_IDENTIFIER,
                    AnsiSqlSink.LOGICAL_PLAN_VISITOR_BY_CLASS,
                    (x, y, z) ->
                    {
                        throw new UnsupportedOperationException();
                    },
                    (x, y, z) ->
                    {
                        throw new UnsupportedOperationException();
                    },
                    (x, y, z) ->
                    {
                        throw new UnsupportedOperationException();
                    });
        }
    }

    private RelationalSink relationalSink = new TestSink();

    // Column missing in main table and add_column capability allowed
    // Add column
    // biz_date DATE
    @Test
    void testSnapshotMilestoningWithAddColumnAndUserProvidedSchemaEvolutionCapability()
    {
        RelationalTransformer transformer = new RelationalTransformer(relationalSink, TransformOptions.builder().build());

        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableShortenedSchema)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(baseTableSchema)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, false);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        // Use the planner utils to return the sql
        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(expectedSchemaEvolutionAddColumn, sqlsForSchemaEvolution.get(0));
    }

    // Column missing in main table and add_column capability allowed (ignore case)
    // Add column
    // biz_date DATE
    @Test
    void testSnapshotMilestoningWithAddColumnAndIgnoreCase()
    {
        RelationalTransformer transformer = new RelationalTransformer(relationalSink, TransformOptions.builder().build());

        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableShortenedSchema)
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
        Assertions.assertEquals(expectedSchemaEvolutionAddColumn, sqlsForSchemaEvolution.get(0));
    }

    // Column missing in main table and add_column capability allowed with upper case optimizer enabled
    // Add column
    // BIZ_DATE DATE
    @Test
    void testSnapshotMilestoningWithAddColumnEvolutionUpperCase()
    {
        RelationalTransformer transformer = new RelationalTransformer(relationalSink, TransformOptions.builder().addOptimizers(new UpperCaseOptimizer()).build());

        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableShortenedSchema)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(baseTableSchema)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, false);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        // Use the planner utils to return the sql
        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(expectedSchemaEvolutionAddColumnWithUpperCase, sqlsForSchemaEvolution.get(0));
    }

    // Column missing in main table but add_column capability not allowed --> throws exception
    // Add column
    // biz_date DATE
    @Test
    void testSnapshotMilestoningWithAddColumnWithoutUserProvidedSchemaEvolutionCapability()
    {
        RelationalTransformer transformer = new RelationalTransformer(relationalSink, TransformOptions.builder().addOptimizers(new UpperCaseOptimizer()).build());

        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableShortenedSchema)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(baseTableSchema)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, Collections.emptySet(), false);
        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
            Assertions.fail("Exception was not thrown");
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Field \"biz_date\" in staging dataset does not exist in main dataset. Couldn't add column since sink/user capabilities do not permit operation.", e.getMessage());
        }
    }

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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, true);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(0, sqlsForSchemaEvolution.size());
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

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(0, sqlsForSchemaEvolution.size());
    }

    // Data sizing (length) changes decrement --> throws exception
    // Alter column
    // DESCRIPTION: VARCHAR(1000) -> VARCHAR(64)
    @Test
    void testSnapshotMilestoningWithColumnLengthChangeEvolutionAndUserProvidedSchemaEvolutionCapability()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchemaWithLongDescription)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableEvolvedLength)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_LENGTH_CHANGE);
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_SCALE_CHANGE);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, true);

        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
            Assertions.fail("Exception was not thrown");
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Data type size is decremented from \"1000\" to \"64\" for column \"description\"", e.getMessage());
        }
    }

    // Data sizing change (scale) in main table column and data_type_size_change capability allowed
    // Alter column
    // decimal_col: DECIMAL(10, 0) -> DECIMAL(10, 2)
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
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_SCALE_CHANGE);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, false);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(expectedSchemaEvolutionModifyScale, sqlsForSchemaEvolution.get(0));
    }

    //Data sizing (scale) changes but user capability doesn't allow it --> throws exception
    // Alter column
    // decimal_col: DECIMAL(10, 0) -> DECIMAL(10, 2)
    @Test
    void testSnapshotMilestoningWithColumnScaleChangeEvolutionAndUserProvidedSchemaEvolutionCapability()
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
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, false);

        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
            Assertions.fail("Exception was not thrown");
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Data type scale changes couldn't be performed on column \"decimal_col\" since sink/user capability does not allow it", e.getMessage());
        }
    }

    // Implicit data type conversion is automatically handled by DB. No additional alter statement generated
    // Alter column
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

    // Nullability change required in main table column and column_nullability_changed capability allowed
    // Alter column
    // amount: DOUBLE NOT NULL -> FLOAT
    // biz_date: DATE NOT NULL -> DATE
    @Test
    void testSnapshotMilestoningWithImplicitDataTypeEvolutionAndAlterNullability()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchemaWithNonNullableColumn)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableImplicitDatatypeChange)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.COLUMN_NULLABILITY_CHANGE);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, true);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(2, sqlsForSchemaEvolution.size());
        Assertions.assertEquals(expectedSchemaImplicitNullabilityChange, sqlsForSchemaEvolution.get(0));
        Assertions.assertEquals(expectedSchemaNullabilityChange, sqlsForSchemaEvolution.get(1));
    }

    // Nullability change required in main table column and column_nullability_changed capability not allowed --> throws exception
    // Alter column
    // amount: DOUBLE NOT NULL -> DOUBLE
    // biz_date: DATE NOT NULL -> DATE
    @Test
    void testSnapshotMilestoningWithAlterNullabilityWithoutUserCapability()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchemaWithNonNullableColumn)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableNullableChange)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, false);
        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
            Assertions.fail("Exception was not thrown");
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Column \"amount\" couldn't be made nullable since user capability does not allow it", e.getMessage());
        }
    }

    // Nullability change required in main table column and column_nullability_changed capability allowed
    // Alter column
    // amount: DOUBLE NOT NULL -> DOUBLE
    // biz_date: DATE NOT NULL -> DATE
    @Test
    void testSnapshotMilestoningWithAlterNullabilityAndUserCapability()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchemaWithNonNullableColumn)
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
        Assertions.assertEquals(expectedSchemaImplicitNullabilityChange, sqlsForSchemaEvolution.get(0));
        Assertions.assertEquals(expectedSchemaNullabilityChange, sqlsForSchemaEvolution.get(1));
    }


    // Data type change required in main table column (float --> double) and data_type_conversion capability allowed
    // Alter column
    // Amount: FLOAT -> DOUBLE
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

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(expectedSchemaNonBreakingChangeWithAmount, sqlsForSchemaEvolution.get(0));
    }

    // Data type change required in main table column (float --> double) and data_type_conversion capability not allowed--> throws exception
    // Alter column
    // Amount: FLOAT -> DOUBLE
    @Test
    void testSnapshotMilestoningWithNonBreakingDataTypeEvolutionAndUserProvidedSchemaEvolutionCapability()
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
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, true);

        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
            Assertions.fail("Exception was not thrown");
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Explicit data type conversion from \"FLOAT\" to \"DOUBLE\" couldn't be performed since sink/user capability does not allow it", e.getMessage());
        }
    }

    // Data type & sizing change required in main table column (float --> double(8))
    // and data_type_conversion capability allowed but sizing not allowed --> throws exception
    // Alter column
    // Amount: FLOAT -> DOUBLE(8)
    @Test
    void testSnapshotMilestoningWithNonBreakingDataTypeEvolutionAndSizingChange()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableExplicitDatatypeChangeSchema)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableNonBreakingDatatypeAndSizingChange)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_CONVERSION);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, true);

        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
            Assertions.fail("Exception was not thrown");
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Data type length changes couldn't be performed on column \"Amount\" since sink/user capability does not allow it", e.getMessage());
        }
    }

    // Data type & sizing change required in main table column (float --> double(8))
    // and data_type_conversion capability and sizing  allowed
    // Alter column
    // Amount: FLOAT -> DOUBLE(8)
    @Test
    void testSnapshotMilestoningWithNonBreakingDataTypeEvolutionAndSizingChangeAllowed()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableExplicitDatatypeChangeSchema)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableNonBreakingDatatypeAndSizingChange)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_CONVERSION);
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_LENGTH_CHANGE);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, true);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(expectedSchemaNonBreakingChangeWithSizing, sqlsForSchemaEvolution.get(0));
    }

    // Breaking data type change from DOUBLE --> VARCHAR. Throws exception
    // Alter column
    // amount: DOUBLE -> VARCHAR(32)
    @Test
    void testSnapshotMilestoningWithBreakingDataTypeEvolution()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchema)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableBreakingDatatypeChange)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_CONVERSION);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, false);

        try
        {
            schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
            Assertions.fail("Exception was not thrown");
        }
        catch (IncompatibleSchemaChangeException e)
        {
            assertEquals("Breaking schema change from datatype \"DOUBLE\" to \"VARCHAR\"", e.getMessage());
        }
    }

    // Nullability change required in main table column since column missing in staging table and column_nullability_changed capability allowed
    // Missing column
    // biz_date
    @Test
    void testSnapshotMilestoningWithColumnMissingInStagingTableAndUserCapability()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchemaWithSingleNonNullableColumn)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableShortenedSchema)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.MARK_MISSING_COLUMN_AS_NULLABLE);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, false);
        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(1, sqlsForSchemaEvolution.size());
        Assertions.assertEquals(expectedSchemaNullabilityChange, sqlsForSchemaEvolution.get(0));
    }

    // Nullability change required in main table column since column missing in staging table
    // and column_nullability_changed capability not allowed --> throws exception
    // Missing column
    // biz_date
    @Test
    void testSnapshotMilestoningWithColumnMissingInStagingTableWithoutUserCapability()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchemaWithSingleNonNullableColumn)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableShortenedSchema)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, false);
        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
            Assertions.fail("Exception was not thrown");
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Column \"biz_date\" is missing from incoming schema, but user capability does not allow marking it to nullable", e.getMessage());
        }
    }

    //Column missing in staging table is already nullable column in main table --> no change require
    // Missing column
    // biz_date
    @Test
    void testSnapshotMilestoningWithNullableColumnMissingInStagingTable()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchema)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableShortenedSchema)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, false);
        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());
        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
        Assertions.assertEquals(0, sqlsForSchemaEvolution.size());
    }


    // No change
    @Test
    void testBitemporalDeltaSourceSpeciesBothFieldsSchemaEvolution()
    {
        RelationalTransformer transformer = new RelationalTransformer(relationalSink, TransformOptions.builder().build());

        BitemporalDeltaSourceSpecifiesFromAndThroughScenarios scenarios = new BitemporalDeltaSourceSpecifiesFromAndThroughScenarios();
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS();

        Dataset mainTable = scenario.getMainTable();
        Dataset stagingTable = scenario.getStagingTable();
        IngestMode ingestMode = scenario.getIngestMode();

        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, false);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        // Use the planner utils to return the sql
        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
        Assertions.assertTrue(sqlsForSchemaEvolution.isEmpty());
    }

    // No change
    @Test
    void testBitemporalDeltaSourceSpeciesFromOnlyFieldsSchemaEvolution()
    {
        RelationalTransformer transformer = new RelationalTransformer(relationalSink, TransformOptions.builder().build());

        BitemporalDeltaSourceSpecifiesFromOnlyScenarios scenarios = new BitemporalDeltaSourceSpecifiesFromOnlyScenarios();
        TestScenario scenario = scenarios.BATCH_ID_BASED__NO_DEL_IND__NO_DATA_SPLITS();

        Dataset mainTable = scenario.getDatasets().mainDataset();
        Dataset stagingTable = scenario.getDatasets().stagingDataset();
        IngestMode ingestMode = scenario.getIngestMode();

        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet, false);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable.schema());
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        // Use the planner utils to return the sql
        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
        Assertions.assertTrue(sqlsForSchemaEvolution.isEmpty());
    }
}
