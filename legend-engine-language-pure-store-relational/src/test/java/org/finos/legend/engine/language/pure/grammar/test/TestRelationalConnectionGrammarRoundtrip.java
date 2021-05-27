// Copyright 2020 Goldman Sachs
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

public class TestRelationalConnectionGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testRelationalDatabaseConnection()
    {
        test("###Connection\n" +
                "RelationalDatabaseConnection simple::H2Connection\n" +
                "{\n" +
                "  store: apps::pure::studio::relational::tests::dbInc;\n" +
                "  type: H2;\n" +
                "  specification: LocalH2\n" +
                "  {\n" +
                "  };\n" +
                "  auth: DefaultH2;\n" +
                "}\n");
    }

    @Test
    public void testDataSourceSpecConfigurations()
    {
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  type: H2;\n" +
                "  specification: LocalH2\n" +
                "  {\n" +
                "    testDataSetupCSV: 'testCSV';\n" +
                "  };\n" +
                "  auth: DefaultH2;\n" +
                "}\n");
    }

    @Test
    public void testDataSourceSpecConfigurationsWithSqls()
    {
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: model::firm::Person;\n" +
                "  type: H2;\n" +
                "  specification: LocalH2\n" +
                "  {\n" +
                "    testDataSetupSqls: [\n" +
                "      'ab',\n" +
                "      'cd'\n" +
                "      ];\n" +
                "  };\n" +
                "  auth: DefaultH2;\n" +
                "}\n");

        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: model::firm::Person;\n" +
                "  type: H2;\n" +
                "  specification: LocalH2\n" +
                "  {\n" +
                "    testDataSetupSqls: [\n" +
                "      'ab'\n" +
                "      ];\n" +
                "  };\n" +
                "  auth: DefaultH2;\n" +
                "}\n");
    }

    @Test
    public void testRelationalDatabaseAuthConfigurations()
    {
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: store::Store;\n" +
                "  type: H2;\n" +
                "  specification: LocalH2\n" +
                "  {\n" +
                "    testDataSetupCSV: 'testCSV';\n" +
                "  };\n" +
                "  auth: DefaultH2;\n" +
                "}\n");
    }

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
    public void testSingleMapperPostProcessors()
    {
        testPostProcessor(
                "    mapper\n" +
                "    {\n" +
                "      mappers:\n" +
                "      [\n" +
                "        table {from: 'a'; to: 'A'; schemaFrom: 'b'; schemaTo: 'B';},\n" +
                "        schema {from: 'c'; to: 'C';}\n" +
                "      ];\n" +
                "    }");
    }

    @Test
    public void testMultipleMapperPostProcessors()
    {
        testPostProcessor(
                "    mapper\n" +
                        "    {\n" +
                        "      mappers:\n" +
                        "      [\n" +
                        "        table {from: 'a'; to: 'A'; schemaFrom: 'b'; schemaTo: 'B';},\n" +
                        "        schema {from: 'c'; to: 'C';}\n" +
                        "      ];\n" +
                        "    }",
                "    mapper\n" +
                        "    {\n" +
                        "      mappers:\n" +
                        "      [\n" +
                        "        schema {from: 'c'; to: 'C';},\n" +
                        "        table {from: 'a'; to: 'A'; schemaFrom: 'b'; schemaTo: 'B';}\n" +
                        "      ];\n" +
                        "    }"
        );
    }

    private void testPostProcessor(String ...postProcessors) {
        test("###Connection\n" +
                "RelationalDatabaseConnection meta::mySimpleConnection\n" +
                "{\n" +
                "  store: model::firm::Person;\n" +
                "  type: H2;\n" +
                "  specification: LocalH2\n" +
                "  {\n" +
                "    testDataSetupCSV: 'testCSV';\n" +
                "  };\n" +
                "  auth: DefaultH2;\n" +
                "  postProcessors:\n" +
                "  [\n" +
                        String.join(",\n", postProcessors) + "\n" +
                "  ];\n" +
                "}\n");
    }

    @Test
    public void testRelationalDatabaseConnectionWithQuoteIdentifiers()
    {
        test("###Connection\n" +
                "RelationalDatabaseConnection simple::H2Connection\n" +
                "{\n" +
                "  store: apps::pure::studio::relational::tests::dbInc;\n" +
                "  type: H2;\n" +
                "  quoteIdentifiers: false;\n" +
                "  specification: LocalH2\n" +
                "  {\n" +
                "  };\n" +
                "  auth: DefaultH2;\n" +
                "}\n");

        test("###Connection\n" +
                "RelationalDatabaseConnection simple::H2Connection\n" +
                "{\n" +
                "  store: apps::pure::studio::relational::tests::dbInc;\n" +
                "  type: H2;\n" +
                "  quoteIdentifiers: true;\n" +
                "  specification: LocalH2\n" +
                "  {\n" +
                "  };\n" +
                "  auth: DefaultH2;\n" +
                "}\n");
    }
}
