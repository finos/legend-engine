//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.plan.execution.result;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.plan.dependencies.domain.date.PureDate;
import org.finos.legend.engine.plan.execution.result.builder.tds.TDSBuilder;
import org.finos.legend.engine.plan.execution.result.serialization.SerializationFormat;
import org.finos.legend.engine.protocol.pure.v1.model.executionPlan.result.TDSColumn;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TDSResultSerializationTest
{
    @Parameterized.Parameters(name = "SerializationFormat.{0}")
    public static Collection<Object[]> serializationFormatsToTest()
    {
        return Arrays.asList(new Object[][]
            {
                { SerializationFormat.DEFAULT,
                        "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"stringCol\",\"type\":\"String\",\"enumMapping\":{}},{\"name\":\"intCol\",\"type\":\"Integer\",\"enumMapping\":{}},{\"name\":\"floatCol\",\"type\":\"Float\",\"enumMapping\":{}},{\"name\":\"boolCol\",\"type\":\"Boolean\",\"enumMapping\":{}},{\"name\":\"decimalCol\",\"type\":\"Decimal\",\"enumMapping\":{}},{\"name\":\"strictDateCol\",\"type\":\"StrictDate\",\"enumMapping\":{}},{\"name\":\"dateTimeCol\",\"type\":\"DateTime\",\"enumMapping\":{}}]},\"result\":{\"columns\":[\"stringCol\",\"intCol\",\"floatCol\",\"boolCol\",\"decimalCol\",\"strictDateCol\",\"dateTimeCol\"],\"rows\":[{\"values\":[\"Hello\",2,1.23,true,2.345,\"2020-01-01\",\"2020-01-01T01:01:01Z\"]},{\"values\":[\"World\",3,2.345,false,3.456,\"2021-02-01\",\"2021-02-01T01:01:01Z\"]},{\"values\":[null,null,null,null,null,null,null]}]},\"activities\":[{\"activity\":\"abc\"}]}" },
                { SerializationFormat.RAW,
                        "{\"columns\":[{\"name\":\"stringCol\",\"type\":\"String\",\"enumMapping\":{}},{\"name\":\"intCol\",\"type\":\"Integer\",\"enumMapping\":{}},{\"name\":\"floatCol\",\"type\":\"Float\",\"enumMapping\":{}},{\"name\":\"boolCol\",\"type\":\"Boolean\",\"enumMapping\":{}},{\"name\":\"decimalCol\",\"type\":\"Decimal\",\"enumMapping\":{}},{\"name\":\"strictDateCol\",\"type\":\"StrictDate\",\"enumMapping\":{}},{\"name\":\"dateTimeCol\",\"type\":\"DateTime\",\"enumMapping\":{}}],\"rows\":[[\"Hello\",2,1.23,true,2.345,\"2020-01-01\",\"2020-01-01T01:01:01Z\"],[\"World\",3,2.345,false,3.456,\"2021-02-01\",\"2021-02-01T01:01:01Z\"],[null,null,null,null,null,null,null]],\"activities\":[{\"activity\":\"abc\"}]}" },
                { SerializationFormat.CSV,
                        "stringCol,intCol,floatCol,boolCol,decimalCol,strictDateCol,dateTimeCol\r\n" +
                        "Hello,2,1.23,true,2.345,2020-01-01,2020-01-01T01:01:01Z\r\n" +
                        "World,3,2.345,false,3.456,2021-02-01,2021-02-01T01:01:01Z\r\n" +
                        ",,,,,,\r\n" },
                { SerializationFormat.CSV_TRANSFORMED,
                        "stringCol,intCol,floatCol,boolCol,decimalCol,strictDateCol,dateTimeCol\r\n" +
                        "Hello,2,1.23,true,2.345,2020-01-01,2020-01-01T01:01:01Z\r\n" +
                        "World,3,2.345,false,3.456,2021-02-01,2021-02-01T01:01:01Z\r\n" +
                        ",,,,,,\r\n" },
                { SerializationFormat.PURE,
                        "{\"builder\":{\"_type\":\"tdsBuilder\",\"columns\":[{\"name\":\"stringCol\",\"type\":\"String\",\"enumMapping\":{}},{\"name\":\"intCol\",\"type\":\"Integer\",\"enumMapping\":{}},{\"name\":\"floatCol\",\"type\":\"Float\",\"enumMapping\":{}},{\"name\":\"boolCol\",\"type\":\"Boolean\",\"enumMapping\":{}},{\"name\":\"decimalCol\",\"type\":\"Decimal\",\"enumMapping\":{}},{\"name\":\"strictDateCol\",\"type\":\"StrictDate\",\"enumMapping\":{}},{\"name\":\"dateTimeCol\",\"type\":\"DateTime\",\"enumMapping\":{}}]},\"result\":{\"columns\":[\"stringCol\",\"intCol\",\"floatCol\",\"boolCol\",\"decimalCol\",\"strictDateCol\",\"dateTimeCol\"],\"rows\":[{\"values\":[\"Hello\",2,1.23,true,2.345,\"2020-01-01\",\"2020-01-01T01:01:01Z\"]},{\"values\":[\"World\",3,2.345,false,3.456,\"2021-02-01\",\"2021-02-01T01:01:01Z\"]},{\"values\":[null,null,null,null,null,null,null]}]},\"activities\":[{\"activity\":\"abc\"}]}" },
                { SerializationFormat.PURE_TDSOBJECT,
                        "[{\"stringCol\":\"Hello\",\"intCol\":2,\"floatCol\":1.23,\"boolCol\":true,\"decimalCol\":2.345,\"strictDateCol\":\"2020-01-01\",\"dateTimeCol\":\"2020-01-01T01:01:01Z\"},{\"stringCol\":\"World\",\"intCol\":3,\"floatCol\":2.345,\"boolCol\":false,\"decimalCol\":3.456,\"strictDateCol\":\"2021-02-01\",\"dateTimeCol\":\"2021-02-01T01:01:01Z\"},{\"stringCol\":null,\"intCol\":null,\"floatCol\":null,\"boolCol\":null,\"decimalCol\":null,\"strictDateCol\":null,\"dateTimeCol\":null}]" },
            }
        );
    }

    @Parameterized.Parameter(0)
    public SerializationFormat format;

    @Parameterized.Parameter(1)
    public String expected;

    @Test
    public void testSerialization() throws IOException
    {
        TDSResult tdsResult = getTdsResult();
        ByteArrayOutputStream targetStream = new ByteArrayOutputStream();
        tdsResult.getSerializer(format).stream(targetStream);
        Assert.assertEquals(expected, targetStream.toString());
    }

    private static TDSResult getTdsResult()
    {
        ExecutionActivity activity = new ExecutionActivity()
        {
            public final String activity = "abc";
        };
        activity._type = "testActivity";
        List<ExecutionActivity> activities = Lists.mutable.with(activity);

        List<TDSColumn> columns = Lists.mutable.with(
                new TDSColumn("stringCol", "String"),
                new TDSColumn("intCol", "Integer"),
                new TDSColumn("floatCol", "Float"),
                new TDSColumn("boolCol", "Boolean"),
                new TDSColumn("decimalCol", "Decimal"),
                new TDSColumn("strictDateCol", "StrictDate"),
                new TDSColumn("dateTimeCol", "DateTime")
        );

        List<Object> row = Lists.mutable.with(
                "Hello",
                2L,
                1.23,
                true,
                new BigDecimal("2.345"),
                PureDate.newPureDate(2020, 1, 1),
                PureDate.newPureDate(2020, 1, 1, 1, 1, 1)
        );

        List<Object> row2 = Lists.mutable.with(
                "World",
                3L,
                2.345,
                false,
                new BigDecimal("3.456"),
                PureDate.newPureDate(2021, 2, 1),
                PureDate.newPureDate(2021, 2, 1, 1, 1, 1)
        );

        List<Object> row3 = Lists.mutable.with(
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        return new TDSResult(
                Lists.mutable.with(
                        row.toArray(),
                        row2.toArray(),
                        row3.toArray()
                ).stream(),
                new TDSBuilder(columns),
                activities,
                null
        );
    }
}
