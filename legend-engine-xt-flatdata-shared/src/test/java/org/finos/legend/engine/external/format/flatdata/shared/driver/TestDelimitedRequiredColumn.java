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

import java.util.Arrays;
import java.util.List;

public class TestDelimitedRequiredColumn extends AbstractDriverTest
{
    @Test
    public void byDefaultOptionalColumnsCanMeMissing()
    {
        FlatData store = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : ',';\n" +
                "  nullString      : '';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    NAME          : STRING;\n" +
                "    WEIGHT        : DECIMAL(optional);\n" +
                "  }\n" +
                "}\n"
        );

        List<IChecked<Person>> records = deserialize(Person.class, store, data("\n", "NAME", "John", "Will"));

        records.forEach(this::assertNoDefects);
        assertSource(1, 2, "John", Arrays.asList(rValue("NAME", "John")), records.get(0));
        Person p1 = records.get(0).getValue();
        Assert.assertEquals("John", p1.name);
        Assert.assertNull(p1.weight);

        assertSource(2, 3, "Will", Arrays.asList(rValue("NAME", "Will")), records.get(1));
        Person p2 = records.get(1).getValue();
        Assert.assertEquals("Will", p2.name);
        Assert.assertNull(p2.weight);
    }

    @Test
    public void whenSpecifiedColumnsCanBeRequired()
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : ',';\n" +
                "  nullString      : '';\n" +
                "  modelledColumnsMustBePresent;\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    NAME   : STRING;\n" +
                "    WEIGHT : DECIMAL;\n" +
                "  }\n" +
                "}\n");

        List<IChecked<Person>> records = deserialize(Person.class, flatData, data("\n", "NAME", "John", "Will"));
        assertHasDefect("Critical", "Heading WEIGHT missing for required column", records.get(0));
        assertHasDefect("Critical", "Header row is invalid. Skipping all data in this section.", records.get(0));
        Assert.assertEquals(1, records.size());
    }

    @Test
    public void whenPresentRequiredFieldsCanBeOptional()
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : ',';\n" +
                "  nullString      : '';\n" +
                "  modelledColumnsMustBePresent;\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    NAME   : STRING;\n" +
                "    WEIGHT : DECIMAL(optional);\n" +
                "  }\n" +
                "}\n"
        );

        List<IChecked<Person>> records = deserialize(Person.class, flatData, data("\n", "NAME,WEIGHT", "John,75", "Will"));

        records.forEach(this::assertNoDefects);
        assertSource(1, 2, "John,75", Arrays.asList(rValue("NAME", "John"), rValue("WEIGHT", "75")), records.get(0));
        Person p1 = records.get(0).getValue();
        Assert.assertEquals("John", p1.name);
        Assert.assertEquals(75.0, p1.weight, 0.00000001);
        assertSource(2, 3, "Will", Arrays.asList(rValue("NAME", "Will")), records.get(1));
        Person p2 = records.get(1).getValue();
        Assert.assertEquals("Will", p2.name);
        Assert.assertNull(p2.weight);
    }

    @Test
    public void byDefaultUnmodelledColumnsCanBePresent()
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : ',';\n" +
                "  nullString      : '';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    NAME : STRING;\n" +
                "  }\n" +
                "}\n"
        );

        List<IChecked<Person>> records = deserialize(Person.class, flatData, data("\n", "NAME,HOBBY", "John,Reading", "Will,Singing"));

        records.forEach(this::assertNoDefects);
        assertSource(1, 2, "John,Reading", Arrays.asList(rValue("NAME", "John"), rValue("HOBBY", "Reading")), records.get(0));
        Assert.assertEquals("John", records.get(0).getValue().name);
        assertSource(2, 3, "Will,Singing", Arrays.asList(rValue("NAME", "Will"), rValue("HOBBY", "Singing")), records.get(1));
        Assert.assertEquals("Will", records.get(1).getValue().name);
    }

    @Test
    public void byUnmodelledColumnsCanBeProhibited()
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : ',';\n" +
                "  nullString      : '';\n" +
                "  onlyModelledColumnsAllowed;\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    NAME : STRING;\n" +
                "  }\n" +
                "}\n"
        );

        List<IChecked<Person>> records = deserialize(Person.class, flatData, data("\n", "NAME,HOBBY", "John,Reading", "Will,Singing"));
        assertHasDefect("Critical", "Unexpected heading HOBBY", records.get(0));
        assertHasDefect("Critical", "Header row is invalid. Skipping all data in this section.", records.get(0));
        Assert.assertEquals(1, records.size());
    }

    public static class Person
    {
        public String name;
        public Double weight;
    }
}
