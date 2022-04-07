// Copyright 2022 Goldman Sachs
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
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.DataParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

import java.util.List;

public class TestDataGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return DataParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Data\n" +
                "Data " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "   ExternalFormat\n" +
                "   #{\n" +
                "     contentType: 'test';\n" +
                "     data: 'test';\n" +
                "   }#\n" +
                "}\n";
    }

    @Test
    public void testMissingFields()
    {
        test("###Data\n" +
                        "Data meta::data::MyData\n" +
                        "{\n" +
                        "   ExternalFormat\n" +
                        "   #{\n" +
                        "     data: '1B4A 9DEA 230F FF20';\n" +
                        "   }#\n" +
                        "}\n",
                "PARSER error at [4:4-7:5]: Field 'contentType' is required"
        );

        test("###Data\n" +
                        "Data meta::data::MyData\n" +
                        "{\n" +
                        "   ExternalFormat\n" +
                        "   #{\n" +
                        "     contentType: 'application/x-protobuf';\n" +
                        "   }#\n" +
                        "}\n",
                "PARSER error at [4:4-7:5]: Field 'data' is required"
        );

        test("###Data\n" +
                        "Data meta::data::MyData\n" +
                        "{\n" +
                        "   Reference\n" +
                        "   #{\n" +
                        "   }#\n" +
                        "}\n",
                "PARSER error at [4:4-6:5]: Path should be provided for DataElementReference"
        );
    }

    @Test
    public void testIncorrectModelStoreData()
    {
        test("###Data\n" +
                        "Data meta::data::MyData\n" +
                        "{\n" +
                        "   ModelStore\n" +
                        "   #{\n" +
                        "     meta::Demo:\n" +
                        "       [\n" +
                        "       ],\n" +
                        "     meta::Demo:\n" +
                        "       [\n" +
                        "       ]\n" +
                        "   }#\n" +
                        "}\n",
                "PARSER error at [6:6-12:8]: Multiple entries found for type: 'meta::Demo'"
        );
    }
}
