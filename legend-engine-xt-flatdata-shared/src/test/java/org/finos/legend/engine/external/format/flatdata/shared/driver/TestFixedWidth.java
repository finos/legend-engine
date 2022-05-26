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

package org.finos.legend.engine.external.format.flatdata.shared.driver;

import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

public class TestFixedWidth extends AbstractDriverTest
{
    @Test
    public void canReadErrorFree()
    {
        FlatData flatData = parseFlatData("section default: FixedWidth\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    FIRM          {1:2} : STRING;\n" +
                "    AGE           {3:4} : INTEGER;\n" +
                "    MASTER        {5:5} : INTEGER(optional);\n" +
                "    WEIGHT        {6:8} : STRING(optional);\n" +
                "    NAME          {9:12} : STRING;\n" +
                "    EMPLOYED_DATE {13:22} : DATE(format='yyyy-MM-dd');\n" +
                "    TITLE         {23:24} : STRING;\n" +
                "  }\n" +
                "}\n");

        String data = data("\r\n",
                "GS2511.1Alex2013-08-13NA2013-01-01",
                "GG2601.2Brad2003-01-01VP2003-01-01",
                "FB2711.3Karl2011-11-26MD2011-01-01"
        );

        List<IChecked<Person>> records = deserialize(Person.class, flatData, data);
        records.forEach(this::assertNoDefects);

        List<ExpectedRecordValue> expectedRecordValues1 = Arrays.asList(
                rValue("1:2", "GS"),
                rValue("3:4", "25"),
                rValue("5:5", "1"),
                rValue("6:8", "1.1"),
                rValue("9:12", "Alex"),
                rValue("13:22", "2013-08-13"),
                rValue("23:24", "NA")
        );

        List<ExpectedRecordValue> expectedRecordValues2 = Arrays.asList(
                rValue("1:2", "GG"),
                rValue("3:4", "26"),
                rValue("5:5", "0"),
                rValue("6:8", "1.2"),
                rValue("9:12", "Brad"),
                rValue("13:22", "2003-01-01"),
                rValue("23:24", "VP")
        );

        List<AbstractDriverTest.ExpectedRecordValue> expectedRecordValues3 = Arrays.asList(
                rValue("1:2", "FB"),
                rValue("3:4", "27"),
                rValue("5:5", "1"),
                rValue("6:8", "1.3"),
                rValue("9:12", "Karl"),
                rValue("13:22", "2011-11-26"),
                rValue("23:24", "MD")
        );

        assertSource(1, 1, "GS2511.1Alex2013-08-13NA2013-01-01", expectedRecordValues1, records.get(0));
        Person p1 = records.get(0).getValue();
        Assert.assertEquals("GS", p1.FIRM);
        Assert.assertEquals(25L, p1.AGE);
        Assert.assertEquals(1, p1.MASTER);
        Assert.assertEquals("1.1", p1.WEIGHT);
        Assert.assertEquals("Alex", p1.NAME);
        Assert.assertEquals(LocalDate.parse("2013-08-13"), p1.EMPLOYED_DATE);
        Assert.assertEquals("NA", p1.TITLE);

        assertSource(2, 2, "GG2601.2Brad2003-01-01VP2003-01-01", expectedRecordValues2, records.get(1));
        Person p2 = records.get(1).getValue();
        Assert.assertEquals("GG", p2.FIRM);
        Assert.assertEquals(26L, p2.AGE);
        Assert.assertEquals(0, p2.MASTER);
        Assert.assertEquals("1.2", p2.WEIGHT);
        Assert.assertEquals("Brad", p2.NAME);
        Assert.assertEquals(LocalDate.parse("2003-01-01"), p2.EMPLOYED_DATE);
        Assert.assertEquals("VP", p2.TITLE);


        assertSource(3, 3, "FB2711.3Karl2011-11-26MD2011-01-01", expectedRecordValues3, records.get(2));
        Person p3 = records.get(2).getValue();
        Assert.assertEquals("FB", p3.FIRM);
        Assert.assertEquals(27L, p3.AGE);
        Assert.assertEquals(1, p3.MASTER);
        Assert.assertEquals("1.3", p3.WEIGHT);
        Assert.assertEquals("Karl", p3.NAME);
        Assert.assertEquals(LocalDate.parse("2011-11-26"), p3.EMPLOYED_DATE);
        Assert.assertEquals("MD", p3.TITLE);
    }

    @SuppressWarnings("WeakerAccess")  // Required for reflective access
    public static class Person
    {
        public String NAME;
        public String FIRM;
        public long AGE;
        public long MASTER;
        public String WEIGHT;
        public LocalDate EMPLOYED_DATE;
        public String TITLE;
    }
}
