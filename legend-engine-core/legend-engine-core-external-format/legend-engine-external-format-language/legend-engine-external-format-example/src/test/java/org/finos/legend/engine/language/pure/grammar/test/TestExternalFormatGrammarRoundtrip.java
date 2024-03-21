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

package org.finos.legend.engine.language.pure.grammar.test;

import org.junit.Test;

public class TestExternalFormatGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testSchemas()
    {
        test("###ExternalFormat\n" +
                "SchemaSet test::Example\n" +
                "{\n" +
                "  format: Example;\n" +
                "  schemas: [\n" +
                "    {\n" +
                "      content: 'Sample Schema Description\\nsome data\\nis described here\\n';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "SchemaSet test::Example2\n" +
                "{\n" +
                "  format: Example;\n" +
                "  schemas: [\n" +
                "    {\n" +
                "      id: ex2_1;\n" +
                "      location: 'ex2_1.schema';\n" +
                "      content: 'Second Sample Schema Description\\nSchema 1 of Example 2';\n" +
                "    },\n" +
                "    {\n" +
                "      id: ex2_2;\n" +
                "      location: 'ex2_2.schema';\n" +
                "      content: 'Third Sample Schema Description\\nSchema 2 of Example 2';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n"
        );
    }

    @Test
    public void testValidSchemaAndBinding()
    {
        test("###ExternalFormat\n" +
                "SchemaSet test::Example\n" +
                "{\n" +
                "  format: Example;\n" +
                "  schemas: [\n" +
                "    {\n" +
                "      content: 'Schema Description';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "Binding test::ExampleBinding\n" +
                "{\n" +
                "  schemaSet: test::Example;\n" +
                "  contentType: 'text/example';\n" +
                "  modelIncludes: [\n" +
                "    my::ClassA,\n" +
                "    my::ClassB\n" +
                "  ];\n" +
                "  modelExcludes: [\n" +
                "    my::ClassC\n" +
                "  ];\n" +
                "}\n"
        );
    }

    @Test
    public void testValidSchemaAndBindingWithId()
    {
        test("###ExternalFormat\n" +
                "SchemaSet test::Example\n" +
                "{\n" +
                "  format: Example;\n" +
                "  schemas: [\n" +
                "    {\n" +
                "      id: s1;\n" +
                "      content: 'Schema Description';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "Binding test::ExampleBinding\n" +
                "{\n" +
                "  schemaSet: test::Example;\n" +
                "  schemaId: s1;\n" +
                "  contentType: 'text/example';\n" +
                "  modelIncludes: [\n" +
                "    my::ClassA,\n" +
                "    my::ClassB\n" +
                "  ];\n" +
                "  modelExcludes: [\n" +
                "    my::ClassC\n" +
                "  ];\n" +
                "}\n"
        );
    }

    @Test
    public void testValidSchemalessBinding()
    {
        test("###ExternalFormat\n" +
                "Binding test::ExampleBinding\n" +
                "{\n" +
                "  contentType: 'text/example';\n" +
                "  modelIncludes: [\n" +
                "    my::ClassA,\n" +
                "    my::ClassB\n" +
                "  ];\n" +
                "  modelExcludes: [\n" +
                "    my::ClassC\n" +
                "  ];\n" +
                "}\n"
        );
    }
}
