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


package org.finos.legend.engine.language.hostedService.compiler.toPureGraph.test;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;

public class TestHostedServiceCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::Name {}\n" +
        "###Mapping\n" +
        "Mapping anything::somethingelse ()\n" +
        "###HostedService\n" +
        "HostedService anything::Name\n" +
        "{" +
        "   ownership : 17;\n" +
        "   pattern : '/a/b';" +
        "   documentation : 'blah';" +
        "   function : a::f():String[1];" +
        "   autoActivateUpdates : true;" +
        "}\n";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:108]: Duplicated element 'anything::Name'";
    }
}
