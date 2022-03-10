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
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.binary.BinaryDataLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.binary.BinaryDataParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.EmbeddedDataParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.data.BinaryData;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

public class BinaryEmbeddedDataParser implements EmbeddedDataParser
{
    @Override
    public String getType()
    {
        return "Binary";
    }

    @Override
    public EmbeddedData parse(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation)
    {
        SourceCodeParserInfo parserInfo = getBinaryDataParserInfo(code, walkerSourceInformation, sourceInformation);

        BinaryDataParserGrammar.DefinitionContext ctx = (BinaryDataParserGrammar.DefinitionContext) parserInfo.rootContext;
        BinaryDataParserGrammar.ContentTypeContext contentTypeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.contentType(), "contentType", sourceInformation);
        BinaryDataParserGrammar.DataContext dataTypeContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.data(), "data", sourceInformation);

        BinaryData result = new BinaryData();
        result._type = getType();
        result.sourceInformation = sourceInformation;
        result.contentType = PureGrammarParserUtility.fromGrammarString(contentTypeContext.STRING().getText(), true);
        result.hexData = toHex(PureGrammarParserUtility.fromGrammarString(dataTypeContext.STRING().getText(), true), walkerSourceInformation.getSourceInformation(contentTypeContext.STRING().getSymbol()));
        return result;
    }

    private static String toHex(String s, SourceInformation sourceInformation)
    {
        StringBuilder builder = new StringBuilder();
        s.chars().forEach(ch ->
        {
            if ((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F'))
            {
                builder.append((char) ch);
            }
            else if (ch >= 'a' && ch <= 'f')
            {
                builder.append(Character.toUpperCase((char) ch));
            }
            else if (!Character.isWhitespace((char) ch))
            {
                throw new EngineException("Invalid hex data", sourceInformation, EngineErrorType.PARSER);
            }
        });
        return builder.toString();
    }

    private static SourceCodeParserInfo getBinaryDataParserInfo(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation)
    {
        CharStream input = CharStreams.fromString(code);
        ParserErrorListener errorListener = new ParserErrorListener(walkerSourceInformation);
        BinaryDataLexerGrammar lexer = new BinaryDataLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        BinaryDataParserGrammar parser = new BinaryDataParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(code, input, sourceInformation, walkerSourceInformation, lexer, parser, parser.definition());
    }
}
