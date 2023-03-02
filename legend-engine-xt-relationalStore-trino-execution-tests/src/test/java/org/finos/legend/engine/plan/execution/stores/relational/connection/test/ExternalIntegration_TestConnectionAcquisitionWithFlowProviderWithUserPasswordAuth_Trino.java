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

import org.eclipse.collections.api.factory.Sets;
import org.finos.legend.engine.authentication.TrinoTestDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.TrinoTestDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.dynamicTestConnections.TrinoTestContainers;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.dynamicTestConnections.TrinoTestContainersWithUserAndPasswordAuth;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.DelegatedKerberosAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.authentication.UserNamePasswordAuthenticationStrategy;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.specification.TrinoDatasourceSpecification;
import org.finos.legend.engine.shared.core.vault.EnvironmentVaultImplementation;
import org.finos.legend.engine.shared.core.vault.PropertiesVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.shared.core.vault.VaultImplementation;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.containers.TrinoContainer;
import org.testcontainers.containers.startupcheck.MinimumDurationRunningStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;

import java.io.IOException;
import java.sql.Connection;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assume.assumeTrue;

public class ExternalIntegration_TestConnectionAcquisitionWithFlowProviderWithUserPasswordAuth_Trino
        extends RelationalConnectionTest
{
    private ConnectionManagerSelector connectionManagerSelector;
    private VaultImplementation vaultImplementation;
   // private TrinoTestContainersWithUserAndPasswordAuth trinoTestContainers;
   public TrinoContainer trinoContainer = new TrinoContainer(DockerImageName.parse("trinodb/trino"))
           .withStartupCheckStrategy(new MinimumDurationRunningStartupCheckStrategy(Duration.ofSeconds(10)));

    @BeforeClass
    public static void setupTest()
    {
        Vault.INSTANCE.registerImplementation(new EnvironmentVaultImplementation());
    }

    @Before
    public void setup()
            throws IOException
    {
        startTrinoContainer();
        TrinoTestDatabaseAuthenticationFlowProviderConfiguration flowProviderConfiguration = null;
        TrinoTestDatabaseAuthenticationFlowProvider flowProvider = new TrinoTestDatabaseAuthenticationFlowProvider();
        flowProvider.configure(new TrinoTestDatabaseAuthenticationFlowProviderConfiguration());


        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList(), Optional.of(flowProvider));
        Properties properties = new Properties();
        properties.put("trino.user", "test");
        properties.put("trino.password", "test");

        this.vaultImplementation = new PropertiesVaultImplementation(properties);
        Vault.INSTANCE.registerImplementation(this.vaultImplementation);
    }

    private void startTrinoContainer()
    {
        try
        {
            this.trinoContainer.start();
        }
        catch (Throwable ex)
        {
            assumeTrue("Cannot start Trino Container", false);
        }
    }

    @Test
    public void testTrinoWithDUserPasswordConnection()
            throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = getConnection();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((Subject)null, systemUnderTest);
        testConnection(connection, 1, "select 1");
    }

    public RelationalDatabaseConnection getConnection()
    {
        TrinoDatasourceSpecification trinoDatasourceSpecification = new TrinoDatasourceSpecification();
        trinoDatasourceSpecification.host = this.trinoContainer.getHost();
        trinoDatasourceSpecification.port = this.trinoContainer.getMappedPort(8080);
        trinoDatasourceSpecification.clientTags = "cg:vega";
        trinoDatasourceSpecification.ssl = false;

        UserNamePasswordAuthenticationStrategy authSpec = new UserNamePasswordAuthenticationStrategy();
        authSpec.baseVaultReference = "trino.";
        authSpec.userNameVaultReference = "user";
        authSpec.passwordVaultReference = "password";

        RelationalDatabaseConnection conn = new RelationalDatabaseConnection(trinoDatasourceSpecification, authSpec, DatabaseType.Trino);
        conn.type = DatabaseType.Trino;         // for compatibility with legacy DatabaseConnection
        conn.element = "";                          // placeholder , will be set by pure tests
        return conn;
    }
}
