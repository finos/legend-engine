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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LegendHandlerConfig implements HandlerConfig
{

    private String protocol;
    private String host;
    private String port;

    public LegendHandlerConfig()
    {
        // DO NOT DELETE: this resets the default constructor for Jackson
    }

    public LegendHandlerConfig(String protocol, String host, String port, String projectId, String sessionCookie)
    {
        this.protocol = protocol;
        this.host = host;
        this.port = port;
    }

    public String getProtocol()
    {
        return protocol;
    }

    public String getHost()
    {
        return host;
    }

    public String getPort()
    {
        return port;
    }

    @Override
    public HandlerType getType()
    {
        return HandlerType.LEGEND;
    }

    @Override
    public String toString()
    {
        return "LegendHandlerConfig{" +
                "protocol='" + protocol + '\'' +
                ", host='" + host + '\'' +
                ", port='" + port + '\'' +
                '}';
    }
}
