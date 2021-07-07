package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.deltalake;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.keys.DeltaLakeAuthenticationStrategyKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.DeltaLakeDataSourceSpecification;

import java.util.Properties;

public class DeltaLakeManager extends DatabaseManager
{
    @Override
    public MutableList<String> getIds()
    {
        return Lists.mutable.with("DeltaLake");
    }

    @Override
    public String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties, AuthenticationStrategy authenticationStrategy)
    {
        if (DeltaLakeAuthenticationStrategyKey.TYPE.equals(authenticationStrategy.getKey().type()))
        {
            return String.format("jdbc:spark://%s;transportMode=http;ssl=1;httpPath=%s;AuthMech=3;UID=token;PWD=%s",
                    DeltaLakeDataSourceSpecification.DELTALAKE_SHARD,
                    DeltaLakeDataSourceSpecification.DELTALAKE_HTTP_PATH,
                    DeltaLakeDataSourceSpecification.DELTALAKE_API_TOKEN
            );
        }
        throw new UnsupportedOperationException("Unsupported auth strategy :" + authenticationStrategy.getKey().type());
    }

    @Override
    public Properties getExtraDataSourceProperties(AuthenticationStrategy authenticationStrategy)
    {
        return new Properties();
    }

    @Override
    public String getDriver()
    {
        return "org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.deltalake.DeltaLakeDriver";
    }

    @Override
    public RelationalDatabaseCommands relationalDatabaseSupport()
    {
        return new DeltaLakeCommands();
    }

    @Override
    public boolean publishMetrics()
    {
        return false;
    }
}