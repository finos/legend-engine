// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.duckdb.sql;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.relational.sql.JdbcPropertiesToLogicalDataTypeMapping;

public class DuckDBJdbcPropertiesToLogicalDataTypeMapping implements JdbcPropertiesToLogicalDataTypeMapping
{
    public FieldType getDataType(String typeName, String dataType, Integer columnSize, Integer decimalDigits)
    {
        switch (typeName)
        {
            case BPCHAR:
                return FieldType.builder().dataType(DataType.CHAR).length(columnSize).build();
            case VARCHAR:
                return FieldType.builder().dataType(DataType.VARCHAR).length(columnSize).build();
            case BYTEA:
                return FieldType.builder().dataType(DataType.BINARY).build();
            case BIT:
                return FieldType.builder().dataType(DataType.BIT).length(columnSize).build();
            case TEXT:
                return FieldType.builder().dataType(DataType.TEXT).build();
            case BOOL:
                return FieldType.builder().dataType(DataType.BOOLEAN).build();
            case INT2:
                return FieldType.builder().dataType(DataType.SMALLINT).build();
            case INT4:
                return FieldType.builder().dataType(DataType.INTEGER).build();
            case INT8:
                return FieldType.builder().dataType(DataType.BIGINT).build();
            case NUMERIC:
                return FieldType.builder().dataType(DataType.NUMERIC).length(columnSize).scale(decimalDigits).build();
            case FLOAT4:
                return FieldType.builder().dataType(DataType.REAL).build();
            case FLOAT8:
                return FieldType.builder().dataType(DataType.DOUBLE).build();
            case DATE:
                return FieldType.builder().dataType(DataType.DATE).build();
            case TIME:
                return FieldType.builder().dataType(DataType.TIME).length(decimalDigits).build();
            case TIMESTAMP:
                return FieldType.builder().dataType(DataType.TIMESTAMP).length(decimalDigits).build();
            case TIMESTAMPTZ:
                return FieldType.builder().dataType(DataType.TIMESTAMP_TZ).length(decimalDigits).build();
            case JSON:
                return FieldType.builder().dataType(DataType.JSON).build();
            default:
                throw new IllegalArgumentException("Unexpected values: JDBC TYPE_NAME " + typeName + ", JDBC DATA_TYPE: " + dataType);
        }
    }
}
