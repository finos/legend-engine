package org.finos.legend.engine.language.pure.grammar.test;

import org.junit.Test;

public class TestAwsS3ConnectionGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testS3Connection()
    {
        test("###Connection\n" +
                "AwsS3Connection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  partition: AWS;\n" +
                "  region: 'US';\n" +
                "  bucket: 'abc';\n" +
                "  key: 'xyz';\n" +
                "}\n");
    }
}
