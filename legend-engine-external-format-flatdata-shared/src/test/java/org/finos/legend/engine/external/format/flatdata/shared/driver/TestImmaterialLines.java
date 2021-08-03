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

public class TestImmaterialLines extends AbstractDriverTest
{
    @Test
    public void canReadAndSkipImmaterialLines()
    {
        FlatData flatData = parseFlatData("section skipMe: ImmaterialLines\n" +
                                                  "{\n" +
                                                  "  scope.forNumberOfLines: 2;\n" +
                                                  "}\n" +
                                                  "\n" +
                                                  "section default: DelimitedWithHeadings\n" +
                                                  "{\n" +
                                                  "  scope.untilEof;\n" +
                                                  "  delimiter       : ',';\n" +
                                                  "\n" +
                                                  "  Record\n" +
                                                  "  {\n" +
                                                  "    NAME : STRING;\n" +
                                                  "  }\n" +
                                                  "}");

        String data = data("\n",
                          "This line should be ignored",
                          "So should this one",
                          "NAME",
                          "John Doe",
                          "Jane Doe"
        );

        List<IChecked<Person>> records = deserialize(Person.class, flatData, data);

        records.forEach(this::assertNoDefects);
        assertSource(1, 4, "John Doe", Arrays.asList(rValue("NAME", "John Doe")), records.get(0));
        Assert.assertEquals("John Doe", records.get(0).getValue().name);
        assertSource(2, 5, "Jane Doe", Arrays.asList(rValue("NAME", "Jane Doe")), records.get(1));
        Assert.assertEquals("Jane Doe", records.get(1).getValue().name);
    }

    @Test
    public void canReadAndSkipHeaderAndTrailer()
    {
        FlatData flatData = parseFlatData("section skipHeader1: ImmaterialLines\n" +
                                                  "{\n" +
                                                  "  scope.forNumberOfLines: 2;\n" +
                                                  "}\n" +
                                                  "section skipHeader2: ImmaterialLines\n" +
                                                  "{\n" +
                                                  "  scope.forNumberOfLines: 1;\n" +
                                                  "}\n" +
                                                  "\n" +
                                                  "section default: DelimitedWithHeadings\n" +
                                                  "{\n" +
                                                  "  scope.default;\n" +
                                                  "  delimiter       : ',';\n" +
                                                  "\n" +
                                                  "  Record\n" +
                                                  "  {\n" +
                                                  "    NAME : STRING;\n" +
                                                  "  }\n" +
                                                  "}\n" +
                                                  "\n" +
                                                  "section skipTrailer1: ImmaterialLines\n" +
                                                  "{\n" +
                                                  "  scope.forNumberOfLines: 3;\n" +
                                                  "}\n" +
                                                  "section skipTrailer2: ImmaterialLines\n" +
                                                  "{\n" +
                                                  "  scope.forNumberOfLines: 1;\n" +
                                                  "}");

        String data = data("\n",
                          "This line should be ignored",
                          "So should this one",
                          "hello",
                          "NAME",
                          "John Doe",
                          "Jane Doe",
                          "Ignore,Ignore",
                          "This line should be ignored",
                          "So should this one",
                          "Another Driver\n"
        );

        List<IChecked<Person>> records = deserialize(Person.class, flatData, data);

        records.forEach(this::assertNoDefects);
        assertSource(1, 5, "John Doe", Arrays.asList(rValue("NAME", "John Doe")), records.get(0));
        Assert.assertEquals("John Doe", records.get(0).getValue().name);
        assertSource(2, 6, "Jane Doe", Arrays.asList(rValue("NAME", "Jane Doe")), records.get(1));
        Assert.assertEquals("Jane Doe", records.get(1).getValue().name);
    }

    public static class Person
    {
        public String name;
    }
}
