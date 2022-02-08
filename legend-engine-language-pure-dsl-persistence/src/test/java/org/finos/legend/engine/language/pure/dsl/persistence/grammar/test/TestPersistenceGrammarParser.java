package org.finos.legend.engine.language.pure.dsl.persistence.grammar.test;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

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
                "PersistencePipe " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass1;\n" +
                "      batchMode: AppendOnly\n" +
                "      {\n" +
                "        auditing: NoAuditing;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
    }

    @Test
    public void pipelineDoc()
    {
        test("###Persistence\n" +
                "\n" +
                "PersistencePipe test::TestPipe \n" +
                "{\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      batchMode: AppendOnly\n" +
                "      {\n" +
                "        auditing: NoAuditing;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [3:1-23:1]: Field 'doc' is required");

        test("###Persistence\n" +
                "\n" +
                "PersistencePipe test::TestPipe \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      batchMode: AppendOnly\n" +
                "      {\n" +
                "        auditing: NoAuditing;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [3:1-25:1]: Field 'doc' should be specified only once");
    }

    @Test
    public void pipelineTrigger()
    {
        test("###Persistence\n" +
                "\n" +
                "PersistencePipe test::TestPipe \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      batchMode: AppendOnly\n" +
                "      {\n" +
                "        auditing: NoAuditing;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [3:1-23:1]: Field 'trigger' is required");

        test("###Persistence\n" +
                "\n" +
                "PersistencePipe test::TestPipe \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      batchMode: AppendOnly\n" +
                "      {\n" +
                "        auditing: NoAuditing;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [3:1-25:1]: Field 'trigger' should be specified only once");
    }

    @Test
    public void pipelineReader()
    {
        test("###Persistence\n" +
                "\n" +
                "PersistencePipe test::TestPipe \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      batchMode: AppendOnly\n" +
                "      {\n" +
                "        auditing: NoAuditing;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [3:1-20:1]: Field 'reader' is required");

        test("###Persistence\n" +
                "\n" +
                "PersistencePipe test::TestPipe \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      batchMode: AppendOnly\n" +
                "      {\n" +
                "        auditing: NoAuditing;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [3:1-28:1]: Field 'reader' should be specified only once");
    }

    @Test
    public void pipelinePersister()
    {
        test("###Persistence\n" +
                "\n" +
                "PersistencePipe test::TestPipe \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "}\n", "PARSER error at [3:1-11:1]: Field 'persister' is required");

        test("###Persistence\n" +
                "\n" +
                "PersistencePipe test::TestPipe \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      batchMode: AppendOnly\n" +
                "      {\n" +
                "        auditing: NoAuditing;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      batchMode: AppendOnly\n" +
                "      {\n" +
                "        auditing: NoAuditing;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [3:1-37:1]: Field 'persister' should be specified only once");
    }

    @Test
    public void success()
    {
        test("###Persistence\n" +
                "\n" +
                "import test::*;\n" +
                "\n" +
                "PersistencePipe test::TestPipe\n" +
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
