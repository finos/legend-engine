package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.redshift;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.RedshiftDataSourceSpecification;
import org.finos.legend.engine.shared.core.operational.Assert;

import java.util.Properties;

public class RedshiftManager extends DatabaseManager
{
    @Override
    public MutableList<String> getIds()
    {
        return Lists.mutable.with("Redshift");
    }

    @Override
    public String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties, AuthenticationStrategy authenticationStrategy)
    {
        String clusterName = extraUserDataSourceProperties.getProperty(RedshiftDataSourceSpecification.REDSHIFT_CLUSTER_NAME);
        String clusterID = extraUserDataSourceProperties.getProperty(RedshiftDataSourceSpecification.REDSHIFT_CLUSTER_ID);
        String region = extraUserDataSourceProperties.get(RedshiftDataSourceSpecification.REDSHIFT_REGION).toString();
        return "jdbc:redshift://" + clusterName + "." + clusterID + "." + region + ".redshift.amazonaws.com:" + port + "/" + databaseName;
    }

    @Override
    public Properties getExtraDataSourceProperties(AuthenticationStrategy authenticationStrategy)
    {
        return new Properties();
    }

    @Override
    public String getDriver()
    {
        return "org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.redshift.RedshiftDriver";
    }

    @Override
    public RelationalDatabaseCommands relationalDatabaseSupport()
    {
        return new RedshiftCommands();
    }
}
