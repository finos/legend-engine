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

package org.finos.legend.engine.language.pure.compiler.test.fromGrammar;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

public class TestRelationNotUsingDatabaseAccessor extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    protected String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Pure\n" +
                "Class anything::somethingelse\n" +
                "{\n" +
                "}\n";
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:1]: Duplicated element 'anything::somethingelse'";
    }

    // More advanced tests can be found in the Relational Section
    // org.finos.legend.engine.language.pure.compiler.test.TestRelationFunctions

    @Test
    public void testProject()
    {
        test(
                "###Pure\n" +
                        "Class test::Person{name : String[1];}" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   test::Person.all()->project(~[mycol:x|$x.name])\n" +
                        "}"
        );
    }

    @Test
    public void testProjectError()
    {
        test(
                "###Pure\n" +
                        "Class test::Person{name : String[1];}" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   test::Person.all()->project(~[mycol:x|$x.nme])\n" +
                        "}","COMPILATION error at [4:45-47]: Can't find property 'nme' in class 'test::Person'"
        );
    }

    @Test
    public void testProjectMulti()
    {
        test(
                "###Pure\n" +
                        "Class test::Person{name : String[1]; val : Integer[1];}" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   test::Person.all()->project(~[mycol:x|$x.name, co:x|$x.val])\n" +
                        "}"
        );
    }

    @Test
    public void testProjectMultiError()
    {
        test(
                "###Pure\n" +
                        "Class test::Person{name : String[1]; val : Integer[1];}" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   test::Person.all()->project(~[mycol:x|$x.name, co:x|$x.vals])\n" +
                        "}","COMPILATION error at [4:59-62]: Can't find property 'vals' in class 'test::Person'"
        );
    }

    @Test
    public void testProjectReturn()
    {
        test(
                "###Pure\n" +
                        "Class test::Person{name : String[1]; val : Integer[1];}" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   test::Person.all()->project(~[mycol:x|$x.name, co:x|$x.val])->filter(x|$x.co > 2)\n" +
                        "}"
        );
    }

    @Test
    public void testProjectReturnError()
    {
        test(
                "###Pure\n" +
                        "Class test::Person{name : String[1]; val : Integer[1];}" +
                        "function test::f():Any[*]\n" +
                        "{\n" +
                        "   test::Person.all()->project(~[mycol:x|$x.name, co:x|$x.val])->filter(x|$x.ceo > 2)\n" +
                        "}", "COMPILATION error at [4:78-80]: The column 'ceo' can't be found in the relation (mycol:String, co:Integer)"
        );
    }
}
