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

package org.finos.legend.engine.persistence.components.relational.memsql.sql;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.relational.memsql.sqldom.schema.Json;
import org.finos.legend.engine.persistence.components.relational.sql.DataTypeMapping;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.BigInt;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Char;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DataType;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Date;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.DateTime;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Decimal;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Double;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Integer;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.LongText;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.SmallInt;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Time;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.Timestamp;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.TinyInt;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.VarChar;
import org.finos.legend.engine.persistence.components.relational.sqldom.schema.VariableSizeDataType;

public class MemSqlDataTypeMapping implements DataTypeMapping
{
    public DataType getDataType(FieldType type)
    {
        VariableSizeDataType dataType;
        switch (type.dataType())
        {
            // Integer Number Data Types
            case BOOLEAN:
            case BOOL:
                dataType = new TinyInt(1);
                break;
            case TINYINT:
                dataType = new TinyInt();
                break;
            case SMALLINT:
                dataType = new SmallInt();
                break;
            case INT:
            case INTEGER:
                dataType = new Integer();
                break;
            case BIGINT:
                dataType = new BigInt();
                break;
            // Real Number Data Types
            case FLOAT:
            case DOUBLE:
            case REAL:
                dataType = new Double();
                break;
            case DECIMAL:
                dataType = new Decimal(type.length().orElse(-1), type.scale().orElse(-1));
                break;
            // Time and Date Types
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
            // String types
            case CHAR:
                dataType = new Char();
                break;
            case VARCHAR:
                dataType = new VarChar();
                if (!type.length().isPresent())
                {
                    type = type.withLength(256);
                }
                break;
            case LONGTEXT:
                dataType = new LongText();
                break;
            case JSON:
                dataType = new Json();
                break;
            default:
                throw new IllegalArgumentException("Unexpected value: " + type.dataType());
        }

        type.length().ifPresent(dataType::setLength);
        type.scale().ifPresent(dataType::setScale);

        return dataType;
    }
}
