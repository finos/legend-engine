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
    public void testFaultyAnnotations()
    {
        // Faulty stereotype
        test("###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Service\n" +
                "Service <<NoProfile.NoKey>> anything::class\n" +
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
                "}\n", "COMPILATION error at [4:11-19]: Can't find the profile 'NoProfile'");
        // Faulty tagged value

        test("###Mapping\n" +
                "Mapping anything::somethingelse ()\n" +
                "###Service\n" +
                "Service { NoProfile.NoKey = 'something' } anything::class\n" +
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
                "}\n", "COMPILATION error at [4:11-19]: Can't find the profile 'NoProfile'");
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

        test(resource + "###Service\n" +
                "Service meta::pure::myServiceMulti\n" +
                "{\n" +
                "  pattern: 'url/myUrl/{env}';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'ownerName'\n" +
                "  ];\n" +
                "  documentation: 'this is just for context';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Multi\n" +
                "  {\n" +
                "    query: {src:    test::class[1]|$src.prop1};\n" +
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
                "    tests['PROD']:\n" +
                "    {\n" +
                "      data: 'moreData';\n" +
                "      asserts:\n" +
                "      [\n" +
                "        { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 },\n" +
                "        { [], res: Result<Any|*>[1]|$res.values->cast(@TabularDataSet).rows->size() == 1 }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "}\n\n" +
                "ExecutionEnvironment test::executionEnvironment\n" +
                "{\n" +
                "      executions:\n" +
                "      [\n" +
                "        QA:\n" +
                "        {\n" +
                "          mapping: test::mapping;\n" +
                "          runtime: test::runtime;\n" +
                "        },\n" +
                "        PROD:\n" +
                "        {\n" +
                "          mapping: test::mapping;\n" +
                "          runtime: test::runtime;\n" +
                "        }\n" +
                "      ];\n" +
                "}\n", "COMPILATION error at [29:14-32:3]: Service multi execution must not be empty");
    }

    @Test
    public void testServiceCompilationWithOnlyLambda()
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
                "    query: |test::class.all()->graphFetch(#{test::class{prop1}}#)->from(test::mapping, test::mapping);\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "    ];\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [28:68-71]: Can't find a match for function 'from(class[*],Mapping[1],Mapping[1])'");

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
                "    query: |test::class.all()->graphFetch(#{test::class{prop1}}#)->from(test::runtime, test::mapping);\n" +
                "  }\n" +
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "    ];\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [28:68-71]: Can't find a match for function 'from(class[*],Runtime[1],Mapping[1])'");
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

    @Test
    public void testServiceTestParameters()
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

        // check for single test parameter
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
                "  test: Single\n" +
                "  {\n" +
                "    data: 'test';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      {['testparameter'],'testexpression'}\n" +
                "    ];\n" +
                "  }\n" +
                "}\n");

        // check for multiple test parameters
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
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      {['parameter1', %23:12:8.54, true, 440, 13.23, 88, -54, 2.3, [1, 2], %2019-05-24T00:00:00, %2019-05-24],1+280+1}\n" + //test expression
                "    ];\n" +
                "  }\n" +
                "}\n");

        // check for undefined enumerations
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
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      {['parameter1', Enum.reference],1+280+1}\n" + //test expression
                "    ];\n" +
                "  }\n" +
                "}\n", "COMPILATION error at [37:23-36]: Can't find enumeration 'Enum'");


        // check for multiple test asserts
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
                "  test: Single\n" +
                "  {\n" +
                "    data: 'moreThanData';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      {['testparameter1', 'testparameter2'],'expression1'},\n" +
                "      {['testparameter', 22, 3.14],'expression2'},\n" +
                "      {[],'expression3'}\n" +
                "    ];\n" +
                "  }\n" +
                "}\n");

        //test  list param
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
                "  test: Single\n" +
                "  {\n" +
                "    data: 'test';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      {[list(['param'])],'testexpression'}\n" +
                "    ];\n" +
                "  }\n" +
                "}\n");


        //test single and list param
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
                "  test: Single\n" +
                "  {\n" +
                "    data: 'test';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      {[list(['param']),'testparameter'],'testexpression'}\n" +
                "    ];\n" +
                "  }\n" +
                "}\n");

        //test many list param
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
                "  test: Single\n" +
                "  {\n" +
                "    data: 'test';\n" +
                "    asserts:\n" +
                "    [\n" +
                "      {[list(['param']), 'singleparam', list([1,2,3]), list([%2019-05-24, %2019-05-25])],'testexpression'}\n" +
                "    ];\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    public void testServiceTestSuite()
    {
        String resource = "Class test::model::Firm\n" +
                "{\n" +
                "  employees: test::model::Person[1..*];\n" +
                "  legalName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::model::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::store::S_Firm\n" +
                "{\n" +
                "  employees: test::store::S_Person[1..*];\n" +
                "  legalName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::store::S_Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::mapping::FirmMapping\n" +
                "(\n" +
                "  *test::model::Firm: Pure\n" +
                "  {\n" +
                "    ~src test::store::S_Firm\n" +
                "    legalName: $src.legalName,\n" +
                "    employees[test_model_Person]: $src.employees\n" +
                "  }\n" +
                "  *test::model::Person: Pure\n" +
                "  {\n" +
                "    ~src test::store::S_Person\n" +
                "    firstName: $src.firstName,\n" +
                "    lastName: $src.lastName\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Connection\n" +
                "JsonModelConnection test::runtime::SFirmConnection\n" +
                "{\n" +
                "  class: test::store::S_Firm;\n" +
                "  url: 'executor:default';\n" +
                "}\n" +
                "JsonModelConnection test::runtime::SPersonConnection\n" +
                "{\n" +
                "  class: test::store::S_Person;\n" +
                "  url: 'executor:default';\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime test::runtime::SFirmRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping::FirmMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection1: test::runtime::SFirmConnection\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n" +
                "Runtime test::runtime::SFirmAndSPersonRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping::FirmMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection1: test::runtime::SFirmConnection,\n" +
                "      connection2: test::runtime::SPersonConnection\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n";

        //Test Empty TestSuite
        test(resource + "###Service\n" +
                "Service test::service::FirmService\n" +
                "{\n" +
                "  pattern: '/testFirmService';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'owner1',\n" +
                "    'owner2'\n" +
                "  ];\n" +
                "  documentation: '';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: name:String[1]|test::model::Firm.all()->filter(f | $f.legalName == $name )->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                "    mapping: test::mapping::FirmMapping;\n" +
                "    runtime: test::runtime::SFirmRuntime;\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "  ]\n" +
                "}\n"
        );

        //Test Single TestSuite without data
        test(resource + "###Service\n" +
                "Service test::service::FirmService\n" +
                "{\n" +
                "  pattern: '/testFirmService';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'owner1',\n" +
                "    'owner2'\n" +
                "  ];\n" +
                "  documentation: '';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: |test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                "    mapping: test::mapping::FirmMapping;\n" +
                "    runtime: test::runtime::SFirmRuntime;\n" +
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
                "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
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
        test(resource + "###Service\n" +
                "Service test::service::FirmService\n" +
                "{\n" +
                "  pattern: '/testFirmService';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'owner1',\n" +
                "    'owner2'\n" +
                "  ];\n" +
                "  documentation: '';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: |test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                "    mapping: test::mapping::FirmMapping;\n" +
                "    runtime: test::runtime::SFirmRuntime;\n" +
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
                "              contentType: 'application/json';\n" +
                "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
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
                "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
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
        test(resource + "###Service\n" +
                "Service test::service::FirmService\n" +
                "{\n" +
                "  pattern: '/testFirmService';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'owner1',\n" +
                "    'owner2'\n" +
                "  ];\n" +
                "  documentation: '';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: |test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                "    mapping: test::mapping::FirmMapping;\n" +
                "    runtime: test::runtime::SFirmAndSPersonRuntime;\n" +
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
                "              contentType: 'application/json';\n" +
                "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
                "            }#,\n" +
                "          connection2:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/json';\n" +
                "              data: '[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}]';\n" +
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
                "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
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
        test(resource + "###Service\n" +
                "Service test::service::FirmService\n" +
                "{\n" +
                "  pattern: '/testFirmService';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'owner1',\n" +
                "    'owner2'\n" +
                "  ];\n" +
                "  documentation: '';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: |test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                "    mapping: test::mapping::FirmMapping;\n" +
                "    runtime: test::runtime::SFirmRuntime;\n" +
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
                "              contentType: 'application/json';\n" +
                "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
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
                "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
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
                "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
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
        test(resource + "###Service\n" +
                "Service test::service::FirmService\n" +
                "{\n" +
                "  pattern: '/testFirmService';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'owner1',\n" +
                "    'owner2'\n" +
                "  ];\n" +
                "  documentation: '';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: param: String[1]|test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                "    mapping: test::mapping::FirmMapping;\n" +
                "    runtime: test::runtime::SFirmRuntime;\n" +
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
                "              contentType: 'application/json';\n" +
                "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          parameters:\n" +
                "          [\n" +
                "            param = 'dummy'" +
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
                "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
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
        test(resource + "###Service\n" +
                "Service test::service::FirmService\n" +
                "{\n" +
                "  pattern: '/testFirmService';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'owner1',\n" +
                "    'owner2'\n" +
                "  ];\n" +
                "  documentation: '';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: {stringParam: String[1],stringOptionalParam: String[0..1],stringListParam: String[*],integerParam: Integer[1],integerOptionalParam: Integer[0..1],integerListParam: Integer[*],floatParam: Float[1],floatOptionalParam: Float[0..1],floatListParam: Float[*],strictDateParam: StrictDate[1],strictDateOptionalParam: StrictDate[0..1],strictDateListParam: StrictDate[*],dateTimeParam: DateTime[1],dateTimeOptionalParam: DateTime[0..1],dateTimeListParam: DateTime[*],booleanParam: Boolean[1],booleanOptionalParam: Boolean[0..1],booleanListParam: Boolean[*]|test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#)};\n" +
                "    mapping: test::mapping::FirmMapping;\n" +
                "    runtime: test::runtime::SFirmRuntime;\n" +
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
                "              contentType: 'application/json';\n" +
                "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
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
                "            integerListParam = [1, 2],\n" +
                "            floatParam = 1.123,\n" +
                "            floatOptionalParam = [],\n" +
                "            floatListParam = 1.123,\n" +
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
                "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n"
        );

        //Test Single TestSuite with multiple asserts
        test(resource + "###Service\n" +
                "Service test::service::FirmService\n" +
                "{\n" +
                "  pattern: '/testFirmService';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'owner1',\n" +
                "    'owner2'\n" +
                "  ];\n" +
                "  documentation: '';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: |test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                "    mapping: test::mapping::FirmMapping;\n" +
                "    runtime: test::runtime::SFirmRuntime;\n" +
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
                "              contentType: 'application/json';\n" +
                "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
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
                "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
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
        test(resource + "###Service\n" +
                "Service test::service::FirmService\n" +
                "{\n" +
                "  pattern: '/testFirmService';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'owner1',\n" +
                "    'owner2'\n" +
                "  ];\n" +
                "  documentation: '';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: |test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                "    mapping: test::mapping::FirmMapping;\n" +
                "    runtime: test::runtime::SFirmAndSPersonRuntime;\n" +
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
                "              contentType: 'application/json';\n" +
                "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
                "            }#,\n" +
                "          connection2:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/json';\n" +
                "              data: '[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}]';\n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          parameters:\n" +
                "          [\n" +
                "            param = 'dummy1'" +
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
                "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        },\n" +
                "        test2:\n" +
                "        {\n" +
                "          parameters:\n" +
                "          [\n" +
                "            param = 'dummy2'" +
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
                "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
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

        //Test Multiple TestSuite with complex semantics
        test(resource + "###Service\n" +
                "Service test::service::FirmService\n" +
                "{\n" +
                "  pattern: '/testFirmService';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'owner1',\n" +
                "    'owner2'\n" +
                "  ];\n" +
                "  documentation: '';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: |test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                "    mapping: test::mapping::FirmMapping;\n" +
                "    runtime: test::runtime::SFirmAndSPersonRuntime;\n" +
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
                "              contentType: 'application/json';\n" +
                "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
                "            }#,\n" +
                "          connection2:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/json';\n" +
                "              data: '[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}]';\n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          parameters:\n" +
                "          [\n" +
                "            param = 'dummy1'" +
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
                "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        },\n" +
                "        test2:\n" +
                "        {\n" +
                "          parameters:\n" +
                "          [\n" +
                "            param = 'dummy2'" +
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
                "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
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
                "              contentType: 'application/json';\n" +
                "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
                "            }#,\n" +
                "          connection2:\n" +
                "            ExternalFormat\n" +
                "            #{\n" +
                "              contentType: 'application/json';\n" +
                "              data: '[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}]';\n" +
                "            }#\n" +
                "        ]\n" +
                "      ]\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          parameters:\n" +
                "          [\n" +
                "            param = 'dummy1'" +
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
                "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        },\n" +
                "        test2:\n" +
                "        {\n" +
                "          serializationFormat: CSV;\n" +
                "          parameters:\n" +
                "          [\n" +
                "            param = 'dummy2'" +
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
                "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
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
    }

    @Test
    public void testServiceTestSuiteCompilationErrorMessages()
    {
        String resource = "Class test::model::Firm\n" +
                "{\n" +
                "  employees: test::model::Person[1..*];\n" +
                "  legalName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::model::Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::store::S_Firm\n" +
                "{\n" +
                "  employees: test::store::S_Person[1..*];\n" +
                "  legalName: String[1];\n" +
                "}\n" +
                "\n" +
                "Class test::store::S_Person\n" +
                "{\n" +
                "  firstName: String[1];\n" +
                "  lastName: String[1];\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Mapping\n" +
                "Mapping test::mapping::FirmMapping\n" +
                "(\n" +
                "  *test::model::Firm: Pure\n" +
                "  {\n" +
                "    ~src test::store::S_Firm\n" +
                "    legalName: $src.legalName,\n" +
                "    employees[test_model_Person]: $src.employees\n" +
                "  }\n" +
                "  *test::model::Person: Pure\n" +
                "  {\n" +
                "    ~src test::store::S_Person\n" +
                "    firstName: $src.firstName,\n" +
                "    lastName: $src.lastName\n" +
                "  }\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Connection\n" +
                "JsonModelConnection test::runtime::SFirmConnection\n" +
                "{\n" +
                "  class: test::store::S_Firm;\n" +
                "  url: 'executor:default';\n" +
                "}\n" +
                "JsonModelConnection test::runtime::SPersonConnection\n" +
                "{\n" +
                "  class: test::store::S_Person;\n" +
                "  url: 'executor:default';\n" +
                "}\n" +
                "\n" +
                "\n" +
                "###Runtime\n" +
                "Runtime test::runtime::SFirmRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping::FirmMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection1: test::runtime::SFirmConnection\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n" +
                "Runtime test::runtime::SFirmAndSPersonRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    test::mapping::FirmMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    ModelStore:\n" +
                "    [\n" +
                "      connection1: test::runtime::SFirmConnection,\n" +
                "      connection2: test::runtime::SPersonConnection\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n";

        //Test Single TestSuite without tests
        test(resource + "###Service\n" +
                        "Service test::service::FirmService\n" +
                        "{\n" +
                        "  pattern: '/testFirmService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'owner1',\n" +
                        "    'owner2'\n" +
                        "  ];\n" +
                        "  documentation: '';\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                        "    mapping: test::mapping::FirmMapping;\n" +
                        "    runtime: test::runtime::SFirmRuntime;\n" +
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
                        "              contentType: 'application/json';\n" +
                        "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "COMPILATION error at [106:5-123:5]: Service TestSuites should have atleast 1 test"
        );

        //Test Single TestSuite without asserts
        test(resource + "###Service\n" +
                        "Service test::service::FirmService\n" +
                        "{\n" +
                        "  pattern: '/testFirmService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'owner1',\n" +
                        "    'owner2'\n" +
                        "  ];\n" +
                        "  documentation: '';\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                        "    mapping: test::mapping::FirmMapping;\n" +
                        "    runtime: test::runtime::SFirmRuntime;\n" +
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
                        "              contentType: 'application/json';\n" +
                        "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          asserts:\n" +
                        "          [\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "COMPILATION error at [122:9-127:9]: Service Tests should have atleast 1 assert"
        );

        //Multiple TestSuites with same ids
        test(resource + "###Service\n" +
                        "Service test::service::FirmService\n" +
                        "{\n" +
                        "  pattern: '/testFirmService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'owner1',\n" +
                        "    'owner2'\n" +
                        "  ];\n" +
                        "  documentation: '';\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                        "    mapping: test::mapping::FirmMapping;\n" +
                        "    runtime: test::runtime::SFirmRuntime;\n" +
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
                        "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    },\n" +
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
                        "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "COMPILATION error at [88:1-151:1]: Multiple testSuites found with ids : 'testSuite1'"
        );

        //Multiple Tests with same ids
        test(resource + "###Service\n" +
                        "Service test::service::FirmService\n" +
                        "{\n" +
                        "  pattern: '/testFirmService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'owner1',\n" +
                        "    'owner2'\n" +
                        "  ];\n" +
                        "  documentation: '';\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                        "    mapping: test::mapping::FirmMapping;\n" +
                        "    runtime: test::runtime::SFirmRuntime;\n" +
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
                        "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        },\n" +
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
                        "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "COMPILATION error at [106:5-143:5]: Multiple tests found with ids : 'test1'"
        );

        //Multiple Asserts with same ids
        test(resource + "###Service\n" +
                        "Service test::service::FirmService\n" +
                        "{\n" +
                        "  pattern: '/testFirmService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'owner1',\n" +
                        "    'owner2'\n" +
                        "  ];\n" +
                        "  documentation: '';\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                        "    mapping: test::mapping::FirmMapping;\n" +
                        "    runtime: test::runtime::SFirmRuntime;\n" +
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
                        "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
                        "                  }#;\n" +
                        "              }#,\n" +
                        "            assert1:\n" +
                        "              EqualToJson\n" +
                        "              #{\n" +
                        "                expected : \n" +
                        "                  ExternalFormat\n" +
                        "                  #{\n" +
                        "                    contentType: 'application/json';\n" +
                        "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "COMPILATION error at [110:9-135:9]: Multiple assertions found with ids : 'assert1'"
        );

        //Multiple Test data with same connection ids
        test(resource + "###Service\n" +
                        "Service test::service::FirmService\n" +
                        "{\n" +
                        "  pattern: '/testFirmService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'owner1',\n" +
                        "    'owner2'\n" +
                        "  ];\n" +
                        "  documentation: '';\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: |test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                        "    mapping: test::mapping::FirmMapping;\n" +
                        "    runtime: test::runtime::SFirmRuntime;\n" +
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
                        "              contentType: 'application/json';\n" +
                        "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
                        "            }#,\n" +
                        "          connection1:\n" +
                        "            ExternalFormat\n" +
                        "            #{\n" +
                        "              contentType: 'application/json';\n" +
                        "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
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
                        "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "COMPILATION error at [108:7-125:7]: Multiple connection test data found with ids : 'connection1'"
        );

        // Mis-match between parameter type & parameter value
        test(resource + "###Service\n" +
                        "Service test::service::FirmService\n" +
                        "{\n" +
                        "  pattern: '/testFirmService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'owner1',\n" +
                        "    'owner2'\n" +
                        "  ];\n" +
                        "  documentation: '';\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: param:String[1]|test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                        "    mapping: test::mapping::FirmMapping;\n" +
                        "    runtime: test::runtime::SFirmRuntime;\n" +
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
                        "              contentType: 'application/json';\n" +
                        "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          parameters:\n" +
                        "          [\n" +
                        "            param = 123" +
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
                        "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "COMPILATION error at [122:9-140:9]: Parameter value type does not match with parameter type for parameter: 'param'"
        );

        test(resource + "###Service\n" +
                        "Service test::service::FirmService\n" +
                        "{\n" +
                        "  pattern: '/testFirmService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'owner1',\n" +
                        "    'owner2'\n" +
                        "  ];\n" +
                        "  documentation: '';\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: param:String[1]|test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                        "    mapping: test::mapping::FirmMapping;\n" +
                        "    runtime: test::runtime::SFirmRuntime;\n" +
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
                        "              contentType: 'application/json';\n" +
                        "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          parameters:\n" +
                        "          [\n" +
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
                        "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "COMPILATION error at [122:9-140:9]: Parameter value required for parameter: 'param'"
        );

        test(resource + "###Service\n" +
                        "Service test::service::FirmService\n" +
                        "{\n" +
                        "  pattern: '/testFirmService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'owner1',\n" +
                        "    'owner2'\n" +
                        "  ];\n" +
                        "  documentation: '';\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: param:String[1]|test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                        "    mapping: test::mapping::FirmMapping;\n" +
                        "    runtime: test::runtime::SFirmRuntime;\n" +
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
                        "              contentType: 'application/json';\n" +
                        "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          parameters:\n" +
                        "          [\n" +
                        "            param = ['dummy1', 'dummy2']" +
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
                        "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "COMPILATION error at [122:9-140:9]: Parameter value type does not match with parameter type for parameter: 'param'"
        );

        test(resource + "###Service\n" +
                        "Service test::service::FirmService\n" +
                        "{\n" +
                        "  pattern: '/testFirmService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'owner1',\n" +
                        "    'owner2'\n" +
                        "  ];\n" +
                        "  documentation: '';\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: param:String[0..1]|test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                        "    mapping: test::mapping::FirmMapping;\n" +
                        "    runtime: test::runtime::SFirmRuntime;\n" +
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
                        "              contentType: 'application/json';\n" +
                        "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          parameters:\n" +
                        "          [\n" +
                        "            param = ['dummy1', 'dummy2']" +
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
                        "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "COMPILATION error at [122:9-140:9]: Parameter value type does not match with parameter type for parameter: 'param'"
        );

        test(resource + "###Service\n" +
                        "Service test::service::FirmService\n" +
                        "{\n" +
                        "  pattern: '/testFirmService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'owner1',\n" +
                        "    'owner2'\n" +
                        "  ];\n" +
                        "  documentation: '';\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: param:String[0..1]|test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                        "    mapping: test::mapping::FirmMapping;\n" +
                        "    runtime: test::runtime::SFirmRuntime;\n" +
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
                        "              contentType: 'application/json';\n" +
                        "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          parameters:\n" +
                        "          [\n" +
                        "            param = 123" +
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
                        "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "COMPILATION error at [122:9-140:9]: Parameter value type does not match with parameter type for parameter: 'param'"
        );

        test(resource + "###Service\n" +
                        "Service test::service::FirmService\n" +
                        "{\n" +
                        "  pattern: '/testFirmService';\n" +
                        "  owners:\n" +
                        "  [\n" +
                        "    'owner1',\n" +
                        "    'owner2'\n" +
                        "  ];\n" +
                        "  documentation: '';\n" +
                        "  autoActivateUpdates: true;\n" +
                        "  execution: Single\n" +
                        "  {\n" +
                        "    query: param:String[*]|test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
                        "    mapping: test::mapping::FirmMapping;\n" +
                        "    runtime: test::runtime::SFirmRuntime;\n" +
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
                        "              contentType: 'application/json';\n" +
                        "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
                        "            }#\n" +
                        "        ]\n" +
                        "      ]\n" +
                        "      tests:\n" +
                        "      [\n" +
                        "        test1:\n" +
                        "        {\n" +
                        "          parameters:\n" +
                        "          [\n" +
                        "            param = 123" +
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
                        "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
                        "                  }#;\n" +
                        "              }#\n" +
                        "          ]\n" +
                        "        }\n" +
                        "      ]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}\n",
                "COMPILATION error at [122:9-140:9]: Parameter value type does not match with parameter type for parameter: 'param'"
        );

        //TODO: Test data with wrong connection id
        //        test(resource + "###Service\n" +
        //                "Service test::service::FirmService\n" +
        //                "{\n" +
        //                "  pattern: '/testFirmService';\n" +
        //                "  owners:\n" +
        //                "  [\n" +
        //                "    'owner1',\n" +
        //                "    'owner2'\n" +
        //                "  ];\n" +
        //                "  documentation: '';\n" +
        //                "  autoActivateUpdates: true;\n" +
        //                "  execution: Single\n" +
        //                "  {\n" +
        //                "    query: |test::model::Firm.all()->graphFetch(#{test::model::Firm{employees{firstName,lastName},legalName}}#)->serialize(#{test::model::Firm{employees{firstName,lastName},legalName}}#);\n" +
        //                "    mapping: test::mapping::FirmMapping;\n" +
        //                "    runtime: test::runtime::SFirmRuntime;\n" +
        //                "  }\n" +
        //                "  testSuites:\n" +
        //                "  [\n" +
        //                "    testSuite1:\n" +
        //                "    {\n" +
        //                "      data:\n" +
        //                "      [\n" +
        //                "        connections:\n" +
        //                "        [\n" +
        //                "          connection_1:\n" +
        //                "            ExternalFormat\n" +
        //                "            #{\n" +
        //                "              contentType: 'application/json';\n" +
        //                "              data: '[{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}, {\"employees\":[{\"firstName\":\"firstName 37\",\"lastName\":\"lastName 78\"}],\"legalName\":\"legalName 20\"}]';\n" +
        //                "            }#\n" +
        //                "        ]\n" +
        //                "      ]\n" +
        //                "      tests:\n" +
        //                "      [\n" +
        //                "        test1:\n" +
        //                "        {\n" +
        //                "          asserts:\n" +
        //                "          [\n" +
        //                "            assert1:\n" +
        //                "              EqualToJson\n" +
        //                "              #{\n" +
        //                "                expected : \n" +
        //                "                  ExternalFormat\n" +
        //                "                  #{\n" +
        //                "                    contentType: 'application/json';\n" +
        //                "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
        //                "                  }#;\n" +
        //                "              }#\n" +
        //                "          ]\n" +
        //                "        }\n" +
        //                "      ]\n" +
        //                "    }\n" +
        //                "  ]\n" +
        //                "}\n",
        //                ""
        //        );
    }

    @Test
    public void testBindingServices()
    {
        String resource = "###Pure\n" +
                "Enum test::firm::model::AddressType\n" +
                "{\n" +
                "   Headquarters,\n" +
                "   RegionalOffice,\n" +
                "   Home,\n" +
                "   Holiday\n" +
                "}\n" +
                "\n" +
                "Class test::firm::model::Firm\n" +
                "{\n" +
                "   name      : String[1];\n" +
                "   ranking   : Integer[0..1];\n" +
                "   addresses : test::firm::model::AddressUse[1..*];\n" +
                "}\n" +
                "\n" +
                "Class test::firm::model::Address\n" +
                "{\n" +
                "   firstLine  : String[1];\n" +
                "   secondLine : String[0..1];\n" +
                "   city       : String[0..1];\n" +
                "   region     : String[0..1];\n" +
                "   country    : String[1];\n" +
                "   position   : test::firm::model::GeographicPosition[0..1];\n" +
                "}\n" +
                "\n" +
                "Class test::firm::model::GeographicPosition\n" +
                "[\n" +
                "   validLatitude: ($this.latitude >= -90) && ($this.latitude <= 90),\n" +
                "   validLongitude: ($this.longitude >= -180) && ($this.longitude <= 180)\n" +
                "]\n" +
                "{\n" +
                "   latitude  : Decimal[1];\n" +
                "   longitude : Decimal[1];\n" +
                "}\n" +
                "\n" +
                "Class test::firm::model::AddressUse\n" +
                "{\n" +
                "   addressType : test::firm::model::AddressType[1];\n" +
                "   address     : test::firm::model::Address[1];\n" +
                "}\n" +
                "\n" +
                "Class test::firm::model::Person\n" +
                "{\n" +
                "   firstName      : String[1];\n" +
                "   lastName       : String[1];\n" +
                "   dateOfBirth    : StrictDate[0..1];   \n" +
                "   addresses      : test::firm::model::AddressUse[*];\n" +
                "   isAlive        : Boolean[1];\n" +
                "   heightInMeters : Float[1];\n" +
                "}\n" +
                "\n" +
                "Association test::firm::model::Firm_Person\n" +
                "{\n" +
                "   firm      : test::firm::model::Firm[1];\n" +
                "   employees : test::firm::model::Person[*];\n" +
                "}\n" +
                "\n\n" +
                "###ExternalFormat\n" +
                "Binding test::firm::model::TestBinding1\n" +
                "{\n" +
                "   contentType   : 'application/json';\n" +
                "   modelIncludes : [ test::firm::model::Firm, test::firm::model::Person, test::firm::model::Address, test::firm::model::AddressUse, test::firm::model::GeographicPosition ];" +
                "}\n" +
                "Binding test::firm::model::TestBinding2\n" +
                "{\n" +
                "   contentType   : 'application/json';\n" +
                "   modelIncludes : [ test::firm::model::Address, test::firm::model::GeographicPosition ];" +
                "}\n" +
                "\n\n";

        test(resource +
                "###Service\n" +
                "Service test::firm::model::myService\n" +
                "{\n" +
                "  pattern: '/showcase/binding';\n" +
                "  documentation: 'Showcase service with binding';\n" +
                "  autoActivateUpdates: false;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: data: String[1]|test::firm::model::Firm->internalize(test::firm::model::TestBinding1, $data)->externalize(test::firm::model::TestBinding1, #{test::firm::model::Firm{name, ranking}}#);\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "\n" +
                "  ]\n" +
                "}\n");

        test(resource +
                "###Service\n" +
                "Service test::firm::model::myService\n" +
                "{\n" +
                "  pattern: '/showcase/binding';\n" +
                "  documentation: 'Showcase service with binding';\n" +
                "  autoActivateUpdates: false;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: data: ByteStream[1]|test::firm::model::Firm->internalize(test::firm::model::TestBinding1, $data)->externalize(test::firm::model::TestBinding1, #{test::firm::model::Firm{name, ranking}}#);\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "\n" +
                "  ]\n" +
                "}\n");

        test(resource +
                "###Service\n" +
                "Service test::firm::model::myService\n" +
                "{\n" +
                "  pattern: '/showcase/binding';\n" +
                "  documentation: 'Showcase service with binding';\n" +
                "  autoActivateUpdates: false;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: data: String[1]|test::firm::model::Firm->internalize(test::firm::model::TestBinding1, $data)->externalize(test::firm::model::TestBinding2, #{test::firm::model::Firm{name, ranking}}#);\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "\n" +
                "  ]\n" +
                "}\n");

        test(resource +
                "###Service\n" +
                "Service test::firm::model::myService\n" +
                "{\n" +
                "  pattern: '/showcase/binding';\n" +
                "  documentation: 'Showcase service with binding';\n" +
                "  autoActivateUpdates: false;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: data: String[1]|test::firm::model::Firm->internalize(test::firm::model::TestBinding1, $data)->externalize(test::firm::model::TestBinding1, #{test::firm::model::Firm{name, ranking}}#);\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          parameters:\n" +
                "          [\n" +
                "            data = '[{\"street\":\"street A\"}]'\n" +
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
                "                    data: '[{\"street\":\"street A\"}]';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n");

        test(resource +
                "###Service\n" +
                "Service test::firm::model::myService\n" +
                "{\n" +
                "  pattern: '/showcase/binding';\n" +
                "  documentation: 'Showcase service with binding';\n" +
                "  autoActivateUpdates: false;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: data: ByteStream[1]|test::firm::model::Firm->internalize(test::firm::model::TestBinding1, $data)->externalize(test::firm::model::TestBinding1, #{test::firm::model::Firm{name, ranking}}#);\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          parameters:\n" +
                "          [\n" +
                "            data = byteStream('[{\"street\":\"street A\"}]')\n" +
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
                "                    data: '[{\"street\":\"street A\"}]';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n");

        test(resource +
                "###Service\n" +
                "Service test::firm::model::myService\n" +
                "{\n" +
                "  pattern: '/showcase/binding';\n" +
                "  documentation: 'Showcase service with binding';\n" +
                "  autoActivateUpdates: false;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: data: ByteStream[1]|test::firm::model::Firm->internalize(test::firm::model::TestBinding1, $data)->externalize(test::firm::model::TestBinding1, #{test::firm::model::Firm{name, ranking}}#);\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          parameters:\n" +
                "          [\n" +
                "            data = '[{\"street\":\"street A\"}]'\n" +
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
                "                    data: '[{\"street\":\"street A\"}]';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n", "COMPILATION error at [87:9-106:9]: Parameter value type does not match with parameter type for parameter: 'data'");
    }

    @Test
    public void testServiceWithPostValidation()
    {
        String resource = "Class test::class\n" +
                "{\n" +
                "  prop1 : String[1];\n" +
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

        // check valid post validation
        test(resource + "###Service \n" +
                "Service test::Service \n" +
                "{ \n" +
                "  pattern: 'url/myUrl/'; \n" +
                "  owners: ['ownerName']; \n" +
                "  documentation: 'test'; \n" +
                "  autoActivateUpdates: true; \n" +
                "  execution: Single \n" +
                "  { \n" +
                "    query: test::class.all()->project([col(p|$p.prop1, 'prop1')]); \n" +
                "    mapping: test::mapping; \n" +
                "    runtime: test::runtime; \n" +
                "  }\n" +
                "  postValidations:\n" +
                "  [\n" +
                "    {\n" +
                "      description: 'A good description of the validation';\n" +
                "      params: [];\n" +
                "      assertions: [\n" +
                "          testAssert: tds: TabularDataSet[1]|$tds->filter(row|$row.getString('prop1')->startsWith('X'))->meta::legend::service::validation::assertTabularDataSetEmpty('Expected no prop1 values to begin with the letter X');\n" +
                "      ];\n" +
                "    }\n" +
                "  ]\n" +
                "}");

        // check matching query and assertion types
        test(resource + "###Service \n" +
                "Service test::Service \n" +
                "{ \n" +
                "  pattern: 'url/myUrl/'; \n" +
                "  owners: ['ownerName']; \n" +
                "  documentation: 'test'; \n" +
                "  autoActivateUpdates: true; \n" +
                "  execution: Single \n" +
                "  { \n" +
                "    query: test::class.all()->project([col(p|$p.prop1, 'prop1')]); \n" +
                "    mapping: test::mapping; \n" +
                "    runtime: test::runtime; \n" +
                "  }\n" +
                "  postValidations:\n" +
                "  [\n" +
                "    {\n" +
                "      description: 'A good description of the validation';\n" +
                "      params: [];\n" +
                "      assertions: [\n" +
                "          testAssert: var: Integer[1]|true;\n" +
                "      ];\n" +
                "    }\n" +
                "  ]\n" +
                "}", " at [20:1-42:1]: Error in 'test::Service': Post validation assertion function parameter type 'Integer[1]' does not match with service execution return type 'TabularDataSet[1]'");

        // check parameter count matches service parameter count (multi execution)
        test(resource + "###Service \n" +
                "Service test::Service \n" +
                "{ \n" +
                "  pattern: 'url/myUrl/{executionKey}'; \n" +
                "  owners: ['ownerName']; \n" +
                "  documentation: 'test'; \n" +
                "  autoActivateUpdates: true; \n" +
                "  execution: Multi \n" +
                "  { \n" +
                "    query: |test::class.all()->project([col(p|$p.prop1, 'prop1')]); \n" +
                "    key: 'executionKey';" +
                "    executions['keyOne']: {" +
                "       mapping: test::mapping; \n" +
                "       runtime: test::runtime; \n" +
                "    }" +
                "    executions['keyTwo']: {" +
                "       mapping: test::mapping; \n" +
                "       runtime: test::runtime; \n" +
                "    }" +
                "  }\n" +
                "  postValidations:\n" +
                "  [\n" +
                "    {\n" +
                "      description: 'A good description of the validation';\n" +
                "      params: [];\n" +
                "      assertions: [\n" +
                "          testAssert: tds: TabularDataSet[1]|$tds->filter(row|$row.getString('firstName')->startsWith('T'))->meta::legend::service::validation::assertTabularDataSetEmpty('Expected no first names to begin with the letter T');\n" +
                "      ];\n" +
                "    }\n" +
                "  ]\n" +
                "}", " at [20:1-44:1]: Error in 'test::Service': Post validation parameter count '0' does not match with service parameter count '1'");

        // check assertion lambda has parameter
        test(resource + "###Service \n" +
                "Service test::Service \n" +
                "{ \n" +
                "  pattern: 'url/myUrl/'; \n" +
                "  owners: ['ownerName']; \n" +
                "  documentation: 'test'; \n" +
                "  autoActivateUpdates: true; \n" +
                "  execution: Single \n" +
                "  { \n" +
                "    query: test::class.all()->project([col(p|$p.prop1, 'prop1')]); \n" +
                "    mapping: test::mapping; \n" +
                "    runtime: test::runtime; \n" +
                "  }\n" +
                "  postValidations:\n" +
                "  [\n" +
                "    {\n" +
                "      description: 'A good description of the validation';\n" +
                "      params: [];\n" +
                "      assertions: [\n" +
                "          testAssert: |true;\n" +
                "      ];\n" +
                "    }\n" +
                "  ]\n" +
                "}", " at [20:1-42:1]: Error in 'test::Service': Post validation assertion function expects 1 parameter");
    }

    @Test
    public void testExecutionEnvironmentCompilation()
    {
        test("###Service\n" +
        "ExecutionEnvironment test::executionEnvironment\n" +
                "{\n" +
                "  executions:\n" +
                "  [\n" +
                "    UAT:\n" +
                "    {\n" +
                "      mapping: test::myMapping1;\n" +
                "      runtime: test::myRuntime1;\n" +
                "    },\n" +
                "    PROD:\n" +
                "    {\n" +
                "      mapping: test::myMapping2;\n" +
                "      runtime: test::myRuntime2;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n","COMPILATION error at [8:16-31]: Can't find mapping 'test::myMapping1'");

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

        test(resource + "###Service\n" +
                "ExecutionEnvironment test::executionEnvironment\n" +
                "{\n" +
                "  executions:\n" +
                "  [\n" +
                "    UAT:\n" +
                "    {\n" +
                "      mapping: test::mapping;\n" +
                "      runtime: test::myRuntime1;\n" +
                "    },\n" +
                "    PROD:\n" +
                "    {\n" +
                "      mapping: test::mapping;\n" +
                "      runtime: test::runtime;\n" +
                "    }\n" +
                "  ];\n" +
                "}\n","COMPILATION error at [27:16-31]: Can't find runtime 'test::myRuntime1'");
    }
}