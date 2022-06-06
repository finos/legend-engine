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

import java.time.Instant;
import java.util.List;

public class TestDelimitedDateTime extends AbstractDriverTest
{
    @Test
    public void dateTimeWithDefaultFormat()
    {
        runTest("2001-08-13T10:23:54.123Z",
                "DATETIME",
                "2001-08-13T10:23:54.123Z"
        );
    }

    @Test
    public void dateTimeWithSpecifiedFormatAndDefaultTimeZone()
    {
        runTest("2001-08-13T15:23:00Z",
                "DATETIME(format='dd/MM/yyyy HH:mm')",
                "13/08/2001 15:23"
        );
    }

    @Test
    public void dateWithSpecified2ndFormat()
    {
        String dataType = "DATETIME(optional, format=['dd/MM/yyyy HH:mm', 'dd MMM yyyy HH:mm:ss'])";
        runTest("2001-08-13T15:23:00Z", dataType, "13/08/2001 15:23");
        runTest("2001-08-13T15:23:15Z", dataType, "13 Aug 2001 15:23:15");
        runTest(new String[] {"2001-08-13T15:23:00Z", "1983-01-01T12:53:00Z"}, dataType, new String[] {"13/08/2001 15:23", "01/01/1983 12:53"});
        runTest(new String[] {null, "1983-01-01T12:53:00Z"}, dataType, new String[] {null, "01/01/1983 12:53"});
        runTestInvalid(new String[] {null, "Failed to read 'DOB' with value: 13 Aug 2001 15:23:15, error: ParseException Unparseable datetime: \"13 Aug 2001 15:23:15\" for format 'dd/MM/yyyy HH:mm'"},
                "",
                dataType,
                new String[] {"13/08/2001 15:23", "13 Aug 2001 15:23:15"}
        );
    }

    @Test
    public void dateWithSpecified3rdFormat()
    {
        String dataType = "DATETIME(optional, format=['dd/MM/yyyy HH:mm', 'dd MMM yyyy HH:mm:ss', 'yyyy-MM-dd\\'T\\'HH:mm:ss'])";
        runTest("2001-08-13T15:23:00Z", dataType, "13/08/2001 15:23");
        runTest("2001-08-13T15:23:15Z", dataType, "13 Aug 2001 15:23:15");
        runTest("2001-08-13T15:23:15Z", dataType, "2001-08-13T15:23:15");
        runTest(new String[] {"2001-08-13T15:23:00Z", "1983-01-01T15:23:00Z"}, dataType, new String[] {"13/08/2001 15:23", "01/01/1983 15:23"});
        runTest(new String[] {"2001-08-13T15:23:15Z", "1983-01-01T15:23:15Z"}, dataType, new String[] {"13 Aug 2001 15:23:15", "01 Jan 1983 15:23:15"});
        runTest(new String[] {"2001-08-13T15:23:15Z", "1983-01-01T15:23:15Z"}, dataType, new String[] {"2001-08-13T15:23:15", "1983-01-01T15:23:15"});
        runTest(new String[] {null, "1983-01-01T15:23:15Z"}, dataType, new String[] {null, "1983-01-01T15:23:15"});
        runTestInvalid(new String[] {null, "Failed to read 'DOB' with value: 13/08/2001 15:23, error: ParseException Unparseable datetime: \"13/08/2001 15:23\" for format 'yyyy-MM-dd'T'HH:mm:ss'"},
                "",
                dataType,
                new String[] {"1983-01-01T15:23:15", "13/08/2001 15:23"}
        );
    }

    @Test
    public void dateTimeWithSpecifiedFormatAndSpecifiedTimeZone()
    {
        runTest("2001-08-13T10:23:54.123Z",
                "DATETIME(format='yyyy-MM-dd\\'T\\'HH:mm:ss.SSS', timeZone='EST')",
                "2001-08-13T05:23:54.123"
        );
    }

    @Test
    public void dateTimeWithSpecifiedDefaultsForFormatAndTimeZone()
    {
        runTest("2001-08-13T10:23:54Z",
                "defaultDateTimeFormat: 'yyyy-MM-dd\\'T\\'HH:mm:ss';\ndefaultTimeZone: 'EST';\n",
                "DATETIME",
                "2001-08-13T05:23:54"
        );
    }

    @Test
    public void dateTimeWithOverriddenDefaultFormat()
    {
        runTest("2001-08-13T15:23:00Z",
                "defaultDateTimeFormat: 'yyyy-MM-dd\\'T\\'HH:mm:ss';\ndefaultTimeZone: 'EST';\n",
                "DATETIME(format='dd/MM/yyyy HH:mm')",
                "13/08/2001 10:23"
        );
    }

    @Test
    public void dateTimeWithOverridden2ndDefaultFormat()
    {
        String property = "defaultDateTimeFormat: ['MM/dd/yyyy HH:mm', 'dd MMM yyyy HH:mm:ss'];";
        runTest("2001-08-13T11:35:00Z", property, "DATETIME", "08/13/2001 11:35");
        runTest("2001-08-13T11:35:12Z", property, "DATETIME", "13 Aug 2001 11:35:12");
    }

