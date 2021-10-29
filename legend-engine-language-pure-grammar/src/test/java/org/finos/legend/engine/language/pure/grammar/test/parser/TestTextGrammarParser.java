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
import org.finos.legend.engine.language.pure.grammar.from.antlr4.text.TextParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

import java.util.List;

public class TestTextGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return TextParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Text\n" +
                "Text " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  type: HTML;\n" +
                "  content: 'test';\n" +
                "}\n";
    }

    @Test
    public void testText()
    {
        // Missing fields
        test("###Text\n" +
                "Text meta::pure::myText\n" +
                "{\n" +
                "  content: 'this is just for context';\n" +
                "}\n", "PARSER error at [2:1-5:1]: Field 'type' is required");
        test("###Text\n" +
                "Text meta::pure::myText\n" +
                "{\n" +
                "  type: STRING;\n" +
                "}\n", "PARSER error at [2:1-5:1]: Field 'content' is required");
        // Duplicated fields
        test("###Text\n" +
                "Text meta::pure::myText\n" +
                "{\n" +
                "  type: STRING;\n" +
                "  content: 'this is just for context';\n" +
                "  type: STRING;\n" +
                "}\n", "PARSER error at [2:1-7:1]: Field 'type' should be specified only once");
        test("###Text\n" +
                "Text meta::pure::myText\n" +
                "{\n" +
                "  content: 'this is just for context';\n" +
                "  type: STRING;\n" +
                "  content: 'this is just for context';\n" +
                "}\n", "PARSER error at [2:1-7:1]: Field 'content' should be specified only once");
    }
}
