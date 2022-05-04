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

package org.finos.legend.engine.language.pure.grammar.from.data;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.serviceStore.ServiceStoreEmbeddedDataLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.serviceStore.ServiceStoreEmbeddedDataParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.language.pure.grammar.from.extension.data.EmbeddedDataParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;

public class ServiceStoreEmbeddedDataParser implements EmbeddedDataParser
{
    public static final String TYPE = "ServiceStore";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public EmbeddedData parse(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation, PureGrammarParserExtensions extensions)
    {
        SourceCodeParserInfo parserInfo = getServiceStoreDataParserInfo(code, walkerSourceInformation, sourceInformation);

        ServiceStoreEmbeddedDataParserGrammar.DefinitionContext ctx = (ServiceStoreEmbeddedDataParserGrammar.DefinitionContext) parserInfo.rootContext;
        return new ServiceStoreEmbeddedDataParseTreeWalker(walkerSourceInformation, sourceInformation, extensions).visit(ctx);
    }

    private static SourceCodeParserInfo getServiceStoreDataParserInfo(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation)
    {
        CharStream input = CharStreams.fromString(code);
        ParserErrorListener errorListener = new ParserErrorListener(walkerSourceInformation);
        ServiceStoreEmbeddedDataLexerGrammar lexer = new ServiceStoreEmbeddedDataLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        ServiceStoreEmbeddedDataParserGrammar parser = new ServiceStoreEmbeddedDataParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(code, input, sourceInformation, walkerSourceInformation, lexer, parser, parser.definition());
    }
}
