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
import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.LegendDefaultDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.shared.core.vault.VaultImplementation;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.MSSQLServerContainer;

import javax.security.auth.Subject;
import java.sql.Connection;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

public class ExternalIntegration_TestConnectionAcquisitionWithFlowProvider_SqlServer extends org.finos.legend.engine.plan.execution.stores.relational.connection.test.DbSpecificTests
{
    public MSSQLServerContainer mssqlserver = new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2019-latest")
            .acceptLicense();

    private ConnectionManagerSelector connectionManagerSelector;
    private VaultImplementation vaultImplementation;

    @Override
    protected Subject getSubject()
    {
        return null;
    }

    @Before
    public void setup() throws Exception
    {
        startMSSQLServerContainer();

        LegendDefaultDatabaseAuthenticationFlowProvider flowProvider = new LegendDefaultDatabaseAuthenticationFlowProvider();
        flowProvider.configure(new LegendDefaultDatabaseAuthenticationFlowProviderConfiguration());
        assertStaticSQLServerFlowProviderIsAvailable(flowProvider);

        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList(), Optional.of(flowProvider));

        Properties properties = new Properties();
        properties.put("sqlServerAccount.user", "SA");
        properties.put("sqlServerAccount.password", "A_Str0ng_Required_Password");
        this.vaultImplementation = new PropertiesVaultImplementation(properties);
        Vault.INSTANCE.registerImplementation(this.vaultImplementation);
    }

    private void startMSSQLServerContainer()
    {
        try
        {
            this.mssqlserver.start();
        }
        catch (Throwable ex)
        {
            assumeTrue("Cannot start MSSQLServerContainer", false);
        }
    }

    public void assertStaticSQLServerFlowProviderIsAvailable(LegendDefaultDatabaseAuthenticationFlowProvider flowProvider)
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
        Vault.INSTANCE.unregisterImplementation(this.vaultImplementation);
        this.mssqlserver.stop();
    }

    @Test
    public void testSqlServerUserNamePasswordConnection() throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.sqlServerWithUserNamePassword();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((Subject) null, systemUnderTest);
        testConnection(connection, "select db_name() as dbname");
    }

    private RelationalDatabaseConnection sqlServerWithUserNamePassword()
    {
        StaticDatasourceSpecification sqlServerDatasourceSpecification = new StaticDatasourceSpecification();
        sqlServerDatasourceSpecification.host = "localhost";
        sqlServerDatasourceSpecification.port = this.mssqlserver.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT);
        sqlServerDatasourceSpecification.databaseName = "master";
        UserNamePasswordAuthenticationStrategy authSpec = new UserNamePasswordAuthenticationStrategy();
        authSpec.baseVaultReference = "sqlServerAccount.";
        authSpec.userNameVaultReference = "user";
        authSpec.passwordVaultReference = "password";
        RelationalDatabaseConnection conn = new RelationalDatabaseConnection(sqlServerDatasourceSpecification, authSpec, DatabaseType.SqlServer);
        conn.type = DatabaseType.SqlServer;
        return conn;
    }
}
