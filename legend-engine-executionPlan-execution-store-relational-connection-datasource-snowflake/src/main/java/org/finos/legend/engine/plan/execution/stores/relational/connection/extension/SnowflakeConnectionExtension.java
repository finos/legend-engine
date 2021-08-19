package org.finos.legend.engine.plan.execution.stores.relational.connection.extension;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionExtension;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake.SnowflakeManager;

public class SnowflakeConnectionExtension implements ConnectionExtension
{
    @Override
    public MutableList<DatabaseManager> getAdditionalDatabaseManager() {
        return Lists.mutable.with(new SnowflakeManager());
    }
}
