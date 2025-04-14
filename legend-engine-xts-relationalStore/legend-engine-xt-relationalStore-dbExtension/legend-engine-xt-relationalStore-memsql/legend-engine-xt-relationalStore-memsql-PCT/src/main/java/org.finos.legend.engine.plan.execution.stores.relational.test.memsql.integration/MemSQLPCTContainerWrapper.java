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

package org.finos.legend.engine.plan.execution.stores.relational.test.memsql.integration;

import java.sql.Connection;
import java.sql.DriverManager;

public class MemSQLPCTContainerWrapper
{
    private final MemSQLPCTContainer memSQLContainer;

    public static MemSQLPCTContainerWrapper build()
    {
        return new MemSQLPCTContainerWrapper();
    }

    private MemSQLPCTContainerWrapper()
    {
        this.memSQLContainer = MemSQLPCTContainer.newMemSQLContainer();
    }

    public void start()
    {
        System.out.println("This is containerID: " + this.memSQLContainer.getContainerId());
        this.memSQLContainer.start();
    }

    public void stop()
    {
        this.memSQLContainer.stop();
    }

    public Connection getConnection() throws Exception
    {
        Class.forName(this.memSQLContainer.getDriverClassName());

        return DriverManager.getConnection(
                this.memSQLContainer.getJdbcUrl(),
                this.memSQLContainer.getUsername(),
                this.memSQLContainer.getPassword());
    }

    public String getHost()
    {
        return this.memSQLContainer.getHost();
    }

    public int getPort()
    {
        return this.memSQLContainer.getMappedPort();
    }

    public String getUser()
    {
        return this.memSQLContainer.getUsername();
    }

    public String getPassword()
    {
        return this.memSQLContainer.getPassword();
    }

    public boolean isRunning()
    {
        return this.memSQLContainer.isRunning();
    }
}
