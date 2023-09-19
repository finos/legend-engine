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

package org.finos.legend.engine.language.pure.dsl.mastery.grammar.to;

import org.eclipse.collections.api.block.function.Function3;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.FTPConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.HTTPConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.KafkaConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.Proxy;

import java.util.List;

import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertPath;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.convertString;
import static org.finos.legend.engine.language.pure.grammar.to.PureGrammarComposerUtility.getTabString;

public class HelperConnectionComposer
{

    public static String renderConnection(Connection connection, int indentLevel, PureGrammarComposerContext context)
    {

        if (connection instanceof FTPConnection)
        {
            return renderFTPConnection((FTPConnection) connection, indentLevel, context);
        }

        else if (connection instanceof KafkaConnection)
        {
            return renderKafkaConnection((KafkaConnection) connection, indentLevel, context);
        }

        else if (connection instanceof HTTPConnection)
        {
            return renderHTTPConnection((HTTPConnection) connection, indentLevel, context);
        }

        return null;
    }

    public static String renderFTPConnection(FTPConnection connection, int indentLevel, PureGrammarComposerContext context)
    {
        return "MasteryConnection " + convertPath(connection.getPath()) + "\n" +
                "{\n" +
                getTabString(indentLevel + 1) + "specification: FTP #{\n" +
                getTabString(indentLevel + 2) + "host: " + convertString(connection.host, true) + ";\n" +
                getTabString(indentLevel + 2) + "port: " + connection.port + ";\n" +
                renderSecure(connection.secure, indentLevel + 2) +
                renderAuthentication(connection.authenticationStrategy, indentLevel + 2, context) +
                getTabString(indentLevel + 1) + "}#;\n" +
                "}";
    }

    public static String renderSecure(Boolean secure, int indentLevel)
    {
        if (secure == null)
        {
            return "";
        }

        return getTabString(indentLevel) + "secure: " + secure + ";\n";
    }

    public static String renderHTTPConnection(HTTPConnection connection, int indentLevel, PureGrammarComposerContext context)
    {

        return "MasteryConnection " + convertPath(connection.getPath()) + "\n" +
                "{\n" +
                getTabString(indentLevel + 1) + "specification: HTTP #{\n" +
                getTabString(indentLevel + 2) + "url: " + convertString(connection.url, true) + ";\n" +
                renderProxy(connection.proxy, indentLevel + 2, context) +
                renderAuthentication(connection.authenticationStrategy, indentLevel + 2, context) +
                getTabString(indentLevel + 1) + "}#;\n" +
                "}";

    }

    public static String renderKafkaConnection(KafkaConnection connection, int indentLevel, PureGrammarComposerContext context)
    {

        return "MasteryConnection " + convertPath(connection.getPath()) + "\n" +
                "{\n" +
                getTabString(indentLevel + 1) + "specification: Kafka #{\n" +
                getTabString(indentLevel + 2) + "topicName: " + convertString(connection.topicName, true) + ";\n" +
                renderTopicUrl(connection.topicUrls, indentLevel + 2) +
                renderAuthentication(connection.authenticationStrategy, indentLevel + 2, context) +
                getTabString(indentLevel + 1) + "}#;\n" +
                "}";
    }

    public static String renderTopicUrl(List<String> topicUrls, int indentLevel)
    {

        return getTabString(indentLevel) + "topicUrls: [\n"
            + String.join(",\n", ListIterate.collect(topicUrls, url ->  getTabString(indentLevel + 1) + convertString(url, true))) + "\n"
            + getTabString(indentLevel) + "];\n";
    }

    private static String renderProxy(Proxy proxy, int indentLevel, PureGrammarComposerContext pureGrammarComposerContext)
    {
        if (proxy == null)
        {
            return "";
        }

        return getTabString(indentLevel) + "proxy: {\n"
                + getTabString(indentLevel + 1) + "host: " + convertString(proxy.host, true) + ";\n"
                + getTabString(indentLevel + 1) + "port: " + proxy.port + ";\n"
                + renderAuthentication(proxy.authenticationStrategy, indentLevel + 1, pureGrammarComposerContext)
                + getTabString(indentLevel) + "};\n";
    }

    private static String renderAuthentication(AuthenticationStrategy authenticationStrategy, int indentLevel, PureGrammarComposerContext context)
    {
        if (authenticationStrategy == null)
        {
            return "";
        }

        String text = IMasteryComposerExtension.process(authenticationStrategy, authComposers(context), indentLevel, context);
        return getTabString(indentLevel) + "authentication: " + text;
    }

    private static List<Function3<AuthenticationStrategy, Integer, PureGrammarComposerContext, String>> authComposers(PureGrammarComposerContext context)
    {
        List<IMasteryComposerExtension> extensions = IMasteryComposerExtension.getExtensions(context);
        return ListIterate.flatCollect(extensions, IMasteryComposerExtension::getExtraAuthenticationStrategyComposers);
    }
}
