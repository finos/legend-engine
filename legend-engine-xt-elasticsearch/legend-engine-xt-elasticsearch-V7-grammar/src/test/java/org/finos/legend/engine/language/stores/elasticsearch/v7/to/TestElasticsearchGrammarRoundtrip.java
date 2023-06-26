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

package org.finos.legend.engine.language.stores.elasticsearch.v7.to;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestElasticsearchGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testWithImport()
    {
        testWithSectionInfoPreserved("###Elasticsearch\n" +
                "import abc::*;\n" +
                "Elasticsearch7Cluster abc::abc::Store\n" +
                "{\n" +
                "  indices: [\n" +
                "    index1: {\n" +
                "      properties: [\n" +
                "        prop1: Keyword\n" +
                "      ];\n" +
                "    }\n" +
                "  ];\n" +
                "}\n\n");
    }

    @Test
    public void testStoreRoundtripSingleIndexSingleProperty()
    {
        test("###Elasticsearch\n" +
                "Elasticsearch7Cluster abc::abc::Store\n" +
                "{\n" +
                "  indices: [\n" +
                "    index1: {\n" +
                "      properties: [\n" +
                "        prop1: Keyword\n" +
                "      ];\n" +
                "    }\n" +
                "  ];\n" +
                "}\n\n");
    }

    @Test
    public void testStoreRoundtripSingleIndexMultipleProperties()
    {
        test("###Elasticsearch\n" +
                "Elasticsearch7Cluster abc::abc::Store\n" +
                "{\n" +
                "  indices: [\n" +
                "    index1: {\n" +
                "      properties: [\n" +
                "        prop1: Keyword,\n" +
                "        prop2: Text,\n" +
                "        prop3: Date,\n" +
                "        prop4: Short,\n" +
                "        prop5: Byte,\n" +
                "        prop6: Integer,\n" +
                "        prop7: Long,\n" +
                "        prop8: Float,\n" +
                "        prop9: HalfFloat,\n" +
                "        prop10: Double,\n" +
                "        prop11: Boolean\n" +
                "      ];\n" +
                "    }\n" +
                "  ];\n" +
                "}\n\n");
    }

    @Test
    public void testStoreRoundtripSingleIndexPropertyWithFieldsNested()
    {
        test("###Elasticsearch\n" +
                "Elasticsearch7Cluster abc::abc::Store\n" +
                "{\n" +
                "  indices: [\n" +
                "    index1: {\n" +
                "      properties: [\n" +
                "        prop1: Keyword {\n" +
                "          fields: [\n" +
                "            nested1: Keyword {\n" +
                "              fields: [\n" +
                "                nested2: Keyword\n" +
                "              ];\n" +
                "            }\n" +
                "          ];\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  ];\n" +
                "}\n\n");
    }

    @Test
    public void testStoreRoundtripSingleIndexWithWrappedNames()
    {
        test("###Elasticsearch\n" +
                "Elasticsearch7Cluster abc::abc::Store\n" +
                "{\n" +
                "  indices: [\n" +
                "    'index-WrappedOnQuotes': {\n" +
                "      properties: [\n" +
                "        'prop1-wrapped': Keyword {\n" +
                "          fields: [\n" +
                "            english: Keyword {\n" +
                "              fields: [\n" +
                "                'nested-wrapped': Keyword\n" +
                "              ];\n" +
                "            }\n" +
                "          ];\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  ];\n" +
                "}\n\n");
    }

    @Test
    public void testStoreRoundtripMultipleIndexProperty()
    {
        test("###Elasticsearch\n" +
                "Elasticsearch7Cluster abc::abc::Store\n" +
                "{\n" +
                "  indices: [\n" +
                "    index1: {\n" +
                "      properties: [\n" +
                "        prop1: Keyword\n" +
                "      ];\n" +
                "    },\n" +
                "    index2: {\n" +
                "      properties: [\n" +
                "        prop1: Keyword\n" +
                "      ];\n" +
                "    }\n" +
                "  ];\n" +
                "}\n\n");
    }

    @Test
    public void testStoreRoundtripSingleIndexObjectProperty()
    {
        test("###Elasticsearch\n" +
                "Elasticsearch7Cluster abc::abc::Store\n" +
                "{\n" +
                "  indices: [\n" +
                "    index1: {\n" +
                "      properties: [\n" +
                "        prop1: Object {\n" +
                "          properties: [\n" +
                "            nested1: Keyword,\n" +
                "            nested2: Keyword,\n" +
                "            nestedObject: Object {\n" +
                "              properties: [\n" +
                "                nested1: Integer\n" +
                "              ];\n" +
                "            }\n" +
                "          ];\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  ];\n" +
                "}\n\n");
    }

    @Test
    public void testStoreRoundtripSingleIndexNestedProperty()
    {
        test("###Elasticsearch\n" +
                "Elasticsearch7Cluster abc::abc::Store\n" +
                "{\n" +
                "  indices: [\n" +
                "    index1: {\n" +
                "      properties: [\n" +
                "        prop1: Nested {\n" +
                "          properties: [\n" +
                "            nested1: Keyword,\n" +
                "            nested2: Keyword,\n" +
                "            nestedObject: Nested {\n" +
                "              properties: [\n" +
                "                nested1: Integer\n" +
                "              ];\n" +
                "            }\n" +
                "          ];\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  ];\n" +
                "}\n\n");
    }
}
