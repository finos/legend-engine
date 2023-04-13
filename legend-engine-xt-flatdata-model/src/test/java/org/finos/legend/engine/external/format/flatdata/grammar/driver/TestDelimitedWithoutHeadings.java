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

package org.finos.legend.engine.external.format.flatdata.grammar.driver;

import org.finos.legend.engine.external.format.flatdata.metamodel.FlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class TestDelimitedWithoutHeadings extends AbstractDriverTest
{
    @Test
    public void canReadErrorFree()
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithoutHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  recordSeparator : '\\r\\n';\n" +
                "  delimiter       : ',';\n" +
                "  escapingChar    : '\\\\';\n" +
                "  nullString      : 'None';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    FIRM          {1} : STRING;\n" +
                "    AGE           {2} : INTEGER;\n" +
                "    MASTER        {3} : BOOLEAN(optional);\n" +
                "    WEIGHT        {4} : DECIMAL(optional);\n" +
                "    NAME          {5} : STRING;\n" +
                "    EMPLOYED_DATE {6} : DATE(format='yyyy-MM-dd');\n" +
                "    TITLE         {7} : STRING;\n" +
                "  }\n" +
                "}\n");

        String data = data("\r\n",
                "'Goldman Sachs',25,true,1.1,Alex,2013-08-13,Other,2013-01-01",
                "Google,26,false,1.2,Brad,2003-01-01,Vice President,2003-01-01",
                "Facebook,27,true,1.3,Karl,2011-11-26,Managing Director,2011-01-01"
        );

        List<IChecked<Person>> records = deserialize(Person.class, flatData, data);
        records.forEach(this::assertNoDefects);

        List<ExpectedRecordValue> expectedRecordValues1 = Arrays.asList(
                AbstractDriverTest.rValue(1L, "'Goldman Sachs'"),
                AbstractDriverTest.rValue(2L, "25"),
                AbstractDriverTest.rValue(3L, "true"),
                AbstractDriverTest.rValue(4L, "1.1"),
                AbstractDriverTest.rValue(5L, "Alex"),
                AbstractDriverTest.rValue(6L, "2013-08-13"),
                AbstractDriverTest.rValue(7L, "Other"),
                AbstractDriverTest.rValue(8L, "2013-01-01")
        );

        List<ExpectedRecordValue> expectedRecordValues2 = Arrays.asList(
                AbstractDriverTest.rValue(1L, "Google"),
                AbstractDriverTest.rValue(2L, "26"),
                AbstractDriverTest.rValue(3L, "false"),
                AbstractDriverTest.rValue(4L, "1.2"),
                AbstractDriverTest.rValue(5L, "Brad"),
                AbstractDriverTest.rValue(6L, "2003-01-01"),
                AbstractDriverTest.rValue(7L, "Vice President"),
                AbstractDriverTest.rValue(8L, "2003-01-01")
        );

        List<ExpectedRecordValue> expectedRecordValues3 = Arrays.asList(
                AbstractDriverTest.rValue(1L, "Facebook"),
                AbstractDriverTest.rValue(2L, "27"),
                AbstractDriverTest.rValue(3L, "true"),
                AbstractDriverTest.rValue(4L, "1.3"),
                AbstractDriverTest.rValue(5L, "Karl"),
                AbstractDriverTest.rValue(6L, "2011-11-26"),
                AbstractDriverTest.rValue(7L, "Managing Director"),
                AbstractDriverTest.rValue(8L, "2011-01-01")
        );

        assertSource(1, 1, "'Goldman Sachs',25,true,1.1,Alex,2013-08-13,Other,2013-01-01", expectedRecordValues1, records.get(0));
        Person p1 = records.get(0).getValue();
        Assert.assertEquals("'Goldman Sachs'", p1.FIRM);
        Assert.assertEquals(25L, p1.AGE);
        Assert.assertEquals(true, p1.MASTER);
        Assert.assertEquals(1.1, p1.WEIGHT, 0.00000001);
        Assert.assertEquals("Alex", p1.NAME);
        Assert.assertEquals(LocalDate.parse("2013-08-13"), p1.EMPLOYED_DATE);
        Assert.assertEquals("Other", p1.TITLE);

        assertSource(2, 2, "Google,26,false,1.2,Brad,2003-01-01,Vice President,2003-01-01", expectedRecordValues2, records.get(1));
        Person p2 = records.get(1).getValue();
        Assert.assertEquals("Google", p2.FIRM);
        Assert.assertEquals(26L, p2.AGE);
        Assert.assertEquals(false, p2.MASTER);
        Assert.assertEquals(1.2, p2.WEIGHT, 0.00000001);
        Assert.assertEquals("Brad", p2.NAME);
        Assert.assertEquals(LocalDate.parse("2003-01-01"), p2.EMPLOYED_DATE);
        Assert.assertEquals("Vice President", p2.TITLE);

        assertSource(3, 3, "Facebook,27,true,1.3,Karl,2011-11-26,Managing Director,2011-01-01", expectedRecordValues3, records.get(2));
        Person p3 = records.get(2).getValue();
        Assert.assertEquals("Facebook", p3.FIRM);
        Assert.assertEquals(27L, p3.AGE);
        Assert.assertEquals(true, p3.MASTER);
        Assert.assertEquals(1.3, p3.WEIGHT, 0.00000001);
        Assert.assertEquals("Karl", p3.NAME);
        Assert.assertEquals(LocalDate.parse("2011-11-26"), p3.EMPLOYED_DATE);
        Assert.assertEquals("Managing Director", p3.TITLE);
    }

    public static class Person
    {
        public String NAME;
        public String FIRM;
        public long AGE;
        public Boolean MASTER;
        public Double WEIGHT;
        public LocalDate EMPLOYED_DATE;
        public String TITLE;
    }
}
