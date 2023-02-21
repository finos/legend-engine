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

package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionException;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.ApiTokenAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.IdentityState;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.ApiTokenCredential;
import org.finos.legend.engine.shared.core.vault.Vault;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class ApiTokenAuthenticationStrategy extends AuthenticationStrategy
{

    private final String apiToken;

    public ApiTokenAuthenticationStrategy(String apiToken)
    {
        this.apiToken = apiToken;
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
        ApiTokenCredential apiTokenCredential = this.resolveCredential(properties, this.apiToken);
        Properties connectionProperties = new Properties();
        connectionProperties.put("PWD", apiTokenCredential.getApiToken());
        return Tuples.pair(url, connectionProperties);
    }

    @Override
    public AuthenticationStrategyKey getKey()
    {
        return new ApiTokenAuthenticationStrategyKey(this.apiToken);
    }

    private ApiTokenCredential resolveCredential(Properties properties, String apiTokenKey)
    {
        IdentityState identityState = ConnectionStateManager.getInstance().getIdentityStateUsing(properties);
        if (!identityState.getCredentialSupplier().isPresent())
        {
            String apiToken = Vault.INSTANCE.getValue(apiTokenKey);
            if (StringUtils.isEmpty(apiToken))
            {
                throw new ConnectionException(new Exception("Could not retrieve API token from default Vault"));
            }
            return new ApiTokenCredential(apiToken);
        }
        return (ApiTokenCredential) super.getDatabaseCredential(identityState);
    }

}