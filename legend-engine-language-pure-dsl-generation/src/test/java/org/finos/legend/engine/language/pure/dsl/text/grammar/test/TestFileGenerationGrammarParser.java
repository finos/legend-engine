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

package org.finos.legend.engine.language.pure.dsl.text.grammar.test;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.FileGenerationParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

import java.util.List;

public class TestFileGenerationGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return FileGenerationParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###FileGeneration\n" +
                "Avro " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  scopeElements: [model::MyClass, model];\n" +
                "  includeNamespace: true;\n" +
                "  propertyProfile: ['model::myProfile', 'model::nextProfile'];\n" +
                "}\n";
    }

    @Test
    public void testFileGeneration()
    {
        // Duplicated fields
        test("###FileGeneration\n" +
                "Avro model::AvroConfig\n" +
                "{\n" +
                "  scopeElements: [model::MyClass, model];\n" +
                "  scopeElements: [model::MyClass, model];\n" +
                "  includeNamespace: true;\n" +
                "  propertyProfile: ['model::myProfile', 'model::nextProfile'];\n" +
                "}\n\n", "PARSER error at [2:1-8:1]: Field 'scopeElements' should be specified only once");
        test("###FileGeneration\n" +
                "Avro model::AvroConfig\n" +
                "{\n" +
                "  generationOutputPath: 'aa';\n" +
                "  generationOutputPath: 'bbbb';\n" +
                "  includeNamespace: true;\n" +
                "  propertyProfile: ['model::myProfile', 'model::nextProfile'];\n" +
                "}\n\n", "PARSER error at [2:1-8:1]: Field 'generationOutputPath' should be specified only once");
    }

    @Test
    public void testBannedConfigPropertyNames()
    {
        test("###FileGeneration\n" +
                "import anything::*;\n" +
                "Avro model::AvroConfig\n" +
                "{\n" +
                "  scopeElements: 1;\n" +
                "  generationOutputPath: [''];\n" +
                "}\n", "PARSER error at [3:1-7:1]: Can't have config property with reserved name 'scopeElements'");
        test("###FileGeneration\n" +
                "import anything::*;\n" +
                "Avro model::AvroConfig\n" +
                "{\n" +
                "  generationOutputPath: [''];\n" +
                "}\n", "PARSER error at [3:1-6:1]: Can't have config property with reserved name 'generationOutputPath'");
    }
}
