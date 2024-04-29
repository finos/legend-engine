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

public class TestDatabricksConnectionGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testDeltaLakeDatabaseConnection()
    {
        test("###Connection\n" +
                "RelationalDatabaseConnection simple::DatabricksConnection\n" +
                "{\n" +
                "  store: apps::pure::studio::relational::tests::dbInc;\n" +
                "  type: Databricks;\n" +
                "  specification: Databricks\n" +
                "  {\n" +
                "    hostname: 'hostname';\n" +
                "    port: 'port';\n" +
                "    protocol: 'protocol';\n" +
                "    httpPath: 'httpPath';\n" +
                "  };\n" +
                "  auth: ApiToken\n" +
                "  {\n" +
                "    apiToken: 'token';\n" +
                "  };\n" +
                "}\n");
    }
}
