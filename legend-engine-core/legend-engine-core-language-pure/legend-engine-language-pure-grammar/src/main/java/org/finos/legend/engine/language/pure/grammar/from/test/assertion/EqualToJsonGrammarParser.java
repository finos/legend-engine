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
import org.finos.legend.engine.language.pure.grammar.from.antlr4.test.assertion.equalToJson.EqualToJsonAssertionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.test.assertion.equalToJson.EqualToJsonAssertionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.data.embedded.HelperEmbeddedDataGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.language.pure.grammar.from.extension.test.assertion.TestAssertionParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.data.ExternalFormatData;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.EqualToJson;
import org.finos.legend.engine.protocol.pure.v1.model.test.assertion.TestAssertion;

public class EqualToJsonGrammarParser implements TestAssertionParser
{
    public static final String TYPE = "EqualToJson";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public TestAssertion parse(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation, PureGrammarParserExtensions extensions)
    {
        SourceCodeParserInfo parserInfo = getEqualToJsonAssertionParserInfo(code, walkerSourceInformation, sourceInformation);

        EqualToJsonAssertionParserGrammar.DefinitionContext ctx = (EqualToJsonAssertionParserGrammar.DefinitionContext) parserInfo.rootContext;
        EqualToJsonAssertionParserGrammar.ExpectedDefinitionContext expectedDefinitionContext = ctx.expectedDefinition();

        EqualToJson result = new EqualToJson();
        result.sourceInformation = sourceInformation;
        result.expected = (ExternalFormatData) HelperEmbeddedDataGrammarParser.parseEmbeddedData(expectedDefinitionContext.embeddedData(), walkerSourceInformation, extensions);

        return result;
    }

    private static SourceCodeParserInfo getEqualToJsonAssertionParserInfo(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation)
    {
        CharStream input = CharStreams.fromString(code);
        ParserErrorListener errorListener = new ParserErrorListener(walkerSourceInformation);
        EqualToJsonAssertionLexerGrammar lexer = new EqualToJsonAssertionLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        EqualToJsonAssertionParserGrammar parser = new EqualToJsonAssertionParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(code, input, sourceInformation, walkerSourceInformation, lexer, parser, parser.definition());
    }
}
