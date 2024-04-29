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

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.GenerationSpecificationParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

import java.util.List;

public class TestGenerationSpecificationGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return GenerationSpecificationParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###GenerationSpecification\n" +
                "GenerationSpecification " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "}\n";
    }

    @Test
    public void testGenerationSpecification()
    {
        // Duplicated fields
        test("###GenerationSpecification\n" +
                "GenerationSpecification model::GenerationSpecification\n" +
                "{\n" +
                "  generationNodes: [{ generationElement: model::MyElement1;}];" +
                "  generationNodes: [{ generationElement: model::MyElement2;}];" +
                "}\n\n", "PARSER error at [2:1-4:125]: Field 'generationNodes' should be specified only once");
        // Duplicated fields
        test("###GenerationSpecification\n" +
                "GenerationSpecification model::GenerationSpecification\n" +
                "{\n" +
                "  fileGenerations: [model::myFileGeneration1];" +
                "  fileGenerations: [model::myFileGeneration1];" +
                "}\n\n", "PARSER error at [2:1-4:93]: Field 'fileGenerations' should be specified only once");

    }
}
