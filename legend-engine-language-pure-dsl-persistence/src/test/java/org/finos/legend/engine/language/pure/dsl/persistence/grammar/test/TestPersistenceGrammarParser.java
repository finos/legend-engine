package org.finos.legend.engine.language.pure.dsl.persistence.grammar.test;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

import java.util.List;

public abstract class TestPersistenceGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    protected abstract String targetFlat();
    protected abstract String targetMulti();
    protected abstract String targetOpaque();
    protected abstract String ingestMode();
    protected abstract String flatTarget();
    protected abstract String parts();

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
                "Persistence " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
    }

    /**********
     * persistence
     **********/

    @Test
    public void persistenceDoc()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [3:1-23:1]: Field 'doc' is required");
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
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
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [3:1-25:1]: Field 'doc' should be specified only once");
    }

    /**********
     * trigger
     **********/

    @Test
    public void trigger()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [3:1-23:1]: Field 'trigger' is required");
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
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
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [3:1-25:1]: Field 'trigger' should be specified only once");
    }

    /**********
     * reader
     **********/

    @Test
    public void reader()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [3:1-20:1]: Field 'reader' is required");
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
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
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [3:1-28:1]: Field 'reader' should be specified only once");
    }

    @Test
    public void readerService()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [7:11-9:3]: Field 'service' is required");
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [7:11-11:3]: Field 'service' should be specified only once");
    }

    /**********
     * persister
     **********/

    @Test
    public void persister()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
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
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [3:1-37:1]: Field 'persister' should be specified only once");
    }

    @Test
    public void persisterStreaming()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Streaming\n" +
                "  {\n" +
                "  }\n" +
                "}\n", "PARSER error at [11:14-13:3]: Field 'target' is required");
    }

    @Test
    public void persisterBatch()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "  }\n" +
                "}\n", "PARSER error at [11:14-13:3]: Field 'target' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [11:14-33:3]: Field 'target' should be specified only once");
    }

    /**********
     * target specification - grouped flat
     **********/

    @Test
    public void batchGroupedFlatModelClass()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetMulti() + "\n" +
                "    {\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      " + parts() + ":\n" +
                "      [\n" +
                "        {\n" +
                "          property: 'Foo';\n" +
                "          " + flatTarget() + ":\n" +
                "          {\n" +
                "            targetName: 'TestDataset1';\n" +
                "            " + ingestMode() + ": AppendOnly\n" +
                "            {\n" +
                "              auditing: None;\n" +
                "              filterDuplicates: false;\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:13-31:5]: Field 'modelClass' is required");
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetMulti() + "\n" +
                "    {\n" +
                "      modelClass: test::ModelClass;\n" +
                "      modelClass: test::ModelClass;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      " + parts() + ":\n" +
                "      [\n" +
                "        {\n" +
                "          property: 'Foo';\n" +
                "          " + flatTarget() + ":\n" +
                "          {\n" +
                "            targetName: 'TestDataset1';\n" +
                "            " + ingestMode() + ": AppendOnly\n" +
                "            {\n" +
                "              auditing: None;\n" +
                "              filterDuplicates: false;\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:13-33:5]: Field 'modelClass' should be specified only once");
    }

    @Test
    public void batchGroupedFlatTransactionScope()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetMulti() + "\n" +
                "    {\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + parts() + ":\n" +
                "      [\n" +
                "        {\n" +
                "          property: 'Foo';\n" +
                "          " + flatTarget() + ":\n" +
                "          {\n" +
                "            targetName: 'TestDataset1';\n" +
                "            " + ingestMode() + ": AppendOnly\n" +
                "            {\n" +
                "              auditing: None;\n" +
                "              filterDuplicates: false;\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:13-31:5]: Field 'transactionScope' is required");
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetMulti() + "\n" +
                "    {\n" +
                "      modelClass: test::ModelClass;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      " + parts() + ":\n" +
                "      [\n" +
                "        {\n" +
                "          property: 'Foo';\n" +
                "          " + flatTarget() + ":\n" +
                "          {\n" +
                "            targetName: 'TestDataset1';\n" +
                "            " + ingestMode() + ": AppendOnly\n" +
                "            {\n" +
                "              auditing: None;\n" +
                "              filterDuplicates: false;\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:13-33:5]: Field 'transactionScope' should be specified only once");
    }

    @Test
    public void batchGroupedFlatComponents()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetMulti() + "\n" +
                "    {\n" +
                "      modelClass: test::ModelClass;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:13-17:5]: Field '" + parts() + "' is required");
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetMulti() + "\n" +
                "    {\n" +
                "      modelClass: test::ModelClass;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      " + parts() + ":\n" +
                "      [\n" +
                "        {\n" +
                "          property: 'Foo';\n" +
                "          " + flatTarget() + ":\n" +
                "          {\n" +
                "            targetName: 'TestDataset1';\n" +
                "            " + ingestMode() + ": AppendOnly\n" +
                "            {\n" +
                "              auditing: None;\n" +
                "              filterDuplicates: false;\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      ];\n" +
                "      " + parts() + ":\n" +
                "      [\n" +
                "        {\n" +
                "          property: 'Foo';\n" +
                "          " + flatTarget() + ":\n" +
                "          {\n" +
                "            targetName: 'TestDataset1';\n" +
                "            " + ingestMode() + ": AppendOnly\n" +
                "            {\n" +
                "              auditing: None;\n" +
                "              filterDuplicates: false;\n" +
                "            }\n" +
                "          }\n" +
                "        }\n" +
                "      ];\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:13-47:5]: Field '" + parts() + "' should be specified only once");
    }

    /**********
     * target specification - flat
     **********/

    @Test
    public void batchFlatTargetName()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:13-21:5]: Field 'targetName' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:13-23:5]: Field 'targetName' should be specified only once");
    }

    @Test
    public void batchFlatModelClass()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:13-21:5]: Field 'modelClass' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:13-23:5]: Field 'modelClass' should be specified only once");
    }

    @Test
    public void batchFlatDeduplicationStrategy()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      deduplicationStrategy: None;\n" +
                "      deduplicationStrategy: None;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:13-24:5]: Field 'deduplicationStrategy' should be specified only once");
    }

    @Test
    public void batchFlatPartitionProperties()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      partitionProperties: [];\n" +
                "      partitionProperties: [];\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:13-24:5]: Field 'partitionProperties' should be specified only once");
    }

    @Test
    public void batchFlatBatchMode()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:13-17:5]: Field '" + ingestMode() + "' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:13-27:5]: Field '" + ingestMode() + "' should be specified only once");
    }

    /**********
     * batch mode - snapshot
     **********/

    @Test
    public void batchModeNontemporalSnapshotAuditing()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": NontemporalSnapshot\n" +
                "      {\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-19:7]: Field 'auditing' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": NontemporalSnapshot\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        auditing: None;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-21:7]: Field 'auditing' should be specified only once");
    }

    @Test
    public void batchModeUnitemporalSnapshotTransactionMilestoning()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": UnitemporalSnapshot\n" +
                "      {\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-19:7]: Field 'transactionMilestoning' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": UnitemporalSnapshot\n" +
                "      {\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-21:7]: Field 'transactionMilestoning' should be specified only once");
    }

    @Test
    public void batchModeBitemporalSnapshotTransactionMilestoning()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalSnapshot\n" +
                "      {\n" +
                "        validityMilestoning: OpaqueValidityMilestoning;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-20:7]: Field 'transactionMilestoning' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalSnapshot\n" +
                "      {\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "        validityMilestoning: OpaqueValidityMilestoning;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-22:7]: Field 'transactionMilestoning' should be specified only once");
    }

    @Test
    public void batchModeBitemporalSnapshotValidityMilestoning()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalSnapshot\n" +
                "      {\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-20:7]: Field 'validityMilestoning' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalSnapshot\n" +
                "      {\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "        validityMilestoning: OpaqueValidityMilestoning;\n" +
                "        validityMilestoning: OpaqueValidityMilestoning;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-22:7]: Field 'validityMilestoning' should be specified only once");
    }

    @Test
    public void batchModeBitemporalSnapshotValidityDerivation()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalSnapshot\n" +
                "      {\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "        validityMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeFromFieldName: 'FROM_Z';\n" +
                "          dateTimeThruFieldName: 'THRU_Z';\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [20:30-24:9]: Field 'derivation' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalSnapshot\n" +
                "      {\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "        validityMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeFromFieldName: 'FROM_Z';\n" +
                "          dateTimeThruFieldName: 'THRU_Z';\n" +
                "          derivation: OpaqueValidityDerivation;\n" +
                "          derivation: OpaqueValidityDerivation;\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [20:30-26:9]: Field 'derivation' should be specified only once");
    }

    /**********
     * ingest mode - delta
     **********/

    @Test
    public void batchModeNontemporalDeltaMergeStrategy()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": NontemporalDelta\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-20:7]: Field 'mergeStrategy' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": NontemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: OpaqueMerge;" +
                "        mergeStrategy: OpaqueMerge;" +
                "        auditing: None;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-20:7]: Field 'mergeStrategy' should be specified only once");
    }

    @Test
    public void batchModeNontemporalDeltaAuditing()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": NontemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: OpaqueMerge;" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-19:42]: Field 'auditing' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": NontemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: OpaqueMerge;" +
                "        auditing: None;\n" +
                "        auditing: None;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-21:7]: Field 'auditing' should be specified only once");
    }

    @Test
    public void batchModeUnitemporalDeltaMergeStrategy()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": UnitemporalDelta\n" +
                "      {\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-20:7]: Field 'mergeStrategy' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": UnitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: OpaqueMerge;\n" +
                "        mergeStrategy: OpaqueMerge;\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-22:7]: Field 'mergeStrategy' should be specified only once");
    }

    @Test
    public void batchModeUnitemporalDeltaTransactionMilestoning()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": UnitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: OpaqueMerge;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-20:7]: Field 'transactionMilestoning' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": UnitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: OpaqueMerge;\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-22:7]: Field 'transactionMilestoning' should be specified only once");
    }

    @Test
    public void batchModeBitemporalDeltaTransactionMilestoning()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: OpaqueMerge;\n" +
                "        validityMilestoning: OpaqueValidityMilestoning;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-21:7]: Field 'transactionMilestoning' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: OpaqueMerge;\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "        validityMilestoning: OpaqueValidityMilestoning;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-23:7]: Field 'transactionMilestoning' should be specified only once");
    }

    @Test
    public void batchModeBitemporalDeltaMergeStrategy()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalDelta\n" +
                "      {\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "        validityMilestoning: OpaqueValidityMilestoning;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-21:7]: Field 'mergeStrategy' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: OpaqueMerge;\n" +
                "        mergeStrategy: OpaqueMerge;\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "        validityMilestoning: OpaqueValidityMilestoning;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-23:7]: Field 'mergeStrategy' should be specified only once");
    }

    @Test
    public void batchModeBitemporalDeltaValidityMilestoning()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: OpaqueMerge;\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-21:7]: Field 'validityMilestoning' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: OpaqueMerge;\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "        validityMilestoning: OpaqueValidityMilestoning;\n" +
                "        validityMilestoning: OpaqueValidityMilestoning;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-23:7]: Field 'validityMilestoning' should be specified only once");
    }

    @Test
    public void batchModeBitemporalDeltaValidityDerivation()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: OpaqueMerge;\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "        validityMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeFromFieldName: 'FROM_Z';\n" +
                "          dateTimeThruFieldName: 'THRU_Z';\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [21:30-25:9]: Field 'derivation' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: OpaqueMerge;\n" +
                "        transactionMilestoning: OpaqueTransactionMilestoning;\n" +
                "        validityMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeFromFieldName: 'FROM_Z';\n" +
                "          dateTimeThruFieldName: 'THRU_Z';\n" +
                "          derivation: OpaqueValidityDerivation;\n" +
                "          derivation: OpaqueValidityDerivation;\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [21:30-27:9]: Field 'derivation' should be specified only once");
    }

    /**********
     * batch mode - append only
     **********/

    @Test
    public void batchModeAppendOnlyAuditing()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-20:7]: Field 'auditing' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-22:7]: Field 'auditing' should be specified only once");
    }

    @Test
    public void batchModeAppendOnlyFilterDuplicates()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-20:7]: Field 'filterDuplicates' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": AppendOnly\n" +
                "      {\n" +
                "        auditing: None;\n" +
                "        filterDuplicates: false;\n" +
                "        filterDuplicates: false;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [17:19-22:7]: Field 'filterDuplicates' should be specified only once");
    }

    @Test
    public void success()
    {
        test("###Persistence\n" +
                "\n" +
                "import test::*;\n" +
                "\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'test doc';\n" +
                "  trigger: OpaqueTrigger;\n" +
                "  reader: Service\n" +
                "  {\n" +
                "    service: test::service::Service;\n" +
                "  }\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetMulti() + "\n" +
                "    {\n" +
                "      modelClass: test::WrapperClass;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "      " + parts() + ":\n" +
                "      [\n" +
                "        {\n" +
                "          property: property1;\n" +
                "          " + flatTarget() + ":\n" +
                "          {\n" +
                "            targetName: 'TestDataset1';\n" +
                "            partitionProperties: [propertyA, propertyB];\n" +
                "            deduplicationStrategy: MaxVersion\n" +
                "            {\n" +
                "              versionProperty: 'updateDateTime';\n" +
                "            }\n" +
                "            " + ingestMode() + ": NontemporalSnapshot\n" +
                "            {\n" +
                "              auditing: DateTime\n" +
                "              {\n" +
                "                dateTimeFieldName: 'updateDateTime';\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          property: property2;\n" +
                "          " + flatTarget() + ":\n" +
                "          {\n" +
                "            targetName: 'TestDataset1';\n" +
                "            partitionProperties: [propertyA, propertyB];\n" +
                "            deduplicationStrategy: MaxVersion\n" +
                "            {\n" +
                "              versionProperty: 'updateDateTime';\n" +
                "            }\n" +
                "            " + ingestMode() + ": UnitemporalDelta\n" +
                "            {\n" +
                "              mergeStrategy: NoDeletes;\n" +
                "              transactionMilestoning: BatchId\n" +
                "              {\n" +
                "                batchIdInFieldName: 'batchIdIn';\n" +
                "                batchIdOutFieldName: 'batchIdOut';\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
                "        },\n" +
                "        {\n" +
                "          property: property3;\n" +
                "          " + flatTarget() + ":\n" +
                "          {\n" +
                "            targetName: 'TestDataset2';\n" +
                "            deduplicationStrategy: OpaqueDeduplication;\n" +
                "            " + ingestMode() + ": BitemporalDelta\n" +
                "            {\n" +
                "              mergeStrategy: DeleteIndicator\n" +
                "              {\n" +
                "                deleteProperty: 'deleted';\n" +
                "                deleteValues: ['Y', '1', 'true'];\n" +
                "              }\n" +
                "              transactionMilestoning: DateTime\n" +
                "              {\n" +
                "                dateTimeInFieldName: 'inZ';\n" +
                "                dateTimeOutFieldName: 'outZ';\n" +
                "              }\n" +
                "              validityMilestoning: DateTime\n" +
                "              {\n" +
                "                dateTimeFromFieldName: 'FROM_Z';\n" +
                "                dateTimeThruFieldName: 'THRU_Z';\n" +
                "                derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "                {\n" +
                "                  sourceDateTimeFromProperty: fromZ;\n" +
                "                  sourceDateTimeThruProperty: thruZ;\n" +
                "                }\n" +
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
