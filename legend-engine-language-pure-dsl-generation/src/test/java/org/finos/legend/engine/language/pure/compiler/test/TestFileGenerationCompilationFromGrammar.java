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

package org.finos.legend.engine.language.pure.compiler.test;

import org.junit.Test;

public class TestFileGenerationCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###FileGeneration\n" +
                "Avro anything::somethingelse\n" +
                "{\n" +
                "  scopeElements: [_meta];\n" +
                "}\n";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-8:1]: Duplicated element 'anything::somethingelse'";
    }

    @Test
    public void testFaultySchemaGenerationCompilation()
    {
        test(
            "###FileGeneration\n" +
            "SchemaGeneration generation::MySchemaGeneration\n" +
            "{\n" +
             "format: 'JSON';\n" +
            " modelIncludes: [meta::MyClass];\n" +
            "}\n", "COMPILATION error at [2:1-6:1]: Can't find the packageable element 'meta::MyClass'");

        test(
            "Class model::MyClass {}\n"
                + "###FileGeneration\n" +
                "SchemaGeneration generation::MySchemaGeneration\n" +
                "{\n" +
                "format: 'JSON';\n" +
                " modelIncludes: [model::MyClass];\n" +
                " modelExcludes: [model::MyClassA];\n" +
                "}\n", "COMPILATION error at [3:1-8:1]: Can't find the packageable element 'model::MyClassA'");
    }


}
