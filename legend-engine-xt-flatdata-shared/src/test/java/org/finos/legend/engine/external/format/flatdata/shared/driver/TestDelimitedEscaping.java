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

public class TestDelimitedEscaping extends AbstractDriverTest
{
    @Test
    public void escapeTheEscapeOutsideQuotes()
    {
        runTest("~Hello", "World", "~~Hello,World", '~');
        runTest("He\\llo", "World", "He\\\\llo,World", '\\');
    }

    @Test
    public void escapeTheEscapeInsideQuotes()
    {
        runTest("~Hello", "World", "'~~Hello','World'", '~');
        runTest("He\\llo", "World", "'He\\\\llo','World'", '\\');
    }

    @Test
    public void escapeTheDelimiterOutsideQuotes()
    {
        runTest("Hello,World", "Bye", "Hello\\,World,Bye", '\\');
    }

    @Test
    public void escapeTheDelimiterInsideQuotes()
    {
        runTest("Hello,World", "Bye", "'Hello\\,World','Bye'", '\\');
    }

    @Test
    public void escapeQuoteOutsideQuotes()
    {
        runTest("'Hello World'", "Bye", "\\'Hello World\\',Bye", '\\');
    }

    @Test
    public void escapeQuoteInsideQuotes()
    {
        runTest("Hello 'World'", "Bye", "'Hello \\'World\\'','Bye'", '\\');
    }

    @Test
    public void unsupportedEscapedCharacterOutsideQuotes()
    {
        runTestInvalid("Unexpected characer following escape 'W' at line 2", "Hello\\World,Bye", '\\');
    }

    @Test
    public void unsupportedEscapedCharacterInsideQuotes()
    {
        runTestInvalid("Unexpected characer following escape 'W' at line 2", "'Hello\\World','Bye'", '\\');
    }

    @Test
    public void escapAtEndOfLineOutsideQuotes()
    {
        runTestInvalid("Escape cannot be the last character of line at line 2", "Hello World,Bye\\", '\\');
    }

    @Test
    public void escapAtEndOfLineInsideQuotes()
    {
        runTestInvalid("Escape cannot be the last character of line at line 2", "'Hello World','Bye\\", '\\');
    }

    private void runTest(String expectedField1, String expectedField2, String rawLine, char quoteChar)
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : ',';\n" +
                "  quoteChar       : '\\'';\n" +
                "  escapingChar    : '" + (quoteChar == '\\' ? "\\\\" : quoteChar + "") + "';\n" +
                "  nullString      : '';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    FIELD1 : STRING;\n" +
                "    FIELD2 : STRING;\n" +
                "  }\n" +
                "}\n"
        );

        List<IChecked<Data>> records = deserialize(Data.class, flatData, data("\n", "FIELD1,FIELD2", rawLine));
        Assert.assertEquals(1, records.size());
        IChecked<Data> record = records.get(0);
        assertNoDefects(record);
        Assert.assertEquals(expectedField1, record.getValue().field1);
        Assert.assertEquals(expectedField2, record.getValue().field2);
    }

    private void runTestInvalid(String expectedDefect, String rawLine, char quoteChar)
    {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : ',';\n" +
                "  quoteChar       : '\\'';\n" +
                "  escapingChar    : '" + (quoteChar == '\\' ? "\\\\" : quoteChar + "") + "';\n" +
                "  nullString      : '';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "    FIELD1 : STRING;\n" +
                "    FIELD2 : STRING;\n" +
                "  }\n" +
                "}\n"
        );

        List<IChecked<Data>> records = deserialize(Data.class, flatData, data("\n", "FIELD1,FIELD2", rawLine));
        Assert.assertEquals(1, records.size());
        assertHasDefect("Critical", expectedDefect, records.get(0));
    }

    public static class Data
    {
        public String field1;
        public String field2;
    }

}
