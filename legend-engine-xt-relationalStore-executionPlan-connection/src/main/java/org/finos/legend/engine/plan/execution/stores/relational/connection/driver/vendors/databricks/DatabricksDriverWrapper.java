// Copyright 2021 Databricks
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.databricks;

import org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.ApiTokenAuthenticationStrategy;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * SparkJDBC42Driver does not extend [java.sql.Driver], resulting in class cast exception in the legend DriverWrapper.
 * We created that wrapper class to connects to DeltaLake through [java.sql.DriverManager] instead of Driver.connect
 */
public class DatabricksDriverWrapper implements Driver
{

    public static String DRIVER_CLASSNAME = "com.simba.spark.spark.jdbc.SparkJDBC42Driver";

    public DatabricksDriverWrapper() throws SQLException
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
        return DriverManager.getConnection(url, "token", info.getProperty(ApiTokenAuthenticationStrategy.API_TOKEN));
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