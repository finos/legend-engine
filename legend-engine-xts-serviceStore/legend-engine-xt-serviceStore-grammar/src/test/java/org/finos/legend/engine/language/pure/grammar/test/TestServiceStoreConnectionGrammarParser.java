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
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ServiceStoreConnectionParserGrammar;
import org.junit.Test;

import java.util.List;

public class TestServiceStoreConnectionGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
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
                ServiceStoreConnectionParserGrammar.VOCABULARY
        );
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Connection\n" +
                "ServiceStoreConnection " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  store: test::serviceStore;\n" +
                "  baseUrl: 'http://dummyurl';\n" +
                "}\n\n";
    }

    @Test
    public void testServiceStoreConnectionBaseUrlMissing()
    {
        test("###Connection\n" +
                "ServiceStoreConnection simple::serviceStoreConnection\n" +
                "{\n" +
                "  store: test::serviceStore;\n" +
                "}\n", "PARSER error at [2:1-5:1]: Field 'baseUrl' is required");
    }

    @Test
    public void testServiceStoreConnectionUrlEndingWithSlash()
    {
        test("###Connection\n" +
                "ServiceStoreConnection simple::serviceStoreConnection\n" +
                "{\n" +
                "  store: test::serviceStore;\n" +
                "  baseUrl: 'http://baseUrl/';\n" +
                "}\n", "PARSER error at [5:3-29]: baseUrl should not end with '/'");
    }

    @Test
    public void testServiceStoreConnectionSemiColonMissing()
    {
        test("###Connection\n" +
                "ServiceStoreConnection simple::serviceStoreConnection\n" +
                "{\n" +
                "  store: test::serviceStore;\n" +
                "  baseUrl: 'http://baseUrl'\n" +
                "}\n", "PARSER error at [6:0]: Unexpected token");
    }
}
