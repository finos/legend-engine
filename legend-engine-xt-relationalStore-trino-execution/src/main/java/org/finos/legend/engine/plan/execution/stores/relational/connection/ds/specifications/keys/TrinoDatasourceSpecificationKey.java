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
    public boolean ssl;
    public String trustStorePathVaultReference;
    public String trustStorePasswordVaultReference;

    /**
     * Kerberos Auth related Properties
     */
    public String kerberosRemoteServiceName;
    public boolean kerberosUseCanonicalHostname;

    @Override
    public String shortId()
    {
        return "TrinoDatasourceSpecificationKey_" +
                "host:'" + host + "_" +
                ", port:" + port + "_" +
                ", trustStorePathVaultReference:'" + trustStorePathVaultReference + "_" +
                ", trustStorePasswordVaultReference:'" + trustStorePasswordVaultReference + "_" +
                ", clientTags:'" + clientTags + "_" +
                ", kerberosUseCanonicalHostname:" + kerberosUseCanonicalHostname;
    }

    public TrinoDatasourceSpecificationKey(String host, int port, String catalog, String schema, String clientTags, boolean ssl, String trustStorePathVaultReference, String trustStorePasswordVaultReference, String kerberosRemoteServiceName, boolean kerberosUseCanonicalHostname)
    {
        this.host = host;
        this.port = port;
        this.catalog = catalog;
        this.schema = schema;
        this.clientTags = clientTags;
        this.ssl = ssl;
        this.trustStorePathVaultReference = trustStorePathVaultReference;
        this.trustStorePasswordVaultReference = trustStorePasswordVaultReference;
        this.kerberosRemoteServiceName = kerberosRemoteServiceName;
        this.kerberosUseCanonicalHostname = kerberosUseCanonicalHostname;
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
        return port == that.port && ssl == that.ssl && kerberosUseCanonicalHostname == that.kerberosUseCanonicalHostname && Objects.equals(host, that.host) && Objects.equals(catalog, that.catalog) && Objects.equals(schema, that.schema) && Objects.equals(clientTags, that.clientTags) && Objects.equals(trustStorePathVaultReference, that.trustStorePathVaultReference) && Objects.equals(trustStorePasswordVaultReference, that.trustStorePasswordVaultReference) && Objects.equals(kerberosRemoteServiceName, that.kerberosRemoteServiceName);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(host, port, catalog, schema, clientTags, ssl, trustStorePathVaultReference, trustStorePasswordVaultReference, kerberosRemoteServiceName, kerberosUseCanonicalHostname);
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
                ", ssl=" + ssl +
                ", trustStorePathVaultReference='" + trustStorePathVaultReference + '\'' +
                ", trustStorePasswordVaultReference='" + trustStorePasswordVaultReference + '\'' +
                ", kerberosRemoteServiceName='" + kerberosRemoteServiceName + '\'' +
                ", kerberosUseCanonicalHostname=" + kerberosUseCanonicalHostname +
                '}';
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

    public String getClientTags()
    {
        return clientTags;
    }

    public boolean isSsl()
    {
        return ssl;
    }

    public String getTrustStorePathVaultReference()
    {
        return trustStorePathVaultReference;
    }

    public String getTrustStorePasswordVaultReference()
    {
        return trustStorePasswordVaultReference;
    }

    public String getKerberosRemoteServiceName()
    {
        return kerberosRemoteServiceName;
    }

    public boolean isKerberosUseCanonicalHostname()
    {
        return kerberosUseCanonicalHostname;
    }

    public String getSchema()
    {
        return schema;
    }
}
