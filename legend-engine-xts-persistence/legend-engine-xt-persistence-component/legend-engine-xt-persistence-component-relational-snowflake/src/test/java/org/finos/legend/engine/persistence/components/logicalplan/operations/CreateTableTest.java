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
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetAdditionalProperties;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.IcebergProperties;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.TableOrigin;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.snowflake.SnowflakeSink;
import org.finos.legend.engine.persistence.components.relational.snowflake.optmizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.finos.legend.engine.persistence.components.BaseTestUtils.schemaWithAllColumns;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.schemaWithClusteringKey;

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
        RelationalTransformer transformer = new RelationalTransformer(SnowflakeSink.get());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expected = "CREATE TABLE IF NOT EXISTS \"my_db\".\"my_schema\".\"my_table\"" +
            "(\"col_int\" INTEGER NOT NULL PRIMARY KEY,\"col_integer\" INTEGER NOT NULL UNIQUE,\"col_bigint\" BIGINT," +
            "\"col_tinyint\" TINYINT,\"col_smallint\" SMALLINT,\"col_char\" CHAR,\"col_varchar\" VARCHAR," +
            "\"col_string\" VARCHAR,\"col_timestamp\" TIMESTAMP,\"col_datetime\" DATETIME,\"col_date\" DATE," +
            "\"col_real\" DOUBLE,\"col_float\" DOUBLE,\"col_decimal\" NUMBER(10,4),\"col_double\" DOUBLE," +
            "\"col_binary\" BINARY,\"col_time\" TIME,\"col_numeric\" NUMBER(38,0),\"col_boolean\" BOOLEAN," +
            "\"col_varbinary\" BINARY(10))";

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
        RelationalTransformer transformer = new RelationalTransformer(SnowflakeSink.get(), TransformOptions.builder().addOptimizers(new UpperCaseOptimizer()).build());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expected = "CREATE TABLE IF NOT EXISTS \"MY_DB\".\"MY_SCHEMA\".\"MY_TABLE\"" +
            "(\"COL_INT\" INTEGER NOT NULL PRIMARY KEY,\"COL_INTEGER\" INTEGER NOT NULL UNIQUE,\"COL_BIGINT\" BIGINT," +
            "\"COL_TINYINT\" TINYINT,\"COL_SMALLINT\" SMALLINT,\"COL_CHAR\" CHAR,\"COL_VARCHAR\" VARCHAR," +
            "\"COL_STRING\" VARCHAR,\"COL_TIMESTAMP\" TIMESTAMP,\"COL_DATETIME\" DATETIME,\"COL_DATE\" DATE," +
            "\"COL_REAL\" DOUBLE,\"COL_FLOAT\" DOUBLE,\"COL_DECIMAL\" NUMBER(10,4),\"COL_DOUBLE\" DOUBLE," +
            "\"COL_BINARY\" BINARY,\"COL_TIME\" TIME,\"COL_NUMERIC\" NUMBER(38,0),\"COL_BOOLEAN\" BOOLEAN," +
            "\"COL_VARBINARY\" BINARY(10))";

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
        RelationalTransformer transformer = new RelationalTransformer(SnowflakeSink.get());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expected = "CREATE TABLE IF NOT EXISTS \"my_db\".\"my_schema\".\"my_table\"" +
            "(\"col_int\" INTEGER NOT NULL PRIMARY KEY," +
            "\"col_integer\" INTEGER NOT NULL UNIQUE," +
            "\"col_string\" VARCHAR," +
            "\"col_timestamp\" TIMESTAMP," +
            "\"col_double\" DOUBLE)" +
            " CLUSTER BY (\"col_timestamp\",SUBSTRING(\"col_int\",1,4),\"col_integer\"%7)";

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
        RelationalTransformer transformer = new RelationalTransformer(SnowflakeSink.get(), TransformOptions.builder().addOptimizers(new UpperCaseOptimizer()).build());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expected = "CREATE TABLE IF NOT EXISTS \"MY_DB\".\"MY_SCHEMA\".\"MY_TABLE\"" +
            "(\"COL_INT\" INTEGER NOT NULL PRIMARY KEY," +
            "\"COL_INTEGER\" INTEGER NOT NULL UNIQUE," +
            "\"COL_STRING\" VARCHAR" +
            ",\"COL_TIMESTAMP\" TIMESTAMP," +
            "\"COL_DOUBLE\" DOUBLE) " +
            "CLUSTER BY (\"COL_TIMESTAMP\",SUBSTRING(\"COL_INT\",1,4),\"COL_INTEGER\"%7)";

        Assertions.assertEquals(expected, list.get(0));
    }

    @Test
    public void testCreateIcebergTablePropertiesMissing()
    {
        DatasetDefinition dataset = DatasetDefinition.builder()
                .database("my_db")
                .group("my_schema")
                .name("my_table")
                .alias("my_alias")
                .schema(schemaWithAllColumns)
                .datasetAdditionalProperties(DatasetAdditionalProperties.builder().tableOrigin(TableOrigin.ICEBERG).build())
                .build();
        Operation create = Create.of(true, dataset);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(create).build();
        RelationalTransformer transformer = new RelationalTransformer(SnowflakeSink.get());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        try
        {
            physicalPlan.getSqlList();
            Assertions.fail("Should not reach here");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e.getMessage().contains("Iceberg properties are mandatory for Iceberg Table"));
        }

    }

    @Test
    public void testCreateIcebergTable()
    {
        DatasetDefinition dataset = DatasetDefinition.builder()
                .database("my_db")
                .group("my_schema")
                .name("my_table")
                .alias("my_alias")
                .schema(schemaWithAllColumns)
                .datasetAdditionalProperties(DatasetAdditionalProperties.builder()
                        .tableOrigin(TableOrigin.ICEBERG)
                        .icebergProperties(IcebergProperties.builder().externalVolume("my_ext_vol").catalog("SNOWFLAKE").baseLocation("my_location").build())
                        .build())
                .build();
        Operation create = Create.of(true, dataset);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(create).build();
        RelationalTransformer transformer = new RelationalTransformer(SnowflakeSink.get());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expected = "CREATE ICEBERG TABLE IF NOT EXISTS \"my_db\".\"my_schema\".\"my_table\"" +
                "(\"col_int\" INTEGER NOT NULL PRIMARY KEY,\"col_integer\" INTEGER NOT NULL UNIQUE,\"col_bigint\" BIGINT," +
                "\"col_tinyint\" TINYINT,\"col_smallint\" SMALLINT,\"col_char\" CHAR,\"col_varchar\" VARCHAR," +
                "\"col_string\" VARCHAR,\"col_timestamp\" TIMESTAMP,\"col_datetime\" DATETIME,\"col_date\" DATE," +
                "\"col_real\" DOUBLE,\"col_float\" DOUBLE,\"col_decimal\" NUMBER(10,4),\"col_double\" DOUBLE," +
                "\"col_binary\" BINARY,\"col_time\" TIME,\"col_numeric\" NUMBER(38,0),\"col_boolean\" BOOLEAN," +
                "\"col_varbinary\" BINARY(10)) CATALOG='SNOWFLAKE', EXTERNAL_VOLUME='my_ext_vol', BASE_LOCATION='my_location'";

        Assertions.assertEquals(expected, list.get(0));
    }

    @Test
    public void testCreateIcebergTableWithCAtalogSync()
    {
        DatasetDefinition dataset = DatasetDefinition.builder()
                .database("my_db")
                .group("my_schema")
                .name("my_table")
                .alias("my_alias")
                .schema(schemaWithAllColumns)
                .datasetAdditionalProperties(DatasetAdditionalProperties.builder()
                        .tableOrigin(TableOrigin.ICEBERG)
                        .icebergProperties(IcebergProperties.builder().externalVolume("my_ext_vol").catalog("SNOWFLAKE").baseLocation("my_location")
                                .catalogSync("polaris").build())
                        .build())
                .build();
        Operation create = Create.of(true, dataset);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(create).build();
        RelationalTransformer transformer = new RelationalTransformer(SnowflakeSink.get());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expected = "CREATE ICEBERG TABLE IF NOT EXISTS \"my_db\".\"my_schema\".\"my_table\"" +
                "(\"col_int\" INTEGER NOT NULL PRIMARY KEY,\"col_integer\" INTEGER NOT NULL UNIQUE,\"col_bigint\" BIGINT," +
                "\"col_tinyint\" TINYINT,\"col_smallint\" SMALLINT,\"col_char\" CHAR,\"col_varchar\" VARCHAR," +
                "\"col_string\" VARCHAR,\"col_timestamp\" TIMESTAMP,\"col_datetime\" DATETIME,\"col_date\" DATE," +
                "\"col_real\" DOUBLE,\"col_float\" DOUBLE,\"col_decimal\" NUMBER(10,4),\"col_double\" DOUBLE," +
                "\"col_binary\" BINARY,\"col_time\" TIME,\"col_numeric\" NUMBER(38,0),\"col_boolean\" BOOLEAN," +
                "\"col_varbinary\" BINARY(10)) CATALOG='SNOWFLAKE', EXTERNAL_VOLUME='my_ext_vol', BASE_LOCATION='my_location', CATALOG_SYNC='polaris'";

        Assertions.assertEquals(expected, list.get(0));
    }

    @Test
    public void testCreateTableWithTags()
    {
        DatasetDefinition dataset = DatasetDefinition.builder()
                .database("my_db")
                .group("my_schema")
                .name("my_table")
                .alias("my_alias")
                .schema(schemaWithAllColumns)
                .datasetAdditionalProperties(DatasetAdditionalProperties.builder()
                        .putTags("key1", "value1").putTags("key2", "value2")
                        .build())
                .build();
        Operation create = Create.of(true, dataset);
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(create).build();
        RelationalTransformer transformer = new RelationalTransformer(SnowflakeSink.get());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();
        String expected = "CREATE TABLE IF NOT EXISTS \"my_db\".\"my_schema\".\"my_table\"" +
                "(\"col_int\" INTEGER NOT NULL PRIMARY KEY,\"col_integer\" INTEGER NOT NULL UNIQUE,\"col_bigint\" BIGINT," +
                "\"col_tinyint\" TINYINT,\"col_smallint\" SMALLINT,\"col_char\" CHAR,\"col_varchar\" VARCHAR," +
                "\"col_string\" VARCHAR,\"col_timestamp\" TIMESTAMP,\"col_datetime\" DATETIME,\"col_date\" DATE," +
                "\"col_real\" DOUBLE,\"col_float\" DOUBLE,\"col_decimal\" NUMBER(10,4),\"col_double\" DOUBLE," +
                "\"col_binary\" BINARY,\"col_time\" TIME,\"col_numeric\" NUMBER(38,0),\"col_boolean\" BOOLEAN," +
                "\"col_varbinary\" BINARY(10)) WITH TAG (key1 = 'value1', key2 = 'value2')";

        Assertions.assertEquals(expected, list.get(0));
    }


}
