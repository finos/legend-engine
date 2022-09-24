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

package org.finos.legend.engine.testable.persistence.mapper;

import org.finos.legend.engine.persistence.components.logicalplan.datasets.DataType;
import org.finos.legend.engine.persistence.components.logicalplan.datasets.FieldType;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldTypeMapper
{
    static String SQL_DATA_TYPE_REGEX = "(.*?)\\((.*?)\\)";

    public static FieldType from(String sqlDataType) throws Exception
    {
        Matcher m = Pattern.compile(SQL_DATA_TYPE_REGEX).matcher(sqlDataType);

        String dataType = sqlDataType.trim();
        String lengthPart = "";
        while (m.find())
        {
            String group1 = m.group(1);
            if (StringUtils.isNotEmpty(group1))
            {
                dataType = group1.trim();
            }
            String group2 = m.group(2);
            if (StringUtils.isNotEmpty(group2))
            {
                lengthPart = group2.trim();
            }
        }
        int length = -1;
        int scale = -1;

        if (StringUtils.isNotEmpty(lengthPart))
        {
            String[] splits = lengthPart.split(",");
            length = Integer.parseInt(splits[0]);
            if (splits.length == 2)
            {
                scale = Integer.parseInt(splits[1]);
            }
        }

        DataType type = getDataType(dataType);
        FieldType fieldType =  FieldType.builder().dataType(type).build();
        if (length != -1)
        {
            fieldType.withLength(length);
        }
        if (scale != -1)
        {
            fieldType.withScale(scale);
        }
        return fieldType;
    }

    private static DataType getDataType(String dataType)
    {
        switch (dataType.toUpperCase())
        {
            case "INT":
                return DataType.INT;
            case "INTEGER":
                return DataType.INTEGER;
            case "FLOAT":
                return DataType.FLOAT;
            case "VARCHAR":
                return DataType.VARCHAR;
            case "CHAR":
                return DataType.CHAR;
            case "DECIMAL":
                return DataType.DECIMAL;
            case "TIMESTAMP":
                return DataType.DATETIME;
            case "DATE":
                return DataType.DATE;
            case "BIGINT":
                return DataType.BIGINT;
            case "SMALLINT":
                return DataType.SMALLINT;
            case "TINYINT":
                return DataType.TINYINT;
            case "DOUBLE":
                return DataType.DOUBLE;
            case "NUMERIC":
                return DataType.NUMERIC;
            case "BIT":
                return DataType.BIT;
            case "BINARY":
                return DataType.BINARY;
            case "REAL":
                return DataType.REAL;
            case "VARBINARY":
                return DataType.VARBINARY;
            default:
                return DataType.STRING;
        }
    }
}
