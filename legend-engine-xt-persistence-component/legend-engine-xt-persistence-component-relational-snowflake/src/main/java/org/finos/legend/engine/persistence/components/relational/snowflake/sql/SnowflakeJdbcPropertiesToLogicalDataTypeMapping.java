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
import org.finos.legend.engine.persistence.components.relational.sql.JdbcPropertiesToLogicalDataTypeMapping;

public class SnowflakeJdbcPropertiesToLogicalDataTypeMapping implements JdbcPropertiesToLogicalDataTypeMapping
{

    // Reference: https://docs.snowflake.com/en/sql-reference/data-types.html

    public DataType getDataType(String typeName, String dataType)
    {
        switch (typeName)
        {
            case NUMBER:
                switch (dataType)
                {
                    case BIGINT:
                        return DataType.BIGINT;
                    case DECIMAL:
                        return DataType.DECIMAL;
                    default:
                        throw new IllegalArgumentException("Unexpected values: JDBC TYPE_NAME " + typeName + ", JDBC DATA_TYPE: " + dataType);
                }
            case DOUBLE:
                return DataType.DOUBLE;
            case VARCHAR:
                return DataType.VARCHAR;
            case BINARY:
                return DataType.BINARY;
            case BOOLEAN:
                return DataType.BOOLEAN;
            case DATE:
                return DataType.DATE;
            case TIME:
                return DataType.TIME;
            case TIMESTAMPNTZ:
                return DataType.TIMESTAMP;
            case TIMESTAMPLTZ:
                return DataType.TIMESTAMP_LTZ;
            case TIMESTAMPTZ:
                return DataType.TIMESTAMP_TZ;
            case VARIANT:
                return DataType.VARIANT;
            case OBJECT:
                return DataType.MAP;
            case ARRAY:
                return DataType.ARRAY;
            default:
                throw new IllegalArgumentException("Unexpected values: JDBC TYPE_NAME " + typeName + ", JDBC DATA_TYPE: " + dataType);
        }
    }
}
