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

package org.finos.legend.engine.persistence.components.relational.sql;

import org.finos.legend.engine.persistence.components.executor.TypeMapping;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;

public interface JdbcPropertiesToLogicalDataTypeMapping extends TypeMapping
{
    String NUMBER = "NUMBER";
    String TINYINT = "TINYINT";
    String SMALLINT = "SMALLINT";
    String INTEGER = "INTEGER";
    String BIGINT = "BIGINT";
    String INT4 = "INT4";
    String DECIMAL = "DECIMAL";
    String DOUBLE = "DOUBLE";
    String REAL = "REAL";
    String CHAR = "CHAR";
    String VARCHAR = "VARCHAR";
    String CLOB = "CLOB";
    String BINARY = "BINARY";
    String VARBINARY = "VARBINARY";
    String BOOLEAN = "BOOLEAN";
    String DATE = "DATE";
    String TIME = "TIME";
    String TIMESTAMP = "TIMESTAMP";
    String TIMESTAMPNTZ = "TIMESTAMPNTZ";
    String TIMESTAMPLTZ = "TIMESTAMPLTZ";
    String TIMESTAMPTZ = "TIMESTAMPTZ";
    String TIMESTAMP_WITH_TIME_ZONE = "TIMESTAMP WITH TIME ZONE";
    String VARIANT = "VARIANT";
    String OBJECT = "OBJECT";
    String ARRAY = "ARRAY";
    String CHARACTER_VARYING = "CHARACTER VARYING";
    String JSON = "JSON";

    FieldType getDataType(String typeName, String dataType, Integer columnSize, Integer decimalDigits);
}
