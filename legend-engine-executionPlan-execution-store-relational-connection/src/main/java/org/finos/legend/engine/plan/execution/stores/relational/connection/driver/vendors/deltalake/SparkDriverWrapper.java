package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.deltalake;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public class SparkDriverWrapper implements Driver
{

    public static String DRIVER_CLASSNAME = "com.simba.spark.spark.jdbc.SparkJDBC42Driver";

    public SparkDriverWrapper() throws ClassNotFoundException
    {
        // Loading driver, making sure we have it on classpath
       try {
           Class.forName(DRIVER_CLASSNAME).getDeclaredConstructor().newInstance();
       } catch (Exception e) {
           throw new ClassNotFoundException("Could not find driver [" + DRIVER_CLASSNAME + "] on classpath");
       }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException
    {
        return DriverManager.getConnection(url, "token", info.getProperty("apiToken"));
    }

    @Override
    public boolean acceptsURL(String url)
    {
        return false;
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
    {
        DriverPropertyInfo[] retVal;
        if (url == null || url.isEmpty()) {
            retVal = new DriverPropertyInfo[1];
            retVal[0] = new DriverPropertyInfo("apiToken", null);
            retVal[0].description = "API token";
            return retVal;
        }
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion()
    {
        return 0;
    }

    @Override
    public int getMinorVersion()
    {
        return 0;
    }

    @Override
    public boolean jdbcCompliant()
    {
        return false;
    }

    @Override
    public Logger getParentLogger()
    {
        return null;
    }
}