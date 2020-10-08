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

package org.finos.legend.engine.language.pure.compiler.test.fromGrammar;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

public class TestRuntimeCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Runtime\n" +
                "Runtime anything::class\n" +
                "{\n" +
                " mappings: [anything::somethingelse];\n" +
                "}";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-8:1]: Duplicated element 'anything::class'";
    }

    @Test
    public void testRuntime()
    {
        String resource = "Class test::class\n" +
                "{\n" +
                "  ok : Integer[0..1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                ")\n" +
                "###Connection\n" +
                "JsonModelConnection test::connection\n" +
                "{\n" +
                "  class : test::class;" +
                "  url : 'asd';\n" +
                "}\n";
        // Unknown mapping
        test(resource +
                "###Runtime\n" +
                "Runtime test::runtime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping2\n" +
                "  ];\n" +
                "}\n", "COMPILATION error at [19:5-18]: Can't find mapping 'test::mapping2'");
        // Runtime does not cover any mapping
        test(resource +
                "###Runtime\n" +
                "Runtime test::runtime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "  ];\n" +
                "}\n", "COMPILATION error at [15:1-20:1]: Runtime must cover at least one mapping");
        // Unknown connection pointer
        test(resource +
                "###Runtime\n" +
                "Runtime test::runtime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "      [\n" +
                "        ModelStore: [id1: test::connection2]\n" +
                "      ];\n" +
                "}\n", "COMPILATION error at [23:27-43]: Can't find connection 'test::connection2'");
        // check compilation for an embedded connection
        test(resource +
                "###Runtime\n" +
                "Runtime test::runtime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping\n" +
                "  ];\n" +
                "      connections: [\n" +
                "      ModelStore: [id1: #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: test::class2;\n" +
                "          url: 'my_url';\n" +
                "        }\n" +
                "      }#]\n" +
                "   ];\n" +
                "}\n", "COMPILATION error at [25:18-29]: Can't find class 'test::class2'");
        // check walker source information processing for embedded connection
        test(resource +
                "###Runtime\n" +
                "Runtime test::runtime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping\n" +
                "  ];\n" +
                "      connections: [\n" +
                // intentionally put the embedded connection in one line to check walker source information processing for island grammar
                "      ModelStore: [id1: #{ JsonModelConnection { class:                   test::class2; url: 'my_url'; }}#]\n" +
                "   ];\n" +
                "}\n", "COMPILATION error at [22:75-86]: Can't find class 'test::class2'");
        // cannot find store used for indexing
        test(resource +
                "###Runtime\n" +
                "Runtime test::runtime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping\n" +
                "  ];\n" +
                "      connections: [\n" +
                "      ModelStore2: [id1: #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: test::class2;\n" +
                "          url: 'my_url';\n" +
                "        }\n" +
                "      }#]\n" +
                "   ];\n" +
                "}\n", "COMPILATION error at [22:7-17]: Can't find store 'ModelStore2'");
    }

    @Test
    public void testRuntimeWithImport()
    {
        test("Class meta::mySimpleClass\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n\n" +
                "###Mapping\n" +
                "Mapping meta::mySimpleMapping\n" +
                "(\n" +
                ")\n\n\n" +
                "###Connection\n" +
                "JsonModelConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  class: meta::mySimpleClass;\n" +
                "  url: 'my_url';\n" +
                "}\n\n\n" +
                "###Runtime\n" +
                "import meta::*;\n" +
                "Runtime meta::mySimpleRuntime\n" +
                "{\n" +
                "  connections:\n" +
                "  [\n" +
                "  ModelStore :   [\n" +
                // connection pointer
                "  id3: mySimpleConnection,\n" +
                "  id4: #{\n" +
                "    JsonModelConnection\n" +
                "    {\n" +
                // check import resolution for embedded connections in runtime
                "      class: mySimpleClass;\n" +
                "      url: 'my_url';\n" +
                "    }\n" +
                "  }#\n" +
                "  ]\n" +
                "];\n" +
                "  mappings   : \n" +
                // embedded runtime mapping
                "  [mySimpleMapping\n" +
                "  ];\n" +
                "}\n");
    }
}
