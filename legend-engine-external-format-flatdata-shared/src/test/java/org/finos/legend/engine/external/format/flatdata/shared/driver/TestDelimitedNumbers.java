// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.shared.driver;

import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

public class TestDelimitedNumbers extends AbstractDriverTest
{
    @Test
    public void defaultInteger()
    {
        String ageGrammar = "INTEGER";
        runTestLong(0L, "", ageGrammar, "0");
        runTestLong(12345L, "", ageGrammar, "12345");
        runTestLong(-35L, "", ageGrammar, "-35");
        runTestLong(35L, "", ageGrammar, "+35");

        runTestDouble(0.0, "", ageGrammar, "0");
        runTestDouble(12345.0, "", ageGrammar, "12345");
        runTestDouble(-35.0, "", ageGrammar, "-35");
        runTestDouble(35.0, "", ageGrammar, "+35");

        runTestBigDecimal(BigDecimal.ZERO, "", ageGrammar, "0");
        runTestBigDecimal(new BigDecimal("12345"), "", ageGrammar, "12345");
        runTestBigDecimal(new BigDecimal("-35"), "", ageGrammar, "-35");
        runTestBigDecimal(new BigDecimal("35"), "", ageGrammar, "+35");

        runTestLongInvalid("Failed to read mandatory 'AGE' with value: 123456789012345678901234567890, error: ParseException Should be digits optionally preceded by '+' or '-'", "", ageGrammar, "123456789012345678901234567890");
        runTestDouble(123456789012345678901234567890.0, "", ageGrammar, "123456789012345678901234567890");
        runTestBigDecimal(new BigDecimal("123456789012345678901234567890"), "", ageGrammar, "123456789012345678901234567890");
    }

    @Test
    public void defaultDecimal()
    {
        String ageGrammar = "DECIMAL";
        runTestDouble(0.0, "", ageGrammar, "0");
        runTestDouble(11.75, "", ageGrammar, "11.75");
        runTestDouble(-23.5, "", ageGrammar, "-23.5");

        runTestBigDecimal(BigDecimal.ZERO, "", ageGrammar, "0");
        runTestBigDecimal(new BigDecimal("11.75"), "", ageGrammar, "11.75");
        runTestBigDecimal(new BigDecimal("-23"), "", ageGrammar, "-23");

        runTestLongInvalid("Failed to read mandatory 'AGE' with value: 0, error: ParseException Not a suitable numeric value", "", ageGrammar, "0");
    }

    @Test
    public void commasFailWithoutFormatString()
    {
        runTestLongInvalid("Failed to read mandatory 'AGE' with value: 12,345, error: ParseException Should be digits optionally preceded by '+' or '-'", "", "INTEGER", "12,345");
        runTestDoubleInvalid("Failed to read mandatory 'AGE' with value: 12,345, error: ParseException Should be an optionally signed simple floating point number or one in scientific notation", "", "DECIMAL", "12,345");
    }

    @Test
    public void commasWorkWithDefaultIntegerFormatString()
    {
        String properties = "defaultIntegerFormat: '#,##0';";
        runTestLong(12345L, properties, "INTEGER", "12,345");
        runTestLong(12345L, properties, "INTEGER", "12345");
    }

    @Test
    public void commasWorkWithDefaultDecimalFormatString()
    {
        String properties = "defaultDecimalFormat: '#,##0.0';";
        runTestDouble(12345.0, properties, "DECIMAL", "12,345");
        runTestDouble(12345.123, properties, "DECIMAL", "12,345.123");
        runTestDouble(12345.0, properties, "DECIMAL", "12345");

        runTestBigDecimal(new BigDecimal("12345"), properties, "DECIMAL", "12,345");
        runTestBigDecimal(new BigDecimal("12345.123"), properties, "DECIMAL", "12,345.123");
        runTestBigDecimal(new BigDecimal("12345"), properties, "DECIMAL", "12345");
    }

