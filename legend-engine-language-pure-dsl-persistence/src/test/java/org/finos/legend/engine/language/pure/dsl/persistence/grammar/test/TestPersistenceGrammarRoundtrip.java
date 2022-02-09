package org.finos.legend.engine.language.pure.dsl.persistence.grammar.test;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Ignore;
import org.junit.Test;

public class TestPersistenceGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Ignore("Fix composer.")
    @Test
    public void testPipeline()
    {
        test("###Persistence\n" +
                "\n" +
                "import test::*;\n" +
                "\n" +
                "PersistencePipe test::TestPipe\n" +
                "{\n" +
                "  doc: 'test doc';\n" +
                "  owners: ['owner1', 'owner2'];\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::service::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: GroupedFlat\n" +
                "    {\n" +
                "      modelClass: test::WrapperClass;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      components:\n" +
                "      [\n" +
                "        {\n" +
                "          property: test::WrapperClass->property1;\n" +
                "          targetSpecification:\n" +
                "          {\n" +
                "            targetName: 'TestDataset1';\n" +
                "            modelClass: test::InnerClass1;\n" +
                "            partitionProperties: [test::InnerClass1->propertyA, test::InnerClass1->propertyB];\n" +
                "            deduplicationStrategy: MaxVersion\n" +
                "            {\n" +
                "              versionProperty: 'updateDateTime';\n" +
                "            }\n" +
                "            batchMode: UnitemporalDelta\n" +
                "            {\n" +
                "              mergeStrategy: NoDeletes;\n" +
                "              transactionMilestoning: BatchIdOnly\n" +
                "              {\n" +
                "                batchIdInProperty: 'batchIdIn';\n" +
                "                batchIdOutProperty: 'batchIdOut';\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          property: test::WrapperClass->property2;\n" +
                "          targetSpecification:\n" +
                "          {\n" +
                "            targetName: 'TestDataset2';\n" +
                "            modelClass: test::InnerClass2;\n" +
                "            partitionProperties: [];\n" +
                "            deduplicationStrategy: OpaqueDeduplication;\n" +
                "            batchMode: BitemporalDelta\n" +
                "            {\n" +
                "              mergeStrategy: DeleteIndicator\n" +
                "              {\n" +
                "                deleteProperty: 'deleted';\n" +
                "                deleteValues: ['Y', '1', 'true'];\n" +
                "              }\n" +
                "              transactionMilestoning: DateTimeOnly\n" +
                "              {\n" +
                "                dateTimeInProperty: 'inZ';\n" +
                "                dateTimeOutProperty: 'outZ';\n" +
                "              }\n" +
                "              validityMilestoning: DateTime\n" +
                "              {\n" +
                "                dateTimeFromProperty: 'fromZ';\n" +
                "                dateTimeThruProperty: 'thruZ';\n" +
                "              }\n" +
                "              validityDerivation: SourceProvidesFromAndThruDateTime\n" +
                "              {\n" +
                "                sourceDateTimeFromProperty: test::InnerClass2->businessDateFrom;\n" +
                "                sourceDateTimeThruProperty: test::InnerClass2->businessDateThru;\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "}\n");
    }
}
