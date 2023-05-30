// Copyright 2023 Goldman Sachs
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
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.bigquery.BigQuerySink;
import org.finos.legend.engine.persistence.components.relational.bigquery.optmizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.finos.legend.engine.persistence.components.BaseTestUtils.*;

public class CreateTableTest
{

    @Test
    public void testCreateTable()
    {
        DatasetDefinition dataset = DatasetDefinition.builder()
            .database("my_db")
            .group("my_schema")
            .name("my_table")
            .alias("my_alias")
            .schema(schemaWithAllColumns)
            .build();
        Operation create = Create.of(true, dataset);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(create).build();
        RelationalTransformer transformer = new RelationalTransformer(BigQuerySink.get());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expected = "CREATE TABLE IF NOT EXISTS `my_db`.`my_schema`.`my_table`(" +
                "`col_int` INTEGER NOT NULL," +
                "`col_integer` INTEGER NOT NULL," +
                "`col_bigint` INTEGER," +
                "`col_tinyint` INTEGER," +
                "`col_smallint` INTEGER," +
                "`col_char` STRING," +
                "`col_varchar` STRING," +
                "`col_string` STRING," +
                "`col_timestamp` TIMESTAMP," +
                "`col_datetime` DATETIME," +
                "`col_date` DATE NOT NULL," +
                "`col_real` FLOAT," +
                "`col_float` FLOAT," +
                "`col_decimal` NUMERIC(10,4)," +
                "`col_double` FLOAT," +
                "`col_binary` BYTES," +
                "`col_time` TIME," +
                "`col_numeric` NUMERIC," +
                "`col_boolean` BOOLEAN," +
                "`col_varbinary` BYTES(10)," +
                "PRIMARY KEY (`col_int`, `col_date`) NOT ENFORCED)";

        Assertions.assertEquals(expected, list.get(0));
    }

    @Test
    public void testCreateTableWithUpperCase()
    {
        DatasetDefinition dataset = DatasetDefinition.builder()
            .database("my_db")
            .group("my_schema")
            .name("my_table")
            .alias("my_alias")
            .schema(schemaWithAllColumns)
            .build();
        Operation create = Create.of(true, dataset);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(create).build();
        RelationalTransformer transformer = new RelationalTransformer(BigQuerySink.get(), TransformOptions.builder().addOptimizers(new UpperCaseOptimizer()).build());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expected = "CREATE TABLE IF NOT EXISTS `MY_DB`.`MY_SCHEMA`.`MY_TABLE`(" +
                "`COL_INT` INTEGER NOT NULL," +
                "`COL_INTEGER` INTEGER NOT NULL," +
                "`COL_BIGINT` INTEGER," +
                "`COL_TINYINT` INTEGER," +
                "`COL_SMALLINT` INTEGER," +
                "`COL_CHAR` STRING," +
                "`COL_VARCHAR` STRING," +
                "`COL_STRING` STRING," +
                "`COL_TIMESTAMP` TIMESTAMP," +
                "`COL_DATETIME` DATETIME," +
                "`COL_DATE` DATE NOT NULL," +
                "`COL_REAL` FLOAT," +
                "`COL_FLOAT` FLOAT," +
                "`COL_DECIMAL` NUMERIC(10,4)," +
                "`COL_DOUBLE` FLOAT," +
                "`COL_BINARY` BYTES," +
                "`COL_TIME` TIME," +
                "`COL_NUMERIC` NUMERIC," +
                "`COL_BOOLEAN` BOOLEAN," +
                "`COL_VARBINARY` BYTES(10)," +
                "PRIMARY KEY (`COL_INT`, `COL_DATE`) NOT ENFORCED)";

        Assertions.assertEquals(expected, list.get(0));
    }

