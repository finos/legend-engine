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

import java.util.function.Consumer;
import java.util.function.Function;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.from.*;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ElasticsearchLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ElasticsearchParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ElasticsearchConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ElasticsearchConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionValueSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.extension.ConnectionValueParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.protocol.store.elasticsearch.v7.metamodel.runtime.Elasticsearch7StoreConnection;

public class ElasticsearchGrammarParserExtension implements PureGrammarParserExtension
{
    public static final String NAME = "Elasticsearch";
    public static final String V7_CONNECTION_TYPE_NAME = "Elasticsearch7ClusterConnection";
    private final Iterable<SectionParser> sectionParser;
    private final Iterable<ConnectionValueParser> connectionValueParser;

    public ElasticsearchGrammarParserExtension()
    {
        this.sectionParser = Lists.immutable.with(SectionParser.newParser(NAME, this::parseSection));
        this.connectionValueParser = Lists.immutable.with(ConnectionValueParser.newParser(V7_CONNECTION_TYPE_NAME, this::parseConnection));
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Elastic");
    }

    @Override
    public Iterable<? extends SectionParser> getExtraSectionParsers()
    {
        return this.sectionParser;
    }

    @Override
    public Iterable<? extends ConnectionValueParser> getExtraConnectionParsers()
    {
        return this.connectionValueParser;
    }

    private Section parseSection(SectionSourceCode sectionSourceCode, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext pureGrammarParserContext)
    {
        SourceCodeParserInfo parserInfo = this.getDefinitionSectionParserInfo(sectionSourceCode.code, sectionSourceCode.walkerSourceInformation, sectionSourceCode.sourceInformation);
        ElasticsearchStoreParseTreeWalker walker = new ElasticsearchStoreParseTreeWalker(pureGrammarParserContext.getPureGrammarParserExtensions(), parserInfo);
        return walker.visit(sectionSourceCode.sectionType, elementConsumer, (ElasticsearchParserGrammar.DefinitionContext) parserInfo.rootContext);
    }

    private SourceCodeParserInfo getDefinitionSectionParserInfo(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation)
    {
        CharStream input = CharStreams.fromString(code);

        ElasticsearchLexerGrammar lexer = new ElasticsearchLexerGrammar(input);
        ElasticsearchParserGrammar parser = new ElasticsearchParserGrammar(new CommonTokenStream(lexer));

        ParserErrorListener errorListener = new ParserErrorListener(walkerSourceInformation, ElasticsearchLexerGrammar.VOCABULARY);
        lexer.addErrorListener(errorListener);
        parser.addErrorListener(errorListener);

        return new SourceCodeParserInfo(code, input, sourceInformation, walkerSourceInformation, lexer, parser, parser.definition());
    }

    private Elasticsearch7StoreConnection parseConnection(ConnectionValueSourceCode connectionValueSourceCode, PureGrammarParserExtensions extension)
    {
        SourceCodeParserInfo parserInfo = this.getConnectionSectionParserInfo(connectionValueSourceCode.code, connectionValueSourceCode.walkerSourceInformation, connectionValueSourceCode.sourceInformation, ElasticsearchConnectionParserGrammar::v7ConnectionDefinition);
        ElasticsearchStoreParseTreeWalker walker = new ElasticsearchStoreParseTreeWalker(extension, parserInfo);
        return walker.visit((ElasticsearchConnectionParserGrammar.V7ConnectionDefinitionContext) parserInfo.rootContext);
    }

    private SourceCodeParserInfo getConnectionSectionParserInfo(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation, Function<ElasticsearchConnectionParserGrammar, ParserRuleContext> rootContext)
    {
        CharStream input = CharStreams.fromString(code);

        ElasticsearchConnectionLexerGrammar lexer = new ElasticsearchConnectionLexerGrammar(input);
        ElasticsearchConnectionParserGrammar parser = new ElasticsearchConnectionParserGrammar(new CommonTokenStream(lexer));

        ParserErrorListener errorListener = new ParserErrorListener(walkerSourceInformation, ElasticsearchConnectionLexerGrammar.VOCABULARY);
        lexer.addErrorListener(errorListener);
        parser.addErrorListener(errorListener);

        return new SourceCodeParserInfo(code, input, sourceInformation, walkerSourceInformation, lexer, parser, rootContext.apply(parser));
    }
}
