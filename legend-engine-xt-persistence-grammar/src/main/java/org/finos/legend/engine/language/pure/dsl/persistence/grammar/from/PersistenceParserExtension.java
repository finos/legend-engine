// Copyright 2022 Goldman Sachs
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

package org.finos.legend.engine.language.pure.dsl.persistence.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.persistence.grammar.from.context.PersistenceContextParseTreeWalker;
import org.finos.legend.engine.language.pure.dsl.persistence.grammar.from.context.PersistencePlatformSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.SectionSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.context.DefaultPersistencePlatform;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.context.PersistencePlatform;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.ManualTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.trigger.Trigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class PersistenceParserExtension implements IPersistenceParserExtension
{
    public static final String NAME = "Persistence";
    public static final String TRIGGER_MANUAL = "Manual";
    public static final String TRIGGER_CRON = "Cron";

    @Override
    public Iterable<? extends SectionParser> getExtraSectionParsers()
    {
        return Lists.fixedSize.of(SectionParser.newParser(NAME, PersistenceParserExtension::parseSection));
    }

    @Override
    public List<Function<TriggerSourceCode, Trigger>> getExtraTriggerParsers()
    {
        return Collections.singletonList(code ->
                {
                    switch (code.getType())
                    {
                        case TRIGGER_MANUAL:
                            return new ManualTrigger();
                        case TRIGGER_CRON:
                            //TODO: ledav -- implement cron parser
                            return null;
                    }
                    return null;
                }
        );
    }

    @Override
    public List<Function<PersistencePlatformSourceCode, PersistencePlatform>> getExtraPersistencePlatformParsers()
    {
        return Collections.singletonList(code ->
                {
                    if ("Default".equals(code.getType()))
                    {
                        DefaultPersistencePlatform platform = new DefaultPersistencePlatform();
                        platform.sourceInformation = code.getSourceInformation();
                        return platform;
                    }
                    return null;
                }
        );
    }

    private static Section parseSection(SectionSourceCode sectionSourceCode, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext context)
    {
        SourceCodeParserInfo parserInfo = getPersistenceParserInfo(sectionSourceCode);
        ImportAwareCodeSection section = new ImportAwareCodeSection();
        section.parserName = sectionSourceCode.sectionType;
        section.sourceInformation = parserInfo.sourceInformation;

        ConnectionParser connectionParser = ConnectionParser.newInstance(context.getPureGrammarParserExtensions());

        List<IPersistenceParserExtension> extensions = IPersistenceParserExtension.getExtensions();
        List<Function<PersistencePlatformSourceCode, PersistencePlatform>> platformProcessors = ListIterate.flatCollect(extensions, IPersistenceParserExtension::getExtraPersistencePlatformParsers);
        List<Function<TriggerSourceCode, Trigger>> triggerProcessors = ListIterate.flatCollect(extensions, IPersistenceParserExtension::getExtraTriggerParsers);

        PersistenceContextParseTreeWalker persistenceContextWalker = new PersistenceContextParseTreeWalker(parserInfo.walkerSourceInformation, connectionParser, platformProcessors);
        PersistenceParseTreeWalker persistenceWalker = new PersistenceParseTreeWalker(parserInfo.walkerSourceInformation, elementConsumer, section, triggerProcessors, persistenceContextWalker);
        persistenceWalker.visit((PersistenceParserGrammar.DefinitionContext) parserInfo.rootContext);

        return section;
    }

    private static SourceCodeParserInfo getPersistenceParserInfo(SectionSourceCode sectionSourceCode)
    {
        CharStream input = CharStreams.fromString(sectionSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(sectionSourceCode.walkerSourceInformation, PersistenceLexerGrammar.VOCABULARY);
        PersistenceLexerGrammar lexer = new PersistenceLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        PersistenceParserGrammar parser = new PersistenceParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(sectionSourceCode.code, input, sectionSourceCode.sourceInformation, sectionSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }
}
