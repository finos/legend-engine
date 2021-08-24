package org.finos.legend.engine.plan.execution.stores.relational.connection.manager.strategic;

import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DatabaseManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;

public interface RelationalConnectionPlugin
{
    /*
        Declares the database type supported by this plugin
     */
    DatabaseType getDatabaseType();

    DatabaseManager getDatabaseManager();

    ConnectionKey buildConnectionKey(RelationalDatabaseConnection relationalDatabaseConnectionProtocol);

    DataSourceSpecification buildDatasourceSpecification(RelationalDatabaseConnection relationalDatabaseConnectionProtocol, RelationalExecutorInfo relationalExecutorInfo);
}
