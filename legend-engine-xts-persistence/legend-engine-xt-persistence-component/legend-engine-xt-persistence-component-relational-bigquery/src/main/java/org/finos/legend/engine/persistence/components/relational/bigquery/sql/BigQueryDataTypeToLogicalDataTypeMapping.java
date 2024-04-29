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

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.finos.legend.engine.persistence.components.relational.sql.JdbcPropertiesToLogicalDataTypeMapping;

public class BigQueryDataTypeToLogicalDataTypeMapping implements JdbcPropertiesToLogicalDataTypeMapping
{
    private static final String INT64 = "INT64";
    private static final String INTEGER = "INTEGER";
    private static final String NUMERIC = "NUMERIC";
    private static final String FLOAT64 = "FLOAT64";
    private static final String FLOAT = "FLOAT";
    private static final String STRING = "STRING";
    private static final String BYTES = "BYTES";
    private static final String BOOL = "BOOL";
    private static final String BOOLEAN = "BOOLEAN";
    private static final String DATE = "DATE";
    private static final String TIME = "TIME";
    private static final String DATETIME = "DATETIME";
    private static final String TIMESTAMP = "TIMESTAMP";
    private static final String JSON = "JSON";

    public FieldType getDataType(String typeName, String dataType, Integer columnSize, Integer decimalDigits)
    {
        switch (typeName)
        {
            case INT64:
            case INTEGER:
                return FieldType.builder().dataType(DataType.INTEGER).build();
            case NUMERIC:
                return FieldType.builder().dataType(DataType.NUMERIC).length(columnSize).scale(decimalDigits).build();
            case FLOAT64:
            case FLOAT:
                return FieldType.builder().dataType(DataType.FLOAT).build();
            case STRING:
                return FieldType.builder().dataType(DataType.STRING).length(columnSize).build();
            case BYTES:
                return FieldType.builder().dataType(DataType.BYTES).length(columnSize).build();
            case BOOL:
            case BOOLEAN:
                return FieldType.builder().dataType(DataType.BOOLEAN).build();
            case DATE:
                return FieldType.builder().dataType(DataType.DATE).build();
            case TIME:
                return FieldType.builder().dataType(DataType.TIME).build();
            case DATETIME:
                return FieldType.builder().dataType(DataType.DATETIME).build();
            case TIMESTAMP:
                return FieldType.builder().dataType(DataType.TIMESTAMP).build();
            case JSON:
                return FieldType.builder().dataType(DataType.JSON).build();
            default:
                throw new IllegalArgumentException("Unexpected values: JDBC TYPE_NAME " + typeName + ", JDBC DATA_TYPE: " + dataType);
        }
    }
}
