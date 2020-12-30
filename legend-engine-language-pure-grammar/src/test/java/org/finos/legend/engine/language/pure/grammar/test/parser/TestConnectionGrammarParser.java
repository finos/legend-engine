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
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.modelConnection.ModelConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

import java.util.List;

public class TestConnectionGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return ConnectionParserGrammar.VOCABULARY;
    }

    @Override
    public List<Vocabulary> getDelegatedParserGrammarVocabulary()
    {
        return FastList.newListWith(
                ModelConnectionParserGrammar.VOCABULARY
        );
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Connection\n" +
                "JsonModelConnection " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  class: test;\n" +
                "  url: 'test';\n" +
                "}\n";
    }

    @Test
    public void testJsonModelConnection()
    {
        // Missing fields
        test("###Connection\n" +
                "JsonModelConnection meta::mySimpleConnection\n" +
                "{\n" +
                "}\n\n", "PARSER error at [2:1-4:1]: Field 'class' is required");
        test("###Connection\n" +
                "JsonModelConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  class: model::firm::Person;\n" +
                "}\n\n", "PARSER error at [2:1-5:1]: Field 'url' is required");
        // Duplicated fields
        test("###Connection\n" +
                "JsonModelConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  class: model::firm::Person;\n" +
                "  class: model::firm::Person;\n" +
                "}\n\n", "PARSER error at [2:1-6:1]: Field 'class' should be specified only once");
        test("###Connection\n" +
                "JsonModelConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  class: model::firm::Person;\n" +
                "  url: '';\n" +
                "  url: '';\n" +
                "}\n\n", "PARSER error at [2:1-7:1]: Field 'url' should be specified only once");
    }

    @Test
    public void testXmlModelConnection()
    {
        // Missing fields
        test("###Connection\n" +
                "XmlModelConnection meta::mySimpleConnection\n" +
                "{\n" +
                "}\n\n", "PARSER error at [2:1-4:1]: Field 'class' is required");
        test("###Connection\n" +
                "XmlModelConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  class: model::firm::Person;\n" +
                "}\n\n", "PARSER error at [2:1-5:1]: Field 'url' is required");
        // Duplicated fields
        test("###Connection\n" +
                "XmlModelConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  class: model::firm::Person;\n" +
                "  class: model::firm::Person;\n" +
                "}\n\n", "PARSER error at [2:1-6:1]: Field 'class' should be specified only once");
        test("###Connection\n" +
                "XmlModelConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  class: model::firm::Person;\n" +
                "  url: '';\n" +
                "  url: '';\n" +
                "}\n\n", "PARSER error at [2:1-7:1]: Field 'url' should be specified only once");
    }

    @Test
    public void testModelChainConnection()
    {
        // Missing fields
        test("###Connection\n" +
                "ModelChainConnection meta::mySimpleConnection\n" +
                "{\n" +
                "}\n\n", "PARSER error at [2:1-4:1]: Field 'mappings' is required");
        // Duplicated fields
        test("###Connection\n" +
                "ModelChainConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  mappings: [model::firm::Person];\n" +
                "  mappings: [model::firm::Person];\n" +
                "}\n\n", "PARSER error at [2:1-6:1]: Field 'mappings' should be specified only once");
    }
}
