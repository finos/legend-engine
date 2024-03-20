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

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestSnowflakeConnectionGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
   @Test
    public void testSnowflakeDatabaseASpecificationPublicAuth()
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
                "    cloudType: 'aws';\n" +
                "  };\n" +
                "  auth: SnowflakePublic\n" +
                "  {\n" +
                "    publicUserName: 'myName';\n" +
                "    privateKeyVaultReference: 'privateKeyRef';\n" +
                "    passPhraseVaultReference: 'passRef';\n" +
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
                "    cloudType: 'aws';\n" +
                "  };\n" +
                "  auth: SnowflakePublic\n" +
                "  {\n" +
                "    publicUserName: 'myName';\n" +
                "    privateKeyVaultReference: 'privateKeyRef';\n" +
                "    passPhraseVaultReference: 'passRef';\n" +
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
                "    cloudType: 'aws';\n" +
                "    proxyHost: 'sampleHost';\n" +
                "    proxyPort: 'samplePort';\n" +
                "    nonProxyHosts: 'sample';\n" +
                "    accountType: MultiTenant;\n" +
                "    organization: 'sampleOrganization';\n" +
                "  };\n" +
                "  auth: SnowflakePublic\n" +
                "  {\n" +
                "    publicUserName: 'myName';\n" +
                "    privateKeyVaultReference: 'privateKeyRef';\n" +
                "    passPhraseVaultReference: 'passRef';\n" +
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
                "    cloudType: 'aws';\n" +
                "    proxyHost: 'sampleHost';\n" +
                "    proxyPort: 'samplePort';\n" +
                "    nonProxyHosts: 'sample';\n" +
                "    accountType: BadOption;\n" +
                "    organization: 'sampleOrganization';\n" +
                "  };\n" +
                "  auth: SnowflakePublic\n" +
                "  {\n" +
                "    publicUserName: 'myName';\n" +
                "    privateKeyVaultReference: 'privateKeyRef';\n" +
                "    passPhraseVaultReference: 'passRef';\n" +
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
                "  {\n" +
                "    publicUserName: 'myName';\n" +
                "    privateKeyVaultReference: 'privateKeyRef';\n" +
                "    passPhraseVaultReference: 'passRef';\n" +
                "  };\n" +
                "}\n");
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: model::firm::Person;\n" +
                "  type: H2;\n" +
                "  specification: Snowflake\n" +
                "  {\n" +
                "    name: 'test';\n" +
                "    account: 'okilkol.asdasd';\n" +
                "    warehouse: 'warehousename';\n" +
                "    region: 'EMEA';\n" +
                "    quotedIdentifiersIgnoreCase: true;\n" +
                "  };\n" +
                "  auth: SnowflakePublic\n" +
                "  {\n" +
                "    publicUserName: 'myName';\n" +
                "    privateKeyVaultReference: 'privateKeyRef';\n" +
                "    passPhraseVaultReference: 'passRef';\n" +
                "  };\n" +
                "}\n");
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: model::firm::Person;\n" +
                "  type: H2;\n" +
                "  specification: Snowflake\n" +
                "  {\n" +
                "    name: 'test';\n" +
                "    account: 'okilkol.asdasd';\n" +
                "    warehouse: 'warehousename';\n" +
                "    region: 'EMEA';\n" +
                "    quotedIdentifiersIgnoreCase: false;\n" +
                "  };\n" +
                "  auth: SnowflakePublic\n" +
                "  {\n" +
                "    publicUserName: 'myName';\n" +
                "    privateKeyVaultReference: 'privateKeyRef';\n" +
                "    passPhraseVaultReference: 'passRef';\n" +
                "  };\n" +
                "}\n");
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: model::firm::Person;\n" +
                "  type: H2;\n" +
                "  specification: Snowflake\n" +
                "  {\n" +
                "    name: 'test';\n" +
                "    account: 'okilkol.asdasd';\n" +
                "    warehouse: 'warehousename';\n" +
                "    region: 'EMEA';\n" +
                "    quotedIdentifiersIgnoreCase: false;\n" +
                "    role: 'aRole';\n" +
                "  };\n" +
                "  auth: SnowflakePublic\n" +
                "  {\n" +
                "    publicUserName: 'myName';\n" +
                "    privateKeyVaultReference: 'privateKeyRef';\n" +
                "    passPhraseVaultReference: 'passRef';\n" +
                "  };\n" +
                "}\n");
    }

    @Test
    public void testSnowflakeLocalConnectionSpecification()
    {
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  type: Snowflake;\n" +
                "  mode: local;\n" +
                "}\n");
    }

    @Test
    public void testSnowflakeEnableQueryTags()
    {
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  type: Snowflake;\n" +
                "  specification: Snowflake\n" +
                "  {\n" +
                "    name: 'test';\n" +
                "    account: 'okilkol.asdasd';\n" +
                "    warehouse: 'warehousename';\n" +
                "    region: 'EMEA';\n" +
                "    quotedIdentifiersIgnoreCase: false;\n" +
                "    enableQueryTags: true;\n" +
                "  };\n" +
                "  auth: SnowflakePublic\n" +
                "  {\n" +
                "    publicUserName: 'myName';\n" +
                "    privateKeyVaultReference: 'privateKeyRef';\n" +
                "    passPhraseVaultReference: 'passRef';\n" +
                "  };\n" +
                "}\n");

    }

    @Test
    public void testSnowflakeTempTableSpec()
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
                "    warehouse: 'warehousename';\n" +
                "    region: 'us-east-1';\n" +
                "    quotedIdentifiersIgnoreCase: false;\n" +
                "    enableQueryTags: true;\n" +
                "    tempTableDb: 'temp_table_db';\n" +
                "    tempTableSchema: 'temp_table_schema';\n" +
                "  };\n" +
                "  auth: SnowflakePublic\n" +
                "  {\n" +
                "    publicUserName: 'myName';\n" +
                "    privateKeyVaultReference: 'privateKeyRef';\n" +
                "    passPhraseVaultReference: 'passRef';\n" +
                "  };\n" +
                "}\n");

    }
}
