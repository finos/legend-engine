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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import static org.junit.jupiter.api.Assertions.*;

public class BigQueryConnectionTest
{
    private BigQueryConnection connection;

    @BeforeEach
    public void setup() throws SQLException
    {
        // Read project ID and dataset ID from environment variables
        String projectId = System.getenv("BIGQUERY_PROJECT_ID");
        String datasetId = System.getenv("BIGQUERY_DATASET_ID");

        // Check if environment variables are set
        if (projectId == null || datasetId == null)
        {
            throw new IllegalStateException("Environment variables BIGQUERY_PROJECT_ID and BIGQUERY_DATASET_ID must be set.");
        }
        connection = new BigQueryConnection(projectId, datasetId);
    }

    @AfterEach
    public void teardown() throws SQLException
    {
        // Close the connection after each test
        if (connection != null)
        {
            connection.close();
        }
    }

    @Test
    public void testCreateStatement() throws SQLException
    {
        try (Statement statement = connection.createStatement())
        {
            assertNotNull(statement);
            assertTrue(statement instanceof BigQueryStatement);
        }
    }

    @Test
    public void testExecuteQuery() throws SQLException
    {
        try (Statement statement = connection.createStatement())
        {
            assertNotNull(statement, "Statement should not be null");
            String sql = "SELECT 1";
            try (ResultSet resultSet = statement.executeQuery(sql))
            {
                assertNotNull(resultSet);
            }
        }
    }
}