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

package org.finos.legend.engine.language.pure.grammar.test.parser;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.runtime.RuntimeParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

import java.util.List;

public class TestRuntimeGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return RuntimeParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Runtime\n" +
                "Runtime " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping::someMapping\n" +
                "  ];\n" +
                "}\n";
    }

    @Test
    public void testRuntime()
    {
        // Missing fields
        test("###Runtime\n" +
                "Runtime test\n" +
                "{\n" +
                "}\n", "PARSER error at [2:1-4:1]: Field 'mappings' is required");
        // Duplicated fields
        test("###Runtime\n" +
                "Runtime test\n" +
                "{\n" +
                " mappings: [me]; mappings: [me];" +
                "}\n" +
                "\n\n", "PARSER error at [2:1-4:33]: Field 'mappings' should be specified only once");
        test("###Runtime\n" +
                "Runtime test\n" +
                "{\n" +
                " mappings: [me];" +
                " connections: []; connections: [];" +
                "}\n" +
                "\n\n", "PARSER error at [2:1-4:51]: Field 'connections' should be specified only once");
        // empty embedded connection
        test("###Runtime\n" +
                "Runtime meta::mySimpleRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    meta::mySimpleMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    test::someStore:\n" +
                "    [\n" +
                "      id1: #{}#\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n", "PARSER error at [12:12-15]: Embedded connection must not be empty");
    }
}
