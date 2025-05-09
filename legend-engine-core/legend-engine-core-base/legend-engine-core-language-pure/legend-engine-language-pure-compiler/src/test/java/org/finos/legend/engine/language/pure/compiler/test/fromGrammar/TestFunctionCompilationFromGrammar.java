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

import org.junit.Test;

import static org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite.test;

public class TestFunctionCompilationFromGrammar
{
    @Test
    public void testFunctionTest()
    {

        test("function model::MyFunc(): String[1]\n" +
                "{\n" +
                "  ''\n" +
                "}\n" +
                "{\n" +
                "  testSuite1\n" +
                "  (\n" +
                "      testFail | MyFunc() => (JSON) '[]';\n" +
                "      testPass | MyFunc() => (JSON) '[]';\n" +
                "  )\n" +
                "  testFail | MyFunc() => (JSON) '[]';\n" +
                "  testPass | MyFunc() => (JSON) '[]';\n" +
                "}\n");

        test("function model::MyFunc(): String[1]\n" +
                "{\n" +
                "  ''\n" +
                "}\n" +
                "{\n" +
                "  testSuite1\n" +
                "  (\n" +
                "      testDuplicate | MyFunc() => (JSON) '[]';\n" +
                "      testDuplicate | MyFunc() => (JSON) '[]';\n" +
                "  )\n" +
                "}\n", "COMPILATION error at [6:3-10:3]: Multiple tests found with ids : 'testDuplicate'");
        test("function model::MyFunc(): String[1]\n" +
                "{\n" +
                "  ''\n" +
                "}\n" +
                "{\n" +
                "  duplicateSuite\n" +
                "  (\n" +
                "      testDuplicate | MyFunc() => (JSON) '[]';\n" +
                "  )\n" +
                "  duplicateSuite\n" +
                "  (\n" +
                "      testDuplicate | MyFunc() => (JSON) '[]';\n" +
                "  )\n" +
                "}\n", "COMPILATION error at [1:1-14:1]: Multiple testSuites found with ids : 'duplicateSuite'");

        test("function model::MyFunc(firstName: String[1]): String[1]\n" +
                "{\n" +
                "  ''\n" +
                "}\n" +
                "{\n" +
                "      testDuplicate | MyFunc('Nicole') => (JSON) '[]';\n" +
                "}\n");

        test("function model::MyFunc(firstName: String[1]): String[1]\n" +
                "{\n" +
                "  ''\n" +
                "}\n" +
                "{\n" +
                "  testDuplicate | MyFunc() => (JSON) '[]';\n" +
                "}\n", "COMPILATION error at [6:3-42]: Parameter value required for parameter: 'firstName'");

        test("function model::MyFunc(firstName: String[1], test: Integer[1], whoops: String[1]): String[1]\n" +
                "{\n" +
                "  ''\n" +
                "}\n" +
                "{\n" +
                "  testDuplicate | MyFunc('John', 1) => (JSON) '[]';\n" +
                "}\n", "COMPILATION error at [6:3-51]: Parameter value required for parameter: 'whoops'");

        test("function model::MyFunc(): String[1]\n" +
                "{\n" +
                "  ''\n" +
                "}\n" +
                "{\n" +
                "  testDuplicate | MyFunc('John') => (JSON) '[]';\n" +
                "}\n", "COMPILATION error at [6:26-31]: No associated parameter found for value.");

        test("function model::Hello(name: String[1]): String[1]\n" +
                "{\n" +
                "  'Hello!. My name is ' + $name + '.';\n" +
                "}\n" +
                "{\n" +
                "  myTest | Hello('John') => 'Hello! My name is John.';\n" +
                "}\n");
    }

    @Test
    public void testRecursiveAppliedFunction()
    {
        test("function model::FunctionA(int: Integer[1]): String[1]\n" +
                "{\n" +
                "  if($int == 0, |$int->toString(), |model::FunctionA($int - 1));\n" +
                "}\n");
    }
}
