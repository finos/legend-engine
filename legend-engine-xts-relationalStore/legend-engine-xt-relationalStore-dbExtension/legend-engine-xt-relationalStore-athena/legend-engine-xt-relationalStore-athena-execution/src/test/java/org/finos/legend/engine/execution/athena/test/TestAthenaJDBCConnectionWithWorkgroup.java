//  Copyright 2025 Goldman Sachs
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

package org.finos.legend.engine.execution.athena.test;

import java.sql.*;
import java.util.Properties;

public class TestAthenaJDBCConnectionWithWorkgroup
{

    // --- Replace with your details ---
    private static final String AWS_REGION = "ap-south-1";
    private static final String DATABASE_NAME = "my_company_db";
    private static final String TABLE_NAME = "employees";
    private static final String S3_DATA_LOCATION = "s3://parth-aws-bucket-1/employee_data/";
    private static final String S3_OUTPUT_LOCATION = "s3://parth-aws-bucket-1/results";
    private static final String WORKGROUP = "parth-workgroup";

    // -----------------------------------

    public static void main(String[] args)
    {
        Connection conn = null;
        Statement stmt = null;

        try
        {
            Class.forName("com.simba.athena.jdbc.Driver"); //v2

            // 1. Establish the connection
            System.out.println("Connecting to Athena...");
            Properties info = new Properties();

            info.put("User", System.getProperty("PARTH_AWS_ACCESS_KEY_ID", System.getenv("PARTH_AWS_ACCESS_KEY_ID")));
            info.put("Password", System.getProperty("PARTH_AWS_SECRET_ACCESS_KEY", System.getenv("PARTH_AWS_SECRET_ACCESS_KEY")));

            String jdbcUrl = String.format("jdbc:awsathena://AwsRegion=%s;S3OutputLocation=%s;Workgroup=%s", AWS_REGION, S3_OUTPUT_LOCATION, WORKGROUP);
            conn = DriverManager.getConnection(jdbcUrl, info);
            System.out.println("Connection successful.");

            stmt = conn.createStatement();

            // 2. Load data from S3 by creating an external table
            // This statement tells Athena how to interpret the files in your S3 data location.
            // You only need to run this once for a given dataset.

            String createTableSql = String.format(
                    "CREATE EXTERNAL TABLE IF NOT EXISTS %s.%s (" +
                            "`employee_id` INT, `employee_name` STRING, `firm_id` INT, `job_title` STRING, `start_date` DATE" +
                            ") ROW FORMAT SERDE 'org.apache.hadoop.hive.serde2.OpenCSVSerde' " +
                            "WITH SERDEPROPERTIES ('skip.header.line.count'='1') " +
                            "STORED AS TEXTFILE LOCATION '%s';",
                    DATABASE_NAME, TABLE_NAME, S3_DATA_LOCATION
            );

            System.out.println("\nquery: " + createTableSql);

            System.out.println("\nExecuting CREATE TABLE statement...");
            stmt.execute(createTableSql);
            System.out.println("Table created or already exists.");

            // 3. Query the data and output results
            String selectSql = String.format("SELECT * FROM %s.%s LIMIT 10;", DATABASE_NAME, TABLE_NAME);

            System.out.println("\nExecuting SELECT query: " + selectSql);
            ResultSet rs = stmt.executeQuery(selectSql);

            // Get column information
            ResultSetMetaData rsmd = rs.getMetaData();
            int columnCount = rsmd.getColumnCount();
            for (int i = 1; i <= columnCount; i++)
            {
                System.out.printf("%-20s", rsmd.getColumnName(i));
            }
            System.out.println("\n------------------------------------------------------------");

            // Print query results
            while (rs.next())
            {
                for (int i = 1; i <= columnCount; i++)
                {
                    System.out.printf("%-20s", rs.getString(i));
                }
                System.out.println();
            }

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            // Clean up resources
            try
            {
                if (stmt != null)
                {
                    stmt.close();
                }
                if (conn != null)
                {
                    conn.close();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}