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
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ExternalFormatParserGrammar;
import org.junit.Test;

import java.util.List;

public class TestExternalFormatGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return ExternalFormatParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###ExternalFormat\n" +
                "SchemaSet " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  format: Example;\n" +
                "  schemas: [ { content: 'Some schema'; } ];\n" +
                "}\n";
    }

    @Test
    public void testUnknownFormat()
    {
        test("###ExternalFormat\n" +
                     "SchemaSet test::Example\n" +
                     "{\n" +
                     "  format: NOTHING;\n" +
                     "  schemas: [ { content: 'Schema Description'; } ];\n" +
                     "}\n",
             "PARSER error at [4:3-18]: Unknown schema format: NOTHING"
        );
    }

    @Test
    public void testAtLeastOneSchema()
    {
        test("###ExternalFormat\n" +
                     "SchemaSet test::Example\n" +
                     "{\n" +
                     "  format: NOTHING;\n" +
                     "  schemas: [];\n" +
                     "}\n",
             "PARSER error at [5:13]: Unexpected token"
        );
    }
}
