// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.persistence.components.e2e;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

public class PostgresTestContainer
{
    private PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer(DockerImageName.parse("postgres").withTag("16.2"));

    public static PostgresTestContainer build()
    {
        PostgresTestContainer postgresTestContainer = new PostgresTestContainer();
        return postgresTestContainer;
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
        return this.postgreSQLContainer.getUsername();
    }

    public String getPassword()
    {
        return this.postgreSQLContainer.getPassword();
    }

    public String getJdbcUrl()
    {
        return this.postgreSQLContainer.getJdbcUrl();
    }

    public void copyFileToContainer(String filePathInSystem, String filePathInContainer)
    {
        postgreSQLContainer.copyFileToContainer(MountableFile.forClasspathResource(filePathInSystem), filePathInContainer);
    }
}
