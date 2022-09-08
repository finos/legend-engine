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
import org.finos.legend.engine.authentication.SqlServerTestDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.SqlServerTestDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.dynamicTestConnections.SqlServerTestContainers;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.security.auth.Subject;
import java.sql.Connection;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class ExternalIntegration_TestConnectionAcquisitionWithFlowProvider_SqlServer extends RelationalConnectionTest
{
    private ConnectionManagerSelector connectionManagerSelector;
    private SqlServerTestContainers sqlServerTestContainer;

    @Before
    public void setup()
    {
        startMSSQLServerContainer();
        SqlServerTestDatabaseAuthenticationFlowProvider flowProvider = new SqlServerTestDatabaseAuthenticationFlowProvider();
        flowProvider.configure(new SqlServerTestDatabaseAuthenticationFlowProviderConfiguration());
        assertStaticSQLServerFlowProviderIsAvailable(flowProvider);
        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList(), Optional.of(flowProvider));
    }

    private void startMSSQLServerContainer()
    {
        try
        {
            this.sqlServerTestContainer = new SqlServerTestContainers();
            this.sqlServerTestContainer.setup();
        }
        catch (Throwable ex)
        {
            assumeTrue("Cannot start MSSQLServerContainer", false);
        }
    }

    public void assertStaticSQLServerFlowProviderIsAvailable(SqlServerTestDatabaseAuthenticationFlowProvider flowProvider)
    {
        StaticDatasourceSpecification staticDatasourceSpecification = new StaticDatasourceSpecification();
        UserNamePasswordAuthenticationStrategy authenticationStrategy = new UserNamePasswordAuthenticationStrategy();
        RelationalDatabaseConnection relationalDatabaseConnection = new RelationalDatabaseConnection(staticDatasourceSpecification, authenticationStrategy, DatabaseType.SqlServer);
        relationalDatabaseConnection.type = DatabaseType.SqlServer;

        Optional<DatabaseAuthenticationFlow> flow = flowProvider.lookupFlow(relationalDatabaseConnection);
        assertTrue("static SqlServer flow does not exist ", flow.isPresent());
    }

    @After
    public void cleanup()
    {
        this.sqlServerTestContainer.cleanup();
    }

    @Test
    public void testSqlServerUserNamePasswordConnection() throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.sqlServerTestContainer.getConnection();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((Subject) null, systemUnderTest);
        testConnection(connection, 5, "select db_name() as dbname");
    }
}
