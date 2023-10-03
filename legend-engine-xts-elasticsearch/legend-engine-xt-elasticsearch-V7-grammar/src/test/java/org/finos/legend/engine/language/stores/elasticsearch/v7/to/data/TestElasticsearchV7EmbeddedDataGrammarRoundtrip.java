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

package org.finos.legend.engine.language.stores.elasticsearch.v7.to.data;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestElasticsearchV7EmbeddedDataGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testGrammarRoundtrip()
    {
        test("###Data\n" +
                "Data meta::data::MyData\n" +
                "{\n" +
                "  Elasticsearch\n" +
                "  #{\n" +
                "    helloIndex:\n" +
                "      [\n" +
                "        {\n" +
                "          \"_id\" : \"uuid1234\",\n" +
                "          \"a\" : \"hello\",\n" +
                "          \"b\" : \"bye\"\n" +
                "        },\n" +
                "        {\n" +
                "          \"_id\" : \"uuid1235\",\n" +
                "          \"a\" : \"hello\",\n" +
                "          \"b\" : \"bye\"\n" +
                "        }\n" +
                "      ];\n" +
                "    'hello-Index2':\n" +
                "      {\n" +
                "        \"_id\" : \"uuid1237\",\n" +
                "        \"a\" : \"hello\",\n" +
                "        \"b\" : \"bye\"\n" +
                "      };\n" +
                "  }#\n" +
                "}\n"
        );
    }
}
