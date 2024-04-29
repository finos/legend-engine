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
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.TrinoSSLSpecification;

import java.util.Objects;

public class TrinoDatasourceSpecificationKey
        implements DataSourceSpecificationKey
{
    public String host;
    public int port;
    public String catalog;
    public String schema;
    public String clientTags;

    /**
     *  SSL level properties
     */
    public TrinoSSLSpecification sslSpecification;


    private String toString(TrinoSSLSpecification sslSpecification)
    {
        if (sslSpecification != null)
        {
            return "TrinoSSLSpecification{" +
                    "ssl=" + sslSpecification.ssl +
                    ", trustStorePathVaultReference='" + sslSpecification.trustStorePathVaultReference + '\'' +
                    ", trustStorePasswordVaultReference='" + sslSpecification.trustStorePasswordVaultReference + '\'' +
                    '}';
        }
        return "_";
    }

    private String shortId(TrinoSSLSpecification sslSpecification)
    {
        if (sslSpecification != null)
        {
            return "TrinoSSLSpecification_" +
                    "ssl:" + sslSpecification.ssl + "_" +
                    "trustStorePathVaultReference:'" + sslSpecification.trustStorePathVaultReference + "_" +
                    "trustStorePasswordVaultReference:'" + sslSpecification.trustStorePasswordVaultReference;
        }
        return "_";
    }

    @Override
    public String shortId()
    {
        return "Trino_" +
            "host:" + host + "_" +
            "port:" + port + "_" +
            "catalog:" + catalog + "_" +
            "schema:" + schema + "_" +
            "clientTags:" + clientTags + "_" +
            "sslSpecification:" + shortId(this.sslSpecification) + "_";
    }

    @Override
    public String toString()
    {
        return "TrinoDatasourceSpecificationKey{" +
                "host='" + host + '\'' +
                ", port=" + port +
                ", catalog='" + catalog + '\'' +
                ", schema='" + schema + '\'' +
                ", clientTags='" + clientTags + '\'' +
                ", sslSpecification=" + toString(this.sslSpecification) +
                '}';
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
        TrinoDatasourceSpecificationKey that = (TrinoDatasourceSpecificationKey) o;
        return port == that.port && Objects.equals(host, that.host) && Objects.equals(catalog, that.catalog) && Objects.equals(schema, that.schema) && Objects.equals(clientTags, that.clientTags) && Objects.equals(sslSpecification.toString(), that.sslSpecification.toString());
    }

    public TrinoDatasourceSpecificationKey(String host, int port, String catalog, String schema, String clientTags, TrinoSSLSpecification sslSpecification)
    {
        this.host = host;
        this.port = port;
        this.catalog = catalog;
        this.schema = schema;
        this.clientTags = clientTags;
        this.sslSpecification = sslSpecification;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(host, port, catalog, schema, clientTags, sslSpecification.toString());
    }

    public String getHost()
    {
        return host;
    }

    public int getPort()
    {
        return port;
    }

    public String getCatalog()
    {
        return catalog;
    }

    public String getSchema()
    {
        return schema;
    }

    public String getClientTags()
    {
        return clientTags;
    }

    public TrinoSSLSpecification getSslSpecification()
    {
        return sslSpecification;
    }
}
