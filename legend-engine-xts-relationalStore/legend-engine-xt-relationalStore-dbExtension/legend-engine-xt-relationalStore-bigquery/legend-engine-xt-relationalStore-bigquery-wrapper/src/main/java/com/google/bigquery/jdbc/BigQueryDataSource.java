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

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;
import javax.sql.DataSource;

public class BigQueryDataSource implements DataSource 
{

    static
    {
        try 
        {
            Class.forName("com.deloitte.legend.engine.bigquery.BigQueryDriver");
        } 
        catch (ClassNotFoundException e) 
        {
            throw new IllegalStateException(
                    "BigQueryDataSource failed to load org.finos.legend.engine.bigquery.BigQueryDriver", e);
        }
    }

    public Logger getParentLogger() throws SQLFeatureNotSupportedException 
    {
        throw new SQLFeatureNotSupportedException();
    }

    public <T> T unwrap(Class<T> iface) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Connection getConnection() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Connection getConnection(String username, String password) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public PrintWriter getLogWriter() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void setLogWriter(PrintWriter out) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void setLoginTimeout(int seconds) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public int getLoginTimeout() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

}
