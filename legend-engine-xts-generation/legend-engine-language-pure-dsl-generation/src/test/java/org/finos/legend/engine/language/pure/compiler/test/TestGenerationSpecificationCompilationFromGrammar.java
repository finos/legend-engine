// Copyright 2026 Goldman Sachs
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

public class TestGenerationSpecificationCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###GenerationSpecification\n" +
                "GenerationSpecification anything::class\n" +
                "{\n" +
                "}\n";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [3:1-5:1]: Duplicated element 'anything::class'";
    }

    @Test
    public void testGenerationElementsMustBeResolvable()
    {
        test("###GenerationSpecification\n" +
                "GenerationSpecification model::MyGenerationSpecification\n" +
                "{\n" +
                "  generationNodes: [\n" +
                "    {\n" +
                "      generationElements: [\n" +
                "        model::MissingElement\n" +
                "      ];\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "COMPILATION error at [5:5-9:5]: Can't find generation element 'model::MissingElement'");
    }

    @Test
    public void testDuplicateGenerationElementInNode()
    {
        test("###GenerationSpecification\n" +
                "GenerationSpecification model::MyGenerationSpecification\n" +
                "{\n" +
                "  generationNodes: [\n" +
                "    {\n" +
                "      id: 'myNode';\n" +
                "      generationElements: [\n" +
                "        model::MyElement,\n" +
                "        model::MyElement\n" +
                "      ];\n" +
                "    }\n" +
                "  ];\n" +
                "}\n", "COMPILATION error at [5:5-11:5]: Duplicate generation element 'model::MyElement' in generation node 'myNode'");
    }
}
