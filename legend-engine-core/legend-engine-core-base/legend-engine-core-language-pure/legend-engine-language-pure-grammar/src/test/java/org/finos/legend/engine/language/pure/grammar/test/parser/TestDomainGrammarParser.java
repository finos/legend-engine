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

package org.finos.legend.engine.language.pure.grammar.test.parser;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.domain.DomainParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

import java.util.List;

public class TestDomainGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return DomainParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "Class " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "{\n" +
                "}\n";
    }

    @Test
    public void testGraphFetchTreeWithSubtypeTreeAtPropertyLevel()
    {
        String nestedSubTypeTree =  "#{\n" +
                                    "    test::Firm{\n" +
                                    "      legalName,\n" +
                                    "      ->subType(@FirmSubType){\n" +
                                    "        subTypeName,\n"  +
                                    "        ->subType(@FirmSubSubType){\n" +
                                    "           subSubTypeName\n" +
                                    "        }\n" +
                                    "      }\n" +
                                    "    }\n" +
                                    "  }#\n";
        String code1 = "function my::test(): Any[*]\n{\n   " + nestedSubTypeTree.replace("\n", "").replace(" ", "") + "\n}\n";
        test(code1,  "PARSER error at [3:50-105]: ->subType() is supported only at root level");

        String subTypeTreeInsidePropertyTre =   "#{\n" +
                                                "    test::Firm{\n" +
                                                "      legalName,\n" +
                                                "      Address{\n" +
                                                "      ->subType(@Street){\n" +
                                                "        streetName\n"  +
                                                "        }\n" +
                                                "      }\n" +
                                                "    }\n" +
                                                "  }#\n";
        String code2 = "function my::test(): Any[*]\n{\n   " + subTypeTreeInsidePropertyTre.replace("\n", "").replace(" ", "") + "\n}\n";
        test(code2,  "PARSER error at [3:34-65]: ->subType() is supported only at root level");

        String emptySubTypeTreesAtRootLevel = "  #{\n" +
                                              "    test::Address {\n" +
                                              "      zipCode,\n" +
                                              "      ->subType(@test::Street) {\n" +
                                              "      }\n" +
                                              "    }\n" +
                                              "  }#\n";
        String code4 = "function my::test(): Any[*]\n{\n   " + emptySubTypeTreesAtRootLevel.replace("\n", "").replace(" ", "") + "\n}\n";
        test(code4,  "PARSER error at [3:53]: Unexpected token '}'");
    }
}
