package org.finos.legend.engine.language.pure.dsl.persistence.grammar.test;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestPersistenceGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
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
                "    ingestMode: BitemporalSnapshot\n" +
                "    {\n" +
                "      transactionMilestoning: BatchId\n" +
                "      {\n" +
                "        batchIdInName: 'batchIdIn';\n" +
                "        batchIdOutName: 'batchIdOut';\n" +
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
                "    ]\n" +
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
                "    binding: test::Binding;\n" +
                "    connection:\n" +
                "    #{\n" +
                "      JsonModelConnection\n" +
                "      {\n" +
                "        class: org::dxl::Animal;\n" +
                "        url: 'my_url2';\n" +
                "      }\n" +
                "    }#\n" +
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
                "          deduplicationStrategy: None;\n" +
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
                "    ]\n" +
                "  }\n" +
                "}\n");
    }
}
