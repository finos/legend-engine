// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.grammar.integration;

import org.antlr.v4.runtime.Vocabulary;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.schema.MongoDBSchemaParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;

import java.util.List;

public class TestMongoDBServiceGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{

    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return MongoDBSchemaParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Service\n" +
                "Service test::currentService\n" +
                "{\n" +
                "  pattern: 'url/myUrl/';\n" +
                "  owners:\n" +
                "  [\n" +
                "    'ownerName'\n" +
                "  ];\n" +
                "  documentation: 'test';\n" +
                "  autoActivateUpdates: true;\n" +
                "  execution: Single\n" +
                "  {\n" +
                "    query: |test::Person.all()->graphFetch(#{test::Person{firstName}}#)->serialize(#{test::Person{firstName}}#);\n" +
                "    mapping: test::mapping;\n" +
                "    runtime: test::runtime;\n" +
                "  }\n" +
                "  testSuites:\n" +
                "  [\n" +
                "    testSuite1:\n" +
                "    {\n" +
                "      tests:\n" +
                "      [\n" +
                "        test1:\n" +
                "        {\n" +
                "          asserts:\n" +
                "          [\n" +
                "            assert1:\n" +
                "              EqualToJson\n" +
                "              #{\n" +
                "                expected : \n" +
                "                  ExternalFormat\n" +
                "                  #{\n" +
                "                    contentType: 'application/json';\n" +
                "                    data: '{\"employees\":[{\"firstName\":\"firstName 36\",\"lastName\":\"lastName 77\"}],\"legalName\":\"legalName 19\"}';\n" +
                "                  }#;\n" +
                "              }#\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  ]\n" +
                "}\n";
    }
}

