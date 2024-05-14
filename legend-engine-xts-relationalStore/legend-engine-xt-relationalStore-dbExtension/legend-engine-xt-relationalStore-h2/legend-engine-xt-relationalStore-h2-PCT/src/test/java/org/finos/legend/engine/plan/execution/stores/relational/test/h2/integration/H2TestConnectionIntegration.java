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

package org.finos.legend.engine.plan.execution.stores.relational.test.h2.integration;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.finos.legend.engine.test.shared.framework.TestServerResource;

public class H2TestConnectionIntegration implements TestConnectionIntegration, TestServerResource
{
    private org.h2.tools.Server h2Server = null;
    private int port;

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "H2");
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.H2;
    }

    @Override
    public void setup() throws Exception
    {
        this.port = DynamicPortGenerator.generatePort();
        this.h2Server = AlloyH2Server.startServer(this.port);
    }

    @Override
    public RelationalDatabaseConnection getConnection()
    {
        StaticDatasourceSpecification H2DataSourceSpecification = new StaticDatasourceSpecification();
        H2DataSourceSpecification.host = "localhost";
        H2DataSourceSpecification.port = this.port;
        H2DataSourceSpecification.databaseName = "default";

        TestDatabaseAuthenticationStrategy authSpec = new TestDatabaseAuthenticationStrategy();

        RelationalDatabaseConnection conn = new RelationalDatabaseConnection(H2DataSourceSpecification, authSpec, DatabaseType.H2);

        conn.type = DatabaseType.H2;           // for compatibility with legacy DatabaseConnection
        conn.element = null;

        return conn;
    }

    @Override
    public void cleanup() throws Exception
    {
        this.h2Server.shutdown();
        this.h2Server.stop();

    }


    @Override
    public void start() throws Exception
    {
        this.setup();
    }

    @Override
    public void shutDown() throws Exception
    {
        this.cleanup();
    }
}
