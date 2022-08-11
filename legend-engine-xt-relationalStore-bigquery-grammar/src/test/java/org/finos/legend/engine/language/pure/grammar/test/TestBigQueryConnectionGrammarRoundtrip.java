package org.finos.legend.engine.language.pure.grammar.test;

import org.junit.Test;

public class TestBigQueryConnectionGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testBigQueryGCPApplicationDefaultCredentialsAuth()
    {
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  type: BigQuery;\n" +
                "  specification: BigQuery\n" +
                "  {\n" +
                "    projectId: 'proj1';\n" +
                "    defaultDataset: 'dataset1';\n" +
                "  };\n" +
                "  auth: GCPApplicationDefaultCredentials;\n" +
                "}\n");

        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  type: BigQuery;\n" +
                "  specification: BigQuery\n" +
                "  {\n" +
                "    projectId: 'proj1';\n" +
                "    defaultDataset: 'dataset1';\n" +
                "    proxyHost: 'proxyHost1';\n" +
                "    proxyPort: 'proxyPort1';\n" +
                "  };\n" +
                "  auth: GCPApplicationDefaultCredentials;\n" +
                "}\n");
    }

    @Test
    public void testBigQueryGCPWorkloadIdentityFederation()
    {
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  type: BigQuery;\n" +
                "  specification: BigQuery\n" +
                "  {\n" +
                "    projectId: 'proj1';\n" +
                "    defaultDataset: 'dataset1';\n" +
                "  };\n" +
                "  auth: GCPWorkloadIdentityFederation\n" +
                "  {\n" +
                "    serviceAccountEmail: 'name';\n" +
                "  };\n" +
                "}\n");

        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  type: BigQuery;\n" +
                "  specification: BigQuery\n" +
                "  {\n" +
                "    projectId: 'proj1';\n" +
                "    defaultDataset: 'dataset1';\n" +
                "  };\n" +
                "  auth: GCPWorkloadIdentityFederation\n" +
                "  {\n" +
                "    serviceAccountEmail: 'name';\n" +
                "    additionalGcpScopes: [\n" +
                "      'gcpScope',\n" +
                "      'anotherGcpScope'\n" +
                "      ];\n" +
                "  };\n" +
                "}\n");
    }
}
