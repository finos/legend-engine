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

package org.finos.legend.connection;

import org.finos.legend.authentication.credentialprovider.CredentialProviderProvider;
import org.finos.legend.authentication.credentialprovider.impl.UserPasswordCredentialProvider;
import org.finos.legend.authentication.intermediationrule.IntermediationRuleProvider;
import org.finos.legend.authentication.intermediationrule.impl.UserPasswordFromVaultRule;
import org.finos.legend.authentication.vault.CredentialVaultProvider;
import org.finos.legend.authentication.vault.PlatformCredentialVaultProvider;
import org.finos.legend.authentication.vault.impl.PropertiesFileCredentialVault;
import org.finos.legend.connection.jdbc.StaticJDBCConnectionSpecification;
import org.finos.legend.engine.plan.execution.stores.relational.connection.postgres.test.PostgresTestContainerWrapper;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.store.relational.connection.DatabaseType;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactoryProvider;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assume.assumeTrue;

public class PostgresConnectionFactoryTest
{
    private static PostgresTestContainerWrapper postgresContainer;

    @BeforeClass
    public static void setupClass()
    {
        try
        {
            postgresContainer = PostgresTestContainerWrapper.build();
            postgresContainer.start();
        }
        catch (Exception e)
        {
            assumeTrue("Cannot start PostgreSQLContainer", false);
        }
    }

    @AfterClass
    public static void shutdownClass()
    {
        if (postgresContainer != null)
        {
            postgresContainer.stop();
        }
    }

    @Test
    public void testConnectionFactory() throws Exception
    {
        Properties properties = new Properties();
        String passRef = "passwordRef1";
        properties.put(passRef, postgresContainer.getPassword());
        PropertiesFileCredentialVault propertiesFileCredentialVault = new PropertiesFileCredentialVault(properties);

        PlatformCredentialVaultProvider platformCredentialVaultProvider = PlatformCredentialVaultProvider.builder()
                .with(propertiesFileCredentialVault)
                .build();

        CredentialVaultProvider credentialVaultProvider = CredentialVaultProvider.builder()
                .with(platformCredentialVaultProvider)
                .build();

        IntermediationRuleProvider intermediationRuleProvider = IntermediationRuleProvider.builder()
                .with(new UserPasswordFromVaultRule(credentialVaultProvider))
                .build();

        CredentialProviderProvider credentialProviderProvider = CredentialProviderProvider.builder()
                .with(new UserPasswordCredentialProvider())
                .with(intermediationRuleProvider)
                .build();

        ConnectionFactoryFlowProvider connectionFactoryFlowProvider = new DefaultConnectionFactoryFlowProvider();
        connectionFactoryFlowProvider.configure();

        ConnectionFactory connectionFactory = new ConnectionFactory(connectionFactoryFlowProvider, credentialProviderProvider);
        connectionFactory.initialize();

        connectionFactory.getConnection(
                new StaticJDBCConnectionSpecification(postgresContainer.getHost(), postgresContainer.getPort(), postgresContainer.getDatabaseName(), DatabaseType.Postgres),
                new UserPasswordAuthenticationSpecification(postgresContainer.getUser(), new PropertiesFileSecret(passRef)),
                IdentityFactoryProvider.getInstance().makeIdentityForTesting("anyone")
        );
    }
}
