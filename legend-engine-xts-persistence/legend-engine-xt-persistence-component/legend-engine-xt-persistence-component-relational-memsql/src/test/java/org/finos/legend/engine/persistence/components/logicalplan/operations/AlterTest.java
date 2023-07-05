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
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DatasetDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.relational.SqlPlan;
import org.finos.legend.engine.persistence.components.relational.ansi.optimizer.UpperCaseOptimizer;
import org.finos.legend.engine.persistence.components.relational.memsql.MemSqlSink;
import org.finos.legend.engine.persistence.components.relational.transformer.RelationalTransformer;
import org.finos.legend.engine.persistence.components.transformer.TransformOptions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.schemaWithAllColumns;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.schemaWithColumnStore;

public class AlterTest
{

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
        Field newColumn = Field.builder().name("column1").type(FieldType.of(DataType.VARCHAR, 64, null)).nullable(false).build();

        Operation add = Alter.of(dataset, Alter.AlterOperation.ADD, column, Optional.empty());
        Operation changeDatatype = Alter.of(dataset, Alter.AlterOperation.CHANGE_DATATYPE, column, Optional.empty());
        Operation nullableColumn = Alter.of(dataset, Alter.AlterOperation.NULLABLE_COLUMN, column, Optional.empty());
        Operation dropColumn = Alter.of(dataset, Alter.AlterOperation.DROP, column, Optional.empty());
        Operation renameColumn = Alter.of(dataset, Alter.AlterOperation.RENAME_COLUMN, column, newColumn);

        RelationalTransformer transformer = new RelationalTransformer(MemSqlSink.get());

        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(add, changeDatatype, nullableColumn, dropColumn, renameColumn).build();
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();

        String expectedAdd = "ALTER TABLE `my_db`.`my_schema`.`my_table` ADD COLUMN `column` VARCHAR(64) NOT NULL";
        String expectedChangeDataType = "ALTER TABLE `my_db`.`my_schema`.`my_table` MODIFY COLUMN `column` VARCHAR(64) NOT NULL";
        String expectedNullableColumn = "ALTER TABLE `my_db`.`my_schema`.`my_table` MODIFY COLUMN `column` VARCHAR(64) NULL";
        String expectedDropColumn = "ALTER TABLE `my_db`.`my_schema`.`my_table` DROP COLUMN `column`";
        String expectedRenameColumn = "ALTER TABLE `my_db`.`my_schema`.`my_table` CHANGE `column` `column1`";

        Assertions.assertEquals(expectedAdd, list.get(0));
        Assertions.assertEquals(expectedChangeDataType, list.get(1));
        Assertions.assertEquals(expectedNullableColumn, list.get(2));
        Assertions.assertEquals(expectedDropColumn, list.get(3));
        Assertions.assertEquals(expectedRenameColumn, list.get(4));
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
        Field newColumn = Field.builder().name("column1").type(FieldType.of(DataType.VARCHAR, 64, null)).nullable(false).build();

        Operation add = Alter.of(dataset, Alter.AlterOperation.ADD, column, Optional.empty());
        Operation changeDatatype = Alter.of(dataset, Alter.AlterOperation.CHANGE_DATATYPE, column, Optional.empty());
        Operation nullableColumn = Alter.of(dataset, Alter.AlterOperation.NULLABLE_COLUMN, column, Optional.empty());
        Operation dropColumn = Alter.of(dataset, Alter.AlterOperation.DROP, column, Optional.empty());
        Operation renameColumn = Alter.of(dataset, Alter.AlterOperation.RENAME_COLUMN, column, newColumn);

        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(add, changeDatatype, nullableColumn, dropColumn, renameColumn).build();

        RelationalTransformer transformer = new RelationalTransformer(
            MemSqlSink.get(),
            TransformOptions.builder().addOptimizers(new UpperCaseOptimizer()).build());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);

        List<String> list = physicalPlan.getSqlList();

        String expectedAdd = "ALTER TABLE `MY_DB`.`MY_SCHEMA`.`MY_TABLE` ADD COLUMN `COLUMN` VARCHAR(64) NOT NULL";
        String expectedChangeDataType = "ALTER TABLE `MY_DB`.`MY_SCHEMA`.`MY_TABLE` MODIFY COLUMN `COLUMN` VARCHAR(64) NOT NULL";
        String expectedNullableColumn = "ALTER TABLE `MY_DB`.`MY_SCHEMA`.`MY_TABLE` MODIFY COLUMN `COLUMN` VARCHAR(64) NULL";
        String expectedDropColumn = "ALTER TABLE `MY_DB`.`MY_SCHEMA`.`MY_TABLE` DROP COLUMN `COLUMN`";
        String expectedRenameColumn = "ALTER TABLE `MY_DB`.`MY_SCHEMA`.`MY_TABLE` CHANGE `COLUMN` `COLUMN1`";

        Assertions.assertEquals(expectedAdd, list.get(0));
        Assertions.assertEquals(expectedChangeDataType, list.get(1));
        Assertions.assertEquals(expectedNullableColumn, list.get(2));
        Assertions.assertEquals(expectedDropColumn, list.get(3));
        Assertions.assertEquals(expectedRenameColumn, list.get(4));
    }

    @Test
    public void testAlterTableChangeDataTypeAndColumnStore()
    {
        DatasetDefinition dataset = DatasetDefinition.builder()
            .database("my_db")
            .group("my_schema")
            .name("my_table")
            .schema(schemaWithColumnStore)
            .build();
        Field column = Field.builder().name("column").type(FieldType.of(DataType.VARCHAR, 64, null)).nullable(false).build();

        Operation changeDataType = Alter.of(dataset, Alter.AlterOperation.CHANGE_DATATYPE, column, Optional.empty());

        LogicalPlan logicalPlan = LogicalPlan.builder().addOps(changeDataType).build();
        RelationalTransformer transformer = new RelationalTransformer(MemSqlSink.get());
        SqlPlan physicalPlan = transformer.generatePhysicalPlan(logicalPlan);
        List<String> list = physicalPlan.getSqlList();

        String expectedAddColumn = "ALTER TABLE `my_db`.`my_schema`.`my_table` ADD COLUMN `column1` VARCHAR(64) NOT NULL";
        String expectedCopyColumn = "UPDATE `my_db`.`my_schema`.`my_table` as my_table SET `column1` = `column`";
        String expectedDropColumn = "ALTER TABLE `my_db`.`my_schema`.`my_table` DROP COLUMN `column`";
        String expectedRenameColumn = "ALTER TABLE `my_db`.`my_schema`.`my_table` CHANGE `column1` `column`";

        Assertions.assertEquals(expectedAddColumn, list.get(0));
        Assertions.assertEquals(expectedCopyColumn, list.get(1));
        Assertions.assertEquals(expectedDropColumn, list.get(2));
        Assertions.assertEquals(expectedRenameColumn, list.get(3));
    }
}
