package org.finos.legend.engine.language.pure.grammar.test;

import org.junit.Test;

public class TestAwsFinCloudConnectionGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testFinCloudConnection()
    {
        test("###Connection\n" +
                "AwsFinCloudConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  datasetId: 'AWS';\n" +
                "  authenticationStrategy: awsOAuth\n" +
                "  {\n" +
                "    secretArn: 'name';\n" +
                "    discoveryUrl: 'name';\n" +
                "  };\n" +
                "  apiUrl: 'test';\n" +
                "  queryInfo: 'info';\n" +
                "}\n");
    }
}
