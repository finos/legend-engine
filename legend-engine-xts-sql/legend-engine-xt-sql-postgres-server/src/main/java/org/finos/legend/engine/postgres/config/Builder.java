// Copyright 2023 Goldman Sachs
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

package org.finos.legend.engine.postgres.config;

import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.finos.legend.engine.postgres.SessionsFactory;
import org.finos.legend.engine.postgres.auth.AnonymousIdentityProvider;
import org.finos.legend.engine.postgres.auth.AuthenticationMethod;
import org.finos.legend.engine.postgres.auth.GSSAuthenticationMethod;
import org.finos.legend.engine.postgres.auth.IdentityProvider;
import org.finos.legend.engine.postgres.auth.IdentityType;
import org.finos.legend.engine.postgres.auth.KerberosIdentityProvider;
import org.finos.legend.engine.postgres.auth.NoPasswordAuthenticationMethod;
import org.finos.legend.engine.postgres.auth.UsernamePasswordAuthenticationMethod;
import org.finos.legend.engine.postgres.handler.jdbc.JDBCSessionFactory;
import org.finos.legend.engine.postgres.handler.legend.LegendSessionFactory;
import org.finos.legend.engine.postgres.handler.legend.LegendTdsClient;

public class Builder
{
    public static SessionsFactory buildSessionFactory(ServerConfig serverConfig)
    {
        if (serverConfig.getHandler().getType() == HandlerType.JDBC)
        {
            JDBCHandlerConfig config = (JDBCHandlerConfig) serverConfig.getHandler();
            return new JDBCSessionFactory(config.getConnectionString(), config.getUser(), config.getPassword());
        }
        else if (serverConfig.getHandler().getType() == HandlerType.LEGEND)
        {
            LegendHandlerConfig config = (LegendHandlerConfig) serverConfig.getHandler();
            LegendTdsClient client = new LegendTdsClient(config.getProtocol(), config.getHost(), config.getPort());
            return new LegendSessionFactory(client);
        }
        else
        {
            throw new UnsupportedOperationException("Handler type not supported :" + serverConfig.getHandler().getType());
        }
    }

    public static AuthenticationMethod buildAuthenticationMethod(ServerConfig serverConfig)
    {
        IdentityProvider identityProvider;
        if (serverConfig.getIdentityType() == IdentityType.KERBEROS)
        {
            identityProvider = new KerberosIdentityProvider();
        }
        else if (serverConfig.getIdentityType() == IdentityType.ANONYMOUS)
        {
            identityProvider = new AnonymousIdentityProvider();
        }
        else
        {
            throw new UnsupportedOperationException("Identity type not supported :" + serverConfig.getIdentityType());
        }

        switch (serverConfig.getAuthenticationMethod())
        {
            case PASSWORD:
                return new UsernamePasswordAuthenticationMethod(identityProvider);
            case NO_PASSWORD:
                return new NoPasswordAuthenticationMethod(identityProvider);
            case GSS:
                return new GSSAuthenticationMethod(identityProvider);
            default:
                throw new UnsupportedOperationException("Authentication Method not supported :" + serverConfig.getAuthenticationMethod());
        }
    }
}
