package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.deltalake;

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.DeltaLakeAuthenticationStrategy;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * For some reason, SparkJDBC42Driver does not extend [java.sql.Driver], resulting in class cast exception in the legend
 * DriverWrapper. We created that wrapper class the connects to DeltaLake through DriverManager instead of Driver.connect
 */
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