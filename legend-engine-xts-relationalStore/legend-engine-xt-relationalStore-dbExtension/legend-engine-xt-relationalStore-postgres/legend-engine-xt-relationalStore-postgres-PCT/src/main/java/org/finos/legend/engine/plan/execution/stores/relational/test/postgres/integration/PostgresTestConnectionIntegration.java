// Copyright 2024 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.postgres.integration;

import org.eclipse.collections.api.list.MutableList;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.TestConnectionIntegration;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.StaticDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.shared.core.vault.VaultImplementation;
import org.finos.legend.engine.test.shared.framework.TestServerResource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Properties;

public class PostgresTestConnectionIntegration implements TestConnectionIntegration, TestServerResource
{
    private static final int DEFAULT_STARTUP_ATTEMPTS = 3;

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Postgres");
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.Postgres;
    }


    public PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer(DockerImageName.parse("postgres").withTag("17.5"));

    private VaultImplementation vaultImplementation;

    @Override
    public void setup()
    {
        this.startPostgresContainer();
        this.registerVault();
    }

    @Override
    public RelationalDatabaseConnection getConnection()
    {
        if (!postgreSQLContainer.isRunning())
        {
            // Start the container is the function is called from within the IDE
            this.setup();
        }
        StaticDatasourceSpecification postgresDatasourceSpecification = new StaticDatasourceSpecification();
        postgresDatasourceSpecification.host = this.postgreSQLContainer.getHost();
        postgresDatasourceSpecification.port = this.postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT);
        postgresDatasourceSpecification.databaseName = "test";

        UserNamePasswordAuthenticationStrategy authSpec = new UserNamePasswordAuthenticationStrategy();
        authSpec.baseVaultReference = "postgresql.";
        authSpec.userNameVaultReference = "user";
        authSpec.passwordVaultReference = "password";

        RelationalDatabaseConnection conn = new RelationalDatabaseConnection(postgresDatasourceSpecification, authSpec, DatabaseType.Postgres);
        conn.type = DatabaseType.Postgres;           // for compatibility with legacy DatabaseConnection
        conn.element = null;
        return conn;
    }

    @Override
    public void cleanup()
    {
        Vault.INSTANCE.unregisterImplementation(this.vaultImplementation);
        this.postgreSQLContainer.stop();
    }


    private void startPostgresContainer()
    {
        System.out.println("Starting setup of dynamic connection for database: Postgres ");

        long start = System.currentTimeMillis();
        this.postgreSQLContainer.withInitScript("postgres/init.sql");
        this.postgreSQLContainer.withStartupAttempts(DEFAULT_STARTUP_ATTEMPTS);
        this.postgreSQLContainer.start();
        String containerHost = this.postgreSQLContainer.getHost();
        int containerPort = this.postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT);
        long end = System.currentTimeMillis();

        System.out.println("Completed setup of dynamic connection for database: Postgres on host:" + containerHost + " and port:" + containerPort + " , time taken(ms):" + (end - start));
    }

    private void registerVault()
    {
        Properties properties = new Properties();
        properties.put("postgresql.user", "test");
        properties.put("postgresql.password", "test");
        this.vaultImplementation = new PropertiesVaultImplementation(properties);
        Vault.INSTANCE.registerImplementation(this.vaultImplementation);
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
