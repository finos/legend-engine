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

package org.finos.legend.engine.external.format.protobuf.tests.grammar;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

public class TestGenerationSpecificationCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###GenerationSpecification\n" +
                "GenerationSpecification anything::somethingelse\n" +
                "{\n" +
                "}\n";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:1]: Duplicated element 'anything::somethingelse'";
    }

    @Test
    public void testGenerationSpecification()
    {
        String fileGen = "###FileGeneration\n" +
                "Protobuf _meta::myAvro\n" +
                "{\n" +
                "  scopeElements: [_meta];\n" +
                "}\n\n";
        // duplicate file generation
        test(fileGen +
                "###GenerationSpecification\n" +
                "GenerationSpecification model::MyGenerationSpecification\n" +
                "{\n" +
                "  fileGenerations: [_meta::myAvro, _meta::myAvro];\n" +
                "}\n", "COMPILATION error at [10:36-48]: Duplicate file generation '_meta::myAvro'");
        // file gen not found
        test(fileGen +
                "###GenerationSpecification\n" +
                "GenerationSpecification model::MyGenerationSpecification\n" +
                "{\n" +
                "  fileGenerations: [_meta::myAvroMisSpelled];\n" +
                "}\n", "COMPILATION error at [10:21-43]: Can't find file generation '_meta::myAvroMisSpelled'");
        test("###GenerationSpecification\n" +
                "GenerationSpecification _meta::MyGenerationSpecification\n" +
                "{\n" +
                "  generationNodes: [\n" +
                "    {\n" +
                "      generationElement: model::MissingElement;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "COMPILATION error at [5:5-7:5]: Can't find generation element 'model::MissingElement'");
    }

    @Test
    public void testGenerationSpecificationWithImport()
    {
        test("###FileGeneration\n" +
                "Protobuf anything::myFileGeneration\n" +
                "{\n" +
                "  scopeElements: [];\n" +
                "}\n\n" +
                "###Mapping\n" +
                "Mapping model::mapping\n" +
                "(\n" +
                ")\n\n" +
                "###GenerationSpecification\n" +
                "import anything::*;\n" +
                "GenerationSpecification test::x\n" +
                "{\n" +
                "  fileGenerations: [\n" +
                // file generation node
                "    myFileGeneration\n" +
                "  ];\n" +
                "}\n");
    }
}
