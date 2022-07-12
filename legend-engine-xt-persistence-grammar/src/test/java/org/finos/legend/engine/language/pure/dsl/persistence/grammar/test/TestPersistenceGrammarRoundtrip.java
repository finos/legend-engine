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

public class TestPersistenceGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void persistenceContextPermitOptionalFieldsToBeEmpty()
    {
        test("###Persistence\n" +
                "import test::*;\n" +
                "PersistenceContext test::TestPersistenceContext\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "}\n");
    }

    @Test
    public void persistenceContextPlatformDefault()
    {
        testFormat(
                "###Persistence\n" +
                        "import test::*;\n" +
                        "PersistenceContext test::TestPersistenceContext\n" +
                        "{\n" +
                        "  persistence: test::TestPersistence;\n" +
                        "}\n",
                "###Persistence\n" +
                        "import test::*;\n" +
                        "PersistenceContext test::TestPersistenceContext\n" +
                        "{\n" +
                        "  persistence: test::TestPersistence;\n" +
                        "  platform: Default;\n" +
                        "}\n");
    }

    @Test
    public void persistenceContextSingleServiceParameter()
    {
        test("###Persistence\n" +
                "import test::*;\n" +
                "PersistenceContext test::TestPersistenceContext\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  serviceParameters:\n" +
                "  [\n" +
                "    foo='hello'\n" +
                "  ];\n" +
                "  sinkConnection: test::TestConnection;\n" +
                "}\n");
    }

    @Test
    public void persistenceContextSinkConnectionPointer()
    {
        test("###Persistence\n" +
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
                "  sinkConnection: test::TestConnection;\n" +
                "}\n");
    }

    @Test
    public void persistenceContextSinkConnectionEmbedded()
    {
        test("###Persistence\n" +
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
                "}\n");
        test("###Persistence\n" +
                "import test::*;\n" +
                "PersistenceContext test::TestPersistenceContext\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  serviceParameters:\n" +
                "  [\n" +
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
                "    }#,\n" +
                "    foo='hello',\n" +
                "    bar=1,\n" +
                "    con1=test::TestConnection\n" +
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
                "}\n");
    }

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
                "}\n");
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
