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

package org.finos.legend.engine.language.pure.dsl.persistence.grammar.test;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestPersistenceGrammarV2Roundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void persistenceOptionalFieldsEmpty()
    {
        test("###Persistence\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  serviceOutputTargets:\n" +
                "  [\n" +
                "    TDS\n" +
                "    {\n" +
                "      keys:\n" +
                "      [\n" +
                "        foo, bar\n" +
                "      ]\n" +
                "      datasetType: Delta\n" +
                "      {\n" +
                "      }\n" +
                "    }\n" +
                "    ->\n" +
                "    {\n" +
                "    },\n" +
                "    #/test::example::MyType/prop#\n" +
                "    {\n" +
                "      keys:\n" +
                "      [\n" +
                "        #/test::example::MyType/prop/foo#, #/test::example::MyType/prop/bar#\n" +
                "      ]\n" +
                "      datasetType: Snapshot\n" +
                "      {\n" +
                "      }\n" +
                "    }\n" +
                "    ->\n" +
                "    {\n" +
                "    }\n" +
                "  ];\n" +
                "}\n");
    }

    @Test
    public void persistenceTdsOptionalFieldsDefaulted()
    {
        test("###Persistence\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  serviceOutputTargets:\n" +
                "  [\n" +
                "    TDS\n" +
                "    {\n" +
                "      keys:\n" +
                "      [\n" +
                "        foo, bar\n" +
                "      ]\n" +
                "      datasetType: Delta\n" +
                "      {\n" +
                "        actionIndicator: None;\n" +
                "      }\n" +
                "      deduplication: None;\n" +
                "    }\n" +
                "    ->\n" +
                "    {\n" +
                "    },\n" +
                "    TDS\n" +
                "    {\n" +
                "      keys:\n" +
                "      [\n" +
                "        hello, hi\n" +
                "      ]\n" +
                "      datasetType: Snapshot\n" +
                "      {\n" +
                "        partitioning: None\n" +
                "        {\n" +
                "          emptyDatasetHandling: NoOp;\n" +
                "        }\n" +
                "      }\n" +
                "      deduplication: None;\n" +
                "    }\n" +
                "    ->\n" +
                "    {\n" +
                "    }\n" +
                "  ];\n" +
                "  tests:\n" +
                "  [\n" +
                "    test1:\n" +
                "    {\n" +
                "      testBatches:\n" +
                "      [\n" +
                "        testBatch1:\n" +
                "        {\n" +
                "          data:\n" +
                "          {\n" +
                "            connection:\n" +
                "            {\n" +
                "              ExternalFormat\n" +
                "              #{\n" +
                "                contentType: 'application/x.flatdata';\n" +
                "                data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                "              }#\n" +
                "            }\n" +
                "          }\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected:\n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{\"Age\":12, \"Name\":\"dummy\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "      isTestDataFromServiceOutput: false;\n" +
                "    }\n" +
                "  ]\n" +
                "}\n");
    }

    @Test
    public void persistenceGraphFetchOptionalFieldsDefaulted()
    {
        test("###Persistence\n" +
            "Persistence test::TestPersistence\n" +
            "{\n" +
            "  doc: 'This is test documentation.';\n" +
            "  trigger: Manual;\n" +
            "  service: test::Service;\n" +
            "  serviceOutputTargets:\n" +
            "  [\n" +
            "    #/test::example::MyType/hello#\n" +
            "    {\n" +
            "      keys:\n" +
            "      [\n" +
            "        #/test::example::MyType/hello/key1#, #/test::example::MyType/hello/key2#\n" +
            "      ]\n" +
            "      datasetType: Delta\n" +
            "      {\n" +
            "        actionIndicator: None;\n" +
            "      }\n" +
            "      deduplication: None;\n" +
            "    }\n" +
            "    ->\n" +
            "    {\n" +
            "    },\n" +
            "    #/test::example::MyType/prop#\n" +
            "    {\n" +
            "      keys:\n" +
            "      [\n" +
            "        #/test::example::MyType/prop/foo#, #/test::example::MyType/prop/bar#\n" +
            "      ]\n" +
            "      datasetType: Snapshot\n" +
            "      {\n" +
            "        partitioning: None\n" +
            "        {\n" +
            "          emptyDatasetHandling: NoOp;\n" +
            "        }\n" +
            "      }\n" +
            "      deduplication: None;\n" +
            "    }\n" +
            "    ->\n" +
            "    {\n" +
            "    }\n" +
            "  ];\n" +
            "  tests:\n" +
            "  [\n" +
            "    test1:\n" +
            "    {\n" +
            "      testBatches:\n" +
            "      [\n" +
            "        testBatch1:\n" +
            "        {\n" +
            "          data:\n" +
            "          {\n" +
            "            connection:\n" +
            "            {\n" +
            "              ExternalFormat\n" +
            "              #{\n" +
            "                contentType: 'application/x.flatdata';\n" +
            "                data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
            "              }#\n" +
            "            }\n" +
            "          }\n" +
            "          asserts:\n" +
            "          [\n" +
            "            assert1:\n" +
            "              EqualToJson\n" +
            "              #{\n" +
            "                expected:\n" +
            "                  ExternalFormat\n" +
            "                  #{\n" +
            "                    contentType: 'application/json';\n" +
            "                    data: '{\"Age\":12, \"Name\":\"dummy\"}';\n" +
            "                  }#;\n" +
            "              }#\n" +
            "          ]\n" +
            "        }\n" +
            "      ]\n" +
            "      isTestDataFromServiceOutput: false;\n" +
            "      graphFetchPath: #/test::example::MyType/hello#;\n" +
            "    }\n" +
            "  ]\n" +
            "}\n");
    }

    @Test
    public void persistenceSnapshot()
    {
        test("###Persistence\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  serviceOutputTargets:\n" +
                "  [\n" +
                "    TDS\n" +
                "    {\n" +
                "      keys:\n" +
                "      [\n" +
                "        foo, bar\n" +
                "      ]\n" +
                "      datasetType: Snapshot\n" +
                "      {\n" +
                "        partitioning: FieldBased\n" +
                "        {\n" +
                "          partitionFields:\n" +
                "          [\n" +
                "            foo, bar\n" +
                "          ];\n" +
                "        }\n" +
                "      }\n" +
                "      deduplication: MaxVersion\n" +
                "      {\n" +
                "        versionField: version;\n" +
                "      }\n" +
                "    }\n" +
                "    ->\n" +
                "    {\n" +
                "    },\n" +
                "    #/test::example::MyType/prop#\n" +
                "    {\n" +
                "      keys:\n" +
                "      [\n" +
                "        #/test::example::MyType/prop/foo#, #/test::example::MyType/prop/bar#\n" +
                "      ]\n" +
                "      datasetType: Snapshot\n" +
                "      {\n" +
                "        partitioning: FieldBased\n" +
                "        {\n" +
                "          partitionFields:\n" +
                "          [\n" +
                "            #/test::example::MyType/prop/foo#, #/test::example::MyType/prop/bar#\n" +
                "          ];\n" +
                "        }\n" +
                "      }\n" +
                "      deduplication: MaxVersion\n" +
                "      {\n" +
                "        versionField: #/test::example::MyType/prop/version#;\n" +
                "      }\n" +
                "    }\n" +
                "    ->\n" +
                "    {\n" +
                "    }\n" +
                "  ];\n" +
                "}\n");
    }

    @Test
    public void persistenceDelta()
    {
        test("###Persistence\n" +
            "Persistence test::TestPersistence\n" +
            "{\n" +
            "  doc: 'This is test documentation.';\n" +
            "  trigger: Manual;\n" +
            "  service: test::Service;\n" +
            "  serviceOutputTargets:\n" +
            "  [\n" +
            "    TDS\n" +
            "    {\n" +
            "      keys:\n" +
            "      [\n" +
            "        foo, bar\n" +
            "      ]\n" +
            "      datasetType: Delta\n" +
            "      {\n" +
            "        actionIndicator: DeleteIndicator\n" +
            "        {\n" +
            "          deleteField: toBeDeleted;\n" +
            "          deleteValues: ['Yes', 'true'];\n" +
            "        }\n" +
            "      }\n" +
            "      deduplication: MaxVersion\n" +
            "      {\n" +
            "        versionField: version;\n" +
            "      }\n" +
            "    }\n" +
            "    ->\n" +
            "    {\n" +
            "    },\n" +
            "    #/test::example::MyType/prop#\n" +
            "    {\n" +
            "      keys:\n" +
            "      [\n" +
            "        #/test::example::MyType/prop/foo#, #/test::example::MyType/prop/bar#\n" +
            "      ]\n" +
            "      datasetType: Delta\n" +
            "      {\n" +
            "        actionIndicator: DeleteIndicator\n" +
            "        {\n" +
            "          deleteField: #/test::example::MyType/prop/toBeDeleted#;\n" +
            "          deleteValues: ['Yes', 'true'];\n" +
            "        }\n" +
            "      }\n" +
            "      deduplication: MaxVersion\n" +
            "      {\n" +
            "        versionField: #/test::example::MyType/prop/version#;\n" +
            "      }\n" +
            "    }\n" +
            "    ->\n" +
            "    {\n" +
            "    }\n" +
            "  ];\n" +
            "}\n");
    }
}
