// Copyright 2021 Goldman Sachs
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
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ExternalFormatConnectionParserGrammar;
import org.junit.Test;

import java.util.List;

public class TestExternalFormatConnectionGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
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
                ExternalFormatConnectionParserGrammar.VOCABULARY
        );
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Connection\n" +
                "ExternalFormatConnection " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  store: test::SchemaBinding;\n" +
                "  source: UrlStream\n" +
                "  {\n" +
                "    url: 'http://host:9000/path';\n" +
                "  };\n" +
                "}\n";
    }

    @Test
    public void testExternalFormatConnection()
    {
        // Missing fields
        test("###Connection\n" +
                "ExternalFormatConnection meta::mySimpleConnection\n" +
                "{\n" +
                "}\n\n", "PARSER error at [2:1-4:1]: Field 'store' is required");
        test("###Connection\n" +
                "ExternalFormatConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: domain::SchemaBinding;\n" +
                "}\n\n", "PARSER error at [2:1-5:1]: Field 'source' is required");
        //Duplicate field
        test("###Connection\n" +
                "ExternalFormatConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: domain::SchemaBinding1;\n" +
                "  store: domain::SchemaBinding2;\n" +
                "}\n\n", "PARSER error at [2:1-6:1]: Field 'store' should be specified only once");
        test("###Connection\n" +
                "ExternalFormatConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: domain::SchemaBinding1;\n" +
                "  source: UrlStream\n" +
                "  {\n" +
                "     url: 'example:://url';\n" +
                "  };\n" +
                "  source: UrlStream\n" +
                "  {\n" +
                "     url: 'example:://url';\n" +
                "  };\n" +
                "}\n\n", "PARSER error at [2:1-13:1]: Field 'source' should be specified only once");
    }

    @Test
    public void testUrlStreamExternalSource()
    {
        // Missing field
        test("###Connection\n" +
                "ExternalFormatConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: domain::SchemaBinding1;\n" +
                "  source: UrlStream\n" +
                "  {\n" +
                "  };\n" +
                "}\n", "PARSER error at [5:3-7:4]: Field 'url' is required");
        //Duplicate field
        test("###Connection\n" +
                "ExternalFormatConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: domain::SchemaBinding1;\n" +
                "  source: UrlStream\n" +
                "  {\n" +
                "     url: 'example:://url1';\n" +
                "     url: 'example:://url2';\n" +
                "  };\n" +
                "}\n", "PARSER error at [5:3-9:4]: Field 'url' should be specified only once");
    }
}
