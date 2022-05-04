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

package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionException;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.GCPWorkloadIdentityFederationAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.IdentityState;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.OAuthCredential;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

public class GCPWorkloadIdentityFederationAuthenticationStrategy extends AuthenticationStrategy {
    private String serviceAccountEmail;
    private List<String> additionalGcpScopes;

    public GCPWorkloadIdentityFederationAuthenticationStrategy(String serviceAccountEmail, List<String> additionalGcpScopes) {
        this.serviceAccountEmail = serviceAccountEmail;
        this.additionalGcpScopes = additionalGcpScopes;
    }

    public String getServiceAccountEmail() {
        return serviceAccountEmail;
    }

    public List<String> getAdditionalGcpScopes() {
        return additionalGcpScopes;
    }

    @Override
    public Connection getConnectionImpl(DataSourceWithStatistics ds, Identity identity) throws ConnectionException
    {
        try
        {
            return ds.getDataSource().getConnection();
        }
        catch (SQLException e)
        {
            throw new ConnectionException(e);
        }
    }

    @Override
    public Pair<String, Properties> handleConnection(String url, Properties properties, DatabaseManager databaseManager)
    {
        OAuthCredential oAuthCredential = this.resolveCredential(properties);
        Properties connectionProperties = new Properties();
        connectionProperties.putAll(properties);
        connectionProperties.put("OAuthAccessToken", oAuthCredential.getAccessToken());
        connectionProperties.put("OAuthType","2");
        return Tuples.pair(url, connectionProperties);
    }

    private OAuthCredential resolveCredential(Properties properties)
    {
        IdentityState identityState = ConnectionStateManager.getInstance().getIdentityStateUsing(properties);
        if (!identityState.getCredentialSupplier().isPresent())
        {
            throw new RuntimeException("Credential Supplier missing for GCPWorkloadIdentityFederationAuthenticationStrategy");
        }
        return (OAuthCredential) super.getDatabaseCredential(identityState);
    }

    @Override
    public AuthenticationStrategyKey getKey()
    {
        return new GCPWorkloadIdentityFederationAuthenticationStrategyKey(this.serviceAccountEmail, this.additionalGcpScopes);
    }
}
