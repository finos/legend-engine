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

package org.finos.legend.engine.language.pure.dsl.persistence.grammar.from.context;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.persistence.grammar.from.IPersistenceParserExtension;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.PersistenceParserGrammar;
import org.finos.legend.engine.language.pure.grammar.from.connection.ConnectionParser;
import org.finos.legend.engine.language.pure.grammar.from.domain.DomainParser;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.connection.ConnectionPointer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.PersistenceContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.context.PersistencePlatform;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.context.DefaultPersistencePlatform;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.service.ConnectionValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.service.PrimitiveTypeValue;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.service.ServiceParameter;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.persistence.service.ServiceParameterValue;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class PersistenceContextParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final ConnectionParser connectionParser;
    private final List<Function<PersistencePlatformSourceCode, PersistencePlatform>> processors;

    public PersistenceContextParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, ConnectionParser connectionParser, List<Function<PersistencePlatformSourceCode, PersistencePlatform>> processors)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.connectionParser = connectionParser;
        this.processors = processors;
    }

    /**********
     * persistence context
     **********/

    public PersistenceContext visitPersistenceContext(PersistenceParserGrammar.ContextContext ctx)
    {
        PersistenceContext context = new PersistenceContext();
        context.name = PureGrammarParserUtility.fromIdentifier(ctx.qualifiedName().identifier());
        context._package = ctx.qualifiedName().packagePath() == null ? "" : PureGrammarParserUtility.fromPath(ctx.qualifiedName().packagePath().identifier());
        context.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // persistence
        PersistenceParserGrammar.ContextPersistenceContext persistenceContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.contextPersistence(), "persistence", context.sourceInformation);
        context.persistence = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : persistenceContext.qualifiedName().packagePath().identifier(), persistenceContext.qualifiedName().identifier());

        // persistence platform
        PersistenceParserGrammar.ContextPlatformContext platformContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.contextPlatform(), "platform", context.sourceInformation);
        context.platform = platformContext == null ? new DefaultPersistencePlatform() : visitPersistencePlatform(platformContext);

        // service parameters
        PersistenceParserGrammar.ContextServiceParametersContext serviceParametersContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.contextServiceParameters(), "serviceParameters", context.sourceInformation);
        context.serviceParameters = serviceParametersContext ==  null ? Collections.emptyList() : ListIterate.collect(serviceParametersContext.serviceParameter(), this::visitServiceParameter);

        // sink connection
        PersistenceParserGrammar.ContextSinkConnectionContext sinkConnectionContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.contextSinkConnection(), "sinkConnection", context.sourceInformation);
        context.sinkConnection = sinkConnectionContext == null ? null : visitConnection(sinkConnectionContext, walkerSourceInformation.getSourceInformation(sinkConnectionContext));

        return context;
    }

    /**********
     * persistence platform
     **********/

    private PersistencePlatform visitPersistencePlatform(PersistenceParserGrammar.ContextPlatformContext ctx)
    {
        PersistenceParserGrammar.PlatformSpecificationContext specificationContext = ctx.platformSpecification();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        PersistencePlatformSourceCode sourceCode = new PersistencePlatformSourceCode(ctx.platformSpecification().getText(), specificationContext.platformType().getText(), sourceInformation, ParseTreeWalkerSourceInformation.offset(walkerSourceInformation, ctx.getStart()));
        return IPersistenceParserExtension.process(sourceCode, processors);
    }

    /**********
     * service parameter
     **********/

    private ServiceParameter visitServiceParameter(PersistenceParserGrammar.ServiceParameterContext ctx)
    {
        ServiceParameter serviceParameter = new ServiceParameter();
        serviceParameter.sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        // name
        serviceParameter.name = PureGrammarParserUtility.fromIdentifier(ctx.identifier());

        // value
        serviceParameter.value = visitServiceParameterValue(ctx, serviceParameter.sourceInformation);

        return serviceParameter;
    }

    private ServiceParameterValue visitServiceParameterValue(PersistenceParserGrammar.ServiceParameterContext ctx, SourceInformation sourceInformation)
    {
        if (ctx.primitiveValue() != null)
        {
            return visitPrimitiveValue(ctx.primitiveValue());
        }
        else if (ctx.connectionPointer() != null)
        {
            ConnectionValue value = new ConnectionValue();
            value.connection = visitConnectionPointer(ctx.connectionPointer());
            return value;
        }
        else if (ctx.embeddedConnection() != null)
        {
            ConnectionValue value = new ConnectionValue();
            value.connection = visitEmbeddedConnection(ctx.embeddedConnection());
            return value;
        }
        throw new EngineException("Unrecognized service parameter value", sourceInformation, EngineErrorType.PARSER);
    }

    private ServiceParameterValue visitPrimitiveValue(PersistenceParserGrammar.PrimitiveValueContext ctx)
    {
        DomainParser parser = new DomainParser();
        int startLine = ctx.getStart().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.getStart().getCharPositionInLine();
        ParseTreeWalkerSourceInformation serviceParamSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).build();

        PrimitiveTypeValue primitiveTypeValue = new PrimitiveTypeValue();
        primitiveTypeValue.primitiveType = parser.parsePrimitiveValue(ctx.getText(), serviceParamSourceInformation, null);
        return primitiveTypeValue;
    }

    /**********
     * connection
     **********/

    private Connection visitConnection(PersistenceParserGrammar.ContextSinkConnectionContext ctx, SourceInformation sourceInformation)
    {
        if (ctx.connectionPointer() != null)
        {
            return visitConnectionPointer(ctx.connectionPointer());
        }
        else if (ctx.embeddedConnection() != null)
        {
            return visitEmbeddedConnection(ctx.embeddedConnection());
        }
        throw new EngineException("Unrecognized connection", sourceInformation, EngineErrorType.PARSER);
    }

    private ConnectionPointer visitConnectionPointer(PersistenceParserGrammar.ConnectionPointerContext ctx)
    {
        ConnectionPointer connectionPointer = new ConnectionPointer();
        connectionPointer.connection = PureGrammarParserUtility.fromQualifiedName(ctx.qualifiedName().packagePath() == null ? Collections.emptyList() : ctx.qualifiedName().packagePath().identifier(), ctx.qualifiedName().identifier());
        connectionPointer.sourceInformation = walkerSourceInformation.getSourceInformation(ctx.qualifiedName());
        return connectionPointer;
    }

    private Connection visitEmbeddedConnection(PersistenceParserGrammar.EmbeddedConnectionContext ctx)
    {
        StringBuilder embeddedConnectionText = new StringBuilder();
        for (PersistenceParserGrammar.EmbeddedConnectionContentContext fragment : ctx.embeddedConnectionContent())
        {
            embeddedConnectionText.append(fragment.getText());
        }
        String embeddedConnectionParsingText = embeddedConnectionText.length() > 0 ? embeddedConnectionText.substring(0, embeddedConnectionText.length() - 2) : embeddedConnectionText.toString();
        // prepare island grammar walker source information
        int startLine = ctx.ISLAND_OPEN().getSymbol().getLine();
        int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
        // only add current walker source information column offset if this is the first line
        int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + ctx.ISLAND_OPEN().getSymbol().getCharPositionInLine() + ctx.ISLAND_OPEN().getSymbol().getText().length();
        ParseTreeWalkerSourceInformation embeddedConnectionWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(this.walkerSourceInformation.getReturnSourceInfo()).build();
        SourceInformation embeddedConnectionSourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        return this.connectionParser.parseEmbeddedRuntimeConnections(embeddedConnectionParsingText, embeddedConnectionWalkerSourceInformation, embeddedConnectionSourceInformation);
    }
}
