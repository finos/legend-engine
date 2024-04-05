// Copyright 2023 Google LLC
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

package com.google.bigquery.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class BigQueryDriver implements Driver 
{
    
    static 
    {
        try 
        {
            register();
        } 
        catch (SQLException e) 
        {
            throw new IllegalStateException("Registering driver failed: " + e.getMessage());
        }
    }

    private static BigQueryDriver registeredDriver;

    static void register() throws SQLException 
    {
        if (isRegistered()) 
        {
            throw new IllegalStateException("Driver is already registered. It can only be registered once.");
        }
        BigQueryDriver registeredDriver = new BigQueryDriver();
        DriverManager.registerDriver(registeredDriver);
        BigQueryDriver.registeredDriver = registeredDriver;
    }

    static boolean isRegistered() 
    {
        return registeredDriver != null;
    }

    static BigQueryDriver getRegisteredDriver() throws SQLException 
    {
        if (isRegistered()) 
        {
            return registeredDriver;
        }
        throw new SQLException("The driver has not been registered");
    }

    public BigQueryDriver() 
    {
    }

    public Connection connect(String url, Properties info) throws SQLException 
    {
        String projectId = info.getProperty("bigquery_projectId");
        String datasetId = info.getProperty("bigquery_defaultDataset");
        return new BigQueryConnection(projectId, datasetId);
    }

    public boolean acceptsURL(String url) throws SQLException 
    {
        return true;
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public int getMajorVersion() 
    {
        return 0;
    }

    public int getMinorVersion() 
    {
        return 0;
    }

    public boolean jdbcCompliant() 
    {
        return false;
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException 
    {
        throw new SQLFeatureNotSupportedException();
    }

}
