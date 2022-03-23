package org.finos.legend.engine.language.pure.dsl.persistence.grammar.test;

import org.junit.Test;

public class TestPersistenceGrammarParserV2 extends TestPersistenceGrammarParser
{
    @Override
    protected String targetFlat()
    {
        return "Flat";
    }

    @Override
    protected String targetMulti()
    {
        return "MultiFlat";
    }

    @Override
    protected String targetOpaque()
    {
        return "OpaqueTarget";
    }

    @Override
    protected String ingestMode()
    {
        return "ingestMode";
    }

    @Override
    protected String flatTarget()
    {
        return "flatTarget";
    }

    @Override
    protected String parts()
    {
        return "parts";
    }

    /**********
     * target specification - nested
     **********/

    @Test
    public void batchOpaqueTargetName()
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
                "    target: " + targetOpaque() + "\n" +
                "    {\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [10:13-12:5]: Field 'targetName' is required");

        test("###Persistence\n" +
                "\n" +
                "Persistence test::TestPersistence \n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    target: " + targetOpaque() + "\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      targetName: 'TestDataset1';\n" +
                "    }\n" +
                "  }\n" +
                "}\n", "PARSER error at [10:13-14:5]: Field 'targetName' should be specified only once");
    }
}
