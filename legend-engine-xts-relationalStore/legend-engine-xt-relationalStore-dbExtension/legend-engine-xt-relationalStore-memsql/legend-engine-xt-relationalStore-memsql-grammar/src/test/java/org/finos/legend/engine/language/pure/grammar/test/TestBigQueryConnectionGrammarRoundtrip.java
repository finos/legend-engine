// Copyright 2021 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
