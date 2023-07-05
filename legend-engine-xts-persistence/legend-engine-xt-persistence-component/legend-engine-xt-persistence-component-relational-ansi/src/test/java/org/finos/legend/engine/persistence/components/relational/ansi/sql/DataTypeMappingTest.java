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

package org.finos.legend.engine.persistence.components.relational.ansi.sql;

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colBigint;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colBinary;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colBit;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colBool;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colBoolean;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colChar;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colDate;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colDatetime;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colDecimal;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colDouble;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colFloat;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colInt;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colInt64;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colInteger;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colLongtext;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colNumeric;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colReal;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colSmallint;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colString;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colTime;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colTimestamp;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colTinyint;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colVarBinary;
import static org.finos.legend.engine.persistence.components.logicalplan.operations.BaseTestUtils.colVarchar;

public class DataTypeMappingTest
{

    @Test
    void testDataType() throws SqlDomException
    {
        Assertions.assertEquals("INTEGER", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colInt.type())));
        Assertions.assertEquals("INTEGER", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colInteger.type())));
        Assertions.assertEquals("BIGINT", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colBigint.type())));
        Assertions.assertEquals("TINYINT", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colTinyint.type())));
        Assertions.assertEquals("SMALLINT", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colSmallint.type())));
        Assertions.assertEquals("CHAR", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colChar.type())));
        Assertions.assertEquals("VARCHAR", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colVarchar.type())));
        Assertions.assertEquals("VARCHAR", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colString.type())));
        Assertions.assertEquals("TIMESTAMP", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colTimestamp.type())));
        Assertions.assertEquals("DATETIME", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colDatetime.type())));
        Assertions.assertEquals("DATE", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colDate.type())));
        Assertions.assertEquals("REAL", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colReal.type())));
        Assertions.assertEquals("REAL", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colFloat.type())));
        Assertions.assertEquals("DECIMAL(10,4)", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colDecimal.type())));
        Assertions.assertEquals("DOUBLE", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colDouble.type())));
        Assertions.assertEquals("BINARY", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colBinary.type())));
        Assertions.assertEquals("TIME", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colTime.type())));
        Assertions.assertEquals("NUMERIC", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colNumeric.type())));
        Assertions.assertEquals("LONGTEXT", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colLongtext.type())));
        Assertions.assertEquals("BOOLEAN", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colBool.type())));
        Assertions.assertEquals("BOOLEAN", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colBoolean.type())));
        Assertions.assertEquals("VARBINARY(10)", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colVarBinary.type())));
        Assertions.assertEquals("INT64", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colInt64.type())));
        Assertions.assertEquals("BIT", getGeneratedSql(new AnsiDatatypeMapping().getDataType(colBit.type())));

    }

    private String getGeneratedSql(SqlGen sqlGen) throws SqlDomException
    {
        StringBuilder builder = new StringBuilder();
        sqlGen.genSql(builder);
        return builder.toString();
    }
}
