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

package org.finos.legend.engine.language.pure.grammar.from.data.embedded;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.text.TextDataLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.text.TextDataParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.EmbeddedDataParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.pure.v1.model.data.TextData;

public class TextEmbeddedDataParser implements EmbeddedDataParser
{
    @Override
    public String getType()
    {
        return "Text";
    }

    @Override
    public EmbeddedData parse(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation)
    {
        SourceCodeParserInfo parserInfo = getTextDataParserInfo(code, walkerSourceInformation, sourceInformation);

        TextDataParserGrammar.DefinitionContext ctx = (TextDataParserGrammar.DefinitionContext) parserInfo.rootContext;
        TextDataParserGrammar.ContentTypeContext contentTypeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.contentType(), "contentType", sourceInformation);
        TextDataParserGrammar.DataContext dataTypeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.data(), "data", sourceInformation);

        TextData result = new TextData();
        result._type = getType();
        result.sourceInformation = sourceInformation;
        result.contentType = PureGrammarParserUtility.fromGrammarString(contentTypeContext.STRING().getText(), true);
        result.data = PureGrammarParserUtility.fromGrammarString(dataTypeContext.STRING().getText(), true);
        return result;
    }

    private static SourceCodeParserInfo getTextDataParserInfo(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation)
    {
        CharStream input = CharStreams.fromString(code);
        ParserErrorListener errorListener = new ParserErrorListener(walkerSourceInformation);
        TextDataLexerGrammar lexer = new TextDataLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        TextDataParserGrammar parser = new TextDataParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(code, input, sourceInformation, walkerSourceInformation, lexer, parser, parser.definition());
    }
}
