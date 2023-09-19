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

package org.finos.legend.engine.language.pure.dsl.mastery.grammar.from.connection;

import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.dsl.mastery.grammar.from.IMasteryParserExtension;
import org.finos.legend.engine.language.pure.dsl.mastery.grammar.from.SpecificationSourceCode;
import org.finos.legend.engine.language.pure.grammar.from.ParseTreeWalkerSourceInformation;
import org.finos.legend.engine.language.pure.grammar.from.PureGrammarParserUtility;
import org.finos.legend.engine.language.pure.grammar.from.antlr4.connection.MasteryConnectionParserGrammar;
import org.finos.legend.engine.protocol.pure.v1.model.SourceInformation;
import org.finos.legend.engine.protocol.pure.v1.model.context.EngineErrorType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.FTPConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.HTTPConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.KafkaConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.Proxy;
import org.finos.legend.engine.shared.core.operational.errorManagement.EngineException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.function.Function;

import static java.lang.String.format;

public class ConnectionParseTreeWalker
{
    private final ParseTreeWalkerSourceInformation walkerSourceInformation;
    private final List<Function<SpecificationSourceCode, AuthenticationStrategy>> authenticationProcessors;

    public ConnectionParseTreeWalker(ParseTreeWalkerSourceInformation walkerSourceInformation, List<Function<SpecificationSourceCode, AuthenticationStrategy>> authenticationProcessors)
    {
        this.walkerSourceInformation = walkerSourceInformation;
        this.authenticationProcessors = authenticationProcessors;
    }

    /**********
     * connection
     **********/

    public Connection visitConnection(MasteryConnectionParserGrammar ctx)
    {
        MasteryConnectionParserGrammar.DefinitionContext definitionContext =  ctx.definition();
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(definitionContext);


        if (definitionContext.ftpConnection() != null)
        {
            return visitFtpConnection(definitionContext.ftpConnection());
        }
        else if (definitionContext.httpConnection() != null)
        {
            return visitHttpConnection(definitionContext.httpConnection());
        }

        else if (definitionContext.kafkaConnection() != null)
        {
            return visitKafkaConnection(definitionContext.kafkaConnection());
        }

        throw new EngineException("Unrecognized element", sourceInformation, EngineErrorType.PARSER);
    }

    /**********
     * ftp connection
     **********/

    private FTPConnection visitFtpConnection(MasteryConnectionParserGrammar.FtpConnectionContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        FTPConnection ftpConnection = new FTPConnection();

        // host
        MasteryConnectionParserGrammar.HostContext hostContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.host(), "host", sourceInformation);
        ftpConnection.host = validateUri(PureGrammarParserUtility.fromGrammarString(hostContext.STRING().getText(), true), sourceInformation);

