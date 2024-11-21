// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.language.pure.compiler.test;

import org.junit.Test;

public class TestRelationalMapperCompilationFromGrammar extends TestCompilationFromGrammar.TestCompilationFromGrammarTestSuite
{
    String store =  "###Relational\n" +
                    "Database test::OrganizationsDB\n" +
                    "(\n" +
                    "  Schema Org\n" +
                    "  (\n" +
                    "    Table Org\n" +
                    "    (\n" +
                    "      OrgId VARCHAR(30) PRIMARY KEY\n" +
                    "    )\n" +
                    "  )\n" +
                    "  Schema Product\n" +
                    "  (\n" +
                    "    Table Product\n" +
                    "    (\n" +
                    "      ProductId VARCHAR(30) PRIMARY KEY\n" +
                    "    )\n" +
                    "  )\n" +
                    ")\n";

    String relationalMapper = "###QueryPostProcessor\n" +
            "RelationalMapper test::testMapper\n" +
            "(\n" +
            "  DatabaseMappers:\n" +
            "  [\n" +
            "    [test::OrganizationsDB.Org] -> 'SnowflakeOrgDB'\n" +
            "  ];\n" +
            "  SchemaMappers:\n" +
            "  [\n" +
            "    test::OrganizationsDB.Product -> 'ProductSchemaNew',\n" +
            "    test::OrganizationsDB.Org -> 'OrgSchemaNew'\n" +
            "  ];\n" +
            "  TableMappers:\n" +
            "  [\n" +
            "    test::OrganizationsDB.Org.Org -> 'OrgNewTable'\n" +
            "  ];\n" +
            ")\n";

    @Test
    public void testRelationalMapper()
    {
        test(store + relationalMapper);
    }

    @Test
    public void testDuplicatedRelationalMapperOnConnection()
    {
        test(store + relationalMapper +
                "###Connection\n" +
                        "RelationalDatabaseConnection test::MapperConnection\n" +
                        "{\n" +
                        "  store: test::OrganizationsDB;\n" +
                        "  type: H2;\n" +
                        "  specification: LocalH2\n" +
                        "  {\n" +
                        "  };\n" +
                        "  auth: DefaultH2;\n" +
                        "  postProcessors:\n" +
                        "  [\n" +
                        "    relationalMapper\n" +
                        "    {\n" +
                        "      test::testMapper, test::testMapper\n" +
                        "    }\n" +
                        "  ];\n" +
                        "}\n",
                "COMPILATION error at [37:1-52:1]: Error in 'test::MapperConnection': Found duplicated relational mapper(s) [test::testMapper]"
        );
    }

    @Test
    public void testDuplicatedDatabaseMapper()
    {
        test(store +
                        "###QueryPostProcessor\n" +
                        "RelationalMapper test::testMapper\n" +
                        "(\n" +
                        "   DatabaseMappers:\n" +
                        "   [\n" +
                        "      [test::OrganizationsDB.Org] -> 'SnowflakeOrgDB1',\n" +
                        "      [test::OrganizationsDB.Org] -> 'SnowflakeOrgDB2',\n" +
                        "      [test::OrganizationsDB.Product] -> 'SnowflakeProduct1',\n" +
                        "      [test::OrganizationsDB.Product] -> 'SnowflakeProduct2'\n" +
                        "   ];\n" +
                        ")",
                "COMPILATION error at [20:1-29:1]: Error in 'test::testMapper': Found duplicated mappers for [test::OrganizationsDB.Org, test::OrganizationsDB.Product]"
        );
    }

    @Test
    public void testInvalidDatabase()
    {
        test(store +
                        "###QueryPostProcessor\n" +
                        "RelationalMapper test::testMapper\n" +
                        "(\n" +
                        "   DatabaseMappers:\n" +
                        "   [\n" +
                        "      [test::OrgDB.Product] -> 'SnowflakeDB'\n" +
                        "   ];\n" +
                        ")",
                "COMPILATION error at [24:8-26]: Can't find database 'test::OrgDB'"
        );
    }

    @Test
    public void testDuplicatedSchemaMapper()
    {
        test(store +
                        "###QueryPostProcessor\n" +
                        "RelationalMapper test::testMapper\n" +
                        "(\n" +
                        "   SchemaMappers:\n" +
                        "   [\n" +
                        "      test::OrganizationsDB.Org -> 'OrgSchema',\n" +
                        "      test::OrganizationsDB.Org -> 'OrgSchemaNew',\n" +
                        "      test::OrganizationsDB.Product -> 'ProductSchema'\n" +
                        "   ];\n" +
                        ")",
                "COMPILATION error at [20:1-28:1]: Error in 'test::testMapper': Found duplicated mappers for [test::OrganizationsDB.Org]"
        );
    }

    @Test
    public void testInvalidSchema()
    {
        test(store +
                        "###QueryPostProcessor\n" +
                        "RelationalMapper test::testMapper\n" +
                        "(\n" +
                        "   SchemaMappers:\n" +
                        "   [\n" +
                        "      test::OrganizationsDB.Productt -> 'ProductSchema'\n" +
                        "   ];\n" +
                        ")",
                "COMPILATION error at [20:1-26:1]: Error in 'test::testMapper': Can't find schema 'Productt' in database 'OrganizationsDB'"
        );
    }

    @Test
    public void testDuplicatedTableMapper()
    {
        test(store +
                        "###QueryPostProcessor\n" +
                        "RelationalMapper test::testMapper\n" +
                        "(\n" +
                        "   TableMappers:\n" +
                        "   [\n" +
                        "      test::OrganizationsDB.Product.Product -> 'ProductTable',\n" +
                        "      test::OrganizationsDB.Product.Product -> 'ProductTableNew'\n" +
                        "   ];\n" +
                        ")",
                "COMPILATION error at [20:1-27:1]: Error in 'test::testMapper': Found duplicated mappers for [test::OrganizationsDB.Product.Product]"
        );
    }

    @Test
    public void testInvalidTable()
    {
        test(store +
                        "###QueryPostProcessor\n" +
                        "RelationalMapper test::testMapper\n" +
                        "(\n" +
                        "   TableMappers:\n" +
                        "   [\n" +
                        "      test::OrganizationsDB.Product.Productt -> 'ProductTable'\n" +
                        "   ];\n" +
                        ")",
                " at [20:1-26:1]: Error in 'test::testMapper': Can't find Productt table in Product schema."
        );
    }

    @Override
    protected String getDuplicatedElementTestCode()
    {
        return "###QueryPostProcessor\n" +
                "RelationalMapper anything::somethingelse\n" +
                "(\n" +
                ")\n" +
                "RelationalMapper anything::somethingelse\n" +
                "(\n" +
                ")\n";
    }

    @Override
    protected String getDuplicatedElementTestExpectedErrorMessage()
    {
        return "COMPILATION error at [5:1-7:1]: Duplicated element 'anything::somethingelse'";
    }
}
