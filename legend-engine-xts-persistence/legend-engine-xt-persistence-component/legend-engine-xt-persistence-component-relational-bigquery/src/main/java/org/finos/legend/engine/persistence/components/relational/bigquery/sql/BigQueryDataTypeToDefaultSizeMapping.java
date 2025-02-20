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

package org.finos.legend.engine.persistence.components.relational.bigquery.sql;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.relational.sql.DataTypeToDefaultSizeMapping;

import java.util.Optional;

// TODO: schema evol - mapping not confirmed
public class BigQueryDataTypeToDefaultSizeMapping implements DataTypeToDefaultSizeMapping
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
            case NUMERIC:
            case DECIMAL:
            case REAL:
            case FLOAT:
            case DOUBLE:
            case CHAR:
            case VARCHAR:
            case LONGVARCHAR:
            case LONGTEXT:
            case TEXT:
            case STRING:
            case BINARY:
            case VARBINARY:
            case LONGVARBINARY:
            case BYTES:
            case DATE:
            case TIME:
            case TIMESTAMP_NTZ:
            case DATETIME:
            case TIMESTAMP:
            case TIMESTAMP_TZ:
            case BOOLEAN:
            case VARIANT:
            case JSON:
                return Optional.empty();
            case BIT:
            case TIMESTAMP_LTZ:
            case MAP:
            case ARRAY:
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
            case NUMERIC:
            case DECIMAL:
            case REAL:
            case FLOAT:
            case DOUBLE:
            case CHAR:
            case VARCHAR:
            case LONGVARCHAR:
            case LONGTEXT:
            case TEXT:
            case STRING:
            case BINARY:
            case VARBINARY:
            case LONGVARBINARY:
            case BYTES:
            case DATE:
            case TIME:
            case TIMESTAMP_NTZ:
            case DATETIME:
            case TIMESTAMP:
            case TIMESTAMP_TZ:
            case BOOLEAN:
            case VARIANT:
            case JSON:
                return Optional.empty();
            case BIT:
            case TIMESTAMP_LTZ:
            case MAP:
            case ARRAY:
            default:
                throw new IllegalArgumentException("Unexpected value: " + type);
        }
    }
}
