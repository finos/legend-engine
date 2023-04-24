// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.memsql.test;

import org.testcontainers.utility.DockerImageName;
import org.testcontainers.containers.JdbcDatabaseContainer;

public class MemSQLContainer extends JdbcDatabaseContainer<MemSQLContainer>
{
    public static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("memsql/cluster-in-a-box");
    public static final String VERSION_TAG = "alma-8.0.17-0553658f69-4.0.11-1.15.3";
    public static final Integer DEFAULT_PORT = 3306;

    private String licenseKey;

    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "admin";
    private static final int DEFAULT_STARTUP_TIMEOUT_SECONDS = 240;
    private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 240;

    public static MemSQLContainer newMemSQLContainer(String licenseKeyEnvVariable)
    {
        if (!System.getenv().containsKey(licenseKeyEnvVariable))
        {
            throw new RuntimeException("System env does not contain a value for variable " + licenseKeyEnvVariable);
        }
        return new MemSQLContainer(DEFAULT_IMAGE_NAME.withTag(VERSION_TAG), System.getenv(licenseKeyEnvVariable));
    }

    private MemSQLContainer(DockerImageName dockerImageName, String licenseKey)
    {
        super(dockerImageName);
        this.licenseKey = licenseKey;
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        this.withStartupTimeoutSeconds(DEFAULT_STARTUP_TIMEOUT_SECONDS);
        this.withConnectTimeoutSeconds(DEFAULT_CONNECT_TIMEOUT_SECONDS);
        this.withUrlParam("permitMysqlScheme", null);
        this.addExposedPort(DEFAULT_PORT);
    }

    protected void configure()
    {
        this.addEnv("START_AFTER_INIT", "Y");
        this.addEnv("ROOT_PASSWORD", DEFAULT_PASSWORD);
        this.addEnv("LICENSE_KEY", this.licenseKey);
    }

    public String getDriverClassName()
    {
        return "org.mariadb.jdbc.Driver";
    }

    public String getJdbcUrl()
    {
        String additionalUrlParams = this.constructUrlParameters("?", "&");
        return "jdbc:mysql://" + this.getHost() + ":" + this.getMappedPort() + additionalUrlParams;
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
