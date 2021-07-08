package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.deltalake;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.DeltaLakeDataSourceSpecification;
import org.finos.legend.engine.shared.core.operational.Assert;

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
        Assert.assertTrue(extraUserDataSourceProperties.getProperty(DeltaLakeDataSourceSpecification.DELTALAKE_HTTP_PATH) != null, () -> DeltaLakeDataSourceSpecification.DELTALAKE_HTTP_PATH + " is not set");
        Assert.assertTrue(extraUserDataSourceProperties.getProperty(DeltaLakeDataSourceSpecification.DELTALAKE_SHARD) != null, () -> DeltaLakeDataSourceSpecification.DELTALAKE_SHARD + " is not set");
        String httpPath = extraUserDataSourceProperties.getProperty(DeltaLakeDataSourceSpecification.DELTALAKE_HTTP_PATH);
        String shard = extraUserDataSourceProperties.getProperty(DeltaLakeDataSourceSpecification.DELTALAKE_SHARD);
        return String.format("jdbc:spark://%s;transportMode=http;ssl=1;httpPath=%s;AuthMech=3;",
                shard,
                httpPath
        );
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