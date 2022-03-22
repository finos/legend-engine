// Copyright 2020 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.dynamicTestConnections;

import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.DynamicTestConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.shared.core.vault.VaultImplementation;
import org.testcontainers.containers.MSSQLServerContainer;

import java.util.Properties;

public class SqlServerTestContainers implements DynamicTestConnection
{
    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.SqlServer;
    }


    public MSSQLServerContainer mssqlserver = new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2019-latest")
                .acceptLicense();
    private VaultImplementation vaultImplementation;

    @Override
    public void setup()
    {
        this.startMSSQLServerContainer();
        this.registerVault();
    }

    private void startMSSQLServerContainer()
    {
        long start = System.currentTimeMillis();
        this.mssqlserver.start();
        String containerHost = this.mssqlserver.getHost();
        int containerPort = this.mssqlserver.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT);
        long end = System.currentTimeMillis();

        System.out.println("SqlServer database started on host:" + containerHost + " and port:" + containerPort + " , time taken(ms):"+ (end-start));
    }

    public void registerVault()
    {
        Properties properties = new Properties();
        properties.put("sqlServerAccount.user", "SA");
        properties.put("sqlServerAccount.password", "A_Str0ng_Required_Password");
        this.vaultImplementation = new PropertiesVaultImplementation(properties);
        Vault.INSTANCE.registerImplementation(this.vaultImplementation);
    }

    @Override
    public RelationalDatabaseConnection getConnection()
    {
        StaticDatasourceSpecification sqlServerDatasourceSpecification = new StaticDatasourceSpecification();
        sqlServerDatasourceSpecification.host = this.mssqlserver.getHost();
        sqlServerDatasourceSpecification.port = this.mssqlserver.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT);
        sqlServerDatasourceSpecification.databaseName = "master";
        UserNamePasswordAuthenticationStrategy authSpec = new UserNamePasswordAuthenticationStrategy();
        authSpec.baseVaultReference = "sqlServerAccount.";
        authSpec.userNameVaultReference = "user";
        authSpec.passwordVaultReference = "password";
        RelationalDatabaseConnection conn = new RelationalDatabaseConnection(sqlServerDatasourceSpecification, authSpec, DatabaseType.SqlServer);
        conn.type = DatabaseType.SqlServer;         // for compatibility with legacy DatabaseConnection
        conn.element = "";                          // placeholder , will be set by pure tests
        return conn;
    }

    @Override
    public void cleanup()
    {
        Vault.INSTANCE.unregisterImplementation(this.vaultImplementation);
        this.mssqlserver.stop();
    }
}
