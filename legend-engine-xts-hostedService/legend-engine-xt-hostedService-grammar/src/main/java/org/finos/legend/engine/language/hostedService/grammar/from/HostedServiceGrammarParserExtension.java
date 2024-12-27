//  Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.language.hostedService.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.SectionSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.HostedServiceLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.HostedServiceParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.extension.PureGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.protocol.pure.v1.model.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;

import java.util.function.Consumer;

public class HostedServiceGrammarParserExtension implements PureGrammarParserExtension
{
    public static final String NAME = "HostedService";

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Function_Activator", "Hosted_Service");
    }

    @Override
    public Iterable<? extends SectionParser> getExtraSectionParsers()
    {
        return Lists.fixedSize.of(SectionParser.newParser(NAME, HostedServiceGrammarParserExtension::parseSection));
    }

    private static Section parseSection(SectionSourceCode sectionSourceCode, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext context)
    {
        SourceCodeParserInfo parserInfo = getHostedServiceInfo(sectionSourceCode);
        ImportAwareCodeSection section = new ImportAwareCodeSection();
        section.parserName = sectionSourceCode.sectionType;
        section.sourceInformation = parserInfo.sourceInformation;

        HostedServiceTreeWalker walker = new HostedServiceTreeWalker(parserInfo.input, parserInfo.walkerSourceInformation, elementConsumer, section, context);
        walker.visit((HostedServiceParserGrammar.DefinitionContext) parserInfo.rootContext);

        return section;
    }

    private static SourceCodeParserInfo getHostedServiceInfo(SectionSourceCode sectionSourceCode)
    {
        CharStream input = CharStreams.fromString(sectionSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(sectionSourceCode.walkerSourceInformation, HostedServiceLexerGrammar.VOCABULARY);
        HostedServiceLexerGrammar lexer = new HostedServiceLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        HostedServiceParserGrammar parser = new HostedServiceParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(sectionSourceCode.code, input, sectionSourceCode.sourceInformation, sectionSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }
}
