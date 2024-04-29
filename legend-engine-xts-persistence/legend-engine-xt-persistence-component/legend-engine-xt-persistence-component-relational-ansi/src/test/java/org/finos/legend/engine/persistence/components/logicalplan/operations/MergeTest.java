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
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReferenceImpl;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Pair;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.ansi.optimizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MergeTest
{

    @Test
    void updateTest()
    {
        DatasetReference dataset = DatasetReferenceImpl.builder().database("mydb").name("mytable").build();
        DatasetReference usingDataset = DatasetReferenceImpl.builder().database("mydb").name("usingTable").build();
        Operation op = Merge.builder()
            .dataset(dataset)
            .usingDataset(usingDataset)
            .onCondition(Equals.of(FieldValue.builder().datasetRef(dataset).fieldName("id").build(), StringValue.of("123")))
            .matchedCondition(Equals.of(FieldValue.builder().datasetRef(dataset).fieldName("id").build(), StringValue.of("456")))
            .addMatchedKeyValuePairs(Pair.of(FieldValue.builder().datasetRef(dataset).fieldName("id").build(), StringValue.of("12345")))
            .addUnmatchedKeyValuePairs(Pair.of(FieldValue.builder().datasetRef(dataset).fieldName("name").build(), StringValue.of("Chloe")))
            .build();

        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get());
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(op).build();
        PhysicalPlan<SqlGen> physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        StringBuilder sb = new StringBuilder();
        for (SqlGen token : physicalPlan.ops())
        {
            token.genSql(sb);
        }

        String expected = "MERGE INTO \"mydb\".\"mytable\" " +
            "USING \"mydb\".\"usingTable\" " +
            "ON \"id\" = '123' " +
            "WHEN MATCHED AND \"id\" = '456' THEN " +
            "UPDATE SET \"id\" = '12345' " +
            "WHEN NOT MATCHED THEN " +
            "INSERT (\"name\") " +
            "VALUES ('Chloe')";

        assertEquals(expected, sb.toString());
    }

    @Test
    void updateTestWithUpperCase()
    {
        DatasetReference dataset = DatasetReferenceImpl.builder().database("mydb").name("mytable").build();
        DatasetReference usingDataset = DatasetReferenceImpl.builder().database("mydb").name("usingTable").build();
        Operation op = Merge.builder()
            .dataset(dataset)
            .usingDataset(usingDataset)
            .onCondition(Equals.of(FieldValue.builder().datasetRef(dataset).fieldName("id").build(), StringValue.of("123")))
            .matchedCondition(Equals.of(FieldValue.builder().datasetRef(dataset).fieldName("id").build(), StringValue.of("456")))
            .addMatchedKeyValuePairs(Pair.of(FieldValue.builder().datasetRef(dataset).fieldName("id").build(), StringValue.of("12345")))
            .addUnmatchedKeyValuePairs(Pair.of(FieldValue.builder().datasetRef(dataset).fieldName("name").build(), StringValue.of("Chloe")))
            .build();

        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(op).build();

        RelationalTransformer transformer = new RelationalTransformer(
            AnsiSqlSink.get(),
            TransformOptions.builder().addOptimizers(new UpperCaseOptimizer()).build());
        PhysicalPlan<SqlGen> physicalPlan = transformer.generatePhysicalPlan(logicalPlan);

        StringBuilder sb = new StringBuilder();
        for (SqlGen token : physicalPlan.ops())
        {
            token.genSql(sb);
        }

        String expected = "MERGE INTO \"MYDB\".\"MYTABLE\" " +
            "USING \"MYDB\".\"USINGTABLE\" " +
            "ON \"ID\" = '123' " +
            "WHEN MATCHED AND \"ID\" = '456' THEN " +
            "UPDATE SET \"ID\" = '12345' " +
            "WHEN NOT MATCHED THEN " +
            "INSERT (\"NAME\") " +
            "VALUES ('Chloe')";

        assertEquals(expected, sb.toString());
    }
}
