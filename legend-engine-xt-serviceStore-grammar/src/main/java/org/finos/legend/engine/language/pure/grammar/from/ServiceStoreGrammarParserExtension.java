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

package org.finos.legend.engine.language.pure.grammar.from;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ServiceStoreLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.ServiceStoreParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ServiceStoreConnectionLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.ServiceStoreConnectionParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.authentication.SecuritySchemeLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.authentication.SecuritySchemeParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.mapping.MappingParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionValueSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.connection.ServiceStoreConnectionParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.connection.authentication.AuthenticationSpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.connection.authentication.SecuritySchemeParseTreeWalker;
import org.finos.legend.engine.language.pure.grammar.from.connection.authentication.SecuritySchemeSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.data.ServiceStoreEmbeddedDataParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.ConnectionValueParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.MappingElementParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.SectionParser;
import org.finos.legend.engine.language.pure.grammar.from.extension.data.EmbeddedDataParser;
import org.finos.legend.engine.language.pure.grammar.from.extensions.IServiceStoreGrammarParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.mapping.MappingElementSourceCode;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.section.DefaultCodeSection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.connection.ServiceStoreConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.mapping.RootServiceStoreClassMapping;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.service.model.SecurityScheme;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.authentication.AuthSpecificationLexerGrammar;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.authentication.AuthSpecificationParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.connection.authentication.AuthenticationSpecificationParseTreeWalker;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class ServiceStoreGrammarParserExtension implements IServiceStoreGrammarParserExtension
{
    public static final String NAME = "ServiceStore";
    public static final String SERVICE_STORE_MAPPING_ELEMENT_TYPE = "ServiceStore";
    public static final String SERVICE_STORE_CONNECTION_TYPE = "ServiceStoreConnection";

    @Override
    public Iterable<? extends SectionParser> getExtraSectionParsers()
    {
        return Collections.singletonList(SectionParser.newParser(NAME, (sectionSourceCode, elementConsumer, context) ->
        {
            SourceCodeParserInfo parserInfo = getServiceStoreParserInfo(sectionSourceCode);
            DefaultCodeSection section = new DefaultCodeSection();
            section.parserName = sectionSourceCode.sectionType;
            section.sourceInformation = parserInfo.sourceInformation;
            ServiceStoreParseTreeWalker walker = new ServiceStoreParseTreeWalker(parserInfo.walkerSourceInformation, elementConsumer, section);
            walker.visit((ServiceStoreParserGrammar.DefinitionContext) parserInfo.rootContext);
            return section;
        }));
    }

    @Override
    public Iterable<? extends MappingElementParser> getExtraMappingElementParsers()
    {
        return Collections.singletonList(MappingElementParser.newParser(SERVICE_STORE_MAPPING_ELEMENT_TYPE,
                (mappingElementSourceCode, parserContext) ->
                {
                    MappingParserGrammar.MappingElementContext ctx = mappingElementSourceCode.mappingElementParserRuleContext;
                    SourceCodeParserInfo parserInfo = getServiceStoreMappingElementParserInfo(mappingElementSourceCode);
                    ServiceStoreParseTreeWalker walker = new ServiceStoreParseTreeWalker(parserInfo.walkerSourceInformation);

                    RootServiceStoreClassMapping classMapping = new RootServiceStoreClassMapping();
                    classMapping._class = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
                    classMapping.id = ctx.mappingElementId() != null ? ctx.mappingElementId().getText() : null;
                    classMapping.root = ctx.STAR() != null;
                    if (ctx.superClassMappingId() != null)
                    {
                        throw new EngineException("Service Store Mapping does not support extends", mappingElementSourceCode.mappingParseTreeWalkerSourceInformation.getSourceInformation(ctx.superClassMappingId()), EngineErrorType.PARSER);
                    }
                    classMapping.sourceInformation = parserInfo.sourceInformation;
                    classMapping.classSourceInformation = mappingElementSourceCode.mappingParseTreeWalkerSourceInformation.getSourceInformation(ctx.qualifiedName());
                    walker.visitRootServiceClassMapping((ServiceStoreParserGrammar.ClassMappingContext) parserInfo.rootContext, classMapping, classMapping._class);
                    return classMapping;
                })
        );
    }

    @Override
    public Iterable<? extends ConnectionValueParser> getExtraConnectionParsers()
    {
        return Collections.singletonList(ConnectionValueParser.newParser(SERVICE_STORE_CONNECTION_TYPE, connectionValueSourceCode ->
        {
            SourceCodeParserInfo parserInfo = getServiceStoreConnectionParserInfo(connectionValueSourceCode);
            ServiceStoreConnectionParseTreeWalker walker = new ServiceStoreConnectionParseTreeWalker(parserInfo.walkerSourceInformation);
            ServiceStoreConnection connectionValue = new ServiceStoreConnection();
            connectionValue.sourceInformation = connectionValueSourceCode.sourceInformation;
            walker.visitServiceStoreConnectionValue((ServiceStoreConnectionParserGrammar.DefinitionContext) parserInfo.rootContext, connectionValue, connectionValueSourceCode.isEmbedded);
            return connectionValue;
        }));
    }

    @Override
    public List<Function<SecuritySchemeSourceCode, SecurityScheme>> getExtraSecuritySchemesParsers()
    {
        return Collections.singletonList(code ->
        {
            SecuritySchemeParseTreeWalker walker = new SecuritySchemeParseTreeWalker();
            switch (code.getType())
            {
                case "Http":
                    return parseSecurityScheme(code, p -> walker.visitSimpleHttpSecurityScheme(code, p.httpSecurityScheme()));
                case "ApiKey":
                    return parseSecurityScheme(code, p -> walker.visitApiKeySecurityScheme(code, p.apiKeySecurityScheme()));
                case "Oauth":
                    return parseSecurityScheme(code, p -> walker.visitOauthSecurityScheme(code, p.oauthSecurityScheme()));
                default:
                    return null;
            }
        });
    }

    public List<Function<AuthenticationSpecificationSourceCode, AuthenticationSpecification>> getExtraAuthenticationGenerationSpecificationParsers()
    {
        return Collections.singletonList(code ->
        {
            AuthenticationSpecificationParseTreeWalker walker = new AuthenticationSpecificationParseTreeWalker();
            switch (code.getType())
            {
                case "ApiKeyAuthentication":
                    return parseAuthTokenGenerationSpecification(code, p -> walker.visitApiKeyAuthentication(code,p.apiKeyAuthentication()));
                case "UsernamePasswordAuthentication":
                    return parseAuthTokenGenerationSpecification(code, p -> walker.visitUsernamePasswordAuthentication(code, p.basicAuthentication()));
                case "OauthAuthentication":
                    return parseAuthTokenGenerationSpecification(code, p -> walker.visitOAuthAuthentication(code, p.oauthAuthentication()));
                default:
                    return null;
            }
        });
    }

    @Override
    public Iterable<? extends EmbeddedDataParser> getExtraEmbeddedDataParsers()
    {
        return Lists.immutable.with(new ServiceStoreEmbeddedDataParser());
    }

    private static SourceCodeParserInfo getServiceStoreMappingElementParserInfo(MappingElementSourceCode mappingElementSourceCode)
    {
        CharStream input = CharStreams.fromString(mappingElementSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(mappingElementSourceCode.mappingElementParseTreeWalkerSourceInformation);
        ServiceStoreLexerGrammar lexer = new ServiceStoreLexerGrammar(CharStreams.fromString(mappingElementSourceCode.code));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        ServiceStoreParserGrammar parser = new ServiceStoreParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        ServiceStoreParserGrammar.MappingContext mappingContext = parser.mapping();
        ParserRuleContext classMappingCtx = mappingContext.classMapping();
        return new SourceCodeParserInfo(mappingElementSourceCode.code, input, mappingElementSourceCode.mappingParseTreeWalkerSourceInformation.getSourceInformation(mappingElementSourceCode.mappingElementParserRuleContext), mappingElementSourceCode.mappingElementParseTreeWalkerSourceInformation, lexer, parser, classMappingCtx);
    }

    private static SourceCodeParserInfo getServiceStoreConnectionParserInfo(ConnectionValueSourceCode connectionValueSourceCode)
    {
        CharStream input = CharStreams.fromString(connectionValueSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(connectionValueSourceCode.walkerSourceInformation);
        ServiceStoreConnectionLexerGrammar lexer = new ServiceStoreConnectionLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        ServiceStoreConnectionParserGrammar parser = new ServiceStoreConnectionParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(connectionValueSourceCode.code, input, connectionValueSourceCode.sourceInformation, connectionValueSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }

    private static SourceCodeParserInfo getServiceStoreParserInfo(SectionSourceCode sectionSourceCode)
    {
        CharStream input = CharStreams.fromString(sectionSourceCode.code);
        ParserErrorListener errorListener = new ParserErrorListener(sectionSourceCode.walkerSourceInformation);
        ServiceStoreLexerGrammar lexer = new ServiceStoreLexerGrammar(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        ServiceStoreParserGrammar parser = new ServiceStoreParserGrammar(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);
        return new SourceCodeParserInfo(sectionSourceCode.code, input, sectionSourceCode.sourceInformation, sectionSourceCode.walkerSourceInformation, lexer, parser, parser.definition());
    }

    private SecurityScheme parseSecurityScheme(SecuritySchemeSourceCode code, Function<SecuritySchemeParserGrammar, SecurityScheme> func)
    {
        CharStream input = CharStreams.fromString(code.getCode());
        ParserErrorListener errorListener = new ParserErrorListener(code.getWalkerSourceInformation());
        SecuritySchemeLexerGrammar lexer = new SecuritySchemeLexerGrammar(input);
        SecuritySchemeParserGrammar parser = new SecuritySchemeParserGrammar(new CommonTokenStream(lexer));

        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return func.apply(parser);
    }

    private AuthenticationSpecification parseAuthTokenGenerationSpecification(AuthenticationSpecificationSourceCode code, Function<AuthSpecificationParserGrammar, AuthenticationSpecification> func)
    {
        CharStream input = CharStreams.fromString(code.getCode());
        ParserErrorListener errorListener = new ParserErrorListener(code.getWalkerSourceInformation());
        AuthSpecificationLexerGrammar lexer = new AuthSpecificationLexerGrammar(input);
        AuthSpecificationParserGrammar parser = new AuthSpecificationParserGrammar(new CommonTokenStream(lexer));

        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return func.apply(parser);
    }
}
