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

package org.finos.legend.engine.language.pure.grammar.from.test.assertion;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.misc.Interval;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.test.assertion.equalTo.EqualToAssertionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.test.assertion.equalTo.EqualToAssertionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.language.pure.grammar.from.extension.test.assertion.TestAssertionParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.EqualTo;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;

public class EqualToGrammarParser implements TestAssertionParser
{
    public static final String TYPE = "EqualTo";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public TestAssertion parse(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation, PureGrammarParserExtensions extensions)
    {
        SourceCodeParserInfo parserInfo = getEqualToAssertionParserInfo(code, walkerSourceInformation, sourceInformation);

        EqualToAssertionParserGrammar.DefinitionContext ctx = (EqualToAssertionParserGrammar.DefinitionContext) parserInfo.rootContext;
        EqualToAssertionParserGrammar.PrimitiveValueContext primitiveValueContext = ctx.expectedDefinition().primitiveValue();

        DomainParser parser = new DomainParser();
        int startLine = primitiveValueContext.getStart().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + primitiveValueContext.getStart().getCharPositionInLine();
        ParseTreeWalkerSourceInformation serviceParamSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).build();
        String expectedValue = primitiveValueContext.start.getInputStream().getText(Interval.of(primitiveValueContext.start.getStartIndex(), primitiveValueContext.stop.getStopIndex()));

        EqualTo result = new EqualTo();
        result.sourceInformation = sourceInformation;
        result.expected = parser.parsePrimitiveValue(expectedValue, serviceParamSourceInformation, null);

        return result;
    }

    private static SourceCodeParserInfo getEqualToAssertionParserInfo(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation)
    {
        CharStream input = CharStreams.fromString(code);
        ParserErrorListener errorListener = new ParserErrorListener(walkerSourceInformation);
        EqualToAssertionLexerGrammar lexer = new EqualToAssertionLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        EqualToAssertionParserGrammar parser = new EqualToAssertionParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(code, input, sourceInformation, walkerSourceInformation, lexer, parser, parser.definition());
    }
}
