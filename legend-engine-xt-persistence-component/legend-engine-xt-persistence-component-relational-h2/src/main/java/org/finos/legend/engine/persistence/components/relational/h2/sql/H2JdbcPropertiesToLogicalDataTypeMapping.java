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
import org.finos.legend.engine.persistence.components.relational.sql.JdbcPropertiesToLogicalDataTypeMapping;

public class H2JdbcPropertiesToLogicalDataTypeMapping implements JdbcPropertiesToLogicalDataTypeMapping
{
    public DataType getDataType(String typeName, String dataType)
    {
        switch (typeName)
        {
            case CHAR:
                return DataType.CHAR;
            case VARCHAR:
                return DataType.VARCHAR;
            case CLOB:
                return DataType.LONGTEXT;
            case VARBINARY:
                return DataType.BINARY;
            case BOOLEAN:
                return DataType.BOOLEAN;
            case TINYINT:
                return DataType.TINYINT;
            case SMALLINT:
                return DataType.SMALLINT;
            case INTEGER:
                return DataType.INTEGER;
            case BIGINT:
                return DataType.BIGINT;
            case DECIMAL:
                return DataType.DECIMAL;
            case REAL:
                return DataType.REAL;
            case DOUBLE:
                return DataType.DOUBLE;
            case DATE:
                return DataType.DATE;
            case TIME:
                return DataType.TIME;
            case TIMESTAMP:
                return DataType.TIMESTAMP;
            case TIMESTAMP_WITH_TIME_ZONE:
                return DataType.TIMESTAMPTZ;
            default:
                throw new IllegalArgumentException("Unexpected values: JDBC TYPE_NAME " + typeName + ", JDBC DATA_TYPE: " + dataType);
        }
    }
}
