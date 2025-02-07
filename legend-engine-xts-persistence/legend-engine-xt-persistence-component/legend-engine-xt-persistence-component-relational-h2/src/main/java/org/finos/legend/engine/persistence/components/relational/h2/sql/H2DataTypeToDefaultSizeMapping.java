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

package org.finos.legend.engine.persistence.components.relational.h2.sql;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.relational.sql.DataTypeToDefaultSizeMapping;

import java.util.Optional;

public class H2DataTypeToDefaultSizeMapping implements DataTypeToDefaultSizeMapping
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
            case DATE:
            case REAL:
            case FLOAT:
            case DOUBLE:
            case BOOLEAN:
            case VARIANT:
            case JSON:
                return Optional.empty();
            case CHAR:
                return Optional.of(1);
            case VARCHAR:
            case STRING:
            case LONGTEXT:
                return Optional.of(1000000000);
            case TIMESTAMP:
            case DATETIME:
            case TIMESTAMP_TZ:
                return Optional.of(6);
            case DECIMAL:
            case NUMERIC:
                return Optional.of(100000);
            case BINARY:
                return Optional.of(1);
            case TIME:
                return Optional.of(0);
            case VARBINARY:
            case LONGVARCHAR:
            case BIT:
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
            case INT:
            case INTEGER:
            case BIGINT:
            case TINYINT:
            case SMALLINT:
            case DATE:
            case REAL:
            case FLOAT:
            case DOUBLE:
            case BOOLEAN:
            case VARIANT:
            case JSON:
            case CHAR:
            case VARCHAR:
            case STRING:
            case LONGTEXT:
            case TIMESTAMP:
            case DATETIME:
            case TIMESTAMP_TZ:
            case BINARY:
            case TIME:
                return Optional.empty();
            case DECIMAL:
            case NUMERIC:
                return Optional.of(0);
            case VARBINARY:
            case LONGVARCHAR:
            case BIT:
            case LONGVARBINARY:
            default:
                throw new IllegalArgumentException("Unexpected value: " + type);
        }
    }
}
