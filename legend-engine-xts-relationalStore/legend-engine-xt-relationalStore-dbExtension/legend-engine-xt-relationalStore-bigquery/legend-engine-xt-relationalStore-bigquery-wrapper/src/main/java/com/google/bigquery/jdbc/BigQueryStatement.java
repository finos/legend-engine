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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.UUID;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Job;
import com.google.cloud.bigquery.JobId;
import com.google.cloud.bigquery.JobInfo;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.TableResult;

public class BigQueryStatement implements Statement 
{

    private final BigQuery bigQuery;
    
    public BigQueryStatement(BigQuery bigQuery) 
    {
        this.bigQuery = bigQuery;
    }

    public <T> T unwrap(Class<T> iface) throws SQLException 
    {
        return null;
    }

    public boolean isWrapperFor(Class<?> iface) throws SQLException 
    {
        return false;
    }

    public ResultSet executeQuery(String sql) throws SQLException 
    {
        QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(sql).setUseLegacySql(false).build();
        JobId jobId = JobId.of(UUID.randomUUID().toString());
        Job queryJob = bigQuery.create(JobInfo.newBuilder(queryConfig).setJobId(jobId).build());
        try 
        {
            queryJob = queryJob.waitFor();
        } 
        catch (InterruptedException e) 
        {
            throw new SQLException("Query job was interrupted, JobId: " + jobId.toString(), e);
        }
        if (queryJob == null) 
        {
            throw new SQLException("Job no longer exists, JobId: " + jobId.toString());
        } 
        else if (queryJob.getStatus().getError() != null) 
        {
            throw new SQLException("BigQuery was unable to execute the query: " + queryJob.getStatus().getError() + " JobId: " + jobId.toString());
        }
        TableResult result = null;
        try 
        {
            result = queryJob.getQueryResults();
        } 
        catch (Exception e) 
        {
            throw new SQLException(e);
        }
        return new BigQueryResultSet(result);
    }

    public int executeUpdate(String sql) throws SQLException 
    {
        return 0;
    }

    public void close() throws SQLException 
    {
        // TODO Auto-generated method stub
    }

    public int getMaxFieldSize() throws SQLException 
    {
        return 0;
    }

    public void setMaxFieldSize(int max) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public int getMaxRows() throws SQLException 
    {
        return 0;
    }

    public void setMaxRows(int max) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void setEscapeProcessing(boolean enable) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public int getQueryTimeout() throws SQLException 
    {
        return 0;
    }

    public void setQueryTimeout(int seconds) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void cancel() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public SQLWarning getWarnings() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void clearWarnings() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void setCursorName(String name) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public boolean execute(String sql) throws SQLException 
    {
        return false;
    }

    public ResultSet getResultSet() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public int getUpdateCount() throws SQLException 
    {
        return 0;
    }

    public boolean getMoreResults() throws SQLException 
    {
        return false;
    }

    public void setFetchDirection(int direction) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public int getFetchDirection() throws SQLException 
    {
        return 0;
    }

    public void setFetchSize(int rows) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public int getFetchSize() throws SQLException 
    {
        return 0;
    }

    public int getResultSetConcurrency() throws SQLException 
    {
        return 0;
    }

    public int getResultSetType() throws SQLException 
    {
        return 0;
    }

    public void addBatch(String sql) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public void clearBatch() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public int[] executeBatch() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public Connection getConnection() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public boolean getMoreResults(int current) throws SQLException 
    {
        return false;
    }

    public ResultSet getGeneratedKeys() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException 
    {
        return 0;
    }

    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException 
    {
        return 0;
    }

    public int executeUpdate(String sql, String[] columnNames) throws SQLException 
    {
        return 0;
    }

    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException 
    {
        return false;
    }

    public boolean execute(String sql, int[] columnIndexes) throws SQLException 
    {
        return false;
    }

    public boolean execute(String sql, String[] columnNames) throws SQLException 
    {
        return false;
    }

    public int getResultSetHoldability() throws SQLException 
    {
        return 0;
    }

    public boolean isClosed() throws SQLException 
    {
        return false;
    }

    public void setPoolable(boolean poolable) throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public boolean isPoolable() throws SQLException 
    {
        return false;
    }

    public void closeOnCompletion() throws SQLException 
    {
        throw new UnsupportedOperationException("Not Implemented");
    }

    public boolean isCloseOnCompletion() throws SQLException 
    {
        return false;
    }

}
