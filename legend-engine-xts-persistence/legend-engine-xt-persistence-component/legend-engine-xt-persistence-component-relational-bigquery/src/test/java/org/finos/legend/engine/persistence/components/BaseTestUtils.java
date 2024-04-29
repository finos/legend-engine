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

package org.finos.legend.engine.persistence.components;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.ClusterKey;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.Field;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.PartitionKey;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.SchemaDefinition;
import org.finos.legend.engine.persistence.components.logicalplan.values.FieldValue;
import org.finos.legend.engine.persistence.components.logicalplan.values.ObjectValue;

import java.util.Optional;

public class BaseTestUtils
{
    public static Field colInt = Field.builder().name("col_int").type(FieldType.of(DataType.INT, Optional.empty(), Optional.empty())).primaryKey(true).build();
    public static Field colInteger = Field.builder().name("col_integer").type(FieldType.of(DataType.INTEGER, Optional.empty(), Optional.empty())).nullable(false).build();
    public static Field colBigint = Field.builder().name("col_bigint").type(FieldType.of(DataType.BIGINT, Optional.empty(), Optional.empty())).build();
    public static Field colTinyint = Field.builder().name("col_tinyint").type(FieldType.of(DataType.TINYINT, Optional.empty(), Optional.empty())).build();
    public static Field colSmallint = Field.builder().name("col_smallint").type(FieldType.of(DataType.SMALLINT, Optional.empty(), Optional.empty())).build();
    public static Field colNumeric = Field.builder().name("col_numeric").type(FieldType.of(DataType.NUMERIC, Optional.empty(), Optional.empty())).build();
    public static Field colNumericWithPrecision = Field.builder().name("col_numeric_with_precision").type(FieldType.of(DataType.NUMERIC, Optional.of(29), Optional.empty())).build();
    public static Field colNumericWithScale = Field.builder().name("col_numeric_with_scale").type(FieldType.of(DataType.NUMERIC, Optional.of(33), Optional.of(4))).build();
    public static Field colDecimal = Field.builder().name("col_decimal").type(FieldType.of(DataType.DECIMAL, Optional.empty(), Optional.empty())).build();
    public static Field colReal = Field.builder().name("col_real").type(FieldType.of(DataType.REAL, Optional.empty(), Optional.empty())).build();
    public static Field colFloat = Field.builder().name("col_float").type(FieldType.of(DataType.FLOAT, Optional.empty(), Optional.empty())).build();
    public static Field colDouble = Field.builder().name("col_double").type(FieldType.of(DataType.DOUBLE, Optional.empty(), Optional.empty())).build();
    public static Field colChar = Field.builder().name("col_char").type(FieldType.of(DataType.CHAR, Optional.empty(), Optional.empty())).build();
    public static Field colVarchar = Field.builder().name("col_varchar").type(FieldType.of(DataType.VARCHAR, Optional.empty(), Optional.empty())).build();
    public static Field colLongVarchar = Field.builder().name("col_longvarchar").type(FieldType.of(DataType.LONGVARCHAR, Optional.empty(), Optional.empty())).build();
    public static Field colLongtext = Field.builder().name("col_longtext").type(FieldType.of(DataType.LONGTEXT, Optional.empty(), Optional.empty())).build();
    public static Field colText = Field.builder().name("col_text").type(FieldType.of(DataType.TEXT, Optional.empty(), Optional.empty())).build();
    public static Field colString = Field.builder().name("col_string").type(FieldType.of(DataType.STRING, Optional.empty(), Optional.empty())).build();
    public static Field colStringWithLength = Field.builder().name("col_string_with_length").type(FieldType.of(DataType.STRING, Optional.of(16), Optional.empty())).build();
    public static Field colBinary = Field.builder().name("col_binary").type(FieldType.of(DataType.BINARY, Optional.empty(), Optional.empty())).build();
    public static Field colVarBinary = Field.builder().name("col_varbinary").type(FieldType.of(DataType.VARBINARY, Optional.empty(), Optional.empty())).build();
    public static Field colLongVarBinary = Field.builder().name("col_longvarbinary").type(FieldType.of(DataType.LONGVARBINARY, Optional.empty(), Optional.empty())).build();
    public static Field colBytes = Field.builder().name("col_bytes").type(FieldType.of(DataType.BYTES, Optional.empty(), Optional.empty())).build();
    public static Field colBytesWithLength = Field.builder().name("col_bytes_with_length").type(FieldType.of(DataType.BYTES, 10, null)).build();
    public static Field colDate = Field.builder().name("col_date").type(FieldType.of(DataType.DATE, Optional.empty(), Optional.empty())).primaryKey(true).build();
    public static Field colTime = Field.builder().name("col_time").type(FieldType.of(DataType.TIME, Optional.empty(), Optional.empty())).build();
    public static Field colDatetime = Field.builder().name("col_datetime").type(FieldType.of(DataType.DATETIME, Optional.empty(), Optional.empty())).build();
    public static Field colTimestamp = Field.builder().name("col_timestamp").type(FieldType.of(DataType.TIMESTAMP, Optional.empty(), Optional.empty())).build();
    public static Field colBoolean = Field.builder().name("col_boolean").type(FieldType.of(DataType.BOOLEAN, Optional.empty(), Optional.empty())).build();
    public static Field colJson = Field.builder().name("col_json").type(FieldType.of(DataType.JSON, Optional.empty(), Optional.empty())).build();

