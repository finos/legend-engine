// Copyright 2025 Goldman Sachs
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
//

package org.finos.legend.engine.language.deephaven.from.connection;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.authentication.AuthenticationParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.DeephavenConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;

import java.util.List;
import org.eclipse.collections.impl.factory.Lists;
import org.junit.Test;

public class TestDeephavenConnectionGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return ConnectionParserGrammar.VOCABULARY;
    }

    @Override
    public List<Vocabulary> getDelegatedParserGrammarVocabulary()
    {
        return Lists.fixedSize.of(DeephavenConnectionParserGrammar.VOCABULARY, AuthenticationParserGrammar.VOCABULARY);
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return  "###Connection\n" +
                "DeephavenConnection " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "    store: test::DeephavenStore;\n" +
                "    serverUrl: 'http://dummyurl.com:12345'\n" +
                "    authentication: # PSK {\n" +
                "        psk: 'abcde';\n" +
                "    }#;\n" +
                "}";
    }

    @Test
    public void testUnquotedServerUrlThrowsError()
    {
        String unquotedCode =  "###Connection\n" +
                                "DeephavenConnection test::UnquotedServerUrl\n" +
                                "{\n" +
                                "    store: test::DeephavenStore;\n" +
                                "    serverUrl: http://dummyurl.com:12345\n" +
                                "    authentication: # PSK {\n" +
                                "        psk: 'abcde';\n" +
                                "    }#;\n" +
                                "}";
        test(unquotedCode, "PARSER error at [5:16-19]: Unexpected token 'http'");
    }
}
