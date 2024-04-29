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

package org.finos.legend.engine.persistence.components.relational.h2.sql;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.relational.sql.DataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.BigInt;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Binary;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Boolean;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Char;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DataType;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Date;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Decimal;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Double;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Integer;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.LongText;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Numeric;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Real;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.SmallInt;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Time;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Timestamp;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.TimestampWithTimezone;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.TinyInt;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.VarChar;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Json;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.VariableSizeDataType;

public class H2DataTypeMapping implements DataTypeMapping
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
            case TINYINT:
                dataType = new TinyInt();
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
            case TIMESTAMP:
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
            case REAL:
            case FLOAT:
                dataType = new Real();
                break;
            case DECIMAL:
                dataType = new Decimal(type.length().orElse(-1), type.scale().orElse(-1));
                type.length().ifPresent(dataType::setLength);
                type.scale().ifPresent(dataType::setScale);
                break;
            case DOUBLE:
                dataType = new Double();
                break;
            case BINARY:
                dataType = new Binary();
                type.length().ifPresent(dataType::setLength);
                break;
            case TIME:
                dataType = new Time();
                type.length().ifPresent(dataType::setLength);
                break;
            case NUMERIC:
                dataType = new Numeric();
                type.length().ifPresent(dataType::setLength);
                type.scale().ifPresent(dataType::setScale);
                break;
            case LONGTEXT:
                dataType = new LongText();
                type.length().ifPresent(dataType::setLength);
                break;
            case BOOLEAN:
                dataType = new Boolean();
                break;
            case VARIANT:
            case JSON:
                dataType = new Json();
                break;
            case VARBINARY:
            case LONGVARCHAR:
            case BIT:
            case LONGVARBINARY:
            default:
                throw new IllegalArgumentException("Unexpected value: " + type.dataType());
        }

        return dataType;
    }
}
