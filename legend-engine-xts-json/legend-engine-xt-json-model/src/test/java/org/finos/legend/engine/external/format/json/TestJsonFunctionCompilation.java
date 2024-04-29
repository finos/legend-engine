//  Copyright 2023 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.external.format.json;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

public class TestJsonFunctionCompilation
{
    @Test
    public void testToJson()
    {
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(completeGrammar("data:String[1] | demo::Person->fromJson($data)"));

        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(completeGrammar("data:Byte[*] | demo::Person->fromJson($data)"));
    }

    @Test
    public void testFromJson()
    {
        TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test(completeGrammar("data:String[1] | demo::Person->fromJson($data)->toJson(#{demo::Person{prop}}#)"));
    }

    private String completeGrammar(String funcExp)
    {
        return
                "Class demo::Person\n" +
                        "{\n" +
                        "  prop: String[1];\n" +
                        "}\n" +
                        "function demo::testFunc():Any[1]\n" +
                        "{\n" +
                        funcExp +
                        "}\n";
    }
}
