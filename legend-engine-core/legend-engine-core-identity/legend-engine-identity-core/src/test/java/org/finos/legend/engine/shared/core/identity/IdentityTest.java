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

package org.finos.legend.engine.shared.core.identity;

import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.MutableListFactoryImpl;
import org.finos.legend.engine.shared.core.identity.credential.AnonymousCredential;
import org.finos.legend.engine.shared.core.identity.factory.IdentityFactory;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class IdentityTest
{

    @Test
    public void shouldReturnUnknownIdentityWhenNoFactoriesAvailable() throws Exception
    {
        setFactories(MutableListFactoryImpl.INSTANCE.empty());
        Object authSource = new Object();
        Identity identity = Identity.makeIdentity(authSource);

        assertEquals("_UNKNOWN_", identity.getName());
    }

    @Test
    public void shouldReturnIdentityWhenSingleFactoryProvided() throws Exception
    {
        IdentityFactory factory = mock(IdentityFactory.class);
        when(factory.makeIdentity(any())).thenReturn(Optional.of(new Identity("TestUser", new AnonymousCredential())));
        MutableList<IdentityFactory> factories = MutableListFactoryImpl.INSTANCE.with(factory);
        setFactories(factories);

        Object authSource = new Object();
        Identity identity = Identity.makeIdentity(authSource);

        assertEquals("TestUser", identity.getName());
        assertEquals(1, identity.countCredentials());

    }

    @Test
    public void shouldCombineCredentialsWhenMultipleFactoriesProvided() throws Exception
    {
        IdentityFactory factory1 = mock(IdentityFactory.class);
        when(factory1.makeIdentity(any())).thenReturn(Optional.of(new Identity("TestUser", new AnonymousCredential())));

        IdentityFactory factory2 = mock(IdentityFactory.class);
        when(factory2.makeIdentity(any())).thenReturn(Optional.of(new Identity("TestUser", new AnonymousCredential())));

        MutableList<IdentityFactory> factories = MutableListFactoryImpl.INSTANCE.with(factory1,factory2);
        setFactories(factories);

        Object authSource = new Object();
        Identity identity = Identity.makeIdentity(authSource);

        assertEquals("TestUser", identity.getName());
        assertEquals(2, identity.countCredentials());
    }

    private void setFactories(MutableList<IdentityFactory> factories) throws Exception
    {
        Field factoriesField = Identity.class.getDeclaredField("FACTORIES");
        factoriesField.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(factoriesField, factoriesField.getModifiers() & ~Modifier.FINAL);

        factoriesField.set(null, factories);
    }
}