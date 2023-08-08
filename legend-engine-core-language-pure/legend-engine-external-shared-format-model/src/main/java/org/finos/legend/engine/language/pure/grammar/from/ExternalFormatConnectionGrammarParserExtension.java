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

package org.finos.legend.engine.language.pure.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ExternalFormatConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ExternalFormatConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.externalSource.ExternalSourceSpecificationLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.externalSource.ExternalSourceSpecificationParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionValueSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.extension.ConnectionValueParser;
import org.finos.legend.engine.language.pure.grammar.from.externalSource.ExternalSourceSpecificationParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.externalSource.ExternalSourceSpecificationSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalFormatConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.externalFormat.ExternalSource;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class ExternalFormatConnectionGrammarParserExtension implements IExternalFormatGrammarParserExtension
{
    @Override
    public Iterable<? extends ConnectionValueParser> getExtraConnectionParsers()
    {
        return Collections.singletonList(ConnectionValueParser.newParser(ExternalFormatGrammarParserExtension.EXTERNAL_FORMAT_CONNECTION_TYPE, connectionValueSourceCode ->
        {
            SourceCodeParserInfo parserInfo = getExternalFormatConnectionParserInfo(connectionValueSourceCode);
            ExternalFormatConnectionParseTreeWalker walker = new ExternalFormatConnectionParseTreeWalker(parserInfo.walkerSourceInformation);
            ExternalFormatConnection connectionValue = new ExternalFormatConnection();
            connectionValue.sourceInformation = connectionValueSourceCode.sourceInformation;
            walker.visitExternalFormatConnectionValue((ExternalFormatConnectionParserGrammar.DefinitionContext) parserInfo.rootContext, connectionValue, connectionValueSourceCode.isEmbedded);
            return connectionValue;
        }));
    }

    private static SourceCodeParserInfo getExternalFormatConnectionParserInfo(ConnectionValueSourceCode connectionValueSourceCode)
    {
        CharStream input = CharStreams.fromString(connectionValueSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(connectionValueSourceCode.walkerSourceInformation);
        ExternalFormatConnectionLexerGrammar lexer = new ExternalFormatConnectionLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        ExternalFormatConnectionParserGrammar parser = new ExternalFormatConnectionParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(connectionValueSourceCode.code, input, connectionValueSourceCode.sourceInformation, connectionValueSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }

    @Override
    public List<Function<ExternalSourceSpecificationSourceCode, ExternalSource>> getExtraExternalSourceSpecificationParsers()
    {
        return Collections.singletonList(code ->
        {
            ExternalSourceSpecificationParseTreeWalker walker = new ExternalSourceSpecificationParseTreeWalker();

            if ("UrlStream".equals(code.getType()))
            {
                return parseDataSourceSpecification(code, p -> walker.visitUrlStreamExternalSourceSpecification(code, p.urlStreamExternalSourceSpecification()));
            }
            return null;
        });
    }

    private ExternalSource parseDataSourceSpecification(ExternalSourceSpecificationSourceCode code, Function<ExternalSourceSpecificationParserGrammar, ExternalSource> func)
    {
        CharStream input = CharStreams.fromString(code.getCode());
        ParserErrorListener errorListener = new ParserErrorListener(code.getWalkerSourceInformation());
        ExternalSourceSpecificationLexerGrammar lexer = new ExternalSourceSpecificationLexerGrammar(input);
        ExternalSourceSpecificationParserGrammar parser = new ExternalSourceSpecificationParserGrammar(new CommonTokenStream(lexer));

        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return func.apply(parser);
    }
}
