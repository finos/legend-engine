// Copyright 2023 Goldman Sachs
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


package org.finos.legend.engine.language.jarService.compiler.toPureGraph.test;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

public class TestJarServiceCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::Name {}\n" +
        "###Mapping\n" +
        "Mapping anything::somethingelse ()\n" +
        "###JarService\n" +
        "JarService anything::Name\n" +
        "{" +
        "   ownership : UserList { users: [\n" +
        "    'user1'\n" +
        "    ] };\n" +
        "   documentation : 'blah';" +
        "   function : a::f():String[1];" +
        "}\n";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-9:58]: Duplicated element 'anything::Name'";
    }

    @Test
    public void testJarServiceGrammar()
    {
        test("###JarService\n" +
                "JarService model::NewActivator\n" +
                "{\n" +
                "   ownership : UserList { users: [\n" +
                "    'user1'\n" +
                "    ] };\n" +
                "   function : model::Firm_QueryFunction():Integer[1];\n" +
                "   documentation : '';\n" +
                "}\n" +
                "###Pure\n" +
                "function model::Firm_QueryFunction(): Integer[1]\n" +
                "{\n" +
                "  1 + 1;\n" +
                "}");
    }
}
