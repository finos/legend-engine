// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.relational.postgres.sql;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.relational.postgres.sqldom.schema.ByteArray;
import org.finos.legend.engine.persistence.components.relational.postgres.sqldom.schema.DoublePrecision;
import org.finos.legend.engine.persistence.components.relational.postgres.sqldom.schema.TimestampWithTimezone;
import org.finos.legend.engine.persistence.components.relational.sql.DataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.BigInt;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Bit;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Boolean;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Char;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DataType;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Date;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Integer;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Json;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Numeric;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Real;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.SmallInt;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Text;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Time;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Timestamp;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.VarChar;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.VariableSizeDataType;

public class PostgresDataTypeMapping implements DataTypeMapping
{
    public DataType getDataType(FieldType type)
    {
        VariableSizeDataType dataType;
        switch (type.dataType())
        {
            case INT:
            case INTEGER:
                dataType = new Integer();
                break;
            case BIGINT:
                dataType = new BigInt();
                break;
            case SMALLINT:
                dataType = new SmallInt();
                break;
            case CHAR:
                dataType = new Char();
                type.length().ifPresent(dataType::setLength);
                break;
            case VARCHAR:
            case STRING:
                dataType = new VarChar();
                type.length().ifPresent(dataType::setLength);
                break;
            case TEXT:
                dataType = new Text();
                break;
            case TIMESTAMP:
            case TIMESTAMP_NTZ:
            case DATETIME:
                dataType = new Timestamp();
                type.length().ifPresent(dataType::setLength);
                break;
            case TIMESTAMP_TZ:
                dataType = new TimestampWithTimezone();
                type.length().ifPresent(dataType::setLength);
                break;
            case DATE:
                dataType = new Date();
                break;
            case TIME:
                dataType = new Time();
                type.length().ifPresent(dataType::setLength);
                break;
            case REAL:
                dataType = new Real();
                break;
            case FLOAT:
            case DOUBLE:
                dataType = new DoublePrecision();
                break;
            case BINARY:
            case BYTES:
                dataType = new ByteArray();
                break;
            case BIT:
                dataType = new Bit();
                type.length().ifPresent(dataType::setLength);
                break;
            case DECIMAL:
            case NUMERIC:
                dataType = new Numeric();
                type.length().ifPresent(dataType::setLength);
                type.scale().ifPresent(dataType::setScale);
                break;
            case BOOLEAN:
                dataType = new Boolean();
                break;
            case VARIANT:
            case JSON:
                dataType = new Json();
                break;
            case LONGTEXT:
            case TINYINT:
            case LONGVARCHAR:
            case VARBINARY:
            case LONGVARBINARY:
            case TIMESTAMP_LTZ:
            case MAP:
            case ARRAY:
            default:
                throw new IllegalArgumentException("Unexpected value: " + type.dataType());
        }

        return dataType;
    }
}
