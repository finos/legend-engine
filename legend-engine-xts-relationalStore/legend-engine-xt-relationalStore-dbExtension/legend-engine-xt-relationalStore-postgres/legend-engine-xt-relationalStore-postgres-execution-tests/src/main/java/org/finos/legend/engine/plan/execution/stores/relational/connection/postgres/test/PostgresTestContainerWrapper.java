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

package org.finos.legend.engine.plan.execution.stores.relational.connection.postgres.test;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class PostgresTestContainerWrapper
{
    public PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer(DockerImageName.parse("postgres").withTag("9.6.12"));

    public static PostgresTestContainerWrapper build()
    {
        PostgresTestContainerWrapper postgresTestContainerWrapper = new PostgresTestContainerWrapper();
        return postgresTestContainerWrapper;
    }

    public void start()
    {
        this.postgreSQLContainer.start();
    }

    public void stop()
    {
        this.postgreSQLContainer.stop();
    }

    public int getPort()
    {
        return this.postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT);
    }

    public String getUser()
    {
        return "test";
    }

    public String getPassword()
    {
        return "test";
    }

    public String getJdbcUrl()
    {
        return this.postgreSQLContainer.getJdbcUrl();
    }

}
