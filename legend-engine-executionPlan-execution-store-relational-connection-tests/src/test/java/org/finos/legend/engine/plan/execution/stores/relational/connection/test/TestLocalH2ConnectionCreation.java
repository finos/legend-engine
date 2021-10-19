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

import java.sql.Connection;
import java.util.Collections;

import javax.security.auth.Subject;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.RelationalExecutorInfo;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.connection.test.utils.ConnectionPoolTestUtils;
import org.finos.legend.engine.plan.execution.stores.relational.connection.test.utils.H2TestUtils;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestLocalH2ConnectionCreation extends DbSpecificTests
{
    private ConnectionManagerSelector connectionManagerSelector;

    @Override
    protected Subject getSubject()
    {
        return null;
    }

    @Before
    public void setup()
    {
        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList(), new RelationalExecutorInfo());
    }

    @Test
    public void userAcquiresSingleConnection() throws Exception
    {
        Identity identity1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("identity1");
        RelationalDatabaseConnection db1Conn1 = this.buildLocalH2DatasourceSpec();

        // User gets a connection
        Connection conn1 = this.connectionManagerSelector.getDatabaseConnection(identity1, db1Conn1);
        String db1Conn1Name = h2Name(conn1);
        assertNotNull(db1Conn1Name);

        // We do not have a connection pool for the user
        assertEquals(0, ConnectionPoolTestUtils.countNumHikariPools(identity1.getName()));
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

        // We do not have a connection pool for the user
        assertEquals(0, ConnectionPoolTestUtils.countNumHikariPools(identity1.getName()));

        H2TestUtils.closeProperly(db1Conn1, db1Conn2);
    }

    @Test
    public void userAcquiresConcurrentConnectionsToDifferentDbs() throws Exception
    {
        Identity identity1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("identity1");
        RelationalDatabaseConnection db1 = this.buildLocalH2DatasourceSpec();

        Connection db1Conn1 = this.connectionManagerSelector.getDatabaseConnection(identity1, db1);
        String db1Conn1Name = h2Name(db1Conn1);
        assertNotNull(db1Conn1Name);

        RelationalDatabaseConnection db2 = this.buildLocalH2DatasourceSpec();

        Connection db2Conn1 = this.connectionManagerSelector.getDatabaseConnection(identity1, db2);
        String db2Conn1Name = h2Name(db1Conn1);

        // Connections are distinct
        assertNotSame(db1Conn1Name, db2Conn1Name);

        // We do not have a connection pool for the user
        assertEquals(0, ConnectionPoolTestUtils.countNumHikariPools(identity1.getName()));

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
        RelationalDatabaseConnection db2 = this.buildLocalH2DatasourceSpec();

        Connection db2Conn1 = this.connectionManagerSelector.getDatabaseConnection(identity2, db2);
        String db2Conn1Name = h2Name(db2Conn1);

        // Connections are distinct
        assertNotSame(db1Conn1Name, db2Conn1Name);

        // We do not have a connection pool for either user
        assertEquals(0, ConnectionPoolTestUtils.countNumHikariPools(identity1.getName()));
        assertEquals(0, ConnectionPoolTestUtils.countNumHikariPools(identity2.getName()));

        H2TestUtils.closeProperly(db1Conn1, db2Conn1);
    }

    private String h2Name(Connection connection)
    {
        return H2TestUtils.unwrapWrappedH2Connection(connection).getTraceObjectName();
    }

    public RelationalDatabaseConnection buildLocalH2DatasourceSpec() throws Exception
    {
        MutableList<String> setupSqls = Lists.mutable.with("drop table if exists PersonTable;",
                "create table PersonTable(id INT, firmId INT, firstName VARCHAR(200), lastName VARCHAR(200));",
                "insert into PersonTable (id, firmId, firstName, lastName) values (1, 1, 'pierre', 'de belen');",
                "drop table if exists FirmTable;",
                "create table FirmTable(id INT, legalName VARCHAR(200));",
                "insert into FirmTable (id, legalName) values (1, 'firm')");

        LocalH2DatasourceSpecification localH2DatasourceSpec = new LocalH2DatasourceSpecification(null, setupSqls);
        TestDatabaseAuthenticationStrategy testDatabaseAuthSpec = new TestDatabaseAuthenticationStrategy();
        return new RelationalDatabaseConnection(localH2DatasourceSpec, testDatabaseAuthSpec, DatabaseType.H2);
    }
}