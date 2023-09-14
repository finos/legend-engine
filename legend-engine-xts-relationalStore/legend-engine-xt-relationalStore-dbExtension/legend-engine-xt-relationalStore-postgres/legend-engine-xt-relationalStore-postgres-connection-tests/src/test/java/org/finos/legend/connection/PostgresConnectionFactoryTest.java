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

import org.eclipse.collections.api.factory.Lists;
import org.finos.legend.authentication.vault.impl.PropertiesFileCredentialVault;
import org.finos.legend.connection.jdbc.StaticJDBCConnectionSpecification;
import org.finos.legend.connection.test.PostgresTestContainerWrapper;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.ApiKeyAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.AuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.specification.UserPasswordAuthenticationSpecification;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.shared.core.identity.Identity;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.Connection;
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
        final String PASS_REF = "passwordRef";
        properties.put(PASS_REF, postgresContainer.getPassword());
        PropertiesFileCredentialVault propertiesFileCredentialVault = new PropertiesFileCredentialVault(properties);

        RelationalDatabaseStoreSupport storeSupport = new RelationalDatabaseStoreSupport.Builder()
                .withIdentifier("Postgres")
                .withDatabaseType("Postgres")
                .withAuthenticationSpecificationTypes(Lists.mutable.of(
                        UserPasswordAuthenticationSpecification.class,
                        ApiKeyAuthenticationSpecification.class
                ))
                .build();

        EnvironmentConfiguration environmentConfiguration = new EnvironmentConfiguration.Builder()
                .withVault(propertiesFileCredentialVault)
                .withStoreSupport(storeSupport)
                .build();

        IdentityFactory identityFactory = new IdentityFactory.Builder(environmentConfiguration)
                .build();

        ConnectionFactory connectionFactory = new ConnectionFactory.Builder(environmentConfiguration)
                .withCredentialBuilderProvider(new DefaultCredentialBuilderProvider())
                .withConnectionBuilderProvider(new DefaultConnectionBuilderProvider())
                .build();

        // --------------------------------- USAGE ---------------------------------

        Identity identity = identityFactory.createIdentity(
                new IdentitySpecification.Builder()
                        .withName("test-user")
                        .build()
        );

        final String STORE_NAME = "test-store";
        ConnectionSpecification connectionSpecification = new StaticJDBCConnectionSpecification(postgresContainer.getHost(), postgresContainer.getPort(), postgresContainer.getDatabaseName());
        StoreInstance testStore = new StoreInstance.Builder(environmentConfiguration)
                .withIdentifier(STORE_NAME)
                .withStoreSupportIdentifier("Postgres")
                .withAuthenticationSpecificationTypes(Lists.mutable.of(UserPasswordAuthenticationSpecification.class))
                .withConnectionSpecification(connectionSpecification)
                .build();
        connectionFactory.registerStoreInstance(testStore);

        AuthenticationSpecification authenticationSpecification = new UserPasswordAuthenticationSpecification(postgresContainer.getUser(), new PropertiesFileSecret(PASS_REF));
        Authenticator authenticator = connectionFactory.getAuthenticator(identity, STORE_NAME, authenticationSpecification);
        Connection connection = connectionFactory.getConnection(authenticator);
    }
}
