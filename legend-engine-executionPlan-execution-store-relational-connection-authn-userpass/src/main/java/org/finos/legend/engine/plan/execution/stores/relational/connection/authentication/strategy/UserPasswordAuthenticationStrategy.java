package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionException;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.UserPasswordAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.pac4j.core.profile.CommonProfile;

import javax.security.auth.Subject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class UserPasswordAuthenticationStrategy extends AuthenticationStrategy
{
    private final String privateKeyVaultReference;
    private final String passPhraseVaultReference;
    private final String publicUserName;

    public UserPasswordAuthenticationStrategy(String privateKeyVaultReference, String passPhraseVaultReference, String publicUserName)
    {
        this.privateKeyVaultReference = privateKeyVaultReference;
        this.passPhraseVaultReference = passPhraseVaultReference;
        this.publicUserName = publicUserName;
    }

    @Override
    public String getLogin()
    {
        return this.publicUserName;
    }

    @Override
    public String getPassword()
    {
        return null;
    }

    @Override
    public Pair<String, Properties> handleConnection(String url, Properties properties, DatabaseManager databaseManager)
    {
        Properties connectionProperties = new Properties();
        connectionProperties.putAll(properties);
        connectionProperties.put("user", "maoo");
        connectionProperties.put("password", "XXXXX");
        return Tuples.pair(url, connectionProperties);
    }

    @Override
    protected Connection getConnectionImpl(DataSourceWithStatistics ds, Subject subject, MutableList<CommonProfile> profiles) throws ConnectionException
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
    public AuthenticationStrategyKey getKey()
    {
        return new UserPasswordAuthenticationStrategyKey(this.privateKeyVaultReference, this.passPhraseVaultReference, this.publicUserName);
    }
}
