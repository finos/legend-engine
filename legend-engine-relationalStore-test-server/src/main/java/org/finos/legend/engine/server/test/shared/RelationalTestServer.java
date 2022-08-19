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

import com.fasterxml.jackson.databind.jsontype.NamedType;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.finos.legend.engine.plan.execution.PlanExecutor;
import org.finos.legend.engine.plan.execution.api.ExecutePlanStrategic;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.DynamicTestConnection;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.Relational;
import org.finos.legend.engine.plan.execution.stores.relational.plugin.RelationalStoreExecutor;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.shared.core.url.EngineUrlStreamHandlerFactory;
import org.finos.legend.engine.shared.core.vault.EnvironmentVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public class RelationalTestServer extends TestServer<RelationalTestServerConfiguration>
{
    private final List<DynamicTestConnection> dynamicTestConnections = new ArrayList<>();
    private final NamedType[] extraConfigTypes;

    public static void main(String[] args) throws Exception
    {
        EngineUrlStreamHandlerFactory.initialize();
        new RelationalTestServer().run(args);
    }

    public RelationalTestServer(NamedType... extraConfigTypes)
    {
        this.extraConfigTypes = extraConfigTypes;
    }

    @Override
    public void initialize(Bootstrap<RelationalTestServerConfiguration> bootstrap)
    {
        super.initialize(bootstrap);
        bootstrap.getObjectMapper().registerSubtypes(this.extraConfigTypes);
    }

    @Override
    public void run(RelationalTestServerConfiguration serverConfiguration, Environment environment)
    {
        super.run(serverConfiguration, environment);
        Vault.INSTANCE.registerImplementation(new EnvironmentVaultImplementation());
        RelationalStoreExecutor relationalStoreExecutor = (RelationalStoreExecutor) Relational.build(serverConfiguration.relationalexecution);

        PlanExecutor planExecutor = PlanExecutor.newPlanExecutor(relationalStoreExecutor);
        environment.jersey().register(new ExecutePlanStrategic(planExecutor));

        environment.jersey().register(new TestConnectionProviderApi(getTestConnections(serverConfiguration)));

        ConnectionManagerSelector connectionManager = relationalStoreExecutor.getStoreState().getRelationalExecutor().getConnectionManager();
        environment.jersey().register(new ExecuteInRelationalDb(connectionManager));
    }

    public Map<DatabaseType, RelationalDatabaseConnection> getTestConnections(RelationalTestServerConfiguration serverConfiguration)
    {
        Map<DatabaseType, RelationalDatabaseConnection> testConnections = new HashMap<>();

        // register static test connections - static connections take precedence over dynamic connections
        for (DatabaseType dbType : serverConfiguration.staticTestConnections.keySet())
        {
            if (serverConfiguration.testConnectionsToEnable.contains(dbType))
            {
                testConnections.putIfAbsent(dbType, serverConfiguration.staticTestConnections.get(dbType));
                System.out.println("Configured to reach static connection for database : " + dbType);
            }
        }

        //Spin up dynamic database instances for testing
        for (DynamicTestConnection dynamicTestConnection : ServiceLoader.load(DynamicTestConnection.class))
        {
            //run only when in config
            if (!testConnections.containsKey(dynamicTestConnection.getDatabaseType())                 // start dynamic connection only if no static connection available for db type
                    && serverConfiguration.testConnectionsToEnable.contains(dynamicTestConnection.getDatabaseType())
                    && serverConfiguration.dynamicTestConnectionCreators.get(dynamicTestConnection.getDatabaseType()).equals(dynamicTestConnection.getClass().getName()))
            {
                //run setup and start connection
                dynamicTestConnection.setup();
                this.dynamicTestConnections.add(dynamicTestConnection);
                //register to list
                testConnections.putIfAbsent(dynamicTestConnection.getDatabaseType(), dynamicTestConnection.getConnection());
            }
        }
        return testConnections;
    }

    @Override
    public void shutDown() throws Exception
    {
        for (DynamicTestConnection dynamicTestConnection : this.dynamicTestConnections)
        {
            dynamicTestConnection.cleanup();
        }
        super.shutDown();
    }
}
