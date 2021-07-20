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

public class RedshiftDataSourceSpecificationKey implements DataSourceSpecificationKey
{
    private final String databaseName;
    private final String endpoint;
    private final int port;


    public RedshiftDataSourceSpecificationKey(String databaseName, String endpoint, int port)
    {
        this.databaseName = databaseName;
        this.endpoint = endpoint;
        this.port = port;
    }

    public String getDatabaseName()
    {
        return databaseName;
    }

    public String getEndpoint()
    {
        return endpoint;
    }

    public int getPort()
    {
        return port;
    }

    @Override
    public String toString()
    {
        return "RedshiftDataSourceSpecificationKey{" +
                "databaseName='" + databaseName + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", port='" + port + '\'' +
                '}';
    }

    @Override
    public String shortId()
    {
        return "Redshift_" +
                "databaseName:" + databaseName + "_" +
                "endpoint:" + endpoint + "_" +
                "port:" + port;
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
        RedshiftDataSourceSpecificationKey that = (RedshiftDataSourceSpecificationKey) o;
        return Objects.equals(databaseName, that.databaseName) &&
                Objects.equals(endpoint, that.endpoint) &&
                Objects.equals(port, that.port);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(databaseName, endpoint, port);
    }
}
