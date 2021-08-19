package org.finos.legend.engine.plan.execution.stores.relational.connection;

import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.eclipse.collections.api.list.MutableList;

public interface ConnectionExtension
{
    MutableList<DatabaseManager> getAdditionalDatabaseManager();
}
