// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.language.stores.elasticsearch.v7.from.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.ElasticsearchEmbeddedDataParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

public class TestElasticsearchV7EmbeddedDataGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return ElasticsearchEmbeddedDataParserGrammar.VOCABULARY;
    }

    @Override
    public List<Vocabulary> getDelegatedParserGrammarVocabulary()
    {
        return Collections.singletonList(
                ElasticsearchEmbeddedDataParserGrammar.VOCABULARY
        );
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Data\n" +
                "Data " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  Elasticsearch\n" +
                "  #{\n" +
                "    helloIndex: \n" +
                "      [\n" +
                "        {\n" +
                "          \"_id\": \"uuid1234\",\n" +
                "          \"a\": \"hello\",\n" +
                "          \"b\": \"bye\",\n" +
                "          \"c\": 124\n" +
                "        },\n" +
                "        {\n" +
                "          \"_id\": \"uuid1235\",\n" +
                "          \"a\": \"hello\",\n" +
                "          \"b\": \"bye\",\n" +
                "          \"c\": 123,\n" +
                "          \"d\": 1.1234e10\n" +
                "        }\n" +
                "      ];\n" +
                "    helloIndex2:       \n" +
                "      {\n" +
                "        \"_id\": \"uuid1237\",\n" +
                "        \"a\": \"hello\",\n" +
                "        \"b\": \"bye\",\n" +
                "        \"c\": 123.123\n" +
                "      };\n" +
                "  }#\n" +
                "}\n\n";
    }

    @Test
    public void testParseProperly()
    {
        String grammar = getParserGrammarIdentifierInclusionTestCode(Arrays.asList("my", "data"));
        test(grammar);
    }

    @Test
    public void testParseErrorOnBadJson()
    {
        String withBadJson = "###Data\n" +
                "Data data::MyData\n" +
                "{\n" +
                "  Elasticsearch\n" +
                "  #{\n" +
                "    helloIndex: \n" +
                "      [\n" +
                "        {\n" +
                "          \"a\": \"hello\",\n" +
                "          \"b\": \"bye\",\n" +
                "        }\n" +
                "      ];\n" +
                "  }#\n" +
                "}\n\n";

        test(withBadJson, "PARSER error at [11:9]: Unexpected token '}'");
    }
}
