//  Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.external.format.flatdata.grammar.driver;

import org.finos.legend.engine.external.format.flatdata.metamodel.FlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TestDelimitedMultiCharacterDelimiter extends AbstractDriverTest
{

    @Test
    public void canReadErrorFree()
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : '~!@';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    NAME          : STRING;\n" +
                "    AGE           : INTEGER;\n" +
                "    TITLE         : STRING;\n" +
                "  }\n" +
                "}\n");

        String data = data("\n",
                "AGE~!@NAME~!@TITLE",
                "25~!@Alex~!@Other",
                "26~!@Brad~!@Vice President",
                "27~!@Karl~!@Managing Director"
        );

        List<IChecked<Person>> records = deserialize(Person.class, flatData, data);

        records.forEach(this::assertNoDefects);


        List<ExpectedRecordValue> expectedRecordValues1 = Arrays.asList(
                AbstractDriverTest.rValue("AGE", "25"),
                AbstractDriverTest.rValue("NAME", "Alex"),
                AbstractDriverTest.rValue("TITLE", "Other")
        );

        List<ExpectedRecordValue> expectedRecordValues2 = Arrays.asList(
                AbstractDriverTest.rValue("AGE", "26"),
                AbstractDriverTest.rValue("NAME", "Brad"),
                AbstractDriverTest.rValue("TITLE", "Vice President")
        );

        List<ExpectedRecordValue> expectedRecordValues3 = Arrays.asList(
                AbstractDriverTest.rValue("AGE", "27"),
                AbstractDriverTest.rValue("NAME", "Karl"),
                AbstractDriverTest.rValue("TITLE", "Managing Director")
        );

        assertSource(1, 2, "25~!@Alex~!@Other", expectedRecordValues1, records.get(0));
        Person p1 = records.get(0).getValue();
        Assert.assertEquals(25L, p1.AGE);
        Assert.assertEquals("Alex", p1.NAME);
        Assert.assertEquals("Other", p1.TITLE);

        assertSource(2, 3, "26~!@Brad~!@Vice President", expectedRecordValues2, records.get(1));
        Person p2 = records.get(1).getValue();
        Assert.assertEquals(26L, p2.AGE);
        Assert.assertEquals("Brad", p2.NAME);
        Assert.assertEquals("Vice President", p2.TITLE);

        assertSource(3, 4, "27~!@Karl~!@Managing Director", expectedRecordValues3, records.get(2));
        Person p3 = records.get(2).getValue();
        Assert.assertEquals(27L, p3.AGE);
        Assert.assertEquals("Karl", p3.NAME);
        Assert.assertEquals("Managing Director", p3.TITLE);
    }

    public static class Person
    {
        public String NAME;
        public long AGE;
        public String TITLE;
    }
}
