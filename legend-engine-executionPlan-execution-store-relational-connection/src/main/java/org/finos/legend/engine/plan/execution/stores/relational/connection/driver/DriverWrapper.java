// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.driver;

import org.eclipse.collections.api.tuple.Pair;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public abstract class DriverWrapper implements Driver
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DriverWrapper.class);

    private final Driver driver;
    private final ConnectionStateManager connectionStateManager;

    public DriverWrapper()
    {
        this.connectionStateManager = ConnectionStateManager.getInstance();
        try
        {
            this.driver = (Driver) DriverWrapper.class.getClassLoader().loadClass(getClassName()).newInstance();
        }
        catch (Exception e)
        {
            LOGGER.error("Error loading driver {}", e);
            throw new RuntimeException(e);
        }
    }

    protected abstract String getClassName();

    protected Driver getDriver() {
        return this.driver;
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException
    {
        DataSourceWithStatistics ds = null;
        try
        {
            String poolName = (String)info.get(ConnectionStateManager.POOL_NAME_KEY);
            if (poolName == null)
            {
                throw new IllegalStateException("Connection properties dont have " + ConnectionStateManager.POOL_NAME_KEY);
            }
            ds = this.connectionStateManager.getDataSourceByPoolName(poolName);
            if (ds == null)
            {
                throw new IllegalStateException("Cannot find state for pool " + poolName);
            }
            Pair<String, Properties> res = ds.getAuthenticationStrategy().handleConnection(url, info, ds.getDatabaseManager());
            LOGGER.info("Handled connection by [{}] Authentication strategy for [{}]", ds.getAuthenticationStrategy().getKey().shortId(), poolName);
            Connection dbConnection = driver.connect(res.getOne(), handlePropertiesPriorToJDBCDriverConnection(res.getTwo()));
            LOGGER.info("[{}] Driver connected ", driver.getClass().getCanonicalName());
            int builtConnections = ds.buildConnection();
            LOGGER.info("Total [{}] connections built for data source [{}]", builtConnections, poolName);
            return dbConnection;
        }
        catch (Exception e)
        {
            ds.logConnectionError();
            LOGGER.error("Error connecting to db [{}], pool stats [{}]", url,connectionStateManager.getPoolStatisticsAsJSON(ds),e);
            if (e instanceof SQLException)
            {
                StringBuffer buffer = new StringBuffer();
                for (SQLException ex = (SQLException)e; ex != null; ex = ex.getNextException())
                {
                    buffer.append("\n------------------------------\n");
                    buffer.append("   State      :" + ex.getSQLState() + "\n");
                    buffer.append("   Code       :" + ex.getErrorCode() + "\n");
                    buffer.append("   exception  :" + ex.toString() + "\n");
                    buffer.append("------------------------------");
                }
                LOGGER.error(buffer.toString());
            }

            throw new RuntimeException(e);
        }
    }

    protected Properties handlePropertiesPriorToJDBCDriverConnection(Properties properties)
    {
        //some drivers have strict checks for expected  properties
        //override this method to remove any internal engine properties
        return properties;
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException
    {
        return driver.acceptsURL(url);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException
    {
        return driver.getPropertyInfo(url, info);
    }

    @Override
    public int getMajorVersion()
    {
        return driver.getMajorVersion();
    }

    @Override
    public int getMinorVersion()
    {
        return driver.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant()
    {
        return driver.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException
    {
        return driver.getParentLogger();
    }
}
