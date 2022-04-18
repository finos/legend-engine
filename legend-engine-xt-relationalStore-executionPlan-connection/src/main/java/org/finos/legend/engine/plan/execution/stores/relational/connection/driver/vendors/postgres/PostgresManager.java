package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.postgres;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.AuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.DelegatedKerberosAuthenticationStrategy;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.commands.RelationalDatabaseCommands;
import org.finos.legend.engine.shared.core.kerberos.SubjectTools;

import java.util.Properties;

public class PostgresManager extends DatabaseManager
{
    @Override
    public MutableList<String> getIds()
    {
        return Lists.mutable.with("Postgres");
    }

    @Override
    public String buildURL(String host, int port, String databaseName, Properties extraUserDataSourceProperties, AuthenticationStrategy authenticationStrategy)
    {
        String additionalProperties = "";
        if (authenticationStrategy instanceof DelegatedKerberosAuthenticationStrategy)
        {
            additionalProperties = "?user=" + SubjectTools.getCurrentPrincipal().getName();
        }

        return "jdbc:postgresql://" + host + ":" + port + "/" + databaseName + additionalProperties;
    }

    @Override
    public String getDriver()
    {
        return "org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.postgres.PostgresDriver";
    }

    @Override
    public RelationalDatabaseCommands relationalDatabaseSupport()
    {
        return new PostgresCommands();
    }
}
