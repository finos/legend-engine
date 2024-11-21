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

package org.finos.legend.engine.plan.execution.stores.relational.test.oracle.integration;

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
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Properties;

public class OracleTestConnectionIntegration implements TestConnectionIntegration, TestServerResource
{
    @Override
    public MutableList<String> group()
    {
        return org.eclipse.collections.impl.factory.Lists.mutable.with("Store", "Relational", "Oracle");
    }

    @Override
    public DatabaseType getDatabaseType()
    {
        return DatabaseType.Oracle;
    }


    public OracleContainer oracleContainer = new OracleContainer(DockerImageName.parse("gvenzl/oracle-xe:21-slim-faststart"));

    private VaultImplementation vaultImplementation;

    @Override
    public void setup()
    {
        this.startOracleContainer();
        this.registerVault();
    }

    @Override
    public RelationalDatabaseConnection getConnection()
    {
        if (!oracleContainer.isRunning())
        {
            // Start the container is the function is called from within the IDE
            this.setup();
        }
        StaticDatasourceSpecification postgresDatasourceSpecification = new StaticDatasourceSpecification();
        postgresDatasourceSpecification.host = this.oracleContainer.getHost();
        postgresDatasourceSpecification.port = this.oracleContainer.getOraclePort();
        postgresDatasourceSpecification.databaseName = "test";

        UserNamePasswordAuthenticationStrategy authSpec = new UserNamePasswordAuthenticationStrategy();
        authSpec.baseVaultReference = "oracle.";
        authSpec.userNameVaultReference = "user";
        authSpec.passwordVaultReference = "password";

        RelationalDatabaseConnection conn = new RelationalDatabaseConnection(postgresDatasourceSpecification, authSpec, DatabaseType.Oracle);
        conn.type = DatabaseType.Oracle;           // for compatibility with legacy DatabaseConnection
        conn.element = null;
        return conn;
    }

    @Override
    public void cleanup()
    {
        Vault.INSTANCE.unregisterImplementation(this.vaultImplementation);
        this.oracleContainer.stop();
    }


    private void startOracleContainer()
    {
        System.out.println("Starting setup of dynamic connection for database: Oracle ");

        long start = System.currentTimeMillis();
        this.oracleContainer.withUsername("system").withPassword("test");
        this.oracleContainer.start();
        String containerHost = this.oracleContainer.getHost();
        int containerPort = this.oracleContainer.getOraclePort();
        long end = System.currentTimeMillis();

        System.out.println("Completed setup of dynamic connection for database: Oracle on host:" + containerHost + " and port:" + containerPort + " , time taken(ms):" + (end - start));
    }

    private void registerVault()
    {
        Properties properties = new Properties();
        properties.put("oracle.user", "system");
        properties.put("oracle.password", "test");
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
