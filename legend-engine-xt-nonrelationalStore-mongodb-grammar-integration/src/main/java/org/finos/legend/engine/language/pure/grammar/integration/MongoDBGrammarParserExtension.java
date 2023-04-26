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
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.SectionSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.MongoDBConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.MongoDBConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.schema.MongoDBSchemaLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.schema.MongoDBSchemaParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionValueSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.extension.ConnectionValueParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.language.pure.grammar.integration.connection.MongoDBConnectionParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.integration.extensions.IMongoDBGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.integration.util.MongoDBSchemaParseTreeWalker;
import org.finos.legend.engine.protocol.mongodb.schema.metamodel.pure.MongoDBConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;

import java.util.Collections;

public class MongoDBGrammarParserExtension implements IMongoDBGrammarParserExtension
{
    public static final String NAME = "MongoDB";
    public static final String MONGO_DB_MAPPING_ELEMENT_TYPE = "MongoDB";
    public static final String MONGO_DB_CONNECTION_TYPE = "MongoDBConnection";

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
            walker.visitServiceStoreConnectionValue((MongoDBConnectionParserGrammar.DefinitionContext) parserInfo.rootContext, connectionValue, connectionValueSourceCode.isEmbedded);
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
}
