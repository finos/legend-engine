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

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.finos.legend.engine.persistence.components.BaseTestUtils.colBigint;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colBinary;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colBoolean;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colChar;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colDate;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colDatetime;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colDecimal;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colDouble;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colFloat;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colInt;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colInteger;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colNumeric;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colReal;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colSmallint;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colString;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colTime;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colTimestamp;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colTinyint;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colVarBinary;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colVarchar;

public class DataTypeMappingTest
{

    @Test
    void testDataType() throws SqlDomException
    {
        Assertions.assertEquals("INTEGER", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colInt.type())));
        Assertions.assertEquals("INTEGER", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colInteger.type())));
        Assertions.assertEquals("BIGINT", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colBigint.type())));
        Assertions.assertEquals("TINYINT", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colTinyint.type())));
        Assertions.assertEquals("SMALLINT", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colSmallint.type())));
        Assertions.assertEquals("CHAR", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colChar.type())));
        Assertions.assertEquals("VARCHAR", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colVarchar.type())));
        Assertions.assertEquals("VARCHAR", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colString.type())));
        Assertions.assertEquals("TIMESTAMP", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colTimestamp.type())));
        Assertions.assertEquals("DATETIME", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colDatetime.type())));
        Assertions.assertEquals("DATE", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colDate.type())));
        Assertions.assertEquals("DOUBLE", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colReal.type())));
        Assertions.assertEquals("DOUBLE", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colFloat.type())));
        Assertions.assertEquals("NUMBER(10,4)", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colDecimal.type())));
        Assertions.assertEquals("DOUBLE", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colDouble.type())));
        Assertions.assertEquals("BINARY", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colBinary.type())));
        Assertions.assertEquals("TIME", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colTime.type())));
        Assertions.assertEquals("NUMBER(38,0)", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colNumeric.type())));
        Assertions.assertEquals("BOOLEAN", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colBoolean.type())));
        Assertions.assertEquals("BINARY(10)", getGeneratedSql(new SnowflakeDataTypeMapping().getDataType(colVarBinary.type())));
    }

    private String getGeneratedSql(SqlGen sqlGen) throws SqlDomException
    {
        StringBuilder builder = new StringBuilder();
        sqlGen.genSql(builder);
        return builder.toString();
    }


}
