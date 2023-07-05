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

package org.finos.legend.engine.external.format.xml.test;

import org.finos.legend.engine.external.shared.format.model.test.ExternalSchemaCompilationTest;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility;
import org.junit.Test;

public class TestXsdCompilation extends ExternalSchemaCompilationTest
{
    @Test
    public void testExternalFormat()
    {
        test("###ExternalFormat\n" +
                "SchemaSet test::Example1\n" +
                "{\n" +
                "  format: XSD;\n" +
                "  schemas: [\n" +
                "    {\n" +
                "        location: 'filename1.xsd';\n" +
                "        content: " + PureGrammarComposerUtility.convertString("<?xml version=\"1.0\"?>\n" +
                "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "  <xs:element name=\"person\">\n" +
                "    <xs:complexType name=\"Square\">\n" +
                "      <xs:sequence>\n" +
                "        <xs:element name=\"first-name\" type=\"xs:token\" />\n" +
                "        <xs:element name=\"last-name\" type=\"xs:token\" />\n" +
                "      </xs:sequence>\n" +
                "    </xs:complexType>\n" +
                "  </xs:element>\n" +
                "</xs:schema>", true) + ";\n" +
                "    },\n" +
                "    {\n" +
                "        location: 'filename2.xsd';\n" +
                "        content: " + PureGrammarComposerUtility.convertString("<?xml version=\"1.0\"?>\n" +
                "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                "  <xs:element name=\"person\">\n" +
                "    <xs:complexType name=\"Square\">\n" +
                "      <xs:sequence>\n" +
                "        <xs:element name=\"first-name\" type=\"xs:token\" />\n" +
                "        <xs:element name=\"last-name\" type=\"xs:token\" />\n" +
                "      </xs:sequence>\n" +
                "    </xs:complexType>\n" +
                "  </xs:element>\n" +
                "</xs:schema>", true) + ";\n" +
                "    }\n" +
                "  ];\n" +
                "}\n"
        );
    }

    @Test
    public void testMissingSchemaLocation()
    {
        test("###ExternalFormat\n" +
                        "SchemaSet test::Example1\n" +
                        "{\n" +
                        "  format: XSD;\n" +
                        "  schemas: [\n" +
                        "    { content: " + PureGrammarComposerUtility.convertString("<?xml version=\"1.0\"?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "  <xs:element name=\"person\">\n" +
                        "    <xs:complexType name=\"Square\">\n" +
                        "      <xs:sequence>\n" +
                        "        <xs:element name=\"first-name\" type=\"xs:token\" />\n" +
                        "        <xs:element name=\"last-name\" type=\"xs:token\" />\n" +
                        "      </xs:sequence>\n" +
                        "    </xs:complexType>\n" +
                        "  </xs:element>\n" +
                        "</xs:schema>", true) + "; }" +
                        "  ];\n" +
                        "}\n",
                "COMPILATION error at [6:7-374]: Error in schema content: Location must be specified for XSD schemas"
        );
    }

    @Test
    public void testNotWellFormedSchema()
    {
        test("###ExternalFormat\n" +
                        "SchemaSet test::Example1\n" +
                        "{\n" +
                        "  format: XSD;\n" +
                        "  schemas: [\n" +
                        "    {\n" +
                        "        location: 'filename1.xsd';\n" +
                        "        content: " + PureGrammarComposerUtility.convertString("<?xml version=\"1.0\"?>\n" +
                        "<xs:schema xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
                        "  <xs:element name=\"person\">\n" +
                        "    <xs:complexType name=\"Square\">\n" +
                        "      <xs:sequence>\n" +
                        "        <xs:element name=\"first-name\" type=\"xs:token\" />\n" +
                        "        <xs:element name=\"last-name\" type=\"xs:token\" />\n" +
                        "    </xs:complexType>\n" +
                        "  </xs:element>\n" +
                        "</xs:schema>", true) + ";\n" +
                        "    }\n" +
                        "  ];\n" +
                        "}\n",
                "COMPILATION error at [8:9-354]: Error in schema content [8:7]: The element type \"xs:sequence\" must be terminated by the matching end-tag \"</xs:sequence>\"."
        );
    }
}
