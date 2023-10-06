// Copyright 2023 Goldman Sachs
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

package demo;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariPoolMXBean;
import org.finos.legend.connection.PostgresTestContainerWrapper;

import java.sql.Connection;
import java.util.Properties;

public class HikariConnectionPoolingConceptDemo
{
    public static void main(String[] args) throws Exception
    {
        PostgresTestContainerWrapper postgresContainer = PostgresTestContainerWrapper.build();
        postgresContainer.start();

        Properties properties = new Properties();
        String poolName = "alice";
        HikariConfig jdbcConfig = new HikariConfig();
        jdbcConfig.setDriverClassName("org.postgresql.Driver");
        jdbcConfig.setPoolName(poolName);
        jdbcConfig.setMaximumPoolSize(2);
        jdbcConfig.setJdbcUrl(String.format("jdbc:postgresql://%s:%s/%s", postgresContainer.getHost(), postgresContainer.getPort(), postgresContainer.getDatabaseName()));
        properties.setProperty("user", postgresContainer.getUser());
        properties.setProperty("password", postgresContainer.getPassword());
        jdbcConfig.setDataSourceProperties(properties);

        HikariDataSource ds1 = new HikariDataSource(jdbcConfig);

        HikariPoolMXBean poolProxy = ds1.getHikariPoolMXBean();

        // 1 connection in pool
        reportPoolStats(poolProxy);
        Connection connection1 = ds1.getConnection();
        reportPoolStats(poolProxy);

        // 2 connections in pool (1st still active)
        Connection connection2 = ds1.getConnection();
        reportPoolStats(poolProxy);

        connection2.close(); // close this is the only way to make this pass
        // 3 connections in pool, error will be thrown if connection1/connection2 are both active due to pool limit=2
        System.out.println("trying to get a connection...");
        reportPoolStats(poolProxy);
        ds1.getConnection();
        reportPoolStats(poolProxy);
    }

    private static void reportPoolStats(HikariPoolMXBean poolProxy)
    {
        System.out.printf("Pool stats:\nactive: %s, idle: %s, total: %s%n", poolProxy.getActiveConnections(), poolProxy.getIdleConnections(), poolProxy.getTotalConnections());
    }
}
