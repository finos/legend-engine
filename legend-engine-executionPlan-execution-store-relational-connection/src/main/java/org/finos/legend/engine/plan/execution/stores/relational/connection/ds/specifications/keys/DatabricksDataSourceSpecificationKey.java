// Copyright 2021 Databricks
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys;

import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecificationKey;

import java.util.Objects;

public class DatabricksDataSourceSpecificationKey implements DataSourceSpecificationKey
{
    private final String hostname;
    private final String port;
    private final String protocol;
    private final String httpPath;

    public DatabricksDataSourceSpecificationKey(String hostname, String port, String protocol, String httpPath)
    {
        this.hostname = hostname;
        this.port = port;
        this.protocol = protocol;
        this.httpPath = httpPath;
    }

    public String getHostname()
    {
        return hostname;
    }

    public String getPort()
    {
        return port;
    }

    public String getProtocol()
    {
        return protocol;
    }

    public String getHttpPath()
    {
        return httpPath;
    }

    @Override
    public String toString()
    {
        return "DatabricksDataSourceSpecificationKey{" +
                "hostname='" + hostname + '\'' +
                "port='" + port + '\'' +
                "protocol='" + protocol + '\'' +
                "httpPath='" + httpPath + '\'' +
                '}';
    }

    @Override
    public String shortId()
    {
        return "Databricks_" +
                "hostname:" + hostname + "_" +
                "port:" + port + "_" +
                "protocol:" + protocol + "_" +
                "httpPath:" + httpPath;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        DatabricksDataSourceSpecificationKey that = (DatabricksDataSourceSpecificationKey) o;
        return Objects.equals(hostname, that.hostname)
                && Objects.equals(port, that.port)
                && Objects.equals(protocol, that.protocol)
                && Objects.equals(httpPath, that.httpPath);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(hostname, port, protocol, httpPath);
    }
}
