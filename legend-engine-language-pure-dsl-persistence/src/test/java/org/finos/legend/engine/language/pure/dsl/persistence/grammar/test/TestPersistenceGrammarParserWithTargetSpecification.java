package org.finos.legend.engine.language.pure.dsl.persistence.grammar.test;

import org.junit.Test;

public class TestPersistenceGrammarParserWithTargetSpecification extends TestPersistenceGrammarParser
{
    @Override
    protected String targetSingle()
    {
        return "Flat";
    }

    @Override
    protected String targetMulti()
    {
        return "GroupedFlat";
    }

    @Override
    protected String targetOpaque()
    {
        return "Nested";
    }

    @Override
    protected String batchMode()
    {
        return "batchMode";
    }

    @Override
    protected String singleFlatTarget()
    {
        return "targetSpecification";
    }

    @Override
    protected String parts()
    {
        return "components";
    }

    /**********
     * target specification - nested
     **********/

    @Test
    public void batchNestedTargetName()
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
                "    target: " + targetOpaque() + "\n" +
                "    {\n" +
                "      modelClass: test::ModelClass;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:13-16:5]: Field 'targetName' is required");

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
                "    target: " + targetOpaque() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:13-18:5]: Field 'targetName' should be specified only once");
    }

    @Test
    public void batchNestedModelClass()
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
                "    target: " + targetOpaque() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:13-16:5]: Field 'modelClass' is required");

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
                "    target: " + targetOpaque() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "      modelClass: test::ModelClass;\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [13:13-18:5]: Field 'modelClass' should be specified only once");
    }
}
