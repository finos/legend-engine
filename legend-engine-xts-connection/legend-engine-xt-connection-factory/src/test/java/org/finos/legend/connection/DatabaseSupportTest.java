// Copyright 2023 Goldman Sachs
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
import org.finos.legend.connection.impl.CoreAuthenticationMechanismType;
import org.finos.legend.engine.protocol.pure.v1.model.packageableElement.authentication.vault.PropertiesFileSecret;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.ApiKeyAuthenticationConfiguration;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.ConnectionSpecification;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.EncryptedPrivateKeyPairAuthenticationConfiguration;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.KerberosAuthenticationConfiguration;
import org.finos.legend.engine.protocol.pure.v1.packageableElement.connection.UserPasswordAuthenticationConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DatabaseSupportTest
{
    @Test
    public void testValidateDatabaseSupportBuilder()
    {
        // success
        DatabaseSupport.builder()
                .type(TestDatabaseType.TEST)
                .build();

        Exception exception;

        // error: missing database type
        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            DatabaseSupport.builder().build();
        });
        Assertions.assertEquals("Database type is missing", exception.getMessage());

        // error: multiple authentication configurations for one authentication mechanism
        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            DatabaseSupport.builder()
                    .type(TestDatabaseType.TEST)
                    .authenticationMechanisms(
                            AuthenticationMechanism.builder()
                                    .type(CoreAuthenticationMechanismType.USER_PASSWORD)
                                    .authenticationConfigurationTypes(UserPasswordAuthenticationConfiguration.class)
                                    .build(),
                            AuthenticationMechanism.builder()
                                    .type(CoreAuthenticationMechanismType.USER_PASSWORD)
                                    .authenticationConfigurationTypes(UserPasswordAuthenticationConfiguration.class)
                                    .build()
                    ).build();
        });
        Assertions.assertEquals("Found multiple authentication mechanisms with type 'UsernamePassword'", exception.getMessage());

        // error: one authentication configuration is associated with multiple authentication mechanisms
        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            DatabaseSupport.builder()
                    .type(TestDatabaseType.TEST)
                    .authenticationMechanisms(
                            AuthenticationMechanism.builder()
                                    .type(CoreAuthenticationMechanismType.USER_PASSWORD)
                                    .authenticationConfigurationTypes(UserPasswordAuthenticationConfiguration.class)
                                    .build(),
                            AuthenticationMechanism.builder()
                                    .type(CoreAuthenticationMechanismType.KERBEROS)
                                    .authenticationConfigurationTypes(UserPasswordAuthenticationConfiguration.class)
                                    .build()
                    ).build();
        });
        Assertions.assertEquals("Authentication configuration type 'UserPasswordAuthenticationConfiguration' is associated with multiple authentication mechanisms", exception.getMessage());

        // error: no authentication configurations is associated with an authentication mechanism
        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            DatabaseSupport.builder()
                    .type(TestDatabaseType.TEST)
                    .authenticationMechanisms(
                            AuthenticationMechanism.builder()
                                    .type(CoreAuthenticationMechanismType.USER_PASSWORD)
                                    .build()
                    ).build();
        });
        Assertions.assertEquals("No authentication configuration type is associated with authentication mechanism 'UsernamePassword'", exception.getMessage());
    }

    @Test
    public void testValidateConnectionBuilder()
    {
        DatabaseSupport databaseSupport = DatabaseSupport.builder()
                .type(TestDatabaseType.TEST)
                .authenticationMechanisms(
                        AuthenticationMechanism.builder()
                                    .type(CoreAuthenticationMechanismType.USER_PASSWORD)
                                .authenticationConfigurationTypes(
                                        UserPasswordAuthenticationConfiguration.class,
                                        EncryptedPrivateKeyPairAuthenticationConfiguration.class
                                )
                                .build(),
                        AuthenticationMechanism.builder()
                                    .type(CoreAuthenticationMechanismType.KERBEROS)
                                .authenticationConfigurationTypes(KerberosAuthenticationConfiguration.class)
                                .build()
                )
                .build();

        // success
        UserPasswordAuthenticationConfiguration userPasswordAuthenticationConfiguration = new UserPasswordAuthenticationConfiguration("some-user", new PropertiesFileSecret("some-secret"));
        Connection testStore = Connection.builder()
                .databaseSupport(databaseSupport)
                .identifier("test::connection")
                .authenticationMechanisms(
                        AuthenticationMechanism.builder()
                                    .type(CoreAuthenticationMechanismType.USER_PASSWORD)
                                .build()
                )
                .authenticationConfiguration(userPasswordAuthenticationConfiguration)
                .connectionSpecification(new TestConnectionSpecification())
                .build();
        Assertions.assertArrayEquals(new AuthenticationMechanismType[]{CoreAuthenticationMechanismType.USER_PASSWORD}, testStore.getAuthenticationMechanisms().toArray());

        // success: make sure if no auth mechanisms is specified, all mechanisms will be supported
        Connection testStore2 = Connection.builder()
                .databaseSupport(databaseSupport)
                .identifier("test::connection")
                .connectionSpecification(new TestConnectionSpecification())
                .authenticationConfiguration(userPasswordAuthenticationConfiguration)
                .build();
        Assertions.assertArrayEquals(new AuthenticationMechanismType[]{CoreAuthenticationMechanismType.USER_PASSWORD, CoreAuthenticationMechanismType.KERBEROS}, testStore2.getAuthenticationMechanisms().toArray());

        // success: make sure if no authentication configuration type is specified, all types will be supported
        Connection testStore3 = Connection.builder()
                .databaseSupport(databaseSupport)
                .identifier("test::connection")
                .connectionSpecification(new TestConnectionSpecification())
                .authenticationConfiguration(userPasswordAuthenticationConfiguration)
                .authenticationMechanism(AuthenticationMechanism.builder()
                                    .type(CoreAuthenticationMechanismType.USER_PASSWORD).build())
                .build();
        Assertions.assertArrayEquals(Lists.mutable.of(
                UserPasswordAuthenticationConfiguration.class,
                EncryptedPrivateKeyPairAuthenticationConfiguration.class
        ).toArray(), testStore3.getAuthenticationMechanism(CoreAuthenticationMechanismType.USER_PASSWORD).getAuthenticationConfigurationTypes().toArray());

        // failure: missing connection specification
        Exception exception;

        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            Connection.builder()
                    .databaseSupport(databaseSupport)
                    .identifier("test::connection")
                    .build();
        });
        Assertions.assertEquals("Connection specification is missing", exception.getMessage());

        // failure: missing authentication configuration
        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            Connection.builder()
                    .databaseSupport(databaseSupport)
                    .identifier("test::connection")
                    .connectionSpecification(new TestConnectionSpecification())
                    .authenticationConfiguration(new ApiKeyAuthenticationConfiguration())
                    .build();
        });
        Assertions.assertEquals("Specified authentication configuration of type 'ApiKeyAuthenticationConfiguration' is not compatible. Supported configuration type(s):\n" +
                "- UserPasswordAuthenticationConfiguration\n" +
                "- EncryptedPrivateKeyPairAuthenticationConfiguration\n" +
                "- KerberosAuthenticationConfiguration", exception.getMessage());

        // failure: specified authentication configuration is not covered by any authentication mechanisms
        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            Connection.builder()
                    .databaseSupport(databaseSupport)
                    .identifier("test::connection")
                    .connectionSpecification(new TestConnectionSpecification())
                    .build();
        });
        Assertions.assertEquals("Authentication configuration is missing", exception.getMessage());

        // failure: multiple configurations for one authentication mechanism
        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            Connection.builder()
                    .databaseSupport(databaseSupport)
                    .identifier("test::connection")
                    .authenticationMechanisms(
                            AuthenticationMechanism.builder()
                                    .type(CoreAuthenticationMechanismType.USER_PASSWORD)
                                    .build(),
                            AuthenticationMechanism.builder()
                                    .type(CoreAuthenticationMechanismType.USER_PASSWORD)
                                    .build()
                    )
                    .connectionSpecification(new TestConnectionSpecification())
                    .authenticationConfiguration(userPasswordAuthenticationConfiguration)
                    .build();
        });
        Assertions.assertEquals("Found multiple configurations for authentication mechanism 'UsernamePassword'", exception.getMessage());

        // failure: specified an authentication mechanism that is not covered by the database support
        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            Connection.builder()
                    .databaseSupport(databaseSupport)
                    .identifier("test::connection")
                    .authenticationMechanisms(
                            AuthenticationMechanism.builder()
                                    .type(CoreAuthenticationMechanismType.API_KEY)
                                    .build()
                    )
                    .connectionSpecification(new TestConnectionSpecification())
                    .authenticationConfiguration(userPasswordAuthenticationConfiguration)
                    .build();
        });
        Assertions.assertEquals("Authentication mechanism 'APIKey' is not covered by database support 'Test'. Supported mechanism(s):\n" +
                "- UsernamePassword\n" +
                "- Kerberos", exception.getMessage());

        // failure: mismatch in authentication configuration types coverage with database support for an authentication mechanism
        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            Connection.builder()
                    .databaseSupport(databaseSupport)
                    .identifier("test::connection")
                    .authenticationMechanisms(
                            AuthenticationMechanism.builder()
                                    .type(CoreAuthenticationMechanismType.USER_PASSWORD)
                                    .authenticationConfigurationTypes(KerberosAuthenticationConfiguration.class)
                                    .build()
                    )
                    .connectionSpecification(new TestConnectionSpecification())
                    .authenticationConfiguration(userPasswordAuthenticationConfiguration)
                    .build();
        });
        Assertions.assertEquals("Authentication configuration type 'KerberosAuthenticationConfiguration' is not covered by database support 'Test' for authentication mechanism 'UsernamePassword'. Supported configuration type(s):\n" +
                "- UserPasswordAuthenticationConfiguration\n" +
                "- EncryptedPrivateKeyPairAuthenticationConfiguration", exception.getMessage());
    }

    private enum TestDatabaseType implements DatabaseType
    {
        TEST()
                {
                    @Override
                    public String getIdentifier()
                    {
                        return "Test";
                    }
                }
    }

    private static class TestConnectionSpecification extends ConnectionSpecification
    {
        @Override
        public String shortId()
        {
            return null;
        }
    }
}
