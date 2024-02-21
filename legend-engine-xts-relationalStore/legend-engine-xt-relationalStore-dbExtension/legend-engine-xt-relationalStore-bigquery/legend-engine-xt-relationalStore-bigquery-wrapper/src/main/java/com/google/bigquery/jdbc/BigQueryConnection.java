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

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.logging.Logger;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;

public class BigQueryConnection implements Connection 
{

    private final BigQuery bigQuery;
    Logger logger = Logger.getLogger(BigQueryConnection.class.getName());
    
    public BigQueryConnection(String projectId, String datasetId) 
    {
        GoogleCredentials credentials = null;
        try 
        {
            credentials = GoogleCredentials.getApplicationDefault();
        } 
        catch (Exception e) 
        {
            throw new RuntimeException(e);
        }
        bigQuery = BigQueryOptions.newBuilder().setProjectId(projectId).setCredentials(credentials).build().getService();
        
        
        Dataset dataset = bigQuery.getDataset(datasetId);
        if (dataset == null) 
        {
            throw new RuntimeException("Dataset Not Found");
        } 
        else 
        {
            logger.info("Connected to Dataset: " + datasetId);
        }
    }
    
    public <T> T unwrap(Class<T> iface) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Statement createStatement() throws SQLException 
    {
        return new BigQueryStatement(bigQuery);
    }

    public PreparedStatement prepareStatement(String sql) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public CallableStatement prepareCall(String sql) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public String nativeSQL(String sql) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void setAutoCommit(boolean autoCommit) throws SQLException 
    {
        // TODO Auto-generated method stub

    }

    public boolean getAutoCommit() throws SQLException 
    {
        return false;
    }

    public void commit() throws SQLException 
    {
        // TODO Auto-generated method stub

    }

    public void rollback() throws SQLException 
    {
        // TODO Auto-generated method stub

    }

    public void close() throws SQLException 
    {
        // TODO Auto-generated method stub

    }

    public boolean isClosed() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public DatabaseMetaData getMetaData() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void setReadOnly(boolean readOnly) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public boolean isReadOnly() throws SQLException 
    {
        return false;
    }

    public void setCatalog(String catalog) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public String getCatalog() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void setTransactionIsolation(int level) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public int getTransactionIsolation() throws SQLException 
    {
        return 0;
    }

    public SQLWarning getWarnings() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void clearWarnings() throws SQLException 
    {
        // TODO Auto-generated method stub

    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Map<String, Class<?>> getTypeMap() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void setTypeMap(Map<String, Class<?>> map) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void setHoldability(int holdability) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public int getHoldability() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Savepoint setSavepoint() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Savepoint setSavepoint(String name) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void rollback(Savepoint savepoint) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void releaseSavepoint(Savepoint savepoint) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
            int resultSetHoldability) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Clob createClob() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Blob createBlob() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public NClob createNClob() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public SQLXML createSQLXML() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public boolean isValid(int timeout) throws SQLException 
    {
        return false;
    }

    public void setClientInfo(String name, String value) throws SQLClientInfoException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void setClientInfo(Properties properties) throws SQLClientInfoException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public String getClientInfo(String name) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Properties getClientInfo() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Array createArrayOf(String typeName, Object[] elements) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Struct createStruct(String typeName, Object[] attributes) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void setSchema(String schema) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public String getSchema() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void abort(Executor executor) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");

    }

    public int getNetworkTimeout() throws SQLException 
    {
        return 0;
    }

}
