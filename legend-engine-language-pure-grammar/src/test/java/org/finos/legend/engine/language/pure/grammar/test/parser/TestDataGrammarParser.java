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

package org.finos.legend.engine.language.pure.grammar.test.parser;

import org.antlr.v4.runtime.Vocabulary;
import org.eclipse.collections.impl.list.mutable.ListAdapter;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.DataParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.junit.Test;

import java.util.List;

public class TestDataGrammarParser extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return DataParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return "###Data\n" +
                "Data " + ListAdapter.adapt(keywords).makeString("::") + "\n" +
                "Text #{\n" +
                "  contentType: 'test';\n" +
                "  data: 'test';\n" +
                "}#\n";
    }

    @Test
    public void testMissingContentType()
    {
        test("###Data\n" +
                        "Data <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'something'} meta::data::MyData\n" +
                        "Binary #{\n" +
                        "  data: '1B4A 9DEA 230F FF20';\n" +
                        "}#\n",
                "PARSER error at [3:1-5:2]: Field 'contentType' is required"
        );

        test("###Data\n" +
                        "Data <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'something'} meta::data::MyData\n" +
                        "Text #{\n" +
                        "  data: '{\"some\":\"data\"}';\n" +
                        "}#\n",
                "PARSER error at [3:1-5:2]: Field 'contentType' is required"
        );
    }

    @Test
    public void testMissingData()
    {
        test("###Data\n" +
                        "Data <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'something'} meta::data::MyData\n" +
                        "Binary #{\n" +
                        "  contentType: 'application/x-protobuf';\n" +
                        "}#\n",
                "PARSER error at [3:1-5:2]: Field 'data' is required"
        );

        test("###Data\n" +
                        "Data <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'something'} meta::data::MyData\n" +
                        "Text #{\n" +
                        "  contentType: 'application/x-protobuf';\n" +
                        "}#\n",
                "PARSER error at [3:1-5:2]: Field 'data' is required"
        );

        test("###Data\n" +
                        "Data <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'something'} meta::data::MyData\n" +
                        "PureCollection #{\n" +
                        "}#\n",
                "PARSER error at [3:1-4:2]: Field 'data' is required"
        );
    }

    @Test
    public void testNonHexBinaryData()
    {
        test("###Data\n" +
                        "Data <<meta::pure::profiles::typemodifiers.abstract>> {doc.doc = 'something'} meta::data::MyData\n" +
                        "Binary #{\n" +
                        "  contentType: 'application/x-protobuf';\n" +
                        "  data: '1BXX 9DEA 230F FF20';\n" +
                        "}#\n",
                "PARSER error at [4:16-39]: Invalid hex data"
        );
    }
}
