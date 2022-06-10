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

package org.finos.legend.engine.authentication;

import java.lang.reflect.Field;
import java.util.Optional;

import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProvider;
import org.finos.legend.engine.authentication.provider.DatabaseAuthenticationFlowProviderSelector;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestDatabaseAuthenticationFlowProviderSelector
{
    @Before
    public void setup() throws NoSuchFieldException, IllegalAccessException
    {
        this.resetFlowProviderSelectorSingletonState();
    }

    private void resetFlowProviderSelectorSingletonState() throws NoSuchFieldException, IllegalAccessException
    {
        Field singletonField = DatabaseAuthenticationFlowProviderSelector.class.getDeclaredField("INSTANCE");
        singletonField.setAccessible(true);
        singletonField.set(null, null);
    }

   /* @Test
    public void explicitlyLoadFlowProvider()
    {
        Optional<DatabaseAuthenticationFlowProvider> providerHolder = DatabaseAuthenticationFlowProviderSelector.getProvider(LegendDefaultDatabaseAuthenticationFlowProvider.class.getCanonicalName());
        assertTrue(providerHolder.get() instanceof LegendDefaultDatabaseAuthenticationFlowProvider);
    }

    @Test
    public void throwIfProviderNotFound()
    {
        try
        {
            DatabaseAuthenticationFlowProviderSelector.getProvider("some.ramdom.class." + System.nanoTime());
            fail("Failed to throw exception for missing flow provider");
        }
        catch (RuntimeException e)
        {
            assertTrue(e.getMessage().contains("Failed to locate database flow provider"));
        }
    }*/
}