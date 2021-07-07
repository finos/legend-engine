package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.deltalake;

import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.DriverWrapper;

public class DeltaLakeDriver extends DriverWrapper
{
    public static String DRIVER_CLASSNAME = "com.simba.spark.spark.jdbc.SparkJDBC42Driver";

    @Override
    protected String getClassName()
    {
        return DRIVER_CLASSNAME;
    }
}