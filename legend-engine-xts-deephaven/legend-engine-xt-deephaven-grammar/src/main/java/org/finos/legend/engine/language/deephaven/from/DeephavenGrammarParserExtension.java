//  Copyright 2025 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.language.deephaven.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.SectionSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DeephavenLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.DeephavenParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.DeephavenConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.DeephavenConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionValueSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.extension.ConnectionValueParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.protocol.deephaven.metamodel.runtime.DeephavenConnection;
import org.finos.legend.engine.protocol.pure.m3.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;

import java.util.function.Consumer;
import java.util.function.Function;

public class DeephavenGrammarParserExtension implements PureGrammarParserExtension
{
    public static final String NAME = "Deephaven";
    public static final String DEEPHAVEN_MAPPING_ELEMENT_TYPE = "Deephaven";
    public static final String DEEPHAVEN_CONNECTION_TYPE = "DeephavenConnection";
    private final Iterable<SectionParser> sectionParser;
    private final Iterable<ConnectionValueParser> connectionValueParser;

    public DeephavenGrammarParserExtension()
    {
        this.sectionParser = Lists.immutable.with(SectionParser.newParser(NAME, this::parseSection));
        this.connectionValueParser = Lists.immutable.with(ConnectionValueParser.newParser(DEEPHAVEN_CONNECTION_TYPE, this::parseConnection));
    }

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Deephaven");
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

    private Section parseSection(SectionSourceCode sectionSourceCode, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext context)
    {
        SourceCodeParserInfo parserInfo = this.getDefinitionSectionParserInfo(sectionSourceCode);
        DeephavenParseTreeWalker walker = new DeephavenParseTreeWalker(context.getPureGrammarParserExtensions(), parserInfo);
        return walker.visit(sectionSourceCode.sectionType, elementConsumer, (DeephavenParserGrammar.DefinitionContext) parserInfo.rootContext);
    }

    private static SourceCodeParserInfo getDefinitionSectionParserInfo(SectionSourceCode sectionSourceCode)
    {
        CharStream input = CharStreams.fromString(sectionSourceCode.code);
        DeephavenLexerGrammar lexer = new DeephavenLexerGrammar(input);

        DeephavenParserGrammar parser = new DeephavenParserGrammar(new CommonTokenStream(lexer));

        ParserErrorListener errorListener = new ParserErrorListener(sectionSourceCode.walkerSourceInformation, DeephavenLexerGrammar.VOCABULARY);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return new SourceCodeParserInfo(sectionSourceCode.code, input, sectionSourceCode.sourceInformation, sectionSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }

    private DeephavenConnection parseConnection(ConnectionValueSourceCode connectionValueSourceCode, PureGrammarParserExtensions extension)
    {
        SourceCodeParserInfo parserInfo = this.getConnectionSectionParserInfo(connectionValueSourceCode.code, connectionValueSourceCode.walkerSourceInformation, connectionValueSourceCode.sourceInformation, DeephavenConnectionParserGrammar::deephavenConnectionDefinition);
        DeephavenParseTreeWalker walker = new DeephavenParseTreeWalker(extension, parserInfo);
        return walker.visit((DeephavenConnectionParserGrammar.DeephavenConnectionDefinitionContext) parserInfo.rootContext);
    }

    private SourceCodeParserInfo getConnectionSectionParserInfo(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation, Function<DeephavenConnectionParserGrammar, ParserRuleContext> rootContext)
    {
        CharStream input = CharStreams.fromString(code);

        DeephavenConnectionLexerGrammar lexer = new DeephavenConnectionLexerGrammar(input);
        DeephavenConnectionParserGrammar parser = new DeephavenConnectionParserGrammar(new CommonTokenStream(lexer));

        ParserErrorListener errorListener = new ParserErrorListener(walkerSourceInformation, DeephavenConnectionLexerGrammar.VOCABULARY);
        lexer.addErrorListener(errorListener);
        parser.addErrorListener(errorListener);

        return new SourceCodeParserInfo(code, input, sourceInformation, walkerSourceInformation, lexer, parser, rootContext.apply(parser));
    }
}
