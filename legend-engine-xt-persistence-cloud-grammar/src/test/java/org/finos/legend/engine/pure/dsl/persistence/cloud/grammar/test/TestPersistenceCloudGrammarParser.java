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

package org.finos.legend.engine.pure.dsl.persistence.cloud.grammar.test;

import org.antlr.v4.runtime.Vocabulary;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceCloudParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

import java.util.List;

public class TestPersistenceCloudGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return PersistenceCloudParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  platform: AwsGlue\n" +
                "  #{\n" +
                "    dataProcessingUnits: 10;\n" +
                "  }#;\n" +
                "}\n";
    }

    /**********
     * data processing units
     **********/

    @Test
    public void dataProcessingUnits()
    {
        test("###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  platform: AwsGlue;\n" +
                "}\n", "PARSER error at [6:3-20]: Persistence platform 'AwsGlue' must have a non-empty body");
        test("###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  platform: AwsGlue\n" +
                "  #{\n" +
                "  }#;\n" +
                "}\n", "PARSER error at [2:1-5]: Field 'dataProcessingUnits' is required");
        test("###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  platform: AwsGlue\n" +
                "  #{\n" +
                "    dataProcessingUnits: 10;\n" +
                "  }#;\n" +
                "}\n");
        test("###Persistence\n" +
                "\n" +
                "PersistenceContext test::TestPersistenceContext\n" +
                "{\n" +
                "  persistence: test::TestPersistence;\n" +
                "  platform: AwsGlue\n" +
                "  #{\n" +
                "    dataProcessingUnits: 10;\n" +
                "    dataProcessingUnits: 10;\n" +
                "  }#;\n" +
                "}\n", "PARSER error at [2:5-4:5]: Field 'dataProcessingUnits' should be specified only once");
    }
}
