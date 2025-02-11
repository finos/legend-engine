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

package org.finos.legend.engine.persistence.components.relational.duckdb.sql;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.relational.sql.DataTypeToDefaultSizeMapping;

import java.util.Optional;

// TODO: schema evol - mapping not confirmed
public class DuckDBDataTypeToDefaultSizeMapping implements DataTypeToDefaultSizeMapping
{
    @Override
    public Optional<Integer> getDefaultLength(DataType type)
    {
        switch (type)
        {
            case BIGINT:
            case BIT:
            case BINARY:
            case VARBINARY:
            case BOOLEAN:
            case DATE:
            case DECIMAL:
            case NUMERIC:
            case DOUBLE:
            case INT:
            case INTEGER:
            case REAL:
            case FLOAT:
            case SMALLINT:
            case TIME:
            case TIMESTAMP_TZ:
            case TIMESTAMP_NTZ:
            case TIMESTAMP:
            case DATETIME:
            case TINYINT:
            case VARCHAR:
            case CHAR:
            case STRING:
            case TEXT:
            case JSON:
            case VARIANT:
                return Optional.empty();
            case MAP:
            case ARRAY:
            case LONGVARCHAR:
            case LONGTEXT:
            case TIMESTAMP_LTZ:
            case BYTES:
            case LONGVARBINARY:
            default:
                throw new IllegalArgumentException("Unexpected value: " + type);
        }
    }

    @Override
    public Optional<Integer> getDefaultScale(DataType type)
    {
        switch (type)
        {
            case BIGINT:
            case BIT:
            case BINARY:
            case VARBINARY:
            case BOOLEAN:
            case DATE:
            case DECIMAL:
            case NUMERIC:
            case DOUBLE:
            case INT:
            case INTEGER:
            case REAL:
            case FLOAT:
            case SMALLINT:
            case TIME:
            case TIMESTAMP_TZ:
            case TIMESTAMP_NTZ:
            case TIMESTAMP:
            case DATETIME:
            case TINYINT:
            case VARCHAR:
            case CHAR:
            case STRING:
            case TEXT:
            case JSON:
            case VARIANT:
                return Optional.empty();
            case MAP:
            case ARRAY:
            case LONGVARCHAR:
            case LONGTEXT:
            case TIMESTAMP_LTZ:
            case BYTES:
            case LONGVARBINARY:
            default:
                throw new IllegalArgumentException("Unexpected value: " + type);
        }
    }
}