        // port
        MasteryConnectionParserGrammar.PortContext portContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.port(), "port", sourceInformation);
        ftpConnection.port = Integer.parseInt(portContext.INTEGER().getText());

        // authentication
        MasteryConnectionParserGrammar.AuthenticationContext authenticationContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.authentication(), "authentication", sourceInformation);
        if (authenticationContext != null)
        {
            ftpConnection.authenticationStrategy = visitAuthentication(authenticationContext);
        }

        // secure
        MasteryConnectionParserGrammar.SecureContext secureContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.secure(), "secure", sourceInformation);
        if (secureContext != null)
        {
            ftpConnection.secure = Boolean.parseBoolean(secureContext.booleanValue().getText());
        }

        return ftpConnection;
    }


    /**********
     * http connection
     **********/

    private HTTPConnection visitHttpConnection(MasteryConnectionParserGrammar.HttpConnectionContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        HTTPConnection httpConnection = new HTTPConnection();

        // url
        MasteryConnectionParserGrammar.UrlContext urlContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.url(), "url", sourceInformation);
        httpConnection.url = PureGrammarParserUtility.fromGrammarString(urlContext.STRING().getText(), true);

        try
        {
            new URL(httpConnection.url);
        }
        catch (MalformedURLException malformedURLException)
        {
            throw new EngineException(format("Invalid url: %s", httpConnection.url), sourceInformation, EngineErrorType.PARSER, malformedURLException);
        }

        // authentication
        MasteryConnectionParserGrammar.AuthenticationContext authenticationContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.authentication(), "authentication", sourceInformation);
        if (authenticationContext != null)
        {
            httpConnection.authenticationStrategy = visitAuthentication(authenticationContext);
        }

        // proxy
        MasteryConnectionParserGrammar.ProxyContext proxyContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.proxy(), "proxy", sourceInformation);
        if (proxyContext != null)
        {
            httpConnection.proxy = visitProxy(proxyContext);
        }

        return httpConnection;
    }

    /**********
     * ftp connection
     **********/
    private KafkaConnection visitKafkaConnection(MasteryConnectionParserGrammar.KafkaConnectionContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
        KafkaConnection kafkaConnection = new KafkaConnection();

        // topicName
        MasteryConnectionParserGrammar.TopicNameContext topicNameContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.topicName(), "topicName", sourceInformation);
        kafkaConnection.topicName = PureGrammarParserUtility.fromGrammarString(topicNameContext.STRING().getText(), true);

        // topicUrls
        MasteryConnectionParserGrammar.TopicUrlsContext topicUrlsContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.topicUrls(), "topicUrls", sourceInformation);
        kafkaConnection.topicUrls = ListIterate.collect(topicUrlsContext.STRING(), node ->
                {
                    String uri = PureGrammarParserUtility.fromGrammarString(node.getText(), true);
                    return validateUri(uri, sourceInformation);
                }
        );
        // authentication
        MasteryConnectionParserGrammar.AuthenticationContext authenticationContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.authentication(), "authentication", sourceInformation);
        if (authenticationContext != null)
        {
            kafkaConnection.authenticationStrategy = visitAuthentication(authenticationContext);
        }

        return kafkaConnection;
    }

    private Proxy visitProxy(MasteryConnectionParserGrammar.ProxyContext ctx)
    {
        SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);

        Proxy proxy = new Proxy();

        // host
        MasteryConnectionParserGrammar.HostContext hostContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.host(), "host", sourceInformation);
        proxy.host = validateUri(PureGrammarParserUtility.fromGrammarString(hostContext.STRING().getText(), true), sourceInformation);

        // port
        MasteryConnectionParserGrammar.PortContext portContext = PureGrammarParserUtility.validateAndExtractRequiredField(ctx.port(), "port", sourceInformation);
        proxy.port = Integer.parseInt(portContext.INTEGER().getText());

        // authentication
        MasteryConnectionParserGrammar.AuthenticationContext authenticationContext = PureGrammarParserUtility.validateAndExtractOptionalField(ctx.authentication(), "authentication", sourceInformation);
        if (authenticationContext != null)
        {
            proxy.authenticationStrategy = visitAuthentication(authenticationContext);
        }

        return proxy;
    }
    
    private AuthenticationStrategy visitAuthentication(MasteryConnectionParserGrammar.AuthenticationContext ctx)
    {
        SpecificationSourceCode specificationSourceCode = extraSpecificationCode(ctx.islandSpecification(), walkerSourceInformation);
        return IMasteryParserExtension.process(specificationSourceCode, authenticationProcessors, "authentication");
    }

    private SpecificationSourceCode extraSpecificationCode(MasteryConnectionParserGrammar.IslandSpecificationContext ctx, ParseTreeWalkerSourceInformation walkerSourceInformation)
    {
        StringBuilder text = new StringBuilder();
        MasteryConnectionParserGrammar.IslandValueContext islandValueContext = ctx.islandValue();
        if (islandValueContext != null)
        {
            for (MasteryConnectionParserGrammar.IslandValueContentContext fragment : islandValueContext.islandValueContent())
            {
                text.append(fragment.getText());
            }
            String textToParse = text.length() > 0 ? text.substring(0, text.length() - 2) : text.toString();

            // prepare island grammar walker source information
            int startLine = islandValueContext.ISLAND_OPEN().getSymbol().getLine();
            int lineOffset = walkerSourceInformation.getLineOffset() + startLine - 1;
            // only add current walker source information column offset if this is the first line
            int columnOffset = (startLine == 1 ? walkerSourceInformation.getColumnOffset() : 0) + islandValueContext.ISLAND_OPEN().getSymbol().getCharPositionInLine() + islandValueContext.ISLAND_OPEN().getSymbol().getText().length();
            ParseTreeWalkerSourceInformation triggerValueWalkerSourceInformation = new ParseTreeWalkerSourceInformation.Builder(walkerSourceInformation.getSourceId(), lineOffset, columnOffset).withReturnSourceInfo(walkerSourceInformation.getReturnSourceInfo()).build();
            SourceInformation triggerValueSourceInformation = walkerSourceInformation.getSourceInformation(ctx);

            return new SpecificationSourceCode(textToParse, ctx.islandType().getText(), triggerValueSourceInformation, triggerValueWalkerSourceInformation);
        }
        else
        {
            SourceInformation sourceInformation = walkerSourceInformation.getSourceInformation(ctx);
            return new SpecificationSourceCode(text.toString(), ctx.islandType().getText(), sourceInformation, walkerSourceInformation);
        }
    }

    private String validateUri(String uri, SourceInformation sourceInformation)
    {
        try
        {
            new URI(uri);
        }
        catch (URISyntaxException uriSyntaxException)
        {
            throw new EngineException(format("Invalid uri: %s", uri), sourceInformation, EngineErrorType.PARSER, uriSyntaxException);
        }

        return uri;
    }
}