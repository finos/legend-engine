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

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.authentication.DatabaseAuthenticationFlow;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProviderSelector;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.SnowflakePublicAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.SnowflakeDatasourceSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.*;
import org.pac4j.core.profile.CommonProfile;
import org.testcontainers.containers.MSSQLServerContainer;

import javax.security.auth.Subject;
import java.sql.Connection;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;

import static org.junit.Assert.assertTrue;

public class TestConnectionAcquisitionWithFlowProvider_Server extends org.finos.legend.engine.plan.execution.stores.relational.connection.test.DbSpecificTests
{
    @Rule
    public MSSQLServerContainer mssqlserver = new MSSQLServerContainer()
            .acceptLicense();

    private ConnectionManagerSelector connectionManagerSelector;

    @Override
    protected Subject getSubject()
    {
        return null;
    }

    @BeforeClass
    public static void setupTest()
    {
        Properties properties = new Properties();
        properties.put("sqlServerAccount.user", "SA");
        properties.put("sqlServerAccount.password", "A_Str0ng_Required_Password");
        Vault.INSTANCE.registerImplementation(new PropertiesVaultImplementation(properties));
    }

    @Before
    public void setup()
    {
        installFlowProvider();
        assertSnowflakeKeyPairFlowIsAvailable();

        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList());
    }

    private void installFlowProvider()
    {
        DatabaseAuthenticationFlowProviderSelector.enableLegendDefaultFlowProvider();
        boolean flowProviderPresent = DatabaseAuthenticationFlowProviderSelector.getProvider().isPresent();
        assertTrue("Flow provider is not available", flowProviderPresent);
    }

    public void assertSnowflakeKeyPairFlowIsAvailable()
    {
        SnowflakeDatasourceSpecification datasourceSpecification = new SnowflakeDatasourceSpecification();
        SnowflakePublicAuthenticationStrategy authenticationStrategy = new SnowflakePublicAuthenticationStrategy();
        RelationalDatabaseConnection relationalDatabaseConnection = new RelationalDatabaseConnection(datasourceSpecification, authenticationStrategy, DatabaseType.Snowflake);
        relationalDatabaseConnection.type = DatabaseType.Snowflake;

        DatabaseAuthenticationFlowProvider flowProvider = DatabaseAuthenticationFlowProviderSelector.getProvider().get();
        Optional<DatabaseAuthenticationFlow> flow = flowProvider.lookupFlow(relationalDatabaseConnection);
        assertTrue("snowflake keypair flow does not exist ", flow.isPresent());
    }

    @After
    public void cleanup()
    {
        DatabaseAuthenticationFlowProviderSelector.disableFlowProvider();
    }

    // TODO - Enable tests when we have Snowflake network connectivity
    @Ignore
    public void testSnowflakePublicConnection_subject() throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.snowflakeWithKeyPairSpec();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((Subject)null, systemUnderTest);
        testConnection(connection, "select * from KNOEMA_RENEWABLES_DATA_ATLAS.RENEWABLES.DATASETS");
    }

    // TODO - Enable tests when we have Snowflake network connectivity
    @Ignore
    public void testSnowflakePublicConnection_profile() throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.snowflakeWithKeyPairSpec();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((MutableList<CommonProfile>)null, systemUnderTest);
        testConnection(connection, "select * from KNOEMA_RENEWABLES_DATA_ATLAS.RENEWABLES.DATASETS");
    }

    private RelationalDatabaseConnection snowflakeWithKeyPairSpec() throws Exception
    {
        SnowflakeDatasourceSpecification snowflakeDatasourceSpecification = new SnowflakeDatasourceSpecification();
        snowflakeDatasourceSpecification.accountName = "ki79827";
        snowflakeDatasourceSpecification.region = "us-east-2";
        snowflakeDatasourceSpecification.warehouseName = "LEGENDRO_WH";
        snowflakeDatasourceSpecification.databaseName = "KNOEMA_RENEWABLES_DATA_ATLAS";
        snowflakeDatasourceSpecification.cloudType = "aws";
        SnowflakePublicAuthenticationStrategy authSpec = new SnowflakePublicAuthenticationStrategy();
        return new RelationalDatabaseConnection(snowflakeDatasourceSpecification, authSpec, DatabaseType.Snowflake);
    }

    @Test
    public void testSqlServerUserNamePasswordConnection() throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.sqlServerWithUserNamePassword();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((Subject)null, systemUnderTest);
        testConnection(connection, "select db_name() as dbname");
    }

    private RelationalDatabaseConnection sqlServerWithUserNamePassword()
    {
        StaticDatasourceSpecification sqlServerDatasourceSpecification = new StaticDatasourceSpecification();
        sqlServerDatasourceSpecification.host = "localhost";
        sqlServerDatasourceSpecification.port = mssqlserver.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT);
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