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

package org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.dynamicTestConnections;

import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.DynamicTestConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.DefaultH2AuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;

public class H2AlloyServer implements DynamicTestConnection
{
    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.H2;
    }

    private org.h2.tools.Server h2Server;

    @Override
    public void setup()
    {
        long start = System.currentTimeMillis();
        System.out.println("Starting setup of dynamic connection for database: H2 ");

        int relationalDBPort = DynamicPortGenerator.generatePort();
        try
        {
            h2Server = AlloyH2Server.startServer(relationalDBPort);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        long end = System.currentTimeMillis();

        System.out.println("Completed setup of dynamic connection for database: H2 on port:" + relationalDBPort + " , time taken(ms):" + (end - start));
    }

    @Override
    public RelationalDatabaseConnection getConnection()
    {
        RelationalDatabaseConnection h2Connection = new RelationalDatabaseConnection();
        StaticDatasourceSpecification ds = new StaticDatasourceSpecification();
        ds.databaseName = "temp";
        ds.host = "127.0.0.1";
        ds.port = h2Server.getPort();
        h2Connection.datasourceSpecification = ds;
        h2Connection.databaseType = DatabaseType.H2;
        h2Connection.authenticationStrategy = new DefaultH2AuthenticationStrategy();
        h2Connection.type = h2Connection.databaseType;        // for compatibility with legacy DatabaseConnection
        h2Connection.element = "";                            // placeholder , will be set by pure tests
        return h2Connection;
    }

    @Override
    public void cleanup()
    {
        h2Server.shutdown();
    }

}
