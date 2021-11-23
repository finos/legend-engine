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

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.connection.test.utils.ConnectionPoolTestUtils;
import org.finos.legend.engine.plan.execution.stores.relational.connection.test.utils.H2TestUtils;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.security.auth.Subject;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;

public class TestLocalH2ConnectionCreation extends DbSpecificTests
{
    private ConnectionManagerSelector connectionManagerSelector;
    private RelationalExecutorInfo relationalExecutorInfo;

    @Override
    protected Subject getSubject()
    {
        return null;
    }

    @Before
    public void setup()
    {
        this.relationalExecutorInfo = new RelationalExecutorInfo();
        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList(),relationalExecutorInfo);
    }

    @Test
    public void userAcquiresSingleConnection()
    {
        Identity identity1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("identity1");
        RelationalDatabaseConnection db1Conn1 = this.buildLocalH2DatasourceSpec();

        // User gets a connection
        Connection conn1 = this.connectionManagerSelector.getDatabaseConnection(identity1, db1Conn1);
        String db1Conn1Name = h2Name(conn1);
        assertNotNull(db1Conn1Name);

        // We do have a connection pool for the user
        assertEquals(1, ConnectionPoolTestUtils.countNumHikariPools(identity1.getName()));
        H2TestUtils.closeProperly(conn1);
    }

    @Test
    public void userAcquiresConcurrentConnectionsToSameDb() throws Exception
    {
        Identity identity1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("identity1");
        RelationalDatabaseConnection db1 = this.buildLocalH2DatasourceSpec();

        // User gets connection 1
        Connection db1Conn1 = this.connectionManagerSelector.getDatabaseConnection(identity1, db1);
        String db1Conn1Name = h2Name(db1Conn1);
        assertNotNull(db1Conn1Name);

        // User gets connection 2
        Connection db1Conn2 = this.connectionManagerSelector.getDatabaseConnection(identity1, db1);
        String db1Conn2Name = h2Name(db1Conn2);
        assertNotNull(db1Conn2Name);

        // Connections are distinct
        assertNotSame(db1Conn1Name, db1Conn2Name);
        //we only have one DS
        ConnectionKey key1 = this.connectionManagerSelector.generateKeyFromDatabaseConnection(db1);
        Assert.assertNotNull(key1);
        assertNotNull(ConnectionPoolTestUtils.getDatasource(key1.shortId()));

        // We have a connection pool for the user
        assertNotNull(ConnectionPoolTestUtils.getDatasource(key1.shortId()).getConnectionPoolByUser().get(identity1.getName()));
        assertEquals(1, ConnectionPoolTestUtils.getDatasource(key1.shortId()).getConnectionPoolByUser().size());

        H2TestUtils.closeProperly(db1Conn1, db1Conn2);
    }

    @Test
    public void userAcquiresConcurrentConnectionsToSameDSSpecification() throws Exception
    {
        Identity identity1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("identity1");
        RelationalDatabaseConnection db1 = this.buildLocalH2DatasourceSpec();

        Connection db1Conn1 = this.connectionManagerSelector.getDatabaseConnection(identity1, db1);
        String db1Conn1Name = h2Name(db1Conn1);
        assertNotNull(db1Conn1Name);

        RelationalDatabaseConnection db2 = this.buildLocalH2DatasourceSpec();
        Connection db2Conn1 = this.connectionManagerSelector.getDatabaseConnection(identity1, db2);
        String db2Conn1Name = h2Name(db2Conn1);
        Assert.assertNotNull(db2Conn1Name);

        // Connections are distinct
        assertNotSame(db1Conn1Name, db2Conn1Name);

        // We have a connection pool for the user
        DataSourceSpecification dataSourceSpecification = ConnectionPoolTestUtils.getDatasource(this.connectionManagerSelector.generateKeyFromDatabaseConnection(db1).shortId());
        Assert.assertNotNull(dataSourceSpecification);
        assertEquals(1,dataSourceSpecification.getConnectionPoolByUser().size());
        assertNotNull(dataSourceSpecification.getConnectionPoolByUser().get(identity1.getName()));

        H2TestUtils.closeProperly(db1Conn1, db2Conn1);
    }

    @Test
    public void userAcquiresConcurrentConnectionsToDifferentDbs() throws Exception
    {
        Identity identity1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("identity1");
        RelationalDatabaseConnection db1 = this.buildLocalH2DatasourceSpec();

        Connection db1Conn1 = this.connectionManagerSelector.getDatabaseConnection(identity1, db1);
        String db1Conn1Name = h2Name(db1Conn1);
        assertNotNull(db1Conn1Name);

        RelationalDatabaseConnection db2 = this.buildLocalH2DatasourceSpec(Lists.mutable.with("drop table if exists Test1;"));

        Connection db2Conn1 = this.connectionManagerSelector.getDatabaseConnection(identity1, db2);
        String db2Conn1Name = h2Name(db1Conn1);

        // Connections are distinct
        assertNotSame(db1Conn1Name, db2Conn1Name);

        // We have 2 connection pools for the user
        assertEquals(2, ConnectionPoolTestUtils.countNumHikariPools(identity1.getName()));
        assertEquals(1, ConnectionPoolTestUtils.getDatasource(this.connectionManagerSelector.generateKeyFromDatabaseConnection(db1).shortId()).getConnectionPoolByUser().size());
        assertEquals(1, ConnectionPoolTestUtils.getDatasource(this.connectionManagerSelector.generateKeyFromDatabaseConnection(db2).shortId()).getConnectionPoolByUser().size());


        H2TestUtils.closeProperly(db1Conn1, db2Conn1);
    }

    @Test
    public void multipleUsersAcquireConnectionsToDifferentDatabases() throws Exception
    {
        Identity identity1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("identity1");
        RelationalDatabaseConnection db1 = this.buildLocalH2DatasourceSpec();

        Connection db1Conn1 = this.connectionManagerSelector.getDatabaseConnection(identity1, db1);
        String db1Conn1Name = h2Name(db1Conn1);

        Identity identity2 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("identity2");
        RelationalDatabaseConnection db2 = this.buildLocalH2DatasourceSpec(Lists.mutable.with("drop table if exists Test2;"));

        Connection db2Conn1 = this.connectionManagerSelector.getDatabaseConnection(identity2, db2);
        String db2Conn1Name = h2Name(db2Conn1);

        // Connections are distinct
        assertNotSame(db1Conn1Name, db2Conn1Name);

        //connections keys are not the same
        ConnectionKey key1 = this.connectionManagerSelector.generateKeyFromDatabaseConnection(db1);
        Assert.assertNotNull(key1);
        ConnectionKey key2 = this.connectionManagerSelector.generateKeyFromDatabaseConnection(db2);
        Assert.assertNotNull(key2);

        Assert.assertNotSame(key1, key2);

        DataSourceSpecification datasourceSpecification1 = ConnectionPoolTestUtils.getDatasource(key1.shortId());
        assertNotNull(datasourceSpecification1);
        Assert.assertEquals(1, datasourceSpecification1.getConnectionPoolByUser().size());

        DataSourceSpecification datasourceSpecification2 = ConnectionPoolTestUtils.getDatasource(key2.shortId());
        assertNotNull(datasourceSpecification2);
        Assert.assertEquals(1, datasourceSpecification2.getConnectionPoolByUser().size());


        H2TestUtils.closeProperly(db1Conn1, db2Conn1);
    }

    @Test
    public void multipleUsersAcquireConnectionsToSameDatabase() throws Exception
    {
        Identity identity1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("identity1");
        RelationalDatabaseConnection db1 = this.buildLocalH2DatasourceSpec();

        Connection db1Conn1 = this.connectionManagerSelector.getDatabaseConnection(identity1, db1);
        String db1Conn1Name = h2Name(db1Conn1);

        Identity identity2 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("identity2");
        RelationalDatabaseConnection db2 = this.buildLocalH2DatasourceSpec();

        Connection db2Conn1 = this.connectionManagerSelector.getDatabaseConnection(identity2, db2);
        String db2Conn1Name = h2Name(db2Conn1);

        // Connections are distinct
        assertNotSame(db1Conn1Name, db2Conn1Name);

        //connections keys are same
        ConnectionKey key1 = this.connectionManagerSelector.generateKeyFromDatabaseConnection(db1);
        Assert.assertNotNull(key1);
        ConnectionKey key2 = this.connectionManagerSelector.generateKeyFromDatabaseConnection(db2);
        Assert.assertNotNull(key2);

        Assert.assertEquals(key1, key2);

        DataSourceSpecification datasourceSpecification1 = ConnectionPoolTestUtils.getDatasource(key1.shortId());
        assertNotNull(datasourceSpecification1);
        Assert.assertEquals(2, datasourceSpecification1.getConnectionPoolByUser().size());

        assertNotNull(datasourceSpecification1.getConnectionPoolByUser().get(identity1.getName()));
        assertNotNull(datasourceSpecification1.getConnectionPoolByUser().get(identity2.getName()));

        H2TestUtils.closeProperly(db1Conn1, db2Conn1);
    }


    private String h2Name(Connection connection)
    {
        return H2TestUtils.unwrapWrappedH2Connection(connection).getTraceObjectName();
    }

    private RelationalDatabaseConnection buildLocalH2DatasourceSpec(List<String> setupSqls)
    {
        LocalH2DatasourceSpecification localH2DatasourceSpec = new LocalH2DatasourceSpecification(null, setupSqls);
        TestDatabaseAuthenticationStrategy testDatabaseAuthSpec = new TestDatabaseAuthenticationStrategy();
        return new RelationalDatabaseConnection(localH2DatasourceSpec, testDatabaseAuthSpec, DatabaseType.H2);
    }

    public RelationalDatabaseConnection buildLocalH2DatasourceSpec()
    {
        return buildLocalH2DatasourceSpec(Lists.mutable.with("drop table if exists PersonTable;",
                "create table PersonTable(id INT, firmId INT, firstName VARCHAR(200), lastName VARCHAR(200));",
                "insert into PersonTable (id, firmId, firstName, lastName) values (1, 1, 'pierre', 'de belen');",
                "drop table if exists FirmTable;",
                "create table FirmTable(id INT, legalName VARCHAR(200));",
                "insert into FirmTable (id, legalName) values (1, 'firm')"));
    }
}