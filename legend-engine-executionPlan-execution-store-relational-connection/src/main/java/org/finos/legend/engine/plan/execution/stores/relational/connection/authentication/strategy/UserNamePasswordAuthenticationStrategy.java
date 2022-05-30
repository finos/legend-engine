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
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.UserNamePasswordAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.IdentityState;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.PlaintextUserPasswordCredential;
import org.finos.legend.engine.shared.core.identity.credential.PrivateKeyCredential;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PrivateKey;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class UserNamePasswordAuthenticationStrategy extends AuthenticationStrategy
{
    private static final Logger LOGGER = LoggerFactory.getLogger(UserNamePasswordAuthenticationStrategy.class);

    private final String userNameVaultReference;
    private final String passwordVaultReference;

    public UserNamePasswordAuthenticationStrategy(String userNameVaultReference, String passwordVaultReference)
    {
        this.userNameVaultReference = userNameVaultReference;
        this.passwordVaultReference = passwordVaultReference;
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
        Properties connectionProperties = new Properties();
        connectionProperties.putAll(properties);

        // IdentityState identityState = ConnectionStateManager.getInstance().getIdentityStateUsing(properties);
        // PlaintextUserPasswordCredential credential = (PlaintextUserPasswordCredential) getDatabaseCredential(identityState);

        PlaintextUserPasswordCredential credential = this.resolveCredential(properties, this.userNameVaultReference, this.passwordVaultReference);
        LOGGER.info("UserNamePasswordAuthenticationStrategy.handleConnections is here");
        LOGGER.info("UserNamePasswordAuthenticationStrategy.handleConnections got credential {}", credential.getUser());
        LOGGER.info("UserNamePasswordAuthenticationStrategy.handleConnections connection properties {}", connectionProperties);

        connectionProperties.put("user", credential.getUser());
        connectionProperties.put("password", credential.getPassword());
        return Tuples.pair(url, connectionProperties);
    }

    private PlaintextUserPasswordCredential resolveCredential(Properties properties, String userNameVaultReference, String passwordVaultReference)
    {
        LOGGER.info("UserNamePasswordAuthenticationStrategy.resolveCredential for {} and {}", userNameVaultReference, passwordVaultReference);
        IdentityState identityState = ConnectionStateManager.getInstance().getIdentityStateUsing(properties);
        if (!identityState.getCredentialSupplier().isPresent())
        {
            String username = Vault.INSTANCE.getValue(userNameVaultReference);
            String password = Vault.INSTANCE.getValue(passwordVaultReference);

            return new PlaintextUserPasswordCredential(username, password);
        }
        return (PlaintextUserPasswordCredential) getDatabaseCredential(identityState);
    }

    @Override
    public AuthenticationStrategyKey getKey()
    {
        return new UserNamePasswordAuthenticationStrategyKey(this.userNameVaultReference, this.passwordVaultReference);
    }
}
