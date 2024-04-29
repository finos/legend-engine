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

package org.finos.legend.engine.language.pure.grammar.from.data.contentPattern;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.serviceStore.contentPattern.equalToJson.EqualToJsonContentPatternLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.serviceStore.contentPattern.equalToJson.EqualToJsonContentPatternParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.ContentPattern;
import org.finos.legend.engine.protocol.pure.v1.model.data.contentPattern.EqualToJsonPattern;

public class EqualToJsonContentPatternGrammarParser implements ContentPatternGrammarParser
{
    public static final String TYPE = "EqualToJson";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public ContentPattern parse(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation, PureGrammarParserExtensions extensions)
    {
        SourceCodeParserInfo parserInfo = getEqualToJsonContentPatternParserInfo(code, walkerSourceInformation, sourceInformation);

        EqualToJsonContentPatternParserGrammar.DefinitionContext ctx = (EqualToJsonContentPatternParserGrammar.DefinitionContext) parserInfo.rootContext;
        EqualToJsonContentPatternParserGrammar.ExpectedDefinitionContext expectedDefinitionContext = ctx.expectedDefinition();

        EqualToJsonPattern result = new EqualToJsonPattern();
        result.expectedValue = PureGrammarParserUtility.fromGrammarString(expectedDefinitionContext.STRING().getText(), false);

        return result;
    }

    private static SourceCodeParserInfo getEqualToJsonContentPatternParserInfo(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation)
    {
        CharStream input = CharStreams.fromString(code);
        ParserErrorListener errorListener = new ParserErrorListener(walkerSourceInformation);
        EqualToJsonContentPatternLexerGrammar lexer = new EqualToJsonContentPatternLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        EqualToJsonContentPatternParserGrammar parser = new EqualToJsonContentPatternParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(code, input, sourceInformation, walkerSourceInformation, lexer, parser, parser.definition());
    }
}
