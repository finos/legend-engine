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
    public void badDateTime()
    {
        runTestInvalid("Failed to read 'DOB' with value: 08/13/2001T36:12:01, error: ParseException Unparseable datetime: \"08/13/2001T36:12:01\"",
                "",
                "DATETIME(optional)",
                "08/13/2001T36:12:01"
        );
    }

    private void runTest(String expected, String dobGrammar, String rawDate)
    {
        runTest(expected, "", dobGrammar, rawDate);
    }

    private void runTest(String expected, String properties, String dobDataType, String rawDate)
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

        List<IChecked<Person>> records = deserialize(Person.class, flatData, data("\n", "NAME,DOB", "Alex," + rawDate));
        Assert.assertEquals(1, records.size());
        assertNoDefects(records.get(0));
        Person person =  records.get(0).getValue();
        Assert.assertEquals("Alex", person.name);
        Assert.assertEquals(Instant.parse(expected), person.dob);
    }

    private void runTestInvalid(String expectedError, String properties, String dobDataType, String rawDate)
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

        List<IChecked<Person>> records = deserialize(Person.class, flatData, data("\n", "NAME,DOB", "Alex," + rawDate));
        Assert.assertEquals(1, records.size());
        assertHasDefect("Error", expectedError, records.get(0));
    }

    public static class Person
    {
        public String name;
        public Instant dob;
    }
}