    @Test
    public void testCreateTableWithClusteringKey()
    {
        DatasetDefinition dataset = DatasetDefinition.builder()
            .database("my_db")
            .group("my_schema")
            .name("my_table")
            .alias("my_alias")
            .schema(schemaWithClusteringKey)
            .build();
        Operation create = Create.of(true, dataset);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(create).build();
        RelationalTransformer transformer = new RelationalTransformer(BigQuerySink.get());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expected = "CREATE TABLE IF NOT EXISTS `my_db`.`my_schema`.`my_table`(" +
                "`col_int` INTEGER NOT NULL PRIMARY KEY NOT ENFORCED," +
                "`col_integer` INTEGER NOT NULL," +
                "`col_string` STRING," +
                "`col_timestamp` TIMESTAMP," +
                "`col_double` FLOAT) " +
                "CLUSTER BY `col_timestamp`,`col_int`";

        Assertions.assertEquals(expected, list.get(0));
    }

    @Test
    public void testCreateTableWithClusteringKeyWithUpperCase()
    {
        DatasetDefinition dataset = DatasetDefinition.builder()
            .database("my_db")
            .group("my_schema")
            .name("my_table")
            .alias("my_alias")
            .schema(schemaWithClusteringKey)
            .build();
        Operation create = Create.of(true, dataset);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(create).build();
        RelationalTransformer transformer = new RelationalTransformer(BigQuerySink.get(), TransformOptions.builder().addOptimizers(new UpperCaseOptimizer()).build());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expected = "CREATE TABLE IF NOT EXISTS `MY_DB`.`MY_SCHEMA`.`MY_TABLE`(" +
                "`COL_INT` INTEGER NOT NULL PRIMARY KEY NOT ENFORCED," +
                "`COL_INTEGER` INTEGER NOT NULL," +
                "`COL_STRING` STRING," +
                "`COL_TIMESTAMP` TIMESTAMP," +
                "`COL_DOUBLE` FLOAT) " +
                "CLUSTER BY `COL_TIMESTAMP`,`COL_INT`";

        Assertions.assertEquals(expected, list.get(0));
    }

    @Test
    public void testCreateTableWithPartitionKeyAndClusteringKey()
    {
        DatasetDefinition dataset = DatasetDefinition.builder()
                .database("my_db")
                .group("my_schema")
                .name("my_table")
                .alias("my_alias")
                .schema(schemaWithClusteringAndPartitionKey)
                .build();
        Operation create = Create.of(true, dataset);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(create).build();
        RelationalTransformer transformer = new RelationalTransformer(BigQuerySink.get());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expected = "CREATE TABLE IF NOT EXISTS `my_db`.`my_schema`.`my_table`(" +
                "`col_int` INTEGER NOT NULL," +
                "`col_date` DATE NOT NULL," +
                "`col_integer` INTEGER NOT NULL," +
                "`col_string` STRING," +
                "`col_timestamp` TIMESTAMP," +
                "`col_double` FLOAT," +
                "PRIMARY KEY (`col_int`, `col_date`) NOT ENFORCED) " +
                "PARTITION BY `col_date` " +
                "CLUSTER BY `col_timestamp`,`col_int`";
        Assertions.assertEquals(expected, list.get(0));
    }

    @Test
    public void testCreateTableWithPartitionKey()
    {
        DatasetDefinition dataset = DatasetDefinition.builder()
                .database("my_db")
                .group("my_schema")
                .name("my_table")
                .alias("my_alias")
                .schema(schemaWithPartitionKey)
                .build();
        Operation create = Create.of(true, dataset);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(create).build();
        RelationalTransformer transformer = new RelationalTransformer(BigQuerySink.get());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expected = "CREATE TABLE IF NOT EXISTS `my_db`.`my_schema`.`my_table`(" +
                "`col_int` INTEGER NOT NULL," +
                "`col_date` DATE NOT NULL," +
                "`col_integer` INTEGER NOT NULL," +
                "`col_string` STRING," +
                "`col_timestamp` TIMESTAMP," +
                "`col_double` FLOAT," +
                "PRIMARY KEY (`col_int`, `col_date`) NOT ENFORCED) " +
                "PARTITION BY _PARTITIONDATE";
        Assertions.assertEquals(expected, list.get(0));
    }


}
