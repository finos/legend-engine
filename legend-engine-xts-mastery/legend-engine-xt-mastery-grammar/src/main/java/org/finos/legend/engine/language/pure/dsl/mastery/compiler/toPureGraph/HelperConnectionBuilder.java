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

package org.finos.legend.engine.language.pure.dsl.mastery.compiler.toPureGraph;

import org.eclipse.collections.api.block.function.Function2;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.impl.utility.ListIterate;
import org.finos.legend.engine.language.pure.compiler.toPureGraph.CompileContext;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.authentication.AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.Connection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.FTPConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.HTTPConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.KafkaConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.mastery.connection.Proxy;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_authentication_AuthenticationStrategy;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_connection_Connection;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_connection_FTPConnection;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_connection_FTPConnection_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_connection_HTTPConnection;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_connection_HTTPConnection_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_connection_KafkaConnection;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_connection_KafkaConnection_Impl;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_connection_Proxy;
import org.finos.legend.pure.generated.Root_meta_pure_mastery_metamodel_connection_Proxy_Impl;

import java.util.List;

public class HelperConnectionBuilder
{

    public static Root_meta_pure_mastery_metamodel_connection_Connection buildConnection(Connection connection, CompileContext context)
    {

        if (connection instanceof FTPConnection)
        {
            return buildFTPConnection((FTPConnection) connection, context);
        }

        else if (connection instanceof KafkaConnection)
        {
            return buildKafkaConnection((KafkaConnection) connection, context);
        }

        else if (connection instanceof HTTPConnection)
        {
            return buildHTTPConnection((HTTPConnection) connection, context);
        }

        return null;
    }

    public static Root_meta_pure_mastery_metamodel_connection_FTPConnection buildFTPConnection(FTPConnection connection, CompileContext context)
    {
        return new Root_meta_pure_mastery_metamodel_connection_FTPConnection_Impl(connection.name)
                ._name(connection.name)
                ._secure(connection.secure)
                ._host(connection.host)
                ._port(connection.port)
                ._authentication(buildAuthentication(connection.authenticationStrategy, context));

    }

    public static Root_meta_pure_mastery_metamodel_connection_HTTPConnection buildHTTPConnection(HTTPConnection connection, CompileContext context)
    {

        Root_meta_pure_mastery_metamodel_connection_HTTPConnection httpConnection = new Root_meta_pure_mastery_metamodel_connection_HTTPConnection_Impl(connection.name)
                ._name(connection.name)
                ._proxy(buildProxy(connection.proxy, context))
                ._url(connection.url)
                ._authentication(buildAuthentication(connection.authenticationStrategy, context));

        return httpConnection;

    }

    public static Root_meta_pure_mastery_metamodel_connection_KafkaConnection buildKafkaConnection(KafkaConnection connection, CompileContext context)
    {

        return new Root_meta_pure_mastery_metamodel_connection_KafkaConnection_Impl(connection.name)
                ._name(connection.name)
                ._topicName(connection.topicName)
                ._topicUrls(Lists.fixedSize.ofAll(connection.topicUrls))
                ._authentication(buildAuthentication(connection.authenticationStrategy, context));

    }

    private static Root_meta_pure_mastery_metamodel_connection_Proxy buildProxy(Proxy proxy, CompileContext context)
    {
        if (proxy == null)
        {
            return null;
        }

       return new Root_meta_pure_mastery_metamodel_connection_Proxy_Impl("")
                ._host(proxy.host)
                ._port(proxy.port)
                ._authentication(buildAuthentication(proxy.authenticationStrategy, context));
    }

    private static Root_meta_pure_mastery_metamodel_authentication_AuthenticationStrategy buildAuthentication(AuthenticationStrategy authenticationStrategy, CompileContext context)
    {
        if (authenticationStrategy == null)
        {
            return null;
        }

        return IMasteryCompilerExtension.process(authenticationStrategy, authProcessors(), context);
    }

    private static List<Function2<AuthenticationStrategy, CompileContext, Root_meta_pure_mastery_metamodel_authentication_AuthenticationStrategy>> authProcessors()
    {
        List<IMasteryCompilerExtension> extensions = IMasteryCompilerExtension.getExtensions();
        return ListIterate.flatCollect(extensions, IMasteryCompilerExtension::getExtraAuthenticationStrategyProcessors);
    }
}
