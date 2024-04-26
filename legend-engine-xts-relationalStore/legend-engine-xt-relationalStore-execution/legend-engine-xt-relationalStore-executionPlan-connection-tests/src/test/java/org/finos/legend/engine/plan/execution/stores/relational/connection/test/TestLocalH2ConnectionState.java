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
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.ds.state.ConnectionStateManager;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.connection.test.utils.H2TestUtils;
import org.finos.legend.engine.plan.execution.stores.relational.connection.test.utils.ReflectionUtils;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.TestDatabaseAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.LocalH2DatasourceSpecification;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.junit.Before;
import org.junit.Test;

import javax.security.auth.Subject;
import java.sql.Connection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;

public class TestLocalH2ConnectionState extends DbSpecificTests
{
    private ConnectionManagerSelector connectionManagerSelector;
    public static final MutableList<String> SETUP_SQLS = Lists.mutable.with("drop table if exists PersonTable;",
            "create table PersonTable(id INT, firmId INT, firstName VARCHAR(200), lastName VARCHAR(200));",
            "insert into PersonTable (id, firmId, firstName, lastName) values (1, 1, 'pierre', 'de belen');",
            "drop table if exists FirmTable;",
            "create table FirmTable(id INT, legalName VARCHAR(200));",
            "insert into FirmTable (id, legalName) values (1, 'firm')");

    @Override
    protected Subject getSubject()
    {
        return null;
    }

    @Before
    public void setup() throws Exception
    {
        // The manager is a singleton. Reset singleton to avoid interference from other tests
        ConcurrentHashMap connectionPools = (ConcurrentHashMap) ReflectionUtils.getFieldUsingReflection(ConnectionStateManager.class, ConnectionStateManager.getInstance(), "connectionPools");
        connectionPools.clear();
        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList());
    }

    @Test
    public void stateAccumulatedForLocalH2() throws Exception
    {
        ConnectionStateManager stateManager = ConnectionStateManager.getInstance();
        assertEquals("mismatch in state count", 0, stateManager.size());

        Identity identity1 = IdentityFactoryProvider.getInstance().makeIdentityForTesting("identity1");
        RelationalDatabaseConnection db1 = this.buildLocalH2DatasourceSpec();

        List<Connection> connections = IntStream.range(0, 10)
                .mapToObj(i -> this.connectionManagerSelector.getDatabaseConnection(identity1, db1))
                .collect(Collectors.toList());

        Set<String> connectionNames = connections.stream()
                .map(connection -> H2TestUtils.unwrapWrappedH2Connection(connection))
                .map(connection -> connection.getTraceObjectName())
                .collect(Collectors.toSet());
        assertEquals("did not create distinct connections", 10, connectionNames.size());

        assertEquals("mismatch in state count", 1, stateManager.size());
    }

    private RelationalDatabaseConnection buildLocalH2DatasourceSpec() throws Exception
    {

        LocalH2DatasourceSpecification localH2DatasourceSpec = buildDataSourceSpecification(SETUP_SQLS);
        TestDatabaseAuthenticationStrategy testDatabaseAuthSpec = new TestDatabaseAuthenticationStrategy();
        return new RelationalDatabaseConnection(localH2DatasourceSpec, testDatabaseAuthSpec, DatabaseType.H2);
    }

    private LocalH2DatasourceSpecification buildDataSourceSpecification(MutableList<String> setupSqls)
    {
        return new LocalH2DatasourceSpecification(null, setupSqls);
    }
}