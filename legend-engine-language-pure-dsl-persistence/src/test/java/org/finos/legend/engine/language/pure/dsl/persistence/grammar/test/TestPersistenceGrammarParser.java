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
                "  trigger: ScheduleTriggered;\n" +
                "  input: Service\n" +
                "  {\n" +
                "    service: test::service::Service;\n" +
                "  }\n" +
                "  persistence: Batch\n" +
                "  {\n" +
                "    target: GroupedFlat\n" +
                "    {\n" +
                "      targetName: 'WrapperTarget';\n" +
                "      modelClass: test::WrapperClass;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      components:\n" +
                "      [\n" +
                "        {\n" +
                "          property: test::WrapperClass->property1;\n" +
                "          targetSpecification:\n" +
                "          {\n" +
                "            targetName: 'TestDataset1';\n" +
                "            modelClass: 'test::InnerClass1';\n" +
                "            partitionProperties: [test::InnerClass1->property2, test::InnerClass1->property3];\n" +
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
                "                batchIdInProperty: 'inZ';\n" +
                "                batchIdOutProperty: 'outZ';\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
//                "        {\n" +
//                "            batchMode: BitemporalDelta\n" +
//                "            {\n" +
//                "              mergeStrategy: DeleteIndicator\n" +
//                "              {\n" +
//                "                deleteProperty: 'deleted';\n" +
//                "                deleteValues: ['Y', '1', 'true'];\n" +
//                "              }\n" +
//                "              transactionMilestoning: BatchIdOnly;\n" +
//                "              validityMilestoning: DateTime;\n" +
//                "              validityDerivation: SourceProvidesFromAndThruDateTime;\n" +
//                "            }\n" +
//                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
    }
}
