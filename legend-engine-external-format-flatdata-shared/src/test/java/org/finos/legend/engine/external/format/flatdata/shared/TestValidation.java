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

package org.finos.legend.engine.external.format.flatdata.shared;

import org.finos.legend.engine.external.format.flatdata.shared.model.FlatData;
import org.finos.legend.engine.external.format.flatdata.shared.grammar.FlatDataSchemaParser;
import org.finos.legend.engine.external.format.flatdata.shared.validation.FlatDataValidation;
import org.finos.legend.engine.external.format.flatdata.shared.validation.FlatDataValidationResult;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.Collectors;

public class TestValidation
{
    @Test
    public void valid()
    {
        test("section default: DelimitedWithHeadings\n" +
                     "{\n" +
                     "  scope.untilEof;\n" +
                     "  recordSeparator: '\\n';\n" +
                     "  delimiter: ',';\n" +
                     "  quoteChar: '\\'';\n" +
                     "  escapingChar: '\\'';\n" +
                     "  nullString: 'null';\n" +
                     "\n" +
                     "  Record\n" +
                     "  {\n" +
                     "    HEADING1: STRING;\n" +
                     "    HEADING2: INTEGER;\n" +
                     "    HEADING3: INTEGER(optional);\n" +
                     "    HEADING4: DATE(format='MM-DD-EE');\n" +
                     "  }\n" +
                     "}"
        );
    }

    @Test
    public void invalidDriverId()
    {
        test("section default: InvalidDriver\n" +
                     "{\n" +
                     "}",
             "Invalid driver ID 'InvalidDriver' specified in section 'default'"
        );
    }

    @Test
    public void missingMandatoryProperty()
    {
        test("section default: DelimitedWithHeadings\n" +
                     "{\n" +
                     "  delimiter: ',';\n" +
                     "\n" +
                     "  Record\n" +
                     "  {\n" +
                     "    HEADING1: STRING;\n" +
                     "    HEADING2: INTEGER;\n" +
                     "    HEADING3: INTEGER(optional);\n" +
                     "    HEADING4: DATE(format='MM-DD-EE');\n" +
                     "  }\n" +
                     "}",
             "scope not specified in section 'default'"
        );

        test("section default: DelimitedWithHeadings\n" +
                     "{\n" +
                     "  scope.untilEof;\n" +
                     "\n" +
                     "  Record\n" +
                     "  {\n" +
                     "    HEADING1: STRING;\n" +
                     "    HEADING2: INTEGER;\n" +
                     "    HEADING3: INTEGER(optional);\n" +
                     "    HEADING4: DATE(format='MM-DD-EE');\n" +
                     "  }\n" +
                     "}\n"
                ,
             "delimiter not specified in section 'default'"
        );
    }

    @Test
    public void invalidExclusiveGroup()
    {
        test("section default: DelimitedWithHeadings\n" +
                     "{\n" +
                     "  scope.untilEof;\n" +
                     "  scope.forNumberOfLines: 6;\n" +
                     "  delimiter: ',';\n" +
                     "\n" +
                     "  Record\n" +
                     "  {\n" +
                     "    HEADING1: STRING;\n" +
                     "    HEADING2: INTEGER;\n" +
                     "    HEADING3: INTEGER(optional);\n" +
                     "    HEADING4: DATE(format='MM-DD-EE');\n" +
                     "  }\n" +
                     "}",
             "scope can only have one subvalue in section 'default'"
        );
    }

    @Test
    public void invalidProperty()
    {
        test("section default: DelimitedWithHeadings\n" +
                     "{\n" +
                     "  scope.unknown;\n" +
                     "  delimiter: ',';\n" +
                     "\n" +
                     "  Record\n" +
                     "  {\n" +
                     "    HEADING1: STRING;\n" +
                     "    HEADING2: INTEGER;\n" +
                     "    HEADING3: INTEGER(optional);\n" +
                     "    HEADING4: DATE(format='MM-DD-EE');\n" +
                     "  }\n" +
                     "}",
             "Invalid property 'scope.unknown' in section 'default'"
        );

        test("section default: DelimitedWithHeadings\n" +
                     "{\n" +
                     "  scope.untilEof;\n" +
                     "  recordSeparator: 3;\n" +
                     "  delimiter: ',';\n" +
                     "\n" +
                     "  Record\n" +
                     "  {\n" +
                     "    HEADING1: STRING;\n" +
                     "    HEADING2: INTEGER;\n" +
                     "    HEADING3: INTEGER(optional);\n" +
                     "    HEADING4: DATE(format='MM-DD-EE');\n" +
                     "  }\n" +
                     "}",
             "Invalid recordSeparator: '3' in section 'default'"
        );

        test("section default: DelimitedWithHeadings\n" +
                     "{\n" +
                     "  scope.untilEof;\n" +
                     "  recordSeparator;\n" +
                     "  delimiter: ',';\n" +
                     "\n" +
                     "  Record\n" +
                     "  {\n" +
                     "    HEADING1: STRING;\n" +
                     "    HEADING2: INTEGER;\n" +
                     "    HEADING3: INTEGER(optional);\n" +
                     "    HEADING4: DATE(format='MM-DD-EE');\n" +
                     "  }\n" +
                     "}",
             "Invalid recordSeparator: 'true' in section 'default'"
        );

        test("section default: DelimitedWithHeadings\n" +
                     "{\n" +
                     "  scope.untilEof;\n" +
                     "  mayContainBlankLines: 'maybe';\n" +
                     "  delimiter: ',';\n" +
                     "\n" +
                     "  Record\n" +
                     "  {\n" +
                     "    HEADING1: STRING;\n" +
                     "    HEADING2: INTEGER;\n" +
                     "    HEADING3: INTEGER(optional);\n" +
                     "    HEADING4: DATE(format='MM-DD-EE');\n" +
                     "  }\n" +
                     "}",
             "Invalid mayContainBlankLines: 'maybe' in section 'default'"
        );

        test("section s1: DelimitedWithHeadings\n" +
                     "{\n" +
                     "  scope.untilEof;\n" +
                     "  recordSeparator: '\\n';\n" +
                     "  baloney: 1;\n" +
                     "  delimiter: ',';\n" +
                     "\n" +
                     "  Record\n" +
                     "  {\n" +
                     "    HEADING1: STRING;\n" +
                     "    HEADING2: INTEGER;\n" +
                     "    HEADING3: INTEGER(optional);\n" +
                     "    HEADING4: DATE(format='MM-DD-EE');\n" +
                     "  }\n" +
                     "}",
             "Invalid property 'baloney' in section 's1'"
        );
    }

