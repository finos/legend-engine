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

import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.driver.vendors.h2.H2Manager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.StaticDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.keys.StaticDataSourceSpecificationKey;
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

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class TestDatasourceCreation
{
    private Server server;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private ConnectionManagerSelector connectionManagerSelector;
    private ConnectionStateManager connectionStateManager;

    @Before
    public void setup() throws Exception
    {
        server = AlloyH2Server.startServer(DynamicPortGenerator.generatePort());

        // We maintain a bunch of singleton state. We have to reset this state so as to avoid interference between tests
        ConnectionPoolTestUtils.resetDatasourceSpecificationSingletonState();
        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList());
        this.connectionStateManager = ConnectionStateManager.getInstance();
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
    public void userAcquiresConcurrentConnectionsToSameDb() throws Exception
    {
        ConcurrentMutableMap datasourceSpecifications = null;

        Identity identity = IdentityFactoryProvider.getInstance().makeIdentityForTesting("testuser1");

        // User gets connection to db1
        RelationalDatabaseConnection database1 = buildStaticDatabaseSpec("127.0.0.1", server.getPort(), "db1");
        ConnectionKey connectionKey = this.connectionManagerSelector.generateKeyFromDatabaseConnection(database1);
        String key = this.connectionStateManager.poolNameFor(identity, connectionKey);
        this.connectionManagerSelector.getDatabaseConnection(identity, database1);

        // We have a single data source
        datasourceSpecifications = ConnectionPoolTestUtils.getDataSourceSpecifications();
        assertEquals(1, datasourceSpecifications.size());

        // We have a single data source for user1
        DataSourceWithStatistics datasource1 = this.getDatasourceByPool(key);

        // User gets another connection to db1
        this.connectionManagerSelector.getDatabaseConnection(identity, database1);

        // We still have a single data source for user1
        datasourceSpecifications = ConnectionPoolTestUtils.getDataSourceSpecifications();
        assertEquals(1, datasourceSpecifications.size());
        DataSourceWithStatistics datasource2 = this.getDatasourceByPool(key);

        assertSame("found distinct datasources when same datasource was expected", datasource1, datasource2);
    }

    @Test
    public void userAcquiresConcurrentConnectionsToDifferentDbs() throws Exception
    {
        ConcurrentMutableMap datasourceSpecifications = null;

        Identity identity = IdentityFactoryProvider.getInstance().makeIdentityForTesting("testuser1");

        // User gets connection to db1
        RelationalDatabaseConnection database1 = buildStaticDatabaseSpec("127.0.0.1", server.getPort(), "db2");
        ConnectionKey connectionKey = this.connectionManagerSelector.generateKeyFromDatabaseConnection(database1);
        String pool1 = this.connectionStateManager.poolNameFor(identity, connectionKey);
        this.connectionManagerSelector.getDatabaseConnection(identity, database1);


        // We have a single data source
        datasourceSpecifications = ConnectionPoolTestUtils.getDataSourceSpecifications();
        assertEquals(1, datasourceSpecifications.size());

        // We have a single data source for user1
        DataSourceWithStatistics datasource1 = this.getDatasourceByPool(pool1);

        // User gets another connection to db2
        RelationalDatabaseConnection database2 = buildStaticDatabaseSpec("127.0.0.1", server.getPort(), "db3");
        ConnectionKey key2 = this.connectionManagerSelector.generateKeyFromDatabaseConnection(database2);
        String pool2 = this.connectionStateManager.poolNameFor(identity, key2);

        this.connectionManagerSelector.getDatabaseConnection(identity, database2);

        // We now have 2 data sources one per database
        datasourceSpecifications = ConnectionPoolTestUtils.getDataSourceSpecifications();
        assertEquals(2, datasourceSpecifications.size());

        // We have a single data source for user2
        DataSourceWithStatistics datasource2 = this.getDatasourceByPool(pool2);

        assertNotSame("found same datasource when distinct datasources was expected", datasource1, datasource2);
    }

    @Test
    public void multipleUsersAcquireConnectionsToDifferentDatabases() throws Exception
    {
        ConcurrentMutableMap datasourceSpecifications = null;

        Identity identity1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("testuser1");

        // User gets connection to db1
        RelationalDatabaseConnection database1 = buildStaticDatabaseSpec("127.0.0.1", server.getPort(), "db4");
        ConnectionKey connectionKey = this.connectionManagerSelector.generateKeyFromDatabaseConnection(database1);
        String key1 = this.connectionStateManager.poolNameFor(identity1, connectionKey);

        this.connectionManagerSelector.getDatabaseConnection(identity1, database1);

        DataSourceSpecification ds = builStaticDataSourceSpecification("127.0.0.1", server.getPort(), "db4");
        assertEquals(ds.getConnectionKey(), connectionKey);

        // We have a single data source
        datasourceSpecifications = ConnectionPoolTestUtils.getDataSourceSpecifications();
        assertEquals(1, datasourceSpecifications.size());

        // We have a single data source for user1
        DataSourceWithStatistics datasource1 = this.getDatasourceByPool(key1);

        Identity identity2 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("testuser2");

        // User gets another connection to db2
        RelationalDatabaseConnection database2 = buildStaticDatabaseSpec("127.0.0.1", server.getPort(), "db5");
        ConnectionKey key2 = this.connectionManagerSelector.generateKeyFromDatabaseConnection(database2);
        String pool2 = this.connectionStateManager.poolNameFor(identity2, key2);
        this.connectionManagerSelector.getDatabaseConnection(identity2, database2);

        // We now have 2 data sources one per database + user
        datasourceSpecifications = ConnectionPoolTestUtils.getDataSourceSpecifications();
        assertEquals(2, datasourceSpecifications.size());

        // We have a single data source for user2
        DataSourceWithStatistics datasource2 = this.getDatasourceByPool(pool2);

        assertNotSame("found same datasource when distinct datasources was expected", datasource1, datasource2);
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

    private DataSourceWithStatistics getDatasourceByPool(String pool)
    {
        return ConnectionStateManager.getInstance().get(pool);
    }

    public StaticDataSourceSpecification builStaticDataSourceSpecification(String host, int port, String databaseName)
    {
        return new StaticDataSourceSpecification(
                new StaticDataSourceSpecificationKey(host, port, databaseName),
                new H2Manager(),
                new org.finos.legend.engine.plan.execution.stores.relational.connection.authentication.strategy.TestDatabaseAuthenticationStrategy());
    }
}