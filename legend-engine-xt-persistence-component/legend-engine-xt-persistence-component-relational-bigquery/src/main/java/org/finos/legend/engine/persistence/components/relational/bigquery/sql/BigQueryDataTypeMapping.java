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

package org.finos.legend.engine.persistence.components.relational.bigquery.sql;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.schema.Bytes;
import org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.schema.Float;
import org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.schema.Json;
import org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.schema.String;
import org.finos.legend.engine.persistence.components.relational.sql.DataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.*;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Boolean;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Integer;

public class BigQueryDataTypeMapping implements DataTypeMapping
{
    public DataType getDataType(FieldType type)
    {
        VariableSizeDataType dataType;
        switch (type.dataType())
        {
            // Numeric Data Types
            case INT:
            case INTEGER:
            case BIGINT:
            case TINYINT:
            case SMALLINT:
            case INT64:
                dataType = new Integer();
                break;
            case NUMBER:
            case NUMERIC:
            case DECIMAL:
                dataType = new Numeric();
                type.length().ifPresent(dataType::setLength);
                type.scale().ifPresent(dataType::setScale);
                break;
            case REAL:
            case FLOAT:
            case DOUBLE:
            case FLOAT64:
                dataType = new Float();
                break;
            // String & Binary types
            case CHAR:
            case CHARACTER:
            case VARCHAR:
            case STRING:
                dataType = new String();
                type.length().ifPresent(dataType::setLength);
                break;
            case BINARY:
            case VARBINARY:
            case LONGVARBINARY:
            case BYTES:
                dataType = new Bytes();
                type.length().ifPresent(dataType::setLength);
                break;
            // Date & Time types
            case DATE:
                dataType = new Date();
                break;
            case TIME:
                dataType = new Time();
                break;
            case DATETIME:
                dataType = new DateTime();
                break;
            case TIMESTAMP:
                dataType = new Timestamp();
                break;
            // Other types
            case BOOLEAN:
            case BOOL:
                dataType = new Boolean();
                break;
            case JSON:
                dataType = new Json();
                break;
            case LONGTEXT:
            case TEXT:
            case LONGVARCHAR:
            case NCHAR:
            case NVARCHAR:
            case LONGNVARCHAR:
            case BIT:
            case TIMESTAMP_NTZ:
            case TIMESTAMP_TZ:
            case TIMESTAMP_LTZ:
            case TIMESTAMPTZ:
            case VARIANT:
            case MAP:
            case ARRAY:
            case NULL:
            case UNDEFINED:
            default:
                throw new IllegalArgumentException("Unexpected value: " + type.dataType());
        }

        return dataType;
    }
}
