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
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [3:1-20:1]: Field 'doc' is required");
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [3:1-22:1]: Field 'doc' should be specified only once");
    }

    @Test
    public void persistenceService()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
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
                "}\n", "PARSER error at [3:1-20:1]: Field 'service' is required");
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [3:1-22:1]: Field 'service' should be specified only once");
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
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [3:1-20:1]: Field 'trigger' is required");
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [3:1-22:1]: Field 'trigger' should be specified only once");
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
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "}\n", "PARSER error at [3:1-8:1]: Field 'persister' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [3:1-34:1]: Field 'persister' should be specified only once");
    }

    /**********
     * notifier
     **********/

    @Test
    public void notifierEmailAddress()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "  notifier:\n" +
                "  {\n" +
                "    notifyees:\n" +
                "    [\n" +
                "      Email\n" +
                "      {\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}\n", "PARSER error at [25:7-27:7]: Field 'address' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "  notifier:\n" +
                "  {\n" +
                "    notifyees:\n" +
                "    [\n" +
                "      Email\n" +
                "      {\n" +
                "        address: 'x.y@z.com';\n" +
                "        address: 'x.y@z.com';\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}\n", "PARSER error at [25:7-29:7]: Field 'address' should be specified only once");
    }

    @Test
    public void notifierPagerDutyUrl()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "  notifier:\n" +
                "  {\n" +
                "    notifyees:\n" +
                "    [\n" +
                "      PagerDuty\n" +
                "      {\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}\n", "PARSER error at [25:7-27:7]: Field 'url' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "  notifier:\n" +
                "  {\n" +
                "    notifyees:\n" +
                "    [\n" +
                "      PagerDuty\n" +
                "      {\n" +
                "        url: 'https://x.com';\n" +
                "        url: 'https://x.com';\n" +
                "      }\n" +
                "    ]\n" +
                "  }\n" +
                "}\n", "PARSER error at [25:7-29:7]: Field 'url' should be specified only once");
    }

    @Test
    public void persisterStreamingConnections()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Streaming\n" +
                "  {\n" +
                "    connections: [];\n" +
                "    connections: [];\n" +
                "  }\n" +
                "}\n", "PARSER error at [8:14-12:3]: Field 'connections' should be specified only once");
    }

    @Test
    public void persisterBatchConnections()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    connections: [];\n" +
                "    connections: [];\n" +
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
                "}\n", "PARSER error at [8:14-22:3]: Field 'connections' should be specified only once");
    }

    @Test
    public void persisterBatchTarget()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "  }\n" +
                "}\n", "PARSER error at [8:14-10:3]: Field 'target' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [8:14-30:3]: Field 'target' should be specified only once");
    }

    /**********
     * target shape - multi flat
     **********/

    @Test
    public void multiFlatModelClass()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [10:13-28:5]: Field 'modelClass' is required");
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [10:13-30:5]: Field 'modelClass' should be specified only once");
    }

    @Test
    public void multiFlatTransactionScope()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [10:13-28:5]: Field 'transactionScope' is required");
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [10:13-30:5]: Field 'transactionScope' should be specified only once");
    }

    @Test
    public void multiFlatComponents()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetMulti() + "\n" +
                "    {\n" +
                "      modelClass: test::ModelClass;\n" +
                "      transactionScope: ALL_TARGETS;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [10:13-14:5]: Field '" + parts() + "' is required");
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [10:13-44:5]: Field '" + parts() + "' should be specified only once");
    }

    /**********
     * target shape - flat
     **********/

    @Test
    public void flatTargetName()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [10:13-18:5]: Field 'targetName' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [10:13-20:5]: Field 'targetName' should be specified only once");
    }

    @Test
    public void flatModelClass()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [10:13-18:5]: Field 'modelClass' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [10:13-20:5]: Field 'modelClass' should be specified only once");
    }

    @Test
    public void flatDeduplicationStrategy()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [10:13-21:5]: Field 'deduplicationStrategy' should be specified only once");
    }

    @Test
    public void flatPartitionProperties()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [10:13-21:5]: Field 'partitionProperties' should be specified only once");
    }

    @Test
    public void flatIngestMode()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [10:13-14:5]: Field '" + ingestMode() + "' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [10:13-24:5]: Field '" + ingestMode() + "' should be specified only once");
    }

    /**********
     * ingest mode - snapshot
     **********/

    @Test
    public void ingestModeNontemporalSnapshotAuditing()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [14:19-16:7]: Field 'auditing' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [14:19-18:7]: Field 'auditing' should be specified only once");
    }

    @Test
    public void ingestModeUnitemporalSnapshotTransactionMilestoning()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [14:19-16:7]: Field 'transactionMilestoning' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": UnitemporalSnapshot\n" +
                "      {\n" +
                "        transactionMilestoning: BatchId\n" +
                "        {\n" +
                "          batchIdInFieldName: 'IN_Z';\n" +
                "          batchIdOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "        transactionMilestoning: BatchId\n" +
                "        {\n" +
                "          batchIdInFieldName: 'IN_Z';\n" +
                "          batchIdOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [14:19-26:7]: Field 'transactionMilestoning' should be specified only once");
    }

    @Test
    public void ingestModeBitemporalSnapshotTransactionMilestoning()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalSnapshot\n" +
                "      {\n" +
                "        validityMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeFromFieldName: 'FROM_Z';\n" +
                "          dateTimeThruFieldName: 'THRU_Z';\n" +
                "          derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "          {\n" +
                "            sourceDateTimeFromProperty: sourceFrom;\n" +
                "            sourceDateTimeThruProperty: sourceThru;\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [14:19-26:7]: Field 'transactionMilestoning' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalSnapshot\n" +
                "      {\n" +
                "        transactionMilestoning: BatchIdAndDateTime\n" +
                "        {\n" +
                "          batchIdInFieldName: 'BATCH_ID_IN';\n" +
                "          batchIdOutFieldName: 'BATCH_ID_OUT';\n" +
                "          dateTimeInFieldName: 'IN_Z';\n" +
                "          dateTimeOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "        transactionMilestoning: BatchIdAndDateTime\n" +
                "        {\n" +
                "          batchIdInFieldName: 'BATCH_ID_IN';\n" +
                "          batchIdOutFieldName: 'BATCH_ID_OUT';\n" +
                "          dateTimeInFieldName: 'IN_Z';\n" +
                "          dateTimeOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "        validityMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeFromFieldName: 'FROM_Z';\n" +
                "          dateTimeThruFieldName: 'THRU_Z';\n" +
                "          derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "          {\n" +
                "            sourceDateTimeFromProperty: sourceFrom;\n" +
                "            sourceDateTimeThruProperty: sourceThru;\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [14:19-40:7]: Field 'transactionMilestoning' should be specified only once");
    }

    @Test
    public void ingestModeBitemporalSnapshotValidityMilestoning()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalSnapshot\n" +
                "      {\n" +
                "        transactionMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeInFieldName: 'IN_Z';\n" +
                "          dateTimeOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [14:19-21:7]: Field 'validityMilestoning' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalSnapshot\n" +
                "      {\n" +
                "        transactionMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeInFieldName: 'IN_Z';\n" +
                "          dateTimeOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "        validityMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeFromFieldName: 'FROM_Z';\n" +
                "          dateTimeThruFieldName: 'THRU_Z';\n" +
                "          derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "          {\n" +
                "            sourceDateTimeFromProperty: sourceFrom;\n" +
                "            sourceDateTimeThruProperty: sourceThru;\n" +
                "          }\n" +
                "        }\n" +
                "        validityMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeFromFieldName: 'FROM_Z';\n" +
                "          dateTimeThruFieldName: 'THRU_Z';\n" +
                "          derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "          {\n" +
                "            sourceDateTimeFromProperty: sourceFrom;\n" +
                "            sourceDateTimeThruProperty: sourceThru;\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [14:19-41:7]: Field 'validityMilestoning' should be specified only once");
    }

    @Test
    public void ingestModeBitemporalSnapshotValidityDerivation()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalSnapshot\n" +
                "      {\n" +
                "        transactionMilestoning: BatchId\n" +
                "        {\n" +
                "          batchIdInFieldName: 'IN_Z';\n" +
                "          batchIdOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
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
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalSnapshot\n" +
                "      {\n" +
                "        transactionMilestoning: BatchId\n" +
                "        {\n" +
                "          batchIdInFieldName: 'IN_Z';\n" +
                "          batchIdOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "        validityMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeFromFieldName: 'FROM_Z';\n" +
                "          dateTimeThruFieldName: 'THRU_Z';\n" +
                "          derivation: SourceSpecifiesFromDateTime\n" +
                "          {\n" +
                "            sourceDateTimeFromProperty: sourceFrom;\n" +
                "          }\n" +
                "          derivation: SourceSpecifiesFromDateTime\n" +
                "          {\n" +
                "            sourceDateTimeFromProperty: sourceFrom;\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [21:30-33:9]: Field 'derivation' should be specified only once");
    }

    /**********
     * ingest mode - delta
     **********/

    @Test
    public void ingestModeNontemporalDeltaMergeStrategy()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [14:19-17:7]: Field 'mergeStrategy' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": NontemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: NoDeletes;" +
                "        mergeStrategy: NoDeletes;" +
                "        auditing: None;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [14:19-17:7]: Field 'mergeStrategy' should be specified only once");
    }

    @Test
    public void ingestModeNontemporalDeltaAuditing()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": NontemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: NoDeletes;" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [14:19-16:40]: Field 'auditing' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": NontemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: NoDeletes;" +
                "        auditing: None;\n" +
                "        auditing: None;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [14:19-18:7]: Field 'auditing' should be specified only once");
    }

    @Test
    public void ingestModeUnitemporalDeltaMergeStrategy()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": UnitemporalDelta\n" +
                "      {\n" +
                "        transactionMilestoning: BatchId\n" +
                "        {\n" +
                "          batchIdInFieldName: 'IN_Z';\n" +
                "          batchIdOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [14:19-21:7]: Field 'mergeStrategy' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": UnitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: NoDeletes;\n" +
                "        mergeStrategy: NoDeletes;\n" +
                "        transactionMilestoning: BatchId\n" +
                "        {\n" +
                "          batchIdInFieldName: 'IN_Z';\n" +
                "          batchIdOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [14:19-23:7]: Field 'mergeStrategy' should be specified only once");
    }

    @Test
    public void ingestModeUnitemporalDeltaTransactionMilestoning()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": UnitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: NoDeletes;\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [14:19-17:7]: Field 'transactionMilestoning' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": UnitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: NoDeletes;\n" +
                "        transactionMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeInFieldName: 'IN_Z';\n" +
                "          dateTimeOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "        transactionMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeInFieldName: 'IN_Z';\n" +
                "          dateTimeOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [14:19-27:7]: Field 'transactionMilestoning' should be specified only once");
    }

    @Test
    public void ingestModeBitemporalDeltaTransactionMilestoning()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: NoDeletes;\n" +
                "        validityMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeFromFieldName: 'FROM_Z';\n" +
                "          dateTimeThruFieldName: 'THRU_Z';\n" +
                "          derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "          {\n" +
                "            sourceDateTimeFromProperty: sourceFrom;\n" +
                "            sourceDateTimeThruProperty: sourceThru;\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [14:19-27:7]: Field 'transactionMilestoning' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: NoDeletes;\n" +
                "        transactionMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeInFieldName: 'IN_Z';\n" +
                "          dateTimeOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "        transactionMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeInFieldName: 'IN_Z';\n" +
                "          dateTimeOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "        validityMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeFromFieldName: 'FROM_Z';\n" +
                "          dateTimeThruFieldName: 'THRU_Z';\n" +
                "          derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "          {\n" +
                "            sourceDateTimeFromProperty: sourceFrom;\n" +
                "            sourceDateTimeThruProperty: sourceThru;\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [14:19-37:7]: Field 'transactionMilestoning' should be specified only once");
    }

    @Test
    public void ingestModeBitemporalDeltaMergeStrategy()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalDelta\n" +
                "      {\n" +
                "        transactionMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeInFieldName: 'IN_Z';\n" +
                "          dateTimeOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "        validityMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeFromFieldName: 'FROM_Z';\n" +
                "          dateTimeThruFieldName: 'THRU_Z';\n" +
                "          derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "          {\n" +
                "            sourceDateTimeFromProperty: sourceFrom;\n" +
                "            sourceDateTimeThruProperty: sourceThru;\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [14:19-31:7]: Field 'mergeStrategy' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: NoDeletes;\n" +
                "        mergeStrategy: NoDeletes;\n" +
                "        transactionMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeInFieldName: 'IN_Z';\n" +
                "          dateTimeOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "        validityMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeFromFieldName: 'FROM_Z';\n" +
                "          dateTimeThruFieldName: 'THRU_Z';\n" +
                "          derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "          {\n" +
                "            sourceDateTimeFromProperty: sourceFrom;\n" +
                "            sourceDateTimeThruProperty: sourceThru;\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [14:19-33:7]: Field 'mergeStrategy' should be specified only once");
    }

    @Test
    public void ingestModeBitemporalDeltaValidityMilestoning()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: NoDeletes;\n" +
                "        transactionMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeInFieldName: 'IN_Z';\n" +
                "          dateTimeOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [14:19-22:7]: Field 'validityMilestoning' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: NoDeletes;\n" +
                "        transactionMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeInFieldName: 'IN_Z';\n" +
                "          dateTimeOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "        validityMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeFromFieldName: 'FROM_Z';\n" +
                "          dateTimeThruFieldName: 'THRU_Z';\n" +
                "          derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "          {\n" +
                "            sourceDateTimeFromProperty: sourceFrom;\n" +
                "            sourceDateTimeThruProperty: sourceThru;\n" +
                "          }\n" +
                "        }\n" +
                "        validityMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeFromFieldName: 'FROM_Z';\n" +
                "          dateTimeThruFieldName: 'THRU_Z';\n" +
                "          derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "          {\n" +
                "            sourceDateTimeFromProperty: sourceFrom;\n" +
                "            sourceDateTimeThruProperty: sourceThru;\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [14:19-42:7]: Field 'validityMilestoning' should be specified only once");
    }

    @Test
    public void ingestModeBitemporalDeltaValidityDerivation()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: NoDeletes;\n" +
                "        transactionMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeInFieldName: 'IN_Z';\n" +
                "          dateTimeOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "        validityMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeFromFieldName: 'FROM_Z';\n" +
                "          dateTimeThruFieldName: 'THRU_Z';\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [22:30-26:9]: Field 'derivation' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetFlat() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      " + ingestMode() + ": BitemporalDelta\n" +
                "      {\n" +
                "        mergeStrategy: NoDeletes;\n" +
                "        transactionMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeInFieldName: 'IN_Z';\n" +
                "          dateTimeOutFieldName: 'OUT_Z';\n" +
                "        }\n" +
                "        validityMilestoning: DateTime\n" +
                "        {\n" +
                "          dateTimeFromFieldName: 'FROM_Z';\n" +
                "          dateTimeThruFieldName: 'THRU_Z';\n" +
                "          derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "          {\n" +
                "            sourceDateTimeFromProperty: sourceFrom;\n" +
                "            sourceDateTimeThruProperty: sourceThru;\n" +
                "          }\n" +
                "          derivation: SourceSpecifiesFromAndThruDateTime\n" +
                "          {\n" +
                "            sourceDateTimeFromProperty: sourceFrom;\n" +
                "            sourceDateTimeThruProperty: sourceThru;\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [22:30-36:9]: Field 'derivation' should be specified only once");
    }

    /**********
     * ingest mode - append only
     **********/

    @Test
    public void ingestModeAppendOnlyAuditing()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [14:19-17:7]: Field 'auditing' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [14:19-19:7]: Field 'auditing' should be specified only once");
    }

    @Test
    public void ingestModeAppendOnlyFilterDuplicates()
    {
        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [14:19-17:7]: Field 'filterDuplicates' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
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
                "}\n", "PARSER error at [14:19-19:7]: Field 'filterDuplicates' should be specified only once");
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
                "  trigger: Manual;\n" +
                "  service: test::service::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    connections:\n" +
                "    [\n" +
                "      id1: test::TestConnection,\n" +
                "      id2:\n" +
                "      #{\n" +
                "        JsonModelConnection\n" +
                "        {\n" +
                "          class: org::dxl::Animal;\n" +
                "          url: 'my_url2';\n" +
                "        }\n" +
                "      }#\n" +
                "    ];\n" +
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
                "            deduplicationStrategy: None;\n" +
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
                "                  sourceDateTimeFromProperty: sourceFrom;\n" +
                "                  sourceDateTimeThruProperty: sourceThru;\n" +
                "                }\n" +
                "              }\n" +
                "            }\n" +
                "          }\n" +
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
