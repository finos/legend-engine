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

public class TestRedshiftConnectionGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testRedShiftConnectionSpecification()
    {
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  type: Redshift;\n" +
                "  specification: Redshift\n" +
                "  {\n" +

                "    host: 'myDBHost';\n" +
                "    port: 1234;\n" +
                "    name: 'database1';\n" +
                "    region: 'east';\n" +
                "    clusterID: 'cluster';\n" +
                "    endpointURL: 'http://www.example.com';\n" +
                "  };\n" +
                "  auth: UserNamePassword\n" +
                "  {\n" +
                "    userNameVaultReference: 'user';\n" +
                "    passwordVaultReference: 'pwd';\n" +
                "  };\n" +
                "}\n");
    }
}
