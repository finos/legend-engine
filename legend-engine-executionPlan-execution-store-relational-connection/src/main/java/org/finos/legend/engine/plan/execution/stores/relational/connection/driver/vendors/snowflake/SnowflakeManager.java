package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.SnowflakeDataSourceSpecification;
import org.finos.legend.engine.shared.core.operational.Assert;

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
        Assert.assertTrue(extraUserDataSourceProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_NAME) != null, () -> SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_NAME + " is not set");
        Assert.assertTrue(extraUserDataSourceProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_REGION) != null, () -> SnowflakeDataSourceSpecification.SNOWFLAKE_REGION + " is not set");
        Assert.assertTrue(extraUserDataSourceProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_WAREHOUSE_NAME) != null, () -> SnowflakeDataSourceSpecification.SNOWFLAKE_WAREHOUSE_NAME + " is not set");
        String accountName = extraUserDataSourceProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_NAME);
        String region = extraUserDataSourceProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_REGION);
        String warehouse = extraUserDataSourceProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_WAREHOUSE_NAME);
        String cloudType = extraUserDataSourceProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_CLOUD_TYPE);
        return "jdbc:snowflake://" + accountName + "." + region + "." + cloudType + ".snowflakecomputing.com/?warehouse=" + warehouse;
    }

    @Override
    public Properties getExtraDataSourceProperties(AuthenticationStrategy authenticationStrategy)
    {
        return new Properties();
    }

    @Override
    public String getDriver()
    {
        return "org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeDriver";
    }

    @Override
    public RelationalDatabaseCommands relationalDatabaseSupport()
    {
        return new SnowflakeCommands();
    }
}
