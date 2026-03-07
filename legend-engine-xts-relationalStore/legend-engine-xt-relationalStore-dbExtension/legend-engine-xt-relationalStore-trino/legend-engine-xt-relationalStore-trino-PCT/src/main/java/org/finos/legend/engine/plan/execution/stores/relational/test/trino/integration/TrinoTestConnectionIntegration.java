// Copyright 2026 Goldman Sachs
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

package org.finos.legend.engine.plan.execution.stores.relational.test.trino.integration;

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
import org.testcontainers.containers.TrinoContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Properties;

public class TrinoTestConnectionIntegration implements TestConnectionIntegration, TestServerResource
{
    private static final int DEFAULT_STARTUP_ATTEMPTS = 3;

    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Trino");
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.Trino;
    }

    public TrinoContainer trinoContainer = new TrinoContainer(DockerImageName.parse("trinodb/trino:438")); // Using a specific Trino version

    private VaultImplementation vaultImplementation;

    @Override
    public void setup()
    {
        this.startTrinoContainer();
        this.registerVault();
    }

    @Override
    public RelationalDatabaseConnection getConnection()
    {
        if (!trinoContainer.isRunning())
        {
            // Start the container if the function is called from within the IDE
            this.setup();
        }
        StaticDatasourceSpecification trinoDatasourceSpecification = new StaticDatasourceSpecification();
        trinoDatasourceSpecification.host = this.trinoContainer.getHost();
        trinoDatasourceSpecification.port = this.trinoContainer.getMappedPort(8080);
        trinoDatasourceSpecification.databaseName = "memory";

        UserNamePasswordAuthenticationStrategy authSpec = new UserNamePasswordAuthenticationStrategy();
        authSpec.baseVaultReference = "trino.";
        authSpec.userNameVaultReference = "user";
        authSpec.passwordVaultReference = "password";

        RelationalDatabaseConnection conn = new RelationalDatabaseConnection(trinoDatasourceSpecification, authSpec, DatabaseType.Trino);
        conn.type = DatabaseType.Trino;
        return conn;
    }

    @Override
    public void cleanup()
    {
        Vault.INSTANCE.unregisterImplementation(this.vaultImplementation);
        this.trinoContainer.stop();
    }

    private void startTrinoContainer()
    {
        System.out.println("Starting setup of dynamic connection for database: Trino ");

        long start = System.currentTimeMillis();

        this.trinoContainer.withStartupAttempts(DEFAULT_STARTUP_ATTEMPTS);
        this.trinoContainer.withEnv("JAVA_TOOL_OPTIONS", "-Xmx2g -Xms512m");
        this.trinoContainer.start();
        String containerHost = this.trinoContainer.getHost();
        int containerPort = this.trinoContainer.getMappedPort(8080);
        long end = System.currentTimeMillis();

        System.out.println("Completed setup of dynamic connection for database: Trino on host:" + containerHost + " and port:" + containerPort + " , time taken(ms):" + (end - start));
    }

    private void registerVault()
    {
        Properties properties = new Properties();
        properties.put("trino.user", "test");
        properties.put("trino.password", "");
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
