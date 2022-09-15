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

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlanFactory;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.memsql.MemSqlSink;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.schemaWithAllColumns;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShowTest
{

    @Test
    public void testShowTablesCommand()
    {
        DatasetDefinition dataset = DatasetDefinition.builder().database("CITIBIKE").name("trips").group("public").schema(schemaWithAllColumns).build();
        RelationalTransformer transformer = new RelationalTransformer(MemSqlSink.get());
        LogicalPlan logicalPlan = LogicalPlanFactory.getLogicalPlanForDoesDatasetExist(dataset);
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();

        String expected = "SHOW TABLES FROM CITIBIKE.public LIKE 'trips'";
        assertEquals(1, list.size());
        assertEquals(expected, list.get(0));
    }

    @Test
    public void testShowTablesCommandWithUpperCase()
    {
        DatasetDefinition dataset = DatasetDefinition.builder().database("CITIBIKE").name("trips").group("public").schema(schemaWithAllColumns).build();
        RelationalTransformer transformer = new RelationalTransformer(MemSqlSink.get(), TransformOptions.builder().addOptimizers(new org.finos.legend.engine.persistence.components.relational.memsql.optimizer.UpperCaseOptimizer()).build());
        LogicalPlan logicalPlan = LogicalPlanFactory.getLogicalPlanForDoesDatasetExist(dataset);
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();

        String expected = "SHOW TABLES FROM CITIBIKE.PUBLIC LIKE 'TRIPS'";
        assertEquals(1, list.size());
        assertEquals(expected, list.get(0));
    }

    @Test
    public void testShowTablesCommandWithoutSchema()
    {
        DatasetDefinition dataset = DatasetDefinition.builder().database("CITIBIKE").name("trips").schema(schemaWithAllColumns).build();
        RelationalTransformer transformer = new RelationalTransformer(MemSqlSink.get());
        LogicalPlan logicalPlan = LogicalPlanFactory.getLogicalPlanForDoesDatasetExist(dataset);
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();

        String expected = "SHOW TABLES LIKE 'trips'";
        assertEquals(1, list.size());
        assertEquals(expected, list.get(0));
    }

    @Test
    public void testShowTablesCommandWithSchemaWithoutDb()
    {
        DatasetDefinition dataset = DatasetDefinition.builder().name("trips").group("public").schema(schemaWithAllColumns).build();
        RelationalTransformer transformer = new RelationalTransformer(MemSqlSink.get());
        LogicalPlan logicalPlan = LogicalPlanFactory.getLogicalPlanForDoesDatasetExist(dataset);
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();

        String expected = "SHOW TABLES FROM public LIKE 'trips'";
        assertEquals(1, list.size());
        assertEquals(expected, list.get(0));
    }

    @Test
    public void testShowSchemasCommand()
    {
        DatasetDefinition dataset = DatasetDefinition.builder().database("CITIBIKE").name("trips").group("public").schema(schemaWithAllColumns).build();
        RelationalTransformer transformer = new RelationalTransformer(MemSqlSink.get());
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(Show.of(Show.ShowType.SCHEMAS, dataset)).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();

        String expected = "SHOW SCHEMAS";
        assertEquals(1, list.size());
        assertEquals(expected, list.get(0));
    }

    @Test
    public void testShowColumnsCommand()
    {
        DatasetDefinition dataset = DatasetDefinition.builder().database("CITIBIKE").name("trips").group("public").schema(schemaWithAllColumns).build();
        RelationalTransformer transformer = new RelationalTransformer(MemSqlSink.get());
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(Show.of(Show.ShowType.COLUMNS, dataset)).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();

        String expected = "SHOW COLUMNS FROM trips IN CITIBIKE.public";
        assertEquals(1, list.size());
        assertEquals(expected, list.get(0));
    }

    @Test
    public void testShowColumnsCommandWithoutSchema()
    {
        DatasetDefinition dataset = DatasetDefinition.builder().database("CITIBIKE").name("trips").schema(schemaWithAllColumns).build();
        RelationalTransformer transformer = new RelationalTransformer(MemSqlSink.get());
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(Show.of(Show.ShowType.COLUMNS, dataset)).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();

        String expected = "SHOW COLUMNS FROM trips";
        assertEquals(1, list.size());
        assertEquals(expected, list.get(0));
    }

    @Test
    public void testShowColumnsCommandWithSchemaWithoutDb()
    {
        DatasetDefinition dataset = DatasetDefinition.builder().name("trips").group("public").schema(schemaWithAllColumns).build();
        RelationalTransformer transformer = new RelationalTransformer(MemSqlSink.get());
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(Show.of(Show.ShowType.COLUMNS, dataset)).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();

        String expected = "SHOW COLUMNS FROM trips IN public";
        assertEquals(1, list.size());
        assertEquals(expected, list.get(0));
    }
}
