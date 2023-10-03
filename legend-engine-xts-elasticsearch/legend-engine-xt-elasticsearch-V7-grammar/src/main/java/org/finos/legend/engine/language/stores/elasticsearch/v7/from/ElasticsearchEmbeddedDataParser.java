// Copyright 2023 Goldman Sachs
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
//

package org.finos.legend.engine.language.stores.elasticsearch.v7.from;

import java.util.stream.Collectors;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.ElasticsearchEmbeddedDataLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.data.ElasticsearchEmbeddedDataParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.language.pure.grammar.from.extension.data.EmbeddedDataParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.data.EmbeddedData;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.data.ElasticsearchV7EmbeddedData;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.store.data.ElasticsearchV7IndexEmbeddedData;

public class ElasticsearchEmbeddedDataParser implements EmbeddedDataParser
{
    public static final String TYPE = "Elasticsearch";

    @Override
    public String getType()
    {
        return TYPE;
    }

    @Override
    public EmbeddedData parse(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation, PureGrammarParserExtensions extensions)
    {
        SourceCodeParserInfo parserInfo = getParserInfo(code, walkerSourceInformation, sourceInformation);
        ElasticsearchEmbeddedDataParserGrammar.DefinitionContext ctx = (ElasticsearchEmbeddedDataParserGrammar.DefinitionContext) parserInfo.rootContext;
        return this.parse(sourceInformation, extensions, ctx);
    }

    private ElasticsearchV7EmbeddedData parse(SourceInformation sourceInformation, PureGrammarParserExtensions extensions, ElasticsearchEmbeddedDataParserGrammar.DefinitionContext ctx)
    {
        ElasticsearchV7EmbeddedData data = new ElasticsearchV7EmbeddedData();
        data.sourceInformation = sourceInformation;
        data.indexData = ctx.indexData().stream().map(this::visit).collect(Collectors.toList());
        return data;
    }

    private ElasticsearchV7IndexEmbeddedData visit(ElasticsearchEmbeddedDataParserGrammar.IndexDataContext indexDataContext)
    {
        ElasticsearchV7IndexEmbeddedData indexEmbeddedData = new ElasticsearchV7IndexEmbeddedData();
        indexEmbeddedData.index = PureGrammarParserUtility.fromIdentifier(indexDataContext.indexName());
        indexEmbeddedData.documentsAsJson = indexDataContext.indexJsonData().getText();
        return indexEmbeddedData;
    }

    private static SourceCodeParserInfo getParserInfo(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation)
    {
        CharStream input = CharStreams.fromString(code);
        ParserErrorListener errorListener = new ParserErrorListener(walkerSourceInformation, ElasticsearchEmbeddedDataLexerGrammar.VOCABULARY);
        ElasticsearchEmbeddedDataLexerGrammar lexer = new ElasticsearchEmbeddedDataLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        ElasticsearchEmbeddedDataParserGrammar parser = new ElasticsearchEmbeddedDataParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(code, input, sourceInformation, walkerSourceInformation, lexer, parser, parser.definition());
    }
}
