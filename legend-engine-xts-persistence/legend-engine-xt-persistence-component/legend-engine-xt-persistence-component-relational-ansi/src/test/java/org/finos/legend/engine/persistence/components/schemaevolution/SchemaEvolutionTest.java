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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        // Use the planner utils to return the sql
        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(expectedSchemaEvolutionAddColumn, sqlsForSchemaEvolution.get(0));
    }

    // Column missing in main table and add_column capability allowed with upper case optimizer enabled
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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        // Use the planner utils to return the sql
        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(expectedSchemaEvolutionAddColumnWithUpperCase, sqlsForSchemaEvolution.get(0));
    }

    // Column missing in main table but add_column capability not allowed --> throws exception
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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, Collections.emptySet());
        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
            SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

            // Use the planner utils to return the sql
            List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
            Assertions.assertEquals(expectedSchemaEvolutionAddColumnWithUpperCase, sqlsForSchemaEvolution.get(0));
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Field \"biz_date\" in staging dataset does not exist in main dataset. Couldn't add column since sink/user capabilities do not permit operation.", e.getMessage());
        }
    }

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
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_SIZE_CHANGE);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
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
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_SIZE_CHANGE);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
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
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);

        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
            RelationalTransformer transformer = new RelationalTransformer(relationalSink);
            SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

            List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
            Assertions.assertEquals(expectedSchemaEvolutionModifySize, sqlsForSchemaEvolution.get(0));

        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Data type length changes couldn't be performed on column \"description\" since user capability does not allow it", e.getMessage());
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
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_SIZE_CHANGE);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(expectedSchemaEvolutionModifyScale, sqlsForSchemaEvolution.get(0));
    }

    //Data sizing (scale) changes but user capability doesn't allow it --> throws exception
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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);

        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
            RelationalTransformer transformer = new RelationalTransformer(relationalSink);
            SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());
            Assertions.fail("Exception was not thrown");
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Data type scale changes couldn't be performed on column \"decimal_col\" since user capability does not allow it", e.getMessage());
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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, Collections.emptySet());

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(0, sqlsForSchemaEvolution.size());
    }

    // Nullability change required in main table column and column_nullability_changed capability allowed
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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(2, sqlsForSchemaEvolution.size());
        Assertions.assertEquals(expectedSchemaImplicitNullabilityChange, sqlsForSchemaEvolution.get(0));
        Assertions.assertEquals(expectedSchemaNullabilityChange, sqlsForSchemaEvolution.get(1));
    }

    // Nullability change required in main table column and column_nullability_changed capability not allowed --> throws exception
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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);
        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
            RelationalTransformer transformer = new RelationalTransformer(relationalSink);
            SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());
            List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
            Assertions.assertEquals(2, sqlsForSchemaEvolution.size());
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Column \"amount\" couldn't be made nullable since user capability does not allow it", e.getMessage());
        }
    }

    // Nullability change required in main table column and column_nullability_changed capability allowed
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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);
        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(2, sqlsForSchemaEvolution.size());
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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(expectedSchemaNonBreakingChange, sqlsForSchemaEvolution.get(0));
    }

    // Data type change required in main table column (float --> double) and data_type_conversion capability not allowed--> throws exception
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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);

        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
            RelationalTransformer transformer = new RelationalTransformer(relationalSink);
            SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

            List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
            Assertions.assertEquals(expectedSchemaNonBreakingChange, sqlsForSchemaEvolution.get(0));
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Explicit data type conversion from \"FLOAT\" to \"DOUBLE\" couldn't be performed since user capability does not allow it", e.getMessage());
        }
    }

    // Data type & sizing change required in main table column (float --> double(8))
    // and data_type_conversion capability allowed but sizing not allowed --> throws exception
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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);

        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
            RelationalTransformer transformer = new RelationalTransformer(relationalSink);
            SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

            List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

            Assertions.assertEquals(expectedSchemaNonBreakingChangeWithSizing, sqlsForSchemaEvolution.get(0));
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Data type length changes couldn't be performed on column \"amount\" since user capability does not allow it", e.getMessage());
        }
    }

    // Data type & sizing change required in main table column (float --> double(8))
    // and data_type_conversion capability and sizing  allowed
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
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.DATA_TYPE_SIZE_CHANGE);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);

        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
            RelationalTransformer transformer = new RelationalTransformer(relationalSink);
            SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

            List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

            Assertions.assertEquals(expectedSchemaNonBreakingChangeWithSizing, sqlsForSchemaEvolution.get(0));
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Data sizing changes couldn't be performed on column \"amount\" since user capability does not allow it", e.getMessage());
        }
    }

    // Breaking data type change from DOUBLE --> VARCHAR. Throws exception
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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, Collections.emptySet());

        try
        {
            schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
            Assertions.fail("Exception was not thrown");
        }
        catch (IncompatibleSchemaChangeException e)
        {
            assertEquals("Breaking schema change from datatype \"DOUBLE\" to \"VARCHAR\"", e.getMessage());
        }
    }

    // Nullability change required in main table column since column missing in staging table and column_nullability_changed capability allowed
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
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.COLUMN_NULLABILITY_CHANGE);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);
        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(1, sqlsForSchemaEvolution.size());
        Assertions.assertEquals(expectedSchemaNullabilityChange, sqlsForSchemaEvolution.get(0));
    }

    // Nullability change required in main table column since column missing in staging table
    // and column_nullability_changed capability not allowed --> throws exception
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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);
        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
            RelationalTransformer transformer = new RelationalTransformer(relationalSink);
            SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());
            List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals("Column \"biz_date\" couldn't be made nullable since user capability does not allow it", e.getMessage());
        }
    }

    //Column missing in staging table is already nullable column in main table --> no change require
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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);
        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());
        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
        Assertions.assertEquals(0, sqlsForSchemaEvolution.size());
    }


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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        // Use the planner utils to return the sql
        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
        Assertions.assertTrue(sqlsForSchemaEvolution.isEmpty());
    }

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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        // Use the planner utils to return the sql
        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
        Assertions.assertTrue(sqlsForSchemaEvolution.isEmpty());
    }
}
