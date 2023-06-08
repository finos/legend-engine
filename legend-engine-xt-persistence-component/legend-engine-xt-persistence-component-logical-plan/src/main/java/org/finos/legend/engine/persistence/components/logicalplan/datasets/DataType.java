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

package org.finos.legend.engine.persistence.components.logicalplan.datasets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public enum DataType
{
    INT,
    INTEGER,
    BIGINT,
    TINYINT,
    SMALLINT,
    CHAR,
    CHARACTER,
    VARCHAR,
    TIMESTAMP,
    TIMESTAMP_NTZ,
    TIMESTAMP_TZ,
    TIMESTAMP_LTZ,
    DATETIME,
    TIMESTAMPTZ,
    DATE,
    REAL,
    DECIMAL,
    FLOAT,
    DOUBLE,
    BIT,
    BINARY,
    VARBINARY,
    NUMBER,
    NUMERIC,
    LONGVARCHAR,
    TIME,
    LONGVARBINARY,
    NULL,
    BOOLEAN,
    NCHAR,
    NVARCHAR,
    LONGNVARCHAR,
    UNDEFINED,
    INT64,
    FLOAT64,
    BYTES,
    STRING,
    BOOL,
    LONGTEXT,
    TEXT,
    JSON,
    VARIANT,
    MAP,
    ARRAY;

    public static boolean isStringDatatype(DataType type)
    {
        List<DataType> stringDatatype = new ArrayList<>(Arrays.asList(CHAR, CHARACTER, VARCHAR, LONGVARCHAR, NCHAR, NVARCHAR, LONGNVARCHAR, LONGTEXT, TEXT, JSON, STRING));
        return stringDatatype.contains(type);
    }

    public static Set<DataType> getComparableDataTypes()
    {
        return new HashSet<>(Arrays.asList(INT, INTEGER, BIGINT, TINYINT, SMALLINT, INT64, FLOAT64, REAL, DECIMAL, FLOAT, DOUBLE, NUMBER, NUMERIC,
            TIME, TIMESTAMP, TIMESTAMP_NTZ, TIMESTAMP_TZ, TIMESTAMP_LTZ, DATETIME, TIMESTAMPTZ, DATE));
    }
}
