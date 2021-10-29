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

package org.finos.legend.engine.language.pure.grammar.from.text;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.finos.legend.engine.language.pure.grammar.from.*;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.text.TextLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.text.TextParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;

import java.util.function.Consumer;

public class TextParser implements DEPRECATED_SectionGrammarParser
{
    public static final String name = "Text";

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public Section parse(SourceCodeParserInfo sectionParserInfo, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext parserContext)
    {
        DefaultCodeSection section = new DefaultCodeSection();
        section.parserName = this.getName();
        section.sourceInformation =sectionParserInfo.sourceInformation;
        TextParseTreeWalker walker = new TextParseTreeWalker(sectionParserInfo.walkerSourceInformation, elementConsumer, section);
        walker.visit((TextParserGrammar.DefinitionContext) sectionParserInfo.rootContext);
        return section;
    }

    @Override
    public SourceCodeParserInfo getParserInfo(String code, SourceInformation sourceInformation, ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        return this.getTextParserInfo(code, sourceInformation, walkerSourceInformation, true);
    }

    private static SourceCodeParserInfo getTextParserInfo(String code, SourceInformation sectionSourceInformation, ParseTreeWalkerSourceInformation walkerSourceInformation, boolean includeRootContext)
    {
        CharStream input = CharStreams.fromString(code);
        ParserErrorListener errorListener = new ParserErrorListener(walkerSourceInformation);
        TextLexerGrammar lexer = new TextLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        TextParserGrammar parser = new TextParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(code, input, sectionSourceInformation, walkerSourceInformation, lexer, parser, includeRootContext ? parser.definition() : null);
    }
}
