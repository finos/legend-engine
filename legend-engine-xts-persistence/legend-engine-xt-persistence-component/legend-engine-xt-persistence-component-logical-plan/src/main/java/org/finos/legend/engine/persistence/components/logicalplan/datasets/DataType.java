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
    // Integer Types
    INT,
    INTEGER,
    BIGINT,
    TINYINT,
    SMALLINT,

    // Character Types
    CHAR,
    VARCHAR,
    STRING,
    LONGVARCHAR,
    TEXT,
    LONGTEXT,

    // Date & Time types
    TIMESTAMP,
    TIMESTAMP_NTZ,
    TIMESTAMP_TZ,
    TIMESTAMP_LTZ,
    DATETIME,
    DATE,
    TIME,

    // Number types
    NUMERIC,
    REAL,
    DECIMAL,
    FLOAT,
    DOUBLE,

    // Binary types
    BIT,
    BYTES,
    BINARY,
    VARBINARY,
    LONGVARBINARY,

    BOOLEAN,

    // Semi-structured types
    JSON,
    VARIANT,
    MAP,
    ARRAY;

    public static boolean isStringDatatype(DataType type)
    {
        List<DataType> stringDatatype = new ArrayList<>(Arrays.asList(CHAR, VARCHAR, LONGVARCHAR, LONGTEXT, TEXT, JSON, STRING));
        return stringDatatype.contains(type);
    }

    public static Set<DataType> getComparableDataTypes()
    {
        return new HashSet<>(Arrays.asList(INT, INTEGER, BIGINT, TINYINT, SMALLINT, REAL, DECIMAL, FLOAT, DOUBLE, NUMERIC,
            TIME, TIMESTAMP, TIMESTAMP_NTZ, TIMESTAMP_TZ, TIMESTAMP_LTZ, DATETIME, DATE));
    }
}
