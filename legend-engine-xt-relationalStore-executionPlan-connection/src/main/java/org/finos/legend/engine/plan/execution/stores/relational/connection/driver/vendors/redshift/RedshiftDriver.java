package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.redshift;

import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DriverWrapper;

public class RedshiftDriver extends DriverWrapper
{
    @Override
    protected String getClassName()
    {
        return "com.amazon.redshift.jdbc.Driver";
    }
}
