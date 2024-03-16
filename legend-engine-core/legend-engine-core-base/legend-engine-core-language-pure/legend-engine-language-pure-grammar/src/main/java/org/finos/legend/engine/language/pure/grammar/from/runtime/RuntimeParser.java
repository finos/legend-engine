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

package org.finos.legend.engine.language.pure.grammar.from.runtime;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.finos.legend.engine.language.pure.grammar.from.DEPRECATED_SectionGrammarParser;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.runtime.RuntimeLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.runtime.RuntimeParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtensions;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.runtime.EngineRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;

import java.util.function.Consumer;

public class RuntimeParser implements DEPRECATED_SectionGrammarParser
{
    public static final String name = "Runtime";

    private final ConnectionParser connectionParser;

    private RuntimeParser(ConnectionParser connectionParser)
    {
        this.connectionParser = connectionParser;
    }

    public static RuntimeParser newInstance(ConnectionParser connectionParser)
    {
        return new RuntimeParser(connectionParser);
    }

    public static RuntimeParser newInstance(PureGrammarParserExtensions extensions)
    {
        return newInstance(ConnectionParser.newInstance(extensions));
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
        RuntimeLexerGrammar lexer = new RuntimeLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        RuntimeParserGrammar parser = new RuntimeParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(code, input, sectionSourceInformation, walkerSourceInformation, lexer, parser, includeRootContext ? parser.definition() : null);
    }

    @Override
    public Section parse(SourceCodeParserInfo sectionParserInfo, Consumer<PackageableElement> pureModelContextData, PureGrammarParserContext parserContext)
    {
        ImportAwareCodeSection section = new ImportAwareCodeSection();
        section.parserName = this.getName();
        section.sourceInformation = sectionParserInfo.sourceInformation;
        RuntimeParseTreeWalker walker = new RuntimeParseTreeWalker(sectionParserInfo.walkerSourceInformation, pureModelContextData, section, this.connectionParser);
        walker.visit((RuntimeParserGrammar.DefinitionContext) sectionParserInfo.rootContext);
        return section;
    }

    public EngineRuntime parseEmbeddedRuntime(String code, ParseTreeWalkerSourceInformation walkerSourceInformation, SourceInformation sourceInformation)
    {
        SourceCodeParserInfo sectionParserInfo = this.getParserInfo(code, null, walkerSourceInformation, false);
        RuntimeParseTreeWalker walker = new RuntimeParseTreeWalker(walkerSourceInformation, null, null, this.connectionParser);
        return walker.visitEmbeddedRuntime(((RuntimeParserGrammar) sectionParserInfo.parser).embeddedRuntime(), sourceInformation);
    }
}
