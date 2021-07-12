package org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionException;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.AuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.DeltaLakeAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.pac4j.core.profile.CommonProfile;

import javax.security.auth.Subject;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DeltaLakeAuthenticationStrategy extends AuthenticationStrategy
{

    public static String DELTALAKE_TOKEN = "legend_deltalake_token";
    private final String apiToken;

    public DeltaLakeAuthenticationStrategy(String apiToken)
    {
        this.apiToken = apiToken;
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

    public Pair<String, Properties> handleConnection(String url, Properties properties, DatabaseManager databaseManager)
    {
        Properties connectionProperties = new Properties();
        connectionProperties.putAll(properties);
        connectionProperties.put(DELTALAKE_TOKEN, this.apiToken);
        return Tuples.pair(url, connectionProperties);
    }

    @Override
    public String getAlternativePrincipal(MutableList<CommonProfile> profiles)
    {
        return "token";
    }

    @Override
    public AuthenticationStrategyKey getKey()
    {
        return new DeltaLakeAuthenticationStrategyKey(this.apiToken);
    }

    @Override
    public String getLogin()
    {
        return "token";
    }

    @Override
    public String getPassword()
    {
        return this.apiToken;
    }
}