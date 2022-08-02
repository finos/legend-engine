// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.persistence.grammar.test;

import org.finos.legend.engine.language.pure.grammar.test.TestGrammarRoundtrip;
import org.junit.Test;

public class TestPersistenceContextGrammarRoundtrip extends TestGrammarRoundtrip.TestGrammarRoundtripTestSuite
{
    @Test
    public void persistenceContextPermitOptionalFieldsToBeEmpty()
    {
        test("###Persistence\n" +
                "import test::*;\n" +
                "PersistenceContext test::TestPersistenceContext\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "}\n");
    }

    @Test
    public void persistenceContextPlatformDefault()
    {
        testFormat(
                "###Persistence\n" +
                        "import test::*;\n" +
                        "PersistenceContext test::TestPersistenceContext\n" +
                        "{\n" +
                        "  persistence: test::TestPersistence;\n" +
                        "}\n",
                "###Persistence\n" +
                        "import test::*;\n" +
                        "PersistenceContext test::TestPersistenceContext\n" +
                        "{\n" +
                        "  persistence: test::TestPersistence;\n" +
                        "  platform: Default;\n" +
                        "}\n");
    }

    @Test
    public void persistenceContextSingleServiceParameter()
    {
        test("###Persistence\n" +
                "import test::*;\n" +
                "PersistenceContext test::TestPersistenceContext\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  serviceParameters:\n" +
                "  [\n" +
                "    foo='hello'\n" +
                "  ];\n" +
                "  sinkConnection: test::TestConnection;\n" +
                "}\n");
    }

    @Test
    public void persistenceContextSinkConnectionPointer()
    {
        test("###Persistence\n" +
                "import test::*;\n" +
                "PersistenceContext test::TestPersistenceContext\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  serviceParameters:\n" +
                "  [\n" +
                "    foo='hello',\n" +
                "    bar=1,\n" +
                "    con1=test::TestConnection,\n" +
                "    con2=\n" +
                "    #{\n" +
                "      RelationalDatabaseConnection\n" +
                "      {\n" +
                "        store: test::TestDatabase;\n" +
                "        type: H2;\n" +
                "        specification: LocalH2\n" +
                "        {\n" +
                "        };\n" +
                "        auth: Test;\n" +
                "      }\n" +
                "    }#\n" +
                "  ];\n" +
                "  sinkConnection: test::TestConnection;\n" +
                "}\n");
    }

    @Test
    public void persistenceContextSinkConnectionEmbedded()
    {
        test("###Persistence\n" +
                "import test::*;\n" +
                "PersistenceContext test::TestPersistenceContext\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  serviceParameters:\n" +
                "  [\n" +
                "    foo='hello',\n" +
                "    bar=1,\n" +
                "    con1=test::TestConnection,\n" +
                "    con2=\n" +
                "    #{\n" +
                "      RelationalDatabaseConnection\n" +
                "      {\n" +
                "        store: test::TestDatabase;\n" +
                "        type: H2;\n" +
                "        specification: LocalH2\n" +
                "        {\n" +
                "        };\n" +
                "        auth: Test;\n" +
                "      }\n" +
                "    }#\n" +
                "  ];\n" +
                "  sinkConnection:\n" +
                "  #{\n" +
                "    RelationalDatabaseConnection\n" +
                "    {\n" +
                "      store: test::TestDatabase;\n" +
                "      type: H2;\n" +
                "      specification: LocalH2\n" +
                "      {\n" +
                "      };\n" +
                "      auth: Test;\n" +
                "    }\n" +
                "  }#;\n" +
                "}\n");
        test("###Persistence\n" +
                "import test::*;\n" +
                "PersistenceContext test::TestPersistenceContext\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  serviceParameters:\n" +
                "  [\n" +
                "    con2=\n" +
                "    #{\n" +
                "      RelationalDatabaseConnection\n" +
                "      {\n" +
                "        store: test::TestDatabase;\n" +
                "        type: H2;\n" +
                "        specification: LocalH2\n" +
                "        {\n" +
                "        };\n" +
                "        auth: Test;\n" +
                "      }\n" +
                "    }#,\n" +
                "    foo='hello',\n" +
                "    bar=1,\n" +
                "    con1=test::TestConnection\n" +
                "  ];\n" +
                "  sinkConnection:\n" +
                "  #{\n" +
                "    RelationalDatabaseConnection\n" +
                "    {\n" +
                "      store: test::TestDatabase;\n" +
                "      type: H2;\n" +
                "      specification: LocalH2\n" +
                "      {\n" +
                "      };\n" +
                "      auth: Test;\n" +
                "    }\n" +
                "  }#;\n" +
                "}\n");
    }
}
