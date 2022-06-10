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

import java.util.List;

public class TestDelimitedBoolean extends AbstractDriverTest
{
    @Test
    public void withoutExplicitStrings()
    {
        String employedGrammar = "BOOLEAN";
        runTest(true, "", employedGrammar, "TRUE");
        runTest(true, "", employedGrammar, "True");
        runTest(true, "", employedGrammar, "true");

        runTest(false, "", employedGrammar, "FALSE");
        runTest(false, "", employedGrammar, "False");
        runTest(false, "", employedGrammar, "false");

        runTest(false, "", employedGrammar, "other");
    }

    @Test
    public void withExplicitTrueString()
    {
        String employedGrammar = "BOOLEAN(trueString='Y')";
        runTest(true, "", employedGrammar, "Y");
        runTest(true, "", employedGrammar, "y");

        runTest(false, "", employedGrammar, "N");
        runTest(false, "", employedGrammar, "n");

        runTest(false, "", employedGrammar, "other");
    }

    @Test
    public void withExplicitFalseString()
    {
        String employedGrammar = "BOOLEAN(falseString='N')";
        runTest(true, "", employedGrammar, "Y");
        runTest(true, "", employedGrammar, "y");

        runTest(false, "", employedGrammar, "N");
        runTest(false, "", employedGrammar, "n");

        runTest(true, "", employedGrammar, "other");
    }

    @Test
    public void withExplicitTrueAndFalseString()
    {
        String employedGrammar = "BOOLEAN(trueString='Y', falseString='N')";
        runTest(true, "", employedGrammar, "Y");
        runTest(true, "", employedGrammar, "y");

        runTest(false, "", employedGrammar, "N");
        runTest(false, "", employedGrammar, "n");

        runTestInvalid("Failed to read mandatory 'EMPLOYED' with value: other, error: ParseException Invalid boolean: neither 'Y' nor 'N'", "", employedGrammar, "other");
    }

    @Test
    public void withDefaultTrueString()
    {
        String employedGrammar = "BOOLEAN";
        String properties = "defaultTrueString: 'Y';";
        runTest(true, properties, employedGrammar, "Y");
        runTest(true, properties, employedGrammar, "y");

        runTest(false, properties, employedGrammar, "N");
        runTest(false, properties, employedGrammar, "n");

        runTest(false, properties, employedGrammar, "other");
    }

    @Test
    public void withDefaultFalseString()
    {
        String employedGrammar = "BOOLEAN";
        String properties = "defaultFalseString: 'N';";
        runTest(true, properties, employedGrammar, "Y");
        runTest(true, properties, employedGrammar, "y");

        runTest(false, properties, employedGrammar, "N");
        runTest(false, properties, employedGrammar, "n");

        runTest(true, properties, employedGrammar, "other");
    }

    @Test
    public void withDefaultTrueAndFalseString()
    {
        String employedGrammar = "BOOLEAN";
        String properties = "defaultTrueString: 'Y'; defaultFalseString: 'N';";
        runTest(true, properties, employedGrammar, "Y");
        runTest(true, properties, employedGrammar, "y");

        runTest(false, properties, employedGrammar, "N");
        runTest(false, properties, employedGrammar, "n");

        runTestInvalid("Failed to read mandatory 'EMPLOYED' with value: other, error: ParseException Invalid boolean: neither 'Y' nor 'N'", properties, employedGrammar, "other");
    }

    private void runTest(boolean expected, String properties, String employedGrammar, String rawData)
    {
        FlatData flatData = flatData(properties, employedGrammar);
        List<IChecked<Person>> records = deserialize(Person.class, flatData, data("\n", "NAME,EMPLOYED", "Alex," + rawData));

        Assert.assertEquals(1, records.size());
        IChecked<Person> record = records.get(0);
        assertNoDefects(record);
        Assert.assertEquals("Alex", record.getValue().name);
        Assert.assertEquals(expected, record.getValue().employed);
    }

    private void runTestInvalid(String expectedDefect, String properties, String employedGrammar, String rawData)
    {
        FlatData flatData = flatData(properties, employedGrammar);
        List<IChecked<Person>> records = deserialize(Person.class, flatData, data("\n", "NAME,EMPLOYED", "Alex," + rawData));

        Assert.assertEquals(1, records.size());
        IChecked<Person> record = records.get(0);
        assertHasDefect("Critical", expectedDefect, record);
    }

    private FlatData flatData(String properties, String employedGrammar)
    {
        return parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : ',';\n" +
                "  " + properties + "\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    NAME      : STRING;\n" +
                "    EMPLOYED  : " + employedGrammar + ";\n" +
                "  }\n" +
                "}\n");
    }

    @SuppressWarnings("WeakerAccess")  // Required for reflective access
    public static class Person
    {
        public String name;
        public boolean employed;
    }
}
