// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.from.connection;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.finos.legend.engine.language.pure.grammar.from.DEPRECATED_SectionGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.function.Consumer;

public class ConnectionParser implements DEPRECATED_SectionGrammarParser
{
    public static final String name = "Connection";

    private final PureGrammarParserExtensions extensions;

    private ConnectionParser(PureGrammarParserExtensions extensions)
    {
        this.extensions = extensions;
    }

    public static ConnectionParser newInstance(PureGrammarParserExtensions extensions)
    {
        return new ConnectionParser(extensions);
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public SourceCodeParserInfo getParserInfo(String code, SourceInformation sourceInformation, ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        return this.getParserInfo(code, sourceInformation, walkerSourceInformation, true);
    }

    private SourceCodeParserInfo getParserInfo(String code, SourceInformation sectionSourceInformation, ParseTreeWalkerSourceInformation walkerSourceInformation, boolean includeRootContext)
    {
        CharStream input = CharStreams.fromString(code);
        ParserErrorListener errorListener = new ParserErrorListener(walkerSourceInformation);
        ConnectionLexerGrammar lexer = new ConnectionLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        ConnectionParserGrammar parser = new ConnectionParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(code, input, sectionSourceInformation, walkerSourceInformation, lexer, parser, includeRootContext ? parser.definition() : null);
    }

    @Override
    public Section parse(SourceCodeParserInfo sectionParserInfo, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext parserContext)
    {
        ImportAwareCodeSection section = new ImportAwareCodeSection();
        section.parserName = this.getName();
        section.sourceInformation = sectionParserInfo.sourceInformation;
        ConnectionParseTreeWalker walker = new ConnectionParseTreeWalker(sectionParserInfo.walkerSourceInformation, this.extensions, elementConsumer, section);
        walker.visit((ConnectionParserGrammar.DefinitionContext) sectionParserInfo.rootContext);
        return section;
    }

    public Connection parseEmbeddedRuntimeConnections(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation)
    {
        if (code == null || code.isEmpty())
        {
            throw new EngineException("Embedded connection must not be empty", sourceInformation, EngineErrorType.PARSER);
        }
        SourceCodeParserInfo sectionParserInfo = this.getParserInfo(code, null, walkerSourceInformation, false);
        ConnectionParseTreeWalker walker = new ConnectionParseTreeWalker(walkerSourceInformation, this.extensions, null, null);
        return walker.visitEmbeddedRuntimeConnection(((ConnectionParserGrammar) sectionParserInfo.parser).embeddedRuntimeConnection(), sourceInformation);
    }
}
