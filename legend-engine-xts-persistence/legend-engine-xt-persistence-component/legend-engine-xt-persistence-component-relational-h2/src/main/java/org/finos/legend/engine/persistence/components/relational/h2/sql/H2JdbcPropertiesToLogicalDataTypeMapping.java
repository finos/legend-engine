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

package org.finos.legend.engine.persistence.components.relational.h2.sql;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.relational.sql.JdbcPropertiesToLogicalDataTypeMapping;

public class H2JdbcPropertiesToLogicalDataTypeMapping implements JdbcPropertiesToLogicalDataTypeMapping
{
    public FieldType getDataType(String typeName, String dataType, Integer columnSize, Integer decimalDigits)
    {

        switch (typeName)
        {
            case CHAR:
                return FieldType.builder().dataType(DataType.CHAR).length(columnSize).build();
            case VARCHAR:
            case CHARACTER_VARYING:
                return FieldType.builder().dataType(DataType.VARCHAR).length(columnSize).build();
            case CLOB:
                return FieldType.builder().dataType(DataType.LONGTEXT).length(columnSize).build();
            case VARBINARY:
                return FieldType.builder().dataType(DataType.BINARY).length(columnSize).build();
            case BOOLEAN:
                return FieldType.builder().dataType(DataType.BOOLEAN).build();
            case TINYINT:
                return FieldType.builder().dataType(DataType.TINYINT).build();
            case SMALLINT:
                return FieldType.builder().dataType(DataType.SMALLINT).build();
            case INTEGER:
                return FieldType.builder().dataType(DataType.INTEGER).build();
            case BIGINT:
                return FieldType.builder().dataType(DataType.BIGINT).build();
            case DECIMAL:
                return FieldType.builder().dataType(DataType.DECIMAL).length(columnSize).scale(decimalDigits).build();
            case REAL:
                return FieldType.builder().dataType(DataType.REAL).build();
            case DOUBLE:
                return FieldType.builder().dataType(DataType.DOUBLE).build();
            case DATE:
                return FieldType.builder().dataType(DataType.DATE).build();
            case TIME:
                return FieldType.builder().dataType(DataType.TIME).scale(decimalDigits).build();
            case TIMESTAMP:
                return FieldType.builder().dataType(DataType.TIMESTAMP).scale(decimalDigits).build();
            case TIMESTAMP_WITH_TIME_ZONE:
                return FieldType.builder().dataType(DataType.TIMESTAMP_TZ).scale(decimalDigits).build();
            case JSON:
                return FieldType.builder().dataType(DataType.JSON).build();
            default:
                throw new IllegalArgumentException("Unexpected values: JDBC TYPE_NAME " + typeName + ", JDBC DATA_TYPE: " + dataType);
        }
    }
}