    public static ClusterKey clusterKey1 = ClusterKey.builder().key(FieldValue.builder().fieldName("col_timestamp").build()).build();
    public static ClusterKey clusterKey2 = ClusterKey.builder().key(FieldValue.builder().fieldName("col_int").build()).build();

    public static PartitionKey partitionKey1 = PartitionKey.of(FieldValue.builder().fieldName("col_date").build());

    public static PartitionKey partitionKey2 = PartitionKey.of(ObjectValue.of("_PARTITIONDATE"));

    public static SchemaDefinition schemaWithAllColumns = SchemaDefinition.builder()
            .addFields(colInt)
            .addFields(colInteger)
            .addFields(colBigint)
            .addFields(colTinyint)
            .addFields(colSmallint)
            .addFields(colNumeric)
            .addFields(colNumericWithPrecision)
            .addFields(colNumericWithScale)
            .addFields(colDecimal)
            .addFields(colReal)
            .addFields(colFloat)
            .addFields(colDouble)
            .addFields(colChar)
            .addFields(colVarchar)
            .addFields(colLongVarchar)
            .addFields(colLongtext)
            .addFields(colText)
            .addFields(colString)
            .addFields(colStringWithLength)
            .addFields(colBinary)
            .addFields(colVarBinary)
            .addFields(colLongVarBinary)
            .addFields(colBytes)
            .addFields(colBytesWithLength)
            .addFields(colDate)
            .addFields(colTime)
            .addFields(colDatetime)
            .addFields(colTimestamp)
            .addFields(colBoolean)
            .addFields(colJson)
            .build();


    public static SchemaDefinition schemaWithAllColumnsFromDb = SchemaDefinition.builder()
            .addFields(colInt.withType(colInt.type().withDataType(DataType.INTEGER)))
            .addFields(colInteger)
            .addFields(colBigint.withType(colBigint.type().withDataType(DataType.INTEGER)))
            .addFields(colTinyint.withType(colTinyint.type().withDataType(DataType.INTEGER)))
            .addFields(colSmallint.withType(colSmallint.type().withDataType(DataType.INTEGER)))
            .addFields(colNumeric)
            .addFields(colNumericWithPrecision)
            .addFields(colNumericWithScale)
            .addFields(colDecimal.withType(colDecimal.type().withDataType(DataType.NUMERIC)))
            .addFields(colReal.withType(colReal.type().withDataType(DataType.FLOAT)))
            .addFields(colFloat)
            .addFields(colDouble.withType(colDouble.type().withDataType(DataType.FLOAT)))
            .addFields(colChar.withType(colChar.type().withDataType(DataType.STRING)))
            .addFields(colVarchar.withType(colVarchar.type().withDataType(DataType.STRING)))
            .addFields(colLongVarchar.withType(colLongVarchar.type().withDataType(DataType.STRING)))
            .addFields(colLongtext.withType(colLongtext.type().withDataType(DataType.STRING)))
            .addFields(colText.withType(colText.type().withDataType(DataType.STRING)))
            .addFields(colString)
            .addFields(colStringWithLength)
            .addFields(colBinary.withType(colBinary.type().withDataType(DataType.BYTES)))
            .addFields(colVarBinary.withType(colVarBinary.type().withDataType(DataType.BYTES)))
            .addFields(colLongVarBinary.withType(colLongVarBinary.type().withDataType(DataType.BYTES)))
            .addFields(colBytes)
            .addFields(colBytesWithLength)
            .addFields(colDate)
            .addFields(colTime)
            .addFields(colDatetime)
            .addFields(colTimestamp)
            .addFields(colBoolean)
            .addFields(colJson)
            .build();

    public static SchemaDefinition schemaWithClusteringKey = SchemaDefinition.builder()
            .addFields(colInt)
            .addFields(colInteger)
            .addFields(colString)
            .addFields(colTimestamp)
            .addFields(colDouble)
            .addClusterKeys(clusterKey1, clusterKey2)
            .build();

    public static SchemaDefinition schemaWithClusteringAndPartitionKey = SchemaDefinition.builder()
            .addFields(colInt)
            .addFields(colDate)
            .addFields(colInteger)
            .addFields(colString)
            .addFields(colTimestamp)
            .addFields(colDouble)
            .addClusterKeys(clusterKey1, clusterKey2)
            .addPartitionKeys(partitionKey1)
            .build();

    public static SchemaDefinition schemaWithPartitionKey = SchemaDefinition.builder()
            .addFields(colInt)
            .addFields(colDate)
            .addFields(colInteger)
            .addFields(colString)
            .addFields(colTimestamp)
            .addFields(colDouble)
            .addPartitionKeys(partitionKey2)
            .build();
}