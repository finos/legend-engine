// Copyright 2023 Goldman Sachs
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.language.pure.grammar.integration;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.SectionSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.MongoDBConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.MongoDBConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MongoDBMappingLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MongoDBMappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.schema.MongoDBSchemaLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.schema.MongoDBSchemaParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionValueSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.extension.ConnectionValueParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.MappingElementParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.language.pure.grammar.from.mapping.MappingElementSourceCode;
import org.finos.legend.engine.language.pure.grammar.integration.connection.MongoDBConnectionParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.integration.extensions.IMongoDBGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.integration.mapping.MongoDBMappingParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.integration.util.MongoDBSchemaParseTreeWalker;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBConnection;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.RootMongoDBClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;

import java.util.Collections;

public class MongoDBGrammarParserExtension implements IMongoDBGrammarParserExtension
{
    public static final String NAME = "MongoDB";
    public static final String MONGO_DB_MAPPING_ELEMENT_TYPE = "MongoDB";
    public static final String MONGO_DB_CONNECTION_TYPE = "MongoDBConnection";

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Mongo");
    }

    private static SourceCodeParserInfo getMongoDBParserInfo(SectionSourceCode sectionSourceCode)
    {
        CharStream input = CharStreams.fromString(sectionSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(sectionSourceCode.walkerSourceInformation);
        MongoDBSchemaLexerGrammar lexer = new MongoDBSchemaLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        MongoDBSchemaParserGrammar parser = new MongoDBSchemaParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(sectionSourceCode.code, input, sectionSourceCode.sourceInformation, sectionSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }

    private static SourceCodeParserInfo getMongoDBConnectionParserInfo(ConnectionValueSourceCode connectionValueSourceCode)
    {
        CharStream input = CharStreams.fromString(connectionValueSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(connectionValueSourceCode.walkerSourceInformation);
        MongoDBConnectionLexerGrammar lexer = new MongoDBConnectionLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        MongoDBConnectionParserGrammar parser = new MongoDBConnectionParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(connectionValueSourceCode.code, input, connectionValueSourceCode.sourceInformation, connectionValueSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }

    @Override
    public Iterable<? extends ConnectionValueParser> getExtraConnectionParsers()
    {
        return Collections.singletonList(ConnectionValueParser.newParser(MONGO_DB_CONNECTION_TYPE, (connectionValueSourceCode, extension) ->
        {
            SourceCodeParserInfo parserInfo = getMongoDBConnectionParserInfo(connectionValueSourceCode);
            MongoDBConnectionParseTreeWalker walker = new MongoDBConnectionParseTreeWalker(parserInfo.walkerSourceInformation, extension);
            MongoDBConnection connectionValue = new MongoDBConnection();
            connectionValue.sourceInformation = connectionValueSourceCode.sourceInformation;
            walker.visitMongoDBConnectionValue((MongoDBConnectionParserGrammar.DefinitionContext) parserInfo.rootContext, connectionValue, connectionValueSourceCode.isEmbedded);
            return connectionValue;
        }));
    }

    @Override
    public Iterable<? extends SectionParser> getExtraSectionParsers()
    {
        return Collections.singletonList(SectionParser.newParser(NAME, (sectionSourceCode, elementConsumer, context) ->
        {
            SourceCodeParserInfo parserInfo = getMongoDBParserInfo(sectionSourceCode);
            DefaultCodeSection section = new DefaultCodeSection();
            section.parserName = sectionSourceCode.sectionType;
            section.sourceInformation = parserInfo.sourceInformation;
            MongoDBSchemaParseTreeWalker walker = new MongoDBSchemaParseTreeWalker(parserInfo.walkerSourceInformation, elementConsumer, section);
            walker.visit((MongoDBSchemaParserGrammar.DefinitionContext) parserInfo.rootContext);
            return section;
        }));
    }

    public static SourceCodeParserInfo getMongoDBMappingElementParserInfo(MappingElementSourceCode mappingElementSourceCode)
    {
        CharStream input = CharStreams.fromString(mappingElementSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(mappingElementSourceCode.mappingParseTreeWalkerSourceInformation);
        MongoDBMappingLexerGrammar lexer = new MongoDBMappingLexerGrammar(CharStreams.fromString(mappingElementSourceCode.code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        MongoDBMappingParserGrammar parser = new MongoDBMappingParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        MongoDBMappingParserGrammar.DefinitionContext definitionContext = parser.definition();
        ParserRuleContext classMapppingCtx = definitionContext.classMapping();
        return new SourceCodeParserInfo(mappingElementSourceCode.code, input, mappingElementSourceCode.mappingParseTreeWalkerSourceInformation.getSourceInformation(mappingElementSourceCode.mappingElementParserRuleContext), mappingElementSourceCode.mappingElementParseTreeWalkerSourceInformation, lexer, parser, classMapppingCtx);
    }

    @Override
    public Iterable<? extends MappingElementParser> getExtraMappingElementParsers()
    {
        return Collections.singletonList(MappingElementParser.newParser(MONGO_DB_MAPPING_ELEMENT_TYPE,
                (mappingElementSourceCode, parserContext) ->
                {
                    MappingParserGrammar.MappingElementContext ctx = mappingElementSourceCode.mappingElementParserRuleContext;
                    SourceCodeParserInfo parserInfo = getMongoDBMappingElementParserInfo(mappingElementSourceCode);
                    MongoDBMappingParseTreeWalker walker = new MongoDBMappingParseTreeWalker();

                    RootMongoDBClassMapping mongoDBClassMapping = new RootMongoDBClassMapping();
                    mongoDBClassMapping._class = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
                    mongoDBClassMapping.root = ctx.STAR() != null;
                    mongoDBClassMapping.id = ctx.mappingElementId() != null ? ctx.mappingElementId().getText() : null;
                    walker.visitMongoDBClassMapping((MongoDBMappingParserGrammar.ClassMappingContext) parserInfo.rootContext, mongoDBClassMapping);
                    return mongoDBClassMapping;
                })
        );
    }
}
