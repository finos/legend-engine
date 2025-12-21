// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.sqlserver.integration;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

public class SqlServerPCTContainer extends JdbcDatabaseContainer<SqlServerPCTContainer>
{
    public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName
            .parse(System.getProperty("legend.engine.testcontainer.registry", "mcr.microsoft.com") + "/mssql/server:2019-latest")
            .asCompatibleSubstituteFor("mcr.microsoft.com/mssql/server:2019-latest");
    public static final Integer DEFAULT_PORT = 1433;

    private static final String DEFAULT_USER = "sa";
    private static final String DEFAULT_PASSWORD = "Strong_P@ssw0rd";
    private static final int DEFAULT_STARTUP_ATTEMPTS = 3;
    private static final int DEFAULT_STARTUP_TIMEOUT_SECONDS = 240;
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 240;

    public static SqlServerPCTContainer newSqlServerContainer()
    {
        return new SqlServerPCTContainer(DEFAULT_IMAGE_NAME);
    }

    private SqlServerPCTContainer(DockerImageName dockerImageName)
    {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        this.withStartupAttempts(DEFAULT_STARTUP_ATTEMPTS);
        this.withStartupTimeoutSeconds(DEFAULT_STARTUP_TIMEOUT_SECONDS);
        this.withConnectTimeoutSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS);
        this.addExposedPort(DEFAULT_PORT);
    }

    protected void configure()
    {
        this.addEnv("ACCEPT_EULA", "Y");
        this.addEnv("SA_PASSWORD", DEFAULT_PASSWORD);
    }

    public String getDriverClassName()
    {
        return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    }

    public String getJdbcUrl()
    {
        String host = this.getHost();
        Integer port = this.getMappedPort();
        return "jdbc:sqlserver://" + host + ":" + port + ";";
    }

    public Integer getMappedPort()
    {
        return this.getMappedPort(DEFAULT_PORT);
    }

    public String getUsername()
    {
        return DEFAULT_USER;
    }

    public String getPassword()
    {
        return DEFAULT_PASSWORD;
    }

    public String getTestQueryString()
    {
        return "SELECT 1";
    }
}
