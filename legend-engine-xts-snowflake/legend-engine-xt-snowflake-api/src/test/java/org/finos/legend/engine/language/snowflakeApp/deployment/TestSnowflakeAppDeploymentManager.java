// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.language.snowflakeApp.deployment;

import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestSnowflakeAppDeploymentManager
{
    @Test
    public void resolveDeployedLocationLeavesAlreadyComputedLocationUnchanged() throws Exception
    {
        SnowflakeAppDeploymentManager manager = new SnowflakeAppDeploymentManager();
        Connection jdbcConnection = mock(Connection.class);

        String result = invokeResolveDeployedLocation(manager, jdbcConnection, "https://app.us-east-1.aws.privatelink.snowflakecomputing.com/us-east-1.aws/myaccount/data/databases/CLIMATE_DB");

        assertEquals("https://app.us-east-1.aws.privatelink.snowflakecomputing.com/us-east-1.aws/myaccount/data/databases/CLIMATE_DB", result);
        Mockito.verifyNoInteractions(jdbcConnection);
    }

    @Test
    public void resolveDeployedLocationQueriesLiveConnectionWhenGenerationLeftItEmpty() throws Exception
    {
        SnowflakeAppDeploymentManager manager = new SnowflakeAppDeploymentManager();
        Connection jdbcConnection = stubbedConnection("myaccount", "us-east-1", "SNOWFLAKE");

        String result = invokeResolveDeployedLocation(manager, jdbcConnection, "");

        assertEquals("https://app.us-east-1.privatelink.snowflakecomputing.com/us-east-1/myaccount/data/databases/SNOWFLAKE", result);
    }

    @Test
    public void resolveDeployedLocationFallsBackToEmptyOnQueryFailure() throws Exception
    {
        SnowflakeAppDeploymentManager manager = new SnowflakeAppDeploymentManager();
        Connection jdbcConnection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        when(jdbcConnection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new SQLException("no permission to query account info"));

        String result = invokeResolveDeployedLocation(manager, jdbcConnection, "");

        assertEquals("", result);
    }

    private static Connection stubbedConnection(String account, String region, String databaseName) throws SQLException
    {
        Connection jdbcConnection = mock(Connection.class);
        when(jdbcConnection.getCatalog()).thenReturn(databaseName);

        Statement accountStatement = mock(Statement.class);
        ResultSet accountResultSet = mock(ResultSet.class);
        when(accountResultSet.next()).thenReturn(true);
        when(accountResultSet.getString(1)).thenReturn(account);
        when(accountStatement.executeQuery("SELECT CURRENT_ACCOUNT()")).thenReturn(accountResultSet);

        Statement regionStatement = mock(Statement.class);
        ResultSet regionResultSet = mock(ResultSet.class);
        when(regionResultSet.next()).thenReturn(true);
        when(regionResultSet.getString(1)).thenReturn(region);
        when(regionStatement.executeQuery("SELECT CURRENT_REGION()")).thenReturn(regionResultSet);

        when(jdbcConnection.createStatement()).thenReturn(accountStatement, regionStatement);
        return jdbcConnection;
    }

    private static String invokeResolveDeployedLocation(SnowflakeAppDeploymentManager manager, Connection jdbcConnection, String generatedDeployedLocation) throws Exception
    {
        Method method = SnowflakeAppDeploymentManager.class.getDeclaredMethod("resolveDeployedLocation", Connection.class, String.class);
        method.setAccessible(true);
        return (String) method.invoke(manager, jdbcConnection, generatedDeployedLocation);
    }
}
