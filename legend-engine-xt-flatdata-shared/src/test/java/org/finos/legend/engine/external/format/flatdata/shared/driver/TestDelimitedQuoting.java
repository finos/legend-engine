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

public class TestDelimitedQuoting extends AbstractDriverTest {
    @Test
    public void simpleQuoting() {
        runTest("Hello", "The", "World", "'Hello','The','World'");
    }

    @Test
    public void ifQuotesAreUsedThereCanBeWhitespaceAroundDelimiters() {
        runTest("Hello", "The", "World", "'Hello'\t , 'The' ,  'World'");
    }

    @Test
    public void ifQuotesAreNotUsedWhitespaceAroundDelimitersIsPartOfValue() {
        runTest("Hello\t", " The ", "  World", "Hello\t, The ,  World");
    }

    @Test
    public void ifQuotesAreNotUsedAndOnlyWhitespaceIsBetweenDelimitersTheWhitespaceIsTheValue()
    {
        runTest("Hello", " ", "World", "Hello, ,World");
        runTest("Hello", "  ", "World", "Hello,  ,World");
    }

    @Test
    public void quoteInsideValueIsJustAChar() {
        runTest("Hello 'World'", "and", "Bye", "Hello 'World','and','Bye'");
    }

    @Test
    public void delimiterInQuotesIsPartOfValue() {
        runTest("Hello, World", "and", "Bye", "'Hello, World','and','Bye'");
    }

    @Test
    public void eolInQuotesIsPartOfValue() {
        runTest("Hello\r\nWorld", "and", "Bye", "'Hello\r\nWorld','and','Bye'");
        runTest("Hello\nWorld", "and", "Bye", "'Hello\nWorld','and','Bye'");
        runTest("Hello\rWorld", "and", "Bye", "'Hello\rWorld','and','Bye'");
    }

    @Test
    public void twoQuotesIsAnEscapedQuote() {
        runTest("'", "X", "Y", "'''','X','Y'");
        runTest("''", "X", "Y","'''''','X','Y'");
        runTest("Hello, 'World'", "and", "Bye", "'Hello, ''World''','and','Bye'");
    }

    @Test
    public void quotedFieldShouldBeFollowedByDelimiter() {
        runTestInvalid("Unexpected text after closing quote in value 1 at line 2", "'Hello' World,'Bye'");
    }

    @Test
    public void quotesShouldBeTerminated() {
        runTestInvalid("Unclosed quotes in value 2 at line 2", "'Hello','W");
    }

    private void runTest(String expectedField1, String expectedField2, String expectedField3, String rawLine) {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : ',';\n" +
                "  quoteChar       : '\\'';\n" +
                "  nullString      : '';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "     FIELD1 : STRING;\n" +
                "     FIELD2 : STRING;\n" +
                "     FIELD3 : STRING;\n" +
                "  }\n" +
                "}\n"
        );

        List<IChecked<Data>> records = deserialize(Data.class, flatData, data("\n", "FIELD1,FIELD2,FIELD3", rawLine));
        Assert.assertEquals(1, records.size());
        IChecked<Data> record = records.get(0);
        assertNoDefects(record);
        Assert.assertEquals(expectedField1, record.getValue().field1);
        Assert.assertEquals(expectedField2, record.getValue().field2);
        Assert.assertEquals(expectedField3, record.getValue().field3);
    }

    private void runTestInvalid(String expectedDefect, String rawLine) {
        FlatData flatData = parseFlatData("section default: DelimitedWithHeadings\n" +
                "{\n" +
                "  scope.untilEof;\n" +
                "  delimiter       : ',';\n" +
                "  quoteChar       : '\\'';\n" +
                "  nullString      : '';\n" +
                "\n" +
                "  Record\n" +
                "  {\n" +
                "     FIELD1 : STRING;\n" +
                "     FIELD2 : STRING;\n" +
                "  }\n" +
                "}\n"
        );

        List<IChecked<Data>> records = deserialize(Data.class, flatData, data("\n", "FIELD1,FIELD2", rawLine));
        Assert.assertEquals(1, records.size());
        assertHasDefect("Critical", expectedDefect, records.get(0));
    }

    public static class Data {
        public String field1;
        public String field2;
        public String field3;
    }
}
