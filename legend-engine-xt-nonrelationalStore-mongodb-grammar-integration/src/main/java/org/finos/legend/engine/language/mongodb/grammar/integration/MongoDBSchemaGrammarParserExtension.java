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

package org.finos.legend.engine.language.mongodb.grammar.integration;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.finos.legend.engine.language.mongodb.grammar.integration.extensions.IMongoDBSchemaGrammarParserExtension;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.antlr4.MongoDBSchemaLexer;
import org.finos.legend.engine.language.mongodb.schema.grammar.from.antlr4.MongoDBSchemaParser;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.SectionSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;

import java.util.Collections;

public class MongoDBSchemaGrammarParserExtension implements IMongoDBSchemaGrammarParserExtension
{
    public static final String NAME = "MongoDB";
    public static final String MONGO_DB_MAPPING_ELEMENT_TYPE = "MongoDB";
    public static final String MONGO_DB_CONNECTION_TYPE = "MongoDBConnection";

    private static SourceCodeParserInfo getMongoDBParserInfo(SectionSourceCode sectionSourceCode)
    {
        CharStream input = CharStreams.fromString(sectionSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(sectionSourceCode.walkerSourceInformation);
        MongoDBSchemaLexer lexer = new MongoDBSchemaLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        MongoDBSchemaParser parser = new MongoDBSchemaParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(sectionSourceCode.code, input, sectionSourceCode.sourceInformation, sectionSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
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
            walker.visit((MongoDBSchemaParser.DefinitionContext) parserInfo.rootContext);
            return section;
        }));
    }
}
