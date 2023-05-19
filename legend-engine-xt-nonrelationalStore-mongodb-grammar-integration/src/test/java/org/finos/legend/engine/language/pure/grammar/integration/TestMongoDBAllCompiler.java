// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.grammar.integration;

import org.finos.legend.engine.language.pure.compiler.test.TestCompilationFromGrammar;
import org.junit.Test;

public class TestMongoDBAllCompiler extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{

    @Override
    protected String getDuplicatedElementTestCode()
    {
        return "Class anything::class {}\n" +
                "###Mapping\n" +
                "Mapping test::mongodb::mapping ()\n" +
                "###Service\n" +
                "Service test::mongodb::mapping\n" +
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
                "    query: |meta::external::store::mongodb::showcase::domain::Person.all()->graphFetch(#{meta::external::store::mongodb::showcase::domain::Person{firstName}}#)->serialize(#{meta::external::store::mongodb::showcase::domain::Person{firstName}}#);\n" +
                "    mapping: test::mongodb::mapping;\n" +
                "    runtime: test::mongodb::runtime;\n" +
                "  }\n" +
                "}";
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-20:1]: Duplicated element 'test::mongodb::mapping'";
    }

    @Test
    public void testMongoDBDefinition()
    {
        test(TestMongoDBCompilerUtil.MODEL_PLUS_BINDING +
                TestMongoDBCompilerUtil.SAMPLE_STORE +
                "###Mapping\n" +
                "Mapping mongo::test::mapping::MongoDBMapping\n" +
                "(\n" +
                "  *meta::external::store::mongodb::showcase::domain::Person[Person]: MongoDB\n" +
                "  {\n" +
                "    ~mainCollection [meta::external::store::mongodb::showcase::store::PersonDatabase] PersonCollection\n" +
                "    ~binding meta::external::store::mongodb::showcase::store::PersonCollectionBinding\n" +
                "  }\n" +
                ")\n\n\n" +
                "###Connection\n" +
                "MongoDBConnection mongo::test::connection::MongoDBConnection\n" +
                "{\n" +
                "  database: userDatabase;\n" +
                "  store: meta::external::store::mongodb::showcase::store::PersonDatabase;\n" +
                "  serverURLs: [localhost:12345];\n" +
                "  authentication: # UserPassword {\n" +
                "    username: 'sa';\n" +
                "    password: SystemPropertiesSecret\n" +
                "    {\n" +
                "      systemPropertyName: 'password';\n" +
                "    };\n" +
                "  }#;\n" +
                "}\n" +
                "###Runtime\n" +
                "Runtime mongo::test::runtime::MongoDBRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    mongo::test::mapping::MongoDBMapping\n" +
                "  ];\n" +
                "  connections:\n" +
                "  [\n" +
                "    meta::external::store::mongodb::showcase::store::PersonDatabase:\n" +
                "    [\n" +
                "      connection_1: mongo::test::connection::MongoDBConnection\n" +
                "    ]\n" +
                "  ];\n" +
                "}\n" +
                "###MongoDB\n" +
                "Database test::testEmptyDatabase\n" +
                "(\n" +
                ")\n\n\n" +
                "###Service\n" +
                "Service test::currentService\n" +
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
                "    query: |meta::external::store::mongodb::showcase::domain::Person.all()->graphFetch(#{meta::external::store::mongodb::showcase::domain::Person{firstName}}#)->serialize(#{meta::external::store::mongodb::showcase::domain::Person{firstName}}#);\n" +
                "    mapping: mongo::test::mapping::MongoDBMapping;\n" +
                "    runtime: mongo::test::runtime::MongoDBRuntime;\n" +
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
                "}\n");
    }
}
