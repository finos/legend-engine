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
                    Capability.DATA_SIZING_CHANGES,
                    Capability.IMPLICIT_DATA_TYPE_CONVERSION,
                    Capability.EXPLICIT_DATA_TYPE_CONVERSION)),
                Collections.singletonMap(DataType.DOUBLE, EnumSet.of(DataType.TINYINT, DataType.SMALLINT, DataType.INTEGER, DataType.INT, DataType.FLOAT, DataType.REAL)),
                Collections.singletonMap(DataType.INT, EnumSet.of(DataType.TINYINT)),
                SqlGenUtils.QUOTE_IDENTIFIER,
                AnsiSqlSink.LOGICAL_PLAN_VISITOR_BY_CLASS,
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

    @Test
    void testSnapshotMilestoningWithAddColumnEvolution()
    {
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

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        // Use the planner utils to return the sql
        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(expectedSchemaEvolutionAddColumn, sqlsForSchemaEvolution.get(0));
    }

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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode,Collections.emptySet());

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        // Use the planner utils to return the sql
        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(expectedSchemaEvolutionAddColumnWithUpperCase, sqlsForSchemaEvolution.get(0));
    }

    //Add column is required but user capability doesn't allow it --> throws exception
    @Test
    void testSnapshotMilestoningWithAddColumnAndUserProvidedSchemaEvolutionCapability()
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
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.IMPLICIT_DATA_TYPE_CONVERSION);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode,schemaEvolutionCapabilitySet);
        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
            SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

            // Use the planner utils to return the sql
            List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();
        }
        catch (IllegalStateException e)
        {
            Assertions.assertEquals(e.getMessage(), "Field \"biz_date\" in staging dataset does not exist in main dataset. Couldn't add column since sink/user capabilities do not permit operation.");
        }
    }

    @Test
    void testSnapshotMilestoningWithUserProvidedSchemaEvolutionCapability()
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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode,schemaEvolutionCapabilitySet);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        // Use the planner utils to return the sql
        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(expectedSchemaEvolutionAddColumnWithUpperCase, sqlsForSchemaEvolution.get(0));
    }


    @Test
    void testSnapshotMilestoningWithSizeChangeEvolution()
    {
        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchema)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(stagingTableEvolvedSize)
            .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode,Collections.emptySet());

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(expectedSchemaEvolutionModifySize, sqlsForSchemaEvolution.get(0));
    }

    @Test
    void testSnapshotMilestoningWithSizeChangeEvolutionWithUpperCase()
    {
        RelationalTransformer transformer = new RelationalTransformer(relationalSink, TransformOptions.builder().addOptimizers(new UpperCaseOptimizer()).build());

        Dataset mainTable = DatasetDefinition.builder()
            .database(mainDbName).name(mainTableName).alias(mainTableAlias)
            .schema(baseTableSchema)
            .build();

        Dataset stagingTable = DatasetDefinition.builder()
            .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
            .schema(stagingTableEvolvedSize)
            .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode,Collections.emptySet());

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(expectedSchemaEvolutionModifySizeWithUpperCase, sqlsForSchemaEvolution.get(0));
    }

    //Data sizing changes but user capability doesn't allow it --> throws exception
    @Test
    void testSnapshotMilestoningWithSizeChangeEvolutionAndUserProvidedSchemaEvolutionCapability()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchema)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableEvolvedSize)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode,schemaEvolutionCapabilitySet);

        try
        {
            SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
            RelationalTransformer transformer = new RelationalTransformer(relationalSink);
            SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

            List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals(e.getMessage(), "Data sizing changes couldn't be performed since user capability does not allow it");
        }
    }

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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode,Collections.emptySet());

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(0, sqlsForSchemaEvolution.size());
    }

    //Implicit data type conversion but user capability doesn't allow it --> no exception is thrown
    @Test
    void testSnapshotMilestoningWithImplicitDataTypeEvolutionAndUserProvidedSchemaEvolutionCapability()
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
        Set<SchemaEvolutionCapability> schemaEvolutionCapabilitySet = new HashSet<>();
        schemaEvolutionCapabilitySet.add(SchemaEvolutionCapability.ADD_COLUMN);
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode, schemaEvolutionCapabilitySet);

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(0, sqlsForSchemaEvolution.size());
    }

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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode,Collections.emptySet());

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(2, sqlsForSchemaEvolution.size());
        Assertions.assertEquals(expectedSchemaImplicitNullabilityChange, sqlsForSchemaEvolution.get(0));
        Assertions.assertEquals(expectedSchemaNullabilityChange, sqlsForSchemaEvolution.get(1));
    }


    //int --> tinyInt
    @Test
    void testSnapshotMilestoningWithNonBreakingDataTypeEvolution()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchema)
                .build();

        Dataset stagingTable = DatasetDefinition.builder()
                .database(stagingDbName).name(stagingTableName).alias(stagingTableAlias)
                .schema(stagingTableNonBreakingDatatypeChange)
                .build();

        NontemporalSnapshot ingestMode = NontemporalSnapshot.builder().auditing(NoAuditing.builder().build()).build();
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode,Collections.emptySet());

        SchemaEvolutionResult result = schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
        RelationalTransformer transformer = new RelationalTransformer(relationalSink);
        SqlPlan physicalPlanForSchemaEvolution = transformer.generatePhysicalPlan(result.logicalPlan());

        List<String> sqlsForSchemaEvolution = physicalPlanForSchemaEvolution.getSqlList();

        Assertions.assertEquals(expectedSchemaNonBreakingChange, sqlsForSchemaEvolution.get(0));
    }

    //Explicit data type conversion but user capability doesn't allow it --> throws exception
    @Test
    void testSnapshotMilestoningWithNonBreakingDataTypeEvolutionAndUserProvidedSchemaEvolutionCapability()
    {
        Dataset mainTable = DatasetDefinition.builder()
                .database(mainDbName).name(mainTableName).alias(mainTableAlias)
                .schema(baseTableSchema)
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
        }
        catch (IncompatibleSchemaChangeException e)
        {
            Assertions.assertEquals(e.getMessage(), "Explicit data type conversion from \"INT\" to \"TINYINT\" couldn't be performed since user capability does not allow it");
        }
    }

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
        SchemaEvolution schemaEvolution = new SchemaEvolution(relationalSink, ingestMode,Collections.emptySet());

        try
        {
            schemaEvolution.buildLogicalPlanForSchemaEvolution(mainTable, stagingTable);
            Assertions.fail("Exception was not thrown");
        }
        catch (IncompatibleSchemaChangeException e)
        {
            assertEquals("Breaking schema change from datatype INT to VARCHAR", e.getMessage());
        }
    }
}
