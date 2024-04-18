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

import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

import java.util.Collections;

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

    @Test
    public void testSingleConnectionRuntimeCompilation()
    {
        String resource = "Class test::class\n" +
                "{\n" +
                "  ok : Integer[0..1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping test::mapping::someMapping\n" +
                "(\n" +
                ")\n" +
                "###Connection\n" +
                "JsonModelConnection com::test::connection\n" +
                "{\n" +
                "  class : test::class;" +
                "  url : 'asd';\n" +
                "}\n";
        test(resource + "###Runtime\n" +
                "SingleConnectionRuntime com::test::singleConnectionRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping::someMapping\n" +
                "  ];\n" +
                "  connection: com::test::connection;\n" +
                "}\n"
        );
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
                "}\n", null, Lists.mutable.with("COMPILATION error at [15:1-20:1]: Runtime must cover at least one mapping"));
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
                "}\n", "COMPILATION error at [22:7-17]: Can't find the packageable element 'ModelStore2'");
    }

    @Test
    public void testRuntimeWithConnectionStores()
    {
        test("Class meta::mySimpleClass\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n\n" +
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
                "  connectionStores:\n" +
                "  [\n" +
                // connection pointer
                "  meta::mySimpleConnection : [\n" +
                "     ModelStore\n" +
                "    ]\n" +
                "  ];\n" +
                "  mappings : \n" +
                // embedded runtime mapping
                "  [meta::mySimpleMapping\n" +
                "  ];\n" +
                "}\n");
    }

    @Test
    public void testRuntimeWithImport()
    {
        test("Class meta::mySimpleClass\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n\n" +
                "Class meta::mySimpleClass2\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n\n" +
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
                "      class: mySimpleClass2;\n" +
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

    @Test
    public void testForMultipleModelChainConnectionsWithinSameStore()
    {
        test("###Pure\n" +
                "Class humanResourceModel::students\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class humanResourceModel::class\n" +
                "{\n" +
                "  studentname: String[1];\n" +
                "}\n" +
                "\n" +
                "Class humanResourceModel::school\n" +
                "{\n" +
                "  studentname: String[1];\n" +
                "}\n" +
                "\n" +
                "Class humanResourceModel::student\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "}\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping humanResourceModel::M2M_Mapping\n" +
                "(\n" +
                "  *humanResourceModel::school: Pure\n" +
                "  {\n" +
                "    ~src humanResourceModel::class\n" +
                "    studentname: $src.studentname\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "Mapping humanResourceModel::M2M_Mapping2\n" +
                "(\n" +
                "  *humanResourceModel::class: Pure\n" +
                "  {\n" +
                "    ~src humanResourceModel::students\n" +
                "    studentname: $src.name->toUpper()\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "Mapping humanResourceModel::M2M_Mapping3\n" +
                "(\n" +
                "  *humanResourceModel::student: Pure\n" +
                "  {\n" +
                "    ~src humanResourceModel::students\n" +
                "    firstName: $src.name->substring(0, $src.name->indexOf(' '))\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "###Connection\n" +
                "ModelChainConnection humanResourceModel::M2MModelChainConnection\n" +
                "{\n" +
                "  mappings: [\n" +
                "    humanResourceModel::M2M_Mapping3\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "ModelChainConnection humanResourceModel::M2MModelChainConnection2\n" +
                "{\n" +
                "  mappings: [\n" +
                "    humanResourceModel::M2M_Mapping3\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "JsonModelConnection humanResourceModel::JsonModelConnection\n" +
                "{\n" +
                "  class: humanResourceModel::students;\n" +
                "  url: 'data:application/json,{\"name\": \"John Johnson\"}';\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime humanResourceModel::M2MModelRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    humanResourceModel::M2M_Mapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection_1: humanResourceModel::M2MModelChainConnection,\n" +
                "      connection_2: humanResourceModel::M2MModelChainConnection2\n" +
                "    ]\n" +
                "  ];\n" +
                "}",null, Collections.singletonList("COMPILATION error at [73:1-87:1]: Multiple ModelChainConnections are Not Supported for the same Runtime."));
    }

    @Test
    public void testToCheckForMultipleModelConnectionsForSameSourceClass()
    {
        test("###Pure\n" +
                "Class modelToModel::test::Firm\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class modelToModel::test::_Firm\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "Class modelToModel::test::__Firm\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping modelToModel::test::BridgeToDestMapping\n" +
                "(\n" +
                "  modelToModel::test::Firm: Pure\n" +
                "  {\n" +
                "    ~src modelToModel::test::_Firm\n" +
                "    name: '|' + $src.name + '|'\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "Mapping modelToModel::test::SrcToBridgeMapping\n" +
                "(\n" +
                "  modelToModel::test::_Firm: Pure\n" +
                "  {\n" +
                "    ~src modelToModel::test::__Firm\n" +
                "    name: '$' + $src.name + '$'\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Connection\n" +
                "ModelChainConnection modelToModel::test::OneMappingConnection\n" +
                "{\n" +
                "  mappings: [\n" +
                "    modelToModel::test::SrcToBridgeMapping\n" +
                "  ];\n" +
                "}\n" +
                "\n" +
                "XmlModelConnection modelToModel::test::XmlConnection\n" +
                "{\n" +
                "  class: modelToModel::test::__Firm;\n" +
                "  url: 'data:application/xml,<__Firm><name>FirmB</name></__Firm>';\n" +
                "}\n" +
                "\n" +
                "JsonModelConnection modelToModel::test::JsonConnection\n" +
                "{\n" +
                "  class: modelToModel::test::__Firm;\n" +
                "  url: 'data:application/json,{\"name\":\"FirmA\"}';\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime modelToModel::test::DemoM2MModelChainConnectionRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    modelToModel::test::BridgeToDestMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection_1: modelToModel::test::OneMappingConnection,\n" +
                "      connection_2: modelToModel::test::JsonConnection,\n" +
                "      connection_3: modelToModel::test::XmlConnection\n" +
                "    ]\n" +
                "  ];\n" +
                "}",null,Collections.singletonList("COMPILATION error at [60:1-75:1]: Multiple Connections available for Source Class - __Firm"));
    }
}
