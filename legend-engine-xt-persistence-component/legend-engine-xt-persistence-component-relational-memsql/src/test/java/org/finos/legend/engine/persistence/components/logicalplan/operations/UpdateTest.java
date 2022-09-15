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
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Condition;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Equals;
import org.finos.legend.engine.persistence.components.logicalplan.conditions.Exists;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReference;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetReferenceImpl;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Selection;
import org.finos.legend.engine.persistence.components.logicalplan.values.All;
import org.finos.legend.engine.persistence.components.logicalplan.values.DiffBinaryValueOperator;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.NumericalValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.Pair;
import org.finos.legend.engine.persistence.components.logicalplan.values.SelectValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.StringValue;
import org.finos.legend.engine.persistence.components.physicalplan.PhysicalPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.optimizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.memsql.MemSqlSink;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class UpdateTest
{

    @Test
    void updateTest()
    {
        DatasetReference dataset = DatasetReferenceImpl.builder().database("mydb").name("mytable").build();

        Operation op = Update.builder()
            .dataset(dataset)
            .whereCondition(Equals.of(FieldValue.builder().datasetRef(dataset).fieldName("id").build(), StringValue.of("123")))
            .addKeyValuePairs(Pair.of(FieldValue.builder().datasetRef(dataset).fieldName("name").build(), StringValue.of("Matteo")))
            .addKeyValuePairs(Pair.of(
                FieldValue.builder().datasetRef(dataset).fieldName("batch_id").build(),
                DiffBinaryValueOperator.of(
                    FieldValue.builder().datasetRef(dataset).fieldName("batch_id").build(),
                    NumericalValue.of(1L))))
            .build();

        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(op).build();
        RelationalTransformer transformer = new RelationalTransformer(MemSqlSink.get());
        PhysicalPlan<SqlGen> physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        StringBuilder sb = new StringBuilder();
        for (SqlGen token : physicalPlan.ops())
        {
            token.genSql(sb);
        }

        String expected = "UPDATE `mydb`.`mytable` SET `name` = 'Matteo',`batch_id` = `batch_id`-1 WHERE `id` = '123'";

        assertEquals(expected, sb.toString());
    }

    @Test
    void updateWithJoinTest()
    {
        DatasetReference dataset = DatasetReferenceImpl.builder().database("mydb").name("tableA").alias("A").build();
        DatasetReference joinDataset = DatasetReferenceImpl.builder().database("mydb").name("tableB").alias("B").build();

        Operation op = Update.builder()
            .dataset(dataset)
            .joinDataset(joinDataset)
            .joinCondition(Equals.of(
                FieldValue.builder().datasetRef(dataset).fieldName("pk").build(),
                FieldValue.builder().datasetRef(joinDataset).fieldName("pk").build()))
            .addKeyValuePairs(Pair.of(
                FieldValue.builder().datasetRef(dataset).fieldName("col1").build(),
                FieldValue.builder().datasetRef(joinDataset).fieldName("col1").build()))
            .addKeyValuePairs(Pair.of(
                FieldValue.builder().datasetRef(dataset).fieldName("col2").build(),
                FieldValue.builder().datasetRef(joinDataset).fieldName("col2").build()))
            .build();

        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(op).build();
        RelationalTransformer transformer = new RelationalTransformer(MemSqlSink.get());
        PhysicalPlan<SqlGen> physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        StringBuilder sb = new StringBuilder();
        for (SqlGen token : physicalPlan.ops())
        {
            token.genSql(sb);
        }

        String expected = "UPDATE `mydb`.`tableA` as A " +
            "INNER JOIN `mydb`.`tableB` as B " +
            "ON A.`pk` = B.`pk` " +
            "SET A.`col1` = B.`col1`," +
            "A.`col2` = B.`col2`";

        assertEquals(expected, sb.toString());
    }

    @Test
    void updateWithJoinAndWhere()
    {
        DatasetReference dataset = DatasetReferenceImpl.builder().database("mydb").name("tableA").alias("A").build();
        DatasetReference joinDataset = DatasetReferenceImpl.builder().database("mydb").name("tableB").alias("B").build();

        Operation op = Update.builder()
            .dataset(dataset)
            .joinDataset(joinDataset)
            .joinCondition(Equals.of(
                FieldValue.builder().datasetRef(dataset).fieldName("pk").build(),
                FieldValue.builder().datasetRef(joinDataset).fieldName("pk").build()))
            .whereCondition(Equals.of(
                FieldValue.builder().datasetRef(dataset).fieldName("status").build(),
                NumericalValue.of(999L)))
            .addKeyValuePairs(Pair.of(
                FieldValue.builder().datasetRef(dataset).fieldName("col1").build(),
                FieldValue.builder().datasetRef(joinDataset).fieldName("col1").build()))
            .addKeyValuePairs(Pair.of(
                FieldValue.builder().datasetRef(dataset).fieldName("col2").build(),
                FieldValue.builder().datasetRef(joinDataset).fieldName("col2").build()))
            .build();

        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(op).build();
        RelationalTransformer transformer = new RelationalTransformer(MemSqlSink.get());
        PhysicalPlan<SqlGen> physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        StringBuilder sb = new StringBuilder();
        for (SqlGen token : physicalPlan.ops())
        {
            token.genSql(sb);
        }

        String expected = "UPDATE `mydb`.`tableA` as A " +
            "INNER JOIN `mydb`.`tableB` as B " +
            "ON A.`pk` = B.`pk` " +
            "SET A.`col1` = B.`col1`," +
            "A.`col2` = B.`col2` " +
            "WHERE A.`status` = 999";

        assertEquals(expected, sb.toString());
    }

    @Test
    void updateWithImplicitJoinTest()
    {
        DatasetReference dataset = DatasetReferenceImpl.builder().database("mydb").name("tableA").alias("A").build();
        DatasetReference joinDataset = DatasetReferenceImpl.builder().database("mydb").name("tableB").alias("B").build();

        Condition pkMatchCondition = Equals.of(
            FieldValue.builder().datasetRef(dataset).fieldName("pk").build(),
            FieldValue.builder().datasetRef(joinDataset).fieldName("pk").build());

        Condition whereCondition = Exists.of(
            Selection.builder().source(joinDataset).condition(pkMatchCondition).addFields(All.INSTANCE).build());

        Operation op = Update.builder()
            .dataset(dataset)
            .whereCondition(whereCondition)
            .addKeyValuePairs(Pair.of(
                FieldValue.builder().datasetRef(dataset).fieldName("col1").build(),
                SelectValue.of(Selection.builder().source(joinDataset).condition(pkMatchCondition).addFields(FieldValue.builder().datasetRef(joinDataset).fieldName("col1").build()).build())))
            .addKeyValuePairs(Pair.of(
                FieldValue.builder().datasetRef(dataset).fieldName("col2").build(),
                SelectValue.of(Selection.builder().source(joinDataset).condition(pkMatchCondition).addFields(FieldValue.builder().datasetRef(joinDataset).fieldName("col2").build()).build())))
            .build();

        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(op).build();
        RelationalTransformer transformer = new RelationalTransformer(MemSqlSink.get());
        PhysicalPlan<SqlGen> physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        StringBuilder sb = new StringBuilder();
        for (SqlGen token : physicalPlan.ops())
        {
            token.genSql(sb);
        }

        String expected = "UPDATE `mydb`.`tableA` as A " +
            "SET A.`col1` = (SELECT B.`col1` FROM `mydb`.`tableB` as B WHERE A.`pk` = B.`pk`)," +
            "A.`col2` = (SELECT B.`col2` FROM `mydb`.`tableB` as B WHERE A.`pk` = B.`pk`) " +
            "WHERE EXISTS (SELECT * FROM `mydb`.`tableB` as B WHERE A.`pk` = B.`pk`)";

        assertEquals(expected, sb.toString());
    }

    @Test
    void updateWithImplicitJoinTestWithUpperCase()
    {
        DatasetReference dataset = DatasetReferenceImpl.builder().database("mydb").name("tableA").alias("A").build();
        DatasetReference joinDataset = DatasetReferenceImpl.builder().database("mydb").name("tableB").alias("B").build();

        Condition pkMatchCondition = Equals.of(
            FieldValue.builder().datasetRef(dataset).fieldName("pk").build(),
            FieldValue.builder().datasetRef(joinDataset).fieldName("pk").build());

        Condition whereCondition = Exists.of(
            Selection.builder().source(joinDataset).condition(pkMatchCondition).addFields(All.INSTANCE).build());

        Operation op = Update.builder()
            .dataset(dataset)
            .whereCondition(whereCondition)
            .addKeyValuePairs(Pair.of(
                FieldValue.builder().datasetRef(dataset).fieldName("col1").build(),
                SelectValue.of(Selection.builder().source(joinDataset).condition(pkMatchCondition).addFields(FieldValue.builder().datasetRef(joinDataset).fieldName("col1").build()).build())))
            .addKeyValuePairs(Pair.of(
                FieldValue.builder().datasetRef(dataset).fieldName("col2").build(),
                SelectValue.of(Selection.builder().source(joinDataset).condition(pkMatchCondition).addFields(FieldValue.builder().datasetRef(joinDataset).fieldName("col2").build()).build())))
            .build();

        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(op).build();
        RelationalTransformer transformer = new RelationalTransformer(MemSqlSink.get(), TransformOptions.builder().addOptimizers(new UpperCaseOptimizer()).build());
        PhysicalPlan<SqlGen> physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        StringBuilder sb = new StringBuilder();
        for (SqlGen token : physicalPlan.ops())
        {
            token.genSql(sb);
        }

        String expected = "UPDATE `MYDB`.`TABLEA` as A " +
            "SET A.`COL1` = (SELECT B.`COL1` FROM `MYDB`.`TABLEB` as B WHERE A.`PK` = B.`PK`)," +
            "A.`COL2` = (SELECT B.`COL2` FROM `MYDB`.`TABLEB` as B WHERE A.`PK` = B.`PK`) " +
            "WHERE EXISTS (SELECT * FROM `MYDB`.`TABLEB` as B WHERE A.`PK` = B.`PK`)";

        assertEquals(expected, sb.toString());
    }
}
