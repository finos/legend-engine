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

package org.finos.legend.engine.language.pure.dsl.mastery.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.eclipse.collections.api.factory.Sets;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.mastery.grammar.from.acquisition.AcquisitionProtocolParseTreeWalker;
import org.finos.legend.engine.language.pure.dsl.mastery.grammar.from.authentication.AuthenticationParseTreeWalker;
import org.finos.legend.engine.language.pure.dsl.mastery.grammar.from.connection.ConnectionParseTreeWalker;
import org.finos.legend.engine.language.pure.dsl.mastery.grammar.from.trigger.TriggerParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.ParserErrorListener;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserContext;
import org.finos.legend.engine.language.pure.grammar.from.SectionSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.SourceCodeParserInfo;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.MasteryLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.MasteryParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.acquisition.AcquisitionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.acquisition.AcquisitionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.authentication.AuthenticationStrategyLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.authentication.AuthenticationStrategyParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.MasteryConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.MasteryConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.trigger.TriggerLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.trigger.TriggerParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.PackageableElement;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.AcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.acquisition.RestAcquisitionProtocol;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.CredentialSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authorization.Authorization;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.runtime.MasteryRuntime;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.ManualTrigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.trigger.Trigger;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.ImportAwareCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.Section;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;


public class MasteryParserExtension implements IMasteryParserExtension
{
    public static final String NAME = "Mastery";

