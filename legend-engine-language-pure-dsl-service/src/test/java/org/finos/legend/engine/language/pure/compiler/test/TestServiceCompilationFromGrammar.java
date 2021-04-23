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

package org.finos.legend.engine.language.pure.compiler.test;

import org.junit.Test;

public class TestServiceCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    @Override
    public String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Service\n" +
                "Service anything::class\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['test'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: '';\n" +
                "    mapping: anything::somethingelse;\n" +
                "    runtime:\n" +
                "    #{\n" +
                "     connections: [];\n" +
                "    }#;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "    ];\n" +
                "  }\n" +
                "}\n";
    }

    @Override
    public String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-27:1]: Duplicated element 'anything::class'";
    }

    @Test
    public void testServiceWithSingleExecution()
    {
        String resource = "Class test::class\n" +
                "{\n" +
                "  prop1 : Integer[0..1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                ")\n" +
                "###Connection\n" +
                "JsonModelConnection test::connection\n" +
                "{\n" +
                "  class : test::class;" +
                "  url : 'asd';\n" +
                "}\n" +
                "###Runtime\n" +
                "Runtime test::runtime\n" +
                "{\n" +
                " mappings: [test::mapping];\n" +
                "}\n";
        // check matching execution and test type
        test(resource + "###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src:    test::class[1]|$src.prop1;\n" +
                "      mapping: test::mapping;\n" +
                "      runtime: test::runtime;\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [32:9-34:3]: Test does not match execution type");
        // test service execution query lambda
        test(resource + "###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                // intentionally mess up the spacing here to test source information
                "    query: src:    test::class2[1]|$src.prop1;\n" +
                "    mapping: test::mapping;\n" +
                "    runtime: test::runtime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "    ];\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [28:20-31]: Can't find type 'test::class2'");
        // test service execution mapping
        test(resource + "###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: test::class[1]|$src.prop1;\n" +
                "    mapping: test::mapping2;\n" +
                "    runtime: test::runtime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "    ];\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [29:14-27]: Can't find mapping 'test::mapping2'");
        // test service execution runtime pointer
        test(resource + "###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: test::class[1]|$src.prop1;\n" +
                "    mapping: test::mapping;\n" +
                "    runtime: test::runtime2;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "    ];\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [30:14-27]: Can't find runtime 'test::runtime2'");
        // test service test assert lambda
        test(resource + "###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
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
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                // intentionally mess up the spacing here to test source information
                "      { [], res:   Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
                "    ];\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [37:20-32]: Can't find type 'Result<Any|*>'");
        // check service execution embedded runtime
        test(resource + "###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: test::class[1]|$src.prop1;\n" +
                "    mapping: test::mapping;\n" +
                "    runtime:\n" +
                // embedded mapping is implied
                "    #{\n" +
                "      connections:\n" +
                "      [\n" +
                "        ModelStore:\n" +
                "        [\n" +
                "          id1: test::connection2,\n" +
                "          id2: #{\n" +
                "            JsonModelConnection\n" +
                "            {\n" +
                "              class: test::class;\n" +
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
                "    ];\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [36:16-32]: Can't find connection 'test::connection2'");
        // check source information processing for embedded runtime
        test(resource + "###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: test::class[1]|$src.prop1;\n" +
                "    mapping: test::mapping;\n" +
                "    runtime: #{ connections: [ ModelStore: [ id1:       test::connection2, id2: #{\n" +
                "            JsonModelConnection\n" +
                "            {\n" +
                "              class: test::class;\n" +
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
                "  }\n" +
                "}\n", "COMPILATION error at [30:57-73]: Can't find connection 'test::connection2'");
        // test service execution embedded runtime connection
        test(resource + "###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName', 'ownerName2'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: test::class[1]|$src.prop1;\n" +
                "    mapping: test::mapping;\n" +
                "    runtime:\n" +
                "    #{\n" +
                "      connections:\n" +
                "      [\n" +
                "        ModelStore:\n" +
                "        [\n" +
                "          id1: test::connection,\n" +
                "          id2: #{\n" +
                "            JsonModelConnection\n" +
                "            {\n" +
                "              class: test::class2;\n" +
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
                "    ];\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [40:22-33]: Can't find class 'test::class2'");
    }

    @Test
    public void testServiceWithMultiExecution()
    {
        String resource = "Class test::class\n" +
                "{\n" +
                "  prop1 : Integer[0..1];\n" +
                "}\n" +
                "###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                ")\n" +
                "###Connection\n" +
                "JsonModelConnection test::connection\n" +
                "{\n" +
                "  class : test::class;" +
                "  url : 'asd';\n" +
                "}\n" +
                "###Runtime\n" +
                "Runtime test::runtime\n" +
                "{\n" +
                " mappings: [test::mapping];\n" +
                "}\n";
        // check for match between execution and test type
        test(resource + "###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                // intentionally mess up the spacing here to test source information
                "    query: src:    test::class[1]|$src.prop1;\n" +
                "    key: 'env';\n" +
                "    executions['PROD']:\n" +
                "    {\n" +
                "      mapping: test::mapping;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: test::mapping;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'testData';\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [41:9-44:3]: Test does not match execution type");
        // check empty multi execution
        test(resource + "###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: src:    test::class[1]|$src.prop1;\n" +
                "    key: 'env';\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [26:14-30:3]: Service multi execution must not be empty");
        // check empty multi test
        test(resource + "###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: src:    test::class[1]|$src.prop1;\n" +
                "    key: 'env';\n" +
                "    executions['PROD']:\n" +
                "    {\n" +
                "      mapping: test::mapping;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [36:9-38:3]: Service multi execution test must not be empty");
        // check duplicated execution parameter key values
        test(resource + "###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: src:    test::class[1]|$src.prop1;\n" +
                "    key: 'env';\n" +
                "    executions['PROD']:\n" +
                "    {\n" +
                "      mapping: test::mapping;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "    executions['PROD']:\n" +
                "    {\n" +
                "      mapping: test::mapping;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['PROD']:\n" +
                "    {\n" +
                "      data: 'testData';\n" +
                "    }\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'testData';\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [35:5-39:5]: Execution parameter with key 'PROD' already existed");
        // check duplicated test key values
        test(resource + "###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                // intentionally mess up the spacing here to test source information
                "    query: src:    test::class[1]|$src.prop1;\n" +
                "    key: 'env';\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: test::mapping;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "    executions['PROD']:\n" +
                "    {\n" +
                "      mapping: test::mapping;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'testData';\n" +
                "    }\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'testData';\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [47:5-50:5]: Service test with key 'QA' already existed");
        test(resource + "###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: src:    test::class[1]|$src.prop1;\n" +
                "    key: 'env';\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: test::mapping;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'testData';\n" +
                "    }\n" +
                "    tests['PROD']:\n" +
                "    {\n" +
                "      data: 'testData';\n" +
                "    }\n" +
                "    tests['INT']:\n" +
                "    {\n" +
                "      data: 'testData';\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [36:9-50:3]: Test(s) with key 'PROD', 'INT' do not have a corresponding execution");
        // check service execution query lambda
        test(resource + "###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                // intentionally mess up the spacing here to test source information
                "    query: src:    test::class2[1]|$src.prop1;\n" +
                "    key: 'env';\n" +
                "    executions['PROD']:\n" +
                "    {\n" +
                "      mapping: test::mapping;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: test::mapping;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['PROD']:\n" +
                "    {\n" +
                "      data: 'testData';\n" +
                "    }\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'testData';\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [28:20-31]: Can't find type 'test::class2'");
        // check service execution mapping
        test(resource + "###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                // intentionally mess up the spacing here to test source information
                "    query: src:    test::class[1]|$src.prop1;\n" +
                "    key: 'env';\n" +
                "    executions['PROD']:\n" +
                "    {\n" +
                "      mapping: test::mapping2;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: test::mapping;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['PROD']:\n" +
                "    {\n" +
                "      data: 'testData';\n" +
                "    }\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'testData';\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [32:16-29]: Can't find mapping 'test::mapping2'");
        // check service execution runtime
        test(resource + "###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                // intentionally mess up the spacing here to test source information
                "    query: src:    test::class[1]|$src.prop1;\n" +
                "    key: 'env';\n" +
                "    executions['PROD']:\n" +
                "    {\n" +
                "      mapping: test::mapping;\n" +
                "      runtime: test::runtime2;\n" +
                "    }\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: test::mapping;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['PROD']:\n" +
                "    {\n" +
                "      data: 'testData';\n" +
                "    }\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'testData';\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [33:16-29]: Can't find runtime 'test::runtime2'");
        // check service test assertion lambda
        test(resource + "###Service\n" +
                "Service test::Service\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners: ['ownerName'];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                // intentionally mess up the spacing here to test source information
                "    query: src:    test::class[1]|$src.prop1;\n" +
                "    key: 'env';\n" +
                "    executions['PROD']:\n" +
                "    {\n" +
                "      mapping: test::mapping;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                "      mapping: test::mapping;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['PROD']:\n" +
                "    {\n" +
                "      data: 'testData';\n" +
                "    }\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'testData';\n" +
                "      asserts: [{ [], res:   Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }];\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [50:30-42]: Can't find type 'Result<Any|*>'");
    }

    @Test
    public void testServiceWithImport()
    {
        test("Class meta::mySimpleClass\n" +
                "{\n" +
                "  name: String[1];\n" +
                "}\n" +
                "\n\n" +
                "###Mapping\n" +
                "Mapping meta::mySimpleMapping\n" +
                "(\n" +
                ")\n\n\n" +
                "###Connection\n" +
                "JsonModelConnection meta::myConnection\n" +
                "{\n" +
                "  class: meta::mySimpleClass;\n" +
                "  url: 'dummy';\n" +
                "}\n\n\n" +
                "###Runtime\n" +
                "Runtime meta::mySimpleRuntime\n" +
                "{\n" +
                "  mappings: [meta::mySimpleMapping];\n" +
                "}\n\n\n" +
                "###Service\n" +
                "import meta::*;\n" +
                "Service meta::pure::myServiceSingle\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners:[];\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                // Pure single execution query
                "    query: src: mySimpleClass[1]|$src.name;\n" +
                // Pure single execution mapping
                "    mapping: mySimpleMapping;\n" +
                // Pure single execution runtime
                "    runtime: mySimpleRuntime;\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                // single service test assertion
                "      { [], res: mySimpleClass[1]|$res.name == '1' }\n" +
                "    ];\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                "Service meta::pure::myServiceMulti\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners:[];\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                // Pure multi execution query
                "    query: src: mySimpleClass[1]|$src.name;\n" +
                "    key: 'env';\n" +
                "    executions['QA']:\n" +
                "    {\n" +
                // Pure multi execution mapping
                "      mapping: mySimpleMapping;\n" +
                // Pure multi execution runtime
                "      runtime: mySimpleRuntime;\n" +
                "    }\n" +
                "  }\n" +
                "  test: Multi\n" +
                "  {\n" +
                "    tests['QA']:\n" +
                "    {\n" +
                "      data: 'moreData';\n" +
                "      asserts:\n" +
                "    [\n" +
                // multi service test assertion
                "      { [], res: mySimpleClass[1]|$res.name == '1' }\n" +
                "    ];\n" +
                "    }\n" +
                "  }\n" +
                "}\n" +
                "\n" +
                // Service with embedded runtime
                "Service meta::pure::myServiceSingleWithEmbeddedRuntime\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: mySimpleClass[1]|$src.name;\n" +
                "    mapping: mySimpleMapping;\n" +
                "    runtime:\n" +
                "    #{\n" +
                "      connections:\n" +
                "      [\n" +
                "        ModelStore:\n" +
                "        [\n" +
                // embedded connection pointer
                "          id1: myConnection,\n" +
                "          id3: #{\n" +
                "            JsonModelConnection\n" +
                "            {\n" +
                // embedded connection value
                "              class: mySimpleClass;\n" +
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
                "    ];\n" +
                "  }\n" +
                "}\n");
    }
}
