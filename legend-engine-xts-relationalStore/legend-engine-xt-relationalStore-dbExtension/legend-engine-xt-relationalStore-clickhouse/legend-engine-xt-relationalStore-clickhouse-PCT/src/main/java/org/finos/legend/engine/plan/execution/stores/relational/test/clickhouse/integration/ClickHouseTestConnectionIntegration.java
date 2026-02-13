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

package org.finos.legend.engine.plan.execution.stores.relational.test.clickhouse.integration;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.test.shared.framework.TestServerResource;

public class ClickHouseTestConnectionIntegration implements TestConnectionIntegration, TestServerResource
{
    public CustomClickHouseContainer clickHouseContainer = new CustomClickHouseContainer("clickhouse/clickhouse-server:25.1.1-alpine");

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "ClickHouse");
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.ClickHouse;
    }

    @Override
    public void setup()
    {
        this.clickHouseContainer.start();
    }

    @Override
    public RelationalDatabaseConnection getConnection()
    {
        if (!clickHouseContainer.isRunning())
        {
            this.setup();
        }
        org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification datasourceSpec = new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification();
        datasourceSpec.host = clickHouseContainer.getHost();
        datasourceSpec.port = clickHouseContainer.getFirstMappedPort();
        datasourceSpec.databaseName = "default";

        org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy authSpec = new org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy();
        authSpec.userNameVaultReference = clickHouseContainer.getUsername();
        authSpec.passwordVaultReference = clickHouseContainer.getPassword();

        RelationalDatabaseConnection conn = new RelationalDatabaseConnection(datasourceSpec, authSpec, DatabaseType.ClickHouse);
        conn.type = DatabaseType.ClickHouse;
        conn.element = null;
        return conn;
    }

    @Override
    public void cleanup()
    {
        if (this.clickHouseContainer != null && this.clickHouseContainer.isRunning())
        {
            this.clickHouseContainer.stop();
        }
    }

    @Override
    public void start() throws Exception
    {
        this.setup();
    }

    @Override
    public void shutDown()
    {
        this.cleanup();
    }
}
