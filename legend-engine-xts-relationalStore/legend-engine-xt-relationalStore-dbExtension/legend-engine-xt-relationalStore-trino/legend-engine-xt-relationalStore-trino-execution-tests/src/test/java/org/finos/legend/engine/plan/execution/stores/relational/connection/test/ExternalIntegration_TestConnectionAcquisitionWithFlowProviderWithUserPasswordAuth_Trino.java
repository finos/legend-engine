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

import org.finos.legend.engine.authentication.TrinoTestDatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.TrinoTestDatabaseAuthenticationFlowProviderConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.config.TemporaryTestDbConfiguration;
import org.finos.legend.engine.plan.execution.stores.relational.connection.manager.ConnectionManagerSelector;
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.dynamicTestConnections.TrinoTestContainersWithUserAndPasswordAuth;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.shared.core.vault.EnvironmentVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.finos.legend.engine.shared.core.vault.VaultImplementation;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.security.auth.Subject;

import java.io.IOException;
import java.sql.Connection;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assume.assumeTrue;

public class ExternalIntegration_TestConnectionAcquisitionWithFlowProviderWithUserPasswordAuth_Trino
        extends RelationalConnectionTest
{
    private ConnectionManagerSelector connectionManagerSelector;
    private VaultImplementation vaultImplementation;
    public TrinoTestContainersWithUserAndPasswordAuth  trinoTestContainersWithUserAndPasswordAuth;

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
    }

    private void startTrinoContainer()
    {
        try
        {
            this.trinoTestContainersWithUserAndPasswordAuth = new TrinoTestContainersWithUserAndPasswordAuth();
            this.trinoTestContainersWithUserAndPasswordAuth.setup();
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

        RelationalDatabaseConnection systemUnderTest = this.trinoTestContainersWithUserAndPasswordAuth.getConnection();
        Connection connection = this.connectionManagerSelector.getDatabaseConnection((Subject) null, systemUnderTest);
        testConnection(connection, 1, "select 1");

    }
}
