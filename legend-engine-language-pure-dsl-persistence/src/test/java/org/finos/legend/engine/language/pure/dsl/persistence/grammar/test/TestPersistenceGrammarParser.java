package org.finos.legend.engine.language.pure.dsl.persistence.grammar.test;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;

import java.util.List;

public class TestPersistenceGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return PersistenceParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Persistence\n" +
                "\n" +
                "import test::*;\n" +
                "\n" +
                "PersistencePipe " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  doc: 'test doc';\n" +
                "  owners: ['test1', 'test2'];\n" +
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
//                "            batchMode: AppendOnly\n" +
//                "            {\n" +
//                "              auditing: BatchDateTime\n" +
//                "              {\n" +
//                "                dateTimePropertyName: 'insertDateTime';\n" +
//                "              }\n" +
//                "              filterDuplicates: false;\n" +
//                "            }\n" +
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
                "}\n";
    }
}
