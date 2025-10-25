// Copyright 2025 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.sqlserver.integration;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.TestVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.test.shared.framework.TestServerResource;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.Assert.fail;

public class SqlServerTestConnectionIntegration implements TestConnectionIntegration, TestServerResource
{
    private static final String USERNAME_REFERENCE = "username";
    private static final String PASSWORD_REFERENCE = "password";
    private static final String DATABASE_NAME = "tempdb";

    private final SqlServerPCTContainerWrapper sqlServerContainerWrapper = SqlServerPCTContainerWrapper.build();
    private TestVaultImplementation vault;

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "SqlServer");
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.SqlServer;
    }

    @Override
    public void setup()
    {
        long start = System.currentTimeMillis();
        System.out.println("Starting setup of dynamic connection for database: SqlServer ");
        this.sqlServerContainerWrapper.start();
        try (Connection connection = this.sqlServerContainerWrapper.getConnection();
             Statement statement = connection.createStatement())
        {
            // Create the database if it doesn't exist
            statement.executeUpdate("IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'testdb') CREATE DATABASE testdb;");
        }
        catch (Exception e)
        {
            fail(e.getMessage());
        }
        this.vault =
                new TestVaultImplementation()
                        .withValue(USERNAME_REFERENCE, this.sqlServerContainerWrapper.getUser())
                        .withValue(PASSWORD_REFERENCE, this.sqlServerContainerWrapper.getPassword());
        Vault.INSTANCE.registerImplementation(this.vault);

        long end = System.currentTimeMillis();
        System.out.println("Completed setup of dynamic connection for database: SqlServer on host:" + this.sqlServerContainerWrapper.getHost() + " and port:" + this.sqlServerContainerWrapper.getPort() + " , time taken(ms):" + (end - start));
    }

    @Override
    public RelationalDatabaseConnection getConnection()
    {
        if (!sqlServerContainerWrapper.isRunning())
        {
            this.setup();
        }
        StaticDatasourceSpecification sqlServerSpecification = new StaticDatasourceSpecification();
        sqlServerSpecification.host = this.sqlServerContainerWrapper.getHost();
        sqlServerSpecification.port = this.sqlServerContainerWrapper.getPort();
        sqlServerSpecification.databaseName = DATABASE_NAME;

        UserNamePasswordAuthenticationStrategy authStrategy = new UserNamePasswordAuthenticationStrategy();
        authStrategy.userNameVaultReference = USERNAME_REFERENCE;
        authStrategy.passwordVaultReference = PASSWORD_REFERENCE;

        RelationalDatabaseConnection connection = new RelationalDatabaseConnection(sqlServerSpecification, authStrategy, DatabaseType.SqlServer);
        connection.type = DatabaseType.SqlServer;

        return connection;
    }

    @Override
    public void cleanup()
    {
        this.sqlServerContainerWrapper.stop();
        Vault.INSTANCE.unregisterImplementation(this.vault);
    }

    @Override
    public void start() throws Exception
    {
        this.setup();
    }

    @Override
    public void shutDown() throws Exception
    {
        this.cleanup();
    }
}
