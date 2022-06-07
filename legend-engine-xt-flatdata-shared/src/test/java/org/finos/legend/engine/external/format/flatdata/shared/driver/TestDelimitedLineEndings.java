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

public class TestDelimitedLineEndings extends AbstractDriverTest
{
    @Test
    public void processesSmartLineEndingsCrLf()
    {
        processesSmartLineEndings("\r\n", false);
    }

    @Test
    public void processesSmartLineEndingsCrLfWithTrailing()
    {
        processesSmartLineEndings("\r\n", true);
    }

    @Test
    public void processesSmartLineEndingsCr()
    {
        processesSmartLineEndings("\n", false);
    }

    @Test
    public void processesSmartLineEndingsCrWithTrailing()
    {
        processesSmartLineEndings("\r", true);
    }

    @Test
    public void processesSmartLineEndingsLf()
    {
        processesSmartLineEndings("\n", false);
    }

    @Test
    public void processesSmartLineEndingsLfWithTrailing()
    {
        processesSmartLineEndings("\n", true);
    }

    @Test
    public void processesSpecifiedOneCharLineEndings()
    {
        processesSpecifiedLineEndings("!", false);
    }

    @Test
    public void processesSpecifiedOneCharLineEndingsWithTrailing()
    {
        processesSpecifiedLineEndings("!", true);
    }

    @Test
    public void processesSpecifiedTwoCharLineEndings()
    {
        processesSpecifiedLineEndings(";)", false);
    }

    @Test
    public void processesSpecifiedTwoCharLineEndingsWithTrailing()
    {
        processesSpecifiedLineEndings(";)", true);
    }

    private void processesSmartLineEndings(String eol, boolean addTrailing)
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : ',';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    NAME : STRING;\n" +
                "  }\n" +
                "}");

        runTest(flatData, data(eol, addTrailing));
    }

    public void processesSpecifiedLineEndings(String eol, boolean addTrailing)
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  recordSeparator : '" + eol + "';\n" +
                "  delimiter       : ',';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    NAME : STRING;\n" +
                "  }\n" +
                "}");

        runTest(flatData, data(eol, addTrailing));
    }

    private String data(String eol, boolean addTrailing)
    {
        return data(eol, addTrailing,
                "NAME",
                "John Doe",
                "Jane Doe"
        );
    }

    private void runTest(FlatData flatData, String data)
    {
        List<IChecked<Person>> records = deserialize(Person.class, flatData, data);

        records.forEach(this::assertNoDefects);
        assertSource(1, 2, "John Doe", Arrays.asList(rValue("NAME", "John Doe")), records.get(0));
        Assert.assertEquals("John Doe", records.get(0).getValue().name);
        assertSource(2, 3, "Jane Doe", Arrays.asList(rValue("NAME", "Jane Doe")), records.get(1));
        Assert.assertEquals("Jane Doe", records.get(1).getValue().name);
    }

    public static class Person
    {
        public String name;
    }
}
