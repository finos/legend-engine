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

public class TestConnectionGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testConnection()
    {
        test("###Connection\n" +
                "JsonModelConnection meta::mySimpleConnection1\n" +
                "{\n" +
                "  class: model::firm::Person;\n" +
                "  url: 'my_url';\n" +
                "}\n\n" +
                "XmlModelConnection meta::mySimpleConnection2\n" +
                "{\n" +
                "  class: model::firm::Person;\n" +
                "  url: 'my_url';\n" +
                "}\n\n" +
                "ModelChainConnection meta::modelChainConnection\n" +
                "{\n" +
                "  mappings: [\n" +
                "    mapping::MyMapping1,\n" +
                "    mapping::MyMapping2\n" +
                "  ];\n" +
                "}\n\n" +
                "ModelChainConnection meta::modelChainConnection2\n" +
                "{\n" +
                "  mappings: [\n" +
                "  ];\n" +
                "}\n"
        );
    }

    @Test
    public void testConnectionWithImport()
    {
        test("Class meta::myClass\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping meta::myMapping\n" +
                "(\n" +
                ")\n\n\n" +
                "###Connection\n" +
                "import meta::*;\n" +
                "JsonModelConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  class: myClass;\n" +
                "  url: 'my_url';\n" +
                "}\n" +
                "\n" +
                "JsonModelConnection meta::mySimpleConnection2\n" +
                "{\n" +
                "  class: ui::myClass;\n" +
                "  url: 'my_url';\n" +
                "}\n" +
                "\n" +
                "XmlModelConnection meta::mySimpleConnection3\n" +
                "{\n" +
                "  class: myClass;\n" +
                "  url: 'my_url';\n" +
                "}\n\n" +
                "ModelChainConnection meta::modelChainConnection\n" +
                "{\n" +
                "  mappings: [\n" +
                "    myMapping\n" +
                "  ];\n" +
                "}\n");
    }
}
