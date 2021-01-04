// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.test.roundtrip;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestRuntimeGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testRuntime()
    {
        test("###Runtime\n" +
                "Runtime test::runtime1\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping::someMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "Runtime test::runtime2\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping::someMapping,\n" +
                "    test::mapping::someMapping2\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "Runtime test::runtime3\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping::someMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    test::someStore:\n" +
                "    [\n" +
                "      id1: test::connection\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "Runtime test::runtime4\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping::someMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    test::someStore:\n" +
                "    [\n" +
                "      id1: test::connection,\n" +
                "      id2: test::connection\n" +
                "    ],\n" +
                "    test::someStore2:\n" +
                "    [\n" +
                "      id3: test::connection\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n");
    }

    @Test
    public void testRuntimeWithImport()
    {
        test("Class meta::mySimpleClass\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping meta::mySimpleMapping\n" +
                "(\n" +
                "  *meta::goes[meta_goes]: Pure\n" +
                "  {\n" +
                "    name: 'hi'\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Connection\n" +
                "JsonModelConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  class: model::firm::Person;\n" +
                "  url: 'my_url';\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "import meta::*;\n" +
                "Runtime meta::mySimpleRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    mySimpleMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      id3: test::connection,\n" +
                "      id4:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: mySimpleClass;\n" +
                "          url: 'my_url';\n" +
                "        }\n" +
                "      }#,\n" +
                "      id5:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: meta::mySimpleClass;\n" +
                "          url: 'my_url';\n" +
                "        }\n" +
                "      }#,\n" +
                "      id6:\n" +
                "      #{\n" +
                "        ModelChainConnection\n" +
                "        {\n" +
                "          mappings: [\n" +
                "            mapping::MyMapping1,\n" +
                "            mapping::MyMapping2\n" +
                "          ];\n" +
                "        }\n" +
                "      }#\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n");
    }

    @Test
    public void testQuotedRuntime()
    {
        test("Class meta::mySimpleClass\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping meta::mySimpleMapping\n" +
                "(\n" +
                "  *meta::goes[meta_goes]: Pure\n" +
                "  {\n" +
                "    name: 'hi'\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Connection\n" +
                "JsonModelConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  class: model::firm::Person;\n" +
                "  url: 'my_url';\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "import meta::*;\n" +
                "Runtime meta::mySimpleRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    mySimpleMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    'my Store':\n" +
                "    [\n" +
                "      'id 1': 'my Simple Connection',\n" +
                "      'id 2': test::'a connection'\n" +
                "    ],\n" +
                "    'Model Store':\n" +
                "    [\n" +
                "      id3: test::connection,\n" +
                "      id4:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: mySimpleClass;\n" +
                "          url: 'my_url';\n" +
                "        }\n" +
                "      }#,\n" +
                "      id5:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: meta::mySimpleClass;\n" +
                "          url: 'my_url';\n" +
                "        }\n" +
                "      }#,\n" +
                "      id6:\n" +
                "      #{\n" +
                "        ModelChainConnection\n" +
                "        {\n" +
                "          mappings: [\n" +
                "            mapping::MyMapping1,\n" +
                "            mapping::MyMapping2\n" +
                "          ];\n" +
                "        }\n" +
                "      }#\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n");
    }
}
