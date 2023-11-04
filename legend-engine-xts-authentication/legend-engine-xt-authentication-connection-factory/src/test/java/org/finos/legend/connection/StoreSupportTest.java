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
import org.finos.legend.connection.impl.DefaultStoreInstanceProvider;
import org.finos.legend.connection.impl.EncryptedPrivateKeyPairAuthenticationConfiguration;
import org.finos.legend.connection.impl.KerberosAuthenticationConfiguration;
import org.finos.legend.connection.impl.UserPasswordAuthenticationConfiguration;
import org.finos.legend.connection.protocol.AuthenticationMechanism;
import org.finos.legend.connection.protocol.AuthenticationMechanismType;
import org.finos.legend.connection.protocol.ConnectionSpecification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StoreSupportTest
{
    @Test
    public void testValidateStoreSupportBuilder()
    {
        // success
        new StoreSupport.Builder()
                .withIdentifier("test")
                .build();

        // failure
        Exception exception;

        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            new StoreSupport.Builder().build();
        });
        Assertions.assertEquals("Identifier is missing", exception.getMessage());

        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            new StoreSupport.Builder()
                    .withIdentifier("test")
                    .withAuthenticationMechanismConfigurations(
                            new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.USER_PASSWORD)
                                    .withAuthenticationConfigurationTypes(UserPasswordAuthenticationConfiguration.class)
                                    .build(),
                            new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.USER_PASSWORD)
                                    .withAuthenticationConfigurationTypes(UserPasswordAuthenticationConfiguration.class)
                                    .build()
                    ).build();
        });
        Assertions.assertEquals("Found multiple configurations for authentication mechanism 'UsernamePassword'", exception.getMessage());

        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            new StoreSupport.Builder()
                    .withIdentifier("test")
                    .withAuthenticationMechanismConfigurations(
                            new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.USER_PASSWORD)
                                    .withAuthenticationConfigurationTypes(UserPasswordAuthenticationConfiguration.class)
                                    .build(),
                            new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.KERBEROS)
                                    .withAuthenticationConfigurationTypes(UserPasswordAuthenticationConfiguration.class)
                                    .build()
                    ).build();
        });
        Assertions.assertEquals("Authentication configuration type 'UserPasswordAuthenticationConfiguration' is associated with multiple authentication mechanisms", exception.getMessage());

        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            new StoreSupport.Builder()
                    .withIdentifier("test")
                    .withAuthenticationMechanismConfigurations(
                            new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.USER_PASSWORD)
                                    .build()
                    ).build();
        });
        Assertions.assertEquals("No authentication configuration type is associated with authentication mechanism 'UsernamePassword'", exception.getMessage());
    }

    @Test
    public void testValidateStoreInstanceBuilder()
    {
        LegendEnvironment environment = new LegendEnvironment.Builder()
                .withStoreSupport(new StoreSupport.Builder()
                        .withIdentifier("test")
                        .withAuthenticationMechanismConfigurations(
                                new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.USER_PASSWORD)
                                        .withAuthenticationConfigurationTypes(
                                                UserPasswordAuthenticationConfiguration.class,
                                                EncryptedPrivateKeyPairAuthenticationConfiguration.class
                                        )
                                        .build(),
                                new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.KERBEROS)
                                        .withAuthenticationConfigurationTypes(KerberosAuthenticationConfiguration.class)
                                        .build()
                        )
                        .build())
                .build();

        // success
        StoreInstance testStore = new StoreInstance.Builder(environment)
                .withIdentifier("test-store")
                .withStoreSupportIdentifier("test")
                .withAuthenticationMechanismConfigurations(
                        new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.USER_PASSWORD)
                                .build()
                )
                .withConnectionSpecification(new TestConnectionSpecification())
                .build();
        Assertions.assertArrayEquals(new AuthenticationMechanism[]{AuthenticationMechanismType.USER_PASSWORD}, testStore.getAuthenticationMechanisms().toArray());

        // make sure if no auth mechanisms is specified, all mechanisms will be supported
        StoreInstance testStore2 = new StoreInstance.Builder(environment)
                .withIdentifier("test-store")
                .withStoreSupportIdentifier("test")
                .withConnectionSpecification(new TestConnectionSpecification())
                .build();
        Assertions.assertArrayEquals(new AuthenticationMechanism[]{AuthenticationMechanismType.USER_PASSWORD, AuthenticationMechanismType.KERBEROS}, testStore2.getAuthenticationMechanisms().toArray());

        // make sure if no authentication configuration type is specified, all types will be supported
        StoreInstance testStore3 = new StoreInstance.Builder(environment)
                .withIdentifier("test-store")
                .withStoreSupportIdentifier("test")
                .withConnectionSpecification(new TestConnectionSpecification())
                .withAuthenticationMechanismConfiguration(new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.USER_PASSWORD).build())
                .build();
        Assertions.assertArrayEquals(Lists.mutable.of(
                UserPasswordAuthenticationConfiguration.class,
                EncryptedPrivateKeyPairAuthenticationConfiguration.class
        ).toArray(), testStore3.getAuthenticationMechanismConfiguration(AuthenticationMechanismType.USER_PASSWORD).getAuthenticationConfigurationTypes().toArray());

        // failure
        Exception exception;

        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            new StoreInstance.Builder(environment)
                    .withIdentifier("test-store")
                    .withStoreSupportIdentifier("test")
                    .build();
        });
        Assertions.assertEquals("Connection specification is missing", exception.getMessage());

        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            new StoreInstance.Builder(environment)
                    .withIdentifier("test-store")
                    .withStoreSupportIdentifier("test")
                    .withAuthenticationMechanismConfigurations(
                            new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.USER_PASSWORD)
                                    .build(),
                            new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.USER_PASSWORD)
                                    .build()
                    )
                    .withConnectionSpecification(new TestConnectionSpecification())
                    .build();
        });
        Assertions.assertEquals("Found multiple configurations for authentication mechanism 'UsernamePassword'", exception.getMessage());

        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            new StoreInstance.Builder(environment)
                    .withIdentifier("test-store")
                    .withStoreSupportIdentifier("test")
                    .withAuthenticationMechanismConfigurations(
                            new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.API_KEY)
                                    .build()
                    )
                    .withConnectionSpecification(new TestConnectionSpecification())
                    .build();
        });
        Assertions.assertEquals("Authentication mechanism 'APIKey' is not covered by store support 'test'. Supported mechanism(s):\n" +
                "- UsernamePassword\n" +
                "- Kerberos", exception.getMessage());

        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            new StoreInstance.Builder(environment)
                    .withIdentifier("test-store")
                    .withStoreSupportIdentifier("test")
                    .withAuthenticationMechanismConfigurations(
                            new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.USER_PASSWORD)
                                    .withAuthenticationConfigurationTypes(KerberosAuthenticationConfiguration.class)
                                    .build()
                    )
                    .withConnectionSpecification(new TestConnectionSpecification())
                    .build();
        });
        Assertions.assertEquals("Authentication configuration type 'KerberosAuthenticationConfiguration' is not covered by store support 'test' for authentication mechanism 'UsernamePassword'. Supported configuration type(s):\n" +
                "- UserPasswordAuthenticationConfiguration\n" +
                "- EncryptedPrivateKeyPairAuthenticationConfiguration", exception.getMessage());
    }

    private static class TestConnectionSpecification extends ConnectionSpecification
    {
        @Override
        public String shortId()
        {
            return null;
        }
    }

    @Test
    public void testStoreInstanceManagement()
    {
        LegendEnvironment environment = new LegendEnvironment.Builder()
                .withStoreSupport(new StoreSupport.Builder()
                        .withIdentifier("test")
                        .withAuthenticationMechanismConfigurations(
                                new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.USER_PASSWORD)
                                        .withAuthenticationConfigurationTypes(UserPasswordAuthenticationConfiguration.class)
                                        .build(),
                                new AuthenticationMechanismConfiguration.Builder(AuthenticationMechanismType.KERBEROS)
                                        .withAuthenticationConfigurationTypes(KerberosAuthenticationConfiguration.class)
                                        .build()
                        )
                        .build())
                .build();

        StoreInstance storeInstance = new StoreInstance.Builder(environment)
                .withIdentifier("test-store")
                .withStoreSupportIdentifier("test")
                .withConnectionSpecification(new TestConnectionSpecification())
                .build();

        StoreInstanceProvider storeInstanceProvider = new DefaultStoreInstanceProvider.Builder().withStoreInstance(storeInstance).build();

        // failure
        Exception exception;

        // error: store already registered
        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            new DefaultStoreInstanceProvider.Builder().withStoreInstances(storeInstance, storeInstance).build();
        });
        Assertions.assertEquals("Found multiple store instances with identifier 'test-store'", exception.getMessage());

        // error: store not found
        exception = Assertions.assertThrows(RuntimeException.class, () ->
        {
            storeInstanceProvider.lookup("unknown");
        });
        Assertions.assertEquals("Can't find store instance with identifier 'unknown'", exception.getMessage());
    }
}
