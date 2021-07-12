package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.deltalake;

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.DeltaLakeAuthenticationStrategy;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * SparkJDBC42Driver does not extend [java.sql.Driver], resulting in class cast exception in the legend DriverWrapper.
 * We created that wrapper class to connects to DeltaLake through [java.sql.DriverManager] instead of Driver.connect
 */
public class SparkDriverWrapper implements Driver
{

    public static String DRIVER_CLASSNAME = "com.simba.spark.spark.jdbc.SparkJDBC42Driver";

    public SparkDriverWrapper() throws SQLException
    {
        // Loading driver, making sure we have spark driver on classpath
       try
       {
           Class.forName(DRIVER_CLASSNAME).getDeclaredConstructor().newInstance();
       } catch (Exception e)
       {
           throw new SQLException("Could not find driver [" + DRIVER_CLASSNAME + "] on classpath");
       }
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException
    {
        return DriverManager.getConnection(url, "token", info.getProperty(DeltaLakeAuthenticationStrategy.DELTALAKE_TOKEN));
    }

    @Override
    public boolean acceptsURL(String url)
    {
        return url.startsWith("jdbc:spark://");
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
    {
        DriverPropertyInfo[] retVal;
        if (url == null || url.isEmpty())
        {
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
        return true;
    }

    @Override
    public Logger getParentLogger()
    {
        return null;
    }
}