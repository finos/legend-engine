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
import org.finos.legend.engine.plan.execution.stores.relational.connection.tests.api.dynamicTestConnections.TrinoTestContainersWithDelegatedKerberosAuth;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.RelationalDatabaseConnection;
import org.finos.legend.engine.shared.core.vault.EnvironmentVaultImplementation;
import org.finos.legend.engine.shared.core.vault.Vault;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosPrincipal;

import java.io.IOException;
import java.sql.Connection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assume.assumeTrue;

public class ExternalIntegration_TestConnectionAcquisitionWithFlowProviderWithDelegatedKerberosAuth_Trino
        extends RelationalConnectionTest
{
    private ConnectionManagerSelector connectionManagerSelector;
    private TrinoTestContainersWithDelegatedKerberosAuth trinoTestContainers;

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
        TrinoTestDatabaseAuthenticationFlowProvider flowProvider = new TrinoTestDatabaseAuthenticationFlowProvider();
        flowProvider.configure(new TrinoTestDatabaseAuthenticationFlowProviderConfiguration());
        this.connectionManagerSelector = new ConnectionManagerSelector(new TemporaryTestDbConfiguration(-1), Collections.emptyList(), Optional.of(flowProvider));
    }

    private void startTrinoContainer()
    {
        try
        {
            this.trinoTestContainers = new TrinoTestContainersWithDelegatedKerberosAuth();
            this.trinoTestContainers.setup();
        }
        catch (Throwable ex)
        {
            assumeTrue("Cannot start TrinoContainer", false);
        }
    }

    @Test
    public void testTrinoWithDelegatedKerberosConnection()
            throws Exception
    {
        RelationalDatabaseConnection systemUnderTest = this.trinoTestContainers.getConnection();

        Set<KerberosPrincipal> principals = new HashSet<>();
        principals.add(new KerberosPrincipal("peter@test.com"));
        Subject testSubject = new Subject(false, principals, Sets.fixedSize.empty(), Sets.fixedSize.empty());

        Connection connection = this.connectionManagerSelector.getDatabaseConnection(testSubject, systemUnderTest);
        testConnection(connection, 1, "select 1");
    }
}
