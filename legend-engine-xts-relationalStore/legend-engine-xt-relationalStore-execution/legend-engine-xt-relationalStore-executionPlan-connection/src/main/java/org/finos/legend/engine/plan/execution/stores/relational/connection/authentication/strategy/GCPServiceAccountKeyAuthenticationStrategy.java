// Copyright 2026 Goldman Sachs
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionException;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.GCPServiceAccountKeyAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.vault.Vault;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/*
    Authentication with a GCP service account key held in a vault. The vault reference resolves to the
    key file verbatim - the JSON Google hands out when a key is created - and the account email and
    private key are read out of it and given to the driver, which signs its own assertion. Unlike
    Application Default Credentials this needs nothing present in the ambient environment.

    See https://cloud.google.com/iam/docs/service-account-creds
 */
public class GCPServiceAccountKeyAuthenticationStrategy extends AuthenticationStrategy
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String CLIENT_EMAIL = "client_email";
    private static final String PRIVATE_KEY = "private_key";

    private final String serviceAccountKeyVaultReference;

    public GCPServiceAccountKeyAuthenticationStrategy(String serviceAccountKeyVaultReference)
    {
        this.serviceAccountKeyVaultReference = serviceAccountKeyVaultReference;
    }

    public String getServiceAccountKeyVaultReference()
    {
        return serviceAccountKeyVaultReference;
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
        String serviceAccountKey = Vault.INSTANCE.getValue(this.serviceAccountKeyVaultReference);
        if (serviceAccountKey == null)
        {
            throw new RuntimeException("Can't find the service account key in the vault");
        }

        JsonNode key;
        try
        {
            key = OBJECT_MAPPER.readTree(serviceAccountKey);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Service account key in the vault is not valid JSON", e);
        }
        if (!key.hasNonNull(CLIENT_EMAIL) || !key.hasNonNull(PRIVATE_KEY))
        {
            throw new RuntimeException("Service account key in the vault is missing '" + CLIENT_EMAIL + "' or '" + PRIVATE_KEY + "'");
        }

        Properties connectionProperties = new Properties();
        connectionProperties.putAll(properties);
        connectionProperties.put("OAuthType", "0");
        connectionProperties.put("OAuthServiceAcctEmail", key.get(CLIENT_EMAIL).asText());
        connectionProperties.put("OAuthPvtKey", key.get(PRIVATE_KEY).asText());
        return Tuples.pair(url, connectionProperties);
    }

    @Override
    public AuthenticationStrategyKey getKey()
    {
        return new GCPServiceAccountKeyAuthenticationStrategyKey(this.serviceAccountKeyVaultReference);
    }
}
