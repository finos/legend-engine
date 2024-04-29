// Copyright 2023 Goldman Sachs
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

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.RelationalDatabaseConnectionParserGrammar;
import org.junit.Test;

import java.util.List;

public class TestSnowflakeConnectionGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return ConnectionParserGrammar.VOCABULARY;
    }

    @Override
    public List<Vocabulary> getDelegatedParserGrammarVocabulary()
    {
        return FastList.newListWith(
                RelationalDatabaseConnectionParserGrammar.VOCABULARY
        );
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Connection\n" +
                "RelationalDatabaseConnection " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  store: model::firm::Person;\n" +
                "  specification: LocalH2 { testDataSetupCSV: 'testCSV'; };\n" +
                "  timezone: +3000;\n" +
                "  type: H2;\n" +
                "  auth: DefaultH2;\n" +
                "}\n\n";
    }

    @Test
    public void testSnowflakePublicAuth()
    {
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  type: Snowflake;\n" +
                "  specification: Snowflake\n" +
                "  {\n" +
                "    name: 'test';\n" +
                "    account: 'account';\n" +
                "    warehouse: 'warehouseName';\n" +
                "    region: 'us-east2';\n" +
                "  };\n" +
                "  auth: SnowflakePublic\n" +
                "  {\n" +
                "  };\n" +
                "}\n", "PARSER error at [13:3-15:4]: Field 'publicUserName' is required");
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  type: Snowflake;\n" +
                "  specification: Snowflake\n" +
                "  {\n" +
                "    name: 'test';\n" +
                "    account: 'account';\n" +
                "    warehouse: 'warehouseName';\n" +
                "    region: 'us-east2';\n" +
                "  };\n" +
                "  auth: SnowflakePublic\n" +
                "  {" +
                "       publicUserName: 'name';\n" +
                "  };\n" +
                "}\n", "PARSER error at [13:3-15:4]: Field 'privateKeyVaultReference' is required");
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  type: Snowflake;\n" +
                "  specification: Snowflake\n" +
                "  {\n" +
                "    name: 'test';\n" +
                "    account: 'account';\n" +
                "    warehouse: 'warehouseName';\n" +
                "    region: 'us-east2';\n" +
                "  };\n" +
                "  auth: SnowflakePublic\n" +
                "  {" +
                "       publicUserName: 'name';\n" +
                "       privateKeyVaultReference : 'key';\n" +
                "  };\n" +
                "}\n", "PARSER error at [13:3-16:4]: Field 'passPhraseVaultReference' is required");
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  type: Snowflake;\n" +
                "  specification: Snowflake\n" +
                "  {\n" +
                "    name: 'test';\n" +
                "    account: 'account';\n" +
                "    warehouse: 'warehouseName';\n" +
                "    region: 'us-east2';\n" +
                "  };\n" +
                "  auth: SnowflakePublic\n" +
                "  {" +
                "       publicUserName: 'name';\n" +
                "       publicUserName: 'name';\n" +
                "  };\n" +
                "}\n", "PARSER error at [13:3-16:4]: Field 'publicUserName' should be specified only once");
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  type: Snowflake;\n" +
                "  specification: Snowflake\n" +
                "  {\n" +
                "    name: 'test';\n" +
                "    account: 'account';\n" +
                "    warehouse: 'warehouseName';\n" +
                "    region: 'us-east2';\n" +
                "    proxyHost: 'sampleHost';\n" +
                "    proxyPort: 'samplePort';\n" +
                "    nonProxyHosts: 'sample';\n" +
                "    accountType: MultiTenant;\n" +
                "    organization: 'sampleOrganization';\n" +
                "  };\n" +
                "  auth: SnowflakePublic\n" +
                "  {" +
                "       publicUserName: 'name';\n" +
                "       privateKeyVaultReference: 'name';\n" +
                "       passPhraseVaultReference: 'name';\n" +
                "  };\n" +
                "}\n");
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  type: Snowflake;\n" +
                "  specification: Snowflake\n" +
                "  {\n" +
                "    name: 'test';\n" +
                "    account: 'account';\n" +
                "    warehouse: 'warehouseName';\n" +
                "    region: 'us-east2';\n" +
                "  };\n" +
                "  auth: SnowflakePublic\n" +
                "  {" +
                "       publicUserName: 'name';\n" +
                "       privateKeyVaultReference: 'name';\n" +
                "       passPhraseVaultReference: 'name';\n" +
                "  };\n" +
                "}\n");
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  type: Snowflake;\n" +
                "  specification: Snowflake\n" +
                "  {\n" +
                "    name: 'test';\n" +
                "    account: 'account';\n" +
                "    warehouse: 'warehouseName';\n" +
                "    region: 'us-east2';\n" +
                "  };\n" +
                "  auth: SnowflakePublic\n" +
                "  {" +
                "       publicUserName: 'name';\n" +
                "       privateKeyVaultReference : 'key';\n" +
                "       privateKeyVaultReference : 'key';\n" +
                "  };\n" +
                "}\n", "PARSER error at [13:3-17:4]: Field 'privateKeyVaultReference' should be specified only once");
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  type: Snowflake;\n" +
                "  specification: Snowflake\n" +
                "  {\n" +
                "    name: 'test';\n" +
                "    account: 'account';\n" +
                "    warehouse: 'warehouseName';\n" +
                "    region: 'us-east2';\n" +
                "  };\n" +
                "  auth: SnowflakePublic\n" +
                "  {" +
                "       publicUserName: 'name';\n" +
                "       privateKeyVaultReference : 'key';\n" +
                "       passPhraseVaultReference : 'pass';\n" +
                "       passPhraseVaultReference : 'pass';\n" +
                "  };\n" +
                "}\n", "PARSER error at [13:3-18:4]: Field 'passPhraseVaultReference' should be specified only once");
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  type: Snowflake;\n" +
                "  specification: Snowflake\n" +
                "  {\n" +
                "    name: 'test';\n" +
                "    account: 'account';\n" +
                "    warehouse: 'warehouseName';\n" +
                "    region: 'us-east2';\n" +
                "    proxyHost: 'sampleHost';\n" +
                "    proxyPort: 'samplePort';\n" +
                "    nonProxyHosts: 'sample';\n" +
                "    accountType: MultiTenant;\n" +
                "    organization: 'sampleOrganization';\n" +
                "    role: 'sampleRole';\n" +
                "  };\n" +
                "  auth: SnowflakePublic\n" +
                "  {" +
                "       publicUserName: 'name';\n" +
                "       privateKeyVaultReference: 'name';\n" +
                "       passPhraseVaultReference: 'name';\n" +
                "  };\n" +
                "}\n");
    }
}
