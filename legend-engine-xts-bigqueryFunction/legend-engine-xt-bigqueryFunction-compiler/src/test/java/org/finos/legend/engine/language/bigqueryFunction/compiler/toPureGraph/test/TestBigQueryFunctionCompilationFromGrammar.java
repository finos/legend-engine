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

package org.finos.legend.engine.language.bigqueryFunction.compiler.toPureGraph.test;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

public class TestBigQueryFunctionCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::Name {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###BigQuery\n" +
                "BigQueryFunction anything::Name\n" +
                "{" +
                "   functionName : 'name';\n" +
                "   function : a::f():String[1];" +
                "}\n";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:32]: Duplicated element 'anything::Name'";
    }

    @Test
    public void testHappyPath()
    {
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(
                "function a::f():String[1]{'ok';}\n" +
                        "###BigQuery\n" +
                        "BigQueryFunction app::pack::MyApp\n" +
                        "{" +
                        "   functionName : 'name';\n" +
                        "   function : a::f():String[1];" +
                        "   ownership : Deployment { identifier: 'pierre' };\n" +
                        "}\n", null);
    }

    @Test
    public void testFunctionError()
    {
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(
                "function a::f():String[1]{'ok';}\n" +
                        "###BigQuery\n" +
                        "BigQueryFunction app::pack::MyApp\n" +
                        "{" +
                        "   functionName : 'name';\n" +
                        "   function : a::fz():String[1];" +
                        "}\n", " at [3:1-5:33]: Error in 'app::pack::MyApp': org.finos.legend.engine.shared.core.operational.errorManagement.EngineException: Can't find the packageable element 'a::fz__String_1_'");
    }
}
