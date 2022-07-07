package org.finos.legend.engine.pure.dsl.persistence.cloud.grammar.test;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestPersistenceCloudGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void persistencePlatformDataProcessingUnits()
    {
        test("###Persistence\n" +
                "PersistenceContext test::TestPersistenceContext\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  platform: AwsGlue\n" +
                "  #{\n" +
                "    dataProcessingUnits: 10;\n" +
                "  }#;\n" +
                "}\n");
    }
}
