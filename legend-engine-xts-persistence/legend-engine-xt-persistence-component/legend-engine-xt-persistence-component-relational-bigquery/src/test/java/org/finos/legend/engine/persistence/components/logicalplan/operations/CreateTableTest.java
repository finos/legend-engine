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
import org.finos.legend.engine.persistence.components.relational.ansi.optimizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.finos.legend.engine.persistence.components.BaseTestUtils.schemaWithAllColumns;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.schemaWithClusteringAndPartitionKey;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.schemaWithClusteringKey;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.schemaWithPartitionKey;

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
                "`col_int` INT64 NOT NULL," +
                "`col_integer` INT64 NOT NULL," +
                "`col_bigint` INT64," +
                "`col_tinyint` INT64," +
                "`col_smallint` INT64," +
                "`col_numeric` NUMERIC," +
                "`col_numeric_with_precision` NUMERIC(29)," +
                "`col_numeric_with_scale` NUMERIC(33,4)," +
                "`col_decimal` NUMERIC," +
                "`col_real` FLOAT64," +
                "`col_float` FLOAT64," +
                "`col_double` FLOAT64," +
                "`col_char` STRING," +
                "`col_varchar` STRING," +
                "`col_longvarchar` STRING," +
                "`col_longtext` STRING," +
                "`col_text` STRING," +
                "`col_string` STRING," +
                "`col_string_with_length` STRING(16)," +
                "`col_binary` BYTES," +
                "`col_varbinary` BYTES," +
                "`col_longvarbinary` BYTES," +
                "`col_bytes` BYTES," +
                "`col_bytes_with_length` BYTES(10)," +
                "`col_date` DATE NOT NULL," +
                "`col_time` TIME," +
                "`col_datetime` DATETIME," +
                "`col_timestamp` TIMESTAMP," +
                "`col_boolean` BOOL," +
                "`col_json` JSON,PRIMARY KEY (`col_int`, `col_date`) NOT ENFORCED)";

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
                "`COL_INT` INT64 NOT NULL," +
                "`COL_INTEGER` INT64 NOT NULL," +
                "`COL_BIGINT` INT64," +
                "`COL_TINYINT` INT64," +
                "`COL_SMALLINT` INT64," +
                "`COL_NUMERIC` NUMERIC," +
                "`COL_NUMERIC_WITH_PRECISION` NUMERIC(29)," +
                "`COL_NUMERIC_WITH_SCALE` NUMERIC(33,4)," +
                "`COL_DECIMAL` NUMERIC," +
                "`COL_REAL` FLOAT64," +
                "`COL_FLOAT` FLOAT64," +
                "`COL_DOUBLE` FLOAT64," +
                "`COL_CHAR` STRING," +
                "`COL_VARCHAR` STRING," +
                "`COL_LONGVARCHAR` STRING," +
                "`COL_LONGTEXT` STRING," +
                "`COL_TEXT` STRING," +
                "`COL_STRING` STRING," +
                "`COL_STRING_WITH_LENGTH` STRING(16)," +
                "`COL_BINARY` BYTES," +
                "`COL_VARBINARY` BYTES," +
                "`COL_LONGVARBINARY` BYTES," +
                "`COL_BYTES` BYTES," +
                "`COL_BYTES_WITH_LENGTH` BYTES(10)," +
                "`COL_DATE` DATE NOT NULL," +
                "`COL_TIME` TIME," +
                "`COL_DATETIME` DATETIME," +
                "`COL_TIMESTAMP` TIMESTAMP," +
                "`COL_BOOLEAN` BOOL," +
                "`COL_JSON` JSON," +
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
                "`col_int` INT64 NOT NULL PRIMARY KEY NOT ENFORCED," +
                "`col_integer` INT64 NOT NULL," +
                "`col_string` STRING," +
                "`col_timestamp` TIMESTAMP," +
                "`col_double` FLOAT64) " +
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
                "`COL_INT` INT64 NOT NULL PRIMARY KEY NOT ENFORCED," +
                "`COL_INTEGER` INT64 NOT NULL," +
                "`COL_STRING` STRING," +
                "`COL_TIMESTAMP` TIMESTAMP," +
                "`COL_DOUBLE` FLOAT64) " +
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
                "`col_int` INT64 NOT NULL," +
                "`col_date` DATE NOT NULL," +
                "`col_integer` INT64 NOT NULL," +
                "`col_string` STRING," +
                "`col_timestamp` TIMESTAMP," +
                "`col_double` FLOAT64," +
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
                "`col_int` INT64 NOT NULL," +
                "`col_date` DATE NOT NULL," +
                "`col_integer` INT64 NOT NULL," +
                "`col_string` STRING," +
                "`col_timestamp` TIMESTAMP," +
                "`col_double` FLOAT64," +
                "PRIMARY KEY (`col_int`, `col_date`) NOT ENFORCED) " +
                "PARTITION BY _PARTITIONDATE";
        Assertions.assertEquals(expected, list.get(0));
    }


}
