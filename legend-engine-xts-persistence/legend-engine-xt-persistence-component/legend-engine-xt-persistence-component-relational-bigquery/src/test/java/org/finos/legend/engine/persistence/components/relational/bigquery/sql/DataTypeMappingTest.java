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

import org.finos.legend.engine.persistence.components.relational.sqldom.SqlDomException;
import org.finos.legend.engine.persistence.components.relational.sqldom.SqlGen;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.finos.legend.engine.persistence.components.BaseTestUtils.colBigint;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colBinary;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colBoolean;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colBytes;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colBytesWithLength;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colChar;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colDate;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colDatetime;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colDecimal;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colDouble;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colFloat;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colInt;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colInteger;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colJson;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colLongVarBinary;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colLongVarchar;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colLongtext;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colNumeric;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colNumericWithPrecision;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colNumericWithScale;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colReal;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colSmallint;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colString;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colStringWithLength;
import static org.finos.legend.engine.persistence.components.BaseTestUtils.colText;
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
        Assertions.assertEquals("INT64", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colInt.type())));
        Assertions.assertEquals("INT64", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colInteger.type())));
        Assertions.assertEquals("INT64", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colBigint.type())));
        Assertions.assertEquals("INT64", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colTinyint.type())));
        Assertions.assertEquals("INT64", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colSmallint.type())));
        Assertions.assertEquals("NUMERIC", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colNumeric.type())));
        Assertions.assertEquals("NUMERIC", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colDecimal.type())));
        Assertions.assertEquals("NUMERIC(29)", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colNumericWithPrecision.type())));
        Assertions.assertEquals("NUMERIC(33,4)", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colNumericWithScale.type())));
        Assertions.assertEquals("FLOAT64", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colReal.type())));
        Assertions.assertEquals("FLOAT64", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colFloat.type())));
        Assertions.assertEquals("FLOAT64", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colDouble.type())));
        Assertions.assertEquals("STRING", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colChar.type())));
        Assertions.assertEquals("STRING", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colVarchar.type())));
        Assertions.assertEquals("STRING", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colLongVarchar.type())));
        Assertions.assertEquals("STRING", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colLongtext.type())));
        Assertions.assertEquals("STRING", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colText.type())));
        Assertions.assertEquals("STRING", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colString.type())));
        Assertions.assertEquals("STRING(16)", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colStringWithLength.type())));
        Assertions.assertEquals("BYTES", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colBinary.type())));
        Assertions.assertEquals("BYTES", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colVarBinary.type())));
        Assertions.assertEquals("BYTES", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colLongVarBinary.type())));
        Assertions.assertEquals("BYTES", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colBytes.type())));
        Assertions.assertEquals("BYTES(10)", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colBytesWithLength.type())));
        Assertions.assertEquals("DATE", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colDate.type())));
        Assertions.assertEquals("TIME", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colTime.type())));
        Assertions.assertEquals("DATETIME", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colDatetime.type())));
        Assertions.assertEquals("TIMESTAMP", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colTimestamp.type())));
        Assertions.assertEquals("BOOL", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colBoolean.type())));
        Assertions.assertEquals("JSON", getGeneratedSql(new BigQueryDataTypeMapping().getDataType(colJson.type())));
    }

    private String getGeneratedSql(SqlGen sqlGen) throws SqlDomException
    {
        StringBuilder builder = new StringBuilder();
        sqlGen.genSql(builder);
        return builder.toString();
    }


}
