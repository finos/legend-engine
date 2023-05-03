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
                "    ROOT\n" +
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
    public void persistenceOptionalFieldsDefaulted()
    {
        test("###Persistence\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  serviceOutputTargets:\n" +
                "  [\n" +
                "    ROOT\n" +
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
                "                expected : \n" +
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
                "    ROOT\n" +
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
                "    }\n" +
                "    ->\n" +
                "    {\n" +
                "    }\n" +
                "  ];\n" +
                "}\n");
    }
}
