package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DriverWrapper;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;

import java.util.Properties;

public class SnowflakeManager extends DatabaseManager
{
    @Override
    public MutableList<String> getIds()
    {
        return Lists.mutable.with("Snowflake");
    }

    @Override
    public String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties, AuthenticationStrategy authenticationStrategy)
    {
        throw new UnsupportedOperationException("Not used");
    }

    @Override
    public Properties getExtraDataSourceProperties(AuthenticationStrategy authenticationStrategy)
    {
        throw new UnsupportedOperationException("Not used");
    }

    @Override
    public String getDriver()
    {
        return SnowflakeDriver.class.getCanonicalName();
    }

    @Override
    public DriverWrapper getDriverWrapper()
    {
        return new SnowflakeDriver();
    }

    @Override
    public RelationalDatabaseCommands relationalDatabaseSupport()
    {
        return new SnowflakeCommands();
    }

}
