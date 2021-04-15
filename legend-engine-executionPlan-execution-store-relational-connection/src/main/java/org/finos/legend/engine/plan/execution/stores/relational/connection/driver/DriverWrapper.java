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

import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.eclipse.collections.api.tuple.Pair;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

public abstract class DriverWrapper implements Driver
{
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(DriverWrapper.class);

    private Driver driver;

    public DriverWrapper()
    {
        try
        {
            this.driver = (Driver) DriverWrapper.class.getClassLoader().loadClass(getClassName()).newInstance();
        }
        catch (Exception e)
        {
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
        try
        {
            DataSourceSpecification ds = DataSourceSpecification.getInstance((String)info.get(DataSourceSpecification.DATASOURCE_SPEC_INSTANCE));
            Pair<String, Properties> res = ds.getAuthenticationStrategy().handleConnection(url, info, ds.getDatabaseManager());
            ds.getDataSourceSpecificationStatistics().builtConnections++;
            LOGGER.debug("connect {}", driver.getClass().getSimpleName());
            return driver.connect(res.getOne(), res.getTwo());
        }
        catch (Exception e)
        {
            if (e instanceof SQLException)
            {
                StringBuffer buffer = new StringBuffer();
                for (SQLException ex = (SQLException) e; ex != null; ex = ex.getNextException())
                {
                    buffer.append("\n------------------------------\n");
                    buffer.append("   State      :"+ex.getSQLState()+"\n");
                    buffer.append("   Code       :"+ex.getErrorCode()+"\n");
                    buffer.append("   exception  :"+ex.toString()+"\n");
                    buffer.append("------------------------------");
                }
                LOGGER.error(buffer.toString());
            }
            LOGGER.error("Error connecting to db", e);
            throw new RuntimeException(e);
        }
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
