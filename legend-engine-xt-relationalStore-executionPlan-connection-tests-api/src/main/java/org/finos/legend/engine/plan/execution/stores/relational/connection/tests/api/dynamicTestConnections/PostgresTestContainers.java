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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import java.util.Properties;

public class PostgresTestContainers implements DynamicTestConnection
{
    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.Postgres;
    }


    public PostgreSQLContainer postgreSQLContainer = new PostgreSQLContainer(
            DockerImageName.parse("postgres").withTag("9.6.12"));

    private VaultImplementation vaultImplementation;

    @Override
    public void setup()
    {
        this.startPostgresContainer();
        this.registerVault();
    }

    private void startPostgresContainer()
    {
        System.out.println("Starting setup of dynamic connection for database: Postgres ");

        long start = System.currentTimeMillis();
        this.postgreSQLContainer.start();
        String containerHost = this.postgreSQLContainer.getHost();
        int containerPort = this.postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT);
        long end = System.currentTimeMillis();

        System.out.println("Completed setup of dynamic connection for database: Postgres on host:" + containerHost + " and port:" + containerPort + " , time taken(ms):" + (end - start));
    }

    public void registerVault()
    {
        Properties properties = new Properties();
        properties.put("postgresql.user", "test");
        properties.put("postgresql.password", "test");
        this.vaultImplementation = new PropertiesVaultImplementation(properties);
        Vault.INSTANCE.registerImplementation(this.vaultImplementation);
    }

    @Override
    public RelationalDatabaseConnection getConnection()
    {
        StaticDatasourceSpecification postgresDatasourceSpecification = new StaticDatasourceSpecification();
        postgresDatasourceSpecification.host = "localhost";
        postgresDatasourceSpecification.port = this.postgreSQLContainer.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT);
        postgresDatasourceSpecification.databaseName = "test";
        UserNamePasswordAuthenticationStrategy authSpec = new UserNamePasswordAuthenticationStrategy();
        authSpec.baseVaultReference = "postgresql.";
        authSpec.userNameVaultReference = "user";
        authSpec.passwordVaultReference = "password";
        RelationalDatabaseConnection conn = new RelationalDatabaseConnection(postgresDatasourceSpecification, authSpec, DatabaseType.Postgres);
        conn.type = DatabaseType.Postgres;           // for compatibility with legacy DatabaseConnection
        conn.element = "";                          // placeholder , will be set by pure tests
        return conn;
    }

    @Override
    public void cleanup()
    {
        Vault.INSTANCE.unregisterImplementation(this.vaultImplementation);
        this.postgreSQLContainer.stop();
    }

}