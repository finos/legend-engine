// Copyright 2021 Goldman Sachs
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

public class StaticDataSourceSpecificationKey implements DataSourceSpecificationKey
{
    private final String host;
    private final int port;
    private final String databaseName;

    public StaticDataSourceSpecificationKey(String host, int port, String databaseName)
    {
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    @Override
    public String toString()
    {
        return "StaticDataSourceSpecificationKey{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", databaseName='" + databaseName + '\'' +
                '}';
    }

    @Override
    public String shortId()
    {
        return "Static_" +
                "host:" + host + "_" +
                "port:" + port + "_" +
                "db:" + databaseName;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof StaticDataSourceSpecificationKey))
        {
            return false;
        }
        StaticDataSourceSpecificationKey that = (StaticDataSourceSpecificationKey) o;
        return port == that.port &&
                Objects.equals(host, that.host) &&
                Objects.equals(databaseName, that.databaseName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(host, port, databaseName);
    }

}
