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
import org.finos.legend.engine.persistence.components.relational.snowflake.SnowflakeSink;
import org.finos.legend.engine.persistence.components.relational.snowflake.optmizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.finos.legend.engine.persistence.components.BaseTestUtils.schemaWithAllColumns;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ShowTest
{

    @Test
    public void testShowCommand()
    {
        DatasetDefinition dataset = DatasetDefinition.builder().database("CITIBIKE").name("trips").group("public").schema(schemaWithAllColumns).build();
        RelationalTransformer transformer = new RelationalTransformer(SnowflakeSink.get());
        LogicalPlan logicalPlan = LogicalPlanFactory.getLogicalPlanForDoesDatasetExist(dataset);
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expected = "SHOW TABLES LIKE 'trips' IN CITIBIKE.public";
        assertEquals(1, list.size());
        assertEquals(expected, list.get(0));
    }

    @Test
    public void testShowCommandWithUpperCase()
    {
        DatasetDefinition dataset = DatasetDefinition.builder().database("CITIBIKE").name("trips").group("public").schema(schemaWithAllColumns).build();

        RelationalTransformer transformer = new RelationalTransformer(
            SnowflakeSink.get(),
            TransformOptions.builder().addOptimizers(new UpperCaseOptimizer()).build());

        LogicalPlan logicalPlan = LogicalPlanFactory.getLogicalPlanForDoesDatasetExist(dataset);
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expected = "SHOW TABLES LIKE 'TRIPS' IN CITIBIKE.PUBLIC";
        assertEquals(1, list.size());
        assertEquals(expected, list.get(0));
    }

    @Test
    public void testShowCommandWithoutSchema()
    {
        DatasetDefinition dataset = DatasetDefinition.builder().database("CITIBIKE").name("trips").schema(schemaWithAllColumns).build();
        RelationalTransformer transformer = new RelationalTransformer(SnowflakeSink.get());
        LogicalPlan logicalPlan = LogicalPlanFactory.getLogicalPlanForDoesDatasetExist(dataset);
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expected = "SHOW TABLES LIKE 'trips'";
        assertEquals(1, list.size());
        assertEquals(expected, list.get(0));
    }

    @Test
    public void testShowCommandWithSchemaWithoutDb()
    {
        DatasetDefinition dataset = DatasetDefinition.builder().name("trips").group("public").schema(schemaWithAllColumns).build();
        RelationalTransformer transformer = new RelationalTransformer(SnowflakeSink.get());
        LogicalPlan logicalPlan = LogicalPlanFactory.getLogicalPlanForDoesDatasetExist(dataset);
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expected = "SHOW TABLES LIKE 'trips' IN public";
        assertEquals(1, list.size());
        assertEquals(expected, list.get(0));
    }
}
