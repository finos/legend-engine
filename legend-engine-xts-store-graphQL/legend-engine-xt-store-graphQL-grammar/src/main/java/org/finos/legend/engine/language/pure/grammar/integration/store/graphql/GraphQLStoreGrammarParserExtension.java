// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.integration.store.graphql;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.SectionSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.store.graphql.GraphQLStoreLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.store.graphql.GraphQLStoreParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.store.graphql.connection.GraphQLConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.store.graphql.connection.GraphQLConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.store.graphql.mapping.GraphQLMappingLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.store.graphql.mapping.GraphQLMappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionValueSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.extension.ConnectionValueParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.MappingElementParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.language.pure.grammar.from.mapping.MappingElementSourceCode;
import org.finos.legend.engine.language.pure.grammar.integration.store.graphql.connection.GraphQLConnectionParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.integration.store.graphql.mapping.GraphQLMappingParseTreeWalker;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLClassMapping;
import org.finos.legend.engine.protocol.graphQLStore.metamodel.pure.GraphQLConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;

import java.util.Collections;

public class GraphQLStoreGrammarParserExtension implements PureGrammarParserExtension
{
    public static final String NAME = "GraphQL";
    public static final String GRAPHQL_STORE_MAPPING_ELEMENT_TYPE = "GraphQL";
    public static final String GRAPHQL_CONNECTION_TYPE = "GraphQLConnection";

    @Override
    public MutableList<String> group()
    {
        return Lists.mutable.with("Store", "GraphQL");
    }

    private static SourceCodeParserInfo getGraphQLStoreParserInfo(SectionSourceCode sectionSourceCode)
    {
        CharStream input = CharStreams.fromString(sectionSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(sectionSourceCode.walkerSourceInformation);
        GraphQLStoreLexerGrammar lexer = new GraphQLStoreLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        GraphQLStoreParserGrammar parser = new GraphQLStoreParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(sectionSourceCode.code, input, sectionSourceCode.sourceInformation, sectionSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }

    private static SourceCodeParserInfo getGraphQLConnectionParserInfo(ConnectionValueSourceCode connectionValueSourceCode)
    {
        CharStream input = CharStreams.fromString(connectionValueSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(connectionValueSourceCode.walkerSourceInformation);
        GraphQLConnectionLexerGrammar lexer = new GraphQLConnectionLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        GraphQLConnectionParserGrammar parser = new GraphQLConnectionParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(connectionValueSourceCode.code, input, connectionValueSourceCode.sourceInformation, connectionValueSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }

    public static SourceCodeParserInfo getGraphQLMappingElementParserInfo(MappingElementSourceCode mappingElementSourceCode)
    {
        CharStream input = CharStreams.fromString(mappingElementSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(mappingElementSourceCode.mappingParseTreeWalkerSourceInformation);
        GraphQLMappingLexerGrammar lexer = new GraphQLMappingLexerGrammar(CharStreams.fromString(mappingElementSourceCode.code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        GraphQLMappingParserGrammar parser = new GraphQLMappingParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        GraphQLMappingParserGrammar.DefinitionContext definitionContext = parser.definition();
        ParserRuleContext classMappingCtx = definitionContext.classMapping();
        return new SourceCodeParserInfo(mappingElementSourceCode.code, input, mappingElementSourceCode.mappingParseTreeWalkerSourceInformation.getSourceInformation(mappingElementSourceCode.mappingElementParserRuleContext), mappingElementSourceCode.mappingElementParseTreeWalkerSourceInformation, lexer, parser, classMappingCtx);
    }

    @Override
    public Iterable<? extends SectionParser> getExtraSectionParsers()
    {
        return Collections.singletonList(SectionParser.newParser(NAME, (sectionSourceCode, elementConsumer, context) ->
        {
            SourceCodeParserInfo parserInfo = getGraphQLStoreParserInfo(sectionSourceCode);
            DefaultCodeSection section = new DefaultCodeSection();
            section.parserName = sectionSourceCode.sectionType;
            section.sourceInformation = parserInfo.sourceInformation;
            GraphQLStoreParseTreeWalker walker = new GraphQLStoreParseTreeWalker(parserInfo.walkerSourceInformation, elementConsumer, section);
            walker.visit((GraphQLStoreParserGrammar.DefinitionContext) parserInfo.rootContext);
            return section;
        }));
    }

    @Override
    public Iterable<? extends MappingElementParser> getExtraMappingElementParsers()
    {
        return Collections.singletonList(MappingElementParser.newParser(GRAPHQL_STORE_MAPPING_ELEMENT_TYPE,
                (mappingElementSourceCode, parserContext) ->
                {
                    MappingParserGrammar.MappingElementContext ctx = mappingElementSourceCode.mappingElementParserRuleContext;
                    SourceCodeParserInfo parserInfo = getGraphQLMappingElementParserInfo(mappingElementSourceCode);
                    GraphQLMappingParseTreeWalker walker = new GraphQLMappingParseTreeWalker();

                    GraphQLClassMapping graphQLClassMapping = new GraphQLClassMapping();
                    graphQLClassMapping._class = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
                    graphQLClassMapping.root = ctx.STAR() != null;
                    graphQLClassMapping.id = ctx.mappingElementId() != null ? ctx.mappingElementId().getText() : null;
                    graphQLClassMapping.extendsClassMappingId = ctx.superClassMappingId() != null ? ctx.superClassMappingId().getText() : null;
                    graphQLClassMapping.sourceInformation = parserInfo.sourceInformation;
                    graphQLClassMapping.classSourceInformation = mappingElementSourceCode.mappingParseTreeWalkerSourceInformation.getSourceInformation(ctx.qualifiedName());
                    walker.visitGraphQLClassMapping((GraphQLMappingParserGrammar.ClassMappingContext) parserInfo.rootContext, graphQLClassMapping);
                    return graphQLClassMapping;
                })
        );
    }

    @Override
    public Iterable<? extends ConnectionValueParser> getExtraConnectionParsers()
    {
        return Collections.singletonList(ConnectionValueParser.newParser(GRAPHQL_CONNECTION_TYPE, (connectionValueSourceCode, extension) ->
        {
            SourceCodeParserInfo parserInfo = getGraphQLConnectionParserInfo(connectionValueSourceCode);
            GraphQLConnectionParseTreeWalker walker = new GraphQLConnectionParseTreeWalker(parserInfo.walkerSourceInformation, extension);
            GraphQLConnection connectionValue = new GraphQLConnection();
            connectionValue.sourceInformation = connectionValueSourceCode.sourceInformation;
            walker.visitGraphQLConnectionValue((GraphQLConnectionParserGrammar.DefinitionContext) parserInfo.rootContext, connectionValue, connectionValueSourceCode.isEmbedded);
            return connectionValue;
        }));
    }
}
