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

import org.junit.Test;

public class TestServiceGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testService()
    {
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'ownerName',\n" +
                "    'ownerName2'\n" +
                "  ];\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 },\n" +
                "      { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
                "    ];\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "Service meta::pure::myServiceMulti\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'ownerName'\n" +
                "  ];\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: |model::pure::mapping::modelToModel::test::shared::dest::Product.all()->graphFetchChecked(#{model::pure::mapping::modelToModel::test::shared::dest::Product{name}}#)->serialize(#{model::pure::mapping::modelToModel::test::shared::dest::Product{name}}#);\n" +
                "    key: 'env';\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: meta::myMapping1;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "    executions['UAT']:\n" +
                "    {\n" +
                "      mapping: meta::myMapping2;\n" +
                "      runtime: meta::myRuntime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'moreData';\n" +
                "      asserts:\n" +
                "      [\n" +
                "        { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 },\n" +
                "        { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
                "      ];\n" +
                "    }\n" +
                "    tests['UAT']:\n" +
                "    {\n" +
                "      data: 'moreData';\n" +
                "      asserts:\n" +
                "      [\n" +
                "        { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 },\n" +
                "        { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "}\n"
        );
    }

    @Test
    public void testServiceWithEmbeddedRuntime()
    {
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime:\n" +
                "    #{\n" +
                "      mappings:\n" +
                "      [\n" +
                // since a mapping is provided, there is no need to
                "        meta::myMapping222\n" +
                "      ];\n" +
                "      connections:\n" +
                "      [\n" +
                "        ModelStore:\n" +
                "        [\n" +
                "          id1: test::myConnection,\n" +
                "          id2:\n" +
                "          #{\n" +
                "            JsonModelConnection\n" +
                "            {\n" +
                "              class: meta::mySimpleClass;\n" +
                "              url: 'my_url';\n" +
                "            }\n" +
                "          }#,\n" +
                "          id3:\n" +
                "          #{\n" +
                "            JsonModelConnection\n" +
                "            {\n" +
                "              class: meta::mySimpleClass;\n" +
                "              url: 'my_url';\n" +
                "            }\n" +
                "          }#\n" +
                "        ]\n" +
                "      ];\n" +
                "    }#;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 },\n" +
                "      { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
                "    ];\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    public void testServiceWithEmbeddedRuntimeWithOptionalMapping()
    {
        String unformatted = "###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                // empty owner list will not be shown
                "  owners:[];\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime:\n" +
                "    #{\n" +
                "      connections:\n" +
                "      [\n" +
                "        ModelStore:\n" +
                "        [\n" +
                "          id1: test::myConnection,\n" +
                "          id2: #{ JsonModelConnection { class: meta::mySimpleClass; url: 'my_url'; }}#,\n" +
                "          id3: #{ JsonModelConnection { class: meta::mySimpleClass; url: 'my_url'; }}#\n" +
                "        ]\n" +
                "      ];\n" +
                "    }#;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 },\n" +
                "      { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
                "    ];\n" +
                "  }\n" +
                "}\n";
        String formatted = "###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime:\n" +
                "    #{\n" +
                "      connections:\n" +
                "      [\n" +
                "        ModelStore:\n" +
                "        [\n" +
                "          id1: test::myConnection,\n" +
                "          id2:\n" +
                "          #{\n" +
                "            JsonModelConnection\n" +
                "            {\n" +
                "              class: meta::mySimpleClass;\n" +
                "              url: 'my_url';\n" +
                "            }\n" +
                "          }#,\n" +
                "          id3:\n" +
                "          #{\n" +
                "            JsonModelConnection\n" +
                "            {\n" +
                "              class: meta::mySimpleClass;\n" +
                "              url: 'my_url';\n" +
                "            }\n" +
                "          }#\n" +
                "        ]\n" +
                "      ];\n" +
                "    }#;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 },\n" +
                "      { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
                "    ];\n" +
                "  }\n" +
                "}\n";
        testFormat(formatted, unformatted);
    }

    @Test
    public void testServiceWithImport()
    {
        test("###Service\n" +
                "import meta::*;\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: mySimpleMapping;\n" +
                "    runtime: mySimpleRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 },\n" +
                "      { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
                "    ];\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    public void testServiceTags()
    {
        //test for single tag
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'ownerName',\n" +
                "    'ownerName2'\n" +
                "  ];\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 },\n" +
                "      { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
                "    ];\n" +
                "  }\n" +
                "  tags:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'testName1';\n" +
                "      value: 'testValue1';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n"
        );

        // test for multiple tags
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'ownerName',\n" +
                "    'ownerName2'\n" +
                "  ];\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 },\n" +
                "      { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
                "    ];\n" +
                "  }\n" +
                "  tags:\n" +
                "  [\n" +
                "    {\n" +
                "      name: 'testName1';\n" +
                "      value: 'testValue1';\n" +
                "    },\n" +
                "    {\n" +
                "      name: 'testName2';\n" +
                "      value: 'testValue2';\n" +
                "    }\n" +
                "  ];\n" +
                "}\n"
        );
    }

    @Test
    public void testServiceTestParameters()
    {
        //test for single test parameter
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'ownerName',\n" +
                "    'ownerName2'\n" +
                "  ];\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::transform::tests::Address[1]|$src.a;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      { ['singleParameter'], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
                "    ];\n" +
                "  }\n" +
                "}\n"
        );
    }
}