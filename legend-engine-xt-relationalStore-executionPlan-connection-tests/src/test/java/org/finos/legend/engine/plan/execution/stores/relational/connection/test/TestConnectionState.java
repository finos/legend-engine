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

import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.IdentityState;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.connection.test.utils.ConnectionPoolTestUtils;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.finos.legend.engine.shared.core.port.DynamicPortGenerator;
import org.h2.tools.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.sql.Connection;
import java.time.Duration;
import java.util.Collections;

import static org.junit.Assert.*;

public class TestConnectionState
{
    private Server server;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private ConnectionManagerSelector connectionManagerSelector;

    @Before
    public void setup() throws Exception
    {
        server = AlloyH2Server.startServer(DynamicPortGenerator.generatePort());

        // We maintain a bunch of singleton state. We have to reset this state so as to avoid interference between tests
        ConnectionPoolTestUtils.resetDatasourceSpecificationSingletonState();
        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList());
    }

    @After
    public void shutDown()
    {
        if (server != null)
        {
            server.shutdown();
            server.stop();
        }
    }

    @Test
    public void connectionStateCreation() throws Exception
    {
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentityForTesting("testuser1");

        // User gets connection to db1
        RelationalDatabaseConnection database1 = buildStaticDatabaseSpec("127.0.0.1", server.getPort(), "db1");
        this.connectionManagerSelector.getDatabaseConnection(identity, database1);

        //verify connection state for user1 exists
        String poolName = String.format("DBPool_Static_host:127.0.0.1_port:%d_db:db1_type:TestDB_testuser1_org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential", server.getPort());
        IdentityState identityState = ConnectionStateManager.getInstance().getConnectionStateManagerPOJO(poolName);
        assertEquals("testuser1", identityState.getIdentity().getName());
        assertNotNull(identityState.getCredentialSupplier());
    }

    @Test
    public void connectionStateUpdate() throws Exception
    {
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentityForTesting("testuser1");

        // User gets connection to db1
        RelationalDatabaseConnection database1 = buildStaticDatabaseSpec("127.0.0.1", server.getPort(), "db1");
        this.connectionManagerSelector.getDatabaseConnection(identity, database1);

        //verify connection state for user1 exists
        String poolName = String.format("DBPool_Static_host:127.0.0.1_port:%d_db:db1_type:TestDB_testuser1_org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential", server.getPort());
        DataSourceWithStatistics identityState1 = ConnectionStateManager.getInstance().getDataSourceByPoolName(poolName);
        assertEquals("testuser1", identityState1.getIdentity().getName());
        assertNotNull(identityState1.getCredentialSupplier());

        // User gets another connection to db1
        this.connectionManagerSelector.getDatabaseConnection(identity, database1);

        //verify connection state for user1 exists
        IdentityState identityState2 = ConnectionStateManager.getInstance().getConnectionStateManagerPOJO(poolName);
        assertEquals("testuser1", identityState2.getIdentity().getName());
        assertNotNull(identityState2.getCredentialSupplier());

        // Verify connection state has been reset
        assertNotSame("expected distinct state objects but got the same one", identityState1, identityState2);
    }

    @Test
    public void connectionStateReset() throws Exception
    {
        Identity identity = IdentityFactoryProvider.getInstance().makeIdentityForTesting("testuser1");

        // User gets connection to db1
        RelationalDatabaseConnection database1 = buildStaticDatabaseSpec("127.0.0.1", server.getPort(), "db1");
        Connection connection = this.connectionManagerSelector.getDatabaseConnection(identity, database1);

        //verify connection state for user1 exists
        String poolName = String.format("DBPool_Static_host:127.0.0.1_port:%d_db:db1_type:TestDB_testuser1_org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential", server.getPort());
        IdentityState identityState1 = ConnectionStateManager.getInstance().getConnectionStateManagerPOJO(poolName);
        assertEquals("testuser1", identityState1.getIdentity().getName());
        assertNotNull(identityState1.getCredentialSupplier());

        connection.close();
        // Reset connection state - This simulates a case where the state manager evicts state objects
        ConnectionStateManager.getInstance().evictUnusedPoolsOlderThan(Duration.ofMillis(1));
        IdentityState identityState = ConnectionStateManager.getInstance().getConnectionStateManagerPOJO(poolName);
        assertNull(identityState);

        // User gets another connection to db1
        this.connectionManagerSelector.getDatabaseConnection(identity, database1);

        //Verify new connection state has been created for user
        IdentityState identityState2 = ConnectionStateManager.getInstance().getConnectionStateManagerPOJO(poolName);
        assertEquals("testuser1", identityState2.getIdentity().getName());
        assertNotNull(identityState2.getCredentialSupplier());

        assertNotSame("expected distinct state objects but got the same one", identityState1, identityState2);
    }

    public RelationalDatabaseConnection buildStaticDatabaseSpec(String host, int port, String databaseName) throws Exception
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