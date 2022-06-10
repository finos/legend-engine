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

package org.finos.legend.engine.external.format.flatdata.shared.driver;

import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.plan.dependencies.domain.dataQuality.IChecked;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class TestDelimitedCaseInsensitiveColumnNames extends AbstractDriverTest
{
    @Test
    public void allUpperCase()
    {
        test("AGE,NAME,TITLE");
    }

    @Test
    public void allLowerCase()
    {
        test("age,name,title");
    }

    @Test
    public void mixedCases()
    {
        test("Age,Name,tItLe");
    }

    private void test(String headings)
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : ',';\n" +
                "  columnsHeadingsAreCaseInsensitive;\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    NAME          : STRING;\n" +
                "    AGE           : INTEGER;\n" +
                "    TITLE         : STRING;\n" +
                "  }\n" +
                "}\n");

        String data = data("\n", headings, "25,Alex,Other");

        List<IChecked<TestDelimitedMultiCharacterDelimiter.Person>> records = deserialize(TestDelimitedMultiCharacterDelimiter.Person.class, flatData, data);

        records.forEach(this::assertNoDefects);

        List<ExpectedRecordValue> expectedRecordValues1 = Arrays.asList(
                rValue("AGE", "25"),
                rValue("NAME", "Alex"),
                rValue("TITLE", "Other")
        );

        assertSource(1, 2, "25,Alex,Other", expectedRecordValues1, records.get(0));
        TestDelimitedMultiCharacterDelimiter.Person p1 = records.get(0).getValue();
        Assert.assertEquals(25L, p1.AGE);
        Assert.assertEquals("Alex", p1.NAME);
        Assert.assertEquals("Other", p1.TITLE);
    }

    public static class Person
    {
        public String NAME;
        public long AGE;
        public String TITLE;
    }
}