    private static final Set<String> CONNECTION_TYPES = Sets.fixedSize.of("FTP", "HTTP", "Kafka");
    private static final Set<String> AUTHENTICATION_TYPES = Sets.fixedSize.of("NTLM", "Token");
    private static final Set<String> ACQUISITION_TYPES = Sets.fixedSize.of("Kafka", "File");
    private static final String CRON_TRIGGER = "Cron";
    private static final String MANUAL_TRIGGER = "Manual";
    private static final String REST_ACQUISITION = "REST";

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("PackageableElement", "Mastery");
    }

    @Override
    public Iterable<? extends SectionParser> getExtraSectionParsers()
    {
        return Lists.fixedSize.of(SectionParser.newParser(NAME, MasteryParserExtension::parseSection));
    }

    private static Section parseSection(SectionSourceCode sectionSourceCode, Consumer<PackageableElement> elementConsumer, PureGrammarParserContext context)
    {
        SourceCodeParserInfo parserInfo = getMasteryParserInfo(sectionSourceCode);
        ImportAwareCodeSection section = new ImportAwareCodeSection();
        section.parserName = sectionSourceCode.sectionType;
        section.sourceInformation = parserInfo.sourceInformation;

        DomainParser domainParser = new DomainParser();

        List<IMasteryParserExtension> extensions = IMasteryParserExtension.getExtensions();
        List<Function<SpecificationSourceCode, Connection>> connectionProcessors = ListIterate.flatCollect(extensions, IMasteryParserExtension::getExtraMasteryConnectionParsers);
        List<Function<SpecificationSourceCode, MasteryRuntime>> masteryRuntimeProcessors = ListIterate.flatCollect(extensions, IMasteryParserExtension::getExtraMasteryRuntimeParsers);
        List<Function<SpecificationSourceCode, Trigger>> triggerProcessors = ListIterate.flatCollect(extensions, IMasteryParserExtension::getExtraTriggerParsers);
        List<Function<SpecificationSourceCode, Authorization>> authorizationProcessors = ListIterate.flatCollect(extensions, IMasteryParserExtension::getExtraAuthorizationParsers);
        List<Function<SpecificationSourceCode, AcquisitionProtocol>> acquisitionProcessors = ListIterate.flatCollect(extensions, IMasteryParserExtension::getExtraAcquisitionProtocolParsers);


        MasteryParseTreeWalker walker = new MasteryParseTreeWalker(parserInfo.walkerSourceInformation, elementConsumer, section, domainParser, connectionProcessors, masteryRuntimeProcessors, triggerProcessors, authorizationProcessors, acquisitionProcessors);
        walker.visit((MasteryParserGrammar.DefinitionContext) parserInfo.rootContext);

        return section;
    }

    private static SourceCodeParserInfo getMasteryParserInfo(SectionSourceCode sectionSourceCode)
    {
        CharStream input = CharStreams.fromString(sectionSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(sectionSourceCode.walkerSourceInformation, MasteryLexerGrammar.VOCABULARY);
        MasteryLexerGrammar lexer = new MasteryLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        MasteryParserGrammar parser = new MasteryParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(sectionSourceCode.code, input, sectionSourceCode.sourceInformation, sectionSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }

    @Override
    public List<Function<SpecificationSourceCode, AcquisitionProtocol>> getExtraAcquisitionProtocolParsers()
    {
        return Collections.singletonList(code ->
        {
            if (REST_ACQUISITION.equals(code.getType()))
            {
                return new RestAcquisitionProtocol();
            }
            else if (ACQUISITION_TYPES.contains(code.getType()))
            {
                AcquisitionParserGrammar acquisitionParserGrammar = getAcquisitionParserGrammar(code);
                List<IMasteryParserExtension> extensions = IMasteryParserExtension.getExtensions();
                List<Function<SpecificationSourceCode, CredentialSecret>> credentialSecretProcessors = ListIterate.flatCollect(extensions, IMasteryParserExtension::getExtraCredentialSecretParsers);
                AcquisitionProtocolParseTreeWalker acquisitionProtocolParseTreeWalker = new AcquisitionProtocolParseTreeWalker(code.getWalkerSourceInformation(), credentialSecretProcessors);
                return acquisitionProtocolParseTreeWalker.visitAcquisitionProtocol(acquisitionParserGrammar);
            }
            return null;
        });
    }

    @Override
    public List<Function<SpecificationSourceCode, Connection>> getExtraMasteryConnectionParsers()
    {
        return Collections.singletonList(code ->
        {
            if (CONNECTION_TYPES.contains(code.getType()))
            {
                List<IMasteryParserExtension> extensions = IMasteryParserExtension.getExtensions();
                List<Function<SpecificationSourceCode, AuthenticationStrategy>> authProcessors = ListIterate.flatCollect(extensions, IMasteryParserExtension::getExtraAuthenticationStrategyParsers);
                MasteryConnectionParserGrammar connectionParserGrammar = getMasteryConnectionParserGrammar(code);
                ConnectionParseTreeWalker connectionParseTreeWalker = new ConnectionParseTreeWalker(code.getWalkerSourceInformation(), authProcessors);
                return connectionParseTreeWalker.visitConnection(connectionParserGrammar);
            }
            return null;
        });
    }

    @Override
    public List<Function<SpecificationSourceCode, AuthenticationStrategy>> getExtraAuthenticationStrategyParsers()
    {
        return Collections.singletonList(code ->
        {
            if (AUTHENTICATION_TYPES.contains(code.getType()))
            {
                List<IMasteryParserExtension> extensions = IMasteryParserExtension.getExtensions();
                List<Function<SpecificationSourceCode, CredentialSecret>> credentialSecretProcessors = ListIterate.flatCollect(extensions, IMasteryParserExtension::getExtraCredentialSecretParsers);
                AuthenticationParseTreeWalker authenticationParseTreeWalker = new AuthenticationParseTreeWalker(code.getWalkerSourceInformation(), credentialSecretProcessors);
                return authenticationParseTreeWalker.visitAuthentication(getAuthenticationParserGrammar(code));
            }
            return null;
        });
    }

    @Override
    public List<Function<SpecificationSourceCode, Trigger>> getExtraTriggerParsers()
    {
        return Collections.singletonList(code ->
        {
            if (code.getType().equals(MANUAL_TRIGGER))
            {
                return new ManualTrigger();
            }

            if (code.getType().equals(CRON_TRIGGER))
            {
                TriggerParseTreeWalker triggerParseTreeWalker = new TriggerParseTreeWalker(code.getWalkerSourceInformation());
                return triggerParseTreeWalker.visitTrigger(getTriggerParserGrammar(code));
            }
            return null;
        });
    }

    private static MasteryConnectionParserGrammar getMasteryConnectionParserGrammar(SpecificationSourceCode connectionSourceCode)
    {

        CharStream input = CharStreams.fromString(connectionSourceCode.getCode());

        ParserErrorListener errorListener = new ParserErrorListener(connectionSourceCode.getWalkerSourceInformation(), MasteryConnectionLexerGrammar.VOCABULARY);
        MasteryConnectionLexerGrammar lexer = new MasteryConnectionLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        MasteryConnectionParserGrammar parser = new MasteryConnectionParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return parser;
    }

    private static TriggerParserGrammar getTriggerParserGrammar(SpecificationSourceCode connectionSourceCode)
    {

        CharStream input = CharStreams.fromString(connectionSourceCode.getCode());

        ParserErrorListener errorListener = new ParserErrorListener(connectionSourceCode.getWalkerSourceInformation(), TriggerLexerGrammar.VOCABULARY);
        TriggerLexerGrammar lexer = new TriggerLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        TriggerParserGrammar parser = new TriggerParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return parser;
    }

    private static AuthenticationStrategyParserGrammar getAuthenticationParserGrammar(SpecificationSourceCode authSourceCode)
    {

        CharStream input = CharStreams.fromString(authSourceCode.getCode());

        ParserErrorListener errorListener = new ParserErrorListener(authSourceCode.getWalkerSourceInformation(), AuthenticationStrategyLexerGrammar.VOCABULARY);
        AuthenticationStrategyLexerGrammar lexer = new AuthenticationStrategyLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        AuthenticationStrategyParserGrammar parser = new AuthenticationStrategyParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return parser;
    }

    private static AcquisitionParserGrammar getAcquisitionParserGrammar(SpecificationSourceCode sourceCode)
    {

        CharStream input = CharStreams.fromString(sourceCode.getCode());

        ParserErrorListener errorListener = new ParserErrorListener(sourceCode.getWalkerSourceInformation(), AcquisitionLexerGrammar.VOCABULARY);
        AcquisitionLexerGrammar lexer = new AcquisitionLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        AcquisitionParserGrammar parser = new AcquisitionParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return parser;
    }
}
