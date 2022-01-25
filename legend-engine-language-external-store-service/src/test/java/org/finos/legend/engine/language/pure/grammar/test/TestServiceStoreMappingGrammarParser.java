// Copyright 2021 Goldman Sachs
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
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ServiceStoreParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
import org.junit.Test;

import java.util.List;

public class TestServiceStoreMappingGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return MappingParserGrammar.VOCABULARY;
    }

    @Override
    public List<Vocabulary> getDelegatedParserGrammarVocabulary()
    {
        return FastList.newListWith(
                ServiceStoreParserGrammar.VOCABULARY
        );
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Mapping\n" +
                "Mapping " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "(\n" +
                "  test::Person: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::TradeProductServiceStore] TestServiceGroup.TestService\n" +
                "    (\n" +
                "       prop: $service.parameters.param\n" +
                "    )\n" +
                "  }\n" +
                ")\n";
    }

    @Test
    public void testMappingInheritance()
    {
        // With extends
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  test::Class[id1] extends [id2]: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup1.TestServiceGroup2.TestService\n" +
                "    (\n" +
                "       prop: $service.parameters.param\n" +
                "    )\n" +
                "  }\n" +
                ")\n", "PARSER error at [4:29-31]: Service Store Mapping does not support extends");
    }

    @Test
    public void testServiceStoreMappingGrammarErrorMessages()
    {
        //Service Store Missing
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service TestServiceGroup.TestService\n" +
                "    (\n" +
                "      prop1 : $service.parameters.param1\n" +
                "    )\n" +
                "  }\n" +
                ")\n", "PARSER error at [6:14-29]: Unexpected token");

        //Service Name Missing
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.\n" +
                "    (\n" +
                "      prop1 : $service.parameters.param1\n" +
                "    )\n" +
                "  }\n" +
                ")\n", "PARSER error at [7:5]: Unexpected token");

        //Incorrect parameter reference
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup.TestServices\n" +
                "    (\n" +
                "      prop1 : $service.param1\n" +
                "    )\n" +
                "  }\n" +
                ")\n", "PARSER error at [8:24-29]: Unexpected token");
    }

    @Test
    public void testServiceStoreMappingPathOffset()
    {
        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup1.TestServiceGroup2.TestService\n" +
                "    (\n" +
                "      ~path response.pathProperty\n" +
                "    )\n" +
                "  }\n" +
                ")\n", "PARSER error at [8:13-20]: Unexpected token");

        test("###Mapping\n" +
                "Mapping test::mapping\n" +
                "(\n" +
                "  *test::model: ServiceStore\n" +
                "  {\n" +
                "    ~service [test::ServiceStore] TestServiceGroup1.TestServiceGroup2.TestService\n" +
                "    (\n" +
                "      ~path \n" +
                "    )\n" +
                "  }\n" +
                ")\n", "PARSER error at [9:5]: Unexpected token");
    }
}