    @Test
    public void recordTypesPerSection()
    {
        test("section sectionName: ImmaterialLines\n" +
                     "{\n" +
                     "  scope.untilEof;\n" +
                     "  recordSeparator: ';';\n" +
                     "\n" +
                     "  Record\n" +
                     "  {\n" +
                     "    A: STRING;\n" +
                     "  }\n" +
                     "}",
             "Must not specify a record type in section 'sectionName'"
        );

        test("section sectionName: DelimitedWithHeadings\n" +
                     "{\n" +
                     "  scope.untilEof;\n" +
                     "  recordSeparator: ';';\n" +
                     "  delimiter: ',';\n" +
                     "}",
             "Must specify a record type in section 'sectionName'"
        );
    }

    @Test
    public void testAddresses()
    {
        test("section sectionName: DelimitedWithHeadings\n" +
                     "{\n" +
                     "  scope.untilEof;\n" +
                     "  recordSeparator: ';';\n" +
                     "  delimiter: ',';\n" +
                     "  quoteChar: '\\'';\n" +
                     "  escapingChar: '\\'';\n" +
                     "  nullString: 'null';\n" +
                     "\n" +
                     "  Record\n" +
                     "  {\n" +
                     "    Heading {4}: STRING;\n" +
                     "  }\n" +
                     "}",
             "Address should not be specified for Heading in section 'sectionName'"
        );

        test("section sectionName: DelimitedWithoutHeadings\n" +
                     "{\n" +
                     "  scope.untilEof;\n" +
                     "  recordSeparator: ';';\n" +
                     "  delimiter: ',';\n" +
                     "  quoteChar: '\\'';\n" +
                     "  escapingChar: '\\'';\n" +
                     "  nullString: 'null';\n" +
                     "\n" +
                     "  Record\n" +
                     "  {\n" +
                     "    Heading: STRING;\n" +
                     "  }\n" +
                     "}",
             "Address must be specified for Heading in section 'sectionName'"
        );

        test("section sectionName: DelimitedWithoutHeadings\n" +
                     "{\n" +
                     "  scope.untilEof;\n" +
                     "  recordSeparator: ';';\n" +
                     "  delimiter: ',';\n" +
                     "  quoteChar: '\\'';\n" +
                     "  escapingChar: '\\'';\n" +
                     "  nullString: 'null';\n" +
                     "\n" +
                     "  Record\n" +
                     "  {\n" +
                     "    Heading {X}: STRING;\n" +
                     "  }\n" +
                     "}",
             "Invalid address for 'Heading' (Expected column number) in section 'sectionName'"
        );
    }

    private void test(String flatDataGrammar, String... expectedErrors)
    {
        FlatData flatData = new FlatDataSchemaParser(flatDataGrammar).parse();
        FlatDataValidationResult result = FlatDataValidation.validate(flatData);
        if (expectedErrors.length == 0)
        {
            if (!result.isValid())
            {
                Assert.fail("Result should be valid but has defects:\n" + result.getDefects().stream().map(Object::toString).collect(Collectors.joining("\n")));
            }
        }
        else
        {
            Assert.assertFalse("Result should be invalid", result.isValid());
            result.getDefects().forEach(d ->
                                        {
                                            if (!Arrays.asList(expectedErrors).contains(d.toString()))
                                            {
                                                Assert.fail("Unexpected defect: " + d);
                                            }
                                        });
            Arrays.asList(expectedErrors).forEach(e ->
                                                  {
                                                      if (!result.getDefects().stream().anyMatch(d -> e.equals(d.toString())))
                                                      {
                                                          Assert.fail("Missing defect: " + e);
                                                      }
                                                  });
        }
    }

}
