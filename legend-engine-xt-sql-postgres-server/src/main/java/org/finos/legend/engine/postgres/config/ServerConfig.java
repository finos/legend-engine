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

import org.finos.legend.engine.postgres.auth.AuthenticationMethodType;
import org.finos.legend.engine.postgres.auth.IdentityType;

public class ServerConfig
{
    private HandlerConfig handler;
    private Integer port;
    private AuthenticationMethodType authenticationMethod;
    private IdentityType identityType;
    private GSSConfig gss;

    public String getLogConfigFile()
    {
        return logConfigFile;
    }

    private String logConfigFile;

    public Integer getPort()
    {
        return port;
    }

    public void setPort(Integer port)
    {
        this.port = port;
    }

    public AuthenticationMethodType getAuthenticationMethod()
    {
        return authenticationMethod;
    }

    public IdentityType getIdentityType()
    {
        return identityType;
    }

    public GSSConfig getGss()
    {
        return gss;
    }

    public ServerConfig()
    {
    }

    public HandlerConfig getHandler()
    {
        return handler;
    }

    @Override
    public String toString()
    {
        return "ServerConfig{" +
                "handler=" + handler +
                ", port=" + port +
                ", authenticationMethod=" + authenticationMethod +
                ", identityType=" + identityType +
                '}';
    }
}
