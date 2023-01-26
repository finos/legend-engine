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

package org.finos.legend.engine.connection.jdbc;

import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.engine.IdentityProvider;
import org.finos.legend.engine.connection.ConnectionSpecification;
import org.finos.legend.engine.connection.ConnectionSpecificationProvider;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.DatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;
import scala.collection.immutable.Map;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Optional;

public class LegendPostgresConnectionProvider extends LegendJdbcConnectionProvider
{
    static
    {
        try
        {
            Class.forName("org.postgresql.Driver");
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private IdentityProvider identityProvider;
    private ConnectionSpecificationProvider connectionSpecificationProvider;

    public LegendPostgresConnectionProvider(IdentityProvider identityProvider, CredentialProviderProvider credentialProviderProvider, ConnectionSpecificationProvider datasourceSpecificationProvider)
    {
        super(credentialProviderProvider);
        this.identityProvider = identityProvider;
        this.connectionSpecificationProvider = datasourceSpecificationProvider;
    }

    public boolean canHandle(Driver driver, Map<String, String> options)
    {
        if (!options.contains("legend-connectionName"))
        {
            return false;
        }
        if (options.get("legend-connectionName").isEmpty())
        {
            return false;
        }
        return driver.getClass().getCanonicalName().equals("org.postgresql.Driver");
    }

    public Connection getConnection(Driver driver, Map<String, String> options) throws Exception
    {
        String legendDatasourceName = options.get("legend-connectionName").get();
        Optional<ConnectionSpecification> connectionSpecificationProvider = this.connectionSpecificationProvider.get(legendDatasourceName);
        if (!connectionSpecificationProvider.isPresent())
        {
            throw new RuntimeException("Datasource specification for key '" + legendDatasourceName + "' not found");
        }
        return handle(connectionSpecificationProvider.get());
    }

    private Connection handle(ConnectionSpecification connectionSpecification) throws Exception
    {
        DatasourceSpecification datasourceSpecification = connectionSpecification.datasourceSpecification;
        if (!(datasourceSpecification instanceof StaticDatasourceSpecification))
        {
            throw new UnsupportedOperationException("Unsupported specification of type " + connectionSpecification.getClass().getCanonicalName());
        }
        StaticDatasourceSpecification staticDatasourceSpecification = (StaticDatasourceSpecification)datasourceSpecification;

        AuthenticationSpecification authenticationSpecification = connectionSpecification.authenticationSpecification;

        Optional<Identity> identityHolder = this.identityProvider.getCurrentIdentity();
        if (!identityHolder.isPresent())
        {
            throw new RuntimeException("Cannot create connection. Current identity not set");
        }
        PlaintextUserPasswordCredential credential = (PlaintextUserPasswordCredential) super.makeCredential(authenticationSpecification, identityHolder.get());
        String url = "jdbc:postgresql://" + staticDatasourceSpecification.host + ":" + staticDatasourceSpecification.port;
        if (staticDatasourceSpecification.databaseName != null)
        {
            url += "/" + staticDatasourceSpecification.databaseName;
        }
        return DriverManager.getConnection(url, credential.getUser(), credential.getPassword());
    }
}
