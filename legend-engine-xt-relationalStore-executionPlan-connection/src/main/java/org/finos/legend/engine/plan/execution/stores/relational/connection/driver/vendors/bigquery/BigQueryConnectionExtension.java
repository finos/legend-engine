package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.bigquery;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionExtension;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;

public class BigQueryConnectionExtension implements ConnectionExtension
{
    @Override
    public MutableList<DatabaseManager> getAdditionalDatabaseManager()
    {
        return Lists.mutable.of(new BigQueryManager());
    }
}
