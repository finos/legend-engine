package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.SnowflakeDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.SnowflakeAccountType;
import org.finos.legend.engine.shared.core.operational.Assert;

import java.util.Properties;

public class SnowflakeManager extends DatabaseManager
{

    public static final String PRIVATELINK_SNOWFLAKECOMPUTING_COM = ".privatelink.snowflakecomputing.com";

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
        String cloudType = extraUserDataSourceProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_CLOUD_TYPE);
        String organisation = extraUserDataSourceProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ORGANIZATION_NAME);

        String accountTypeName = extraUserDataSourceProperties.getProperty(SnowflakeDataSourceSpecification.SNOWFLAKE_ACCOUNT_TYPE_NAME);
        SnowflakeAccountType accountType = accountTypeName != null ? SnowflakeAccountType.valueOf(accountTypeName) : null;


        StringBuilder URL = new StringBuilder().append("jdbc:snowflake://");
        if (accountType != null)
        {
            if (SnowflakeAccountType.VPS.equals(accountType))
            {
                URL.append(accountName)
                        .append(".").append(organisation)
                        .append(".").append(region)
                        .append(".").append(cloudType);
            }
            else if (SnowflakeAccountType.MultiTenant.equals(accountType))
            {
                this.buildMultiTenantHostname(accountName, region, URL);
            }
        }
        else
        {
            buildMultiTenantHostname(accountName, region, URL);
        }

        URL.append(PRIVATELINK_SNOWFLAKECOMPUTING_COM);
        return URL.toString();
    }


    public void buildMultiTenantHostname(String accountName, String region, StringBuilder url)
    {
        url.append(accountName).append(".").append(region);
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
