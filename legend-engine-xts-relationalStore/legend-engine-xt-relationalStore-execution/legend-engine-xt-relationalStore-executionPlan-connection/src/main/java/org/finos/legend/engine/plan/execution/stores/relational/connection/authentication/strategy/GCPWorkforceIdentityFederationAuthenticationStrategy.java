package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy;

import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionException;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.GCPWorkforceIdentityFederationAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.IdentityState;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.credential.OAuthCredential;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class GCPWorkforceIdentityFederationAuthenticationStrategy extends AuthenticationStrategy {
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
        connectionProperties.put("OAuthType", "2");
        return Tuples.pair(url, connectionProperties);
    }

    private OAuthCredential resolveCredential(Properties properties)
    {
        IdentityState identityState = ConnectionStateManager.getInstance().getIdentityStateUsing(properties);
        if (!identityState.getCredentialSupplier().isPresent())
        {
            throw new RuntimeException("Credential Supplier missing for GCPWorkforceIdentityFederationAuthenticationStrategy");
        }
        return (OAuthCredential) super.getDatabaseCredential(identityState);
    }

    @Override
    public AuthenticationStrategyKey getKey() {
        return new GCPWorkforceIdentityFederationAuthenticationStrategyKey();
    }
}