    @Test
    public void multipleDefaultFormatsFailWhenColumnsContradict()
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter : ',';\n" +
                "  defaultDateTimeFormat: ['MM/dd/yyyy HH:mm', 'dd MMM yyyy HH:mm:ss'];\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    NAME : STRING;\n" +
                "    DOB  : DATETIME;\n" +
                "    ANOTHER : DATETIME;\n" +
                "  }\n" +
                "}\n"
        );

        String goodData = data("\n", "NAME,DOB,ANOTHER", "John,13 Aug 2001 12:54:59,01 Jan 1986 14:02:32");

        List<IChecked<Person>> goodRecords = deserialize(Person.class, flatData, goodData);
        Assert.assertEquals(1, goodRecords.size());
        assertNoDefects(goodRecords.get(0));

        String badData = data("\n", "NAME,DOB,ANOTHER", "John,13 Aug 2001 12:54:59,01/01/1986 19:48");
        List<IChecked<Person>> badRecords = deserialize(Person.class, flatData, badData);
        Assert.assertEquals(1, badRecords.size());
        assertHasDefect("Critical", "Failed to read mandatory 'ANOTHER' with value: 01/01/1986 19:48, error: ParseException Unparseable datetime: \"01/01/1986 19:48\" for format 'dd MMM yyyy HH:mm:ss'", badRecords.get(0));
    }

    @Test
    public void badDateTime()
    {
        runTestInvalid("Failed to read 'DOB' with value: 08/13/2001T36:12:01, error: ParseException Unparseable datetime: \"08/13/2001T36:12:01\" for format 'yyyy-MM-dd'T'HH:mm:ss.SSSXX'",
                "",
                "DATETIME(optional)",
                "08/13/2001T36:12:01"
        );
        runTestInvalid("Failed to read 'DOB' with value: 08/50/2001 10:21, error: ParseException Unparseable date: \"08/50/2001 10:21\" for formats 'dd/MM/yyyy HH:mm', 'MM/dd/yyyy HH:mm'",
                "",
                "DATE(optional, format=['dd/MM/yyyy HH:mm', 'MM/dd/yyyy HH:mm'])",
                "08/50/2001 10:21"
        );
    }

    private void runTest(String expected, String dobGrammar, String rawDateTime)
    {
        runTest(new String[] {expected}, "", dobGrammar, new String[] {rawDateTime});
    }

    private void runTest(String[] expected, String dobDataType, String[] rawDateTimes)
    {
        runTest(expected, "", dobDataType, rawDateTimes);
    }

    private void runTest(String expected, String properties, String dobGrammar, String rawDateTime)
    {
        runTest(new String[] {expected}, properties, dobGrammar, new String[] {rawDateTime});
    }

    private void runTest(String[] expected, String properties, String dobDataType, String[] rawDateTimes)
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : ',';\n" +
                "  nullString      : '';\n" +
                "  " + properties + "\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    NAME : STRING;\n" +
                "    DOB  : " + dobDataType + ";\n" +
                "  }\n" +
                "}\n"
        );

        List<IChecked<Person>> records = deserialize(Person.class, flatData, testCsv(rawDateTimes));
        Assert.assertEquals(rawDateTimes.length, records.size());
        for (int i = 0; i < rawDateTimes.length; i++)
        {
            assertNoDefects(records.get(i));
            TestDelimitedDateTime.Person person = records.get(i).getValue();
            Assert.assertEquals("name" + i, person.name);
            if (expected[i] == null)
            {
                Assert.assertNull(person.dob);
            }
            else
            {
                Assert.assertEquals(Instant.parse(expected[i]), person.dob);
            }
        }
    }

    private void runTestInvalid(String expectedError, String properties, String dobDataType, String rawDateTime)
    {
        runTestInvalid(new String[] {expectedError}, properties, dobDataType, new String[] {rawDateTime});
    }

    private void runTestInvalid(String[] expectedErrors, String properties, String dobDataType, String[] rawDateTimes)
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : ',';\n" +
                "  " + properties + "\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    NAME : STRING;\n" +
                "    DOB  : " + dobDataType + ";\n" +
                "  }\n" +
                "}\n"
        );

        List<IChecked<Person>> records = deserialize(Person.class, flatData, testCsv(rawDateTimes));
        Assert.assertEquals(rawDateTimes.length, records.size());
        for (int i = 0; i < expectedErrors.length; i++)
        {
            if (expectedErrors[i] == null)
            {
                assertNoDefects(records.get(i));
            }
            else
            {
                assertHasDefect("Error", expectedErrors[i], records.get(i));
            }
        }
    }

    private String testCsv(String[] rawDateTimes)
    {
        String[] rows = new String[rawDateTimes.length + 1];
        rows[0] = "NAME,DOB";
        for (int i = 0; i < rawDateTimes.length; i++)
        {
            rows[i + 1] = "name" + i + "," + (rawDateTimes[i] == null ? "" : rawDateTimes[i]);
        }
        return data("\n", rows);
    }

    public static class Person
    {
        public String name;
        public Instant dob;
    }
}
