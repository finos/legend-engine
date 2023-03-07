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

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;

public interface JdbcPropertiesToLogicalDataTypeMapping
{
    public static String NUMBER = "NUMBER";
    public static String TINYINT = "TINYINT";
    public static String SMALLINT = "SMALLINT";
    public static String INTEGER = "INTEGER";
    public static String BIGINT = "BIGINT";
    public static String DECIMAL = "DECIMAL";
    public static String DOUBLE = "DOUBLE";
    public static String REAL = "REAL";
    public static String CHAR = "CHAR";
    public static String VARCHAR = "VARCHAR";
    public static String CLOB = "CLOB";
    public static String BINARY = "BINARY";
    public static String VARBINARY = "VARBINARY";
    public static String BOOLEAN = "BOOLEAN";
    public static String DATE = "DATE";
    public static String TIME = "TIME";
    public static String TIMESTAMP = "TIMESTAMP";
    public static String TIMESTAMPNTZ = "TIMESTAMPNTZ";
    public static String TIMESTAMPLTZ = "TIMESTAMPLTZ";
    public static String TIMESTAMPTZ = "TIMESTAMPTZ";
    public static String TIMESTAMP_WITH_TIME_ZONE = "TIMESTAMP WITH TIME ZONE";
    public static String VARIANT = "VARIANT";
    public static String OBJECT = "OBJECT";
    public static String ARRAY = "ARRAY";

    DataType getDataType(String typeName, String dataType);
}
