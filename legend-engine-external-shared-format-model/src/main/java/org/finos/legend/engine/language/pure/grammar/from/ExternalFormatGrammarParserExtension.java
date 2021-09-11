// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.language.pure.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ExternalFormatLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ExternalFormatParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class ExternalFormatGrammarParserExtension implements PureGrammarParserExtension
{
    public static final String NAME = "ExternalFormat";
    public static final String URL_CONNECTION_TYPE = "UrlConnection";
    public static final String EXTERNAL_FORMAT_CONNECTION_TYPE = "ExternalFormatConnection";

    public List<SectionParser> getExtraSectionParsers()
    {
        return Collections.singletonList(SectionParser.newParser(NAME, ExternalFormatGrammarParserExtension::parse));
    }

    private static Section parse(SectionSourceCode sectionSourceCode, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext context)
    {
        SourceCodeParserInfo parserInfo = getFormatSchemaParserInfo(sectionSourceCode);
        ImportAwareCodeSection section = new ImportAwareCodeSection();
        section.parserName = sectionSourceCode.sectionType;
        section.sourceInformation = parserInfo.sourceInformation;
        ExternalFormatParseTreeWalker walker = new ExternalFormatParseTreeWalker(parserInfo.walkerSourceInformation, elementConsumer, context, section);
        walker.visit((ExternalFormatParserGrammar.DefinitionContext) parserInfo.rootContext);
        return section;
    }

    private static SourceCodeParserInfo getFormatSchemaParserInfo(SectionSourceCode sourceCode)
    {
        CharStream input = CharStreams.fromString(sourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(sourceCode.walkerSourceInformation);
        ExternalFormatLexerGrammar lexer = new ExternalFormatLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        ExternalFormatParserGrammar parser = new ExternalFormatParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(sourceCode.code, input, sourceCode.sourceInformation, sourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }
}
