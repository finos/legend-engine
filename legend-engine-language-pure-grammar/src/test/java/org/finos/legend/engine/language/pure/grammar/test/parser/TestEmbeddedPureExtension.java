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
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.domain.DomainParserGrammar;
import org.finos.legend.engine.language.pure.grammar.test.TestGrammarParser;
import org.finos.legend.engine.language.pure.grammar.test.roundtrip.embedded.extensions.NewValueSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.valueSpecification.ValueSpecification;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class TestEmbeddedPureExtension extends TestGrammarParser.TestGrammarParserTestSuite
{
    @Override
    public Vocabulary getParserGrammarVocabulary()
    {
        return DomainParserGrammar.VOCABULARY;
    }

    @Override
    public String getParserGrammarIdentifierInclusionTestCode(List<String> keywords)
    {
        return null;
    }

    @Test
    public void testUnknownExtension()
    {
        test("function f(): Any[*]\n" +
                "{\n" +
                "   let x = #Unknown{My random Parser #Test{ OK OK } Yo}#\n" +
                "}\n", "PARSER error at [6:12-56]: Can't find an embedded Pure parser for the type 'Unknown' available ones: [Test]");
    }

    @Test
    public void testParseValueSpecification()
    {
        ValueSpecification vs = PureGrammarParser.newInstance().parseValueSpecification("#Test{My random Parser #Test{ OK OK } Yo}#", "", 1, 1, false);
        NewValueSpecification nv = (NewValueSpecification)vs;
        Assert.assertEquals("My random Parser #Test{ OK OK } Yo", nv.x);
    }

}
