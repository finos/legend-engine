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
                "Service <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'something'} meta::pure::myServiceSingle\n" +
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
                "    query: p1: String[1]|service_parameters::_NPerson.all()->graphFetch(#{service_parameters::_NPerson{Age,Name}}#)->serialize(#{service_parameters::_NPerson{Age,Name,f1($p1)}}#);\n" +
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

        //test for multiple test parameter
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'ownerName'\n" +
                "  ];\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: p1: String[1]|service_parameters::_NPerson.all()->graphFetch(#{service_parameters::_NPerson{Age,Name}}#)->serialize(#{service_parameters::_NPerson{Age,Name,f1($p1)}}#);\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      { ['singleParameter', 22, Enum.Reference, -3.14, [1.8, 2, -3], %2019-05-24T00:00:00], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
                "    ];\n" +
                "  }\n" +
                "}\n"
        );

        //test for multiple test asserts
        test("###Service\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'ownerName'\n" +
                "  ];\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: p1: String[1]|service_parameters::_NPerson.all()->graphFetch(#{service_parameters::_NPerson{Age,Name}}#)->serialize(#{service_parameters::_NPerson{Age,Name,f1($p1)}}#);\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      { ['StringParameter', [1, 2, 3], %23:12:8.54, Enum.Reference], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 },\n" +
                "      { ['parameter1', %23:12:8.54, true, 440, 13.23, 88, -54, 2.3, [1, 2], %2019-05-24T00:00:00, %2019-05-24], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
                "    ];\n" +
                "  }\n" +
                "}\n"
        );
    }

    @Test
    public void testList()
    {
        test("###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'ownerName'\n" +
                "  ];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: test::class[1]|$src.prop1;\n" +
                "    mapping: test::mapping;\n" +
                "    runtime: test::runtime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'test';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      { [list(['param','param2'])], |'testexpression' },\n" +
                "      { [list([1,2])], |'testexpression2' },\n" +
                "      { [list([1,2]), 1], |'testexpression3' }\n" +
                "    ];\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    public void testServiceTestSuite()
    {
        //Test Empty TestSuite
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
                "    query: |demo::_NPerson.all()->graphFetch(#{demo::_NPerson{Age,Name}}#)->serialize(#{demo::_NPerson{Age,Name}}#);\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "\n" +
                "  ]\n" +
                "}\n"
        );

        //Test Single TestSuite without data
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
                "    query: |demo::_NPerson.all()->graphFetch(#{demo::_NPerson{Age,Name}}#)->serialize(#{demo::_NPerson{Age,Name}}#);\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{Age:12, Name:\"dummy\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n"
        );

        //Test Single TestSuite with data
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
                "    query: |demo::_NPerson.all()->graphFetch(#{demo::_NPerson{Age,Name}}#)->serialize(#{demo::_NPerson{Age,Name}}#);\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection1:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/x.flatdata';\n" +
                "              data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{Age:12, Name:\"dummy\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n"
        );

        //Test Single TestSuite with multiple connections data
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
                "    query: |demo::_NPerson.all()->graphFetch(#{demo::_NPerson{Age,Name}}#)->serialize(#{demo::_NPerson{Age,Name}}#);\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection1:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/x.flatdata';\n" +
                "              data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                "            }#,\n" +
                "          connection2:\n" +
                "            ModelStore\n" +
                "            #{\n" +
                "              my::Person:\n" +
                "                [\n" +
                "                  ^my::Person(\n" +
                "                    givenNames = ['Fred', 'William'],\n" +
                "                    address = ^my::Address(street = 'A Road')\n" +
                "                  )\n" +
                "                ]\n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{Age:12, Name:\"dummy\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n"
        );

        //Test Single Execution with Test Assertion containing KeysInScope size() > 1
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
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection1:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/x.flatdata';\n" +
                "              data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                "            }#,\n" +
                "          connection2:\n" +
                "            ModelStore\n" +
                "            #{\n" +
                "              my::Person:\n" +
                "                [\n" +
                "                  ^my::Person(\n" +
                "                    givenNames = ['Fred', 'William'],\n" +
                "                    address = ^my::Address(street = 'A Road')\n" +
                "                  )\n" +
                "                ]\n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          assertForKeys:\n" +
                "          [\n" +
                "            'UAT'\n" +
                "          ];\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{Age:12, Name:\"dummy\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n"
        );

        //Test Single TestSuite with multiple tests
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
                "    query: |demo::_NPerson.all()->graphFetch(#{demo::_NPerson{Age,Name}}#)->serialize(#{demo::_NPerson{Age,Name}}#);\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection1:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/x.flatdata';\n" +
                "              data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{Age:12, Name:\"dummy\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        },\n" +
                "        test2:\n" +
                "        {\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{Age:12, Name:\"dummy\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n"
        );

        //Test Single TestSuite with parameter
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
                "    query: param: String[1]|demo::_NPerson.all()->graphFetch(#{demo::_NPerson{Age,Name}}#)->serialize(#{demo::_NPerson{Age,Name}}#);\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection1:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/x.flatdata';\n" +
                "              data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          parameters:\n" +
                "          [\n" +
                "            param = 'dummy'\n" +
                "          ]\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{Age:12, Name:\"dummy\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n"
        );

        //Test Single TestSuite with multiple parameters
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
                "    query: {stringParam: String[1],stringOptionalParam: String[0..1],stringListParam: String[*],integerParam: Integer[1],integerOptionalParam: Integer[0..1],integerListParam: Integer[*],floatParam: Float[1],floatOptionalParam: Float[0..1],floatListParam: Float[*],strictDateParam: StrictDate[1],strictDateOptionalParam: StrictDate[0..1],strictDateListParam: StrictDate[*],dateTimeParam: DateTime[1],dateTimeOptionalParam: DateTime[0..1],dateTimeListParam: DateTime[*],booleanParam: Boolean[1],booleanOptionalParam: Boolean[0..1],booleanListParam: Boolean[*]|demo::_NPerson.all()->graphFetch(#{demo::_NPerson{Age,Name}}#)->serialize(#{demo::_NPerson{Age,Name}}#)};\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection1:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/x.flatdata';\n" +
                "              data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          parameters:\n" +
                "          [\n" +
                "            stringParam = 'dummy',\n" +
                "            stringOptionalParam = 'dummy',\n" +
                "            stringListParam = ['dummy', 'dummy'],\n" +
                "            integerParam = 1,\n" +
                "            integerOptionalParam = [],\n" +
                "            integerListParam = [1, 2],\n" +
                "            floatParam = 1.123,\n" +
                "            floatOptionalParam = [],\n" +
                "            floatListParam = [1.123, 4.456],\n" +
                "            strictDateParam = %2020-1-1,\n" +
                "            strictDateOptionalParam = [],\n" +
                "            strictDateListParam = [%2020-1-1, %2020-1-2],\n" +
                "            dateTimeParam = %2020-1-1T12:12:12,\n" +
                "            dateTimeOptionalParam = [],\n" +
                "            dateTimeListParam = [%2020-1-1T12:12:12, %2020-1-1T12:12:13],\n" +
                "            booleanParam = false,\n" +
                "            booleanOptionalParam = [],\n" +
                "            booleanListParam = [true, false]\n" +
                "          ]\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{Age:12, Name:\"dummy\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n"
        );

        //Test Single TestSuite without asserts
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
                "    query: |demo::_NPerson.all()->graphFetch(#{demo::_NPerson{Age,Name}}#)->serialize(#{demo::_NPerson{Age,Name}}#);\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection1:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/x.flatdata';\n" +
                "              data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          asserts:\n" +
                "          [\n" +
                "\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n"
        );

        //Test Single TestSuite with multiple asserts
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
                "    query: |demo::_NPerson.all()->graphFetch(#{demo::_NPerson{Age,Name}}#)->serialize(#{demo::_NPerson{Age,Name}}#);\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection1:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/x.flatdata';\n" +
                "              data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{Age:12, Name:\"dummy\"}';\n" +
                "                  }#;\n" +
                "              }#,\n" +
                "            assert2:\n" +
                "              EqualTo\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  'expected result content';\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n"
        );

        //Test Single TestSuite with complex semantics
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
                "    query: param: String[1]|demo::_NPerson.all()->graphFetch(#{demo::_NPerson{Age,Name}}#)->serialize(#{demo::_NPerson{Age,Name}}#);\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection1:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/x.flatdata';\n" +
                "              data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                "            }#,\n" +
                "          connection2:\n" +
                "            ModelStore\n" +
                "            #{\n" +
                "              my::Person:\n" +
                "                [\n" +
                "                  ^my::Person(\n" +
                "                    givenNames = ['Fred', 'William'],\n" +
                "                    address = ^my::Address(street = 'A Road')\n" +
                "                  )\n" +
                "                ]\n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          serializationFormat: myFormat;\n" +
                "          parameters:\n" +
                "          [\n" +
                "            param = 'value1'\n" +
                "          ]\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{Age:12, Name:\"dummy\"}';\n" +
                "                  }#;\n" +
                "              }#,\n" +
                "            assert2:\n" +
                "              EqualTo\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  'expected result content';\n" +
                "              }#\n" +
                "          ]\n" +
                "        },\n" +
                "        test2:\n" +
                "        {\n" +
                "          parameters:\n" +
                "          [\n" +
                "            param = 'value2'\n" +
                "          ]\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualTo\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  'expected result content';\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n"
        );

        //Test Multiple TestSuite with complex semantics
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
                "    query: param: String[1]|demo::_NPerson.all()->graphFetch(#{demo::_NPerson{Age,Name}}#)->serialize(#{demo::_NPerson{Age,Name}}#);\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection1:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/x.flatdata';\n" +
                "              data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                "            }#,\n" +
                "          connection2:\n" +
                "            ModelStore\n" +
                "            #{\n" +
                "              my::Person:\n" +
                "                [\n" +
                "                  ^my::Person(\n" +
                "                    givenNames = ['Fred', 'William'],\n" +
                "                    address = ^my::Address(street = 'A Road')\n" +
                "                  )\n" +
                "                ]\n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          parameters:\n" +
                "          [\n" +
                "            param = 'value1'\n" +
                "          ]\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{Age:12, Name:\"dummy\"}';\n" +
                "                  }#;\n" +
                "              }#,\n" +
                "            assert2:\n" +
                "              EqualTo\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  'expected result content';\n" +
                "              }#\n" +
                "          ]\n" +
                "        },\n" +
                "        test2:\n" +
                "        {\n" +
                "          parameters:\n" +
                "          [\n" +
                "            param = 'value2'\n" +
                "          ]\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualTo\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  'expected result content';\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    testSuite2:\n" +
                "    {\n" +
                "      data:\n" +
                "      [\n" +
                "        connections:\n" +
                "        [\n" +
                "          connection1:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/x.flatdata';\n" +
                "              data: 'FIRST_NAME,LAST_NAME\\nJohn,Doe\\nfirstName,lastName';\n" +
                "            }#,\n" +
                "          connection2:\n" +
                "            ModelStore\n" +
                "            #{\n" +
                "              my::Person:\n" +
                "                [\n" +
                "                  ^my::Person(\n" +
                "                    givenNames = ['John', 'Doe'],\n" +
                "                    address = ^my::Address(street = 'A Street')\n" +
                "                  )\n" +
                "                ]\n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          parameters:\n" +
                "          [\n" +
                "            param = 'value1'\n" +
                "          ]\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{Age:12, Name:\"dummy\"}';\n" +
                "                  }#;\n" +
                "              }#,\n" +
                "            assert2:\n" +
                "              EqualTo\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  'expected result content';\n" +
                "              }#\n" +
                "          ]\n" +
                "        },\n" +
                "        test2:\n" +
                "        {\n" +
                "          parameters:\n" +
                "          [\n" +
                "            param = 'value2'\n" +
                "          ]\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualTo\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  'expected result content';\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n"
        );
    }
}