// Copyright 2025 Goldman Sachs
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
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.values.DistinctFunction;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.FunctionName;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.snowflake.SnowflakeSink;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.finos.legend.engine.persistence.components.BaseTestUtils.schemaWithAllColumns;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CountDistinctTest
{

    @Test
    public void testSingleValue()
    {
        DatasetDefinition dataset = DatasetDefinition.builder().database("CITIBIKE").name("trips").group("public").schema(schemaWithAllColumns).build();

        List<FieldValue> fields = new ArrayList<>();
        FieldValue field1 = FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName("field1").build();
        fields.add(field1);

        FunctionImpl countDistinct = FunctionImpl.builder()
            .functionName(FunctionName.COUNT)
            .addValue(DistinctFunction.builder().addAllValues(fields).build())
            .alias("distinctRowCount")
            .build();

        Selection selection = Selection.builder()
            .source(dataset)
            .addFields(countDistinct)
            .build();

        RelationalTransformer transformer = new RelationalTransformer(SnowflakeSink.get());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(LogicalPlan.builder().addOps(selection).build());

        List<String> list = physicalPlan.getSqlList();
        String expected = "SELECT COUNT(DISTINCT trips.\"field1\") as \"distinctRowCount\" FROM \"CITIBIKE\".\"public\".\"trips\" as trips";
        assertEquals(1, list.size());
        assertEquals(expected, list.get(0));
    }

    @Test
    public void testMultipleValues()
    {
        DatasetDefinition dataset = DatasetDefinition.builder().database("CITIBIKE").name("trips").group("public").schema(schemaWithAllColumns).build();

        List<FieldValue> fields = new ArrayList<>();
        FieldValue field1 = FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName("field1").build();
        FieldValue field2 = FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName("field2").build();
        FieldValue field3 = FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName("field3").build();
        FieldValue field4 = FieldValue.builder().datasetRef(dataset.datasetReference()).fieldName("field4").build();
        fields.add(field1);
        fields.add(field2);
        fields.add(field3);
        fields.add(field4);

        FunctionImpl countDistinct = FunctionImpl.builder()
            .functionName(FunctionName.COUNT)
            .addValue(DistinctFunction.builder().addAllValues(fields).build())
            .alias("distinctRowCount")
            .build();

        Selection selection = Selection.builder()
            .source(dataset)
            .addFields(countDistinct)
            .build();

        RelationalTransformer transformer = new RelationalTransformer(SnowflakeSink.get());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(LogicalPlan.builder().addOps(selection).build());

        List<String> list = physicalPlan.getSqlList();
        String expected = "SELECT COUNT(DISTINCT trips.\"field1\",trips.\"field2\",trips.\"field3\",trips.\"field4\") as \"distinctRowCount\" FROM \"CITIBIKE\".\"public\".\"trips\" as trips";
        assertEquals(1, list.size());
        assertEquals(expected, list.get(0));
    }
}
