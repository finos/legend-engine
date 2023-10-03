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

import org.finos.legend.connection.protocol.AuthenticationMechanism;
import org.finos.legend.connection.protocol.AuthenticationMechanismType;
import org.finos.legend.connection.protocol.ConnectionSpecification;
import org.junit.Assert;
import org.junit.Test;

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

        exception = Assert.assertThrows(RuntimeException.class, () ->
        {
            new StoreSupport.Builder().build();
        });
        Assert.assertEquals("Store support identifier is required", exception.getMessage());
    }

    @Test
    public void testValidateStoreInstanceBuilder()
    {
        LegendEnvironment environment = new LegendEnvironment.Builder()
                .withStoreSupport(new StoreSupport.Builder()
                        .withIdentifier("test")
                        .withAuthenticationMechanisms(
                                AuthenticationMechanismType.USER_PASSWORD,
                                AuthenticationMechanismType.KERBEROS
                        )
                        .build())
                .build();

        // success
        StoreInstance testStore = new StoreInstance.Builder(environment)
                .withIdentifier("test-store")
                .withStoreSupportIdentifier("test")
                .withAuthenticationMechanisms(AuthenticationMechanismType.USER_PASSWORD)
                .withConnectionSpecification(new TestConnectionSpecification())
                .build();
        Assert.assertArrayEquals(new AuthenticationMechanism[]{AuthenticationMechanismType.USER_PASSWORD}, testStore.getAuthenticationMechanisms().toArray());

        // make sure if no auth mechanisms is specified, all mechanisms will be supported
        StoreInstance testStore2 = new StoreInstance.Builder(environment)
                .withIdentifier("test-store")
                .withStoreSupportIdentifier("test")
                .withConnectionSpecification(new TestConnectionSpecification())
                .build();
        Assert.assertArrayEquals(new AuthenticationMechanism[]{AuthenticationMechanismType.USER_PASSWORD, AuthenticationMechanismType.KERBEROS}, testStore2.getAuthenticationMechanisms().toArray());

        // failure
        Exception exception;

        exception = Assert.assertThrows(RuntimeException.class, () ->
        {
            new StoreInstance.Builder(environment)
                    .withIdentifier("test-store")
                    .withStoreSupportIdentifier("test")
                    .build();
        });
        Assert.assertEquals("Store instance connection specification is required", exception.getMessage());

        exception = Assert.assertThrows(RuntimeException.class, () ->
        {
            new StoreInstance.Builder(environment)
                    .withIdentifier("test-store")
                    .withStoreSupportIdentifier("test")
                    .withAuthenticationMechanisms(AuthenticationMechanismType.API_KEY)
                    .withConnectionSpecification(new TestConnectionSpecification())
                    .build();
        });
        Assert.assertEquals("Store instance specified with authentication configuration types (API_KEY) which are not covered by its store support 'test'", exception.getMessage());
    }

    private static class TestConnectionSpecification extends ConnectionSpecification
    {
    }

    @Test
    public void testStoreInstanceManagement()
    {
        LegendEnvironment environment = new LegendEnvironment.Builder()
                .withStoreSupport(new StoreSupport.Builder()
                        .withIdentifier("test")
                        .withAuthenticationMechanisms(
                                AuthenticationMechanismType.USER_PASSWORD,
                                AuthenticationMechanismType.KERBEROS
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
        exception = Assert.assertThrows(RuntimeException.class, () ->
        {
            new DefaultStoreInstanceProvider.Builder().withStoreInstances(storeInstance, storeInstance).build();
        });
        Assert.assertEquals("Can't register store instance: found multiple store instances with identifier 'test-store'", exception.getMessage());

        // error: store not found
        exception = Assert.assertThrows(RuntimeException.class, () ->
        {
            storeInstanceProvider.lookup("unknown");
        });
        Assert.assertEquals("Can't find store instance with identifier 'unknown'", exception.getMessage());
    }
}
