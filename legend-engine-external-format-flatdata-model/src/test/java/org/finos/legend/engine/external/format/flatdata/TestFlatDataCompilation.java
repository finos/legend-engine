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

package org.finos.legend.engine.external.format.flatdata;

import org.finos.legend.engine.external.shared.format.model.test.ExternalSchemaCompilationTest;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.junit.Test;

public class TestFlatDataCompilation extends ExternalSchemaCompilationTest
{
    @Test
    public void testExternalFormat()
    {
        testFlatData("section readLines: ImmaterialLines\n" +
                             "{\n" +
                             "  scope.forNumberOfLines: 2;\n" +
                             "  recordSeparator: ';';\n" +
                             "}\n" +
                             "\n" +
                             "section sectionName: DelimitedWithHeadings\n" +
                             "{\n" +
                             "  scope.untilEof;\n" +
                             "  recordSeparator: ';';\n" +
                             "  delimiter: ',';\n" +
                             "  quoteChar: '\\'';\n" +
                             "  escapingChar: '\\'';\n" +
                             "  nullString: 'null';\n" +
                             "  mayContainBlankLines;\n" +
                             "\n" +
                             "  Record\n" +
                             "  {\n" +
                             "    HEADING1: STRING;\n" +
                             "    HEADING2: INTEGER(optional);\n" +
                             "    HEADING3: BOOLEAN(trueString='Y', falseString='N', optional);\n" +
                             "    'My Fourth Heading': DECIMAL(optional);\n" +
                             "    HEADING5: DATE(format='MM-DD-EE');\n" +
                             "    HEADING6: DATETIME(format='MM-DD-EE');\n" +
                             "  }\n" +
                             "}\n" +
                             "\n" +
                             "section sectionName2: DelimitedWithoutHeadings\n" +
                             "{\n" +
                             "  scope.forNumberOfLines: 1;\n" +
                             "  recordSeparator: '\\n';\n" +
                             "  delimiter: ',';\n" +
                             "  quoteChar: '\\'';\n" +
                             "  escapingChar: '\\'';\n" +
                             "  nullString: 'None';\n" +
                             "\n" +
                             "  Record\n" +
                             "  {\n" +
                             "    A {1}: STRING;\n" +
                             "    B {2}: INTEGER(format='#,##0', optional);\n" +
                             "    C {3}: DECIMAL(format='#,##0.0#', optional);\n" +
                             "    D {4}: BOOLEAN(optional);\n" +
                             "    E {5}: DATE(format='MM-DD-EE');\n" +
                             "    F {6}: DATETIME(format='MM-DD-EE');\n" +
                             "  }\n" +
                             "}"
        );
    }

    @Test
    public void testOnlyOneRecordType()
    {
        testFlatData("section sectionName: DelimitedWithoutHeadings\n" +
                             "{\n" +
                             "  Record \n" +
                             "  {\n" +
                             "    A {1}: STRING;\n" +
                             "  }\n" +
                             "  Record\n" +
                             "  {\n" +
                             "    A {1}: STRING;\n" +
                             "  }\n" +
                             "}",
                     "COMPILATION error at [5:16-159]: Error in schema content [7:3-10:3]: Only one Record is permitted in a section");
    }

    @Test
    public void testNoSections()
    {
        testFlatData("",
                     "COMPILATION error at [5:16-27]: Error in schema content [1:1]: Must specify at least one section");
    }

    @Test
    public void testDuplicateSectionName()
    {
        testFlatData("section sameSectionName: DelimitedWithHeadings\n" +
                             "{\n" +
                             "  scope.untilEof;\n" +
                             "\n" +
                             "  Record\n" +
                             "  {\n" +
                             "    HEADING1: STRING;\n" +
                             "  }\n" +
                             "}\n" +
                             "section sameSectionName: DelimitedWithHeadings\n" +
                             "{\n" +
                             "  scope.foNumberOfLines: 1;\n" +
                             "\n" +
                             "  Record\n" +
                             "  {\n" +
                             "      HEADING1: STRING;\n" +
                             "  }\n" +
                             "}",
                     "COMPILATION error at [5:16-273]: Error in schema content [10:9]: Duplicated section name 'sameSectionName'");
    }

    @Test
    public void duplicatedDataTypeAttribute()
    {
        testFlatData("section sectionName: DelimitedWithHeadings\n" +
                             "{\n" +
                             "  scope.untilEof;\n" +
                             "\n" +
                             "  Record\n" +
                             "  {\n" +
                             "    HEADING1: BOOLEAN(trueString='Y', trueString='true');\n" +
                             "  }\n" +
                             "}",
                     "COMPILATION error at [5:16-179]: Error in schema content [7:39]: Attribute 'trueString' duplicated for record type property"
        );
    }

    @Test
    public void duplicatedDataTypeOptional()
    {
        testFlatData("section sectionName: DelimitedWithHeadings\n" +
                             "{\n" +
                             "  scope.untilEof;\n" +
                             "\n" +
                             "  Record\n" +
                             "  {\n" +
                             "    HEADING1: BOOLEAN(optional, optional);\n" +
                             "  }\n" +
                             "}",
                     "COMPILATION error at [5:16-160]: Error in schema content [7:33]: Attribute 'optional' duplicated for record type property"
        );
    }

    @Test
    public void unknownDataTypeAttributes()
    {
        testFlatData("section sectionName: DelimitedWithHeadings\n" +
                             "{\n" +
                             "  scope.untilEof;\n" +
                             "\n" +
                             "  Record\n" +
                             "  {\n" +
                             "    HEADING1: BOOLEAN(yesString='Y', noString='N');\n" +
                             "  }\n" +
                             "}",
                     "COMPILATION error at [5:16-173]: Error in schema content [7:23]: Unexpected token: yesString"
        );
    }

    @Test
    public void unknownDataType()
    {
        testFlatData("section sectionName: DelimitedWithHeadings\n" +
                             "{\n" +
                             "  scope.untilEof;\n" +
                             "\n" +
                             "  Record\n" +
                             "  {\n" +
                             "    HEADING1: BLAH;\n" +
                             "  }\n" +
                             "}",
                     "COMPILATION error at [5:16-137]: Error in schema content [7:15]: Unexpected token: BLAH"
        );
    }

    @Test
    public void driverValiationFailure()
    {
        testFlatData("section sectionName: DelimitedWithHeadings\n" +
                             "{\n" +
                             "  Record\n" +
                             "  {\n" +
                             "    HEADING1: STRING;\n" +
                             "  }\n" +
                             "}",
                     "COMPILATION error at [5:16-118]: Error in schema content: scope not specified in section 'sectionName', delimiter not specified in section 'sectionName'"
        );
    }

    public void testFlatData(String flatData)
    {
        testFlatData(flatData, null);
    }

    public void testFlatData(String flatData, String expectedError)
    {
        test("###ExternalFormat\n" +
                     "SchemaSet test::Example1\n" +
                     "{\n" +
                     "  format: FlatData;\n" +
                     "  schemas: [ { content: " + PureGrammarComposerUtility.convertString(flatData, true) + "; } ];\n" +
                     "}\n",
             expectedError
        );
    }
}
