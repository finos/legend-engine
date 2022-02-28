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
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.pureCollection.PureCollectionDataLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.embedded.pureCollection.PureCollectionDataParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.EmbeddedDataParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;

public class PureCollectionEmbeddedDataParser implements EmbeddedDataParser
{
    @Override
    public String getType()
    {
        return "PureCollection";
    }

    @Override
    public EmbeddedData parse(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation)
    {
        SourceCodeParserInfo parserInfo = getPureCollectionDataParserInfo(code, walkerSourceInformation, sourceInformation);

        PureCollectionDataParserGrammar.DefinitionContext ctx = (PureCollectionDataParserGrammar.DefinitionContext) parserInfo.rootContext;
        return new PureCollectionDataParseTreeWalker(walkerSourceInformation, sourceInformation).visit(ctx);
    }

    private static SourceCodeParserInfo getPureCollectionDataParserInfo(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation)
    {
        CharStream input = CharStreams.fromString(code);
        ParserErrorListener errorListener = new ParserErrorListener(walkerSourceInformation);
        PureCollectionDataLexerGrammar lexer = new PureCollectionDataLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        PureCollectionDataParserGrammar parser = new PureCollectionDataParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(code, input, sourceInformation, walkerSourceInformation, lexer, parser, parser.definition());
    }
}
