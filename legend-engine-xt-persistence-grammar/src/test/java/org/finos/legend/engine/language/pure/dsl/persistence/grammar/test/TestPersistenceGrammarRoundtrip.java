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

import com.fasterxml.jackson.core.JsonProcessingException;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestPersistenceGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void persistencePermitOptionalFieldsToBeEmptyFlat()
    {
        test("###Persistence\n" +
                "import test::*;\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'test doc';\n" +
                "  trigger: Manual;\n" +
                "  service: test::service::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: Relational\n" +
                "    {\n" +
                "      database: test::Database;\n" +
                "    }\n" +
                "    ingestMode: BitemporalSnapshot\n" +
                "    {\n" +
                "      transactionMilestoning: BatchIdAndDateTime\n" +
                "      {\n" +
                "        batchIdInName: 'batchIdIn';\n" +
                "        batchIdOutName: 'batchIdOut';\n" +
                "        dateTimeInName: 'inZ';\n" +
                "        dateTimeOutName: 'outZ';\n" +
                "      }\n" +
                "      validityMilestoning: DateTime\n" +
                "      {\n" +
                "        dateTimeFromName: 'FROM_Z';\n" +
                "        dateTimeThruName: 'THRU_Z';\n" +
                "        derivation: SourceSpecifiesFromDateTime\n" +
                "        {\n" +
                "          sourceDateTimeFromField: sourceFrom;\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "    targetShape: Flat\n" +
                "    {\n" +
                "      modelClass: test::ModelClass;\n" +
                "      targetName: 'TestDataset1';\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    public void persistencePermitOptionalFieldsToBeEmptyMultiFlat()
    {
        test("###Persistence\n" +
                "import test::*;\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'test doc';\n" +
                "  trigger: Manual;\n" +
                "  service: test::service::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: Relational\n" +
                "    {\n" +
                "      database: test::Database;\n" +
                "    }\n" +
                "    ingestMode: BitemporalDelta\n" +
                "    {\n" +
                "      mergeStrategy: DeleteIndicator\n" +
                "      {\n" +
                "        deleteField: deleted;\n" +
                "        deleteValues: ['Y', '1', 'true'];\n" +
                "      }\n" +
                "      transactionMilestoning: DateTime\n" +
                "      {\n" +
                "        dateTimeInName: 'inZ';\n" +
                "        dateTimeOutName: 'outZ';\n" +
                "      }\n" +
                "      validityMilestoning: DateTime\n" +
                "      {\n" +
                "        dateTimeFromName: 'FROM_Z';\n" +
                "        dateTimeThruName: 'THRU_Z';\n" +
                "        derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "        {\n" +
                "          sourceDateTimeFromField: sourceFrom;\n" +
                "          sourceDateTimeThruField: sourceThru;\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "    targetShape: MultiFlat\n" +
                "    {\n" +
                "      modelClass: test::WrapperClass;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      parts:\n" +
                "      [\n" +
                "        {\n" +
                "          modelProperty: property1;\n" +
                "          targetName: 'TestDataset1';\n" +
                "        },\n" +
                "        {\n" +
                "          modelProperty: property3;\n" +
                "          targetName: 'TestDataset2';\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "  tests:\n" +
                "  [\n" +
                "    test1:\n" +
                "    {\n" +
                "      testBatches:\n" +
                "      [\n" +
                "\n" +
                "      ]\n" +
                "      isTestDataFromServiceOutput: false;\n" +
                "    }\n" +
                "  ]\n" +
                "}\n");
    }

    @Test
    public void persistenceFlat()
    {
        test("###Persistence\n" +
                "import test::*;\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'test doc';\n" +
                "  trigger: Manual;\n" +
                "  service: test::service::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: Relational\n" +
                "    {\n" +
                "      database: test::Database;\n" +
                "    }\n" +
                "    ingestMode: BitemporalSnapshot\n" +
                "    {\n" +
                "      transactionMilestoning: BatchIdAndDateTime\n" +
                "      {\n" +
                "        batchIdInName: 'batchIdIn';\n" +
                "        batchIdOutName: 'batchIdOut';\n" +
                "        dateTimeInName: 'inZ';\n" +
                "        dateTimeOutName: 'outZ';\n" +
                "        derivation: SourceSpecifiesInDateTime\n" +
                "        {\n" +
                "          sourceDateTimeInField: sourceIn;\n" +
                "        }\n" +
                "      }\n" +
                "      validityMilestoning: DateTime\n" +
                "      {\n" +
                "        dateTimeFromName: 'FROM_Z';\n" +
                "        dateTimeThruName: 'THRU_Z';\n" +
                "        derivation: SourceSpecifiesFromDateTime\n" +
                "        {\n" +
                "          sourceDateTimeFromField: sourceFrom;\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "    targetShape: Flat\n" +
                "    {\n" +
                "      modelClass: test::ModelClass;\n" +
                "      targetName: 'TestDataset1';\n" +
                "      partitionFields: [propertyA, propertyB];\n" +
                "      deduplicationStrategy: MaxVersion\n" +
                "      {\n" +
                "        versionField: version;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "  notifier:\n" +
                "  {\n" +
                "    notifyees:\n" +
                "    [\n" +
                "      Email\n" +
                "      {\n" +
                "        address: 'x.y@z.com';\n" +
                "      },\n" +
                "      PagerDuty\n" +
                "      {\n" +
                "        url: 'https://x.com';\n" +
                "      }\n" +
                "    ];\n" +
                "  }\n" +
                "  tests:\n" +
                "  [\n" +
                "    test1:\n" +
                "    {\n" +
                "      testBatches:\n" +
                "      [\n" +
                "\n" +
                "      ]\n" +
                "      isTestDataFromServiceOutput: false;\n" +
                "    }\n" +
                "  ]\n" +
                "}\n");

        test("###Persistence\n" +
                "import test::*;\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'test doc';\n" +
                "  trigger: Manual;\n" +
                "  service: test::service::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: Relational\n" +
                "    {\n" +
                "      database: test::Database;\n" +
                "    }\n" +
                "    ingestMode: AppendOnly\n" +
                "    {\n" +
                "      auditing: None;\n" +
                "      filterDuplicates: true;\n" +
                "    }\n" +
                "    targetShape: Flat\n" +
                "    {\n" +
                "      modelClass: test::ModelClass;\n" +
                "      targetName: 'TestDataset1';\n" +
                "      partitionFields: [propertyA, propertyB];\n" +
                "      deduplicationStrategy: MaxVersion\n" +
                "      {\n" +
                "        versionField: version;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "  notifier:\n" +
                "  {\n" +
                "    notifyees:\n" +
                "    [\n" +
                "      Email\n" +
                "      {\n" +
                "        address: 'x.y@z.com';\n" +
                "      },\n" +
                "      PagerDuty\n" +
                "      {\n" +
                "        url: 'https://x.com';\n" +
                "      }\n" +
                "    ];\n" +
                "  }\n" +
                "}\n");
    }

    @Test
    public void persistenceFlatWithTest()
    {
        String persistenceCodeBlock = "###Persistence\n" +
                "import test::*;\n" +
                "PersistenceContext test::TestPersistenceContext\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  serviceParameters:\n" +
                "  [\n" +
                "    foo='hello',\n" +
                "    bar=1,\n" +
                "    con1=test::TestConnection,\n" +
                "    con2=\n" +
                "    #{\n" +
                "      RelationalDatabaseConnection\n" +
                "      {\n" +
                "        store: test::TestDatabase;\n" +
                "        type: H2;\n" +
                "        specification: LocalH2\n" +
                "        {\n" +
                "        };\n" +
                "        auth: Test;\n" +
                "      }\n" +
                "    }#\n" +
                "  ];\n" +
                "  sinkConnection:\n" +
                "  #{\n" +
                "    RelationalDatabaseConnection\n" +
                "    {\n" +
                "      store: test::TestDatabase;\n" +
                "      type: H2;\n" +
                "      specification: LocalH2\n" +
                "      {\n" +
                "      };\n" +
                "      auth: Test;\n" +
                "    }\n" +
                "  }#;\n" +
                "}\n" +
                "\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'test doc';\n" +
                "  trigger: Manual;\n" +
                "  service: test::service::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: Relational\n" +
                "    {\n" +
                "      database: test::Database;\n" +
                "    }\n" +
                "    ingestMode: BitemporalDelta\n" +
                "    {\n" +
                "      mergeStrategy: DeleteIndicator\n" +
                "      {\n" +
                "        deleteField: deleted;\n" +
                "        deleteValues: ['Y', '1', 'true'];\n" +
                "      }\n" +
                "      transactionMilestoning: DateTime\n" +
                "      {\n" +
                "        dateTimeInName: 'inZ';\n" +
                "        dateTimeOutName: 'outZ';\n" +
                "        derivation: SourceSpecifiesInAndOutDateTime\n" +
                "        {\n" +
                "          sourceDateTimeInField: sourceIn;\n" +
                "          sourceDateTimeOutField: sourceOut;\n" +
                "        }\n" +
                "      }\n" +
                "      validityMilestoning: DateTime\n" +
                "      {\n" +
                "        dateTimeFromName: 'FROM_Z';\n" +
                "        dateTimeThruName: 'THRU_Z';\n" +
                "        derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "        {\n" +
                "          sourceDateTimeFromField: sourceFrom;\n" +
                "          sourceDateTimeThruField: sourceThru;\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "    targetShape: MultiFlat\n" +
                "    {\n" +
                "      modelClass: meta::test::TestClass2;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      parts:\n" +
                "      [\n" +
                "        {\n" +
                "          modelProperty: property1;\n" +
                "          targetName: 'TestDataset1';\n" +
                "          partitionFields: [propertyA, propertyB];\n" +
                "          deduplicationStrategy: MaxVersion\n" +
                "          {\n" +
                "            versionField: updateDateTime;\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          modelProperty: property2;\n" +
                "          targetName: 'TestDataset1';\n" +
                "          partitionFields: [propertyA, propertyB];\n" +
                "          deduplicationStrategy: DuplicateCount\n" +
                "          {\n" +
                "            duplicateCountName: 'duplicateCount';\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          modelProperty: property3;\n" +
                "          targetName: 'TestDataset2';\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "  notifier:\n" +
                "  {\n" +
                "    notifyees:\n" +
                "    [\n" +
                "      Email\n" +
                "      {\n" +
                "        address: 'x.y@z.com';\n" +
                "      },\n" +
                "      PagerDuty\n" +
                "      {\n" +
                "        url: 'https://x.com';\n" +
                "      }\n" +
                "    ];\n" +
                "  }\n" +
                "%s" +
                "}\n" +
                "%s";

        // Test Pure MockUp
        String testMockUp = "\n\n" +
                "###Pure\n" +
                "Class meta::test::TestClass1\n" +
                "{\n" +
                "  name: String[1];\n" +
                "  admin: meta::test::Person[1];\n" +
                "  owner: meta::test::Person[1];\n" +
                "  version: Integer[1];\n" +
                "}\n" +
                "\n" +
                "Class meta::test::Person\n" +
                "{\n" +
                "  name: String[1];\n" +
                "  propertyA: String[1];\n" +
                "  propertyB: String[1];\n" +
                "  sourceIn: DateTime[1];\n" +
                "  sourceOut: DateTime[1];\n" +
                "  sourceFrom: DateTime[1];\n" +
                "  sourceThru: DateTime[1];\n" +
                "  timeIn: DateTime[1];\n" +
                "  timeOut: DateTime[1];\n" +
                "  updateDateTime: DateTime[1];\n" +
                "  effectiveFrom: DateTime[1];\n" +
                "  effectiveThru: DateTime[1];\n" +
                "  version: Integer[1];\n" +
                "  deleted: Boolean[1];\n" +
                "}\n" +
                "\n" +
                "Class meta::test::TestClass2\n" +
                "{\n" +
                "  name: String[1];\n" +
                "  property1: meta::test::Person[1];\n" +
                "  property2: meta::test::Person[1];\n" +
                "  property3: meta::test::Person[1];\n" +
                "}\n" +
                "\n\n" +
                "###Relational\n" +
                "Database test::Database\n" +
                "(\n" +
                "  Table personTable\n" +
                "  (\n" +
                "    ID INTEGER PRIMARY KEY,\n" +
                "    NAME VARCHAR(100)\n" +
                "  )\n" +
                ")\n" +
                "\n\n" +
                "###Service\n" +
                "Service test::service::Service\n" +
                "{\n" +
                "  pattern: 'test';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'test'\n" +
                "  ];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: src: meta::test::TestClass2[1]|$src.name;\n" +
                "    mapping: meta::myMapping;\n" +
                "    runtime: meta::myRuntime;\n" +
                "  }\n" +
                "}\n" +
                "\n\n" +
                "###Connection" +
                "\n" +
                "RelationalDatabaseConnection test::TestConnection\n" +
                "{\n" +
                "  store: test::TestDatabase;\n" +
                "  type: Snowflake;\n" +
                "  specification: LocalH2\n" +
                "  {\n" +
                "  };\n" +
                "  auth: Test;\n" +
                "}\n" +
                "\n\n" +
                "###Mapping\n" +
                "Mapping meta::myMapping\n" +
                "(\n" +
                ")\n" +
                "\n\n" +
                "###Runtime\n" +
                "Runtime meta::myRuntime\n" +
                "{\n" +
                "  mappings:\n" +
                "  [\n" +
                "    meta::myMapping\n" +
                "  ];\n" +
                "}\n";

        String persistenceTestCodeBlock = "  tests:\n" +
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
                "                ExternalFormat\n" +
                "                #{\n" +
                "                  contentType: 'application/x.flatdata';\n" +
                "                  data: 'FIRST_NAME,LAST_NAME\\nFred,Bloggs\\nJane,Doe';\n" +
                "                }#\n" +
                "            }\n" +
                "          }\n" +
                "          assert:\n" +
                "          {\n" +
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
                "          }\n" +
                "        }\n" +
                "      ]\n" +
                "      isTestDataFromServiceOutput: true;\n" +
                "    }\n" +
                "  ]\n";

        String persistenceCodeWithTest = String.format(persistenceCodeBlock, persistenceTestCodeBlock, testMockUp);
        test(persistenceCodeWithTest);
    }


    @Test
    public void persistenceMultiFlat()
    {
        test("###Persistence\n" +
                "import test::*;\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'test doc';\n" +
                "  trigger: Manual;\n" +
                "  service: test::service::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: ObjectStorage\n" +
                "    {\n" +
                "      binding: test::Binding;\n" +
                "    }\n" +
                "    ingestMode: BitemporalDelta\n" +
                "    {\n" +
                "      mergeStrategy: DeleteIndicator\n" +
                "      {\n" +
                "        deleteField: deleted;\n" +
                "        deleteValues: ['Y', '1', 'true'];\n" +
                "      }\n" +
                "      transactionMilestoning: DateTime\n" +
                "      {\n" +
                "        dateTimeInName: 'inZ';\n" +
                "        dateTimeOutName: 'outZ';\n" +
                "        derivation: SourceSpecifiesInAndOutDateTime\n" +
                "        {\n" +
                "          sourceDateTimeInField: sourceIn;\n" +
                "          sourceDateTimeOutField: sourceOut;\n" +
                "        }\n" +
                "      }\n" +
                "      validityMilestoning: DateTime\n" +
                "      {\n" +
                "        dateTimeFromName: 'FROM_Z';\n" +
                "        dateTimeThruName: 'THRU_Z';\n" +
                "        derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "        {\n" +
                "          sourceDateTimeFromField: sourceFrom;\n" +
                "          sourceDateTimeThruField: sourceThru;\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "    targetShape: MultiFlat\n" +
                "    {\n" +
                "      modelClass: test::WrapperClass;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      parts:\n" +
                "      [\n" +
                "        {\n" +
                "          modelProperty: property1;\n" +
                "          targetName: 'TestDataset1';\n" +
                "          partitionFields: [propertyA, propertyB];\n" +
                "          deduplicationStrategy: AnyVersion;\n" +
                "        },\n" +
                "        {\n" +
                "          modelProperty: property2;\n" +
                "          targetName: 'TestDataset1';\n" +
                "          partitionFields: [propertyA, propertyB];\n" +
                "          deduplicationStrategy: DuplicateCount\n" +
                "          {\n" +
                "            duplicateCountName: 'duplicateCount';\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          modelProperty: property3;\n" +
                "          targetName: 'TestDataset2';\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "  notifier:\n" +
                "  {\n" +
                "    notifyees:\n" +
                "    [\n" +
                "      Email\n" +
                "      {\n" +
                "        address: 'x.y@z.com';\n" +
                "      },\n" +
                "      PagerDuty\n" +
                "      {\n" +
                "        url: 'https://x.com';\n" +
                "      }\n" +
                "    ];\n" +
                "  }\n" +
                "}\n");
    }
}
