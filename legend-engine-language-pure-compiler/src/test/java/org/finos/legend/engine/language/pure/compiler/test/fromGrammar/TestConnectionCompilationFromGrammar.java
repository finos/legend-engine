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

public class TestConnectionCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Connection\n" +
                "JsonModelConnection anything::class\n" +
                "{\n" +
                "  class : anything::class;\n" +
                "  url : 'asd';\n" +
                "}\n";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-9:1]: Duplicated element 'anything::class'";
    }

    @Test
    public void testJsonModelConnection()
    {
        test("###Connection\n" +
                "JsonModelConnection test::connection\n" +
                "{\n" +
                "  class : test::someClass;" +
                "  url : 'asd';\n" +
                "}\n", "COMPILATION error at [4:11-25]: Can't find class 'test::someClass'"
        );
    }

    @Test
    public void testXmlModelConnection()
    {
        test("###Connection\n" +
                "XmlModelConnection test::connection\n" +
                "{\n" +
                "  class : test::someClass;" +
                "  url : 'asd';\n" +
                "}\n", "COMPILATION error at [4:11-25]: Can't find class 'test::someClass'"
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
                // model connection class
                "  class: myClass;\n" +
                "  url: 'my_url';\n" +
                "}\n" +
                "\n" +
                "XmlModelConnection meta::mySimpleConnection3\n" +
                "{\n" +
                // model connection class
                "  class: myClass;\n" +
                "  url: 'my_url';\n" +
                "}\n" +
                "\n");
    }
}
