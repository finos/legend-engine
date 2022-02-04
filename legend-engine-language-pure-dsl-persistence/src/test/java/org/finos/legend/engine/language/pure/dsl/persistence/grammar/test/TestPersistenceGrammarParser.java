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
                "import test::input::*;\n" +
                "\n" +
                "ServicePersistence " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  doc: 'test doc';\n" +
                "  owners: ['test1', 'test2'];\n" +
                "  trigger: ScheduleTriggered;\n" +
                "  service: test::service::Service;\n" +
                "  persistence: Batch\n" +
                "  {\n" +
                "    inputShape: GROUPED_FLAT;\n" +
                "    inputClass: test::InputClass;\n" +
                "    transactionMode: ALL_DATASETS;\n" +
                "    target: Datastore\n" +
                "    {\n" +
                "      datastoreName: 'TestDatastore';\n" +
                "      datasets:\n" +
                "      [\n" +
                "        {\n" +
                "          datasetName: 'TestDataset1';\n" +
                "          partitionProperties: [test::InputClass->property1, test::InputClass->property2];\n" +
                "          deduplicationStrategy: MaxVersionDedup\n" +
                "          {\n" +
                "            versionProperty: 'updateDateTime';\n" +
                "          }\n" +
//                "          batchMode: AppendOnly\n" +
//                "          {\n" +
//                "            auditScheme: BatchDateTime\n" +
//                "            {\n" +
//                "              transactionDateTimePropertyName: 'insertDateTime';\n" +
//                "            }\n" +
//                "            filterDuplicates: false;\n" +
//                "          }\n" +
//                "          batchMode: UnitemporalDelta\n" +
//                "          {\n" +
//                "            mergeScheme: NoDeletes;\n" +
//                "            transactionMilestoning: BatchIdOnly;\n" +
//                "          }\n" +
                "          batchMode: BitemporalDelta\n" +
                "          {\n" +
                "            mergeScheme: DeleteIndicator\n" +
                "            {\n" +
                "              deleteProperty: 'deleted';\n" +
                "              deleteValues: ['Y', '1', 'true'];\n" +
                "            }\n" +
                "            transactionMilestoning: BatchIdOnly;\n" +
                "            validityMilestoning: DateTime;\n" +
                "            validityDerivation: SourceProvidesFromAndThruDateTime;\n" +
                "          }\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
    }
}
