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

package org.finos.legend.engine.server.test.shared;

import io.dropwizard.setup.Environment;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.ConnectionTestManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.DynamicTestConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.server.Server;
import org.finos.legend.engine.server.ServerConfiguration;
import org.finos.legend.engine.shared.core.url.EngineUrlStreamHandlerFactory;
import org.slf4j.Logger;
import java.util.*;

public class TestServer extends Server
{
    private final List<DynamicTestConnection> dynamicTestConnections = new ArrayList<>();

    public static void main(String[] args) throws Exception
    {
        EngineUrlStreamHandlerFactory.initialize();
        new TestServer().run(args);
    }

    @Override
    public void run(ServerConfiguration serverConfiguration, Environment environment)
    {
        super.run(serverConfiguration, environment);

        environment.jersey().register(new ExecutePlanInTestDatabase(planExecutor,
                relationalStoreExecutor.getStoreState().getRelationalExecutor().getConnectionManager(),
                getTestConnections(serverConfiguration)));
    }

    public Map<DatabaseType, RelationalDatabaseConnection> getTestConnections (ServerConfiguration serverConfiguration)
    {
        Map<DatabaseType, RelationalDatabaseConnection> testConnections = new HashMap<>();

        // register static connections
        for (DatabaseType dbType : serverConfiguration.staticTestConnections.keySet())
        {
            if (serverConfiguration.testConnectionsToEnable.contains(dbType))
            {
                testConnections.putIfAbsent(dbType, serverConfiguration.staticTestConnections.get(dbType));
            }
        }

        //Spin up dynamic database instances for testing
        for (ConnectionTestManager connectionTestManager : ServiceLoader.load(ConnectionTestManager.class))
        {
            //run only when in config
            if (serverConfiguration.testConnectionsToEnable.contains(connectionTestManager.getDatabaseType()))
            {
                DynamicTestConnection dynamicTestConnection = connectionTestManager.getDynamicTestConnection();
                if (dynamicTestConnection != null)
                {
                    //run connection
                    dynamicTestConnection.setup();
                    dynamicTestConnections.add(dynamicTestConnection);
                    //register to list
                    testConnections.putIfAbsent(connectionTestManager.getDatabaseType(), dynamicTestConnection.getConnection());
                }
            }
        }
        return testConnections;
    }

    @Override
    public void shutDown() throws Exception
    {
        for (DynamicTestConnection dynamicTestConnection : dynamicTestConnections )
        {
            dynamicTestConnection.cleanup();
        }
        super.shutDown();
    }

}
