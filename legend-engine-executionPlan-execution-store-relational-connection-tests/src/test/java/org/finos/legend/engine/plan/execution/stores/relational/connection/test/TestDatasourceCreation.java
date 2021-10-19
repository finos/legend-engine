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

import java.sql.SQLException;
import java.util.Collections;

import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.finos.legend.engine.plan.execution.stores.relational.AlloyH2Server;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceWithStatistics;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.StaticDataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.connection.test.utils.ConnectionPoolTestUtils;
import org.finos.legend.engine.plan.execution.stores.relational.connection.test.utils.ReflectionUtils;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.h2.tools.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class TestDatasourceCreation
{
    private static Server server;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private ConnectionManagerSelector connectionManagerSelector;

    @BeforeClass
    public static void setupClass() throws SQLException
    {
        server = AlloyH2Server.startServer(9095);
    }

    @After
    public void shutDownClass()
    {
        if (server != null)
        {
            server.shutdown();
        }
    }

    @Before
    public void setup() throws Exception
    {
        // We maintain a bunch of singleton state. We have to reset this state so as to avoid interference between tests
        ConnectionPoolTestUtils.resetDatasourceSpecificationSingletonState();
        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList(), new RelationalExecutorInfo());
    }

    @Test
    public void userAcquiresConcurrentConnectionsToSameDb() throws Exception
    {
        ConcurrentMutableMap datasourceSpecifications = null;

        Identity identity = IdentityFactoryProvider.getInstance().makeIdentityForTesting("testuser1");

        // User gets connection to db1
        RelationalDatabaseConnection database1 = buildStaticDatabaseSpec("127.0.0.1", this.server.getPort(), "db1");
        String key = "Static_host:127.0.0.1_port:9095_db:db1_type:TestDB";
        this.connectionManagerSelector.getDatabaseConnection(identity, database1);

        // We have a single data source
        datasourceSpecifications = ConnectionPoolTestUtils.getDataSourceSpecifications();
        assertEquals(1, datasourceSpecifications.size());

        // We have a single data source for user1
        DataSourceWithStatistics datasource1 = this.getDatasourceForUser(this.getDatasourceBykey(datasourceSpecifications, key), identity);

        // User gets another connection to db1
        this.connectionManagerSelector.getDatabaseConnection(identity, database1);

        // We still have a single data source for user1
        datasourceSpecifications = ConnectionPoolTestUtils.getDataSourceSpecifications();
        assertEquals(1, datasourceSpecifications.size());
        DataSourceWithStatistics datasource2 = this.getDatasourceForUser(this.getDatasourceBykey(datasourceSpecifications, key), identity);

        assertSame("found distinct datasources when same datasource was expected", datasource1, datasource2);
    }

    @Test
    public void userAcquiresConcurrentConnectionsToDifferentDbs() throws Exception
    {
        ConcurrentMutableMap datasourceSpecifications = null;

        Identity identity = IdentityFactoryProvider.getInstance().makeIdentityForTesting("testuser1");

        // User gets connection to db1
        RelationalDatabaseConnection database1 = buildStaticDatabaseSpec("127.0.0.1", this.server.getPort(), "db2");
        String key1 = "Static_host:127.0.0.1_port:9095_db:db2_type:TestDB";
        this.connectionManagerSelector.getDatabaseConnection(identity, database1);

        // We have a single data source
        datasourceSpecifications = ConnectionPoolTestUtils.getDataSourceSpecifications();
        assertEquals(1, datasourceSpecifications.size());

        // We have a single data source for user1
        DataSourceWithStatistics datasource1 = this.getDatasourceForUser(this.getDatasourceBykey(datasourceSpecifications, key1), identity);

        // User gets another connection to db2
        RelationalDatabaseConnection database2 = buildStaticDatabaseSpec("127.0.0.1", this.server.getPort(), "db3");
        String key2 = "Static_host:127.0.0.1_port:9095_db:db3_type:TestDB";
        this.connectionManagerSelector.getDatabaseConnection(identity, database2);

        // We now have 2 data sources one per database
        datasourceSpecifications = ConnectionPoolTestUtils.getDataSourceSpecifications();
        assertEquals(2, datasourceSpecifications.size());

        // We have a single data source for user2
        DataSourceWithStatistics datasource2 = this.getDatasourceForUser(this.getDatasourceBykey(datasourceSpecifications, key2), identity);

        assertNotSame("found same datasource when distinct datasources was expected", datasource1, datasource2);
    }

    @Test
    public void multipleUsersAcquireConnectionsToDifferentDatabases() throws Exception
    {
        ConcurrentMutableMap datasourceSpecifications = null;

        Identity identity1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("testuser1");

        // User gets connection to db1
        RelationalDatabaseConnection database1 = buildStaticDatabaseSpec("127.0.0.1", this.server.getPort(), "db4");
        String key1 = "Static_host:127.0.0.1_port:9095_db:db4_type:TestDB";
        this.connectionManagerSelector.getDatabaseConnection(identity1, database1);

        // We have a single data source
        datasourceSpecifications = ConnectionPoolTestUtils.getDataSourceSpecifications();
        assertEquals(1, datasourceSpecifications.size());

        // We have a single data source for user1
        DataSourceWithStatistics datasource1 = this.getDatasourceForUser(this.getDatasourceBykey(datasourceSpecifications, key1), identity1);

        Identity identity2 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("testuser2");

        // User gets another connection to db2
        RelationalDatabaseConnection database2 = buildStaticDatabaseSpec("127.0.0.1", this.server.getPort(), "db5");
        String key2 = "Static_host:127.0.0.1_port:9095_db:db5_type:TestDB";
        this.connectionManagerSelector.getDatabaseConnection(identity2, database2);

        // We now have 2 data sources one per database + user
        datasourceSpecifications = ConnectionPoolTestUtils.getDataSourceSpecifications();
        assertEquals(2, datasourceSpecifications.size());

        // We have a single data source for user2
        DataSourceWithStatistics datasource2 = this.getDatasourceForUser(this.getDatasourceBykey(datasourceSpecifications, key2), identity2);

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

    private DataSourceWithStatistics getDatasourceForUser(org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.StaticDataSourceSpecification dataSourceSpecification, Identity identity) throws Exception
    {
        ConcurrentMutableMap pools = (ConcurrentMutableMap)ReflectionUtils.getFieldUsingReflection(org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.StaticDataSourceSpecification.class, dataSourceSpecification, "connectionPoolByUser");
        return (DataSourceWithStatistics) pools.get(identity.getName());
    }

    private StaticDataSourceSpecification getDatasourceBykey(ConcurrentMutableMap datasourceSpecifications, String key)
    {
        return (org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.StaticDataSourceSpecification)datasourceSpecifications.get(key);
    }
}