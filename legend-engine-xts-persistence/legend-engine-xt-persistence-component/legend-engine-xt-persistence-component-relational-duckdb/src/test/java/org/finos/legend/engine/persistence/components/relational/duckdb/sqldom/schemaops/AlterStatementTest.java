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

package org.finos.legend.engine.persistence.components.relational.duckdb.sqldom.schemaops;

import org.finos.legend.engine.persistence.components.logicalplan.LogicalPlan;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Alter;
import org.finos.legend.engine.persistence.components.logicalplan.operations.Operation;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.optimizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.duckdb.DuckDBSink;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.*;

public class AlterStatementTest
{
    public static SchemaDefinition schemaWithAllColumns = SchemaDefinition.builder()
        .addFields(colInt)
        .addFields(colInteger)
        .addFields(colBigint)
        .addFields(colTinyint)
        .addFields(colSmallint)
        .addFields(colChar)
        .addFields(colVarchar)
        .addFields(colString)
        .addFields(colTimestamp)
        .addFields(colDatetime)
        .addFields(colDate)
        .addFields(colReal)
        .addFields(colFloat)
        .addFields(colDecimal)
        .addFields(colDouble)
        .addFields(colBinary)
        .addFields(colTime)
        .addFields(colNumeric)
        .addFields(colBoolean)
        .addFields(colVarBinary)
        .build();

    @Test
    public void testAlterTable()
    {
        DatasetDefinition dataset = DatasetDefinition.builder()
            .database("my_db")
            .group("my_schema")
            .name("my_table")
            .alias("my_alias")
            .schema(schemaWithAllColumns)
            .build();
        Field column = Field.builder().name("column").type(FieldType.of(DataType.VARCHAR, 64, null)).nullable(false).build();

        Operation add = Alter.of(dataset, Alter.AlterOperation.ADD, column, Optional.empty());
        Operation changeDatatype = Alter.of(dataset, Alter.AlterOperation.CHANGE_DATATYPE, column, Optional.empty());
        Operation nullableColumn = Alter.of(dataset, Alter.AlterOperation.NULLABLE_COLUMN, column, Optional.empty());

        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(add, changeDatatype, nullableColumn).build();
        RelationalTransformer transformer = new RelationalTransformer(DuckDBSink.get());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();

        String expectedAdd = "ALTER TABLE \"my_db\".\"my_schema\".\"my_table\" ADD COLUMN \"column\" VARCHAR NOT NULL";
        String expectedChangeDataType = "ALTER TABLE \"my_db\".\"my_schema\".\"my_table\" ALTER COLUMN \"column\" VARCHAR";
        String expectedNullableColumn = "ALTER TABLE \"my_db\".\"my_schema\".\"my_table\" ALTER COLUMN \"column\" DROP NOT NULL";

        Assertions.assertEquals(expectedAdd, list.get(0));
        Assertions.assertEquals(expectedChangeDataType, list.get(1));
        Assertions.assertEquals(expectedNullableColumn, list.get(2));
    }

    @Test
    public void testAlterTableWithUpperCase()
    {
        DatasetDefinition dataset = DatasetDefinition.builder()
            .database("my_db")
            .group("my_schema")
            .name("my_table")
            .alias("my_alias")
            .schema(schemaWithAllColumns)
            .build();
        Field column = Field.builder().name("column").type(FieldType.of(DataType.VARCHAR, 64, null)).nullable(false).build();

        Operation add = Alter.of(dataset, Alter.AlterOperation.ADD, column, Optional.empty());
        Operation changeDatatype = Alter.of(dataset, Alter.AlterOperation.CHANGE_DATATYPE, column, Optional.empty());
        Operation nullableColumn = Alter.of(dataset, Alter.AlterOperation.NULLABLE_COLUMN, column, Optional.empty());

        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(add, changeDatatype, nullableColumn).build();
        RelationalTransformer transformer = new RelationalTransformer(DuckDBSink.get(), TransformOptions.builder().addOptimizers(new UpperCaseOptimizer()).build());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();

        String expectedAdd = "ALTER TABLE \"MY_DB\".\"MY_SCHEMA\".\"MY_TABLE\" ADD COLUMN \"COLUMN\" VARCHAR NOT NULL";
        String expectedChangeDataType = "ALTER TABLE \"MY_DB\".\"MY_SCHEMA\".\"MY_TABLE\" ALTER COLUMN \"COLUMN\" VARCHAR";
        String expectedNullableColumn = "ALTER TABLE \"MY_DB\".\"MY_SCHEMA\".\"MY_TABLE\" ALTER COLUMN \"COLUMN\" DROP NOT NULL";

        Assertions.assertEquals(expectedAdd, list.get(0));
        Assertions.assertEquals(expectedChangeDataType, list.get(1));
        Assertions.assertEquals(expectedNullableColumn, list.get(2));
    }
}
