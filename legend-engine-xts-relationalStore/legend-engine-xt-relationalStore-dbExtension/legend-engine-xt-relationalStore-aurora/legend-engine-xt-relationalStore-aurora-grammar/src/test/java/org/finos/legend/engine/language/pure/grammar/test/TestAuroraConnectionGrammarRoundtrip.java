// Copyright 2026 Goldman Sachs
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

public class TestAuroraConnectionGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testAuroraConnectionGrammar()
    {
        test("###Connection\n" +
                "RelationalDatabaseConnection simple::AuroraConnection\n" +
                "{\n" +
                "  store: apps::pure::studio::relational::tests::dbInc;\n" +
                "  type: Aurora;\n" +
                "  specification: Aurora\n" +
                "  {\n" +
                "    host: 'mydb.cluster-xyz.us-east-1.rds.amazonaws.com';\n" +
                "    port: 5432;\n" +
                "    name: 'mydb';\n" +
                "  };\n" +
                "  auth: UserNamePassword\n" +
                "  {\n" +
                "    userNameVaultReference: 'user';\n" +
                "    passwordVaultReference: 'pwd';\n" +
                "  };\n" +
                "}\n");
    }

    @Test
    public void testAuroraConnectionGrammarWithClusterPattern()
    {
        test("###Connection\n" +
                "RelationalDatabaseConnection simple::AuroraConnection\n" +
                "{\n" +
                "  store: apps::pure::studio::relational::tests::dbInc;\n" +
                "  type: Aurora;\n" +
                "  specification: Aurora\n" +
                "  {\n" +
                "    host: 'mydb.cluster-xyz.us-east-1.rds.amazonaws.com';\n" +
                "    port: 5432;\n" +
                "    name: 'mydb';\n" +
                "    clusterInstanceHostPattern: '?.xyz.us-east-1.rds.amazonaws.com';\n" +
                "  };\n" +
                "  auth: UserNamePassword\n" +
                "  {\n" +
                "    userNameVaultReference: 'user';\n" +
                "    passwordVaultReference: 'pwd';\n" +
                "  };\n" +
                "}\n");
    }

    @Test
    public void testGlobalAuroraConnectionGrammar()
    {
        test("###Connection\n" +
                "RelationalDatabaseConnection simple::GlobalAuroraConnection\n" +
                "{\n" +
                "  store: apps::pure::studio::relational::tests::dbInc;\n" +
                "  type: Aurora;\n" +
                "  specification: GlobalAurora\n" +
                "  {\n" +
                "    host: 'mydb.global-xyz.rds.amazonaws.com';\n" +
                "    port: 5432;\n" +
                "    name: 'mydb';\n" +
                "    region: 'us-east-1';\n" +
                "    globalClusterInstanceHostPatterns: ['?.XYZ1.us-east-1.rds.amazonaws.com', '?.XYZ2.us-west-2.rds.amazonaws.com'];\n" +
                "  };\n" +
                "  auth: UserNamePassword\n" +
                "  {\n" +
                "    userNameVaultReference: 'user';\n" +
                "    passwordVaultReference: 'pwd';\n" +
                "  };\n" +
                "}\n");
    }
}
