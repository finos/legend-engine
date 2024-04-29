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
import org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.schema.Bool;
import org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.schema.Bytes;
import org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.schema.Float64;
import org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.schema.Json;
import org.finos.legend.engine.persistence.components.relational.bigquery.sqldom.schema.String;
import org.finos.legend.engine.persistence.components.relational.sql.DataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DataType;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Date;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DateTime;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Int64;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Numeric;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Time;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Timestamp;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.VariableSizeDataType;


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
                dataType = new Int64();
                break;
            case NUMERIC:
            case DECIMAL:
                dataType = new Numeric();
                type.length().ifPresent(dataType::setLength);
                type.scale().ifPresent(dataType::setScale);
                break;
            case REAL:
            case FLOAT:
            case DOUBLE:
                dataType = new Float64();
                break;
            // String & Binary types
            case CHAR:
            case VARCHAR:
            case LONGVARCHAR:
            case LONGTEXT:
            case TEXT:
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
            case TIMESTAMP_NTZ:
            case DATETIME:
                dataType = new DateTime();
                break;
            case TIMESTAMP:
            case TIMESTAMP_TZ:
                dataType = new Timestamp();
                break;
            // Other types
            case BOOLEAN:
                dataType = new Bool();
                break;
            case VARIANT:
            case JSON:
                dataType = new Json();
                break;
            case BIT:
            case TIMESTAMP_LTZ:
            case MAP:
            case ARRAY:
            default:
                throw new IllegalArgumentException("Unexpected value: " + type.dataType());
        }

        return dataType;
    }
}
