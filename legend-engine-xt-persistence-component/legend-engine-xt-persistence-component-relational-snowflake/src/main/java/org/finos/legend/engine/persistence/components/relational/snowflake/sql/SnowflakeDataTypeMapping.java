// Copyright 2022 Goldman Sachs
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

import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schema.Array;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schema.Object;
import org.finos.legend.engine.persistence.components.relational.snowflake.sqldom.schema.Variant;
import org.finos.legend.engine.persistence.components.relational.sql.DataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.BigInt;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Binary;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Boolean;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Char;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DataType;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Date;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DateTime;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Double;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Integer;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Number;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.SmallInt;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Time;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Timestamp;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.TimestampWithLocalTimezone;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.TimestampWithNoTimeZone;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.TimestampWithTimezone;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.TinyInt;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.VarChar;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.VariableSizeDataType;

public class SnowflakeDataTypeMapping implements DataTypeMapping
{

    // Reference: https://docs.snowflake.com/en/sql-reference/data-types.html

    public DataType getDataType(FieldType type)
    {
        VariableSizeDataType dataType;
        switch (type.dataType())
        {
            // Numeric Data Types
            case INT:
            case INTEGER:
                dataType = new Integer();
                break;
            case BIGINT:
                dataType = new BigInt();
                break;
            case TINYINT:
                dataType = new TinyInt();
                break;
            case SMALLINT:
                dataType = new SmallInt();
                break;
            case NUMERIC:
            case NUMBER:
            case DECIMAL:
                dataType = new Number(38, 0);
                type.length().ifPresent(dataType::setLength);
                type.scale().ifPresent(dataType::setScale);
                break;
            case REAL:
            case FLOAT:
            case DOUBLE:
                dataType = new Double();
                break;
            // String & Binary types
            case CHAR:
            case CHARACTER:
                dataType = new Char();
                type.length().ifPresent(dataType::setLength);
                break;
            case VARCHAR:
            case STRING:
            case TEXT:
                dataType = new VarChar();
                type.length().ifPresent(dataType::setLength);
                break;
            case BINARY:
            case VARBINARY:
                dataType = new Binary();
                type.length().ifPresent(dataType::setLength);
                break;
            // Other types
            case BOOLEAN:
                dataType = new Boolean();
                break;
            case DATE:
                dataType = new Date();
                break;
            case TIME:
                dataType = new Time();
                type.scale().ifPresent(dataType::setScale);
                break;
            case DATETIME:
                dataType = new DateTime();
                type.scale().ifPresent(dataType::setScale);
                break;
            case TIMESTAMP:
                dataType = new Timestamp();
                type.scale().ifPresent(dataType::setScale);
                break;
            case TIMESTAMP_NTZ:
                dataType = new TimestampWithNoTimeZone();
                type.scale().ifPresent(dataType::setScale);
                break;
            case TIMESTAMP_TZ:
                dataType = new TimestampWithTimezone();
                type.scale().ifPresent(dataType::setScale);
                break;
            case TIMESTAMP_LTZ:
                dataType = new TimestampWithLocalTimezone();
                type.scale().ifPresent(dataType::setScale);
                break;
            case JSON:
            case VARIANT:
                dataType = new Variant();
                break;
            case ARRAY:
                dataType = new Array();
                break;
            case MAP:
                dataType = new Object();
                break;
            default:
                throw new IllegalArgumentException("Unexpected value: " + type.dataType());
        }

        return dataType;
    }
}
