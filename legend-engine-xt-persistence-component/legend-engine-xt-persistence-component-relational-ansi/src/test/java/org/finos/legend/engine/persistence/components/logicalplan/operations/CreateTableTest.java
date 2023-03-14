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
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.AnsiSqlSink;
import org.finos.legend.engine.persistence.components.relational.ansi.optimizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.schemaWithAllColumns;

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

        RelationalTransformer transformer = new RelationalTransformer(AnsiSqlSink.get());
        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(create).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();

        String expected = "CREATE TABLE IF NOT EXISTS \"my_db\".\"my_schema\".\"my_table\"(\"col_int\" INTEGER NOT NULL PRIMARY KEY," +
            "\"col_integer\" INTEGER NOT NULL UNIQUE,\"col_bigint\" BIGINT,\"col_tinyint\" TINYINT," +
            "\"col_smallint\" SMALLINT,\"col_char\" CHAR,\"col_varchar\" VARCHAR,\"col_string\" VARCHAR," +
            "\"col_timestamp\" TIMESTAMP,\"col_datetime\" DATETIME,\"col_date\" DATE,\"col_real\" REAL," +
            "\"col_float\" REAL,\"col_decimal\" DECIMAL(10,4),\"col_double\" DOUBLE,\"col_binary\" BINARY," +
            "\"col_time\" TIME,\"col_numeric\" NUMERIC,\"col_longtext\" LONGTEXT,\"col_bool\" BOOLEAN," +
            "\"col_boolean\" BOOLEAN,\"col_varbinary\" VARBINARY(10),\"col_int64\" INT64,\"col_bit\" BIT" +
            ",INDEX \"my_idx\" (\"col_int\", \"col_varchar\"))";

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

        RelationalTransformer transformer = new RelationalTransformer(
            AnsiSqlSink.get(),
            TransformOptions.builder().addOptimizers(new UpperCaseOptimizer()).build());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);

        List<String> list = physicalPlan.getSqlList();

        String expected = "CREATE TABLE IF NOT EXISTS \"MY_DB\".\"MY_SCHEMA\".\"MY_TABLE\"" +
            "(\"COL_INT\" INTEGER NOT NULL PRIMARY KEY,\"COL_INTEGER\" INTEGER NOT NULL UNIQUE,\"COL_BIGINT\" BIGINT," +
            "\"COL_TINYINT\" TINYINT,\"COL_SMALLINT\" SMALLINT,\"COL_CHAR\" CHAR,\"COL_VARCHAR\" VARCHAR," +
            "\"COL_STRING\" VARCHAR,\"COL_TIMESTAMP\" TIMESTAMP,\"COL_DATETIME\" DATETIME,\"COL_DATE\" DATE," +
            "\"COL_REAL\" REAL,\"COL_FLOAT\" REAL,\"COL_DECIMAL\" DECIMAL(10,4),\"COL_DOUBLE\" DOUBLE," +
            "\"COL_BINARY\" BINARY,\"COL_TIME\" TIME,\"COL_NUMERIC\" NUMERIC,\"COL_LONGTEXT\" LONGTEXT," +
            "\"COL_BOOL\" BOOLEAN,\"COL_BOOLEAN\" BOOLEAN,\"COL_VARBINARY\" VARBINARY(10),\"COL_INT64\" INT64," +
            "\"COL_BIT\" BIT,INDEX \"MY_IDX\" (\"COL_INT\", \"COL_VARCHAR\"))";

        Assertions.assertEquals(expected, list.get(0));
    }
}
