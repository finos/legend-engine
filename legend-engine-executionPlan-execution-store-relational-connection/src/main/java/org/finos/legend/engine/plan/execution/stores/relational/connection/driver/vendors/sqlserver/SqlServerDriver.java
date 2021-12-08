package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.sqlserver;

import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DriverWrapper;

public class SqlServerDriver extends DriverWrapper
{
    @Override
    protected String getClassName()
    {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }
}
