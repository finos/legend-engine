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
import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ConnectionKey;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.specifications.LocalH2DataSourceSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.connection.test.utils.ConnectionPoolTestUtils;
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

import static org.junit.Assert.*;

public class TestLocalH2DatasourceSpecificationCaching extends DbSpecificTests {
    private ConnectionManagerSelector connectionManagerSelector;

    @Override
    protected Subject getSubject() {
        return null;
    }

    @Before
    public void setup() throws Exception
    {
        // DatasourceSpecification class maintains static state. We clear it to avoid interference between tests
        ConnectionPoolTestUtils.resetDatasourceSpecificationSingletonState();

        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList());
    }

    /*
        This test exists to capture existing "buggy" behavior wrto caching of LocalH2DatasourceSpecification objects.
        DatasourceSpecification.java maintains a map of DatasourceSpecification instances.
        For LocalH2, these instances are keyed by key truncating the test setup SQLs. See LocalH2DataSourceSpecificationKey.
        This means that multiple LocalH2DatasourceSpecifications that might be created to serve test requests from distinct projects/users get overridden.
        They get overridden as long they have the same test setup SQLs.

        Even though distinct user requests might end up using the same LocalH2DatasourceSpecification, the underlying H2 connections are not shared across test requests.
        This is because we construct H2 connections with a database name of "". This results in H2 creating a unique database per connection.
     */

    @Test
    public void multipleRequestsShareSameLocalH2DatasourceSpecification() throws Exception {
        Identity identity1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("identity1");

        assertEquals(0, ConnectionPoolTestUtils.getDataSourceSpecifications().size());
        assertEquals(0, ConnectionPoolTestUtils.getConnectionPools().size());

        // User gets a connection
        RelationalDatabaseConnection spec1 = this.buildLocalH2DatasourceSpec();
        Connection conn1 = this.connectionManagerSelector.getDatabaseConnection(identity1, spec1);

        assertEquals(1, ConnectionPoolTestUtils.getDataSourceSpecifications().size());
        LocalH2DataSourceSpecification localH2DatasourceSpecification1 = (LocalH2DataSourceSpecification) (ConnectionPoolTestUtils.getDataSourceSpecifications().stream().findFirst().get());

        // User gets a connection
        RelationalDatabaseConnection spec2 = this.buildLocalH2DatasourceSpec();
        Connection conn2 = this.connectionManagerSelector.getDatabaseConnection(identity1, spec2);

        assertEquals(1, ConnectionPoolTestUtils.getDataSourceSpecifications().size());
        LocalH2DataSourceSpecification localH2DatasourceSpecification2 = (LocalH2DataSourceSpecification) (ConnectionPoolTestUtils.getDataSourceSpecifications().stream().findFirst().get());

        // User gets a connection
        RelationalDatabaseConnection spec3 = this.buildLocalH2DatasourceSpec();
        Connection conn3 = this.connectionManagerSelector.getDatabaseConnection(identity1, spec3);

        assertEquals(1, ConnectionPoolTestUtils.getDataSourceSpecifications().size());
        LocalH2DataSourceSpecification localH2DatasourceSpecification3 = (LocalH2DataSourceSpecification) (ConnectionPoolTestUtils.getDataSourceSpecifications().stream().findFirst().get());

        assertSame(localH2DatasourceSpecification1, localH2DatasourceSpecification2);
        assertSame(localH2DatasourceSpecification2, localH2DatasourceSpecification3);
    }

    public RelationalDatabaseConnection buildLocalH2DatasourceSpec() throws Exception {
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

    @Test
    public void multipleRequestsDoNoShareSameLocalH2DatasourceSpecificationWhenSetupSQLsDiffer() throws Exception {
        Identity identity1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("identity1");

        // User gets a connection
        RelationalDatabaseConnection spec1 = this.buildLocalH2DatasourceSpecWithTableName("PERSON1");
        Connection conn1 = this.connectionManagerSelector.getDatabaseConnection(identity1, spec1);


        LocalH2DataSourceSpecification localH2DatasourceSpecification1 = (LocalH2DataSourceSpecification)getDatasourceSpecification(this.connectionManagerSelector.generateKeyFromDatabaseConnection(spec1));

        // User gets a connection
        RelationalDatabaseConnection spec2 = this.buildLocalH2DatasourceSpecWithTableName("PERSON2");
        Connection conn2 = this.connectionManagerSelector.getDatabaseConnection(identity1, spec2);


        LocalH2DataSourceSpecification localH2DatasourceSpecification2 = (LocalH2DataSourceSpecification)getDatasourceSpecification( this.connectionManagerSelector.generateKeyFromDatabaseConnection(spec2));

        // User gets a connection
        RelationalDatabaseConnection spec3 = this.buildLocalH2DatasourceSpecWithTableName("PERSON3");
        Connection conn3 = this.connectionManagerSelector.getDatabaseConnection(identity1, spec3);


        LocalH2DataSourceSpecification localH2DatasourceSpecification3 = (LocalH2DataSourceSpecification)getDatasourceSpecification(this.connectionManagerSelector.generateKeyFromDatabaseConnection(spec3));

        assertNotSame(localH2DatasourceSpecification1, localH2DatasourceSpecification2);
        assertNotSame(localH2DatasourceSpecification2, localH2DatasourceSpecification3);
        assertNotSame(localH2DatasourceSpecification1, localH2DatasourceSpecification3);
    }

    public DataSourceSpecification getDatasourceSpecification(ConnectionKey connectionKey) throws Exception
    {
        return ConnectionPoolTestUtils.getConnectionPools().valuesView().detect(pool-> pool.getConnectionKey().equals(connectionKey)).getDataSourceSpecification();
    }

    public RelationalDatabaseConnection buildLocalH2DatasourceSpecWithTableName(String personTableName)
    {
        MutableList<String> setupSqls = Lists.mutable.with
                        (
                                "drop table if exists __PERSON_TABLE__;",
                                "create table __PERSON_TABLE__(id INT, firmId INT, firstName VARCHAR(200), lastName VARCHAR(200));",
                                "insert into __PERSON_TABLE__ (id, firmId, firstName, lastName) values (1, 1, 'pierre', 'de belen');",
                                "drop table if exists FirmTable;",
                                "create table FirmTable(id INT, legalName VARCHAR(200));",
                                "insert into FirmTable (id, legalName) values (1, 'firm')")
                .collect(sql -> sql.replaceAll("__PERSON_TABLE__", personTableName));

        LocalH2DatasourceSpecification localH2DatasourceSpec = new LocalH2DatasourceSpecification(null, setupSqls);
        TestDatabaseAuthenticationStrategy testDatabaseAuthSpec = new TestDatabaseAuthenticationStrategy();
        return new RelationalDatabaseConnection(localH2DatasourceSpec, testDatabaseAuthSpec, DatabaseType.H2);
    }
}