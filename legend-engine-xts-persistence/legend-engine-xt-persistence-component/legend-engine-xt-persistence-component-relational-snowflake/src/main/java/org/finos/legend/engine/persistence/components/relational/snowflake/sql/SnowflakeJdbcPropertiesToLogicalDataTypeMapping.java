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

package org.finos.legend.engine.persistence.components.relational.snowflake.sql;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.relational.sql.JdbcPropertiesToLogicalDataTypeMapping;

public class SnowflakeJdbcPropertiesToLogicalDataTypeMapping implements JdbcPropertiesToLogicalDataTypeMapping
{

    // Reference: https://docs.snowflake.com/en/sql-reference/data-types.html

    public FieldType getDataType(String typeName, String dataType, Integer columnSize, Integer decimalDigits)
    {
        switch (typeName)
        {
            case NUMBER:
                switch (dataType)
                {
                    case BIGINT:
                        return FieldType.builder().dataType(DataType.BIGINT).build();
                    case DECIMAL:
                        return FieldType.builder().dataType(DataType.DECIMAL).length(columnSize).scale(decimalDigits).build();
                    default:
                        throw new IllegalArgumentException("Unexpected values: JDBC TYPE_NAME " + typeName + ", JDBC DATA_TYPE: " + dataType);
                }
            case DOUBLE:
                return FieldType.builder().dataType(DataType.DOUBLE).build();
            case VARCHAR:
                return FieldType.builder().dataType(DataType.VARCHAR).length(columnSize).build();
            case BINARY:
                return FieldType.builder().dataType(DataType.BINARY).length(columnSize).build();
            case BOOLEAN:
                return FieldType.builder().dataType(DataType.BOOLEAN).build();
            case DATE:
                return FieldType.builder().dataType(DataType.DATE).build();
            case TIME:
                return FieldType.builder().dataType(DataType.TIME).length(decimalDigits).build();
            case TIMESTAMPNTZ:
                return FieldType.builder().dataType(DataType.TIMESTAMP).length(decimalDigits).build();
            case TIMESTAMPLTZ:
                return FieldType.builder().dataType(DataType.TIMESTAMP_LTZ).length(decimalDigits).build();
            case TIMESTAMPTZ:
                return FieldType.builder().dataType(DataType.TIMESTAMP_TZ).length(decimalDigits).build();
            case VARIANT:
                return FieldType.builder().dataType(DataType.JSON).build();
            case OBJECT:
                return FieldType.builder().dataType(DataType.MAP).build();
            case ARRAY:
                return FieldType.builder().dataType(DataType.ARRAY).build();
            default:
                throw new IllegalArgumentException("Unexpected values: JDBC TYPE_NAME " + typeName + ", JDBC DATA_TYPE: " + dataType);
        }
    }
}
