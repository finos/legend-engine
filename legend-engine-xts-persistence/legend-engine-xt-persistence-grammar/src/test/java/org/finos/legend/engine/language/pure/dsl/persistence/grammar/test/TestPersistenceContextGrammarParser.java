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

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

import java.util.List;

public class TestPersistenceContextGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return PersistenceParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Persistence\n" +
                "\n" +
                "PersistenceContext " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "}\n" +
                "\n" +
                "Persistence test::TestPersistence\n" +
                "{\n" +
                "  doc: 'This is test documentation.';\n" +
                "  trigger: Manual;\n" +
                "  service: test::Service;\n" +
                "  persister: Batch\n" +
                "  {\n" +
                "    sink: Relational\n" +
                "    {\n" +
                "      database: test::Database;\n" +
                "    }\n" +
                "    targetShape: Flat\n" +
                "    {\n" +
                "      targetName: 'TestDataset1';\n" +
                "      modelClass: test::ModelClass;\n" +
                "    }\n" +
                "    ingestMode: AppendOnly\n" +
                "    {\n" +
                "      auditing: None;\n" +
                "      filterDuplicates: false;\n" +
                "    }\n" +
                "  }\n" +
                "}\n";
    }

    /**********
     * persistence context
     **********/

    @Test
    public void persistenceContextPersistence()
    {
        test("###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext \n" +
                "{\n" +
                "}\n", "PARSER error at [3:1-5:1]: Field 'persistence' is required");

        test("###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext \n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  persistence: test::TestPersistence;\n" +
                "}\n", "PARSER error at [3:1-7:1]: Field 'persistence' should be specified only once");
    }

    @Test
    public void persistenceContextPersistencePlatform()
    {
        test("###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext \n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "}\n");
        test("###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext \n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  platform: Default;\n" +
                "}\n");
        test("###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext \n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  platform: Unknown;\n" +
                "}\n", "PARSER error at [6:13-19]: Unsupported persistence platform type 'Unknown'");
        test("###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext \n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  platform: Default;\n" +
                "  platform: Default;\n" +
                "}\n", "PARSER error at [3:1-8:1]: Field 'platform' should be specified only once");
    }

    @Test
    public void persistenceContextServiceParameters()
    {
        test("###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext \n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "}\n");

        test("###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext \n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  serviceParameters: [foo='hello',bar=1];\n" +
                "  serviceParameters: [foo='hello',bar=1];\n" +
                "}\n", "PARSER error at [3:1-8:1]: Field 'serviceParameters' should be specified only once");
    }

    @Test
    public void persistenceContextSinkConnection()
    {
        test("###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext \n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "}\n");

        test("###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext \n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  serviceParameters: [foo='hello',bar=1];\n" +
                "  sinkConnection: #{\n" +
                "    RelationalDatabaseConnection {\n" +
                "      store: test::TestDatabase;\n" +
                "      type: H2;\n" +
                "      specification: LocalH2{};\n" +
                "      auth: Test;\n" +
                "    }\n" +
                "  }#;\n" +
                "  sinkConnection: #{\n" +
                "    RelationalDatabaseConnection {\n" +
                "      store: test::TestDatabase;\n" +
                "      type: H2;\n" +
                "      specification: LocalH2{};\n" +
                "      auth: Test;\n" +
                "    }\n" +
                "  }#;\n" +
                "}\n", "PARSER error at [3:1-23:1]: Field 'sinkConnection' should be specified only once");
    }
}
