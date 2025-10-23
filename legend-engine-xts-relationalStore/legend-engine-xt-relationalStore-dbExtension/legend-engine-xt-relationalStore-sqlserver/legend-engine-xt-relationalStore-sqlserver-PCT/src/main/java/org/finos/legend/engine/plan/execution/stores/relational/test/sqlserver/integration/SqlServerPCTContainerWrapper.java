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

import java.sql.Connection;
import java.sql.DriverManager;

public class SqlServerPCTContainerWrapper
{
    private final SqlServerPCTContainer sqlServerContainer;

    public static SqlServerPCTContainerWrapper build()
    {
        return new SqlServerPCTContainerWrapper();
    }

    private SqlServerPCTContainerWrapper()
    {
        this.sqlServerContainer = SqlServerPCTContainer.newSqlServerContainer();
    }

    public void start()
    {
        this.sqlServerContainer.start();
    }

    public void stop()
    {
        this.sqlServerContainer.stop();
    }

    public Connection getConnection() throws Exception
    {
        Class.forName(this.sqlServerContainer.getDriverClassName());

        return DriverManager.getConnection(
                this.sqlServerContainer.getJdbcUrl(),
                this.sqlServerContainer.getUsername(),
                this.sqlServerContainer.getPassword());
    }

    public String getHost()
    {
        return this.sqlServerContainer.getHost();
    }

    public int getPort()
    {
        return this.sqlServerContainer.getMappedPort();
    }

    public String getUser()
    {
        return this.sqlServerContainer.getUsername();
    }

    public String getPassword()
    {
        return this.sqlServerContainer.getPassword();
    }

    public boolean isRunning()
    {
        return this.sqlServerContainer.isRunning();
    }
}
