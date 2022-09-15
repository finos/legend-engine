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

import org.finos.legend.engine.persistence.components.logicalplan.datasets.ColumnStoreSpecification;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;

import java.util.Optional;

public class BaseTestUtils
{
    static Field colInt = Field.builder().name("col_int").type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).primaryKey(true).build();
    static Field colInteger = Field.builder().name("col_integer").type(FieldType.of(DataType.INTEGER, Optional.empty(), Optional.empty())).nullable(false).unique(true).build();
    static Field colBigint = Field.builder().name("col_bigint").type(FieldType.of(DataType.BIGINT, Optional.empty(), Optional.empty())).build();
    static Field colTinyint = Field.builder().name("col_tinyint").type(FieldType.of(DataType.TINYINT, Optional.empty(), Optional.empty())).build();
    static Field colSmallint = Field.builder().name("col_smallint").type(FieldType.of(DataType.SMALLINT, Optional.empty(), Optional.empty())).build();
    static Field colChar = Field.builder().name("col_char").type(FieldType.of(DataType.CHAR, Optional.empty(), Optional.empty())).build();
    static Field colVarchar = Field.builder().name("col_varchar").type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).build();
    static Field colTimestamp = Field.builder().name("col_timestamp").type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty())).build();
    static Field colDatetime = Field.builder().name("col_datetime").type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build();
    static Field colDate = Field.builder().name("col_date").type(FieldType.of(DataType.DATE, Optional.empty(), Optional.empty())).build();
    static Field colReal = Field.builder().name("col_real").type(FieldType.of(DataType.REAL, Optional.empty(), Optional.empty())).build();
    static Field colFloat = Field.builder().name("col_float").type(FieldType.of(DataType.FLOAT, Optional.empty(), Optional.empty())).build();
    static Field colDecimal = Field.builder().name("col_decimal").type(FieldType.of(DataType.DECIMAL, 10, 4)).build();
    static Field colDouble = Field.builder().name("col_double").type(FieldType.of(DataType.DOUBLE, Optional.empty(), Optional.empty())).build();
    static Field colTime = Field.builder().name("col_time").type(FieldType.of(DataType.TIME, Optional.empty(), Optional.empty())).build();
    static Field colBoolean = Field.builder().name("col_boolean").type(FieldType.of(DataType.BOOLEAN, Optional.empty(), Optional.empty())).build();

    public static SchemaDefinition schemaWithAllColumns = SchemaDefinition.builder()
        .addFields(colInt)
        .addFields(colInteger)
        .addFields(colBigint)
        .addFields(colTinyint)
        .addFields(colSmallint)
        .addFields(colChar)
        .addFields(colVarchar)
        .addFields(colTimestamp)
        .addFields(colDatetime)
        .addFields(colDate)
        .addFields(colReal)
        .addFields(colFloat)
        .addFields(colDecimal)
        .addFields(colDouble)
        .addFields(colTime)
        .addFields(colBoolean)
        .build();

    public static SchemaDefinition schemaWithColumnStore = SchemaDefinition.builder()
        .addFields(colInt)
        .addFields(colInteger)
        .addFields(colBigint)
        .addFields(colTinyint)
        .addFields(colSmallint)
        .addFields(colChar)
        .addFields(colVarchar)
        .addFields(colTimestamp)
        .addFields(colDatetime)
        .addFields(colDate)
        .addFields(colReal)
        .addFields(colFloat)
        .addFields(colDecimal)
        .addFields(colDouble)
        .addFields(colTime)
        .addFields(colBoolean)
        .columnStoreSpecification(ColumnStoreSpecification.builder()
            .columnStore(true)
            .addColumnStoreKeys(colBigint)
            .build())
        .build();
}
