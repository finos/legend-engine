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

import java.time.LocalDate;
import java.util.List;

public class TestDelimitedDate extends AbstractDriverTest
{
    @Test
    public void dateWithDefaultFormat()
    {
        runTest("2001-08-13",
                "DATE",
                "2001-08-13"
        );
    }

    @Test
    public void dateWithSpecifiedFormat()
    {
        runTest("2001-08-13",
                "DATE(format='dd/MM/yyyy')",
                "13/08/2001"
        );
    }

    @Test
    public void dateWithSpecified2ndFormat()
    {
        String dataType = "DATE(optional, format=['dd/MM/yyyy', 'dd MMM yyyy'])";
        runTest("2001-08-13", dataType, "13/08/2001");
        runTest("2001-08-13", dataType, "13 Aug 2001");
        runTest(new String[] {"2001-08-13","1983-01-01"}, dataType, new String[] {"13 Aug 2001", "01 Jan 1983"});
        runTest(new String[] {null,"1983-01-01"}, dataType, new String[] {null, "01 Jan 1983"});
        runTestInvalid(new String[] {null, "Failed to read 'DOB' with value: 13/08/2001, error: ParseException Unparseable date: \"13/08/2001\" for format 'dd MMM yyyy'"},
                "",
                dataType,
                new String[] {"13 Aug 2001", "13/08/2001"}
        );
    }

    @Test
    public void dateWithSpecified3rdFormat()
    {
        String dataType = "DATE(optional, format=['dd/MM/yyyy', 'dd MMM yyyy', 'yyyy-MM-dd'])";
        runTest("2001-08-13", dataType, "13/08/2001");
        runTest("2001-08-13", dataType, "13 Aug 2001");
        runTest("2001-08-13", dataType, "2001-08-13");
        runTest(new String[] {"2001-08-13","1983-01-01"}, dataType, new String[] {"13/08/2001", "01/01/1983"});
        runTest(new String[] {"2001-08-13","1983-01-01"}, dataType, new String[] {"13 Aug 2001", "01 Jan 1983"});
        runTest(new String[] {"2001-08-13","1983-01-01"}, dataType, new String[] {"2001-08-13", "1983-01-01"});
        runTest(new String[] {null,"1983-01-01"}, dataType, new String[] {null, "1983-01-01"});
        runTestInvalid(new String[] {null, "Failed to read 'DOB' with value: 13/08/2001, error: ParseException Unparseable date: \"13/08/2001\" for format 'yyyy-MM-dd'"},
                "",
                dataType,
                new String[] {"1983-01-01", "13/08/2001"}
        );
    }

    @Test
    public void dateWithOverriddenDefaultFormat()
    {
        runTest("2001-08-13",
                "defaultDateFormat: 'MM/dd/yyyy';",
                "DATE",
                "08/13/2001"
        );
    }

    @Test
    public void dateWithOverridden2ndDefaultFormat()
    {
        String property = "defaultDateFormat: ['MM/dd/yyyy', 'dd MMM yyyy'];";
        runTest("2001-08-13", property, "DATE", "08/13/2001");
        runTest("2001-08-13", property, "DATE", "13 Aug 2001");
    }

    @Test
    public void multipleDefaultFormatsFailWhenColumnsContradict()
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter : ',';\n" +
                "  defaultDateFormat: ['MM/dd/yyyy', 'dd MMM yyyy'];\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    NAME : STRING;\n" +
                "    DOB  : DATE;\n" +
                "    ANOTHER : DATE;\n" +
                "  }\n" +
                "}\n"
        );

        String goodData = data("\n", "NAME,DOB,ANOTHER", "John,13 Aug 2001,01 Jan 1986");

        List<IChecked<Person>> goodRecords = deserialize(Person.class, flatData, goodData);
        Assert.assertEquals(1, goodRecords.size());
        assertNoDefects(goodRecords.get(0));

        String badData = data("\n", "NAME,DOB,ANOTHER", "John,13 Aug 2001,01/01/1986");
        List<IChecked<Person>> badRecords = deserialize(Person.class, flatData, badData);
        Assert.assertEquals(1, badRecords.size());
        assertHasDefect("Critical", "Failed to read mandatory 'ANOTHER' with value: 01/01/1986, error: ParseException Unparseable date: \"01/01/1986\" for format 'dd MMM yyyy'", badRecords.get(0));
    }

    @Test
    public void badDate()
    {
        runTestInvalid("Failed to read 'DOB' with value: 08/50/2001, error: ParseException Unparseable date: \"08/50/2001\" for format 'yyyy-MM-dd'",
                "",
                "DATE(optional)",
                "08/50/2001"
        );
        runTestInvalid("Failed to read 'DOB' with value: 08/50/2001, error: ParseException Unparseable date: \"08/50/2001\" for formats 'dd/MM/yyyy', 'MM/dd/yyyy'",
                "",
                "DATE(optional, format=['dd/MM/yyyy', 'MM/dd/yyyy'])",
                "08/50/2001"
        );
    }

    private void runTest(String expected, String dobGrammar, String rawDate)
    {
        runTest(new String[] {expected}, "", dobGrammar, new String[] {rawDate});
    }

    private void runTest(String[] expected, String dobGrammar, String[] rawDates)
    {
        runTest(expected, "", dobGrammar, rawDates);
    }

    private void runTest(String expected, String properties, String dobDataType, String rawDate)
    {
        runTest(new String[] {expected}, properties, dobDataType, new String[] {rawDate});
    }

    private void runTest(String[] expected, String properties, String dobDataType, String[] rawDates)
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

        List<IChecked<Person>> records = deserialize(Person.class, flatData, testCsv(rawDates));
        Assert.assertEquals(rawDates.length, records.size());
        for (int i=0; i<rawDates.length; i++)
        {
            assertNoDefects(records.get(i));
            Person person = records.get(i).getValue();
            Assert.assertEquals("name"+i, person.name);
            if (expected[i] == null)
            {
                Assert.assertNull(person.dob);
            }
            else
            {
                Assert.assertEquals(LocalDate.parse(expected[i]), person.dob);
            }
        }
    }

    private void runTestInvalid(String expectedError, String properties, String dobDataType, String rawDate)
    {
        runTestInvalid(new String[] {expectedError}, properties, dobDataType, new String[] {rawDate});
    }

    private void runTestInvalid(String[] expectedErrors, String properties, String dobDataType, String[] rawDates)
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

        List<IChecked<Person>> records = deserialize(Person.class, flatData, testCsv(rawDates));
        Assert.assertEquals(rawDates.length, records.size());
        for (int i=0; i<expectedErrors.length; i++)
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

    private String testCsv(String[] rawDates)
    {
        String[] rows = new String[rawDates.length+1];
        rows[0] = "NAME,DOB";
        for (int i=0; i<rawDates.length; i++)
        {
            rows[i+1] = "name" + i + "," + (rawDates[i] == null ? "" : rawDates[i]);
        }
        return data("\n", rows);
    }

    public static class Person
    {
        public String name;
        public LocalDate dob;
    }
}
