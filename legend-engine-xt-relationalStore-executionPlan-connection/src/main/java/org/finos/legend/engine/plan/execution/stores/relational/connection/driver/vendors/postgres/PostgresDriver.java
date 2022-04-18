package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.postgres;

import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DriverWrapper;

public class PostgresDriver extends DriverWrapper
{
    @Override
    protected String getClassName()
    {
        return "org.postgresql.Driver";
    }
}