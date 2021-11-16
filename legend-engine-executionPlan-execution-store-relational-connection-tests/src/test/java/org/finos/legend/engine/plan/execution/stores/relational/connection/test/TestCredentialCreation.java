// Copyright 2021 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.test;

import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProviderSelector;
import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.connection.test.utils.H2TestUtils;
import org.finos.legend.engine.plan.execution.stores.relational.connection.test.utils.ReflectionUtils;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.h2.jdbc.JdbcConnection;
import org.h2.tools.Server;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.*;

public class TestCredentialCreation
{
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private ConnectionManagerSelector connectionManagerSelector;

    private TestDatabaseAuthenticationFlowProvider testFlowProvider = null;

    private Server server;

    @Before
    public void setup() throws Exception
    {
        server = AlloyH2Server.startServer(DynamicPortGenerator.generatePort());

        installTestFlowProvider();
        assertStaticH2FlowIsAvailable();

        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList(), new RelationalExecutorInfo());
    }

    @After
    public void shutdown()
    {
        if (server != null)
        {
            server.shutdown();
            server.stop();
        }
    }

    private void installTestFlowProvider() throws Exception
    {
        ReflectionUtils.resetStaticField(DatabaseAuthenticationFlowProviderSelector.class, "INSTANCE");
        DatabaseAuthenticationFlowProviderSelector.enableFlowProvider(TestDatabaseAuthenticationFlowProvider.class);

        testFlowProvider = (TestDatabaseAuthenticationFlowProvider) DatabaseAuthenticationFlowProviderSelector.getProvider().get();
    }

    public void assertStaticH2FlowIsAvailable()
    {
        StaticDatasourceSpecification staticDatasourceSpecification = new StaticDatasourceSpecification();
        TestDatabaseAuthenticationStrategy testDatabaseAuthenticationStrategy = new TestDatabaseAuthenticationStrategy();
        RelationalDatabaseConnection relationalDatabaseConnection = new RelationalDatabaseConnection(staticDatasourceSpecification, testDatabaseAuthenticationStrategy, DatabaseType.H2);
        relationalDatabaseConnection.type = DatabaseType.H2;

        DatabaseAuthenticationFlowProvider flowProvider = DatabaseAuthenticationFlowProviderSelector.getProvider().get();
        Optional<DatabaseAuthenticationFlow> flow = flowProvider.lookupFlow(relationalDatabaseConnection);
        assertTrue("static h2 flow does not exist ", flow.isPresent());
    }

    @After
    public void cleanup()
    {
        DatabaseAuthenticationFlowProviderSelector.disableFlowProvider();
    }

    @Test
    public void credentialCreatedOnFirstConnectionCreation() throws Exception
    {
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentityForTesting("testuser1");

        // User gets connection to db1
        RelationalDatabaseConnection database1 = buildStaticDatabaseSpec("127.0.0.1", server.getPort(), "db1");
        Connection connection = this.connectionManagerSelector.getDatabaseConnection(identity, database1);
        assertNotNull(connection);

        // Credential is created for this connection
        assertEquals("mismatch in number of credentials creation requests", 2, testFlowProvider.testFlow.invocations.size());
    }

    @Test
    public void credentialNotCreatedWhenConnectionIsReused() throws Exception
    {
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentityForTesting("testuser1");

        // User gets connection to db1
        RelationalDatabaseConnection database1 = buildStaticDatabaseSpec("127.0.0.1", server.getPort(), "db1");
        Connection connection1 = this.connectionManagerSelector.getDatabaseConnection(identity, database1);
        assertNotNull(connection1);
        JdbcConnection underlyingJdbcConnection1 = H2TestUtils.unwrapHikariProxyConnection(connection1);

        // Credential is created for this connection
        assertEquals("mismatch in number of credentials creation requests", 2, testFlowProvider.testFlow.invocations.size());
        connection1.close();

        // User asks for a new connection - But connection is reused (because previously acquired connection was closed)
        Connection connection2 = this.connectionManagerSelector.getDatabaseConnection(identity, database1);
        JdbcConnection underlyingJdbcConnection2 = H2TestUtils.unwrapHikariProxyConnection(connection2);
        assertSame("found distinct connections when same connection was expected", underlyingJdbcConnection1, underlyingJdbcConnection2);

        // New credential was not created as new connection was not created
        assertEquals("mismatch in number of credentials creation requests", 2, testFlowProvider.testFlow.invocations.size());

        // User asks for a new connection - while keeping the old connection open
        Connection connection3 = this.connectionManagerSelector.getDatabaseConnection(identity, database1);
        JdbcConnection underlyingJdbcConnection3 = H2TestUtils.unwrapHikariProxyConnection(connection3);
        assertNotSame("found same connection when distinct connections was expected", underlyingJdbcConnection1, underlyingJdbcConnection3);

        // New credential was created as a new connection was created
        assertEquals("mismatch in number of credentials creation requests", 3, testFlowProvider.testFlow.invocations.size());
    }

    public RelationalDatabaseConnection buildStaticDatabaseSpec(String host, int port, String databaseName)
    {
        StaticDatasourceSpecification staticDatasourceSpecification = new StaticDatasourceSpecification();
        staticDatasourceSpecification.host = host;
        staticDatasourceSpecification.port = port;
        staticDatasourceSpecification.databaseName = databaseName;
        TestDatabaseAuthenticationStrategy testDatabaseAuthSpec = new TestDatabaseAuthenticationStrategy();
        RelationalDatabaseConnection relationalDatabaseConnection = new RelationalDatabaseConnection(staticDatasourceSpecification, testDatabaseAuthSpec, DatabaseType.H2);
        relationalDatabaseConnection.type = DatabaseType.H2;
        return relationalDatabaseConnection;
    }
}