package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.snowflake;

import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DriverWrapper;

public class SnowflakeDriver extends DriverWrapper
{
    @Override
    protected String getClassName()
    {
        return "net.snowflake.client.jdbc.SnowflakeDriver";
    }
}
