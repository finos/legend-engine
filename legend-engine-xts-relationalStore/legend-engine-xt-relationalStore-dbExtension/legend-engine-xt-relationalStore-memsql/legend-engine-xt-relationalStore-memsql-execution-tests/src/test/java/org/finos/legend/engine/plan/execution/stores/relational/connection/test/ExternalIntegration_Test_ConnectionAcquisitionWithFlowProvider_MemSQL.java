// Copyright 2023 Goldman Sachs
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
import org.finos.legend.engine.authentication.MemSQLTestDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.MemSQLTestDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.dynamicTestConnections.MemSQLTestContainer;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.DockerClientFactory;

import javax.security.auth.Subject;
import java.sql.Connection;
import java.util.Optional;

import static org.junit.Assume.assumeTrue;

public class ExternalIntegration_Test_ConnectionAcquisitionWithFlowProvider_MemSQL extends RelationalConnectionTest
{
    private ConnectionManagerSelector connectionManagerSelector;
    private MemSQLTestContainer memSQLTestContainer;

    @Before
    public void setUp()
    {
        assumeTrue("Cannot start MemSQL Container, skipping test.", DockerClientFactory.instance().isDockerAvailable());
        this.memSQLTestContainer = new MemSQLTestContainer();
        this.memSQLTestContainer.setup();
        MemSQLTestDatabaseAuthenticationFlowProvider flowProvider = new MemSQLTestDatabaseAuthenticationFlowProvider();
        flowProvider.configure(new MemSQLTestDatabaseAuthenticationFlowProviderConfiguration());
        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Lists.fixedSize.empty(), Optional.of(flowProvider));
    }

    @After
    public void cleanUp()
    {
        this.memSQLTestContainer.cleanup();
    }

    @Test
    public void testMemSQLUserNamePasswordConnection() throws Exception
    {
        RelationalDatabaseConnection memSQLConnection = this.memSQLTestContainer.getConnection();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((Subject) null, memSQLConnection);
        testConnection(connection, 5, "select current_user()");
    }
}
