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

package org.finos.legend.engine.language.pure.grammar.test;

import org.junit.Test;

public class TestRelationalMapperGrammarRoundTrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void testRelationalMapperRoundTrip()
    {
        test("###QueryPostProcessor\n" +
                "RelationalMapper test::testMapper\n" +
                "(\n" +
                "   DatabaseMappers:\n" +
                "   [\n" +
                "      [test::AccountDB.alloytest, test::AccountDB.default] -> 'SnowflakeOrgDB',\n" +
                "      [test::AccountDB.alloytest1] -> 'SnowflakeOrgDBNew'\n" +
                "   ];\n" +
                "   SchemaMappers:\n" +
                "   [\n" +
                "      test::AccountDB.alloytest -> 'AlloySchema',\n" +
                "      test::AccountDB.alloytest -> 'AlloySchemaNew'\n" +
                "   ];\n" +
                "   TableMappers:\n" +
                "   [\n" +
                "      test::AccountDB.alloytest.ACCOUNT -> 'AccountTable',\n" +
                "      test::AccountDB.alloytest.ACCOUNT1 -> 'AccountTableNew'\n" +
                "   ];\n" +
                ")\n" +
                "\n" +
                "\n" +
                "###Connection\n" +
                "RelationalDatabaseConnection test::MapperConnection\n" +
                "{\n" +
                "  store: test::AccountDB;\n" +
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
                "}\n");
    }
}
