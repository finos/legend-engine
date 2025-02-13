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

package org.finos.legend.engine.persistence.components.relational.snowflake.sql;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.relational.sql.DataTypeToDefaultSizeMapping;

import java.util.Optional;

public class SnowflakeDataTypeToDefaultSizeMapping implements DataTypeToDefaultSizeMapping
{
    @Override
    public Optional<Integer> getDefaultLength(DataType type)
    {
        switch (type)
        {
            case INT:
            case INTEGER:
            case BIGINT:
            case TINYINT:
            case SMALLINT:
            case REAL:
            case FLOAT:
            case DOUBLE:
            case BOOLEAN:
            case DATE:
            case JSON:
            case VARIANT:
            case ARRAY:
            case MAP:
                return Optional.empty();
            case NUMERIC:
            case DECIMAL:
                return Optional.of(38);
            case CHAR:
                return Optional.of(1);
            case VARCHAR:
            case STRING:
            case TEXT:
                return Optional.of(16777216);
            case BINARY:
            case VARBINARY:
                return Optional.of(8388608);
            case TIME:
            case DATETIME:
            case TIMESTAMP:
            case TIMESTAMP_NTZ:
            case TIMESTAMP_TZ:
            case TIMESTAMP_LTZ:
                return Optional.of(9);
            default:
                throw new IllegalArgumentException("Unexpected value: " + type);
        }
    }

    @Override
    public Optional<Integer> getDefaultScale(DataType type)
    {
        switch (type)
        {
            case INT:
            case INTEGER:
            case BIGINT:
            case TINYINT:
            case SMALLINT:
            case REAL:
            case FLOAT:
            case DOUBLE:
            case CHAR:
            case VARCHAR:
            case STRING:
            case TEXT:
            case BINARY:
            case VARBINARY:
            case BOOLEAN:
            case DATE:
            case TIME:
            case DATETIME:
            case TIMESTAMP:
            case TIMESTAMP_NTZ:
            case TIMESTAMP_TZ:
            case TIMESTAMP_LTZ:
            case JSON:
            case VARIANT:
            case ARRAY:
            case MAP:
                return Optional.empty();
            case NUMERIC:
            case DECIMAL:
                return Optional.of(0);
            default:
                throw new IllegalArgumentException("Unexpected value: " + type);
        }
    }
}
