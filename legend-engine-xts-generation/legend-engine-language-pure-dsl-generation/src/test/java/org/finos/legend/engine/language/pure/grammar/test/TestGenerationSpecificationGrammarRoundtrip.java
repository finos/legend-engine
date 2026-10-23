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

package org.finos.legend.engine.language.pure.grammar.test;

import org.junit.Test;

public class TestGenerationSpecificationGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testGenerationSpecification()
    {
        test("###GenerationSpecification\n" +
                "GenerationSpecification test::x\n" +
                "{\n" +
                "  generationNodes: [\n" +
                "    {\n" +
                "      generationElement: model::serializableSpec;\n" +
                "    },\n" +
                "    {\n" +
                "      generationElement: model::modelSpec;\n" +
                "    },\n" +
                "    {\n" +
                "      id: 'secondGeneration';\n" +
                "      generationElement: model::modelSpec;\n" +
                "    }\n" +
                "  ];\n" +
                "  fileGenerations: [\n" +
                "    model::myFileGeneration,\n" +
                "    model::myFileGeneration\n" +
                "  ];\n" +
                "}\n");
    }

    @Test
    public void testGenerationSpecificationWithParallelGenerationElements()
    {
        test("###GenerationSpecification\n" +
                "GenerationSpecification test::x\n" +
                "{\n" +
                "  generationNodes: [\n" +
                "    {\n" +
                "      generationElements: [\n" +
                "        model::serializableSpec,\n" +
                "        model::modelSpec\n" +
                "      ];\n" +
                "    },\n" +
                "    {\n" +
                "      id: 'secondGeneration';\n" +
                "      generationElements: [\n" +
                "        model::otherSpec\n" +
                "      ];\n" +
                "    },\n" +
                "    {\n" +
                "      generationElement: model::lastSpec;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n");
    }

    @Test
    public void testGenerationSpecificationWithKeywordPaths()
    {
        test("###GenerationSpecification\n" +
                "GenerationSpecification test::x\n" +
                "{\n" +
                "  generationNodes: [\n" +
                "    {\n" +
                "      generationElement: generationElements::generationElement;\n" +
                "    },\n" +
                "    {\n" +
                "      generationElements: [\n" +
                "        model::generationElements\n" +
                "      ];\n" +
                "    }\n" +
                "  ];\n" +
                "  fileGenerations: [\n" +
                "    model::generationElements\n" +
                "  ];\n" +
                "}\n");
    }

    @Test
    public void testGenerationSpecificationWithImport()
    {
        testWithSectionInfoPreserved("###GenerationSpecification\n" +
                "import anything::*;\n" +
                "GenerationSpecification test::x\n" +
                "{\n" +
                "}\n");
    }
}
