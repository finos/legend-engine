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
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.test.assertion.assertActiveRows.AssertActiveRowsAssertionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.test.assertion.assertActiveRows.AssertActiveRowsAssertionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.data.embedded.HelperEmbeddedDataGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.language.pure.grammar.from.extension.test.assertion.TestAssertionParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.data.ExternalFormatData;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.AssertActiveRows;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;

public class AssertActiveRowsGrammarParser implements TestAssertionParser
{
    public static final String TYPE = "AssertActiveRows";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public TestAssertion parse(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation, PureGrammarParserExtensions extensions)
    {
        SourceCodeParserInfo parserInfo = getAssertActiveRowsAssertionParserInfo(code, walkerSourceInformation, sourceInformation);

        AssertActiveRowsAssertionParserGrammar.DefinitionContext ctx = (AssertActiveRowsAssertionParserGrammar.DefinitionContext) parserInfo.rootContext;
        AssertActiveRowsAssertionParserGrammar.ExpectedDefinitionContext expectedDefinitionContext = ctx.expectedDefinition();

        AssertActiveRows result = new AssertActiveRows();
        result.sourceInformation = sourceInformation;
        result.expected = (ExternalFormatData) HelperEmbeddedDataGrammarParser.parseEmbeddedData(expectedDefinitionContext.embeddedData(), walkerSourceInformation, extensions);

        return result;
    }

    private static SourceCodeParserInfo getAssertActiveRowsAssertionParserInfo(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation)
    {
        CharStream input = CharStreams.fromString(code);
        ParserErrorListener errorListener = new ParserErrorListener(walkerSourceInformation);
        AssertActiveRowsAssertionLexerGrammar lexer = new AssertActiveRowsAssertionLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        AssertActiveRowsAssertionParserGrammar parser = new AssertActiveRowsAssertionParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(code, input, sourceInformation, walkerSourceInformation, lexer, parser, parser.definition());
    }
}
