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

package org.finos.legend.engine.persistence.components.relational.ansi.sql;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.relational.sql.DataTypeToDefaultSizeMapping;

import java.util.Optional;

public class AnsiDatatypeToDefaultSizeMapping implements DataTypeToDefaultSizeMapping
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
            case CHAR:
            case VARCHAR:
            case STRING:
            case TIMESTAMP:
            case DATETIME:
            case TIMESTAMP_TZ:
            case DATE:
            case REAL:
            case FLOAT:
            case DECIMAL:
            case DOUBLE:
            case BINARY:
            case TIME:
            case NUMERIC:
            case LONGTEXT:
            case BOOLEAN:
            case VARBINARY:
            case BIT:
            case JSON:
                return Optional.empty();
            case LONGVARCHAR:
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
            case CHAR:
            case VARCHAR:
            case STRING:
            case TIMESTAMP:
            case DATETIME:
            case TIMESTAMP_TZ:
            case DATE:
            case REAL:
            case FLOAT:
            case DECIMAL:
            case DOUBLE:
            case BINARY:
            case TIME:
            case NUMERIC:
            case LONGTEXT:
            case BOOLEAN:
            case VARBINARY:
            case BIT:
            case JSON:
                return Optional.empty();
            case LONGVARCHAR:
            case LONGVARBINARY:
            default:
                throw new IllegalArgumentException("Unexpected value: " + type);
        }
    }
}