    @Test
    public void worksWithMissingValues()
    {
        runTestBigDecimal(null, "", "DECIMAL(optional)", "");
        runTestBigDecimal(null, "defaultDecimalFormat: '#,##0';", "DECIMAL(optional)", "");
    }

    private void runTestLong(long expected, String properties, String ageGrammar, String rawData)
    {
        FlatData flatData = flatData(properties, ageGrammar);
        List<IChecked<PersonWithLong>> records = deserialize(PersonWithLong.class, flatData, data("\n", "NAME|AGE", "Alex|" + rawData));

        Assert.assertEquals(1, records.size());
        IChecked<PersonWithLong> record = records.get(0);
        assertNoDefects(record);
        Assert.assertEquals("Alex", record.getValue().name);
        Assert.assertEquals(expected, record.getValue().age);
    }

    private void runTestDouble(double expected, String properties, String ageGrammar, String rawData)
    {
        FlatData flatData = flatData(properties, ageGrammar);
        List<IChecked<PersonWithDouble>> records = deserialize(PersonWithDouble.class, flatData, data("\n", "NAME|AGE", "Alex|" + rawData));

        Assert.assertEquals(1, records.size());
        IChecked<PersonWithDouble> record = records.get(0);
        assertNoDefects(record);
        Assert.assertEquals("Alex", record.getValue().name);
        Assert.assertEquals(expected, record.getValue().age, 0.00000001);
    }

    private void runTestBigDecimal(BigDecimal expected, String properties, String ageGrammar, String rawData)
    {
        FlatData flatData = flatData(properties, ageGrammar);
        List<IChecked<PersonWithBigDecimal>> records = deserialize(PersonWithBigDecimal.class, flatData, data("\n", "NAME|AGE", "Alex|" + rawData));

        Assert.assertEquals(1, records.size());
        IChecked<PersonWithBigDecimal> record = records.get(0);
        assertNoDefects(record);
        Assert.assertEquals("Alex", record.getValue().name);
        Assert.assertEquals(expected, record.getValue().age);
    }

    private void runTestLongInvalid(String expectedDefect, String properties, String employedGrammar, String rawData)
    {
        FlatData flatData = flatData(properties, employedGrammar);
        List<IChecked<PersonWithLong>> records = deserialize(PersonWithLong.class, flatData, data("\n", "NAME|AGE", "Alex|" + rawData));

        Assert.assertEquals(1, records.size());
        IChecked<PersonWithLong> record = records.get(0);
        assertHasDefect("Critical", expectedDefect, record);
    }

    private void runTestDoubleInvalid(String expectedDefect, String properties, String employedGrammar, String rawData)
    {
        FlatData flatData = flatData(properties, employedGrammar);
        List<IChecked<PersonWithDouble>> records = deserialize(PersonWithDouble.class, flatData, data("\n", "NAME|AGE", "Alex|" + rawData));

        Assert.assertEquals(1, records.size());
        IChecked<PersonWithDouble> record = records.get(0);
        assertHasDefect("Critical", expectedDefect, record);
    }

    private FlatData flatData(String properties, String ageGrammar)
    {
        return parseFlatData("section default: DelimitedWithHeadings\n" +
                                     "{\n" +
                                     "  scope.untilEof;\n" +
                                     "  delimiter       : '|';\n" +
                                     "  quoteChar       : '\"';\n" +
                                     "  nullString      : '';\n" +
                                     "  " + properties + "\n" +
                                     "\n" +
                                     "  Record\n" +
                                     "  {\n" +
                                     "    NAME : STRING;\n" +
                                     "    AGE  : " + ageGrammar + ";\n" +
                                     "  }\n" +
                                     "}\n");
    }

    public static class PersonWithLong
    {
        String name;
        long age;
    }

    public static class PersonWithDouble
    {
        String name;
        double age;
    }

    public static class PersonWithBigDecimal
    {
        String name;
        BigDecimal age;
    }
}
